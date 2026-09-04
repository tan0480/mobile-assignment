package com.example.gadgetmover.model

import kotlinx.serialization.Serializable

@Serializable
enum class OrderStatus(val label: String) {
    PENDING("Pending"),
    CONFIRMED("Confirmed"),
    ACTIVE("Active"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled"),
    // Appended for the real checkout/payment flow rather than renaming/removing anything
    // above — the values above are already persisted as raw strings in the live `orders`
    // table, and a rename would silently break `enumOrDefault`'s lookup for existing rows.
    PAYMENT_PENDING("Payment Pending"),
    PAID("Paid"),
    PROCESSING("Processing"),
    READY_FOR_HANDOVER("Ready for Handover"),
    RENTING("Renting"),
    RETURN_PENDING("Return Pending"),
    RETURNED("Returned"),
    // Appended for the granular ship/receive/review/return-refund lifecycle — same
    // append-only convention as the block above. PROCESSING/READY_FOR_HANDOVER/RETURNED
    // are now vestigial (kept so old rows still decode) since no new transition reaches them.
    SHIPPED("To Receive"),
    TO_REVIEW("To Review"),
    RENTAL_SHIPPED("To Receive"),
    RETURN_REQUESTED("Return Requested"),
    RETURN_AWAITING_SHIP("Return Approved"),
    RETURN_AWAITING_RECEIPT("Return Shipped"),
    REFUNDED("Refunded")
}

/** Which granular [OrderStatus] values fall under each My Activities status-filter chip for a BUY order (Purchases/Sales tabs). `statuses == null` means "All" (no filter). */
enum class BuyActivityTab(val label: String, val statuses: Set<OrderStatus>?) {
    ALL("All", null),
    TO_SHIP("To Ship", setOf(OrderStatus.PAID)),
    TO_RECEIVE("To Receive", setOf(OrderStatus.SHIPPED)),
    TO_REVIEW("To Review", setOf(OrderStatus.TO_REVIEW)),
    RETURN_REFUND(
        "Return/Refund",
        setOf(OrderStatus.RETURN_REQUESTED, OrderStatus.RETURN_AWAITING_SHIP, OrderStatus.RETURN_AWAITING_RECEIPT, OrderStatus.REFUNDED)
    ),
    CANCELLATION("Cancellation", setOf(OrderStatus.CANCELLED))
}

/** Same idea as [BuyActivityTab] but for the Rentals/Leases tabs — the two-leg ship/receive cycle plus review. */
enum class RentActivityTab(val label: String, val statuses: Set<OrderStatus>?) {
    ALL("All", null),
    TO_SHIP("To Ship", setOf(OrderStatus.PAID)),
    TO_RECEIVE_RENTER("To Receive (Renter)", setOf(OrderStatus.RENTAL_SHIPPED)),
    TO_RETURN("To Return", setOf(OrderStatus.RENTING)),
    TO_RECEIVE_OWNER("To Receive (Owner)", setOf(OrderStatus.RETURN_PENDING)),
    TO_REVIEW("To Review", setOf(OrderStatus.TO_REVIEW)),
    COMPLETED("Completed", setOf(OrderStatus.COMPLETED))
}

/** The Stripe-side payment state for one order — distinct from [OrderStatus], which also covers non-payment lifecycle stages (handover, return, ...). */
@Serializable
enum class PaymentRecordStatus {
    PENDING, PAID, FAILED, REFUNDED
}

/**
 * Checkout-time details shared by both order types, stored as a single `checkout_details`
 * jsonb column rather than a dozen new flat columns — the same "compound, varies per row"
 * precedent already set by `products.specs`.
 */
@Serializable
data class CheckoutDetails(
    val platformFee: Double = 0.0,
    val shippingFee: Double = 0.0,
    val voucherDiscount: Double = 0.0,
    val receivingMethod: FulfillmentMethod = FulfillmentMethod.MEETUP,
    /** Rental-only — a buyer can return by a different method than they received by. */
    val returningMethod: FulfillmentMethod? = null,
    val shippingAddressId: String? = null,
    /**
     * A copy of the buyer's chosen address at checkout time, taken because [shippingAddressId]
     * alone isn't enough for the seller to ship — [com.example.gadgetmover.data.AddressRepository]
     * only ever holds the *current* user's own saved addresses, so the seller can never resolve
     * the buyer's address by that id. Null for shipping-less orders and for orders placed before
     * this snapshot existed.
     */
    val shippingReceiverName: String? = null,
    val shippingPhoneNumber: String? = null,
    val shippingFullAddress: String? = null,
    /**
     * Rental-only, only set when [returningMethod] is SHIPPING — the *owner's* address for the
     * return leg, copied from the product's own listing-time snapshot
     * ([com.example.gadgetmover.model.Product.returnFullAddress]) rather than any address book,
     * unlike [shippingFullAddress] above which is the buyer's own chosen delivery address.
     */
    val returnReceiverName: String? = null,
    val returnPhoneNumber: String? = null,
    val returnFullAddress: String? = null,
    val receivingMeetup: MeetupLocation? = null,
    val returningMeetup: MeetupLocation? = null,
    /** Rental-only, mirrors [RentalOrder.deposit]'s lifecycle. */
    val depositStatus: DepositStatus? = null,
    /**
     * Rental-only — the shipping speed actually chosen when [receivingMethod]/[returningMethod]
     * is SHIPPING, stored as the raw `ShippingTier` enum name (screen.checkout.ShippingTier;
     * kept as a String here rather than a direct reference since `model` doesn't depend on the
     * `screen` layer). Needed to size this order's booking-conflict lockout window accurately
     * (see util/RentalAvailability.kt) since different tiers have different transit-day buffers.
     */
    val shippingTierUsed: String? = null,
    /**
     * Courier + tracking number for the outbound leg (seller/owner shipping to the
     * buyer/renter), captured via [com.example.gadgetmover.screen.components.ShipmentDialog]
     * when [receivingMethod] is SHIPPING — null for meetup handovers. These are plain
     * camelCase keys inside the `checkout_details` jsonb column (no @SerialName override
     * anywhere on this class), so any SQL writing them via jsonb_set must use these exact
     * key names or the write silently no-ops on read (ignoreUnknownKeys + coerceInputValues).
     */
    val outboundCourier: String? = null,
    val outboundTrackingNumber: String? = null,
    /** Same as [outboundCourier]/[outboundTrackingNumber] but for a return leg — a rental's
     * renter-ships-back step, or a BUY return/refund's buyer-ships-the-item-back step. */
    val returnCourier: String? = null,
    val returnTrackingNumber: String? = null
)

@Serializable
sealed class Order {
    abstract val id: String
    /** The id of the account this order record belongs to (whose activity list it appears in). */
    abstract val ownerId: String
    abstract val productId: String
    abstract val productTitle: String
    abstract val productImage: String
    abstract val counterpartyName: String
    abstract val status: OrderStatus
    abstract val createdDate: String
    /** When the seller/owner marked the outbound leg shipped — null until then, and never touched by the return leg. */
    abstract val shippedAt: String?
    /** When the buyer/renter confirmed they received the outbound shipment — null until then. */
    abstract val receivedAt: String?
    /** When the renter/buyer shipped the return leg — null until then, and null forever for an order with no return leg. */
    abstract val returnShippedAt: String?
    /** When the owner/seller confirmed receiving the returned item — null until then. */
    abstract val returnReceivedAt: String?
    /** The Stripe PaymentIntent id backing this order's payment — null only for pre-checkout-era test data. */
    abstract val paymentId: String?
    abstract val paymentStatus: PaymentRecordStatus
    abstract val checkout: CheckoutDetails
}

@Serializable
data class BuyOrder(
    override val id: String,
    override val ownerId: String,
    override val productId: String,
    override val productTitle: String,
    override val productImage: String,
    override val counterpartyName: String,
    override val status: OrderStatus,
    override val createdDate: String,
    override val shippedAt: String? = null,
    override val receivedAt: String? = null,
    override val returnShippedAt: String? = null,
    override val returnReceivedAt: String? = null,
    override val paymentId: String? = null,
    override val paymentStatus: PaymentRecordStatus = PaymentRecordStatus.PENDING,
    override val checkout: CheckoutDetails = CheckoutDetails(),
    val price: Double,
    val isPurchase: Boolean
) : Order()

@Serializable
data class RentalOrder(
    override val id: String,
    override val ownerId: String,
    override val productId: String,
    override val productTitle: String,
    override val productImage: String,
    override val counterpartyName: String,
    override val status: OrderStatus,
    override val createdDate: String,
    override val shippedAt: String? = null,
    override val receivedAt: String? = null,
    override val returnShippedAt: String? = null,
    override val returnReceivedAt: String? = null,
    override val paymentId: String? = null,
    override val paymentStatus: PaymentRecordStatus = PaymentRecordStatus.PENDING,
    override val checkout: CheckoutDetails = CheckoutDetails(),
    val startDateMillis: Long,
    val endDateMillis: Long,
    val days: Int,
    val dailyRate: Double,
    val deposit: Double,
    val totalAmount: Double,
    val isRenter: Boolean
) : Order()
