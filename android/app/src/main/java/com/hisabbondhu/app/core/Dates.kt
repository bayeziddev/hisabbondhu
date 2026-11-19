package com.hisabbondhu.app.core

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

enum class ReportPeriod { TODAY, YESTERDAY, LAST_7_DAYS, THIS_MONTH, LAST_MONTH, ALL }

/** Date helpers. Data is stored as UTC epoch millis; the UI always uses Bangladesh time. */
object Dates {
    val zone: ZoneId = ZoneId.of("Asia/Dhaka")

    private val bnMonths = arrayOf(
        "জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন",
        "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর",
    )
    private val enMonths = arrayOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
    )

    fun now(): Long = System.currentTimeMillis()

    fun localDate(ms: Long): LocalDate = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()

    fun startOfDay(date: LocalDate): Long = date.atStartOfDay(zone).toInstant().toEpochMilli()

    fun startOfToday(now: Long = now()): Long = startOfDay(localDate(now))

    /** Returns [from, to) for a report period. */
    fun range(period: ReportPeriod, now: Long = now()): Pair<Long, Long> {
        val today = localDate(now)
        return when (period) {
            ReportPeriod.TODAY -> startOfDay(today) to startOfDay(today.plusDays(1))
            ReportPeriod.YESTERDAY -> startOfDay(today.minusDays(1)) to startOfDay(today)
            ReportPeriod.LAST_7_DAYS -> startOfDay(today.minusDays(6)) to startOfDay(today.plusDays(1))
            ReportPeriod.THIS_MONTH -> startOfDay(today.withDayOfMonth(1)) to
                startOfDay(today.withDayOfMonth(1).plusMonths(1))
            ReportPeriod.LAST_MONTH -> startOfDay(today.withDayOfMonth(1).minusMonths(1)) to
                startOfDay(today.withDayOfMonth(1))
            ReportPeriod.ALL -> 0L to Long.MAX_VALUE
        }
    }

    fun formatDate(ms: Long, bengali: Boolean): String {
        val d = localDate(ms)
        return if (bengali) {
            Money.toBengaliDigits("${d.dayOfMonth} ") + bnMonths[d.monthValue - 1] +
                Money.toBengaliDigits(" ${d.year}")
        } else {
            "${d.dayOfMonth} ${enMonths[d.monthValue - 1]} ${d.year}"
        }
    }

    fun formatTime(ms: Long, bengali: Boolean): String {
        val t: ZonedDateTime = Instant.ofEpochMilli(ms).atZone(zone)
        val h12 = if (t.hour % 12 == 0) 12 else t.hour % 12
        val text = "$h12:" + t.minute.toString().padStart(2, '0')
        return if (bengali) {
            val part = when (t.hour) {
                in 4..11 -> "সকাল"
                in 12..15 -> "দুপুর"
                in 16..17 -> "বিকাল"
                in 18..19 -> "সন্ধ্যা"
                else -> "রাত"
            }
            "$part " + Money.toBengaliDigits(text)
        } else {
            text + if (t.hour < 12) " AM" else " PM"
        }
    }

    fun formatDateTime(ms: Long, bengali: Boolean): String =
        formatDate(ms, bengali) + ", " + formatTime(ms, bengali)

    /** Combine a picked calendar date with the current wall-clock time. */
    fun withDate(date: LocalDate, now: Long = now()): Long {
        val time = Instant.ofEpochMilli(now).atZone(zone).toLocalTime()
        return date.atTime(time).atZone(zone).toInstant().toEpochMilli()
    }

    /** DatePicker returns UTC midnight millis; convert to a LocalDate. */
    fun fromPickerMillis(utcMillis: Long): LocalDate =
        Instant.ofEpochMilli(utcMillis).atZone(ZoneId.of("UTC")).toLocalDate()

    fun toPickerMillis(ms: Long): Long =
        localDate(ms).atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
}
