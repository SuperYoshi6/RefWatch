package com.databelay.refwatch.wear.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.databelay.refwatch.R

@Composable
fun PairingScreen(
    onPairingSuccess: () -> Unit,
    onCancel: () -> Unit,
    viewModel: PairingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onPairingSuccess()
        }
    }

    ScreenScaffold {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator()
                Spacer(Modifier.height(8.dp))
                Text("Generiere Code...", style = MaterialTheme.typography.bodySmall)
            } else if (uiState.errorMessage != null) {
                Text("Fehler: ${uiState.errorMessage}", color = Color.Red, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Button(onClick = onCancel) { Text("Zurück") }
            } else {
                Text(
                    "Uhr koppeln",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Gib diesen Code in der Handy-App ein:",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = uiState.code?.chunked(3)?.joinToString(" ") ?: "",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onCancel,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Text("Abbrechen")
                }
            }
        }
    }
}
