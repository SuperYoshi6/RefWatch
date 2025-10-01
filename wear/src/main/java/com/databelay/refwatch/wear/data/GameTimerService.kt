package com.databelay.refwatch.wear.data // Or your correct package

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
import android.os.Binder
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status
import com.databelay.refwatch.R
import com.databelay.refwatch.common.Game
import com.databelay.refwatch.common.GamePhase
import com.databelay.refwatch.common.GameStatus
import com.databelay.refwatch.common.formatTime
import com.databelay.refwatch.common.hasTimer
import com.databelay.refwatch.common.readable
import com.databelay.refwatch.wear.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Define your constants for notification
private const val ONGOING_NOTIFICATION_ID_SERVICE = 2 // Different from OngoingActivity's ID
private const val ONGOING_NOTIFICATION_CHANNEL_ID = "RefWatchGameTimerChannel" // Ensure this channel is created
const val ONGOING_NOTIFICATION_CHANNEL_NAME = "RefWatch Timer"

const val ONGOING_NOTIFICATION_ID_VM = 123 // ID for the notification that drives OngoingActivity
const val COUNTDOWN_INTERVAL_MS = 1000L
const val MAX_ADDED_TIME_COUNTUP_DURATION = 1000L*60*60 // one hour in milliseconds
// Data class for timer updates
data class TimerState(
    val actualTimeElapsedInPeriodMillis: Long = 0L,
    val isTimerRunning: Boolean = false, // Service's knowledge: is the CountDownTimer ticking?
    val currentPhase: GamePhase = GamePhase.NOT_STARTED,
    val regulationPeriodDurationMillis: Long = 0L,
    val displayedMillis: Long = 0L,
    val inAddedTime: Boolean = false
)


class GameTimerService : Service() {
    private val TAG = "GameTimerService"
    private val binder = LocalBinder()
    private lateinit var powerManager: PowerManager
    private lateinit var notificationManager: NotificationManager

    private var wakeLock: PowerManager.WakeLock? = null

    private var gameCountDownTimer: CountDownTimer? = null
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob) // Use Dispatchers.Main for CountDownTimer

    // --- StateFlow for communication with ViewModel ---
    private val _timerStateFlow = MutableStateFlow(TimerState())
    val timerStateFlow: StateFlow<TimerState> = _timerStateFlow.asStateFlow()

    // To hold the full game state or relevant parts passed from ViewModel
    private var currentInternalGame: Game? = null
    private var timeTickerStartedSystemTime = 0L // SystemClock.elapsedRealtime() when a ticker starts
    private var initialMillisForCurrentTicker = 0L // The millisInFuture the current ticker was started with

    inner class LocalBinder : Binder() {
        fun getService(): GameTimerService = this@GameTimerService
    }

    override fun onCreate() {
        super.onCreate()
        powerManager = getSystemService(POWER_SERVICE) as PowerManager
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager // Initialize here
        createNotificationChannel()
        Log.d(TAG, "Service Created.")
    }
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            ONGOING_NOTIFICATION_CHANNEL_ID,
            ONGOING_NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW 
        ).apply {
            description = "Shows the current game timer"
            setSound(null, null)
            enableVibration(false)
        }
        notificationManager.createNotificationChannel(channel)
        Log.d(TAG, "Notification channel created: $ONGOING_NOTIFICATION_CHANNEL_ID")
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    @SuppressLint("WakelockTimeout")
    private fun acquireWakeLock() {
        if (wakeLock?.isHeld != true) {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "RefWatch::GameTimerWakeLockTag").apply {
                setReferenceCounted(false)
                acquire() 
            }
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        wakeLock = null
    }

    fun configureTimerForGame(game: Game, startImmediately: Boolean) {
        serviceScope.launch { 
/*            Log.d(TAG, "Service configuring for game: ${game.id}, phase: ${game.currentPhase}, startImmediately: $startImmediately")
            currentInternalGame = game
            val initialElapsed = game.actualTimeElapsedInPeriodMillis
            val regulationDuration = game.regulationPeriodDurationMillis()
            val isAdded = game.currentPhase.hasTimer() && initialElapsed >= regulationDuration
            val displayedMillis = if (isAdded) {initialElapsed - regulationDuration} else {regulationDuration - initialElapsed}
            val initialIsTimerRunning = startImmediately && (game.currentPhase.hasTimer())

            _timerStateFlow.update {
                it.copy(
                    currentPhase = game.currentPhase,
                    isTimerRunning = initialIsTimerRunning,
                    displayedMillis = displayedMillis,
                    actualTimeElapsedInPeriodMillis = initialElapsed,
                    inAddedTime = isAdded,
                    regulationPeriodDurationMillis = game.regulationPeriodDurationMillis(),
                )
            }*/
            Log.d(TAG, "Service configuring for game: ${game.id}, phase: ${game.currentPhase}, startImmediately: $startImmediately, incomingElapsed: ${game.actualTimeElapsedInPeriodMillis}")
            currentInternalGame = game // Store the game state from ViewModel

            val initialElapsedForThisConfig: Long
            val isAddedTimeForThisConfig: Boolean
            val displayedMillisForThisConfig: Long
            val currentRegulationDuration = game.regulationPeriodDurationMillis()

            if (startImmediately) {
                // Logic for when timer should start or resume
                initialElapsedForThisConfig = game.actualTimeElapsedInPeriodMillis // Use what VM says
                isAddedTimeForThisConfig = game.currentPhase.hasTimer() && initialElapsedForThisConfig >= currentRegulationDuration
                displayedMillisForThisConfig = if (isAddedTimeForThisConfig) {
                    initialElapsedForThisConfig - currentRegulationDuration
                } else {
                    currentRegulationDuration - initialElapsedForThisConfig
                }
                // Ensure timer starts if startImmediately is true
                if (game.currentPhase.hasTimer()) {
                    startGameTimer(game, initialElapsedForThisConfig, isAddedTimeForThisConfig)
                }
            } else {
                // Timer SHOULD NOT be running. Reset elapsed time for this new period.
                // This is crucial for phases like SECOND_HALF starting.
                Log.d(TAG, "configureTimerForGame: Timer not starting immediately for ${game.currentPhase}. Resetting elapsed time for service state.")
                initialElapsedForThisConfig = 0L // <<<< RESETTING SERVICE'S VIEW
                isAddedTimeForThisConfig = false
                displayedMillisForThisConfig = currentRegulationDuration // Show full duration remaining

                // Explicitly stop any existing countdown timer in the service
                gameCountDownTimer?.cancel()
                // The isTimerRunning in the flow will be updated by the _timerStateFlow.update below
            }

            _timerStateFlow.update {
                it.copy(
                    currentPhase = game.currentPhase,
                    isTimerRunning = startImmediately && game.currentPhase.hasTimer(), // Service's knowledge
                    actualTimeElapsedInPeriodMillis = initialElapsedForThisConfig,
                    inAddedTime = isAddedTimeForThisConfig,
                    regulationPeriodDurationMillis = currentRegulationDuration,
                    displayedMillis = displayedMillisForThisConfig
                )
            }
            Log.d(TAG, "Service _timerStateFlow updated: running=${_timerStateFlow.value.isTimerRunning}, elapsed=${_timerStateFlow.value.actualTimeElapsedInPeriodMillis}")


            // ------------ Ongoing Activity update logic ---------------
            val ongoingActivityText: String
            val makeOngoing: Boolean

            if (game.currentPhase == GamePhase.PRE_GAME) {
                ongoingActivityText = "Pre-Game: ${game.homeTeamName} vs ${game.awayTeamName}"
                makeOngoing = true 
                // Ensure service is foreground for PRE_GAME ongoing activity
                if (canPostNotifications()) {
                     startForeground(
                         ONGOING_NOTIFICATION_ID_SERVICE,
                         createServiceNotification("Pre-Game Setup"),
                         FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                         )
                     Log.d(TAG, "Service brought to foreground for PRE_GAME.")
                } else {
                    Log.w(TAG, "Cannot start foreground for PRE_GAME, notifications disabled.")
                }
            } else if (startImmediately && game.currentPhase.hasTimer()) {
                ongoingActivityText = _timerStateFlow.value.displayedMillis.formatTime(_timerStateFlow.value.inAddedTime)
                makeOngoing = true
            } else {
                ongoingActivityText = "Ready: ${game.currentPhase.readable()}"
                makeOngoing = _timerStateFlow.value.isTimerRunning // Only ongoing if timer was already running and we are reconfiguring
            }
            updateNotificationAndOngoingActivity(ongoingActivityText, isOngoing = makeOngoing)

            if (startImmediately && game.currentPhase.hasTimer()) {
                startGameTimer(game, _timerStateFlow.value.actualTimeElapsedInPeriodMillis, _timerStateFlow.value.inAddedTime)
            } else if (_timerStateFlow.value.isTimerRunning && !(startImmediately && (game.currentPhase.hasTimer()))) {
                // This case handles if the service was running a timer for a previous configuration,
                // and the new configuration does not start immediately.
                Log.w(TAG, "Configuring for new game/phase, but timer was running and new config not starting immediately. Pausing timer.")
                pauseGameTimerInternally("Timer Stopped")
            }
        }
    }


    private fun canPostNotifications(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        }
        return true 
    }

    fun startGameTimer(
        game: Game,
        elapsedMillisAtActivation: Long = 0L, // How much time had ALREADY elapsed in this period (or reg duration if starting added time)
        isInAddedTimeInitially: Boolean = false

    ) {
        Log.d(TAG, "SERVICE startGameTimer: Phase: ${game.currentPhase}, " +
                "elapsedMillisAtActivation: $elapsedMillisAtActivation, " + // <<< THIS IS THE CRITICAL VALUE
                "isInAddedTimeInitially: $isInAddedTimeInitially")
        serviceScope.launch {
            // ... (acquireWakeLock, startForeground) ...

            val currentStateBeforeStart = _timerStateFlow.value
            val currentRegulationDuration = game.regulationPeriodDurationMillis(currentStateBeforeStart.currentPhase) // Use phase from viewmodel if more current

            _timerStateFlow.update {
                it.copy(
                    isTimerRunning = true,
                    currentPhase = game.currentPhase, // Ensure phase is updated
                    regulationPeriodDurationMillis = currentRegulationDuration,
                    // actualTimeElapsedInPeriodMillis will be updated in onTick.
                    // displayedMillis will be updated in onTick.
                    inAddedTime = isInAddedTimeInitially
                )
            }

            // Cancel any existing timer defensively
            gameCountDownTimer?.cancel()

            initialMillisForCurrentTicker = if (isInAddedTimeInitially) {
                MAX_ADDED_TIME_COUNTUP_DURATION // Ticker counts "up" by counting down from a large number
            } else {
                // Time remaining in the regulation period
                val remainingInRegulation = currentRegulationDuration - elapsedMillisAtActivation
                if (remainingInRegulation <= 0) { // Should not happen if logic is correct, but handle
                    Log.w(TAG, "Attempting to start timer with 0 or negative time remaining in regulation. Finishing period.")
                    // For now, just don't start the timer and update state.
                    _timerStateFlow.update { it.copy(isTimerRunning = false, actualTimeElapsedInPeriodMillis = currentRegulationDuration, displayedMillis = 0) }
                    onTimerFinishActions(game.currentPhase) // Call a method that handles transitions
                    return@launch
                }
                remainingInRegulation
            }

            if (initialMillisForCurrentTicker <= 0 && !isInAddedTimeInitially) {
                Log.e(TAG, "Error: initialMillisForCurrentTicker is zero or negative for regulation time. Cannot start timer.")
                _timerStateFlow.update { it.copy(isTimerRunning = false) }
                releaseWakeLock()
                stopForegroundSafely("Timer Error")
                return@launch
            }


            Log.d(TAG, "Starting CountdownTimer. For Phase: ${game.currentPhase}, Initial Ticker ms: $initialMillisForCurrentTicker, ElapsedAtActivation: $elapsedMillisAtActivation, IsInAddedTime: $isInAddedTimeInitially, RegDuration: $currentRegulationDuration")

            timeTickerStartedSystemTime = SystemClock.elapsedRealtime()

            gameCountDownTimer = object : CountDownTimer(initialMillisForCurrentTicker, COUNTDOWN_INTERVAL_MS) {
                override fun onTick(millisUntilFinished: Long) {
                    val currentTimerState = _timerStateFlow.value // Get latest state
                    val timeThisTickerHasRun = initialMillisForCurrentTicker - millisUntilFinished

                    val newActualElapsed: Long
                    val newDisplayedMillis: Long

                    if (currentTimerState.inAddedTime) {
                        // elapsedMillisAtActivation should be currentRegulationDuration when added time starts
                        newActualElapsed = elapsedMillisAtActivation + timeThisTickerHasRun
                        newDisplayedMillis = timeThisTickerHasRun // Display shows time *into* added time
                    } else {
                        // In regulation time
                        newActualElapsed = elapsedMillisAtActivation + timeThisTickerHasRun
                        newDisplayedMillis = currentRegulationDuration - newActualElapsed // Display shows time remaining in regulation
                    }

                    _timerStateFlow.update {
                        it.copy(
                            actualTimeElapsedInPeriodMillis = newActualElapsed,
                            displayedMillis = newDisplayedMillis
                            // inAddedTime is managed by phase transitions or explicit setting
                        )
                    }
                    updateNotificationAndOngoingActivity(_timerStateFlow.value.displayedMillis.formatTime(currentTimerState.inAddedTime), isOngoing = true)
                }

                override fun onFinish() {
                    Log.d(TAG, "CountDownTimer finished. Phase was: ${_timerStateFlow.value.currentPhase}")
                    val finishedState = _timerStateFlow.value
                    // Ensure actualTimeElapsed is set to the full duration if it was regulation time finishing
                    if (!finishedState.inAddedTime) {
                        _timerStateFlow.update {
                            it.copy(
                                actualTimeElapsedInPeriodMillis = finishedState.regulationPeriodDurationMillis,
                                displayedMillis = 0L, // Show 0 when regulation period finishes
                                isTimerRunning = false // Will be set true if next phase has timer
                            )
                        }
                    } else {
                        // If added time ticker finishes (e.g. if MAX_ADDED_TIME_COUNTUP_DURATION wasn't Long.MAX_VALUE)
                        // The actualTimeElapsed and displayedMillis would be based on the last tick.
                        // This scenario (added time ticker actually finishing) needs careful thought
                        // if you don't use Long.MAX_VALUE. For now, assume it runs "forever".
                        _timerStateFlow.update { it.copy(isTimerRunning = false) }
                    }
                    // Call a method to handle phase transitions, etc.
                    onTimerFinishActions(finishedState.currentPhase)
                }
            }.start()
        }
    }
/*
    fun startGameTimer(game: Game, initialElapsedMillis: Long = 0L, isAddedTimeInitially: Boolean = false) {
        serviceScope.launch {
            if (_timerStateFlow.value.isTimerRunning && gameCountDownTimer != null) {
                // If configuring for a new phase/game and timer is already running for the current game, let it continue or be reconfigured by new call.
                // If it's for a genuinely different game instance, it should have been stopped before.
                // This check is more about avoiding multiple concurrent countdown timers for the same logical session.
                if (currentInternalGame?.id != game.id || currentInternalGame?.currentPhase != game.currentPhase) {
                    gameCountDownTimer?.cancel()
                }
            } else if (_timerStateFlow.value.isTimerRunning) {
                 gameCountDownTimer?.cancel() // Cancel if running without a timer object reference (should not happen)
            }

            currentInternalGame = game
            acquireWakeLock()
            val regulationDuration = game.regulationPeriodDurationMillis()

            _timerStateFlow.update {
                it.copy(
                    currentPhase = game.currentPhase,
                    isTimerRunning = true,
                    actualTimeElapsedInPeriodMillis = initialElapsedMillis,
                    inAddedTime = isAddedTimeInitially,
                    displayedMillis = if (isAddedTimeInitially) initialElapsedMillis else regulationDuration - initialElapsedMillis,
                    regulationPeriodDurationMillis = regulationDuration,
                )
            }
            val notificationText = _timerStateFlow.value.displayedMillis.formatTime()
            startForeground(ONGOING_NOTIFICATION_ID_SERVICE, createServiceNotification(notificationText))
            updateNotificationAndOngoingActivity(notificationText, isOngoing = true)
            Log.d(TAG, "GameTimerService brought to foreground explicitly for timer start.")

            val timeToCountFrom = Long.MAX_VALUE
            gameCountDownTimer = object : CountDownTimer(timeToCountFrom, COUNTDOWN_INTERVAL_MS) {
                override fun onTick(millisUntilFinished_unused: Long) {
                    if (!(_timerStateFlow.value.isTimerRunning)) {
                        this.cancel()
                        return
                    }
                    _timerStateFlow.update { currentState ->
                        val newElapsed = currentState.actualTimeElapsedInPeriodMillis + COUNTDOWN_INTERVAL_MS
                        val currentRegulationDuration = currentState.regulationPeriodDurationMillis
                        val isInAddedTimeNow = newElapsed >= currentRegulationDuration
                        val displayValue = if (isInAddedTimeNow) {
                            newElapsed - currentRegulationDuration
                        } else {
                            currentRegulationDuration - newElapsed
                        }
                        currentState.copy(
                            actualTimeElapsedInPeriodMillis = newElapsed,
                            displayedMillis = displayValue,
                            inAddedTime = isInAddedTimeNow
                        )
                    }
                    updateNotificationAndOngoingActivity(_timerStateFlow.value.displayedMillis.formatTime(), isOngoing = true)
                }

                override fun onFinish() {
                    Log.w(TAG, "CountDownTimer finished unexpectedly!")
                    _timerStateFlow.update { it.copy(isTimerRunning = false) }
                    releaseWakeLock()
                    stopForegroundSafely("Timer Finished Unexpectedly")
                }
            }.start()
        }
    }
*/



    private fun onTimerFinishActions(finishedPhase: GamePhase) {
        serviceScope.launch {
            Log.d(TAG, "onTimerFinishActions for phase: ${finishedPhase.readable()}")

            val gameBeforeTransition = currentInternalGame // Get the game state before any changes
            if (gameBeforeTransition == null) {
                Log.e(TAG, "onTimerFinishActions: currentInternalGame is null. Cannot proceed.")
                _timerStateFlow.update { it.copy(isTimerRunning = false) } // Ensure timer is marked as stopped
                updateNotificationAndOngoingActivity("Error", isOngoing = false)
                releaseWakeLock()
                stopForegroundSafely("Error: No game state")
                return@launch
            }

            val regulationDurationOfFinishedPhase = gameBeforeTransition.regulationPeriodDurationMillis(finishedPhase)

            // Determine if we should transition to added time for this phase
            val shouldTransitionToAddedTime = finishedPhase.hasTimer()
            if (shouldTransitionToAddedTime) {
                Log.i(TAG, "Regulation time for ${finishedPhase.readable()} ended. Transitioning to ADDED TIME.")
                // No explicit phase change needed in 'Game' object for 'inAddedTime' state.
                // The 'inAddedTime' flag in TimerState and Game object will handle this.
                // currentInternalGame is already up-to-date.

                // Ensure the state reflects that regulation is fully elapsed.
                _timerStateFlow.update {
                    it.copy(
                        isTimerRunning = true, // Will be set by startGameTimer
                        // currentPhase remains the same (e.g. FIRST_HALF), but inAddedTime becomes true
                        actualTimeElapsedInPeriodMillis = regulationDurationOfFinishedPhase,
                        inAddedTime = true,
                        // displayedMillis will be 0 for start of added time, handled by startGameTimer's onTick
                        regulationPeriodDurationMillis = regulationDurationOfFinishedPhase // Keep this consistent
                    )
                }
                // Now start the timer for added time.
                // For added time, elapsedMillisAtActivation should be the full duration of the regulation period that just ended.
                startGameTimer(
                    gameBeforeTransition,
                    regulationDurationOfFinishedPhase,
                    true // isInAddedTimeInitially = true
                )
            } else {
                // This phase does not transition to added time (e.g., a half-time countdown finished, or added time itself finished)
                // Or it's a phase that doesn't have a timer that leads to added time.
                Log.i(TAG, "${finishedPhase.readable()} timer finished. No transition to added time for this phase. Determining next logical phase.")

                // For now, just stop the timer and update UI as "finished" or "paused" for the current phase.
                // The _timerStateFlow was already updated in onFinish to set isTimerRunning = false.
                val finalState = _timerStateFlow.value
                updateNotificationAndOngoingActivity(
                    finalState.displayedMillis.formatTime(finalState.inAddedTime),
                    // isOngoing should be true if the game session is still active (e.g. waiting for user to start next half)
                    // or if it's a break phase like HALF_TIME that has its own ongoing presence.
                    isOngoing = gameBeforeTransition.status == GameStatus.IN_PROGRESS // A simple check
                )

                // If no timer auto-starts for a next phase, and current phase doesn't keep service alive on its own when timer is off
                if (!finalState.isTimerRunning && gameBeforeTransition.status != GameStatus.IN_PROGRESS) {
                    Log.d(TAG, "onTimerFinishActions: Releasing wakelock and stopping foreground as phase ${finalState.currentPhase} is not keeping service alive without a timer.")
                    releaseWakeLock()
                    stopForegroundSafely("Timer Finished for ${finalState.currentPhase.readable()}")
                } else {
                    Log.d(TAG, "onTimerFinishActions: Timer finished for ${finalState.currentPhase}. State: isRunning=${finalState.isTimerRunning}, isOngoing set based on game status.")
                }
            }
        }
    }

    private fun pauseGameTimerInternally(notificationText: String) {
        Log.i(TAG, "Pausing timer. Current elapsed before pause: ${_timerStateFlow.value.actualTimeElapsedInPeriodMillis}") // <<< ADD THIS LOG
        _timerStateFlow.update { it.copy(isTimerRunning = false) }
        gameCountDownTimer?.cancel() 
        updateNotificationAndOngoingActivity(notificationText, isOngoing = false) 
        Log.d(TAG, "Timer paused internally. Notification: $notificationText")
        Log.i(TAG, "Timer paused. State after update: isRunning=${_timerStateFlow.value.isTimerRunning}, Elapsed=${_timerStateFlow.value.actualTimeElapsedInPeriodMillis}") // <<< ADD THIS LOG
    }

    fun pauseGameTimer(updateNotificationText: String? = null) {
        serviceScope.launch {
            if (!_timerStateFlow.value.isTimerRunning) {
                Log.d(TAG, "pauseGameTimer called, but timer was not running.")
                return@launch
            }
            val textForNotification = updateNotificationText ?: "Paused: ${_timerStateFlow.value.displayedMillis.formatTime(_timerStateFlow.value.inAddedTime)}"
            pauseGameTimerInternally(textForNotification)
        }
    }

    fun resumeGameTimer(game: Game) {
        Log.d(TAG, "resumeGameTimer called.")
        serviceScope.launch {
            Log.i(TAG, "SERVICE resumeGameTimer: Received Game from VM. Phase: ${game.currentPhase}, " +
                    "VM's Elapsed: ${game.actualTimeElapsedInPeriodMillis}, VM's InAddedTime: ${game.inAddedTime}")

            val currentState = _timerStateFlow.value
            if (currentState.isTimerRunning) {
                Log.w(TAG, "resumeGameTimer called, but timer is already running.")
                return@launch
            }
            if (!game.currentPhase.hasTimer()) {
                Log.w(TAG, "Attempted to resume timer in non-timed phase: ${game.currentPhase}. Not resuming.")
                _timerStateFlow.update { it.copy(isTimerRunning = false, currentPhase = game.currentPhase,
                    actualTimeElapsedInPeriodMillis = game.actualTimeElapsedInPeriodMillis) } // Keep elapsed consistent with VM
                if (currentState.currentPhase == GamePhase.PRE_GAME) {
                     updateNotificationAndOngoingActivity("${game.currentPhase.readable()}: Ready", isOngoing = true)
                } else {
                     updateNotificationAndOngoingActivity("${game.currentPhase.readable()}: Paused", isOngoing = false)
                }
                return@launch
            }
            // --- Critical Decision Point ---
            // The ViewModel is explicitly asking to resume. It provides the 'gameFromViewModel'
            // which includes the elapsed time it believes is correct for resuming.
            // We should trust the elapsed time from gameFromViewModel for the resume operation.

            currentInternalGame = game // Update the service's idea of the game

            // Before calling startGameTimer, ensure the service's _timerStateFlow
            // reflects the state we are about to resume from, especially if startGameTimer
            // or its onTick reads from _timerStateFlow.value for things like regulationDuration.
            // This is a "pre-synchronization" step.
            val regulationDuration = game.regulationPeriodDurationMillis(game.currentPhase)
            val isInAddedTime = game.actualTimeElapsedInPeriodMillis >= regulationDuration && game.currentPhase.hasTimer() // Re-evaluate based on VM data

            _timerStateFlow.update {
                it.copy(
                    currentPhase = game.currentPhase,
                    isTimerRunning = false, // Will be set to true by startGameTimer
                    actualTimeElapsedInPeriodMillis = game.actualTimeElapsedInPeriodMillis,
                    inAddedTime = isInAddedTime,
                    regulationPeriodDurationMillis = regulationDuration,
                    // displayedMillis will be recalculated by startGameTimer or its first tick
                    displayedMillis = if (isInAddedTime) game.actualTimeElapsedInPeriodMillis - regulationDuration else regulationDuration - game.actualTimeElapsedInPeriodMillis
                )
            }
            Log.i(TAG, "SERVICE resumeGameTimer: Pre-synchronized _timerStateFlow with VM data. Effective elapsed for timer: ${game.actualTimeElapsedInPeriodMillis}")

            startGameTimer(
                game, // Pass the authoritative game object from VM
                game.actualTimeElapsedInPeriodMillis, // <<< USE THE CORRECT VALUE FROM THE VM
                isInAddedTime // Use the re-evaluated inAddedTime based on VM data
            )
            Log.d(TAG, "Timer instructed to resume for ${game.currentPhase} from VM's elapsed time.")

        }
    }

    fun stopGameTimerAndSession() {
        serviceScope.launch {
            Log.i(TAG, "Stopping game timer and session.")
            _timerStateFlow.update { it.copy(isTimerRunning = false, currentPhase = GamePhase.GAME_ENDED) }
            gameCountDownTimer?.cancel()
            gameCountDownTimer = null
            releaseWakeLock()
            stopForegroundSafely("Game Ended") 
            Log.d(TAG, "Ongoing Activity should be cancelled by stopForegroundSafely.")
        }
    }

    fun commandStopGameSessionAndCleanup(onCleanupComplete: () -> Unit) {
        serviceScope.launch {
            Log.i(TAG, "COMMAND: Stop Game Session & Cleanup. Releasing WakeLock.")
            gameCountDownTimer?.cancel()
            gameCountDownTimer = null

            _timerStateFlow.update {
                it.copy(
                    isTimerRunning = false,
                    displayedMillis = 0L, 
                    actualTimeElapsedInPeriodMillis = 0L,
                    currentPhase = GamePhase.GAME_ENDED 
                )
            }
            releaseWakeLock() 
            updateNotificationAndOngoingActivity("Game Session Ended", isOngoing = false) // Explicitly set isOngoing to false
            stopForegroundSafely()
            currentInternalGame = null // Clear the game state
            onCleanupComplete()
        }
    }

    private fun stopForegroundSafely(notificationTextIfLingering: String? = null) {
        Log.d(TAG, "stopForegroundSafely called.")
        stopForeground(STOP_FOREGROUND_REMOVE)
        if (notificationTextIfLingering != null && canPostNotifications()) {
            val finalNotification = NotificationCompat.Builder(this, ONGOING_NOTIFICATION_CHANNEL_ID)
                .setContentTitle("RefWatch")
                .setContentText(notificationTextIfLingering)
                .setSmallIcon(R.drawable.ic_stat_refwatch)
                .setAutoCancel(true) 
                .build()
            notificationManager.notify(ONGOING_NOTIFICATION_ID_SERVICE + 100, finalNotification) 
        }
        if (canPostNotifications()) {
            notificationManager.cancel(ONGOING_NOTIFICATION_ID_VM)
            Log.d(TAG, "In stopForegroundSafely, cancelled notification (ID: $ONGOING_NOTIFICATION_ID_VM) for OngoingActivity.")
        }
        Log.i(TAG, "Service stopped foreground state.")
    }

      private fun createServiceNotification(contentText: String): Notification {
        val activityIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, ONGOING_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_refwatch) 
            .setContentTitle("RefWatch") // Simplified title for service notification
            .setContentText(contentText)
            .setCategory(NotificationCompat.CATEGORY_SERVICE) 
            .setOngoing(true) 
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent) // Main pending intent for the service notification
            .build()
    }

    // Updated to handle general status text and OngoingActivity more flexibly
    private fun updateNotificationAndOngoingActivity(statusText: String, isOngoing: Boolean) {
        if (!canPostNotifications()) {
            Log.w(TAG, "Cannot post notifications, permission denied or not available.")
            return
        }

        // Update the service's own foreground notification (if it's running in foreground)
        // This notification (ID_SERVICE) is primarily for keeping the service alive.
        // Its content can be simpler than the OngoingActivity's notification.
        val serviceNotificationText = if (_timerStateFlow.value.isTimerRunning) {
            _timerStateFlow.value.displayedMillis.formatTime(_timerStateFlow.value.inAddedTime)
        } else {
            currentInternalGame?.currentPhase?.readable() ?: statusText
        }
        // Only call startForeground if we intend for the service to be in foreground, 
        // e.g. timer running or in PRE_GAME state.
        // The call to startForeground is now in configureTimerForGame (for PRE_GAME) 
        // and startGameTimer (for active timer).
        notificationManager.notify(ONGOING_NOTIFICATION_ID_SERVICE, createServiceNotification(serviceNotificationText))


        // Logic for the Notification that is also an Ongoing Activity (ID_VM)
        if (isOngoing && currentInternalGame != null) {
            val ongoingActivityStatus = Status.Builder()
                .addTemplate(statusText) // Use the provided statusText directly for the OA template
                .build()

            val oaNotificationBuilder = NotificationCompat.Builder(this, ONGOING_NOTIFICATION_CHANNEL_ID) // Use same channel
                .setSmallIcon(R.drawable.ic_stat_refwatch) // Consider a distinct icon for OA vs service
                .setContentTitle(currentInternalGame?.homeTeamName + " vs " + currentInternalGame?.awayTeamName) // Game Title
                .setContentText(statusText) // Main status text (e.g., "Pre-Game", "05:34 - First Half")
                .setCategory(NotificationCompat.CATEGORY_EVENT) 
                .setOngoing(true)
                .setOnlyAlertOnce(true)

            val ongoingActivity = OngoingActivity.Builder(applicationContext, ONGOING_NOTIFICATION_ID_VM, oaNotificationBuilder)
                .setStaticIcon(R.drawable.ic_stat_refwatch)
                .setTouchIntent(
                    PendingIntent.getActivity(
                        this, 0, Intent(this, MainActivity::class.java),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
                .setStatus(ongoingActivityStatus)
                .build()
            ongoingActivity.apply(applicationContext) 

            notificationManager.notify(ONGOING_NOTIFICATION_ID_VM, oaNotificationBuilder.build())
//            Log.d(TAG, "Ongoing Activity notification posted/updated: ID $ONGOING_NOTIFICATION_ID_VM, Text: $statusText")

        } else {
            // Keeps game in FG even if timer isn't running
//            notificationManager.cancel(ONGOING_NOTIFICATION_ID_VM)
            Log.d(TAG, "Ongoing Activity notification cancelled (ID: $ONGOING_NOTIFICATION_ID_VM) as isOngoing=false or no current game.")
        }
    }

    fun commandStartGameSessionAndTimer(game: Game, initialElapsedMillis: Long = 0L) {
        serviceScope.launch {
            currentInternalGame = game
            Log.i(TAG, "COMMAND: Start Game Session & Timer for ${game.currentPhase}. Acquiring WakeLock.")
            acquireWakeLock() 

            _timerStateFlow.update {
                it.copy(
                    currentPhase = game.currentPhase,
                    isTimerRunning = false, // Will be set true by startGameTimer
                    actualTimeElapsedInPeriodMillis = initialElapsedMillis,
                    inAddedTime = false,
                    displayedMillis = game.regulationPeriodDurationMillis() - initialElapsedMillis,
                    regulationPeriodDurationMillis = game.regulationPeriodDurationMillis()
                )
            }
             // startGameTimer will call startForeground and updateNotificationAndOngoingActivity
            if (game.currentPhase.hasTimer()) {
                 startGameTimer(game, initialElapsedMillis, false)
            } else {
                Log.w(TAG, "Commanded to start timer for non-timed phase: ${game.currentPhase}")
                // Still ensure ongoing activity reflects current state if it's PRE_GAME or similar
                 updateNotificationAndOngoingActivity("${game.currentPhase.readable()}: Ready", isOngoing = game.currentPhase == GamePhase.PRE_GAME)
                 if(game.currentPhase == GamePhase.PRE_GAME && !isForegroundServiceRunning()) {
                     startForeground(ONGOING_NOTIFICATION_ID_SERVICE, createServiceNotification("${game.currentPhase.readable()}"))
                 }
            }
        }
    }
    private fun isForegroundServiceRunning(): Boolean {
        // A simple check based on wakeLock or a dedicated flag could be used.
        // For a more robust check, you might need to inspect ActivityManager, but that's heavier.
        return wakeLock?.isHeld == true // Assuming wakelock is held when foreground service is active with a timer
    }

    override fun onDestroy() {
        Log.d(TAG, "Service Destroyed.")
        releaseWakeLock() 
        gameCountDownTimer?.cancel()
        serviceJob.cancel() 
        if (canPostNotifications()) {
            notificationManager.cancel(ONGOING_NOTIFICATION_ID_VM)
            Log.d(TAG, "In onDestroy, cancelled notification (ID: $ONGOING_NOTIFICATION_ID_VM) for OngoingActivity.")
        }
        super.onDestroy()
    }
}
