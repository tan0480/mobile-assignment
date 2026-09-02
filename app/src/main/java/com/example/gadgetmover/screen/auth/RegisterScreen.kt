package com.example.gadgetmover.screen.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.gadgetmover.data.AuthRepository
import com.example.gadgetmover.data.OtpSendResult
import com.example.gadgetmover.data.PendingRegistration
import com.example.gadgetmover.model.OtpPurpose
import com.example.gadgetmover.screen.components.PhoneNumberField
import com.example.gadgetmover.ui.theme.BrandOrange
import com.example.gadgetmover.util.PASSWORD_REQUIREMENTS_HINT
import com.example.gadgetmover.util.defaultCountryCode
import com.example.gadgetmover.util.isValidEmail
import com.example.gadgetmover.util.isValidPhoneNumber
import com.example.gadgetmover.util.isValidUserId
import com.example.gadgetmover.util.sanitizeUserIdInput
import com.example.gadgetmover.util.validatePassword
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onBackClick: () -> Unit,
    onOtpRequired: (email: String) -> Unit,
    onLoginClick: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var userId by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var country by remember { mutableStateOf(defaultCountryCode) }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var userIdError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var fallbackDialogCode by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun submit() {
        val pwdError = validatePassword(password)
        phoneError = !isValidPhoneNumber(country, phone)
        confirmPasswordError = if (confirmPassword != password) "Passwords do not match" else null
        userIdError = if (!isValidUserId(userId)) "3-20 characters: lowercase letters, numbers, underscores" else null
        when {
            userIdError != null -> Unit
            !isValidEmail(email) -> emailError = "Enter a valid email address"
            phoneError -> Unit
            pwdError != null -> passwordError = pwdError
            confirmPasswordError != null -> Unit
            else -> {
                isSubmitting = true
                scope.launch {
                    val outcome = AuthRepository.sendOtp(
                        email = email,
                        purpose = OtpPurpose.REGISTRATION,
                        registration = PendingRegistration(
                            name = name.ifBlank { "New User" },
                            userId = userId,
                            email = email.trim(),
                            password = password,
                            phone = "${country.dialCode} $phone"
                        )
                    )
                    isSubmitting = false
                    when (outcome.result) {
                        OtpSendResult.SENT -> onOtpRequired(email.trim())
                        OtpSendResult.EMAIL_DELIVERY_FAILED -> fallbackDialogCode = outcome.fallbackCode
                        OtpSendResult.EMAIL_ALREADY_REGISTERED ->
                            emailError = "That email is already registered — try logging in instead"
                        OtpSendResult.USER_ID_TAKEN ->
                            userIdError = "That user ID is taken — try another"
                        OtpSendResult.EMAIL_NOT_FOUND -> Unit
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Account") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Join Gadget Mover",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Create an account to start buying, selling, and renting.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(28.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = userId,
                onValueChange = { userId = sanitizeUserIdInput(it); userIdError = null },
                label = { Text("User ID") },
                placeholder = { Text("your_unique_id") },
                leadingIcon = { Text("@", style = MaterialTheme.typography.bodyLarge) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                isError = userIdError != null
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                userIdError ?: "This is your unique public ID — others can find you by it.",
                color = if (userIdError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it; emailError = null },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                isError = emailError != null,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Email)
            )
            if (emailError != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(emailError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(16.dp))
            PhoneNumberField(
                country = country,
                onCountryChange = { country = it; phoneError = false },
                localNumber = phone,
                onLocalNumberChange = { phone = it; phoneError = false },
                modifier = Modifier.fillMaxWidth(),
                isError = phoneError
            )
            if (phoneError) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Enter a valid ${country.localDigits.first}-${country.localDigits.last} digit number for ${country.countryName}.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; passwordError = null },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                isError = passwordError != null,
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = null
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation()
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                passwordError ?: PASSWORD_REQUIREMENTS_HINT,
                color = if (passwordError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it; confirmPasswordError = null },
                label = { Text("Confirm Password") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                isError = confirmPasswordError != null,
                trailingIcon = {
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(
                            imageVector = if (confirmPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = null
                        )
                    }
                },
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation()
            )
            if (confirmPasswordError != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(confirmPasswordError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(28.dp))
            Button(
                onClick = { submit() },
                enabled = !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp), color = Color.White)
                } else {
                    Text("Create Account", style = MaterialTheme.typography.titleMedium)
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Already have an account?", color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = onLoginClick) {
                    Text("Log In")
                }
            }
        }
    }

    val dialogCode = fallbackDialogCode
    if (dialogCode != null) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Email couldn't be delivered") },
            text = {
                Text(
                    "We generated your verification code, but couldn't email it right now. " +
                        "For testing, your code is:\n\n$dialogCode"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    fallbackDialogCode = null
                    onOtpRequired(email.trim())
                }) {
                    Text("Continue")
                }
            }
        )
    }
}
