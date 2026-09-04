package com.example.gadgetmover.navigation

sealed class Screen(val route: String) {
    object Intro : Screen("intro")
    object Login : Screen("login")
    object Register : Screen("register")
    object ForgotPassword : Screen("forgot_password")
    object OtpVerification : Screen("otp_verification/{contact}/{purpose}") {
        fun createRoute(contact: String, purpose: String) = "otp_verification/$contact/$purpose"
    }
    object ResetPassword : Screen("reset_password/{email}") {
        fun createRoute(email: String) = "reset_password/$email"
    }

    object Home : Screen("home")
    object Explore : Screen("explore?openCategoryPicker={openCategoryPicker}") {
        fun createRoute(openCategoryPicker: Boolean = false) = "explore?openCategoryPicker=$openCategoryPicker"
    }
    object DynamicFilter : Screen("dynamic_filter")
    object SearchUsers : Screen("search_users")
    object LocationRadiusFilter : Screen("location_radius_filter")
    object ListingWizard : Screen("listing_wizard/{editProductId}") {
        const val NEW_LISTING_ID = "new"
        fun createRoute(editProductId: String = NEW_LISTING_ID) = "listing_wizard/$editProductId"
    }

    object ProductDetail : Screen("product_detail/{productId}") {
        fun createRoute(productId: String) = "product_detail/$productId"
    }

    object SellerProfile : Screen("seller_profile/{sellerId}") {
        fun createRoute(sellerId: String) = "seller_profile/$sellerId"
    }

    object BuyConfirmation : Screen("buy_confirmation/{orderId}") {
        fun createRoute(orderId: String) = "buy_confirmation/$orderId"
    }

    object Checkout : Screen("checkout/{productId}/{transactionType}/{negotiatedPrice}") {
        /** Sentinel meaning "no negotiated price" — matches the sentinel-default pattern used by [ListingWizard.NEW_LISTING_ID]. */
        const val NO_NEGOTIATED_PRICE = -1.0
        fun createRoute(productId: String, transactionType: String, negotiatedPrice: Double = NO_NEGOTIATED_PRICE) =
            "checkout/$productId/$transactionType/$negotiatedPrice"
    }

    object LocationPicker : Screen("location_picker")

    object MessageInbox : Screen("message_inbox")
    object ChatDetail : Screen("chat_detail/{threadId}") {
        fun createRoute(threadId: String) = "chat_detail/$threadId"
    }
    object Notifications : Screen("notifications")

    object Profile : Screen("profile")
    object AccountInfo : Screen("account_info")
    object ChangePassword : Screen("change_password")
    object CreatePassword : Screen("create_password")
    object MyActivities : Screen("my_activities")
    object OrderDetail : Screen("order_detail/{orderId}") {
        fun createRoute(orderId: String) = "order_detail/$orderId"
    }
    object ReturnRequest : Screen("return_request/{orderId}") {
        fun createRoute(orderId: String) = "return_request/$orderId"
    }
    object WriteReview : Screen("write_review/{orderId}") {
        fun createRoute(orderId: String) = "write_review/$orderId"
    }
    object SavedItems : Screen("saved_items")

    object PaymentMethods : Screen("payment_methods")
    object ShippingAddress : Screen("shipping_address")
    /** Same address book as [ShippingAddress], but for picking one address for the checkout in progress rather than managing the book — tapping a row selects it and returns to Checkout instead of just toggling its default flag. */
    object SelectShippingAddress : Screen("select_shipping_address")
    object EditAddress : Screen("edit_address/{addressId}") {
        fun createRoute(addressId: String) = "edit_address/$addressId"
        const val NEW_ADDRESS_ID = "new"
    }
    object MyListings : Screen("my_listings")
    object Wallet : Screen("wallet")
    object WalletAddFundsAmount : Screen("wallet_add_funds_amount")
    object WalletAddFundsPayment : Screen("wallet_add_funds_payment/{amount}") {
        fun createRoute(amount: Double) = "wallet_add_funds_payment/$amount"
    }
    object WalletWithdrawAmount : Screen("wallet_withdraw_amount")
    object WalletWithdrawDestination : Screen("wallet_withdraw_destination/{amount}") {
        fun createRoute(amount: Double) = "wallet_withdraw_destination/$amount"
    }
    object Reviews : Screen("reviews")
    object SellerReviews : Screen("seller_reviews/{sellerId}") {
        fun createRoute(sellerId: String) = "seller_reviews/$sellerId"
    }
    object Analytics : Screen("analytics")
    object BrowseHistory : Screen("browse_history")
    object Settings : Screen("settings")
    object HelpCentre : Screen("help_centre")
}
