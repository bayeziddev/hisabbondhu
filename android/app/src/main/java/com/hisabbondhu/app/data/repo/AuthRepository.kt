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
import com.hisabbondhu.app.data.remote.dto.ProfileRequest
import com.hisabbondhu.app.data.remote.dto.RefreshRequest
import com.hisabbondhu.app.data.remote.dto.VerifyOtpRequest
import com.hisabbondhu.app.sync.SyncScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Every shop has its own account (email + one-time code). After the first login the app keeps
 * working fully offline; the session refreshes itself whenever the phone is online.
 */
class AuthRepository(
    private val db: AppDatabase,
    private val settings: SettingsStore,
    private val network: Network,
    private val scheduler: SyncScheduler,
) {
    suspend fun requestOtp(email: String): OtpRequestResponse =
        network.api.requestOtp(OtpRequest(email.trim().lowercase()))

    /** Verifies the emailed code. Restores the account's business (and its data) if it already exists. */
    suspend fun verifyOtp(email: String, code: String) {
        val res = network.api.verifyOtp(
            VerifyOtpRequest(
                email = email.trim().lowercase(),
                code = code,
                deviceId = settings.deviceId(),
                deviceName = "${Build.MANUFACTURER} ${Build.MODEL}".trim().take(60),
            ),
        )
        // A different account on this phone must never see or merge the previous shop's data.
        val owner = settings.dataOwner()
        if (owner != null && owner != res.user.id) wipeLocal()

        settings.saveAuth(
            userId = res.user.id, email = res.user.email, name = res.user.name, phone = res.user.phone,
            optIn = res.user.marketingOptIn, access = res.accessToken, refresh = res.refreshToken,
        )
        network.onTokensChanged(res.accessToken)

        val remote = runCatching { network.api.businesses().businesses.firstOrNull() }.getOrNull()
        if (remote != null) {
            val local = settings.currentBusiness()
            if (local == null || local.id == remote.id || !settings.isBusinessLinked()) {
                if (local != null && local.id != remote.id) {
                    // Unlinked offline entries made before this login are merged into the account's business.
                    db.dao().markAllPartiesPending(); db.dao().markAllProductsPending(); db.dao().markAllTxPending()
                }
                settings.saveBusiness(remote.toProfile(), dirty = false)
                settings.setBusinessLinked(true)
                settings.setOnboardingDone()
            }
        }
        settings.saveCursors(SyncCursors())
        scheduler.requestSync(delaySeconds = 0)
    }

    /** Name + Bangladeshi mobile number are required for every account (used for support and leads). */
    suspend fun updateProfile(name: String, phone: String, marketingOptIn: Boolean) {
        val res = network.api.updateProfile(ProfileRequest(name.trim(), phone, marketingOptIn))
        settings.saveProfile(res.user.name, res.user.phone, res.user.marketingOptIn)
    }

    /** Creates the shop locally and links it to the account right away (or on the next sync if offline). */
    suspend fun createBusiness(profile: BusinessProfile) {
        settings.saveBusiness(profile, dirty = true)
        settings.setOnboardingDone()
        runCatching {
            val server = network.api.linkBusiness(profile.toDto())
            if (server.id != profile.id) settings.saveBusiness(server.toProfile(), dirty = false)
            settings.setBusinessLinked(true)
            settings.clearBusinessDirty(profile.updatedAt)
        }
        scheduler.requestSync(delaySeconds = 0)
    }

    fun newBusiness(name: String, owner: String, phone: String, address: String, type: String, openingCash: Long) =
        BusinessProfile(newId(), name, owner, phone, address, type, openingCash, Dates.now())

    suspend fun pendingCount(): Int = db.dao().pendingCounts().total

    /** Logs out and removes this shop's data from the phone (the cloud copy stays safe). */
    suspend fun logout() {
        val refresh = settings.currentAuth().refreshToken
        if (refresh != null) runCatching { network.api.logout(RefreshRequest(refresh)) }
        wipeLocal()
    }

    /** Permanently deletes the account, all cloud data and everything on this phone. */
    suspend fun deleteAccountAndData() {
        if (settings.currentAuth().loggedIn) network.api.deleteAccount()
        wipeLocal()
    }

    private suspend fun wipeLocal() {
        scheduler.cancelAll()
        withContext(Dispatchers.IO) { db.clearAllTables() }
        settings.clearAll(keepLanguage = true)
        network.onTokensChanged(null)
    }
}
