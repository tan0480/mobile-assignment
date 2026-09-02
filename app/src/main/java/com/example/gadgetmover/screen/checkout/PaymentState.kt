package com.example.gadgetmover.screen.checkout

/** Drives the Checkout screen's payment UI/CTA — see spec §13. */
sealed class PaymentState {
    data object Idle : PaymentState()
    data object CreatingPayment : PaymentState()
    data object PaymentReady : PaymentState()
    data object Processing : PaymentState()
    data object Success : PaymentState()
    data class Failed(val reason: String) : PaymentState()
    data object Cancelled : PaymentState()
    data object Pending : PaymentState()
    data object Expired : PaymentState()

    /** Whether a "Pay" tap should be accepted right now — guards against duplicate submissions while a payment is already in flight. */
    val acceptsNewAttempt: Boolean
        get() = this is Idle || this is Failed || this is Cancelled || this is Expired
}

enum class CheckoutPaymentMethod(val label: String, val isAvailable: Boolean) {
    STRIPE("Credit or Debit Card", isAvailable = true),
    WALLET("Gadget Mover Wallet", isAvailable = true)
}

/**
 * [fee] is now only a reference placeholder shown while a seller is setting their own
 * shipping fee at listing time (see [com.example.gadgetmover.model.Product.standardShippingFee]) —
 * the actual checkout price reads the listing's own fee, not this constant.
 * [transitDays] estimates how many days each shipping leg adds, used to size a rental
 * booking's locked-out date range (see util/RentalAvailability.kt).
 */
enum class ShippingTier(val label: String, val fee: Double, val etaLabel: String, val transitDays: Int) {
    STANDARD("Standard Delivery", 8.0, "3-5 days", transitDays = 3),
    EXPRESS("Express Delivery", 15.0, "1-2 days", transitDays = 1)
}
