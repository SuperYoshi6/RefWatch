package com.databelay.refwatch.wear.presentation.screens

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.AlertDialogDefaults
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text

@Composable
fun UnifiedConfirmationDialog(dialogInfo: ConfirmationDialogInfo) {
    Log.d("ConfirmationDialog", "Showing dialog: ${dialogInfo.title}")
    AlertDialog(
        visible = true,
        onDismissRequest = {
            dialogInfo.onDismissDialogAction() // This handles specific dismiss logic + common close logic
        },
        title = { Text(dialogInfo.title, color = MaterialTheme.colorScheme.primary) },
        dismissButton = {
            AlertDialogDefaults.DismissButton(
                onClick = {
                    dialogInfo.onDismissDialogAction() // This handles specific dismiss logic + common close logic
                },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent, // Standard for dismiss
//                        contentColor = MaterialTheme.colorScheme.primary
                )
            ) { Text(dialogInfo.dismissButtonText) }
        },
        text = { dialogInfo.text?.let { Text(it) } },
        confirmButton = {
            AlertDialogDefaults.ConfirmButton(
                onClick = {
                    dialogInfo.onConfirmAction() // This handles specific confirm logic + common close logic
                }
            ) { Text(dialogInfo.confirmButtonText) }
        },
    )
}