package com.example.gadgetmover.screen.auth

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.gadgetmover.data.AuthRepository
import com.example.gadgetmover.data.CompleteRegistrationResult
import com.example.gadgetmover.data.OtpSendResult
import com.example.gadgetmover.data.OtpVerifyResult
import com.example.gadgetmover.model.OtpPurpose
import com.example.gadgetmover.ui.theme.BrandOrange
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtpVerificationScreen(
    email: String,
    purpose: OtpPurpose,
    onBackClick: () -> Unit,
    onVerified: () -> Unit
) {
    var code by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isVerifying by remember { mutableStateOf(false) }
    var isResending by remember { mutableStateOf(false) }
    var resendCount by remember { mutableStateOf(0) }
    var secondsRemaining by remember { mutableStateOf(60) }
    val focusRequester = remember { FocusRequester() }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(resendCount) {
        secondsRemaining = 60
        while (secondsRemaining > 0) {
            delay(1000)
            secondsRemaining--
        }
    }

    val purposeDescription = when (purpose) {
        OtpPurpose.REGISTRATION -> "verify your account"
        OtpPurpose.FORGOT_PASSWORD -> "reset your password"
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Verify Code") },
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
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState())
                .imePadding()
        ) {
            Text(
                "Enter verification code",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "We emailed a 6-digit code to $email to $purposeDescription. It expires in 5 minutes.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(28.dp))

            // A single real text field (invisible, sized to match the whole row) drives 6 visual
            // boxes rendered from its value — rather than 6 separate text fields. That old
            // approach made correcting a typo fiddly (each box only ever held one character, so
            // fixing a middle digit meant precise taps plus backspacing across boxes); this way
            // backspace/select-all/paste-a-whole-code all behave exactly like a normal text box,
            // because it is one.
            Box(modifier = Modifier.fillMaxWidth()) {
                BasicTextField(
                    value = code,
                    onValueChange = { input ->
                        errorMessage = null
                        code = input.filter { it.isDigit() }.take(6)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    singleLine = true,
                    cursorBrush = SolidColor(Color.Transparent),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    decorationBox = { innerTextField ->
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                repeat(6) { index ->
                                    val isNextToFill = index == code.length
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(0.85f)
                                            .border(
                                                width = if (errorMessage != null || isNextToFill) 2.dp else 1.dp,
                                                color = when {
                                                    errorMessage != null -> MaterialTheme.colorScheme.error
                                                    isNextToFill -> BrandOrange
                                                    else -> MaterialTheme.colorScheme.outline
                                                },
                                                shape = RoundedCornerShape(12.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(code.getOrNull(index)?.toString() ?: "", style = MaterialTheme.typography.headlineSmall)
                                    }
                                }
                            }
                            // Required by BasicTextField's API (this is what actually owns focus/IME/
                            // selection) but invisible — the boxes above are the only thing shown; a tap
                            // anywhere in this row still focuses/edits the real field underneath.
                            Box(modifier = Modifier.matchParentSize().alpha(0f)) { innerTextField() }
                        }
                    }
                )
            }
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(errorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    isVerifying = true
                    scope.launch {
                        when (AuthRepository.verifyOtp(email, purpose, code)) {
                            OtpVerifyResult.SUCCESS -> {
                                val proceed = if (purpose == OtpPurpose.REGISTRATION) {
                                    when (val result = AuthRepository.completeRegistration()) {
                                        is CompleteRegistrationResult.Success -> true
                                        is CompleteRegistrationResult.Failed -> {
                                            errorMessage = result.reason
                                            false
                                        }
                                    }
                                } else {
                                    true
                                }
                                isVerifying = false
                                if (proceed) {
                                    onVerified()
                                }
                            }
                            OtpVerifyResult.INCORRECT -> {
                                isVerifying = false
                                errorMessage = "Incorrect code. Please try again."
                            }
                            OtpVerifyResult.EXPIRED -> {
                                isVerifying = false
                                errorMessage = "This code has expired. Tap Resend to get a new one."
                            }
                            OtpVerifyResult.NOT_FOUND -> {
                                isVerifying = false
                                errorMessage = "Something went wrong. Please request a new code."
                            }
                        }
                    }
                },
                enabled = code.length == 6 && !isVerifying,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
            ) {
                if (isVerifying) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp), color = Color.White)
                } else {
                    Text("Verify", style = MaterialTheme.typography.titleMedium)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Didn't get a code?", color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(
                    onClick = {
                        scope.launch {
                            isResending = true
                            val outcome = AuthRepository.sendOtp(email, purpose)
                            isResending = false
                            resendCount++
                            code = ""
                            focusRequester.requestFocus()
                            val message = when (outcome.result) {
                                OtpSendResult.SENT -> "A new code was sent to $email"
                                OtpSendResult.EMAIL_DELIVERY_FAILED ->
                                    "Email delivery failed — for testing, your new code is ${outcome.fallbackCode}"
                                OtpSendResult.EMAIL_ALREADY_REGISTERED -> "That email is already registered"
                                OtpSendResult.USER_ID_TAKEN -> "That user ID was taken while you were verifying — go back and pick another"
                                OtpSendResult.EMAIL_NOT_FOUND -> "No account found for that email"
                            }
                            snackbarHostState.showSnackbar(message)
                        }
                    },
                    enabled = secondsRemaining == 0 && !isResending
                ) {
                    Text(if (secondsRemaining > 0) "Resend in ${secondsRemaining}s" else "Resend")
                }
            }
        }
    }
}
