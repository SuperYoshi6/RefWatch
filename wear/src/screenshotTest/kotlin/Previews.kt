package com.databelay.refwatch.wear

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.ui.tooling.preview.WearPreviewFontScales
import com.android.tools.screenshot.PreviewTest
import com.databelay.refwatch.common.CardType
import com.databelay.refwatch.common.Game
import com.databelay.refwatch.common.GamePhase
import com.databelay.refwatch.common.Team
import com.databelay.refwatch.common.theme.RefWatchWearTheme
import com.databelay.refwatch.wear.presentation.screens.GameScreenWithPager
import com.google.android.horologist.compose.layout.ColumnItemType
import com.google.android.horologist.compose.layout.ColumnItemType.Companion.EdgeButtonPadding
import com.google.android.horologist.compose.layout.rememberResponsiveColumnPadding

// -------------------------------- Previews -----------------------------------------------
// FIXME: screenshot testing doesn't generate screenshots
// FIXME: cannot run gradlew --full-stacktrace wear:updateDebugScreenshotTest
// > Process 'command 'C:\Program Files\Android\Android Studio\jbr\bin\kotlin.exe'' could not be started because the command line exceed operating system limits.
@PreviewTest
@OptIn(ExperimentalFoundationApi::class)
@Preview(
    device = "id:wearos_small_round",
    showSystemUi = true,
    backgroundColor = 0xff000000,
    showBackground = true
)
@Composable
private fun TestPreview() {
    Text("Hello World")
/*
    val sampleGame = Game.Companion.defaults().copy(
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
@Preview(
device = "id:wearos_small_round",
name = "Settings Page Open",
showSystemUi = true,
backgroundColor = 0xff000000,
showBackground = true
)
@Composable
fun GameScreenWithPagerPreviewSettingsOpen() {
val sampleGame = Game.Companion.defaults().copy(currentPhase = GamePhase.FIRST_HALF)
val horizontalPagerState = rememberPagerState(initialPage = 1, pageCount = { 3 })
val verticalPagerState =
    rememberPagerState(initialPage = 1, pageCount = { 2 }) // Start on settings page

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

@Preview(device = "id:wearos_small_round", name = "AddedTime SmRnd", showBackground = true)
@Preview(device = "id:wearos_square", name = "AddedTime Sqr", showBackground = true)
@Preview(
device = "id:wearos_large_round",
name = "AddedTime LrgRnd",
showSystemUi = true,
backgroundColor = 0xff000000,
showBackground = true
)
@WearPreviewFontScales
@Composable
fun Preview_MainGameDisplay_Penalties() {
val sampleGame = Game.Companion.defaults().copy(currentPhase = GamePhase.PENALTIES)
val horizontalPagerState = rememberPagerState(initialPage = 1, pageCount = { 1 })
val verticalPagerState =
    rememberPagerState(initialPage = 0, pageCount = { 2 }) // Start on settings page

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
}*/
}
