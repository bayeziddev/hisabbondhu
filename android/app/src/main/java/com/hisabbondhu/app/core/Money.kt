package com.hisabbondhu.app.core

import kotlin.math.abs

/**
 * All money is stored as integer **paisa** (1 টাকা = 100 পয়সা) to avoid floating point errors.
 */
object Money {
    private val bnDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')

    fun toBengaliDigits(s: String): String = buildString(s.length) {
        s.forEach { c -> append(if (c in '0'..'9') bnDigits[c - '0'] else c) }
    }

    fun toAsciiDigits(s: String): String = buildString(s.length) {
        s.forEach { c ->
            val i = bnDigits.indexOf(c)
            append(if (i >= 0) '0' + i else c)
        }
    }

    /** Bangladesh / Indian digit grouping, e.g. 1234567 -> 12,34,567 */
    fun groupIndian(n: Long): String {
        val s = n.toString()
        if (s.length <= 3) return s
        val last3 = s.takeLast(3)
        var rest = s.dropLast(3)
        val parts = ArrayList<String>()
        while (rest.length > 2) {
            parts.add(0, rest.takeLast(2))
            rest = rest.dropLast(2)
        }
        if (rest.isNotEmpty()) parts.add(0, rest)
        return parts.joinToString(",") + "," + last3
    }

    fun format(
        paisa: Long,
        bengali: Boolean,
        withSymbol: Boolean = true,
        showPlus: Boolean = false,
    ): String {
        val negative = paisa < 0
        val absPaisa = abs(paisa)
        val taka = absPaisa / 100
        val fraction = (absPaisa % 100).toInt()
        val sb = StringBuilder()
        if (negative) sb.append('-') else if (showPlus && paisa > 0) sb.append('+')
        if (withSymbol) sb.append('৳')
        sb.append(groupIndian(taka))
        if (fraction != 0) sb.append('.').append(fraction.toString().padStart(2, '0'))
        val out = sb.toString()
        return if (bengali) toBengaliDigits(out) else out
    }

    private val amountRegex = Regex("^\\d{0,12}(\\.\\d{0,2})?$")

    /** Parses "1,250.50", "১২৫০" or "৳ 500" into paisa. Returns null when invalid or empty. */
    fun parseToPaisa(input: String): Long? {
        val cleaned = toAsciiDigits(input)
            .replace(",", "")
            .replace("৳", "")
            .replace(" ", "")
            .trim()
        if (cleaned.isEmpty() || cleaned == ".") return null
        if (!amountRegex.matches(cleaned)) return null
        val parts = cleaned.split('.')
        val whole = parts[0].ifEmpty { "0" }.toLong()
        val frac = if (parts.size > 1) parts[1].padEnd(2, '0').take(2).toInt() else 0
        return whole * 100 + frac
    }

    /** Converts paisa back to a plain editable string ("1250" or "1250.5"). */
    fun toInput(paisa: Long): String {
        if (paisa == 0L) return ""
        val taka = paisa / 100
        val frac = (paisa % 100).toInt()
        return if (frac == 0) taka.toString() else "$taka." + frac.toString().padStart(2, '0').trimEnd('0')
    }
}

/** Quantities are stored as integer milli-units (1 kg = 1000) to support 0.5 kg etc. */
object Qty {
    private val qtyRegex = Regex("^\\d{0,9}(\\.\\d{0,3})?$")

    fun parseToMilli(input: String): Long? {
        val cleaned = Money.toAsciiDigits(input).replace(",", "").trim()
        if (cleaned.isEmpty() || cleaned == ".") return null
        if (!qtyRegex.matches(cleaned)) return null
        val parts = cleaned.split('.')
        val whole = parts[0].ifEmpty { "0" }.toLong()
        val frac = if (parts.size > 1) parts[1].padEnd(3, '0').take(3).toInt() else 0
        return whole * 1000 + frac
    }

    fun format(milli: Long, bengali: Boolean): String {
        val negative = milli < 0
        val a = abs(milli)
        val whole = a / 1000
        val frac = (a % 1000).toInt()
        var out = Money.groupIndian(whole)
        if (frac != 0) out += "." + frac.toString().padStart(3, '0').trimEnd('0')
        if (negative) out = "-$out"
        return if (bengali) Money.toBengaliDigits(out) else out
    }

    fun toInput(milli: Long): String {
        if (milli == 0L) return ""
        val whole = milli / 1000
        val frac = (milli % 1000).toInt()
        return if (frac == 0) whole.toString() else "$whole." + frac.toString().padStart(3, '0').trimEnd('0')
    }

    /** total paisa for qty (milli) × rate (paisa per unit), rounded half-up. */
    fun lineTotal(qtyMilli: Long, ratePaisa: Long): Long = (qtyMilli * ratePaisa + 500) / 1000
}
