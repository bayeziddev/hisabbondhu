package com.hisabbondhu.app.ui.screens.parties

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.hisabbondhu.app.AppContainer
import com.hisabbondhu.app.R
import com.hisabbondhu.app.core.Dates
import com.hisabbondhu.app.data.local.PartyWithBalance
import com.hisabbondhu.app.data.local.TxRow
import com.hisabbondhu.app.domain.PartyType
import com.hisabbondhu.app.domain.TxType
import com.hisabbondhu.app.sync.Notifications
import com.hisabbondhu.app.ui.appContainer
import com.hisabbondhu.app.ui.appViewModel
import com.hisabbondhu.app.ui.components.AppTopBar
import com.hisabbondhu.app.ui.components.ConfirmDialog
import com.hisabbondhu.app.ui.components.EmptyState
import com.hisabbondhu.app.ui.components.NameAvatar
import com.hisabbondhu.app.ui.components.SectionCard
import com.hisabbondhu.app.ui.date
import com.hisabbondhu.app.ui.money
import com.hisabbondhu.app.ui.nav.Routes
import com.hisabbondhu.app.ui.screens.tx.TxListItem
import com.hisabbondhu.app.ui.theme.LocalMoneyColors
import com.hisabbondhu.app.util.Messages
import com.hisabbondhu.app.util.PdfStatement
import com.hisabbondhu.app.util.Share
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PartyDetailUi(
    val party: PartyWithBalance? = null,
    val rows: List<TxRow> = emptyList(),
    val businessName: String = "",
    val businessPhone: String = "",
    val loaded: Boolean = false,
)

class PartyDetailViewModel(private val c: AppContainer, state: SavedStateHandle) : ViewModel() {
    val id: String = checkNotNull(state["id"])

    val ui: StateFlow<PartyDetailUi> = combine(
        c.ledger.party(id), c.ledger.partyTransactions(id), c.settings.business,
    ) { p, rows, biz ->
        PartyDetailUi(p, rows, biz?.name.orEmpty(), biz?.phone.orEmpty(), loaded = true)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PartyDetailUi())

    fun setReminder(at: Long?) = viewModelScope.launch { c.ledger.setReminder(id, at) }
    fun delete(onDone: () -> Unit) = viewModelScope.launch { c.ledger.deleteParty(id); onDone() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartyDetailScreen(nav: NavHostController) {
    val vm = appViewModel { c, s -> PartyDetailViewModel(c, s) }
    val ui by vm.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val container = appContainer()
    val scope = rememberCoroutineScope()
    val mc = LocalMoneyColors.current
    var menu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var pickDate by remember { mutableStateOf(false) }
    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    val p = ui.party
    if (p == null) {
        Scaffold(topBar = { AppTopBar("", onBack = { nav.popBackStack() }) }) { pad ->
            if (ui.loaded) {
                EmptyState(Icons.Rounded.ReceiptLong, stringResource(R.string.not_found), "", Modifier.padding(pad))
            }
        }
        return
    }
    val party = p.party
    val isCustomer = party.type == PartyType.CUSTOMER
    val (label, color) = balanceLabel(party.type, p.receivablePaisa)
    val statementText = { Messages.statement(context, party.name, party.type, p.receivablePaisa, ui.businessName) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = party.name,
                subtitle = stringResource(if (isCustomer) R.string.customer else R.string.supplier),
                onBack = { nav.popBackStack() },
                actions = {
                    party.phone?.let { phone ->
                        IconButton(onClick = { Share.dial(context, phone) }) { Icon(Icons.Rounded.Call, stringResource(R.string.call)) }
                    }
                    IconButton(onClick = { nav.navigate(Routes.partyEdit(party.type, party.id)) }) {
                        Icon(Icons.Rounded.Edit, stringResource(R.string.edit))
                    }
                    IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, null) }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete)) },
                            leadingIcon = { Icon(Icons.Rounded.Delete, null) },
                            onClick = { menu = false; confirmDelete = true },
                        )
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                SectionCard(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        NameAvatar(party.name, 52.dp)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(label, style = MaterialTheme.typography.labelLarge, color = color)
                            Text(money(kotlin.math.abs(p.receivablePaisa)), style = MaterialTheme.typography.headlineMedium, color = color)
                            party.phone?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                    }
                    if (isCustomer) {
                        Spacer(Modifier.height(10.dp))
                        TextButton(onClick = { pickDate = true }) {
                            Icon(Icons.Rounded.Alarm, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                party.reminderAt?.let { stringResource(R.string.reminder_on, date(it)) }
                                    ?: stringResource(R.string.set_reminder),
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (isCustomer) {
                            Button(
                                onClick = { nav.navigate(Routes.txNew(TxType.PAYMENT_IN, partyId = party.id)) },
                                modifier = Modifier.weight(1f).height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = mc.paymentIn),
                            ) { Text(stringResource(R.string.action_payment_in)) }
                            OutlinedButton(
                                onClick = { nav.navigate(Routes.txNew(TxType.SALE, partyId = party.id)) },
                                modifier = Modifier.weight(1f).height(48.dp),
                            ) { Text(stringResource(R.string.action_credit_sale)) }
                        } else {
                            Button(
                                onClick = { nav.navigate(Routes.txNew(TxType.PAYMENT_OUT, partyId = party.id)) },
                                modifier = Modifier.weight(1f).height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = mc.purchase),
                            ) { Text(stringResource(R.string.action_payment_out)) }
                            OutlinedButton(
                                onClick = { nav.navigate(Routes.txNew(TxType.PURCHASE, partyId = party.id)) },
                                modifier = Modifier.weight(1f).height(48.dp),
                            ) { Text(stringResource(R.string.action_credit_purchase)) }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        ShareButton(Icons.Rounded.Chat, "WhatsApp") { Share.whatsApp(context, party.phone, statementText()) }
                        ShareButton(Icons.Rounded.Sms, stringResource(R.string.sms)) { Share.sms(context, party.phone, statementText()) }
                        ShareButton(Icons.Rounded.Share, stringResource(R.string.share)) { Share.text(context, statementText()) }
                        ShareButton(Icons.Rounded.PictureAsPdf, "PDF") {
                            scope.launch {
                                val rows = container.ledger.partyStatementRows(party.id)
                                val biz = container.settings.business.first()
                                val file = PdfStatement.create(context, p, rows, biz?.name.orEmpty(), biz?.phone.orEmpty())
                                Share.file(context, file, "application/pdf", context.getString(R.string.pdf_statement_title))
                            }
                        }
                    }
                }
            }
            item {
                Text(
                    stringResource(R.string.transaction_history),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            if (ui.rows.isEmpty()) {
                item {
                    EmptyState(Icons.Rounded.ReceiptLong, stringResource(R.string.no_transactions_yet), stringResource(R.string.no_transactions_party))
                }
            }
            items(ui.rows, key = { it.tx.id }) { row -> TxListItem(row) { nav.navigate(Routes.tx(row.tx.id)) } }
        }
    }

    if (pickDate) {
        val state = rememberDatePickerState(initialSelectedDateMillis = Dates.toPickerMillis(party.reminderAt ?: Dates.now()))
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    pickDate = false
                    state.selectedDateMillis?.let { vm.setReminder(Dates.startOfDay(Dates.fromPickerMillis(it)) + 10L * 3600 * 1000) }
                    if (Build.VERSION.SDK_INT >= 33 && !Notifications.canNotify(context)) {
                        notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    Toast.makeText(context, R.string.reminder_saved, Toast.LENGTH_SHORT).show()
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { pickDate = false; vm.setReminder(null) }) { Text(stringResource(R.string.remove_reminder)) }
            },
        ) { DatePicker(state = state) }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.delete_party_title),
            message = stringResource(R.string.delete_party_message),
            confirmLabel = stringResource(R.string.delete),
            destructive = true,
            onConfirm = { confirmDelete = false; vm.delete { nav.popBackStack() } },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun ShareButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onClick) { Icon(icon, label, tint = MaterialTheme.colorScheme.primary) }
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
