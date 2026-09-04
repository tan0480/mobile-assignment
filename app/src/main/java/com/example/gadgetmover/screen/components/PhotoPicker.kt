package com.example.gadgetmover.screen.components

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File

/** A fresh cache file the system Camera app can write a capture into, exposed as a content:// Uri
 * via [FileProvider] (Camera can't write to a plain file:// path). [subDir] namespaces the cache
 * folder per feature (e.g. "listing_photos", "return_request_photos") so concurrent flows never
 * collide on the same filenames. */
fun createCameraCaptureUri(context: Context, subDir: String): Uri {
    val dir = File(context.cacheDir, subDir).apply { mkdirs() }
    val file = File(dir, "IMG_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

/**
 * A "+" tile that opens a Take Photo / Choose from Gallery menu — shared by the listing wizard's
 * Photos step and the return/refund request form's evidence upload, so every photo-upload spot in
 * the app offers the same camera-or-gallery choice rather than gallery-only.
 */
@Composable
fun AddPhotoTile(
    maxSelectable: Int,
    cameraSubDir: String,
    onPhotosPicked: (List<Uri>) -> Unit,
    onPhotoCaptured: (Uri) -> Unit,
    modifier: Modifier = Modifier,
    tileSize: Dp = 84.dp
) {
    val context = LocalContext.current
    val pickPhotos = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxSelectable)
    ) { uris -> if (uris.isNotEmpty()) onPhotosPicked(uris) }

    // TakePicture() writes into a Uri we hand it up front rather than returning one itself, so
    // the Uri to report on success has to be stashed here between launch and callback.
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    val takePhoto = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCameraUri
        pendingCameraUri = null
        if (success && uri != null) onPhotoCaptured(uri)
    }
    var showMenu by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .size(tileSize)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { showMenu = true },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.AddAPhoto, contentDescription = "Add photo")
        }
        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(
                text = { Text("Take Photo") },
                onClick = {
                    showMenu = false
                    val uri = createCameraCaptureUri(context, cameraSubDir)
                    pendingCameraUri = uri
                    takePhoto.launch(uri)
                }
            )
            DropdownMenuItem(
                text = { Text("Choose from Gallery") },
                onClick = {
                    showMenu = false
                    pickPhotos.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
            )
        }
    }
}
