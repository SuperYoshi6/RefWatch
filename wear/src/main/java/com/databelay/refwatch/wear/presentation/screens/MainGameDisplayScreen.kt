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
import com.databelay.refwatch.common.regulationPeriodDurationMillis
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
import kotlinx.coroutines.delay
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import com.databelay.refwatch.R
import com.databelay.refwatch.common.Game
import com.databelay.refwatch.common.GamePhase
import com.databelay.refwatch.common.Team
import com.databelay.refwatch.common.formatTime
import com.databelay.refwatch.common.hasTimer
import com.databelay.refwatch.common.isPlayablePhase
import com.databelay.refwatch.common.theme.RefWatchWearTheme
import com.databelay.refwatch.wear.presentation.components.ColorIndicator
import com.databelay.refwatch.wear.presentation.utils.localizedName
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SportsFootball
import androidx.compose.material.icons.filled.Square
import com.databelay.refwatch.common.GoalType
import com.databelay.refwatch.common.CardType

@Composable
fun MainGameDisplayScreen(
    game: Game,
    isAmbient: Boolean = false,
    kickoffCountdownSeconds: Int? = null,
    isPlayedTime: Boolean = false,
    onToggleTimerDisplayMode: () -> Unit = {},
    onKickOff: () -> Unit,
    onToggleTimer: () -> Unit,
    onToggleStoppageTimer: () -> Unit = {},
    onOpenGameMenu: () -> Unit = {},
    onNavigateToLogGoal: (Team, GoalType) -> Unit = { _, _ -> },
    onNavigateToLogCard: (Team, CardType) -> Unit = { _, _ -> },
    onQuickGoal: (Team) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (isAmbient) {
        AmbientMainDisplay(game, isPlayedTime)
        return
    }

    val view = LocalView.current
    val keepScreenOn = game.currentPhase.hasTimer() || game.isTimerRunning || game.isStoppageTimerRunning
    DisposableEffect(view, keepScreenOn) {
        view.keepScreenOn = keepScreenOn
        onDispose { view.keepScreenOn = false }
    }

    var clickCount by remember { mutableStateOf(0) }
    val DOUBLE_CLICK_TIME = 500L

    LaunchedEffect(clickCount) {
        if (clickCount == 1) {
            delay(DOUBLE_CLICK_TIME)
            if (clickCount == 1) {
                val phase = game.currentPhase
                val canToggleStoppage = phase.isPlayablePhase() && 
                    game.isTimerRunning && // Only allow if main timer is actually running
                    phase != GamePhase.PENALTIES &&
                    phase != GamePhase.GAME_ENDED && 
                    phase != GamePhase.HALF_TIME && 
                    phase != GamePhase.EXTRA_TIME_HALF_TIME
                
                if (canToggleStoppage) {
                    onToggleStoppageTimer()
                }
                clickCount = 0
            }
        } else if (clickCount >= 2) {
            // Long press or double back is handled by individual elements now if needed,
            // but we'll keep the double-back logic for kickoff as a fallback or if requested.
            // However, the user specifically asked for a BUTTON for kickoff.
            if (game.actualTimeElapsedInPeriodMillis == 0L && !game.isTimerRunning && game.currentPhase.isPlayablePhase()) {
                onKickOff()
            }
            clickCount = 0
        }
    }

    BackHandler { clickCount++ }

    val regulationDuration = remember(game.currentPhase) { game.regulationPeriodDurationMillis() }
    val isPlayablePhaseAndInAddedTime = game.currentPhase.isPlayablePhase() &&
            game.actualTimeElapsedInPeriodMillis >= regulationDuration &&
            regulationDuration > 0

    val addedTime = game.actualTimeElapsedInPeriodMillis - regulationDuration
    val parStyle = remember {
        ParagraphStyle(
            lineBreak = LineBreak.Simple,
            lineHeight = 24.sp,
            textAlign = TextAlign.Center
        )
    }
    val stoppageColor = ComposeColor(0xFF00E676)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 24.dp), // TimeText space
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Score & Teams
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TeamInfo(
                game = game,
                team = Team.HOME,
                hasKickOff = game.kickOffTeam == Team.HOME,
                onClick = { onNavigateToLogGoal(Team.HOME, GoalType.REGULAR) },
                onLongClick = { onQuickGoal(Team.HOME) },
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${game.homeScore} : ${game.awayScore}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            TeamInfo(
                game = game,
                team = Team.AWAY,
                hasKickOff = game.kickOffTeam == Team.AWAY,
                onClick = { onNavigateToLogGoal(Team.AWAY, GoalType.REGULAR) },
                onLongClick = { onQuickGoal(Team.AWAY) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Timer Section
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
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (kickoffCountdownSeconds != null) {
                    Text(
                        text = kickoffCountdownSeconds.toString(),
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(stringResource(R.string.kick_off), style = MaterialTheme.typography.labelSmall)
                } else {
                    val mainTimerText = if (isPlayedTime) {
                        game.actualTimeElapsedInPeriodMillis.formatTime()
                    } else {
                        if (isPlayablePhaseAndInAddedTime) {
                            // Show total time (regulation + added) instead of pinning at 45:00
                            game.actualTimeElapsedInPeriodMillis.formatTime()
                        }
                        else (regulationDuration - game.actualTimeElapsedInPeriodMillis).coerceAtLeast(0).formatTime()
                    }

                    if (game.actualTimeElapsedInPeriodMillis == 0L && !game.isTimerRunning && game.currentPhase.isPlayablePhase()) {
                         Button(
                             onClick = onKickOff,
                             modifier = Modifier.size(70.dp),
                             shape = androidx.compose.foundation.shape.CircleShape,
                             colors = ButtonDefaults.buttonColors(
                                 containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                             )
                         ) {
                             Text(
                                 "Anstoß",
                                 style = MaterialTheme.typography.labelLarge,
                                 fontWeight = FontWeight.Bold,
                                 textAlign = TextAlign.Center
                             )
                         }
                    } else {
                        Text(
                            text = mainTimerText,
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isPlayablePhaseAndInAddedTime -> ComposeColor.Red
                                game.isTimerRunning -> ComposeColor.White
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }

                    if (game.stoppageTimeMillis > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Removed the +addedTime display to keep only two timers max
                            Text(
                                text = game.stoppageTimeMillis.formatTime(isInAddedTime = true),
                                style = MaterialTheme.typography.titleLarge,
                                color = stoppageColor
                            )
                        }
                    }
                }
            }
        }


    }
}

@Composable
private fun TeamInfo(
    game: Game, 
    team: Team, 
    hasKickOff: Boolean, 
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val name = if (team == Team.HOME) game.homeTeamAbbr else game.awayTeamAbbr
    val color = if (team == Team.HOME) game.homeTeamColor else game.awayTeamColor
    val captain = if (team == Team.HOME) game.homeCaptainNumber else game.awayCaptainNumber

    Column(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongClick() }
                )
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ColorIndicator(color = color, indicatorSize = 12.dp, hasKickOffBorder = hasKickOff)
        Text(name ?: "", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold)
        Text(
            text = "© ${captain ?: "--"}",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium
        )
    }
}



@Composable
private fun AmbientMainDisplay(game: Game, isPlayedTime: Boolean) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${game.homeScore} : ${game.awayScore}",
                style = MaterialTheme.typography.titleLarge,
                color = ComposeColor.White
            )
            Spacer(modifier = Modifier.size(8.dp))
            val regulationDuration = game.regulationPeriodDurationMillis()
            val timerText = if (isPlayedTime) {
                game.actualTimeElapsedInPeriodMillis.formatTime()
            } else {
                (regulationDuration - game.actualTimeElapsedInPeriodMillis).coerceAtLeast(0).formatTime()
            }
            Text(
                text = timerText,
                style = MaterialTheme.typography.displayMedium,
                color = ComposeColor.White
            )
            if (game.stoppageTimeMillis > 0) {
                Text(
                    text = "ST: ${game.stoppageTimeMillis.formatTime()}",
                    style = MaterialTheme.typography.titleSmall,
                    color = ComposeColor.Gray
                )
            }
        }
    }
}
