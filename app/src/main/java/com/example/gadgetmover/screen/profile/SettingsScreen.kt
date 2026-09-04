package com.example.gadgetmover.screen.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.example.gadgetmover.data.BiometricPreferences
import com.example.gadgetmover.data.SettingsRepository
import com.example.gadgetmover.data.ThemePreferences
import com.example.gadgetmover.util.BiometricAuthResult
import com.example.gadgetmover.util.BiometricAvailability
import com.example.gadgetmover.util.authenticateWithBiometrics
import com.example.gadgetmover.util.biometricAvailability
import kotlinx.coroutines.launch

private enum class ThemeOption(val label: String, val forcedValue: Boolean?) {
    SYSTEM("System", null),
    LIGHT("Light", false),
    DARK("Dark", true)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onAccountInfoClick: () -> Unit,
    /** Gates an action behind creating a password first (see NavGraph's runOrRequirePassword) — a
     * Google sign-in account with no password yet can't meaningfully enable a fingerprint fallback
     * for it. Defaults to running the action straight away for callers that don't need the gate. */
    onRequirePassword: (() -> Unit) -> Unit = { it() }
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var biometricPaymentsEnabled by remember { mutableStateOf(BiometricPreferences.isPaymentsEnabled(context)) }

    // Turning a biometric toggle on requires a live successful check first — never flips the
    // preference on trust alone, since that would silently "enable" a gate that then can't
    // actually challenge anyone (no hardware, or no fingerprint enrolled on this device).
    fun requestEnableBiometric(onEnabled: () -> Unit) {
        if (activity == null) return
        when (biometricAvailability(activity)) {
            BiometricAvailability.AVAILABLE -> scope.launch {
                val result = authenticateWithBiometrics(activity, "Confirm to enable")
                if (result is BiometricAuthResult.Success) {
                    onEnabled()
                } else if (result is BiometricAuthResult.Failed) {
                    snackbarHostState.showSnackbar(result.message)
                }
            }
            BiometricAvailability.NOT_ENROLLED -> scope.launch {
                snackbarHostState.showSnackbar("Set up a fingerprint in your device settings first")
            }
            else -> scope.launch {
                snackbarHostState.showSnackbar("This device doesn't support fingerprint unlock")
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") // TODO: swap with custom ImageVector
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Account", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            SettingsNavigationRow(
                title = "Account Information",
                subtitle = "Name, username, phone, and password",
                onClick = onAccountInfoClick
            )

            Spacer(modifier = Modifier.height(24.dp))
            Text("Notifications", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            SettingsToggleRow(
                title = "Push notifications",
                subtitle = "Order updates, messages, and price alerts",
                checked = SettingsRepository.notificationsEnabled.value,
                onCheckedChange = { scope.launch { SettingsRepository.setNotificationsEnabled(it) } }
            )
            HorizontalDivider()
            SettingsToggleRow(
                title = "Marketing emails",
                subtitle = "Deals, promotions, and product news",
                checked = SettingsRepository.marketingEmailsEnabled.value,
                onCheckedChange = { scope.launch { SettingsRepository.setMarketingEmailsEnabled(it) } }
            )

            Spacer(modifier = Modifier.height(24.dp))
            Text("Appearance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Theme", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(
                "Choose how the app looks. \"System\" follows your device setting.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))

            val currentForced = SettingsRepository.forceDarkMode.value
            val selectedOption = ThemeOption.entries.first { it.forcedValue == currentForced }

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ThemeOption.entries.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = option == selectedOption,
                        onClick = {
                            SettingsRepository.forceDarkMode.value = option.forcedValue
                            ThemePreferences.save(context, option.forcedValue)
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = ThemeOption.entries.size)
                    ) {
                        Text(option.label)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Security", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            SettingsToggleRow(
                title = "Fingerprint",
                subtitle = "Use your fingerprint instead of typing your password whenever one is needed",
                checked = biometricPaymentsEnabled,
                onCheckedChange = { enable ->
                    if (enable) {
                        onRequirePassword {
                            requestEnableBiometric { biometricPaymentsEnabled = true; BiometricPreferences.setPaymentsEnabled(context, true) }
                        }
                    } else {
                        biometricPaymentsEnabled = false
                        BiometricPreferences.setPaymentsEnabled(context, false)
                    }
                }
            )
        }
    }
}

@Composable
private fun SettingsToggleRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsNavigationRow(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
