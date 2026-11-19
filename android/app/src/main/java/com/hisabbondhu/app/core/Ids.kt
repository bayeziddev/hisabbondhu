package com.hisabbondhu.app.core

import java.util.UUID

/** Globally unique ids so rows created offline on many phones never collide during sync. */
fun newId(): String = UUID.randomUUID().toString()

/** Normalises Bangladeshi mobile numbers to +8801XXXXXXXXX. Returns null if invalid. */
fun normalizeBdPhone(input: String): String? {
    val digits = Money.toAsciiDigits(input).filter { it.isDigit() }
    val local = when {
        digits.startsWith("8801") && digits.length == 13 -> digits.substring(2)
        digits.startsWith("01") && digits.length == 11 -> digits
        digits.startsWith("1") && digits.length == 10 -> "0$digits"
        else -> return null
    }
    if (!Regex("^01[3-9]\\d{8}$").matches(local)) return null
    return "+88$local"
}
