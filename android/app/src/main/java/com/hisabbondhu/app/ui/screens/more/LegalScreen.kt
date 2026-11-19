package com.hisabbondhu.app.ui.screens.more

import android.annotation.SuppressLint
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import com.hisabbondhu.app.BuildConfig
import com.hisabbondhu.app.R
import com.hisabbondhu.app.ui.LocalBengali
import com.hisabbondhu.app.ui.components.AppTopBar
import com.hisabbondhu.app.util.Share

private fun legalTitle(page: String): Int = when (page) {
    "terms" -> R.string.terms
    "your-rights" -> R.string.your_rights
    "licenses" -> R.string.open_source_licenses
    else -> R.string.privacy_policy
}

/** Full legal text bundled in the APK (same files as the website) so it is readable offline. */
@Composable
fun LegalScreen(nav: NavHostController, page: String) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            AppTopBar(
                stringResource(legalTitle(page)),
                onBack = { nav.popBackStack() },
                actions = {
                    if (page != "licenses") {
                        IconButton(onClick = { Share.openUrl(context, BuildConfig.WEBSITE_URL + "$page.html") }) {
                            Icon(Icons.Rounded.OpenInBrowser, stringResource(R.string.open_website))
                        }
                    }
                },
            )
        },
    ) { padding ->
        LegalWebView(page, Modifier.fillMaxSize().padding(padding))
    }
}

/** Used inside a dialog during onboarding. */
@Composable
fun LegalContent(page: String, onClose: () -> Unit) {
    Scaffold(
        topBar = {
            AppTopBar(
                stringResource(legalTitle(page)),
                actions = { IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, stringResource(R.string.close)) } },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) { LegalWebView(page, Modifier.fillMaxSize()) }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun LegalWebView(page: String, modifier: Modifier) {
    val lang = if (LocalBengali.current) "bn" else "en"
    val asset = if (page == "licenses") "file:///android_asset/licenses/index.html" else "file:///android_asset/legal/$page.html"
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                // Only our own bundled files are loaded; the inline script picks the language and hides site navigation.
                settings.javaScriptEnabled = true
                settings.allowFileAccess = true
                settings.domStorageEnabled = false
                settings.userAgentString = settings.userAgentString + " HisabBondhuApp lang/$lang"
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        val url = request.url.toString()
                        if (url.startsWith("file:///android_asset/")) return false
                        Share.openUrl(ctx, url)
                        return true
                    }
                }
                loadUrl(asset)
            }
        },
    )
}
