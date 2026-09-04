package com.example.gadgetmover.model.filter

/**
 * One of a motherboard's actual physical PCIe slots, in the shape a [PcieSlotRequirement] needs to
 * be checked against. A product's stored PCIe slots are themselves a `List<PcieSlotRequirement>`
 * (the seller fills it in with the same [FilterType.PcieSlotBuilder] widget a buyer's search
 * filter uses), so [toActualPcieSlot] bridges the two rather than anything constructing this
 * directly — see [satisfiesAllPcieSlotRequirements] below and [CategoryFilterMatching].
 */
data class ActualPcieSlot(
    val laneWidth: PcieLaneWidth,
    val generation: PcieGeneration
)

/** True if [slot] alone can satisfy [this] requirement. */
fun PcieSlotRequirement.isSatisfiedBy(slot: ActualPcieSlot): Boolean =
    laneWidth == slot.laneWidth && generation == slot.generation

/**
 * True only if every requirement in [this] list can be matched to a *different* slot in [slots] —
 * order-independent, but one physical slot can never be double-booked to satisfy two requirements
 * at once ("PCIe 5.0 x16 AND PCIe 3.0 x1" needs two distinct slots). Same maximum-bipartite-matching
 * approach as `CameraRequirementMatching.satisfiesAllCameraRequirements` — requirement counts are
 * always small (at most [MAX_PCIE_SLOT_REQUIREMENTS]), so a plain augmenting-path search is fine.
 */
fun List<PcieSlotRequirement>.satisfiesAllPcieSlotRequirements(slots: List<ActualPcieSlot>): Boolean {
    if (isEmpty()) return true
    val matchedSlotForRequirement = IntArray(size) { -1 }

    fun tryAssign(reqIndex: Int, visited: BooleanArray): Boolean {
        for (slotIndex in slots.indices) {
            if (visited[slotIndex]) continue
            if (!this[reqIndex].isSatisfiedBy(slots[slotIndex])) continue
            visited[slotIndex] = true
            val currentOwner = matchedSlotForRequirement.indexOf(slotIndex)
            if (currentOwner == -1 || tryAssign(currentOwner, visited)) {
                matchedSlotForRequirement[reqIndex] = slotIndex
                return true
            }
        }
        return false
    }

    var matchedCount = 0
    for (reqIndex in indices) {
        if (tryAssign(reqIndex, BooleanArray(slots.size))) matchedCount++
    }
    return matchedCount == size
}

/** A product's stored PCIe slots are also a `List<PcieSlotRequirement>` — the listing wizard reuses the same [FilterType.PcieSlotBuilder] widget a buyer's search filter uses — so this is a trivial 1:1 shape bridge for [satisfiesAllPcieSlotRequirements]. */
fun PcieSlotRequirement.toActualPcieSlot(): ActualPcieSlot = ActualPcieSlot(laneWidth = laneWidth, generation = generation)
