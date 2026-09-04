package com.example.gadgetmover.screen.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.gadgetmover.util.CountryCode
import com.example.gadgetmover.util.countryCodes

/**
 * A phone number input rendered as a single [OutlinedTextField] with the country-code picker
 * embedded as its leading icon, e.g. "[🇲🇾 +60 ▾] 12-345 6789" — one continuous outlined box
 * rather than two separate bordered controls glued together, so there's no risk of the two
 * pieces drifting out of height/baseline alignment (a real OutlinedTextField grows to fit large
 * accessibility font sizes; a second, independently-bordered box next to it can't track that).
 * [localNumber] holds digits only (no dial code).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneNumberField(
    country: CountryCode,
    onCountryChange: (CountryCode) -> Unit,
    localNumber: String,
    onLocalNumberChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    enabled: Boolean = true,
    label: String = "Phone Number"
) {
    var menuExpanded by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = localNumber,
        onValueChange = { input -> onLocalNumberChange(input.filter { it.isDigit() }) },
        label = { Text(label) },
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        isError = isError,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        leadingIcon = {
            Box {
                Row(
                    modifier = Modifier
                        .clickable(enabled = enabled) { menuExpanded = true }
                        .padding(start = 12.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${country.flagEmoji} ${country.dialCode}", style = MaterialTheme.typography.bodyMedium)
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = "Choose country code")
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    countryCodes.forEach { option ->
                        DropdownMenuItem(
                            text = { Text("${option.flagEmoji} ${option.countryName} (${option.dialCode})") },
                            onClick = {
                                onCountryChange(option)
                                menuExpanded = false
                            }
                        )
                    }
                }
            }
        }
    )
}
