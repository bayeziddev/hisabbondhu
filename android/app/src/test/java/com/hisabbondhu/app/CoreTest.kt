package com.hisabbondhu.app

import com.hisabbondhu.app.core.Dates
import com.hisabbondhu.app.core.Money
import com.hisabbondhu.app.core.Qty
import com.hisabbondhu.app.core.ReportPeriod
import com.hisabbondhu.app.core.normalizeBdPhone
import com.hisabbondhu.app.domain.Ledger
import com.hisabbondhu.app.domain.TxType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MoneyTest {
    @Test fun parsesTakaToPaisa() {
        assertEquals(125000L, Money.parseToPaisa("1250"))
        assertEquals(125050L, Money.parseToPaisa("1,250.50"))
        assertEquals(125050L, Money.parseToPaisa("১২৫০.৫"))
        assertEquals(5L, Money.parseToPaisa("0.05"))
        assertNull(Money.parseToPaisa(""))
        assertNull(Money.parseToPaisa("12.345"))
        assertNull(Money.parseToPaisa("abc"))
    }

    @Test fun formatsWithLakhGrouping() {
        assertEquals("৳1,25,000", Money.format(12_500_000, bengali = false))
        assertEquals("৳১,২৫,০০০", Money.format(12_500_000, bengali = true))
        assertEquals("-৳500.50", Money.format(-50_050, bengali = false))
        assertEquals("৳0", Money.format(0, bengali = false))
        assertEquals("৳12,34,567.05", Money.format(123_456_705, bengali = false))
    }

    @Test fun inputRoundTrip() {
        listOf(0L, 100L, 125050L, 99L, 1L).forEach { p ->
            val back = Money.parseToPaisa(Money.toInput(p)) ?: 0L
            assertEquals(p, back)
        }
    }
}

class QtyTest {
    @Test fun quantities() {
        assertEquals(2500L, Qty.parseToMilli("2.5"))
        assertEquals("2.5", Qty.format(2500, bengali = false))
        assertEquals("১২", Qty.format(12000, bengali = true))
        // 1.5 kg × ৳120.00 = ৳180.00
        assertEquals(18000L, Qty.lineTotal(1500, 12000))
        // rounding half-up: 0.333 × ৳10.00 = 333 paisa
        assertEquals(333L, Qty.lineTotal(333, 1000))
    }
}

class PhoneTest {
    @Test fun normalisesBangladeshNumbers() {
        assertEquals("+8801712345678", normalizeBdPhone("01712345678"))
        assertEquals("+8801712345678", normalizeBdPhone("+880 1712-345678"))
        assertEquals("+8801712345678", normalizeBdPhone("8801712345678"))
        assertEquals("+8801712345678", normalizeBdPhone("০১৭১২৩৪৫৬৭৮"))
        assertNull(normalizeBdPhone("01212345678")) // invalid operator prefix
        assertNull(normalizeBdPhone("12345"))
    }
}

class LedgerRulesTest {
    @Test fun creditSaleThenPaymentSettles() {
        val sale = Ledger.receivableDelta(TxType.SALE, total = 125_000, paid = 75_000)
        val payment = Ledger.receivableDelta(TxType.PAYMENT_IN, total = 50_000, paid = 50_000)
        assertEquals(50_000L, sale)
        assertEquals(0L, sale + payment)
    }

    @Test fun reversalCancelsEverything() {
        val types = TxType.entries
        types.forEach { t ->
            assertEquals(0L, Ledger.receivableDelta(t, 1000, 400) + Ledger.receivableDelta(t, -1000, -400))
            assertEquals(0L, Ledger.cashDelta(t, 400) + Ledger.cashDelta(t, -400))
            assertEquals(0L, Ledger.stockDelta(t, 2000) + Ledger.stockDelta(t, -2000))
        }
    }

    @Test fun purchaseCreatesPayable() {
        // Bought 10,000 from a supplier, paid 4,000 → we owe 6,000 (negative receivable).
        assertEquals(-600_000L, Ledger.receivableDelta(TxType.PURCHASE, 1_000_000, 400_000))
        assertEquals(-400_000L, Ledger.cashDelta(TxType.PURCHASE, 400_000))
        assertEquals(5000L, Ledger.stockDelta(TxType.PURCHASE, 5000))
    }

    @Test fun expenseOnlyAffectsCash() {
        assertEquals(0L, Ledger.receivableDelta(TxType.EXPENSE, 500, 500))
        assertEquals(-500L, Ledger.cashDelta(TxType.EXPENSE, 500))
    }
}

class DatesTest {
    @Test fun todayRangeIs24h() {
        val (from, to) = Dates.range(ReportPeriod.TODAY)
        assertEquals(24L * 3600 * 1000, to - from)
        assertTrue(Dates.now() in from until to)
    }

    @Test fun bengaliDate() {
        val ms = Dates.startOfDay(LocalDate.of(2026, 11, 20)) + 3_600_000
        assertEquals("২০ নভেম্বর ২০২৬", Dates.formatDate(ms, bengali = true))
        assertEquals("20 Nov 2026", Dates.formatDate(ms, bengali = false))
    }
}
