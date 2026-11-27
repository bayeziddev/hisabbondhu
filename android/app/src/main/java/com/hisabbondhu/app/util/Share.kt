package com.hisabbondhu.app.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.FileProvider
import com.hisabbondhu.app.R
import java.io.File

object Share {
    /** The Play Store listing. Always the release id — debug builds are "….debug" and have no listing. */
    const val PLAY_PACKAGE = "com.hisabbondhu.myapp"
    const val PLAY_URL = "https://play.google.com/store/apps/details?id=$PLAY_PACKAGE"

    fun text(context: Context, text: String, title: String? = null) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        start(context, Intent.createChooser(send, title ?: context.getString(R.string.share_via)))
    }

    fun sms(context: Context, phone: String?, text: String) {
        val uri = Uri.parse("smsto:" + (phone ?: ""))
        val intent = Intent(Intent.ACTION_SENDTO, uri).apply { putExtra("sms_body", text) }
        start(context, intent)
    }

    /** Opens WhatsApp chat with a BD number pre-filled, falls back to the share sheet. */
    fun whatsApp(context: Context, phone: String?, text: String) {
        val digits = phone?.filter { it.isDigit() }.orEmpty()
        val intl = when {
            digits.startsWith("880") -> digits
            digits.startsWith("0") -> "88$digits"
            else -> digits
        }
        val uri = Uri.parse("https://wa.me/$intl?text=" + Uri.encode(text))
        val intent = Intent(Intent.ACTION_VIEW, uri)
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
            text(context, text)
        }
    }

    fun dial(context: Context, phone: String) {
        start(context, Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
    }

    fun email(context: Context, to: String, subject: String, body: String = "") {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply {
            putExtra(Intent.EXTRA_EMAIL, arrayOf(to))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }
        start(context, intent)
    }

    fun openUrl(context: Context, url: String) {
        try {
            CustomTabsIntent.Builder().setShowTitle(true).build().launchUrl(context, Uri.parse(url))
        } catch (_: Exception) {
            start(context, Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

    fun file(context: Context, file: File, mime: String, title: String) {
        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        start(context, Intent.createChooser(send, title))
    }

    fun exportDir(context: Context): File = File(context.cacheDir, "exports").apply { mkdirs() }

    private fun start(context: Context, intent: Intent) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, R.string.no_app_found, Toast.LENGTH_SHORT).show()
        }
    }
}
