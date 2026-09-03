package com.example.gadgetmover.screen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
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
 * A phone number input with a country-code picker on the left and the local number field on the
 * right, e.g. "🇲🇾 +60 ▾ | 12-345 6789". [localNumber] holds digits only (no dial code).
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
    label: String = "Phone Number"
) {
    var menuExpanded by remember { mutableStateOf(false) }

    // A fixed 56.dp — Material 3's standard single-line OutlinedTextField height — rather than
    // IntrinsicSize.Min: OutlinedTextField's intrinsic-height reporting doesn't reliably match its
    // actual laid-out height (its label/decoration layout isn't a plain intrinsics-friendly
    // Composable), so matching against it via intrinsics left this box visibly shorter than the
    // field beside it.
    val fieldHeight = 56.dp

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.height(fieldHeight)) {
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .border(
                        width = 1.dp,
                        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                    .clickable { menuExpanded = true }
                    .padding(horizontal = 10.dp),
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

        Spacer(modifier = Modifier.width(8.dp))

        OutlinedTextField(
            value = localNumber,
            onValueChange = { input -> onLocalNumberChange(input.filter { it.isDigit() }) },
            label = { Text(label) },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            isError = isError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
        )
    }
}
