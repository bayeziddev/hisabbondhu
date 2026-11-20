package com.hisabbondhu.app.ui.screens.onboarding

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.MarkEmailRead
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.hisabbondhu.app.AppContainer
import com.hisabbondhu.app.R
import com.hisabbondhu.app.core.Money
import com.hisabbondhu.app.data.remote.apiErrorCode
import com.hisabbondhu.app.ui.appViewModel
import com.hisabbondhu.app.ui.num
import com.hisabbondhu.app.ui.screens.more.LegalContent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

private val EMAIL_RE = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$")

class AuthViewModel(private val c: AppContainer) : ViewModel() {
    var email by mutableStateOf("")
    var code by mutableStateOf("")
    var codeSent by mutableStateOf(false)
    var loading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var resendIn by mutableIntStateOf(0)
    var devCode by mutableStateOf<String?>(null)
    val online = c.connectivity.online

    fun sendCode() {
        val e = email.trim().lowercase()
        if (!EMAIL_RE.matches(e)) { error = "invalid_email"; return }
        loading = true; error = null
        viewModelScope.launch {
            try {
                val res = c.auth.requestOtp(e)
                devCode = res.devCode
                codeSent = true
                code = ""
                countdown(res.resendAfter)
            } catch (t: Exception) {
                error = mapError(t)
            } finally {
                loading = false
            }
        }
    }

    fun verify() {
        val digits = Money.toAsciiDigits(code).filter { it.isDigit() }
        if (digits.length != 6) { error = "invalid_code"; return }
        loading = true; error = null
        viewModelScope.launch {
            try {
                c.auth.verifyOtp(email, digits) // root screen switches automatically when auth is saved
            } catch (t: Exception) {
                error = mapError(t)
            } finally {
                loading = false
            }
        }
    }

    private fun countdown(seconds: Int) {
        viewModelScope.launch {
            resendIn = seconds
            while (resendIn > 0) { delay(1000); resendIn-- }
        }
    }

    private fun mapError(e: Exception): String = when (e) {
        is HttpException -> e.apiErrorCode() ?: "server"
        is IOException -> "network"
        else -> "server"
    }
}

/** Sign in / create account with email + 6-digit code. Required once; the app then works offline. */
@Composable
fun AuthScreen() {
    val vm = appViewModel { c, _ -> AuthViewModel(c) }
    val online by vm.online.collectAsStateWithLifecycle(initialValue = true)
    var legalPage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(vm.devCode) { vm.devCode?.let { vm.code = it } }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        ) {
            Spacer(Modifier.height(16.dp))
            Image(painterResource(R.drawable.ic_brand_logo), null, Modifier.size(64.dp))
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(if (vm.codeSent) R.string.enter_otp_title else R.string.auth_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (vm.codeSent) stringResource(R.string.otp_sent_to_email, vm.email.trim().lowercase())
                else stringResource(R.string.auth_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!online) {
                Spacer(Modifier.height(12.dp))
                Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = MaterialTheme.shapes.medium) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.WifiOff, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(stringResource(R.string.auth_needs_internet_once), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            if (!vm.codeSent) {
                OutlinedTextField(
                    value = vm.email,
                    onValueChange = { vm.email = it.trim(); vm.error = null },
                    label = { Text(stringResource(R.string.email_address)) },
                    placeholder = { Text("name@gmail.com") },
                    leadingIcon = { Icon(Icons.Rounded.Email, null) },
                    singleLine = true,
                    isError = vm.error != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                OutlinedTextField(
                    value = vm.code,
                    onValueChange = { vm.code = it.filter { c -> c.isDigit() || c in '০'..'৯' }.take(6); vm.error = null },
                    label = { Text(stringResource(R.string.otp_code)) },
                    leadingIcon = { Icon(Icons.Rounded.MarkEmailRead, null) },
                    singleLine = true,
                    isError = vm.error != null,
                    textStyle = MaterialTheme.typography.headlineSmall.copy(letterSpacing = 8.sp, textAlign = TextAlign.Center),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    stringResource(R.string.check_spam_folder),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            vm.error?.let {
                Spacer(Modifier.height(6.dp))
                Text(authErrorText(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { if (vm.codeSent) vm.verify() else vm.sendCode() },
                enabled = !vm.loading,
                modifier = Modifier.fillMaxWidth().height(54.dp),
            ) {
                if (vm.loading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                else Text(stringResource(if (vm.codeSent) R.string.verify else R.string.send_otp), style = MaterialTheme.typography.titleMedium)
            }
            if (vm.codeSent) {
                TextButton(onClick = { vm.sendCode() }, enabled = vm.resendIn == 0 && !vm.loading, modifier = Modifier.fillMaxWidth()) {
                    Text(if (vm.resendIn > 0) stringResource(R.string.resend_in, num(vm.resendIn)) else stringResource(R.string.resend_otp))
                }
                TextButton(onClick = { vm.codeSent = false; vm.code = "" }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.change_email))
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.auth_consent),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row {
                TextButton(onClick = { legalPage = "terms" }) { Text(stringResource(R.string.terms)) }
                TextButton(onClick = { legalPage = "privacy" }) { Text(stringResource(R.string.privacy_policy)) }
            }
        }
    }

    legalPage?.let { page ->
        Dialog(onDismissRequest = { legalPage = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            LegalContent(page = page, onClose = { legalPage = null })
        }
    }
}

@Composable
fun authErrorText(code: String): String = stringResource(
    when (code) {
        "invalid_email" -> R.string.err_invalid_email
        "invalid_phone" -> R.string.err_invalid_phone
        "invalid_code" -> R.string.err_invalid_code
        "code_expired" -> R.string.err_code_expired
        "too_many_attempts" -> R.string.err_too_many_attempts
        "rate_limited" -> R.string.err_rate_limited
        "email_failed" -> R.string.err_email_failed
        "network" -> R.string.err_network
        else -> R.string.err_server
    },
)
