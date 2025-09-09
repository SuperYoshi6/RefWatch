package com.databelay.refwatch.wear.navigation

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.databelay.refwatch.common.isPlayablePhase
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
            mapGamePhaseToRoute(it.currentPhase)
        } ?: WearNavRoutes.GAME_LIST_SCREEN
    }

    Scaffold(
        modifier = Modifier.background(MaterialTheme.colorScheme.background),
    ) {
        SwipeDismissableNavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(it)
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
                            gameViewModel.recordPenaltyAttempt(
                                scored
                            )
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

                if (team != null && cardType != null) {
                    LogCardScreen(
                        preselectedTeam = team,
                        cardType = cardType,
                        onLogCard = { loggedTeam, playerNum, loggedCardType ->
                            gameViewModel.addCard(team, playerNum, cardType) // Use arguments from closure
                            navController.navigate(WearNavRoutes.GAME_IN_PROGRESS_SCREEN) {
                                popUpTo(WearNavRoutes.GAME_LIST_SCREEN) { inclusive = false }
                                launchSingleTop = true
                            }
                        },
                        onCancel = {
                            navController.navigate(WearNavRoutes.GAME_IN_PROGRESS_SCREEN) {
                                popUpTo(WearNavRoutes.GAME_LIST_SCREEN) { inclusive = false }
                                launchSingleTop = true
                            }
                        },
                    )
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
                val gameIdString = backStackEntry.arguments?.getString(WearNavRoutes.GAME_ID_ARG)
                val gameForLog = gameIdString?.let { idToFind ->
                    allGames.find { game -> game.id == idToFind } // Assuming Game has a String id property
                }
                GameLogScreen(
                    game = gameForLog!!,
                    onDismiss = { navController.popBackStack() },
                    onUndoEvent = { event ->
                        gameViewModel.undoEvent(event) },
                )
            }
        }
    }
}

@SuppressLint("RestrictedApi")
fun logBackStack(navController: NavController, contextMessage: String = "") {
    val stack = navController.currentBackStack.value
    val currentNavControllerDestination = navController.currentDestination
    val currentNavControllerRoute = currentNavControllerDestination?.route
    val currentNavControllerId = currentNavControllerDestination?.id
    val currentNavControllerClass =
        currentNavControllerDestination?.displayName

    Log.d("${TAG}:stack", "---- NavController Back Stack ($contextMessage) ----")
    Log.d(
        "${TAG}:stack",
        "NavController Current Destination: Route='${currentNavControllerRoute ?: "null"}', ID='${currentNavControllerId ?: "null"}', Class='${currentNavControllerClass ?: "null"}'"
    )

    if (stack.isEmpty()) {
        Log.d("${TAG}:stack", "Back stack is empty.")
    } else {
        stack.forEachIndexed { index, navBackStackEntry ->
            val entryDestination = navBackStackEntry.destination
            val route = entryDestination.route
            val arguments = navBackStackEntry.arguments?.toString() ?: "null"
            val destDisplayName = entryDestination.displayName

            Log.d(
                "${TAG}:stack",
                "$index: Route='${route ?: "null"}', Args=[$arguments], ID='${navBackStackEntry.id}', NavDestId='${entryDestination.id}', NavDestClass='${destDisplayName}'"
            )
        }
    }
    Log.d("${TAG}:stack", "------------------------------------------")
}

fun mapGamePhaseToRoute(phase: GamePhase): String {
    return when (phase) {
        GamePhase.FIRST_HALF, GamePhase.HALF_TIME, GamePhase.SECOND_HALF,
        GamePhase.EXTRA_TIME_FIRST_HALF, GamePhase.EXTRA_TIME_HALF_TIME, GamePhase.EXTRA_TIME_SECOND_HALF,
        GamePhase.PENALTIES, GamePhase.GAME_ENDED -> WearNavRoutes.GAME_IN_PROGRESS_SCREEN

        GamePhase.ABANDONED, GamePhase.NOT_STARTED -> WearNavRoutes.GAME_LIST_SCREEN
        GamePhase.PRE_GAME -> WearNavRoutes.PRE_GAME_SETUP_SCREEN
        GamePhase.KICK_OFF_SELECTION_FIRST_HALF,
        GamePhase.KICK_OFF_SELECTION_EXTRA_TIME,
        GamePhase.KICK_OFF_SELECTION_PENALTIES -> WearNavRoutes.KICK_OFF_SELECTION_SCREEN
    }
}
