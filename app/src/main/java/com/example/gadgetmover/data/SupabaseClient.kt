package com.example.gadgetmover.data

import com.example.gadgetmover.BuildConfig
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.github.jan.supabase.storage.Storage
import kotlinx.serialization.json.Json

val supabase = createSupabaseClient(
    supabaseUrl = BuildConfig.SUPABASE_URL,
    supabaseKey = BuildConfig.SUPABASE_ANON_KEY
) {
    // coerceInputValues lets an enum column that's explicitly null (e.g. checkout_details ->
    // receivingMethod on a BUY order, which has no meetup/shipping method to record) fall back
    // to its Kotlin default instead of throwing — without this, decoding a single such row
    // fails the *entire* orders list, which silently broke every My Activities refresh.
    defaultSerializer = KotlinXSerializer(Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    })
    install(Auth)
    install(Postgrest)
    install(Storage)
    // Calls the create-payment-intent/get-payment-status Edge Functions (supabase/functions/) —
    // the Stripe secret key lives only in those functions' own secrets, never in this app.
    install(Functions)
    // Live-subscribes to new `notifications` rows so a system tray notification can be posted
    // while the app is running (see NotificationRepository.startRealtimeListening).
    install(Realtime)
}
