package com.databelay.refwatch.wear.presentation.screens

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.databelay.refwatch.common.homeTeamColor
import com.databelay.refwatch.common.awayTeamColor
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.databelay.refwatch.R
import com.databelay.refwatch.common.Game
import com.databelay.refwatch.common.GamePhase
import com.databelay.refwatch.common.Team
import com.databelay.refwatch.common.formatTime
import com.databelay.refwatch.common.hasTimer
import com.databelay.refwatch.common.isPlayablePhase
import com.databelay.refwatch.common.theme.RefWatchWearTheme
import com.databelay.refwatch.wear.data.TimerState
import com.databelay.refwatch.wear.presentation.components.ColorIndicator
import com.databelay.refwatch.wear.presentation.utils.localizedName
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SportsFootball
import androidx.compose.material.icons.filled.Square
import com.databelay.refwatch.common.GoalType
import com.databelay.refwatch.common.CardType
import com.databelay.refwatch.common.TemporaryDismissalEvent
import androidx.wear.compose.material3.Dialog

@Composable
fun MainGameDisplayScreen(
    game: Game,
    timerState: TimerState,
    isAmbient: Boolean = false,
    kickoffCountdownSeconds: Int? = null,
    activeDismissals: List<TemporaryDismissalEvent> = emptyList(),
    pendingReturnConfirmations: List<TemporaryDismissalEvent> = emptyList(),
    onConfirmReturn: (TemporaryDismissalEvent) -> Unit = {},
    isPlayedTime: Boolean = false,
    onToggleTimerDisplayMode: () -> Unit = {},
    onKickOff: () -> Unit,
    onToggleTimer: () -> Unit,
    onToggleStoppageTimer: () -> Unit = {},
    onOpenGameMenu: () -> Unit = {},
    onNavigateToLogGoal: (Team, GoalType) -> Unit = { _, _ -> },
    onNavigateToLogCard: (Team, CardType) -> Unit = { _, _ -> },
    onQuickSubstitution: (Team, Int, Int) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    if (isAmbient) {
        AmbientMainDisplay(game, timerState, isPlayedTime)
        return
    }

    val pendingReturn = pendingReturnConfirmations.firstOrNull()
    if (pendingReturn != null) {
        Dialog(visible = true, onDismissRequest = { /* Force OK */ }) {
            Column(
                modifier = Modifier.fillMaxSize().padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SportsFootball,
                    contentDescription = null,
                    tint = ComposeColor.Green,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Spieler #${pendingReturn.playerNumber} darf wieder rein",
                    style = MaterialTheme.typography.titleSmall,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = if (pendingReturn.team == Team.HOME) game.homeTeamName else game.awayTeamName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { onConfirmReturn(pendingReturn) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("OK")
                }
            }
        }
    }

    // Quick-substitution dialog: opens when the referee double-taps the team color
    // on the main scoreboard. Referees can also reach it via the TeamActionsPage,
    // but the scoreboard is the default landing surface during a live match.
    //
    // The dialog itself handles both steps (outgoing -> incoming) inside a
    // single window, swapping the prompt and label in place.
    var quickSubTeam by remember { mutableStateOf<Team?>(null) }

    val view = LocalView.current
    val keepScreenOn = remember(game.currentPhase, timerState.isTimerRunning, timerState.isStoppageTimerRunning) {
        game.currentPhase.hasTimer() || timerState.isTimerRunning || timerState.isStoppageTimerRunning
    }
    DisposableEffect(view, keepScreenOn) {
        view.keepScreenOn = keepScreenOn
        onDispose { view.keepScreenOn = false }
    }

    val currentPhase = game.currentPhase
    val canToggleStoppageFromBack = remember(currentPhase) {
        currentPhase.isPlayablePhase() &&
            currentPhase != GamePhase.PENALTIES &&
            currentPhase != GamePhase.GAME_ENDED &&
            currentPhase != GamePhase.HALF_TIME &&
            currentPhase != GamePhase.EXTRA_TIME_HALF_TIME
    }
    val canKickOffFromBack = remember(timerState.actualTimeElapsedInPeriodMillis, timerState.isTimerRunning, currentPhase) {
        timerState.actualTimeElapsedInPeriodMillis == 0L && !timerState.isTimerRunning && currentPhase.isPlayablePhase()
    }

    var lastBackPressTime by remember { mutableStateOf(0L) }

    BackHandler {
        val now = System.currentTimeMillis()
        if (now - lastBackPressTime < 500L) {
            if (canKickOffFromBack) {
                onKickOff()
            }
            lastBackPressTime = 0L
        } else {
            if (canToggleStoppageFromBack) {
                onToggleStoppageTimer()
            }
            lastBackPressTime = now
        }
    }

    val regulationDuration = timerState.regulationPeriodDurationMillis
    val isPlayablePhaseAndInAddedTime = remember(
        currentPhase, timerState.actualTimeElapsedInPeriodMillis, regulationDuration
    ) {
        currentPhase.isPlayablePhase() &&
            timerState.actualTimeElapsedInPeriodMillis >= regulationDuration &&
            regulationDuration > 0
    }

    val parStyle = remember {
        ParagraphStyle(
            lineBreak = LineBreak.Simple,
            lineHeight = 24.sp,
            textAlign = TextAlign.Center
        )
    }

    val canToggleStoppageFromKey = remember(currentPhase) {
        currentPhase.isPlayablePhase() &&
            currentPhase != GamePhase.PENALTIES &&
            currentPhase != GamePhase.GAME_ENDED &&
            currentPhase != GamePhase.HALF_TIME &&
            currentPhase != GamePhase.EXTRA_TIME_HALF_TIME
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 24.dp) // TimeText space
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && canToggleStoppageFromKey) {
                    when (event.key) {
                        Key.DirectionCenter, // Wear OS side button (default on Pixel Watch / Galaxy Watch 4+)
                        Key.Enter,
                        Key.ButtonSelect,
                        Key.Power -> {
                            onToggleStoppageTimer()
                            true
                        }
                        else -> false
                    }
                } else {
                    false
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Score & Teams
        // Hoist team info into stable values so TeamInfo does not recompose
        // every second when the timer ticks inside the Game data class.
        val homeTeamName = remember(game.homeTeamName, game.homeTeamAbbr) {
            (game.homeTeamAbbr?.takeIf { it.isNotBlank() }
                ?: game.homeTeamName.uppercase().filter { it.isLetterOrDigit() }.take(3))
                .ifBlank { "HOM" }
        }
        val awayTeamName = remember(game.awayTeamName, game.awayTeamAbbr) {
            (game.awayTeamAbbr?.takeIf { it.isNotBlank() }
                ?: game.awayTeamName.uppercase().filter { it.isLetterOrDigit() }.take(3))
                .ifBlank { "AWA" }
        }
        val homeColor = remember(game.homeTeamColorArgb) { game.homeTeamColor }
        val awayColor = remember(game.awayTeamColorArgb) { game.awayTeamColor }
        val homeCaptain = remember(game.homeCaptainNumber) { game.homeCaptainNumber }
        val awayCaptain = remember(game.awayCaptainNumber) { game.awayCaptainNumber }
        val scoreText = remember(game.homeScore, game.awayScore) { "${game.homeScore} : ${game.awayScore}" }
        val homeHasKickOff = remember(game.kickOffTeam) { game.kickOffTeam == Team.HOME }
        val awayHasKickOff = remember(game.kickOffTeam) { game.kickOffTeam == Team.AWAY }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TeamInfo(
                teamName = homeTeamName,
                teamColor = homeColor,
                captainNumber = homeCaptain,
                hasKickOff = homeHasKickOff,
                onLongClick = { onNavigateToLogGoal(Team.HOME, GoalType.REGULAR) },
                onDoubleTap = { quickSubTeam = Team.HOME },
                modifier = Modifier.weight(1f)
            )
            Text(
                text = scoreText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            TeamInfo(
                teamName = awayTeamName,
                teamColor = awayColor,
                captainNumber = awayCaptain,
                hasKickOff = awayHasKickOff,
                onLongClick = { onNavigateToLogGoal(Team.AWAY, GoalType.REGULAR) },
                onDoubleTap = { quickSubTeam = Team.AWAY },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Timer Section
        // We hoist the touch handler into a stable lambda-capturing Modifier and
        // pass already-derived string values into a child Composable, so when
        // `Game` mutates every second only the inner TimerText recomposes — the
        // surrounding Row (score, team colors) stays skipped.
        val mainTimerText = remember(
            isPlayedTime,
            isPlayablePhaseAndInAddedTime,
            regulationDuration,
            currentPhase,
            timerState.actualTimeElapsedInPeriodMillis,
            timerState.isTimerRunning
        ) {
            if (isPlayablePhaseAndInAddedTime && regulationDuration > 0) {
                val addedMillis = timerState.actualTimeElapsedInPeriodMillis - regulationDuration
                val baseMillis = when (currentPhase) {
                    GamePhase.SECOND_HALF -> regulationDuration * 2
                    else -> regulationDuration
                }
                (baseMillis + addedMillis).formatTime()
            } else if (timerState.actualTimeElapsedInPeriodMillis == 0L && !timerState.isTimerRunning) {
                ""
            } else if (isPlayedTime) {
                timerState.actualTimeElapsedInPeriodMillis.formatTime()
            } else {
                val remainingMillis = (regulationDuration - timerState.actualTimeElapsedInPeriodMillis).coerceAtLeast(0)
                // Ceiling rounding for remaining time: if 54.1s remain, we show "00:55".
                // This ensures that at any point (Played + Remaining) equals Regulation,
                // and avoids the "1 second jump" discrepancy.
                val roundedRemaining = if (remainingMillis > 0 && timerState.isTimerRunning) {
                    ((remainingMillis + 999) / 1000) * 1000
                } else {
                    remainingMillis
                }
                roundedRemaining.formatTime()
            }
        }
        val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant
        val mainTimerColor = remember(
            isPlayablePhaseAndInAddedTime,
            timerState.isTimerRunning,
            inactiveColor
        ) {
            when {
                isPlayablePhaseAndInAddedTime -> ComposeColor.Red
                timerState.isTimerRunning -> ComposeColor.White
                else -> inactiveColor
            }
        }
        val stoppageText = remember(timerState.stoppageTimeMillis, timerState.isStoppageTimerRunning, isPlayablePhaseAndInAddedTime) {
            // Hide the green stoppage timer once we are in added time (main timer red),
            // to keep the display clean as requested.
            if (!isPlayablePhaseAndInAddedTime && (timerState.isStoppageTimerRunning || timerState.stoppageTimeMillis > 0)) {
                timerState.stoppageTimeMillis.formatTime(isInAddedTime = true)
            } else ""
        }
        val showKickoffButton = remember(timerState.actualTimeElapsedInPeriodMillis, timerState.isTimerRunning, currentPhase) {
            timerState.actualTimeElapsedInPeriodMillis == 0L && !timerState.isTimerRunning && currentPhase.isPlayablePhase()
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onToggleTimerDisplayMode() },
                        onLongPress = { onOpenGameMenu() }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            TimerContent(
                kickoffCountdownSeconds = kickoffCountdownSeconds,
                mainTimerText = mainTimerText,
                mainTimerColor = mainTimerColor,
                stoppageText = stoppageText,
                activeDismissals = activeDismissals,
                currentMatchTimeMillis = timerState.actualTimeElapsedInPeriodMillis,
                showKickoffButton = showKickoffButton,
                onKickOff = onKickOff
            )
        }

    }

    quickSubTeam?.let { team ->
        val roster = if (team == Team.HOME) game.homeRoster else game.awayRoster
        // Single window that swaps outgoing -> incoming in place.
        QuickSubstitutionDialog(
            team = team,
            roster = roster,
            onConfirm = { outNum, inNum ->
                onQuickSubstitution(team, outNum, inNum)
                quickSubTeam = null
            },
            onDismiss = {
                quickSubTeam = null
            }
        )
    }
}

/**
 * Pure-display timer content. Receives already-computed strings + colors so
 * Compose can skip this Composable entirely when only the surrounding `Game`
 * object mutates (e.g. every second when the timer ticks). Without this split,
 * the entire MainGameDisplayScreen recomposed every second because `Game` is
 * a 40-field data class and the timer read every timer field.
 */
@Composable
private fun TimerContent(
    kickoffCountdownSeconds: Int?,
    mainTimerText: String,
    mainTimerColor: ComposeColor,
    stoppageText: String,
    activeDismissals: List<TemporaryDismissalEvent> = emptyList(),
    currentMatchTimeMillis: Long = 0L,
    showKickoffButton: Boolean,
    onKickOff: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (kickoffCountdownSeconds != null) {
            Text(
                text = kickoffCountdownSeconds.toString(),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(stringResource(R.string.kick_off), style = MaterialTheme.typography.labelSmall)
        } else if (showKickoffButton) {
            Button(
                onClick = onKickOff,
                modifier = Modifier.size(70.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                )
            ) {
                Text(
                    stringResource(R.string.kick_off_button),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        } else if (mainTimerText.isNotEmpty()) {
            Text(
                text = mainTimerText,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = mainTimerColor
            )
        }
        
        if (stoppageText.isNotEmpty()) {
            Text(
                text = stoppageText,
                style = MaterialTheme.typography.titleLarge,
                color = ComposeColor(0xFF00E676)
            )
        }

        // PERFORMANCE: Only iterate over dismissals if there are any active.
        if (activeDismissals.isNotEmpty()) {
            activeDismissals.forEach { dismissal ->
                val elapsedSinceStart = currentMatchTimeMillis - dismissal.startMatchTimeMillis
                val remainingMillis = (dismissal.durationMinutes * 60 * 1000L - elapsedSinceStart).coerceAtLeast(0.0).toLong()
                if (remainingMillis > 0) {
                    Box(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .background(ComposeColor.Yellow, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${dismissal.team.name.take(1)} #${dismissal.playerNumber}: ${remainingMillis.formatTime()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ComposeColor.Black,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TeamInfo(
    teamName: String,
    teamColor: ComposeColor,
    captainNumber: Int?,
    hasKickOff: Boolean,
    onLongClick: () -> Unit,
    onDoubleTap: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { onDoubleTap() },
                    onLongPress = { onLongClick() }
                )
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ColorIndicator(color = teamColor, indicatorSize = 12.dp, hasKickOffBorder = hasKickOff)
        Text(teamName, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold)
        Text(
            text = stringResource(R.string.captain_indicator, captainNumber?.toString() ?: "--"),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium
        )
    }
}



@Composable
private fun AmbientMainDisplay(game: Game, timerState: TimerState, isPlayedTime: Boolean) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${game.homeScore} : ${game.awayScore}",
                style = MaterialTheme.typography.titleLarge,
                color = ComposeColor.White
            )
            Spacer(modifier = Modifier.size(8.dp))
            val regulationDuration = timerState.regulationPeriodDurationMillis
            val timerText = if (isPlayedTime) {
                timerState.actualTimeElapsedInPeriodMillis.formatTime()
            } else {
                (regulationDuration - timerState.actualTimeElapsedInPeriodMillis).coerceAtLeast(0).formatTime()
            }
            Text(
                text = timerText,
                style = MaterialTheme.typography.displayMedium,
                color = ComposeColor.White
            )
            if (timerState.stoppageTimeMillis > 0) {
                Text(
                    text = stringResource(R.string.stoppage_indicator, timerState.stoppageTimeMillis.formatTime()),
                    style = MaterialTheme.typography.titleSmall,
                    color = ComposeColor.Gray
                )
            }
        }
    }
}
