package com.example.gadgetmover.model

import kotlinx.serialization.Serializable

@Serializable
data class Address(
    val id: String,
    /** Short user-facing label, e.g. "Home"/"Office" — matches the character already shown on the address book screen's avatar circle. */
    val label: String,
    val receiverName: String,
    val phoneNumber: String,
    val fullAddress: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isDefault: Boolean = false
)
