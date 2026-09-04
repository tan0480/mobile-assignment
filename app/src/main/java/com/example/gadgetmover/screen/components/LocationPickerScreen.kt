package com.example.gadgetmover.screen.components

import android.Manifest
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PointOfInterest
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Locale

/** Center of Kuala Lumpur — used when no initial coordinates are given and location permission isn't granted. */
private val DEFAULT_CENTER = LatLng(3.1390, 101.6869)

/** What [LocationPickerScreen] hands back once confirmed. [suggestedName] is the place's own name (e.g. "Shell") when it was picked via search, or null when the pin was just dragged/dropped manually — [ListingWizardScreen] uses it to pre-fill the meet-up spot's name. */
data class PickedLocation(
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val suggestedName: String? = null
)

/** One row of the search dropdown, built from a [Geocoder] forward-lookup [Address]. */
private data class SearchResult(val primary: String, val secondary: String, val latLng: LatLng)

private fun Address.toSearchResult(): SearchResult {
    val full = addressLineWithoutCountry()
    val name = featureName
    val primary = if (!name.isNullOrBlank() && !full.startsWith(name)) name else full
    return SearchResult(primary = primary, secondary = full, latLng = LatLng(latitude, longitude))
}

/** [Address.getAddressLine] includes the country name as its final component — stripped here since a user picking a spot on the map already knows what country they're in and doesn't need it repeated in every address string. */
private fun Address.addressLineWithoutCountry(): String {
    val full = getAddressLine(0) ?: return "Unnamed location"
    val country = countryName?.takeIf { it.isNotBlank() } ?: return full
    if (!full.endsWith(country)) return full
    return full.removeSuffix(country).trimEnd().trimEnd(',').trimEnd()
}

/**
 * A modern "drag the map, pin stays centered" location picker (spec §8): the pin is fixed at the
 * screen center and the map moves underneath it; the center is reverse-geocoded via Android's
 * built-in [Geocoder] whenever the camera settles. A search bar additionally lets the user jump
 * straight to a named place by forward-geocoding the query through the same [Geocoder] (no Maps
 * API key or Places billing needed for either) — selecting a result just moves the camera there,
 * so the same drag-to-fine-tune/reverse-geocode flow still finalizes the address. Returns the
 * confirmed location via [onConfirm].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPickerScreen(
    initialLatitude: Double?,
    initialLongitude: Double?,
    onBackClick: () -> Unit,
    onConfirm: (PickedLocation) -> Unit
) {
    val context = LocalContext.current
    val initialCenter = if (initialLatitude != null && initialLongitude != null) {
        LatLng(initialLatitude, initialLongitude)
    } else DEFAULT_CENTER

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialCenter, 16f)
    }
    var resolvedAddress by remember { mutableStateOf("") }
    var isResolving by remember { mutableStateOf(false) }
    var isLocating by remember { mutableStateOf(false) }
    var suggestedName by remember { mutableStateOf<String?>(null) }

    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<SearchResult>>(emptyList()) }
    var searchError by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) moveToCurrentLocation(context, cameraPositionState) { isLocating = it }
    }

    // Only auto-center on the user's current position when the caller didn't already hand in a
    // specific spot (e.g. editing an existing address) — and only if location permission is
    // already granted, so opening this screen never itself triggers a permission prompt.
    LaunchedEffect(Unit) {
        if (initialLatitude == null && initialLongitude == null &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        ) {
            moveToCurrentLocation(context, cameraPositionState) { isLocating = it }
        }
    }

    // Debounced forward-geocode lookup as the user types. Uses the same on-device Geocoder
    // already used below for reverse-geocoding — no API key or Places billing needed, at the
    // cost of no live ranked autocomplete and weaker landmark/business-name coverage.
    LaunchedEffect(searchQuery) {
        if (searchQuery.isBlank()) {
            searchResults = emptyList()
            searchError = false
            return@LaunchedEffect
        }
        delay(300)
        val results = withContext(Dispatchers.IO) {
            try {
                @Suppress("DEPRECATION")
                Geocoder(context, Locale.getDefault()).getFromLocationName(searchQuery, 5)
            } catch (e: Exception) {
                Log.e("LocationPicker", "Geocoder search failed", e)
                null
            }
        }
        if (results == null) {
            searchResults = emptyList()
            searchError = true
        } else {
            searchResults = results.map { it.toSearchResult() }
            searchError = false
        }
    }

    // Reverse-geocode whenever the camera finishes moving — debounced slightly so a drag
    // gesture doesn't fire a Geocoder lookup on every intermediate frame. Also fires after a
    // search selection re-centers the camera, so the address line stays sourced from one place.
    LaunchedEffect(cameraPositionState.isMoving) {
        if (!cameraPositionState.isMoving) {
            delay(300)
            isResolving = true
            resolvedAddress = reverseGeocode(context, cameraPositionState.position.target)
            isResolving = false
        }
    }

    fun selectResult(result: SearchResult) {
        searchQuery = ""
        searchResults = emptyList()
        suggestedName = result.primary
        cameraPositionState.position = CameraPosition.fromLatLngZoom(result.latLng, 17f)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pick a location") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                // Tapping a labeled landmark (a POI icon baked into the map tiles — malls,
                // campuses, transit stops) is a free event the base Maps SDK already fires;
                // it needs no Places API call. Re-center on it exactly like selectPrediction
                // does for a search result, so the fixed center pin lands on that spot and the
                // existing camera-settled effect reverse-geocodes it.
                onPOIClick = { poi: PointOfInterest ->
                    suggestedName = poi.name
                    cameraPositionState.position = CameraPosition.fromLatLngZoom(poi.latLng, 17f)
                }
            )

            // Fixed center pin — the map moves underneath it, the standard modern e-commerce
            // address-picker pattern, rather than a draggable marker.
            Icon(
                Icons.Filled.PinDrop,
                contentDescription = "Selected location",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(bottom = 32.dp)
                    .size(40.dp)
            )

            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it; suggestedName = null },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search a mall, landmark, business...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
                if (searchError) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Search is unavailable right now — you can still drag the map to pick a spot.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(10.dp)
                    )
                } else if (searchResults.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(modifier = Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                            searchResults.forEachIndexed { index, result ->
                                if (index > 0) HorizontalDivider()
                                SearchResultRow(result = result, onClick = { selectResult(result) })
                            }
                        }
                    }
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
                    .padding(bottom = 140.dp)
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isResolving) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                    Text(
                        resolvedAddress.ifBlank { "Move the map to pick a location" },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        val target = cameraPositionState.position.target
                        onConfirm(PickedLocation(target.latitude, target.longitude, resolvedAddress, suggestedName))
                    },
                    enabled = resolvedAddress.isNotBlank() && !isResolving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Confirm this location", style = MaterialTheme.typography.titleMedium, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(result: SearchResult, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(
            result.primary,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (result.secondary.isNotBlank() && result.secondary != result.primary) {
            Text(
                result.secondary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
                cameraPositionState.position = CameraPosition.fromLatLngZoom(LatLng(location.latitude, location.longitude), 16f)
            }
        }
        .addOnFailureListener { onLoading(false) }
}

private suspend fun reverseGeocode(context: android.content.Context, target: LatLng): String = withContext(Dispatchers.IO) {
    try {
        @Suppress("DEPRECATION")
        val results = Geocoder(context, Locale.getDefault()).getFromLocation(target.latitude, target.longitude, 1)
        results?.firstOrNull()?.addressLineWithoutCountry() ?: "Unnamed location (${"%.5f".format(target.latitude)}, ${"%.5f".format(target.longitude)})"
    } catch (e: Exception) {
        "Unnamed location (${"%.5f".format(target.latitude)}, ${"%.5f".format(target.longitude)})"
    }
}
