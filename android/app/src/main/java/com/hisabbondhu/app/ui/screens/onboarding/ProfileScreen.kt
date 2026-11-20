package com.hisabbondhu.app.ui.screens.onboarding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.hisabbondhu.app.R
import com.hisabbondhu.app.core.normalizeBdPhone
import com.hisabbondhu.app.data.remote.apiErrorCode
import com.hisabbondhu.app.ui.appContainer
import com.hisabbondhu.app.ui.components.AppTopBar
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

/** Required after the first login (and editable from Settings): name + mobile number. */
@Composable
fun ProfileScreen(editMode: Boolean = false, onDone: () -> Unit = {}) {
    val container = appContainer()
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var optIn by rememberSaveable { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var loaded by rememberSaveable { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val a = container.settings.auth.first()
        email = a.email.orEmpty()
        if (!loaded) {
            name = a.name.orEmpty()
            phone = a.phone?.removePrefix("+88").orEmpty()
            optIn = a.marketingOptIn
            loaded = true
        }
    }

    fun save() {
        if (name.isBlank()) { error = "name_required"; return }
        if (normalizeBdPhone(phone) == null) { error = "invalid_phone"; return }
        saving = true; error = null
        scope.launch {
            try {
                container.auth.updateProfile(name, normalizeBdPhone(phone)!!, optIn)
                onDone()
            } catch (e: Exception) {
                error = when (e) {
                    is HttpException -> e.apiErrorCode() ?: "server"
                    is IOException -> "network"
                    else -> "server"
                }
            } finally {
                saving = false
            }
        }
    }

    Scaffold(
        topBar = { if (editMode) AppTopBar(stringResource(R.string.your_profile), onBack = onDone) },
        bottomBar = {
            Button(
                onClick = { save() },
                enabled = !saving,
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(16.dp).height(52.dp),
            ) {
                if (saving) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                else Text(stringResource(if (editMode) R.string.save else R.string.continue_label))
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            if (!editMode) {
                Spacer(Modifier.height(40.dp))
                Text(stringResource(R.string.profile_title), style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.profile_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(16.dp))
            if (email.isNotBlank()) {
                OutlinedTextField(
                    value = email, onValueChange = {}, enabled = false,
                    label = { Text(stringResource(R.string.email_address)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
            }
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; error = null },
                label = { Text(stringResource(R.string.your_name) + " *") },
                leadingIcon = { Icon(Icons.Rounded.Person, null) },
                singleLine = true,
                isError = error == "name_required",
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth().testTag("profile_name"),
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it.filter { c -> c.isDigit() || c in '০'..'৯' }.take(13); error = null },
                label = { Text(stringResource(R.string.mobile_number) + " *") },
                placeholder = { Text("01XXXXXXXXX") },
                prefix = { Text("+88 ") },
                leadingIcon = { Icon(Icons.Rounded.Phone, null) },
                singleLine = true,
                isError = error == "invalid_phone",
                supportingText = { Text(stringResource(R.string.phone_not_verified_note)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth().testTag("profile_phone"),
            )
            Spacer(Modifier.height(6.dp))
            Row(
                Modifier.fillMaxWidth().clickable { optIn = !optIn }.padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = optIn, onCheckedChange = { optIn = it })
                Text(stringResource(R.string.marketing_opt_in), style = MaterialTheme.typography.bodyMedium)
            }
            error?.let {
                Spacer(Modifier.height(6.dp))
                Text(
                    if (it == "name_required") stringResource(R.string.error_required) else authErrorText(it),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (!editMode) {
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = { scope.launch { container.auth.logout() } }) { Text(stringResource(R.string.use_other_email)) }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
