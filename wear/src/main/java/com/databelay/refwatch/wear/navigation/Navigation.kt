package com.databelay.refwatch.wear.navigation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Report
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.databelay.refwatch.R
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.AlertDialogDefaults
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.ConfirmationDialog
import androidx.wear.compose.material3.ConfirmationDialogDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.databelay.refwatch.common.CardIssuedEvent
import com.databelay.refwatch.common.CardType
import com.databelay.refwatch.common.Game
import com.databelay.refwatch.common.GamePhase
import com.databelay.refwatch.common.GameStatus
import com.databelay.refwatch.common.Team
import com.databelay.refwatch.common.isPlayablePhase
import com.databelay.refwatch.wear.WearGameViewModel
import com.databelay.refwatch.wear.data.TimerState
import com.databelay.refwatch.wear.presentation.screens.GameListScreen
import com.databelay.refwatch.wear.presentation.screens.GameLogScreen
import com.databelay.refwatch.wear.presentation.screens.GameScreenWithPager
import com.databelay.refwatch.wear.presentation.screens.KickOffSelectionScreen
import com.databelay.refwatch.wear.presentation.screens.LogCardScreen
import com.databelay.refwatch.wear.presentation.screens.LogSubstitutionScreen
import com.databelay.refwatch.wear.presentation.screens.PairingScreen
import com.databelay.refwatch.wear.presentation.screens.WatchLoginScreen
import com.databelay.refwatch.wear.presentation.screens.PreGameSetupRoute
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.delay

const val TAG = "NavigationRoutes"


@Composable
fun NavigationRoutes(isAmbient: Boolean = false) {
    val navController = rememberSwipeDismissableNavController()
    val gameViewModel: WearGameViewModel = hiltViewModel()
    
    // Select ONLY the phase for navigation purposes. This prevents the entire
    // NavHost from recomposing every second when the timer ticks or any other
    // field in the Game object changes.
    val activeGamePhase by remember(gameViewModel) {
        gameViewModel.activeGame.map { it?.currentPhase }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(null)

    val activeGame by gameViewModel.activeGame.collectAsStateWithLifecycle()
    val timerState by gameViewModel.timerDisplayState.collectAsStateWithLifecycle()
    val allGames by gameViewModel.gamesList.collectAsStateWithLifecycle() // Assuming gamesList is the correct source
    val isOnline by gameViewModel.isOnline.collectAsStateWithLifecycle()
    val timerDisplayMode by gameViewModel.timerDisplayMode.collectAsStateWithLifecycle()
    val kickoffCountdownSeconds by gameViewModel.kickoffCountdownSeconds.collectAsStateWithLifecycle()
    val activeDismissals by gameViewModel.activeDismissals.collectAsStateWithLifecycle()
    val pendingReturnConfirmations by gameViewModel.pendingReturnConfirmations.collectAsStateWithLifecycle()
    val context = LocalContext.current // Get the context
    // State to track if the permission has been explicitly denied by the user.
    var showPermissionDeniedDialog by remember { mutableStateOf(false) }
    // SET UP THE PERMISSION LAUNCHER
    // This launcher will receive the result (true/false) from the user's choice.
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                Log.i(TAG, "Notification permission GRANTED by user.")
                showPermissionDeniedDialog = false // Ensure dialog is hidden if they grant it later
                // You can now be confident that the GameTimerService can post notifications.
            } else {
                Log.w(TAG, "Notification permission DENIED by user.")
                showPermissionDeniedDialog = true
                // The user denied the permission. Your app should handle this gracefully.
                // The service's `canPostNotifications()` check will now correctly return false.
            }
        }
    )
    // TRIGGER THE CHECK ONCE WHEN THE APP LAUNCHES
    LaunchedEffect(key1 = true) {
        val activity = context as? android.app.Activity ?: return@LaunchedEffect

        when {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED -> {
            }
                ActivityCompat.shouldShowRequestPermissionRationale(
                    activity, Manifest.permission.POST_NOTIFICATIONS) -> {
                    // In an educational UI, explain to the user why your app requires this
                    // permission for a specific feature to behave as expected, and what
                    // features are disabled if it's declined. In this UI, include a
                    // "cancel" or "no thanks" button that lets the user continue
                    // using your app without granting the permission.
                    showPermissionDeniedDialog = true
                }
                else -> {
                    Log.d(TAG, "Notification permission is not granted. Requesting it now.")
                    // Launch the system dialog to ask the user for permission.
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
    }

    // Intent to open app settings
    val openSettingsIntent = remember {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    // Dialog to show when permission is denied
    if (showPermissionDeniedDialog) {
        AlertDialog(
            visible = true,
            onDismissRequest = { showPermissionDeniedDialog = false }, // Hide dialog on timeout
            icon = {
                Icon(
                    imageVector = Icons.Default.Report,
                    contentDescription = stringResource(R.string.warning_icon),
                    modifier = Modifier.size(ConfirmationDialogDefaults.IconSize)
                )
            },
            title = {
                Text(stringResource(R.string.permission_required))
            },
            text = {
                Text(
                    stringResource(R.string.permission_notif_msg),
                    style = MaterialTheme.typography.bodySmall
                )
            },
            confirmButton = {
                AlertDialogDefaults.ConfirmButton(
                    onClick = {
                        showPermissionDeniedDialog = false
                        context.startActivity(openSettingsIntent) // Go to settings
                    },
                )
            },
            dismissButton = {
                AlertDialogDefaults.DismissButton(
                    onClick = { showPermissionDeniedDialog = false }, // Just dismiss the dialog
                )
            },

        )
    }

    val startDestination = remember(activeGamePhase) {
        activeGamePhase?.let {
            Log.d(
                TAG,
                "Determined start destination based on active game phase: $it"
            )
            mapGamePhaseToRoute(it)
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
                        
                        // AUTO-JOIN: If the game is already in progress, skip setup
                        if (selectedGame.status == GameStatus.IN_PROGRESS) {
                             navController.navigate(WearNavRoutes.GAME_IN_PROGRESS_SCREEN) {
                                popUpTo(WearNavRoutes.GAME_LIST_SCREEN) { inclusive = false }
                                launchSingleTop = true
                            }
                        } else {
                            gameViewModel.activeGame.value?.let { game ->
                                gameViewModel.proceedToNextPhaseManager(game.copy())
                            } ?: Log.w(
                                TAG,
                                "onGameSelected: Cannot proceed, active game is null after selection."
                            )
                            navController.navigate(WearNavRoutes.PRE_GAME_SETUP_SCREEN)
                        }
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
                    },
                    onNavigateToPairing = {
                        navController.navigate(WearNavRoutes.PAIRING_SCREEN)
                    },
                    onNavigateToLogin = {
                        navController.navigate(WearNavRoutes.LOGIN_SCREEN)
                    }
                )
            }
            composable(WearNavRoutes.PAIRING_SCREEN) {
                PairingScreen(
                    onPairingSuccess = {
                        navController.popBackStack(WearNavRoutes.GAME_LIST_SCREEN, false)
                    },
                    onCancel = {
                        navController.popBackStack()
                    }
                )
            }
            composable(WearNavRoutes.LOGIN_SCREEN) {
                WatchLoginScreen(
                    onLoginSuccess = {
                        navController.popBackStack(WearNavRoutes.GAME_LIST_SCREEN, false)
                    },
                    onCancel = {
                        navController.popBackStack()
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
                
                val canToggleStoppageTimer = isPlayableRegularPhase &&
                    (timerState.isTimerRunning || timerState.isStoppageTimerRunning)

                val horizontalPagerState = rememberPagerState(
                    initialPage = 1,
                    pageCount = { if (isPlayableRegularPhase) 3 else 1 })
                val verticalPagerState =
                    rememberPagerState(initialPage = 0, pageCount = { 2 }) // 0: Game, 1: Settings

                if (activeGame != null) {
                    GameScreenWithPager(
                        modifier = Modifier.fillMaxSize(),
                        game = activeGame!!,
                        timerState = timerState,
                        isAmbient = isAmbient,
                        kickoffCountdownSeconds = kickoffCountdownSeconds,
                        activeDismissals = activeDismissals,
                        pendingReturnConfirmations = pendingReturnConfirmations,
                        onConfirmReturn = { gameViewModel.dismissReturnConfirmation(it) },
                        timerDisplayMode = timerDisplayMode,
                        onToggleTimerDisplayMode = { gameViewModel.toggleTimerDisplayMode() },
                        horizontalPagerState = horizontalPagerState,
                        verticalPagerState = verticalPagerState,
                        onKickOff = { gameViewModel.kickOff() },
                        onResetGame = { gameViewModel.resetGame() },
                        onSetToHaveExtraTime = { gameViewModel.setToHaveExtraTime() },
                        onSetToHavePenalties = { gameViewModel.setToHavePenalties() },
                        onToggleTimer = { gameViewModel.toggleTimer() },
                        onToggleStoppageTimer = { 
                            if (canToggleStoppageTimer) {
                                gameViewModel.toggleStoppageTimer()
                            }
                        },
                        onNavigateToLogGoal = { team, goalType ->
                            navController.navigate(WearNavRoutes.logGoalRoute(team, goalType))
                        },
                        onNavigateToLogCard = { team, cardType ->
                            navController.navigate(WearNavRoutes.logCardRoute(team, cardType))
                        },
                        onNavigateToLogSubstitution = { team ->
                            navController.navigate(WearNavRoutes.logSubstitutionRoute(team))
                        },
                        onQuickSubstitution = { team, outgoing, incoming ->
                            gameViewModel.logSubstitution(team, outgoing, incoming)
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
                        onAbortMatch = {
                            gameViewModel.abortGame()
                            navController.navigate(WearNavRoutes.GAME_LIST_SCREEN) {
                                popUpTo(WearNavRoutes.GAME_LIST_SCREEN) { inclusive = true }
                                launchSingleTop = true
                            }
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
                        onPenaltyAttemptRecorded = { scored, kickerNumber ->
                            gameViewModel.recordPenaltyAttempt(scored, kickerNumber)
                        }
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.loading_game_details))
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
                    val roster = if (team == Team.HOME) activeGame?.homeRoster else activeGame?.awayRoster
                    val officials = if (team == Team.HOME) activeGame?.homeOfficials else activeGame?.awayOfficials
                    LogCardScreen(
                        preselectedTeam = team,
                        cardType = cardType,
                        roster = roster ?: emptyList(),
                        officials = officials ?: emptyList(),
                        hasTemporaryDismissals = activeGame?.hasTemporaryDismissals ?: false,
                        temporaryDismissalMinutes = activeGame?.temporaryDismissalMinutes ?: 0,
                        onLogCard = { loggedTeam, playerNum, loggedCardType, applyDismissal, isOfficial, officialName ->
                            var autoRedCardIssued = false

                            // Log the original card (yellow or direct red)
                            gameViewModel.addCard(loggedTeam, playerNum, loggedCardType, applyDismissal, isOfficial, officialName)

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
                                text = stringResource(R.string.second_yellow_auto_red, confirmedRedPlayerNumber ?: 0, team.name),
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
                    Text(stringResource(R.string.error_invalid_nav_args))
                    LaunchedEffect(Unit) {
                        delay(2000)
                        navController.popBackStack()
                    }
                }
            }
            composable(
                route = "${WearNavRoutes.LOG_GOAL_SCREEN}/{${WearNavRoutes.TEAM_ARG}}/{${WearNavRoutes.GOAL_TYPE_ARG}}",
                arguments = listOf(
                    navArgument(WearNavRoutes.TEAM_ARG) { type = NavType.StringType },
                    navArgument(WearNavRoutes.GOAL_TYPE_ARG) { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val teamId = backStackEntry.arguments?.getString(WearNavRoutes.TEAM_ARG)
                val goalTypeString = backStackEntry.arguments?.getString(WearNavRoutes.GOAL_TYPE_ARG)

                val team = teamId?.let { Team.valueOf(it.uppercase()) }
                val goalType = goalTypeString?.let { com.databelay.refwatch.common.GoalType.valueOf(it.uppercase()) }

                if (team != null && goalType != null) {
                    val roster = if (team == Team.HOME) activeGame?.homeRoster else activeGame?.awayRoster
                    com.databelay.refwatch.wear.presentation.screens.LogGoalScreen(
                        preselectedTeam = team,
                        goalType = goalType,
                        roster = roster ?: emptyList(),
                        onLogGoal = { loggedTeam, playerNum, loggedGoalType ->
                            gameViewModel.addGoal(loggedTeam, playerNum, loggedGoalType)
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
                        }
                    )
                }
            }

            composable(
                route = "${WearNavRoutes.LOG_SUBSTITUTION_SCREEN}/{${WearNavRoutes.TEAM_ARG}}",
                arguments = listOf(
                    navArgument(WearNavRoutes.TEAM_ARG) { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val teamId = backStackEntry.arguments?.getString(WearNavRoutes.TEAM_ARG)
                val team = teamId?.let { Team.valueOf(it.uppercase()) }

                if (team != null) {
                    val roster = if (team == Team.HOME) activeGame?.homeRoster else activeGame?.awayRoster
                    LogSubstitutionScreen(
                        team = team,
                        roster = roster ?: emptyList(),
                        onLogSubstitution = { outgoing, incoming ->
                            gameViewModel.logSubstitution(team, outgoing, incoming)
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
                        }
                    )
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
        GamePhase.PENALTIES, GamePhase.GAME_ENDED, GamePhase.ABORTED -> WearNavRoutes.GAME_IN_PROGRESS_SCREEN

        GamePhase.NOT_STARTED -> WearNavRoutes.GAME_LIST_SCREEN
        GamePhase.PRE_GAME -> WearNavRoutes.PRE_GAME_SETUP_SCREEN
        GamePhase.KICK_OFF_SELECTION_FIRST_HALF,
        GamePhase.KICK_OFF_SELECTION_EXTRA_TIME,
        GamePhase.KICK_OFF_SELECTION_PENALTIES -> WearNavRoutes.KICK_OFF_SELECTION_SCREEN
    }
}
