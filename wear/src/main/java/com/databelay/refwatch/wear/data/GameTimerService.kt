package com.databelay.refwatch.wear.data

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
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.os.Vibrator
import android.os.VibrationEffect
import com.databelay.refwatch.common.regulationPeriodDurationMillis
import android.util.Log
import androidx.compose.runtime.Immutable
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
import com.databelay.refwatch.common.isPlayablePhase
import com.databelay.refwatch.common.readable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import com.databelay.refwatch.wear.MainActivity

private const val ONGOING_NOTIFICATION_ID_SERVICE = 2
private const val ONGOING_NOTIFICATION_CHANNEL_ID = "RefWatchGameTimerChannel"
const val ONGOING_NOTIFICATION_CHANNEL_NAME = "RefWatch Timer"

const val ONGOING_NOTIFICATION_ID_VM = 123
const val COUNTDOWN_INTERVAL_MS = 1000L
const val MAX_ADDED_TIME_COUNTUP_DURATION = 1000L*60*60

@Immutable
data class TimerState(
    val actualTimeElapsedInPeriodMillis: Long = 0L,
    val isTimerRunning: Boolean = false,
    val currentPhase: GamePhase = GamePhase.NOT_STARTED,
    val regulationPeriodDurationMillis: Long = 0L,
    val displayedMillis: Long = 0L,
    val inAddedTime: Boolean = false,
    val stoppageTimeMillis: Long = 0L,
    val isStoppageTimerRunning: Boolean = false
)

class GameTimerService : Service() {
    private val TAG = "GameTimerService"
    private val binder = LocalBinder()
    private lateinit var powerManager: PowerManager
    private lateinit var notificationManager: NotificationManager
    private lateinit var vibrator: Vibrator

    private var wakeLock: PowerManager.WakeLock? = null
    private var mainTimerJob: Job? = null
    private var stoppageCountUpTimer: Job? = null
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private val _timerStateFlow = MutableStateFlow(TimerState())
    val timerStateFlow: StateFlow<TimerState> = _timerStateFlow.asStateFlow()

    private var currentInternalGame: Game? = null
    private var hasWarnedForCurrentPeriod = false
    private var timerStartTimeRealtime: Long = 0L

    inner class LocalBinder : Binder() {
        fun getService(): GameTimerService = this@GameTimerService
    }

    override fun onCreate() {
        super.onCreate()
        powerManager = getSystemService(POWER_SERVICE) as PowerManager
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        createNotificationChannel()
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
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_NOT_STICKY
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
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    fun configureTimerForGame(game: Game, startImmediately: Boolean) {
        serviceScope.launch { 
            currentInternalGame = game
            val currentRegulationDuration = game.regulationPeriodDurationMillis()
            val initialElapsed: Long

            if (startImmediately) {
                initialElapsed = game.actualTimeElapsedInPeriodMillis
                if (game.currentPhase.hasTimer()) {
                    startGameTimer(game, initialElapsed, initialElapsed >= currentRegulationDuration)
                }
            } else {
                initialElapsed = game.actualTimeElapsedInPeriodMillis
                mainTimerJob?.cancel()
            }

            if (initialElapsed == 0L) hasWarnedForCurrentPeriod = false

            _timerStateFlow.update {
                it.copy(
                    currentPhase = game.currentPhase,
                    isTimerRunning = startImmediately && game.currentPhase.hasTimer(),
                    actualTimeElapsedInPeriodMillis = initialElapsed,
                    inAddedTime = initialElapsed >= currentRegulationDuration,
                    regulationPeriodDurationMillis = currentRegulationDuration,
                    displayedMillis = if (initialElapsed >= currentRegulationDuration) initialElapsed - currentRegulationDuration else currentRegulationDuration - initialElapsed,
                    stoppageTimeMillis = game.stoppageTimeMillis,
                    isStoppageTimerRunning = false
                )
            }
            stopStoppageTimer()

            val ongoingActivityText = if (game.currentPhase == GamePhase.PRE_GAME) {
                if (canPostNotifications()) {
                    startForeground(ONGOING_NOTIFICATION_ID_SERVICE, createServiceNotification("Pre-Game Setup"), FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
                }
                "Pre-Game: ${game.homeTeamName} vs ${game.awayTeamName}"
            } else if (startImmediately && game.currentPhase.hasTimer()) {
                _timerStateFlow.value.displayedMillis.formatTime()
            } else {
                "Ready: ${game.currentPhase.readable()}"
            }
            updateNotificationAndOngoingActivity(ongoingActivityText, isOngoing = _timerStateFlow.value.isTimerRunning || game.currentPhase == GamePhase.PRE_GAME)
        }
    }

    private fun canPostNotifications(): Boolean = ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun startGameTimer(game: Game, elapsedMillisAtActivation: Long = 0L, isInAddedTimeInitially: Boolean = false, shouldVibrate: Boolean = true) {
        // IMMEDIATE UPDATE: Tell everyone we are running NOW, don't wait for coroutine launch.
        _timerStateFlow.update { it.copy(isTimerRunning = true, currentPhase = game.currentPhase, inAddedTime = isInAddedTimeInitially) }

        serviceScope.launch {
            acquireWakeLock()
            val currentRegulationDuration = game.regulationPeriodDurationMillis()
            mainTimerJob?.cancel()
            timerStartTimeRealtime = SystemClock.elapsedRealtime()

            val initialMillis = if (isInAddedTimeInitially) MAX_ADDED_TIME_COUNTUP_DURATION else {
                val remaining = currentRegulationDuration - elapsedMillisAtActivation
                if (remaining <= 0) {
                    _timerStateFlow.update { it.copy(isTimerRunning = false, actualTimeElapsedInPeriodMillis = currentRegulationDuration, displayedMillis = 0) }
                    onTimerFinishActions(game.currentPhase)
                    return@launch
                }
                remaining
            }

            if (shouldVibrate) vibratePeriodStart()
            // Coroutine-based tick at 1Hz. We DO NOT rebuild the notification on
            // every tick — that's a Notification + OngoingActivity rebuild per
            // second, which was a major jank source on the Galaxy Watch 4.
            // Instead, we update the notification once per 10s (when the
            // displayed minute text changes), and on every second-tenth change
            // (so the status ring still ticks visually). The state flow
            // continues to update at 1Hz so the UI is smooth.
            mainTimerJob = serviceScope.launch {
                var lastNotificationTenthSec = -1
                while (isActive) {
                    delay(COUNTDOWN_INTERVAL_MS)
                    val currentTimerState = _timerStateFlow.value
                    val timeThisTickerHasRun = SystemClock.elapsedRealtime() - timerStartTimeRealtime
                    val newActualElapsed = elapsedMillisAtActivation + timeThisTickerHasRun
                    val newDisplayedMillis = if (currentTimerState.inAddedTime) {
                        (newActualElapsed - currentRegulationDuration).coerceAtLeast(0L)
                    } else {
                        (currentRegulationDuration - newActualElapsed).coerceAtLeast(0L)
                    }

                    _timerStateFlow.update { it.copy(actualTimeElapsedInPeriodMillis = newActualElapsed, displayedMillis = newDisplayedMillis) }

                    if (!currentTimerState.inAddedTime && !hasWarnedForCurrentPeriod) {
                        if (newActualElapsed >= currentRegulationDuration - 60_000L) {
                            vibratePeriodWarning()
                            hasWarnedForCurrentPeriod = true
                        }
                    }

                    // Throttle notification rebuilds to once every 10 seconds.
                    // Watch OS doesn't refresh a 1Hz-tick notification visibly
                    // anyway, and rebuilding it costs a JNI hop + builder alloc.
                    val currentTenthSec = (newDisplayedMillis / 10_000L).toInt()
                    if (currentTenthSec != lastNotificationTenthSec) {
                        lastNotificationTenthSec = currentTenthSec
                        updateNotificationAndOngoingActivity(_timerStateFlow.value.displayedMillis.formatTime(), isOngoing = true)
                    }

                    // End-of-period check
                    if (!currentTimerState.inAddedTime && newActualElapsed >= currentRegulationDuration) {
                        _timerStateFlow.update { it.copy(actualTimeElapsedInPeriodMillis = currentRegulationDuration, displayedMillis = 0L, isTimerRunning = false) }
                        vibratePeriodEnd(currentTimerState.currentPhase)
                        onTimerFinishActions(currentTimerState.currentPhase)
                        break
                    }
                    if (currentTimerState.inAddedTime && timeThisTickerHasRun >= MAX_ADDED_TIME_COUNTUP_DURATION) {
                        _timerStateFlow.update { it.copy(isTimerRunning = false) }
                        vibratePeriodEnd(currentTimerState.currentPhase)
                        break
                    }

                }
            }
            _timerStateFlow.update { it.copy(isStoppageTimerRunning = false) }
            stopStoppageTimer()
        }
    }

    private fun startStoppageTimer() {
        if (stoppageCountUpTimer?.isActive == true) return
        stoppageCountUpTimer = serviceScope.launch {
            while (isActive && _timerStateFlow.value.isStoppageTimerRunning) {
                delay(COUNTDOWN_INTERVAL_MS)
                _timerStateFlow.update { it.copy(stoppageTimeMillis = it.stoppageTimeMillis + COUNTDOWN_INTERVAL_MS) }
            }
        }
    }

    private fun stopStoppageTimer() {
        stoppageCountUpTimer?.cancel()
        stoppageCountUpTimer = null
    }

    private fun onTimerFinishActions(finishedPhase: GamePhase) {
        serviceScope.launch {
            val game = currentInternalGame ?: return@launch
            if (finishedPhase.hasTimer() && !(_timerStateFlow.value.inAddedTime)) {
                val regDur = game.regulationPeriodDurationMillis(finishedPhase)
                
                // NO GAP TRANSITION: Immediately set isTimerRunning=true for added time
                _timerStateFlow.update { it.copy(
                    isTimerRunning = true, 
                    actualTimeElapsedInPeriodMillis = regDur, 
                    inAddedTime = true,
                    displayedMillis = 0L // Starts counting up from here
                ) }
                
                startGameTimer(game, regDur, true)
            } else {
                val state = _timerStateFlow.value
                updateNotificationAndOngoingActivity(state.displayedMillis.formatTime(), isOngoing = game.status == GameStatus.IN_PROGRESS)
                if (!state.isTimerRunning && game.status != GameStatus.IN_PROGRESS) {
                    releaseWakeLock()
                    stopForegroundSafely("Timer Finished")
                }
            }
        }
    }

    private fun pauseGameTimerInternally(notificationText: String) {
        _timerStateFlow.update { it.copy(isTimerRunning = false) }
        mainTimerJob?.cancel()
        updateNotificationAndOngoingActivity(notificationText, isOngoing = false)
    }

    fun toggleStoppageTimer() {
        val newState = !_timerStateFlow.value.isStoppageTimerRunning
        updateStoppageTimerState(newState)
    }

    private fun updateStoppageTimerState(isRunning: Boolean) {
        if (isRunning) {
            // Pause the main timer and start the stoppage (green) timer.
            _timerStateFlow.update { it.copy(isTimerRunning = false, isStoppageTimerRunning = true) }
            mainTimerJob?.cancel()
            startStoppageTimer()
            updateNotificationAndOngoingActivity("Stoppage Time", isOngoing = true)
        } else {
            // Stop the stoppage timer and resume the main timer.
            stopStoppageTimer()
            _timerStateFlow.update { it.copy(isStoppageTimerRunning = false) }
            val game = currentInternalGame ?: return
            if (game.currentPhase.hasTimer()) {
                val regDur = game.regulationPeriodDurationMillis(game.currentPhase)
                val elapsed = _timerStateFlow.value.actualTimeElapsedInPeriodMillis
                val isInAdded = elapsed >= regDur
                startGameTimer(game, elapsed, isInAdded, shouldVibrate = false)
            }
        }
    }

    /**
     * ATOMIC SYNC: Applies a full timer state from a remote source (Firestore).
     * This avoids individual field updates/toggles fighting each other.
     */
    fun applyRemoteState(
        remoteGame: Game
    ) {
        // ALWAYS update the internal game reference first so we have the latest durations
        currentInternalGame = remoteGame
        
        val regDur = remoteGame.regulationPeriodDurationMillis(remoteGame.currentPhase)
        val isTimerRunning = remoteGame.isTimerRunning
        val isStoppageTimerRunning = remoteGame.isStoppageTimerRunning
        val actualElapsed = remoteGame.actualTimeElapsedInPeriodMillis
        val stoppageMillis = remoteGame.stoppageTimeMillis
        
        // 1. Update the flows SYNC (immediately)
        _timerStateFlow.update { it.copy(
            currentPhase = remoteGame.currentPhase,
            actualTimeElapsedInPeriodMillis = actualElapsed,
            stoppageTimeMillis = stoppageMillis,
            isTimerRunning = isTimerRunning,
            isStoppageTimerRunning = isStoppageTimerRunning,
            inAddedTime = actualElapsed >= regDur,
            regulationPeriodDurationMillis = regDur
        ) }

        // 2. Manage the background jobs in scope
        serviceScope.launch {
            if (isStoppageTimerRunning) {
                mainTimerJob?.cancel()
                startStoppageTimer()
            } else {
                stopStoppageTimer()
            }

            if (isTimerRunning) {
                startGameTimer(remoteGame, actualElapsed, actualElapsed >= regDur, shouldVibrate = false)
            } else {
                mainTimerJob?.cancel()
            }
            
            // 3. UI Update
            val text = if (isStoppageTimerRunning) "Stoppage Time" else _timerStateFlow.value.displayedMillis.formatTime()
            updateNotificationAndOngoingActivity(text, isOngoing = isTimerRunning || isStoppageTimerRunning)
        }
    }

    fun pauseGameTimer(updateNotificationText: String? = null) {
        serviceScope.launch {
            if (!_timerStateFlow.value.isTimerRunning) return@launch
            pauseGameTimerInternally(updateNotificationText ?: "Paused")
        }
    }

    private fun vibrate(effect: VibrationEffect) { if (vibrator.hasVibrator()) vibrator.vibrate(effect) }
    private fun vibratePeriodStart() = vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
    
    private fun vibratePeriodEnd(phase: GamePhase) {
        if (phase == GamePhase.SECOND_HALF || phase == GamePhase.EXTRA_TIME_SECOND_HALF) {
            // Spielende: 3 Mal stark wie eine Pfeife
            vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 300, 500, 300, 500), -1))
        } else if (phase == GamePhase.FIRST_HALF || phase == GamePhase.EXTRA_TIME_FIRST_HALF) {
            // Halbzeit (Ende 1. Halbzeit): zweimal stark
            vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 300, 500), -1))
        } else {
            // Other phases: single pulse
            vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    private fun vibratePeriodWarning() {
        // 1 Minute vor Ablauf: leichte Vibration
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrate(VibrationEffect.createOneShot(200, 80)) 
        } else {
            vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    fun resumeGameTimer(game: Game, shouldVibrate: Boolean = true) {
        if (_timerStateFlow.value.isTimerRunning || !game.currentPhase.hasTimer()) return
        
        currentInternalGame = game
        val regDur = game.regulationPeriodDurationMillis(game.currentPhase)
        val isInAdded = game.actualTimeElapsedInPeriodMillis >= regDur
        
        // IMMEDIATE UPDATE
        _timerStateFlow.update { it.copy(currentPhase = game.currentPhase, actualTimeElapsedInPeriodMillis = game.actualTimeElapsedInPeriodMillis, inAddedTime = isInAdded, isTimerRunning = true) }

        serviceScope.launch {
            startGameTimer(game, game.actualTimeElapsedInPeriodMillis, isInAdded, shouldVibrate = shouldVibrate)
        }
    }

    fun stopGameTimerAndSession() {
        serviceScope.launch {
            _timerStateFlow.update { it.copy(isTimerRunning = false, currentPhase = GamePhase.GAME_ENDED) }
            mainTimerJob?.cancel()
            releaseWakeLock()
            stopForegroundSafely("Game Ended")
        }
    }

    fun commandStopGameSessionAndCleanup(onCleanupComplete: () -> Unit) {
        serviceScope.launch {
            mainTimerJob?.cancel()
            _timerStateFlow.update { it.copy(isTimerRunning = false, currentPhase = GamePhase.GAME_ENDED) }
            releaseWakeLock()
            stopForegroundSafely()
            onCleanupComplete()
        }
    }

    private fun stopForegroundSafely(text: String? = null) {
        stopForeground(STOP_FOREGROUND_REMOVE)
        if (text != null && canPostNotifications()) {
            val n = NotificationCompat.Builder(this, ONGOING_NOTIFICATION_CHANNEL_ID).setContentTitle("RefWatch").setContentText(text).setSmallIcon(R.drawable.ic_stat_refwatch).setAutoCancel(true).build()
            notificationManager.notify(ONGOING_NOTIFICATION_ID_SERVICE + 100, n) 
        }
        notificationManager.cancel(ONGOING_NOTIFICATION_ID_VM)
    }

    private fun createServiceNotification(text: String): Notification {
        val intent = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, ONGOING_NOTIFICATION_CHANNEL_ID).setSmallIcon(R.drawable.ic_stat_refwatch).setContentTitle("RefWatch").setContentText(text).setOngoing(true).setContentIntent(intent).build()
    }

    private fun updateNotificationAndOngoingActivity(text: String, isOngoing: Boolean) {
        if (!canPostNotifications()) return
        notificationManager.notify(ONGOING_NOTIFICATION_ID_SERVICE, createServiceNotification(text))
        if (isOngoing && currentInternalGame != null) {
            val status = Status.Builder().addTemplate(text).build()
            val intent = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val builder = NotificationCompat.Builder(this, ONGOING_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_refwatch)
                .setContentTitle("${currentInternalGame?.homeTeamName} vs ${currentInternalGame?.awayTeamName}")
                .setContentText(text)
                .setOngoing(true)
                .setContentIntent(intent)

            OngoingActivity.Builder(applicationContext, ONGOING_NOTIFICATION_ID_VM, builder)
                .setStatus(status)
                .setTouchIntent(intent)
                .build()
                .apply(applicationContext)
            notificationManager.notify(ONGOING_NOTIFICATION_ID_VM, builder.build())
        }
    }

    fun commandStartGameSessionAndTimer(game: Game, elapsed: Long = 0L) {
        serviceScope.launch {
            currentInternalGame = game
            acquireWakeLock() 
            if (game.currentPhase.hasTimer()) startGameTimer(game, elapsed, false)
        }
    }

    override fun onDestroy() {
        releaseWakeLock()
        mainTimerJob?.cancel()
        serviceJob.cancel()
        super.onDestroy()
    }
}
