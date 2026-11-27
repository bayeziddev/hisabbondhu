package com.hisabbondhu.app.data.repo

import android.content.Context
import com.hisabbondhu.app.data.prefs.SettingsStore
import com.hisabbondhu.app.data.remote.Network
import com.hisabbondhu.app.data.remote.dto.RedeemRequest
import com.hisabbondhu.app.data.remote.dto.ReferralApplyRequest
import com.hisabbondhu.app.data.remote.dto.ReferralSummary
import com.hisabbondhu.app.util.InstallReferrer
import retrofit2.HttpException

/** Referral codes and Hisab points. Everything lives on the server; this needs the internet. */
class ReferralRepository(
    private val network: Network,
    private val settings: SettingsStore,
) {
    suspend fun summary(): ReferralSummary = network.api.referral()

    suspend fun apply(code: String): ReferralSummary {
        val res = network.api.applyReferral(ReferralApplyRequest(code.trim()))
        settings.setReferralRetry(null)
        return res
    }

    suspend fun redeem(optionId: String): ReferralSummary = network.api.redeemPoints(RedeemRequest(optionId))

    /** Code to pre-fill on the profile step (from the friend's Play Store link). */
    suspend fun codeFromInstall(): String? = settings.referralFromInstall()

    /** Keep a code the user entered while offline; [applyRetryIfAny] sends it later. */
    suspend fun rememberForRetry(code: String) = settings.setReferralRetry(code.trim())

    /** First launch after install: remember the code a referral link carried. Runs once per install. */
    suspend fun captureInstallReferrer(context: Context) {
        if (settings.installReferrerChecked()) return
        InstallReferrer.parse(InstallReferrer.read(context))?.let { settings.setReferralFromInstall(it) }
        settings.setInstallReferrerChecked()
    }

    /** Sends a code that was entered offline. A code the server refuses is dropped; a network error keeps it. */
    suspend fun applyRetryIfAny() {
        val code = settings.referralRetry() ?: return
        if (!settings.currentAuth().loggedIn) return
        try {
            apply(code)
        } catch (e: HttpException) {
            settings.setReferralRetry(null)
        }
    }
}
