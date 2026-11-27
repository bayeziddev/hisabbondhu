package com.hisabbondhu.app.util

import android.content.Context
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.net.URLDecoder
import kotlin.coroutines.resume

/**
 * Reads the referral code a friend's share link carries through the Play Store:
 * https://play.google.com/store/apps/details?id=…&referrer=ref%3DK7M2QX  →  "K7M2QX".
 * Organic installs carry "utm_source=google-play&utm_medium=organic" and give null.
 */
object InstallReferrer {
    private val CODE = Regex("(?:^|&)ref=([A-Za-z0-9-]{4,12})(?:&|$)")

    fun parse(referrer: String?): String? {
        if (referrer.isNullOrBlank()) return null
        val decoded = runCatching { URLDecoder.decode(referrer, "UTF-8") }.getOrDefault(referrer)
        return CODE.find(decoded)?.groupValues?.get(1)?.replace("-", "")?.uppercase()
    }

    /** The raw referrer string, or null (not installed from Play, service missing, or timeout). */
    suspend fun read(context: Context): String? = withTimeoutOrNull(10_000) {
        suspendCancellableCoroutine { cont ->
            val client = InstallReferrerClient.newBuilder(context.applicationContext).build()
            fun finish(value: String?) {
                runCatching { client.endConnection() }
                if (cont.isActive) cont.resume(value)
            }
            cont.invokeOnCancellation { runCatching { client.endConnection() } }
            try {
                client.startConnection(object : InstallReferrerStateListener {
                    override fun onInstallReferrerSetupFinished(responseCode: Int) {
                        val ref = if (responseCode == InstallReferrerClient.InstallReferrerResponse.OK) {
                            runCatching { client.installReferrer.installReferrer }.getOrNull()
                        } else {
                            null
                        }
                        finish(ref)
                    }

                    override fun onInstallReferrerServiceDisconnected() = finish(null)
                })
            } catch (e: Exception) {
                finish(null)
            }
        }
    }
}
