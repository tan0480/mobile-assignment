package com.example.gadgetmover

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.example.gadgetmover.data.ProductCache
import com.example.gadgetmover.data.ProductRepository
import com.example.gadgetmover.data.SettingsRepository
import com.example.gadgetmover.data.ThemePreferences
import com.example.gadgetmover.navigation.GadgetMoverNavGraph
import com.example.gadgetmover.ui.theme.GadgetMoverTheme

// FragmentActivity (not just ComponentActivity) — androidx.biometric.BiometricPrompt's classic
// constructor requires one; see util/BiometricAuthenticator.kt.
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        SettingsRepository.forceDarkMode.value = ThemePreferences.load(this)
        ProductRepository.loadCache(ProductCache.load(this))
        setContent {
            val forcedDark = SettingsRepository.forceDarkMode.value
            GadgetMoverTheme(darkTheme = forcedDark ?: isSystemInDarkTheme()) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    GadgetMoverNavGraph()
                }
            }
        }
    }
}
