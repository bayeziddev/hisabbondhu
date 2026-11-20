package com.hisabbondhu.app

import android.graphics.Bitmap
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Walks through the app and saves screenshots for the Play Store.
 * Run by android/tools/screenshot-tour.sh (args: prefix=phone|tablet7|tablet10, lang=bn|en).
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class ScreenshotTour {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val args = InstrumentationRegistry.getArguments()
    private val prefix = args.getString("prefix") ?: "phone"
    private val lang = args.getString("lang") ?: "bn"
    private val bn = lang == "bn"
    private val app get() = ApplicationProvider.getApplicationContext<HisabApp>()
    private val out by lazy { File(app.filesDir, "screenshots").apply { mkdirs() } }

    private fun shot(name: String) {
        compose.waitForIdle()
        Thread.sleep(900)
        val bmp: Bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot() ?: return
        File(out, "${prefix}_${lang}_$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        Log.i("ScreenshotTour", "saved $name")
    }

    private fun waitTag(tag: String, timeout: Long = 20_000) =
        compose.waitUntil(timeout) { compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }

    private fun step(name: String, block: () -> Unit) {
        try { block() } catch (t: Throwable) { Log.e("ScreenshotTour", "step $name failed", t) }
    }

    @Test
    fun tour() {
        step("language") {
            waitTag("lang_start")
            if (!bn) compose.onNodeWithTag("lang_en").performClick()
            shot("01_language")
            compose.onNodeWithTag("lang_start").performClick()
        }
        step("signin") {
            waitTag("auth_email")
            compose.onNodeWithTag("auth_email").performTextInput("rahim.store@gmail.com")
            Espresso.closeSoftKeyboard()
            shot("02_signin")
        }
        step("profile") {
            runBlocking {
                app.container.settings.saveAuth(
                    "demo-user", "rahim.store@gmail.com", null, null, false, "demo-access", "demo-refresh",
                )
            }
            waitTag("profile_name")
            compose.onNodeWithTag("profile_name").performTextInput(if (bn) "রহিম উদ্দিন" else "Rahim Uddin")
            compose.onNodeWithTag("profile_phone").performTextInput("01712345678")
            Espresso.closeSoftKeyboard()
            shot("03_profile")
        }
        step("setup") {
            runBlocking { app.container.settings.saveProfile(if (bn) "রহিম উদ্দিন" else "Rahim Uddin", "+8801712345678", true) }
            waitTag("setup_name")
            compose.onNodeWithTag("setup_name").performTextInput(if (bn) "রহিম জেনারেল স্টোর" else "Rahim General Store")
            Espresso.closeSoftKeyboard()
            shot("04_setup")
        }
        step("home") {
            runBlocking {
                val c = app.container
                DemoData.seed(c, bn)
                c.settings.saveBusiness(DemoData.business(bn), dirty = false)
                c.settings.setBusinessLinked(true)
                c.settings.setSyncResult(System.currentTimeMillis(), null)
                c.settings.setOnboardingDone()
            }
            waitTag("qa_sale")
            Thread.sleep(1500)
            shot("05_home")
        }
        step("sale") {
            compose.onNodeWithTag("qa_sale").performClick()
            waitTag("amount")
            compose.onNodeWithTag("amount").performTextInput("1250")
            Espresso.closeSoftKeyboard()
            shot("06_sale_entry")
            Espresso.pressBack()
        }
        step("customers") {
            waitTag("tab_parties")
            compose.onNodeWithTag("tab_parties").performClick()
            waitTag("party_row")
            shot("07_customers")
            compose.onAllNodesWithTag("party_row")[0].performClick()
            Thread.sleep(800)
            shot("08_customer_ledger")
            Espresso.pressBack()
        }
        step("products") {
            waitTag("tab_products")
            compose.onNodeWithTag("tab_products").performClick()
            shot("09_stock")
        }
        step("reports") {
            compose.onNodeWithTag("tab_reports").performClick()
            shot("10_reports")
        }
        step("more") {
            compose.onNodeWithTag("tab_more").performClick()
            shot("11_more")
        }
        step("contact") {
            waitTag("nav_contact")
            compose.onNodeWithTag("nav_contact").performClick()
            shot("12_contact")
            Espresso.pressBack()
        }
        step("dark") {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
            Thread.sleep(1500)
            waitTag("tab_home")
            compose.onNodeWithTag("tab_home").performClick()
            waitTag("qa_sale")
            shot("13_home_dark")
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
        }
    }
}
