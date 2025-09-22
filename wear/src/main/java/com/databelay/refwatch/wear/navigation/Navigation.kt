package com.databelay.refwatch.wear.navigation

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Announcement
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.databelay.refwatch.common.CardType
import com.databelay.refwatch.common.GamePhase
import com.databelay.refwatch.common.Team
import com.databelay.refwatch.wear.WearGameViewModel
import com.databelay.refwatch.wear.presentation.screens.GameListScreen
import com.databelay.refwatch.wear.presentation.screens.GameLogScreen
import com.databelay.refwatch.wear.presentation.screens.GameScreenWithPager
import com.databelay.refwatch.wear.presentation.screens.KickOffSelectionScreen
import com.databelay.refwatch.wear.presentation.screens.LogCardScreen
import com.databelay.refwatch.wear.presentation.screens.PreGameSetupRoute
import kotlinx.coroutines.delay
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material.dialog.Confirmation
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.ConfirmationDialog
import androidx.wear.compose.material3.ConfirmationDialogDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.SuccessConfirmationDialog
import androidx.wear.compose.material3.confirmationDialogCurvedText
import com.databelay.refwatch.common.CardIssuedEvent
import com.databelay.refwatch.common.Game
import com.databelay.refwatch.common.isPlayablePhase
import com.databelay.refwatch.wear.presentation.screens.ConfirmationDialogInfo
import com.databelay.refwatch.wear.presentation.screens.UnifiedConfirmationDialog
import kotlin.let

const val TAG = "NavigationRoutes"

@Composable
fun NavigationRoutes() {
    val navController = rememberSwipeDismissableNavController()
    val gameViewModel: WearGameViewModel = hiltViewModel()
    val activeGame by gameViewModel.activeGame.collectAsStateWithLifecycle()
    val allGames by gameViewModel.gamesList.collectAsStateWithLifecycle() // Assuming gamesList is the correct source
    val isOnline by gameViewModel.isOnline.collectAsStateWithLifecycle()
    val startDestination = remember(activeGame) {
        activeGame?.let {
            Log.d(
                TAG,
                "Determined start destination based on active game phase: ${activeGame?.currentPhase}"
            )
            mapGamePhaseToRoute(it.currentPhase)
        } ?: WearNavRoutes.GAME_LIST_SCREEN
    }

    AppScaffold(
        timeText = { },
        modifier = Modifier.background(MaterialTheme.colorScheme.background),
    ) {
        SwipeDismissableNavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(8.dp)
        ) {
            composable(WearNavRoutes.GAME_LIST_SCREEN) {
                GameListScreen(
                    allGames = allGames,
                    activeGame = activeGame,
                    isOnline = isOnline,
                    onGameSelected = { selectedGame ->
                        gameViewModel.selectGameToStart(selectedGame)
                        gameViewModel.activeGame.value?.let { game ->
                            gameViewModel.proceedToNextPhaseManager(game.copy())
                        } ?: Log.w(
                            TAG,
                            "onGameSelected: Cannot proceed, active game is null after selection."
                        )
                        // Consider navigating only after activeGame is confirmed non-null or phase has progressed
                        navController.navigate(WearNavRoutes.PRE_GAME_SETUP_SCREEN)
                    },
                    onViewLog = { gameId ->
                        navController.navigate(WearNavRoutes.gameLogRoute(gameId))
                    },
                    onNavigateToNewGame = {
                        gameViewModel.createNewDefaultGame()
                        gameViewModel.activeGame.value?.let { game ->
                            gameViewModel.proceedToNextPhaseManager(game.copy())
                        } ?: Log.w(
                            TAG,
                            "onNavigateToNewGame: Cannot proceed, active game is null after creating new game."
                        )
                        navController.navigate(WearNavRoutes.PRE_GAME_SETUP_SCREEN)
                    }
                )
            }
            composable(WearNavRoutes.PRE_GAME_SETUP_SCREEN) {
                PreGameSetupRoute(
                    navController = navController,
                    gameViewModel = gameViewModel
                )

            }

            composable(WearNavRoutes.KICK_OFF_SELECTION_SCREEN) {
                KickOffSelectionScreen(
                    game = activeGame!!, // Assuming activeGame will not be null here
                    onSetKickOffTeam = { team ->
                        gameViewModel.setKickOffTeam(team)
                        gameViewModel.activeGame.value?.let { game ->
                            gameViewModel.proceedToNextPhaseManager(game.copy())
                        } ?: Log.w(
                            TAG,
                            "KickOffSelectionScreen onConfirm: Cannot proceed, active game is null."
                        )
                        navController.navigate(WearNavRoutes.GAME_IN_PROGRESS_SCREEN) {
                            popUpTo(WearNavRoutes.GAME_LIST_SCREEN) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(WearNavRoutes.GAME_IN_PROGRESS_SCREEN) {
                val isPlayableRegularPhase = activeGame?.currentPhase?.isPlayablePhase() == true &&
                        activeGame?.currentPhase != GamePhase.PENALTIES

                val horizontalPagerState = rememberPagerState(
                    initialPage = 1,
                    pageCount = { if (isPlayableRegularPhase) 3 else 1 })
                val verticalPagerState =
                    rememberPagerState(initialPage = 0, pageCount = { 2 }) // 0: Game, 1: Settings

                if (activeGame != null) {
                    GameScreenWithPager(
                        modifier = Modifier.fillMaxSize(),
                        game = activeGame!!,
                        horizontalPagerState = horizontalPagerState,
                        verticalPagerState = verticalPagerState,
                        onKickOff = { gameViewModel.kickOff() },
                        onResetGame = { gameViewModel.resetGame() },
                        onSetToHaveExtraTime = { gameViewModel.setToHaveExtraTime() },
                        onSetToHavePenalties = { gameViewModel.setToHavePenalties() },
                        onToggleTimer = { gameViewModel.toggleTimer() },
                        onAddGoal = { team -> gameViewModel.addGoal(team) },
                        onNavigateToLogCard = { team, cardType ->
                            navController.navigate(WearNavRoutes.logCardRoute(team, cardType))
                        },
                        onNavigateToGameLog = {
                            activeGame?.let { game ->
                                navController.navigate(WearNavRoutes.gameLogRoute(game.id))
                            } ?: Log.w(TAG, "Cannot navigate to game log, active game is null")
                        },
                        onEndPhase = {
                            gameViewModel.activeGame.value?.let { game ->
                                gameViewModel.proceedToNextPhaseManager(game.copy())
                            } ?: Log.w(
                                TAG,
                                "GameScreenWithPager onEndPhase: Cannot proceed, active game is null."
                            )
                        },
                        onResetPeriodTimer = { gameViewModel.resetTimer() },
                        onConfirmEndMatch = {
                            val gameToEnd = activeGame
                            if (gameToEnd != null) {
                                gameViewModel.finishAndSyncActiveGame(gameToEnd.id)
                                navController.navigate(WearNavRoutes.GAME_LIST_SCREEN) {
                                    popUpTo(WearNavRoutes.GAME_LIST_SCREEN) { inclusive = true }
                                    launchSingleTop = true
                                }
                            } else {
                                Log.w(
                                    TAG,
                                    "onConfirmEndMatch: Cannot finish game, activeGame is null."
                                )
                                navController.popBackStack(WearNavRoutes.GAME_LIST_SCREEN, false)
                            }
                        },
                        onPenaltyAttemptRecorded = { scored ->
                            gameViewModel.recordPenaltyAttempt(scored)
                        }
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Loading Game Details...")
                    }
                }
            }

            composable(
                route = "${WearNavRoutes.LOG_CARD_SCREEN}/{${WearNavRoutes.TEAM_ARG}}/{${WearNavRoutes.CARD_TYPE_ARG}}",
                arguments = listOf(
                    navArgument(WearNavRoutes.TEAM_ARG) { type = NavType.StringType },
                    navArgument(WearNavRoutes.CARD_TYPE_ARG) { type = NavType.StringType }
                )
            )
            { backStackEntry ->

                val teamId = backStackEntry.arguments?.getString(WearNavRoutes.TEAM_ARG)
                val cardTypeString = backStackEntry.arguments?.getString("cardType")

                val team =
                    teamId?.let { Team.valueOf(it.uppercase()) }
                val cardType = cardTypeString?.let { CardType.valueOf(it.uppercase()) }
                var confirmedRedPlayerNumber by remember { mutableStateOf<Int?>(null) }
                var showRedCardConfirmationDialog by remember { mutableStateOf(false) }
                if (team != null && cardType != null) {
                    LogCardScreen(
                        preselectedTeam = team,
                        cardType = cardType,
                        onLogCard = { loggedTeam, playerNum, loggedCardType ->
                            var autoRedCardIssued = false

                            // Log the original card (yellow or direct red)
                            gameViewModel.addCard(loggedTeam, playerNum, loggedCardType)

                            // --- Logic for two yellows leading to a red ---
                            if (loggedCardType == CardType.YELLOW) {
                                val activeGameSnapshot = gameViewModel.activeGame.value
                                if (activeGameSnapshot != null) {
                                    val yellowCardsForPlayer =
                                        activeGameSnapshot.events.filterIsInstance<CardIssuedEvent>()
                                            .count { event ->
                                                event.team == loggedTeam &&
                                                        event.playerNumber == playerNum &&
                                                        event.cardType == CardType.YELLOW
                                            }

                                    if (yellowCardsForPlayer == 2) { // Current yellow makes it the second
                                        // Issue an automatic red card
                                        gameViewModel.addCard(loggedTeam, playerNum, CardType.RED)
                                        // Unified Confirmation Dialog
                                        confirmedRedPlayerNumber = playerNum
                                        showRedCardConfirmationDialog = true
                                        Log.i(
                                            TAG,
                                            "Second yellow for player $playerNum of team $loggedTeam. Auto red card issued."
                                        )
                                    }
                                }
                            }

                            if (!showRedCardConfirmationDialog) { // Navigate only if dialog isn't shown
                                navController.navigate(WearNavRoutes.GAME_IN_PROGRESS_SCREEN) {
                                    popUpTo(WearNavRoutes.GAME_LIST_SCREEN) { inclusive = false }
                                    launchSingleTop = true
                                }
                            }
                        },
                        onCancel = {
                            navController.navigate(WearNavRoutes.GAME_IN_PROGRESS_SCREEN) {
                                popUpTo(WearNavRoutes.GAME_LIST_SCREEN) { inclusive = false }
                                launchSingleTop = true
                            }
                        },
                    )
                    ConfirmationDialog(
                        visible = showRedCardConfirmationDialog,
                        onDismissRequest = {
                            showRedCardConfirmationDialog = false
                            navController.navigate(WearNavRoutes.GAME_IN_PROGRESS_SCREEN) {
                                popUpTo(WearNavRoutes.GAME_LIST_SCREEN) { inclusive = false }
                                launchSingleTop = true
                            }
                        },
                        text = {
                            Text(
                                text = "Second yellow for player $confirmedRedPlayerNumber of team $team. Auto red card issued.",
                                color = MaterialTheme.colorScheme.onError

                            )
                        },
                        colors = ConfirmationDialogDefaults.colors(
                            iconColor = MaterialTheme.colorScheme.onErrorContainer,
                            iconContainerColor = MaterialTheme.colorScheme.primary,
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Report,
                            contentDescription = null,
                            modifier = Modifier.size(ConfirmationDialogDefaults.SmallIconSize),
                        )
                    }
                } else {
                    Text("Error: Invalid navigation arguments for Log Card.")
                    LaunchedEffect(Unit) {
                        delay(2000)
                        navController.popBackStack()
                    }
                }
            }
            composable(
                "${WearNavRoutes.GAME_LOG_SCREEN}/{${WearNavRoutes.GAME_ID_ARG}}",
                arguments = listOf(
                    navArgument(WearNavRoutes.GAME_ID_ARG) {
                        type = NavType.StringType
                    }
                )
            ) { backStackEntry ->
                val currentActiveGameSnapshot = activeGame
                val gameIdString =
                    backStackEntry.arguments?.getString(WearNavRoutes.GAME_ID_ARG)
                var game: Game? = null
                if (currentActiveGameSnapshot != null && currentActiveGameSnapshot.id == gameIdString) {
                    // The requested gameId is the currently active game. Use its state directly.
                    Log.i(
                        TAG,
                        "GameLog: Using current active game data (ID: $gameIdString) from ViewModel. Event count: ${currentActiveGameSnapshot.events.size}"
                    )
                    game = currentActiveGameSnapshot
                } else {
                    game = gameIdString?.let { idToFind ->
                        allGames.find { game -> game.id == idToFind } // Assuming Game has a String id property
                    }
                }
                game?.let { game ->
                    GameLogScreen(
                        game = game,
                        onDismiss = { navController.popBackStack() },
                        onRemoveEvent = { event ->
                            gameViewModel.removeEvent(event, gameIdString)
                        },
                    )
                }
            }
        }
    }
}


fun mapGamePhaseToRoute(phase: GamePhase): String {
    return when (phase) {
        GamePhase.FIRST_HALF, GamePhase.HALF_TIME, GamePhase.SECOND_HALF,
        GamePhase.EXTRA_TIME_FIRST_HALF, GamePhase.EXTRA_TIME_HALF_TIME, GamePhase.EXTRA_TIME_SECOND_HALF,
        GamePhase.PENALTIES, GamePhase.GAME_ENDED -> WearNavRoutes.GAME_IN_PROGRESS_SCREEN

        GamePhase.NOT_STARTED -> WearNavRoutes.GAME_LIST_SCREEN
        GamePhase.PRE_GAME -> WearNavRoutes.PRE_GAME_SETUP_SCREEN
        GamePhase.KICK_OFF_SELECTION_FIRST_HALF,
        GamePhase.KICK_OFF_SELECTION_EXTRA_TIME,
        GamePhase.KICK_OFF_SELECTION_PENALTIES -> WearNavRoutes.KICK_OFF_SELECTION_SCREEN
    }
}