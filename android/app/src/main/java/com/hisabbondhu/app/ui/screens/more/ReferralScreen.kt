package com.hisabbondhu.app.ui.screens.more

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Redeem
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.hisabbondhu.app.R
import com.hisabbondhu.app.data.remote.apiErrorCode
import com.hisabbondhu.app.data.remote.dto.PointsEntryDto
import com.hisabbondhu.app.data.remote.dto.RedeemOptionDto
import com.hisabbondhu.app.data.remote.dto.ReferralSummary
import com.hisabbondhu.app.ui.appContainer
import com.hisabbondhu.app.ui.components.AppTopBar
import com.hisabbondhu.app.ui.components.ConfirmDialog
import com.hisabbondhu.app.ui.components.IconBadge
import com.hisabbondhu.app.ui.components.SectionCard
import com.hisabbondhu.app.ui.date
import com.hisabbondhu.app.ui.num
import com.hisabbondhu.app.ui.theme.Danger
import com.hisabbondhu.app.ui.theme.Success
import com.hisabbondhu.app.ui.theme.Warning
import com.hisabbondhu.app.util.Share
import com.hisabbondhu.app.util.cleanReferralCode
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

/** "বন্ধুকে আনুন, পয়েন্ট নিন": share code, Hisab points, redeem for Pro, enter a friend's code. */
@Composable
fun ReferralScreen(nav: NavHostController) {
    val container = appContainer()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var summary by remember { mutableStateOf<ReferralSummary?>(null) }
    var loading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var actionError by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var codeInput by rememberSaveable { mutableStateOf("") }
    var confirm by remember { mutableStateOf<RedeemOptionDto?>(null) }

    val copiedMsg = stringResource(R.string.referral_copied)
    val proOnMsg = stringResource(R.string.referral_redeem_done)
    val appliedMsg = stringResource(R.string.referral_applied_ok)

    fun codeOf(e: Exception): String = when (e) {
        is HttpException -> e.apiErrorCode() ?: "server"
        is IOException -> "network"
        else -> "server"
    }

    fun load() {
        scope.launch {
            loading = true
            loadError = null
            try {
                container.referral.applyRetryIfAny()
                summary = container.referral.summary()
            } catch (e: Exception) {
                loadError = codeOf(e)
            } finally {
                loading = false
            }
        }
    }

    fun applyCode() {
        busy = true
        actionError = null
        scope.launch {
            try {
                summary = container.referral.apply(codeInput)
                codeInput = ""
                snackbar.showSnackbar(appliedMsg)
            } catch (e: Exception) {
                actionError = codeOf(e)
            } finally {
                busy = false
            }
        }
    }

    fun redeem(option: RedeemOptionDto) {
        busy = true
        actionError = null
        scope.launch {
            try {
                summary = container.referral.redeem(option.id)
                snackbar.showSnackbar(proOnMsg)
            } catch (e: Exception) {
                actionError = codeOf(e)
            } finally {
                busy = false
            }
        }
    }

    LaunchedEffect(Unit) { load() }

    Scaffold(
        topBar = { AppTopBar(stringResource(R.string.referral_title), onBack = { nav.popBackStack() }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val s = summary
        when {
            s == null && loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            s == null -> Column(
                Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    if (loadError == "network") stringResource(R.string.referral_need_internet) else referralErrorText(loadError ?: "server"),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = { load() }) { Text(stringResource(R.string.referral_retry)) }
            }
            else -> Column(
                Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val shareText = stringResource(R.string.referral_share_text, s.code, s.shareUrl, num(s.rules.welcomePoints))

                // Code + share
                SectionCard {
                    Text(stringResource(R.string.referral_your_code), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        s.code,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 6.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 4.dp).testTag("referral_code"),
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                context.getSystemService(ClipboardManager::class.java)
                                    ?.setPrimaryClip(ClipData.newPlainText("referral", s.code))
                                scope.launch { snackbar.showSnackbar(copiedMsg) }
                            },
                            modifier = Modifier.weight(1f),
                        ) { ButtonLabel(Icons.Rounded.ContentCopy, stringResource(R.string.referral_copy)) }
                        Button(
                            onClick = { Share.whatsApp(context, null, shareText) },
                            modifier = Modifier.weight(1f),
                        ) { ButtonLabel(Icons.Rounded.Chat, "WhatsApp") }
                        OutlinedButton(
                            onClick = { Share.text(context, shareText) },
                            modifier = Modifier.weight(1f).testTag("referral_share"),
                        ) { ButtonLabel(Icons.Rounded.Share, stringResource(R.string.referral_share)) }
                    }
                }

                // Points + Pro status + friends
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Rounded.WorkspacePremium, Warning)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.referral_points), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(num(s.points), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.testTag("referral_points"))
                        }
                    }
                    if (s.proActive && s.proUntil != null) {
                        Spacer(Modifier.height(6.dp))
                        Text(stringResource(R.string.referral_pro_until, date(s.proUntil)), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Groups, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.referral_friends, num(s.stats.qualified), num(s.stats.pending)),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                // How it works
                SectionCard(title = stringResource(R.string.referral_how_title)) {
                    HowStep(1, stringResource(R.string.referral_how_1))
                    HowStep(2, stringResource(R.string.referral_how_2, num(s.rules.applyWindowDays), num(s.rules.welcomePoints)))
                    HowStep(3, stringResource(R.string.referral_how_3, num(s.rules.minTransactions), num(s.rules.referrerPoints)))
                }

                // Redeem
                SectionCard(title = stringResource(R.string.referral_redeem_title)) {
                    if (!s.redeemEnabled) {
                        Text(stringResource(R.string.referral_redeem_soon), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                    }
                    s.rules.redeemOptions.forEach { option ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Redeem, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(optionLabel(option), style = MaterialTheme.typography.titleSmall)
                                Text(stringResource(R.string.referral_redeem_cost, num(option.points)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Button(
                                onClick = { confirm = option },
                                enabled = s.redeemEnabled && s.points >= option.points && !busy,
                            ) { Text(stringResource(R.string.referral_redeem_button)) }
                        }
                    }
                }

                // Enter a friend's code (new accounts only)
                if (s.canApply) {
                    SectionCard(title = stringResource(R.string.referral_enter_code)) {
                        OutlinedTextField(
                            value = codeInput,
                            onValueChange = { codeInput = cleanReferralCode(it); actionError = null },
                            label = { Text(stringResource(R.string.referral_code_label)) },
                            leadingIcon = { Icon(Icons.Rounded.CardGiftcard, null) },
                            singleLine = true,
                            isError = actionError != null,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                            modifier = Modifier.fillMaxWidth().testTag("referral_input"),
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { applyCode() },
                            enabled = codeInput.length == 6 && !busy,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(stringResource(R.string.referral_apply)) }
                    }
                } else if (s.appliedCode != null) {
                    Text(
                        stringResource(R.string.referral_applied, s.appliedCode),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }

                actionError?.let {
                    Text(referralErrorText(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }

                // History
                if (s.history.isNotEmpty()) {
                    SectionCard(title = stringResource(R.string.referral_history)) {
                        s.history.forEachIndexed { i, entry ->
                            if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            HistoryRow(entry)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    confirm?.let { option ->
        ConfirmDialog(
            title = stringResource(R.string.referral_redeem_title),
            message = stringResource(R.string.referral_redeem_confirm, num(option.points), optionLabel(option)),
            confirmLabel = stringResource(R.string.referral_redeem_button),
            onConfirm = { confirm = null; redeem(option) },
            onDismiss = { confirm = null },
        )
    }
}

@Composable
private fun ButtonLabel(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Icon(icon, null, modifier = Modifier.size(18.dp))
    Spacer(Modifier.width(6.dp))
    Text(label, maxLines = 1)
}

@Composable
private fun HowStep(n: Int, text: String) {
    Row(Modifier.padding(vertical = 4.dp)) {
        Text("${num(n)}.", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(24.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun HistoryRow(entry: PointsEntryDto) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(
                    when (entry.reason) {
                        "referral_referrer" -> R.string.referral_reason_referrer
                        "referral_welcome" -> R.string.referral_reason_welcome
                        "redeem_pro" -> R.string.referral_reason_redeem
                        else -> R.string.referral_reason_adjust
                    },
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(date(entry.createdAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            (if (entry.delta > 0) "+" else "−") + num(kotlin.math.abs(entry.delta)),
            color = if (entry.delta > 0) Success else Danger,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun optionLabel(option: RedeemOptionDto): String =
    if (option.days == 365) stringResource(R.string.referral_redeem_year)
    else stringResource(R.string.referral_redeem_days, num(option.days))

/** Message for a referral error code from the server (or "network" / "server"). */
@Composable
fun referralErrorText(code: String): String = stringResource(
    when (code) {
        "invalid_code", "invalid_input" -> R.string.referral_err_invalid
        "code_not_found" -> R.string.referral_err_not_found
        "own_code" -> R.string.referral_err_own
        "already_applied" -> R.string.referral_err_already
        "apply_window_closed" -> R.string.referral_err_window
        "mutual_referral" -> R.string.referral_err_mutual
        "not_enough_points" -> R.string.referral_err_points
        "redeem_disabled" -> R.string.referral_redeem_soon
        "network" -> R.string.err_network
        else -> R.string.err_server
    },
)
