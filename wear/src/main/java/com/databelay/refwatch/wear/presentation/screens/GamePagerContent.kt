package com.databelay.refwatch.wear.presentation.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.PagerState
import androidx.wear.compose.material.HorizontalPageIndicator
import androidx.wear.compose.material.PageIndicatorState
import com.databelay.refwatch.common.CardType
import com.databelay.refwatch.common.Game
import com.databelay.refwatch.common.GamePhase
import com.databelay.refwatch.common.Team
import com.databelay.refwatch.common.isPlayablePhase
import kotlinx.coroutines.launch
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material3.AnimatedPage
import androidx.wear.compose.material3.HorizontalPagerScaffold
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.PagerScaffoldDefaults
import androidx.wear.compose.material3.VerticalPageIndicator
import androidx.wear.tooling.preview.devices.WearDevices
import com.databelay.refwatch.common.theme.RefWatchWearTheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GamePagerContent(
    // State Props
    game: Game, // Game object, non-null as per the logic snippet
    pagerState: PagerState, // For HorizontalPager
    pageIndicatorState: PageIndicatorState, // For HorizontalPageIndicator

    // Event Lambdas
    onKickOff: () -> Unit,
    onAddGoal: (Team) -> Unit,
    onNavigateToLogCard: (Team, CardType) -> Unit,
    onPenaltyAttemptRecorded: (Boolean) -> Unit,

    modifier: Modifier = Modifier // Standard modifier
) {
    val coroutineScope = rememberCoroutineScope()

    // Determine UI based on game phase
    val isPenaltiesPhase = game.currentPhase == GamePhase.PENALTIES
    val isPlayableRegularPhase = game.currentPhase.isPlayablePhase() && !isPenaltiesPhase

    Box(modifier = modifier) { // Use a Box to potentially overlay the indicator
        when {
            isPenaltiesPhase -> {
                PenaltyShootoutScreen(
                    game = game,
                    onPenaltyAttemptRecorded = onPenaltyAttemptRecorded,
                    modifier = Modifier.fillMaxSize() // Penalty screen takes the whole area
                )
            }

            isPlayableRegularPhase -> {
                // Box to hold the Pager and its Indicator if HorizontalPager is used
                HorizontalPagerScaffold(
                    pagerState = pagerState,
                    pageIndicator = { HorizontalPageIndicator(pageIndicatorState) },

                    ) {
                    HorizontalPager(
                        state = pagerState,
                        flingBehavior = PagerScaffoldDefaults.snapWithSpringFlingBehavior(state = pagerState),
                        modifier = Modifier.fillMaxSize(),
                        // beyondBoundsPageCount = 1 // Consider if needed for performance/preloading
                    ) { page ->
                        when (page) {
                            0 -> AnimatedPage(pageIndex = page, pagerState = pagerState) {
                                TeamActionsPage(
                                    team = Team.HOME,
                                    game = game,
                                    onAddGoal = {
                                        onAddGoal(Team.HOME)
                                        coroutineScope.launch { pagerState.animateScrollToPage(1) }
                                    },
                                    onNavigateToLogCard = onNavigateToLogCard
                                )
                            }
                            1 -> AnimatedPage(pageIndex = page, pagerState = pagerState) {
                                MainGameDisplayScreen(
                                    game = game,
                                    onKickOff = onKickOff
                                    // Modifier.fillMaxSize() is handled by Pager item implicitly
                                )
                            }
                            2 -> AnimatedPage(pageIndex = page, pagerState = pagerState) {
                                TeamActionsPage(
                                    team = Team.AWAY,
                                    game = game,
                                    onAddGoal = {
                                        onAddGoal(Team.AWAY)
                                        coroutineScope.launch { pagerState.animateScrollToPage(1) }
                                    },
                                    onNavigateToLogCard = onNavigateToLogCard
                                )
                            }
                        }
                    }
                }
            }

            else -> {
                // For non-playable, non-penalty phases (e.g., HALF_TIME, GAME_ENDED, NOT_STARTED)
                // Show only the MainGameDisplayScreen
                MainGameDisplayScreen(
                    game = game,
                    onKickOff = onKickOff,
                    modifier = Modifier.fillMaxSize() // MainGameDisplayScreen takes the whole area
                )
            }
        }
        // Note: The "Swipe Up Indicator" Icon is not part of this snippet's logic.
        // If needed, it would be added here, aligned within the outer Box,
        // or handled by a parent composable.
        // The global "Swipe Up Indicator Icon" for settings would also be in this Box,
        // aligned appropriately if it's managed by GameScreenWithPager.
/*        Spacer(modifier = Modifier.height(1.dp)) // Space from top or TimeText
        Icon(
            imageVector = Icons.Filled.KeyboardArrowUp,
            contentDescription = "Swipe up for menu",
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )*/
    }

}

// -------------------------------- Previews -----------------------------------------------
// -----------------------------------------------------------------------------------------

@OptIn(ExperimentalFoundationApi::class)
@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true, name = "GamePagerContent - Playable")
@Composable
fun GamePagerContentPreview_Playable() {
    val sampleGame = Game.defaults().copy(currentPhase = GamePhase.FIRST_HALF)
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 3 })
    val pageIndicatorState = remember {
        object : PageIndicatorState {
            override val pageOffset: Float get() = pagerState.currentPageOffsetFraction
            override val selectedPage: Int get() = pagerState.currentPage
            override val pageCount: Int get() = pagerState.pageCount
        }
    }
    RefWatchWearTheme { // Your app's theme
        GamePagerContent(
            game = sampleGame,
            pagerState = pagerState,
            pageIndicatorState = pageIndicatorState,
            onKickOff = {},
            onAddGoal = {},
            onNavigateToLogCard = { _, _ -> },
            onPenaltyAttemptRecorded = {},
            modifier = Modifier.fillMaxSize()
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true, name = "GamePagerContent - Half Time")
@Composable
fun GamePagerContentPreview_HalfTime() {
    val sampleGame = Game.defaults().copy(currentPhase = GamePhase.HALF_TIME)
    // For non-playable phases, pagerState might be for a single page
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 1 })
     val pageIndicatorState = remember { // Still provide, though indicator won't show for 1 page
        object : PageIndicatorState {
            override val pageOffset: Float get() = pagerState.currentPageOffsetFraction
            override val selectedPage: Int get() = pagerState.currentPage
            override val pageCount: Int get() = pagerState.pageCount
        }
    }
    RefWatchWearTheme {
        GamePagerContent(
            game = sampleGame,
            pagerState = pagerState,
            pageIndicatorState = pageIndicatorState,
            onKickOff = {},
            onAddGoal = {},
            onNavigateToLogCard = { _, _ -> },
            onPenaltyAttemptRecorded = {},
            modifier = Modifier.fillMaxSize()
        )
    }
}
