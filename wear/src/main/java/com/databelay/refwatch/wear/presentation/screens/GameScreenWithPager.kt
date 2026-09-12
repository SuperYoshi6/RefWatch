package com.databelay.refwatch.wear.presentation.screens

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.ui.tooling.preview.Preview
import com.databelay.refwatch.common.isTied
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.foundation.pager.PagerState
import androidx.wear.compose.foundation.pager.VerticalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material.PageIndicatorState
import androidx.wear.compose.material3.*
import androidx.wear.compose.ui.tooling.preview.WearPreviewFontScales
import com.android.tools.screenshot.PreviewTest
import com.databelay.refwatch.R
import com.databelay.refwatch.common.*
import com.databelay.refwatch.common.theme.RefWatchWearTheme
import com.databelay.refwatch.wear.TimerDisplayMode
import com.databelay.refwatch.wear.data.TimerState
import com.databelay.refwatch.wear.presentation.utils.localizedName
import com.google.android.horologist.compose.layout.ColumnItemType
import com.google.android.horologist.compose.layout.ColumnItemType.Companion.EdgeButtonPadding
import com.google.android.horologist.compose.layout.rememberResponsiveColumnPadding
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource

// Sealed class to define the information for different confirmation dialogs
// All string parameters must be pre-localized (call stringResource before constructing).
sealed class ConfirmationDialogInfo(
    val title: String,
    val text: String? = null,
    val confirmButtonText: String,
    val dismissButtonText: String,
    val onConfirmAction: () -> Unit,
    val onDismissDialogAction: () -> Unit
) {
    class EndPhase(
        title: String,
        onConfirm: () -> Unit,
        onDialogClose: () -> Unit
    ) : ConfirmationDialogInfo(
        title = title,
        confirmButtonText = "",
        dismissButtonText = "",
        onConfirmAction = { 
            onConfirm()
            onDialogClose()
        },
        onDismissDialogAction = onDialogClose
    )

    class FinishGame(
        title: String,
        text: String?,
        onConfirm: () -> Unit, 
        onDialogClose: () -> Unit
    ) : ConfirmationDialogInfo(
        title = title,
        text = text,
        confirmButtonText = "",
        dismissButtonText = "",
        onConfirmAction = {
            onConfirm()
            onDialogClose()
        },
        onDismissDialogAction = onDialogClose
    )

    class ResetPeriodTimer(
        title: String,
        text: String,
        onConfirm: () -> Unit,
        onDialogClose: () -> Unit
    ) : ConfirmationDialogInfo(
        title = title,
        text = text,
        confirmButtonText = "",
        dismissButtonText = "",
        onConfirmAction = {
            onConfirm()
            onDialogClose()
        },
        onDismissDialogAction = onDialogClose
    )

    class ResetFullGame(
        title: String,
        text: String,
        confirmButtonText: String,
        dismissButtonText: String,
        onConfirm: () -> Unit,
        onDialogClose: () -> Unit
    ) : ConfirmationDialogInfo(
        title = title,
        text = text,
        confirmButtonText = confirmButtonText,
        dismissButtonText = dismissButtonText,
        onConfirmAction = {
            onConfirm()
            onDialogClose()
        },
        onDismissDialogAction = onDialogClose
    )

    class EndOfMainTime(
        title: String,
        confirmButtonText: String,
        dismissButtonText: String,
        onSetExtraTimeAndPenalties: () -> Unit,
        onEndPhaseWithoutExtraTime: () -> Unit,
        onDialogClose: () -> Unit
    ) : ConfirmationDialogInfo(
        title = title,
        confirmButtonText = confirmButtonText,
        dismissButtonText = dismissButtonText,
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
        title: String,
        text: String,
        confirmButtonText: String,
        dismissButtonText: String,
        onConfirm: () -> Unit,
        onDialogClose: () -> Unit
    ) : ConfirmationDialogInfo(
        title = title,
        text = text,
        confirmButtonText = confirmButtonText,
        dismissButtonText = dismissButtonText,
        onConfirmAction = {
            onConfirm()
            onDialogClose()
        },
        onDismissDialogAction = onDialogClose
    )

    class AbortGame(
        title: String,
        text: String,
        onConfirm: () -> Unit,
        onDialogClose: () -> Unit
    ) : ConfirmationDialogInfo(
        title = title,
        text = text,
        confirmButtonText = "",
        dismissButtonText = "",
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
    timerStateFlow: StateFlow<TimerState>,
    isAmbient: Boolean = false,
    kickoffCountdownSeconds: Int? = null,
    timerDisplayMode: TimerDisplayMode = TimerDisplayMode.REMAINING,
    activeDismissals: List<TemporaryDismissalEvent> = emptyList(),
    pendingReturnConfirmations: List<TemporaryDismissalEvent> = emptyList(),
    onConfirmReturn: (TemporaryDismissalEvent) -> Unit = {},
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
    onQuickSubstitution: (Team, String, String) -> Unit = { _, _, _ -> },
    onNavigateToGameLog: () -> Unit,
    onEndPhase: () -> Unit,
    onAbortMatch: () -> Unit,
    onResetPeriodTimer: () -> Unit, // For current period's timer
    onConfirmEndMatch: () -> Unit, // For finishing the game
    onPenaltyAttemptRecorded: (scored: Boolean, kickerNumber: Int?) -> Unit,
    onUndoLastEvent: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    var activeDialogInfo: ConfirmationDialogInfo? by remember { mutableStateOf(null) }
    // transitions blink white borders after the confirmation dialog
    //        animatescrolltopage flashing white borders (commented out for now)
    val animateToMainPage: () -> Unit = {
        coroutineScope.launch { verticalPagerState.scrollToPage(1) }
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
                userScrollEnabled = !isAmbient,
                modifier = Modifier
                    .fillMaxSize()
                    .onRotaryScrollEvent {
                        coroutineScope.launch {
                            if (it.verticalScrollPixels > 0) {
                                val next = (verticalPagerState.currentPage + 1).coerceAtMost(verticalPagerState.pageCount - 1)
                                verticalPagerState.animateScrollToPage(next)
                            } else {
                                val prev = (verticalPagerState.currentPage - 1).coerceAtLeast(0)
                                verticalPagerState.animateScrollToPage(prev)
                            }
                        }
                        true
                    }
                    .focusRequester(focusRequester)
                    .focusable()
            ) { page ->
                when (page) {
                    0 -> {
                        DismissalOverviewPage(
                            game = game,
                            activeDismissals = activeDismissals,
                            timerStateFlow = timerStateFlow
                        )
                    }
                    1 -> {
                        GamePagerContent(
                            game = game,
                            timerStateFlow = timerStateFlow,
                            isAmbient = isAmbient,
                            kickoffCountdownSeconds = kickoffCountdownSeconds,
                            timerDisplayMode = timerDisplayMode,
                            activeDismissals = activeDismissals,
                            pendingReturnConfirmations = pendingReturnConfirmations,
                            onConfirmReturn = onConfirmReturn,
                            onToggleTimerDisplayMode = onToggleTimerDisplayMode,
                            pagerState = horizontalPagerState,
                            pageIndicatorState = pageIndicatorState,
                            onKickOff = onKickOff,
                            onNavigateToLogGoal = onNavigateToLogGoal,
                            onNavigateToLogCard = onNavigateToLogCard,
                            onNavigateToLogSubstitution = onNavigateToLogSubstitution,
                            onQuickSubstitution = onQuickSubstitution,
                            onPenaltyAttemptRecorded = onPenaltyAttemptRecorded,
                            onToggleTimer = onToggleTimer,
                            onToggleStoppageTimer = onToggleStoppageTimer,
                            onUndoLastEvent = onUndoLastEvent,
                            onOpenGameMenu = {
                                coroutineScope.launch { verticalPagerState.scrollToPage(2) }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    2 -> {
                        BackHandler {
                            coroutineScope.launch { verticalPagerState.scrollToPage(1) }
                        }
                        val currentPhaseLocalized = game.currentPhase.localizedName()
                        val finishTitle = stringResource(R.string.finish_game_title)
                        val finishMsg = stringResource(R.string.finish_game_msg)
                        val resetTimerTitle = stringResource(R.string.reset_timer_title)
                        val fullResetTitle = stringResource(R.string.full_reset_title)
                        val fullResetMsg = stringResource(R.string.full_reset_msg)
                        val yesResetText = stringResource(R.string.confirm_yes_reset)
                        val noText = stringResource(R.string.dismiss_no)
                        val extraTimeQuestion = stringResource(R.string.extra_time_question)
                        val extraTimeConfirm = stringResource(R.string.extra_time_confirm)
                        val endGameDismiss = stringResource(R.string.dismiss_end_game)
                        val resetTimerMsg = stringResource(R.string.reset_timer_msg, game.currentPhase.localizedName())
                        val endPhaseTitle = stringResource(R.string.end_phase_confirm_title, currentPhaseLocalized)
                        val abortTitle = stringResource(R.string.abort_game_title)
                        val abortMsg = stringResource(R.string.abort_game_msg)
                        
                        var showRosterSelection by remember { mutableStateOf(false) }
                        var selectedRosterTeam by remember { mutableStateOf<Team?>(null) }
                        
                        if (showRosterSelection) {
                            Dialog(visible = true, onDismissRequest = { showRosterSelection = false }) {
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(stringResource(R.string.select_roster), style = MaterialTheme.typography.titleSmall)
                                    Spacer(Modifier.height(8.dp))
                                    Button(
                                        onClick = { 
                                            selectedRosterTeam = Team.HOME
                                            showRosterSelection = false
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text(game.homeTeamName) }
                                    Spacer(Modifier.height(4.dp))
                                    Button(
                                        onClick = { 
                                            selectedRosterTeam = Team.AWAY
                                            showRosterSelection = false
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text(game.awayTeamName) }
                                    Spacer(Modifier.height(8.dp))
                                    TextButton(onClick = { showRosterSelection = false }) { Text(stringResource(R.string.back)) }
                                }
                            }
                        }

                        if (selectedRosterTeam != null) {
                            val roster = if (selectedRosterTeam == Team.HOME) game.homeRoster else game.awayRoster
                            Dialog(visible = true, onDismissRequest = { selectedRosterTeam = null }) {
                                val listState = rememberTransformingLazyColumnState()
                                TransformingLazyColumn(
                                    state = listState,
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    contentPadding = PaddingValues(top = 32.dp, bottom = 32.dp, start = 8.dp, end = 8.dp)
                                ) {
                                    item {
                                        Text(
                                            text = "${stringResource(R.string.player_label)}: ${if (selectedRosterTeam == Team.HOME) game.homeTeamName else game.awayTeamName}",
                                            style = MaterialTheme.typography.titleSmall,
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )
                                    }
                                    
                                    val sortedRoster = roster.sortedBy { it.number }
                                    items(sortedRoster) { player ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = player.number.toString(),
                                                fontWeight = FontWeight.Black,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.width(28.dp),
                                                textAlign = TextAlign.Center
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = player.name.ifBlank { stringResource(R.string.player_label) },
                                                style = MaterialTheme.typography.bodySmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                text = if (player.onField) "⚽" else "🪑",
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                    
                                    item {
                                        Button(
                                            onClick = { selectedRosterTeam = null },
                                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                        ) {
                                            Text(stringResource(R.string.close))
                                        }
                                    }
                                }
                            }
                        }

                        GameSettingsScreen(
                            game = game,
                            onAttemptFinishGame = {
                                activeDialogInfo = ConfirmationDialogInfo.FinishGame(
                                    title = finishTitle,
                                    text = finishMsg,
                                    onConfirm = onConfirmEndMatch,
                                    onDialogClose = createDialogCloseHandler(true)
                                )
                            },
                            onAttemptResetPeriodTimer = {
                                if (game.currentPhase.hasTimer()) {
                                    activeDialogInfo = ConfirmationDialogInfo.ResetPeriodTimer(
                                        title = resetTimerTitle,
                                        text = resetTimerMsg,
                                        onConfirm = onResetPeriodTimer,
                                        onDialogClose = createDialogCloseHandler(true)
                                    )
                                } else {
                                    Toast.makeText(context, context.getString(R.string.no_timer_in_phase), Toast.LENGTH_SHORT).show()
                                    animateToMainPage()
                                }
                            },
                            onAttemptResetFullGame = {
                                activeDialogInfo = ConfirmationDialogInfo.ResetFullGame(
                                    title = fullResetTitle,
                                    text = fullResetMsg,
                                    confirmButtonText = yesResetText,
                                    dismissButtonText = noText,
                                    onConfirm = onResetGame,
                                    onDialogClose = createDialogCloseHandler(false)
                                )
                            },
                            onViewLog = onNavigateToGameLog,
                            onShowRoster = { showRosterSelection = true },
                            onToggleTimer = onToggleTimer,
                            onAttemptEndPhase = {
                                if (game.currentPhase == GamePhase.SECOND_HALF && game.isTied) {
                                    activeDialogInfo = ConfirmationDialogInfo.EndOfMainTime(
                                        title = extraTimeQuestion,
                                        confirmButtonText = extraTimeConfirm,
                                        dismissButtonText = endGameDismiss,
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
                                        title = endPhaseTitle,
                                        onConfirm = onEndPhase,
                                        onDialogClose = createDialogCloseHandler(true)
                                    )
                                }
                            },
                            onAttemptAbortGame = {
                                activeDialogInfo = ConfirmationDialogInfo.AbortGame(
                                    title = abortTitle,
                                    text = abortMsg,
                                    onConfirm = onAbortMatch,
                                    onDialogClose = createDialogCloseHandler(true)
                                )
                            },
                            onUndoLastEvent = onUndoLastEvent,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }

    // Unified Confirmation Dialog
    activeDialogInfo?.let { dialogInfo ->
        if (dialogInfo is ConfirmationDialogInfo.AbortGame) {
             AlertDialog(
                visible = true,
                onDismissRequest = dialogInfo.onDismissDialogAction,
                title = { Text(dialogInfo.title) },
                text = { dialogInfo.text?.let { Text(it) } },
                confirmButton = {
                    AlertDialogDefaults.ConfirmButton(onClick = dialogInfo.onConfirmAction)
                },
                dismissButton = {
                    AlertDialogDefaults.DismissButton(onClick = dialogInfo.onDismissDialogAction)
                }
            )
        } else {
            UnifiedConfirmationDialog(dialogInfo = dialogInfo)
        }
    }
}

@Composable
private fun DismissalOverviewPage(
    game: Game,
    activeDismissals: List<TemporaryDismissalEvent>,
    timerStateFlow: StateFlow<TimerState>
) {
    val listState = rememberScalingLazyListState()
    val timerState by timerStateFlow.collectAsStateWithLifecycle()
    val currentMatchTime = timerState.actualTimeElapsedInPeriodMillis

    ScalingLazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(top = 32.dp, bottom = 32.dp, start = 8.dp, end = 8.dp)
    ) {
        item {
            Text(
                stringResource(R.string.time_penalties),
                style = MaterialTheme.typography.titleMedium,
                color = Color.Yellow,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        if (activeDismissals.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.no_active_dismissals),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                )
            }
        } else {
            items(activeDismissals) { dismissal ->
                val elapsedSinceStart = currentMatchTime - dismissal.startMatchTimeMillis
                val remainingMillis = (dismissal.durationMinutes * 60 * 1000L - elapsedSinceStart.toLong()).coerceAtLeast(0L)
                val teamAbbr = if (dismissal.team == Team.HOME) {
                    game.homeTeamAbbr?.takeIf { it.isNotBlank() } ?: "HEI"
                } else {
                    game.awayTeamAbbr?.takeIf { it.isNotBlank() } ?: "GAS"
                }

                Card(
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Yellow.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "$teamAbbr #${dismissal.playerNumber}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = remainingMillis.formatTime(false),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Yellow
                        )
                    }
                }
            }
        }
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
            timerStateFlow = MutableStateFlow(TimerState()),
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
            onAbortMatch = {},
            onResetPeriodTimer = {},
            onConfirmEndMatch = {},
            onPenaltyAttemptRecorded = { _, _ -> },
            onNavigateToLogSubstitution = {},
            onQuickSubstitution = { _, _, _ -> }
        )
    }
}
