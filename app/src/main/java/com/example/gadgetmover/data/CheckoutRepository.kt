package com.example.gadgetmover.data

import com.example.gadgetmover.model.BuyOrder
import com.example.gadgetmover.model.CheckoutDetails
import com.example.gadgetmover.model.Product
import com.example.gadgetmover.model.RentalOrder
import com.example.gadgetmover.util.BookedRange
import com.example.gadgetmover.util.lockedRangeFor
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import io.ktor.client.call.body
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * The only thing in the app that talks to Stripe or to the two Supabase Edge Functions backing
 * it — `CheckoutViewModel` calls through here rather than hitting `supabase.functions` or Stripe
 * directly, keeping payment/network concerns out of the ViewModel per the checkout architecture.
 * The Stripe *secret* key never appears anywhere in this file or the app — it lives only in the
 * Edge Functions' own secrets (see supabase/functions/create-payment-intent, get-payment-status).
 */
object CheckoutRepository {

    @Serializable
    private data class CreatePaymentIntentRequest(val amount: Long, val currency: String = "myr")

    @Serializable
    data class PaymentIntentInfo(
        @SerialName("client_secret") val clientSecret: String,
        @SerialName("payment_intent_id") val paymentIntentId: String
    )

    @Serializable
    private data class GetPaymentStatusRequest(@SerialName("payment_intent_id") val paymentIntentId: String)

    @Serializable
    data class PaymentStatusInfo(val status: String)

    /** [amountCents] is the final total in the smallest currency unit (sen, i.e. RM × 100) — Stripe amounts are always integer minor units. */
    suspend fun createPaymentIntent(amountCents: Long): Result<PaymentIntentInfo> = runCatching {
        supabase.functions.invoke("create-payment-intent", body = CreatePaymentIntentRequest(amount = amountCents)).body()
    }

    /** Confirms server-side (via Stripe's own API, not a client-trusted flag) whether [paymentIntentId] actually succeeded — called right after `PaymentSheetResult.Completed`, before any order is created. */
    suspend fun getPaymentStatus(paymentIntentId: String): Result<PaymentStatusInfo> = runCatching {
        supabase.functions.invoke("get-payment-status", body = GetPaymentStatusRequest(paymentIntentId)).body()
    }

    @Serializable
    private data class GetBookedRentalRangesParams(@SerialName("p_product_id") val productId: String)

    @Serializable
    private data class BookedRangeRow(
        @SerialName("rental_start_date") val rentalStartDate: String? = null,
        @SerialName("rental_end_date") val rentalEndDate: String? = null,
        @SerialName("checkout_details") val checkoutDetails: CheckoutDetails = CheckoutDetails()
    )

    /** The already-locked-out date ranges for [productId]'s existing rental bookings, for the checkout calendar to disable. */
    suspend fun getBookedRentalRanges(productId: String): List<BookedRange> = runCatching {
        val rows = supabase.postgrest.rpc(
            "get_booked_rental_ranges",
            GetBookedRentalRangesParams(productId)
        ).decodeList<BookedRangeRow>()
        rows.mapNotNull { row ->
            val start = row.rentalStartDate?.let { LocalDate.parse(it) } ?: return@mapNotNull null
            val end = row.rentalEndDate?.let { LocalDate.parse(it) } ?: start
            val days = (ChronoUnit.DAYS.between(start, end) + 1).toInt().coerceAtLeast(1)
            lockedRangeFor(start, days, row.checkoutDetails)
        }
    }.getOrElse { emptyList() }

    suspend fun placeBuyOrder(product: Product, paymentId: String, checkout: CheckoutDetails, totalAmount: Double): BuyOrder? =
        OrderRepository.placeBuyOrder(product, paymentId, checkout, totalAmount)

    suspend fun placeRentalOrder(
        product: Product,
        startDateMillis: Long,
        endDateMillis: Long,
        days: Int,
        dailyRate: Double,
        deposit: Double,
        totalAmount: Double,
        paymentId: String,
        checkout: CheckoutDetails
    ): RentalOrder? = OrderRepository.placeRentalOrder(
        product, startDateMillis, endDateMillis, days, dailyRate, deposit, totalAmount, paymentId, checkout
    )
}
