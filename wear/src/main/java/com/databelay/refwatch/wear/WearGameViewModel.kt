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
import com.databelay.refwatch.common.GameEvent
import com.databelay.refwatch.common.GamePhase
import com.databelay.refwatch.common.GameStatus
import com.databelay.refwatch.common.GenericLogEvent
import com.databelay.refwatch.common.GoalScoredEvent
import com.databelay.refwatch.common.PhaseChangedEvent
import com.databelay.refwatch.common.GoalType
import com.databelay.refwatch.common.IWearGameViewModel
import com.databelay.refwatch.common.PenaltyEvent
import com.databelay.refwatch.common.Team
import com.databelay.refwatch.common.formatTime
import com.databelay.refwatch.common.hasTimer
import com.databelay.refwatch.common.isBreak
import com.databelay.refwatch.common.isKickOffSelectionPhase
import com.databelay.refwatch.common.isPlayablePhase
import com.databelay.refwatch.common.needsKickOff
import com.databelay.refwatch.common.needsKickOffSelection
import com.databelay.refwatch.common.opposite
import com.databelay.refwatch.common.readable
import com.databelay.refwatch.common.toSnapshotForStorage
import com.databelay.refwatch.common.regulationPeriodDurationMillis
import com.databelay.refwatch.common.formattedGameDateTime
import com.databelay.refwatch.common.isTied
import com.databelay.refwatch.common.homeTeamColor
import com.databelay.refwatch.common.awayTeamColor
import com.databelay.refwatch.common.usesHalfDuration
import com.databelay.refwatch.wear.data.GameStorageWear
import com.databelay.refwatch.wear.data.GameTimerService
import com.databelay.refwatch.wear.data.TimerState
import com.databelay.refwatch.wear.util.ConnectivityObserver // For network status
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import com.databelay.refwatch.common.SubstitutionEvent
import com.databelay.refwatch.common.TemporaryDismissalEvent
import com.databelay.refwatch.wear.auth.WatchAuthManager
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
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import javax.inject.Inject
import java.util.Locale


enum class TimerDisplayMode {
    REMAINING, PLAYED
}

@OptIn(FlowPreview::class)
@HiltViewModel
class WearGameViewModel @Inject constructor(
    @ApplicationContext applicationContext: Context, // Renamed for clarity
    private val savedStateHandle: SavedStateHandle,
    private val gameStorage: GameStorageWear,
    private val watchAuthManager: WatchAuthManager,
    private val vibrator: Vibrator?
) : AndroidViewModel(applicationContext as Application), IWearGameViewModel {
    private val tag = "WearGameViewModel" // Renamed for uniqueness from class name

    override val gamesList: StateFlow<List<Game>> = gameStorage.gamesListFlow
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    override val isOnline: StateFlow<Boolean> = gameStorage.networkStatusFlow.map {
        it == ConnectivityObserver.Status.AVAILABLE
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _activeGame = MutableStateFlow<Game?>(null) // Start as null
    override val activeGame: StateFlow<Game?> = _activeGame.asStateFlow() // Expose as nullable

    private val _timerDisplayState = MutableStateFlow(TimerState())
    val timerDisplayState: StateFlow<TimerState> = _timerDisplayState.asStateFlow()

    private val _timerDisplayMode = MutableStateFlow(TimerDisplayMode.REMAINING)
    val timerDisplayMode: StateFlow<TimerDisplayMode> = _timerDisplayMode.asStateFlow()
    private val _kickoffCountdownSeconds = MutableStateFlow<Int?>(null)
    val kickoffCountdownSeconds: StateFlow<Int?> = _kickoffCountdownSeconds.asStateFlow()

    private val _activeDismissals = MutableStateFlow<List<TemporaryDismissalEvent>>(emptyList())
    val activeDismissals: StateFlow<List<TemporaryDismissalEvent>> = _activeDismissals.asStateFlow()

    private val _pendingReturnConfirmations = MutableStateFlow<List<TemporaryDismissalEvent>>(emptyList())
    val pendingReturnConfirmations: StateFlow<List<TemporaryDismissalEvent>> = _pendingReturnConfirmations.asStateFlow()

    fun dismissReturnConfirmation(event: TemporaryDismissalEvent) {
        _pendingReturnConfirmations.update { it.filterNot { e -> e.id == event.id } }
    }

    fun toggleTimerDisplayMode() {
        _timerDisplayMode.update {
            if (it == TimerDisplayMode.REMAINING) TimerDisplayMode.PLAYED else TimerDisplayMode.REMAINING
        }
    }


    private var isCurrentGameSessionActive = false

    private var isAddedTimeReminderVibrating = false
    private var gameTimerService: GameTimerService? = null
    private var isServiceBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(tag, "GameTimerService connected")
            val binder = service as GameTimerService.LocalBinder
            gameTimerService = binder.getService()
            isServiceBound = true

            gameTimerService?.timerStateFlow
                ?.distinctUntilChanged { oldState, newState ->
                    oldState.isTimerRunning == newState.isTimerRunning &&
                            oldState.displayedMillis == newState.displayedMillis &&
                            oldState.actualTimeElapsedInPeriodMillis == newState.actualTimeElapsedInPeriodMillis &&
                            oldState.inAddedTime == newState.inAddedTime &&
                            oldState.stoppageTimeMillis == newState.stoppageTimeMillis &&
                            oldState.isStoppageTimerRunning == newState.isStoppageTimerRunning
                }
                ?.onEach { serviceState ->
                    _timerDisplayState.value = serviceState
                    
                    // PERFORMANCE: Only check for expired dismissals if there are any active ones.
                    if (_activeDismissals.value.isNotEmpty()) {
                        val currentMatchTime = serviceState.actualTimeElapsedInPeriodMillis
                        _activeDismissals.update { current ->
                            val expired = current.filter { 
                                currentMatchTime >= it.startMatchTimeMillis + (it.durationMinutes * 60 * 1000L)
                            }
                            if (expired.isNotEmpty()) {
                                _pendingReturnConfirmations.update { it + expired }
                                expired.forEach { e ->
                                    addEvent(GenericLogEvent(
                                        message = "Zeitstrafe beendet: ${e.team.name} Nr.${e.playerNumber}",
                                        phase = serviceState.currentPhase,
                                        gameTimeMillis = currentMatchTime.toDouble()
                                    ))
                                }
                                vibrate(VibrationPattern.GAME_END)
                                current.filterNot { it in expired }
                            } else {
                                current
                            }
                        }
                    }

                    val currentActiveGame = _activeGame.value

                    // Only vibrate when a period timer is actually running and we are
                    // already in the added-time phase. Pre-Match setup / kick-off
                    // selection / paused timers must stay silent so the watch does not
                    // buzz continuously while the referee is still configuring the game.
                    val shouldVibrateAddedTimeReminder =
                        serviceState.inAddedTime &&
                        serviceState.isTimerRunning &&
                        currentActiveGame?.status == GameStatus.IN_PROGRESS &&
                        currentActiveGame.currentPhase.isPlayablePhase()

                    if (shouldVibrateAddedTimeReminder) {
                        if (!isAddedTimeReminderVibrating) {
                            startAddedTimeReminderVibration()
                        }
                    } else {
                        if (isAddedTimeReminderVibrating) {
                            stopAddedTimeReminderVibration()
                        }
                    }

                    // Only sync isTimerRunning to activeGame (other code paths need it).
                    // Timer display fields go through timerDisplayState to avoid
                    // copying the entire 40+ field Game object every second.
                    _activeGame.update { currentGame ->
                        if (currentGame?.isTimerRunning != serviceState.isTimerRunning) {
                            currentGame?.copy(isTimerRunning = serviceState.isTimerRunning)
                        } else {
                            currentGame
                        }
                    }
                }
                ?.launchIn(viewModelScope)

            val currentActiveGameOnConnect = _activeGame.value
            currentActiveGameOnConnect?.let { gameToConfigure ->
                if (gameToConfigure.status == GameStatus.IN_PROGRESS) {
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

            loadedGame.let { game ->
                _activeGame.update { current ->
                    current?.copy(
                        displayedTimeMillis = calculateInitialDisplayTime(game)
                    )
                }
            }

            // SIDELINE SUPPORT: Monitor the background games list for updates to the ACTIVE game.
            // If another device (e.g. Papa) adds an event or changes something, we merge it.
            gamesList.collect { latestGames ->
                val activeId = _activeGame.value?.id ?: return@collect
                val remoteGame = latestGames.find { it.id == activeId } ?: return@collect
                
                _activeGame.update { current ->
                    if (current == null) return@update remoteGame
                    
                    // PERFORMANCE: Use a very strict "deep change" check to avoid redundant UI work
                    // especially critical for Galaxy Watch 4 smoothness.
                    if (remoteGame.lastUpdated > current.lastUpdated) {
                        val localEventIds = current.events.map { it.id }.toSet()
                        val newRemoteEvents = remoteGame.events.filter { it.id !in localEventIds }
                        
                        val scoreChanged = remoteGame.homeScore != current.homeScore || remoteGame.awayScore != current.awayScore
                        val phaseChanged = remoteGame.currentPhase != current.currentPhase
                        val rosterChanged = remoteGame.homeRoster != current.homeRoster || remoteGame.awayRoster != current.awayRoster
                        
                        if (newRemoteEvents.isNotEmpty() || scoreChanged || phaseChanged || rosterChanged) {
                            Log.i(tag, "Sideline Sync: Merging changes (Events: ${newRemoteEvents.size}, Score: $scoreChanged, Phase: $phaseChanged)")
                            
                            if (newRemoteEvents.isNotEmpty() || scoreChanged || phaseChanged) {
                                vibrate(VibrationPattern.GENERIC_EVENT)
                            }

                            current.copy(
                                homeScore = remoteGame.homeScore,
                                awayScore = remoteGame.awayScore,
                                currentPhase = remoteGame.currentPhase,
                                events = (current.events + newRemoteEvents).distinctBy { it.id }.sortedBy { it.timestamp },
                                lastUpdated = remoteGame.lastUpdated,
                                homeRoster = remoteGame.homeRoster,
                                awayRoster = remoteGame.awayRoster,
                                displayedTimeMillis = if (phaseChanged) remoteGame.displayedTimeMillis else current.displayedTimeMillis
                            )
                        } else {
                            current
                        }
                    } else {
                        current
                    }
                }
            }
        }

        _activeGame.filterNotNull()
            .distinctUntilChanged { old, new -> 
                // Only trigger savedStateHandle save if important fields changed, 
                // and debounce it to avoid main thread jank during timer ticks.
                old.currentPhase == new.currentPhase && 
                old.homeScore == new.homeScore && 
                old.awayScore == new.awayScore &&
                old.kickOffTeam == new.kickOffTeam &&
                old.events.size == new.events.size
            }
            .debounce(1000L)
            .onEach { game ->
                if (game.status != GameStatus.COMPLETED) {
                    saveActiveGameStateToHandle()
                }
            }.launchIn(viewModelScope)

        _activeGame.filterNotNull()
            .distinctUntilChanged { old, new ->
                // PERFORMANCE: Check for data changes BEFORE converting the entire 40+ field 
                // object to a Map/JSON. We ignore timer fields here as they are synced 
                // separately via the service or only periodically.
                old.id == new.id &&
                old.homeScore == new.homeScore &&
                old.awayScore == new.awayScore &&
                old.currentPhase == new.currentPhase &&
                old.events.size == new.events.size &&
                old.homeTeamName == new.homeTeamName &&
                old.awayTeamName == new.awayTeamName &&
                old.hasTemporaryDismissals == new.hasTemporaryDismissals &&
                old.temporaryDismissalMinutes == new.temporaryDismissalMinutes
            }
            .map { game -> game.toSnapshotForStorage() } 
            .debounce(1000L) 
            .onEach { snapshot ->
                val latestGameToSave = _activeGame.value
                if (latestGameToSave != null && latestGameToSave.id == snapshot["id"] && latestGameToSave.status != GameStatus.COMPLETED) {
                    // Avoid saving a completely untouched default game
                    val isUntouchedDefault = latestGameToSave.currentPhase == GamePhase.NOT_STARTED &&
                            latestGameToSave.homeTeamName.isBlank() &&
                            latestGameToSave.awayTeamName.isBlank() &&
                            latestGameToSave.events.isEmpty()
                    if (!isUntouchedDefault) {
                        viewModelScope.launch {
                            gameStorage.addOrUpdateGame(latestGameToSave)
                        }
                    }
                }
            }.launchIn(viewModelScope)

        bindToGameTimerService()
    }


    val allGamesMap: StateFlow<Map<String, Game>> =
        kotlinx.coroutines.flow.combine(gamesList, _activeGame) { scheduled, active ->
            val gameMap = scheduled.associateBy { it.id }.toMutableMap()
            active?.let {
                val isUntouchedDefault = it.currentPhase == GamePhase.NOT_STARTED &&
                        it.homeTeamName.isBlank() &&
                        it.awayTeamName.isBlank() &&
                        it.events.isEmpty()
                if (!isUntouchedDefault) {
                    gameMap[it.id] = it
                }
            }
            gameMap.toMap()
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    private fun calculateInitialDisplayTime(game: Game): Long { 
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
                getApplication<Application>().bindService(
                    intent,
                    serviceConnection,
                    Context.BIND_AUTO_CREATE
                )
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
    private var addedTimeReminderJob: Job? = null

    @RequiresPermission(Manifest.permission.VIBRATE)
    private fun startAddedTimeReminderVibration() {
        if (vibrator?.hasVibrator() != true) return
        stopAddedTimeReminderVibration()

        isAddedTimeReminderVibrating = true
        addedTimeReminderJob = viewModelScope.launch {
            while (isAddedTimeReminderVibrating && isActive) {
                val oneShotPattern = VibrationEffect.createWaveform(
                    longArrayOf(0, 150, 50, 150),
                    intArrayOf(0, VibrationEffect.DEFAULT_AMPLITUDE, 0, VibrationEffect.DEFAULT_AMPLITUDE),
                    -1
                )
                vibrator.vibrate(oneShotPattern)
                delay(5000)
            }
        }
    }

    private fun stopAddedTimeReminderVibration() {
        addedTimeReminderJob?.cancel()
        addedTimeReminderJob = null
        vibrator?.cancel()
        isAddedTimeReminderVibrating = false
    }

    override fun onCleared() {
        super.onCleared()
        stopAddedTimeReminderVibration()
        unbindFromGameTimerService()
    }

    private fun loadInitialActiveGameInternal(currentGames: List<Game>): Game {
        val savedGameJson: String? = savedStateHandle["activeGameJson"]
        savedGameJson?.let { json ->
            try {
                return AppJsonConfiguration.decodeFromString<Game>(json)
            } catch (e: Exception) {
                Log.e(tag, "Error decoding game from SavedStateHandle", e)
            }
        }

        val firstScheduledGame = currentGames.firstOrNull { it.status == GameStatus.SCHEDULED }

        return firstScheduledGame?.copy(
            currentPhase = GamePhase.NOT_STARTED,
            homeScore = 0, awayScore = 0, events = emptyList()
        ) ?: Game()
    }

    private fun saveActiveGameStateToHandle() {
        _activeGame.value?.let { game ->
            try {
                val activeGameJson = AppJsonConfiguration.encodeToString(game)
                savedStateHandle["activeGameJson"] = activeGameJson
            } catch (e: Exception) {
                Log.e(tag, "Error saving active game state to JSON for SavedStateHandle", e)
            }
        }
    }

    fun addEvent(event: GameEvent) {
        var updatedGameInstance: Game? = null
        _activeGame.update { currentGame ->
            val gameAfterEventAdded = currentGame?.addEvent(event)
            updatedGameInstance = gameAfterEventAdded
            gameAfterEventAdded
        }

        updatedGameInstance?.let { gameToSave ->
            viewModelScope.launch {
                gameStorage.addOrUpdateGame(gameToSave)
            }
        }
    }

    fun removeEvent(eventToRemove: GameEvent, gameId: String? = null) {
        viewModelScope.launch {
            val gameToModify: Game?
            val modifyingActiveGame: Boolean

            if (gameId == null) {
                gameToModify = _activeGame.value
                modifyingActiveGame = true
            } else {
                gameToModify = gamesList.value.firstOrNull { it.id == gameId }
                modifyingActiveGame = _activeGame.value?.id == gameId
            }

            if (gameToModify == null) return@launch

            val updatedGame = gameToModify.removeEvent(eventToRemove)
            if (updatedGame == gameToModify) return@launch

            gameStorage.addOrUpdateGame(updatedGame)

            if (modifyingActiveGame || (gameId == null && _activeGame.value?.id == updatedGame.id) ) {
                _activeGame.value = updatedGame
            }
        }
    }


    fun createNewDefaultGame() {
        cancelTimer()
        val currentUid = watchAuthManager.currentWatchUserId.value ?: ""
        val newDefaultGame = Game(
            userId = currentUid,
            gameDateTimeEpochMillis = System.currentTimeMillis()
        )
        _activeGame.value = newDefaultGame.copy(
            displayedTimeMillis = newDefaultGame.regulationPeriodDurationMillis(GamePhase.FIRST_HALF),
        )
    }

    fun selectGameToStart(gameFromList: Game) {
        cancelTimer()
        val currentUid = watchAuthManager.currentWatchUserId.value ?: ""
        val cleanGameForStart = gameFromList.copy(
            userId = currentUid,
            currentPhase = GamePhase.NOT_STARTED,
            homeScore = 0,
            awayScore = 0,
            displayedTimeMillis = gameFromList.regulationPeriodDurationMillis(GamePhase.FIRST_HALF),
            actualTimeElapsedInPeriodMillis = 0L,
            isTimerRunning = false,
            events = emptyList(),
            lastUpdated = System.currentTimeMillis()
        )
        _activeGame.value = cleanGameForStart
    }

    fun finishAndSyncActiveGame(gameId: String) {
        viewModelScope.launch {
            cancelTimer()

            val gameToFinish = _activeGame.value?.takeIf { it.id == gameId }
                ?: gamesList.value.firstOrNull { it.id == gameId }

            if (gameToFinish == null) return@launch

            if (isServiceBound && gameTimerService != null && (gameToFinish.status == GameStatus.IN_PROGRESS || gameToFinish.isTimerRunning)) {
                gameTimerService?.stopGameTimerAndSession()
            }

            val finishedGame = gameToFinish.copy(
                isTimerRunning = false,
                displayedTimeMillis = 0L, 
                actualTimeElapsedInPeriodMillis = gameToFinish.actualTimeElapsedInPeriodMillis,
                currentPhase = GamePhase.GAME_ENDED,
                lastUpdated = System.currentTimeMillis()
            )

            _activeGame.update {
                if (it?.id == finishedGame.id) finishedGame else it
            }

            gameStorage.addOrUpdateGame(finishedGame)
            
            // Spielende Vibration: 3 Mal stark
            vibrate(VibrationPattern.GAME_END)
        }
    }

    fun abortGame() {
        viewModelScope.launch {
            val currentGame = _activeGame.value ?: return@launch
            cancelTimer()
            
            if (isServiceBound && gameTimerService != null) {
                gameTimerService?.stopGameTimerAndSession()
            }

            val abortedGame = currentGame.copy(
                isTimerRunning = false,
                currentPhase = GamePhase.ABORTED,
                lastUpdated = System.currentTimeMillis()
            )

            _activeGame.value = abortedGame
            gameStorage.addOrUpdateGame(abortedGame)
            
            vibrate(VibrationPattern.GAME_END)
        }
    }

    fun toggleTimer() {
        val currentGame = _activeGame.value ?: return
        val currentPhase = currentGame.currentPhase
        vibrate(VibrationPattern.GENERIC_EVENT)

        if (!currentPhase.hasTimer()) return

        // If it's a playable phase (match is on), using the pause button in the menu
        // should automatically trigger the stoppage timer (green clock).
        if (currentPhase.isPlayablePhase()) {
            gameTimerService?.toggleStoppageTimer()
        } else {
            // For non-playable phases (e.g. Half-time), use standard pause/resume.
            if (currentGame.isTimerRunning) {
                gameTimerService?.pauseGameTimer(updateNotificationText = "Paused: ${currentPhase.readable()}")
            } else {
                gameTimerService?.resumeGameTimer(currentGame)
            }
        }
    }

    fun cancelTimer() {
        gameTimerService?.commandStopGameSessionAndCleanup() {
            stopAddedTimeReminderVibration()
        }
        stopAddedTimeReminderVibration()
        isCurrentGameSessionActive = false
        _activeGame.update {
            it?.copy(isTimerRunning = false)
        }
    }

    fun proceedToNextPhaseManager(gameAtPeriodEndInput: Game) {
        val gameAtPeriodEnd = gameAtPeriodEndInput.copy(isTimerRunning = false)

        val lastPhaseKickOffTeam = gameAtPeriodEnd.kickOffTeam
        val nextPhase: GamePhase = when (gameAtPeriodEnd.currentPhase) {
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

        // Log phase change event so the game log clearly shows transitions
        val phaseChangeEvent = PhaseChangedEvent(
            newPhase = nextPhase,
            gameTimeMillis = gameAtPeriodEnd.actualTimeElapsedInPeriodMillis.toDouble(),
            phase = gameAtPeriodEnd.currentPhase
        )
        val updatedGame = gameAtPeriodEnd.copy(
            currentPhase = nextPhase,
            actualTimeElapsedInPeriodMillis = 0L,
            displayedTimeMillis = gameAtPeriodEnd.regulationPeriodDurationMillis(nextPhase),
            kickOffTeam = newKickOffTeam,
            stoppageTimeMillis = 0L,
            lastUpdated = System.currentTimeMillis()
        ).addEvent(phaseChangeEvent)

        _activeGame.value = updatedGame
        viewModelScope.launch { gameStorage.addOrUpdateGame(updatedGame) }
        
        if (!nextPhase.isKickOffSelectionPhase()) {
            gameTimerService?.configureTimerForGame(
                game = updatedGame,
                startImmediately = updatedGame.currentPhase.isBreak()
            )
        }

        if (updatedGame.currentPhase == GamePhase.GAME_ENDED) {
            gameTimerService?.commandStopGameSessionAndCleanup() {
                stopAddedTimeReminderVibration() 
            }
            isCurrentGameSessionActive = false
        }
        
        if (gameAtPeriodEndInput.currentPhase == GamePhase.SECOND_HALF || gameAtPeriodEndInput.currentPhase == GamePhase.EXTRA_TIME_SECOND_HALF) {
            vibrate(VibrationPattern.GAME_END)
        } else if (gameAtPeriodEndInput.currentPhase == GamePhase.HALF_TIME || gameAtPeriodEndInput.currentPhase == GamePhase.EXTRA_TIME_HALF_TIME) {
            vibrate(VibrationPattern.HALF_TIME_END)
        }
    }


    fun setKickOffTeam(team: Team) {
        _activeGame.update {
            it?.copy(
                kickOffTeam = team,
                lastUpdated = System.currentTimeMillis()
            )
        }
        vibrate(VibrationPattern.GENERIC_EVENT)
    }


    fun kickOff() {
        val currentGame = _activeGame.value ?: return
        val currentPhase = currentGame.currentPhase

        if (currentPhase.needsKickOff()) {
            if (_kickoffCountdownSeconds.value != null) return
            viewModelScope.launch {
                _kickoffCountdownSeconds.value = 10
                while ((_kickoffCountdownSeconds.value ?: 0) > 0) {
                    val value = _kickoffCountdownSeconds.value ?: 0
                    if (value <= 3) vibrateKickoffTick()
                    delay(1000)
                    _kickoffCountdownSeconds.value = (value - 1).coerceAtLeast(0)
                }
                vibrateKickoffGo()
                val latestGame = _activeGame.value ?: return@launch
                val teamName = if (latestGame.kickOffTeam == Team.HOME) latestGame.homeTeamName else latestGame.awayTeamName
                val kickOffMessage = "Anstoß ${latestGame.currentPhase.readable()} (${teamName})"
                addEvent(GenericLogEvent(message = kickOffMessage, phase = latestGame.currentPhase, gameTimeMillis = 0.0))
                gameTimerService?.startGameTimer(latestGame)
                _kickoffCountdownSeconds.value = null
            }
        }
    }


    fun setToHaveExtraTime() {
        _activeGame.update {
            it?.copy(
                hasExtraTime = true,
                lastUpdated = System.currentTimeMillis()
            )
        }
    }

    fun setToHavePenalties() {
        _activeGame.update {
            it?.copy(
                hasPenalties = true,
                lastUpdated = System.currentTimeMillis()
            )
        }
    }

    fun setHasTemporaryDismissals(enabled: Boolean) {
        _activeGame.update {
            it?.copy(
                hasTemporaryDismissals = enabled,
                lastUpdated = System.currentTimeMillis()
            )
        }
        _activeGame.value?.let { game -> viewModelScope.launch { gameStorage.addOrUpdateGame(game) } }
    }

    fun setTemporaryDismissalMinutes(minutes: Int) {
        val safe = minutes.coerceIn(0, 60)
        _activeGame.update {
            it?.copy(
                temporaryDismissalMinutes = safe,
                lastUpdated = System.currentTimeMillis()
            )
        }
        _activeGame.value?.let { game -> viewModelScope.launch { gameStorage.addOrUpdateGame(game) } }
    }

    /**
     * Toggle the on-watch setting for the penalty shootout. Drives the same
     * `hasPenalties` flag the mobile form edits, so the two stay in sync via
     * Firestore sync. When toggled off, the shootout is skipped even if the
     * game ends tied after extra time.
     */
    fun setHasPenalties(enabled: Boolean) {
        _activeGame.update {
            it?.copy(
                hasPenalties = enabled,
                lastUpdated = System.currentTimeMillis()
            )
        }
        _activeGame.value?.let { game -> viewModelScope.launch { gameStorage.addOrUpdateGame(game) } }
    }

    fun setPenaltyKicksPerTeam(count: Int) {
        val safe = count.coerceIn(1, 20)
        _activeGame.update {
            it?.copy(
                penaltyKicksPerTeam = safe,
                lastUpdated = System.currentTimeMillis()
            )
        }
        _activeGame.value?.let { game -> viewModelScope.launch { gameStorage.addOrUpdateGame(game) } }
    }

    fun toggleStoppageTimer() {
        gameTimerService?.toggleStoppageTimer()
        vibrate(VibrationPattern.GENERIC_EVENT)
    }

    private fun currentElapsedInPeriodMillis(): Long =
        _timerDisplayState.value.actualTimeElapsedInPeriodMillis

    fun addGoal(team: Team, playerNumber: Int? = null, goalType: GoalType = GoalType.REGULAR) {
        val currentGame = _activeGame.value ?: return
        if (!currentGame.currentPhase.isPlayablePhase()) return

        // If it's an own goal, the score goes to the OPPOSITE team
        val scoringTeam = if (goalType == GoalType.OWN_GOAL) team.opposite() else team

        val newHomeScore = if (scoringTeam == Team.HOME) currentGame.homeScore + 1 else currentGame.homeScore
        val newAwayScore = if (scoringTeam == Team.AWAY) currentGame.awayScore + 1 else currentGame.awayScore

        val goalEvent = GoalScoredEvent(
            team = team, // The event still logs which team "scored" (or whose player scored the OG)
            teamDisplayName = if (team == Team.HOME) currentGame.homeTeamName else currentGame.awayTeamName,
            goalType = goalType,
            playerNumber = playerNumber,
            gameTimeMillis = currentElapsedInPeriodMillis().toDouble(),
            homeScoreAtTime = newHomeScore,
            awayScoreAtTime = newAwayScore,
            phase = currentGame.currentPhase
        )
        _activeGame.update {
            it?.copy(
                homeScore = newHomeScore,
                awayScore = newAwayScore
            )?.addEvent(goalEvent)
        }
        vibrate(VibrationPattern.GOAL_SCORED)
    }


    fun addCard(
        team: Team, 
        playerNumber: Int, 
        cardType: CardType, 
        applyTemporaryDismissal: Boolean = false,
        isOfficial: Boolean = false,
        officialName: String? = null
    ) {
        val currentGame = _activeGame.value ?: return
        if (!currentGame.currentPhase.isPlayablePhase()) return

        val cardEvent = CardIssuedEvent(
            team = team,
            playerNumber = playerNumber,
            cardType = cardType,
            isOfficial = isOfficial,
            officialName = officialName,
            gameTimeMillis = currentElapsedInPeriodMillis().toDouble(),
            phase = currentGame.currentPhase
        )
        
        _activeGame.update { 
            var game = it?.addEvent(cardEvent)
            if (applyTemporaryDismissal && cardType == CardType.YELLOW) {
                val dismissalEvent = TemporaryDismissalEvent(
                    team = team,
                    playerNumber = playerNumber,
                    durationMinutes = currentGame.temporaryDismissalMinutes,
                    startMatchTimeMillis = currentElapsedInPeriodMillis().toDouble(),
                    gameTimeMillis = currentElapsedInPeriodMillis().toDouble(),
                    phase = currentGame.currentPhase
                )
                game = game?.addEvent(dismissalEvent)
                _activeDismissals.update { current -> current + dismissalEvent }
            }
            game
        }
        vibrate(VibrationPattern.GENERIC_EVENT)
    }

    fun updateGameNumber(gameNumber: String) {
        val updated = _activeGame.updateAndGet {
            it?.copy(
                gameNumber = gameNumber,
                lastUpdated = System.currentTimeMillis()
            )
        }
        updated?.let { viewModelScope.launch { gameStorage.addOrUpdateGame(it) } }
    }

    fun updateHomeTeamName(name: String) {
        val updated = _activeGame.updateAndGet {
            it?.copy(
                homeTeamName = name,
                lastUpdated = System.currentTimeMillis()
            )
        }
        updated?.let { viewModelScope.launch { gameStorage.addOrUpdateGame(it) } }
    }

    fun updateAwayTeamName(name: String) {
        val updated = _activeGame.updateAndGet {
            it?.copy(
                awayTeamName = name,
                lastUpdated = System.currentTimeMillis()
            )
        }
        updated?.let { viewModelScope.launch { gameStorage.addOrUpdateGame(it) } }
    }

    fun updateHomeTeamAbbr(abbr: String) {
        val sanitizedAbbr = abbr.take(3).uppercase()
        _activeGame.update {
            it?.copy(
                homeTeamAbbr = sanitizedAbbr,
                lastUpdated = System.currentTimeMillis()
            )
        }
        _activeGame.value?.let { viewModelScope.launch { gameStorage.addOrUpdateGame(it) } }
    }

    fun updateAwayTeamAbbr(abbr: String) {
        val sanitizedAbbr = abbr.take(3).uppercase()
        _activeGame.update {
            it?.copy(
                awayTeamAbbr = sanitizedAbbr,
                lastUpdated = System.currentTimeMillis()
            )
        }
        _activeGame.value?.let { viewModelScope.launch { gameStorage.addOrUpdateGame(it) } }
    }

    fun updateHomeCaptainNumber(number: Int?) {
        _activeGame.update {
            it?.copy(
                homeCaptainNumber = number,
                lastUpdated = System.currentTimeMillis()
            )
        }
        _activeGame.value?.let { viewModelScope.launch { gameStorage.addOrUpdateGame(it) } }
    }

    fun updateAwayCaptainNumber(number: Int?) {
        _activeGame.update {
            it?.copy(
                awayCaptainNumber = number,
                lastUpdated = System.currentTimeMillis()
            )
        }
        _activeGame.value?.let { viewModelScope.launch { gameStorage.addOrUpdateGame(it) } }
    }



    fun updateHomeTeamColor(color: Color) {
        val updated = _activeGame.updateAndGet {
            it?.copy(
                homeTeamColorArgb = color.toArgb(),
                lastUpdated = System.currentTimeMillis()
            )
        }
        updated?.let { viewModelScope.launch { gameStorage.addOrUpdateGame(it) } }
    }

    fun updateAwayTeamColor(color: Color) {
        val updated = _activeGame.updateAndGet {
            it?.copy(
                awayTeamColorArgb = color.toArgb(),
                lastUpdated = System.currentTimeMillis()
            )
        }
        updated?.let { viewModelScope.launch { gameStorage.addOrUpdateGame(it) } }
    }

    fun updateMaxSubstitutions(max: Int) {
        val updated = _activeGame.updateAndGet {
            it?.copy(
                maxSubstitutionsAllowed = max,
                lastUpdated = System.currentTimeMillis()
            )
        }
        updated?.let { viewModelScope.launch { gameStorage.addOrUpdateGame(it) } }
    }

    fun updateKickOffTeam(team: Team) {
        val updated = _activeGame.updateAndGet {
            it?.copy(
                kickOffTeam = team,
                lastUpdated = System.currentTimeMillis()
            )
        }
        updated?.let { viewModelScope.launch { gameStorage.addOrUpdateGame(it) } }
    }

    fun applySetupFromPhoneGame() {
        val currentGame = _activeGame.value ?: return
        val sourceGame = gamesList.value
            .asSequence()
            .filter { it.status == GameStatus.SCHEDULED && it.id != currentGame.id }
            .sortedByDescending { it.lastUpdated }
            .firstOrNull() ?: return

        val updated = currentGame.copy(
            gameNumber = sourceGame.gameNumber,
            homeTeamName = sourceGame.homeTeamName,
            awayTeamName = sourceGame.awayTeamName,
            homeTeamAbbr = sourceGame.homeTeamAbbr,
            awayTeamAbbr = sourceGame.awayTeamAbbr,
            homeCaptainNumber = sourceGame.homeCaptainNumber,
            awayCaptainNumber = sourceGame.awayCaptainNumber,
            homeTeamColorArgb = sourceGame.homeTeamColorArgb,
            awayTeamColorArgb = sourceGame.awayTeamColorArgb,
            halfDurationMinutes = sourceGame.halfDurationMinutes,
            halftimeDurationMinutes = sourceGame.halftimeDurationMinutes,
            extraTimeHalfDurationMinutes = sourceGame.extraTimeHalfDurationMinutes,
            maxSubstitutionsAllowed = sourceGame.maxSubstitutionsAllowed,
            kickOffTeam = sourceGame.kickOffTeam,
            displayedTimeMillis = sourceGame.regulationPeriodDurationMillis(currentGame.currentPhase),
            lastUpdated = System.currentTimeMillis()
        )
        _activeGame.value = updated
        viewModelScope.launch { gameStorage.addOrUpdateGame(updated) }
    }

    fun logSubstitution(team: Team, outgoing: Int, incoming: Int) {
        _activeGame.value?.let { game ->
            val teamName = if (team == Team.HOME) (game.homeTeamAbbr?.takeIf { it.isNotBlank() } ?: game.homeTeamName) else (game.awayTeamAbbr?.takeIf { it.isNotBlank() } ?: game.awayTeamName)
            val event = SubstitutionEvent(
                team = team,
                teamDisplayName = teamName,
                outgoingPlayerNumber = outgoing,
                incomingPlayerNumber = incoming,
                gameTimeMillis = currentElapsedInPeriodMillis().toDouble(),
                phase = game.currentPhase
            )
            _activeGame.update { it?.addEvent(event) }
            vibrate(VibrationPattern.GENERIC_EVENT)
        }
    }

    fun getSubstitutionsCount(team: Team): Int {
        return _activeGame.value?.events?.filterIsInstance<SubstitutionEvent>()?.count { it.team == team } ?: 0
    }


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
                ).also { updated ->
                    viewModelScope.launch { gameStorage.addOrUpdateGame(updated) }
                }
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
                ).also { updated ->
                    viewModelScope.launch { gameStorage.addOrUpdateGame(updated) }
                }
            }
        }
    }

    fun setExtraTimeDuration(minutes: Int) {
        _activeGame.update { currentGame ->
            currentGame?.let { game ->
                var newDisplayedTime = game.displayedTimeMillis
                if ((game.currentPhase == GamePhase.EXTRA_TIME_FIRST_HALF || game.currentPhase == GamePhase.EXTRA_TIME_SECOND_HALF) && !game.isTimerRunning) {
                    newDisplayedTime = (minutes * 60 * 1000L - game.actualTimeElapsedInPeriodMillis).coerceAtLeast(0L)
                }
                game.copy(
                    extraTimeHalfDurationMinutes = minutes,
                    displayedTimeMillis = newDisplayedTime,
                    lastUpdated = System.currentTimeMillis()
                ).also { updated ->
                    viewModelScope.launch { gameStorage.addOrUpdateGame(updated) }
                }
            }
        }
    }

    fun resetGame() {
        val originalGame = _activeGame.value ?: run {
            createNewDefaultGame()
            return
        }
        cancelTimer()

        val resetGame = Game(
            id = originalGame.id,
            gameDateTimeEpochMillis = originalGame.gameDateTimeEpochMillis
        ).copy(
            gameNumber = originalGame.gameNumber,
            fieldNumber = originalGame.fieldNumber,
            homeTeamName = originalGame.homeTeamName,
            awayTeamName = originalGame.awayTeamName,
            ageGroup = originalGame.ageGroup,
            halfDurationMinutes = originalGame.halfDurationMinutes,
            halftimeDurationMinutes = originalGame.halftimeDurationMinutes,
            hasExtraTime = originalGame.hasExtraTime,
            hasPenalties = originalGame.hasPenalties,
            penaltyKicksPerTeam = originalGame.penaltyKicksPerTeam,
            kickOffTeam = originalGame.kickOffTeam,
            displayedTimeMillis = Game().regulationPeriodDurationMillis(GamePhase.FIRST_HALF),
            currentPhase = GamePhase.NOT_STARTED,
        )
        _activeGame.value = resetGame
        viewModelScope.launch { gameStorage.addOrUpdateGame(resetGame) }
    }

    fun resetTimer() {
        val gameBeforeReset = _activeGame.value ?: return
        cancelTimer()
        _activeGame.update {
            it?.copy(
                displayedTimeMillis = gameBeforeReset.regulationPeriodDurationMillis(gameBeforeReset.currentPhase),
                actualTimeElapsedInPeriodMillis = 0L,
                lastUpdated = System.currentTimeMillis()
            )
        }
        val gameAfterReset = _activeGame.value ?: return
        val resetMessage = "Timer for the period ${gameAfterReset.currentPhase.readable()} has been reset."
        addEvent(GenericLogEvent(message = resetMessage, phase = gameAfterReset.currentPhase, gameTimeMillis = 0.0))

        gameTimerService?.configureTimerForGame(
            game = gameAfterReset,
            startImmediately = false
        ) 
    }

    fun recordPenaltyAttempt(scored: Boolean, kickerNumber: Int? = null) {
        val currentGame = _activeGame.value ?: return
        val taker = currentGame.kickOffTeam

        if (currentGame.currentPhase != GamePhase.PENALTIES) return
        vibrate(VibrationPattern.GOAL_SCORED)

        _activeGame.update { game ->
            game?.let {
                var newScoreHome = it.homeScore
                var newScoreAway = it.awayScore
                val newKickOffTeamForNext = it.kickOffTeam.opposite()
                var updatedPenaltiesTakenHome = it.penaltiesTakenHome
                var updatedPenaltiesTakenAway = it.penaltiesTakenAway

                if (taker == Team.HOME) {
                    updatedPenaltiesTakenHome++
                    if (scored) newScoreHome++
                } else {
                    updatedPenaltiesTakenAway++
                    if (scored) newScoreAway++
                }
                val penaltyEvent = PenaltyEvent(
                    team = taker,
                    kickerNumber = kickerNumber,
                    gameTimeMillis = currentElapsedInPeriodMillis().toDouble(),
                    homeScoreAtTime = newScoreHome,
                    awayScoreAtTime = newScoreAway,
                    scored = scored,
                    phase = it.currentPhase
                )

                var newPhase = it.currentPhase
                if (checkShootoutEndCondition(newScoreHome, newScoreAway, updatedPenaltiesTakenHome, updatedPenaltiesTakenAway, it.penaltyKicksPerTeam)) {
                    newPhase = GamePhase.GAME_ENDED
                }

                it.copy(
                    homeScore = newScoreHome,
                    awayScore = newScoreAway,
                    penaltiesTakenHome = updatedPenaltiesTakenHome,
                    penaltiesTakenAway = updatedPenaltiesTakenAway,
                    kickOffTeam = newKickOffTeamForNext,
                    currentPhase = newPhase
                ).addEvent(penaltyEvent)
            }
        }
    }


    private fun checkShootoutEndCondition(
        currentHomeScore: Int, currentAwayScore: Int,
        penaltiesTakenHome: Int, penaltiesTakenAway: Int,
        shootoutRoundLimit: Int = 5
    ): Boolean {
        // Clamp the per-team limit so a corrupt/legacy value never breaks the
        // shootout. Default FIFA rule is 5, but the game form lets the referee
        // pick anything in 1..20.
        val safeLimit = shootoutRoundLimit.coerceIn(1, 20)
        if (penaltiesTakenHome >= safeLimit && penaltiesTakenAway >= safeLimit) {
            return currentHomeScore != currentAwayScore && penaltiesTakenHome == penaltiesTakenAway
        } else {
            val kicksRemainingHome = safeLimit - penaltiesTakenHome
            val kicksRemainingAway = safeLimit - penaltiesTakenAway

            if (currentHomeScore > currentAwayScore + kicksRemainingAway) return true
            if (currentAwayScore > currentHomeScore + kicksRemainingHome) return true
            return false
        }
    }


    @RequiresPermission(Manifest.permission.VIBRATE)
    private fun vibrate(pattern: VibrationPattern) {
        if (vibrator?.hasVibrator() == true) {
            val effect = when (pattern) {
                VibrationPattern.ADDED_TIME_REMINDER -> VibrationEffect.createWaveform(
                    longArrayOf(0, 150, 50, 150, 450, 150, 50, 150),
                    intArrayOf(0, VibrationEffect.DEFAULT_AMPLITUDE, 0, VibrationEffect.DEFAULT_AMPLITUDE, 0, VibrationEffect.DEFAULT_AMPLITUDE, 0, VibrationEffect.DEFAULT_AMPLITUDE),
                    0
                )
                VibrationPattern.GOAL_SCORED -> VibrationEffect.createWaveform(longArrayOf(0, 150, 50, 150, 50), -1)
                VibrationPattern.GENERIC_EVENT -> VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE)
                VibrationPattern.GAME_END -> VibrationEffect.createWaveform(longArrayOf(0, 500, 300, 500, 300, 500), -1)
            VibrationPattern.HALF_TIME_END -> VibrationEffect.createOneShot(300, 150)
            }
            vibrator.vibrate(effect)
        }
    }

    private fun vibrateKickoffTick() {
        if (vibrator?.hasVibrator() == true) {
            vibrator.vibrate(VibrationEffect.createOneShot(90, 80))
        }
    }

    private fun vibrateKickoffGo() {
        if (vibrator?.hasVibrator() == true) {
            vibrator.vibrate(VibrationEffect.createOneShot(320, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    enum class VibrationPattern {
        ADDED_TIME_REMINDER, GOAL_SCORED, GENERIC_EVENT, GAME_END, HALF_TIME_END
    }

}
