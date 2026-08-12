package com.databelay.refwatch.wear.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.databelay.refwatch.R
import com.databelay.refwatch.common.Game
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LoadIcsScreen(
    onIcsLoaded: (List<Game>) -> Unit, // <<<< Callback with List<GameSettings>    // viewModel: SomeViewModel // ViewModel to handle loading logic
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val successMsg = stringResource(R.string.load_ics_sim_success)

        Text(
            stringResource(R.string.load_game_schedule),
            style = MaterialTheme.typography.title3,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            stringResource(R.string.load_ics_body),
            style = MaterialTheme.typography.body2,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Placeholder for actual loading mechanism
        Button(
            onClick = {
                isLoading = true
                message = null
                coroutineScope.launch {
                    // ** SIMULATED/PLACEHOLDER ICS LOADING **
                    // In a real app, this would involve:
                    // 1. Triggering file selection on a companion app.
                    // 2. Receiving file data via Wearable Data Layer.
                    // 3. OR, for testing, load from app assets.
                    try {
                        // val games: List<GameSettings> = parseIcsAndConvertToGameSettings(inputStream)
                        delay(2000) // Simulate loading
                        // val games = parseIcsStream(inputStream)
                        // onIcsLoadedAndParsed(games)
                        message = successMsg
                    } catch (e: Exception) {
                        message = context.getString(R.string.load_ics_error, e.message ?: "")
                    } finally {
                        isLoading = false
                    }
                }
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            if (isLoading) {
                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
            } else {
                Text(stringResource(R.string.load_example_file))
            }
        }

        message?.let {
            Spacer(Modifier.height(16.dp))
            Text(it, style = MaterialTheme.typography.caption1, textAlign = TextAlign.Center)
        }
    }
}