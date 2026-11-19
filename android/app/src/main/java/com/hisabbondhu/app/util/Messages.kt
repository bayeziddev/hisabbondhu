package com.hisabbondhu.app.util

import android.content.Context
import com.hisabbondhu.app.R
import com.hisabbondhu.app.core.Dates
import com.hisabbondhu.app.core.Money
import com.hisabbondhu.app.data.local.TxRow
import com.hisabbondhu.app.domain.PartyType
import com.hisabbondhu.app.domain.TxType

/** Builds polite Bangla/English messages for WhatsApp, SMS, Messenger, IMO… */
object Messages {
    private fun bn(context: Context) = context.resources.configuration.locales[0].language == "bn"

    fun forTransaction(
        context: Context,
        row: TxRow,
        partyType: PartyType?,
        balanceAfter: Long?,
        businessName: String,
    ): String {
        val b = bn(context)
        val tx = row.tx
        val name = row.partyName ?: context.getString(R.string.msg_dear_customer)
        val date = Dates.formatDate(tx.occurredAt, b)
        val shop = businessName.ifBlank { context.getString(R.string.app_name) }
        fun m(p: Long) = Money.format(p, b)
        val main = when (tx.type) {
            TxType.SALE -> context.getString(R.string.msg_sale, name, date, m(tx.totalPaisa), m(tx.paidPaisa))
            TxType.PAYMENT_IN -> context.getString(R.string.msg_payment_in, name, date, m(tx.paidPaisa))
            TxType.PAYMENT_OUT -> context.getString(R.string.msg_payment_out, name, date, m(tx.paidPaisa))
            TxType.PURCHASE -> context.getString(R.string.msg_purchase, name, date, m(tx.totalPaisa), m(tx.paidPaisa))
            TxType.SALE_RETURN, TxType.PURCHASE_RETURN ->
                context.getString(R.string.msg_return, name, date, m(tx.totalPaisa))
            else -> context.getString(R.string.msg_generic, name, date, m(tx.totalPaisa))
        }
        val balance = balanceAfter?.let { balanceLine(context, partyType, it) }.orEmpty()
        return listOf(main, balance, "— $shop").filter { it.isNotBlank() }.joinToString("\n")
    }

    fun statement(context: Context, name: String, partyType: PartyType, balance: Long, businessName: String): String {
        val b = bn(context)
        val shop = businessName.ifBlank { context.getString(R.string.app_name) }
        val date = Dates.formatDate(Dates.now(), b)
        val line = when {
            balance > 0 && partyType == PartyType.CUSTOMER ->
                context.getString(R.string.msg_reminder_due, name, date, Money.format(balance, b))
            balance < 0 && partyType == PartyType.SUPPLIER ->
                context.getString(R.string.msg_statement_payable, name, date, Money.format(-balance, b))
            balance == 0L -> context.getString(R.string.msg_statement_clear, name, date)
            else -> context.getString(R.string.msg_statement_advance, name, date, Money.format(kotlin.math.abs(balance), b))
        }
        return "$line\n— $shop"
    }

    private fun balanceLine(context: Context, type: PartyType?, receivable: Long): String {
        val b = bn(context)
        return when {
            receivable > 0 -> context.getString(R.string.msg_balance_due, Money.format(receivable, b))
            receivable < 0 && type == PartyType.SUPPLIER ->
                context.getString(R.string.msg_balance_payable, Money.format(-receivable, b))
            receivable < 0 -> context.getString(R.string.msg_balance_advance, Money.format(-receivable, b))
            else -> context.getString(R.string.msg_balance_clear)
        }
    }
}
