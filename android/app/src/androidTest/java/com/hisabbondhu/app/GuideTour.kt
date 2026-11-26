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
 * Walks every feature of the app in Bangla and saves a screenshot of each step, for the
 * full user-guide video. Run by android/tools/guide-tour.sh on a 1080x1920 emulator.
 * Each step is independent: if one fails, the tour goes back home and carries on.
 */
@RunWith(AndroidJUnit4::class)
class GuideTour {
    private val inst = InstrumentationRegistry.getInstrumentation()
    private val device = UiDevice.getInstance(inst)
    private val app get() = ApplicationProvider.getApplicationContext<HisabApp>()
    private val out by lazy { File(app.filesDir, "screenshots").apply { mkdirs() } }

    private fun log(msg: String) = Log.i("GuideTour", msg)

    private fun shot(name: String) {
        device.waitForIdle(3_000)
        Thread.sleep(1_000)
        val ok = device.takeScreenshot(File(out, "guide_$name.png"))
        log("shot $name ok=$ok")
    }

    private fun pause(ms: Long = 700) { device.waitForIdle(2_000); Thread.sleep(ms) }

    // --- by test tag -------------------------------------------------------------------
    private fun find(tag: String, timeout: Long = 15_000): UiObject2? =
        device.wait(Until.findObject(By.res(tag)), timeout).also { if (it == null) log("not found: $tag") }

    private fun tap(tag: String, timeout: Long = 15_000): Boolean {
        val o = find(tag, timeout) ?: return false
        o.click(); pause(); return true
    }

    private fun type(tag: String, text: String) { find(tag)?.text = text; pause(400) }

    // --- by visible text ---------------------------------------------------------------
    private fun has(text: String, timeout: Long = 8_000): Boolean =
        device.wait(Until.hasObject(By.text(text)), timeout) == true

    /** Taps the top-most (or bottom-most) element showing exactly [text]. */
    private fun tapText(text: String, timeout: Long = 8_000, last: Boolean = false): Boolean {
        if (!has(text, timeout)) { log("not found text: $text"); return false }
        val all = device.findObjects(By.text(text))
        val o = (if (last) all.maxByOrNull { it.visibleBounds.top } else all.minByOrNull { it.visibleBounds.top })
            ?: return false
        o.click(); pause(); return true
    }

    private fun tapContains(text: String, timeout: Long = 8_000): Boolean {
        val o = device.wait(Until.findObject(By.textContains(text)), timeout)
            ?: run { log("not found text~: $text"); return false }
        o.click(); pause(); return true
    }

    /** Taps [text], scrolling down once if it is not on screen yet. */
    private fun tapTextScroll(text: String, last: Boolean = false): Boolean =
        tapText(text, 3_000, last) || run { scrollDown(); tapText(text, 3_000, last) }

    // --- text fields in screen order ---------------------------------------------------
    private fun edits(): List<UiObject2> = device.findObjects(By.clazz("android.widget.EditText"))
        .sortedWith(compareBy<UiObject2>({ it.visibleBounds.top / 20 }, { it.visibleBounds.left }))

    private fun fill(index: Int, text: String) {
        val e = edits()
        if (index < e.size) { e[index].text = text; pause(400) } else log("no field $index (have ${e.size})")
    }

    // --- scrolling ---------------------------------------------------------------------
    private fun scrollDown(fraction: Double = 0.45) {
        val w = device.displayWidth; val h = device.displayHeight
        device.swipe(w / 2, (h * 0.78).toInt(), w / 2, (h * (0.78 - fraction)).toInt(), 30); pause(500)
    }

    private fun scrollUp(fraction: Double = 0.45) {
        val w = device.displayWidth; val h = device.displayHeight
        device.swipe(w / 2, (h * 0.3).toInt(), w / 2, (h * (0.3 + fraction)).toInt(), 30); pause(500)
    }

    private fun scrollTop() = repeat(4) { scrollUp(0.55) }

    /** Swipes a horizontal row (found by one of its labels) to the left. */
    private fun swipeRowLeft(anchor: String) {
        val o = device.wait(Until.findObject(By.text(anchor)), 5_000) ?: return
        val y = o.visibleBounds.centerY(); val w = device.displayWidth
        device.swipe((w * 0.85).toInt(), y, (w * 0.12).toInt(), y, 30); pause(500)
    }

    private fun back() { device.pressBack(); pause(600) }

    private fun home() {
        if (!tap("tab_home", 3_000)) { repeat(3) { if (!device.hasObject(By.res("qa_sale"))) back() } }
        find("qa_sale", 10_000); scrollTop()
    }

    private fun savedDone() {
        if (has("সংরক্ষিত হয়েছে!", 8_000)) { shotIfAsked?.let { shot(it) }; tapText("শেষ") }
        shotIfAsked = null
    }
    private var shotIfAsked: String? = null

    private fun step(name: String, block: () -> Unit) {
        log("step $name")
        try { block() } catch (t: Throwable) { Log.e("GuideTour", "step $name failed", t) }
    }

    private fun launchApp() {
        val intent = app.packageManager.getLaunchIntentForPackage(app.packageName)!!
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        app.startActivity(intent)
        device.wait(Until.hasObject(By.pkg(app.packageName).depth(0)), 30_000)
    }

    @Test
    fun guide() {
        device.executeShellCommand("cmd uimode night no")
        launchApp()

        // ---------- first run: language, sign-in, profile, shop ----------
        step("onboarding") {
            find("lang_start", 30_000)
            tap("lang_start")
            Thread.sleep(2_500)
            type("auth_email", "rahim.store@gmail.com")
            runBlocking {
                app.container.settings.saveAuth("demo-user", "rahim.store@gmail.com", null, null, false, "demo-access", "demo-refresh")
            }
            type("profile_name", "রহিম উদ্দিন")
            type("profile_phone", "01712345678")
            runBlocking { app.container.settings.saveProfile("রহিম উদ্দিন", "+8801712345678", true) }
            type("setup_name", "রহিম জেনারেল স্টোর")
            runBlocking {
                val c = app.container
                DemoData.seed(c, true)
                c.settings.saveBusiness(DemoData.business(true), dirty = false)
                c.settings.setBusinessLinked(true)
                c.settings.setSyncResult(System.currentTimeMillis(), null)
                c.settings.setOnboardingDone()
            }
            find("qa_sale", 30_000)
            Thread.sleep(1_500)
        }

        // ---------- home ----------
        step("home") {
            shot("01_home")
            scrollDown(0.5); shot("02_home_today")
            scrollDown(0.5); shot("03_home_recent")
            scrollTop()
        }

        // ---------- cash sale with a product ----------
        step("sale_cash") {
            tap("qa_sale")
            has("সংরক্ষণ করুন")
            shot("04_sale_new")
            if (tapText("পণ্য বাছাই করুন")) {
                has("মিনিকেট চাল"); shot("05_product_picker")
                tapText("মিনিকেট চাল")
                fill(0, "3")
                shot("06_sale_product")
            }
            scrollDown(0.4); shot("07_sale_method")
            shotIfAsked = "08_sale_saved"
            tapText("সংরক্ষণ করুন", last = true); savedDone()
            home()
        }

        // ---------- credit (baki) sale ----------
        step("sale_credit") {
            tap("qa_sale")
            tapText("বাকি"); shot("09_credit_mode")
            if (tapText("কাস্টমার বাছাই করুন")) {
                has("করিম ভাই"); shot("10_customer_picker")
                tapText("করিম ভাই", last = true)
            }
            type("amount", "1500")
            edits().firstOrNull { it.resourceName != "amount" }?.let { it.text = "500"; pause(400) }
            shot("11_credit_filled")
            shotIfAsked = "12_credit_saved"
            tapText("সংরক্ষণ করুন", last = true); savedDone()
            home()
        }

        // ---------- payment received (baki collection) ----------
        step("payment_in") {
            tapText("টাকা পেলাম"); shot("13_payment_in")
            if (tapText("কাস্টমার বাছাই করুন")) tapText("সালমা আপা", last = true)
            type("amount", "1000")
            if (!tapText("বিকাশ", 2_000)) { scrollDown(0.35); tapText("বিকাশ", 2_000) }
            shot("14_payment_in_filled")
            shotIfAsked = "15_payment_in_saved"
            tapText("সংরক্ষণ করুন", last = true); savedDone()
            home()
        }

        // ---------- expense ----------
        step("expense") {
            tapText("খরচ"); shot("16_expense")
            tapText("বিদ্যুৎ বিল")
            type("amount", "1200")
            shot("17_expense_filled")
            tapText("সংরক্ষণ করুন", last = true); savedDone()
            home()
        }

        // ---------- purchase on credit from a supplier ----------
        step("purchase") {
            tapText("কেনা"); shot("18_purchase")
            tapText("বাকিতে কেনা")
            if (tapText("সাপ্লায়ার বাছাই করুন")) tapText("মেঘনা ডিস্ট্রিবিউটর", last = true)
            if (tapText("পণ্য বাছাই করুন")) { tapText("মসুর ডাল", last = true); fill(0, "50") }
            shot("19_purchase_filled")
            scrollDown(0.4); shot("20_purchase_paid")
            shotIfAsked = "21_purchase_saved"
            tapText("সংরক্ষণ করুন", last = true); savedDone()
            home()
        }

        // ---------- the other entries on the home row ----------
        step("payment_out") {
            if (tapText("টাকা দিলাম")) {
                if (tapText("সাপ্লায়ার বাছাই করুন")) tapText("মেঘনা ডিস্ট্রিবিউটর", last = true)
                type("amount", "2000")
                shot("22_payment_out")
                tapText("সংরক্ষণ করুন", last = true); savedDone()
            }
            home()
        }
        step("sale_return") {
            if (tapText("বিক্রি ফেরত")) { shot("23_sale_return"); back() }
            home()
        }
        step("stock_adjust") {
            swipeRowLeft("টাকা দিলাম")
            if (tapText("স্টক সমন্বয়")) {
                if (tapText("পণ্য বাছাই করুন")) tapText("চিনি", last = true)
                fill(0, "5")
                shot("24_stock_adjust")
                back()
            }
            home()
        }
        step("cash_in_out") {
            swipeRowLeft("টাকা দিলাম")
            if (tapText("ক্যাশ যোগ")) { type("amount", "5000"); shot("25_cash_in"); back() }
            home()
            swipeRowLeft("টাকা দিলাম")
            if (tapText("ক্যাশ তোলা")) { type("amount", "2000"); shot("26_cash_out"); back() }
            home()
        }

        // ---------- all transactions → detail → cancel (reversal) ----------
        step("transactions") {
            scrollDown(0.5)
            if (tapTextScroll("সব দেখুন")) {
                shot("27_all_transactions")
                if (tapText("করিম ভাই")) {
                    shot("28_tx_detail")
                    if (tapTextScroll("হিসাব বাতিল")) {
                        shot("29_cancel_confirm")
                        tapText("হিসাব বাতিল", last = true)
                        shot("30_cancelled")
                    }
                    back()
                }
                back()
            }
            home()
        }

        // ---------- customers & suppliers ----------
        step("parties") {
            if (tap("tab_parties")) {
                find("party_row"); shot("31_customers")
                if (tapText("নতুন কাস্টমার")) {
                    shot("32_add_customer")
                    fill(0, "মিতু আক্তার"); fill(1, "01712000111"); fill(2, "750")
                    shot("33_add_customer_filled")
                    tapTextScroll("সংরক্ষণ করুন", last = true)
                    pause(1_000)
                    if (!device.hasObject(By.res("party_row"))) back()
                }
                tapText("সাপ্লায়ার"); shot("34_suppliers")
                tapText("কাস্টমার")
            }
        }
        step("party_detail") {
            if (tapText("করিম ভাই")) {
                shot("35_party_detail")
                scrollDown(0.5); shot("36_party_history"); scrollTop()
                if (tapContains("টাকা তোলার")) { shot("37_reminder"); tapText("সংরক্ষণ করুন", last = true); pause(1_500) }
                if (tapText("PDF")) { Thread.sleep(2_500); shot("38_pdf_share"); back() }
                if (tapText("টাকা পেলাম")) { shot("39_party_payment"); back() }
                back()
            }
            home()
        }

        // ---------- products & stock ----------
        step("products") {
            if (tap("tab_products")) {
                shot("40_products")
                if (tapText("শুধু কম স্টক")) { shot("41_low_stock"); tapText("শুধু কম স্টক") }
                if (tapText("নতুন পণ্য")) {
                    shot("42_add_product")
                    fill(0, "আটা")
                    tapText("কেজি")
                    fill(1, "60"); fill(2, "52"); fill(3, "40"); fill(4, "10")
                    shot("43_add_product_filled")
                    tapTextScroll("সংরক্ষণ করুন", last = true)
                    pause(1_000)
                }
                if (tapText("চিনি")) {
                    shot("44_product_detail")
                    scrollDown(0.5); shot("45_stock_history"); scrollTop()
                    if (tapText("স্টক যোগ")) { shot("46_stock_in"); back() }
                    back()
                }
            }
            home()
        }

        // ---------- reports ----------
        step("reports") {
            if (tap("tab_reports")) {
                shot("47_reports_today")
                tapText("এই মাস"); shot("48_reports_month")
                scrollDown(0.5); shot("49_reports_cashflow")
                scrollDown(0.5); shot("50_reports_methods")
                scrollDown(0.6); shot("51_reports_export")
            }
            home()
        }

        // ---------- more: account, language, delete ----------
        step("more") {
            if (tap("tab_more")) {
                shot("52_more")
                if (tapText("অনলাইন ব্যাকআপ ও sync")) { shot("53_account"); back() }
                if (tapText("ভাষা")) { shot("54_language"); tapText("বন্ধ করুন") }
                scrollDown(0.6); shot("55_more_help")
                if (tapTextScroll("অ্যাকাউন্ট ও ডেটা মুছুন")) { shot("56_delete"); back() }
            }
            home()
        }

        // ---------- offline ----------
        step("offline") {
            device.executeShellCommand("svc wifi disable")
            device.executeShellCommand("svc data disable")
            Thread.sleep(4_000)
            home()
            shot("57_home_offline")
            device.executeShellCommand("svc wifi enable")
            device.executeShellCommand("svc data enable")
        }
        log("guide finished")
    }
}
