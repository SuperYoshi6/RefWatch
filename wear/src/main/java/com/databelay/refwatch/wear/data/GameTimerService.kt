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
import android.os.CountDownTimer
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
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
    private var gameCountDownTimer: CountDownTimer? = null
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
                initialElapsed = 0L
                gameCountDownTimer?.cancel()
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
        serviceScope.launch {
            acquireWakeLock()
            val currentRegulationDuration = game.regulationPeriodDurationMillis()
            _timerStateFlow.update { it.copy(isTimerRunning = true, currentPhase = game.currentPhase, inAddedTime = isInAddedTimeInitially) }
            gameCountDownTimer?.cancel()
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
            gameCountDownTimer = object : CountDownTimer(initialMillis, COUNTDOWN_INTERVAL_MS) {
                override fun onTick(millisUntilFinished: Long) {
                    val currentTimerState = _timerStateFlow.value
                    val timeThisTickerHasRun = SystemClock.elapsedRealtime() - timerStartTimeRealtime
                    val newActualElapsed = elapsedMillisAtActivation + timeThisTickerHasRun
                    val newDisplayedMillis = if (currentTimerState.inAddedTime) timeThisTickerHasRun else currentRegulationDuration - newActualElapsed

                    _timerStateFlow.update { it.copy(actualTimeElapsedInPeriodMillis = newActualElapsed, displayedMillis = newDisplayedMillis) }

                    if (!currentTimerState.inAddedTime && !hasWarnedForCurrentPeriod) {
                        if (newActualElapsed >= currentRegulationDuration - 60_000L) {
                            vibratePeriodWarning()
                            hasWarnedForCurrentPeriod = true
                        }
                    }
                    updateNotificationAndOngoingActivity(_timerStateFlow.value.displayedMillis.formatTime(), isOngoing = true)
                }

                override fun onFinish() {
                    val finishedState = _timerStateFlow.value
                    if (!finishedState.inAddedTime) {
                        _timerStateFlow.update { it.copy(actualTimeElapsedInPeriodMillis = currentRegulationDuration, displayedMillis = 0L, isTimerRunning = false) }
                        vibratePeriodEnd(finishedState.currentPhase)
                        onTimerFinishActions(finishedState.currentPhase)
                    } else {
                        _timerStateFlow.update { it.copy(isTimerRunning = false) }
                        vibratePeriodEnd(finishedState.currentPhase)
                    }
                }
            }.start()
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
                _timerStateFlow.update { it.copy(isTimerRunning = true, actualTimeElapsedInPeriodMillis = regDur, inAddedTime = true) }
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
        gameCountDownTimer?.cancel() 
        updateNotificationAndOngoingActivity(notificationText, isOngoing = false) 
    }

    fun toggleStoppageTimer() {
        val newState = !_timerStateFlow.value.isStoppageTimerRunning
        _timerStateFlow.update { it.copy(isStoppageTimerRunning = newState) }
        
        if (newState) {
            // Starting stoppage tracking: pause main timer if it's running
            if (_timerStateFlow.value.isTimerRunning) {
                pauseGameTimerInternally("Tracking Stoppage") 
            }
            startStoppageTimer()
        } else {
            // Stopping stoppage tracking: resume main timer if we have a game
            stopStoppageTimer()
            currentInternalGame?.let { game ->
                if (game.currentPhase.hasTimer()) {
                     // Update the game object with current elapsed time from state
                     val gameToResume = game.copy(
                         actualTimeElapsedInPeriodMillis = _timerStateFlow.value.actualTimeElapsedInPeriodMillis
                     )
                     resumeGameTimer(gameToResume, shouldVibrate = false)
                }
            }
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
        } else {
            // Halbzeit (Ende 1. Halbzeit): zweimal stark
            vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 300, 500), -1))
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
        serviceScope.launch {
            if (_timerStateFlow.value.isTimerRunning || !game.currentPhase.hasTimer()) return@launch
            currentInternalGame = game
            val regDur = game.regulationPeriodDurationMillis(game.currentPhase)
            val isInAdded = game.actualTimeElapsedInPeriodMillis >= regDur
            _timerStateFlow.update { it.copy(currentPhase = game.currentPhase, actualTimeElapsedInPeriodMillis = game.actualTimeElapsedInPeriodMillis, inAddedTime = isInAdded) }
            startGameTimer(game, game.actualTimeElapsedInPeriodMillis, isInAdded, shouldVibrate = shouldVibrate)
        }
    }

    fun stopGameTimerAndSession() {
        serviceScope.launch {
            _timerStateFlow.update { it.copy(isTimerRunning = false, currentPhase = GamePhase.GAME_ENDED) }
            gameCountDownTimer?.cancel()
            releaseWakeLock()
            stopForegroundSafely("Game Ended") 
        }
    }

    fun commandStopGameSessionAndCleanup(onCleanupComplete: () -> Unit) {
        serviceScope.launch {
            gameCountDownTimer?.cancel()
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
        gameCountDownTimer?.cancel()
        serviceJob.cancel() 
        super.onDestroy()
    }
}
