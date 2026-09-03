package com.example.gadgetmover.screen.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.example.gadgetmover.data.AuthRepository
import com.example.gadgetmover.screen.components.PasswordConfirmDialog
import com.example.gadgetmover.util.BiometricAuthResult
import com.example.gadgetmover.util.authenticateWithBiometrics
import kotlinx.coroutines.launch

/**
 * Shown in front of the whole app on a cold start when the seller has an already-persisted
 * session (so [com.example.gadgetmover.data.AuthRepository.restoreSession] would otherwise drop
 * them straight into Home) and turned on "Log in with fingerprint" in Settings. Nothing behind
 * this screen renders until [onUnlocked] fires, so it's a real gate, not just a suggestion.
 */
@Composable
fun BiometricUnlockScreen(userName: String, onUnlocked: () -> Unit, onLogout: () -> Unit) {
    val activity = LocalContext.current as FragmentActivity
    val scope = rememberCoroutineScope()
    var showPasswordFallback by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var checking by remember { mutableStateOf(false) }

    fun tryBiometric() {
        checking = true
        error = null
        scope.launch {
            when (val result = authenticateWithBiometrics(activity, "Unlock Gadget Mover", "Welcome back, $userName", negativeButtonText = "Use password")) {
                is BiometricAuthResult.Success -> onUnlocked()
                is BiometricAuthResult.Failed -> error = result.message
                is BiometricAuthResult.Cancelled -> Unit
            }
            checking = false
        }
    }

    // Offers the prompt immediately on arrival rather than waiting for a tap.
    LaunchedEffect(Unit) { tryBiometric() }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Fingerprint,
                    contentDescription = null,
                    modifier = Modifier.size(44.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text("Welcome back, $userName", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Confirm it's you to continue",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            error?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            }
            Spacer(modifier = Modifier.height(28.dp))
            Button(
                onClick = ::tryBiometric,
                enabled = !checking,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Unlock with Fingerprint")
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = { showPasswordFallback = true }) {
                Text("Use password instead")
            }
            TextButton(onClick = onLogout) {
                Text("Not you? Log out", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    if (showPasswordFallback) {
        PasswordConfirmDialog(
            title = "Confirm your password",
            message = "Enter your password to continue.",
            onDismiss = { showPasswordFallback = false },
            onConfirmed = {
                showPasswordFallback = false
                onUnlocked()
            },
            allowBiometric = false
        )
    }
}
