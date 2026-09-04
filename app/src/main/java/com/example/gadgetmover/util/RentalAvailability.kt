package com.example.gadgetmover.util

import com.example.gadgetmover.model.CheckoutDetails
import com.example.gadgetmover.model.FulfillmentMethod
import com.example.gadgetmover.screen.checkout.ShippingTier
import java.time.LocalDate

/** An inclusive [start, end] date range that is unavailable for a new rental booking. */
data class BookedRange(val start: LocalDate, val end: LocalDate)

/**
 * Expands an existing rental order's raw `[rentalStart, rentalStart + days - 1]` window into the
 * actual locked-out range, adding shipping transit-day buffers on whichever leg (receiving/
 * returning) was fulfilled by SHIPPING — a MEETUP leg adds no buffer. E.g. Aug 31 + 4 days with
 * both legs shipped via Standard (3-day transit): 3 (out) + 4 (rental) + 3 (back) = 10 days locked,
 * Aug 31-Sep 9. Same booking with both legs meet-up: just the 4 raw days, Aug 31-Sep 3.
 */
fun lockedRangeFor(rentalStart: LocalDate, rentalDays: Int, checkout: CheckoutDetails): BookedRange {
    val tier = checkout.shippingTierUsed?.let { name ->
        runCatching { ShippingTier.valueOf(name) }.getOrNull()
    }
    val transitDays = tier?.transitDays ?: 0
    val outBufferDays = if (checkout.receivingMethod == FulfillmentMethod.SHIPPING) transitDays else 0
    val backBufferDays = if (checkout.returningMethod == FulfillmentMethod.SHIPPING) transitDays else 0
    // The lockout starts on rentalStart itself (not shifted earlier) — the buffers extend the
    // *end* of the locked window, matching the user's own worked example: Aug 31 + 4 days,
    // shipped both ways (3-day transit) locks Aug 31-Sep 9 (3+4+3=10 days from Aug 31), not
    // Aug 28-Sep 6.
    val lockedEnd = rentalStart.plusDays((outBufferDays + rentalDays + backBufferDays - 1).toLong())
    return BookedRange(rentalStart, lockedEnd)
}

/** Whether two inclusive date ranges share at least one day. */
fun overlaps(a: BookedRange, b: BookedRange): Boolean = !a.end.isBefore(b.start) && !b.end.isBefore(a.start)
