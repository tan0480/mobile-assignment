package com.example.gadgetmover.util

/**
 * Fixed courier list for the "Mark as Shipped" flow. Named carriers' [trackingPattern]s are
 * reasonable approximations of their real tracking-number formats (not independently verifiable
 * without each carrier's own docs) — refine later if a real number gets rejected. [OTHER]'s
 * pattern is the one explicitly specified by product: 8-30 alphanumeric characters.
 *
 * [formatHint] is a human-readable example matching [trackingPattern], shown under the tracking
 * number field before the user has typed anything — the regex alone (surfaced only as a rejection
 * after a bad guess) doesn't tell anyone what a *correct* number looks like up front.
 */
enum class Courier(val label: String, val trackingPattern: Regex, val formatHint: String) {
    JNT_EXPRESS("J&T Express", Regex("^JT\\d{9,13}$"), "e.g. JT123456789 (JT + 9-13 digits)"),
    POS_LAJU("Pos Laju", Regex("^[A-Z]{2}\\d{9}MY$"), "e.g. EA123456789MY (2 letters + 9 digits + MY)"),
    NINJA_VAN("Ninja Van", Regex("^[A-Z0-9]{10,15}$"), "e.g. RN1234567890 (10-15 letters/digits)"),
    DHL_ECOMMERCE("DHL eCommerce", Regex("^\\d{10,12}$"), "e.g. 1234567890 (10-12 digits)"),
    CITY_LINK_EXPRESS("City-Link Express", Regex("^CL\\d{8,10}$"), "e.g. CL12345678 (CL + 8-10 digits)"),
    SKYNET("Skynet", Regex("^SKY\\d{8,10}$"), "e.g. SKY12345678 (SKY + 8-10 digits)"),
    OTHER("Other", Regex("^[A-Za-z0-9]{8,30}$"), "8-30 letters/digits")
}

fun Courier.validate(trackingNumber: String): Boolean = trackingPattern.matches(trackingNumber.trim())
