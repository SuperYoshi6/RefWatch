package com.databelay.refwatch.wear.presentation.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.PagerState
import androidx.wear.compose.material.HorizontalPageIndicator
import androidx.wear.compose.material.PageIndicatorState
import com.databelay.refwatch.common.CardType
import com.databelay.refwatch.common.GamePhase
import com.databelay.refwatch.wear.presentation.screens.PenaltyShootoutScreen
import com.databelay.refwatch.common.Team
import com.databelay.refwatch.common.isPlayablePhase
import kotlinx.coroutines.launch
import androidx.wear.compose.material3.AnimatedPage
import androidx.wear.compose.material3.HorizontalPagerScaffold
import androidx.wear.compose.material3.PagerScaffoldDefaults
import com.databelay.refwatch.common.GoalType
import com.databelay.refwatch.wear.TimerDisplayMode

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GamePagerContent(
    game: Game,
    isAmbient: Boolean = false,
    kickoffCountdownSeconds: Int? = null,
    timerDisplayMode: TimerDisplayMode = TimerDisplayMode.REMAINING,
    pagerState: PagerState,
    pageIndicatorState: PageIndicatorState,

    onKickOff: () -> Unit,
    onToggleTimerDisplayMode: () -> Unit = {},
    onNavigateToLogGoal: (Team, GoalType) -> Unit,
    onNavigateToLogCard: (Team, CardType) -> Unit,
    onNavigateToLogSubstitution: (Team) -> Unit,
    onPenaltyAttemptRecorded: (Boolean) -> Unit,
    onToggleTimer: () -> Unit,
    onToggleStoppageTimer: () -> Unit = {},
    onOpenGameMenu: () -> Unit = {},
    onQuickGoal: (Team) -> Unit = {},

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
                                    onNavigateToLogSubstitution = onNavigateToLogSubstitution
                                )
                            }
                            1 -> AnimatedPage(pageIndex = page, pagerState = pagerState) {
                                MainGameDisplayScreen(
                                    game = game,
                                    isAmbient = isAmbient,
                                    kickoffCountdownSeconds = kickoffCountdownSeconds,
                                    isPlayedTime = timerDisplayMode == TimerDisplayMode.PLAYED,
                                    onToggleTimerDisplayMode = onToggleTimerDisplayMode,
                                    onKickOff = onKickOff,
                                    onToggleTimer = onToggleTimer,
                                    onToggleStoppageTimer = onToggleStoppageTimer,
                                    onOpenGameMenu = onOpenGameMenu,
                                    onNavigateToLogGoal = onNavigateToLogGoal,
                                    onNavigateToLogCard = onNavigateToLogCard,
                                    onQuickGoal = onQuickGoal
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
                                    onNavigateToLogSubstitution = onNavigateToLogSubstitution
                                )
                            }
                        }
                    }
                }
            }

            else -> {
                MainGameDisplayScreen(
                    game = game,
                    isAmbient = isAmbient,
                    kickoffCountdownSeconds = kickoffCountdownSeconds,
                    isPlayedTime = timerDisplayMode == TimerDisplayMode.PLAYED,
                    onToggleTimerDisplayMode = onToggleTimerDisplayMode,
                    onKickOff = onKickOff,
                    onToggleTimer = onToggleTimer,
                    onToggleStoppageTimer = onToggleStoppageTimer,
                    onOpenGameMenu = onOpenGameMenu,
                    onNavigateToLogGoal = onNavigateToLogGoal,
                    onNavigateToLogCard = onNavigateToLogCard,
                    onQuickGoal = onQuickGoal,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
