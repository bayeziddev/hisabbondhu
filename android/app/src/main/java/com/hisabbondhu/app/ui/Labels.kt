package com.hisabbondhu.app.ui

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.AddShoppingCart
import androidx.compose.material.icons.rounded.CallMade
import androidx.compose.material.icons.rounded.CallReceived
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Inventory
import androidx.compose.material.icons.rounded.Money
import androidx.compose.material.icons.rounded.RemoveCircle
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.RemoveShoppingCart
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.hisabbondhu.app.R
import com.hisabbondhu.app.core.Dates
import com.hisabbondhu.app.core.Money
import com.hisabbondhu.app.core.Qty
import com.hisabbondhu.app.domain.PartyType
import com.hisabbondhu.app.domain.PayMethod
import com.hisabbondhu.app.domain.TxType
import com.hisabbondhu.app.ui.theme.LocalMoneyColors

/** true when the UI language is Bangla → show Bengali digits (০১২৩…). */
val LocalBengali = staticCompositionLocalOf { true }

@Composable
@ReadOnlyComposable
fun money(paisa: Long, plus: Boolean = false): String =
    Money.format(paisa, LocalBengali.current, showPlus = plus)

@Composable
@ReadOnlyComposable
fun qty(milli: Long): String = Qty.format(milli, LocalBengali.current)

@Composable
@ReadOnlyComposable
fun num(n: Number): String = if (LocalBengali.current) Money.toBengaliDigits(n.toString()) else n.toString()

@Composable
@ReadOnlyComposable
fun date(ms: Long): String = Dates.formatDate(ms, LocalBengali.current)

@Composable
@ReadOnlyComposable
fun time(ms: Long): String = Dates.formatTime(ms, LocalBengali.current)

@Composable
@ReadOnlyComposable
fun dateTime(ms: Long): String = Dates.formatDateTime(ms, LocalBengali.current)

@StringRes
fun TxType.labelRes(): Int = when (this) {
    TxType.SALE -> R.string.tx_sale
    TxType.PAYMENT_IN -> R.string.tx_payment_in
    TxType.PAYMENT_OUT -> R.string.tx_payment_out
    TxType.PURCHASE -> R.string.tx_purchase
    TxType.EXPENSE -> R.string.tx_expense
    TxType.SALE_RETURN -> R.string.tx_sale_return
    TxType.PURCHASE_RETURN -> R.string.tx_purchase_return
    TxType.STOCK_ADJUST -> R.string.tx_stock_adjust
    TxType.CASH_IN -> R.string.tx_cash_in
    TxType.CASH_OUT -> R.string.tx_cash_out
}

fun TxType.icon(): ImageVector = when (this) {
    TxType.SALE -> Icons.Rounded.ShoppingBag
    TxType.PAYMENT_IN -> Icons.Rounded.CallReceived
    TxType.PAYMENT_OUT -> Icons.Rounded.CallMade
    TxType.PURCHASE -> Icons.Rounded.AddShoppingCart
    TxType.EXPENSE -> Icons.Rounded.Receipt
    TxType.SALE_RETURN -> Icons.Rounded.Undo
    TxType.PURCHASE_RETURN -> Icons.Rounded.RemoveShoppingCart
    TxType.STOCK_ADJUST -> Icons.Rounded.Inventory
    TxType.CASH_IN -> Icons.Rounded.AddCircle
    TxType.CASH_OUT -> Icons.Rounded.RemoveCircle
}

@Composable
@ReadOnlyComposable
fun TxType.color(): Color {
    val c = LocalMoneyColors.current
    return when (this) {
        TxType.SALE, TxType.CASH_IN, TxType.PURCHASE_RETURN -> c.sale
        TxType.PAYMENT_IN -> c.paymentIn
        TxType.EXPENSE, TxType.CASH_OUT, TxType.SALE_RETURN -> c.expense
        TxType.PURCHASE, TxType.PAYMENT_OUT -> c.purchase
        TxType.STOCK_ADJUST -> c.warning
    }
}

@StringRes
fun PayMethod.labelRes(): Int = when (this) {
    PayMethod.CASH -> R.string.method_cash
    PayMethod.BKASH -> R.string.method_bkash
    PayMethod.NAGAD -> R.string.method_nagad
    PayMethod.ROCKET -> R.string.method_rocket
    PayMethod.BANK -> R.string.method_bank
    PayMethod.CARD -> R.string.method_card
    PayMethod.OTHER -> R.string.method_other
}

fun PayMethod.icon(): ImageVector = when (this) {
    PayMethod.CASH -> Icons.Rounded.Money
    PayMethod.BKASH, PayMethod.NAGAD, PayMethod.ROCKET -> Icons.Rounded.PhoneAndroid
    PayMethod.BANK -> Icons.Rounded.AccountBalance
    PayMethod.CARD -> Icons.Rounded.CreditCard
    PayMethod.OTHER -> Icons.Rounded.AccountBalanceWallet
}

@StringRes
fun PartyType.labelRes(): Int = when (this) {
    PartyType.CUSTOMER -> R.string.customer
    PartyType.SUPPLIER -> R.string.supplier
}

@StringRes
fun expenseCategoryRes(key: String?): Int = when (key) {
    "rent" -> R.string.exp_rent
    "salary" -> R.string.exp_salary
    "electricity" -> R.string.exp_electricity
    "transport" -> R.string.exp_transport
    "food" -> R.string.exp_food
    "mobile" -> R.string.exp_mobile
    "repair" -> R.string.exp_repair
    "tax" -> R.string.exp_tax
    else -> R.string.exp_other
}

@StringRes
fun businessTypeRes(key: String): Int = when (key) {
    "general_store" -> R.string.biz_general_store
    "grocery" -> R.string.biz_grocery
    "pharmacy" -> R.string.biz_pharmacy
    "restaurant" -> R.string.biz_restaurant
    "fashion" -> R.string.biz_fashion
    "electronics" -> R.string.biz_electronics
    "wholesale" -> R.string.biz_wholesale
    "service" -> R.string.biz_service
    else -> R.string.biz_other
}

@StringRes
fun unitRes(key: String): Int = when (key) {
    "pcs" -> R.string.unit_pcs
    "kg" -> R.string.unit_kg
    "g" -> R.string.unit_g
    "litre" -> R.string.unit_litre
    "ml" -> R.string.unit_ml
    "dozen" -> R.string.unit_dozen
    "box" -> R.string.unit_box
    "carton" -> R.string.unit_carton
    "packet" -> R.string.unit_packet
    "metre" -> R.string.unit_metre
    "bag" -> R.string.unit_bag
    else -> R.string.unit_pcs
}
