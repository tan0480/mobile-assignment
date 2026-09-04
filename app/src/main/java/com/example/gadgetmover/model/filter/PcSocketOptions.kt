package com.example.gadgetmover.model.filter

/** CPU socket labels shared across [CpuFilterSchema], [MotherboardFilterSchema], and [CpuCoolerFilterSchema] so "LGA 1700" means the same option id everywhere a buyer cross-checks compatibility. */
object PcSocketOptions {
    /** Ids assigned explicitly rather than derived from each label — a generic slug would collapse "FM2" and "FM2+" (or "AM3+"/"AM3") to the same id once the "+" is stripped. */
    val options: List<FilterOption> = listOf(
        FilterOption("lga_1155", "LGA 1155"),
        FilterOption("lga_1150", "LGA 1150"),
        FilterOption("lga_1151", "LGA 1151"),
        FilterOption("lga_1200", "LGA 1200"),
        FilterOption("lga_1700", "LGA 1700"),
        FilterOption("lga_1851", "LGA 1851"),
        FilterOption("lga_2011", "LGA 2011"),
        FilterOption("lga_2011_3", "LGA 2011-3"),
        FilterOption("lga_2066", "LGA 2066"),
        FilterOption("lga_3647", "LGA 3647"),
        FilterOption("lga_4189", "LGA 4189"),
        FilterOption("lga_4677", "LGA 4677"),
        FilterOption("lga_4710", "LGA 4710"),
        FilterOption("lga_7529", "LGA 7529"),
        FilterOption("am3_plus", "AM3+"),
        FilterOption("fm1", "FM1"),
        FilterOption("fm2", "FM2"),
        FilterOption("fm2_plus", "FM2+"),
        FilterOption("am1", "AM1"),
        FilterOption("am4", "AM4"),
        FilterOption("am5", "AM5"),
        FilterOption("tr4", "TR4"),
        FilterOption("strx4", "sTRX4"),
        FilterOption("swrx8", "sWRX8"),
        FilterOption("str5", "sTR5"),
        FilterOption("sp3", "SP3"),
        FilterOption("sp5", "SP5"),
        FilterOption("other", "Other")
    )
}
