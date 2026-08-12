package com.databelay.refwatch.wear.presentation.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.databelay.refwatch.R
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.PagerState
import androidx.wear.compose.material.HorizontalPageIndicator
import androidx.wear.compose.material.PageIndicatorState
import com.databelay.refwatch.common.CardType
import com.databelay.refwatch.common.Game
import com.databelay.refwatch.common.GamePhase
import com.databelay.refwatch.common.TemporaryDismissalEvent
import com.databelay.refwatch.wear.presentation.screens.PenaltyShootoutScreen
import com.databelay.refwatch.common.Team
import com.databelay.refwatch.common.isPlayablePhase
import kotlinx.coroutines.launch
import androidx.wear.compose.material3.AnimatedPage
import androidx.wear.compose.material3.HorizontalPagerScaffold
import androidx.wear.compose.material3.PagerScaffoldDefaults
import com.databelay.refwatch.common.GoalType
import com.databelay.refwatch.wear.data.TimerState
import com.databelay.refwatch.wear.TimerDisplayMode

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GamePagerContent(
    game: Game,
    timerState: TimerState,
    isAmbient: Boolean = false,
    kickoffCountdownSeconds: Int? = null,
    timerDisplayMode: TimerDisplayMode = TimerDisplayMode.REMAINING,
    activeDismissals: List<TemporaryDismissalEvent> = emptyList(),
    pendingReturnConfirmations: List<TemporaryDismissalEvent> = emptyList(),
    onConfirmReturn: (TemporaryDismissalEvent) -> Unit = {},
    pagerState: PagerState,
    pageIndicatorState: PageIndicatorState,

    onKickOff: () -> Unit,
    onToggleTimerDisplayMode: () -> Unit = {},
    onNavigateToLogGoal: (Team, GoalType) -> Unit,
    onNavigateToLogCard: (Team, CardType) -> Unit,
    onNavigateToLogSubstitution: (Team) -> Unit,
    onQuickSubstitution: (Team, Int, Int) -> Unit = { _, _, _ -> },
    onPenaltyAttemptRecorded: (Boolean, Int?) -> Unit,
    onToggleTimer: () -> Unit,
    onToggleStoppageTimer: () -> Unit = {},
    onOpenGameMenu: () -> Unit = {},

    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    val isPenaltiesPhase = game.currentPhase == GamePhase.PENALTIES
    val isPlayableRegularPhase = game.currentPhase.isPlayablePhase() && !isPenaltiesPhase

    Box(modifier = modifier) {
        when {
            isPenaltiesPhase -> {
                PenaltyShootoutScreen(
                    game = game,
                    onPenaltyAttemptRecorded = onPenaltyAttemptRecorded,
                    modifier = Modifier.fillMaxSize()
                )
            }

            game.currentPhase == GamePhase.GAME_ENDED -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.phase_game_ended),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${game.homeScore} : ${game.awayScore}",
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            isPlayableRegularPhase -> {
                HorizontalPagerScaffold(
                    pagerState = pagerState,
                    pageIndicator = { if (!isAmbient) HorizontalPageIndicator(pageIndicatorState) },
                ) {
                    HorizontalPager(
                        state = pagerState,
                        flingBehavior = PagerScaffoldDefaults.snapWithSpringFlingBehavior(state = pagerState),
                        modifier = Modifier.fillMaxSize(),
                        userScrollEnabled = !isAmbient
                    ) { page ->
                        when (page) {
                            0 -> AnimatedPage(pageIndex = page, pagerState = pagerState) {
                                TeamActionsPage(
                                    team = Team.HOME,
                                    game = game,
                                    onNavigateToLogGoal = { team, goalType ->
                                        onNavigateToLogGoal(team, goalType)
                                        coroutineScope.launch { pagerState.animateScrollToPage(1) }
                                    },
                                    onNavigateToLogCard = onNavigateToLogCard,
                                    onNavigateToLogSubstitution = onNavigateToLogSubstitution,
                                    onQuickSubstitution = onQuickSubstitution
                                )
                            }
                            1 -> AnimatedPage(pageIndex = page, pagerState = pagerState) {
                                MainGameDisplayScreen(
                                    game = game,
                                    timerState = timerState,
                                    isAmbient = isAmbient,
                                    kickoffCountdownSeconds = kickoffCountdownSeconds,
                                    activeDismissals = activeDismissals,
                                    pendingReturnConfirmations = pendingReturnConfirmations,
                                    onConfirmReturn = onConfirmReturn,
                                    isPlayedTime = timerDisplayMode == TimerDisplayMode.PLAYED,
                                    onToggleTimerDisplayMode = onToggleTimerDisplayMode,
                                    onKickOff = onKickOff,
                                    onToggleTimer = onToggleTimer,
                                    onToggleStoppageTimer = onToggleStoppageTimer,
                                    onOpenGameMenu = onOpenGameMenu,
                                    onNavigateToLogGoal = onNavigateToLogGoal,
                                    onNavigateToLogCard = onNavigateToLogCard,
                                    onQuickSubstitution = onQuickSubstitution
                                )
                            }
                            2 -> AnimatedPage(pageIndex = page, pagerState = pagerState) {
                                TeamActionsPage(
                                    team = Team.AWAY,
                                    game = game,
                                    onNavigateToLogGoal = { team, goalType ->
                                        onNavigateToLogGoal(team, goalType)
                                        coroutineScope.launch { pagerState.animateScrollToPage(1) }
                                    },
                                    onNavigateToLogCard = onNavigateToLogCard,
                                    onNavigateToLogSubstitution = onNavigateToLogSubstitution,
                                    onQuickSubstitution = onQuickSubstitution
                                )
                            }
                        }
                    }
                }
            }

            else -> {
                MainGameDisplayScreen(
                    game = game,
                    timerState = timerState,
                    isAmbient = isAmbient,
                    kickoffCountdownSeconds = kickoffCountdownSeconds,
                    activeDismissals = activeDismissals,
                    pendingReturnConfirmations = pendingReturnConfirmations,
                    onConfirmReturn = onConfirmReturn,
                    isPlayedTime = timerDisplayMode == TimerDisplayMode.PLAYED,
                    onToggleTimerDisplayMode = onToggleTimerDisplayMode,
                    onKickOff = onKickOff,
                    onToggleTimer = onToggleTimer,
                    onToggleStoppageTimer = onToggleStoppageTimer,
                    onOpenGameMenu = onOpenGameMenu,
                    onNavigateToLogGoal = onNavigateToLogGoal,
                    onNavigateToLogCard = onNavigateToLogCard,
                    onQuickSubstitution = onQuickSubstitution,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
