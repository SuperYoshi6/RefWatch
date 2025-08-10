package com.databelay.refwatch.wear // Your Wear OS package

import android.Manifest
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.compose.animation.core.copy
import androidx.compose.ui.geometry.isEmpty
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
import com.databelay.refwatch.common.WearSyncConstants
import com.databelay.refwatch.common.formatTime
import com.databelay.refwatch.common.hasTimer
import com.databelay.refwatch.common.isBreak
import com.databelay.refwatch.common.isPlayablePhase
import com.databelay.refwatch.common.opposite
import com.databelay.refwatch.common.readable
import com.databelay.refwatch.common.shouldTimerAutostart
import com.databelay.refwatch.wear.data.DataFetchStatus
import com.databelay.refwatch.wear.data.GameStorageWear
import com.databelay.refwatch.wear.data.GameTimerService
import com.databelay.refwatch.wear.data.TimerState
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.CapabilityInfo
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.encodeToString
import javax.inject.Inject

// In your main source set (e.g., in the same file as WearGameViewModel or a separate interfaces file)
interface IWearGameViewModel {
    val gamesList: StateFlow<List<Game>>
    val dataFetchStatus: StateFlow<DataFetchStatus>

    val isPhoneConnected: StateFlow<Boolean> // Assuming this is a flow
    val activeGame: StateFlow<Game?> // Assuming Game? as it can be null

}

@HiltViewModel
class WearGameViewModel @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    private val application: Application, // For NodeClient/CapabilityClient if used here
    private val savedStateHandle: SavedStateHandle,
    private val gameStorage: GameStorageWear,
    private val dataClient: DataClient, // For syncing game updates
    private val vibrator: Vibrator?
) : AndroidViewModel(application),  IWearGameViewModel {
    private val TAG = "WearGameViewModel"

    // --- Scheduled Games List and Sync Status (from GameStorageWear) ---
    override val gamesList: StateFlow<List<Game>> = gameStorage.gamesListFlow
    override val dataFetchStatus: StateFlow<DataFetchStatus> = gameStorage.dataFetchStatusFlow
    override val isPhoneConnected: StateFlow<Boolean> = gameStorage.isPhoneConnected

    // --- Active Game State (managed by this ViewModel) ---
    private val _activeGame = MutableStateFlow(loadInitialActiveGame())
    override val activeGame: StateFlow<Game> = _activeGame.asStateFlow()

    init {
        // Observe active game changes to save them
        _activeGame.onEach { game ->
            if (game.status != GameStatus.COMPLETED) { // Don't re-save if just completed by this VM
                saveActiveGameState()
            }
        }.launchIn(viewModelScope)

        // Attempt to sync pending games on ViewModel initialization
        attemptSyncPendingGames()

        // Observe phone connectivity changes from GameStorageWear
        gameStorage.isPhoneConnected
            // TODO: test this with phone disconnects, phone overwrites!!! not-synced status not showing on watch
            //  TODO: test with log-offs,  what happends if the user is logged off from the phone? where do the games sync to?
            .onEach { isConnected ->
                if (isConnected) {
                    Log.i(TAG, "Phone connection now active (observed in ViewModel), attempting sync of pending games.")
                    attemptSyncPendingGames()
                } else {
                    Log.i(TAG, "Phone connection lost (observed in ViewModel).")
                    // Optionally, update UI or behavior if phone disconnects
                }
            }
            .launchIn(viewModelScope) // Launch the collection in the ViewModel's scope
    }

    // --- Service Connection ---
    private var isCurrentGameSessionActive = false
    private var gameTimerService: GameTimerService? = null
    private var isServiceBound = false
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(TAG, "GameTimerService connected")
            val binder = service as GameTimerService.LocalBinder
            gameTimerService = binder.getService()
            isServiceBound = true

            // Start observing timer state FROM THE SERVICE
            gameTimerService?.timerStateFlow // THIS IS THE CORRECT FLOW TO OBSERVE
                ?.onEach { serviceState -> // serviceState is the TimerState object emitted by the service
                    // --- Logic to START vibration ---
                    // FIXME:  notification not allowed upon install, why?
                    if (!activeGame.value.inAddedTime && serviceState.inAddedTime) {
                        // inAddedTime just became true
                        Log.i(TAG, "Added time is now ACTIVE via TimerService. Starting reminder vibration.")
                        vibrate(VibrationPattern.ADDED_TIME_REMINDER)
                    }
                    // --- Logic to STOP vibration (if service can also signal end of added time) ---
                    if (activeGame.value.inAddedTime && !serviceState.inAddedTime) {
                        // inAddedTime just became false
                        Log.i(TAG, "Added time is now INACTIVE via TimerService. Stopping reminder vibration.")
                        vibrator?.cancel()
                    }
                    // Update _activeGame based on serviceState
                    _activeGame.update { currentGame ->
                        // Only update if the service state is relevant (e.g. for the current game)
                        // You might add checks here if the service could, in theory, be timing for a different game
                        // (though with current design, it's configured for one game at a time)
                        currentGame.copy(
                            isTimerRunning = serviceState.isTimerRunning,
                            displayedTimeMillis = serviceState.displayedMillis,
                            actualTimeElapsedInPeriodMillis = serviceState.actualTimeElapsedInPeriodMillis,
                            inAddedTime = serviceState.inAddedTime,
                           )
                    }
                }
                ?.launchIn(viewModelScope)

            // Now that service is connected, if there's an active game,
            // tell the service to configure itself based on the current game state.
            // This is important if the ViewModel is recreated while the service is already running.
            val currentActiveGame = _activeGame.value
            if (currentActiveGame.status == GameStatus.IN_PROGRESS) { // Or some other relevant condition
                Log.d(TAG, "Service connected. Current game phase: ${currentActiveGame.currentPhase}, timer running: ${currentActiveGame.isTimerRunning}")
                gameTimerService?.configureTimerForGame(
                    game = currentActiveGame,
                    startImmediately = currentActiveGame.isTimerRunning // Or determine if it should resume
                )
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.w(TAG, "GameTimerService disconnected")
            gameTimerService = null
            isServiceBound = false
            // Optionally, you could set _timerServiceState to a disconnected/default state
            // _timerServiceState.value = TimerState(isTimerRunning = false, displayedMillis = 0L)
        }
    }

    // Combined map of all games (scheduled and the current active one)
    // Useful for screens that might need to look up any game by ID.
    val allGamesMap: StateFlow<Map<String, Game>> =
        combine(gamesList, _activeGame) { scheduled, active ->
            val gameMap = scheduled.associateBy { it.id }.toMutableMap()
            // The active game instance (potentially with live updates) overwrites any scheduled version
            gameMap[active.id] = active
            gameMap
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    init {
        Log.d(TAG, "WearGameViewModel initializing.")

        // 1. Observe _activeGame (Potentially for service commands - Re-evaluate)
        viewModelScope.launch {
            _activeGame.collectLatest { game ->
                // This was for managing OngoingActivity or commanding service.
                // Service binding and initial configuration is now more explicit.
                // Might still be useful for saving state or other reactive logic to game changes.
            }
        }

        // 2. Start and Bind to Service (This is generally correct)
        Intent(applicationContext, GameTimerService::class.java).also { intent ->
            try {
                ContextCompat.startForegroundService(applicationContext, intent)
                Log.d(TAG, "Requested to start GameTimerService.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start GameTimerService", e)
            }
        }
        bindToGameTimerService() // Sets up the connection

        // 3. Observe dataFetchStatus (Seems reasonable)
        viewModelScope.launch {
            dataFetchStatus.collect { status ->
                Log.i(TAG, "DataFetchStatus collected in ViewModel: $status")
                // Potentially trigger actions based on status
            }
        }

        // 4. Observe gamesList (Seems reasonable)
        viewModelScope.launch {
            gamesList.collect { games ->
                Log.i(TAG, "Scheduled games list collected in ViewModel: ${games.size} games")
                // Logic if active game needs update based on list changes
            }
        }

        val initialGame = _activeGame.value
        val initialGameLoaded = _activeGame.value // Get the game loaded by loadInitialActiveGame()
        _activeGame.update { game ->
            var initialDisplayTime = game.regulationPeriodDurationMillis() // Default to period duration
            if (!game.isTimerRunning) { // Only set initial if not expecting service to immediately say it's running
                if (game.currentPhase.hasTimer()) {
                    val regulationDuration = game.regulationPeriodDurationMillis()
                    if (game.actualTimeElapsedInPeriodMillis >= regulationDuration) { // In added time
                        initialDisplayTime = game.actualTimeElapsedInPeriodMillis - regulationDuration
                    } else { // In regulation
                        initialDisplayTime = regulationDuration - game.actualTimeElapsedInPeriodMillis
                    }
                }
            }
            // If game.isTimerRunning is true, displayedTimeMillis will be quickly overridden by the service.
            // If it's false, this provides a sensible initial display.
            game.copy(displayedTimeMillis = initialDisplayTime)
        }

        updateCurrentPeriodKickOffTeam(initialGame.currentPhase, initialGame.kickOffTeam)
        Log.d(TAG, "Initial active game ID: ${initialGame.id}, Phase: ${initialGame.currentPhase}")
    }


    private fun bindToGameTimerService() {
        if (!isServiceBound && gameTimerService == null) {
            Intent(applicationContext, GameTimerService::class.java).also { intent ->
                applicationContext.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            }
        }
    }

    private fun unbindFromGameTimerService() {
        if (isServiceBound) {
            applicationContext.unbindService(serviceConnection)
            isServiceBound = false
            gameTimerService = null
            Log.d(TAG, "Unbound from GameTimerService.")
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "WearGameViewModel onCleared")
        unbindFromGameTimerService()
        // Decide if the service should be stopped when the ViewModel is cleared.
        // If the timer should continue even if the UI is gone for a long time (e.g. app killed),
        // then the service should manage its own lifecycle via startForeground/stopSelf.
        // If the timer is only relevant while this ViewModel (and thus a UI part) is active, then stop it.
        // For a game timer, it often makes sense for it to continue if started.
        // So, you might NOT call stopService here unless explicitly intended.
        // The service will stop itself via stopSelfSafely() when the timer finishes or is explicitly stopped.
    }

    private fun loadInitialActiveGame(): Game {
        val savedGameJson: String? = savedStateHandle.get("activeGameJson")
        return if (savedGameJson != null) {
            try {
                Log.d(TAG, "Loading active game from SavedStateHandle.")
                AppJsonConfiguration.decodeFromString<Game>(savedGameJson)
            } catch (e: Exception) {
                Log.e(TAG, "Error decoding active game from JSON, using default.", e)
                Game() // Default new game
            }
        } else {
            Log.d(TAG, "No active game in SavedStateHandle, using default new game.")
            // On first start, try to load the first scheduled game if available
            val firstScheduledGame = gameStorage.getGames().firstOrNull()
            if (firstScheduledGame != null) {
                Log.d(
                    TAG,
                    "No active game, but found scheduled game. Preparing ${firstScheduledGame.id}."
                )
                // Return a "clean" version of the first scheduled game, ready to start
                firstScheduledGame.copy(
                    currentPhase = GamePhase.NOT_STARTED,
                    homeScore = 0, awayScore = 0, events = emptyList()
                )
            } else {
                Game() // Default new game if no scheduled games either
            }
        }
    }
    private fun saveActiveGameState() {
        try {
            val activeGameJson = AppJsonConfiguration.encodeToString(_activeGame.value)
            savedStateHandle["activeGameJson"] = activeGameJson
            Log.d(TAG, "Active game state saved to SavedStateHandle. ID: ${_activeGame.value.id}")
            // Consider if/when to send active game updates to the phone here or more explicitly
            // sendActiveGameUpdateToPhone(_activeGame.value)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving active game state to JSON", e)
        }
    }

    fun createNewDefaultGame() {
        cancelTimer()
        val newDefaultGame = Game(gameDateTimeEpochMillis = System.currentTimeMillis())
        _activeGame.value = newDefaultGame
        updateCurrentPeriodKickOffTeam(GamePhase.PRE_GAME, newDefaultGame.kickOffTeam)
        _activeGame.update {
            it.copy(
                displayedTimeMillis = it.regulationPeriodDurationMillis(GamePhase.FIRST_HALF),
                status = GameStatus.IN_PROGRESS // Ad-hoc games are immediately in progress conceptually
            )
        }
        Log.d(TAG, "New default game created with ID: ${newDefaultGame.id}. Setting as active.")
        saveActiveGameState()
        syncNewAdHocGameToPhone(newDefaultGame) // Sync this new game to the phone
    }

    fun selectGameToStart(gameFromList: Game) {
        cancelTimer()
        Log.d(TAG, "Selecting game from list to start: ${gameFromList.id}, current status: ${gameFromList.status}")
        // Reset live state fields for the selected game, but keep its schedule info
        val cleanGameForStart = gameFromList.copy(
            currentPhase = GamePhase.NOT_STARTED,
            homeScore = 0,
            awayScore = 0,
            displayedTimeMillis = gameFromList.regulationPeriodDurationMillis(GamePhase.FIRST_HALF),
            actualTimeElapsedInPeriodMillis = 0L,
            isTimerRunning = false,
            events = emptyList(),
            status = GameStatus.IN_PROGRESS, // Mark as IN_PROGRESS once user selects it to start
            lastUpdated = System.currentTimeMillis(),
            kickOffTeam = gameFromList.kickOffTeam // Keep original kick-off team
        )
        _activeGame.value = cleanGameForStart
        Log.d(TAG, "Selected game ${cleanGameForStart.id} set as active. Phase: PRE_GAME.")
        saveActiveGameState()
        // Optionally send an update to the phone that this game is now active on the watch
        // sendActiveGameUpdateToPhone(cleanGameForStart)
    }

    fun finishAndSyncActiveGame(onSyncComplete: () -> Unit) {
        Log.d(TAG, "finishAndSyncActiveGame called for game: ${_activeGame.value.id}")
        cancelTimer()
        var finalGameData = _activeGame.value.copy(
            currentPhase = GamePhase.GAME_ENDED,
            status = GameStatus.COMPLETED,
            isTimerRunning = false,
            lastUpdated = System.currentTimeMillis()
        )
        // Preserve existing needsSyncWithPhone status initially, might be updated by sync attempt
        finalGameData = finalGameData.copy(needsSyncWithPhone = _activeGame.value.needsSyncWithPhone)
        _activeGame.update { finalGameData }

        Log.d(TAG, "Game ${finalGameData.id} marked as COMPLETED. Syncing to phone with ${finalGameData.events.size} events.")

        viewModelScope.launch(Dispatchers.IO) {
            var syncAttemptedAndSuccessful = false
            var needsSyncForLocalSave = finalGameData.needsSyncWithPhone // Start with current state

            if (gameStorage.isPhoneConnected.value) {
                Log.i(TAG, "Phone connected. Attempting to sync final game state for ${finalGameData.id} to phone.")
                val path = "${WearSyncConstants.GAME_UPDATE_FROM_WATCH_PATH_PREFIX}/${finalGameData.id}"
                // sendGameDataToPhone will send it with needsSyncWithPhone = false
                syncAttemptedAndSuccessful = sendGameDataToPhone(finalGameData, path)
                needsSyncForLocalSave = !syncAttemptedAndSuccessful // If sync failed, it needs sync
            } else {
                Log.w(TAG, "Phone not connected. Final game state for ${finalGameData.id} will be saved locally and marked for later sync.")
                needsSyncForLocalSave = true // Mark for later sync
            }

            // Update finalGameData with the correct needsSyncWithPhone status for local save
            val gameToSaveLocally = finalGameData.copy(needsSyncWithPhone = needsSyncForLocalSave)
            Log.i(TAG, "Attempting to save final game state locally for ${gameToSaveLocally.id} (needsSync=${gameToSaveLocally.needsSyncWithPhone})")

            try {
                gameStorage.locallyUpdateGameDetails(gameToSaveLocally)
                Log.i(TAG, "Successfully saved final game state locally for ${gameToSaveLocally.id}.")
            } catch (localSaveError: Exception) {
                Log.e(TAG, "CRITICAL: Failed to save game locally for ${gameToSaveLocally.id}", localSaveError)
                // Even if local save fails, the intended sync status (needsSyncForLocalSave) is what we have.
                // The actual game object _activeGame still holds finalGameData which might have needsSync=false if sync was attempted.
                // This state is a bit tricky. The source of truth for needsSync should ideally be after both attempt and save.
            } finally {
                // Update the active game flow to reflect the state that was attempted to be saved.
                _activeGame.update { gameToSaveLocally } // Reflect the state that was attempted to be saved

                launch(Dispatchers.Main) {
                    resetActiveGameToDefaultOrNextScheduled()
                    onSyncComplete()
                }
            }
        }
    }

    /**
     * Attempts to sync games that are marked as COMPLETED and needsSyncWithPhone = true.
     */
    fun attemptSyncPendingGames() {
        viewModelScope.launch(Dispatchers.IO) {
            val allGames = gameStorage.getGames()
            val gamesNeedingSync = allGames.filter { it.status == GameStatus.COMPLETED && it.needsSyncWithPhone }

            if (gamesNeedingSync.isEmpty()) {
                Log.d(TAG, "No pending games to sync.")
                return@launch
            }

            Log.i(TAG, "Found ${gamesNeedingSync.size} game(s) needing sync. Attempting now.")

            if (!gameStorage.isPhoneConnected.value) {
                Log.w(TAG, "attemptSyncPendingGames: Phone not reachable. Skipping sync attempt for pending games.")
                return@launch
            }

            gamesNeedingSync.forEach { gameToSync ->
                Log.i(TAG, "Attempting to sync pending game: ${gameToSync.id}")
                val path = "${WearSyncConstants.GAME_UPDATE_FROM_WATCH_PATH_PREFIX}/${gameToSync.id}"
                // sendGameDataToPhone will send with needsSyncWithPhone = false
                val success = sendGameDataToPhone(gameToSync, path)
                if (success) {
                    Log.i(TAG, "Successfully synced pending game: ${gameToSync.id}")
                    // Update local game to mark it as synced
                    try {
                        gameStorage.locallyUpdateGameDetails(gameToSync.copy(needsSyncWithPhone = false))
                    } catch (e: Exception) {
                        Log.e(TAG, "Error updating locally synced pending game ${gameToSync.id}", e)
                    }
                } else {
                    Log.e(TAG, "Error syncing pending game ${gameToSync.id}. It remains marked for sync.")
                    // No need to update locally, as needsSyncWithPhone is already true
                }
            }
        }
    }

    fun resetActiveGameToDefaultOrNextScheduled() {
        cancelTimer()
        // Try to load the next available scheduled game that isn't completed
        val nextScheduledGame = gamesList.value
            .filter { it.status == GameStatus.SCHEDULED && it.id != _activeGame.value.id }
            .minByOrNull { it.gameDateTimeEpochMillis ?: Long.MAX_VALUE }

        if (nextScheduledGame != null) {
            Log.d(TAG, "Resetting active game to next scheduled: ${nextScheduledGame.id}")
            // Prepare it similarly to loadInitialActiveGame or selectGameToStart
            val cleanNextGame = nextScheduledGame.copy(
                currentPhase = GamePhase.NOT_STARTED, homeScore = 0, awayScore = 0, events = emptyList(),
                status = GameStatus.SCHEDULED, isTimerRunning = false, actualTimeElapsedInPeriodMillis = 0L,
                displayedTimeMillis = nextScheduledGame.regulationPeriodDurationMillis(GamePhase.FIRST_HALF)
            )
            _activeGame.value = cleanNextGame
        } else {
            Log.d(TAG, "No next scheduled game. Resetting to new default game.")
            val newDefaultGame = Game()
            _activeGame.value = newDefaultGame.copy(
                displayedTimeMillis = newDefaultGame.regulationPeriodDurationMillis(GamePhase.FIRST_HALF)
            )
        }
        saveActiveGameState()
        Log.d(TAG, "Active game has been reset. New active game ID: ${_activeGame.value.id}")
    }

    private suspend fun sendGameDataToPhone(gameData: Game, path: String, markAsSyncedOnSend: Boolean = true): Boolean {
        return try {
            // If markAsSyncedOnSend is true, we assume this send is the definitive one for this version
            // and the data being sent should reflect that it no longer "needs sync" from this attempt.
            val dataToSend = if (markAsSyncedOnSend) gameData.copy(needsSyncWithPhone = false) else gameData
            val jsonString = AppJsonConfiguration.encodeToString(dataToSend)
            val putDataMapReq = PutDataMapRequest.create(path)
            putDataMapReq.dataMap.putString(WearSyncConstants.GAME_UPDATE_PAYLOAD_KEY, jsonString)
            putDataMapReq.dataMap.putLong("timestamp", System.currentTimeMillis())
            putDataMapReq.setUrgent()

            dataClient.putDataItem(putDataMapReq.asPutDataRequest()).await()
            Log.i(TAG, "Successfully sent game data for ID ${gameData.id} to path: $path")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send game data for ID ${gameData.id} to path $path.", e)
            false
        }
    }

    private fun syncNewAdHocGameToPhone(gameToSync: Game) {
        viewModelScope.launch(Dispatchers.IO) {
            if (gameStorage.isPhoneConnected.value) {
                Log.i(TAG, "Phone connected. Attempting to sync new ad-hoc game ${gameToSync.id} to phone.")
                val path = "${WearSyncConstants.GAME_UPDATE_FROM_WATCH_PATH_PREFIX}/${gameToSync.id}"
                // For a new ad-hoc game, we send it as is. If successful, its needsSyncWithPhone might already be false or be updated by phone ack.
                // The `markAsSyncedOnSend = true` in sendGameDataToPhone will ensure it's sent with needsSyncWithPhone = false.
                val success = sendGameDataToPhone(gameToSync, path)
                if (success) {
                    Log.i(TAG, "Successfully synced new ad-hoc game ${gameToSync.id} to phone.")
                    // Optionally, update the local game state if the send itself implies it's synced
                    // gameStorage.locallyUpdateGameDetails(gameToSync.copy(needsSyncWithPhone = false))
                    // This depends on whether `sendGameDataToPhone` sending with `needsSyncWithPhone = false` is enough,
                    // or if you need an explicit local save after the fact.
                    // For now, let's assume sendGameDataToPhone handles the state sent, and local save is separate.
                } else {
                    Log.w(TAG, "Failed to sync new ad-hoc game ${gameToSync.id}. It will be marked for later sync if not already.")
                    // Ensure it's marked for sync locally if the send failed
                    // This might already be handled by how newDefaultGame is created, or needs explicit update.
                    // gameStorage.locallyUpdateGameDetails(gameToSync.copy(needsSyncWithPhone = true))
                }
            } else {
                Log.w(TAG, "Phone not connected. New ad-hoc game ${gameToSync.id} will be saved locally and marked for later sync.")
                // Ensure it's marked for sync locally
                // gameStorage.locallyUpdateGameDetails(gameToSync.copy(needsSyncWithPhone = true))
            }
        }
    }

    fun toggleTimer() {
        val currentGame = _activeGame.value
        val currentPhase = currentGame.currentPhase

        vibrate(VibrationPattern.GENERIC_EVENT) // Haptic feedback for interaction

        // Timer can only be toggled for playable phases or timed breaks
        if (!currentPhase.hasTimer()) {
            Log.w(TAG, "toggleTimer called in a non-timed phase: ${currentPhase.readable()}. No action.")
            return
        }

        if(!isCurrentGameSessionActive) {
            Log.i(TAG, "toggleTimer: STARTING NEW GAME SESSION phase ${currentGame.currentPhase.readable()}.")
            gameTimerService?.commandStartGameSessionAndTimer(currentGame)
            isCurrentGameSessionActive = true
        }

        if (currentGame.isTimerRunning) {
            // Timer is RUNNING -> PAUSE IT (for the current phase)
            gameTimerService?.pauseGameTimer(updateNotificationText = "Paused: ${currentGame.currentPhase.readable()}")
            Log.d(TAG, "Timer PAUSED for ${currentPhase.readable()}.")
            // _activeGame.isTimerRunning will update via the service's TimerState collector
        }
        else {
            // Timer is NOT RUNNING (either paused or never started for this phase) -> START or RESUME IT
            // If the game is in a terminal state, don't allow starting the timer.
            if (currentGame.status != GameStatus.IN_PROGRESS) {
                Log.w(TAG, "Attempted to start timer in a game that is not in progress: ${currentPhase.readable()}. No action.")
                return
            }

            // Check if the current phase is actually supposed to have a timer.
            // PRE_GAME and KICK_OFF_SELECTION phases typically don't have a timer started by Play/Pause.
            // The timer for FIRST_HALF (etc.) starts after kick-off is confirmed.
            if (!currentPhase.hasTimer()) {
                Log.w(TAG, "toggleTimer: Timer cannot be started directly from ${currentPhase.readable()} with Play/Pause. Requires phase progression first.")
                // Optionally, show a message to the user: "Select kick-off first" or "Start period first"
                return
            }
            // Timer was PAUSED for the current phase -> RESUME IT
            gameTimerService?.resumeGameTimer(currentGame)
            Log.d(TAG, "Timer RESUMED for ${currentPhase.readable()}.")

            // Update ViewModel's state to reflect intent (service will confirm)
        }
        _activeGame.update { it.copy(isTimerRunning = currentGame.isTimerRunning, status = if (it.status == GameStatus.SCHEDULED) GameStatus.IN_PROGRESS else it.status) }
    }

    /**
     * Commands the GameTimerService to stop and reset its timer.
     * This is typically used when the game is reset, a new game is selected,
     * or the current period is definitively over and a new one isn't immediately starting.
     */
    fun cancelTimer() { // Renamed for clarity, implies service interaction
        Log.d(TAG, "cancelTimerAndResetServiceState called in ViewModel.")
        // Command the service to stop and reset its timer and state.
        gameTimerService?.commandStopGameSessionAndCleanup() // Or a similar method in your service
        vibrator?.cancel()
        // Update the ViewModel's state to reflect that the timer is no longer running.
        // The service's TimerStateFlow will also eventually emit a non-running state,
        // but updating the ViewModel's activeGame preemptively can make UI updates snappier.
        _activeGame.update {
            it.copy(
                isTimerRunning = false,
                // displayedTimeMillis = it.regulationPeriodDurationMillis(), // Or 0, depending on desired reset display
                // actualTimeElapsedInPeriodMillis = 0L // Service should reset this internally
            )
        }
        // Note: If the service's TimerState resets displayedTimeMillis and actualTimeElapsedInPeriodMillis,
        // the ViewModel will pick that up. The main thing here is to signal isTimerRunning = false.
        saveActiveGameState() // Save timer state on pause
    }

    // `proceedToNextPhaseManager` needs to reset `actualTimeElapsedInPeriodMillis`
    // and correctly set `displayedTimeMillis` for the *start* of the new phase.
    fun proceedToNextPhaseManager(gameAtPeriodEnd: Game) {
        var updatedGame = gameAtPeriodEnd.copy(
            isTimerRunning = false
            // actualTimeElapsedInPeriodMillis from the period that just ended is preserved in gameAtPeriodEnd
        )

        // Log the added time if the period that just ended was a playable phase
        if (gameAtPeriodEnd.currentPhase.isPlayablePhase()) {
            val regulationDur = gameAtPeriodEnd.regulationPeriodDurationMillis()
            if (gameAtPeriodEnd.actualTimeElapsedInPeriodMillis > regulationDur) {
                val addedTimePlayed = gameAtPeriodEnd.actualTimeElapsedInPeriodMillis - regulationDur
                Log.i(TAG, "Period ${gameAtPeriodEnd.currentPhase} ended. Added time played: ${addedTimePlayed.formatTime()}")
                // You might want to create a GameEvent here.
            }
        }

        val nextPhase: GamePhase
        when (gameAtPeriodEnd.currentPhase) {
            GamePhase.NOT_STARTED -> nextPhase = GamePhase.PRE_GAME
            GamePhase.PRE_GAME -> nextPhase = GamePhase.KICK_OFF_SELECTION_FIRST_HALF
            GamePhase.KICK_OFF_SELECTION_FIRST_HALF -> nextPhase = GamePhase.FIRST_HALF
            GamePhase.FIRST_HALF -> nextPhase = GamePhase.HALF_TIME
            GamePhase.HALF_TIME -> nextPhase = GamePhase.SECOND_HALF
            GamePhase.SECOND_HALF ->
                if (_activeGame.value.hasExtraTime)
                    nextPhase = GamePhase.KICK_OFF_SELECTION_EXTRA_TIME
                else
                    nextPhase = GamePhase.GAME_ENDED

            GamePhase.KICK_OFF_SELECTION_EXTRA_TIME -> nextPhase = GamePhase.EXTRA_TIME_FIRST_HALF
            GamePhase.EXTRA_TIME_FIRST_HALF -> nextPhase = GamePhase.EXTRA_TIME_HALF_TIME
            GamePhase.EXTRA_TIME_HALF_TIME -> nextPhase = GamePhase.EXTRA_TIME_SECOND_HALF
            GamePhase.EXTRA_TIME_SECOND_HALF ->
                // If score is tied up after extra time move to penalties
                if (_activeGame.value.homeScore == _activeGame.value.awayScore) {
                    _activeGame.update { it.copy(hasPenalties = true) }
                    nextPhase = GamePhase.KICK_OFF_SELECTION_PENALTIES
                } else
                    nextPhase = GamePhase.GAME_ENDED

            GamePhase.KICK_OFF_SELECTION_PENALTIES -> nextPhase = GamePhase.PENALTIES
            GamePhase.PENALTIES -> nextPhase = GamePhase.GAME_ENDED

            else -> {
                Log.w(
                    TAG,
                    "Transitioning from unhandled or terminal phase: ${gameAtPeriodEnd.currentPhase}"
                )
                nextPhase = gameAtPeriodEnd.currentPhase // No change
            }
        }


        updatedGame = updatedGame.copy(
            currentPhase = nextPhase,
            actualTimeElapsedInPeriodMillis = 0L, // Reset for the new phase
            displayedTimeMillis = updatedGame.regulationPeriodDurationMillis(nextPhase), // Set initial display for new phase
            status = if (nextPhase == GamePhase.GAME_ENDED) GameStatus.COMPLETED else GameStatus.IN_PROGRESS,
            kickOffTeam = if (updatedGame.currentPhase.isBreak()) updatedGame.kickOffTeam.opposite() else updatedGame.kickOffTeam
        )

        _activeGame.value = updatedGame
        Log.i(TAG, "Period ${gameAtPeriodEnd.currentPhase} ended. New phase: ${updatedGame.currentPhase}. Display for new: ${updatedGame.displayedTimeMillis.formatTime()}")
        saveActiveGameState()

        // --- Command the GameTimerService ---
        // In proceedToNextPhaseManager
        if (updatedGame.currentPhase == GamePhase.GAME_ENDED) {
            gameTimerService?.commandStopGameSessionAndCleanup() // Or false if you want a "Game Ended" notification
        } else {
            gameTimerService?.configureTimerForGame(
                game = updatedGame, // Pass the fully updated game state
                startImmediately = updatedGame.currentPhase.isBreak() // Start countdown for breaks immediately upon finishing the previous phase
            )
//            if (shouldAutoStartTimerForNewPhase) {
//                // Update the isTimerRunning in _activeGame if the service is told to start immediately
//                _activeGame.update { it.copy(isTimerRunning = true) }
//            }
        }
    }

    fun setToHaveExtraTime() {
        // Mark that extra time will be played
        _activeGame.update { it.copy(hasExtraTime = true, lastUpdated = System.currentTimeMillis()) }
        saveActiveGameState()
    }

    fun setToHavePenalties() {
        // Mark that extra time will be played
        _activeGame.update { it.copy(hasPenalties = true, lastUpdated = System.currentTimeMillis()) }
        saveActiveGameState()
    }

    // --- Game State Modifiers (Goals, Cards, Subs etc.) ---
    fun addGoal(team: Team) {
        val currentGame = _activeGame.value
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
            it.copy(
                homeScore = newHomeScore,
                awayScore = newAwayScore,
                events = it.events + goalEvent,
                lastUpdated = System.currentTimeMillis()
            )
        }
        vibrate(VibrationPattern.GOAL_SCORED)
        saveActiveGameState()
        Log.d(TAG, "Goal added for $team. Score: ${_activeGame.value.homeScore}-${_activeGame.value.awayScore}")
    }

    fun updateGameNumber(gameNumber: String) {
        _activeGame.update { it.copy(gameNumber = gameNumber, lastUpdated = System.currentTimeMillis()) }
        saveActiveGameState()
    }
    fun updateHomeTeamName(name: String) {
        _activeGame.update { it.copy(homeTeamName = name, lastUpdated = System.currentTimeMillis()) }
        saveActiveGameState()
    }

    fun updateAwayTeamName(name: String) {
        _activeGame.update { it.copy(awayTeamName = name, lastUpdated = System.currentTimeMillis()) }
        saveActiveGameState()
    }
    // --- Other Game Actions (Placeholder for brevity, implement as needed) ---

    fun startSubstitution(team: Team) { /* ... */ saveActiveGameState() }
    fun endSubstitution(team: Team, playerIn: String, playerOut: String) { /* ... */ saveActiveGameState() }
    fun recordFreeKick(team: Team) { /* ... */ saveActiveGameState() }
    fun recordCorner(team: Team) { /* ... */ saveActiveGameState() }
    fun recordThrowIn(team: Team) { /* ... */ saveActiveGameState() }

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
        saveActiveGameState()
    }


    // --- UI Configuration / Game Setup ---
    fun updateHomeTeamColor(color: Color) {
        _activeGame.update { it.copy(homeTeamColorArgb = color.toArgb(), lastUpdated = System.currentTimeMillis()) }
        saveActiveGameState()
    }

    fun updateAwayTeamColor(color: Color) {
        _activeGame.update { it.copy(awayTeamColorArgb = color.toArgb(), lastUpdated = System.currentTimeMillis()) }
        saveActiveGameState()
    }

    fun setKickOffTeam(team: Team) { // This is the overall designated kick-off team for 1st half
        _activeGame.update {
            it.copy(
                kickOffTeam = team,
                lastUpdated = System.currentTimeMillis()
            )
        }
        saveActiveGameState()
    }

    fun setHalfDuration(minutes: Int) {
        _activeGame.update { currentGame ->
            val newHalfDurationMillis = minutes * 60 * 1000L
            var newDisplayedTime = currentGame.displayedTimeMillis

            // Adjust displayed time if timer not running or in pre-game
            if (currentGame.currentPhase == GamePhase.PRE_GAME || !currentGame.isTimerRunning) {
                // If it's a phase that uses half duration, update displayed time
                if (currentGame.currentPhase.usesHalfDuration()) {
                    val timeAlreadyElapsed = if (currentGame.isTimerRunning) { // Should be false here
                        (currentGame.halfDurationMillis - currentGame.displayedTimeMillis).coerceAtLeast(0L)
                    } else {
                        currentGame.actualTimeElapsedInPeriodMillis
                    }
                    newDisplayedTime = (newHalfDurationMillis - timeAlreadyElapsed).coerceAtLeast(0L)
                }
            }
            // For halftime, its duration might be set independently.
            // For now, only adjust if displayedTime is for a half.

            currentGame.copy(
                halfDurationMinutes = minutes,
                // halftimeDurationMinutes will be set by setHalftimeDuration
                displayedTimeMillis = newDisplayedTime,
                lastUpdated = System.currentTimeMillis()
            )
        }
        saveActiveGameState()
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
        saveActiveGameState()
    }

    private fun updateCurrentPeriodKickOffTeam(phase: GamePhase, initialGameKickOffTeam: Team) {
        _activeGame.update { currentGame ->
            val newCurrentKickOff = when {
                // For breaks, the kick-off is for the *next* period
                phase.isBreak() -> initialGameKickOffTeam.opposite()
                else -> initialGameKickOffTeam
            }
            currentGame.copy(kickOffTeam = newCurrentKickOff)
            // No need to save state here, changePhase or other callers will save
        }
        Log.d("$TAG:updateCurrentPeriodKickOffTeam", "Updated kick-off team for phase $phase to ${_activeGame.value.kickOffTeam}")
    }

    fun kickOff() {
        val currentGame = _activeGame.value
        val teamName = if (currentGame.kickOffTeam == Team.HOME) currentGame.homeTeamName else currentGame.awayTeamName
        val kickOffMessage = "Kick Off - ${teamName} - ${currentGame.currentPhase.readable()}"
        val kickOffEvent = GenericLogEvent(message = kickOffMessage)

        _activeGame.update { it.copy(events = it.events + kickOffEvent) } // Log before changing phase
        toggleTimer()
    }

    fun resetGame() {
        Log.d(TAG, "Reset game called for game: ${_activeGame.value.id}")
        cancelTimer()
        val finalGameData = _activeGame.value.copy(
            currentPhase = GamePhase.NOT_STARTED,
            status = GameStatus.SCHEDULED,
            isTimerRunning = false,
            lastUpdated = System.currentTimeMillis(),
            homeScore = 0, awayScore = 0, events = emptyList(),
            penaltiesTakenHome = 0, penaltiesTakenAway = 0,
            actualTimeElapsedInPeriodMillis = 0

        )
        _activeGame.value = finalGameData // Update local state immediately
    }

    fun resetTimer() {
        cancelTimer()
        val currentGame = _activeGame.value
        _activeGame.update {
            it.copy(
                displayedTimeMillis = currentGame.regulationPeriodDurationMillis(currentGame.currentPhase),
                actualTimeElapsedInPeriodMillis = 0L, // Reset elapsed time for the current period
                lastUpdated = System.currentTimeMillis()
            )
        }
        saveActiveGameState()
    }
    /**
     * Records the result of a penalty attempt during a shootout.
     *
     * @param scored True if the penalty was scored, false otherwise.
     */
    fun recordPenaltyAttempt(scored: Boolean) {
        val currentGame = _activeGame.value
        val taker = currentGame.kickOffTeam // Get the calculated current taker

        if (currentGame.currentPhase != GamePhase.PENALTIES) {
            Log.w(TAG, "recordPenaltyAttempt called but game is not in PENALTIES phase.")
            return
        }

        Log.d(TAG, "Recording penalty attempt for ${taker.name}. Scored: $scored")

        _activeGame.update { game ->
            var newScoreHome = game.homeScore
            var newScoreAway = game.awayScore
            val newKickOffTeam = if (taker == Team.HOME) Team.AWAY else Team.HOME
            var updatedPenaltiesTakenHome = game.penaltiesTakenHome
            var updatedPenaltiesTakenAway = game.penaltiesTakenAway

            val eventMessage: String

            if (taker == Team.HOME) {
                updatedPenaltiesTakenHome++
                if (scored) {
                    newScoreHome++
                }
                eventMessage = "Penalty by ${game.homeTeamName} (${taker.name}): ${if (scored) "SCORED" else "MISSED/SAVED"}"
            } else { // taker == Team.AWAY
                updatedPenaltiesTakenAway++
                if (scored) {
                    newScoreAway++
                }
                eventMessage = "Penalty by ${game.awayTeamName} (${taker.name}): ${if (scored) "SCORED" else "MISSED/SAVED"}"
            }

            val penaltyEvent = GenericLogEvent( message = eventMessage)
            val updatedEvents = game.events + penaltyEvent

            // This is a critical piece.
            // Example conditions:
            // 1. After 5 kicks each, if scores are different.
            // 2. Before 5 kicks if one team has an unassailable lead
            //    (e.g., Home scores 3, Away misses 3; Home leads 3-0 with 2 kicks left for Away, Away can only reach 2).
            // 3. In sudden death (after 5 kicks each and scores are tied), if one team scores and the other misses in the same round.
            var newPhase = game.currentPhase
            if (checkShootoutEndCondition(newScoreHome, newScoreAway, updatedPenaltiesTakenHome, updatedPenaltiesTakenAway)) {
                newPhase = GamePhase.GAME_ENDED
                Log.i(TAG, "Penalty shootout ended. Final Score: H $newScoreHome - A $newScoreAway")
                // You might want to set a flag or specific event indicating the shootout winner
            }

            game.copy(
                homeScore = newScoreHome,
                awayScore = newScoreAway,
                penaltiesTakenHome = updatedPenaltiesTakenHome,
                penaltiesTakenAway = updatedPenaltiesTakenAway,
                events = updatedEvents,
                kickOffTeam = newKickOffTeam,
                currentPhase = newPhase // Update phase if shootout ended
            )
        }
        // syncActiveGameToMobile() // Sync after update
    }
    /**
    * Checks if the shootout has ended based on logic to determine if the penalty shootout has concluded.
    * This needs to be implemented thoroughly.
    */

    private fun checkShootoutEndCondition(
        currentHomeScore: Int,
        currentAwayScore: Int,
        penaltiesTakenHome: Int,
        penaltiesTakenAway: Int,
        shootoutRoundLimit: Int = 5 // Standard is 5 rounds before sudden death
    ): Boolean {
        // Only check if both teams have taken the same number of penalties
        // or if one team has completed their set of 'shootoutRoundLimit' kicks
        // and the other team cannot catch up.

        // If less than shootoutRoundLimit kicks each, check for unassailable lead
        if (penaltiesTakenHome < shootoutRoundLimit || penaltiesTakenAway < shootoutRoundLimit) {
            val kicksRemainingHome = shootoutRoundLimit - penaltiesTakenHome
            val kicksRemainingAway = shootoutRoundLimit - penaltiesTakenAway

            // Home has unassailable lead
            if (currentHomeScore > currentAwayScore + kicksRemainingAway) return true
            // Away has unassailable lead
            if (currentAwayScore > currentHomeScore + kicksRemainingHome) return true
        }

        // After shootoutRoundLimit kicks (or during if unassailable lead isn't met but kicks are equal)
        if (penaltiesTakenHome >= shootoutRoundLimit && penaltiesTakenAway >= shootoutRoundLimit && penaltiesTakenHome == penaltiesTakenAway) {
            return currentHomeScore != currentAwayScore // If scores are different after 5 rounds, it's over
        }

        // Sudden death logic (both teams have taken same number of kicks beyond shootoutRoundLimit)
        if (penaltiesTakenHome > shootoutRoundLimit && penaltiesTakenHome == penaltiesTakenAway) {
            return currentHomeScore != currentAwayScore // In any sudden death round, if scores differ, it's over
        }

        return false // Shootout continues
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

// Extension function for GamePhase
fun GamePhase.usesHalfDuration(): Boolean {
    return this == GamePhase.FIRST_HALF || this == GamePhase.SECOND_HALF ||
            this == GamePhase.EXTRA_TIME_FIRST_HALF || this == GamePhase.EXTRA_TIME_SECOND_HALF ||
            this == GamePhase.PRE_GAME // PRE_GAME often shows the upcoming half duration
}

