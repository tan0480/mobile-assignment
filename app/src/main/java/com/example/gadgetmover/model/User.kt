package com.example.gadgetmover.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
    val name: String,
    /** Public, unique handle (e.g. "cjgoh84", shown as "@cjgoh84") — distinct from [name], which is a free-text display name that's allowed to repeat between accounts. */
    val userId: String = "",
    val email: String,
    val phone: String,
    val avatarUrl: String,
    val rating: Float,
    val ratingCount: Int,
    val location: String,
    val joinedDate: String,
    val isVerified: Boolean = false,
    val walletBalance: Double = 0.0,
    val password: String = "",
    /** False for an account that signed up via Google and has never set a Gadget Mover password — gates Buy/Rent/List an item behind creating one first, since those flows assume a real password exists. */
    val hasPassword: Boolean = true
)
