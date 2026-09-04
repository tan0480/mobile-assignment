package com.example.gadgetmover.screen.profile

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.gadgetmover.data.AuthRepository
import com.example.gadgetmover.data.OrderRepository
import com.example.gadgetmover.data.ReviewRepository
import com.example.gadgetmover.model.Order
import com.example.gadgetmover.screen.components.AddPhotoTile
import com.example.gadgetmover.ui.theme.BrandOrange
import kotlinx.coroutines.launch

private const val MAX_REVIEW_PHOTOS = 5
const val REVIEW_CHAR_LIMIT = 500

/**
 * Full-screen "Leave a Review" — replaces the old [com.example.gadgetmover.screen.components.ReviewDialog]
 * popup with its own route (same rationale as [ChangePasswordScreen]: a page has room for the
 * photo picker/grid that a dialog doesn't), reachable from both My Activities and Order Detail.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WriteReviewScreen(
    order: Order,
    onBackClick: () -> Unit,
    onSubmitted: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var rating by remember { mutableStateOf(5) }
    var comment by remember { mutableStateOf("") }
    var photoUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    val commentOverLimit = comment.length > REVIEW_CHAR_LIMIT

    fun submit() {
        if (commentOverLimit) return
        val reviewerId = AuthRepository.currentUser.value?.id ?: return
        busy = true
        scope.launch {
            val imageUrls = ReviewRepository.uploadPhotos(order.id, reviewerId, photoUris, context.contentResolver)
            val succeeded = ReviewRepository.submitReview(order.id, rating, comment, imageUrls)
            busy = false
            if (succeeded) {
                OrderRepository.refreshFromRemote()
                onSubmitted()
            } else {
                snackbarHostState.showSnackbar("Couldn't submit your review. Please try again.")
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Leave a Review") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = order.productImage,
                    contentDescription = null,
                    modifier = Modifier.size(52.dp).clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(order.productTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 2)
                    Text(
                        "with ${order.counterpartyName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Rating", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (1..5).forEach { star ->
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = "$star star${if (star == 1) "" else "s"}",
                        tint = if (star <= rating) BrandOrange else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .size(36.dp)
                            .clickable { rating = star }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Review", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("How was your experience? (optional)") },
                minLines = 4,
                enabled = !busy,
                isError = commentOverLimit,
                supportingText = if (commentOverLimit) {
                    { Text("Review cannot exceed $REVIEW_CHAR_LIMIT characters") }
                } else null
            )

            Spacer(modifier = Modifier.height(24.dp))
            Text("Photos (optional)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(photoUris) { uri ->
                    Box {
                        AsyncImage(
                            model = uri,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp).clip(RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = { photoUris = photoUris - uri },
                            modifier = Modifier
                                .size(22.dp)
                                .align(Alignment.TopEnd)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.6f))
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Remove photo", tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                }
                if (photoUris.size < MAX_REVIEW_PHOTOS) {
                    item {
                        AddPhotoTile(
                            maxSelectable = MAX_REVIEW_PHOTOS - photoUris.size,
                            cameraSubDir = "review_photos",
                            tileSize = 80.dp,
                            onPhotosPicked = { uris -> photoUris = (photoUris + uris).take(MAX_REVIEW_PHOTOS) },
                            onPhotoCaptured = { uri -> photoUris = (photoUris + uri).take(MAX_REVIEW_PHOTOS) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
            Button(
                onClick = ::submit,
                enabled = !busy && !commentOverLimit,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
            ) {
                if (busy) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp), color = Color.White)
                } else {
                    Text("Submit Review", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
