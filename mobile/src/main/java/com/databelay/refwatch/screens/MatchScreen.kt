package com.databelay.refwatch.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.databelay.refwatch.common.Game
import com.databelay.refwatch.common.formatTime
import com.databelay.refwatch.common.GamePhase
import com.databelay.refwatch.common.hasTimer
import com.databelay.refwatch.common.isPlayablePhase
import com.databelay.refwatch.common.readable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchScreen(
    game: Game,
    onNavigateBack: () -> Unit,
    onTakeCurrentTime: () -> Unit,
    onHalfTime: () -> Unit
) {
    val view = LocalView.current
    val keepScreenOn = game.currentPhase.hasTimer() || game.isTimerRunning || game.isStoppageTimerRunning
    DisposableEffect(view, keepScreenOn) {
        view.keepScreenOn = keepScreenOn
        onDispose { view.keepScreenOn = false }
    }

    var kickoffCountdownSeconds by remember { mutableStateOf<Int?>(null) }

    // Handle kickoff countdown
    LaunchedEffect(game.actualTimeElapsedInPeriodMillis, game.isTimerRunning) {
        if (game.actualTimeElapsedInPeriodMillis == 0L && !game.isTimerRunning && game.currentPhase.isPlayablePhase()) {
            // Start countdown if needed
            for (i in 5 downTo 1) {
                kickoffCountdownSeconds = i
                delay(1000)
            }
            kickoffCountdownSeconds = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${game.homeTeamName} vs ${game.awayTeamName}") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // Score Display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = game.homeTeamAbbr ?: game.homeTeamName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "${game.homeScore} : ${game.awayScore}",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Text(
                    text = game.awayTeamAbbr ?: game.awayTeamName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Timer Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (kickoffCountdownSeconds != null) {
                        Text(
                            text = kickoffCountdownSeconds.toString(),
                            style = MaterialTheme.typography.displayLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text("Anstoß", style = MaterialTheme.typography.labelLarge)
                    } else {
                        val regulationDuration = remember(game.currentPhase) { game.regulationPeriodDurationMillis() }
                        val isPlayablePhaseAndInAddedTime = game.currentPhase.isPlayablePhase() &&
                                game.actualTimeElapsedInPeriodMillis >= regulationDuration &&
                                regulationDuration > 0

                        val mainTimerText = if (isPlayablePhaseAndInAddedTime) {
                            game.actualTimeElapsedInPeriodMillis.formatTime()
                        } else {
                            (regulationDuration - game.actualTimeElapsedInPeriodMillis).coerceAtLeast(0).formatTime()
                        }

                        if (game.actualTimeElapsedInPeriodMillis == 0L && !game.isTimerRunning && game.currentPhase.isPlayablePhase()) {
                            Button(
                                onClick = { /* TODO: onKickOff */ },
                                modifier = Modifier.size(100.dp),
                                shape = androidx.compose.foundation.shape.CircleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text(
                                    "Anstoß",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            Text(
                                text = mainTimerText,
                                style = MaterialTheme.typography.displayLarge,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    isPlayablePhaseAndInAddedTime -> Color.Red
                                    game.isTimerRunning -> Color.White
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }

                        if (game.stoppageTimeMillis > 0) {
                            Text(
                                text = "+${game.stoppageTimeMillis.formatTime()}",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color(0xFF00E676)
                            )
                        }
                    }
                }
            }

            // Control Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = onTakeCurrentTime,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text("Take Current")
                }

                Button(
                    onClick = onHalfTime,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary
                    )
                ) {
                    Text("Half Time")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Phase Info
            Text(
                text = game.currentPhase.readable(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}