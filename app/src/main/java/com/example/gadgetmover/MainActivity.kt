package com.example.gadgetmover

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.example.gadgetmover.data.ProductCache
import com.example.gadgetmover.data.ProductRepository
import com.example.gadgetmover.data.SettingsRepository
import com.example.gadgetmover.data.ThemePreferences
import com.example.gadgetmover.navigation.GadgetMoverNavGraph
import com.example.gadgetmover.notification.SystemNotifier
import com.example.gadgetmover.ui.theme.GadgetMoverTheme

// FragmentActivity (not just ComponentActivity) — androidx.biometric.BiometricPrompt's classic
// constructor requires one; see util/BiometricAuthenticator.kt.
class MainActivity : FragmentActivity() {
    private val pendingNotificationOrderId = mutableStateOf<String?>(null)
    private val pendingNotificationRecipientId = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        acceptNotificationIntent(intent)
        enableEdgeToEdge()
        SettingsRepository.forceDarkMode.value = ThemePreferences.load(this)
        ProductRepository.loadCache(ProductCache.load(this))
        setContent {
            val forcedDark = SettingsRepository.forceDarkMode.value
            GadgetMoverTheme(darkTheme = forcedDark ?: isSystemInDarkTheme()) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    GadgetMoverNavGraph(
                        notificationOrderId = pendingNotificationOrderId.value,
                        notificationRecipientUserId = pendingNotificationRecipientId.value,
                        onNotificationOrderConsumed = {
                            pendingNotificationOrderId.value = null
                            pendingNotificationRecipientId.value = null
                            intent?.removeExtra(SystemNotifier.EXTRA_ORDER_ID)
                            intent?.removeExtra(SystemNotifier.EXTRA_FROM_NOTIFICATION)
                            intent?.removeExtra(SystemNotifier.EXTRA_RECIPIENT_USER_ID)
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        acceptNotificationIntent(intent)
    }

    private fun acceptNotificationIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(SystemNotifier.EXTRA_FROM_NOTIFICATION, false) == true) {
            pendingNotificationOrderId.value = intent.getStringExtra(SystemNotifier.EXTRA_ORDER_ID)
            pendingNotificationRecipientId.value = intent.getStringExtra(SystemNotifier.EXTRA_RECIPIENT_USER_ID)
        }
    }
}
