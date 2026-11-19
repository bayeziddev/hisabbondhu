package com.hisabbondhu.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hisabbondhu.app.ui.LocalBengali
import com.hisabbondhu.app.ui.nav.AppRoot
import com.hisabbondhu.app.ui.nav.StartState
import com.hisabbondhu.app.ui.theme.HisabTheme
import kotlinx.coroutines.flow.combine

class MainActivity : AppCompatActivity() {

    @Volatile private var ready = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        splash.setKeepOnScreenCondition { !ready }

        val container = (application as HisabApp).container
        val startFlow = combine(container.settings.languageChosen, container.settings.onboardingDone) { lang, done ->
            when {
                !lang -> StartState.Language
                !done -> StartState.Setup
                else -> StartState.Main
            }
        }

        setContent {
            val bengali = LocalConfiguration.current.locales[0].language == "bn"
            val start by startFlow.collectAsStateWithLifecycle<StartState?>(initialValue = null)
            LaunchedEffect(start) { if (start != null) ready = true }
            CompositionLocalProvider(LocalBengali provides bengali) {
                HisabTheme {
                    start?.let { AppRoot(container, it) }
                }
            }
        }
    }
}
