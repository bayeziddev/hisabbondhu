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
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.hisabbondhu.app.AppContainer
import com.hisabbondhu.app.R
import com.hisabbondhu.app.data.prefs.AuthState
import com.hisabbondhu.app.data.prefs.SyncInfo
import com.hisabbondhu.app.ui.appViewModel
import com.hisabbondhu.app.ui.components.AppTopBar
import com.hisabbondhu.app.ui.components.ConfirmDialog
import com.hisabbondhu.app.ui.components.LabelValue
import com.hisabbondhu.app.ui.components.SectionCard
import com.hisabbondhu.app.ui.components.SyncChip
import com.hisabbondhu.app.ui.components.SyncUiState
import com.hisabbondhu.app.ui.dateTime
import com.hisabbondhu.app.ui.nav.Routes
import com.hisabbondhu.app.ui.num
import com.hisabbondhu.app.ui.screens.home.syncUiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AccountUi(
    val auth: AuthState = AuthState(),
    val info: SyncInfo = SyncInfo(),
    val pending: Int = 0,
    val state: SyncUiState = SyncUiState.OFFLINE_ONLY,
)

class AccountViewModel(val c: AppContainer) : ViewModel() {
    val ui: StateFlow<AccountUi> = combine(
        c.settings.auth, c.settings.syncInfo, c.ledger.pending(), c.scheduler.running, c.connectivity.online,
    ) { auth, info, pending, running, online ->
        AccountUi(auth, info, pending.total, syncUiState(auth.loggedIn, online, running, pending.total, info.lastError))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccountUi())

    fun syncNow() = c.scheduler.requestSync(delaySeconds = 0)

    fun logout(done: () -> Unit) = viewModelScope.launch { c.auth.logout(); done() }
}

@Composable
fun AccountScreen(nav: NavHostController) {
    val vm = appViewModel { c, _ -> AccountViewModel(c) }
    val ui by vm.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var confirmLogout by remember { mutableStateOf(false) }

    Scaffold(topBar = { AppTopBar(stringResource(R.string.online_backup_sync), onBack = { nav.popBackStack() }) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            run {
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.AccountCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(44.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(ui.auth.name.orEmpty(), style = MaterialTheme.typography.titleMedium)
                            Text(ui.auth.email.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(ui.auth.phone.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        OutlinedButton(onClick = { nav.navigate(Routes.PROFILE) }) { Text(stringResource(R.string.edit)) }
                    }
                }
            }
            if (ui.auth.loggedIn) {
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.status), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        SyncChip(ui.state, ui.pending) { vm.syncNow() }
                    }
                    Spacer(Modifier.height(8.dp))
                    LabelValue(
                        stringResource(R.string.last_sync),
                        ui.info.lastSyncAt?.let { dateTime(it) } ?: stringResource(R.string.never),
                    )
                    LabelValue(stringResource(R.string.pending_entries), num(ui.pending))
                    ui.info.lastError?.let { LabelValue(stringResource(R.string.last_error), syncErrorText(it), MaterialTheme.colorScheme.error) }
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = {
                        vm.syncNow()
                        Toast.makeText(context, R.string.sync_started, Toast.LENGTH_SHORT).show()
                    }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Rounded.Sync, null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.sync_now))
                    }
                }
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.sync_explainer), style = MaterialTheme.typography.bodySmall)
                    }
                }
                OutlinedButton(onClick = { confirmLogout = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.Logout, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.logout))
                }
            }
        }
    }

    if (confirmLogout) {
        ConfirmDialog(
            title = stringResource(R.string.logout),
            message = if (ui.pending > 0) {
                stringResource(R.string.logout_pending_warning, num(ui.pending))
            } else {
                stringResource(R.string.logout_message)
            },
            confirmLabel = stringResource(R.string.logout),
            destructive = ui.pending > 0,
            onConfirm = { confirmLogout = false; vm.logout { } },
            onDismiss = { confirmLogout = false },
        )
    }
}

@Composable
fun syncErrorText(code: String): String = stringResource(
    when (code) {
        "network" -> R.string.err_network
        "session_expired", "http_401" -> R.string.err_session_expired
        "rate_limited", "http_429" -> R.string.err_rate_limited
        else -> R.string.err_server
    },
)
