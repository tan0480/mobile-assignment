package com.example.gadgetmover.model.filter

import kotlinx.serialization.Serializable

/** Physical lane width of one PCIe slot. */
@Serializable
enum class PcieLaneWidth(val label: String) {
    X1("x1"), X4("x4"), X8("x8"), X16("x16"), OTHER("Other")
}

/** PCIe generation/speed of one slot. */
@Serializable
enum class PcieGeneration(val label: String) {
    GEN3("3.0"), GEN4("4.0"), GEN5("5.0"), OTHER("Other")
}

/** Hard cap enforced by the "+ Add PCIe Slot" button. */
const val MAX_PCIE_SLOT_REQUIREMENTS = 8

/**
 * One independent "the board must have a slot like this" requirement inside a
 * [FilterFieldValue.PcieSlotRequirements] list — e.g. a search for "PCIe 5.0 x16 AND PCIe 3.0 x1"
 * is two of these, and a board must satisfy every one of them (see `PcieSlotMatching`).
 */
@Serializable
data class PcieSlotRequirement(
    val id: String,
    val laneWidth: PcieLaneWidth = PcieLaneWidth.X16,
    val generation: PcieGeneration = PcieGeneration.GEN4
)

/** Short one-line description for a collapsed requirement card, e.g. "PCIe 4.0 x16". */
fun PcieSlotRequirement.summaryText(): String = "PCIe ${generation.label} ${laneWidth.label}"
