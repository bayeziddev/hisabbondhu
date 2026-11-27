package com.hisabbondhu.app.util

/** Upper-case letters and digits only, at most 6 — what a referral code can contain. */
fun cleanReferralCode(input: String): String = input.uppercase().filter { it in 'A'..'Z' || it in '0'..'9' }.take(6)
