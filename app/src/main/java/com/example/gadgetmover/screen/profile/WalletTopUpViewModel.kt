package com.example.gadgetmover.screen.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.gadgetmover.data.WalletRepository
import com.example.gadgetmover.screen.checkout.PaymentState
import com.example.gadgetmover.screen.checkout.checkoutUserMessage
import com.stripe.android.paymentsheet.PaymentSheetResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToLong

/**
 * Owns the "Add Funds" card-payment flow — mirrors [com.example.gadgetmover.screen.checkout.CheckoutViewModel]'s
 * two-step Stripe pattern (create a PaymentIntent, present PaymentSheet, then re-verify with the
 * server before trusting it) closely enough to reuse its [PaymentState]/[checkoutUserMessage],
 * since crediting a wallet needs exactly the same "never trust the client alone" guarantee as
 * creating a paid order does.
 */
class WalletTopUpViewModel(
    val amount: Double,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _paymentState = MutableStateFlow<PaymentState>(PaymentState.Idle)
    val paymentState: StateFlow<PaymentState> = _paymentState

    private val _clientSecretToPresent = MutableStateFlow<String?>(null)
    val clientSecretToPresent: StateFlow<String?> = _clientSecretToPresent

    private var lastPaymentIntentId: String? = savedStateHandle.get<String>(KEY_INTENT_ID)

    init {
        // Only non-null after a killed-and-restarted process while a charge might have been
        // mid-flight — recover via the same re-verify path a manual retry would use, so a
        // successful charge is never left uncredited just because the app died.
        lastPaymentIntentId?.let(::confirmTopUp)
    }

    fun startPayment() {
        if (_paymentState.value !is PaymentState.Idle && _paymentState.value !is PaymentState.Failed && _paymentState.value !is PaymentState.Cancelled) return
        _paymentState.value = PaymentState.CreatingPayment
        viewModelScope.launch {
            val amountCents = (amount * 100).roundToLong()
            WalletRepository.createTopUpIntent(amountCents).fold(
                onSuccess = { info ->
                    lastPaymentIntentId = info.paymentIntentId
                    savedStateHandle[KEY_INTENT_ID] = info.paymentIntentId
                    _paymentState.value = PaymentState.PaymentReady
                    _clientSecretToPresent.value = info.clientSecret
                },
                onFailure = { error ->
                    _paymentState.value = PaymentState.Failed(checkoutUserMessage(error, "Couldn't start payment. Please try again."))
                }
            )
        }
    }

    fun onPaymentSheetResult(result: PaymentSheetResult) {
        when (result) {
            is PaymentSheetResult.Canceled -> _paymentState.value = PaymentState.Cancelled
            is PaymentSheetResult.Failed -> _paymentState.value = PaymentState.Failed(checkoutUserMessage(result.error, "Payment failed. Please try again."))
            is PaymentSheetResult.Completed -> lastPaymentIntentId?.let(::confirmTopUp)
        }
    }

    private fun confirmTopUp(paymentIntentId: String) {
        _paymentState.value = PaymentState.Processing
        viewModelScope.launch {
            WalletRepository.confirmTopUp(paymentIntentId).fold(
                onSuccess = {
                    _paymentState.value = PaymentState.Success
                    savedStateHandle[KEY_INTENT_ID] = null
                },
                onFailure = { error ->
                    _paymentState.value = PaymentState.Failed(
                        checkoutUserMessage(error, "Payment received but we couldn't credit your wallet yet. Tap Retry — you won't be charged again.")
                    )
                }
            )
        }
    }

    /** What the CTA calls on a retry tap — re-confirms an already-started charge if one exists, otherwise starts a fresh one. */
    fun retry() {
        val existing = lastPaymentIntentId
        if (existing != null) confirmTopUp(existing) else startPayment()
    }

    private companion object {
        const val KEY_INTENT_ID = "wallet_topup_intent_id"
    }
}

fun walletTopUpViewModelFactory(amount: Double): ViewModelProvider.Factory =
    viewModelFactory {
        initializer {
            WalletTopUpViewModel(amount, createSavedStateHandle())
        }
    }
