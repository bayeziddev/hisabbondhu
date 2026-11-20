package com.hisabbondhu.app.ui.screens.onboarding

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.hisabbondhu.app.R
import com.hisabbondhu.app.ui.appContainer
import kotlinx.coroutines.launch

/** First screen: bilingual on purpose, because the app language is not chosen yet. */
@Composable
fun LanguageScreen() {
    val container = appContainer()
    val scope = rememberCoroutineScope()
    var selected by rememberSaveable { mutableStateOf("bn") }
    val bn = selected == "bn"

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))
            Image(painterResource(R.drawable.ic_brand_logo), contentDescription = null, modifier = Modifier.size(104.dp))
            Spacer(Modifier.height(16.dp))
            Text("হিসাববন্ধু", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
            Text("HisabBondhu", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Text(
                if (bn) "দোকানের হিসাব, সবসময় হাতের কাছে" else "Your shop's accounts, always at hand",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            Feature(Icons.Rounded.WifiOff, if (bn) "ইন্টারনেট ছাড়াই হিসাব লিখুন" else "Works fully without internet")
            Feature(Icons.Rounded.CloudSync, if (bn) "নেট এলে নিজে থেকেই ব্যাকআপ" else "Auto backup when you're online")
            Feature(Icons.Rounded.Lock, if (bn) "আপনার হিসাব শুধু আপনার" else "Your data stays yours")
            Spacer(Modifier.height(24.dp))
            Text(
                "ভাষা নির্বাচন করুন • Choose language",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            LanguageOption("বাংলা", "Bangla", selected == "bn") { selected = "bn" }
            Spacer(Modifier.height(10.dp))
            Box(Modifier.testTag("lang_en")) { LanguageOption("English", "ইংরেজি", selected == "en") { selected = "en" } }
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = {
                    scope.launch {
                        container.settings.setLanguageChosen()
                        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(selected))
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp).testTag("lang_start"),
            ) {
                Text(if (bn) "শুরু করুন" else "Get started", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun Feature(icon: ImageVector, text: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.size(12.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun LanguageOption(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = selected, onClick = onClick)
            Spacer(Modifier.size(8.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (selected) Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}
