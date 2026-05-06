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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ScreenScaffold
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.ui.tooling.preview.WearPreviewFontScales
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
import com.databelay.refwatch.common.isBreak
import com.databelay.refwatch.common.isPlayablePhase
import com.databelay.refwatch.common.readable
import com.databelay.refwatch.common.theme.RefWatchWearTheme
import com.databelay.refwatch.wear.presentation.components.ColorIndicator
import com.databelay.refwatch.wear.presentation.utils.localizedName
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput

@Composable
fun MainGameDisplayScreen(
    game: Game,
    kickoffCountdownSeconds: Int? = null,
    isPlayedTime: Boolean = false,
    onToggleTimerDisplayMode: () -> Unit = {},
    onKickOff: () -> Unit,
    onToggleTimer: () -> Unit,
    onToggleStoppageTimer: () -> Unit = {},
    onOpenGameMenu: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var clickCount by remember { mutableStateOf(0) }
    val DOUBLE_CLICK_TIME = 300L

    LaunchedEffect(clickCount) {
        if (clickCount == 1) {
            delay(DOUBLE_CLICK_TIME)
            if (clickCount == 1) {
                // Single click logic: Start/Resume (not kick-off)
                if (!game.isTimerRunning &&
                    game.currentPhase.hasTimer() &&
                    game.actualTimeElapsedInPeriodMillis > 0L
                ) {
                    onToggleTimer()
                }
                clickCount = 0
            }
        } else if (clickCount >= 2) {
            // Double click logic on hardware action key
            if (game.actualTimeElapsedInPeriodMillis == 0L && !game.isTimerRunning && game.currentPhase.isPlayablePhase()) {
                onKickOff()
            } else if (game.currentPhase.isPlayablePhase() && game.currentPhase.hasTimer()) {
                onToggleStoppageTimer()
            }
            clickCount = 0
        }
    }

    BackHandler {
        clickCount++
    }

    val regulationDuration = game.regulationPeriodDurationMillis()
    // Determine if we are in "added time" for a playable phase
    val isPlayablePhaseAndInAddedTime = game.currentPhase.isPlayablePhase() &&
            game.actualTimeElapsedInPeriodMillis >= regulationDuration &&
            regulationDuration > 0 // Ensure regulation duration is positive to avoid division by zero or weird states

    val addedTime = game.actualTimeElapsedInPeriodMillis - regulationDuration
    val parStyle = ParagraphStyle(
        lineBreak = LineBreak.Simple,
        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight,
        textAlign = TextAlign.Center
    )
    val stoppageColor = ComposeColor(0xFF00E676)

    val onMainTimerTap: () -> Unit = {
        if (game.isStoppageTimerRunning) {
            onToggleStoppageTimer()
            if (!game.isTimerRunning && game.currentPhase.hasTimer()) {
                onToggleTimer()
            }
        }
    }

    val onStoppageTap: () -> Unit = {
        if (game.currentPhase.isPlayablePhase() && game.currentPhase.hasTimer()) {
            if (game.isTimerRunning) onToggleTimer()
            if (!game.isStoppageTimerRunning) onToggleStoppageTimer()
        }
    }

    val mainTimerStyle = MaterialTheme.typography.displayLarge.toSpanStyle().copy(
        fontWeight = FontWeight.Bold,
        color = if (game.isTimerRunning) ComposeColor.White else MaterialTheme.colorScheme.onSurface
    )
    val halfTimerStyle = MaterialTheme.typography.displaySmall.toSpanStyle().copy()
    val extraTimerStyle = MaterialTheme.typography.bodyLarge.toSpanStyle().copy(
        color = stoppageColor,
    )
    val displayTimerText = buildAnnotatedString {
        withStyle(parStyle) {
            if (game.currentPhase.isPlayablePhase()) {
                if (isPlayablePhaseAndInAddedTime) {
                    withStyle(mainTimerStyle) {
                        append(regulationDuration.formatTime())
                    }
                } else {
                    // In regulation
                    if (isPlayedTime) {
                        withStyle(mainTimerStyle) {
                            append(game.actualTimeElapsedInPeriodMillis.formatTime())
                        }
                    } else {
                        val timeRemaining = regulationDuration - game.actualTimeElapsedInPeriodMillis
                        withStyle(mainTimerStyle) {
                            append(timeRemaining.formatTime())
                        }
                    }
                }
            } else if (game.currentPhase.isBreak()) {
                // For breaks, display time remaining in the break
                val breakDuration = game.regulationPeriodDurationMillis() // Use your VM function
                val timeRemainingInBreak = breakDuration - game.actualTimeElapsedInPeriodMillis
                if (timeRemainingInBreak > 0)
                    withStyle(halfTimerStyle) {
                        append(timeRemainingInBreak.formatTime())
                    }
                else {
                    withStyle(extraTimerStyle) {
                        append(game.regulationPeriodDurationMillis().formatTime())
                        append("\nis over")
                    }
                }
            } else {
                // Pre-game, ended, etc.
                append("") // Or "--:--"
            }
        }
    }
    ScreenScaffold() {
        Column(
            modifier = modifier // Use the passed modifier
                .fillMaxSize(),
            // Add your own general horizontal padding for the content
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceAround
        ) {
            Spacer(modifier = Modifier.height(1.dp))
            // Time display if TimeText isn't sufficient or for specific styling
            Text(
                text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(java.util.Date()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
            )
            // Score and Team Colors
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                // ... content of score row (ColorIndicator, Score Text) ...
                val homeHasKickOff =
                    game.kickOffTeam == Team.HOME && game.currentPhase.isPlayablePhase()
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    ColorIndicator(
                        color = game.homeTeamColor,
                        hasKickOffBorder = homeHasKickOff,
                        indicatorSize = 18.dp
                    )
                    Text(
                        game.homeTeamAbbr ?: game.homeTeamName.take(3).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Text(
                    "${game.homeScore} - ${game.awayScore}",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                val awayHasKickOff =
                    game.kickOffTeam == Team.AWAY && game.currentPhase.isPlayablePhase()
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    ColorIndicator(
                        color = game.awayTeamColor,
                        hasKickOffBorder = awayHasKickOff,
                        indicatorSize = 18.dp
                    )
                    Text(
                        game.awayTeamAbbr ?: game.awayTeamName.take(3).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            // Current Phase
            Text(
                game.currentPhase.localizedName(),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.padding(1.dp))

            // Conditionally show Timer or Kickoff Button
            // Using a Box to center its content if the content itself doesn't fill width

            Box(
                modifier = Modifier
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (kickoffCountdownSeconds != null && game.currentPhase.isPlayablePhase()) {
                    Text(
                        text = kickoffCountdownSeconds.toString(),
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center
                    )
                } else if (game.actualTimeElapsedInPeriodMillis == 0L && !game.isTimerRunning && game.currentPhase.isPlayablePhase()) {
                    Button(
                        onClick = onKickOff,
                        modifier = Modifier
                            .wrapContentWidth()
                            .padding(horizontal = 32.dp),
                    ) {
                        Text(
                            stringResource(R.string.kick_off),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )
                    }

                } else {
                    if (game.currentPhase.hasTimer()) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = displayTimerText,
                                style = TextStyle(
                                    fontFamily = MaterialTheme.typography.displayLarge.fontFamily,
                                    textAlign = TextAlign.Center
                                ),
                                modifier = Modifier.pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = { onMainTimerTap() },
                                        onDoubleTap = { onToggleTimerDisplayMode() },
                                        onLongPress = { onOpenGameMenu() }
                                    )
                                }
                            )
                            if (game.currentPhase.hasTimer()) {
                                Text(
                                    text = if (game.stoppageTimeMillis > 0 || game.isStoppageTimerRunning)
                                        "+ ${game.stoppageTimeMillis.formatTime(false)}"
                                    else if (game.isTimerRunning) "+ 00:00" else "",
                                    color = if (game.isStoppageTimerRunning) stoppageColor else MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.displayMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier
                                        .padding(top = 8.dp)
                                        .pointerInput(Unit) {
                                            detectTapGestures(
                                                onTap = { onStoppageTap() },
                                                onLongPress = { onOpenGameMenu() }
                                            )
                                        }
                                )
                            }
                        }
                    } else {
                        // Consistent placeholder height if no timer
                        Spacer(modifier = Modifier.height(50.dp)) // Adjust to match approx. timer height
                    }
                }
            }

            // Captain Numbers Row below Timer
            if (game.homeCaptainNumber != null || game.awayCaptainNumber != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (game.homeCaptainNumber != null) "C: ${game.homeCaptainNumber}" else "",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (game.awayCaptainNumber != null) "C: ${game.awayCaptainNumber}" else "",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.padding(8.dp))
        }
    }
}

// --------------------------------------- Previews ----------------------------------------
// -----------------------------------------------------------------------------------------

// Helper function to create a base game for previews
private fun createPreviewGame(
    currentPhase: GamePhase,
    actualTimeElapsedInPeriodMillis: Long,
    kickOffTeam: Team = Team.AWAY,
    homeScore: Int = 0,
    awayScore: Int = 0,
    halfDurationMinutes: Int = 45,
    halftimeDurationMinutes: Int = 15,
    homeTeamColorArgb: Int = android.graphics.Color.BLACK, // ARGB Int
    awayTeamColorArgb: Int = android.graphics.Color.YELLOW, // ARGB Int
    isTimerRunningOverride: Boolean? = null
): Game {
    val baseGame = Game.defaults().copy(
        id = "previewGame-${currentPhase.name}-${System.nanoTime()}",
        currentPhase = currentPhase,
        homeTeamName = "Red Team",
        awayTeamName = "Blue Team",
        homeTeamColorArgb = homeTeamColorArgb,
        awayTeamColorArgb = awayTeamColorArgb,
        kickOffTeam = kickOffTeam,
        actualTimeElapsedInPeriodMillis = actualTimeElapsedInPeriodMillis,
        halfDurationMinutes = halfDurationMinutes,
        halftimeDurationMinutes = halftimeDurationMinutes,
        homeScore = homeScore,
        awayScore = awayScore
        // isTimerRunning will be set below
    )

    val finalIsTimerRunning = isTimerRunningOverride ?: (actualTimeElapsedInPeriodMillis > 0 &&
            baseGame.currentPhase.hasTimer() &&
            (baseGame.currentPhase.isPlayablePhase() && actualTimeElapsedInPeriodMillis < baseGame.regulationPeriodDurationMillis()) || // Timer runs if playable and not past full reg time
            (baseGame.currentPhase.isBreak() && actualTimeElapsedInPeriodMillis < baseGame.regulationPeriodDurationMillis())) // Timer runs for break if not past break duration

    return baseGame.copy(isTimerRunning = finalIsTimerRunning)
}

@Preview(device = "id:wearos_small_round",name = "AddedTime SmRnd",showBackground = true)
@Preview(device = "id:wearos_large_round",name = "AddedTime LrgRnd",showBackground = true)
@Preview(device = "id:wearos_square",name = "AddedTime Sqr",showBackground = true)
@WearPreviewFontScales
@Composable
fun Preview_MainGameDisplay_RegulationTime() {
    RefWatchWearTheme {
        MainGameDisplayScreen(
            game = createPreviewGame(
                currentPhase = GamePhase.FIRST_HALF,
                actualTimeElapsedInPeriodMillis = 5 * 60000L, // 5 minutes into the game
                homeScore = 2,
                isTimerRunningOverride = true
            ),
            onKickOff = {},
            onToggleTimer = {}
        )
    }
}

@Preview(device = "id:wearos_small_round",name = "AddedTime SmRnd",showBackground = true)
@Preview(device = "id:wearos_large_round",name = "AddedTime LrgRnd",showBackground = true)
@Preview(device = "id:wearos_square",name = "AddedTime Sqr",showBackground = true)
@WearPreviewFontScales
@Composable
fun Preview_MainGameDisplay_AddedTime() {
    RefWatchWearTheme {
        MainGameDisplayScreen(
            game = createPreviewGame(
                currentPhase = GamePhase.FIRST_HALF,
                actualTimeElapsedInPeriodMillis = (45 * 60000L) + (2 * 60000L) + 15000L, // 45min + 2min 15sec
                halfDurationMinutes = 45,
                homeScore = 2,
                isTimerRunningOverride = true
            ),
            onKickOff = {},
            onToggleTimer = {}
        )
    }
}
@Preview(device = "id:wearos_small_round",name = "AddedTime SmRnd",showBackground = true)
@Preview(device = "id:wearos_large_round",name = "AddedTime LrgRnd",showBackground = true)
@Preview(device = "id:wearos_square",name = "AddedTime Sqr",showBackground = true)
@WearPreviewFontScales
@Composable
fun Preview_MainGameDisplay_Halftime() {
    RefWatchWearTheme {
        MainGameDisplayScreen(
            game = createPreviewGame(
                currentPhase = GamePhase.HALF_TIME,
                actualTimeElapsedInPeriodMillis = 5 * 60000L + 25000, // 5 minutes into halftime
                halftimeDurationMinutes = 15,
                kickOffTeam = Team.HOME,
                awayScore = 3,
                awayTeamColorArgb = android.graphics.Color.RED,
                isTimerRunningOverride = true // Assuming halftime timer runs
            ),
            onKickOff = {},
            onToggleTimer = {}
        )
    }
}

@Preview(device = "id:wearos_small_round",name = "AddedTime SmRnd",showBackground = true)
@Preview(device = "id:wearos_large_round",name = "AddedTime LrgRnd",showBackground = true)
@Preview(device = "id:wearos_square",name = "AddedTime Sqr",showBackground = true)
@WearPreviewFontScales
@Composable
fun Preview_MainGameDisplay_Kickoff() {
    RefWatchWearTheme {
        MainGameDisplayScreen(
            game = createPreviewGame(
                currentPhase = GamePhase.FIRST_HALF,
                actualTimeElapsedInPeriodMillis = 0L,
                homeScore = 0,
                awayScore = 0,
                isTimerRunningOverride = false // Important: timer NOT running for kickoff button
            ),
            onKickOff = {},
            onToggleTimer = {}
        )
    }
}
