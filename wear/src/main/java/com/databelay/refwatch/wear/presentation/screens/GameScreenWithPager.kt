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
import androidx.activity.compose.BackHandler
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
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
import com.android.tools.screenshot.PreviewTest
import com.databelay.refwatch.common.CardType
import com.databelay.refwatch.common.Game
import com.databelay.refwatch.common.GamePhase
import com.databelay.refwatch.common.Team
import com.databelay.refwatch.common.hasTimer
import com.databelay.refwatch.common.logBackStack
import com.databelay.refwatch.common.readable
import com.databelay.refwatch.common.theme.RefWatchWearTheme
import com.databelay.refwatch.wear.TimerDisplayMode
import com.google.android.horologist.compose.layout.ColumnItemType
import com.google.android.horologist.compose.layout.ColumnItemType.Companion.EdgeButtonPadding
import com.google.android.horologist.compose.layout.rememberResponsiveColumnPadding
import kotlinx.coroutines.launch

// Sealed class to define the information for different confirmation dialogs
sealed class ConfirmationDialogInfo(
    val title: String,
    val text: String? = null,
    val confirmButtonText: String = "Bestätigen",
    val dismissButtonText: String = "Abbrechen",
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
        title = "Spiel beenden?",
        text = "Bist du sicher, dass du dieses Spiel beenden und speichern möchtest?",
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
        title = "Spielzeit zurücksetzen?",
        text = "Spielzeit für $gamePhaseReadable zurücksetzen?",
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
        title = "Vollständiges Spiel zurücksetzen?",
        text = "Diese Aktion wird alle Ergebnisse und das komplette Protokoll dieses Spiels löschen. Bist du sicher?",
        confirmButtonText = "Ja, zurücksetzen",
        dismissButtonText = "Nein",
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
        title = "Verlängerung?",
        confirmButtonText = "Ja, Verlängerung",
        dismissButtonText = "Nein, Spiel beenden",
        onConfirmAction = {
            onSetExtraTimeAndPenalties()
            onDialogClose()
        },
        onDismissDialogAction = {
            onEndPhaseWithoutExtraTime()
            onDialogClose()
        }
    )

    class RemoveLogEvent(
        onConfirm: () -> Unit,
        onDialogClose: () -> Unit // Even if no animation, good to have consistent close logic
    ) : ConfirmationDialogInfo(
        title = "Protokoll-Ereignis löschen?",
        text = "Diese Aktion wird das Ereignis löschen und möglicherweise den Spielstand aktualisieren. Bist du sicher?",
        confirmButtonText = "Ja",
        dismissButtonText = "Nein",
        onConfirmAction = {
            onConfirm()
            onDialogClose()
        },
        onDismissDialogAction = onDialogClose
    )
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GameScreenWithPager(
    game: Game,
    kickoffCountdownSeconds: Int? = null,
    timerDisplayMode: TimerDisplayMode = TimerDisplayMode.REMAINING,
    onToggleTimerDisplayMode: () -> Unit = {},
    horizontalPagerState: PagerState,
    verticalPagerState: PagerState,
    onKickOff: () -> Unit,
    onResetGame: () -> Unit, // For full game reset
    onSetToHaveExtraTime: () -> Unit,
    onSetToHavePenalties: () -> Unit,
    onToggleTimer: () -> Unit,
    onToggleStoppageTimer: () -> Unit = {},
    onNavigateToLogGoal: (Team, com.databelay.refwatch.common.GoalType) -> Unit,
    onNavigateToLogCard: (team: Team, cardType: CardType) -> Unit,
    onNavigateToLogSubstitution: (Team) -> Unit,
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
    // transitions blink white borders after the confirmation dialog
    //        animatescrolltopage flashing white borders (commented out for now)
    val animateToMainPage: () -> Unit = {
//        coroutineScope.launch { verticalPagerState.animateScrollToPage(0) }
        coroutineScope.launch { verticalPagerState.scrollToPage(0) }
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
                userScrollEnabled = false,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> {
                        GamePagerContent(
                            game = game,
                            kickoffCountdownSeconds = kickoffCountdownSeconds,
                            timerDisplayMode = timerDisplayMode,
                            onToggleTimerDisplayMode = onToggleTimerDisplayMode,
                            pagerState = horizontalPagerState,
                            pageIndicatorState = pageIndicatorState,
                            onKickOff = onKickOff,
                            onNavigateToLogGoal = onNavigateToLogGoal,
                            onNavigateToLogCard = onNavigateToLogCard,
                            onNavigateToLogSubstitution = onNavigateToLogSubstitution,
                            onPenaltyAttemptRecorded = onPenaltyAttemptRecorded,
                            onToggleTimer = onToggleTimer,
                            onToggleStoppageTimer = onToggleStoppageTimer,
                            onOpenGameMenu = {
                                coroutineScope.launch { verticalPagerState.scrollToPage(1) }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    1 -> {
                        BackHandler {
                            coroutineScope.launch { verticalPagerState.scrollToPage(0) }
                        }
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
        UnifiedConfirmationDialog(dialogInfo = dialogInfo)
    }
}

// -------------------------------- Previews -----------------------------------------------
@PreviewTest
@OptIn(ExperimentalFoundationApi::class)
@Preview(device = "id:wearos_large_round", showSystemUi = true, backgroundColor = 0xff000000, showBackground = true)
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
            onToggleStoppageTimer = {},
            onNavigateToLogGoal = { _, _ -> },
            onNavigateToLogCard = { _: Team, _: CardType -> },
            onNavigateToGameLog = {},
            onEndPhase = {},
            onResetPeriodTimer = {},
            onConfirmEndMatch = {},
            onPenaltyAttemptRecorded = {},
            onNavigateToLogSubstitution = {}
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Preview(device = "id:wearos_large_round", name = "Settings Page Open", showSystemUi = true, backgroundColor = 0xff000000, showBackground = true)
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
            onToggleStoppageTimer = {},
            onNavigateToLogGoal = { _, _ -> },
            onNavigateToLogCard = { _: Team, _: CardType -> },
            onNavigateToGameLog = {},
            onEndPhase = {},
            onResetPeriodTimer = {},
            onConfirmEndMatch = {},
            onPenaltyAttemptRecorded = {},
            onNavigateToLogSubstitution = {}
        )
    }
}

//@Preview(device = "id:wearos_small_round",name = "AddedTime SmRnd",showBackground = true)
//@Preview(device = "id:wearos_square",name = "AddedTime Sqr",showBackground = true)
@Preview(device = "id:wearos_large_round",name = "AddedTime LrgRnd",showSystemUi = true, backgroundColor = 0xff000000, showBackground = true)
//@WearPreviewFontScales
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
            onToggleStoppageTimer = {},
            onNavigateToLogGoal = { _, _ -> },
            onNavigateToLogCard = { _: Team, _: CardType -> },
            onNavigateToGameLog = {},
            onEndPhase = {},
            onResetPeriodTimer = {},
            onConfirmEndMatch = {},
            onPenaltyAttemptRecorded = {},
            onNavigateToLogSubstitution = {}
        )
    }
}

@Composable
fun Test() {
    val sampleGame = Game.Companion.defaults().copy(
        currentPhase = GamePhase.FIRST_HALF,
        isTimerRunning = true,
        actualTimeElapsedInPeriodMillis = (10 * 60000L)
    )
    val horizontalPagerState = rememberPagerState(initialPage = 1, pageCount = { 3 })
    val verticalPagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })


    val scrollState = rememberTransformingLazyColumnState()

    /* If you have enough items in your list, use [TransformingLazyColumn] which is an optimized
     * version of LazyColumn for wear devices with some added features. For more information,
     * see d.android.com/wear/compose.
     */
    ScreenScaffold(
        scrollState = scrollState,
        contentPadding =
            rememberResponsiveColumnPadding(
                first = ColumnItemType.ListHeader,
                last = EdgeButtonPadding
            )
    ) { contentPadding ->
        // Use workaround from Horologist for padding or wait until fix lands
        TransformingLazyColumn(
            state = scrollState,
            contentPadding = contentPadding
        ) {
            item { Text("Header") }
        }
    }
}
