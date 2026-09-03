package com.example.gadgetmover.util

import java.time.OffsetDateTime
import java.time.Period
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val displayDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.US)
private val displayDateOnlyFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)

/**
 * [raw] is a `created_at`-style timestamptz exactly as Postgres/PostgREST returns it (e.g.
 * "2026-08-31T18:14:35.259301+00:00") — reformatted to the device's local time zone, 24-hour,
 * no fractional seconds, for every screen that shows a record's creation time. Falls back to the
 * raw string if it doesn't parse (e.g. blank pre-checkout-era test data).
 */
fun formatDisplayDate(raw: String): String = try {
    OffsetDateTime.parse(raw).atZoneSameInstant(ZoneId.systemDefault()).format(displayDateFormatter)
} catch (e: Exception) {
    raw
}

/** Same as [formatDisplayDate] but without the time-of-day, for places that only need the date (e.g. "Member since"). */
fun formatDisplayDateOnly(raw: String): String = try {
    OffsetDateTime.parse(raw).atZoneSameInstant(ZoneId.systemDefault()).format(displayDateOnlyFormatter)
} catch (e: Exception) {
    raw
}

/** Full years elapsed between [raw] (a `created_at`-style timestamptz) and now — e.g. a seller profile's "X years on Gadget Mover" stat. Falls back to 0 if [raw] doesn't parse. */
fun yearsSince(raw: String): Int = try {
    Period.between(
        OffsetDateTime.parse(raw).atZoneSameInstant(ZoneId.systemDefault()).toLocalDate(),
        OffsetDateTime.now().atZoneSameInstant(ZoneId.systemDefault()).toLocalDate()
    ).years
} catch (e: Exception) {
    0
}
