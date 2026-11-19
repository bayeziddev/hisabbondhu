package com.hisabbondhu.app.data.repo

import android.os.Build
import com.hisabbondhu.app.core.Dates
import com.hisabbondhu.app.core.newId
import com.hisabbondhu.app.data.local.AppDatabase
import com.hisabbondhu.app.data.prefs.BusinessProfile
import com.hisabbondhu.app.data.prefs.SettingsStore
import com.hisabbondhu.app.data.prefs.SyncCursors
import com.hisabbondhu.app.data.remote.Network
import com.hisabbondhu.app.data.remote.dto.OtpRequest
import com.hisabbondhu.app.data.remote.dto.OtpRequestResponse
import com.hisabbondhu.app.data.remote.dto.RefreshRequest
import com.hisabbondhu.app.data.remote.dto.VerifyOtpRequest
import com.hisabbondhu.app.sync.SyncScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository(
    private val db: AppDatabase,
    private val settings: SettingsStore,
    private val network: Network,
    private val scheduler: SyncScheduler,
) {
    suspend fun requestOtp(phone: String): OtpRequestResponse =
        network.api.requestOtp(OtpRequest(phone))

    /** Verifies the OTP, then links this phone's offline data to the account's business and syncs. */
    suspend fun verifyOtp(phone: String, code: String) {
        val res = network.api.verifyOtp(
            VerifyOtpRequest(
                phone = phone,
                code = code,
                deviceId = settings.deviceId(),
                deviceName = "${Build.MANUFACTURER} ${Build.MODEL}".trim().take(60),
            ),
        )
        settings.saveAuth(res.user?.id.orEmpty(), res.user?.phone ?: phone, res.accessToken, res.refreshToken)
        network.onTokensChanged(res.accessToken)

        val local = settings.currentBusiness() ?: BusinessProfile(
            id = newId(), name = "", ownerName = "", phone = phone, address = "",
            type = "general_store", openingCashPaisa = 0, updatedAt = Dates.now(),
        ).also { settings.saveBusiness(it, dirty = true) }

        val server = network.api.linkBusiness(local.toDto())
        if (server.id != local.id) {
            // The account already owns a business: merge this device's offline entries into it.
            settings.saveBusiness(server.toProfile(), dirty = false)
            db.dao().markAllPartiesPending()
            db.dao().markAllProductsPending()
            db.dao().markAllTxPending()
        } else if (server.updatedAt > local.updatedAt) {
            settings.saveBusiness(server.toProfile(), dirty = false)
        } else {
            settings.clearBusinessDirty(local.updatedAt)
        }
        settings.saveCursors(SyncCursors())
        scheduler.requestSync(delaySeconds = 0)
    }

    suspend fun pendingCount(): Int = db.dao().pendingCounts().total

    /** Logs out and removes local data from this phone (cloud copy stays safe). */
    suspend fun logout() {
        val refresh = settings.currentAuth().refreshToken
        if (refresh != null) runCatching { network.api.logout(RefreshRequest(refresh)) }
        wipeLocal()
    }

    /** Permanently deletes the cloud account (if logged in) and all data on this phone. */
    suspend fun deleteAccountAndData() {
        if (settings.currentAuth().loggedIn) {
            network.api.deleteAccount() // must succeed before we wipe local copy
        }
        wipeLocal()
    }

    suspend fun wipeLocal() {
        scheduler.cancelAll()
        withContext(Dispatchers.IO) { db.clearAllTables() }
        settings.clearAll()
        network.onTokensChanged(null)
    }
}
