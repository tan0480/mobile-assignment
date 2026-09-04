package com.example.gadgetmover.screen.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.gadgetmover.data.AddressRepository
import com.example.gadgetmover.model.Address
import com.example.gadgetmover.screen.components.PickedLocation
import com.example.gadgetmover.util.isPlausiblePhoneNumber
import kotlinx.coroutines.launch

/**
 * Add/Edit Address — reused for both flows (spec §7): [existing] null means "Add Address",
 * non-null means "Edit". [pickedLocation], when non-null, is the (lat, lng, formattedAddress)
 * just returned by the Google Maps picker (§5) and pre-fills [fullAddress]/coordinates.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAddressScreen(
    existing: Address?,
    pickedLocation: PickedLocation?,
    onBackClick: () -> Unit,
    onPickOnMap: () -> Unit,
    onSaved: () -> Unit
) {
    // rememberSaveable (not remember): Navigation-Compose fully disposes this screen's composition
    // while the Location Picker is on top of it, so these need to survive that round trip rather
    // than reset — same fix as ListingWizardScreen's draft.
    var label by rememberSaveable { mutableStateOf(existing?.label.orEmpty()) }
    var receiverName by rememberSaveable { mutableStateOf(existing?.receiverName.orEmpty()) }
    var phoneNumber by rememberSaveable { mutableStateOf(existing?.phoneNumber.orEmpty()) }
    var fullAddress by rememberSaveable { mutableStateOf(existing?.fullAddress.orEmpty()) }
    var latitude by rememberSaveable { mutableStateOf(existing?.latitude) }
    var longitude by rememberSaveable { mutableStateOf(existing?.longitude) }
    var isSaving by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // A freshly picked location arrives as a one-shot value from the picker (cleared before each
    // navigate-to-picker call), so apply it once here rather than as an initializer — rememberSaveable
    // restores the previously saved value on recomposition and would otherwise never see this update.
    LaunchedEffect(pickedLocation) {
        if (pickedLocation != null) {
            latitude = pickedLocation.latitude
            longitude = pickedLocation.longitude
            fullAddress = pickedLocation.address
        }
    }

    val phoneError = phoneNumber.isNotBlank() && !isPlausiblePhoneNumber(phoneNumber)
    val isValid = label.isNotBlank() && receiverName.isNotBlank() &&
        phoneNumber.isNotBlank() && !phoneError && fullAddress.isNotBlank()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (existing == null) "Add Address" else "Edit Address") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
        ) {
            LabeledField("Label (e.g. Home, Office)") {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }
            LabeledField("Receiver Name") {
                OutlinedTextField(
                    value = receiverName,
                    onValueChange = { receiverName = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }
            LabeledField("Phone Number") {
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it.filter { c -> c.isDigit() || c == '+' || c == '-' || c == ' ' } },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    isError = phoneError,
                    supportingText = if (phoneError) {
                        { Text("Enter a valid phone number") }
                    } else null,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
            }
            LabeledField("Full Address") {
                OutlinedTextField(
                    value = fullAddress,
                    onValueChange = { fullAddress = it; latitude = null; longitude = null },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    shape = RoundedCornerShape(12.dp),
                    placeholder = { Text("Unit, street, city, state, postcode") }
                )
            }
            OutlinedButton(
                onClick = onPickOnMap,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.padding(4.dp))
                Text(if (latitude != null) "Location pinned — pick again" else "Pick on map")
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    isSaving = true
                    scope.launch {
                        val success = if (existing == null) {
                            AddressRepository.add(label, receiverName, phoneNumber, fullAddress, latitude, longitude)
                        } else {
                            AddressRepository.update(
                                existing.copy(
                                    label = label,
                                    receiverName = receiverName,
                                    phoneNumber = phoneNumber,
                                    fullAddress = fullAddress,
                                    latitude = latitude,
                                    longitude = longitude
                                )
                            )
                        }
                        isSaving = false
                        if (success) {
                            onSaved()
                        } else {
                            snackbarHostState.showSnackbar("Couldn't save this address. Please try again.")
                        }
                    }
                },
                enabled = isValid && !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                } else {
                    Text("Save Address", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun LabeledField(label: String, field: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        field()
    }
}
