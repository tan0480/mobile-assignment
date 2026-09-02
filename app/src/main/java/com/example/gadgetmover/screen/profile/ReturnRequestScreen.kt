package com.example.gadgetmover.screen.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import android.net.Uri
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.gadgetmover.data.ReturnRequestRepository
import com.example.gadgetmover.model.BuyOrder
import com.example.gadgetmover.model.Order
import com.example.gadgetmover.model.ReturnMethod
import com.example.gadgetmover.model.ReturnRequest
import com.example.gadgetmover.model.ReturnRequestStatus
import com.example.gadgetmover.model.ReturnRequestType
import com.example.gadgetmover.model.returnRequestReasons
import com.example.gadgetmover.util.formatMoney
import kotlinx.coroutines.launch

private const val MAX_RETURN_REQUEST_PHOTOS = 5
private const val MAX_RETURN_REQUEST_ATTEMPTS = 2

/**
 * Buyer-submitted return/refund dispute — one screen with two modes depending on which side of
 * the order the current user is on. Buyer (order status SHIPPED): fills out the request form.
 * Seller (order status RETURN_REQUESTED): reviews the buyer's submitted request and accepts or
 * rejects it. Both call back into `mark_order_shipped`-adjacent server functions that also move
 * the order's own status, so [onFinished] just needs to pop back to Order Detail.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReturnRequestScreen(order: Order, onBackClick: () -> Unit, onFinished: () -> Unit) {
    if (order !is BuyOrder) {
        onBackClick()
        return
    }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isBuyerSide = order.isPurchase
    var requests by remember { mutableStateOf<List<ReturnRequest>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(order.id) {
        requests = ReturnRequestRepository.getForOrder(order.id)
        loading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isBuyerSide) "Request Return/Refund" else "Review Request") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (isBuyerSide) {
                SubmitReturnRequestForm(
                    order = order,
                    attemptsUsed = requests.size,
                    onSubmitted = onFinished
                )
            } else {
                val pending = requests.lastOrNull { it.status == ReturnRequestStatus.PENDING }
                if (pending == null) {
                    Text("No pending request.", modifier = Modifier.align(Alignment.Center))
                } else {
                    DecideReturnRequestForm(request = pending, onDecided = onFinished)
                }
            }
        }
    }
}

@Composable
private fun SubmitReturnRequestForm(order: BuyOrder, attemptsUsed: Int, onSubmitted: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    if (attemptsUsed >= MAX_RETURN_REQUEST_ATTEMPTS) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Your previous return/refund requests for this order were both declined. Please contact customer support for further help.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    var reason by remember { mutableStateOf(returnRequestReasons.first()) }
    var reasonOther by remember { mutableStateOf("") }
    var requestType by remember { mutableStateOf(ReturnRequestType.RETURN) }
    var returnMethod by remember { mutableStateOf(ReturnMethod.MEETUP) }
    var refundAmountText by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var photoUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val pickPhotos = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(MAX_RETURN_REQUEST_PHOTOS)
    ) { uris -> if (uris.isNotEmpty()) photoUris = uris }

    val refundAmount = refundAmountText.toDoubleOrNull()
    val canSubmit = when (requestType) {
        ReturnRequestType.RETURN -> true
        ReturnRequestType.REFUND -> refundAmount != null && refundAmount > 0 && refundAmount <= order.price
    } && (reason != "Other" || reasonOther.isNotBlank())

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Reason", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        returnRequestReasons.forEach { option ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                RadioButton(selected = reason == option, onClick = { reason = option })
                Text(option, style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (reason == "Other") {
            OutlinedTextField(
                value = reasonOther,
                onValueChange = { reasonOther = it },
                label = { Text("Tell us more") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("What would you like?", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(ReturnRequestType.RETURN to "Return item", ReturnRequestType.REFUND to "Refund only").forEach { (type, label) ->
                OutlinedButton(
                    onClick = { requestType = type },
                    modifier = Modifier.weight(1f),
                    colors = if (requestType == type) {
                        androidx.compose.material3.ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    } else androidx.compose.material3.ButtonDefaults.outlinedButtonColors()
                ) { Text(label) }
            }
        }

        if (requestType == ReturnRequestType.RETURN) {
            Spacer(modifier = Modifier.height(12.dp))
            Text("How will you send it back?", style = MaterialTheme.typography.labelLarge)
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = returnMethod == ReturnMethod.MEETUP, onClick = { returnMethod = ReturnMethod.MEETUP })
                Text("Meet-up")
                Spacer(modifier = Modifier.width(12.dp))
                RadioButton(selected = returnMethod == ReturnMethod.SHIPPING, onClick = { returnMethod = ReturnMethod.SHIPPING })
                Text("Shipping")
            }
        } else {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = refundAmountText,
                onValueChange = { refundAmountText = it },
                label = { Text("Refund amount (max ${formatMoney(order.price)})") },
                singleLine = true,
                isError = refundAmountText.isNotBlank() && (refundAmount == null || refundAmount <= 0 || refundAmount > order.price),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "Adding photos makes your request more likely to be approved.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(photoUris) { uri ->
                AsyncImage(
                    model = uri,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp).clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            item {
                Box(
                    modifier = Modifier.size(72.dp).clip(RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = {
                        pickPhotos.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) {
                        Icon(Icons.Filled.AddAPhoto, contentDescription = "Add photo")
                    }
                }
            }
        }

        errorMessage?.let {
            Spacer(modifier = Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(modifier = Modifier.height(20.dp))
        Button(
            enabled = canSubmit && !busy,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                busy = true
                errorMessage = null
                scope.launch {
                    val photoUrls = ReturnRequestRepository.uploadPhotos(order.id, photoUris, context.contentResolver)
                    val result = ReturnRequestRepository.submitRequest(
                        orderId = order.id,
                        type = requestType,
                        reasonCode = reason,
                        reasonOther = if (reason == "Other") reasonOther else "",
                        refundAmount = if (requestType == ReturnRequestType.REFUND) refundAmount else null,
                        returnMethod = if (requestType == ReturnRequestType.RETURN) returnMethod else null,
                        description = description,
                        photoUrls = photoUrls
                    )
                    busy = false
                    if (result.isSuccess) onSubmitted() else errorMessage = "Couldn't submit your request. Please try again."
                }
            }
        ) {
            if (busy) CircularProgressIndicator(modifier = Modifier.size(18.dp)) else Text("Submit")
        }
    }
}

@Composable
private fun DecideReturnRequestForm(request: ReturnRequest, onDecided: () -> Unit) {
    val scope = rememberCoroutineScope()
    var rejectionReason by remember { mutableStateOf("") }
    var showRejectField by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Card(shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(1.dp)) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Request type: ${if (request.requestType == ReturnRequestType.RETURN) "Return item" else "Refund only"}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Reason: ${request.reasonCode}${if (request.reasonCode == "Other") " — ${request.reasonOtherText}" else ""}", style = MaterialTheme.typography.bodyMedium)
                request.refundAmount?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Requested refund: ${formatMoney(it)}", style = MaterialTheme.typography.bodyMedium)
                }
                request.returnMethod?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Return method: ${if (it == ReturnMethod.MEETUP) "Meet-up" else "Shipping"}", style = MaterialTheme.typography.bodyMedium)
                }
                if (request.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(request.description, style = MaterialTheme.typography.bodySmall)
                }
                if (request.photoUrls.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(request.photoUrls) { url ->
                            AsyncImage(
                                model = url,
                                contentDescription = null,
                                modifier = Modifier.size(80.dp).clip(RoundedCornerShape(10.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        if (showRejectField) {
            OutlinedTextField(
                value = rejectionReason,
                onValueChange = { rejectionReason = it },
                label = { Text("Reason for declining") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                enabled = rejectionReason.isNotBlank() && !busy,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    busy = true
                    scope.launch {
                        ReturnRequestRepository.decide(request.id, accept = false, rejectionReason = rejectionReason)
                        busy = false
                        onDecided()
                    }
                }
            ) { Text("Confirm Decline") }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    enabled = !busy,
                    modifier = Modifier.weight(1f),
                    onClick = { showRejectField = true }
                ) { Text("Reject") }
                Button(
                    enabled = !busy,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        busy = true
                        scope.launch {
                            ReturnRequestRepository.decide(request.id, accept = true, rejectionReason = null)
                            busy = false
                            onDecided()
                        }
                    }
                ) { Text("Accept") }
            }
        }
    }
}
