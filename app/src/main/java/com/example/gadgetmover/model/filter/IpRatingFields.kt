package com.example.gadgetmover.model.filter

/**
 * The Ingress Protection (IP Rating) field, shared verbatim by every category that carries one
 * (Headphones, Wireless Earphones, Audio & Speakers, Smartphones, Tablets) — each used to keep its
 * own small hand-picked option list, but the full IEC 60529 code space is the same regardless of
 * category, so it's factored out once here rather than duplicated per schema. A searchable popup
 * (rather than a chip grid) since the full code space is ~85 entries; multi-select since a listing
 * can legitimately carry more than one rating (e.g. "IP54 or IP67 depending on region").
 */
object IpRatingFields {
    private fun slug(label: String): String =
        label.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')

    private fun options(vararg labels: String): List<FilterOption> =
        labels.map { FilterOption(id = slug(it), label = it) }

    val ipRating = FilterField(
        key = "ip_rating",
        label = "Ingress Protection (IP Rating)",
        type = FilterType.SearchablePopupSelect(isMultiSelect = true, allowCustomInput = true),
        options = options(
            "IPX0", "IPX1", "IPX2", "IPX3", "IPX4", "IPX5", "IPX6", "IPX7", "IPX8", "IPX9", "IPX9K",
            "IP0X",
            "IP10", "IP11", "IP12", "IP13", "IP14", "IP15", "IP16", "IP17", "IP18", "IP19", "IP19K", "IP1X",
            "IP20", "IP21", "IP22", "IP23", "IP24", "IP25", "IP26", "IP27", "IP28", "IP29", "IP29K", "IP2X",
            "IP30", "IP31", "IP32", "IP33", "IP34", "IP35", "IP36", "IP37", "IP38", "IP39", "IP39K", "IP3X",
            "IP40", "IP41", "IP42", "IP43", "IP44", "IP45", "IP46", "IP47", "IP48", "IP49", "IP49K", "IP4X",
            "IP50", "IP51", "IP52", "IP53", "IP54", "IP55", "IP56", "IP57", "IP58", "IP59", "IP59K", "IP5X",
            "IP60", "IP61", "IP62", "IP63", "IP64", "IP65", "IP66", "IP67", "IP68", "IP69", "IP69K", "IP6X"
        )
    )
}
