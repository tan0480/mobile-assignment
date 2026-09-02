package com.example.gadgetmover.screen.profile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.gadgetmover.data.AuthRepository
import com.example.gadgetmover.screen.components.PhoneNumberField
import com.example.gadgetmover.ui.theme.BrandBlueDark
import com.example.gadgetmover.util.isValidPhoneNumber
import com.example.gadgetmover.util.isValidUserId
import com.example.gadgetmover.util.parsePhoneNumber
import com.example.gadgetmover.util.sanitizeUserIdInput
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(onBackClick: () -> Unit) {
    val user = AuthRepository.currentUser.value
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (user == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Please log in to edit your profile.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Scaffold
        }

        var name by remember(user.id) { mutableStateOf(user.name) }
        var userId by remember(user.id) { mutableStateOf(user.userId) }
        var userIdError by remember(user.id) { mutableStateOf<String?>(null) }
        val (initialCountry, initialLocalNumber) = remember(user.id) { parsePhoneNumber(user.phone) }
        var country by remember(user.id) { mutableStateOf(initialCountry) }
        var phone by remember(user.id) { mutableStateOf(initialLocalNumber) }
        var phoneError by remember(user.id) { mutableStateOf(false) }
        var location by remember(user.id) { mutableStateOf(user.location) }
        var isSubmitting by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
                .imePadding()
        ) {
            Spacer(modifier = Modifier.height(12.dp))
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
                leadingIcon = { Text("@", style = MaterialTheme.typography.bodyLarge) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                isError = userIdError != null
            )
            if (userIdError != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(userIdError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = user.email,
                onValueChange = {},
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                enabled = false
            )
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
                value = location,
                onValueChange = { location = it },
                label = { Text("Location") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(28.dp))
            Button(
                onClick = {
                    if (!isValidUserId(userId)) {
                        userIdError = "3-20 characters: lowercase letters, numbers, underscores"
                    } else if (phone.isNotBlank() && !isValidPhoneNumber(country, phone)) {
                        phoneError = true
                    } else {
                        isSubmitting = true
                        scope.launch {
                            if (!AuthRepository.isUserIdAvailable(userId, excludingUserId = user.id)) {
                                isSubmitting = false
                                userIdError = "That user ID is taken — try another"
                                return@launch
                            }
                            val success = AuthRepository.updateCurrentUser(
                                user.copy(
                                    name = name.ifBlank { user.name },
                                    userId = userId,
                                    phone = if (phone.isBlank()) "" else "${country.dialCode} $phone",
                                    location = location
                                )
                            )
                            isSubmitting = false
                            if (success) {
                                onBackClick()
                            } else {
                                snackbarHostState.showSnackbar("Couldn't save your changes. Please try again.")
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandBlueDark, contentColor = Color.White),
                enabled = name.isNotBlank() && !isSubmitting
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp), color = Color.White)
                } else {
                    Text("Save Changes", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
