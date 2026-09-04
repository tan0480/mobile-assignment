package com.example.gadgetmover.screen.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.example.gadgetmover.data.AuthRepository
import com.example.gadgetmover.data.BiometricPreferences
import com.example.gadgetmover.util.BiometricAuthResult
import com.example.gadgetmover.util.BiometricAvailability
import com.example.gadgetmover.util.authenticateWithBiometrics
import com.example.gadgetmover.util.biometricAvailability
import kotlinx.coroutines.launch

/**
 * Confirms it's really the account holder before a sensitive action (wallet checkout payment,
 * withdrawals, saving profile changes) that doesn't already go through its own re-auth step.
 * Tries a fingerprint check first when the user has turned that on in Settings and the device
 * supports it — the password dialog isn't shown at all while that's in progress, only appearing
 * once it's actually needed (biometric unavailable, or the user cancelled/failed it and needs the
 * fallback). [onConfirmed] only fires once one of the two has actually succeeded — biometric
 * success never bypasses [AuthRepository.verifyPassword] silently, it's just a faster path to the
 * same gate. The system biometric prompt itself uses the caller's own [title] rather than a fixed
 * "payment"-flavored one, since this dialog is also used for non-payment confirmations.
 */
@Composable
fun PasswordConfirmDialog(
    message: String,
    onDismiss: () -> Unit,
    onConfirmed: () -> Unit,
    title: String = "Confirm your password",
    /** False when a caller already handles biometrics itself (e.g. the app-unlock screen tries it first and only falls back to this dialog for the password field) and doesn't want a second, redundant prompt. */
    allowBiometric: Boolean = true
) {
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val biometricAvailable = allowBiometric &&
        activity != null &&
        BiometricPreferences.isPaymentsEnabled(context) &&
        biometricAvailability(activity) == BiometricAvailability.AVAILABLE

    // Starts hidden when biometric is the first thing to try — the password field only appears
    // once it's actually needed (unavailable to begin with, or the user cancelled/failed the
    // fingerprint check), never composed underneath the system biometric prompt at the same time.
    var showPasswordDialog by remember { mutableStateOf(!biometricAvailable) }

    fun tryBiometric() {
        if (activity == null) return
        busy = true
        scope.launch {
            when (val result = authenticateWithBiometrics(activity, title, message, negativeButtonText = "Use password")) {
                is BiometricAuthResult.Success -> onConfirmed()
                is BiometricAuthResult.Failed -> { error = result.message; showPasswordDialog = true }
                is BiometricAuthResult.Cancelled -> showPasswordDialog = true
            }
            busy = false
        }
    }

    // Offers the fingerprint prompt right away rather than making the user reach for the button —
    // "Use password" in the prompt's negative button, or just backing out of it, reveals the
    // password dialog below instead of it being visible the whole time.
    LaunchedEffect(biometricAvailable) {
        if (biometricAvailable) tryBiometric()
    }

    if (showPasswordDialog) {
        AlertDialog(
            onDismissRequest = { if (!busy) onDismiss() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, modifier = androidx.compose.ui.Modifier.weight(1f))
                    if (biometricAvailable) {
                        IconButton(onClick = ::tryBiometric, enabled = !busy) {
                            Icon(Icons.Filled.Fingerprint, contentDescription = "Use fingerprint")
                        }
                    }
                }
            },
            text = {
                Column {
                    Text(message, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = androidx.compose.ui.Modifier.height(12.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; error = null },
                        label = { Text("Password") },
                        singleLine = true,
                        enabled = !busy,
                        isError = error != null,
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        supportingText = error?.let { { Text(it, color = MaterialTheme.colorScheme.error) } }
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = !busy,
                    onClick = {
                        if (password.isBlank()) {
                            error = "Enter your password"
                            return@Button
                        }
                        busy = true
                        scope.launch {
                            val ok = AuthRepository.verifyPassword(password)
                            busy = false
                            if (ok) onConfirmed() else error = "Incorrect password"
                        }
                    }
                ) {
                    if (busy) CircularProgressIndicator(modifier = androidx.compose.ui.Modifier.height(18.dp)) else Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancel") }
            }
        )
    }
}
