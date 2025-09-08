package com.databelay.refwatch.wear.data // Or your correct package

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status
import com.databelay.refwatch.R
import com.databelay.refwatch.common.Game // Assuming you need parts of Game state
import com.databelay.refwatch.common.GamePhase
import com.databelay.refwatch.common.formatTime
import com.databelay.refwatch.common.hasTimer
import com.databelay.refwatch.common.isPlayablePhase
import com.databelay.refwatch.common.readable
import com.databelay.refwatch.wear.MainActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// Define your constants for notification
private const val ONGOING_NOTIFICATION_ID_SERVICE = 2 // Different from OngoingActivity's ID
private const val ONGOING_NOTIFICATION_CHANNEL_ID = "RefWatchGameTimerChannel" // Ensure this channel is created
const val ONGOING_NOTIFICATION_CHANNEL_NAME = "RefWatch Timer"

const val ONGOING_NOTIFICATION_ID_VM = 123 // ID for the notification that drives OngoingActivity
const val COUNTDOWN_INTERVAL_MS = 1000L
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
            Log.d(TAG, "Service configuring for game: ${game.id}, phase: ${game.currentPhase}, startImmediately: $startImmediately")
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
            }

            val ongoingActivityText: String
            val makeOngoing: Boolean

            if (game.currentPhase == GamePhase.PRE_GAME) {
                ongoingActivityText = "Pre-Game: ${game.homeTeamName} vs ${game.awayTeamName}"
                makeOngoing = true 
                // Ensure service is foreground for PRE_GAME ongoing activity
                if (canPostNotifications()) {
                     startForeground(ONGOING_NOTIFICATION_ID_SERVICE, createServiceNotification("Pre-Game Setup"))
                     Log.d(TAG, "Service brought to foreground for PRE_GAME.")
                } else {
                    Log.w(TAG, "Cannot start foreground for PRE_GAME, notifications disabled.")
                }
            } else if (startImmediately && game.currentPhase.hasTimer()) {
                ongoingActivityText = _timerStateFlow.value.displayedMillis.formatTime()
                makeOngoing = true
            } else {
                ongoingActivityText = "Ready: ${game.currentPhase.readable()}"
                makeOngoing = _timerStateFlow.value.isTimerRunning // Only ongoing if timer was already running and we are reconfiguring
            }
            updateNotificationAndOngoingActivity(ongoingActivityText, isOngoing = makeOngoing)

            if (startImmediately && game.currentPhase.hasTimer()) {
                startGameTimer(game, _timerStateFlow.value.actualTimeElapsedInPeriodMillis, _timerStateFlow.value.inAddedTime)
            } else if (_timerStateFlow.value.isTimerRunning && !initialIsTimerRunning) {
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

    private fun pauseGameTimerInternally(notificationText: String) {
        _timerStateFlow.update { it.copy(isTimerRunning = false) }
        gameCountDownTimer?.cancel() 
        updateNotificationAndOngoingActivity(notificationText, isOngoing = false) 
        Log.d(TAG, "Timer paused internally. Notification: $notificationText")
    }

    fun pauseGameTimer(updateNotificationText: String? = null) {
        serviceScope.launch {
            if (!_timerStateFlow.value.isTimerRunning) {
                Log.d(TAG, "pauseGameTimer called, but timer was not running.")
                return@launch
            }
            val textForNotification = updateNotificationText ?: "Paused: ${_timerStateFlow.value.displayedMillis.formatTime()}"
            pauseGameTimerInternally(textForNotification)
        }
    }

    fun resumeGameTimer(game: Game) {
        Log.d(TAG, "resumeGameTimer called.")
        serviceScope.launch {
            val currentState = _timerStateFlow.value
            if (currentState.isTimerRunning) {
                Log.w(TAG, "resumeGameTimer called, but timer is already running.")
                return@launch
            }
            if (!game.currentPhase.hasTimer()) {
                Log.w(TAG, "Attempted to resume timer in non-timed phase: ${game.currentPhase}. Not resuming.")
                _timerStateFlow.update { it.copy(isTimerRunning = false) }
                // Potentially update ongoing activity to reflect non-timed phase if it was PRE_GAME
                if (currentState.currentPhase == GamePhase.PRE_GAME) {
                     updateNotificationAndOngoingActivity("${game.currentPhase.readable()}: Ready", isOngoing = true)
                } else {
                     updateNotificationAndOngoingActivity("${game.currentPhase.readable()}: Paused", isOngoing = false)
                }
                return@launch
            }
            startGameTimer(game, currentState.actualTimeElapsedInPeriodMillis, currentState.inAddedTime)
            Log.d(TAG, "Timer resumed for ${game.currentPhase}.")
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
            _timerStateFlow.value.displayedMillis.formatTime()
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
            Log.d(TAG, "Ongoing Activity notification posted/updated: ID $ONGOING_NOTIFICATION_ID_VM, Text: $statusText")

        } else {
            notificationManager.cancel(ONGOING_NOTIFICATION_ID_VM)
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
