package com.example.gadgetmover.screen.explore

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.gadgetmover.model.LocationRadiusFilter
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.rememberCameraPositionState
import com.example.gadgetmover.ui.theme.BrandOrange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Locale

private val DEFAULT_CENTER = LatLng(3.1390, 101.6869)
private const val MIN_RADIUS_KM = 1.0f
private const val MAX_RADIUS_KM = 50.0f

/**
 * Carousell-style "browse meet-up listings near me" radius picker: the map center is fixed (the
 * map itself moves underneath it, same drag pattern as [com.example.gadgetmover.screen.components.LocationPickerScreen]),
 * a semi-transparent circle overlay grows/shrinks live with the radius slider, and confirming
 * hands back a [LocationRadiusFilter] that [com.example.gadgetmover.data.ProductRepository.search]
 * uses to narrow meet-up listings by [com.example.gadgetmover.util.haversineDistanceKm].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationRadiusFilterScreen(
    initial: LocationRadiusFilter?,
    onBackClick: () -> Unit,
    onConfirm: (LocationRadiusFilter) -> Unit,
    onClear: () -> Unit
) {
    val context = LocalContext.current
    val initialCenter = initial?.let { LatLng(it.latitude, it.longitude) } ?: DEFAULT_CENTER

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialCenter, 13f)
    }
    var radiusKm by remember { mutableFloatStateOf(initial?.radiusKm ?: 5.0f) }
    var resolvedAddress by remember { mutableStateOf(initial?.address ?: "") }
    var isResolving by remember { mutableStateOf(false) }
    var isLocating by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) moveToCurrentLocation(context, cameraPositionState) { isLocating = it }
    }

    // Only auto-center on GPS when the buyer hasn't already got a saved center from last time —
    // and only if location permission is already granted, so opening this screen never itself
    // triggers a permission prompt.
    LaunchedEffect(Unit) {
        if (initial == null &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        ) {
            moveToCurrentLocation(context, cameraPositionState) { isLocating = it }
        }
    }

    LaunchedEffect(cameraPositionState.isMoving) {
        if (!cameraPositionState.isMoving) {
            delay(300)
            isResolving = true
            resolvedAddress = withContext(Dispatchers.IO) {
                try {
                    @Suppress("DEPRECATION")
                    Geocoder(context, Locale.getDefault())
                        .getFromLocation(cameraPositionState.position.target.latitude, cameraPositionState.position.target.longitude, 1)
                        ?.firstOrNull()?.getAddressLine(0)
                } catch (e: Exception) {
                    null
                } ?: "Unnamed location"
            }
            isResolving = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Browse near me") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (initial != null) {
                        TextButton(onClick = onClear) { Text("Clear") }
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState
            ) {
                Circle(
                    center = cameraPositionState.position.target,
                    radius = radiusKm * 1000.0,
                    fillColor = BrandOrange.copy(alpha = 0.20f),
                    strokeColor = BrandOrange,
                    strokeWidth = 3f
                )
            }

            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (isResolving) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                    Text(
                        "Browsing from: ${resolvedAddress.ifBlank { "Move the map to pick a spot" }}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            FloatingActionButton(
                onClick = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        moveToCurrentLocation(context, cameraPositionState) { isLocating = it }
                    } else {
                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .padding(bottom = 180.dp)
            ) {
                if (isLocating) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Icon(Icons.Filled.MyLocation, contentDescription = "Use my location")
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Browsing radius: %.1f km".format(radiusKm),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Slider(
                            value = radiusKm,
                            onValueChange = { radiusKm = it },
                            valueRange = MIN_RADIUS_KM..MAX_RADIUS_KM,
                            colors = androidx.compose.material3.SliderDefaults.colors(thumbColor = BrandOrange, activeTrackColor = BrandOrange)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.align(Alignment.End)) {
                    FloatingActionButton(
                        onClick = {
                            val target = cameraPositionState.position.target
                            onConfirm(
                                LocationRadiusFilter(
                                    latitude = target.latitude,
                                    longitude = target.longitude,
                                    address = resolvedAddress,
                                    radiusKm = radiusKm
                                )
                            )
                        },
                        containerColor = BrandOrange
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = "Confirm radius")
                    }
                }
            }
        }
    }
}

private fun moveToCurrentLocation(
    context: android.content.Context,
    cameraPositionState: com.google.maps.android.compose.CameraPositionState,
    onLoading: (Boolean) -> Unit
) {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return
    onLoading(true)
    LocationServices.getFusedLocationProviderClient(context).lastLocation
        .addOnSuccessListener { location ->
            onLoading(false)
            if (location != null) {
                cameraPositionState.position = CameraPosition.fromLatLngZoom(LatLng(location.latitude, location.longitude), 13f)
            }
        }
        .addOnFailureListener { onLoading(false) }
}
