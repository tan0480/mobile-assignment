package com.example.gadgetmover.model

import kotlinx.serialization.Serializable

@Serializable
enum class NotificationType(val label: String) {
    PAYMENT("Payment confirmed"),
    RENTAL_REQUEST("Rental request"),
    PRICE_ALERT("Price alert"),
    NEW_MESSAGE("New message"),
    LISTING_UPDATE("Listing update"),
    REVIEW("New review"),
    ORDER_UPDATE("Order update")
}

@Serializable
data class Notification(
    val id: String,
    val type: NotificationType,
    val title: String,
    val message: String,
    val timestamp: String,
    val isRead: Boolean = false,
    val relatedThreadId: String? = null
)
