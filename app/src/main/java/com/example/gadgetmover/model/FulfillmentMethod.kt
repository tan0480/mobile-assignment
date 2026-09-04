package com.example.gadgetmover.model

import kotlinx.serialization.Serializable

/** How a listing can be handed over to (or returned by) a buyer — set by the seller when listing, chosen by the buyer at checkout. */
@Serializable
enum class FulfillmentMethod(val label: String) {
    SHIPPING("Shipping"),
    MEETUP("Meet-up / Self-pickup")
}
