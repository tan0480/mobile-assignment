package com.example.gadgetmover.screen.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.gadgetmover.data.AuthRepository
import com.example.gadgetmover.data.ChangePasswordResult
import com.example.gadgetmover.util.PASSWORD_REQUIREMENTS_HINT
import com.example.gadgetmover.util.validatePassword
import kotlinx.coroutines.launch

/**
 * A dedicated full-screen route rather than an [androidx.compose.material3.AlertDialog] — a Dialog
 * hosting several password fields made the keyboard visibly hide and re-show every time focus
 * moved between them (a known Compose Dialog + multi-TextField IME interaction that
 * `decorFitsSystemWindows = false` didn't fully resolve here); a plain screen never has that
 * problem, matching [com.example.gadgetmover.screen.auth.ResetPasswordScreen]'s own password form.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(
    email: String,
    onBackClick: () -> Unit,
    onChanged: () -> Unit
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var currentPasswordVisible by remember { mutableStateOf(false) }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun submit() {
        if (newPassword == currentPassword) {
            error = "New password must be different from your current password"
            return
        }
        validatePassword(newPassword)?.let {
            error = it
            return
        }
        if (newPassword != confirmPassword) {
            error = "Passwords don't match"
            return
        }
        busy = true
        scope.launch {
            val result = AuthRepository.changePassword(email, currentPassword, newPassword)
            busy = false
            when (result) {
                ChangePasswordResult.SUCCESS -> {
                    AuthRepository.refreshCurrentUser()
                    onChanged()
                }
                ChangePasswordResult.INCORRECT_CURRENT -> error = "Current password is incorrect"
                ChangePasswordResult.FAILED -> error = "Couldn't change your password. Please try again."
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Change Password") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            OutlinedTextField(
                value = currentPassword,
                onValueChange = { currentPassword = it; error = null },
                label = { Text("Current password") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                enabled = !busy,
                trailingIcon = { PasswordVisibilityToggle(currentPasswordVisible) { currentPasswordVisible = !currentPasswordVisible } },
                visualTransformation = if (currentPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it; error = null },
                label = { Text("New password") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                enabled = !busy,
                trailingIcon = { PasswordVisibilityToggle(newPasswordVisible) { newPasswordVisible = !newPasswordVisible } },
                visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                error ?: PASSWORD_REQUIREMENTS_HINT,
                color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it; error = null },
                label = { Text("Confirm new password") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                enabled = !busy,
                trailingIcon = { PasswordVisibilityToggle(confirmPasswordVisible) { confirmPasswordVisible = !confirmPasswordVisible } },
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
            Spacer(modifier = Modifier.height(28.dp))
            Button(
                onClick = ::submit,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (busy) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp), color = Color.White)
                } else {
                    Text("Confirm", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun PasswordVisibilityToggle(visible: Boolean, onToggle: () -> Unit) {
    IconButton(onClick = onToggle) {
        Icon(
            imageVector = if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
            contentDescription = if (visible) "Hide password" else "Show password"
        )
    }
}
