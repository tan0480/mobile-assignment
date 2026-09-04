package com.example.gadgetmover.screen.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun LoginRequiredDialog(onDismiss: () -> Unit, onLoginClick: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log in required") },
        text = { Text("Log in to your Gadget Mover account to continue.") },
        confirmButton = {
            Button(onClick = {
                onDismiss()
                onLoginClick()
            }) {
                Text("Log In")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
