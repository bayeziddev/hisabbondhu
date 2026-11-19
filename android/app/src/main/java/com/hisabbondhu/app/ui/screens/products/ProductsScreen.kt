package com.hisabbondhu.app.ui.screens.products

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.hisabbondhu.app.AppContainer
import com.hisabbondhu.app.R
import com.hisabbondhu.app.data.local.ProductWithStock
import com.hisabbondhu.app.ui.appViewModel
import com.hisabbondhu.app.ui.components.AppTopBar
import com.hisabbondhu.app.ui.components.EmptyState
import com.hisabbondhu.app.ui.components.IconBadge
import com.hisabbondhu.app.ui.components.SearchField
import com.hisabbondhu.app.ui.components.StatTile
import com.hisabbondhu.app.ui.money
import com.hisabbondhu.app.ui.nav.Routes
import com.hisabbondhu.app.ui.num
import com.hisabbondhu.app.ui.qty
import com.hisabbondhu.app.ui.theme.LocalMoneyColors
import com.hisabbondhu.app.ui.unitRes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ProductsUi(
    val items: List<ProductWithStock> = emptyList(),
    val query: String = "",
    val lowOnly: Boolean = false,
    val total: Int = 0,
    val lowCount: Int = 0,
    val stockValue: Long = 0,
    val loaded: Boolean = false,
)

fun ProductWithStock.isLow(): Boolean = product.minStockMilli > 0 && stockMilli <= product.minStockMilli

class ProductsViewModel(c: AppContainer) : ViewModel() {
    val query = MutableStateFlow("")
    val lowOnly = MutableStateFlow(false)

    val ui: StateFlow<ProductsUi> = combine(c.ledger.products(), c.ledger.stockValue(), query, lowOnly) { list, value, q, low ->
        val needle = q.trim().lowercase()
        val filtered = list.filter {
            (needle.isEmpty() || it.product.name.lowercase().contains(needle) ||
                (it.product.barcode ?: "").contains(needle) || (it.product.sku ?: "").lowercase().contains(needle)) &&
                (!low || it.isLow())
        }
        ProductsUi(filtered, q, low, list.size, list.count { it.isLow() }, value, loaded = true)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProductsUi())
}

@Composable
fun ProductsScreen(nav: NavHostController) {
    val vm = appViewModel { c, _ -> ProductsViewModel(c) }
    val ui by vm.ui.collectAsStateWithLifecycle()
    val mc = LocalMoneyColors.current

    Scaffold(
        topBar = { AppTopBar(stringResource(R.string.nav_products)) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { nav.navigate(Routes.productEdit()) },
                icon = { Icon(Icons.Rounded.Add, null) },
                text = { Text(stringResource(R.string.add_product)) },
            )
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 96.dp)) {
            item {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile(stringResource(R.string.stock_value), money(ui.stockValue), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                    StatTile(stringResource(R.string.total_products), num(ui.total), MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
                    StatTile(stringResource(R.string.low_stock), num(ui.lowCount), mc.warning, Modifier.weight(1f))
                }
            }
            item {
                SearchField(ui.query, { vm.query.value = it }, stringResource(R.string.search_product), Modifier.padding(horizontal = 16.dp))
                Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    FilterChip(ui.lowOnly, { vm.lowOnly.value = !ui.lowOnly }, { Text(stringResource(R.string.low_stock_only)) })
                }
            }
            if (ui.loaded && ui.items.isEmpty()) {
                item {
                    EmptyState(
                        Icons.Rounded.Inventory2,
                        stringResource(R.string.empty_products_title),
                        stringResource(R.string.empty_products_message),
                    )
                }
            }
            items(ui.items, key = { it.product.id }) { p ->
                ProductRow(p) { nav.navigate(Routes.product(p.product.id)) }
            }
        }
    }
}

@Composable
fun ProductRow(p: ProductWithStock, onClick: () -> Unit) {
    val mc = LocalMoneyColors.current
    val low = p.isLow()
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(Icons.Rounded.Inventory2, if (low) mc.warning else MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(p.product.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                stringResource(R.string.sale_price_short, money(p.product.salePricePaisa)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                qty(p.stockMilli) + " " + stringResource(unitRes(p.product.unit)),
                style = MaterialTheme.typography.titleMedium,
                color = if (low || p.stockMilli <= 0) mc.negative else MaterialTheme.colorScheme.onSurface,
            )
            if (low) Text(stringResource(R.string.low_stock), style = MaterialTheme.typography.labelSmall, color = mc.warning)
        }
    }
}
