package com.databelay.refwatch.wear.presentation.screens

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.AlertDialogDefaults
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.databelay.refwatch.R

@Composable
fun UnifiedConfirmationDialog(dialogInfo: ConfirmationDialogInfo) {
    Log.d("ConfirmationDialog", "Showing dialog: ${dialogInfo.title}")
    val confirmText = dialogInfo.confirmButtonText.ifEmpty { stringResource(R.string.confirm) }
    val dismissText = dialogInfo.dismissButtonText.ifEmpty { stringResource(R.string.dismiss) }
    AlertDialog(
        visible = true,
        onDismissRequest = {
            dialogInfo.onDismissDialogAction()
        },
        title = { Text(dialogInfo.title, color = MaterialTheme.colorScheme.primary) },
        dismissButton = {
            AlertDialogDefaults.DismissButton(
                onClick = {
                    dialogInfo.onDismissDialogAction()
                },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                )
            ) { Text(dismissText) }
        },
        text = { dialogInfo.text?.let { Text(it) } },
        confirmButton = {
            AlertDialogDefaults.ConfirmButton(
                onClick = {
                    dialogInfo.onConfirmAction()
                }
            ) { Text(confirmText) }
        },
    )
}