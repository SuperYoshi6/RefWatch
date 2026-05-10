package com.databelay.refwatch.wear.presentation.screens // Or your chosen package

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PauseCircleFilled
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.*
import com.databelay.refwatch.R
import com.databelay.refwatch.common.Game
import com.databelay.refwatch.common.GamePhase
import com.databelay.refwatch.common.hasTimer
import com.databelay.refwatch.common.readable
import com.databelay.refwatch.common.theme.RefWatchWearTheme
import com.databelay.refwatch.wear.presentation.utils.localizedName

@Composable
fun GameSettingsScreen(
    game: Game,
    onAttemptFinishGame: () -> Unit,
    onAttemptResetPeriodTimer: () -> Unit,
    onAttemptResetFullGame: () -> Unit,
    onViewLog: () -> Unit,
    onToggleTimer: () -> Unit,
    onAttemptEndPhase: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberScalingLazyListState()
    ScreenScaffold(
        scrollIndicator = {
            ScrollIndicator(
                modifier = Modifier.align(Alignment.CenterStart),
                state = listState
            )
        },
        modifier = modifier
            .fillMaxSize(),
        contentPadding = PaddingValues(2.dp),
    ) { contentPadding ->
        ScalingLazyColumn(
            state = listState,
            modifier = modifier
                .padding(horizontal = 8.dp, vertical = 0.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically)
        ) {

            item {
                Text(
                    "Spiel Menü",
                    style = MaterialTheme.typography.displaySmall,
                    textAlign = TextAlign.Center
                )
            }

            if (game.currentPhase.hasTimer()) {
                item {
                    Button(
                        onClick = onToggleTimer,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (game.isTimerRunning) Color(0xFFFF6822) else Color.Green,
                            contentColor = Color.Black
                        ),
                    ) {
                        Icon(
                            imageVector = if (game.isTimerRunning) Icons.Filled.PauseCircleFilled else Icons.Filled.PlayCircleFilled,
                            contentDescription = if (game.isTimerRunning) "Pause Timer" else "Start Timer",
                        )
                    }
                }
                item {
                    Button(
                        onClick = onAttemptEndPhase,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        modifier = Modifier.fillMaxWidth(),

                        ) {
                        Text(
                            text = stringResource(R.string.end_phase_action, game.currentPhase.localizedName()),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = onAttemptFinishGame,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        "Spiel beenden",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item {
                Button(
                    onClick = onAttemptResetPeriodTimer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        "Halbzeit zurücksetzen",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }


            item {
                Button(
                    onClick = onViewLog,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        "Spielprotokoll anzeigen",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item {
                Button(
                    onClick = onAttemptResetFullGame,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Spiel zurücksetzen")
                        Icon(
                            imageVector = Icons.Filled.PriorityHigh,
                            contentDescription = "Warnung"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewableAlertDialog(
    title: String,
    message: String? = null,
    confirmButtonText: String = "Ja, zurücksetzen",
    dismissButtonText: String = "Nein",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    RefWatchWearTheme { // Ensure the dialog is themed
        ConfirmationDialogInfo.FinishGame(
            onConfirm = { },
            onDialogClose = { }
        )
    }
}

@Preview(
    device = "id:wearos_small_round",
    name = "Beende Halbzeit Protokoll nsicht",
    showSystemUi = true,
    backgroundColor = 0xff000000,
    showBackground = true
)
@Composable
fun PreviewSettingsEndPhaseDialog() {
    PreviewableAlertDialog(
        title = "1. Halbzeit beenden",
        onConfirm = {},
        onDismiss = {}
    )
}

@Preview(
    device = "id:wearos_small_round",
    name = "Beende Spiel Protokoll Ansicht",
    showSystemUi = true,
    backgroundColor = 0xff000000,
    showBackground = true
)
@Composable
fun PreviewSettingsFinishGameDialog() {
    PreviewableAlertDialog(
        title = "Finish Game?",
        message = "Are you sure you want to end and save this game?",
        onConfirm = {},
        onDismiss = {}
    )
}

@Preview(
    device = "id:wearos_small_round",
    name = "Halbziet Protokoll ansicht zurücksetzen",
    showSystemUi = true,
    backgroundColor = 0xff000000,
    showBackground = true
)
@Composable
fun PreviewSettingsResetPeriodTimerDialog() {
    PreviewableAlertDialog(
        title = "Reset Timer?",
        message = "Reset timer for First Half?",
        onConfirm = {},
        onDismiss = {}
    )
}

@Preview(
    device = "id:wearos_small_round",
    name = "Reset Full Game Dialog Preview",
    showSystemUi = true,
    backgroundColor = 0xff000000,
    showBackground = true
)
@Composable
fun PreviewSettingsResetFullGameDialog() {
    PreviewableAlertDialog(
        title = "Spiel zurücksetzen?",
        message = "Diese Aktion wird alle Ergebnisse und das komplette Protokoll dieses Spiels löschen.",
        confirmButtonText = "Ja, zurücksetzen",
        onConfirm = {},
        onDismiss = {}
    )
}

@Preview(
    device = "id:wearos_small_round",
    name = "Verlängerungs Protokoll Ansicht",
    showSystemUi = true,
    backgroundColor = 0xff000000,
    showBackground = true
)
@Composable
fun PreviewSettingsExtraTimeDialog() {
    PreviewableAlertDialog(
        title = "Verlängerung?",
        confirmButtonText = "Ja",
        dismissButtonText = "Nein",
        onConfirm = {},
        onDismiss = {}
    )
}


@Preview(
    device = "id:wearos_small_round",
    showSystemUi = true,
    backgroundColor = 0xff000000,
    showBackground = true
)
@Composable
fun SettingsPageContentPreview() {
    RefWatchWearTheme {
        GameSettingsScreen(
            game = Game.defaults().copy(currentPhase = GamePhase.FIRST_HALF, isTimerRunning = true),
            onAttemptFinishGame = {},
            onAttemptResetPeriodTimer = {},
            onAttemptResetFullGame = {},
            onViewLog = {},
            onToggleTimer = {},
            onAttemptEndPhase = {},
            modifier = Modifier.fillMaxSize()
        )
    }
}
