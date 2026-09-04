package com.example.gadgetmover.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/** Opens [url] in the user's browser (or whichever app handles it), same defensive fallback pattern as [openInGoogleMaps]. */
fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: ActivityNotFoundException) {
        // No app can handle it — nothing further we can do.
    }
}
