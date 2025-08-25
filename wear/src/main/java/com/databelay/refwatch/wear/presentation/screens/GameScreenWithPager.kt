package com.databelay.refwatch.wear.presentation.screens

import android.util.Log
import android.widget.Toast
import androidx.activity.result.launch
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import com.databelay.refwatch.common.CardType
import com.databelay.refwatch.common.Game
import com.databelay.refwatch.common.GamePhase
import com.databelay.refwatch.common.Team
import com.databelay.refwatch.common.hasTimer
import com.databelay.refwatch.common.isPlayablePhase
import com.databelay.refwatch.common.readable
import com.databelay.refwatch.presentation.screens.pager.PenaltyShootoutScreen
import com.databelay.refwatch.wear.WearGameViewModel
import com.databelay.refwatch.wear.navigation.TAG
import com.databelay.refwatch.wear.presentation.components.ConfirmationDialog
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GameScreenWithPager(
    gameViewModel: WearGameViewModel,
// Lambdas for actions the Pager or its settings dialog might trigger
    onToggleTimer: () -> Unit,
    onAddGoal: (Team) -> Unit,
    onNavigateToLogCard: (team: Team, cardType: CardType) -> Unit,
    onNavigateToGameLog: () -> Unit,
    onEndPhase: () -> Unit,
    onResetPeriodTimer: () -> Unit,
    onConfirmEndMatch: () -> Unit, // To distinguish from onFinishGame
    onPenaltyAttemptRecorded: (scored: Boolean) -> Unit, // New callback for penalty attempts
) {

    val context = LocalContext.current
    val activeGame by gameViewModel.activeGame.collectAsState() // activeGame is Game?
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var showEndOfMainTimeDialog by remember { mutableStateOf(false) }

    // Use safe calls for properties of activeGame
    val isPenaltiesPhase = activeGame?.currentPhase == GamePhase.PENALTIES
    val isPlayableRegularPhase = activeGame?.currentPhase?.isPlayablePhase() == true && !isPenaltiesPhase

    // Pager state is only relevant if it's a playable regular phase
    val pagerState = rememberPagerState(
        initialPage = 1, // Default to main display (index 1 of 3) when pager is active
        pageCount = { if (isPlayableRegularPhase) 3 else 1 } // Pager has 3 pages only if playable
    )
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) { // Long press anywhere to open settings
                detectTapGestures(
                    onLongPress = {
                        showSettingsDialog = true
                    }
                )
            }
    ) {
        activeGame?.let { game -> // game is non-null Game here
            when {
                isPenaltiesPhase -> { // isPenaltiesPhase was calculated using activeGame?.currentPhase
                    // Directly show PenaltyShootoutScreen
                    PenaltyShootoutScreen(
                        game = game,
                        onPenaltyAttemptRecorded = onPenaltyAttemptRecorded,
                        modifier = Modifier.fillMaxSize() // It takes the whole screen
                    )
                }
                isPlayableRegularPhase -> { // isPlayableRegularPhase also used activeGame?
                    // Show HorizontalPager for playable regular phases
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        // beyondBoundsPageCount = 1 // Consider if needed for performance/preloading
                    ) { page ->
                        when (page) {
                            0 -> TeamActionsPage(
                                team = Team.HOME,
                                game = game,
                                onAddGoal = {
                                    onAddGoal(Team.HOME)
                                    coroutineScope.launch {pagerState.animateScrollToPage(1)}
                                },
                                onNavigateToLogCard = onNavigateToLogCard
                            )

                            1 -> MainGameDisplayScreen( // The main timer/score view
                                game = game,
                                onKickOff = { gameViewModel.kickOff() }
                            )

                            2 -> TeamActionsPage(
                                team = Team.AWAY,
                                game = game,
                                onAddGoal = {
                                    onAddGoal(Team.AWAY)
                                    coroutineScope.launch {pagerState.animateScrollToPage(1)}
                                },
                                onNavigateToLogCard = onNavigateToLogCard
                            )
                        }
                    }
                }
                else -> {
                    // For non-playable, non-penalty phases (e.g., HALF_TIME, GAME_ENDED, NOT_STARTED)
                    // Show only the MainGameDisplayScreen (which typically shows timer/status)
                    MainGameDisplayScreen( // The main timer/score view
                        game = game,
                        onKickOff = { gameViewModel.kickOff() }
                    )
                }
            }

            if (showSettingsDialog) {
                GameSettingsDialog(
                    game = game, // Pass non-null game
                    onDismiss = { showSettingsDialog = false },
                    onViewLog = {
                        showSettingsDialog = false
                        onNavigateToGameLog() // Use the passed lambda
                    },
                    onFinishGame = {
                        showSettingsDialog = false
                        onConfirmEndMatch()
                    },
                    onResetPeriodTimer = {
                        showSettingsDialog = false
                        showResetConfirmDialog = true
                    },
                    onResetGame = {
                        showSettingsDialog = false
                        gameViewModel.resetGame()
                    },
                    onToggleTimer = {
                        showSettingsDialog = false
                        onToggleTimer()
                    },
                    onEndPhase = {
                        showSettingsDialog = false
                        // Use game (non-null) here instead of activeGame (nullable)
                        if (game.currentPhase == GamePhase.SECOND_HALF && game.isTied)
                            showEndOfMainTimeDialog = true
                        else
                            onEndPhase()
                    },
                )
            }
        } ?: run {
            Log.d(TAG, "GameScreenWithPager: activeGame is null, game content UI not rendered.")
            // Optionally, show a loading indicator or placeholder here
            // Text("Loading game details...")
        }


        if (showResetConfirmDialog) {
            // Use activeGame with safe calls for properties
            if (activeGame?.currentPhase?.hasTimer() == true) {
                ConfirmationDialog(
                    message = "Reset timer for ${activeGame?.currentPhase?.readable() ?: "current phase"}?",
                    onConfirm = {
                        showResetConfirmDialog = false
                        onResetPeriodTimer() // Call the passed lambda for resetting
                    },
                    onDismiss = { showResetConfirmDialog = false }
                )
            } else {
                Toast.makeText(context, "No timer in this phase.", Toast.LENGTH_SHORT).show()
                showResetConfirmDialog = false
            }
        }

        if (showEndOfMainTimeDialog)
            EndOfMainTimeDialog(
                onDismiss = {
                    showEndOfMainTimeDialog = false
                },
                onStartExtraTime = {
                    showEndOfMainTimeDialog = false
                    gameViewModel.setToHaveExtraTime()
                    gameViewModel.setToHavePenalties()
                    onEndPhase()
                },
                onEndMatch = {
                    showEndOfMainTimeDialog = false
                    onEndPhase()
                }
            )
    }
}
