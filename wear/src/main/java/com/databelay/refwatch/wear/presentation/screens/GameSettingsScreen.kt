package com.databelay.refwatch.wear.presentation.screens

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
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.PauseCircleFilled
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
 * Returns true when the current game has just ended a penalty shootout.
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
    modifier: Modifier = Modifier,
    onUndoLastEvent: () -> Unit = {},
) {
    val listState = rememberScalingLazyListState()
    ScreenScaffold(
        scrollIndicator = {
            ScrollIndicator(
                modifier = Modifier.align(Alignment.CenterStart),
                state = listState
            )
        },
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(2.dp),
    ) { contentPadding ->
        ScalingLazyColumn(
            state = listState,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 0.dp),
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

            if (!game.isPostShootout() && game.currentPhase.hasTimer()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onToggleTimer,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (game.isTimerRunning) Color(0xFFFF6822) else Color.Green,
                                contentColor = Color.Black
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (game.isTimerRunning) Icons.Filled.PauseCircleFilled else Icons.Filled.PlayCircleFilled,
                                contentDescription = if (game.isTimerRunning) stringResource(R.string.timer_pause_content_desc) else stringResource(R.string.timer_play_content_desc),
                            )
                        }

                        Button(
                            onClick = onUndoLastEvent,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Gray.copy(alpha = 0.3f),
                                contentColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Undo,
                                contentDescription = "Undo"
                            )
                        }
                    }
                }
                item {
                    val willMatchEnd = remember(game.homeScore, game.awayScore, game.currentPhase, game.hasExtraTime, game.hasPenalties) {
                        val isTied = game.homeScore == game.awayScore
                        when (game.currentPhase) {
                            GamePhase.SECOND_HALF -> !isTied || (!game.hasExtraTime && !game.hasPenalties)
                            GamePhase.EXTRA_TIME_SECOND_HALF -> !isTied || !game.hasPenalties
                            GamePhase.PENALTIES -> true
                            else -> false
                        }
                    }

                    Button(
                        onClick = onAttemptEndPhase,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (willMatchEnd) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.errorContainer,
                            contentColor = if (willMatchEnd) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onErrorContainer
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = if (willMatchEnd) stringResource(R.string.end_match_action)
                                   else stringResource(R.string.end_phase_action, game.currentPhase.localizedName()),
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

            if (game.homeRoster.isNotEmpty() || game.awayRoster.isNotEmpty()) {
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

            if (game.currentPhase != GamePhase.GAME_ENDED && game.currentPhase != GamePhase.ABORTED) {
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
                            stringResource(R.string.abort_match),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
