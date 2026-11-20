package com.hisabbondhu.app.ui.screens.parties

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.hisabbondhu.app.AppContainer
import com.hisabbondhu.app.R
import com.hisabbondhu.app.data.local.PartyWithBalance
import com.hisabbondhu.app.domain.PartyType
import com.hisabbondhu.app.ui.appViewModel
import com.hisabbondhu.app.ui.components.AppTopBar
import com.hisabbondhu.app.ui.components.EmptyState
import com.hisabbondhu.app.ui.components.NameAvatar
import com.hisabbondhu.app.ui.components.SearchField
import com.hisabbondhu.app.ui.components.StatTile
import com.hisabbondhu.app.ui.date
import com.hisabbondhu.app.ui.money
import com.hisabbondhu.app.ui.nav.Routes
import com.hisabbondhu.app.ui.num
import com.hisabbondhu.app.ui.theme.LocalMoneyColors
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

enum class PartySort { DUE, RECENT, NAME }

data class PartiesUi(
    val type: PartyType = PartyType.CUSTOMER,
    val query: String = "",
    val sort: PartySort = PartySort.DUE,
    val items: List<PartyWithBalance> = emptyList(),
    val total: Long = 0,
    val count: Int = 0,
    val loaded: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class PartiesViewModel(c: AppContainer, state: SavedStateHandle) : ViewModel() {
    val type = MutableStateFlow(state.get<String>("tab")?.let { PartyType.valueOf(it) } ?: PartyType.CUSTOMER)
    val query = MutableStateFlow("")
    val sort = MutableStateFlow(PartySort.DUE)

    val ui: StateFlow<PartiesUi> = combine(
        type.flatMapLatest { c.ledger.parties(it) }, type, query, sort,
    ) { list, t, q, s ->
        val needle = q.trim().lowercase()
        val filtered = if (needle.isEmpty()) list else list.filter {
            it.party.name.lowercase().contains(needle) || (it.party.phone ?: "").contains(needle)
        }
        val sorted = when (s) {
            PartySort.DUE -> filtered.sortedByDescending { if (t == PartyType.CUSTOMER) it.receivablePaisa else -it.receivablePaisa }
            PartySort.RECENT -> filtered.sortedByDescending { it.lastTxAt ?: it.party.updatedAt }
            PartySort.NAME -> filtered.sortedBy { it.party.name.lowercase() }
        }
        val total = if (t == PartyType.CUSTOMER) {
            list.sumOf { maxOf(it.receivablePaisa, 0) }
        } else {
            list.sumOf { maxOf(-it.receivablePaisa, 0) }
        }
        PartiesUi(t, q, s, sorted, total, list.size, loaded = true)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PartiesUi())
}

@Composable
fun PartiesScreen(nav: NavHostController) {
    val vm = appViewModel { c, s -> PartiesViewModel(c, s) }
    val ui by vm.ui.collectAsStateWithLifecycle()
    val mc = LocalMoneyColors.current
    val isCustomer = ui.type == PartyType.CUSTOMER

    Scaffold(
        topBar = { AppTopBar(stringResource(R.string.nav_khata)) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { nav.navigate(Routes.partyEdit(ui.type)) },
                icon = { Icon(Icons.Rounded.PersonAdd, null) },
                text = { Text(stringResource(if (isCustomer) R.string.add_customer else R.string.add_supplier)) },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = if (isCustomer) 0 else 1) {
                Tab(selected = isCustomer, onClick = { vm.type.value = PartyType.CUSTOMER }, text = { Text(stringResource(R.string.customers)) })
                Tab(selected = !isCustomer, onClick = { vm.type.value = PartyType.SUPPLIER }, text = { Text(stringResource(R.string.suppliers)) })
            }
            LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                item {
                    Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatTile(
                            stringResource(if (isCustomer) R.string.total_receivable else R.string.total_payable),
                            money(ui.total),
                            if (isCustomer) mc.negative else mc.warning,
                            Modifier.weight(1f),
                        )
                        StatTile(
                            stringResource(if (isCustomer) R.string.customer_count else R.string.supplier_count),
                            num(ui.count),
                            MaterialTheme.colorScheme.primary,
                            Modifier.weight(1f),
                        )
                    }
                }
                item {
                    SearchField(
                        ui.query, { vm.query.value = it },
                        stringResource(R.string.search_name_phone),
                        Modifier.padding(horizontal = 16.dp),
                    )
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(ui.sort == PartySort.DUE, { vm.sort.value = PartySort.DUE }, { Text(stringResource(R.string.sort_due)) })
                        FilterChip(ui.sort == PartySort.RECENT, { vm.sort.value = PartySort.RECENT }, { Text(stringResource(R.string.sort_recent)) })
                        FilterChip(ui.sort == PartySort.NAME, { vm.sort.value = PartySort.NAME }, { Text(stringResource(R.string.sort_name)) })
                    }
                }
                if (ui.loaded && ui.items.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Rounded.Groups,
                            title = stringResource(if (isCustomer) R.string.empty_customers_title else R.string.empty_suppliers_title),
                            message = stringResource(R.string.empty_parties_message),
                        )
                    }
                }
                items(ui.items, key = { it.party.id }) { p ->
                    PartyRow(p) { nav.navigate(Routes.party(p.party.id)) }
                }
            }
        }
    }
}

@Composable
fun PartyRow(p: PartyWithBalance, onClick: () -> Unit) {
    val (label, color) = balanceLabel(p.party.type, p.receivablePaisa)
    Row(
        Modifier.fillMaxWidth().testTag("party_row").clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NameAvatar(p.party.name)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(p.party.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val sub = p.lastTxAt?.let { stringResource(R.string.last_tx_on, date(it)) } ?: p.party.phone.orEmpty()
            if (sub.isNotBlank()) {
                Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(money(kotlin.math.abs(p.receivablePaisa)), style = MaterialTheme.typography.titleMedium, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = color)
        }
    }
}

/** "পাবো" (they owe us), "দেবো" (we owe), or "পরিশোধিত" (settled). */
@Composable
fun balanceLabel(type: PartyType, receivable: Long): Pair<String, Color> {
    val mc = LocalMoneyColors.current
    return when {
        receivable > 0 -> stringResource(R.string.balance_get) to mc.negative
        receivable < 0 -> stringResource(R.string.balance_give) to (if (type == PartyType.SUPPLIER) mc.warning else mc.positive)
        else -> stringResource(R.string.balance_settled) to MaterialTheme.colorScheme.onSurfaceVariant
    }
}
