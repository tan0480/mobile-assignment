package com.example.gadgetmover.util

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** Whether this device can actually be prompted for a fingerprint/face check right now. */
enum class BiometricAvailability {
    AVAILABLE,
    /** No fingerprint/face sensor on this device at all. */
    NO_HARDWARE,
    /** Has a sensor, but the user hasn't enrolled a fingerprint/face in system settings. */
    NOT_ENROLLED,
    /** Sensor exists but is temporarily unusable (e.g. mid system update) or otherwise unsupported. */
    UNAVAILABLE
}

fun biometricAvailability(activity: FragmentActivity): BiometricAvailability =
    when (BiometricManager.from(activity).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
        BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.AVAILABLE
        BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
        BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricAvailability.NO_HARDWARE
        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NOT_ENROLLED
        else -> BiometricAvailability.UNAVAILABLE
    }

sealed class BiometricAuthResult {
    object Success : BiometricAuthResult()
    /** The user backed out (system back, the negative button) rather than the check itself failing. */
    object Cancelled : BiometricAuthResult()
    data class Failed(val message: String) : BiometricAuthResult()
}

/**
 * Shows the system's fingerprint/face prompt and suspends until the user succeeds, cancels, or it
 * errors out. The app never sees any biometric data itself — only this pass/fail result, verified
 * by the OS against secure hardware. Callers should already have checked [biometricAvailability]
 * before offering this (e.g. graying out a settings toggle) rather than relying on this failing.
 */
suspend fun authenticateWithBiometrics(
    activity: FragmentActivity,
    title: String,
    subtitle: String? = null,
    negativeButtonText: String = "Cancel"
): BiometricAuthResult = suspendCancellableCoroutine { continuation ->
    val callback = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            if (continuation.isActive) continuation.resume(BiometricAuthResult.Success)
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            if (!continuation.isActive) return
            val cancelled = errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                errorCode == BiometricPrompt.ERROR_CANCELED
            continuation.resume(if (cancelled) BiometricAuthResult.Cancelled else BiometricAuthResult.Failed(errString.toString()))
        }

        // A single unrecognized-finger attempt — the prompt stays open for another try, so this
        // isn't terminal; only onAuthenticationError/-Succeeded actually resolve the coroutine.
        override fun onAuthenticationFailed() = Unit
    }

    val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), callback)
    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle(title)
        .apply { subtitle?.let { setSubtitle(it) } }
        .setNegativeButtonText(negativeButtonText)
        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        .build()

    prompt.authenticate(promptInfo)
    continuation.invokeOnCancellation { prompt.cancelAuthentication() }
}
