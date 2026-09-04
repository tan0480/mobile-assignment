package com.example.gadgetmover.screen.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import com.example.gadgetmover.data.AuthRepository
import com.example.gadgetmover.data.supabase
import com.example.gadgetmover.screen.components.CreatePasswordDialog
import com.example.gadgetmover.screen.components.PasswordConfirmDialog
import io.github.jan.supabase.auth.user.Identity
import io.github.jan.supabase.compose.auth.ComposeAuth
import io.github.jan.supabase.compose.auth.composable.NativeSignInResult
import io.github.jan.supabase.compose.auth.composable.rememberSignInWithGoogle
import io.github.jan.supabase.compose.auth.composeAuth
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
fun AccountInfoScreen(
    onBackClick: () -> Unit,
    onChangePasswordClick: () -> Unit,
    successMessage: String? = null,
    onSuccessMessageShown: () -> Unit = {}
) {
    val currentUser = AuthRepository.currentUser.value ?: return

    var name by rememberSaveable(currentUser.id) { mutableStateOf(currentUser.name) }
    var userId by rememberSaveable(currentUser.id) { mutableStateOf(currentUser.userId) }
    var phone by rememberSaveable(currentUser.id) { mutableStateOf(currentUser.phone) }
    var userIdError by remember { mutableStateOf<String?>(null) }
    var checkingUserId by remember { mutableStateOf(false) }
    var showConfirmSave by remember { mutableStateOf(false) }
    var isSaving by rememberSaveable { mutableStateOf(false) }
    var showCreatePassword by remember { mutableStateOf(false) }
    var googleIdentity by remember { mutableStateOf<Identity?>(null) }
    var showUnlinkGoogleConfirm by remember { mutableStateOf(false) }
    var unlinkingGoogle by remember { mutableStateOf(false) }
    var linkingGoogle by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    suspend fun refreshGoogleIdentity() {
        googleIdentity = AuthRepository.currentIdentities().firstOrNull { it.provider == "google" }
    }

    LaunchedEffect(currentUser.id) { refreshGoogleIdentity() }

    // ChangePasswordScreen navigates back here immediately on success rather than blocking on a
    // snackbar on its own screen — the confirmation message is handed off via the nav back stack
    // entry instead, same idiom as WalletScreen's Add Funds/Withdraw success handoff.
    LaunchedEffect(successMessage) {
        successMessage?.let {
            snackbarHostState.showSnackbar(it)
            onSuccessMessageShown()
        }
    }

    // Reuses the same native Credential Manager flow as "Continue with Google" on the login
    // screen, but with LINK_IDENTITY_CALLBACK instead of the default sign-in callback — this
    // attaches Google to the already-logged-in account rather than starting a new session.
    val linkGoogle = supabase.composeAuth.rememberSignInWithGoogle(
        onResult = { result ->
            when (result) {
                is NativeSignInResult.Success -> scope.launch {
                    linkingGoogle = false
                    AuthRepository.refreshCurrentUser()
                    refreshGoogleIdentity()
                    snackbarHostState.showSnackbar("Google account linked")
                }
                is NativeSignInResult.ClosedByUser -> linkingGoogle = false
                is NativeSignInResult.NetworkError -> {
                    linkingGoogle = false
                    scope.launch { snackbarHostState.showSnackbar("Network error — please try again") }
                }
                is NativeSignInResult.Error -> {
                    linkingGoogle = false
                    scope.launch { snackbarHostState.showSnackbar("Couldn't link Google — that account may already be linked elsewhere") }
                }
            }
        },
        onIdToken = ComposeAuth.LINK_IDENTITY_CALLBACK
    )

    val isDirty = name != currentUser.name || userId != currentUser.userId || phone != currentUser.phone
    val isValid = name.isNotBlank() && userId.isNotBlank()

    fun requestSave() {
        if (!currentUser.hasPassword) {
            scope.launch { snackbarHostState.showSnackbar("Create a password below first to save changes") }
            return
        }
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
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            if (!currentUser.hasPassword) {
                Text(
                    "Create a password below first to edit your profile.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
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
                    singleLine = true,
                    enabled = currentUser.hasPassword
                )
            }
            LabeledField("Username") {
                OutlinedTextField(
                    value = userId,
                    onValueChange = { userId = it.trim(); userIdError = null },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    enabled = currentUser.hasPassword,
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
                    enabled = currentUser.hasPassword,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = ::requestSave,
                enabled = currentUser.hasPassword && isDirty && isValid && !isSaving && !checkingUserId,
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
                OutlinedButton(onClick = onChangePasswordClick, modifier = Modifier.fillMaxWidth()) {
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

            Spacer(modifier = Modifier.height(28.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(20.dp))
            Text("Google Account", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            val identity = googleIdentity
            if (identity != null) {
                Text(
                    "Your account is connected to Google. Unlinking it means you'll only be able to log in with your email and password.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { showUnlinkGoogleConfirm = true },
                    enabled = !unlinkingGoogle,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    if (unlinkingGoogle) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.error)
                    } else {
                        Text("Unlink Google Account")
                    }
                }
            } else {
                Text(
                    "Link your Google account for a faster, one-tap log in next time.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { linkingGoogle = true; linkGoogle.startFlow() },
                    enabled = !linkingGoogle,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (linkingGoogle) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp))
                    } else {
                        Text("Link Google Account")
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
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
                    if (success) {
                        AuthRepository.refreshCurrentUser()
                        refreshGoogleIdentity()
                    }
                    isSaving = false
                    snackbarHostState.showSnackbar(if (success) "Profile updated" else "Couldn't save changes. Please try again.")
                }
            }
        )
    }

    if (showCreatePassword) {
        CreatePasswordDialog(
            onDismiss = { showCreatePassword = false },
            onCreated = {
                showCreatePassword = false
                scope.launch {
                    AuthRepository.refreshCurrentUser()
                    refreshGoogleIdentity()
                    snackbarHostState.showSnackbar("Password created")
                }
            }
        )
    }

    if (showUnlinkGoogleConfirm) {
        val identity = googleIdentity
        AlertDialog(
            onDismissRequest = { if (!unlinkingGoogle) showUnlinkGoogleConfirm = false },
            title = { Text("Unlink Google account?") },
            text = { Text("You'll only be able to log in with your email and password afterward.") },
            confirmButton = {
                Button(
                    enabled = !unlinkingGoogle && identity != null,
                    onClick = {
                        val identityId = identity?.identityId ?: return@Button
                        unlinkingGoogle = true
                        scope.launch {
                            val success = AuthRepository.unlinkIdentity(identityId)
                            unlinkingGoogle = false
                            showUnlinkGoogleConfirm = false
                            if (success) {
                                AuthRepository.refreshCurrentUser()
                                refreshGoogleIdentity()
                                snackbarHostState.showSnackbar("Google account unlinked")
                            } else {
                                snackbarHostState.showSnackbar(
                                    if (!currentUser.hasPassword) {
                                        "Create a password above first — you need another way to sign in before unlinking Google"
                                    } else {
                                        "Couldn't unlink your Google account. Please try again."
                                    }
                                )
                            }
                        }
                    }
                ) {
                    if (unlinkingGoogle) CircularProgressIndicator(modifier = Modifier.height(18.dp), color = Color.White) else Text("Unlink")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnlinkGoogleConfirm = false }, enabled = !unlinkingGoogle) { Text("Cancel") }
            }
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
