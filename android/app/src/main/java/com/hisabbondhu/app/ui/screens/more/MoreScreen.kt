package com.hisabbondhu.app.ui.screens.more

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.hisabbondhu.app.BuildConfig
import com.hisabbondhu.app.R
import com.hisabbondhu.app.sync.Notifications
import com.hisabbondhu.app.ui.LocalBengali
import com.hisabbondhu.app.ui.appContainer
import com.hisabbondhu.app.ui.businessTypeRes
import com.hisabbondhu.app.ui.components.AppTopBar
import com.hisabbondhu.app.ui.components.NameAvatar
import com.hisabbondhu.app.ui.components.NavRow
import com.hisabbondhu.app.ui.components.RowDivider
import com.hisabbondhu.app.ui.components.SectionCard
import com.hisabbondhu.app.ui.nav.Routes
import com.hisabbondhu.app.util.Share
import kotlinx.coroutines.launch

@Composable
fun MoreScreen(nav: NavHostController) {
    val container = appContainer()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val business by container.settings.business.collectAsStateWithLifecycle<com.hisabbondhu.app.data.prefs.BusinessProfile?>(initialValue = null)
    val auth by container.settings.auth.collectAsStateWithLifecycle<com.hisabbondhu.app.data.prefs.AuthState?>(initialValue = null)
    val reminders by container.settings.remindersEnabled.collectAsStateWithLifecycle(initialValue = true)
    var showLanguage by remember { mutableStateOf(false) }
    val bengali = LocalBengali.current
    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    Scaffold(topBar = { AppTopBar(stringResource(R.string.nav_more)) }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        NameAvatar(business?.name ?: "H", 52.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(business?.name.orEmpty(), style = MaterialTheme.typography.titleMedium)
                            val sub = listOfNotNull(
                                business?.ownerName?.takeIf { it.isNotBlank() },
                                business?.type?.let { stringResource(businessTypeRes(it)) },
                            ).joinToString(" • ")
                            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { nav.navigate(Routes.BUSINESS) }) { Icon(Icons.Rounded.Edit, stringResource(R.string.edit)) }
                    }
                }
            }
            item {
                SectionCard(title = stringResource(R.string.section_backup), contentPadding = PaddingValues(vertical = 8.dp)) {
                    NavRow(
                        Icons.Rounded.CloudSync,
                        stringResource(R.string.online_backup_sync),
                        if (auth?.loggedIn == true) stringResource(R.string.logged_in_as, auth?.phone.orEmpty())
                        else stringResource(R.string.not_logged_in_desc),
                    ) { nav.navigate(Routes.ACCOUNT) }
                }
            }
            item {
                SectionCard(title = stringResource(R.string.section_settings), contentPadding = PaddingValues(vertical = 8.dp)) {
                    NavRow(Icons.Rounded.Language, stringResource(R.string.language), if (bengali) "বাংলা" else "English") { showLanguage = true }
                    RowDivider()
                    NavRow(
                        Icons.Rounded.Notifications,
                        stringResource(R.string.due_reminders),
                        stringResource(R.string.due_reminders_desc),
                        trailing = {
                            Switch(checked = reminders, onCheckedChange = { on ->
                                scope.launch { container.settings.setRemindersEnabled(on) }
                                if (on && Build.VERSION.SDK_INT >= 33 && !Notifications.canNotify(context)) {
                                    notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            })
                        },
                    ) { scope.launch { container.settings.setRemindersEnabled(!reminders) } }
                }
            }
            item {
                SectionCard(title = stringResource(R.string.section_help_legal), contentPadding = PaddingValues(vertical = 8.dp)) {
                    NavRow(Icons.Rounded.SupportAgent, stringResource(R.string.contact_us), stringResource(R.string.contact_us_desc)) { nav.navigate(Routes.CONTACT) }
                    RowDivider()
                    NavRow(Icons.Rounded.Policy, stringResource(R.string.privacy_policy)) { nav.navigate(Routes.legal("privacy")) }
                    RowDivider()
                    NavRow(Icons.Rounded.Gavel, stringResource(R.string.terms)) { nav.navigate(Routes.legal("terms")) }
                    RowDivider()
                    NavRow(Icons.Rounded.VerifiedUser, stringResource(R.string.your_rights)) { nav.navigate(Routes.legal("your-rights")) }
                    RowDivider()
                    NavRow(
                        Icons.Rounded.DeleteForever,
                        stringResource(R.string.delete_account_data),
                        stringResource(R.string.delete_account_data_desc),
                        tint = MaterialTheme.colorScheme.error,
                    ) { nav.navigate(Routes.DELETE) }
                }
            }
            item {
                SectionCard(title = stringResource(R.string.section_about), contentPadding = PaddingValues(vertical = 8.dp)) {
                    NavRow(Icons.Rounded.Star, stringResource(R.string.rate_app)) {
                        val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}"))
                        runCatching { context.startActivity(market) }.onFailure {
                            Share.openUrl(context, "https://play.google.com/store/apps/details?id=${context.packageName}")
                        }
                    }
                    RowDivider()
                    NavRow(Icons.Rounded.Share, stringResource(R.string.share_app)) {
                        Share.text(context, context.getString(R.string.share_app_text, "https://play.google.com/store/apps/details?id=${context.packageName}"))
                    }
                    RowDivider()
                    NavRow(Icons.Rounded.Info, stringResource(R.string.app_version), BuildConfig.VERSION_NAME, trailing = {}) {
                        nav.navigate(Routes.legal("licenses"))
                    }
                }
            }
            item {
                Text(
                    stringResource(R.string.made_in_bd),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                )
            }
        }
    }

    if (showLanguage) {
        AlertDialog(
            onDismissRequest = { showLanguage = false },
            title = { Text(stringResource(R.string.language)) },
            text = {
                Column {
                    listOf("bn" to "বাংলা", "en" to "English").forEach { (tag, label) ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            RadioButton(
                                selected = (tag == "bn") == bengali,
                                onClick = {
                                    showLanguage = false
                                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
                                },
                            )
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showLanguage = false }) { Text(stringResource(R.string.close)) } },
        )
    }
}
