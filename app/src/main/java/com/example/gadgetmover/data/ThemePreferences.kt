package com.example.gadgetmover.data

import android.content.Context

/**
 * Persists the user's dark-mode override across launches — a device display preference, not an
 * account fact, so (unlike [SettingsRepository]'s notification toggles) it stays local rather
 * than syncing through `profiles`.
 */
object ThemePreferences {

    private const val PREFS_NAME = "gadget_mover_prefs"
    private const val KEY_FORCE_DARK_MODE = "force_dark_mode"
    private const val VALUE_LIGHT = 0
    private const val VALUE_DARK = 1
    private const val VALUE_SYSTEM = -1

    /** null = follow system theme, true = force dark, false = force light. */
    fun load(context: Context): Boolean? =
        when (context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getInt(KEY_FORCE_DARK_MODE, VALUE_SYSTEM)) {
            VALUE_LIGHT -> false
            VALUE_DARK -> true
            else -> null
        }

    fun save(context: Context, forceDarkMode: Boolean?) {
        val value = when (forceDarkMode) {
            false -> VALUE_LIGHT
            true -> VALUE_DARK
            null -> VALUE_SYSTEM
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_FORCE_DARK_MODE, value)
            .apply()
    }
}
