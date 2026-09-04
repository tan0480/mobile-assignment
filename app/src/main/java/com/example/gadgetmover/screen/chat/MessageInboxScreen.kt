package com.example.gadgetmover.screen.chat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.gadgetmover.data.AuthRepository
import com.example.gadgetmover.data.ChatRepository
import com.example.gadgetmover.data.NotificationRepository
import com.example.gadgetmover.model.ChatThread
import com.example.gadgetmover.screen.components.AppPullToRefreshBox
import com.example.gadgetmover.screen.components.BackgroundLoadingBadge
import com.example.gadgetmover.screen.components.UserAvatar
import com.example.gadgetmover.ui.theme.BrandBlueDark
import com.example.gadgetmover.ui.theme.BrandOrange
import com.example.gadgetmover.util.formatDisplayDate
import kotlinx.coroutines.launch

private val avatarPalette = listOf(
    Color(0xFFDDD6FE), Color(0xFFBBF7D0), Color(0xFFFED7AA), Color(0xFFBAE6FD), Color(0xFFFBCFE8)
)

@Composable
fun MessageInboxScreen(
    onNotificationsClick: () -> Unit,
    onThreadClick: (ChatThread) -> Unit,
    onLoginClick: () -> Unit
) {
    val isLoggedIn by AuthRepository.isLoggedIn
    val sessionRestored by AuthRepository.sessionRestored
    var query by remember { mutableStateOf("") }
    var isRefreshing by remember { mutableStateOf(false) }
    var isBackgroundLoading by remember { mutableStateOf(false) }
    var threadPendingDelete by remember { mutableStateOf<ChatThread?>(null) }
    val scope = rememberCoroutineScope()
    val threads = ChatRepository.threads.filter {
        query.isBlank() ||
            it.participantName.contains(query, ignoreCase = true) ||
            it.productTitle?.contains(query, ignoreCase = true) == true
    }

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            isBackgroundLoading = true
            ChatRepository.refreshFromRemote()
            isBackgroundLoading = false
        }
    }

    threadPendingDelete?.let { thread ->
        AlertDialog(
            onDismissRequest = { threadPendingDelete = null },
            title = { Text("Delete conversation?") },
            text = { Text("This removes it from your inbox. It'll come back if ${thread.participantName} sends a new message.") },
            confirmButton = {
                TextButton(onClick = {
                    threadPendingDelete = null
                    scope.launch { ChatRepository.deleteThread(thread) }
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { threadPendingDelete = null }) { Text("Cancel") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        MessagesTopBar()

        if (!sessionRestored) {
            // Still checking for a persisted session — showing nothing here rather than the
            // "log in" state below, which would otherwise flash for an already-logged-in user
            // for the brief moment before AuthRepository.restoreSession() resolves.
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return
        }

        if (!isLoggedIn) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.Login,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Log in to view your messages", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onLoginClick, colors = ButtonDefaults.buttonColors(containerColor = BrandBlueDark, contentColor = Color.White)) {
                        Text("Log In")
                    }
                }
            }
            return
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search conversations") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(14.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))
        NotificationsSummaryRow(onClick = onNotificationsClick)

        AppPullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                scope.launch {
                    isRefreshing = true
                    ChatRepository.refreshFromRemote()
                    isRefreshing = false
                }
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
        if (threads.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.Forum,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text("No conversations yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)) {
                items(threads) { thread ->
                    ChatThreadRow(
                        thread = thread,
                        onClick = { onThreadClick(thread) },
                        onLongClick = { threadPendingDelete = thread }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
        BackgroundLoadingBadge(visible = isBackgroundLoading, modifier = Modifier.align(Alignment.TopCenter))
        }
    }
}

@Composable
private fun MessagesTopBar() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("Messages", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun NotificationsSummaryRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(BrandBlueDark),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Notifications, contentDescription = null, tint = Color.White)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Notifications", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                NotificationRepository.recentSummary(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (NotificationRepository.unreadCount > 0) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatThreadRow(thread: ChatThread, onClick: () -> Unit, onLongClick: () -> Unit) {
    val avatarColor = avatarPalette[kotlin.math.abs(thread.id.hashCode()) % avatarPalette.size]

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(
            avatarUrl = thread.participantAvatar,
            displayName = thread.participantName,
            modifier = Modifier.size(48.dp),
            fallbackBackground = avatarColor,
            fallbackContentColor = BrandBlueDark,
            contentDescription = "${thread.participantName}'s avatar"
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    thread.participantName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (thread.unreadCount > 0) FontWeight.Bold else FontWeight.Medium
                )
                Text(
                    formatDisplayDate(thread.lastMessageTime),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    thread.lastMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (thread.unreadCount > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (thread.unreadCount > 0) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(BrandOrange)
                    )
                }
            }
            thread.productTitle?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
