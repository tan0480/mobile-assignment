package com.example.gadgetmover.screen.checkout

import com.example.gadgetmover.model.Address
import com.example.gadgetmover.model.FulfillmentMethod
import com.example.gadgetmover.model.ListingType
import com.example.gadgetmover.model.MeetupLocation
import com.example.gadgetmover.model.Order
import com.example.gadgetmover.model.Product
import com.example.gadgetmover.util.BookedRange
import java.time.LocalDate

/** The Checkout screen's entire state — computed/mutated only by [CheckoutViewModel], never inside a Composable (spec §14/§19). */
data class CheckoutUiState(
    val product: Product? = null,
    val transactionType: ListingType = ListingType.BUY,
    /** Overrides [product]'s listing price/rate when checkout was reached from a chat "Special Price" offer — null means charge the normal listing price. */
    val negotiatedPrice: Double? = null,

    // Rent only
    val rentalStartMillis: Long? = null,
    val rentalEndMillis: Long? = null,
    val rentalDuration: Int = 0,

    val receivingMethod: FulfillmentMethod? = null,
    /** Rent only — independently selectable from [receivingMethod] (spec §4). */
    val returningMethod: FulfillmentMethod? = null,

    val shippingTier: ShippingTier = ShippingTier.STANDARD,
    val selectedAddress: Address? = null,

    val receivingMeetup: MeetupLocation? = null,
    val returningMeetup: MeetupLocation? = null,

    val paymentMethod: CheckoutPaymentMethod = CheckoutPaymentMethod.STRIPE,
    val paymentState: PaymentState = PaymentState.Idle,
    /** Kept once a payment succeeds even if order creation then fails, so the failure can be shown/retried without re-charging (spec §21). */
    val lastPaymentIntentId: String? = null,
    /** True only when Stripe confirmed the charge but finalizing the order failed — the ONLY case where retrying must re-check the existing session ([CheckoutViewModel.confirmAndCreateOrder]) instead of starting a brand-new one ([CheckoutViewModel.startPayment]), so the customer is never charged twice. */
    val orderCreationFailedAfterPayment: Boolean = false,

    val voucherDiscount: Double = 0.0,

    val createdOrder: Order? = null,

    /** Rent only — other renters' already-locked-out date ranges for this product, fetched once at load (spec §1 booking-conflict locking). */
    val bookedRanges: List<BookedRange> = emptyList(),

    val isLoading: Boolean = true,
    val errorMessage: String? = null
) {
    private val isRent: Boolean get() = transactionType == ListingType.RENT

    val itemSubtotal: Double get() = if (!isRent) negotiatedPrice ?: product?.price ?: 0.0 else 0.0
    val rentalSubtotal: Double get() = if (isRent) (negotiatedPrice ?: product?.rentalRatePerDay ?: 0.0) * rentalDuration else 0.0
    val refundableDeposit: Double get() = if (isRent) product?.deposit ?: 0.0 else 0.0

    /** Shipping/fulfillment fee: one shipping leg's fee per method that's actually SHIPPING — a rental with both receiving and returning by shipping pays it twice, matching real courier cost. Reads the seller's own per-listing fee, not a platform-fixed amount. */
    val shippingFee: Double get() {
        val tierFee = when (shippingTier) {
            ShippingTier.STANDARD -> product?.standardShippingFee ?: 0.0
            ShippingTier.EXPRESS -> product?.expressShippingFee ?: 0.0
        }
        var fee = 0.0
        if (receivingMethod == FulfillmentMethod.SHIPPING) fee += tierFee
        if (isRent && returningMethod == FulfillmentMethod.SHIPPING) fee += tierFee
        return fee
    }

    /** At least RM2, or 4% of the item/rental subtotal, whichever is larger — scales with order size instead of a flat rate. */
    val platformFee: Double get() = maxOf(2.0, 0.04 * (itemSubtotal + rentalSubtotal))

    val finalTotal: Double get() = itemSubtotal + rentalSubtotal + platformFee + shippingFee + refundableDeposit - voucherDiscount

    val needsAddress: Boolean get() = receivingMethod == FulfillmentMethod.SHIPPING || (isRent && returningMethod == FulfillmentMethod.SHIPPING)
    val needsReceivingMeetup: Boolean get() = receivingMethod == FulfillmentMethod.MEETUP
    val needsReturningMeetup: Boolean get() = isRent && returningMethod == FulfillmentMethod.MEETUP

    /** True when the currently-picked rental window overlaps another renter's already-locked date range — the new booking's own raw window is checked as-is, since its fulfillment method (and thus its own buffer) isn't chosen until later in the flow. */
    val hasDateConflict: Boolean
        get() {
            if (!isRent || rentalStartMillis == null || rentalDuration <= 0) return false
            val start = LocalDate.ofEpochDay(rentalStartMillis / 86_400_000L)
            val end = start.plusDays((rentalDuration - 1).toLong())
            return bookedRanges.any { booked -> !end.isBefore(booked.start) && !booked.end.isBefore(start) }
        }

    /** Everything spec §22 requires before the "Pay" CTA may be tapped. */
    val isReadyToPay: Boolean
        get() {
            if (product == null || isLoading) return false
            if (isRent && rentalDuration <= 0) return false
            if (isRent && hasDateConflict) return false
            if (receivingMethod == null) return false
            if (isRent && returningMethod == null) return false
            if (needsAddress && selectedAddress == null) return false
            if (needsReceivingMeetup && receivingMeetup == null) return false
            if (needsReturningMeetup && returningMeetup == null) return false
            if (!paymentState.acceptsNewAttempt) return false
            return true
        }
}
