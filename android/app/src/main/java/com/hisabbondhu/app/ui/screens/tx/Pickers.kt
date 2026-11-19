package com.hisabbondhu.app.ui.screens.tx

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hisabbondhu.app.R
import com.hisabbondhu.app.data.local.PartyWithBalance
import com.hisabbondhu.app.data.local.ProductWithStock
import com.hisabbondhu.app.domain.PartyType
import com.hisabbondhu.app.ui.components.SearchField
import com.hisabbondhu.app.ui.screens.parties.PartyRow
import com.hisabbondhu.app.ui.screens.products.ProductRow

@Composable
private fun PickerFrame(title: String, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize().imePadding(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, stringResource(R.string.close)) }
                }
                content()
            }
        }
    }
}

@Composable
fun PartyPickerDialog(
    parties: List<PartyWithBalance>,
    initialType: PartyType,
    onPick: (PartyWithBalance) -> Unit,
    onQuickAdd: (name: String, phone: String, type: PartyType) -> Unit,
    onDismiss: () -> Unit,
) {
    var type by remember { mutableStateOf(initialType) }
    var query by remember { mutableStateOf("") }
    var adding by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newPhone by remember { mutableStateOf("") }
    val needle = query.trim().lowercase()
    val list = parties.filter { it.party.type == type }
        .filter { needle.isEmpty() || it.party.name.lowercase().contains(needle) || (it.party.phone ?: "").contains(needle) }

    PickerFrame(stringResource(if (type == PartyType.CUSTOMER) R.string.select_customer else R.string.select_supplier), onDismiss) {
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(type == PartyType.CUSTOMER, { type = PartyType.CUSTOMER }, { Text(stringResource(R.string.customers)) })
            FilterChip(type == PartyType.SUPPLIER, { type = PartyType.SUPPLIER }, { Text(stringResource(R.string.suppliers)) })
        }
        SearchField(query, { query = it }, stringResource(R.string.search_name_phone), Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        if (adding) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                OutlinedTextField(
                    newName, { newName = it }, Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.name) + " *") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    newPhone, { newPhone = it.filter { c -> c.isDigit() || c == '+' } }, Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.mobile_number)) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                )
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { adding = false }) { Text(stringResource(R.string.cancel)) }
                    Spacer(Modifier.width(8.dp))
                    Button(enabled = newName.isNotBlank(), onClick = { onQuickAdd(newName, newPhone, type) }) { Text(stringResource(R.string.add_and_select)) }
                }
            }
            HorizontalDivider()
        } else {
            TextButton(onClick = { adding = true; newName = query }, modifier = Modifier.padding(horizontal = 8.dp)) {
                Icon(Icons.Rounded.Add, null)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(if (type == PartyType.CUSTOMER) R.string.add_customer else R.string.add_supplier))
            }
        }
        LazyColumn(Modifier.fillMaxWidth()) {
            items(list, key = { it.party.id }) { p -> PartyRow(p) { onPick(p) } }
            if (list.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.no_match),
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
fun ProductPickerDialog(
    products: List<ProductWithStock>,
    onPick: (ProductWithStock) -> Unit,
    onAddNew: () -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val needle = query.trim().lowercase()
    val list = products.filter {
        needle.isEmpty() || it.product.name.lowercase().contains(needle) || (it.product.barcode ?: "").contains(needle)
    }
    PickerFrame(stringResource(R.string.select_product), onDismiss) {
        SearchField(query, { query = it }, stringResource(R.string.search_product), Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        TextButton(onClick = onAddNew, modifier = Modifier.padding(horizontal = 8.dp)) {
            Icon(Icons.Rounded.Add, null)
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.add_product))
        }
        LazyColumn(Modifier.fillMaxWidth()) {
            items(list, key = { it.product.id }) { p -> ProductRow(p) { onPick(p) } }
            if (list.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.no_match),
                        modifier = Modifier.fillMaxWidth().clickable(onClick = onAddNew).padding(24.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
