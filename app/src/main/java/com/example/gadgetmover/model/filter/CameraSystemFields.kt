package com.example.gadgetmover.model.filter

/**
 * The "Camera System" section shared identically by Phones and Tablets — a repeatable "Add
 * Camera" requirement builder ([FilterType.CameraSystemBuilder]) rather than a fixed field per
 * camera slot. Replaces the old [FieldDependency]-gated fixed checkbox list this app used before
 * (each camera role's spec fields only appearing once that role's checkbox was ticked), which
 * couldn't express "I want two telephoto-role cameras" or avoid a made-up "how many cameras"
 * count. See [CameraRequirement] for what one requirement slot holds.
 */
object CameraSystemFields {
    val cameraSystem = FilterField(
        key = "camera_system",
        label = "Camera System",
        type = FilterType.CameraSystemBuilder
    )

    val fields: List<FilterField> = listOf(cameraSystem)
}
