package com.example.gadgetmover.screen.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.gadgetmover.data.AuthRepository
import com.example.gadgetmover.data.ChangePasswordResult
import com.example.gadgetmover.screen.components.CreatePasswordDialog
import com.example.gadgetmover.screen.components.PasswordConfirmDialog
import kotlinx.coroutines.launch

/**
 * Lets the seller edit their own profile fields (name, handle, phone, location) and change their
 * password. Every actual change is gated behind [PasswordConfirmDialog] (fingerprint first, falls
 * back to password) or, for a password change specifically, the current password itself — see
 * that section's comment for why biometrics alone can't stand in there. Email is intentionally
 * read-only: it's the GoTrue login credential, and changing it safely needs its own verified
 * re-confirmation flow that doesn't exist yet, so this screen doesn't let it drift out of sync.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountInfoScreen(onBackClick: () -> Unit) {
    val currentUser = AuthRepository.currentUser.value ?: return

    var name by rememberSaveable(currentUser.id) { mutableStateOf(currentUser.name) }
    var userId by rememberSaveable(currentUser.id) { mutableStateOf(currentUser.userId) }
    var phone by rememberSaveable(currentUser.id) { mutableStateOf(currentUser.phone) }
    var userIdError by remember { mutableStateOf<String?>(null) }
    var checkingUserId by remember { mutableStateOf(false) }
    var showConfirmSave by remember { mutableStateOf(false) }
    var isSaving by rememberSaveable { mutableStateOf(false) }
    var showChangePassword by remember { mutableStateOf(false) }
    var showCreatePassword by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val isDirty = name != currentUser.name || userId != currentUser.userId || phone != currentUser.phone
    val isValid = name.isNotBlank() && userId.isNotBlank()

    fun requestSave() {
        if (userId == currentUser.userId) {
            userIdError = null
            showConfirmSave = true
            return
        }
        checkingUserId = true
        scope.launch {
            val available = AuthRepository.isUserIdAvailable(userId, excludingUserId = currentUser.id)
            checkingUserId = false
            if (available) {
                userIdError = null
                showConfirmSave = true
            } else {
                userIdError = "This handle is already taken"
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Account Information") },
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
                .padding(20.dp)
        ) {
            LabeledField("Email") {
                OutlinedTextField(
                    value = currentUser.email,
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    enabled = false,
                    supportingText = { Text("Your email is your login and can't be changed here") }
                )
            }
            LabeledField("Name") {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }
            LabeledField("Username") {
                OutlinedTextField(
                    value = userId,
                    onValueChange = { userId = it.trim(); userIdError = null },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    prefix = { Text("@") },
                    isError = userIdError != null,
                    supportingText = userIdError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } }
                )
            }
            LabeledField("Phone Number") {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it.filter { c -> c.isDigit() || c == '+' || c == '-' || c == ' ' } },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = ::requestSave,
                enabled = isDirty && isValid && !isSaving && !checkingUserId,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (isSaving || checkingUserId) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                } else {
                    Text("Save Changes", style = MaterialTheme.typography.titleMedium)
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(20.dp))
            Text("Password", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            if (currentUser.hasPassword) {
                Text(
                    "Changing your password always needs your current password — a fingerprint alone can't authorize that on its own.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(onClick = { showChangePassword = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Change Password")
                }
            } else {
                Text(
                    "You signed in with Google and don't have a password yet. Create one to also be able to log in with your email.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(onClick = { showCreatePassword = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Create Password")
                }
            }
        }
    }

    if (showConfirmSave) {
        PasswordConfirmDialog(
            title = "Confirm it's you",
            message = "Confirm your password or fingerprint to save these changes.",
            onDismiss = { showConfirmSave = false },
            onConfirmed = {
                showConfirmSave = false
                isSaving = true
                scope.launch {
                    val success = AuthRepository.updateCurrentUser(
                        currentUser.copy(name = name, userId = userId, phone = phone)
                    )
                    isSaving = false
                    snackbarHostState.showSnackbar(if (success) "Profile updated" else "Couldn't save changes. Please try again.")
                }
            }
        )
    }

    if (showChangePassword) {
        ChangePasswordDialog(
            email = currentUser.email,
            onDismiss = { showChangePassword = false },
            onSuccess = {
                showChangePassword = false
                scope.launch { snackbarHostState.showSnackbar("Password changed") }
            }
        )
    }

    if (showCreatePassword) {
        CreatePasswordDialog(
            onDismiss = { showCreatePassword = false },
            onCreated = {
                showCreatePassword = false
                scope.launch { snackbarHostState.showSnackbar("Password created") }
            }
        )
    }
}

@Composable
private fun ChangePasswordDialog(email: String, onDismiss: () -> Unit, onSuccess: () -> Unit) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var currentPasswordVisible by remember { mutableStateOf(false) }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("Change Password") },
        text = {
            Column {
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it; error = null },
                    label = { Text("Current password") },
                    singleLine = true,
                    enabled = !busy,
                    trailingIcon = { PasswordVisibilityToggle(currentPasswordVisible) { currentPasswordVisible = !currentPasswordVisible } },
                    visualTransformation = if (currentPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it; error = null },
                    label = { Text("New password") },
                    singleLine = true,
                    enabled = !busy,
                    trailingIcon = { PasswordVisibilityToggle(newPasswordVisible) { newPasswordVisible = !newPasswordVisible } },
                    visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; error = null },
                    label = { Text("Confirm new password") },
                    singleLine = true,
                    enabled = !busy,
                    isError = error != null,
                    trailingIcon = { PasswordVisibilityToggle(confirmPasswordVisible) { confirmPasswordVisible = !confirmPasswordVisible } },
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    supportingText = error?.let { { Text(it, color = MaterialTheme.colorScheme.error) } }
                )
            }
        },
        confirmButton = {
            Button(
                enabled = !busy,
                onClick = {
                    if (newPassword.length < 6) {
                        error = "New password must be at least 6 characters"
                        return@Button
                    }
                    if (newPassword != confirmPassword) {
                        error = "Passwords don't match"
                        return@Button
                    }
                    busy = true
                    scope.launch {
                        val result = AuthRepository.changePassword(email, currentPassword, newPassword)
                        busy = false
                        when (result) {
                            ChangePasswordResult.SUCCESS -> onSuccess()
                            ChangePasswordResult.INCORRECT_CURRENT -> error = "Current password is incorrect"
                            ChangePasswordResult.FAILED -> error = "Couldn't change your password. Please try again."
                        }
                    }
                }
            ) {
                if (busy) CircularProgressIndicator(modifier = Modifier.height(18.dp), color = Color.White) else Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancel") }
        }
    )
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

@Composable
private fun LabeledField(label: String, field: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        field()
    }
}
