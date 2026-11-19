package com.hisabbondhu.app.ui.screens.more

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Circle
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.hisabbondhu.app.BuildConfig
import com.hisabbondhu.app.R
import com.hisabbondhu.app.data.prefs.AuthState
import com.hisabbondhu.app.ui.appContainer
import com.hisabbondhu.app.ui.components.AppTopBar
import com.hisabbondhu.app.ui.components.ConfirmDialog
import com.hisabbondhu.app.ui.components.SectionCard
import com.hisabbondhu.app.util.Exporter
import com.hisabbondhu.app.util.Share
import kotlinx.coroutines.launch

/** Google Play requirement: users can delete their account and data from inside the app. */
@Composable
fun DeleteDataScreen(nav: NavHostController) {
    val container = appContainer()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth by container.settings.auth.collectAsStateWithLifecycle(initialValue = AuthState())
    var understood by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf(false) }
    var working by remember { mutableStateOf(false) }

    Scaffold(topBar = { AppTopBar(stringResource(R.string.delete_account_data), onBack = { nav.popBackStack() }) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Rounded.DeleteForever, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
            Text(stringResource(R.string.delete_title), style = MaterialTheme.typography.headlineSmall)
            SectionCard(title = stringResource(R.string.what_gets_deleted)) {
                Bullet(stringResource(R.string.deleted_item_phone))
                if (auth.loggedIn) {
                    Bullet(stringResource(R.string.deleted_item_cloud))
                    Bullet(stringResource(R.string.deleted_item_account))
                }
                Bullet(stringResource(R.string.deleted_item_backups))
            }
            Text(
                stringResource(if (auth.loggedIn) R.string.delete_cloud_note else R.string.delete_local_note),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(onClick = {
                scope.launch {
                    val file = Exporter.fullBackupJson(context, container)
                    Share.file(context, file, "application/json", context.getString(R.string.export_backup))
                }
            }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.Backup, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.download_backup_first))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = understood, onCheckedChange = { understood = it })
                Text(stringResource(R.string.delete_understood), style = MaterialTheme.typography.bodyMedium)
            }
            Button(
                enabled = understood && !working,
                onClick = { confirm = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                if (working) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                else Text(stringResource(if (auth.loggedIn) R.string.delete_everything else R.string.delete_phone_data))
            }
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.delete_web_option), style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { Share.openUrl(context, BuildConfig.WEBSITE_URL + "data-deletion.html") }) {
                Text(BuildConfig.WEBSITE_URL + "data-deletion.html")
            }
        }
    }

    if (confirm) {
        ConfirmDialog(
            title = stringResource(R.string.delete_confirm_title),
            message = stringResource(R.string.delete_confirm_message),
            confirmLabel = stringResource(R.string.delete),
            destructive = true,
            onConfirm = {
                confirm = false
                working = true
                scope.launch {
                    val result = runCatching { container.auth.deleteAccountAndData() }
                    working = false
                    if (result.isSuccess) {
                        Toast.makeText(context, R.string.delete_done, Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, R.string.delete_failed_online, Toast.LENGTH_LONG).show()
                    }
                }
            },
            onDismiss = { confirm = false },
        )
    }
}

@Composable
private fun Bullet(text: String) {
    Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
        Icon(Icons.Rounded.Circle, null, Modifier.padding(top = 7.dp).size(6.dp), tint = MaterialTheme.colorScheme.error)
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}
