package com.hisabbondhu.app.domain

enum class PartyType { CUSTOMER, SUPPLIER }

/**
 * Every money movement is one append-only transaction. Corrections are made with a
 * reversal row (same type, negated amounts) — never by editing or deleting.
 */
enum class TxType {
    SALE,            // বিক্রি (cash or credit; due = total - paid)
    PAYMENT_IN,      // টাকা পেলাম (customer pays due)
    PAYMENT_OUT,     // টাকা দিলাম (pay supplier / give money)
    PURCHASE,        // কেনা (stock in; payable = total - paid)
    EXPENSE,         // খরচ
    SALE_RETURN,     // বিক্রি ফেরত
    PURCHASE_RETURN, // কেনা ফেরত
    STOCK_ADJUST,    // স্টক সমন্বয় (signed quantity)
    CASH_IN,         // ক্যাশ যোগ (owner adds cash)
    CASH_OUT,        // ক্যাশ তোলা (owner withdraws cash)
}

enum class PayMethod { CASH, BKASH, NAGAD, ROCKET, BANK, CARD, OTHER }

enum class SyncState { PENDING, SYNCED, REJECTED }

/** Kotlin mirror of the SQL ledger rules in [com.hisabbondhu.app.data.local.LedgerSql]. */
object Ledger {
    /** Positive = the party owes us (customer due). Negative = we owe the party (supplier payable). */
    fun receivableDelta(type: TxType, total: Long, paid: Long): Long = when (type) {
        TxType.SALE -> total - paid
        TxType.PAYMENT_IN -> -paid
        TxType.PAYMENT_OUT -> paid
        TxType.SALE_RETURN -> -(total - paid)
        TxType.PURCHASE -> -(total - paid)
        TxType.PURCHASE_RETURN -> total - paid
        else -> 0
    }

    /** Effect on money in hand (cash + mobile banking + bank). */
    fun cashDelta(type: TxType, paid: Long): Long = when (type) {
        TxType.SALE, TxType.PAYMENT_IN, TxType.PURCHASE_RETURN, TxType.CASH_IN -> paid
        TxType.PURCHASE, TxType.PAYMENT_OUT, TxType.EXPENSE, TxType.SALE_RETURN, TxType.CASH_OUT -> -paid
        else -> 0
    }

    fun stockDelta(type: TxType, qtyMilli: Long): Long = when (type) {
        TxType.SALE, TxType.PURCHASE_RETURN -> -qtyMilli
        TxType.PURCHASE, TxType.SALE_RETURN, TxType.STOCK_ADJUST -> qtyMilli
        else -> 0
    }

    fun needsParty(type: TxType): Boolean = type == TxType.PAYMENT_IN || type == TxType.PAYMENT_OUT

    fun usesProduct(type: TxType): Boolean = type in setOf(
        TxType.SALE, TxType.PURCHASE, TxType.SALE_RETURN, TxType.PURCHASE_RETURN, TxType.STOCK_ADJUST,
    )

    /** Types where "paid" can be less than total (rest becomes due/payable). */
    fun allowsCredit(type: TxType): Boolean = type in setOf(
        TxType.SALE, TxType.PURCHASE, TxType.SALE_RETURN, TxType.PURCHASE_RETURN,
    )

    fun defaultPartyType(type: TxType): PartyType? = when (type) {
        TxType.SALE, TxType.SALE_RETURN, TxType.PAYMENT_IN -> PartyType.CUSTOMER
        TxType.PURCHASE, TxType.PURCHASE_RETURN, TxType.PAYMENT_OUT -> PartyType.SUPPLIER
        else -> null
    }
}

val EXPENSE_CATEGORIES = listOf(
    "rent", "salary", "electricity", "transport", "food", "mobile", "repair", "tax", "other",
)

val BUSINESS_TYPES = listOf(
    "general_store", "grocery", "pharmacy", "restaurant", "fashion", "electronics",
    "wholesale", "service", "other",
)

val UNITS = listOf("pcs", "kg", "g", "litre", "ml", "dozen", "box", "carton", "packet", "metre", "bag")
