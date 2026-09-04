package com.example.gadgetmover.util

import android.net.Uri

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
enum class Courier(
    val label: String,
    val trackingPattern: Regex,
    val formatHint: String,
    /**
     * The courier's official tracking page — `{number}` is substituted with the tracking number
     * for the one courier ([NINJA_VAN]) whose tracking URL is confirmed to accept it as a query
     * parameter and pre-fill the search; every other courier's official site tracking search runs
     * through client-side JS rather than a plain URL parameter, so those just open the tracking
     * page itself (the number is copied to the clipboard alongside, see [openTrackingPage]). Null
     * for [OTHER] since there's no single official site for a free-text courier name.
     */
    val trackingUrlTemplate: String?
) {
    JNT_EXPRESS("J&T Express", Regex("^JT\\d{9,13}$"), "e.g. JT123456789 (JT + 9-13 digits)", "https://www.jtexpress.my/tracking"),
    POS_LAJU("Pos Laju", Regex("^[A-Z]{2}\\d{9}MY$"), "e.g. EA123456789MY (2 letters + 9 digits + MY)", "https://tracking.pos.com.my/tracking/"),
    NINJA_VAN("Ninja Van", Regex("^[A-Z0-9]{10,15}$"), "e.g. RN1234567890 (10-15 letters/digits)", "https://www.ninjavan.co/en-my/tracking?id={number}"),
    DHL_ECOMMERCE("DHL eCommerce", Regex("^\\d{10,12}$"), "e.g. 1234567890 (10-12 digits)", "https://www.dhl.com/my-en/home/tracking.html"),
    CITY_LINK_EXPRESS("City-Link Express", Regex("^CL\\d{8,10}$"), "e.g. CL12345678 (CL + 8-10 digits)", "https://www.citylinkexpress.com/track-your-shipment/"),
    SKYNET("Skynet", Regex("^SKY\\d{8,10}$"), "e.g. SKY12345678 (SKY + 8-10 digits)", "https://www.skynet.com.my/track"),
    OTHER("Other", Regex("^[A-Za-z0-9]{8,30}$"), "8-30 letters/digits", null)
}

fun Courier.validate(trackingNumber: String): Boolean = trackingPattern.matches(trackingNumber.trim())

/** True only for [Courier.NINJA_VAN] today — whether [trackingUrl] actually pre-fills the tracking number rather than just opening the courier's tracking page. */
fun Courier.tracksAutomatically(): Boolean = trackingUrlTemplate?.contains("{number}") == true

fun Courier.trackingUrl(trackingNumber: String): String? = trackingUrlTemplate?.replace("{number}", Uri.encode(trackingNumber))
