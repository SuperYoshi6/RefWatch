package com.databelay.refwatch.wear // Your Wear OS package

import android.Manifest
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.size
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.databelay.refwatch.common.AppJsonConfiguration
import com.databelay.refwatch.common.CardIssuedEvent
import com.databelay.refwatch.common.CardType
import com.databelay.refwatch.common.Game
import com.databelay.refwatch.common.GamePhase
import com.databelay.refwatch.common.GameStatus
import com.databelay.refwatch.common.GenericLogEvent
import com.databelay.refwatch.common.GoalScoredEvent
import com.databelay.refwatch.common.Team
import com.databelay.refwatch.common.formatTime
import com.databelay.refwatch.common.hasTimer
import com.databelay.refwatch.common.isBreak
import com.databelay.refwatch.common.isPlayablePhase
import com.databelay.refwatch.common.opposite
import com.databelay.refwatch.common.readable
import com.databelay.refwatch.common.toSnapshotForStorage
import com.databelay.refwatch.wear.data.DataFetchStatus
import com.databelay.refwatch.wear.data.GameStorageWear
import com.databelay.refwatch.wear.data.GameTimerService
import com.databelay.refwatch.wear.util.ConnectivityObserver // For network status
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map // For mapping network status
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import javax.inject.Inject

interface IWearGameViewModel {
    val gamesList: StateFlow<List<Game>>
    val dataFetchStatus: StateFlow<DataFetchStatus>
    val isOnline: StateFlow<Boolean>
    val activeGame: StateFlow<Game?> // <<<< CHANGE TO NULLABLE HERE
}

@OptIn(FlowPreview::class)
@HiltViewModel
class WearGameViewModel @Inject constructor(
    @ApplicationContext applicationContext: Context, // Renamed for clarity
    private val savedStateHandle: SavedStateHandle,
    private val gameStorage: GameStorageWear,
    private val vibrator: Vibrator?
) : AndroidViewModel(applicationContext as Application), IWearGameViewModel {
    private val tag = "WearGameViewModel" // Renamed for uniqueness from class name

    override val gamesList: StateFlow<List<Game>> = gameStorage.gamesListFlow
        .onEach { list -> // DEBUG LOGGING
            Log.d(tag, "gamesList updated. Total games: ${list.size}")
            list.forEach { game ->
                Log.d(tag, "Game in gamesList - ID: ${game.id}, Status: ${game.status}, Score: ${game.homeScore}-${game.awayScore}, Events: ${game.events.size}")
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()) // Ensure initialValue and proper stateIn usage

    override val dataFetchStatus: StateFlow<DataFetchStatus> = gameStorage.dataFetchStatusFlow

    override val isOnline: StateFlow<Boolean> = gameStorage.networkStatusFlow.map {
        it == ConnectivityObserver.Status.AVAILABLE
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Initialize _activeGame as potentially null or with a loading state initially
    private val _activeGame = MutableStateFlow<Game?>(null) // Start as null
    override val activeGame: StateFlow<Game?> = _activeGame.asStateFlow() // Expose as nullable


    private var isCurrentGameSessionActive = false
    private var gameTimerService: GameTimerService? = null
    private var isServiceBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(tag, "GameTimerService connected")
            val binder = service as GameTimerService.LocalBinder
            gameTimerService = binder.getService()
            isServiceBound = true

            gameTimerService?.timerStateFlow
                ?.onEach { serviceState ->
                    _activeGame.value?.let { currentNonNullGame -> // Use .value and safe call + let
                        // --- Inside this 'let' block, currentNonNullGame is non-null ---
                        if (!currentNonNullGame.inAddedTime && serviceState.inAddedTime) {
                            Log.i(tag, "Added time is now ACTIVE via TimerService. Starting reminder vibration.")
                            vibrate(VibrationPattern.ADDED_TIME_REMINDER)
                        }
                        if (currentNonNullGame.inAddedTime && !serviceState.inAddedTime) {
                            Log.i(tag, "Added time is now INACTIVE via TimerService. Stopping reminder vibration.")
                            vibrator?.cancel()
                        }
                        _activeGame.update { currentGame -> // currentGame here is Game?
                            currentGame?.copy( // Safe call for copy
                                isTimerRunning = serviceState.isTimerRunning,
                                displayedTimeMillis = serviceState.displayedMillis,
                                actualTimeElapsedInPeriodMillis = serviceState.actualTimeElapsedInPeriodMillis,
                                inAddedTime = serviceState.inAddedTime,
                            )
                        }
                    }
                }
                ?.launchIn(viewModelScope)

            val currentActiveGameOnConnect = _activeGame.value // This is Game?
            // Check if game exists AND is IN_PROGRESS
            currentActiveGameOnConnect?.let { gameToConfigure ->
                if (gameToConfigure.status == GameStatus.IN_PROGRESS) {
                    Log.d(tag, "Service connected. Current game phase: ${gameToConfigure.currentPhase}, timer running: ${gameToConfigure.isTimerRunning}")
                    gameTimerService?.configureTimerForGame(
                        game = gameToConfigure,
                        startImmediately = gameToConfigure.isTimerRunning
                    )
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.w(tag, "GameTimerService disconnected")
            gameTimerService = null
            isServiceBound = false
        }
    }

    init {
        Log.d(tag, "WearGameViewModel initializing.")

        viewModelScope.launch {
            val initialGames = gameStorage.gamesListFlow
                .filter { it.isNotEmpty() }
                .stateIn(viewModelScope)
                .first()

            val loadedGame = loadInitialActiveGameInternal(initialGames)
            _activeGame.value = loadedGame

            loadedGame?.let { game -> // Ensure loadedGame is not null before using it
                _activeGame.update { current ->
                    current?.copy(
                        displayedTimeMillis = calculateInitialDisplayTime(game) // game here is non-null from loadedGame.let
                    )
                }
            }
        }

        // Collector 1: For SavedStateHandle (fairly frequent)
        _activeGame.filterNotNull()
            // .debounce(100L) // Optional: very short debounce if even this is too much
            .onEach { game ->
                if (game.status != GameStatus.COMPLETED) {
                    saveActiveGameStateToHandle()
                    // Log.d(tag, "Game ${game.id} saved to SavedStateHandle.") // Can be verbose
                }
            }.launchIn(viewModelScope)

        // Collector 2: For gameStorage (more selective and debounced)

        _activeGame.filterNotNull()
            .map { game -> game.toSnapshotForStorage() } // Use the extension function here
            .distinctUntilChanged() // Only emit if the *significant* snapshot changes
            .debounce(750L) // Debounce for 750ms to group rapid significant changes
            .onEach { snapshot ->
                // Now that a significant, debounced change has occurred,
                // get the *latest* full Game object to save all fields including recent timer values.
                val latestGameToSave = _activeGame.value
                if (latestGameToSave != null && latestGameToSave.id == snapshot.id && latestGameToSave.status != GameStatus.COMPLETED) {
                    viewModelScope.launch {
                        Log.i(tag, "Significant change for game ${snapshot.id}. Persisting to gameStorage.")
                        gameStorage.addOrUpdateGame(latestGameToSave)
                    }
                } else if (latestGameToSave == null || latestGameToSave.id != snapshot.id) {
                    Log.w(tag, "Snapshot changed for ${snapshot.id}, but current active game is now different (${latestGameToSave?.id}) or null. Skipping save to gameStorage.")
                }
            }.launchIn(viewModelScope)


        isOnline.onEach { online ->
            Log.i(tag, "Network status in ViewModel: ${if (online) "Online" else "Offline"}")
        }.launchIn(viewModelScope)

        Intent(getApplication(), GameTimerService::class.java).also { intent ->
            try {
                ContextCompat.startForegroundService(getApplication(), intent)
                Log.d(tag, "Requested to start GameTimerService.")
            } catch (e: Exception) {
                Log.e(tag, "Failed to start GameTimerService", e)
            }
        }
        bindToGameTimerService()
    }


    val allGamesMap: StateFlow<Map<String, Game>> =
        kotlinx.coroutines.flow.combine(gamesList, _activeGame) { scheduled, active ->
            val gameMap = scheduled.associateBy { it.id }.toMutableMap()
            active?.let { gameMap[it.id] = it }
            gameMap.toMap()
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    private fun calculateInitialDisplayTime(game: Game): Long { // game parameter is non-null
        var initialDisplayTime = game.regulationPeriodDurationMillis()
        if (!game.isTimerRunning && game.currentPhase.hasTimer()) {
            val regulationDuration = game.regulationPeriodDurationMillis()
            initialDisplayTime = if (game.actualTimeElapsedInPeriodMillis >= regulationDuration) {
                game.actualTimeElapsedInPeriodMillis - regulationDuration
            } else {
                regulationDuration - game.actualTimeElapsedInPeriodMillis
            }
        }
        return initialDisplayTime
    }

    private fun bindToGameTimerService() {
        if (!isServiceBound && gameTimerService == null) {
            Intent(getApplication(), GameTimerService::class.java).also { intent ->
                getApplication<Application>().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            }
        }
    }

    private fun unbindFromGameTimerService() {
        if (isServiceBound) {
            getApplication<Application>().unbindService(serviceConnection)
            isServiceBound = false
            gameTimerService = null
            Log.d(tag, "Unbound from GameTimerService.")
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(tag, "WearGameViewModel onCleared")
        unbindFromGameTimerService()
    }

    private fun loadInitialActiveGameInternal(currentGames: List<Game>): Game {
        val savedGameJson: String? = savedStateHandle.get("activeGameJson")
        savedGameJson?.let { json ->
            try {
                val gameFromState = AppJsonConfiguration.decodeFromString<Game>(json)
                Log.d(tag, "Loaded active game from SavedStateHandle: ${gameFromState.id}")
                return gameFromState
            } catch (e: Exception) {
                Log.e(tag, "Error decoding game from SavedStateHandle", e)
            }
        }

        Log.d(tag, "No active game in SavedStateHandle, trying from provided scheduled games.")
        val firstScheduledGame = currentGames.firstOrNull { it.status == GameStatus.SCHEDULED }

        return firstScheduledGame?.copy(
            currentPhase = GamePhase.NOT_STARTED,
            homeScore = 0, awayScore = 0, events = emptyList()
        ) ?: Game().also {
            Log.e(tag, "LOAD_INITIAL_ACTIVE_GAME_INTERNAL: No game from state or scheduled. Using new default. ID: ${it.id}")
        }
    }

    private fun saveActiveGameStateToHandle() {
        _activeGame.value?.let { game ->
            try {
                val activeGameJson = AppJsonConfiguration.encodeToString(game)
                savedStateHandle["activeGameJson"] = activeGameJson
//                Log.d(tag, "Active game state saved to SavedStateHandle. ID: ${game.id}")
            } catch (e: Exception) {
                Log.e(tag, "Error saving active game state to JSON for SavedStateHandle", e)
            }
        }
    }

    fun createNewDefaultGame() {
        cancelTimer()
        val newDefaultGame = Game(gameDateTimeEpochMillis = System.currentTimeMillis())
        _activeGame.value = newDefaultGame.copy(
            displayedTimeMillis = newDefaultGame.regulationPeriodDurationMillis(GamePhase.FIRST_HALF),
            status = GameStatus.IN_PROGRESS
        )
        Log.d(tag, "New default game created with ID: ${newDefaultGame.id}. Setting as active.")
    }

    fun selectGameToStart(gameFromList: Game) {
        cancelTimer()
        Log.d(tag, "Selecting game from list to start: ${gameFromList.id}")
        val cleanGameForStart = gameFromList.copy(
            currentPhase = GamePhase.NOT_STARTED,
            homeScore = 0,
            awayScore = 0,
            displayedTimeMillis = gameFromList.regulationPeriodDurationMillis(GamePhase.FIRST_HALF),
            actualTimeElapsedInPeriodMillis = 0L,
            isTimerRunning = false,
            events = emptyList(),
            status = GameStatus.IN_PROGRESS,
            lastUpdated = System.currentTimeMillis()
        )
        _activeGame.value = cleanGameForStart
        Log.d(tag, "Selected game ${cleanGameForStart.id} set as active.")
    }

    fun finishAndSyncActiveGame(onSyncComplete: () -> Unit) {
        val finishedEvent = GenericLogEvent(message = "Finished game")
        _activeGame.update { game -> game?.copy(events = game.events + finishedEvent) }

        _activeGame.value?.let { currentGame ->
            Log.d(tag, "finishAndSyncActiveGame called for game: ${currentGame.id}")
            cancelTimer()
            val finalGame = currentGame.copy(
                currentPhase = GamePhase.GAME_ENDED,
                status = GameStatus.COMPLETED,
                isTimerRunning = false,
                lastUpdated = System.currentTimeMillis()
            )
            _activeGame.value = finalGame
            Log.d(tag, "Game ${finalGame.id} marked as COMPLETED in ViewModel.")

            viewModelScope.launch {
                Log.i(tag, "finishAndSyncActiveGame: Explicitly saving COMPLETED game ${finalGame.id} to gameStorage. Events: ${finalGame.events.size}")
                val result = gameStorage.addOrUpdateGame(finalGame) // <<<< CRITICAL CALL
                if (result.isSuccess) {
                    Log.i(tag, "finishAndSyncActiveGame: Successfully initiated save for COMPLETED game ${finalGame.id}.")
                } else {
                    Log.e(tag, "finishAndSyncActiveGame: Failed to initiate save for COMPLETED game ${finalGame.id}. Error: ${result.exceptionOrNull()?.message}")
                }

                Log.i(tag, "Game ${finalGame.id} processing for completion finished. Resetting UI.")
                resetActiveGameToDefaultOrNextScheduled()
                onSyncComplete()
            }
        } ?: run {
            Log.w(tag, "finishAndSyncActiveGame called but no active game.")
            onSyncComplete() // Still call callback to not block UI flows
        }
    }

    @Deprecated("GameStorageWear handles sync automatically. This function may be removed.")
    fun attemptSyncPendingGames() {
        Log.w(tag, "attemptSyncPendingGames called, but this is now primarily handled by GameStorageWear.")
    }

    fun resetActiveGameToDefaultOrNextScheduled() {
        cancelTimer()
        val activeGameIdBeforeReset = _activeGame.value?.id // Mostly for logging or comparison

        val nextScheduledGame = gamesList.value
            .filter { it.status == GameStatus.SCHEDULED && it.id != (activeGameIdBeforeReset ?: "") }
            .minByOrNull { it.gameDateTimeEpochMillis ?: Long.MAX_VALUE }

        val newActiveState: Game? // Explicitly nullable

        if (nextScheduledGame != null) {
            newActiveState = nextScheduledGame.copy(
                currentPhase = GamePhase.NOT_STARTED, // Or PRE_GAME
                homeScore = 0,
                awayScore = 0,
                events = emptyList(),
                status = GameStatus.SCHEDULED, // It's scheduled, not yet in progress
                isTimerRunning = false,
                actualTimeElapsedInPeriodMillis = 0L,
                displayedTimeMillis = nextScheduledGame.regulationPeriodDurationMillis(GamePhase.FIRST_HALF) // Or NOT_STARTED phase default
            )
            Log.d(tag, "Resetting active game to next scheduled: ${newActiveState.id}")
        } else {
            // No next scheduled game. Should we create a default one or set to null?
            // Option 1: Set to null (Recommended for clarity)
            newActiveState = null
            Log.d(tag, "No next scheduled game. Resetting active game to null.")

            // Option 2: Create a new blank/default game (Your current approach)
            /*
            newActiveState = Game().let { defaultGame ->
                defaultGame.copy(
                    displayedTimeMillis = defaultGame.regulationPeriodDurationMillis(GamePhase.FIRST_HALF),
                    status = GameStatus.SCHEDULED // Or some other initial status
                )
            }
            Log.d(tag, "No next scheduled game. Creating a new default game: ${newActiveState.id}")
            */
        }
        _activeGame.value = newActiveState
    }


    fun toggleTimer() {
        val currentGame = _activeGame.value ?: return
        val currentPhase = currentGame.currentPhase
        vibrate(VibrationPattern.GENERIC_EVENT)

        if (!currentPhase.hasTimer()) {
            Log.w(tag, "toggleTimer called in a non-timed phase: ${currentPhase.readable()}. No action.")
            return
        }

        if (currentGame.status == GameStatus.IN_PROGRESS && !currentGame.isTimerRunning && !isCurrentGameSessionActive) {
            Log.i(tag, "toggleTimer: Starting NEW GAME SESSION for phase ${currentPhase.readable()}.")
            gameTimerService?.commandStartGameSessionAndTimer(currentGame)
            isCurrentGameSessionActive = true
        }

        if (currentGame.isTimerRunning) {
            gameTimerService?.pauseGameTimer(updateNotificationText = "Paused: ${currentPhase.readable()}")
            Log.d(tag, "Timer PAUSED for ${currentPhase.readable()}.")
        } else {
            if (currentGame.status != GameStatus.IN_PROGRESS) {
                Log.w(tag, "Attempted to start timer for a game not in progress: ${currentPhase.readable()}.")
                return
            }
            gameTimerService?.resumeGameTimer(currentGame)
            Log.d(tag, "Timer RESUMED for ${currentPhase.readable()}.")
            if (!isCurrentGameSessionActive) { // Fallback, should have been set above
                gameTimerService?.commandStartGameSessionAndTimer(currentGame)
                isCurrentGameSessionActive = true
            }
        }
        if (currentGame.status == GameStatus.SCHEDULED && !currentGame.isTimerRunning) {
            _activeGame.update { it?.copy(status = GameStatus.IN_PROGRESS) }
        }
    }

    fun cancelTimer() {
        Log.d(tag, "cancelTimer called in ViewModel.")
        gameTimerService?.commandStopGameSessionAndCleanup()
        vibrator?.cancel()
        isCurrentGameSessionActive = false
        _activeGame.update {
            it?.copy(isTimerRunning = false)
        }
    }

    fun proceedToNextPhaseManager(gameAtPeriodEndInput: Game) {
        var gameAtPeriodEnd = gameAtPeriodEndInput.copy(isTimerRunning = false)

        if (gameAtPeriodEnd.currentPhase.isPlayablePhase()) {
            val regulationDur = gameAtPeriodEnd.regulationPeriodDurationMillis()
            if (gameAtPeriodEnd.actualTimeElapsedInPeriodMillis > regulationDur) {
                val addedTimePlayed = gameAtPeriodEnd.actualTimeElapsedInPeriodMillis - regulationDur
                Log.i(tag, "Period ${gameAtPeriodEnd.currentPhase} ended. Added time played: ${addedTimePlayed.formatTime()}")
            }
        }

        val lastPhaseKickOffTeam = gameAtPeriodEnd.kickOffTeam
        var nextPhase: GamePhase = when (gameAtPeriodEnd.currentPhase) {
            GamePhase.NOT_STARTED -> GamePhase.PRE_GAME
            GamePhase.PRE_GAME -> GamePhase.KICK_OFF_SELECTION_FIRST_HALF
            GamePhase.KICK_OFF_SELECTION_FIRST_HALF -> GamePhase.FIRST_HALF
            GamePhase.FIRST_HALF -> GamePhase.HALF_TIME
            GamePhase.HALF_TIME -> GamePhase.SECOND_HALF
            GamePhase.SECOND_HALF -> if (gameAtPeriodEnd.hasExtraTime) GamePhase.KICK_OFF_SELECTION_EXTRA_TIME else GamePhase.GAME_ENDED
            GamePhase.KICK_OFF_SELECTION_EXTRA_TIME -> GamePhase.EXTRA_TIME_FIRST_HALF
            GamePhase.EXTRA_TIME_FIRST_HALF -> GamePhase.EXTRA_TIME_HALF_TIME
            GamePhase.EXTRA_TIME_HALF_TIME -> GamePhase.EXTRA_TIME_SECOND_HALF
            GamePhase.EXTRA_TIME_SECOND_HALF -> if (gameAtPeriodEnd.homeScore == gameAtPeriodEnd.awayScore && gameAtPeriodEnd.hasPenalties) GamePhase.KICK_OFF_SELECTION_PENALTIES else GamePhase.GAME_ENDED
            GamePhase.KICK_OFF_SELECTION_PENALTIES -> GamePhase.PENALTIES
            GamePhase.PENALTIES -> GamePhase.GAME_ENDED
            else -> gameAtPeriodEnd.currentPhase
        }

        val newKickOffTeam = when (nextPhase) {
            GamePhase.FIRST_HALF, GamePhase.EXTRA_TIME_FIRST_HALF, GamePhase.PENALTIES -> lastPhaseKickOffTeam
            GamePhase.SECOND_HALF, GamePhase.EXTRA_TIME_SECOND_HALF -> lastPhaseKickOffTeam.opposite()
            else -> lastPhaseKickOffTeam
        }

        val updatedGame = gameAtPeriodEnd.copy(
            currentPhase = nextPhase,
            actualTimeElapsedInPeriodMillis = 0L,
            displayedTimeMillis = gameAtPeriodEnd.regulationPeriodDurationMillis(nextPhase),
            status = if (nextPhase == GamePhase.GAME_ENDED) GameStatus.COMPLETED else GameStatus.IN_PROGRESS,
            kickOffTeam = newKickOffTeam,
            lastUpdated = System.currentTimeMillis()
        )

        _activeGame.value = updatedGame
        Log.i(tag, "Phase ${gameAtPeriodEnd.currentPhase} ended. New phase: ${updatedGame.currentPhase}. Kick-off: ${updatedGame.kickOffTeam}")

        if (updatedGame.currentPhase == GamePhase.GAME_ENDED) {
            gameTimerService?.commandStopGameSessionAndCleanup()
            isCurrentGameSessionActive = false
        } else if (updatedGame.currentPhase.needsKickOffSelection()) {
            gameTimerService?.configureTimerForGame(game = updatedGame, startImmediately = false)
        } else {
             gameTimerService?.configureTimerForGame(
                game = updatedGame,
                startImmediately = updatedGame.currentPhase.isBreak()
            )
        }
    }


    fun setKickOffTeam(team: Team) {
        _activeGame.update {
            it?.copy(
                kickOffTeam = team,
                lastUpdated = System.currentTimeMillis()
            )
        }
        Log.d(tag, "Kick-off team for current context set to $team")
    }

    // FIXME: don't run timer service when no game in on

    // FIXME: Buzzing only once on half end but ok on halftime end

    fun kickOff() {
        val currentGame = _activeGame.value ?: return
        val currentPhase = currentGame.currentPhase

        if (currentPhase.needsKickOff()) {
            val teamName = if (currentGame.kickOffTeam == Team.HOME) currentGame.homeTeamName else currentGame.awayTeamName
            val kickOffMessage = "Kick Off - ${teamName} - ${currentPhase.readable()}"
            val kickOffEvent = GenericLogEvent(message = kickOffMessage)
            Log.i(tag, kickOffMessage)
            _activeGame.update { game -> game?.copy(events = game.events + kickOffEvent) }
            gameTimerService?.startGameTimer(currentGame) // Pass nullable game
        } else {
            Log.w(tag, "KickOff action attempted in inappropriate phase: $currentPhase")
        }
    }


    fun setToHaveExtraTime() {
        _activeGame.update { it?.copy(hasExtraTime = true, lastUpdated = System.currentTimeMillis()) }
    }

    fun setToHavePenalties() {
        _activeGame.update { it?.copy(hasPenalties = true, lastUpdated = System.currentTimeMillis()) }
    }

    fun addGoal(team: Team) {
        val currentGame = _activeGame.value ?: return
        if (!currentGame.currentPhase.isPlayablePhase()) return

        val newHomeScore = if (team == Team.HOME) currentGame.homeScore + 1 else currentGame.homeScore
        val newAwayScore = if (team == Team.AWAY) currentGame.awayScore + 1 else currentGame.awayScore
        val goalEvent = GoalScoredEvent(
            team = team,
            gameTimeMillis = currentGame.actualTimeElapsedInPeriodMillis.toDouble(),
            homeScoreAtTime = newHomeScore,
            awayScoreAtTime = newAwayScore
        )
        _activeGame.update {
            it?.copy(
                homeScore = newHomeScore,
                awayScore = newAwayScore,
                events = it.events + goalEvent,
                lastUpdated = System.currentTimeMillis()
            )
        }
        vibrate(VibrationPattern.GOAL_SCORED)
        Log.d(tag, "Goal added for $team. Score: ${_activeGame.value?.homeScore}-${_activeGame.value?.awayScore}")
    }


    fun addCard(team: Team, playerNumber: Int, cardType: CardType) {
        val currentGame = _activeGame.value ?: return
        if (!currentGame.currentPhase.isPlayablePhase()) return

        val cardEvent = CardIssuedEvent(
            team = team,
            playerNumber = playerNumber,
            cardType = cardType,
            gameTimeMillis = currentGame.actualTimeElapsedInPeriodMillis.toDouble()
        )
        _activeGame.update {
            it?.copy(
                events = it.events + cardEvent,
                lastUpdated = System.currentTimeMillis()
            )
        }
    }

    fun updateGameNumber(gameNumber: String) { _activeGame.update { it?.copy(gameNumber = gameNumber, lastUpdated = System.currentTimeMillis()) } }
    fun updateHomeTeamName(name: String) { _activeGame.update { it?.copy(homeTeamName = name, lastUpdated = System.currentTimeMillis()) } }
    fun updateAwayTeamName(name: String) { _activeGame.update { it?.copy(awayTeamName = name, lastUpdated = System.currentTimeMillis()) } }
    fun updateHomeTeamColor(color: Color) { _activeGame.update { it?.copy(homeTeamColorArgb = color.toArgb(), lastUpdated = System.currentTimeMillis()) } }
    fun updateAwayTeamColor(color: Color) { _activeGame.update { it?.copy(awayTeamColorArgb = color.toArgb(), lastUpdated = System.currentTimeMillis()) } }

    fun setHalfDuration(minutes: Int) {
        _activeGame.update { currentGame ->
            currentGame?.let { game ->
                val newHalfDurationMillis = minutes * 60 * 1000L
                var newDisplayedTime = game.displayedTimeMillis
                if ((game.currentPhase == GamePhase.PRE_GAME || game.currentPhase.isKickOffSelectionPhase()) && !game.isTimerRunning) {
                    newDisplayedTime = newHalfDurationMillis
                } else if (game.currentPhase.usesHalfDuration() && !game.isTimerRunning) {
                    val timeAlreadyElapsed = game.actualTimeElapsedInPeriodMillis
                    newDisplayedTime = (newHalfDurationMillis - timeAlreadyElapsed).coerceAtLeast(0L)
                }
                game.copy(
                    halfDurationMinutes = minutes,
                    displayedTimeMillis = newDisplayedTime,
                    lastUpdated = System.currentTimeMillis()
                )
            }
        }
    }

    fun setHalftimeDuration(minutes: Int) {
        _activeGame.update { currentGame ->
            currentGame?.let { game ->
                var newDisplayedTime = game.displayedTimeMillis
                if (game.currentPhase == GamePhase.HALF_TIME && !game.isTimerRunning) {
                    newDisplayedTime = (minutes * 60 * 1000L - game.actualTimeElapsedInPeriodMillis).coerceAtLeast(0L)
                }
                game.copy(
                    halftimeDurationMinutes = minutes,
                    displayedTimeMillis = newDisplayedTime,
                    lastUpdated = System.currentTimeMillis()
                )
            }
        }
    }

    fun resetGame() {
        val originalGame = _activeGame.value ?: run {
            Log.w(tag, "resetGame called but no active game to reset. Creating new default game instead.")
            createNewDefaultGame() // Or simply return if that's preferred
            return
        }
        Log.d(tag, "Reset game called for game: ${originalGame.id}")
        cancelTimer()

        val resetGame = Game(id = originalGame.id, gameDateTimeEpochMillis = originalGame.gameDateTimeEpochMillis)
            .copy(
                gameNumber = originalGame.gameNumber,
                fieldNumber = originalGame.fieldNumber,
                homeTeamName = originalGame.homeTeamName,
                awayTeamName = originalGame.awayTeamName,
                ageGroup = originalGame.ageGroup,
                halfDurationMinutes = originalGame.halfDurationMinutes,
                halftimeDurationMinutes = originalGame.halftimeDurationMinutes,
                hasExtraTime = originalGame.hasExtraTime,
                hasPenalties = originalGame.hasPenalties,
                kickOffTeam = originalGame.kickOffTeam,
                displayedTimeMillis = Game().regulationPeriodDurationMillis(GamePhase.FIRST_HALF),
                status = GameStatus.SCHEDULED,
                currentPhase = GamePhase.NOT_STARTED,
            )
        _activeGame.value = resetGame
    }

    fun resetTimer() {
        val gameBeforeReset = _activeGame.value ?: return
        cancelTimer()
        _activeGame.update {
            it?.copy(
                displayedTimeMillis = gameBeforeReset.regulationPeriodDurationMillis(gameBeforeReset.currentPhase), // gameBeforeReset is non-null here
                actualTimeElapsedInPeriodMillis = 0L,
                lastUpdated = System.currentTimeMillis()
            )
        }
        val gameAfterReset = _activeGame.value ?: return
        val resetMessage = "Timer for the period ${gameAfterReset.currentPhase.readable()} has been reset."
        val resetEvent = GenericLogEvent(message = resetMessage)
        _activeGame.update { game -> game?.copy(events = game.events + resetEvent) }

        gameTimerService?.configureTimerForGame(game = gameAfterReset, startImmediately = false) // Pass nullable game
    }

    fun recordPenaltyAttempt(scored: Boolean) {
        val currentGame = _activeGame.value ?: return
        val taker = currentGame.kickOffTeam

        if (currentGame.currentPhase != GamePhase.PENALTIES) {
            Log.w(tag, "recordPenaltyAttempt called but game is not in PENALTIES phase.")
            return
        }
        Log.d(tag, "Recording penalty attempt for ${taker.name}. Scored: $scored")

        _activeGame.update { game ->
            game?.let {
                var newScoreHome = it.homeScore
                var newScoreAway = it.awayScore
                val newKickOffTeamForNext = it.kickOffTeam.opposite()
                var updatedPenaltiesTakenHome = it.penaltiesTakenHome
                var updatedPenaltiesTakenAway = it.penaltiesTakenAway
                val eventMessage: String

                if (taker == Team.HOME) {
                    updatedPenaltiesTakenHome++
                    if (scored) newScoreHome++
                    eventMessage = "Penalty by ${it.homeTeamName} (${taker.name}): ${if (scored) "SCORED" else "MISSED/SAVED"}"
                } else { // taker == Team.AWAY
                    updatedPenaltiesTakenAway++
                    if (scored) newScoreAway++
                    eventMessage = "Penalty by ${it.awayTeamName} (${taker.name}): ${if (scored) "SCORED" else "MISSED/SAVED"}"
                }
                val penaltyEvent = GenericLogEvent(message = eventMessage)
                var newPhase = it.currentPhase
                if (checkShootoutEndCondition(newScoreHome, newScoreAway, updatedPenaltiesTakenHome, updatedPenaltiesTakenAway)) {
                    newPhase = GamePhase.GAME_ENDED
                    Log.i(tag, "Penalty shootout ended. Final Score: H $newScoreHome - A $newScoreAway")
                }

                it.copy(
                    homeScore = newScoreHome,
                    awayScore = newScoreAway,
                    penaltiesTakenHome = updatedPenaltiesTakenHome,
                    penaltiesTakenAway = updatedPenaltiesTakenAway,
                    events = it.events + penaltyEvent,
                    kickOffTeam = newKickOffTeamForNext,
                    currentPhase = newPhase,
                    lastUpdated = System.currentTimeMillis()
                )
            }
        }
    }


    private fun checkShootoutEndCondition(
        currentHomeScore: Int, currentAwayScore: Int,
        penaltiesTakenHome: Int, penaltiesTakenAway: Int,
        shootoutRoundLimit: Int = 5
    ): Boolean {
        if (penaltiesTakenHome >= shootoutRoundLimit && penaltiesTakenAway >= shootoutRoundLimit) {
            return currentHomeScore != currentAwayScore && penaltiesTakenHome == penaltiesTakenAway
        }
        else {
            val kicksRemainingHome = shootoutRoundLimit - penaltiesTakenHome
            val kicksRemainingAway = shootoutRoundLimit - penaltiesTakenAway

            if (currentHomeScore > currentAwayScore + kicksRemainingAway) return true
            if (currentAwayScore > currentHomeScore + kicksRemainingHome) return true
            return false
        }
    }


    @RequiresPermission(Manifest.permission.VIBRATE)
    private fun vibrate(pattern: VibrationPattern) {
        if (vibrator?.hasVibrator() == true) {
            val effect = when (pattern) {
                VibrationPattern.ADDED_TIME_REMINDER -> VibrationEffect.createWaveform(longArrayOf(0, 300, 100, 300, 10000), 0)
                VibrationPattern.GOAL_SCORED -> VibrationEffect.createWaveform(longArrayOf(0, 150, 50, 150, 50), -1)
                VibrationPattern.GENERIC_EVENT -> VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE)
            }
            vibrator.vibrate(effect)
        }
    }

    enum class VibrationPattern {
        ADDED_TIME_REMINDER, GOAL_SCORED, GENERIC_EVENT
    }
}

// Helper extension function, consider moving to common GamePhase related file
fun GamePhase.needsKickOffSelection(): Boolean {
    return this == GamePhase.FIRST_HALF ||
            this == GamePhase.EXTRA_TIME_FIRST_HALF ||
            this == GamePhase.PENALTIES
}

fun GamePhase.needsKickOff(): Boolean {
    return this == GamePhase.FIRST_HALF || this == GamePhase.SECOND_HALF ||
            this == GamePhase.EXTRA_TIME_FIRST_HALF || this == GamePhase.EXTRA_TIME_SECOND_HALF ||
            this == GamePhase.PENALTIES
}
fun GamePhase.isKickOffSelectionPhase(): Boolean { // More precise than needsKickOffSelection
    return this == GamePhase.KICK_OFF_SELECTION_FIRST_HALF ||
           this == GamePhase.KICK_OFF_SELECTION_EXTRA_TIME ||
           this == GamePhase.KICK_OFF_SELECTION_PENALTIES
}

fun GamePhase.usesHalfDuration(): Boolean {
    return this == GamePhase.FIRST_HALF || this == GamePhase.SECOND_HALF ||
            this == GamePhase.EXTRA_TIME_FIRST_HALF || this == GamePhase.EXTRA_TIME_SECOND_HALF ||
            this == GamePhase.PRE_GAME // Allow setting duration in pre-game
}
