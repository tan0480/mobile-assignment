package com.example.gadgetmover.screen.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val OUTPUT_SIZE_PX = 512
private const val JPEG_QUALITY = 85
private const val MAX_DECODE_DIMENSION = 1600
private val CROP_VIEWPORT = 280.dp

/**
 * Full-screen "pinch to zoom, drag to reposition" circular crop step between picking a photo
 * (camera or gallery, via [AddPhotoTile]'s same take-photo/choose-from-gallery pattern) and
 * actually uploading it as an avatar. The circle is only a visual crop guide — the raster output
 * is a plain square JPEG, since every place this app shows an avatar already clips it into a
 * circle at display time, so a square source is all a circular *display* ever needs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvatarCropDialog(
    sourceUri: Uri,
    onCancel: () -> Unit,
    onCropped: (ByteArray) -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    var bitmap by remember(sourceUri) { mutableStateOf<Bitmap?>(null) }
    var loadFailed by remember(sourceUri) { mutableStateOf(false) }
    var scale by remember(sourceUri) { mutableFloatStateOf(1f) }
    var offset by remember(sourceUri) { mutableStateOf(Offset.Zero) }

    LaunchedEffect(sourceUri) {
        val decoded = decodeOrientedBitmap(context, sourceUri)
        if (decoded == null) loadFailed = true else bitmap = decoded
    }

    Dialog(onDismissRequest = onCancel, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Adjust Photo") },
                    navigationIcon = {
                        IconButton(onClick = onCancel) {
                            Icon(Icons.Filled.Close, contentDescription = "Cancel")
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                val bmp = bitmap
                Box(
                    modifier = Modifier.size(CROP_VIEWPORT),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        bmp != null -> {
                            val viewportPx = with(density) { CROP_VIEWPORT.toPx() }
                            val baseScale = viewportPx / min(bmp.width, bmp.height).toFloat()
                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(sourceUri) {
                                        detectTransformGestures { _, pan, zoom, _ ->
                                            val newScale = (scale * zoom).coerceIn(1f, 4f)
                                            val totalScale = baseScale * newScale
                                            val maxOffsetX = max(0f, (bmp.width * totalScale - viewportPx) / 2f)
                                            val maxOffsetY = max(0f, (bmp.height * totalScale - viewportPx) / 2f)
                                            scale = newScale
                                            offset = Offset(
                                                (offset.x + pan.x).coerceIn(-maxOffsetX, maxOffsetX),
                                                (offset.y + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                                            )
                                        }
                                    }
                            ) {
                                val totalScale = baseScale * scale
                                val drawWidth = bmp.width * totalScale
                                val drawHeight = bmp.height * totalScale
                                val left = (size.width - drawWidth) / 2f + offset.x
                                val top = (size.height - drawHeight) / 2f + offset.y
                                drawImage(
                                    image = bmp.asImageBitmap(),
                                    dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
                                    dstSize = IntSize(drawWidth.roundToInt(), drawHeight.roundToInt())
                                )

                                val radius = size.minDimension / 2f
                                val scrimPath = Path().apply {
                                    addRect(Rect(0f, 0f, size.width, size.height))
                                    addOval(Rect(center = center, radius = radius))
                                    fillType = PathFillType.EvenOdd
                                }
                                drawPath(scrimPath, color = Color.Black.copy(alpha = 0.55f))
                                drawCircle(color = Color.White, radius = radius, style = Stroke(width = 2.dp.toPx()))
                            }
                        }
                        loadFailed -> Text(
                            "Couldn't open that photo",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        else -> CircularProgressIndicator()
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Pinch to zoom, drag to reposition",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = {
                        val bmpForCrop = bitmap ?: return@Button
                        val viewportPx = with(density) { CROP_VIEWPORT.toPx() }
                        onCropped(cropToJpeg(bmpForCrop, viewportPx, scale, offset))
                    },
                    enabled = bitmap != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(52.dp)
                ) {
                    Text("Use Photo", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

/** Decodes [uri] downsampled to at most [MAX_DECODE_DIMENSION] on its longer side, rotated upright per its own EXIF orientation tag (camera captures — gallery picks are already upright). */
private suspend fun decodeOrientedBitmap(context: android.content.Context, uri: Uri): Bitmap? =
    withContext(Dispatchers.IO) {
        try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@withContext null

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            var sampleSize = 1
            while (bounds.outWidth / sampleSize > MAX_DECODE_DIMENSION || bounds.outHeight / sampleSize > MAX_DECODE_DIMENSION) {
                sampleSize *= 2
            }
            val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sampleSize })
                ?: return@withContext null

            val orientation = ExifInterface(ByteArrayInputStream(bytes))
                .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            val rotationDegrees = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
            if (rotationDegrees == 0f) {
                decoded
            } else {
                val matrix = Matrix().apply { postRotate(rotationDegrees) }
                Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            }
        } catch (e: Exception) {
            null
        }
    }

/** Mirrors the draw-time math in [AvatarCropDialog]'s `Canvas` to find which square region of [bitmap] is currently visible inside the circular viewport, crops exactly that region, and scales it down to a fixed [OUTPUT_SIZE_PX] JPEG. */
private fun cropToJpeg(bitmap: Bitmap, viewportPx: Float, scale: Float, offset: Offset): ByteArray {
    val baseScale = viewportPx / min(bitmap.width, bitmap.height).toFloat()
    val totalScale = baseScale * scale
    val srcSize = (viewportPx / totalScale)
        .coerceAtMost(min(bitmap.width, bitmap.height).toFloat())

    val srcLeft = ((bitmap.width - srcSize) / 2f - offset.x / totalScale)
        .coerceIn(0f, (bitmap.width - srcSize).coerceAtLeast(0f))
    val srcTop = ((bitmap.height - srcSize) / 2f - offset.y / totalScale)
        .coerceIn(0f, (bitmap.height - srcSize).coerceAtLeast(0f))

    val cropped = Bitmap.createBitmap(
        bitmap,
        srcLeft.roundToInt(),
        srcTop.roundToInt(),
        srcSize.roundToInt().coerceAtLeast(1).coerceAtMost(bitmap.width - srcLeft.roundToInt()),
        srcSize.roundToInt().coerceAtLeast(1).coerceAtMost(bitmap.height - srcTop.roundToInt())
    )
    val output = Bitmap.createScaledBitmap(cropped, OUTPUT_SIZE_PX, OUTPUT_SIZE_PX, true)
    val stream = ByteArrayOutputStream()
    output.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream)
    return stream.toByteArray()
}
