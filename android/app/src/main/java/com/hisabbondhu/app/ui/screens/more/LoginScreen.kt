package com.hisabbondhu.app.ui.screens.more

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.Button
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.hisabbondhu.app.AppContainer
import com.hisabbondhu.app.R
import com.hisabbondhu.app.core.Money
import com.hisabbondhu.app.core.normalizeBdPhone
import com.hisabbondhu.app.data.remote.apiErrorCode
import com.hisabbondhu.app.ui.appViewModel
import com.hisabbondhu.app.ui.components.AppTopBar
import com.hisabbondhu.app.ui.num
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class LoginViewModel(private val c: AppContainer) : ViewModel() {
    var phone by mutableStateOf("")
    var code by mutableStateOf("")
    var otpSent by mutableStateOf(false)
    var loading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var resendIn by mutableIntStateOf(0)
    var devCode by mutableStateOf<String?>(null)
    private var normalized: String? = null

    fun requestOtp() {
        val p = normalizeBdPhone(phone)
        if (p == null) { error = "invalid_phone"; return }
        normalized = p
        loading = true; error = null
        viewModelScope.launch {
            try {
                val res = c.auth.requestOtp(p)
                devCode = res.devCode
                otpSent = true
                startCountdown(res.resendAfter)
            } catch (e: Exception) {
                error = mapError(e)
            } finally {
                loading = false
            }
        }
    }

    fun verify(onSuccess: () -> Unit) {
        val p = normalized ?: return
        val digits = Money.toAsciiDigits(code).filter { it.isDigit() }
        if (digits.length != 6) { error = "invalid_code"; return }
        loading = true; error = null
        viewModelScope.launch {
            try {
                c.auth.verifyOtp(p, digits)
                onSuccess()
            } catch (e: Exception) {
                error = mapError(e)
            } finally {
                loading = false
            }
        }
    }

    private fun startCountdown(seconds: Int) {
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

@Composable
fun LoginScreen(nav: NavHostController) {
    val vm = appViewModel { c, _ -> LoginViewModel(c) }
    val context = LocalContext.current

    LaunchedEffect(vm.devCode) { vm.devCode?.let { vm.code = it } }

    Scaffold(topBar = { AppTopBar(stringResource(R.string.login), onBack = { nav.popBackStack() }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp)) {
            Icon(
                if (vm.otpSent) Icons.Rounded.Sms else Icons.Rounded.Lock,
                null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(if (vm.otpSent) R.string.enter_otp_title else R.string.login_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (vm.otpSent) stringResource(R.string.otp_sent_to, vm.phone) else stringResource(R.string.login_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))
            if (!vm.otpSent) {
                OutlinedTextField(
                    value = vm.phone,
                    onValueChange = { vm.phone = it.filter { c -> c.isDigit() || c == '+' || c in '০'..'৯' }.take(14); vm.error = null },
                    label = { Text(stringResource(R.string.mobile_number)) },
                    placeholder = { Text("01XXXXXXXXX") },
                    prefix = { Text("+88 ") },
                    singleLine = true,
                    isError = vm.error != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                OutlinedTextField(
                    value = vm.code,
                    onValueChange = { vm.code = it.filter { c -> c.isDigit() || c in '০'..'৯' }.take(6); vm.error = null },
                    label = { Text(stringResource(R.string.otp_code)) },
                    singleLine = true,
                    isError = vm.error != null,
                    textStyle = MaterialTheme.typography.headlineSmall.copy(letterSpacing = 8.sp, textAlign = TextAlign.Center),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            vm.error?.let {
                Spacer(Modifier.height(6.dp))
                Text(loginErrorText(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    if (!vm.otpSent) {
                        vm.requestOtp()
                    } else {
                        vm.verify {
                            Toast.makeText(context, R.string.login_success, Toast.LENGTH_LONG).show()
                            nav.popBackStack()
                        }
                    }
                },
                enabled = !vm.loading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                if (vm.loading) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(if (vm.otpSent) R.string.verify else R.string.send_otp))
                }
            }
            if (vm.otpSent) {
                TextButton(onClick = { vm.requestOtp() }, enabled = vm.resendIn == 0 && !vm.loading, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        if (vm.resendIn > 0) stringResource(R.string.resend_in, num(vm.resendIn))
                        else stringResource(R.string.resend_otp),
                    )
                }
                TextButton(onClick = { vm.otpSent = false; vm.code = "" }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.change_number))
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.login_privacy_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun loginErrorText(code: String): String = stringResource(
    when (code) {
        "invalid_phone" -> R.string.err_invalid_phone
        "invalid_code" -> R.string.err_invalid_code
        "code_expired" -> R.string.err_code_expired
        "too_many_attempts" -> R.string.err_too_many_attempts
        "rate_limited" -> R.string.err_rate_limited
        "network" -> R.string.err_network
        else -> R.string.err_server
    },
)
