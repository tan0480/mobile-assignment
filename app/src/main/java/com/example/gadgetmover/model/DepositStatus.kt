package com.example.gadgetmover.model

import kotlinx.serialization.Serializable

/** Lifecycle of a rental's refundable security deposit — separate from the deposit *amount* stored on [RentalOrder.deposit]. No automatic refund flow exists yet; this just records state for a future one. */
@Serializable
enum class DepositStatus {
    HELD,
    REFUNDED,
    PARTIALLY_REFUNDED,
    FORFEITED
}
