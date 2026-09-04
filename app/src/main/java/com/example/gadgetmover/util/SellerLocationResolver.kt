package com.example.gadgetmover.util

import android.content.Context
import android.location.Geocoder
import com.example.gadgetmover.model.filter.MalaysiaStates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

data class ResolvedSellerLocation(val city: String, val state: String)

/**
 * Reverse-geocodes [latitude]/[longitude] into a city + normalized Malaysia state (see
 * [MalaysiaStates.normalize]) — used to opportunistically capture a seller's city/state onto
 * their own profile the moment they pick a meet-up location while creating a listing (see
 * `ListingWizardScreen`'s meet-up-confirm step, which calls
 * [com.example.gadgetmover.data.AuthRepository.updateSellerLocation] with the result). Returns
 * null on any geocoding failure, or when the point doesn't resolve to one of Malaysia's own
 * states — this app only operates in Malaysia, so a foreign coordinate shouldn't get stored as
 * the seller's state.
 */
suspend fun resolveSellerLocation(context: Context, latitude: Double, longitude: Double): ResolvedSellerLocation? =
    withContext(Dispatchers.IO) {
        try {
            @Suppress("DEPRECATION")
            val address = Geocoder(context, Locale.getDefault())
                .getFromLocation(latitude, longitude, 1)
                ?.firstOrNull() ?: return@withContext null
            val state = MalaysiaStates.normalize(address.adminArea) ?: return@withContext null
            val city = address.locality ?: address.subAdminArea ?: state
            ResolvedSellerLocation(city, state)
        } catch (e: Exception) {
            null
        }
    }
