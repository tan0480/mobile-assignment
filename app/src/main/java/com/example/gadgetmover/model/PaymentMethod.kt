package com.example.gadgetmover.model

import kotlinx.serialization.Serializable

@Serializable
data class PaymentMethod(
    val id: String,
    val brand: String,
    val last4: String,
    val expMonth: Int,
    val expYear: Int,
    val isDefault: Boolean = false
) {
    /** e.g. "03/27" — [expYear] is Stripe's full 4-digit year. */
    val expiry: String get() = "%02d/%02d".format(expMonth, expYear % 100)
}
