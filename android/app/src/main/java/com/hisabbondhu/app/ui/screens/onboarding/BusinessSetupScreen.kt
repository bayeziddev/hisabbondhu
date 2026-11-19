package com.hisabbondhu.app.ui.screens.onboarding

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hisabbondhu.app.R
import com.hisabbondhu.app.core.Dates
import com.hisabbondhu.app.core.Money
import com.hisabbondhu.app.core.newId
import com.hisabbondhu.app.data.prefs.BusinessProfile
import com.hisabbondhu.app.domain.BUSINESS_TYPES
import com.hisabbondhu.app.ui.appContainer
import com.hisabbondhu.app.ui.businessTypeRes
import com.hisabbondhu.app.ui.components.AmountField
import com.hisabbondhu.app.ui.components.AppTopBar
import com.hisabbondhu.app.ui.screens.more.LegalContent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Setup wizard (first run) and business profile editor (from Settings). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BusinessSetupScreen(editMode: Boolean = false, onDone: () -> Unit = {}) {
    val container = appContainer()
    val scope = rememberCoroutineScope()
    var loaded by remember { mutableStateOf(false) }
    var existing by remember { mutableStateOf<BusinessProfile?>(null) }
    var name by rememberSaveable { mutableStateOf("") }
    var owner by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf("general_store") }
    var cash by rememberSaveable { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }
    var legalPage by remember { mutableStateOf<String?>(null) }
    val defaultName = stringResource(R.string.default_shop_name)

    LaunchedEffect(Unit) {
        container.settings.business.first()?.let { b ->
            existing = b
            if (!loaded) {
                name = b.name; owner = b.ownerName; phone = b.phone; address = b.address
                type = b.type; cash = Money.toInput(b.openingCashPaisa)
            }
        }
        loaded = true
    }

    fun save(skip: Boolean) {
        val finalName = name.trim().ifBlank { if (skip) defaultName else "" }
        if (finalName.isBlank()) { nameError = true; return }
        scope.launch {
            val profile = BusinessProfile(
                id = existing?.id ?: newId(),
                name = finalName,
                ownerName = owner.trim(),
                phone = phone.trim(),
                address = address.trim(),
                type = type,
                openingCashPaisa = Money.parseToPaisa(cash) ?: 0,
                updatedAt = maxOf(Dates.now(), (existing?.updatedAt ?: 0) + 1),
            )
            container.settings.saveBusiness(profile, dirty = true)
            container.settings.setOnboardingDone()
            if (container.settings.currentAuth().loggedIn) container.scheduler.requestSync()
            onDone()
        }
    }

    Scaffold(
        topBar = {
            if (editMode) AppTopBar(stringResource(R.string.business_profile), onBack = onDone)
        },
        bottomBar = {
            Column(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(16.dp)) {
                Button(onClick = { save(skip = false) }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Text(stringResource(if (editMode) R.string.save else R.string.setup_start))
                }
                if (!editMode) {
                    TextButton(onClick = { save(skip = true) }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.setup_skip))
                    }
                }
            }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            if (!editMode) {
                Spacer(Modifier.height(40.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(R.drawable.ic_brand_logo), null, Modifier.size(48.dp))
                    Spacer(Modifier.size(12.dp))
                    Column {
                        Text(stringResource(R.string.setup_title), style = MaterialTheme.typography.headlineSmall)
                        Text(
                            stringResource(R.string.setup_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
            } else {
                Spacer(Modifier.height(8.dp))
            }
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; nameError = false },
                label = { Text(stringResource(R.string.shop_name) + " *") },
                isError = nameError,
                supportingText = if (nameError) { { Text(stringResource(R.string.error_required)) } } else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = owner, onValueChange = { owner = it },
                label = { Text(stringResource(R.string.owner_name)) }, singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = phone, onValueChange = { phone = it.filter { c -> c.isDigit() || c == '+' } },
                label = { Text(stringResource(R.string.mobile_number)) }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.business_type), style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BUSINESS_TYPES.forEach { key ->
                    FilterChip(
                        selected = type == key,
                        onClick = { type = key },
                        label = { Text(stringResource(businessTypeRes(key))) },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            AmountField(
                value = cash, onValueChange = { cash = it },
                label = stringResource(R.string.opening_cash),
                supportingText = stringResource(R.string.opening_cash_help),
            )
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = address, onValueChange = { address = it },
                label = { Text(stringResource(R.string.address)) },
                modifier = Modifier.fillMaxWidth(), minLines = 2,
            )
            if (!editMode) {
                Spacer(Modifier.height(16.dp))
                Text(
                    stringResource(R.string.setup_privacy_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row {
                    TextButton(onClick = { legalPage = "terms" }) { Text(stringResource(R.string.terms)) }
                    TextButton(onClick = { legalPage = "privacy" }) { Text(stringResource(R.string.privacy_policy)) }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    legalPage?.let { page ->
        Dialog(onDismissRequest = { legalPage = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            LegalContent(page = page, onClose = { legalPage = null })
        }
    }
}
