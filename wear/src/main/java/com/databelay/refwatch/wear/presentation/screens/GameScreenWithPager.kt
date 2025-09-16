package com.databelay.refwatch.wear.presentation.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.pager.PagerState
import androidx.wear.compose.foundation.pager.VerticalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material.PageIndicatorState
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.AlertDialogDefaults
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.PagerScaffoldDefaults
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.material3.VerticalPageIndicator
import androidx.wear.compose.material3.VerticalPagerScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.ui.tooling.preview.WearPreviewFontScales
import com.databelay.refwatch.common.CardType
import com.databelay.refwatch.common.Game
import com.databelay.refwatch.common.GamePhase
import com.databelay.refwatch.common.Team
import com.databelay.refwatch.common.hasTimer
import com.databelay.refwatch.common.logBackStack
import com.databelay.refwatch.common.readable
import com.databelay.refwatch.common.theme.RefWatchWearTheme
import kotlinx.coroutines.launch

// Sealed class to define the information for different confirmation dialogs
sealed class ConfirmationDialogInfo(
    val title: String,
    val text: String? = null,
    val confirmButtonText: String = "Confirm",
    val dismissButtonText: String = "Dismiss",
    val onConfirmAction: () -> Unit, // Action for confirm button
    val onDismissDialogAction: () -> Unit // Action for dismiss button AND onDismissRequest
) {
    class EndPhase(
        gamePhaseReadable: String,
        onConfirm: () -> Unit,
        onDialogClose: () -> Unit // Common action for closing dialog (e.g., animate, clear state)
    ) : ConfirmationDialogInfo(
        title = "End $gamePhaseReadable?",
        onConfirmAction = { 
            onConfirm()
            onDialogClose()
        },
        onDismissDialogAction = onDialogClose
    )

    class FinishGame(
        onConfirm: () -> Unit, 
        onDialogClose: () -> Unit
    ) : ConfirmationDialogInfo(
        title = "Finish Game?",
        text = "Are you sure you want to end and save this game?",
        onConfirmAction = {
            onConfirm()
            onDialogClose()
        },
        onDismissDialogAction = onDialogClose
    )

    class ResetPeriodTimer(
        gamePhaseReadable: String,
        onConfirm: () -> Unit,
        onDialogClose: () -> Unit
    ) : ConfirmationDialogInfo(
        title = "Reset Timer?",
        text = "Reset timer for $gamePhaseReadable?",
        onConfirmAction = {
            onConfirm()
            onDialogClose()
        },
        onDismissDialogAction = onDialogClose
    )

    class ResetFullGame(
        onConfirm: () -> Unit,
        onDialogClose: () -> Unit // Even if no animation, good to have consistent close logic
    ) : ConfirmationDialogInfo(
        title = "Reset Full Game?",
        text = "This will erase all scores and logs for this game. Are you sure?",
        confirmButtonText = "Yes",
        dismissButtonText = "No",
        onConfirmAction = {
            onConfirm()
            onDialogClose()
        },
        onDismissDialogAction = onDialogClose
    )

    class EndOfMainTime(
        onSetExtraTimeAndPenalties: () -> Unit, // Specific action for confirm
        onEndPhaseWithoutExtraTime: () -> Unit,   // Specific action for dismiss
        onDialogClose: () -> Unit // Common action for closing dialog
    ) : ConfirmationDialogInfo(
        title = "Extra Time?",
        confirmButtonText = "Yes",
        dismissButtonText = "No",
        onConfirmAction = {
            onSetExtraTimeAndPenalties()
            onDialogClose()
        },
        onDismissDialogAction = {
            onEndPhaseWithoutExtraTime()
            onDialogClose()
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GameScreenWithPager(
    game: Game,
    horizontalPagerState: PagerState,
    verticalPagerState: PagerState,
    onKickOff: () -> Unit,
    onResetGame: () -> Unit, // For full game reset
    onSetToHaveExtraTime: () -> Unit,
    onSetToHavePenalties: () -> Unit,
    onToggleTimer: () -> Unit,
    onAddGoal: (Team) -> Unit,
    onNavigateToLogCard: (team: Team, cardType: CardType) -> Unit,
    onNavigateToGameLog: () -> Unit,
    onEndPhase: () -> Unit,
    onResetPeriodTimer: () -> Unit, // For current period's timer
    onConfirmEndMatch: () -> Unit, // For finishing the game
    onPenaltyAttemptRecorded: (scored: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var activeDialogInfo: ConfirmationDialogInfo? by remember { mutableStateOf(null) }

    val animateToMainPage: () -> Unit = {
        coroutineScope.launch { verticalPagerState.animateScrollToPage(0) }
    }

    // Common logic for closing any dialog and animating back to the main page (if applicable)
    val createDialogCloseHandler: (Boolean) -> () -> Unit = { shouldAnimate ->
        { 
            activeDialogInfo = null 
            if (shouldAnimate) animateToMainPage() 
        }
    }

/*// Example: Call this from within your GameScreenWithPager when phase is HALF_TIME
    LaunchedEffect(game?.currentPhase) {
        if (game?.currentPhase == GamePhase.HALF_TIME) {
            logBackStack(navController, "Half-Time UI in GameScreenWithPager")
        }
    }*/

    val pageIndicatorState: PageIndicatorState = remember(
        horizontalPagerState.currentPage,
        horizontalPagerState.pageCount,
        horizontalPagerState.currentPageOffsetFraction
    ) {
        object : PageIndicatorState {
            override val pageOffset: Float
                get() = horizontalPagerState.currentPageOffsetFraction
            override val selectedPage: Int
                get() = horizontalPagerState.currentPage
            override val pageCount: Int
                get() = horizontalPagerState.pageCount
        }
    }
    ScreenScaffold(
        modifier = modifier
            .fillMaxSize()
            .padding(2.dp)
    ) { _ ->
        VerticalPagerScaffold(
            pagerState = verticalPagerState,
            pageIndicator = { VerticalPageIndicator(pagerState = verticalPagerState) },
        )
        {
            VerticalPager(
                flingBehavior =
                PagerScaffoldDefaults.snapWithSpringFlingBehavior(state = verticalPagerState),
                state = verticalPagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> {
                        GamePagerContent(
                            game = game,
                            pagerState = horizontalPagerState,
                            pageIndicatorState = pageIndicatorState,
                            onKickOff = onKickOff,
                            onAddGoal = onAddGoal,
                            onNavigateToLogCard = onNavigateToLogCard,
                            onPenaltyAttemptRecorded = onPenaltyAttemptRecorded,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    1 -> {
                        GameSettingsScreen(
                            game = game,
                            onAttemptFinishGame = {
                                activeDialogInfo = ConfirmationDialogInfo.FinishGame(
                                    onConfirm = onConfirmEndMatch,
                                    onDialogClose = createDialogCloseHandler(true)
                                )
                            },
                            onAttemptResetPeriodTimer = {
                                if (game.currentPhase.hasTimer()) {
                                    activeDialogInfo = ConfirmationDialogInfo.ResetPeriodTimer(
                                        gamePhaseReadable = game.currentPhase.readable(),
                                        onConfirm = onResetPeriodTimer,
                                        onDialogClose = createDialogCloseHandler(true)
                                    )
                                } else {
                                    Toast.makeText(context, "No timer in this phase.", Toast.LENGTH_SHORT).show()
                                    animateToMainPage() 
                                }
                            },
                            onAttemptResetFullGame = {
                                activeDialogInfo = ConfirmationDialogInfo.ResetFullGame(
                                    onConfirm = onResetGame,
                                    onDialogClose = createDialogCloseHandler(false) // No animation for full game reset
                                )
                            },
                            onViewLog = onNavigateToGameLog,
                            onToggleTimer = onToggleTimer,
                            onAttemptEndPhase = {
                                if (game.currentPhase == GamePhase.SECOND_HALF && game.isTied) {
                                    activeDialogInfo = ConfirmationDialogInfo.EndOfMainTime(
                                        onSetExtraTimeAndPenalties = {
                                            onSetToHaveExtraTime()
                                            onSetToHavePenalties()
                                            onEndPhase() 
                                        },
                                        onEndPhaseWithoutExtraTime = {
                                            onEndPhase() 
                                        },
                                        onDialogClose = createDialogCloseHandler(true)
                                    )
                                } else {
                                    activeDialogInfo = ConfirmationDialogInfo.EndPhase(
                                        gamePhaseReadable = game.currentPhase.readable(),
                                        onConfirm = onEndPhase,
                                        onDialogClose = createDialogCloseHandler(true)
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }

    // Unified Confirmation Dialog
    activeDialogInfo?.let { dialogInfo ->
        Log.d("ConfirmationDialog", "Showing dialog: ${dialogInfo.title}")
        AlertDialog(
            visible = true,
            onDismissRequest = {
                dialogInfo.onDismissDialogAction() // This handles specific dismiss logic + common close logic
            },
            title = { Text(dialogInfo.title, color = MaterialTheme.colorScheme.primary) },
            dismissButton = {
                AlertDialogDefaults.DismissButton(
                    onClick = {
                        dialogInfo.onDismissDialogAction() // This handles specific dismiss logic + common close logic
                    },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = androidx.compose.ui.graphics.Color.Transparent, // Standard for dismiss
//                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) { Text(dialogInfo.dismissButtonText) }
            },
            text = { dialogInfo.text?.let { Text(it) } },
            confirmButton = {
                AlertDialogDefaults.ConfirmButton(
                    onClick = {
                        dialogInfo.onConfirmAction() // This handles specific confirm logic + common close logic
                    }
                ) { Text(dialogInfo.confirmButtonText) }
            },
        )
    }
}

// -------------------------------- Previews -----------------------------------------------
@OptIn(ExperimentalFoundationApi::class)
@Preview(device = "id:wearos_small_round", showSystemUi = true, backgroundColor = 0xff000000, showBackground = true)
@Composable
fun GameScreenWithPagerPreviewSmallRegulationTime() {
    val sampleGame = Game.defaults().copy(
        currentPhase = GamePhase.FIRST_HALF,
        isTimerRunning = true,
        actualTimeElapsedInPeriodMillis = (10 * 60000L)
    )
    val horizontalPagerState = rememberPagerState(initialPage = 1, pageCount = { 3 })
    val verticalPagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })

    RefWatchWearTheme {
        GameScreenWithPager(
            game = sampleGame,
            horizontalPagerState = horizontalPagerState,
            verticalPagerState = verticalPagerState,
            onKickOff = {},
            onResetGame = {},
            onSetToHaveExtraTime = {},
            onSetToHavePenalties = {},
            onToggleTimer = {},
            onAddGoal = {},
            onNavigateToLogCard = { _: Team, _: CardType -> },
            onNavigateToGameLog = {},
            onEndPhase = {},
            onResetPeriodTimer = {},
            onConfirmEndMatch = {},
            onPenaltyAttemptRecorded = {}
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Preview(device = "id:wearos_small_round", name = "Settings Page Open", showSystemUi = true, backgroundColor = 0xff000000, showBackground = true)
@Composable
fun GameScreenWithPagerPreviewSettingsOpen() {
    val sampleGame = Game.defaults().copy(currentPhase = GamePhase.FIRST_HALF)
    val horizontalPagerState = rememberPagerState(initialPage = 1, pageCount = { 3 })
    val verticalPagerState = rememberPagerState(initialPage = 1, pageCount = { 2 }) // Start on settings page

    RefWatchWearTheme {
        GameScreenWithPager(
            game = sampleGame,
            horizontalPagerState = horizontalPagerState,
            verticalPagerState = verticalPagerState,
            onKickOff = {},
            onResetGame = {},
            onSetToHaveExtraTime = {},
            onSetToHavePenalties = {},
            onToggleTimer = {},
            onAddGoal = {},
            onNavigateToLogCard = { _: Team, _: CardType -> },
            onNavigateToGameLog = {},
            onEndPhase = {},
            onResetPeriodTimer = {},
            onConfirmEndMatch = {},
            onPenaltyAttemptRecorded = {}
        )
    }
}


@Preview(device = "id:wearos_small_round",name = "AddedTime SmRnd",showBackground = true)
@Preview(device = "id:wearos_large_round",name = "AddedTime LrgRnd",showBackground = true)
@Preview(device = "id:wearos_square",name = "AddedTime Sqr",showBackground = true)
@WearPreviewFontScales
@Composable
fun Preview_MainGameDisplay_Penalties() {
    val sampleGame = Game.defaults().copy(currentPhase = GamePhase.PENALTIES)
    val horizontalPagerState = rememberPagerState(initialPage = 1, pageCount = { 1 })
    val verticalPagerState = rememberPagerState(initialPage = 0, pageCount = { 2 }) // Start on settings page

    RefWatchWearTheme {
        GameScreenWithPager(
            game = sampleGame,
            horizontalPagerState = horizontalPagerState,
            verticalPagerState = verticalPagerState,
            onKickOff = {},
            onResetGame = {},
            onSetToHaveExtraTime = {},
            onSetToHavePenalties = {},
            onToggleTimer = {},
            onAddGoal = {},
            onNavigateToLogCard = { _: Team, _: CardType -> },
            onNavigateToGameLog = {},
            onEndPhase = {},
            onResetPeriodTimer = {},
            onConfirmEndMatch = {},
            onPenaltyAttemptRecorded = {}
        )
    }
}