package com.example.gadgetmover.model

import kotlinx.serialization.Serializable

@Serializable
data class PaymentMethod(
    val id: String,
    val brand: String,
    val last4: String,
    val expiry: String,
    val isDefault: Boolean = false
)
