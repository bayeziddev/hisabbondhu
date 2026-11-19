package com.hisabbondhu.app.ui.screens.products

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.hisabbondhu.app.AppContainer
import com.hisabbondhu.app.R
import com.hisabbondhu.app.data.local.ProductWithStock
import com.hisabbondhu.app.data.local.TxRow
import com.hisabbondhu.app.domain.TxType
import com.hisabbondhu.app.ui.appViewModel
import com.hisabbondhu.app.ui.components.AppTopBar
import com.hisabbondhu.app.ui.components.ConfirmDialog
import com.hisabbondhu.app.ui.components.EmptyState
import com.hisabbondhu.app.ui.components.LabelValue
import com.hisabbondhu.app.ui.components.SectionCard
import com.hisabbondhu.app.ui.money
import com.hisabbondhu.app.ui.nav.Routes
import com.hisabbondhu.app.ui.qty
import com.hisabbondhu.app.ui.screens.tx.TxListItem
import com.hisabbondhu.app.ui.theme.LocalMoneyColors
import com.hisabbondhu.app.ui.unitRes
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProductDetailUi(val product: ProductWithStock? = null, val rows: List<TxRow> = emptyList(), val loaded: Boolean = false)

class ProductDetailViewModel(private val c: AppContainer, state: SavedStateHandle) : ViewModel() {
    val id: String = checkNotNull(state["id"])
    val ui: StateFlow<ProductDetailUi> = combine(c.ledger.product(id), c.ledger.productTransactions(id)) { p, rows ->
        ProductDetailUi(p, rows, true)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProductDetailUi())

    fun delete(onDone: () -> Unit) = viewModelScope.launch { c.ledger.deleteProduct(id); onDone() }
}

@Composable
fun ProductDetailScreen(nav: NavHostController) {
    val vm = appViewModel { c, s -> ProductDetailViewModel(c, s) }
    val ui by vm.ui.collectAsStateWithLifecycle()
    val mc = LocalMoneyColors.current
    var confirmDelete by remember { mutableStateOf(false) }
    val p = ui.product

    Scaffold(
        topBar = {
            AppTopBar(
                p?.product?.name.orEmpty(),
                onBack = { nav.popBackStack() },
                actions = {
                    if (p != null) {
                        IconButton(onClick = { nav.navigate(Routes.productEdit(p.product.id)) }) { Icon(Icons.Rounded.Edit, stringResource(R.string.edit)) }
                        IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Rounded.Delete, stringResource(R.string.delete)) }
                    }
                },
            )
        },
    ) { padding ->
        if (p == null) {
            if (ui.loaded) EmptyState(Icons.Rounded.ReceiptLong, stringResource(R.string.not_found), "", Modifier.padding(padding))
            return@Scaffold
        }
        val unit = stringResource(unitRes(p.product.unit))
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                SectionCard(Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.current_stock), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        qty(p.stockMilli) + " " + unit,
                        style = MaterialTheme.typography.headlineMedium,
                        color = if (p.isLow() || p.stockMilli <= 0) mc.negative else MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(8.dp))
                    LabelValue(stringResource(R.string.sale_price), money(p.product.salePricePaisa))
                    LabelValue(stringResource(R.string.purchase_price), money(p.product.purchasePricePaisa))
                    if (p.product.minStockMilli > 0) LabelValue(stringResource(R.string.min_stock), qty(p.product.minStockMilli) + " " + unit)
                    p.product.sku?.let { LabelValue("SKU", it) }
                    p.product.barcode?.let { LabelValue(stringResource(R.string.barcode), it) }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { nav.navigate(Routes.txNew(TxType.SALE, productId = p.product.id)) }, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.action_sale))
                        }
                        OutlinedButton(onClick = { nav.navigate(Routes.txNew(TxType.PURCHASE, productId = p.product.id)) }, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.stock_in))
                        }
                        OutlinedButton(onClick = { nav.navigate(Routes.txNew(TxType.STOCK_ADJUST, productId = p.product.id)) }, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.adjust))
                        }
                    }
                }
            }
            item {
                Text(stringResource(R.string.stock_history), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            }
            items(ui.rows, key = { it.tx.id }) { row -> TxListItem(row) { nav.navigate(Routes.tx(row.tx.id)) } }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.delete_product_title),
            message = stringResource(R.string.delete_product_message),
            confirmLabel = stringResource(R.string.delete),
            destructive = true,
            onConfirm = { confirmDelete = false; vm.delete { nav.popBackStack() } },
            onDismiss = { confirmDelete = false },
        )
    }
}
