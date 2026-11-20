package com.hisabbondhu.app.ui.screens.tx

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.hisabbondhu.app.R
import com.hisabbondhu.app.core.Dates
import com.hisabbondhu.app.data.local.TxRow
import com.hisabbondhu.app.data.repo.LedgerValidationException.Reason
import com.hisabbondhu.app.domain.EXPENSE_CATEGORIES
import com.hisabbondhu.app.domain.PartyType
import com.hisabbondhu.app.domain.PayMethod
import com.hisabbondhu.app.domain.TxType
import com.hisabbondhu.app.ui.appViewModel
import com.hisabbondhu.app.ui.color
import com.hisabbondhu.app.ui.components.AmountField
import com.hisabbondhu.app.ui.components.AppTopBar
import com.hisabbondhu.app.ui.components.NameAvatar
import com.hisabbondhu.app.ui.date
import com.hisabbondhu.app.ui.expenseCategoryRes
import com.hisabbondhu.app.ui.icon
import com.hisabbondhu.app.ui.labelRes
import com.hisabbondhu.app.ui.money
import com.hisabbondhu.app.ui.nav.Routes
import com.hisabbondhu.app.ui.qty
import com.hisabbondhu.app.ui.screens.products.QtyField
import com.hisabbondhu.app.ui.theme.LocalMoneyColors
import com.hisabbondhu.app.ui.unitRes
import com.hisabbondhu.app.util.Messages
import com.hisabbondhu.app.util.Share
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TxEntryScreen(nav: NavHostController) {
    val vm = appViewModel { c, s -> TxEntryViewModel(c, s) }
    val parties by vm.parties.collectAsStateWithLifecycle()
    val products by vm.products.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mc = LocalMoneyColors.current
    val type = vm.type
    val accent = type.color()

    var showPartyPicker by remember { mutableStateOf(false) }
    var showProductPicker by remember { mutableStateOf(false) }
    var showDate by remember { mutableStateOf(false) }
    val amountFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        if (!vm.showsProduct && type != TxType.STOCK_ADJUST) runCatching { amountFocus.requestFocus() }
    }

    val stock = products.firstOrNull { it.product.id == vm.product?.id }?.stockMilli

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(type.labelRes()),
                onBack = { nav.popBackStack() },
                containerColor = accent,
                contentColor = Color.White,
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp, shadowElevation = 8.dp) {
                Column(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(16.dp)) {
                    if (type != TxType.STOCK_ADJUST) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.total), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            Text(money(vm.totalPaisa), style = MaterialTheme.typography.titleLarge, color = accent)
                        }
                        if (vm.allowsCredit && vm.credit && vm.duePaisa > 0) {
                            Text(
                                stringResource(
                                    if (type == TxType.PURCHASE) R.string.will_be_payable else R.string.will_be_due,
                                    money(vm.duePaisa),
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = mc.negative,
                            )
                        }
                    }
                    vm.error?.let { Text(errorText(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp)) }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { vm.save() },
                        enabled = !vm.saving,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.White),
                    ) { Text(stringResource(R.string.save), style = MaterialTheme.typography.titleMedium) }
                }
            }
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Cash / credit switch
            if (vm.allowsCredit) {
                val (cashLabel, creditLabel) = when (type) {
                    TxType.SALE -> R.string.mode_cash_sale to R.string.mode_credit_sale
                    TxType.PURCHASE -> R.string.mode_cash_purchase to R.string.mode_credit_purchase
                    else -> R.string.mode_cash_refund to R.string.mode_adjust_due
                }
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = !vm.credit, onClick = { vm.credit = false },
                        shape = SegmentedButtonDefaults.itemShape(0, 2),
                    ) { Text(stringResource(cashLabel)) }
                    SegmentedButton(
                        selected = vm.credit, onClick = { vm.credit = true },
                        shape = SegmentedButtonDefaults.itemShape(1, 2),
                    ) { Text(stringResource(creditLabel)) }
                }
            }

            // Party
            if (vm.showsParty) {
                val required = type == TxType.PAYMENT_IN || type == TxType.PAYMENT_OUT || (vm.allowsCredit && vm.credit)
                val p = vm.party
                SelectorCard(
                    icon = Icons.Rounded.Person,
                    title = p?.name ?: stringResource(
                        if (vm.defaultPartyType == PartyType.CUSTOMER) R.string.select_customer else R.string.select_supplier,
                    ),
                    subtitle = p?.phone ?: stringResource(if (required) R.string.required else R.string.optional),
                    selected = p != null,
                    highlight = required && p == null && vm.error == Reason.PARTY_REQUIRED,
                    onClick = { showPartyPicker = true },
                    onClear = if (p != null) { { vm.party = null } } else null,
                    avatarName = p?.name,
                )
            }

            // Product
            if (vm.showsProduct) {
                val pr = vm.product
                SelectorCard(
                    icon = Icons.Rounded.Inventory2,
                    title = pr?.name ?: stringResource(R.string.select_product),
                    subtitle = if (pr != null && stock != null) {
                        stringResource(R.string.in_stock, qty(stock) + " " + stringResource(unitRes(pr.unit)))
                    } else {
                        stringResource(if (type == TxType.STOCK_ADJUST) R.string.required else R.string.product_optional_hint)
                    },
                    selected = pr != null,
                    highlight = vm.error == Reason.PRODUCT_REQUIRED,
                    onClick = { showProductPicker = true },
                    onClear = if (pr != null) { { vm.clearProduct() } } else null,
                )
                if (pr != null && type != TxType.STOCK_ADJUST) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        QtyField(vm.qty, { vm.qty = it }, stringResource(R.string.quantity) + " (" + stringResource(unitRes(pr.unit)) + ")", Modifier.weight(1f))
                        AmountField(vm.rate, { vm.rate = it }, stringResource(R.string.rate), Modifier.weight(1f))
                    }
                    AmountField(vm.discount, { vm.discount = it }, stringResource(R.string.discount))
                }
            }

            // Stock adjustment
            if (type == TxType.STOCK_ADJUST) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    SegmentedButton(vm.increase, { vm.increase = true }, SegmentedButtonDefaults.itemShape(0, 2)) { Text(stringResource(R.string.stock_increase)) }
                    SegmentedButton(!vm.increase, { vm.increase = false }, SegmentedButtonDefaults.itemShape(1, 2)) { Text(stringResource(R.string.stock_decrease)) }
                }
                QtyField(vm.qty, { vm.qty = it }, stringResource(R.string.quantity), Modifier.fillMaxWidth())
            }

            // Amount (when no product line)
            if (type != TxType.STOCK_ADJUST && vm.product == null) {
                AmountField(
                    value = vm.amount,
                    onValueChange = { vm.amount = it; vm.error = null },
                    label = stringResource(amountLabel(type)),
                    big = true,
                    isError = vm.error == Reason.AMOUNT_REQUIRED,
                    modifier = Modifier.focusRequester(amountFocus).testTag("amount"),
                    imeAction = ImeAction.Done,
                )
            }

            // Paid now (credit mode)
            if (vm.allowsCredit && vm.credit) {
                AmountField(
                    value = vm.paid,
                    onValueChange = { vm.paid = it; vm.error = null },
                    label = stringResource(paidLabel(type)),
                    isError = vm.error == Reason.PAID_TOO_HIGH,
                    supportingText = stringResource(R.string.paid_now_help),
                )
            }

            // Expense category
            if (type == TxType.EXPENSE) {
                Text(stringResource(R.string.category), style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EXPENSE_CATEGORIES.forEach { key ->
                        FilterChip(vm.category == key, { vm.category = key }, { Text(stringResource(expenseCategoryRes(key))) })
                    }
                }
            }

            // Payment method
            if (type != TxType.STOCK_ADJUST && vm.paidPaisa > 0) {
                Text(stringResource(R.string.payment_method), style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PayMethod.entries.filter { it != PayMethod.OTHER }.forEach { m ->
                        FilterChip(
                            selected = vm.method == m,
                            onClick = { vm.method = m },
                            label = { Text(stringResource(m.labelRes())) },
                            leadingIcon = { Icon(m.icon(), null, Modifier.size(18.dp)) },
                        )
                    }
                }
            }

            // Date + note
            AssistChip(
                onClick = { showDate = true },
                label = { Text(date(vm.occurredAt)) },
                leadingIcon = { Icon(Icons.Rounded.CalendarMonth, null, Modifier.size(18.dp)) },
            )
            OutlinedTextField(
                value = vm.note,
                onValueChange = { vm.note = it },
                label = { Text(stringResource(if (type == TxType.STOCK_ADJUST || type == TxType.SALE_RETURN || type == TxType.PURCHASE_RETURN) R.string.reason else R.string.note)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 1,
                maxLines = 3,
            )
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showPartyPicker) {
        PartyPickerDialog(
            parties = parties,
            initialType = vm.defaultPartyType,
            onPick = { vm.party = it.party; vm.error = null; showPartyPicker = false },
            onQuickAdd = { n, ph, t -> vm.quickAddParty(n, ph, t); showPartyPicker = false },
            onDismiss = { showPartyPicker = false },
        )
    }
    if (showProductPicker) {
        ProductPickerDialog(
            products = products,
            onPick = { vm.selectProduct(it); showProductPicker = false },
            onAddNew = { showProductPicker = false; nav.navigate(Routes.productEdit()) },
            onDismiss = { showProductPicker = false },
        )
    }
    if (showDate) {
        val state = rememberDatePickerState(initialSelectedDateMillis = Dates.toPickerMillis(vm.occurredAt))
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { vm.occurredAt = Dates.withDate(Dates.fromPickerMillis(it)) }
                    showDate = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text(stringResource(R.string.cancel)) } },
        ) { DatePicker(state = state) }
    }

    vm.saved?.let { tx ->
        AlertDialog(
            onDismissRequest = { nav.popBackStack() },
            icon = { Icon(Icons.Rounded.CheckCircle, null, tint = mc.positive, modifier = Modifier.size(40.dp)) },
            title = { Text(stringResource(R.string.saved_title)) },
            text = {
                Column {
                    Text(stringResource(type.labelRes()) + ": " + money(if (type == TxType.PAYMENT_IN || type == TxType.PAYMENT_OUT) tx.paidPaisa else tx.totalPaisa), fontWeight = FontWeight.SemiBold)
                    if (tx.totalPaisa - tx.paidPaisa > 0 && vm.party != null) {
                        Text(stringResource(R.string.due_amount, money(tx.totalPaisa - tx.paidPaisa)), color = mc.negative)
                    }
                    Text(stringResource(R.string.saved_offline_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                Button(onClick = { nav.popBackStack() }) { Text(stringResource(R.string.done)) }
            },
            dismissButton = {
                Row {
                    if (vm.party != null) {
                        TextButton(onClick = {
                            val party = vm.party ?: return@TextButton
                            scope.launch {
                                val balance = vm.partyBalance(party.id)
                                val row = TxRow(tx, party.name, party.phone, vm.product?.name, vm.product?.unit, false)
                                val msg = Messages.forTransaction(context, row, party.type, balance, vm.businessName())
                                Share.text(context, msg)
                            }
                        }) { Text(stringResource(R.string.send_message)) }
                    }
                    TextButton(onClick = { vm.reset() }) { Text(stringResource(R.string.add_another)) }
                }
            },
        )
    }
}

@Composable
private fun SelectorCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    selected: Boolean,
    highlight: Boolean,
    onClick: () -> Unit,
    onClear: (() -> Unit)?,
    avatarName: String? = null,
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (highlight) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (avatarName != null) NameAvatar(avatarName, 40.dp) else Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = if (highlight) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (onClear != null) {
                IconButton(onClick = onClear) { Icon(Icons.Rounded.Close, stringResource(R.string.clear)) }
            } else {
                OutlinedButton(onClick = onClick) { Text(stringResource(R.string.choose)) }
            }
        }
    }
}

private fun amountLabel(type: TxType): Int = when (type) {
    TxType.SALE -> R.string.sale_amount
    TxType.PURCHASE -> R.string.purchase_amount
    TxType.PAYMENT_IN -> R.string.amount_received
    TxType.PAYMENT_OUT -> R.string.amount_paid
    TxType.EXPENSE -> R.string.expense_amount
    TxType.SALE_RETURN, TxType.PURCHASE_RETURN -> R.string.return_amount
    else -> R.string.amount
}

private fun paidLabel(type: TxType): Int = when (type) {
    TxType.SALE -> R.string.received_now
    TxType.PURCHASE -> R.string.paid_now
    TxType.SALE_RETURN -> R.string.refund_given_now
    else -> R.string.refund_received_now
}

@Composable
private fun errorText(reason: Reason): String = stringResource(
    when (reason) {
        Reason.AMOUNT_REQUIRED -> R.string.error_amount_required
        Reason.PAID_TOO_HIGH -> R.string.error_paid_too_high
        Reason.PARTY_REQUIRED -> R.string.error_party_required
        Reason.PRODUCT_REQUIRED -> R.string.error_product_required
        Reason.QTY_REQUIRED -> R.string.error_qty_required
        Reason.ALREADY_REVERSED -> R.string.error_already_reversed
        Reason.NOT_FOUND -> R.string.not_found
    },
)
