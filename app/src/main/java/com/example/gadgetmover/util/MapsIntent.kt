package com.example.gadgetmover.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.gadgetmover.model.MeetupLocation

/**
 * Opens [location] in the Google Maps app via a plain `geo:` Intent — no Maps SDK or API key
 * needed for this, unlike the in-app picker in `LocationPickerScreen`. Falls back to a browser
 * Google Maps URL if no maps app can handle the `geo:` intent (e.g. a device without Google Play
 * Services, or simply no maps app installed).
 */
fun openInGoogleMaps(context: Context, location: MeetupLocation) {
    val label = Uri.encode(location.name)
    val geoIntent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("geo:${location.latitude},${location.longitude}?q=${location.latitude},${location.longitude}($label)")
    ).apply {
        setPackage("com.google.android.apps.maps")
    }
    try {
        context.startActivity(geoIntent)
    } catch (e: ActivityNotFoundException) {
        val browserIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://www.google.com/maps/search/?api=1&query=${location.latitude},${location.longitude}")
        )
        try {
            context.startActivity(browserIntent)
        } catch (e2: ActivityNotFoundException) {
            // No app on the device can open a URL at all — nothing more we can do.
        }
    }
}
