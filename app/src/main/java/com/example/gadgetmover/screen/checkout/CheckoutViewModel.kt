package com.example.gadgetmover.screen.checkout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.gadgetmover.data.AddressRepository
import com.example.gadgetmover.data.CheckoutRepository
import com.example.gadgetmover.data.ProductRepository
import com.example.gadgetmover.data.WalletRepository
import com.example.gadgetmover.model.Address
import com.example.gadgetmover.model.CheckoutDetails
import com.example.gadgetmover.model.FulfillmentMethod
import com.example.gadgetmover.model.ListingType
import com.example.gadgetmover.model.MeetupLocation
import com.example.gadgetmover.model.Product
import com.example.gadgetmover.model.WalletTransactionType
import com.stripe.android.paymentsheet.PaymentSheetResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.roundToLong

/**
 * Everything `CheckoutScreen` needs to present PaymentSheet with saved-card support — bundled
 * into one value (rather than a bare client secret alongside separate customer/key state) so the
 * screen can never observe a client secret without its matching [PaymentSheet.CustomerConfiguration]
 * from the same [CheckoutViewModel.startPayment] call.
 */
data class PaymentSheetPresentation(val clientSecret: String, val customerId: String, val ephemeralKey: String)

/**
 * Owns every bit of Checkout's business logic — rental-day math, fulfillment/shipping/address
 * selection, price recomputation, Stripe payment orchestration, and order creation — so
 * `CheckoutScreen` only ever reads [uiState] and calls back into this class (spec §19).
 */
class CheckoutViewModel(
    private val product: Product,
    transactionType: ListingType,
    negotiatedPrice: Double?,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        CheckoutUiState(product = product, transactionType = transactionType, negotiatedPrice = negotiatedPrice, isLoading = true)
    )
    val uiState: StateFlow<CheckoutUiState> = _uiState

    /** Set once `startPayment()` succeeds — read by CheckoutScreen to present PaymentSheet. Not part of [CheckoutUiState] since it's a one-shot side-effect value, not persisted UI state. */
    private val _paymentSheetPresentation = MutableStateFlow<PaymentSheetPresentation?>(null)
    val paymentSheetPresentation: StateFlow<PaymentSheetPresentation?> = _paymentSheetPresentation

    init {
        // Only non-null if this instance was reconstructed from a killed-and-restarted process
        // (not a normal recomposition/rotation — those keep the same ViewModel instance via the
        // NavBackStackEntry's ViewModelStore) while a payment might have been mid-flight. Recover
        // it via the same verify-then-create-order path used for a normal retry, so a charge that
        // actually succeeded is never silently orphaned.
        val restoredPaymentIntentId = savedStateHandle.get<String>(KEY_LAST_PAYMENT_INTENT_ID)
        if (restoredPaymentIntentId != null) {
            _uiState.update { it.copy(lastPaymentIntentId = restoredPaymentIntentId, orderCreationFailedAfterPayment = true) }
            confirmAndCreateOrder()
        }
        viewModelScope.launch {
            AddressRepository.refreshFromRemote()
            val defaultMethod = product.fulfillmentMethods.firstOrNull()
            val bookedRanges = if (transactionType == ListingType.RENT) {
                CheckoutRepository.getBookedRentalRanges(product.id)
            } else {
                emptyList()
            }
            val defaultShippingTier = when {
                product.standardShippingFee != null -> ShippingTier.STANDARD
                product.expressShippingFee != null -> ShippingTier.EXPRESS
                else -> ShippingTier.STANDARD
            }
            _uiState.update {
                it.copy(
                    receivingMethod = defaultMethod,
                    returningMethod = if (transactionType == ListingType.RENT) defaultMethod else null,
                    shippingTier = defaultShippingTier,
                    selectedAddress = AddressRepository.addresses.find { addr -> addr.isDefault } ?: AddressRepository.addresses.firstOrNull(),
                    receivingMeetup = product.meetupLocations.firstOrNull(),
                    returningMeetup = product.meetupLocations.firstOrNull(),
                    bookedRanges = bookedRanges,
                    isLoading = false
                )
            }
        }
    }

    /**
     * Re-syncs the address book from [AddressRepository] on every screen resume (since selection
     * here isn't observed reactively), while preserving whatever address is currently picked —
     * including one just set via [selectAddress] from "Change Address", which this used to
     * unconditionally stomp back to the default a moment later because ON_RESUME also fires when
     * returning from that picker. Only falls back to the default (or first) address if the
     * previously selected one no longer exists, e.g. it was deleted elsewhere.
     */
    fun refreshSelectedAddress() {
        viewModelScope.launch {
            AddressRepository.refreshFromRemote()
            val current = _uiState.value.selectedAddress
            val resolved = current?.let { sel -> AddressRepository.addresses.find { it.id == sel.id } }
                ?: AddressRepository.addresses.find { it.isDefault }
                ?: AddressRepository.addresses.firstOrNull()
            if (resolved != null) _uiState.update { it.copy(selectedAddress = resolved) }
        }
    }

    /** Picks the rental start date — the buyer then sizes the window with [selectRentalDuration] rather than picking an end date directly. */
    fun selectRentalStart(startMillis: Long) {
        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        if (startMillis < startOfToday) {
            _uiState.update { it.copy(errorMessage = "Please pick a valid rental start date.") }
            return
        }
        _uiState.update {
            val duration = if (it.rentalDuration > 0) it.rentalDuration else 1
            val endMillis = startMillis + TimeUnit.DAYS.toMillis((duration - 1).toLong())
            it.copy(rentalStartMillis = startMillis, rentalDuration = duration, rentalEndMillis = endMillis, errorMessage = null)
        }
    }

    /** Duration stepper (spec §1) — bounded 1..90 days; recomputes the end date from the already-picked start. */
    fun selectRentalDuration(days: Int) {
        val bounded = days.coerceIn(1, 90)
        _uiState.update { state ->
            val start = state.rentalStartMillis
            val endMillis = start?.plus(TimeUnit.DAYS.toMillis((bounded - 1).toLong()))
            state.copy(rentalDuration = bounded, rentalEndMillis = endMillis)
        }
    }

    fun selectReceivingMethod(method: FulfillmentMethod) {
        _uiState.update { it.copy(receivingMethod = method) }
    }

    fun selectReturningMethod(method: FulfillmentMethod) {
        _uiState.update { it.copy(returningMethod = method) }
    }

    fun selectShippingTier(tier: ShippingTier) {
        _uiState.update { it.copy(shippingTier = tier) }
    }

    fun selectAddress(address: Address) {
        _uiState.update { it.copy(selectedAddress = address) }
    }

    fun selectReceivingMeetup(location: MeetupLocation) {
        _uiState.update { it.copy(receivingMeetup = location) }
    }

    fun selectReturningMeetup(location: MeetupLocation) {
        _uiState.update { it.copy(returningMeetup = location) }
    }

    fun selectPaymentMethod(method: CheckoutPaymentMethod) {
        if (!method.isAvailable) return
        _uiState.update { it.copy(paymentMethod = method) }
    }

    /** Step 1 of paying: for [CheckoutPaymentMethod.STRIPE], asks `create-payment-intent` for a client secret and hands it to CheckoutScreen to present PaymentSheet; for [CheckoutPaymentMethod.WALLET], there's no PaymentSheet step at all — see [payWithWallet]. */
    fun startPayment() {
        val state = _uiState.value
        if (!state.isReadyToPay) return
        if (state.paymentMethod == CheckoutPaymentMethod.WALLET) {
            payWithWallet(state)
            return
        }
        _uiState.update { it.copy(paymentState = PaymentState.CreatingPayment, errorMessage = null) }
        viewModelScope.launch {
            val customerResult = CheckoutRepository.getOrCreateStripeCustomer()
            val customer = customerResult.getOrNull()
            if (customerResult.isFailure || customer == null) {
                _uiState.update { it.copy(paymentState = PaymentState.Failed(checkoutUserMessage(customerResult.exceptionOrNull(), "Couldn't start payment. Please try again."))) }
                return@launch
            }
            val amountCents = (state.finalTotal * 100).roundToLong()
            CheckoutRepository.createPaymentIntent(amountCents, customer.customerId).fold(
                onSuccess = { info ->
                    _uiState.update { it.copy(paymentState = PaymentState.PaymentReady, lastPaymentIntentId = info.paymentIntentId) }
                    savedStateHandle[KEY_LAST_PAYMENT_INTENT_ID] = info.paymentIntentId
                    _paymentSheetPresentation.value = PaymentSheetPresentation(info.clientSecret, customer.customerId, customer.ephemeralKey)
                },
                onFailure = { error ->
                    _uiState.update { it.copy(paymentState = PaymentState.Failed(checkoutUserMessage(error, "Couldn't start payment. Please try again."))) }
                }
            )
        }
    }

    /**
     * Pays for the order out of the buyer's own wallet balance instead of a card — debits via
     * [WalletRepository.debit] (server-checked balance, see `debit_wallet` in schema.sql) and, only
     * if that succeeds, creates the order directly, skipping Stripe/PaymentSheet entirely. Reuses
     * [createOrder] with a synthetic `wallet-<uuid>` reference in place of a Stripe PaymentIntent id,
     * since [Order.paymentId] is just an opaque reference to whatever payment record backs it.
     */
    private fun payWithWallet(state: CheckoutUiState) {
        if (WalletRepository.balance.value < state.finalTotal) {
            _uiState.update { it.copy(paymentState = PaymentState.Failed("Insufficient wallet balance. Please add funds or choose another payment method.")) }
            return
        }
        _uiState.update { it.copy(paymentState = PaymentState.Processing, errorMessage = null) }
        viewModelScope.launch {
            val debited = WalletRepository.debit(WalletTransactionType.PURCHASE, state.finalTotal, "Payment for ${product.title}")
            if (!debited) {
                _uiState.update { it.copy(paymentState = PaymentState.Failed("Couldn't charge your wallet. Please try again.")) }
                return@launch
            }
            // Set before creating the order (matching startPayment()'s Stripe path) so that if
            // order creation fails, retry() -> confirmAndCreateOrder() has a reference to recover
            // with — the wallet was already debited, so this must never re-charge on retry.
            val walletPaymentRef = "wallet-${UUID.randomUUID()}"
            _uiState.update { it.copy(lastPaymentIntentId = walletPaymentRef) }
            savedStateHandle[KEY_LAST_PAYMENT_INTENT_ID] = walletPaymentRef
            createOrder(walletPaymentRef, _uiState.value)
        }
    }

    /** Called once PaymentSheet's own UI flow finishes. Never creates an order from this callback alone — always re-verifies the payment status server-side first (spec §12/§21). */
    fun onPaymentSheetResult(result: PaymentSheetResult) {
        when (result) {
            is PaymentSheetResult.Canceled -> _uiState.update { it.copy(paymentState = PaymentState.Cancelled) }
            is PaymentSheetResult.Failed -> _uiState.update { it.copy(paymentState = PaymentState.Failed(checkoutUserMessage(result.error, "Payment failed. Please try again."))) }
            is PaymentSheetResult.Completed -> confirmAndCreateOrder()
        }
    }

    /** Re-verifies the last payment and (re)attempts order creation — also used as the recovery action when payment succeeded but order creation previously failed. */
    fun confirmAndCreateOrder() {
        val state = _uiState.value
        val paymentIntentId = state.lastPaymentIntentId ?: return
        _uiState.update { it.copy(paymentState = PaymentState.Processing) }
        // A wallet payment has no Stripe PaymentIntent to re-verify — the balance check already
        // happened server-side inside debit_wallet() before this reference was ever set, so a
        // retry here just re-attempts order creation directly.
        if (paymentIntentId.startsWith("wallet-")) {
            viewModelScope.launch { createOrder(paymentIntentId, state) }
            return
        }
        viewModelScope.launch {
            CheckoutRepository.getPaymentStatus(paymentIntentId).fold(
                onSuccess = { status ->
                    when (status.status) {
                        "succeeded" -> createOrder(paymentIntentId, state)
                        "processing" -> _uiState.update { it.copy(paymentState = PaymentState.Pending) }
                        "canceled" -> _uiState.update { it.copy(paymentState = PaymentState.Cancelled) }
                        else -> _uiState.update { it.copy(paymentState = PaymentState.Failed("Payment status: ${status.status}. Please try again.")) }
                    }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(paymentState = PaymentState.Failed(checkoutUserMessage(error, "Couldn't confirm payment status."))) }
                }
            )
        }
    }

    private suspend fun createOrder(paymentIntentId: String, state: CheckoutUiState) {
        val checkoutDetails = CheckoutDetails(
            platformFee = state.platformFee,
            shippingFee = state.shippingFee,
            voucherDiscount = state.voucherDiscount,
            receivingMethod = state.receivingMethod ?: FulfillmentMethod.MEETUP,
            returningMethod = state.returningMethod,
            shippingAddressId = state.selectedAddress?.id,
            shippingReceiverName = state.selectedAddress?.receiverName,
            shippingPhoneNumber = state.selectedAddress?.phoneNumber,
            shippingFullAddress = state.selectedAddress?.fullAddress,
            returnReceiverName = if (state.returningMethod == FulfillmentMethod.SHIPPING) product.returnReceiverName else null,
            returnPhoneNumber = if (state.returningMethod == FulfillmentMethod.SHIPPING) product.returnPhoneNumber else null,
            returnFullAddress = if (state.returningMethod == FulfillmentMethod.SHIPPING) product.returnFullAddress else null,
            receivingMeetup = state.receivingMeetup,
            returningMeetup = state.returningMeetup,
            shippingTierUsed = if (state.receivingMethod == FulfillmentMethod.SHIPPING || state.returningMethod == FulfillmentMethod.SHIPPING) {
                state.shippingTier.name
            } else {
                null
            }
        )
        val order = if (state.transactionType == ListingType.RENT) {
            CheckoutRepository.placeRentalOrder(
                product = product,
                startDateMillis = state.rentalStartMillis ?: 0L,
                endDateMillis = state.rentalEndMillis ?: 0L,
                days = state.rentalDuration,
                dailyRate = product.rentalRatePerDay ?: 0.0,
                deposit = state.refundableDeposit,
                totalAmount = state.finalTotal,
                paymentId = paymentIntentId,
                checkout = checkoutDetails
            )
        } else {
            CheckoutRepository.placeBuyOrder(
                product = product,
                paymentId = paymentIntentId,
                checkout = checkoutDetails,
                totalAmount = state.finalTotal
            )
        }
        if (order != null) {
            if (state.transactionType != ListingType.RENT) {
                ProductRepository.markSold(order.id, product.id)
            }
            _uiState.update { it.copy(paymentState = PaymentState.Success, createdOrder = order, orderCreationFailedAfterPayment = false) }
            // Fully finalized into an order now — clear it so a later process death doesn't
            // re-trigger recovery and attempt to create a second order for the same charge.
            savedStateHandle[KEY_LAST_PAYMENT_INTENT_ID] = null
        } else {
            // Payment is confirmed PAID with Stripe — never re-charge here. orderCreationFailedAfterPayment
            // tells the UI to retry via confirmAndCreateOrder() (re-checks this same payment) rather
            // than startPayment() (which would create a brand-new PaymentIntent).
            _uiState.update {
                it.copy(
                    paymentState = PaymentState.Failed("Payment received (ref: ${paymentIntentId.takeLast(8)}) but we couldn't finalize your order. Tap Retry — you won't be charged again."),
                    orderCreationFailedAfterPayment = true
                )
            }
        }
    }

    /** What the CTA calls on a retry tap — re-checks an already-successful payment if order creation was the failure, otherwise starts a fresh payment attempt. */
    fun retry() {
        if (_uiState.value.orderCreationFailedAfterPayment) confirmAndCreateOrder() else startPayment()
    }

    private companion object {
        const val KEY_LAST_PAYMENT_INTENT_ID = "last_payment_intent_id"
    }
}

/** SavedStateHandle-aware so an in-flight payment survives the ViewModelStore being lost to process death (see [CheckoutViewModel]'s init block) — a plain [androidx.lifecycle.ViewModelProvider.Factory] can't supply a [SavedStateHandle]. */
fun checkoutViewModelFactory(product: Product, transactionType: ListingType, negotiatedPrice: Double? = null): ViewModelProvider.Factory =
    viewModelFactory {
        initializer {
            CheckoutViewModel(product, transactionType, negotiatedPrice, createSavedStateHandle())
        }
    }
