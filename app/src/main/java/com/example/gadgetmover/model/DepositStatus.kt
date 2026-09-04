package com.example.gadgetmover.model

import kotlinx.serialization.Serializable

/** Lifecycle of a rental's refundable security deposit, separate from [RentalOrder.deposit]. */
@Serializable
enum class DepositStatus {
    /** Canonical value for newly-created rentals. */
    HOLDING,
    /** Legacy value kept so already-persisted checkout JSON continues to decode. */
    HELD,
    REFUNDED,
    PARTIALLY_REFUNDED,
    FORFEITED
}

val DepositStatus?.isHolding: Boolean
    get() = this == null || this == DepositStatus.HOLDING || this == DepositStatus.HELD
