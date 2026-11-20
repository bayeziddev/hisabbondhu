package com.hisabbondhu.app

import android.content.Intent
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Walks through the app like a user (UiAutomator) and saves Play Store screenshots.
 * Run by android/tools/screenshot-tour.sh (args: prefix=phone|tablet7|tablet10, lang=bn|en).
 * Each step is independent: if one fails, the rest still run.
 */
@RunWith(AndroidJUnit4::class)
class ScreenshotTour {
    private val inst = InstrumentationRegistry.getInstrumentation()
    private val device = UiDevice.getInstance(inst)
    private val args = InstrumentationRegistry.getArguments()
    private val prefix = args.getString("prefix") ?: "phone"
    private val lang = args.getString("lang") ?: "bn"
    private val bn = lang == "bn"
    private val app get() = ApplicationProvider.getApplicationContext<HisabApp>()
    private val out by lazy { File(app.filesDir, "screenshots").apply { mkdirs() } }

    private fun log(msg: String) = Log.i("ScreenshotTour", msg)

    private fun shot(name: String) {
        device.waitForIdle(3_000)
        Thread.sleep(1_200)
        val f = File(out, "${prefix}_${lang}_$name.png")
        val ok = device.takeScreenshot(f)
        log("shot $name ok=$ok")
    }

    private fun find(tag: String, timeout: Long = 20_000): UiObject2? =
        device.wait(Until.findObject(By.res(tag)), timeout).also { if (it == null) log("not found: $tag") }

    private fun tap(tag: String, timeout: Long = 20_000): Boolean {
        val o = find(tag, timeout) ?: return false
        o.click()
        device.waitForIdle(2_000)
        return true
    }

    private fun type(tag: String, text: String) {
        find(tag)?.text = text
        device.waitForIdle(1_000)
    }

    private fun back() {
        device.pressBack()
        device.waitForIdle(2_000)
        Thread.sleep(600)
    }

    private fun step(name: String, block: () -> Unit) {
        log("step $name")
        try { block() } catch (t: Throwable) { Log.e("ScreenshotTour", "step $name failed", t) }
    }

    private fun launchApp() {
        val intent = app.packageManager.getLaunchIntentForPackage(app.packageName)!!
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        app.startActivity(intent)
        device.wait(Until.hasObject(By.pkg(app.packageName).depth(0)), 30_000)
    }

    @Test
    fun tour() {
        device.executeShellCommand("cmd uimode night no")
        launchApp()

        step("language") {
            find("lang_start", 30_000)
            if (!bn) tap("lang_en")
            shot("01_language")
            tap("lang_start")
            Thread.sleep(2_500) // activity is recreated in the chosen language
        }
        step("signin") {
            type("auth_email", "rahim.store@gmail.com")
            shot("02_signin")
        }
        step("profile") {
            runBlocking {
                app.container.settings.saveAuth(
                    "demo-user", "rahim.store@gmail.com", null, null, false, "demo-access", "demo-refresh",
                )
            }
            type("profile_name", if (bn) "রহিম উদ্দিন" else "Rahim Uddin")
            type("profile_phone", "01712345678")
            shot("03_profile")
        }
        step("setup") {
            runBlocking { app.container.settings.saveProfile(if (bn) "রহিম উদ্দিন" else "Rahim Uddin", "+8801712345678", true) }
            type("setup_name", if (bn) "রহিম জেনারেল স্টোর" else "Rahim General Store")
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
            find("qa_sale", 30_000)
            Thread.sleep(1_500)
            shot("05_home")
        }
        step("sale") {
            if (tap("qa_sale")) {
                type("amount", "1250")
                shot("06_sale_entry")
                back()
            }
        }
        step("customers") {
            if (tap("tab_parties")) {
                find("party_row")
                shot("07_customers")
                find("party_row")?.click()
                Thread.sleep(1_200)
                shot("08_customer_ledger")
                back()
            }
        }
        step("stock") { if (tap("tab_products")) shot("09_stock") }
        step("reports") { if (tap("tab_reports")) shot("10_reports") }
        step("more") { if (tap("tab_more")) shot("11_more") }
        step("contact") {
            if (tap("nav_contact")) {
                shot("12_contact")
                back()
            }
        }
        step("dark") {
            device.executeShellCommand("cmd uimode night yes")
            Thread.sleep(3_000)
            if (tap("tab_home")) {
                find("qa_sale")
                shot("13_home_dark")
            }
            device.executeShellCommand("cmd uimode night no")
        }
        log("tour finished")
    }
}
