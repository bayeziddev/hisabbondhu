package com.hisabbondhu.app.ui.screens.parties

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.hisabbondhu.app.R
import com.hisabbondhu.app.core.Money
import com.hisabbondhu.app.domain.PartyType
import com.hisabbondhu.app.ui.appContainer
import com.hisabbondhu.app.ui.components.AmountField
import com.hisabbondhu.app.ui.components.AppTopBar
import kotlinx.coroutines.launch

/** ACTION_PICK on phone numbers: grants one-time access to the chosen contact, no READ_CONTACTS needed. */
private class PickPhoneContract : ActivityResultContract<Unit, Uri?>() {
    override fun createIntent(context: Context, input: Unit) =
        Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
        if (resultCode == Activity.RESULT_OK) intent?.data else null
}

@Composable
fun PartyEditScreen(nav: NavHostController) {
    val container = appContainer()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val args = nav.currentBackStackEntry?.arguments
    val id = remember { args?.getString("id") }
    val type = remember { args?.getString("type")?.let { PartyType.valueOf(it) } ?: PartyType.CUSTOMER }

    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var opening by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var reminderAt by remember { mutableStateOf<Long?>(null) }
    var loaded by rememberSaveable { mutableStateOf(false) }
    var nameError by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(id) {
        if (id != null && !loaded) {
            container.ledger.getParty(id)?.let {
                name = it.name; phone = it.phone.orEmpty(); address = it.address.orEmpty()
                opening = Money.toInput(it.openingBalancePaisa); note = it.note.orEmpty(); reminderAt = it.reminderAt
            }
        }
        loaded = true
    }

    val picker = rememberLauncherForActivityResult(remember { PickPhoneContract() }) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER),
                null, null, null,
            )?.use { c ->
                if (c.moveToFirst()) {
                    if (name.isBlank()) name = c.getString(0).orEmpty()
                    phone = c.getString(1).orEmpty().filter { ch -> ch.isDigit() || ch == '+' }
                }
            }
        }
    }

    val isCustomer = type == PartyType.CUSTOMER
    val title = stringResource(
        when {
            id != null -> R.string.edit
            isCustomer -> R.string.add_customer
            else -> R.string.add_supplier
        },
    )

    Scaffold(
        topBar = { AppTopBar(title, onBack = { nav.popBackStack() }) },
        bottomBar = {
            Button(
                enabled = !saving,
                onClick = {
                    if (name.isBlank()) { nameError = true; return@Button }
                    saving = true
                    scope.launch {
                        container.ledger.saveParty(
                            id = id, type = type, name = name, phone = phone, address = address, note = note,
                            openingBalancePaisa = Money.parseToPaisa(opening) ?: 0, reminderAt = reminderAt,
                        )
                        nav.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(16.dp).height(52.dp),
            ) { Text(stringResource(R.string.save)) }
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; nameError = false },
                label = { Text(stringResource(R.string.name) + " *") },
                isError = nameError,
                supportingText = if (nameError) { { Text(stringResource(R.string.error_required)) } } else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it.filter { c -> c.isDigit() || c == '+' } },
                label = { Text(stringResource(R.string.mobile_number)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                trailingIcon = {
                    IconButton(onClick = { runCatching { picker.launch(Unit) } }) {
                        Icon(Icons.Rounded.Contacts, stringResource(R.string.pick_from_contacts))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            AmountField(
                value = opening,
                onValueChange = { opening = it },
                label = stringResource(if (isCustomer) R.string.opening_due_customer else R.string.opening_due_supplier),
                supportingText = stringResource(R.string.opening_due_help),
            )
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = address, onValueChange = { address = it },
                label = { Text(stringResource(R.string.address)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = note, onValueChange = { note = it },
                label = { Text(stringResource(R.string.note)) },
                modifier = Modifier.fillMaxWidth(), minLines = 2,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}
