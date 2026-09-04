package com.example.gadgetmover.model.filter

/** CPU socket labels shared across [CpuFilterSchema], [MotherboardFilterSchema], and [CpuCoolerFilterSchema] so "LGA1700" means the same option id everywhere a buyer cross-checks compatibility. */
object PcSocketOptions {
    val labels = arrayOf(
        "LGA1851", "LGA1700", "LGA1200", "LGA1151",
        "AM5", "AM4", "sTRX4", "sWRX8", "Other"
    )
}
