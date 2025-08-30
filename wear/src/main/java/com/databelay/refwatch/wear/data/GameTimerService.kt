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

const val ONGOING_NOTIFICATION_ID_VM = 123
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
        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager // Initialize here
        // Ensure your notification channel is created (ideally in Application.onCreate)
        createNotificationChannel()
        Log.d(TAG, "Service Created. Not starting foreground yet.")
    }
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            ONGOING_NOTIFICATION_CHANNEL_ID,
            ONGOING_NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW // Or IMPORTANCE_DEFAULT. Avoid HIGH for ongoing unless absolutely necessary.
        ).apply {
            description = "Shows the current game timer"
            // Configure other channel properties if needed (e.g., enableLights, lightColor)
            // For ongoing timers, you usually don't want sound or vibration on the channel itself,
            // as the notification updates frequently.
            setSound(null, null)
            enableVibration(false)
        }
        notificationManager.createNotificationChannel(channel)
        Log.d(TAG, "Notification channel created: $ONGOING_NOTIFICATION_CHANNEL_ID")
    }
    // Not using onStartCommand directly for primary control if using binding for start/stop
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // You might use this if you want the service to auto-restart or handle deep links to start timer
        // For now, let's assume ViewModel controls via binding.
        // START_NOT_STICKY is fine if ViewModel is expected to always re-bind and restart if needed.
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    @SuppressLint("WakelockTimeout")
    private fun acquireWakeLock() {
        if (wakeLock?.isHeld != true) {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "RefWatch::GameTimerWakeLockTag").apply {
                setReferenceCounted(false)
                acquire() // No timeout, relies on explicit release
            }
            // Log.d("GameTimerService", "WakeLock acquired")
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
                // Log.d("GameTimerService", "WakeLock released")
            }
        }
        wakeLock = null
    }

    fun configureTimerForGame(game: Game, startImmediately: Boolean) {
        serviceScope.launch { // Ensure on Main thread for CountDownTimer interaction
            Log.d(TAG, "Service configuring for game: ${game.id}, phase: ${game.currentPhase}, startImmediately: $startImmediately")
            currentInternalGame = game
            val initialElapsed = game.actualTimeElapsedInPeriodMillis
            val regulationDuration = game.regulationPeriodDurationMillis()
//            val isAdded = game.currentPhase.isPlayablePhase() && initialElapsed >= regulationDuration
            val isAdded = game.currentPhase.hasTimer() && initialElapsed >= regulationDuration
            val displayedMillis = if (isAdded) {initialElapsed - regulationDuration} else {regulationDuration - initialElapsed}
            val initialIsTimerRunning = startImmediately && (game.currentPhase.hasTimer())
            _timerStateFlow.update {
                it.copy(
                    currentPhase = game.currentPhase,
                    isTimerRunning = initialIsTimerRunning, // Will be set true by startGameTimer if called
                    displayedMillis = displayedMillis,
                    actualTimeElapsedInPeriodMillis = initialElapsed,
                    inAddedTime = isAdded,
                    regulationPeriodDurationMillis = game.regulationPeriodDurationMillis(),
                )
            }
            // Update notification text but don't necessarily start foreground service here yet
            // The actual startForeground will happen in startGameTimer if startImmediately is true.
            updateNotificationAndOngoingActivity(
                if (startImmediately && game.currentPhase.hasTimer()) {
                    _timerStateFlow.value.displayedMillis.formatTime() // Or calculate fresh
                } else {
                    "Ready: ${game.currentPhase.readable()}"
                },
                isOngoing = startImmediately && game.currentPhase.hasTimer()
            )

            if (startImmediately && game.currentPhase.hasTimer()) {
                // startGameTimer will handle startForeground
                startGameTimer(game, _timerStateFlow.value.actualTimeElapsedInPeriodMillis, _timerStateFlow.value.inAddedTime)
            } else if (_timerStateFlow.value.isTimerRunning) {
                // If we configure for a new game, but timer was running for an old one and new one shouldn't start.
                // This scenario needs careful handling. Typically, ViewModel would stop timer before configuring new one.
                Log.w(TAG, "Configuring for new game, but timer was running and new game not starting immediately. Pausing timer.")
                pauseGameTimerInternally("Timer Stoppped") // An internal pause without full stop
            }
        }
    }


    private fun canPostNotifications(): Boolean {
        // POST_NOTIFICATIONS permission is only required from Android 13 (API 33) onwards.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val result = ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            )
            Log.d("$TAG:timer", "POST_NOTIFICATIONS permission check result: $result")
            return result == PackageManager.PERMISSION_GRANTED
        }
        return true // Automatically granted on versions below Android 13
    }


    /**
     * Starts the game timer for the given game.
     * If a timer is already running, it will be cancelled and a new one started
     * with the provided parameters.
     *
     * This method acquires a wakelock, updates the internal timer state,
     * starts the service in the foreground (if not already), and initializes
     * a [CountDownTimer] to manage the timing logic.
     *
     * @param game The [Game] object containing details about the current game state.
     * @param initialElapsedMillis The elapsed time in milliseconds to start the timer from.
     *                             Defaults to 0L.
     * @param isAddedTimeInitially A boolean indicating if the timer is starting directly into added time.
     *                             Defaults to false.
     */
    fun startGameTimer(game: Game, initialElapsedMillis: Long = 0L, isAddedTimeInitially: Boolean = false) {
        serviceScope.launch {
            if (_timerStateFlow.value.isTimerRunning) {
                gameCountDownTimer?.cancel()
            }
            currentInternalGame = game // Store the game
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

            // Start Foreground Service if not already started with the right state
            // This is crucial. Even if service is running, ensure it's in FG mode now.
            startForeground(ONGOING_NOTIFICATION_ID_SERVICE, createServiceNotification(_timerStateFlow.value.displayedMillis.formatTime()))
            updateNotificationAndOngoingActivity(_timerStateFlow.value.displayedMillis.formatTime(), isOngoing = true)
            Log.d(TAG, "GameTimerService brought to foreground explicitly for timer start.")

            val timeToCountFrom = Long.MAX_VALUE // For continuous running
            gameCountDownTimer = object : CountDownTimer(timeToCountFrom, COUNTDOWN_INTERVAL_MS) {
                override fun onTick(millisUntilFinished_unused: Long) {
                    if (!(_timerStateFlow.value.isTimerRunning)) { // Check our own state flag
                        this.cancel() // Stop if service state says so
                        // Don't release wakelock or stop foreground if it's just a pause
                        return
                    }

                    _timerStateFlow.update { currentState ->
                        val newElapsed = currentState.actualTimeElapsedInPeriodMillis + COUNTDOWN_INTERVAL_MS
                        val currentRegulationDuration =
                            currentState.regulationPeriodDurationMillis // Use from state
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

//                    Log.d(TAG, "Timer tick: ${_timerStateFlow.value.displayedMillis.formatTime()}")
                    // Update notification if needed
                    updateNotificationAndOngoingActivity(_timerStateFlow.value.displayedMillis.formatTime(), isOngoing = true)
                }

                override fun onFinish() {
                    // This should not be reached with Long.MAX_VALUE
                    // Handle as an abnormal stop
                    Log.w(TAG, "CountDownTimer finished unexpectedly!")
                    _timerStateFlow.update { it.copy(isTimerRunning = false) }
                    releaseWakeLock()
                    stopForegroundSafely("Timer Finished Unexpectedly")
                    // No stopSelf() here, let ViewModel decide if service should stop.
                }
            }.start()
            updateNotificationAndOngoingActivity(_timerStateFlow.value.displayedMillis.formatTime())
        }
    }

    /**
     * Internal pause logic, keeps wakelock, updates notification.
     * Does not call stopForeground.
     */
    private fun pauseGameTimerInternally(notificationText: String) {
        _timerStateFlow.update { it.copy(isTimerRunning = false) }
        gameCountDownTimer?.cancel() // Cancel the actual countdown
        // Wakelock is intentionally kept if this is a temporary pause during an active game session
        updateNotificationAndOngoingActivity(notificationText, isOngoing = false) // Update notification to show "Paused" or similar
        Log.d(TAG, "Timer paused internally. Notification: $notificationText")
    }


    /**
     * Pauses the current countdown timer, keeps the wakelock (if configured to do so on pause),
     * and updates the timer state.
     * @param updateNotificationText Optional text to update the ongoing notification with (e.g., "Paused", "Period Ended")
     */
    fun pauseGameTimer(updateNotificationText: String? = null) {
        serviceScope.launch {
            if (!_timerStateFlow.value.isTimerRunning) {
                Log.d(TAG, "pauseGameTimer called, but timer was not running.")
                return@launch
            }
            // gameCountDownTimer?.cancel() // Already handled by pauseGameTimerInternally if called through it
            // _timerStateFlow.update { it.copy(isTimerRunning = false) } // Also handled

            val textForNotification = updateNotificationText ?: "Paused: ${_timerStateFlow.value.displayedMillis.formatTime()}"
            pauseGameTimerInternally(textForNotification)

            // DECISION: Do you want to remove the foreground state when paused?
            // Option A: Keep it foreground, notification updates to "Paused" (current behavior of pauseGameTimerInternally)
            // Option B: Make notification dismissible but keep service running
            // stopForeground(Service.STOP_FOREGROUND_DETACH) // Notification can be dismissed, service runs
            // Option C: Remove notification entirely if pause means "not actively ongoing" for a while
            // stopForeground(Service.STOP_FOREGROUND_REMOVE)

            // For now, let's assume pause keeps the service prominent if the game session is still active.
            // The notification text already indicates it's paused.
            // Wakelock is still held by pauseGameTimerInternally.
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
                return@launch
            }

            // startGameTimer will handle acquiring wakelock (if not already held)
            // and calling startForeground.
            startGameTimer(game, currentState.actualTimeElapsedInPeriodMillis, currentState.inAddedTime)
            Log.d(TAG, "Timer resumed for ${game.currentPhase}.")
        }
    }


    // Call this when the game session fully ends or timer is no longer needed at all
    fun stopGameTimerAndSession() {
        serviceScope.launch {
            Log.i(TAG, "Stopping game timer and session.")
            _timerStateFlow.update { it.copy(isTimerRunning = false, currentPhase = GamePhase.GAME_ENDED) }
            gameCountDownTimer?.cancel()
            gameCountDownTimer = null
            releaseWakeLock()
            stopForegroundSafely("Game Ended") // Custom message
            // updateNotificationAndOngoingActivity("Game Ended", isOngoing = false) // Not needed if stopForegroundSafely handles it
            Log.d(TAG, "Ongoing Activity should be cancelled by updateNotificationAndOngoingActivity due to isOngoing=false or by stopForegroundSafely.")

            // Optional: If no clients are bound and no more work, stop the service.
            // This depends on your service lifecycle management.
            // If ViewModel always unbinds and stops, this might not be needed here.
            // stopSelf()
        }
    }

    /**
     * Called by ViewModel when the game session is completely finished.
     * Stops timer, RELEASES WAKELOCK, and cleans up.
     */
    fun commandStopGameSessionAndCleanup() {
        serviceScope.launch {
            Log.i(TAG, "COMMAND: Stop Game Session & Cleanup. Releasing WakeLock.")
            gameCountDownTimer?.cancel()
            gameCountDownTimer = null

            _timerStateFlow.update {
                it.copy(
                    isTimerRunning = false,
                    displayedMillis = 0L, // Or relevant end-game display
                    actualTimeElapsedInPeriodMillis = 0L,
                    currentPhase = it.currentPhase // Default to a terminal phase
                )
            }
            releaseWakeLock() // <<<< WAKELOCK RELEASED HERE >>>>
            updateNotificationAndOngoingActivity("Game Ended")
            stopForegroundSafely()
        }
    }

    // Your existing stopForegroundSafely method

    private fun stopForegroundSafely(notificationTextIfLingering: String? = null) {
        Log.d(TAG, "stopForegroundSafely called.")
        stopForeground(Service.STOP_FOREGROUND_REMOVE) // Removes notification
        // If you want a final "Game Ended" notification that is NOT ongoing:
        if (notificationTextIfLingering != null && canPostNotifications()) {
            val finalNotification = NotificationCompat.Builder(this, ONGOING_NOTIFICATION_CHANNEL_ID)
                .setContentTitle("RefWatch")
                .setContentText(notificationTextIfLingering)
                .setSmallIcon(R.drawable.ic_stat_refwatch)
                .setAutoCancel(true) // Make it dismissible
                .build()
            notificationManager.notify(ONGOING_NOTIFICATION_ID_SERVICE + 100, finalNotification) // Use different ID or cancel previous
        }

        // Cancel the specific notification that was driving the OngoingActivity display
        if (canPostNotifications()) {
            notificationManager.cancel(ONGOING_NOTIFICATION_ID_VM)
            Log.d(TAG, "In stopForegroundSafely, cancelled notification (ID: $ONGOING_NOTIFICATION_ID_VM) to stop associated OngoingActivity.")
        }

        Log.i(TAG, "Service stopped foreground state. OngoingActivity display should be dismissed.")
    }

      private fun createServiceNotification(contentText: String): Notification {
        val activityIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, ONGOING_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_refwatch) // USE YOUR ACTUAL ICON
            .setContentTitle("RefWatch Timer Active")
            .setContentText(contentText)
            .setCategory(NotificationCompat.CATEGORY_SERVICE) // Or CATEGORY_STOPWATCH
            .setOngoing(true) // Crucial for foreground service + ongoing activity behavior
            .setOnlyAlertOnce(true)
        // .setContentIntent(pendingIntent) // Set by OngoingActivity builder if used

        // --- Integrate OngoingActivity API ---
        val status = Status.Builder()
            .addTemplate(contentText) // Or a more structured template like "# $contentText"
            // Add other parts if needed, e.g., .addPart("phase", Status.TextPart(...))
            .build()

        // If using androidx.wear:wear-ongoing:1.1.0 or later
        // (You have androidx.wear:wear-ongoing:1.1.0-beta01 [3] which is fine)
        val ongoingActivity = OngoingActivity.Builder(applicationContext, ONGOING_NOTIFICATION_ID_SERVICE, notificationBuilder)
            .setStaticIcon(R.drawable.ic_stat_refwatch) // USE YOUR ACTUAL ICON
            .setTouchIntent(pendingIntent)
            .setStatus(status)
            .build()

        // Apply the OngoingActivity features to the notification builder
        ongoingActivity.apply(this) // Modifies the notificationBuilder

        return notificationBuilder.build()
        // --- End OngoingActivity Integration ---
    }

    // Call this when you need to update the text AND the OngoingActivity status
    private fun updateNotificationAndOngoingActivity(text: String, isOngoing: Boolean = _timerStateFlow.value.isTimerRunning) {
        if (!canPostNotifications()) {
            Log.w(TAG, "Cannot post notifications, permission denied or not available.")
            return
        }
        // Only update if the service is actually in foreground mode OR if we intend to start it.
        // The check for isTimerRunning helps decide if the notification should be truly "ongoing"
        // or just an update (e.g., "Paused").

        val notification = createServiceNotification(text)
        notificationManager.notify(ONGOING_NOTIFICATION_ID_SERVICE, notification)

        // Update Ongoing Activity status
        // Logic for the Notification that is also an Ongoing Activity
        if (isOngoing && currentInternalGame != null) {
            val gamePhaseText = currentInternalGame?.currentPhase?.readable() ?: "Game Active"
            val status = Status.Builder()
                .addTemplate("$text - $gamePhaseText")
                .build()

            // This is the Notification Builder for the Ongoing Activity
            val oaNotificationBuilder = NotificationCompat.Builder(this, ONGOING_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_refwatch) // Use a distinct icon for OA
                .setContentTitle("RefWatch Active Game")
                .setContentText("$text - $gamePhaseText")
                .setCategory(NotificationCompat.CATEGORY_EVENT) // Or other relevant category
                .setOngoing(true) // The notification itself should be ongoing if it represents an ongoing task

            val ongoingActivity = OngoingActivity.Builder(applicationContext, ONGOING_NOTIFICATION_ID_VM, oaNotificationBuilder)
                .setStaticIcon(R.drawable.ic_stat_refwatch) // REPLACE
                .setTouchIntent(
                    PendingIntent.getActivity(
                        this, 0, Intent(this, MainActivity::class.java),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
                .setStatus(status)
                .build()
            ongoingActivity.apply(applicationContext) // Apply OA features to oaNotificationBuilder

            if (canPostNotifications()) {
                notificationManager.notify(ONGOING_NOTIFICATION_ID_VM, oaNotificationBuilder.build())
                Log.d(TAG, "Ongoing Activity notification posted/updated: ID $ONGOING_NOTIFICATION_ID_VM, Text: $text")
            } else {
                Log.w(TAG, "Cannot post Ongoing Activity notification, permission denied.")
            }

        } else {
            // If not ongoing, cancel the notification that was driving the OngoingActivity
            if (canPostNotifications()) {
                notificationManager.cancel(ONGOING_NOTIFICATION_ID_VM)
                Log.d(TAG, "Ongoing Activity notification cancelled (ID: $ONGOING_NOTIFICATION_ID_VM) because timer/game is not ongoing.")
            }
        }

        // Update the service's own foreground notification (if it's different)
        // This example assumes ONGOING_NOTIFICATION_ID_SERVICE is for the service's foreground state,
        // and ONGOING_NOTIFICATION_ID_VM is for the user-visible Ongoing Activity.
        // If they are the same, the above logic might be slightly different.
        if (_timerStateFlow.value.isTimerRunning) { // Or some other condition for the service's own FG notification
            val serviceNotification = createServiceNotification(text) // Your existing method
            // startForeground(ONGOING_NOTIFICATION_ID_SERVICE, serviceNotification) // This is handled in startGameTimer
            notificationManager.notify(ONGOING_NOTIFICATION_ID_SERVICE, serviceNotification)
        } else {
            // Optionally update or remove the service's own notification if it's paused but not fully stopped
            // stopForeground(Service.STOP_FOREGROUND_DETACH) or update its text
        }
    }

    /**
     * Called by ViewModel when a game session truly starts (e.g., moving to first half).
     * Acquires WakeLock for the session and starts the timer.
     */
    fun commandStartGameSessionAndTimer(game: Game, initialElapsedMillis: Long = 0L) {
        serviceScope.launch {
            currentInternalGame = game
            Log.i(TAG, "COMMAND: Start Game Session & Timer for ${game.currentPhase}. Acquiring WakeLock.")
            acquireWakeLock() // <<<< WAKELOCK ACQUIRED HERE (once per session) >>>>

            // Update state to reflect timer is running and other game parameters
            _timerStateFlow.update {
                it.copy(
                    currentPhase = game.currentPhase,
                    isTimerRunning = false,
                    actualTimeElapsedInPeriodMillis = initialElapsedMillis,
                    inAddedTime = false,
                    displayedMillis = game.regulationPeriodDurationMillis() - initialElapsedMillis,
                    regulationPeriodDurationMillis = game.regulationPeriodDurationMillis()
                )
            }
            startForeground(ONGOING_NOTIFICATION_ID_SERVICE, createServiceNotification(_timerStateFlow.value.displayedMillis.formatTime()))
            acquireWakeLock()
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "Service Destroyed.")
        releaseWakeLock() // Ensure wakelock is released
        gameCountDownTimer?.cancel()
        serviceJob.cancel() // Cancel all coroutines started by serviceScope
        // Cancel the specific notification that was driving the OngoingActivity display
        if (canPostNotifications()) {
            notificationManager.cancel(ONGOING_NOTIFICATION_ID_VM)
            Log.d(TAG, "In onDestroy, cancelled notification (ID: $ONGOING_NOTIFICATION_ID_VM) to stop associated OngoingActivity.")
        }
        super.onDestroy()
    }
}
