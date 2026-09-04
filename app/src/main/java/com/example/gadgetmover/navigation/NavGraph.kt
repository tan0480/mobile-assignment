package com.example.gadgetmover.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.launch
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.example.gadgetmover.data.AddressRepository
import com.example.gadgetmover.data.AuthRepository
import com.example.gadgetmover.data.BrowseHistoryRepository
import com.example.gadgetmover.data.ChatRepository
import com.example.gadgetmover.data.NotificationRepository
import com.example.gadgetmover.data.OnboardingPreferences
import com.example.gadgetmover.data.OrderRepository
import com.example.gadgetmover.data.ProductCache
import com.example.gadgetmover.data.ProductRepository
import com.example.gadgetmover.data.SettingsRepository
import com.example.gadgetmover.data.WalletRepository
import com.example.gadgetmover.model.ListingType
import com.example.gadgetmover.model.LocationRadiusFilter
import com.example.gadgetmover.model.OtpPurpose
import com.example.gadgetmover.model.ProductCategory
import com.example.gadgetmover.model.RentalOrder
import com.example.gadgetmover.model.filter.CategoryFilterState
import com.example.gadgetmover.notification.SystemNotifier
import com.example.gadgetmover.screen.auth.ForgotPasswordScreen
import com.example.gadgetmover.screen.auth.IntroScreen
import com.example.gadgetmover.screen.auth.LoginScreen
import com.example.gadgetmover.screen.auth.OtpVerificationScreen
import com.example.gadgetmover.screen.auth.RegisterScreen
import com.example.gadgetmover.screen.auth.ResetPasswordScreen
import com.example.gadgetmover.screen.chat.ChatDetailScreen
import com.example.gadgetmover.screen.chat.MessageInboxScreen
import com.example.gadgetmover.screen.chat.NotificationScreen
import com.example.gadgetmover.screen.checkout.CheckoutScreen
import com.example.gadgetmover.screen.explore.ExploreScreen
import com.example.gadgetmover.screen.explore.LocationRadiusFilterScreen
import com.example.gadgetmover.screen.explore.SearchUserScreen
import com.example.gadgetmover.screen.explore.filter.DynamicFilterScreen
import com.example.gadgetmover.screen.home.HomeScreen
import com.example.gadgetmover.screen.listing.ListingWizardScreen
import com.example.gadgetmover.screen.listing.WizardUnsavedChanges
import com.example.gadgetmover.screen.product.BuyConfirmationScreen
import com.example.gadgetmover.screen.product.ProductDetailScreen
import com.example.gadgetmover.screen.profile.SellerProfileScreen
import com.example.gadgetmover.screen.components.LocationPickerScreen
import com.example.gadgetmover.screen.components.PickedLocation
import com.example.gadgetmover.screen.components.RequestStartupPermissions
import com.example.gadgetmover.screen.profile.AccountInfoScreen
import com.example.gadgetmover.screen.profile.ChangePasswordScreen
import com.example.gadgetmover.screen.profile.CreatePasswordScreen
import com.example.gadgetmover.screen.profile.AccountSupportAction
import com.example.gadgetmover.screen.profile.AnalyticsScreen
import com.example.gadgetmover.screen.profile.BrowseHistoryScreen
import com.example.gadgetmover.screen.profile.EditAddressScreen
import com.example.gadgetmover.screen.profile.HelpCentreScreen
import com.example.gadgetmover.screen.profile.MyActivitiesScreen
import com.example.gadgetmover.screen.profile.OrderDetailScreen
import com.example.gadgetmover.screen.profile.ReturnRequestScreen
import com.example.gadgetmover.screen.profile.MyListingsScreen
import com.example.gadgetmover.screen.profile.PaymentMethodsScreen
import com.example.gadgetmover.screen.profile.ProfileQuickAction
import com.example.gadgetmover.screen.profile.ProfileScreen
import com.example.gadgetmover.screen.profile.ReviewsScreen
import com.example.gadgetmover.screen.profile.SavedItemsScreen
import com.example.gadgetmover.screen.profile.SettingsScreen
import com.example.gadgetmover.screen.profile.ShippingAddressScreen
import com.example.gadgetmover.screen.profile.WalletAddFundsAmountScreen
import com.example.gadgetmover.screen.profile.WalletAddFundsPaymentScreen
import com.example.gadgetmover.screen.profile.WalletScreen
import com.example.gadgetmover.screen.profile.WalletWithdrawAmountScreen
import com.example.gadgetmover.screen.profile.WalletWithdrawDestinationScreen
import com.example.gadgetmover.screen.profile.WriteReviewScreen
import com.example.gadgetmover.util.formatMoney

private val bottomBarRoutes = setOf(
    Screen.Home.route,
    Screen.Explore.route,
    Screen.ListingWizard.route,
    Screen.MessageInbox.route,
    Screen.Profile.route
)

/**
 * Drops taps emitted by an outgoing destination during a transition and makes repeated taps on
 * the same action idempotent. Navigation moves the current entry below RESUMED immediately, so a
 * second tap in the same animation window cannot enqueue another copy.
 */
private fun NavHostController.navigateSafely(
    route: String,
    builder: NavOptionsBuilder.() -> Unit = {}
) {
    if (currentBackStackEntry?.lifecycle?.currentState != Lifecycle.State.RESUMED) return
    navigate(route) {
        launchSingleTop = true
        builder()
    }
}

@Composable
fun GadgetMoverNavGraph(
    notificationOrderId: String? = null,
    notificationRecipientUserId: String? = null,
    onNotificationOrderConsumed: () -> Unit = {}
) {
    RequestStartupPermissions()

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.hierarchy?.firstOrNull()?.route
    // ListingWizard.route is shared by both "new listing" (a bottom-bar tab) and "edit listing"
    // (pushed from My Listings), which currentRoute alone can't tell apart — only the actual
    // editProductId argument on this entry distinguishes them.
    val isEditingExistingListing = currentRoute == Screen.ListingWizard.route &&
        backStackEntry?.arguments?.getString("editProductId")?.let { it != Screen.ListingWizard.NEW_LISTING_ID } == true

    val context = LocalContext.current
    // The intro screen is only ever the start destination on the very first launch;
    // every subsequent launch drops straight into Home with login left optional.
    val startDestination = remember {
        if (OnboardingPreferences.hasSeenIntro(context)) Screen.Home.route else Screen.Intro.route
    }

    var pendingQuery by remember { mutableStateOf("") }
    var pendingCategory by remember { mutableStateOf<ProductCategory?>(null) }
    var pendingTransactionType by remember { mutableStateOf<ListingType?>(null) }
    var pendingActivitiesTab by remember { mutableStateOf(0) }
    var dynamicFilterCategory by remember { mutableStateOf<ProductCategory?>(null) }
    var exploreCategoryFilterState by remember { mutableStateOf(CategoryFilterState()) }
    var appliedDynamicFilterCategory by remember { mutableStateOf<ProductCategory?>(null) }
    var dynamicFilterApplyVersion by remember { mutableStateOf(0L) }
    val scope = rememberCoroutineScope()

    // The listing wizard (new-listing draft or an in-progress Edit) reports its unsaved-changes
    // state here (null when there's nothing worth saving) so the bottom nav bar's other four tabs
    // can be guarded behind a "leave without saving?" prompt instead of silently discarding it —
    // see `interceptNavigation` below.
    var currentWizardChanges by remember { mutableStateOf<WizardUnsavedChanges?>(null) }
    var pendingLeaveNav by remember { mutableStateOf<(() -> Unit)?>(null) }

    // Every bottom-nav tab switch clears the back stack down to just the tapped tab (see
    // GadgetMoverBottomBar's popUpTo(0)), so system/gesture back from a non-Home tab's root — or
    // from any screen reached via a flow that similarly cleared history (e.g. post-login) — would
    // otherwise find nothing left to pop and exit the app straight from there. Only enabled when
    // there's genuinely nothing left to pop and we're not already on Home, so it never overrides
    // NavHost's own back handling for a normal push (e.g. Home -> ProductDetail).
    BackHandler(enabled = navController.previousBackStackEntry == null && currentRoute != Screen.Home.route) {
        val goHome = { navController.navigateSafely(Screen.Home.route) { popUpTo(0); launchSingleTop = true } }
        if (currentRoute == Screen.ListingWizard.route && currentWizardChanges != null) {
            pendingLeaveNav = goHome
        } else {
            goHome()
        }
    }

    // A Google sign-in account has no Gadget Mover password yet (see User.hasPassword) — Buy,
    // Rent, and starting a new listing all funnel through this before they're allowed to proceed.
    // Unlike the direct "Create Password" button on Account Information, this path always confirms
    // first via [showCreatePasswordPrompt] — the user didn't ask for a password screen, so jumping
    // straight there without warning would be a surprising interruption to whatever they were doing.
    var pendingAfterPassword by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showCreatePasswordPrompt by remember { mutableStateOf(false) }
    fun runOrRequirePassword(action: () -> Unit) {
        if (AuthRepository.currentUser.value?.hasPassword == false) {
            pendingAfterPassword = action
            showCreatePasswordPrompt = true
        } else {
            action()
        }
    }

    // Subscribes/unsubscribes to live `notifications` rows for whichever account is currently
    // logged in — restarts on login/logout/account switch, and tears down when logged out.
    val currentUserId = AuthRepository.currentUser.value?.id
    val appContext = context.applicationContext
    LaunchedEffect(currentUserId) {
        if (currentUserId != null) {
            NotificationRepository.startRealtimeListening(currentUserId) { notification ->
                SystemNotifier.post(appContext, notification)
            }
        } else {
            NotificationRepository.stopRealtimeListening()
        }
    }

    // MainActivity receives both cold-start and singleTop tray intents. Wait until session restore
    // has identified the recipient and the current destination is interactive before dispatching;
    // then consume the one-shot id so recomposition cannot open the same order twice.
    LaunchedEffect(notificationOrderId, notificationRecipientUserId, currentUserId, backStackEntry) {
        val orderId = notificationOrderId ?: return@LaunchedEffect
        if (currentUserId == null || backStackEntry?.lifecycle?.currentState != Lifecycle.State.RESUMED) {
            return@LaunchedEffect
        }
        if (notificationRecipientUserId != currentUserId) {
            onNotificationOrderConsumed()
            return@LaunchedEffect
        }
        navController.navigate(Screen.OrderDetail.createRoute(orderId, fromNotification = true)) {
            popUpTo(0)
            launchSingleTop = true
        }
        onNotificationOrderConsumed()
    }

    LaunchedEffect(Unit) {
        AuthRepository.restoreSession()
        ProductRepository.refreshFromRemote()
        // Updates the on-disk cache MainActivity seeds Home from on the next cold start — kept
        // in step with whatever refreshFromRemote() just pulled, so the cache is never more than
        // one session stale.
        ProductCache.save(appContext, ProductRepository.products)
        ProductRepository.refreshSavedIds()
        OrderRepository.refreshFromRemote()
        WalletRepository.refreshFromRemote()
        ChatRepository.refreshFromRemote()
        NotificationRepository.refreshFromRemote()
        AddressRepository.refreshFromRemote()
        BrowseHistoryRepository.refreshFromRemote()
        SettingsRepository.refreshFromRemote()
    }

    Scaffold(
        bottomBar = {
            if (currentRoute in bottomBarRoutes && !isEditingExistingListing) {
                val unreadCount = if (AuthRepository.isLoggedIn.value) ChatRepository.totalUnread else 0
                GadgetMoverBottomBar(
                    navController = navController,
                    unreadCount = unreadCount,
                    interceptNavigation = { targetMatchRoute, navigate ->
                        if (currentRoute == Screen.ListingWizard.route && currentWizardChanges != null) {
                            pendingLeaveNav = navigate
                        } else if (targetMatchRoute == Screen.ListingWizard.route) {
                            runOrRequirePassword(navigate)
                        } else {
                            navigate()
                        }
                    }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            enterTransition = { fadeIn(tween(200)) },
            exitTransition = { fadeOut(tween(90)) },
            popEnterTransition = { fadeIn(tween(200)) },
            popExitTransition = { fadeOut(tween(90)) },
            // .padding(padding) alone physically pushes every screen below the status bar, which
            // is all Home/Explore/etc. need since they have no top bar of their own. But screens
            // that DO have their own inner Scaffold+TopAppBar (ChatDetailScreen, ProductDetail,
            // ...) read WindowInsets.statusBars directly to size that bar, and padding() doesn't
            // tell the WindowInsets system that inset was already consumed here — so those screens
            // reserved the status bar height a second time on top of this padding, doubling the
            // gap above their top bar. consumeWindowInsets marks it consumed for descendants
            // without touching the physical .padding() that the top-bar-less screens still need.
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .consumeWindowInsets(padding)
        ) {
            composable(Screen.Intro.route) {
                IntroScreen(
                    onStartBrowsingClick = {
                        OnboardingPreferences.markIntroSeen(context)
                        navController.navigateSafely(Screen.Home.route) {
                            popUpTo(Screen.Intro.route) { inclusive = true }
                        }
                    },
                    onLoginClick = {
                        OnboardingPreferences.markIntroSeen(context)
                        navController.navigateSafely(Screen.Login.route)
                    }
                )
            }
            composable(Screen.Login.route) {
                LoginScreen(
                    onBackClick = { navController.popBackStack() },
                    onLoginSuccess = {
                        navController.navigateSafely(Screen.Home.route) {
                            popUpTo(0)
                        }
                    },
                    onRegisterClick = { navController.navigateSafely(Screen.Register.route) },
                    onForgotPasswordClick = { navController.navigateSafely(Screen.ForgotPassword.route) }
                )
            }
            composable(Screen.Register.route) {
                RegisterScreen(
                    onBackClick = { navController.popBackStack() },
                    onOtpRequired = { email ->
                        navController.navigateSafely(Screen.OtpVerification.createRoute(email, OtpPurpose.REGISTRATION.name))
                    },
                    onLoginClick = { navController.navigateSafely(Screen.Login.route) }
                )
            }
            composable(Screen.ForgotPassword.route) {
                ForgotPasswordScreen(
                    onBackClick = { navController.popBackStack() },
                    onSendCode = { email ->
                        navController.navigateSafely(Screen.OtpVerification.createRoute(email, OtpPurpose.FORGOT_PASSWORD.name))
                    }
                )
            }
            composable(
                route = Screen.OtpVerification.route,
                arguments = listOf(
                    navArgument("contact") { type = NavType.StringType },
                    navArgument("purpose") { type = NavType.StringType }
                )
            ) { entry ->
                val contact = entry.arguments?.getString("contact").orEmpty()
                val purpose = OtpPurpose.valueOf(entry.arguments?.getString("purpose").orEmpty())
                OtpVerificationScreen(
                    email = contact,
                    purpose = purpose,
                    onBackClick = { navController.popBackStack() },
                    onVerified = {
                        when (purpose) {
                            OtpPurpose.REGISTRATION -> navController.navigateSafely(Screen.Home.route) {
                                popUpTo(0)
                            }
                            OtpPurpose.FORGOT_PASSWORD -> navController.navigateSafely(Screen.ResetPassword.createRoute(contact)) {
                                popUpTo(Screen.Login.route) { inclusive = false }
                            }
                        }
                    }
                )
            }
            composable(
                route = Screen.ResetPassword.route,
                arguments = listOf(navArgument("email") { type = NavType.StringType })
            ) { entry ->
                val email = entry.arguments?.getString("email").orEmpty()
                ResetPasswordScreen(
                    email = email,
                    onBackClick = { navController.popBackStack() },
                    onResetSuccess = {
                        navController.navigateSafely(Screen.Login.route) {
                            popUpTo(0)
                        }
                    }
                )
            }

            composable(Screen.Home.route) {
                HomeScreen(
                    onProductClick = { product ->
                        navController.navigateSafely(Screen.ProductDetail.createRoute(product.id))
                    },
                    onCategoryClick = { category ->
                        pendingCategory = category
                        pendingQuery = ""
                        pendingTransactionType = null
                        navController.navigateSafely(Screen.Explore.createRoute())
                    },
                    onSearchSubmit = { query ->
                        pendingQuery = query
                        pendingCategory = null
                        pendingTransactionType = null
                        navController.navigateSafely(Screen.Explore.createRoute())
                    },
                    onSeeAllCategories = {
                        pendingQuery = ""
                        pendingCategory = null
                        pendingTransactionType = null
                        navController.navigateSafely(Screen.Explore.createRoute(openCategoryPicker = true))
                    },
                    onLoginClick = { navController.navigateSafely(Screen.Login.route) }
                )
            }

            composable(
                route = Screen.Explore.route,
                arguments = listOf(
                    navArgument("openCategoryPicker") {
                        type = NavType.BoolType
                        defaultValue = false
                    }
                )
            ) { entry ->
                val categoryPickerConsumed by entry.savedStateHandle
                    .getStateFlow("open_category_picker_consumed", false)
                    .collectAsState()
                val shouldOpenCategoryPicker =
                    entry.arguments?.getBoolean("openCategoryPicker") == true && !categoryPickerConsumed
                val locationLat by entry.savedStateHandle.getStateFlow<Double?>("location_filter_lat", null).collectAsState()
                val locationLng by entry.savedStateHandle.getStateFlow<Double?>("location_filter_lng", null).collectAsState()
                val locationAddress by entry.savedStateHandle.getStateFlow<String?>("location_filter_address", null).collectAsState()
                val locationRadius by entry.savedStateHandle.getStateFlow<Float?>("location_filter_radius", null).collectAsState()
                val pickedLocationFilter = if (locationLat != null && locationLng != null && locationAddress != null && locationRadius != null) {
                    LocationRadiusFilter(locationLat!!, locationLng!!, locationAddress!!, locationRadius!!)
                } else null

                ExploreScreen(
                    initialQuery = pendingQuery,
                    initialCategory = pendingCategory,
                    initialTransactionType = pendingTransactionType,
                    onProductClick = { product ->
                        navController.navigateSafely(Screen.ProductDetail.createRoute(product.id))
                    },
                    onUserClick = { user ->
                        navController.navigateSafely(Screen.SellerProfile.createRoute(user.id))
                    },
                    onSearchUsersClick = { navController.navigateSafely(Screen.SearchUsers.route) },
                    pickedLocationFilter = pickedLocationFilter,
                    onLocationFilterClick = { navController.navigateSafely(Screen.LocationRadiusFilter.route) },
                    categoryFilterState = exploreCategoryFilterState,
                    appliedFilterCategory = appliedDynamicFilterCategory,
                    appliedFilterVersion = dynamicFilterApplyVersion,
                    openCategoryPicker = shouldOpenCategoryPicker,
                    onCategoryPickerOpened = {
                        entry.savedStateHandle["open_category_picker_consumed"] = true
                    },
                    onCategoryFilterStateChange = { exploreCategoryFilterState = it },
                    onOpenFilters = { category ->
                        dynamicFilterCategory = category
                        navController.navigateSafely(Screen.DynamicFilter.route)
                    }
                )
            }

            composable(Screen.DynamicFilter.route) {
                val exploreEntry = remember(navController) { navController.getBackStackEntry(Screen.Explore.route) }
                val locationLat by exploreEntry.savedStateHandle.getStateFlow<Double?>("location_filter_lat", null).collectAsState()
                val locationLng by exploreEntry.savedStateHandle.getStateFlow<Double?>("location_filter_lng", null).collectAsState()
                val locationAddress by exploreEntry.savedStateHandle.getStateFlow<String?>("location_filter_address", null).collectAsState()
                val locationRadius by exploreEntry.savedStateHandle.getStateFlow<Float?>("location_filter_radius", null).collectAsState()
                val locationFilter = if (locationLat != null && locationLng != null && locationAddress != null && locationRadius != null) {
                    LocationRadiusFilter(locationLat!!, locationLng!!, locationAddress!!, locationRadius!!)
                } else null
                val category = dynamicFilterCategory

                if (category != null) {
                    DynamicFilterScreen(
                        category = category,
                        filterState = exploreCategoryFilterState,
                        onDismiss = { navController.popBackStack() },
                        onApply = { selectedCategory, selectedFilters ->
                            dynamicFilterCategory = selectedCategory
                            exploreCategoryFilterState = selectedFilters
                            appliedDynamicFilterCategory = selectedCategory
                            dynamicFilterApplyVersion += 1
                            navController.popBackStack()
                        },
                        onReset = { selectedCategory ->
                            dynamicFilterCategory = selectedCategory
                            exploreCategoryFilterState = CategoryFilterState()
                            appliedDynamicFilterCategory = selectedCategory
                            dynamicFilterApplyVersion += 1
                            navController.popBackStack()
                        },
                        locationFilter = locationFilter,
                        onLocationFilterClick = { navController.navigateSafely(Screen.LocationRadiusFilter.route) }
                    )
                } else {
                    LaunchedEffect(Unit) { navController.popBackStack() }
                }
            }

            composable(Screen.SearchUsers.route) {
                SearchUserScreen(
                    onBackClick = { navController.popBackStack() },
                    onUserClick = { user -> navController.navigateSafely(Screen.SellerProfile.createRoute(user.id)) }
                )
            }

            composable(Screen.LocationRadiusFilter.route) {
                val exploreEntry = remember(navController) { navController.getBackStackEntry(Screen.Explore.route) }
                val currentLat by exploreEntry.savedStateHandle.getStateFlow<Double?>("location_filter_lat", null).collectAsState()
                val currentLng by exploreEntry.savedStateHandle.getStateFlow<Double?>("location_filter_lng", null).collectAsState()
                val currentAddress by exploreEntry.savedStateHandle.getStateFlow<String?>("location_filter_address", null).collectAsState()
                val currentRadius by exploreEntry.savedStateHandle.getStateFlow<Float?>("location_filter_radius", null).collectAsState()
                val currentFilter = if (currentLat != null && currentLng != null && currentAddress != null && currentRadius != null) {
                    LocationRadiusFilter(currentLat!!, currentLng!!, currentAddress!!, currentRadius!!)
                } else null

                LocationRadiusFilterScreen(
                    initial = currentFilter,
                    onBackClick = { navController.popBackStack() },
                    onConfirm = { filter ->
                        exploreEntry.savedStateHandle.apply {
                            set("location_filter_lat", filter.latitude)
                            set("location_filter_lng", filter.longitude)
                            set("location_filter_address", filter.address)
                            set("location_filter_radius", filter.radiusKm)
                        }
                        navController.popBackStack()
                    },
                    onClear = {
                        exploreEntry.savedStateHandle.apply {
                            set<Double?>("location_filter_lat", null)
                            set<Double?>("location_filter_lng", null)
                            set<String?>("location_filter_address", null)
                            set<Float?>("location_filter_radius", null)
                        }
                        navController.popBackStack()
                    }
                )
            }

            composable(
                route = Screen.ListingWizard.route,
                arguments = listOf(navArgument("editProductId") { type = NavType.StringType })
            ) { entry ->
                val editProductId = entry.arguments?.getString("editProductId")
                val existingProduct = if (editProductId != null && editProductId != Screen.ListingWizard.NEW_LISTING_ID) {
                    ProductRepository.getById(editProductId)
                } else null

                val pickedLat by entry.savedStateHandle.getStateFlow<Double?>("picked_lat", null).collectAsState()
                val pickedLng by entry.savedStateHandle.getStateFlow<Double?>("picked_lng", null).collectAsState()
                val pickedAddress by entry.savedStateHandle.getStateFlow<String?>("picked_address", null).collectAsState()
                val pickedName by entry.savedStateHandle.getStateFlow<String?>("picked_name", null).collectAsState()
                val pickedLocation = if (pickedLat != null && pickedLng != null && pickedAddress != null) {
                    PickedLocation(pickedLat!!, pickedLng!!, pickedAddress!!, pickedName)
                } else null

                ListingWizardScreen(
                    existingProduct = existingProduct,
                    onBackClick = { navController.popBackStack() },
                    onPublished = {
                        if (existingProduct != null) {
                            navController.popBackStack()
                        } else {
                            navController.navigateSafely(Screen.Home.route) {
                                popUpTo(Screen.Home.route) { inclusive = false }
                            }
                        }
                    },
                    onLoginClick = { navController.navigateSafely(Screen.Login.route) },
                    pickedMeetupLocation = pickedLocation,
                    onPickMeetupLocation = {
                        entry.savedStateHandle.remove<Double>("picked_lat")
                        entry.savedStateHandle.remove<Double>("picked_lng")
                        entry.savedStateHandle.remove<String>("picked_address")
                        entry.savedStateHandle.remove<String>("picked_name")
                        navController.navigateSafely(Screen.LocationPicker.route)
                    },
                    onUnsavedChangesChanged = { currentWizardChanges = it }
                )
            }

            composable(
                route = Screen.ProductDetail.route,
                arguments = listOf(navArgument("productId") { type = NavType.StringType }),
                deepLinks = listOf(navDeepLink { uriPattern = "https://gadgetmover.app/product/{productId}" })
            ) { entry ->
                val productId = entry.arguments?.getString("productId").orEmpty()
                val product = ProductRepository.getById(productId)
                if (product != null) {
                    // Viewing your own listing shouldn't pollute your own "recently viewed" list.
                    LaunchedEffect(productId) {
                        if (product.sellerId != AuthRepository.currentUser.value?.id) BrowseHistoryRepository.recordView(productId)
                    }
                    ProductDetailScreen(
                        product = product,
                        onBackClick = { navController.popBackStack() },
                        onBuyNowClick = {
                            runOrRequirePassword { navController.navigateSafely(Screen.Checkout.createRoute(it.id, ListingType.BUY.name)) }
                        },
                        onRentClick = {
                            runOrRequirePassword { navController.navigateSafely(Screen.Checkout.createRoute(it.id, ListingType.RENT.name)) }
                        },
                        onMessageSellerClick = {
                            scope.launch {
                                val thread = ChatRepository.findOrCreateThreadForProduct(it)
                                navController.navigateSafely(Screen.ChatDetail.createRoute(thread.id))
                            }
                        },
                        onEditClick = { navController.navigateSafely(Screen.ListingWizard.createRoute(it.id)) },
                        onLoginRequired = { navController.navigateSafely(Screen.Login.route) },
                        onSellerClick = { navController.navigateSafely(Screen.SellerProfile.createRoute(it.sellerId)) }
                    )
                }
            }

            composable(
                route = Screen.SellerProfile.route,
                arguments = listOf(navArgument("sellerId") { type = NavType.StringType })
            ) { entry ->
                val sellerId = entry.arguments?.getString("sellerId").orEmpty()
                val fallbackName = ProductRepository.myListings(sellerId).firstOrNull()?.sellerName.orEmpty()
                SellerProfileScreen(
                    sellerId = sellerId,
                    sellerNameFallback = fallbackName,
                    onBackClick = { navController.popBackStack() },
                    onProductClick = { navController.navigateSafely(Screen.ProductDetail.createRoute(it.id)) },
                    onReviewsClick = { navController.navigateSafely(Screen.SellerReviews.createRoute(sellerId)) }
                )
            }

            composable(
                route = Screen.SellerReviews.route,
                arguments = listOf(navArgument("sellerId") { type = NavType.StringType })
            ) { entry ->
                val sellerId = entry.arguments?.getString("sellerId").orEmpty()
                ReviewsScreen(sellerId = sellerId, onBackClick = { navController.popBackStack() })
            }

            composable(
                route = Screen.Checkout.route,
                arguments = listOf(
                    navArgument("productId") { type = NavType.StringType },
                    navArgument("transactionType") { type = NavType.StringType },
                    navArgument("negotiatedPrice") { type = NavType.StringType }
                )
            ) { entry ->
                val productId = entry.arguments?.getString("productId").orEmpty()
                val transactionType = entry.arguments?.getString("transactionType")?.let { raw ->
                    runCatching { ListingType.valueOf(raw) }.getOrNull()
                } ?: ListingType.BUY
                val negotiatedPrice = entry.arguments?.getString("negotiatedPrice")?.toDoubleOrNull()
                    ?.takeIf { it > 0.0 }
                val product = ProductRepository.getById(productId)
                val pickedAddressId by entry.savedStateHandle.getStateFlow<String?>("selected_address_id", null).collectAsState()
                if (product != null) {
                    CheckoutScreen(
                        product = product,
                        transactionType = transactionType,
                        pickedAddressId = pickedAddressId,
                        negotiatedPrice = negotiatedPrice,
                        onBackClick = { navController.popBackStack() },
                        onChangeAddress = { navController.navigateSafely(Screen.SelectShippingAddress.route) },
                        onOrderConfirmed = { order ->
                            navController.navigateSafely(Screen.BuyConfirmation.createRoute(order.id)) {
                                popUpTo(Screen.Checkout.route) { inclusive = true }
                            }
                        }
                    )
                }
            }

            composable(
                route = Screen.BuyConfirmation.route,
                arguments = listOf(navArgument("orderId") { type = NavType.StringType })
            ) { entry ->
                val orderId = entry.arguments?.getString("orderId").orEmpty()
                val order = OrderRepository.orders.find { it.id == orderId }
                if (order != null) {
                    BuyConfirmationScreen(
                        order = order,
                        onDoneClick = {
                            navController.navigateSafely(Screen.Home.route) {
                                popUpTo(Screen.Home.route) { inclusive = false }
                            }
                        },
                        onViewActivitiesClick = {
                            pendingActivitiesTab = if (order is RentalOrder) 2 else 0
                            // Reset the back stack down to Profile first (mirroring the login-success
                            // reset pattern above) so back from My Activities lands on Profile — not on
                            // this now-stale confirmation screen or whatever preceded checkout.
                            navController.navigateSafely(Screen.Profile.route) {
                                popUpTo(0)
                            }
                            navController.navigateSafely(Screen.MyActivities.route)
                        }
                    )
                }
            }

            composable(Screen.MessageInbox.route) {
                MessageInboxScreen(
                    onNotificationsClick = { navController.navigateSafely(Screen.Notifications.route) },
                    onThreadClick = { thread ->
                        navController.navigateSafely(Screen.ChatDetail.createRoute(thread.id))
                    },
                    onLoginClick = { navController.navigateSafely(Screen.Login.route) }
                )
            }

            composable(Screen.Notifications.route) {
                NotificationScreen(
                    onBackClick = { navController.popBackStack() },
                    onNotificationClick = { notification ->
                        if (notification.relatedOrderId != null) {
                            navController.navigateSafely(
                                Screen.OrderDetail.createRoute(notification.relatedOrderId, fromNotification = true)
                            )
                        } else notification.relatedThreadId?.let { threadId ->
                            navController.navigateSafely(Screen.ChatDetail.createRoute(threadId))
                        }
                    }
                )
            }

            composable(
                route = Screen.ChatDetail.route,
                arguments = listOf(navArgument("threadId") { type = NavType.StringType })
            ) { entry ->
                val threadId = entry.arguments?.getString("threadId").orEmpty()
                val thread = ChatRepository.getThread(threadId)
                if (thread != null) {
                    val pickedLat by entry.savedStateHandle.getStateFlow<Double?>("picked_lat", null).collectAsState()
                    val pickedLng by entry.savedStateHandle.getStateFlow<Double?>("picked_lng", null).collectAsState()
                    val pickedAddress by entry.savedStateHandle.getStateFlow<String?>("picked_address", null).collectAsState()
                    val pickedLocation = if (pickedLat != null && pickedLng != null && pickedAddress != null) {
                        PickedLocation(pickedLat!!, pickedLng!!, pickedAddress!!)
                    } else null

                    ChatDetailScreen(
                        thread = thread,
                        onBackClick = { navController.popBackStack() },
                        pickedLocation = pickedLocation,
                        onPickLocationClick = {
                            entry.savedStateHandle.remove<Double>("picked_lat")
                            entry.savedStateHandle.remove<Double>("picked_lng")
                            entry.savedStateHandle.remove<String>("picked_address")
                            entry.savedStateHandle.remove<String>("picked_name")
                            navController.navigateSafely(Screen.LocationPicker.route)
                        },
                        onLocationConsumed = {
                            entry.savedStateHandle.remove<Double>("picked_lat")
                            entry.savedStateHandle.remove<Double>("picked_lng")
                            entry.savedStateHandle.remove<String>("picked_address")
                            entry.savedStateHandle.remove<String>("picked_name")
                        },
                        onProductClick = { productId ->
                            navController.navigateSafely(Screen.ProductDetail.createRoute(productId))
                        },
                        onNegotiatedCheckout = { productId, type, price ->
                            runOrRequirePassword { navController.navigateSafely(Screen.Checkout.createRoute(productId, type.name, price)) }
                        }
                    )
                }
            }

            composable(Screen.Profile.route) {
                ProfileScreen(
                    onQuickActionClick = { action ->
                        when (action) {
                            ProfileQuickAction.MY_LISTINGS -> navController.navigateSafely(Screen.MyListings.route)
                            ProfileQuickAction.PURCHASES -> {
                                pendingActivitiesTab = 0
                                navController.navigateSafely(Screen.MyActivities.route)
                            }
                            ProfileQuickAction.SALES -> {
                                pendingActivitiesTab = 1
                                navController.navigateSafely(Screen.MyActivities.route)
                            }
                            ProfileQuickAction.RENTALS -> {
                                pendingActivitiesTab = 2
                                navController.navigateSafely(Screen.MyActivities.route)
                            }
                            ProfileQuickAction.LEASES -> {
                                pendingActivitiesTab = 3
                                navController.navigateSafely(Screen.MyActivities.route)
                            }
                            ProfileQuickAction.WALLET -> runOrRequirePassword { navController.navigateSafely(Screen.Wallet.route) }
                            ProfileQuickAction.SAVED_ITEMS -> navController.navigateSafely(Screen.SavedItems.route)
                            ProfileQuickAction.REVIEWS -> navController.navigateSafely(Screen.Reviews.route)
                            ProfileQuickAction.ANALYTICS -> navController.navigateSafely(Screen.Analytics.route)
                            ProfileQuickAction.BROWSE_HISTORY -> navController.navigateSafely(Screen.BrowseHistory.route)
                            ProfileQuickAction.SETTINGS -> navController.navigateSafely(Screen.Settings.route)
                            ProfileQuickAction.HELP_CENTRE -> navController.navigateSafely(Screen.HelpCentre.route)
                        }
                    },
                    onAccountSupportClick = { action ->
                        when (action) {
                            AccountSupportAction.PAYMENT_METHODS -> runOrRequirePassword { navController.navigateSafely(Screen.PaymentMethods.route) }
                            AccountSupportAction.SHIPPING_ADDRESS -> navController.navigateSafely(Screen.ShippingAddress.route)
                        }
                    },
                    onLogoutClick = {
                        scope.launch {
                            AuthRepository.logout()
                            navController.navigateSafely(Screen.Intro.route) {
                                popUpTo(0)
                            }
                        }
                    },
                    onLoginClick = { navController.navigateSafely(Screen.Login.route) },
                    onRegisterClick = { navController.navigateSafely(Screen.Register.route) }
                )
            }

            composable(Screen.MyActivities.route) {
                MyActivitiesScreen(
                    onBackClick = { navController.popBackStack() },
                    onOrderClick = { order -> navController.navigateSafely(Screen.OrderDetail.createRoute(order.id)) },
                    onRequestReturnClick = { order -> navController.navigateSafely(Screen.ReturnRequest.createRoute(order.id)) },
                    onWriteReviewClick = { order -> navController.navigateSafely(Screen.WriteReview.createRoute(order.id)) },
                    initialTab = pendingActivitiesTab
                )
            }

            composable(
                route = Screen.OrderDetail.route,
                arguments = listOf(
                    navArgument("orderId") { type = NavType.StringType },
                    navArgument("fromNotification") {
                        type = NavType.BoolType
                        defaultValue = false
                    }
                )
            ) { entry ->
                val orderId = entry.arguments?.getString("orderId").orEmpty()
                val fromNotification = entry.arguments?.getBoolean("fromNotification") ?: false
                val order = OrderRepository.orders.find { it.id == orderId }
                var orderLookupFinished by remember(orderId) { mutableStateOf(order != null) }
                LaunchedEffect(orderId, currentUserId) {
                    if (order == null && currentUserId != null) OrderRepository.refreshFromRemote()
                    orderLookupFinished = true
                }
                if (order != null) {
                    val returnToActivities = {
                        // A seller always lands in Sales, including a rental owner, per the
                        // notification-entry contract. Buyer-side rentals use Rentals.
                        pendingActivitiesTab = when {
                            order is com.example.gadgetmover.model.BuyOrder && !order.isPurchase -> 1
                            order is RentalOrder && !order.isRenter -> 1
                            order is RentalOrder -> 2
                            else -> 0
                        }
                        // Build a proper backstack (Home → Profile → MyActivities) so pressing
                        // back from MyActivities lands on Profile instead of getting stuck.
                        // The previous popUpTo(0) wiped the entire backstack, leaving
                        // MyActivities orphaned with nothing to pop back to.
                        navController.navigateSafely(Screen.Profile.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                inclusive = false
                            }
                        }
                        navController.navigateSafely(Screen.MyActivities.route)
                    }
                    OrderDetailScreen(
                        order = order,
                        onBackClick = { navController.popBackStack() },
                        onDeleted = if (fromNotification) returnToActivities else ({
                            navController.popBackStack()
                            Unit
                        }),
                        fromNotification = fromNotification,
                        onNotificationBack = returnToActivities,
                        onRequestReturnClick = { navController.navigateSafely(Screen.ReturnRequest.createRoute(order.id)) },
                        onReviewRequestClick = { navController.navigateSafely(Screen.ReturnRequest.createRoute(order.id)) },
                        onWriteReviewClick = { navController.navigateSafely(Screen.WriteReview.createRoute(order.id)) },
                        onProductClick = { navController.navigateSafely(Screen.ProductDetail.createRoute(order.productId)) }
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (!orderLookupFinished) CircularProgressIndicator()
                        else Text("This order is unavailable for the signed-in account")
                    }
                }
            }

            composable(
                route = Screen.WriteReview.route,
                arguments = listOf(navArgument("orderId") { type = NavType.StringType })
            ) { entry ->
                val orderId = entry.arguments?.getString("orderId").orEmpty()
                val order = OrderRepository.orders.find { it.id == orderId }
                if (order != null) {
                    WriteReviewScreen(
                        order = order,
                        onBackClick = { navController.popBackStack() },
                        onSubmitted = { navController.popBackStack() }
                    )
                }
            }

            composable(
                route = Screen.ReturnRequest.route,
                arguments = listOf(navArgument("orderId") { type = NavType.StringType })
            ) { entry ->
                val orderId = entry.arguments?.getString("orderId").orEmpty()
                val order = OrderRepository.orders.find { it.id == orderId }
                val pickedAddressId by entry.savedStateHandle.getStateFlow<String?>("selected_address_id", null).collectAsState()
                val pickedLat by entry.savedStateHandle.getStateFlow<Double?>("picked_lat", null).collectAsState()
                val pickedLng by entry.savedStateHandle.getStateFlow<Double?>("picked_lng", null).collectAsState()
                val pickedAddress by entry.savedStateHandle.getStateFlow<String?>("picked_address", null).collectAsState()
                val pickedName by entry.savedStateHandle.getStateFlow<String?>("picked_name", null).collectAsState()
                val pickedMeetupLocation = if (pickedLat != null && pickedLng != null && pickedAddress != null) {
                    PickedLocation(pickedLat!!, pickedLng!!, pickedAddress!!, pickedName)
                } else null
                if (order != null) {
                    ReturnRequestScreen(
                        order = order,
                        onBackClick = { navController.popBackStack() },
                        onFinished = {
                            scope.launch { OrderRepository.refreshFromRemote() }
                            navController.popBackStack()
                        },
                        pickedAddressId = pickedAddressId,
                        onChangeAddress = { navController.navigateSafely(Screen.SelectShippingAddress.route) },
                        pickedMeetupLocation = pickedMeetupLocation,
                        onPickMeetupLocation = {
                            entry.savedStateHandle.remove<Double>("picked_lat")
                            entry.savedStateHandle.remove<Double>("picked_lng")
                            entry.savedStateHandle.remove<String>("picked_address")
                            entry.savedStateHandle.remove<String>("picked_name")
                            navController.navigateSafely(Screen.LocationPicker.route)
                        },
                        onContactSupportClick = { navController.navigateSafely(Screen.HelpCentre.route) }
                    )
                }
            }

            composable(Screen.SavedItems.route) {
                SavedItemsScreen(
                    onBackClick = { navController.popBackStack() },
                    onProductClick = { product ->
                        navController.navigateSafely(Screen.ProductDetail.createRoute(product.id))
                    }
                )
            }

            composable(Screen.PaymentMethods.route) {
                PaymentMethodsScreen(onBackClick = { navController.popBackStack() })
            }

            composable(Screen.ShippingAddress.route) {
                ShippingAddressScreen(
                    onBackClick = { navController.popBackStack() },
                    onAddAddress = { navController.navigateSafely(Screen.EditAddress.createRoute(Screen.EditAddress.NEW_ADDRESS_ID)) },
                    onEditAddress = { address -> navController.navigateSafely(Screen.EditAddress.createRoute(address.id)) }
                )
            }

            composable(Screen.SelectShippingAddress.route) {
                ShippingAddressScreen(
                    onBackClick = { navController.popBackStack() },
                    onAddAddress = { navController.navigateSafely(Screen.EditAddress.createRoute(Screen.EditAddress.NEW_ADDRESS_ID)) },
                    onEditAddress = { address -> navController.navigateSafely(Screen.EditAddress.createRoute(address.id)) },
                    selectionMode = true,
                    onAddressSelected = { address ->
                        navController.previousBackStackEntry?.savedStateHandle?.set("selected_address_id", address.id)
                        navController.popBackStack()
                    }
                )
            }

            composable(
                route = Screen.EditAddress.route,
                arguments = listOf(navArgument("addressId") { type = NavType.StringType })
            ) { entry ->
                val addressId = entry.arguments?.getString("addressId").orEmpty()
                val existing = if (addressId == Screen.EditAddress.NEW_ADDRESS_ID) null else AddressRepository.addresses.find { it.id == addressId }
                val pickedLat by entry.savedStateHandle.getStateFlow<Double?>("picked_lat", null).collectAsState()
                val pickedLng by entry.savedStateHandle.getStateFlow<Double?>("picked_lng", null).collectAsState()
                val pickedAddress by entry.savedStateHandle.getStateFlow<String?>("picked_address", null).collectAsState()
                val pickedName by entry.savedStateHandle.getStateFlow<String?>("picked_name", null).collectAsState()
                val pickedLocation = if (pickedLat != null && pickedLng != null && pickedAddress != null) {
                    PickedLocation(pickedLat!!, pickedLng!!, pickedAddress!!, pickedName)
                } else null

                EditAddressScreen(
                    existing = existing,
                    pickedLocation = pickedLocation,
                    onBackClick = { navController.popBackStack() },
                    onPickOnMap = { navController.navigateSafely(Screen.LocationPicker.route) },
                    onSaved = { navController.popBackStack() }
                )
            }

            composable(Screen.LocationPicker.route) {
                LocationPickerScreen(
                    initialLatitude = null,
                    initialLongitude = null,
                    onBackClick = { navController.popBackStack() },
                    onConfirm = { picked ->
                        navController.previousBackStackEntry?.savedStateHandle?.set("picked_lat", picked.latitude)
                        navController.previousBackStackEntry?.savedStateHandle?.set("picked_lng", picked.longitude)
                        navController.previousBackStackEntry?.savedStateHandle?.set("picked_address", picked.address)
                        navController.previousBackStackEntry?.savedStateHandle?.set("picked_name", picked.suggestedName)
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.MyListings.route) {
                MyListingsScreen(
                    onBackClick = { navController.popBackStack() },
                    onProductClick = { product ->
                        navController.navigateSafely(Screen.ProductDetail.createRoute(product.id))
                    }
                )
            }

            composable(Screen.Wallet.route) { entry ->
                val successMessage by entry.savedStateHandle.getStateFlow<String?>("wallet_success_message", null).collectAsState()
                WalletScreen(
                    onBackClick = { navController.popBackStack() },
                    onAddFundsClick = { navController.navigateSafely(Screen.WalletAddFundsAmount.route) },
                    onWithdrawClick = { navController.navigateSafely(Screen.WalletWithdrawAmount.route) },
                    successMessage = successMessage,
                    onSuccessMessageShown = { entry.savedStateHandle["wallet_success_message"] = null }
                )
            }

            composable(Screen.WalletAddFundsAmount.route) {
                WalletAddFundsAmountScreen(
                    onBackClick = { navController.popBackStack() },
                    onContinue = { amount -> navController.navigateSafely(Screen.WalletAddFundsPayment.createRoute(amount)) }
                )
            }

            composable(
                route = Screen.WalletAddFundsPayment.route,
                arguments = listOf(navArgument("amount") { type = NavType.StringType })
            ) { entry ->
                val amount = entry.arguments?.getString("amount")?.toDoubleOrNull() ?: 0.0
                WalletAddFundsPaymentScreen(
                    amount = amount,
                    onBackClick = { navController.popBackStack() },
                    onCompleted = {
                        navController.getBackStackEntry(Screen.Wallet.route)
                            .savedStateHandle["wallet_success_message"] = "${formatMoney(amount)} added to your wallet"
                        navController.popBackStack(Screen.Wallet.route, inclusive = false)
                    }
                )
            }

            composable(Screen.WalletWithdrawAmount.route) {
                WalletWithdrawAmountScreen(
                    onBackClick = { navController.popBackStack() },
                    onContinue = { amount -> navController.navigateSafely(Screen.WalletWithdrawDestination.createRoute(amount)) }
                )
            }

            composable(
                route = Screen.WalletWithdrawDestination.route,
                arguments = listOf(navArgument("amount") { type = NavType.StringType })
            ) { entry ->
                val amount = entry.arguments?.getString("amount")?.toDoubleOrNull() ?: 0.0
                WalletWithdrawDestinationScreen(
                    amount = amount,
                    onBackClick = { navController.popBackStack() },
                    onCompleted = {
                        navController.getBackStackEntry(Screen.Wallet.route)
                            .savedStateHandle["wallet_success_message"] = "Withdrawal of ${formatMoney(amount)} submitted"
                        navController.popBackStack(Screen.Wallet.route, inclusive = false)
                    }
                )
            }

            composable(Screen.Reviews.route) {
                ReviewsScreen(onBackClick = { navController.popBackStack() })
            }

            composable(Screen.Analytics.route) {
                AnalyticsScreen(onBackClick = { navController.popBackStack() })
            }

            composable(Screen.BrowseHistory.route) {
                BrowseHistoryScreen(
                    onBackClick = { navController.popBackStack() },
                    onProductClick = { product ->
                        navController.navigateSafely(Screen.ProductDetail.createRoute(product.id))
                    }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onBackClick = { navController.popBackStack() },
                    onAccountInfoClick = { navController.navigateSafely(Screen.AccountInfo.route) },
                    onRequirePassword = ::runOrRequirePassword
                )
            }

            composable(Screen.AccountInfo.route) { entry ->
                val successMessage by entry.savedStateHandle.getStateFlow<String?>("account_info_success_message", null).collectAsState()
                AccountInfoScreen(
                    onBackClick = { navController.popBackStack() },
                    onChangePasswordClick = { navController.navigateSafely(Screen.ChangePassword.route) },
                    onCreatePasswordClick = { navController.navigateSafely(Screen.CreatePassword.route) },
                    successMessage = successMessage,
                    onSuccessMessageShown = { entry.savedStateHandle["account_info_success_message"] = null }
                )
            }

            composable(Screen.ChangePassword.route) {
                ChangePasswordScreen(
                    email = AuthRepository.currentUser.value?.email.orEmpty(),
                    onBackClick = { navController.popBackStack() },
                    onChanged = {
                        navController.getBackStackEntry(Screen.AccountInfo.route)
                            .savedStateHandle["account_info_success_message"] = "Password changed"
                        navController.popBackStack(Screen.AccountInfo.route, inclusive = false)
                    }
                )
            }

            // Reached either straight from Account Information's "Create Password" button (no
            // confirmation needed — the user asked for this explicitly) or via [runOrRequirePassword]'s
            // confirm-first gate for Buy/Rent/List — [pendingAfterPassword] being non-null distinguishes
            // the two so this one route can serve both without knowing which screen pushed it.
            composable(Screen.CreatePassword.route) {
                CreatePasswordScreen(
                    onBackClick = { navController.popBackStack() },
                    onCreated = {
                        val pending = pendingAfterPassword
                        pendingAfterPassword = null
                        if (pending != null) {
                            navController.popBackStack()
                            pending()
                        } else {
                            navController.getBackStackEntry(Screen.AccountInfo.route)
                                .savedStateHandle["account_info_success_message"] = "Password created"
                            navController.popBackStack(Screen.AccountInfo.route, inclusive = false)
                        }
                    }
                )
            }

            composable(Screen.HelpCentre.route) {
                HelpCentreScreen(onBackClick = { navController.popBackStack() })
            }
        }
    }

    pendingLeaveNav?.let { navigate ->
        AlertDialog(
            onDismissRequest = { pendingLeaveNav = null },
            title = { Text("Leave without saving?") },
            text = { Text("You have unsaved changes. You can save them first, or leave without saving.") },
            confirmButton = {
                Button(onClick = {
                    pendingLeaveNav = null
                    navigate()
                }) {
                    Text("Leave")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        val changes = currentWizardChanges
                        pendingLeaveNav = null
                        if (changes != null) changes.onSaveAndLeave { navigate() } else navigate()
                    }) {
                        Text("Save")
                    }
                    TextButton(onClick = { pendingLeaveNav = null }) { Text("Cancel") }
                }
            }
        )
    }

    if (showCreatePasswordPrompt) {
        AlertDialog(
            onDismissRequest = {
                showCreatePasswordPrompt = false
                pendingAfterPassword = null
            },
            title = { Text("Create a password") },
            text = { Text("You signed in with Google. Create a password for your Gadget Mover account before you buy, rent, or list an item.") },
            confirmButton = {
                Button(onClick = {
                    showCreatePasswordPrompt = false
                    navController.navigateSafely(Screen.CreatePassword.route)
                }) {
                    Text("Create Password")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCreatePasswordPrompt = false
                    pendingAfterPassword = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}
