package com.databelay.refwatch.common

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.databelay.refwatch.common.SimpleIcsEvent
import com.databelay.refwatch.common.GameEvent
import com.databelay.refwatch.common.theme.DefaultAwayJerseyColor
import com.databelay.refwatch.common.theme.DefaultHomeJerseyColor
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import java.util.UUID
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import kotlinx.serialization.encodeToString // For GameEvent serialization
import kotlinx.serialization.json.jsonObject // For GameEvent serialization
import kotlin.Int

// --- Game Settings ---
@Serializable // Add this
@IgnoreExtraProperties // Add this annotation to the class
data class Game(
    // --- Core Game Mechanics Settings ---
    val id: String = UUID.randomUUID().toString(), // Unique ID for these game settings instance
    val userId: String = "", // User who created this game (for cache and syncing
    var lastUpdated: Long = System.currentTimeMillis(), // Timestamp for when this was last updated by user
    var halfDurationMinutes: Int = 45,
    var halftimeDurationMinutes: Int = 15,
    var extraTimeHalfDurationMinutes: Int = 15, // Optional for future
    var extraTimeHalftimeDurationMinutes: Int = 1, // Optional for future
//
    // --- Match Information (can be pre-filled from a schedule) ---
    var gameNumber: String = "XXXX", // Default, can be overridden
    var fieldNumber: String? = null, // Default, can be overridden
    var homeTeamName: String = "Home", // Default, can be overridden
    var awayTeamName: String = "Away", // Default, can be overridden
    var ageGroup: AgeGroup? = null,          // e.g., "U12 Boys", "Adult Men"
    var competition: String? = null,       // e.g., "League Match", "Cup Final"
    var venue: String? = null,             // e.g., "Field 3, West Park"
    var gameDateTimeEpochMillis: Long? = null, // Start date & time of the match, UTC epoch ms
    var notes: String? = null,
    // Live State Fields (updated by watch, synced via phone to Firebase)
    val inAddedTime: Boolean = false, // Is the current playable period in added time?
    var hasExtraTime: Boolean = false, // True if extra time has been initiated
    var hasPenalties: Boolean = false, // True if extra time has been initiated
    var homeTeamColorArgb: Int = DefaultHomeJerseyColor.toArgb(),
    var awayTeamColorArgb: Int = DefaultAwayJerseyColor.toArgb(),
    var kickOffTeam: Team = Team.HOME, // Actual team kicking off current period (managed by ViewModel)
    var penaltiesTakenHome: Int = 0, // Number of penalties scored by home team
    var penaltiesTakenAway: Int = 0, // Number of penalties scored by away team
    var status: GameStatus = GameStatus.SCHEDULED,
    var currentPhase: GamePhase = GamePhase.NOT_STARTED,
    var homeScore: Int = 0,
    var awayScore: Int = 0,
    var displayedTimeMillis: Long = 45,
    var actualTimeElapsedInPeriodMillis: Long = 0L,
    var isTimerRunning: Boolean = false,
    @get:Exclude
    val needsSyncWithPhone: Boolean = false, // Store locally on watch, needs sync with phone
    @get:Exclude
    val events: List<GameEvent> = emptyList() // We will sync this one manually
)  {
    companion object {
        fun defaults(): Game {
            val game = Game(
                id = UUID.randomUUID().toString(), // Or a default placeholder ID
                homeTeamName = "Home",
                awayTeamName = "Away",
                homeTeamColorArgb = DefaultHomeJerseyColor.toArgb(),
                awayTeamColorArgb = DefaultAwayJerseyColor.toArgb(),
                // Other fields will use their default values from the primary constructor
            )
            // Initialize other fields if their defaults in the primary constructor are not sufficient
            // or if you want specific values for the "defaults" case.
            game.lastUpdated = System.currentTimeMillis()
            game.halfDurationMinutes = 45
            game.halftimeDurationMinutes = 15
            game.extraTimeHalfDurationMinutes = 15
            game.extraTimeHalftimeDurationMinutes = 1
            game.ageGroup = null
            game.competition = null
            game.venue = null
            game.fieldNumber = null
            game.gameNumber = "XXXX"
            game.gameDateTimeEpochMillis = null
            game.notes = null
            game.hasExtraTime = false
            game.hasPenalties = false
            game.kickOffTeam = Team.HOME
            game.penaltiesTakenHome = 0
            game.penaltiesTakenAway = 0
            game.status = GameStatus.SCHEDULED
            game.currentPhase = GamePhase.NOT_STARTED
            // displayedTimeMillis, actualTimeElapsedInPeriodMillis, isTimerRunning already defaulted
            return game
        }
    }
    // Constructor to initialize from SimpleIcsEvent
    constructor(icsEvent: SimpleIcsEvent) : this(
        id = icsEvent.uid ?: UUID.randomUUID().toString(), // Assign if uid is not null, otherwise generate
        gameNumber = icsEvent.gameNumber ?: "XXXX",
        fieldNumber = icsEvent.fieldNumber,
        homeTeamName = icsEvent.homeTeam ?: "Home",
        awayTeamName = icsEvent.awayTeam ?: "Away",
        venue = icsEvent.location,
        gameDateTimeEpochMillis = icsEvent.dtStart?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
        // Initialize durations and notes based on parsed AgeGroup
        halfDurationMinutes = icsEvent.ageGroup?.defaultHalfDurationMinutes ?: 45, // Fallback to 45
        halftimeDurationMinutes = icsEvent.ageGroup?.defaultHalftimeDurationMinutes ?: 10, // Fallback to 10 (as per your rule)
        ageGroup = icsEvent.ageGroup,
        // You could combine ICS notes with age group notes:
        notes = listOfNotNull(icsEvent.description, icsEvent.ageGroup?.notes).joinToString(" / ").ifEmpty { null }
        // competition might be part of description or summary - needs more complex parsing or manual entry
    )
    constructor() : this(
        id = java.util.UUID.randomUUID().toString(), // Ensure id is always initialized
        // other fields with defaults
    )

    // --- Methods to modify events ---
    fun addEvent(event: GameEvent): Game {
        val updatedEvents = events + event
        return this.copy(events = updatedEvents, lastUpdated = System.currentTimeMillis())
    }

    // In Game.kt
    fun removeEvent(eventToRemove: GameEvent): Game {
        if (!events.contains(eventToRemove)) {
            return this // Event not found
        }
        val updatedEvents = events.filterNot { it == eventToRemove }
        var gameWithEventRemoved = this.copy(events = updatedEvents, lastUpdated = System.currentTimeMillis())

        // Adjust scores/stats based on the type of event removed
        when (eventToRemove) {
            is GoalScoredEvent -> {
                gameWithEventRemoved = gameWithEventRemoved.copy(
                    homeScore = if (eventToRemove.team == Team.HOME) gameWithEventRemoved.homeScore - 1 else gameWithEventRemoved.homeScore,
                    awayScore = if (eventToRemove.team == Team.AWAY) gameWithEventRemoved.awayScore - 1 else gameWithEventRemoved.awayScore
                )
            }
            is PenaltyEvent -> {
                gameWithEventRemoved = gameWithEventRemoved.copy(
                    homeScore = if (eventToRemove.team == Team.HOME && eventToRemove.scored) gameWithEventRemoved.homeScore - 1 else gameWithEventRemoved.homeScore,
                    awayScore = if (eventToRemove.team == Team.AWAY && eventToRemove.scored) gameWithEventRemoved.awayScore - 1 else gameWithEventRemoved.awayScore,
                    penaltiesTakenHome = if (eventToRemove.team == Team.HOME) (gameWithEventRemoved.penaltiesTakenHome - 1).coerceAtLeast(0) else gameWithEventRemoved.penaltiesTakenHome,
                    penaltiesTakenAway = if (eventToRemove.team == Team.AWAY) (gameWithEventRemoved.penaltiesTakenAway - 1).coerceAtLeast(0) else gameWithEventRemoved.penaltiesTakenAway
                )
            }
            else -> {

            }
            // Add cases for other events that affect Game's summary stats, like CardIssuedEvent if it impacts a displayed card count on Game.
            // For example:
            // is CardIssuedEvent -> { /* adjust card counts if Game stores them directly */ }
        }
        return gameWithEventRemoved
    }

    // --- Computed Properties for UI ---
    // Example: If GameSettings was embedded or properties are direct
    // For this to work, ensure halfDurationMinutes, extraTimeHalfDurationMinutes are properties of Game
    fun regulationPeriodDurationMillis(phase: GamePhase = this.currentPhase): Long {
        return when (phase) {
            GamePhase.FIRST_HALF, GamePhase.SECOND_HALF -> halfDurationMinutes * 60 * 1000L
            GamePhase.EXTRA_TIME_FIRST_HALF, GamePhase.EXTRA_TIME_SECOND_HALF -> extraTimeHalfDurationMinutes * 60 * 1000L
            GamePhase.HALF_TIME -> halftimeDurationMinutes * 60 * 1000L
            GamePhase.EXTRA_TIME_HALF_TIME -> extraTimeHalftimeDurationMinutes * 60 * 1000L
            else -> 0L // Other phases don't have a "playable" regulation duration
        }
    }
    val addedTimePlayedMillis: Long
        @JvmName("getAddedTimePlayedMillisInternal") // Optional: For Java interop if needed
        get() {
            val regulationDuration = this.regulationPeriodDurationMillis() // Access the property
            return if (actualTimeElapsedInPeriodMillis > regulationDuration) {
                actualTimeElapsedInPeriodMillis - regulationDuration
            } else {
                0L
            }
        }
    val isTied: Boolean
        get() = homeScore == awayScore

    val homeTeamColor: Color
        get() = Color(homeTeamColorArgb)

    val awayTeamColor: Color
        get() = Color(awayTeamColorArgb)

    val halfDurationMillis: Long
        get() = halfDurationMinutes * 60 * 1000L

    val halftimeDurationMillis: Long
        get() = halftimeDurationMinutes * 60 * 1000L


    // Optional: Formatted date/time string for display
    val formattedGameDateTime: String?
        get() = gameDateTimeEpochMillis?.let {
            val sdf = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault())
            // Consider device's time zone for display if gameDateTimeEpochMillis is UTC
            // sdf.timeZone = java.util.TimeZone.getDefault() // Example
            sdf.format(Date(it))
        }
    val summary: String
        get() {
            // ... (your existing summary logic)
            val parts = mutableListOf<String>()
            val teamsPart = if (homeTeamName.isNotBlank() && homeTeamName != "Home" || awayTeamName.isNotBlank() && awayTeamName != "Away") {
                "${homeTeamName.trim()} vs ${awayTeamName.trim()}"
            } else {
                "Game"
            }
            parts.add(teamsPart)
            ageGroup?.displayName?.let { if (it.isNotBlank() && it.lowercase() != "unknown") parts.add("($it)") }
            competition?.let { if (it.isNotBlank() && it.lowercase() != ageGroup?.displayName?.lowercase()) parts.add("- $it") }
            var summaryText = parts.joinToString(" ")
            if (summaryText == "Game") {
                formattedGameDateTime?.let { summaryText = "Game on $it" } ?: run { summaryText = "Game ID: ${id.substring(0, 8)}" }
            }
            return summaryText
        }
}

// Helper data class to identify significant game changes for persistence
data class GameSnapshotForStorage(
    val id: String,
    val status: GameStatus,
    val currentPhase: GamePhase,
    val homeScore: Int,
    val awayScore: Int,
    val events: List<GameEvent>, // Assuming GameEvent has a stable equals/hashCode
    val gameNumber: String,
    val homeTeamName: String,
    val awayTeamName: String,
    val halfDurationMinutes: Int,
    val halftimeDurationMinutes: Int,
    val penaltiesTakenHome: Int,
    val penaltiesTakenAway: Int,
)


fun Game.toSnapshotForStorage(): GameSnapshotForStorage {
    return GameSnapshotForStorage(
        id = this.id,
        status = this.status,
        currentPhase = this.currentPhase,
        homeScore = this.homeScore,
        awayScore = this.awayScore,
        events = this.events.toList(), // Ensure a copy if events list might be a mutable reference
        gameNumber = this.gameNumber,
        homeTeamName = this.homeTeamName,
        awayTeamName = this.awayTeamName,
        halfDurationMinutes = this.halfDurationMinutes,
        halftimeDurationMinutes = this.halftimeDurationMinutes,
        penaltiesTakenHome = this.penaltiesTakenHome,
        penaltiesTakenAway = this.penaltiesTakenAway,
    )
}

// Assuming AppJsonConfiguration and jsonObjectToMap are accessible/moved to common
// If not, you might need to pass them or adjust.
fun Game.toFirestoreMap(): Map<String, Any?> {
    val gameData = mutableMapOf<String, Any?>(
        "id" to this.id,
        // Ensure lastUpdated is handled correctly.
        // If it's a @ServerTimestamp Date, Firestore handles it.
        // If you're setting it manually to a Long (epoch millis), that's fine too.
        "lastUpdated" to this.lastUpdated, // Or System.currentTimeMillis() if you set it here
        "halfDurationMinutes" to this.halfDurationMinutes,
        "halftimeDurationMinutes" to this.halftimeDurationMinutes,
        "extraTimeHalfDurationMinutes" to this.extraTimeHalfDurationMinutes,
        "gameNumber" to this.gameNumber,
        "fieldNumber" to this.fieldNumber,
        "homeTeamName" to this.homeTeamName,
        "awayTeamName" to this.awayTeamName,
        "hasExtraTime" to this.hasExtraTime,
        "hasPenalties" to this.hasPenalties,
        "penaltiesTakenHome" to this.penaltiesTakenHome,
        "penaltiesTakenAway" to this.penaltiesTakenAway,
        "ageGroup" to this.ageGroup?.name, // Assuming AgeGroup is an enum
        "competition" to this.competition,
        "venue" to this.venue,
        "gameDateTimeEpochMillis" to this.gameDateTimeEpochMillis, // Assuming this is Long or Date
        "notes" to this.notes,
        "homeTeamColorArgb" to this.homeTeamColorArgb,
        "awayTeamColorArgb" to this.awayTeamColorArgb,
        "status" to this.status.name, // Assuming status is an enum
        "currentPhase" to this.currentPhase.name, // Assuming currentPhase is an enum
        "homeScore" to this.homeScore,
        "awayScore" to this.awayScore,
        "userId" to this.userId
        // Add any other fields from your Game class
    )

    // Manual mapping for events
    val eventsForFirestore = this.events.mapNotNull { event ->
        try {
            // This relies on your common AppJsonConfiguration and GameEvent being serializable
            val eventJsonString = AppJsonConfiguration.encodeToString(event)
            val jsonObject = AppJsonConfiguration.parseToJsonElement(eventJsonString).jsonObject
            jsonObjectToMap(jsonObject) // Assumes jsonObjectToMap is accessible here
        } catch (e: Exception) {
            // Log this error appropriately, perhaps with more context (e.g., from where it's called)
            // Log.e("Game.toFirestoreMap", "Failed to process a GameEvent for Firestore. Game: ${this.id}, Event: $event, Error: ${e.message}", e)
            null
        }
    }
    gameData["events"] = eventsForFirestore

    return gameData
}
