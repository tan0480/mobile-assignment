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
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
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
    BottomNavItem(
        route = Screen.Explore.createRoute(),
        label = "Explore",
        selectedIcon = Icons.Filled.Explore,
        unselectedIcon = Icons.Outlined.Explore,
        matchRoute = Screen.Explore.route
    ),
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
fun GadgetMoverBottomBar(
    navController: NavHostController,
    unreadCount: Int,
    /** Called instead of navigating directly — lets the caller guard the switch (e.g. the listing wizard has unsaved changes, or the target tab itself requires a check like "Sell" needing a password) behind a confirmation before running [navigate]. [targetMatchRoute] is the tapped tab's [BottomNavItem.matchRoute]. Most callers should just invoke `navigate` immediately. */
    interceptNavigation: (targetMatchRoute: String, navigate: () -> Unit) -> Unit = { _, navigate -> navigate() }
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar {
        bottomNavItems.forEach { item ->
            val selected = currentDestination?.hierarchy?.any { it.route == item.matchRoute } == true
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected && navController.currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
                        interceptNavigation(item.matchRoute) {
                            if (navController.currentBackStackEntry?.lifecycle?.currentState != Lifecycle.State.RESUMED) {
                                return@interceptNavigation
                            }
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
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
