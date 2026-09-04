package com.example.gadgetmover.screen.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.gadgetmover.data.AuthRepository
import com.example.gadgetmover.data.ReviewRepository
import com.example.gadgetmover.model.Review
import com.example.gadgetmover.screen.components.FullScreenImageViewer
import com.example.gadgetmover.ui.theme.AccentLime
import com.example.gadgetmover.ui.theme.BrandBlueDark
import com.example.gadgetmover.util.formatDisplayDate
import kotlinx.coroutines.launch

/**
 * [sellerId] null means "my own reviews as a seller" (the ProfileQuickAction.REVIEWS entry point)
 * — an explicit id is [SellerProfileScreen]'s "See reviews" entry into someone else's reviews.
 * Reply is only offered when the viewer *is* the reviewed seller, whichever way this was reached.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewsScreen(onBackClick: () -> Unit, sellerId: String? = null) {
    val reviews = ReviewRepository.reviews
    val currentUserId = AuthRepository.currentUser.value?.id
    val targetSellerId = sellerId ?: currentUserId
    val isOwnProfile = targetSellerId != null && targetSellerId == currentUserId
    val scope = rememberCoroutineScope()
    var previewImageUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(targetSellerId) {
        targetSellerId?.let { ReviewRepository.refreshForSeller(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reviews") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Star, contentDescription = null, tint = AccentLime, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "%.1f".format(ReviewRepository.averageRating),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "based on ${reviews.size} review${if (reviews.size == 1) "" else "s"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(reviews, key = { it.id }) { review ->
                    ReviewCard(
                        review = review,
                        canReply = isOwnProfile,
                        onImageClick = { previewImageUrl = it },
                        onReply = { reply ->
                            scope.launch {
                                if (ReviewRepository.replyToReview(review.id, reply)) {
                                    targetSellerId?.let { ReviewRepository.refreshForSeller(it) }
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    previewImageUrl?.let { url ->
        FullScreenImageViewer(imageUrl = url, onDismiss = { previewImageUrl = null })
    }
}

@Composable
private fun ReviewCard(review: Review, canReply: Boolean, onImageClick: (String) -> Unit, onReply: (String) -> Unit) {
    var showReplyField by remember(review.id) { mutableStateOf(false) }
    var replyText by remember(review.id) { mutableStateOf("") }

    Card(shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(1.dp)) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = review.reviewerAvatar,
                    contentDescription = review.reviewerName,
                    modifier = Modifier.size(36.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(review.reviewerName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(formatDisplayDate(review.date), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Star, contentDescription = null, tint = BrandBlueDark, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(review.rating.toString(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(review.comment, style = MaterialTheme.typography.bodyMedium)
            review.relatedProductTitle?.let {
                Spacer(modifier = Modifier.height(6.dp))
                Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }

            if (review.imageUrls.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(review.imageUrls) { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onImageClick(url) },
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            val sellerReply = review.sellerReply
            if (!sellerReply.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(10.dp)
                ) {
                    Text("Seller's reply", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(sellerReply, style = MaterialTheme.typography.bodySmall)
                }
                if (canReply) {
                    Spacer(modifier = Modifier.height(6.dp))
                    TextButton(onClick = { replyText = sellerReply; showReplyField = true }) { Text("Edit reply") }
                }
            } else if (canReply) {
                Spacer(modifier = Modifier.height(6.dp))
                if (!showReplyField) {
                    TextButton(onClick = { showReplyField = true }) { Text("Reply") }
                }
            }

            if (canReply && showReplyField) {
                val replyOverLimit = replyText.length > REVIEW_CHAR_LIMIT
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = replyText,
                    onValueChange = { replyText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Write a public reply") },
                    minLines = 2,
                    isError = replyOverLimit,
                    supportingText = if (replyOverLimit) {
                        { Text("Review cannot exceed $REVIEW_CHAR_LIMIT characters") }
                    } else null
                )
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = { showReplyField = false; replyText = sellerReply.orEmpty() }) { Text("Cancel") }
                    TextButton(
                        enabled = !replyOverLimit,
                        onClick = {
                            onReply(replyText)
                            showReplyField = false
                        }
                    ) { Text("Post") }
                }
            }
        }
    }
}
