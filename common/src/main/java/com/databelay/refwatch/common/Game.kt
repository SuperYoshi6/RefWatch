package com.databelay.refwatch.common

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.databelay.refwatch.common.theme.DefaultAwayJerseyColor
import com.databelay.refwatch.common.theme.DefaultHomeJerseyColor
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import java.util.UUID

// --- Player Model ---
@Serializable
@Immutable
@IgnoreExtraProperties
@Keep
data class Player(
    val name: String = "",
    val number: Int = 0,
    val isCaptain: Boolean = false,
    val isOnField: Boolean = true
)

@Serializable
@Immutable
@IgnoreExtraProperties
@Keep
data class TeamOfficial(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val role: String = "",
    val number: Int? = null // Optional ID number
)

// --- Game Settings ---
@Serializable // Add this
@Immutable // Tells Compose that all fields are stable; allows skippable parameters
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
    var homeTeamName: String = "", // Default, can be overridden
    var awayTeamName: String = "", // Default, can be overridden
    var homeTeamAbbr: String? = null,
    var awayTeamAbbr: String? = null,
    var homeCaptainNumber: Int? = null,
    var awayCaptainNumber: Int? = null,
    @PropertyName("homeRoster")
    var homeRoster: List<Player> = emptyList(),
    @PropertyName("awayRoster")
    var awayRoster: List<Player> = emptyList(),
    @PropertyName("homeOfficials")
    var homeOfficials: List<TeamOfficial> = emptyList(),
    @PropertyName("awayOfficials")
    var awayOfficials: List<TeamOfficial> = emptyList(),
    var ageGroup: AgeGroup? = null,          // e.g., "U12 Boys", "Adult Men"
    var competition: String? = null,       // e.g., "League Match", "Cup Final"
    var refereeAssignment: String? = null, // e.g. Assistant Referee
    var venue: String? = null,             // e.g., "Field 3, West Park"
    var gameDateTimeEpochMillis: Long? = null, // Start date & time of the match, UTC epoch ms
    var notes: String? = null,
    var fourthOfficial: String? = null,
    var observer: String? = null,
    var mainReferee: String? = null,
    var assistantReferee1: String? = null,
    var assistantReferee2: String? = null,
    // Live State Fields (updated by watch, synced via phone to Firebase)
    val inAddedTime: Boolean = false, // Is the current playable period in added time?
    var hasExtraTime: Boolean = false, // True if extra time has been initiated
    var hasPenalties: Boolean = false, // True if extra time has been initiated
    var hasTemporaryDismissals: Boolean = false,
    /**
     * Number of kicks each team gets in a penalty shootout before sudden death
     * kicks are needed. Defaults to 5 (FIFA standard) but can be lowered (e.g. 3)
     * for youth / amateur games. Used by WearGameViewModel#checkShootoutEndCondition
     * to decide when the shootout is over.
     *
     * Acceptable range: 1..20. 0 / negative values are clamped to 5 by
     * [clampPenaltyKicksPerTeam].
     */
    var penaltyKicksPerTeam: Int = 5,
    var homeTeamColorArgb: Int = Color.Yellow.toArgb(),
    var awayTeamColorArgb: Int = DefaultAwayJerseyColor.toArgb(),
    var kickOffTeam: Team = Team.HOME, // Actual team kicking off current period (managed by ViewModel)
    var penaltiesTakenHome: Int = 0, // Number of penalties scored by home team
    var penaltiesTakenAway: Int = 0, // Number of penalties scored by away team
    var currentPhase: GamePhase = GamePhase.NOT_STARTED,
    var homeScore: Int = 0,
    var awayScore: Int = 0,
    var displayedTimeMillis: Long = 2700000L,
    var actualTimeElapsedInPeriodMillis: Long = 0L,
    var stoppageTimeMillis: Long = 0L,
    var isTimerRunning: Boolean = false,
    var isStoppageTimerRunning: Boolean = false,
    @get:Exclude
    val needsSyncWithPhone: Boolean = false, // Store locally on watch, needs sync with phone
    var maxSubstitutionsAllowed: Int = 5,
    var temporaryDismissalMinutes: Int = 0,
    @get:Exclude
    val events: List<GameEvent> = emptyList() // We will sync this one manually
)  {
    // Computed property for GameStatus
    // If you use Kotlinx.serialization and don't want this in Firestore,
    // you might not need @Transient if it's just a getter.
    // If it were a var with a backing field you didn't want to store, you'd use @Transient.
    @get:Exclude
    val status: GameStatus
        get() {
            return currentPhase.status()
        }
    // Secondary constructor for Firestore deserialization, ensures `id` is always present.
    // No-argument constructor is required by Firestore for deserialization to a custom object.
    // It's good practice to initialize all fields to default values.
    constructor() : this(
        id = UUID.randomUUID().toString(), // Generate a new ID if none provided
        userId = "",
        lastUpdated = System.currentTimeMillis(),
        halfDurationMinutes = 45,
        halftimeDurationMinutes = 15,
        extraTimeHalfDurationMinutes = 15,
        extraTimeHalftimeDurationMinutes = 1,
        gameNumber = "XXXX",
        fieldNumber = null,
        homeTeamName = "",
        awayTeamName = "",
        homeTeamAbbr = null,
        awayTeamAbbr = null,
        homeCaptainNumber = null,
        awayCaptainNumber = null,
        homeRoster = emptyList(),
        awayRoster = emptyList(),
        homeOfficials = emptyList(),
        awayOfficials = emptyList(),
        ageGroup = null,
        competition = null,
        refereeAssignment = null,
        venue = null,
        fourthOfficial = null,
        observer = null,
        mainReferee = null,
        assistantReferee1 = null,
        assistantReferee2 = null,
        gameDateTimeEpochMillis = null,
        notes = null,
        inAddedTime = false,
        hasExtraTime = false,
        hasPenalties = false,
        hasTemporaryDismissals = false,
        penaltyKicksPerTeam = 5,
        homeTeamColorArgb = Color.Yellow.toArgb(),
        awayTeamColorArgb = DefaultAwayJerseyColor.toArgb(),
        kickOffTeam = Team.HOME,
        penaltiesTakenHome = 0,
        penaltiesTakenAway = 0,
        currentPhase = GamePhase.NOT_STARTED,
        homeScore = 0,
        awayScore = 0,
        displayedTimeMillis = 2700000L,
        actualTimeElapsedInPeriodMillis = 0L,
        stoppageTimeMillis = 0L,
        isTimerRunning = false,
        isStoppageTimerRunning = false,
        needsSyncWithPhone = false,
        maxSubstitutionsAllowed = 5,
        temporaryDismissalMinutes = 0,
        events = emptyList()
    )

    companion object {
        fun defaults(): Game {
            return Game( // Call primary constructor with all defaults explicitly for clarity
                id = UUID.randomUUID().toString(),
                userId = "",
                lastUpdated = System.currentTimeMillis(),
                halfDurationMinutes = 45,
                halftimeDurationMinutes = 15,
                extraTimeHalfDurationMinutes = 15,
                extraTimeHalftimeDurationMinutes = 1,
                gameNumber = "XXXX",
                fieldNumber = null,
                homeTeamName = "",
                awayTeamName = "",
                homeTeamAbbr = null,
                awayTeamAbbr = null,
                homeCaptainNumber = null,
                awayCaptainNumber = null,
                homeRoster = emptyList(),
                awayRoster = emptyList(),
                homeOfficials = emptyList(),
                awayOfficials = emptyList(),
                ageGroup = null,
                competition = null,
                refereeAssignment = null,
                venue = null,
                gameDateTimeEpochMillis = null,
                notes = null,
                stoppageTimeMillis = 0L,
                inAddedTime = false,
                hasExtraTime = false,
                hasPenalties = false,
                hasTemporaryDismissals = false,
                penaltyKicksPerTeam = 5,
                homeTeamColorArgb = Color.Yellow.toArgb(),
                awayTeamColorArgb = DefaultAwayJerseyColor.toArgb(),
                kickOffTeam = Team.HOME,
                penaltiesTakenHome = 0,
                penaltiesTakenAway = 0,
                currentPhase = GamePhase.NOT_STARTED,
                homeScore = 0,
                awayScore = 0,
                displayedTimeMillis = 45L * 60 * 1000, // Default to half duration in millis
                actualTimeElapsedInPeriodMillis = 0L,
                isTimerRunning = false,
                isStoppageTimerRunning = false,
                needsSyncWithPhone = false, // Not typically set in defaults directly
                maxSubstitutionsAllowed = 5,
                temporaryDismissalMinutes = 0,
                events = emptyList()
            )
        }
    }
    // Constructor to initialize from SimpleIcsEvent
    constructor(icsEvent: SimpleIcsEvent) : this(
        id = icsEvent.uid ?: UUID.randomUUID().toString(),
        gameNumber = icsEvent.gameNumber ?: "XXXX",
        fieldNumber = icsEvent.fieldNumber,
        homeTeamName = icsEvent.homeTeam ?: "",
        awayTeamName = icsEvent.awayTeam ?: "",
        refereeAssignment = icsEvent.refereeAssignment,
        venue = icsEvent.location,
        gameDateTimeEpochMillis = icsEvent.dtStart?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
        halfDurationMinutes = icsEvent.ageGroup?.defaultHalfDurationMinutes ?: 45, 
        halftimeDurationMinutes = icsEvent.ageGroup?.defaultHalftimeDurationMinutes ?: 10, 
        homeCaptainNumber = null,
        awayCaptainNumber = null,
        homeRoster = emptyList(),
        awayRoster = emptyList(),
        homeOfficials = emptyList(),
        awayOfficials = emptyList(),
        ageGroup = icsEvent.ageGroup,
        notes = listOfNotNull(icsEvent.summary, icsEvent.ageGroup?.notes).joinToString(" / ").ifEmpty { null },
        // Other fields will take defaults from the primary constructor via `this(...)` call
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
                val scoringTeam = if (eventToRemove.goalType == GoalType.OWN_GOAL) eventToRemove.team.opposite() else eventToRemove.team
                gameWithEventRemoved = gameWithEventRemoved.copy(
                    homeScore = if (scoringTeam == Team.HOME) gameWithEventRemoved.homeScore - 1 else gameWithEventRemoved.homeScore,
                    awayScore = if (scoringTeam == Team.AWAY) gameWithEventRemoved.awayScore - 1 else gameWithEventRemoved.awayScore
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
        } 

        return gameWithEventRemoved
    }
}

// --- Game Extension Functions ---

/** Coerces a user-entered "kicks per team" value into the legal 1..20 range.
 *  Used when the watch / mobile form accepts arbitrary input from the referee. */
fun Game.clampPenaltyKicksPerTeam(): Int = penaltyKicksPerTeam.coerceIn(1, 20)

/**
 * Returns the regulation period duration in milliseconds for the given phase.
 * For halftime phases, returns the halftime duration.
 */
fun Game.regulationPeriodDurationMillis(phase: GamePhase = currentPhase): Long {
    return when (phase) {
        GamePhase.FIRST_HALF, GamePhase.SECOND_HALF -> halfDurationMinutes * 60 * 1000L
        GamePhase.EXTRA_TIME_FIRST_HALF, GamePhase.EXTRA_TIME_SECOND_HALF -> extraTimeHalfDurationMinutes * 60 * 1000L
        GamePhase.HALF_TIME -> halftimeDurationMinutes * 60 * 1000L
        GamePhase.EXTRA_TIME_HALF_TIME -> extraTimeHalftimeDurationMinutes * 60 * 1000L
        else -> halfDurationMinutes * 60 * 1000L // Default to half duration
    }
}

/**
 * Converts the game to a snapshot suitable for storage (e.g., Firestore).
 * Excludes certain fields that shouldn't be stored or synced.
 */
fun Game.toSnapshotForStorage(): Map<String, Any?> {
    return mapOf(
        "id" to id,
        "userId" to userId,
        "lastUpdated" to lastUpdated,
        "halfDurationMinutes" to halfDurationMinutes,
        "halftimeDurationMinutes" to halftimeDurationMinutes,
        "extraTimeHalfDurationMinutes" to extraTimeHalfDurationMinutes,
        "extraTimeHalftimeDurationMinutes" to extraTimeHalftimeDurationMinutes,
        "gameNumber" to gameNumber,
        "fieldNumber" to fieldNumber,
        "homeTeamName" to homeTeamName,
        "awayTeamName" to awayTeamName,
        "homeTeamAbbr" to homeTeamAbbr,
        "awayTeamAbbr" to awayTeamAbbr,
        "homeCaptainNumber" to homeCaptainNumber,
        "awayCaptainNumber" to awayCaptainNumber,
        "homeRoster" to homeRoster.map { mapOf("name" to it.name, "number" to it.number, "isCaptain" to it.isCaptain, "isOnField" to it.isOnField) },
        "awayRoster" to awayRoster.map { mapOf("name" to it.name, "number" to it.number, "isCaptain" to it.isCaptain, "isOnField" to it.isOnField) },
        "homeOfficials" to homeOfficials.map { mapOf("id" to it.id, "name" to it.name, "role" to it.role, "number" to it.number) },
        "awayOfficials" to awayOfficials.map { mapOf("id" to it.id, "name" to it.name, "role" to it.role, "number" to it.number) },
        "ageGroup" to ageGroup?.name,
        "competition" to competition,
        "refereeAssignment" to refereeAssignment,
        "venue" to venue,
        "fourthOfficial" to fourthOfficial,
        "observer" to observer,
        "mainReferee" to mainReferee,
        "assistantReferee1" to assistantReferee1,
        "assistantReferee2" to assistantReferee2,
        "gameDateTimeEpochMillis" to gameDateTimeEpochMillis,
        "notes" to notes,
        "inAddedTime" to inAddedTime,
        "hasExtraTime" to hasExtraTime,
        "hasPenalties" to hasPenalties,
        "hasTemporaryDismissals" to hasTemporaryDismissals,
        "penaltyKicksPerTeam" to penaltyKicksPerTeam,
        "homeTeamColorArgb" to homeTeamColorArgb,
        "awayTeamColorArgb" to awayTeamColorArgb,
        "kickOffTeam" to kickOffTeam.name,
        "penaltiesTakenHome" to penaltiesTakenHome,
        "penaltiesTakenAway" to penaltiesTakenAway,
        "currentPhase" to currentPhase.name,
        "homeScore" to homeScore,
        "awayScore" to awayScore,
        "displayedTimeMillis" to displayedTimeMillis,
        "actualTimeElapsedInPeriodMillis" to actualTimeElapsedInPeriodMillis,
        "stoppageTimeMillis" to stoppageTimeMillis,
        "isTimerRunning" to isTimerRunning,
        "isStoppageTimerRunning" to isStoppageTimerRunning,
        "maxSubstitutionsAllowed" to maxSubstitutionsAllowed,
        "temporaryDismissalMinutes" to temporaryDismissalMinutes,
        "events" to events.map { event ->
            val jsonElement = AppJsonConfiguration.encodeToJsonElement(GameEvent.serializer(), event)
            jsonElementToAny(jsonElement)
        }
    )
}

/**
 * Converts the game to a Firestore-compatible map.
 */
fun Game.toFirestoreMap(): Map<String, Any?> = toSnapshotForStorage()

/**
 * Returns true if the game is tied (home and away scores are equal).
 */
val Game.isTied: Boolean
    get() = homeScore == awayScore

/**
 * Returns the formatted game date and time as a string.
 */
val Game.formattedGameDateTime: String
    get() {
        return gameDateTimeEpochMillis?.let {
            val sdf = java.text.SimpleDateFormat("EEE, MMM d, yyyy HH:mm", java.util.Locale.getDefault())
            sdf.format(java.util.Date(it))
        } ?: "Kein Datum festgelegt"
    }

/**
 * Returns the home team color as a Color object.
 */
val Game.homeTeamColor: androidx.compose.ui.graphics.Color
    get() = androidx.compose.ui.graphics.Color(homeTeamColorArgb)

/**
 * Returns the away team color as a Color object.
 */
val Game.awayTeamColor: androidx.compose.ui.graphics.Color
    get() = androidx.compose.ui.graphics.Color(awayTeamColorArgb)
