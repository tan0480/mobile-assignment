package com.example.gadgetmover.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    /** The route *template* to match against the current destination's `.route` for tab-highlighting — differs from [route] only for a screen (like ListingWizard) whose navigable path carries a resolved argument. */
    val matchRoute: String = route
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Home.route, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(Screen.Explore.route, "Explore", Icons.Filled.Explore, Icons.Outlined.Explore),
    BottomNavItem(
        route = Screen.ListingWizard.createRoute(),
        label = "Sell",
        selectedIcon = Icons.Filled.AddCircle,
        unselectedIcon = Icons.Filled.AddCircle,
        matchRoute = Screen.ListingWizard.route
    ),
    BottomNavItem(Screen.MessageInbox.route, "Messages", Icons.Filled.Chat, Icons.Outlined.Chat),
    BottomNavItem(Screen.Profile.route, "Profile", Icons.Filled.Person, Icons.Outlined.Person)
)

@Composable
fun GadgetMoverBottomBar(navController: NavHostController, unreadCount: Int) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar {
        bottomNavItems.forEach { item ->
            val selected = currentDestination?.hierarchy?.any { it.route == item.matchRoute } == true
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected) {
                        navController.navigate(item.route) {
                            // popUpTo(Screen.Home.route) + restoreState reproducibly fails
                            // to navigate when the tapped tab IS Home itself (the pop target
                            // and the nav target being the same route breaks restoration, so
                            // tapping Home from e.g. a category-filtered Explore screen was a
                            // no-op). None of these screens rely on saved scroll/filter state
                            // surviving a tab switch anyway, so just clear the back stack.
                            popUpTo(0)
                            launchSingleTop = true
                        }
                    }
                },
                icon = {
                    if (item.route == Screen.MessageInbox.route && unreadCount > 0) {
                        BadgedBox(badge = { Badge { Text(unreadCount.toString()) } }) {
                            Icon(
                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label
                            )
                        }
                    } else {
                        Icon(
                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.label
                        )
                    }
                },
                label = { Text(item.label) }
            )
        }
    }
}
