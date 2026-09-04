package com.example.gadgetmover.data

import android.content.Context

/**
 * Whether this device should gate app unlock / wallet payments behind a fingerprint check —
 * device-local settings, same shape as [ThemePreferences], not something synced to the account.
 */
object BiometricPreferences {

    private const val PREFS_NAME = "gadget_mover_prefs"
    private const val KEY_BIOMETRIC_PAYMENTS = "biometric_payments_enabled"

    /** Try a fingerprint check first (falling back to the password prompt) whenever a password would otherwise be required (wallet payments/withdrawals, saving profile changes, etc). */
    fun isPaymentsEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_BIOMETRIC_PAYMENTS, false)

    fun setPaymentsEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean(KEY_BIOMETRIC_PAYMENTS, enabled).apply()
    }
}
