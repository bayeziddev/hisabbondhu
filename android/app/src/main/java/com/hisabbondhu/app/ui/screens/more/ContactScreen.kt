package com.hisabbondhu.app.ui.screens.more

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.hisabbondhu.app.BuildConfig
import com.hisabbondhu.app.R
import com.hisabbondhu.app.data.remote.dto.ContactRequest
import com.hisabbondhu.app.ui.appContainer
import com.hisabbondhu.app.ui.components.AppTopBar
import com.hisabbondhu.app.ui.components.NavRow
import com.hisabbondhu.app.ui.components.RowDivider
import com.hisabbondhu.app.ui.components.SectionCard
import com.hisabbondhu.app.util.Share
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ContactScreen(nav: NavHostController) {
    val container = appContainer()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val topics = listOf(R.string.topic_problem, R.string.topic_suggestion, R.string.topic_account, R.string.topic_other)
    var topic by rememberSaveable { mutableStateOf(R.string.topic_problem) }
    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    val emailSubject = stringResource(R.string.app_name) + " — " + stringResource(topic)

    Scaffold(topBar = { AppTopBar(stringResource(R.string.contact_us), onBack = { nav.popBackStack() }) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.contact_intro), style = MaterialTheme.typography.bodyMedium)
            SectionCard(contentPadding = PaddingValues(vertical = 8.dp)) {
                NavRow(Icons.Rounded.Email, stringResource(R.string.email_us), BuildConfig.SUPPORT_EMAIL) {
                    Share.email(context, BuildConfig.SUPPORT_EMAIL, emailSubject)
                }
                RowDivider()
                NavRow(Icons.Rounded.Public, stringResource(R.string.website), BuildConfig.WEBSITE_URL) {
                    Share.openUrl(context, BuildConfig.WEBSITE_URL + "contact.html")
                }
            }
            SectionCard(title = stringResource(R.string.send_us_message)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    topics.forEach { t -> FilterChip(topic == t, { topic = t }, { Text(stringResource(t)) }) }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.your_name)) }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    phone, { phone = it }, Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.mobile_or_email)) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    message, { message = it }, Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.message)) }, minLines = 4,
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    enabled = !sending && message.trim().length >= 5 && name.isNotBlank(),
                    onClick = {
                        sending = true
                        scope.launch {
                            val subject = context.getString(topic)
                            val ok = runCatching {
                                container.network.api.contact(
                                    ContactRequest(
                                        name = name.trim(),
                                        phone = phone.trim().takeIf { !it.contains("@") && it.isNotBlank() },
                                        email = phone.trim().takeIf { it.contains("@") },
                                        subject = subject,
                                        message = message.trim(),
                                    ),
                                )
                            }.isSuccess
                            sending = false
                            if (ok) {
                                Toast.makeText(context, R.string.message_sent, Toast.LENGTH_LONG).show()
                                message = ""
                            } else {
                                Toast.makeText(context, R.string.message_failed_email, Toast.LENGTH_LONG).show()
                                Share.email(context, BuildConfig.SUPPORT_EMAIL, emailSubject, "${message.trim()}\n\n— $name $phone")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (sending) CircularProgressIndicator(strokeWidth = 2.dp) else Text(stringResource(R.string.send))
                }
            }
            Text(
                stringResource(R.string.contact_response_time),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
