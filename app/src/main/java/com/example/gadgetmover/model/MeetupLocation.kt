package com.example.gadgetmover.model

import kotlinx.serialization.Serializable

/** A seller-declared meet-up spot for one listing — shown at checkout and opened in Google Maps via [com.example.gadgetmover.util.openInGoogleMaps]. */
@Serializable
data class MeetupLocation(
    val id: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double
)
