package com.example.gadgetmover.screen.components

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember

/**
 * Requests every runtime permission the app can use, once, as soon as it enters composition —
 * rather than only asking mid-flow when a feature (the location picker's "use my current
 * location", a future push notification) first needs it. The system dialog itself never
 * reappears for a permission that's already granted or permanently denied, so re-running this
 * on every cold launch is harmless; a feature that's denied here still falls back gracefully
 * when actually used (e.g. the location picker keeps working with a fixed map center).
 *
 * Deliberately doesn't request any storage/media permission: photo selection goes through the
 * system Photo Picker ([androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia],
 * see ListingWizardScreen), which needs no runtime permission at all on this app's minSdk.
 */
@Composable
fun RequestStartupPermissions() {
    val permissions = remember {
        buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = {}
    )
    LaunchedEffect(Unit) {
        launcher.launch(permissions.toTypedArray())
    }
}
