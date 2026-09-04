package com.example.gadgetmover.model.filter

/**
 * The 13 states + 3 federal territories of Malaysia — the fixed catalogue behind
 * [CommonFilterFields.sellerState] (the buyer-facing "State" common filter) and the seller-side
 * state resolved from a picked meet-up location (see `util/SellerLocationResolver.kt`). This app
 * only operates within Malaysia, so both sides of that match share this one list.
 */
object MalaysiaStates {
    val ALL: List<String> = listOf(
        "Johor", "Kedah", "Kelantan", "Melaka", "Negeri Sembilan", "Pahang", "Perak", "Perlis",
        "Pulau Pinang", "Sabah", "Sarawak", "Selangor", "Terengganu",
        "WP Kuala Lumpur", "WP Labuan", "WP Putrajaya"
    )

    /**
     * Normalizes a free-text admin-area string — as [android.location.Geocoder] returns it, which
     * varies ("Wilayah Persekutuan Kuala Lumpur", "Penang", "W.P. Putrajaya", plain "Selangor") —
     * to one of [ALL], or null if it doesn't resolve to a Malaysian state/federal territory at all
     * (e.g. the device is outside Malaysia).
     */
    fun normalize(rawAdminArea: String?): String? {
        val normalized = rawAdminArea?.trim()?.lowercase() ?: return null
        return when {
            "kuala lumpur" in normalized -> "WP Kuala Lumpur"
            "labuan" in normalized -> "WP Labuan"
            "putrajaya" in normalized -> "WP Putrajaya"
            "penang" in normalized || "pulau pinang" in normalized -> "Pulau Pinang"
            "negeri sembilan" in normalized -> "Negeri Sembilan"
            else -> ALL.find { it.lowercase() == normalized }
        }
    }
}
