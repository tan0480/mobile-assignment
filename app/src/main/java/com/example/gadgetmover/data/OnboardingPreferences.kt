package com.example.gadgetmover.data

import android.content.Context

/**
 * Tracks whether the one-time [com.example.gadgetmover.screen.auth.IntroScreen] has already
 * been shown, so it only appears on the very first app launch and is skipped afterwards.
 */
object OnboardingPreferences {

    private const val PREFS_NAME = "gadget_mover_prefs"
    private const val KEY_HAS_SEEN_INTRO = "has_seen_intro"

    fun hasSeenIntro(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_HAS_SEEN_INTRO, false)

    fun markIntroSeen(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_HAS_SEEN_INTRO, true)
            .apply()
    }
}
