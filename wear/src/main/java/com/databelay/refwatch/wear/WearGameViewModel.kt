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
import com.databelay.refwatch.wear.data.DataFetchStatus
import com.databelay.refwatch.wear.data.GameStorageWear
import com.databelay.refwatch.wear.data.GameTimerService
import com.databelay.refwatch.wear.util.ConnectivityObserver // For network status
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val activeGame: StateFlow<Game>
}

@HiltViewModel
class WearGameViewModel @Inject constructor(
    @ApplicationContext applicationContext: Context, // Renamed for clarity
    private val savedStateHandle: SavedStateHandle,
    private val gameStorage: GameStorageWear,
    private val vibrator: Vibrator?
) : AndroidViewModel(applicationContext as Application), IWearGameViewModel {
    private val tag = "WearGameViewModelTAG" // Renamed for uniqueness from class name

    override val gamesList: StateFlow<List<Game>> = gameStorage.gamesListFlow
    override val dataFetchStatus: StateFlow<DataFetchStatus> = gameStorage.dataFetchStatusFlow

    override val isOnline: StateFlow<Boolean> = gameStorage.networkStatusFlow.map {
        it == ConnectivityObserver.Status.AVAILABLE
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _activeGame = MutableStateFlow(loadInitialActiveGame())
    override val activeGame: StateFlow<Game> = _activeGame.asStateFlow()

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
                // FIXME: why service is still in addedtime? buzzing timer after extra time 2nd half is over  during penalties
                ?.onEach { serviceState ->
                    if (!_activeGame.value.inAddedTime && serviceState.inAddedTime) {
                        Log.i(tag, "Added time is now ACTIVE via TimerService. Starting reminder vibration.")
                        vibrate(VibrationPattern.ADDED_TIME_REMINDER)
                    }
                    if (_activeGame.value.inAddedTime && !serviceState.inAddedTime) {
                        Log.i(tag, "Added time is now INACTIVE via TimerService. Stopping reminder vibration.")
                        vibrator?.cancel()
                    }
                    _activeGame.update { currentGame ->
                        currentGame.copy(
                            isTimerRunning = serviceState.isTimerRunning,
                            displayedTimeMillis = serviceState.displayedMillis,
                            actualTimeElapsedInPeriodMillis = serviceState.actualTimeElapsedInPeriodMillis,
                            inAddedTime = serviceState.inAddedTime,
                        )
                    }
                }
                ?.launchIn(viewModelScope)

            val currentActiveGameOnConnect = _activeGame.value // Use a local val
            if (currentActiveGameOnConnect.status == GameStatus.IN_PROGRESS) {
                Log.d(tag, "Service connected. Current game phase: ${currentActiveGameOnConnect.currentPhase}, timer running: ${currentActiveGameOnConnect.isTimerRunning}")
                gameTimerService?.configureTimerForGame(
                    game = currentActiveGameOnConnect,
                    startImmediately = currentActiveGameOnConnect.isTimerRunning
                )
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

        _activeGame.onEach { game ->
            if (game.status != GameStatus.COMPLETED) { // Avoid re-saving if just completed by this VM
                saveActiveGameStateToHandle()
                viewModelScope.launch { // Launch in viewModelScope for suspend function
                    gameStorage.addOrUpdateGame(game)
                }
                Log.d(tag, "Active game ${game.id} state change persisted.")
            }
        }.launchIn(viewModelScope)

        isOnline.onEach { online ->
            Log.i(tag, "Network status in ViewModel: ${if (online) "Online" else "Offline"}")
            // GameStorageWear's init block handles triggering syncPendingGames.
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

        val initialGame = _activeGame.value
        _activeGame.update { game ->
            var initialDisplayTime = game.regulationPeriodDurationMillis()
            if (!game.isTimerRunning && game.currentPhase.hasTimer()) {
                val regulationDuration = game.regulationPeriodDurationMillis()
                initialDisplayTime = if (game.actualTimeElapsedInPeriodMillis >= regulationDuration) {
                    game.actualTimeElapsedInPeriodMillis - regulationDuration
                } else {
                    regulationDuration - game.actualTimeElapsedInPeriodMillis
                }
            }
            game.copy(displayedTimeMillis = initialDisplayTime)
        }
        // updateCurrentPeriodKickOffTeam(initialGame.currentPhase, initialGame.kickOffTeam) // This was problematic, kickOffTeam should be set by user or phase progression
        Log.d(tag, "Initial active game ID: ${initialGame.id}, Phase: ${initialGame.currentPhase}, KickOff: ${initialGame.kickOffTeam}")
    }


    val allGamesMap: StateFlow<Map<String, Game>> =
        kotlinx.coroutines.flow.combine(gamesList, _activeGame) { scheduled, active ->
            val gameMap = scheduled.associateBy { it.id }.toMutableMap()
            active?.let { gameMap[it.id] = it } // Handle active potentially being null if interface changes
            gameMap.toMap() // Return immutable map
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

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

    private fun loadInitialActiveGame(): Game {
        val savedGameJson: String? = savedStateHandle.get("activeGameJson")
        val gameFromState = savedGameJson?.let {
            try {
                Log.d(tag, "Loading active game from SavedStateHandle.")
                AppJsonConfiguration.decodeFromString<Game>(it)
            } catch (e: Exception) {
                Log.e(tag, "Error decoding active game from JSON for SavedStateHandle.", e)
                null
            }
        }
        if (gameFromState != null) return gameFromState

        Log.d(tag, "No active game in SavedStateHandle, trying from scheduled or default.")
        val firstScheduledGame = gamesList.value.firstOrNull { it.status == GameStatus.SCHEDULED }
        return firstScheduledGame?.copy(
            currentPhase = GamePhase.NOT_STARTED,
            homeScore = 0, awayScore = 0, events = emptyList()
            // Retain its scheduled status and other details
        ) ?: Game().also {
            Log.d(tag, "No game in SavedStateHandle or scheduled. Using new default game.")
        }
    }

    private fun saveActiveGameStateToHandle() {
        try {
            val activeGameJson = AppJsonConfiguration.encodeToString(_activeGame.value)
            savedStateHandle["activeGameJson"] = activeGameJson
            Log.d(tag, "Active game state saved to SavedStateHandle. ID: ${_activeGame.value.id}")
        } catch (e: Exception) {
            Log.e(tag, "Error saving active game state to JSON for SavedStateHandle", e)
        }
    }

    fun createNewDefaultGame() {
        cancelTimer() // Resets isCurrentGameSessionActive
        val newDefaultGame = Game(gameDateTimeEpochMillis = System.currentTimeMillis())
        _activeGame.value = newDefaultGame.copy(
            displayedTimeMillis = newDefaultGame.regulationPeriodDurationMillis(GamePhase.FIRST_HALF),
            status = GameStatus.IN_PROGRESS
        )
        // KickOffTeam should be set via UI interaction if needed, or defaults to HOME
        Log.d(tag, "New default game created with ID: ${newDefaultGame.id}. Setting as active.")
    }

    fun selectGameToStart(gameFromList: Game) {
        cancelTimer() // Resets isCurrentGameSessionActive
        Log.d(tag, "Selecting game from list to start: ${gameFromList.id}")
        val cleanGameForStart = gameFromList.copy(
            currentPhase = GamePhase.NOT_STARTED,
            homeScore = 0,
            awayScore = 0,
            displayedTimeMillis = gameFromList.regulationPeriodDurationMillis(GamePhase.FIRST_HALF),
            actualTimeElapsedInPeriodMillis = 0L,
            isTimerRunning = false,
            events = emptyList(),
            status = GameStatus.IN_PROGRESS, // Mark as IN_PROGRESS once user selects it
            lastUpdated = System.currentTimeMillis()
            // kickOffTeam is retained from gameFromList
        )
        _activeGame.value = cleanGameForStart
        Log.d(tag, "Selected game ${cleanGameForStart.id} set as active.")
    }

    fun finishAndSyncActiveGame(onSyncComplete: () -> Unit) {
        Log.d(tag, "finishAndSyncActiveGame called for game: ${_activeGame.value.id}")
        cancelTimer() // Resets isCurrentGameSessionActive
        val finalGameData = _activeGame.value.copy(
            currentPhase = GamePhase.GAME_ENDED,
            status = GameStatus.COMPLETED,
            isTimerRunning = false,
            lastUpdated = System.currentTimeMillis()
        )
        // Update _activeGame. This triggers the onEach in init to save to GameStorageWear
        _activeGame.value = finalGameData
        Log.d(tag, "Game ${finalGameData.id} marked as COMPLETED in ViewModel.")

        // The save to GameStorageWear is now handled by the _activeGame.onEach collector.
        // GameStorageWear will attempt to sync this update to Firestore.
        // This callback is called immediately after handing off.
        viewModelScope.launch {
            Log.i(tag, "Game ${finalGameData.id} processing for completion finished. Resetting UI.")
            resetActiveGameToDefaultOrNextScheduled()
            onSyncComplete()
        }
    }

    @Deprecated("GameStorageWear handles sync automatically. This function may be removed.")
    fun attemptSyncPendingGames() {
        Log.w(tag, "attemptSyncPendingGames called, but this is now primarily handled by GameStorageWear.")
        // If an explicit trigger is truly needed from UI:
        // viewModelScope.launch { gameStorage.syncPendingGames() }
        // Note: syncPendingGames in GameStorageWear now doesn't require userId as it uses currentUserId from WatchAuthManager.
    }

    fun resetActiveGameToDefaultOrNextScheduled() {
        cancelTimer()
        val nextScheduledGame = gamesList.value
            .filter { it.status == GameStatus.SCHEDULED && it.id != _activeGame.value.id }
            .minByOrNull { it.gameDateTimeEpochMillis ?: Long.MAX_VALUE }

        val newActiveGame = nextScheduledGame?.copy(
            currentPhase = GamePhase.NOT_STARTED, homeScore = 0, awayScore = 0, events = emptyList(),
            status = GameStatus.SCHEDULED, // Keep its scheduled status
            isTimerRunning = false, actualTimeElapsedInPeriodMillis = 0L,
            displayedTimeMillis = nextScheduledGame.regulationPeriodDurationMillis(GamePhase.FIRST_HALF)
        ) ?: Game().let { defaultGame ->
            defaultGame.copy(
                displayedTimeMillis = defaultGame.regulationPeriodDurationMillis(GamePhase.FIRST_HALF)
            )
        }
        _activeGame.value = newActiveGame
        Log.d(tag, "Active game has been reset. New active game ID: ${_activeGame.value.id}")
    }


    fun toggleTimer() {
        val currentGame = _activeGame.value
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
        if (currentGame.status == GameStatus.SCHEDULED && !currentGame.isTimerRunning) { // If starting timer for a scheduled game
            _activeGame.update { it.copy(status = GameStatus.IN_PROGRESS) }
        }
    }

    fun cancelTimer() {
        Log.d(tag, "cancelTimer called in ViewModel.")
        gameTimerService?.commandStopGameSessionAndCleanup()
        vibrator?.cancel()
        isCurrentGameSessionActive = false
        _activeGame.update {
            it.copy(isTimerRunning = false)
        }
    }

    fun proceedToNextPhaseManager(gameAtPeriodEndInput: Game) {
//        gameTimerService?.pauseGameTimer(updateNotificationText = "Paused: ${gameAtPeriodEndInput.currentPhase.readable()}")

        var gameAtPeriodEnd = gameAtPeriodEndInput.copy(isTimerRunning = false) // Ensure timer is marked as stopped

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
////             TODO: BEGIN TEMP remove
//            GamePhase.PRE_GAME -> GamePhase.EXTRA_TIME_SECOND_HALF
////             TODO: END TEMP remove
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
            else -> gameAtPeriodEnd.currentPhase // Should not happen
        }
//        // TODO: BEGIN TEMP remove
//        gameAtPeriodEnd = gameAtPeriodEnd.copy(
//            hasExtraTime = true,
//            hasPenalties = true
//        )
//        // TODO: END TEMP remove

        val newKickOffTeam = when (nextPhase) {
            GamePhase.FIRST_HALF, GamePhase.EXTRA_TIME_FIRST_HALF, GamePhase.PENALTIES -> lastPhaseKickOffTeam
            GamePhase.SECOND_HALF, GamePhase.EXTRA_TIME_SECOND_HALF -> lastPhaseKickOffTeam.opposite()
            GamePhase.HALF_TIME -> lastPhaseKickOffTeam.opposite() // Who kicks off after halftime
            GamePhase.EXTRA_TIME_HALF_TIME -> lastPhaseKickOffTeam.opposite() // Who kicks off after ET halftime
            else -> lastPhaseKickOffTeam // For KICK_OFF_SELECTION phases, GAME_ENDED, etc.
        }

        val updatedGame = gameAtPeriodEnd.copy(
            currentPhase = nextPhase,
            actualTimeElapsedInPeriodMillis = 0L,
            displayedTimeMillis = gameAtPeriodEnd.regulationPeriodDurationMillis(nextPhase), // Use gameAtPeriodEnd as context for duration
            status = if (nextPhase == GamePhase.GAME_ENDED) GameStatus.COMPLETED else GameStatus.IN_PROGRESS,
            kickOffTeam = newKickOffTeam,
            lastUpdated = System.currentTimeMillis()
        )

        _activeGame.value = updatedGame
        Log.i(tag, "Phase ${gameAtPeriodEnd.currentPhase} ended. New phase: ${updatedGame.currentPhase}. Kick-off: ${updatedGame.kickOffTeam}")

        if (updatedGame.currentPhase == GamePhase.GAME_ENDED) {
            gameTimerService?.commandStopGameSessionAndCleanup()
            isCurrentGameSessionActive = false // Explicitly ensure session is marked inactive
            // UI should prompt to confirm game end, which might call finishAndSyncActiveGame
        } else if (updatedGame.currentPhase.needsKickOffSelection()) {
            // If next phase is a kick-off selection, do not start timer. Let user interact.
            gameTimerService?.configureTimerForGame(game = updatedGame, startImmediately = false)
        } else {
            // For playable phases or breaks, configure timer (breaks might auto-start)
             gameTimerService?.configureTimerForGame(
                game = updatedGame,
                startImmediately = updatedGame.currentPhase.isBreak() // Auto-start timer for breaks
            )
        }
    }


    fun setKickOffTeam(team: Team) {
        // This function primarily sets the team that will take the *first* kick-off of the game,
        // or the team for a specific kick-off selection phase (like penalties).
        _activeGame.update {
            it.copy(
                kickOffTeam = team,
                lastUpdated = System.currentTimeMillis()
            )
        }
        Log.d(tag, "Kick-off team for current context set to $team")
    }

    // Called from UI after user presses "Kick Off"

    fun kickOff() {
        val currentGame = _activeGame.value
        val currentPhase = currentGame.currentPhase

        if (currentPhase.needsKickOff()) {
            val teamName =
                if (currentGame.kickOffTeam == Team.HOME) currentGame.homeTeamName else currentGame.awayTeamName
            val kickOffMessage = "Kick Off - ${teamName} - ${currentPhase.readable()}"
            val kickOffEvent = GenericLogEvent(message = kickOffMessage)
            Log.i(tag, kickOffMessage)
            _activeGame.update {currentGame -> currentGame.copy(events = currentGame.events+kickOffEvent)}
            gameTimerService?.startGameTimer(_activeGame.value)

            // Proceed to the actual playing phase (e.g., FIRST_HALF from KICK_OFF_SELECTION_FIRST_HALF)
//            proceedToNextPhaseManager(currentGame.copy(events = currentGame.events + kickOffEvent))
        } else {
            Log.w(tag, "KickOff action attempted in inappropriate phase: $currentPhase")
        }
    }


    fun setToHaveExtraTime() {
        _activeGame.update { it.copy(hasExtraTime = true, lastUpdated = System.currentTimeMillis()) }
    }

    fun setToHavePenalties() {
        _activeGame.update { it.copy(hasPenalties = true, lastUpdated = System.currentTimeMillis()) }
    }

    fun addGoal(team: Team) {
        val currentGame = _activeGame.value
        if (!currentGame.currentPhase.isPlayablePhase()) return

        val newHomeScore = if (team == Team.HOME) currentGame.homeScore + 1 else currentGame.homeScore
        val newAwayScore = if (team == Team.AWAY) currentGame.awayScore + 1 else currentGame.awayScore
        val goalEvent = GoalScoredEvent(
            team = team,
            gameTimeMillis = currentGame.actualTimeElapsedInPeriodMillis.toDouble(), // Capture time at event
            homeScoreAtTime = newHomeScore,
            awayScoreAtTime = newAwayScore
        )
        _activeGame.update {
            it.copy(
                homeScore = newHomeScore,
                awayScore = newAwayScore,
                events = it.events + goalEvent,
                lastUpdated = System.currentTimeMillis()
            )
        }
        vibrate(VibrationPattern.GOAL_SCORED)
        Log.d(tag, "Goal added for $team. Score: ${_activeGame.value.homeScore}-${_activeGame.value.awayScore}")
    }


    fun addCard(team: Team, playerNumber: Int, cardType: CardType) {
        val currentGame = _activeGame.value
        if (!currentGame.currentPhase.isPlayablePhase()) return

        val cardEvent = CardIssuedEvent(
            team = team,
            playerNumber = playerNumber,
            cardType = cardType,
            gameTimeMillis = currentGame.actualTimeElapsedInPeriodMillis.toDouble()
        )
        _activeGame.update {
            it.copy(
                events = it.events + cardEvent,
                lastUpdated = System.currentTimeMillis()
            )
        }
    }

    fun updateGameNumber(gameNumber: String) { _activeGame.update { it.copy(gameNumber = gameNumber, lastUpdated = System.currentTimeMillis()) } }
    fun updateHomeTeamName(name: String) { _activeGame.update { it.copy(homeTeamName = name, lastUpdated = System.currentTimeMillis()) } }
    fun updateAwayTeamName(name: String) { _activeGame.update { it.copy(awayTeamName = name, lastUpdated = System.currentTimeMillis()) } }
    fun updateHomeTeamColor(color: Color) { _activeGame.update { it.copy(homeTeamColorArgb = color.toArgb(), lastUpdated = System.currentTimeMillis()) } }
    fun updateAwayTeamColor(color: Color) { _activeGame.update { it.copy(awayTeamColorArgb = color.toArgb(), lastUpdated = System.currentTimeMillis()) } }

    fun setHalfDuration(minutes: Int) {
        _activeGame.update { currentGame ->
            val newHalfDurationMillis = minutes * 60 * 1000L
            var newDisplayedTime = currentGame.displayedTimeMillis
            if ((currentGame.currentPhase == GamePhase.PRE_GAME || currentGame.currentPhase.isKickOffSelectionPhase()) && !currentGame.isTimerRunning) {
                newDisplayedTime = newHalfDurationMillis // Set full duration if pre-game/selection
            } else if (currentGame.currentPhase.usesHalfDuration() && !currentGame.isTimerRunning) {
                // If timer not running in a playable phase, adjust based on elapsed time if any
                val timeAlreadyElapsed = currentGame.actualTimeElapsedInPeriodMillis
                newDisplayedTime = (newHalfDurationMillis - timeAlreadyElapsed).coerceAtLeast(0L)
            }
            currentGame.copy(
                halfDurationMinutes = minutes,
                displayedTimeMillis = newDisplayedTime,
                lastUpdated = System.currentTimeMillis()
            )
        }
    }

    fun setHalftimeDuration(minutes: Int) {
        _activeGame.update { currentGame ->
            var newDisplayedTime = currentGame.displayedTimeMillis
            if (currentGame.currentPhase == GamePhase.HALF_TIME && !currentGame.isTimerRunning) {
                newDisplayedTime = (minutes * 60 * 1000L - currentGame.actualTimeElapsedInPeriodMillis).coerceAtLeast(0L)
            }
            currentGame.copy(
                halftimeDurationMinutes = minutes,
                displayedTimeMillis = newDisplayedTime,
                lastUpdated = System.currentTimeMillis()
            )
        }
    }

    fun resetGame() {
        Log.d(tag, "Reset game called for game: ${_activeGame.value.id}")
        cancelTimer()
        val originalGame = _activeGame.value
        val resetGame = Game(id = originalGame.id, gameDateTimeEpochMillis = originalGame.gameDateTimeEpochMillis)
            .copy(
                gameNumber = originalGame.gameNumber,
                homeTeamName = originalGame.homeTeamName,
                awayTeamName = originalGame.awayTeamName,
                ageGroup = originalGame.ageGroup,
                halfDurationMinutes = originalGame.halfDurationMinutes, // Keep configured durations
                halftimeDurationMinutes = originalGame.halftimeDurationMinutes,
                hasExtraTime = originalGame.hasExtraTime,
                hasPenalties = originalGame.hasPenalties,
                kickOffTeam = originalGame.kickOffTeam, // Keep original kick-off team setting
                displayedTimeMillis = Game().regulationPeriodDurationMillis(GamePhase.FIRST_HALF), // Reset display time
                status = if (originalGame.status == GameStatus.COMPLETED || originalGame.status == GameStatus.CANCELLED) GameStatus.SCHEDULED else originalGame.status // Revert to scheduled if was finished
            )
        _activeGame.value = resetGame
    }

    fun resetTimer() {
        val gameBeforeReset = _activeGame.value
        cancelTimer() // Stops service timer and updates local isTimerRunning, resets session flag
        _activeGame.update {
            it.copy(
                displayedTimeMillis = gameBeforeReset.regulationPeriodDurationMillis(gameBeforeReset.currentPhase),
                actualTimeElapsedInPeriodMillis = 0L,
                lastUpdated = System.currentTimeMillis()
            )
        }
        gameTimerService?.configureTimerForGame(game = _activeGame.value, startImmediately = false)
    }

    fun recordPenaltyAttempt(scored: Boolean) {
        val currentGame = _activeGame.value
        val taker = currentGame.kickOffTeam

        if (currentGame.currentPhase != GamePhase.PENALTIES) {
            Log.w(tag, "recordPenaltyAttempt called but game is not in PENALTIES phase.")
            return
        }
        Log.d(tag, "Recording penalty attempt for ${taker.name}. Scored: $scored")

        _activeGame.update { game ->
            var newScoreHome = game.homeScore
            var newScoreAway = game.awayScore
            val newKickOffTeamForNext = game.kickOffTeam.opposite()
            var updatedPenaltiesTakenHome = game.penaltiesTakenHome
            var updatedPenaltiesTakenAway = game.penaltiesTakenAway
            val eventMessage: String

            if (taker == Team.HOME) {
                updatedPenaltiesTakenHome++
                if (scored) newScoreHome++
                eventMessage = "Penalty by ${game.homeTeamName} (${taker.name}): ${if (scored) "SCORED" else "MISSED/SAVED"}"
            } else { // taker == Team.AWAY
                updatedPenaltiesTakenAway++
                if (scored) newScoreAway++
                eventMessage = "Penalty by ${game.awayTeamName} (${taker.name}): ${if (scored) "SCORED" else "MISSED/SAVED"}"
            }
            val penaltyEvent = GenericLogEvent(message = eventMessage)
            var newPhase = game.currentPhase
            if (checkShootoutEndCondition(newScoreHome, newScoreAway, updatedPenaltiesTakenHome, updatedPenaltiesTakenAway)) {
                newPhase = GamePhase.GAME_ENDED
                Log.i(tag, "Penalty shootout ended. Final Score: H $newScoreHome - A $newScoreAway")
            }

            game.copy(
                homeScore = newScoreHome,
                awayScore = newScoreAway,
                penaltiesTakenHome = updatedPenaltiesTakenHome,
                penaltiesTakenAway = updatedPenaltiesTakenAway,
                events = game.events + penaltyEvent,
                kickOffTeam = newKickOffTeamForNext,
                currentPhase = newPhase,
                lastUpdated = System.currentTimeMillis()
            )
        }
    }

    private fun checkShootoutEndCondition(
        currentHomeScore: Int, currentAwayScore: Int,
        penaltiesTakenHome: Int, penaltiesTakenAway: Int,
        shootoutRoundLimit: Int = 5 // Standard is 5
    ): Boolean {
        // Check after each pair of kicks if shootout is beyond initial 5 rounds
        if (penaltiesTakenHome >= shootoutRoundLimit && penaltiesTakenAway >= shootoutRoundLimit && penaltiesTakenHome == penaltiesTakenAway) {
            return currentHomeScore != currentAwayScore // Sudden death: any score difference ends it
        }

        // Check for decisive lead within the first 5 rounds
        val kicksRemainingHome = shootoutRoundLimit - penaltiesTakenHome
        val kicksRemainingAway = shootoutRoundLimit - penaltiesTakenAway

        if (currentHomeScore > currentAwayScore + kicksRemainingAway) return true // Home has an insurmountable lead
        if (currentAwayScore > currentHomeScore + kicksRemainingHome) return true // Away has an insurmountable lead

        // If all 5 rounds are complete for both, and scores are still tied, it continues to sudden death (handled by first condition on subsequent kicks)
        // If all 5 rounds complete and scores are different, it's over.
        return penaltiesTakenHome >= shootoutRoundLimit && penaltiesTakenAway >= shootoutRoundLimit && currentHomeScore != currentAwayScore
    }


    @RequiresPermission(Manifest.permission.VIBRATE)
    private fun vibrate(pattern: VibrationPattern) {
        if (vibrator?.hasVibrator() == true) {
            val effect = when (pattern) {
                VibrationPattern.ADDED_TIME_REMINDER -> VibrationEffect.createWaveform(longArrayOf(0, 300, 100, 300, 100, 300, 5000), 0)
                VibrationPattern.GOAL_SCORED -> VibrationEffect.createWaveform(longArrayOf(0, 150, 50, 150, 50, 300), -1)
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

