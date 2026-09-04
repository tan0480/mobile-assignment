package com.example.gadgetmover.navigation

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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
import com.example.gadgetmover.model.OtpPurpose
import com.example.gadgetmover.model.ProductCategory
import com.example.gadgetmover.model.RentalOrder
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
import com.example.gadgetmover.screen.home.HomeScreen
import com.example.gadgetmover.screen.listing.ListingWizardScreen
import com.example.gadgetmover.screen.listing.WizardUnsavedChanges
import com.example.gadgetmover.screen.product.BuyConfirmationScreen
import com.example.gadgetmover.screen.product.ProductDetailScreen
import com.example.gadgetmover.screen.profile.SellerProfileScreen
import com.example.gadgetmover.screen.components.CreatePasswordDialog
import com.example.gadgetmover.screen.components.LocationPickerScreen
import com.example.gadgetmover.screen.components.PickedLocation
import com.example.gadgetmover.screen.components.RequestStartupPermissions
import com.example.gadgetmover.screen.profile.AccountInfoScreen
import com.example.gadgetmover.screen.profile.ChangePasswordScreen
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
import com.example.gadgetmover.util.formatMoney

private val bottomBarRoutes = setOf(
    Screen.Home.route,
    Screen.Explore.route,
    Screen.ListingWizard.route,
    Screen.MessageInbox.route,
    Screen.Profile.route
)

@Composable
fun GadgetMoverNavGraph() {
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
    val scope = rememberCoroutineScope()

    // The listing wizard (new-listing draft or an in-progress Edit) reports its unsaved-changes
    // state here (null when there's nothing worth saving) so the bottom nav bar's other four tabs
    // can be guarded behind a "leave without saving?" prompt instead of silently discarding it —
    // see `interceptNavigation` below.
    var currentWizardChanges by remember { mutableStateOf<WizardUnsavedChanges?>(null) }
    var pendingLeaveNav by remember { mutableStateOf<(() -> Unit)?>(null) }

    // A Google sign-in account has no Gadget Mover password yet (see User.hasPassword) — Buy,
    // Rent, and starting a new listing all funnel through this before they're allowed to proceed.
    var pendingAfterPassword by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showCreatePasswordDialog by remember { mutableStateOf(false) }
    fun runOrRequirePassword(action: () -> Unit) {
        if (AuthRepository.currentUser.value?.hasPassword == false) {
            pendingAfterPassword = action
            showCreatePasswordDialog = true
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
            // .padding(padding) alone physically pushes every screen below the status bar, which
            // is all Home/Explore/etc. need since they have no top bar of their own. But screens
            // that DO have their own inner Scaffold+TopAppBar (ChatDetailScreen, ProductDetail,
            // ...) read WindowInsets.statusBars directly to size that bar, and padding() doesn't
            // tell the WindowInsets system that inset was already consumed here — so those screens
            // reserved the status bar height a second time on top of this padding, doubling the
            // gap above their top bar. consumeWindowInsets marks it consumed for descendants
            // without touching the physical .padding() that the top-bar-less screens still need.
            modifier = Modifier.padding(padding).consumeWindowInsets(padding)
        ) {
            composable(Screen.Intro.route) {
                IntroScreen(
                    onStartBrowsingClick = {
                        OnboardingPreferences.markIntroSeen(context)
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Intro.route) { inclusive = true }
                        }
                    },
                    onLoginClick = {
                        OnboardingPreferences.markIntroSeen(context)
                        navController.navigate(Screen.Login.route)
                    }
                )
            }
            composable(Screen.Login.route) {
                LoginScreen(
                    onBackClick = { navController.popBackStack() },
                    onLoginSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(0)
                        }
                    },
                    onRegisterClick = { navController.navigate(Screen.Register.route) },
                    onForgotPasswordClick = { navController.navigate(Screen.ForgotPassword.route) }
                )
            }
            composable(Screen.Register.route) {
                RegisterScreen(
                    onBackClick = { navController.popBackStack() },
                    onOtpRequired = { email ->
                        navController.navigate(Screen.OtpVerification.createRoute(email, OtpPurpose.REGISTRATION.name))
                    },
                    onLoginClick = { navController.navigate(Screen.Login.route) }
                )
            }
            composable(Screen.ForgotPassword.route) {
                ForgotPasswordScreen(
                    onBackClick = { navController.popBackStack() },
                    onSendCode = { email ->
                        navController.navigate(Screen.OtpVerification.createRoute(email, OtpPurpose.FORGOT_PASSWORD.name))
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
                            OtpPurpose.REGISTRATION -> navController.navigate(Screen.Home.route) {
                                popUpTo(0)
                            }
                            OtpPurpose.FORGOT_PASSWORD -> navController.navigate(Screen.ResetPassword.createRoute(contact)) {
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
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0)
                        }
                    }
                )
            }

            composable(Screen.Home.route) {
                HomeScreen(
                    onProductClick = { product ->
                        navController.navigate(Screen.ProductDetail.createRoute(product.id))
                    },
                    onCategoryClick = { category ->
                        pendingCategory = category
                        pendingQuery = ""
                        pendingTransactionType = null
                        navController.navigate(Screen.Explore.route)
                    },
                    onSearchSubmit = { query ->
                        pendingQuery = query
                        pendingCategory = null
                        pendingTransactionType = null
                        navController.navigate(Screen.Explore.route)
                    },
                    onSeeAllFeatured = {
                        pendingQuery = ""
                        pendingCategory = null
                        pendingTransactionType = null
                        navController.navigate(Screen.Explore.route)
                    },
                    onLoginClick = { navController.navigate(Screen.Login.route) }
                )
            }

            composable(Screen.Explore.route) {
                ExploreScreen(
                    initialQuery = pendingQuery,
                    initialCategory = pendingCategory,
                    initialTransactionType = pendingTransactionType,
                    onProductClick = { product ->
                        navController.navigate(Screen.ProductDetail.createRoute(product.id))
                    },
                    onUserClick = { user ->
                        navController.navigate(Screen.SellerProfile.createRoute(user.id))
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
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Home.route) { inclusive = false }
                            }
                        }
                    },
                    onLoginClick = { navController.navigate(Screen.Login.route) },
                    pickedMeetupLocation = pickedLocation,
                    onPickMeetupLocation = {
                        entry.savedStateHandle.remove<Double>("picked_lat")
                        entry.savedStateHandle.remove<Double>("picked_lng")
                        entry.savedStateHandle.remove<String>("picked_address")
                        entry.savedStateHandle.remove<String>("picked_name")
                        navController.navigate(Screen.LocationPicker.route)
                    },
                    onUnsavedChangesChanged = { currentWizardChanges = it }
                )
            }

            composable(
                route = Screen.ProductDetail.route,
                arguments = listOf(navArgument("productId") { type = NavType.StringType })
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
                            runOrRequirePassword { navController.navigate(Screen.Checkout.createRoute(it.id, ListingType.BUY.name)) }
                        },
                        onRentClick = {
                            runOrRequirePassword { navController.navigate(Screen.Checkout.createRoute(it.id, ListingType.RENT.name)) }
                        },
                        onMessageSellerClick = {
                            scope.launch {
                                val thread = ChatRepository.findOrCreateThreadForProduct(it)
                                navController.navigate(Screen.ChatDetail.createRoute(thread.id))
                            }
                        },
                        onEditClick = { navController.navigate(Screen.ListingWizard.createRoute(it.id)) },
                        onLoginRequired = { navController.navigate(Screen.Login.route) },
                        onSellerClick = { navController.navigate(Screen.SellerProfile.createRoute(it.sellerId)) }
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
                    onProductClick = { navController.navigate(Screen.ProductDetail.createRoute(it.id)) }
                )
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
                        onChangeAddress = { navController.navigate(Screen.SelectShippingAddress.route) },
                        onOrderConfirmed = { order ->
                            navController.navigate(Screen.BuyConfirmation.createRoute(order.id)) {
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
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Home.route) { inclusive = false }
                            }
                        },
                        onViewActivitiesClick = {
                            pendingActivitiesTab = if (order is RentalOrder) 2 else 0
                            // Reset the back stack down to Profile first (mirroring the login-success
                            // reset pattern above) so back from My Activities lands on Profile — not on
                            // this now-stale confirmation screen or whatever preceded checkout.
                            navController.navigate(Screen.Profile.route) {
                                popUpTo(0)
                            }
                            navController.navigate(Screen.MyActivities.route)
                        }
                    )
                }
            }

            composable(Screen.MessageInbox.route) {
                MessageInboxScreen(
                    onNotificationsClick = { navController.navigate(Screen.Notifications.route) },
                    onThreadClick = { thread ->
                        navController.navigate(Screen.ChatDetail.createRoute(thread.id))
                    },
                    onLoginClick = { navController.navigate(Screen.Login.route) }
                )
            }

            composable(Screen.Notifications.route) {
                NotificationScreen(
                    onBackClick = { navController.popBackStack() },
                    onNotificationClick = { notification ->
                        notification.relatedThreadId?.let { threadId ->
                            navController.navigate(Screen.ChatDetail.createRoute(threadId))
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
                            navController.navigate(Screen.LocationPicker.route)
                        },
                        onLocationConsumed = {
                            entry.savedStateHandle.remove<Double>("picked_lat")
                            entry.savedStateHandle.remove<Double>("picked_lng")
                            entry.savedStateHandle.remove<String>("picked_address")
                            entry.savedStateHandle.remove<String>("picked_name")
                        },
                        onProductClick = { productId ->
                            navController.navigate(Screen.ProductDetail.createRoute(productId))
                        },
                        onNegotiatedCheckout = { productId, type, price ->
                            runOrRequirePassword { navController.navigate(Screen.Checkout.createRoute(productId, type.name, price)) }
                        }
                    )
                }
            }

            composable(Screen.Profile.route) {
                ProfileScreen(
                    onQuickActionClick = { action ->
                        when (action) {
                            ProfileQuickAction.MY_LISTINGS -> navController.navigate(Screen.MyListings.route)
                            ProfileQuickAction.PURCHASES -> {
                                pendingActivitiesTab = 0
                                navController.navigate(Screen.MyActivities.route)
                            }
                            ProfileQuickAction.SALES -> {
                                pendingActivitiesTab = 1
                                navController.navigate(Screen.MyActivities.route)
                            }
                            ProfileQuickAction.RENTALS -> {
                                pendingActivitiesTab = 2
                                navController.navigate(Screen.MyActivities.route)
                            }
                            ProfileQuickAction.LEASES -> {
                                pendingActivitiesTab = 3
                                navController.navigate(Screen.MyActivities.route)
                            }
                            ProfileQuickAction.WALLET -> runOrRequirePassword { navController.navigate(Screen.Wallet.route) }
                            ProfileQuickAction.SAVED_ITEMS -> navController.navigate(Screen.SavedItems.route)
                            ProfileQuickAction.REVIEWS -> navController.navigate(Screen.Reviews.route)
                            ProfileQuickAction.ANALYTICS -> navController.navigate(Screen.Analytics.route)
                            ProfileQuickAction.BROWSE_HISTORY -> navController.navigate(Screen.BrowseHistory.route)
                            ProfileQuickAction.SETTINGS -> navController.navigate(Screen.Settings.route)
                            ProfileQuickAction.HELP_CENTRE -> navController.navigate(Screen.HelpCentre.route)
                        }
                    },
                    onAccountSupportClick = { action ->
                        when (action) {
                            AccountSupportAction.PAYMENT_METHODS -> runOrRequirePassword { navController.navigate(Screen.PaymentMethods.route) }
                            AccountSupportAction.SHIPPING_ADDRESS -> navController.navigate(Screen.ShippingAddress.route)
                        }
                    },
                    onLogoutClick = {
                        scope.launch {
                            AuthRepository.logout()
                            navController.navigate(Screen.Intro.route) {
                                popUpTo(0)
                            }
                        }
                    },
                    onLoginClick = { navController.navigate(Screen.Login.route) },
                    onRegisterClick = { navController.navigate(Screen.Register.route) }
                )
            }

            composable(Screen.MyActivities.route) {
                MyActivitiesScreen(
                    onBackClick = { navController.popBackStack() },
                    onOrderClick = { order -> navController.navigate(Screen.OrderDetail.createRoute(order.id)) },
                    onRequestReturnClick = { order -> navController.navigate(Screen.ReturnRequest.createRoute(order.id)) },
                    initialTab = pendingActivitiesTab
                )
            }

            composable(
                route = Screen.OrderDetail.route,
                arguments = listOf(navArgument("orderId") { type = NavType.StringType })
            ) { entry ->
                val orderId = entry.arguments?.getString("orderId").orEmpty()
                val order = OrderRepository.orders.find { it.id == orderId }
                if (order != null) {
                    OrderDetailScreen(
                        order = order,
                        onBackClick = { navController.popBackStack() },
                        onDeleted = { navController.popBackStack() },
                        onRequestReturnClick = { navController.navigate(Screen.ReturnRequest.createRoute(order.id)) },
                        onReviewRequestClick = { navController.navigate(Screen.ReturnRequest.createRoute(order.id)) }
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
                        onChangeAddress = { navController.navigate(Screen.SelectShippingAddress.route) },
                        pickedMeetupLocation = pickedMeetupLocation,
                        onPickMeetupLocation = {
                            entry.savedStateHandle.remove<Double>("picked_lat")
                            entry.savedStateHandle.remove<Double>("picked_lng")
                            entry.savedStateHandle.remove<String>("picked_address")
                            entry.savedStateHandle.remove<String>("picked_name")
                            navController.navigate(Screen.LocationPicker.route)
                        },
                        onContactSupportClick = { navController.navigate(Screen.HelpCentre.route) }
                    )
                }
            }

            composable(Screen.SavedItems.route) {
                SavedItemsScreen(
                    onBackClick = { navController.popBackStack() },
                    onProductClick = { product ->
                        navController.navigate(Screen.ProductDetail.createRoute(product.id))
                    }
                )
            }

            composable(Screen.PaymentMethods.route) {
                PaymentMethodsScreen(onBackClick = { navController.popBackStack() })
            }

            composable(Screen.ShippingAddress.route) {
                ShippingAddressScreen(
                    onBackClick = { navController.popBackStack() },
                    onAddAddress = { navController.navigate(Screen.EditAddress.createRoute(Screen.EditAddress.NEW_ADDRESS_ID)) },
                    onEditAddress = { address -> navController.navigate(Screen.EditAddress.createRoute(address.id)) }
                )
            }

            composable(Screen.SelectShippingAddress.route) {
                ShippingAddressScreen(
                    onBackClick = { navController.popBackStack() },
                    onAddAddress = { navController.navigate(Screen.EditAddress.createRoute(Screen.EditAddress.NEW_ADDRESS_ID)) },
                    onEditAddress = { address -> navController.navigate(Screen.EditAddress.createRoute(address.id)) },
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
                    onPickOnMap = { navController.navigate(Screen.LocationPicker.route) },
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
                        navController.navigate(Screen.ProductDetail.createRoute(product.id))
                    }
                )
            }

            composable(Screen.Wallet.route) { entry ->
                val successMessage by entry.savedStateHandle.getStateFlow<String?>("wallet_success_message", null).collectAsState()
                WalletScreen(
                    onBackClick = { navController.popBackStack() },
                    onAddFundsClick = { navController.navigate(Screen.WalletAddFundsAmount.route) },
                    onWithdrawClick = { navController.navigate(Screen.WalletWithdrawAmount.route) },
                    successMessage = successMessage,
                    onSuccessMessageShown = { entry.savedStateHandle["wallet_success_message"] = null }
                )
            }

            composable(Screen.WalletAddFundsAmount.route) {
                WalletAddFundsAmountScreen(
                    onBackClick = { navController.popBackStack() },
                    onContinue = { amount -> navController.navigate(Screen.WalletAddFundsPayment.createRoute(amount)) }
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
                    onContinue = { amount -> navController.navigate(Screen.WalletWithdrawDestination.createRoute(amount)) }
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
                        navController.navigate(Screen.ProductDetail.createRoute(product.id))
                    }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onBackClick = { navController.popBackStack() },
                    onAccountInfoClick = { navController.navigate(Screen.AccountInfo.route) },
                    onRequirePassword = ::runOrRequirePassword
                )
            }

            composable(Screen.AccountInfo.route) { entry ->
                val successMessage by entry.savedStateHandle.getStateFlow<String?>("account_info_success_message", null).collectAsState()
                AccountInfoScreen(
                    onBackClick = { navController.popBackStack() },
                    onChangePasswordClick = { navController.navigate(Screen.ChangePassword.route) },
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

    if (showCreatePasswordDialog) {
        CreatePasswordDialog(
            onDismiss = {
                showCreatePasswordDialog = false
                pendingAfterPassword = null
            },
            onCreated = {
                showCreatePasswordDialog = false
                pendingAfterPassword?.invoke()
                pendingAfterPassword = null
            }
        )
    }
}
