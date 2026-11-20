package com.hisabbondhu.app

import com.hisabbondhu.app.core.Dates
import com.hisabbondhu.app.data.local.PartyEntity
import com.hisabbondhu.app.data.local.ProductEntity
import com.hisabbondhu.app.data.local.TxEntity
import com.hisabbondhu.app.data.prefs.BusinessProfile
import com.hisabbondhu.app.domain.PartyType
import com.hisabbondhu.app.domain.PayMethod
import com.hisabbondhu.app.domain.SyncState
import com.hisabbondhu.app.domain.TxType
import java.util.UUID

/** Realistic sample shop used only for Play Store screenshots. */
object DemoData {
    private fun id() = UUID.randomUUID().toString()

    fun business(bn: Boolean) = BusinessProfile(
        id = id(),
        name = if (bn) "রহিম জেনারেল স্টোর" else "Rahim General Store",
        ownerName = if (bn) "রহিম উদ্দিন" else "Rahim Uddin",
        phone = "01712345678",
        address = if (bn) "স্টেশন রোড, টাঙ্গাইল" else "Station Road, Tangail",
        type = "general_store",
        openingCashPaisa = 1_500_000,
        updatedAt = Dates.now(),
    )

    suspend fun seed(c: AppContainer, bn: Boolean) {
        val dao = c.db.dao()
        val dev = c.settings.deviceId()
        val now = Dates.now()
        val today = Dates.startOfToday()
        val hour = 3_600_000L
        val day = 24 * hour
        fun at(dayOffset: Int, h: Int, m: Int = 0) = today - dayOffset * day + h * hour + m * 60_000L
        fun t(s: String, e: String) = if (bn) s else e

        fun party(type: PartyType, name: String, phone: String, opening: Long, reminder: Long? = null) = PartyEntity(
            id(), type, name, phone, null, null, opening, reminder, now - 30 * day, now, false, dev, SyncState.SYNCED,
        )
        val karim = party(PartyType.CUSTOMER, t("করিম ভাই", "Karim Bhai"), "01811223344", 50_000, today + 10 * hour)
        val salma = party(PartyType.CUSTOMER, t("সালমা আপা", "Salma Apa"), "01911556677", 120_000)
        val jasim = party(PartyType.CUSTOMER, t("জসিম মিয়া", "Jasim Mia"), "01611889900", 0)
        val nasrin = party(PartyType.CUSTOMER, t("নাসরিন বেগম", "Nasrin Begum"), "01511334455", 35_000)
        val meghna = party(PartyType.SUPPLIER, t("মেঘনা ডিস্ট্রিবিউটর", "Meghna Distributor"), "01711000111", 150_000)
        val fresh = party(PartyType.SUPPLIER, t("ফ্রেশ এগ সাপ্লাই", "Fresh Egg Supply"), "01811000222", 0)
        dao.upsertParties(listOf(karim, salma, jasim, nasrin, meghna, fresh))

        fun product(name: String, unit: String, buy: Long, sell: Long, stock: Long, min: Long) = ProductEntity(
            id(), name, null, null, null, unit, buy, sell, stock, min, now - 30 * day, now, false, dev, SyncState.SYNCED,
        )
        val rice = product(t("মিনিকেট চাল", "Miniket Rice"), "kg", 6_200, 6_800, 120_000, 20_000)
        val oil = product(t("সয়াবিন তেল", "Soybean Oil"), "litre", 16_500, 17_800, 40_000, 10_000)
        val egg = product(t("ডিম", "Eggs"), "dozen", 13_500, 15_000, 30_000, 5_000)
        val sugar = product(t("চিনি", "Sugar"), "kg", 12_000, 13_500, 8_000, 10_000)
        val soap = product(t("সাবান", "Soap"), "pcs", 3_800, 4_500, 60_000, 12_000)
        val lentil = product(t("মসুর ডাল", "Red Lentils"), "kg", 10_500, 11_500, 45_000, 10_000)
        dao.upsertProducts(listOf(rice, oil, egg, sugar, soap, lentil))

        val txs = mutableListOf<TxEntity>()
        fun tx(
            type: TxType, time: Long, total: Long, paid: Long, party: PartyEntity? = null, product: ProductEntity? = null,
            qty: Long = 0, method: PayMethod = PayMethod.CASH, category: String? = null, note: String? = null,
        ) {
            txs += TxEntity(
                id(), type, party?.id, product?.id, qty, product?.let { if (type == TxType.SALE) it.salePricePaisa else it.purchasePricePaisa } ?: 0,
                if (type == TxType.SALE) product?.purchasePricePaisa ?: 0 else 0, 0, total, paid, method, category, note,
                time, time, dev, null, SyncState.SYNCED,
            )
        }
        // Earlier this month (for reports)
        for (d in 1..9) {
            tx(TxType.SALE, at(d, 10), 420_000 + d * 15_000L, 420_000 + d * 15_000L)
            tx(TxType.SALE, at(d, 17), 180_000 + d * 9_000L, 180_000 + d * 9_000L, method = PayMethod.BKASH)
        }
        tx(TxType.EXPENSE, at(5, 12), 800_000, 800_000, category = "rent", note = t("দোকান ভাড়া", "Shop rent"))
        tx(TxType.EXPENSE, at(3, 19), 250_000, 250_000, category = "salary")
        tx(TxType.SALE, at(2, 11), 210_000, 60_000, party = salma)
        tx(TxType.PURCHASE, at(4, 15), 620_000, 470_000, party = meghna, product = rice, qty = 100_000)
        // Today
        tx(TxType.SALE, at(0, 9, 15), 34_000, 34_000, product = rice, qty = 5_000)
        tx(TxType.SALE, at(0, 10, 40), 35_600, 10_000, party = karim, product = oil, qty = 2_000, note = t("মাসের বাজার", "Monthly groceries"))
        tx(TxType.PAYMENT_IN, at(0, 11, 20), 50_000, 50_000, party = salma, method = PayMethod.BKASH)
        tx(TxType.SALE, at(0, 12, 5), 30_000, 30_000, product = egg, qty = 2_000, method = PayMethod.NAGAD)
        tx(TxType.EXPENSE, at(0, 13, 0), 120_000, 120_000, category = "electricity")
        tx(TxType.PURCHASE, at(0, 14, 30), 270_000, 270_000, party = fresh, product = egg, qty = 20_000)
        tx(TxType.SALE, at(0, 15, 10), 245_000, 245_000)
        tx(TxType.SALE, at(0, 16, 45), 92_000, 50_000, party = nasrin, product = lentil, qty = 8_000)
        tx(TxType.SALE, at(0, 17, 30), 27_000, 27_000, product = sugar, qty = 2_000)
        dao.insertTxIgnore(txs)
    }
}
