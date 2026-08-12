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
import com.databelay.refwatch.common.PenaltyEvent
import com.databelay.refwatch.common.hasTimer
import com.databelay.refwatch.common.theme.RefWatchWearTheme
import com.databelay.refwatch.wear.presentation.utils.localizedName

/**
 * Returns true when the current game has just ended a penalty shootout
 * (i.e. the phase was advanced to GAME_ENDED by checkShootoutEndCondition,
 * not by a normal end-of-regulation). Used to restrict the in-game settings
 * menu after a shootout — the only allowed actions are viewing the log,
 * resetting the game, or finishing it.
 */
private fun Game.isPostShootout(): Boolean =
    currentPhase == GamePhase.GAME_ENDED &&
            events.any { it is PenaltyEvent }

@Composable
fun GameSettingsScreen(
    game: Game,
    onAttemptFinishGame: () -> Unit,
    onAttemptResetPeriodTimer: () -> Unit,
    onAttemptResetFullGame: () -> Unit,
    onViewLog: () -> Unit,
    onShowRoster: () -> Unit,
    onToggleTimer: () -> Unit,
    onAttemptEndPhase: () -> Unit,
    onAttemptAbortGame: () -> Unit,
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
                    text = if (game.isPostShootout()) stringResource(R.string.post_shootout_menu_title)
                           else stringResource(R.string.sp_menu),
                    style = MaterialTheme.typography.displaySmall,
                    textAlign = TextAlign.Center
                )
            }

            // In-game controls are hidden once the shootout is over.
            if (!game.isPostShootout() && game.currentPhase.hasTimer()) {
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
                            contentDescription = if (game.isTimerRunning) stringResource(R.string.timer_pause_content_desc) else stringResource(R.string.timer_play_content_desc),
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

            // --- NAVIGATION ACTIONS ---

            item {
                Button(
                    onClick = onViewLog,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        stringResource(R.string.view_log_action),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item {
                Button(
                    onClick = onShowRoster,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        "Kader anzeigen",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item {
                Button(
                    onClick = onAttemptFinishGame,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        stringResource(R.string.end_match_action),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // --- RESET ACTIONS ---

            if (!game.isPostShootout()) {
                item {
                    Button(
                        onClick = onAttemptResetPeriodTimer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            stringResource(R.string.reset_period_action),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
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
                        Text(stringResource(R.string.reset_game_action))
                        Icon(
                            imageVector = Icons.Filled.PriorityHigh,
                            contentDescription = stringResource(R.string.warning_icon)
                        )
                    }
                }
            }

            // --- CRITICAL ACTIONS ---

            item {
                Button(
                    onClick = onAttemptAbortGame,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(
                        "Spiel abbrechen",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewableAlertDialog(
    title: String,
    message: String? = null,
    confirmButtonText: String = "Yes",
    dismissButtonText: String = "No",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    RefWatchWearTheme { // Ensure the dialog is themed
        UnifiedConfirmationDialog(
            ConfirmationDialogInfo.FinishGame(
                title = title,
                text = message ?: "",
                onConfirm = onConfirm,
                onDialogClose = onDismiss
            )
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
            onShowRoster = {},
            onToggleTimer = {},
            onAttemptEndPhase = {},
            onAttemptAbortGame = {},
            modifier = Modifier.fillMaxSize()
        )
    }
}
