package com.hisabbondhu.app.ui.screens.products

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.hisabbondhu.app.R
import com.hisabbondhu.app.core.Money
import com.hisabbondhu.app.core.Qty
import com.hisabbondhu.app.domain.UNITS
import com.hisabbondhu.app.ui.appContainer
import com.hisabbondhu.app.ui.components.AmountField
import com.hisabbondhu.app.ui.components.AppTopBar
import com.hisabbondhu.app.ui.unitRes
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProductEditScreen(nav: NavHostController) {
    val container = appContainer()
    val scope = rememberCoroutineScope()
    val id = remember { nav.currentBackStackEntry?.arguments?.getString("id") }

    var name by rememberSaveable { mutableStateOf("") }
    var unit by rememberSaveable { mutableStateOf("pcs") }
    var salePrice by rememberSaveable { mutableStateOf("") }
    var buyPrice by rememberSaveable { mutableStateOf("") }
    var openingStock by rememberSaveable { mutableStateOf("") }
    var minStock by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("") }
    var sku by rememberSaveable { mutableStateOf("") }
    var barcode by rememberSaveable { mutableStateOf("") }
    var loaded by rememberSaveable { mutableStateOf(false) }
    var nameError by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(id) {
        if (id != null && !loaded) {
            container.ledger.getProduct(id)?.let {
                name = it.name; unit = it.unit; salePrice = Money.toInput(it.salePricePaisa)
                buyPrice = Money.toInput(it.purchasePricePaisa); openingStock = Qty.toInput(it.openingStockMilli)
                minStock = Qty.toInput(it.minStockMilli); category = it.category.orEmpty()
                sku = it.sku.orEmpty(); barcode = it.barcode.orEmpty()
            }
        }
        loaded = true
    }

    Scaffold(
        topBar = { AppTopBar(stringResource(if (id == null) R.string.add_product else R.string.edit_product), onBack = { nav.popBackStack() }) },
        bottomBar = {
            Button(
                enabled = !saving,
                onClick = {
                    if (name.isBlank()) { nameError = true; return@Button }
                    saving = true
                    scope.launch {
                        container.ledger.saveProduct(
                            id = id, name = name, category = category, sku = sku, barcode = barcode, unit = unit,
                            purchasePricePaisa = Money.parseToPaisa(buyPrice) ?: 0,
                            salePricePaisa = Money.parseToPaisa(salePrice) ?: 0,
                            openingStockMilli = Qty.parseToMilli(openingStock) ?: 0,
                            minStockMilli = Qty.parseToMilli(minStock) ?: 0,
                        )
                        nav.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(16.dp).height(52.dp),
            ) { Text(stringResource(R.string.save)) }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = name, onValueChange = { name = it; nameError = false },
                label = { Text(stringResource(R.string.product_name) + " *") },
                isError = nameError,
                supportingText = if (nameError) { { Text(stringResource(R.string.error_required)) } } else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.unit), style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                UNITS.forEach { u -> FilterChip(unit == u, { unit = u }, { Text(stringResource(unitRes(u))) }) }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AmountField(salePrice, { salePrice = it }, stringResource(R.string.sale_price), Modifier.weight(1f))
                AmountField(buyPrice, { buyPrice = it }, stringResource(R.string.purchase_price), Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QtyField(openingStock, { openingStock = it }, stringResource(R.string.opening_stock), Modifier.weight(1f))
                QtyField(minStock, { minStock = it }, stringResource(R.string.min_stock), Modifier.weight(1f))
            }
            Text(
                stringResource(R.string.min_stock_help),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp),
            )
            OutlinedTextField(
                value = category, onValueChange = { category = it },
                label = { Text(stringResource(R.string.category)) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(sku, { sku = it }, Modifier.weight(1f), label = { Text("SKU") }, singleLine = true)
                OutlinedTextField(
                    barcode, { barcode = it }, Modifier.weight(1f),
                    label = { Text(stringResource(R.string.barcode)) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun QtyField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = { new -> onValueChange(new.filter { it.isDigit() || it == '.' || it in '০'..'৯' }) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
    )
}
