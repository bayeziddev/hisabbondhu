package com.hisabbondhu.app.ui.screens.tx

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.hisabbondhu.app.R
import com.hisabbondhu.app.data.repo.LedgerValidationException
import com.hisabbondhu.app.domain.SyncState
import com.hisabbondhu.app.domain.TxType
import com.hisabbondhu.app.ui.appContainer
import com.hisabbondhu.app.ui.color
import com.hisabbondhu.app.ui.components.AppTopBar
import com.hisabbondhu.app.ui.components.ConfirmDialog
import com.hisabbondhu.app.ui.components.EmptyState
import com.hisabbondhu.app.ui.components.IconBadge
import com.hisabbondhu.app.ui.components.LabelValue
import com.hisabbondhu.app.ui.components.SectionCard
import com.hisabbondhu.app.ui.dateTime
import com.hisabbondhu.app.ui.expenseCategoryRes
import com.hisabbondhu.app.ui.icon
import com.hisabbondhu.app.ui.labelRes
import com.hisabbondhu.app.ui.money
import com.hisabbondhu.app.ui.nav.Routes
import com.hisabbondhu.app.ui.qty
import com.hisabbondhu.app.ui.theme.LocalMoneyColors
import com.hisabbondhu.app.util.Messages
import com.hisabbondhu.app.util.Share
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun TxDetailScreen(nav: NavHostController) {
    val container = appContainer()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val id = remember { nav.currentBackStackEntry?.arguments?.getString("id").orEmpty() }
    val row by remember(id) { container.ledger.transaction(id) }.collectAsStateWithLifecycle(initialValue = null)
    var confirmReverse by remember { mutableStateOf(false) }
    val mc = LocalMoneyColors.current

    Scaffold(topBar = { AppTopBar(stringResource(R.string.transaction_details), onBack = { nav.popBackStack() }) }) { padding ->
        val r = row
        if (r == null) {
            EmptyState(Icons.Rounded.ReceiptLong, stringResource(R.string.loading), "", Modifier.padding(padding))
            return@Scaffold
        }
        val t = r.tx
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(t.type.icon(), t.type.color(), 48.dp, 26.dp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(stringResource(t.type.labelRes()), style = MaterialTheme.typography.titleMedium)
                        Text(dateTime(t.occurredAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(12.dp))
                if (r.isReversed || t.reversalOf != null) {
                    Tag(stringResource(R.string.cancelled), MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(8.dp))
                }
                r.partyName?.let { LabelValue(stringResource(R.string.party), it) }
                r.productName?.let { LabelValue(stringResource(R.string.product), it) }
                if (t.qtyMilli != 0L) LabelValue(stringResource(R.string.quantity), qty(t.qtyMilli) + " " + (r.productUnit ?: ""))
                if (t.ratePaisa != 0L) LabelValue(stringResource(R.string.rate), money(t.ratePaisa))
                if (t.discountPaisa != 0L) LabelValue(stringResource(R.string.discount), money(t.discountPaisa))
                if (t.type != TxType.STOCK_ADJUST) {
                    LabelValue(stringResource(R.string.total), money(t.totalPaisa), t.type.color())
                    LabelValue(stringResource(R.string.paid_received), money(t.paidPaisa))
                    if (t.totalPaisa - t.paidPaisa != 0L) LabelValue(stringResource(R.string.due), money(t.totalPaisa - t.paidPaisa), mc.negative)
                    LabelValue(stringResource(R.string.payment_method), stringResource(t.method.labelRes()))
                }
                if (t.type == TxType.EXPENSE) LabelValue(stringResource(R.string.category), stringResource(expenseCategoryRes(t.category)))
                t.note?.let { LabelValue(stringResource(R.string.note), it) }
                LabelValue(
                    stringResource(R.string.sync_status),
                    stringResource(
                        when (t.syncState) {
                            SyncState.SYNCED -> R.string.sync_done
                            SyncState.PENDING -> R.string.sync_saved_on_phone
                            SyncState.REJECTED -> R.string.sync_retry_needed
                        },
                    ),
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val balance = t.partyId?.let { container.ledger.partyWithBalance(it) }
                            val biz = container.settings.business.first()?.name.orEmpty()
                            Share.text(context, Messages.forTransaction(context, r, balance?.party?.type, balance?.receivablePaisa, biz))
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Rounded.Share, null)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.share))
                }
                if (!r.isReversed && t.reversalOf == null) {
                    OutlinedButton(onClick = { confirmReverse = true }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.Undo, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.cancel_transaction), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            if (t.partyId != null) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { nav.navigate(Routes.party(t.partyId)) }, modifier = Modifier.padding(top = 4.dp)) {
                    Text(stringResource(R.string.open_party_ledger))
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.reversal_explainer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (confirmReverse) {
        ConfirmDialog(
            title = stringResource(R.string.cancel_transaction_title),
            message = stringResource(R.string.cancel_transaction_message),
            confirmLabel = stringResource(R.string.cancel_transaction),
            destructive = true,
            onConfirm = {
                confirmReverse = false
                scope.launch {
                    try {
                        container.ledger.reverseTransaction(id)
                        Toast.makeText(context, R.string.transaction_cancelled, Toast.LENGTH_SHORT).show()
                    } catch (_: LedgerValidationException) {
                        Toast.makeText(context, R.string.error_already_reversed, Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onDismiss = { confirmReverse = false },
        )
    }
}
