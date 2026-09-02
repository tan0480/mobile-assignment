package com.example.gadgetmover.screen.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.gadgetmover.data.AuthRepository
import com.example.gadgetmover.data.OrderRepository
import com.example.gadgetmover.data.ProductRepository
import com.example.gadgetmover.data.WalletRepository
import com.example.gadgetmover.model.OrderStatus
import com.example.gadgetmover.model.User
import com.example.gadgetmover.screen.components.LoginRequiredDialog
import com.example.gadgetmover.ui.theme.AccentLime
import com.example.gadgetmover.ui.theme.BrandBlueDark
import com.example.gadgetmover.ui.theme.BrandOrange
import com.example.gadgetmover.util.formatDisplayDateOnly
import com.example.gadgetmover.util.formatMoney

enum class ProfileQuickAction {
    MY_LISTINGS, PURCHASES, SALES, RENTALS, LEASES, WALLET,
    SAVED_ITEMS, REVIEWS, ANALYTICS, BROWSE_HISTORY, SETTINGS, HELP_CENTRE
}

enum class AccountSupportAction {
    ACCOUNT_SETTINGS, PAYMENT_METHODS, SHIPPING_ADDRESS, PRIVACY_SECURITY
}

@Composable
fun ProfileScreen(
    onNotificationsClick: () -> Unit,
    onQuickActionClick: (ProfileQuickAction) -> Unit,
    onAccountSupportClick: (AccountSupportAction) -> Unit,
    onLogoutClick: () -> Unit,
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit
) {
    val isLoggedIn by AuthRepository.isLoggedIn
    val user = AuthRepository.currentUser.value
    var showLoginDialog by remember { mutableStateOf(false) }

    fun requireLogin(action: () -> Unit) {
        if (isLoggedIn) action() else showLoginDialog = true
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ProfileTopBar(
            onSettingsClick = { requireLogin { onQuickActionClick(ProfileQuickAction.SETTINGS) } }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            ProfileHeaderCard(
                isLoggedIn = isLoggedIn,
                user = user,
                onLoginClick = onLoginClick,
                onRegisterClick = onRegisterClick
            )

            Spacer(modifier = Modifier.height(24.dp))
            Text("Your marketplace", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            MarketplaceGrid(onQuickActionClick = { action -> requireLogin { onQuickActionClick(action) } })

            Spacer(modifier = Modifier.height(24.dp))
            Text("Account & support", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(4.dp)) {
                Column {
                    AccountSupportRow(
                        icon = Icons.Filled.ManageAccounts, // TODO: swap with custom ImageVector
                        tint = MaterialTheme.colorScheme.primary,
                        title = "Account settings",
                        subtitle = "Profile, identity & preferences",
                        onClick = { requireLogin { onAccountSupportClick(AccountSupportAction.ACCOUNT_SETTINGS) } }
                    )
                    AccountSupportRow(
                        icon = Icons.Filled.CreditCard, // TODO: swap with custom ImageVector
                        tint = MaterialTheme.colorScheme.primary,
                        title = "Payment methods",
                        subtitle = "Cards and wallet",
                        onClick = { requireLogin { onAccountSupportClick(AccountSupportAction.PAYMENT_METHODS) } }
                    )
                    AccountSupportRow(
                        icon = Icons.Filled.LocationOn, // TODO: swap with custom ImageVector
                        tint = MaterialTheme.colorScheme.primary,
                        title = "Shipping address",
                        subtitle = "Manage pickup addresses",
                        onClick = { requireLogin { onAccountSupportClick(AccountSupportAction.SHIPPING_ADDRESS) } }
                    )
                    AccountSupportRow(
                        icon = Icons.Filled.Security, // TODO: swap with custom ImageVector
                        tint = BrandBlueDark,
                        title = "Privacy & security",
                        subtitle = "Password and login",
                        onClick = { requireLogin { onAccountSupportClick(AccountSupportAction.PRIVACY_SECURITY) } }
                    )
                    AccountSupportRow(
                        icon = Icons.Filled.Notifications, // TODO: swap with custom ImageVector
                        tint = MaterialTheme.colorScheme.primary,
                        title = "Notifications",
                        subtitle = "Manage alerts and updates",
                        onClick = { requireLogin(onNotificationsClick) }
                    )
                    AccountSupportRow(
                        icon = Icons.Filled.Analytics, // TODO: swap with custom ImageVector
                        tint = MaterialTheme.colorScheme.primary,
                        title = "Analytics",
                        subtitle = "Your listing performance",
                        onClick = { requireLogin { onQuickActionClick(ProfileQuickAction.ANALYTICS) } }
                    )
                    AccountSupportRow(
                        icon = Icons.AutoMirrored.Filled.HelpOutline, // TODO: swap with custom ImageVector
                        tint = MaterialTheme.colorScheme.primary,
                        title = "Help centre",
                        subtitle = "FAQs and contact support",
                        onClick = { requireLogin { onQuickActionClick(ProfileQuickAction.HELP_CENTRE) } },
                        showDivider = isLoggedIn
                    )
                    if (isLoggedIn) {
                        AccountSupportRow(
                            icon = Icons.AutoMirrored.Filled.Logout, // TODO: swap with custom ImageVector
                            tint = MaterialTheme.colorScheme.error,
                            title = "Log out",
                            subtitle = "Sign out of your account",
                            onClick = onLogoutClick,
                            showDivider = false
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showLoginDialog) {
        LoginRequiredDialog(onDismiss = { showLoginDialog = false }, onLoginClick = onLoginClick)
    }
}

@Composable
private fun ProfileTopBar(onSettingsClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Profile",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        TopBarIconButton(
            icon = Icons.Filled.Settings, // TODO: swap with custom ImageVector
            contentDescription = "Settings",
            onClick = onSettingsClick
        )
    }
}

@Composable
private fun TopBarIconButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Icon(icon, contentDescription = contentDescription)
    }
}

@Composable
private fun ProfileHeaderCard(
    isLoggedIn: Boolean,
    user: User?,
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(BrandBlueDark)
            .padding(20.dp)
    ) {
        if (isLoggedIn && user != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(AccentLime),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        user.name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = BrandBlueDark
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            user.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        if (user.isVerified) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                Icons.Filled.CheckCircle, // TODO: swap with custom ImageVector
                                contentDescription = "Verified",
                                tint = AccentLime,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    if (user.userId.isNotBlank()) {
                        Text(
                            "@${user.userId}",
                            style = MaterialTheme.typography.bodySmall,
                            color = AccentLime,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "Member since ${formatDisplayDateOnly(user.joinedDate)} · ${user.location}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Star, // TODO: swap with custom ImageVector
                            contentDescription = null,
                            tint = AccentLime,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "${user.rating} · ${user.ratingCount} reviews",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = AccentLime
                        )
                    }
                }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.AccountCircle, // TODO: swap with custom ImageVector
                        contentDescription = "Guest",
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(38.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Welcome to Gadget Mover",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "Log in to access your marketplace",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Button(
                        onClick = onLoginClick,
                        modifier = Modifier.height(36.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = BrandBlueDark)
                    ) {
                        Text("Log In", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(
                        onClick = onRegisterClick,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text("Register", style = MaterialTheme.typography.labelMedium, color = AccentLime, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private data class MarketplaceItem(
    val action: ProfileQuickAction,
    val icon: ImageVector,
    val tint: Color,
    val title: String,
    val subtitle: String
)

@Composable
private fun MarketplaceGrid(onQuickActionClick: (ProfileQuickAction) -> Unit) {
    val currentUser = AuthRepository.currentUser.value
    val myListingsCount = ProductRepository.myListings(currentUser?.id.orEmpty()).size
    val purchasesCount = OrderRepository.myPurchases().size
    val salesCount = OrderRepository.mySales().size
    val activeRentalsCount = OrderRepository.myRentals().count { it.status == OrderStatus.ACTIVE }
    val pendingLeasesCount = OrderRepository.myLeases().count { it.status == OrderStatus.PENDING }
    val savedCount = ProductRepository.getSaved().size
    val reviewCount = currentUser?.ratingCount ?: 0
    val walletBalance = WalletRepository.balance.value

    // Icons below are Material placeholders — swap each with your own ImageVector
    // (e.g. from Google Fonts / Material Symbols) whenever you're ready.
    val items = listOf(
        MarketplaceItem(ProfileQuickAction.MY_LISTINGS, Icons.Filled.Add, Color(0xFF7C3AED), "My listings", "$myListingsCount active"),
        MarketplaceItem(ProfileQuickAction.PURCHASES, Icons.Filled.ShoppingBag, BrandOrange, "Purchases", "$purchasesCount items"),
        MarketplaceItem(ProfileQuickAction.SALES, Icons.Filled.ShoppingCart, Color(0xFF334155), "Sales", "$salesCount items"),
        MarketplaceItem(ProfileQuickAction.RENTALS, Icons.Filled.DateRange, Color(0xFF2563EB), "Rentals", "$activeRentalsCount active"),
        MarketplaceItem(ProfileQuickAction.LEASES, Icons.Filled.Description, BrandOrange, "Leases", "$pendingLeasesCount pending"),
        MarketplaceItem(ProfileQuickAction.WALLET, Icons.Filled.Wallet, Color(0xFF16A34A), "Wallet", formatMoney(walletBalance)),
        MarketplaceItem(ProfileQuickAction.SAVED_ITEMS, Icons.Filled.Favorite, Color(0xFFDC2626), "Saved items", "$savedCount saved"),
        MarketplaceItem(ProfileQuickAction.REVIEWS, Icons.Filled.Star, Color(0xFFCA8A04), "Reviews", "$reviewCount reviews"),
        MarketplaceItem(ProfileQuickAction.BROWSE_HISTORY, Icons.Filled.History, Color(0xFF64748B), "Browse history", "Recent views")
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items.chunked(3).forEach { row ->
            Row(modifier = Modifier.height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { item ->
                    MarketplaceActionCard(
                        item = item,
                        onClick = { onQuickActionClick(item.action) },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(3 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MarketplaceActionCard(item: MarketplaceItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxHeight().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(item.tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(item.icon, contentDescription = item.title, tint = item.tint, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                item.subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AccountSupportRow(
    icon: ImageVector,
    tint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    showDivider: Boolean = true
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(tint.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = tint, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(
                Icons.Filled.ChevronRight, // TODO: swap with custom ImageVector
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (showDivider) {
            androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(start = 68.dp))
        }
    }
}
