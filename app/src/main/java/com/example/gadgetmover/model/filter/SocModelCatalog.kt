package com.example.gadgetmover.model.filter

/**
 * The SoC model catalogue behind [PhoneFilterSchema.socModel] and [TabletFilterSchema.socModel] —
 * shared here since both narrow to the picked SoC Brand's own chips (same mechanism, same real
 * chip families). Apple is split into [appleASeries] and [appleMSeries] rather than one combined
 * list: Phones only ever narrow to A-series chips, while Tablets narrow to a brand id that covers
 * both series (iPads ship both), so each schema composes the split it actually needs instead of
 * every phone listing being able to "pick" an M-series Mac chip.
 */
object SocModelCatalog {
    private fun slug(label: String): String =
        label.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')

    private fun options(vararg labels: String): List<FilterOption> =
        labels.map { FilterOption(id = slug(it), label = it) }

    val qualcommSnapdragon: List<FilterOption> = options(
        "Snapdragon 820", "Snapdragon 821", "Snapdragon 835", "Snapdragon 845", "Snapdragon 855", "Snapdragon 855+",
        "Snapdragon 865", "Snapdragon 865+", "Snapdragon 870", "Snapdragon 888", "Snapdragon 888+",
        "Snapdragon 8 Gen 1", "Snapdragon 8+ Gen 1", "Snapdragon 8 Gen 2", "Snapdragon 8 Gen 3", "Snapdragon 8s Gen 3",
        "Snapdragon 8 Elite", "Snapdragon 8 Elite Gen 5",
        "Snapdragon 7 Gen 1", "Snapdragon 7+ Gen 2", "Snapdragon 7 Gen 3", "Snapdragon 7+ Gen 3",
        "Snapdragon 7s Gen 2", "Snapdragon 7s Gen 3",
        "Snapdragon 6 Gen 1", "Snapdragon 6 Gen 3", "Snapdragon 6s Gen 3",
        "Snapdragon 4 Gen 1", "Snapdragon 4 Gen 2",
        "Snapdragon X Elite", "Snapdragon X Plus"
    )

    /** Phone-native chips — [TabletFilterSchema] adds [appleMSeries] on top of this for its combined "Apple M-Series / A-Series" brand. */
    val appleASeries: List<FilterOption> = options(
        "Apple A10 Fusion", "Apple A10X Fusion", "Apple A11 Bionic",
        "Apple A12 Bionic", "Apple A12X Bionic", "Apple A12Z Bionic",
        "Apple A13 Bionic", "Apple A14 Bionic", "Apple A15 Bionic", "Apple A16 Bionic",
        "Apple A17 Pro", "Apple A18", "Apple A18 Pro", "Apple A19", "Apple A19 Pro"
    )

    val appleMSeries: List<FilterOption> = options(
        "Apple M1", "Apple M1 Pro", "Apple M1 Max", "Apple M1 Ultra",
        "Apple M2", "Apple M2 Pro", "Apple M2 Max", "Apple M2 Ultra",
        "Apple M3", "Apple M3 Pro", "Apple M3 Max",
        "Apple M4", "Apple M4 Pro", "Apple M4 Max", "Apple M5"
    )

    /** Both the Dimensity and Helio product lines — the app's brand catalogue only exposes one MediaTek option ("MediaTek Dimensity"), so both live in the one bucket that narrows to it. */
    val mediatekDimensity: List<FilterOption> = options(
        "Dimensity 700", "Dimensity 720", "Dimensity 800", "Dimensity 800U", "Dimensity 810", "Dimensity 820",
        "Dimensity 900", "Dimensity 920", "Dimensity 1000", "Dimensity 1000+", "Dimensity 1100", "Dimensity 1200",
        "Dimensity 1300", "Dimensity 7050", "Dimensity 7200", "Dimensity 7300", "Dimensity 7400",
        "Dimensity 8000", "Dimensity 8020", "Dimensity 8050", "Dimensity 8100", "Dimensity 8200", "Dimensity 8250",
        "Dimensity 8300", "Dimensity 8350", "Dimensity 8400",
        "Dimensity 9000", "Dimensity 9000+", "Dimensity 9200", "Dimensity 9200+", "Dimensity 9300", "Dimensity 9300+",
        "Dimensity 9400", "Dimensity 9400+", "Dimensity 9500",
        "Helio P20", "Helio P22", "Helio P23", "Helio P25", "Helio P30", "Helio P35", "Helio P60", "Helio P65",
        "Helio P70", "Helio P90",
        "Helio G25", "Helio G35", "Helio G70", "Helio G80", "Helio G85", "Helio G88", "Helio G90T", "Helio G95",
        "Helio G96", "Helio G99",
        "Helio X20", "Helio X23", "Helio X25", "Helio X27", "Helio X30"
    )

    val samsungExynos: List<FilterOption> = options(
        "Exynos 7570", "Exynos 7870", "Exynos 7880", "Exynos 7885", "Exynos 8890", "Exynos 8895",
        "Exynos 9609", "Exynos 9610", "Exynos 9611",
        "Exynos 980", "Exynos 1080", "Exynos 1280", "Exynos 1330", "Exynos 1380", "Exynos 1480", "Exynos 1580",
        "Exynos 2100", "Exynos 2200", "Exynos 2400", "Exynos 2500"
    )

    val googleTensor: List<FilterOption> = options(
        "Google Tensor", "Google Tensor G2", "Google Tensor G3", "Google Tensor G4", "Google Tensor G5"
    )

    val huaweiKirin: List<FilterOption> = options(
        "Kirin 650", "Kirin 655", "Kirin 658", "Kirin 659",
        "Kirin 710", "Kirin 710A", "Kirin 710F", "Kirin 810", "Kirin 820",
        "Kirin 9000", "Kirin 9000E", "Kirin 9000S", "Kirin 9000SL", "Kirin 9010", "Kirin 9020",
        "Kirin 950", "Kirin 955", "Kirin 960", "Kirin 970", "Kirin 980", "Kirin 985", "Kirin 990", "Kirin 990 5G"
    )
}
