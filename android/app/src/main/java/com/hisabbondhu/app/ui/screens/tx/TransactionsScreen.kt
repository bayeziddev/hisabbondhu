package com.hisabbondhu.app.ui.screens.tx

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.hisabbondhu.app.AppContainer
import com.hisabbondhu.app.R
import com.hisabbondhu.app.core.Dates
import com.hisabbondhu.app.core.ReportPeriod
import com.hisabbondhu.app.data.local.TxRow
import com.hisabbondhu.app.domain.TxType
import com.hisabbondhu.app.ui.appViewModel
import com.hisabbondhu.app.ui.components.AppTopBar
import com.hisabbondhu.app.ui.components.EmptyState
import com.hisabbondhu.app.ui.date
import com.hisabbondhu.app.ui.labelRes
import com.hisabbondhu.app.ui.nav.Routes
import com.hisabbondhu.app.ui.screens.reports.periodLabel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionsViewModel(c: AppContainer) : ViewModel() {
    val period = MutableStateFlow(ReportPeriod.LAST_7_DAYS)
    val typeFilter = MutableStateFlow<TxType?>(null)

    val rows: StateFlow<List<TxRow>?> = combine(
        period.flatMapLatest { p -> Dates.range(p).let { (f, t) -> c.ledger.range(f, t, 1000) } },
        typeFilter,
    ) { list, type -> if (type == null) list else list.filter { it.tx.type == type } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@Composable
fun TransactionsScreen(nav: NavHostController) {
    val vm = appViewModel { c, _ -> TransactionsViewModel(c) }
    val rows by vm.rows.collectAsStateWithLifecycle()
    val period by vm.period.collectAsStateWithLifecycle()
    val typeFilter by vm.typeFilter.collectAsStateWithLifecycle()

    Scaffold(topBar = { AppTopBar(stringResource(R.string.all_transactions), onBack = { nav.popBackStack() }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ReportPeriod.entries.forEach { p ->
                    FilterChip(period == p, { vm.period.value = p }, { Text(stringResource(periodLabel(p))) })
                }
            }
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(typeFilter == null, { vm.typeFilter.value = null }, { Text(stringResource(R.string.all)) })
                listOf(TxType.SALE, TxType.PAYMENT_IN, TxType.EXPENSE, TxType.PURCHASE, TxType.PAYMENT_OUT).forEach { t ->
                    FilterChip(typeFilter == t, { vm.typeFilter.value = t }, { Text(stringResource(t.labelRes())) })
                }
            }
            val list = rows
            if (list != null && list.isEmpty()) {
                EmptyState(Icons.Rounded.ReceiptLong, stringResource(R.string.no_transactions_yet), stringResource(R.string.no_transactions_period))
            }
            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                val grouped = (list ?: emptyList()).groupBy { Dates.localDate(it.tx.occurredAt) }
                grouped.forEach { (_, dayRows) ->
                    item(key = "h-" + dayRows.first().tx.id) {
                        Text(
                            date(dayRows.first().tx.occurredAt),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer).padding(horizontal = 16.dp, vertical = 6.dp),
                        )
                    }
                    items(dayRows, key = { it.tx.id }) { row -> TxListItem(row, showDate = false) { nav.navigate(Routes.tx(row.tx.id)) } }
                }
            }
        }
    }
}
