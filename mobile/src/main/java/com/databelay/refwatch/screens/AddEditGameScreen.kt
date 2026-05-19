package com.databelay.refwatch.commons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.databelay.refwatch.common.theme.DefaultAwayJerseyColor
import com.databelay.refwatch.common.theme.DefaultHomeJerseyColor
import com.google.firebase.firestore.Excluderangement
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import kotlinx.serialization.Serializable.Row
import kotlinx.serialization.encodeToStringpacer
import kotlinx.serialization.json.jsonObjectllMaxWidth
import java.text.SimpleDateFormatn.layout.height
import java.time.ZoneId.foundation.layout.heightIn
import java.util.Datese.foundation.layout.padding
import java.util.Locale.foundation.layout.size
import java.util.UUIDse.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
// --- Player Model ---.foundation.text.KeyboardOptions
@Serializabledx.compose.foundation.verticalScroll
@IgnoreExtraPropertiese.material.icons.Icons
data class Player(mpose.material.icons.automirrored.filled.ArrowBack
    val name: String,se.material.icons.filled.DateRange
    val number: Int,ose.material.icons.filled.Delete
    val isCaptain: Boolean = falsecons.filled.Save
)mport androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
// --- Game Settings ---material3.DatePicker
@Serializable // Add thisaterial3.DatePickerDialog
@IgnoreExtraProperties // Add this annotation to the class
data class Game(compose.material3.FloatingActionButton
    // --- Core Game Mechanics Settings ---
    val id: String = UUID.randomUUID().toString(), // Unique ID for these game settings instance
    val userId: String = "", // User who created this game (for cache and syncing
    var lastUpdated: Long = System.currentTimeMillis(), // Timestamp for when this was last updated by user
    var halfDurationMinutes: Int = 45,inedTextFieldDefaults
    var halftimeDurationMinutes: Int = 15,
    var extraTimeHalfDurationMinutes: Int = 15, // Optional for future
    var extraTimeHalftimeDurationMinutes: Int = 1, // Optional for future
//port androidx.compose.material3.TimePicker
    // --- Match Information (can be pre-filled from a schedule) ---le) ---
    var gameNumber: String = "XXXX", // Default, can be overridden
    var fieldNumber: String? = null, // Default, can be overridden
    var homeTeamName: String = "", // Default, can be overriddenn be overridden
    var awayTeamName: String = "", // Default, can be overridden
    var homeTeamAbbr: String? = null,hedEffect
    var awayTeamAbbr: String? = null,lue
    var homeCaptainNumber: Int? = null,StateOf
    var awayCaptainNumber: Int? = null,r
    @PropertyName("homeRoster").setValue
    var homeRoster: List<Player> = emptyList(),,
    @PropertyName("awayRoster")fier
    var awayRoster: List<Player> = emptyList(),
    var ageGroup: AgeGroup? = null,          // e.g., "U12 Boys", "Adult Men"
    var competition: String? = null,       // e.g., "League Match", "Cup Final"
    var refereeAssignment: String? = null, // e.g. Assistant Refereent Referee
    var venue: String? = null,             // e.g., "Field 3, West Park"
    var gameDateTimeEpochMillis: Long? = null, // Start date & time of the match, UTC epoch msch, UTC epoch ms
    var notes: String? = null,t.dp
    // Live State Fields (updated by watch, synced via phone to Firebase)
    val inAddedTime: Boolean = false, // Is the current playable period in added time?period in added time?
    var hasExtraTime: Boolean = false, // True if extra time has been initiatedinitiated
    var hasPenalties: Boolean = false, // True if extra time has been initiatedf extra time has been initiated
    var homeTeamColorArgb: Int = Color.Yellow.toArgb(),
    var awayTeamColorArgb: Int = DefaultAwayJerseyColor.toArgb(),
    var kickOffTeam: Team = Team.HOME, // Actual team kicking off current period (managed by ViewModel)
    var penaltiesTakenHome: Int = 0, // Number of penalties scored by home team
    var penaltiesTakenAway: Int = 0, // Number of penalties scored by away teamnalties scored by away team
    var currentPhase: GamePhase = GamePhase.NOT_STARTED,STARTED,
    var homeScore: Int = 0,= 0,
    var awayScore: Int = 0,
    var displayedTimeMillis: Long = 45,dTimeMillis: Long = 45,
    var actualTimeElapsedInPeriodMillis: Long = 0L,
    var stoppageTimeMillis: Long = 0L,
    var isTimerRunning: Boolean = false,
    var isStoppageTimerRunning: Boolean = false,merRunning: Boolean = false,
    @get:Exclude
    val needsSyncWithPhone: Boolean = false, // Store locally on watch, needs sync with phone
    var maxSubstitutionsAllowed: Int = 5,,
    @get:Excludeck: () -> Unit,
    val events: List<GameEvent> = emptyList() // We will sync this one manually
)  {onAwayTeamNameChange: (String) -> Unit,
    // Computed property for GameStatusnit,
    // If you use Kotlinx.serialization and don't want this in Firestore,
    // you might not need @Transient if it's just a getter.
    // If it were a var with a backing field you didn't want to store, you'd use @Transient.
    val status: GameStatusist<com.databelay.refwatch.common.Player>) -> Unit,
        get() {rChange: (List<com.databelay.refwatch.common.Player>) -> Unit,
            return currentPhase.status()t,
        }ereeAssignmentChange: (String) -> Unit,
    // Secondary constructor for Firestore deserialization, ensures `id` is always present.
    // No-argument constructor is required by Firestore for deserialization to a custom object..
    // It's good practice to initialize all fields to default values.
    constructor() : this( (String) -> Unit,
        id = UUID.randomUUID().toString(), // Generate a new ID if none provided
        userId = "",urationChange: (String) -> Unit,
        lastUpdated = System.currentTimeMillis(),lis(),
        halfDurationMinutes = 45,-> Unit,
        halftimeDurationMinutes = 15,nit,
        extraTimeHalfDurationMinutes = 15,
        extraTimeHalftimeDurationMinutes = 1,ew callback
        gameNumber = "XXXX",ng) -> Unit, // New callback,
        fieldNumber = null,
        homeTeamName = "",ng, String) -> Unit,
        awayTeamName = "",layer) -> Unit,
        homeTeamAbbr = null,, String) -> Unit,
        awayTeamAbbr = null,yer) -> Unit,     awayTeamAbbr = null,
        homeCaptainNumber = null,tring) -> Unit,
        awayCaptainNumber = null,it,        awayCaptainNumber = null,
        homeRoster = emptyList(), -> Unit,
        awayRoster = emptyList(),-> Unit,
        ageGroup = null,: (String) -> Unit,   ageGroup = null,
        competition = null,e: (String) -> Unit,        competition = null,
        refereeAssignment = null,: (String) -> Unit,
        venue = null,sChange: (String) -> Unit,
        gameDateTimeEpochMillis = null,t,
        notes = null,ed: (Color) -> Unit,
        inAddedTime = false, -> Unit,
        hasExtraTime = false,g) -> Unit, // New callbackfalse,
        hasPenalties = false,g) -> Unit, // New callback   hasPenalties = false,
        homeTeamColorArgb = Color.Yellow.toArgb(),
        awayTeamColorArgb = DefaultAwayJerseyColor.toArgb(),
        kickOffTeam = Team.HOME, -> Unit,
        penaltiesTakenHome = 0,tring) -> Unit,
        penaltiesTakenAway = 0,) -> Unit,        penaltiesTakenAway = 0,
        currentPhase = GamePhase.NOT_STARTED,it,ntPhase = GamePhase.NOT_STARTED,
        homeScore = 0,ring) -> Unit,= 0,
        awayScore = 0,e: (String) -> Unit,
        displayedTimeMillis = 45,-> Unit,
        actualTimeElapsedInPeriodMillis = 0L,lis = 0L,
        stoppageTimeMillis = 0L,tring) -> Unit,
        isTimerRunning = false,ge: (String) -> Unit,
        isStoppageTimerRunning = false,-> Unit,erRunning = false,
        needsSyncWithPhone = false, Unit,WithPhone = false,
        maxSubstitutionsAllowed = 5,Unit,bstitutionsAllowed = 5,
        events = emptyList() -> Unit,ents = emptyList()
    )nHomeScoreChange: (String) -> Unit, // New callback
    onAwayScoreChange: (String) -> Unit, // New callback
    companion object {Unit,
        fun defaults(): Game {String) -> Unit,efaults(): Game {
            return Game( // Call primary constructor with all defaults explicitly for clarity defaults explicitly for clarity
                id = UUID.randomUUID().toString(),ndomUUID().toString(),
                userId = "",yer) -> Unit, userId = "",
                lastUpdated = System.currentTimeMillis(),rentTimeMillis(),
                halfDurationMinutes = 45,
                halftimeDurationMinutes = 15,nutes = 15,
                extraTimeHalfDurationMinutes = 15,
                extraTimeHalftimeDurationMinutes = 1,
                gameNumber = "XXXX",g) -> Unit,     gameNumber = "XXXX",
                fieldNumber = null,(String) -> Unit,null,
                homeTeamName = "",ing) -> Unit,
                awayTeamName = "",> Unit,
                homeTeamAbbr = null,Unit,
                awayTeamAbbr = null,,
                homeCaptainNumber = null,// New callback
                awayCaptainNumber = null,// New callback
                homeRoster = emptyList(),
                awayRoster = emptyList(),Unit,   awayRoster = emptyList(),
                ageGroup = null, -> Unit,l,
                competition = null,g) -> Unit,
                refereeAssignment = null,
                venue = null,: (String) -> Unit,
                gameDateTimeEpochMillis = null,
                notes = null,ing) -> Unit,
                stoppageTimeMillis = 0L,,
                inAddedTime = false,> Unit,se,
                hasExtraTime = false,) -> Unit,   hasExtraTime = false,
                hasPenalties = false,tring) -> Unit,
                homeTeamColorArgb = Color.Yellow.toArgb(),low.toArgb(),
                awayTeamColorArgb = DefaultAwayJerseyColor.toArgb(),
                kickOffTeam = Team.HOME,,
                penaltiesTakenHome = 0,
                penaltiesTakenAway = 0,, // New callback
                currentPhase = GamePhase.NOT_STARTED,ackD,
                homeScore = 0,
                awayScore = 0,String) -> Unit,wayScore = 0,
                displayedTimeMillis = 45L * 60 * 1000, // Default to half duration in millis millis
                actualTimeElapsedInPeriodMillis = 0L,
                isTimerRunning = false,t,
                isStoppageTimerRunning = false,,
                needsSyncWithPhone = false, // Not typically set in defaults directly
                maxSubstitutionsAllowed = 5,
                events = emptyList()Unit,
            )ationChange: (String) -> Unit,
        }ftimeDurationChange: (String) -> Unit,
    }nExtraTimeHalfDurationChange: (String) -> Unit,    }
    // Constructor to initialize from SimpleIcsEventimpleIcsEvent
    constructor(icsEvent: SimpleIcsEvent) : this(
        id = icsEvent.uid ?: UUID.randomUUID().toString(),ring(),
        gameNumber = icsEvent.gameNumber ?: "XXXX",
        fieldNumber = icsEvent.fieldNumber, New callback
        homeTeamName = icsEvent.homeTeam ?: "", callback
        awayTeamName = icsEvent.awayTeam ?: "",
        refereeAssignment = icsEvent.refereeAssignment,
        venue = icsEvent.location,> Unit,
        gameDateTimeEpochMillis = icsEvent.dtStart?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),.toInstant()?.toEpochMilli(),
        halfDurationMinutes = icsEvent.ageGroup?.defaultHalfDurationMinutes ?: 45, ?: 45, 
        halftimeDurationMinutes = icsEvent.ageGroup?.defaultHalftimeDurationMinutes ?: 10, 
        homeCaptainNumber = null,it,
        awayCaptainNumber = null, -> Unit,
        homeRoster = emptyList(),-> Unit,
        awayRoster = emptyList(),) -> Unit,
        ageGroup = icsEvent.ageGroup,) -> Unit,
        notes = listOfNotNull(icsEvent.summary, icsEvent.ageGroup?.notes).joinToString(" / ").ifEmpty { null },inToString(" / ").ifEmpty { null },
        // Other fields will take defaults from the primary constructor via `this(...)` cally constructor via `this(...)` call
    )nHomeColorSelected: (Color) -> Unit,
    onAwayColorSelected: (Color) -> Unit,
    // --- Methods to modify events ------
    fun addEvent(event: GameEvent): Game {/ New callback
        val updatedEvents = events + event/ New callback
        return this.copy(events = updatedEvents, lastUpdated = System.currentTimeMillis())
    }nAddHomePlayer: (String, String) -> Unit,
    onRemoveHomePlayer: (Player) -> Unit,
    // In Game.kter: (String, String) -> Unit,
    fun removeEvent(eventToRemove: GameEvent): Game {move: GameEvent): Game {
        if (!events.contains(eventToRemove)) {t,ToRemove)) {
            return this // Event not found
        }petitionChange: (String) -> Unit,
        val updatedEvents = events.filterNot { it == eventToRemove }
        var gameWithEventRemoved = this.copy(events = updatedEvents, lastUpdated = System.currentTimeMillis())
    onHalftimeDurationChange: (String) -> Unit,
        // Adjust scores/stats based on the type of event removedf event removed
        when (eventToRemove) {(String) -> Unit,ntToRemove) {
            is GoalScoredEvent -> { Unit,s GoalScoredEvent -> {
                val scoringTeam = if (eventToRemove.goalType == GoalType.OWN_GOAL) eventToRemove.team.opposite() else eventToRemove.teamRemove.team
                gameWithEventRemoved = gameWithEventRemoved.copy(
                    homeScore = if (scoringTeam == Team.HOME) gameWithEventRemoved.homeScore - 1 else gameWithEventRemoved.homeScore,
                    awayScore = if (scoringTeam == Team.AWAY) gameWithEventRemoved.awayScore - 1 else gameWithEventRemoved.awayScore
                )) -> Unit,
            }Player: (String, String) -> Unit,
             is PenaltyEvent -> {-> Unit,
                gameWithEventRemoved = gameWithEventRemoved.copy(oved = gameWithEventRemoved.copy(
                    homeScore = if (eventToRemove.team == Team.HOME && eventToRemove.scored) gameWithEventRemoved.homeScore - 1 else gameWithEventRemoved.homeScore,ved.homeScore,
                    awayScore = if (eventToRemove.team == Team.AWAY && eventToRemove.scored) gameWithEventRemoved.awayScore - 1 else gameWithEventRemoved.awayScore,
                    penaltiesTakenHome = if (eventToRemove.team == Team.HOME) (gameWithEventRemoved.penaltiesTakenHome - 1).coerceAtLeast(0) else gameWithEventRemoved.penaltiesTakenHome,e,
                    penaltiesTakenAway = if (eventToRemove.team == Team.AWAY) (gameWithEventRemoved.penaltiesTakenAway - 1).coerceAtLeast(0) else gameWithEventRemoved.penaltiesTakenAway
                )eChange: (Long) -> Unit,
            }ationChange: (String) -> Unit,
            else -> {nChange: (String) -> Unit,
    onExtraTimeHalfDurationChange: (String) -> Unit,
            }titutionsChange: (String) -> Unit,
        } ColorSelected: (Color) -> Unit,
    onAwayColorSelected: (Color) -> Unit,
        return gameWithEventRemovedt,
    }nHomeScoreChange: (String) -> Unit, // New callback
}   onAwayScoreChange: (String) -> Unit, // New callback
    onSaveGame: () -> Unit,
// --- UI State ---: (String, String) -> Unit,
_uiState.value = AddEditGameUiState(Unit,
    isEditing = false,String, String) -> Unit,alse,
    gameId = null,ayer: (Player) -> Unit,null,
    gameDateTimeEpochMillis = System.currentTimeMillis()urrentTimeMillis()
)   onVenueChange: (String) -> Unit,
    onCompetitionChange: (String) -> Unit,
// --- Game Save ---ange: (Long) -> Unit,
fun saveGame(gameToSave: Game) {g) -> Unit,
    // Save the game to Firestorering) -> Unit,
    // gameToSave = existingGame.copy(ring) -> Unit,copy(
    //                     gameNumber = currentState.gameNumber,eNumber = currentState.gameNumber,
    //                     homeTeamName = currentState.homeTeamName,homeTeamName = currentState.homeTeamName,
    //                     awayTeamName = currentState.awayTeamName,awayTeamName = currentState.awayTeamName,
    //                     homeTeamAbbr = currentState.homeTeamAbbr.ifBlank { null },
    //                     awayTeamAbbr = currentState.awayTeamAbbr.ifBlank { null },
    //                     homeCaptainNumber = currentState.homeCaptainNumber.toIntOrNull(),ike the game's location, weather, etc.    // This is a simplified example, you may need to handle errors and loading states    // Save the game settings to Firestorefun saveGame(gameToSave: Game) {// --- Game Save ---                label = { Text(stringResource(R.string.competition_optional)) },
    //                     awayCaptainNumber = currentState.awayCaptainNumber.toIntOrNull(),
    //                     fieldNumber = currentState.fieldNumber.takeIf { it.isNotBlank() },
    //                     refereeAssignment = currentState.refereeAssignment.takeIf { it.isNotBlank() },reeAssignment.takeIf { it.isNotBlank() },
    //                     venue = currentState.venue.takeIf { it.isNotBlank() },
    //                     competition = currentState.competition.takeIf { it.isNotBlank() },eIf { it.isNotBlank() },
    //                     gameDateTimeEpochMillis = currentState.gameDateTimeEpochMillis,meEpochMillis,
    //                     halfDurationMinutes = currentState.halfDurationMinutes,
    //                     halftimeDurationMinutes = currentState.halftimeDurationMinutes,
    //                     extraTimeHalfDurationMinutes = currentState.extraTimeHalfDurationMinutes,
    //                     maxSubstitutionsAllowed = currentState.maxSubstitutionsAllowed,
    //                     homeTeamColorArgb = currentState.homeTeamColorArgb,mColorArgb,
    //                     awayTeamColorArgb = currentState.awayTeamColorArgb,
    //                     kickOffTeam = currentState.kickOffTeam,
    //                     notes = currentState.notes.takeIf { it.isNotBlank() },
    //                     ageGroup = currentState.ageGroup,
    //                     homeScore = currentState.homeScore.toIntOrNull() ?: existingGame.homeScore,
    //                     awayScore = currentState.awayScore.toIntOrNull() ?: existingGame.awayScore,
    //                     homeRoster = currentState.homeRoster,
    //                     awayRoster = currentState.awayRoster,ster, cursor
    //                     lastUpdated = System.currentTimeMillis()meMillis()
    //                 )(Player) -> Unit,
}   onAddAwayPlayer: (String, String) -> Unit,Game(                   colors = OutlinedTextFieldDefaults.colors(
    onRemoveAwayPlayer: (Player) -> Unit,edTextColor = MaterialTheme.colorScheme.onSurface,









    onSaveGame: () -> Unit    onRemoveAwayPlayer: (Player) -> Unit,    onAddAwayPlayer: (String, String) -> Unit,    onRemoveHomePlayer: (Player) -> Unit,    onAddHomePlayer: (String, String) -> Unit,    onAwayScoreChange: (String) -> Unit, // New callbackonHomeScoreChange: (String) -> Unit, // New callback// --- Game UI Callbacks ---    onRefereeAssignmentChange: (String) -> Unit,
    onVenueChange: (String) -> Unit,
    onCompetitionChange: (String) -> Unit,
    onGameDateTimeChange: (Long) -> Unit,
    onHalfDurationChange: (String) -> Unit,
    onHalftimeDurationChange: (String) -> Unit,
    onExtraTimeHalfDurationChange: (String) -> Unit,
    onMaxSubstitutionsChange: (String) -> Unit,
    onHomeColorSelected: (Color) -> Unit,
    onAwayColorSelected: (Color) -> Unit,
    onNotesChanged: (String) -> Unit,
    onHomeScoreChange: (String) -> Unit, // New callback
    onAwayScoreChange: (String) -> Unit, // New callback
    onSaveGame: () -> Unit,
    onAddHomePlayer: (String, String) -> Unit,
    onRemoveHomePlayer: (Player) -> Unit,
    onAddAwayPlayer: (String, String) -> Unit,
    onRemoveAwayPlayer: (Player) -> Unit,
    onRefereeAssignmentChange: (String) -> Unit,
    onVenueChange: (String) -> Unit,
    onCompetitionChange: (String) -> Unit,
    onGameDateTimeChange: (Long) -> Unit,
    onHalfDurationChange: (String) -> Unit,
    onHalftimeDurationChange: (String) -> Unit,
    onExtraTimeHalfDurationChange: (String) -> Unit,
    onMaxSubstitutionsChange: (String) -> Unit,
    onHomeColorSelected: (Color) -> Unit,
    onAwayColorSelected: (Color) -> Unit,
    onNotesChanged: (String) -> Unit,
    onHomeScoreChange: (String) -> Unit, // New callback)                    lastUpdated = System.currentTimeMillis()                    awayRoster = currentState.awayRoster,                    homeRoster = currentState.homeRoster,                    awayScore = currentState.awayScore.toIntOrNull() ?: 0,                    homeScore = currentState.homeScore.toIntOrNull() ?: 0,                    ageGroup = currentState.ageGroup,                    notes = currentState.notes.takeIf { it.isNotBlank() },                    kickOffTeam = currentState.kickOffTeam,                    awayTeamColorArgb = currentState.awayTeamColorArgb,                    homeTeamColorArgb = currentState.homeTeamColorArgb,                    maxSubstitutionsAllowed = currentState.maxSubstitutionsAllowed,                    extraTimeHalfDurationMinutes = currentState.extraTimeHalfDurationMinutes,                    halftimeDurationMinutes = currentState.halftimeDurationMinutes,                    halfDurationMinutes = currentState.halfDurationMinutes,                    gameDateTimeEpochMillis = currentState.gameDateTimeEpochMillis,                    competition = currentState.competition.takeIf { it.isNotBlank() },                    venue = currentState.venue.takeIf { it.isNotBlank() },                    refereeAssignment = currentState.refereeAssignment.takeIf { it.isNotBlank() },                    fieldNumber = currentState.fieldNumber.takeIf { it.isNotBlank() },                    awayCaptainNumber = currentState.awayCaptainNumber.toIntOrNull(),                    homeCaptainNumber = currentState.homeCaptainNumber.toIntOrNull(),                    awayTeamAbbr = currentState.awayTeamAbbr.ifBlank { null },                    homeTeamAbbr = currentState.homeTeamAbbr.ifBlank { null },                    awayTeamName = currentState.awayTeamName,                    homeTeamName = currentState.homeTeamName,                    gameNumber = currentState.gameNumber,                        disabledBorderColor = MaterialTheme.colorScheme.outline,
    onAwayScoreChange: (String) -> Unit, // New callback           disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
    onSaveGame: () -> Unit,                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    onAddHomePlayer: (String, String) -> Unit,railingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
    onRemoveHomePlayer: (Player) -> Unit,
    onAddAwayPlayer: (String, String) -> Unit,
    onRemoveAwayPlayer: (Player) -> Unit,Icon(Icons.Filled.DateRange, contentDescription = stringResource(R.string.select_date)) }
    onRefereeAssignmentChange: (String) -> Unit,
    onVenueChange: (String) -> Unit, a covering Box
    onCompetitionChange: (String) -> Unit,
    onGameDateTimeChange: (Long) -> Unit,
    onHalfDurationChange: (String) -> Unit,
    onHalftimeDurationChange: (String) -> Unit,
    onExtraTimeHalfDurationChange: (String) -> Unit,
    onMaxSubstitutionsChange: (String) -> Unit,
    onHomeColorSelected: (Color) -> Unit,
    onAwayColorSelected: (Color) -> Unit,
    onNotesChanged: (String) -> Unit,DismissRequest = { showDatePicker = false },
    onHomeScoreChange: (String) -> Unit, // New callback
    onAwayScoreChange: (String) -> Unit, // New callback
    onSaveGame: () -> Unit,
    onAddHomePlayer: (String, String) -> Unit,{ selectedDate ->
    onRemoveHomePlayer: (Player) -> Unit,           val cal = Calendar.getInstance()
    onAddAwayPlayer: (String, String) -> Unit,             uiState.gameDateTimeEpochMillis?.let { cal.timeInMillis = it }
    onRemoveAwayPlayer: (Player) -> Unit,
    onRefereeAssignmentChange: (String) -> Unit,               timePickerState.hour = cal.get(Calendar.HOUR_OF_DAY)
    onVenueChange: (String) -> Unit,                   timePickerState.minute = cal.get(Calendar.MINUTE)
    onCompetitionChange: (String) -> Unit,                                showTimePicker = true
    onGameDateTimeChange: (Long) -> Unit,
    onHalfDurationChange: (String) -> Unit,stringResource(R.string.ok)) }
    onHalftimeDurationChange: (String) -> Unit,
    onExtraTimeHalfDurationChange: (String) -> Unit,
    onMaxSubstitutionsChange: (String) -> Unit,
    onHomeColorSelected: (Color) -> Unit,
    onAwayColorSelected: (Color) -> Unit,.string.cancel)) }
    onNotesChanged: (String) -> Unit,   }
    onHomeScoreChange: (String) -> Unit, // New callback
    onAwayScoreChange: (String) -> Unit, // New callback
    onSaveGame: () -> Unit,
    onAddHomePlayer: (String, String) -> Unit,
    onRemoveHomePlayer: (Player) -> Unit,
    onAddAwayPlayer: (String, String) -> Unit,
    onRemoveAwayPlayer: (Player) -> Unit,imePickerDialog(
    onRefereeAssignmentChange: (String) -> Unit,       onDismissRequest = { showTimePicker = false },
    onVenueChange: (String) -> Unit,
    onCompetitionChange: (String) -> Unit,(onClick = {
    onGameDateTimeChange: (Long) -> Unit,
    onHalfDurationChange: (String) -> Unit,
    onHalftimeDurationChange: (String) -> Unit,llis = it }
    onExtraTimeHalfDurationChange: (String) -> Unit,
    onMaxSubstitutionsChange: (String) -> Unit,E, timePickerState.minute)
    onHomeColorSelected: (Color) -> Unit,           cal.set(Calendar.SECOND, 0)
    onAwayColorSelected: (Color) -> Unit,t(Calendar.MILLISECOND, 0)
    onNotesChanged: (String) -> Unit,
    onHomeScoreChange: (String) -> Unit, // New callback) }
    onAwayScoreChange: (String) -> Unit, // New callback
    onSaveGame: () -> Unit,
    onAddHomePlayer: (String, String) -> Unit,
    onRemoveHomePlayer: (Player) -> Unit,icker = false
    onAddAwayPlayer: (String, String) -> Unit,       }) { Text(stringResource(R.string.cancel)) }
    onRemoveAwayPlayer: (Player) -> Unit,       }
    onRefereeAssignmentChange: (String) -> Unit,                ) {
    onVenueChange: (String) -> Unit,    TimePicker(state = timePickerState, modifier = Modifier.padding(16.dp))
    onCompetitionChange: (String) -> Unit,
    onGameDateTimeChange: (Long) -> Unit,
    onHalfDurationChange: (String) -> Unit,
    onHalftimeDurationChange: (String) -> Unit,(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    onExtraTimeHalfDurationChange: (String) -> Unit,
    onMaxSubstitutionsChange: (String) -> Unit,
    onHomeColorSelected: (Color) -> Unit,hange = onHalfDurationChange,
    onAwayColorSelected: (Color) -> Unit,ce(R.string.half_minutes)) },
    onNotesChanged: (String) -> Unit,rdOptions(keyboardType = KeyboardType.Number),
    onHomeScoreChange: (String) -> Unit, // New callback
    onAwayScoreChange: (String) -> Unit, // New callback
    onSaveGame: () -> Unit,
    onAddHomePlayer: (String, String) -> Unit,ing(),
    onRemoveHomePlayer: (Player) -> Unit,nValueChange = onHalftimeDurationChange,
    onAddAwayPlayer: (String, String) -> Unit,   label = { Text(stringResource(R.string.halftime_minutes)) },
    onRemoveAwayPlayer: (Player) -> Unit, = KeyboardType.Number),
    onRefereeAssignmentChange: (String) -> Unit,
    onVenueChange: (String) -> Unit,
    onCompetitionChange: (String) -> Unit,
    onGameDateTimeChange: (Long) -> Unit,izontalArrangement = Arrangement.spacedBy(8.dp)) {
    onHalfDurationChange: (String) -> Unit,
    onHalftimeDurationChange: (String) -> Unit, = uiState.extraTimeHalfDurationMinutes.toString(),
    onExtraTimeHalfDurationChange: (String) -> Unit,urationChange,
    onMaxSubstitutionsChange: (String) -> Unit,time_minutes)) },
    onHomeColorSelected: (Color) -> Unit,eyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    onAwayColorSelected: (Color) -> Unit,   modifier = Modifier.weight(1f)
    onNotesChanged: (String) -> Unit,   )
    onHomeScoreChange: (String) -> Unit, // New callback                OutlinedTextField(
    onAwayScoreChange: (String) -> Unit, // New callbackxSubstitutionsAllowed.toString(),
    onSaveGame: () -> Unit,ubstitutionsChange,
    onAddHomePlayer: (String, String) -> Unit,tions_mobile)) },
    onRemoveHomePlayer: (Player) -> Unit, KeyboardType.Number),
    onAddAwayPlayer: (String, String) -> Unit,
    onRemoveAwayPlayer: (Player) -> Unit,
    onRefereeAssignmentChange: (String) -> Unit,
    onVenueChange: (String) -> Unit,
    onCompetitionChange: (String) -> Unit,
    onGameDateTimeChange: (Long) -> Unit,
    onHalfDurationChange: (String) -> Unit,   Modifier.fillMaxWidth(),
    onHalftimeDurationChange: (String) -> Unit,= Arrangement.SpaceAround,
    onExtraTimeHalfDurationChange: (String) -> Unit,ent.CenterVertically
    onMaxSubstitutionsChange: (String) -> Unit,
    onHomeColorSelected: (Color) -> Unit,
    onAwayColorSelected: (Color) -> Unit,enterVertically) {
    onNotesChanged: (String) -> Unit,
    onHomeScoreChange: (String) -> Unit, // New callback
    onAwayScoreChange: (String) -> Unit, // New callback          .size(20.dp)
    onSaveGame: () -> Unit,eamColorArgb))
    onAddHomePlayer: (String, String) -> Unit,       )
    onRemoveHomePlayer: (Player) -> Unit,           Spacer(Modifier.width(4.dp))
    onAddAwayPlayer: (String, String) -> Unit,                        Text(stringResource(R.string.home_color))
    onRemoveAwayPlayer: (Player) -> Unit,
    onRefereeAssignmentChange: (String) -> Unit,
    onVenueChange: (String) -> Unit,nClick = { showAwayColorPicker = true }) {
    onCompetitionChange: (String) -> Unit,Alignment.CenterVertically) {
    onGameDateTimeChange: (Long) -> Unit,
    onHalfDurationChange: (String) -> Unit,               modifier = Modifier
    onHalftimeDurationChange: (String) -> Unit,                                .size(20.dp)
    onExtraTimeHalfDurationChange: (String) -> Unit,      .background(Color(uiState.awayTeamColorArgb))
    onMaxSubstitutionsChange: (String) -> Unit,
    onHomeColorSelected: (Color) -> Unit,Spacer(Modifier.width(4.dp))
    onAwayColorSelected: (Color) -> Unit,R.string.away_color))
    onNotesChanged: (String) -> Unit,
    onHomeScoreChange: (String) -> Unit, // New callback   }
    onAwayScoreChange: (String) -> Unit, // New callback            }
    onSaveGame: () -> Unit,
    onAddHomePlayer: (String, String) -> Unit,
    onRemoveHomePlayer: (Player) -> Unit,
    onAddAwayPlayer: (String, String) -> Unit,
    onRemoveAwayPlayer: (Player) -> Unit,Color(uiState.homeTeamColorArgb),
    onRefereeAssignmentChange: (String) -> Unit, = { color: Color ->
    onVenueChange: (String) -> Unit,(color)
    onCompetitionChange: (String) -> Unit,
    onGameDateTimeChange: (Long) -> Unit,       },
    onHalfDurationChange: (String) -> Unit,                    onDismiss = { showHomeColorPicker = false }
    onHalftimeDurationChange: (String) -> Unit,
    onExtraTimeHalfDurationChange: (String) -> Unit,
    onMaxSubstitutionsChange: (String) -> Unit,yColorPicker) {
    onHomeColorSelected: (Color) -> Unit,
    onAwayColorSelected: (Color) -> Unit,y_color),
    onNotesChanged: (String) -> Unit,   initialColor = Color(uiState.awayTeamColorArgb),
    onHomeScoreChange: (String) -> Unit, // New callback       onColorSelected = { color: Color ->
    onAwayScoreChange: (String) -> Unit, // New callback                        onAwayColorSelected(color)
    onSaveGame: () -> Unit,
    onAddHomePlayer: (String, String) -> Unit,           },
    onRemoveHomePlayer: (Player) -> Unit,               onDismiss = { showAwayColorPicker = false }
    onAddAwayPlayer: (String, String) -> Unit,               )
    onRemoveAwayPlayer: (Player) -> Unit,            }
    onRefereeAssignmentChange: (String) -> Unit,
    onVenueChange: (String) -> Unit,
    onCompetitionChange: (String) -> Unit, Text("Heim-Kader", style = MaterialTheme.typography.titleMedium)
    onGameDateTimeChange: (Long) -> Unit,ut(
    onHalfDurationChange: (String) -> Unit,homeRoster,
    onHalftimeDurationChange: (String) -> Unit,
    onExtraTimeHalfDurationChange: (String) -> Unit,         )
    onMaxSubstitutionsChange: (String) -> Unit,
    onHomeColorSelected: (Color) -> Unit,
    onAwayColorSelected: (Color) -> Unit,            Text("Gast-Kader", style = MaterialTheme.typography.titleMedium)
    onNotesChanged: (String) -> Unit,
    onHomeScoreChange: (String) -> Unit, // New callback                roster = uiState.awayRoster,
    onAwayScoreChange: (String) -> Unit, // New callbackge = onAwayRosterChange
    onSaveGame: () -> Unit,
    onAddHomePlayer: (String, String) -> Unit,
    onRemoveHomePlayer: (Player) -> Unit,
    onAddAwayPlayer: (String, String) -> Unit,       value = uiState.notes,
    onRemoveAwayPlayer: (Player) -> Unit,
    onRefereeAssignmentChange: (String) -> Unit,ing.notes_optional)) },
    onVenueChange: (String) -> Unit,
    onCompetitionChange: (String) -> Unit,
    onGameDateTimeChange: (Long) -> Unit,           .heightIn(min = 80.dp),
    onHalfDurationChange: (String) -> Unit,           keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
    onHalftimeDurationChange: (String) -> Unit,            )
    onExtraTimeHalfDurationChange: (String) -> Unit,
    onMaxSubstitutionsChange: (String) -> Unit,essage?.let {
    onHomeColorSelected: (Color) -> Unit,
    onAwayColorSelected: (Color) -> Unit,
    onNotesChanged: (String) -> Unit,
    onHomeScoreChange: (String) -> Unit, // New callback
    onAwayScoreChange: (String) -> Unit, // New callback
    onSaveGame: () -> Unit,
    onAddHomePlayer: (String, String) -> Unit,
    onRemoveHomePlayer: (Player) -> Unit,
    onAddAwayPlayer: (String, String) -> Unit,
    onRemoveAwayPlayer: (Player) -> Unit,
    onRefereeAssignmentChange: (String) -> Unit,
    onVenueChange: (String) -> Unit,
    onCompetitionChange: (String) -> Unit,
    onGameDateTimeChange: (Long) -> Unit,
    onHalfDurationChange: (String) -> Unit,
    onHalftimeDurationChange: (String) -> Unit,
    onExtraTimeHalfDurationChange: (String) -> Unit,
    onMaxSubstitutionsChange: (String) -> Unit,
    onHomeColorSelected: (Color) -> Unit,
    onAwayColorSelected: (Color) -> Unit,
    onNotesChanged: (String) -> Unit,WithLifecycle()
    onHomeScoreChange: (String) -> Unit, // New callback
    onAwayScoreChange: (String) -> Unit, // New callback
    onSaveGame: () -> Unit,
    onAddHomePlayer: (String, String) -> Unit,
    onRemoveHomePlayer: (Player) -> Unit,
    onAddAwayPlayer: (String, String) -> Unit,            tag,
    onRemoveAwayPlayer: (Player) -> Unit,   "LaunchedEffect triggered. gameIdFromNav: $gameId, Current uiState.gameId: ${uiState.gameId}, isEditing: ${uiState.isEditing}"
    onRefereeAssignmentChange: (String) -> Unit,   )
    onVenueChange: (String) -> Unit,       if (gameId != null && !uiState.isEditing) {
    onCompetitionChange: (String) -> Unit,            addEditViewModel.initializeForm(gameId)
    onGameDateTimeChange: (Long) -> Unit, {
    onHalfDurationChange: (String) -> Unit, addEditViewModel.initializeForm(null) // New game
    onHalftimeDurationChange: (String) -> Unit,
    onExtraTimeHalfDurationChange: (String) -> Unit,
    onMaxSubstitutionsChange: (String) -> Unit,
    onHomeColorSelected: (Color) -> Unit, AddEditGameScreen(
    onAwayColorSelected: (Color) -> Unit,
    onNotesChanged: (String) -> Unit,k() },
    onHomeScoreChange: (String) -> Unit, // New callback        onHomeTeamNameChange = addEditViewModel::onHomeTeamNameChange,
    onAwayScoreChange: (String) -> Unit, // New callback:onAwayTeamNameChange,
    onSaveGame: () -> Unit,:onHomeTeamAbbrChange,
    onAddHomePlayer: (String, String) -> Unit,amAbbrChange = addEditViewModel::onAwayTeamAbbrChange,
    onRemoveHomePlayer: (Player) -> Unit,l::onHomeCaptainNumberChange,
    onAddAwayPlayer: (String, String) -> Unit,NumberChange,
    onRemoveAwayPlayer: (Player) -> Unit,nge,
    onRefereeAssignmentChange: (String) -> Unit,osterChange = addEditViewModel::onAwayRosterChange,
    onVenueChange: (String) -> Unit,ditViewModel::onFieldNumberChange,
    onCompetitionChange: (String) -> Unit,ditViewModel::onRefereeAssignmentChange,
    onGameDateTimeChange: (Long) -> Unit,enueChange,
    onHalfDurationChange: (String) -> Unit,e,
    onHalftimeDurationChange: (String) -> Unit,
    onExtraTimeHalfDurationChange: (String) -> Unit,lfDurationChange,
    onMaxSubstitutionsChange: (String) -> Unit,tionChange = addEditViewModel::onHalftimeDurationChange,
    onHomeColorSelected: (Color) -> Unit,itViewModel::onExtraTimeHalfDurationChange,
    onAwayColorSelected: (Color) -> Unit,::onMaxSubstitutionsChange,
    onNotesChanged: (String) -> Unit,ViewModel::onHomeColorSelected,
    onHomeScoreChange: (String) -> Unit, // New callbackorSelected = addEditViewModel::onAwayColorSelected,
    onAwayScoreChange: (String) -> Unit, // New callbackewModel::onNotesChanged,
    onSaveGame: () -> Unit,coreChange, // Pass new handler
    onAddHomePlayer: (String, String) -> Unit,::onAwayScoreChange, // Pass new handler
    onRemoveHomePlayer: (Player) -> Unit,
    onAddAwayPlayer: (String, String) -> Unit,
    onRemoveAwayPlayer: (Player) -> Unit,
    onRefereeAssignmentChange: (String) -> Unit,
    onVenueChange: (String) -> Unit,
    onCompetitionChange: (String) -> Unit,
    onGameDateTimeChange: (Long) -> Unit,
    onHalfDurationChange: (String) -> Unit,
    onHalftimeDurationChange: (String) -> Unit,ut Composable ---
    onExtraTimeHalfDurationChange: (String) -> Unit,
    onMaxSubstitutionsChange: (String) -> Unit,
    onHomeColorSelected: (Color) -> Unit,
    onAwayColorSelected: (Color) -> Unit,
    onNotesChanged: (String) -> Unit,
    onHomeScoreChange: (String) -> Unit, // New callback
    onAwayScoreChange: (String) -> Unit, // New callback by remember { mutableStateOf("") }
    onSaveGame: () -> Unit,
    onAddHomePlayer: (String, String) -> Unit,n(modifier = Modifier.fillMaxWidth()) {
    onRemoveHomePlayer: (Player) -> Unit,er.forEachIndexed { index, player ->
    onAddAwayPlayer: (String, String) -> Unit,
    onRemoveAwayPlayer: (Player) -> Unit,
    onRefereeAssignmentChange: (String) -> Unit,By(8.dp),
    onVenueChange: (String) -> Unit,     verticalAlignment = Alignment.CenterVertically
    onCompetitionChange: (String) -> Unit,
    onGameDateTimeChange: (Long) -> Unit,d(
    onHalfDurationChange: (String) -> Unit,
    onHalftimeDurationChange: (String) -> Unit,
    onExtraTimeHalfDurationChange: (String) -> Unit,ter.toMutableList()
    onMaxSubstitutionsChange: (String) -> Unit,ster[index] = player.copy(name = name)
    onHomeColorSelected: (Color) -> Unit,           onRosterChange(updatedRoster)
    onAwayColorSelected: (Color) -> Unit,
    onNotesChanged: (String) -> Unit,("Name") },
    onHomeScoreChange: (String) -> Unit, // New callback
    onAwayScoreChange: (String) -> Unit, // New callback
    onSaveGame: () -> Unit,
    onAddHomePlayer: (String, String) -> Unit,
    onRemoveHomePlayer: (Player) -> Unit,r.number.toString(),
    onAddAwayPlayer: (String, String) -> Unit,       onValueChange = { num ->
    onRemoveAwayPlayer: (Player) -> Unit,datedRoster = roster.toMutableList()
    onRefereeAssignmentChange: (String) -> Unit,um.toIntOrNull() ?: 0)
    onVenueChange: (String) -> Unit,
    onCompetitionChange: (String) -> Unit,
    onGameDateTimeChange: (Long) -> Unit,xt("Nr.") },
    onHalfDurationChange: (String) -> Unit,s = KeyboardOptions(keyboardType = KeyboardType.Number),
    onHalftimeDurationChange: (String) -> Unit,   modifier = Modifier.width(80.dp),
    onExtraTimeHalfDurationChange: (String) -> Unit,    singleLine = true
    onMaxSubstitutionsChange: (String) -> Unit,
    onHomeColorSelected: (Color) -> Unit,   IconButton(onClick = {
    onAwayColorSelected: (Color) -> Unit,           val updatedRoster = roster.toMutableList()
    onNotesChanged: (String) -> Unit,               updatedRoster.removeAt(index)
    onHomeScoreChange: (String) -> Unit, // New callback                   onRosterChange(updatedRoster)
    onAwayScoreChange: (String) -> Unit, // New callback                }) {
    onSaveGame: () -> Unit, Icon(Icons.Default.Delete, contentDescription = "Entfernen")
    onAddHomePlayer: (String, String) -> Unit,                }
    onRemoveHomePlayer: (Player) -> Unit,   }
    onAddAwayPlayer: (String, String) -> Unit,
    onRemoveAwayPlayer: (Player) -> Unit,
    onRefereeAssignmentChange: (String) -> Unit,ifier.fillMaxWidth(),
    onVenueChange: (String) -> Unit,ent.spacedBy(8.dp),
    onCompetitionChange: (String) -> Unit,           verticalAlignment = Alignment.CenterVertically
    onGameDateTimeChange: (Long) -> Unit, {
    onHalfDurationChange: (String) -> Unit,
    onHalftimeDurationChange: (String) -> Unit,
    onExtraTimeHalfDurationChange: (String) -> Unit,nge = { newName = it },
    onMaxSubstitutionsChange: (String) -> Unit,r Name") },
    onHomeColorSelected: (Color) -> Unit,               modifier = Modifier.weight(1f),
    onAwayColorSelected: (Color) -> Unit,       singleLine = true
    onNotesChanged: (String) -> Unit,
    onHomeScoreChange: (String) -> Unit, // New callbackeld(
    onAwayScoreChange: (String) -> Unit, // New callback
    onSaveGame: () -> Unit,               onValueChange = { newNumber = it },
    onAddHomePlayer: (String, String) -> Unit,     label = { Text("Nr.") },
    onRemoveHomePlayer: (Player) -> Unit,ardOptions(keyboardType = KeyboardType.Number),
    onAddAwayPlayer: (String, String) -> Unit,= Modifier.width(80.dp),
    onRemoveAwayPlayer: (Player) -> Unit, = true
    onRefereeAssignmentChange: (String) -> Unit,
    onVenueChange: (String) -> Unit,
    onCompetitionChange: (String) -> Unit,Blank() && newNumber.isNotBlank()) {
    onGameDateTimeChange: (Long) -> Unit,r = Player(name = newName, number = newNumber.toIntOrNull() ?: 0)
    onHalfDurationChange: (String) -> Unit,ge(roster + newPlayer)
    onHalftimeDurationChange: (String) -> Unit,
    onExtraTimeHalfDurationChange: (String) -> Unit,      newNumber = ""
    onMaxSubstitutionsChange: (String) -> Unit,
    onHomeColorSelected: (Color) -> Unit,
    onAwayColorSelected: (Color) -> Unit,
    onNotesChanged: (String) -> Unit,
    onHomeScoreChange: (String) -> Unit, // New callback
    onAwayScoreChange: (String) -> Unit, // New callback
    onSaveGame: () -> Unit,
    onAddHomePlayer: (String, String) -> Unit,
    onRemoveHomePlayer: (Player) -> Unit,
    onAddAwayPlayer: (String, String) -> Unit,
    onRemoveAwayPlayer: (Player) -> Unit,
    onRefereeAssignmentChange: (String) -> Unit,
    onVenueChange: (String) -> Unit,
    onCompetitionChange: (String) -> Unit,
    onGameDateTimeChange: (Long) -> Unit,ES
    onHalfDurationChange: (String) -> Unit,
    onHalftimeDurationChange: (String) -> Unit,
    onExtraTimeHalfDurationChange: (String) -> Unit,,
    onMaxSubstitutionsChange: (String) -> Unit,
    onHomeColorSelected: (Color) -> Unit,
    onAwayColorSelected: (Color) -> Unit,
    onNotesChanged: (String) -> Unit,
    onHomeScoreChange: (String) -> Unit, // New callback
    onAwayScoreChange: (String) -> Unit, // New callbackame = "Add New Game (Dark)",
    onSaveGame: () -> Unit,   showBackground = true,
    onAddHomePlayer: (String, String) -> Unit,    uiMode = Configuration.UI_MODE_NIGHT_YES
    onRemoveHomePlayer: (Player) -> Unit,)
    onAddAwayPlayer: (String, String) -> Unit,
    onRemoveAwayPlayer: (Player) -> Unit,GameScreen_AddNewPreview() {
    onRefereeAssignmentChange: (String) -> Unit,
    onVenueChange: (String) -> Unit,(
    onCompetitionChange: (String) -> Unit,ditGameUiState(
    onGameDateTimeChange: (Long) -> Unit,
    onHalfDurationChange: (String) -> Unit,
    onHalftimeDurationChange: (String) -> Unit,
    onExtraTimeHalfDurationChange: (String) -> Unit,
    onMaxSubstitutionsChange: (String) -> Unit,= ""
    onHomeColorSelected: (Color) -> Unit,
    onAwayColorSelected: (Color) -> Unit,
    onNotesChanged: (String) -> Unit,
    onHomeScoreChange: (String) -> Unit, // New callback
    onAwayScoreChange: (String) -> Unit, // New callback
    onSaveGame: () -> Unit,
    onAddHomePlayer: (String, String) -> Unit,
    onRemoveHomePlayer: (Player) -> Unit,
    onAddAwayPlayer: (String, String) -> Unit,
    onRemoveAwayPlayer: (Player) -> Unit,
    onRefereeAssignmentChange: (String) -> Unit,
    onVenueChange: (String) -> Unit,= {},
    onCompetitionChange: (String) -> Unit,{},
    onGameDateTimeChange: (Long) -> Unit,HalftimeDurationChange = {},
    onHalfDurationChange: (String) -> Unit,ionChange = {},
    onHalftimeDurationChange: (String) -> Unit, {},
    onExtraTimeHalfDurationChange: (String) -> Unit,
    onMaxSubstitutionsChange: (String) -> Unit,
    onHomeColorSelected: (Color) -> Unit,
    onAwayColorSelected: (Color) -> Unit,dummy handler
    onNotesChanged: (String) -> Unit,dummy handler
    onHomeScoreChange: (String) -> Unit, // New callback = {}, // Add dummy handler
    onAwayScoreChange: (String) -> Unit, // New callback
    onSaveGame: () -> Unit,
    onAddHomePlayer: (String, String) -> Unit,
    onRemoveHomePlayer: (Player) -> Unit,
    onAddAwayPlayer: (String, String) -> Unit,
    onRemoveAwayPlayer: (Player) -> Unit,
    onRefereeAssignmentChange: (String) -> Unit,und = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
    onVenueChange: (String) -> Unit,
    onCompetitionChange: (String) -> Unit,
    onGameDateTimeChange: (Long) -> Unit,
    onHalfDurationChange: (String) -> Unit,
    onHalftimeDurationChange: (String) -> Unit,
    onExtraTimeHalfDurationChange: (String) -> Unit,
    onMaxSubstitutionsChange: (String) -> Unit,e = "Preview Home Team",
    onHomeColorSelected: (Color) -> Unit,       awayTeamName = "Preview Away Team",
    onAwayColorSelected: (Color) -> Unit,           fieldNumber = "7",
    onNotesChanged: (String) -> Unit,               refereeAssignment = "AR 1",
    onHomeScoreChange: (String) -> Unit, // New callback                venue = "Preview Venue",
    onAwayScoreChange: (String) -> Unit, // New callback
    onSaveGame: () -> Unit,     gameDateTimeEpochMillis = System.currentTimeMillis() + (3 * 24 * 60 * 60 * 1000),
    onAddHomePlayer: (String, String) -> Unit,urationMinutes = 40,
    onRemoveHomePlayer: (Player) -> Unit,inutes = 10,
    onAddAwayPlayer: (String, String) -> Unit,Blue.toArgb(),
    onRemoveAwayPlayer: (Player) -> Unit,Red.toArgb(),
    onRefereeAssignmentChange: (String) -> Unit,note for the preview of an edited game.",
    onVenueChange: (String) -> Unit,             homeScore = "1", // Example score
    onCompetitionChange: (String) -> Unit,awayScore = "2", // Example score
    onGameDateTimeChange: (Long) -> Unit,
    onHalfDurationChange: (String) -> Unit,
    onHalftimeDurationChange: (String) -> Unit,
    onExtraTimeHalfDurationChange: (String) -> Unit,
    onMaxSubstitutionsChange: (String) -> Unit,,
    onHomeColorSelected: (Color) -> Unit,       onAwayTeamNameChange = {},
    onAwayColorSelected: (Color) -> Unit,           onHomeTeamAbbrChange = {},
    onNotesChanged: (String) -> Unit,            onAwayTeamAbbrChange = {},

















































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































    onAway    onHomeScoreChange: (String) -> Unit, // New callback    onNotesChanged: (String) -> Unit,    onAwayColorSelected: (Color) -> Unit,    onHomeColorSelected: (Color) -> Unit,    onMaxSubstitutionsChange: (String) -> Unit,    onExtraTimeHalfDurationChange: (String) -> Unit,    onHalftimeDurationChange: (String) -> Unit,    onHalfDurationChange: (String) -> Unit,    onGameDateTimeChange: (Long) -> Unit,    onCompetitionChange: (String) -> Unit,    onVenueChange: (String) -> Unit,    onRefereeAssignmentChange: (String) -> Unit,    onRemoveAwayPlayer: (Player) -> Unit,    onAddAwayPlayer: (String, String) -> Unit,    onRemoveHomePlayer: (Player) -> Unit,    onAddHomePlayer: (String, String) -> Unit,    onSaveGame: () -> Unit,    onAwayScoreChange: (String) -> Unit, // New callback    onHomeScoreChange: (String) -> Unit, // New callback    onNotesChanged: (String) -> Unit,    onAwayColorSelected: (Color) -> Unit,    onHomeColorSelected: (Color) -> Unit,    onMaxSubstitutionsChange: (String) -> Unit,    onExtraTimeHalfDurationChange: (String) -> Unit,    onHalftimeDurationChange: (String) -> Unit,    onHalfDurationChange: (String) -> Unit,    onGameDateTimeChange: (Long) -> Unit,    onCompetitionChange: (String) -> Unit,    onVenueChange: (String) -> Unit,    onRefereeAssignmentChange: (String) -> Unit,    onRemoveAwayPlayer: (Player) -> Unit,    onAddAwayPlayer: (String, String) -> Unit,    onRemoveHomePlayer: (Player) -> Unit,    onAddHomePlayer: (String, String) -> Unit,    onSaveGame: () -> Unit,    onAwayScoreChange: (String) -> Unit, // New callback    onHomeScoreChange: (String) -> Unit, // New callback    onNotesChanged: (String) -> Unit,    onAwayColorSelected: (Color) -> Unit,    onHomeColorSelected: (Color) -> Unit,    onMaxSubstitutionsChange: (String) -> Unit,    onExtraTimeHalfDurationChange: (String) -> Unit,    onHalftimeDurationChange: (String) -> Unit,    onHalfDurationChange: (String) -> Unit,    onGameDateTimeChange: (Long) -> Unit,    onCompetitionChange: (String) -> Unit,    onVenueChange: (String) -> Unit,    onRefereeAssignmentChange: (String) -> Unit,    onRemoveAwayPlayer: (Player) -> Unit,    onAddAwayPlayer: (String, String) -> Unit,    onRemoveHomePlayer: (Player) -> Unit,    onAddHomePlayer: (String, String) -> Unit,    onSaveGame: () -> Unit,    onAwayScoreChange: (String) -> Unit, // New callback    onHomeScoreChange: (String) -> Unit, // New callback    onNotesChanged: (String) -> Unit,    onAwayColorSelected: (Color) -> Unit,    onHomeColorSelected: (Color) -> Unit,    onMaxSubstitutionsChange: (String) -> Unit,    onExtraTimeHalfDurationChange: (String) -> Unit,    onHalftimeDurationChange: (String) -> Unit,    onHalfDurationChange: (String) -> Unit,    onGameDateTimeChange: (Long) -> Unit,    onCompetitionChange: (String) -> Unit,    onVenueChange: (String) -> Unit,    onRefereeAssignmentChange: (String) -> Unit,    onRemoveAwayPlayer: (Player) -> Unit,    onAddAwayPlayer: (String, String) -> Unit,    onRemoveHomePlayer: (Player) -> Unit,    onAddHomePlayer: (String, String) -> Unit,    onSaveGame: () -> Unit,    onAwayScoreChange: (String) -> Unit, // New callback    onHomeScoreChange: (String) -> Unit, // New callback    onNotesChanged: (String) -> Unit,    onAwayColorSelected: (Color) -> Unit,    onHomeColorSelected: (Color) -> Unit,    onMaxSubstitutionsChange: (String) -> Unit,    onExtraTimeHalfDurationChange: (String) -> Unit,    onHalftimeDurationChange: (String) -> Unit,    onHalfDurationChange: (String) -> Unit,    onGameDateTimeChange: (Long) -> Unit,    onCompetitionChange: (String) -> Unit,    onVenueChange: (String) -> Unit,    onRefereeAssignmentChange: (String) -> Unit,    onRemoveAwayPlayer: (Player) -> Unit,    onAddAwayPlayer: (String, String) -> Unit,    onRemoveHomePlayer: (Player) -> Unit,    onAddHomePlayer: (String, String) -> Unit,    onSaveGame: () -> Unit,    onAwayScoreChange: (String) -> Unit, // New callback    onHomeScoreChange: (String) -> Unit, // New callback    onNotesChanged: (String) -> Unit,    onAwayColorSelected: (Color) -> Unit,    onHomeColorSelected: (Color) -> Unit,    onMaxSubstitutionsChange: (String) -> Unit,    onExtraTimeHalfDurationChange: (String) -> Unit,    onHalftimeDurationChange: (String) -> Unit,    onHalfDurationChange: (String) -> Unit,    onGameDateTimeChange: (Long) -> Unit,    onCompetitionChange: (String) -> Unit,    onVenueChange: (String) -> Unit,    onRefereeAssignmentChange: (String) -> Unit,    onRemoveAwayPlayer: (Player) -> Unit,    onAddAwayPlayer: (String, String) -> Unit,    onRemoveHomePlayer: (Player) -> Unit,    onAddHomePlayer: (String, String) -> Unit,    onSaveGame: () -> Unit,    onAwayScoreChange: (String) -> Unit, // New callback    onHomeScoreChange: (String) -> Unit, // New callback    onNotesChanged: (String) -> Unit,    onAwayColorSelected: (Color) -> Unit,    onHomeColorSelected: (Color) -> Unit,    onMaxSubstitutionsChange: (String) -> Unit,    onExtraTimeHalfDurationChange: (String) -> Unit,    onHalftimeDurationChange: (String) -> Unit,    onHalfDurationChange: (String) -> Unit,    onGameDateTimeChange: (Long) -> Unit,    onCompetitionChange: (String) -> Unit,    onVenueChange: (String) -> Unit,    onRefereeAssignmentChange: (String) -> Unit,    onRemoveAwayPlayer: (Player) -> Unit,    onAddAwayPlayer: (String, String) -> Unit,    onRemoveHomePlayer: (Player) -> Unit,    onAddHomePlayer: (String, String) -> Unit,    onSaveGame: () -> Unit,    onAwayScoreChange: (String) -> Unit, // New callback    onHomeScoreChange: (String) -> Unit, // New callback    onNotesChanged: (String) -> Unit,    onAwayColorSelected: (Color) -> Unit,    onHomeColorSelected: (Color) -> Unit,    onMaxSubstitutionsChange: (String) -> Unit,    onExtraTimeHalfDurationChange: (String) -> Unit,    onHalftimeDurationChange: (String) -> Unit,    onHalfDurationChange: (String) -> Unit,    onGameDateTimeChange: (Long) -> Unit,    onCompetitionChange: (String) -> Unit,    onVenueChange: (String) -> Unit,    onRefereeAssignmentChange: (String) -> Unit,    onRemoveAwayPlayer: (Player) -> Unit,    onAddAwayPlayer: (String, String) -> Unit,    onRemoveHomePlayer: (Player) -> Unit,    onAddHomePlayer: (String, String) -> Unit,    onSaveGame: () -> Unit,    onAwayScoreChange: (String) -> Unit, // New callback    onHomeScoreChange: (String) -> Unit, // New callback    onNotesChanged: (String) -> Unit,    onAwayColorSelected: (Color) -> Unit,    onHomeColorSelected: (Color) -> Unit,    onMaxSubstitutionsChange: (String) -> Unit,    onExtraTimeHalfDurationChange: (String) -> Unit,    onHalftimeDurationChange: (String) -> Unit,    onHalfDurationChange: (String) -> Unit,    onGameDateTimeChange: (Long) -> Unit,    onCompetitionChange: (String) -> Unit,    onVenueChange: (String) -> Unit,    onRefereeAssignmentChange: (String) -> Unit,    onRemoveAwayPlayer: (Player) -> Unit,    onAddAwayPlayer: (String, String) -> Unit,    onRemoveHomePlayer: (Player) -> Unit,    onAddHomePlayer: (String, String) -> Unit,    onSaveGame: () -> Unit,    onAwayScoreChange: (String) -> Unit, // New callback    onHomeScoreChange: (String) -> Unit, // New callback    onNotesChanged: (String) -> Unit,    onAwayColorSelected: (Color) -> Unit,    onHomeColorSelected: (Color) -> Unit,    onMaxSubstitutionsChange: (String) -> Unit,    onExtraTimeHalfDurationChange: (String) -> Unit,    onHalftimeDurationChange: (String) -> Unit,    onHalfDurationChange: (String) -> Unit,    onGameDateTimeChange: (Long) -> Unit,    onCompetitionChange: (String) -> Unit,    onVenueChange: (String) -> Unit,    onRefereeAssignmentChange: (String) -> Unit,    onRemoveAwayPlayer: (Player) -> Unit,    onAddAwayPlayer: (String, String) -> Unit,    onRemoveHomePlayer: (Player) -> Unit,    onAddHomePlayer: (String, String) -> Unit,    onSaveGame: () -> Unit,    onAwayScoreChange: (String) -> Unit, // New callback    onHomeScoreChange: (String) -> Unit, // New callback    onNotesChanged: (String) -> Unit,    onAwayColorSelected: (Color) -> Unit,    onHomeColorSelected: (Color) -> Unit,    onMaxSubstitutionsChange: (String) -> Unit,    onExtraTimeHalfDurationChange: (String) -> Unit,    onHalftimeDurationChange: (String) -> Unit,    onHalfDurationChange: (String) -> Unit,    onGameDateTimeChange: (Long) -> Unit,    onCompetitionChange: (String) -> Unit,    onVenueChange: (String) -> Unit,    onRefereeAssignmentChange: (String) -> Unit,    onRemoveAwayPlayer: (Player) -> Unit,    onAddAwayPlayer: (String, String) -> Unit,    onRemoveHomePlayer: (Player) -> Unit,    onAddHomePlayer: (String, String) -> Unit,    onSaveGame: () -> Unit,    onAwayScoreChange: (String) -> Unit, // New callback    onHomeScoreChange: (String) -> Unit, // New callback    onNotesChanged: (String) -> Unit,    onAwayColorSelected: (Color) -> Unit,    onHomeColorSelected: (Color) -> Unit,    onMaxSubstitutionsChange: (String) -> Unit,    onExtraTimeHalfDurationChange: (String) -> Unit,    onHalftimeDurationChange: (String) -> Unit,    onHalfDurationChange: (String) -> Unit,    onGameDateTimeChange: (Long) -> Unit,    onCompetitionChange: (String) -> Unit,    onVenueChange: (String) -> Unit,    onRefereeAssignmentChange: (String) -> Unit,    onRemoveAwayPlayer: (Player) -> Unit,    onAddAwayPlayer: (String, String) -> Unit,    onRemoveHomePlayer: (Player) -> Unit,    onAddHomePlayer: (String, String) -> Unit,    onSaveGame: () -> Unit,    onAwayScoreChange: (String) -> Unit, // New callback    onHomeScoreChange: (String) -> Unit, // New callback    onNotesChanged: (String) -> Unit,    onAwayColorSelected: (Color) -> Unit,    onHomeColorSelected: (Color) -> Unit,    onMaxSubstitutionsChange: (String) -> Unit,    onExtraTimeHalfDurationChange: (String) -> Unit,    onHalftimeDurationChange: (String) -> Unit,    onHalfDurationChange: (String) -> Unit,    onGameDateTimeChange: (Long) -> Unit,    onCompetitionChange: (String) -> Unit,    onVenueChange: (String) -> Unit,    onRefereeAssignmentChange: (String) -> Unit,    onRemoveAwayPlayer: (Player) -> Unit,    onAddAwayPlayer: (String, String) -> Unit,    onRemoveHomePlayer: (Player) -> Unit,    onAddHomePlayer: (String, String) -> Unit,    onSaveGame: () -> Unit,    onAwayScoreChange: (String) -> Unit, // New callback    onHomeScoreChange: (String) -> Unit, // New callback    onNotesChanged: (String) -> Unit,    onAwayColorSelected: (Color) -> Unit,    onHomeColorSelected: (Color) -> Unit,    onMaxSubstitutionsChange: (String) -> Unit,    onExtraTimeHalfDurationChange: (String) -> Unit,    onHalftimeDurationChange: (String) -> Unit,    onHalfDurationChange: (String) -> Unit,    onGameDateTimeChange: (Long) -> Unit,    onCompetitionChange: (String) -> Unit,    onVenueChange: (String) -> Unit,    onRefereeAssignmentChange: (String) -> Unit,    onRemoveAwayPlayer: (Player) -> Unit,    onAddAwayPlayer: (String, String) -> Unit,    onRemoveHomePlayer: (Player) -> Unit,    onAddHomePlayer: (String, String) -> Unit,    onSaveGame: () -> Unit,    onAwayScoreChange: (String) -> Unit, // New callback    onHomeScoreChange: (String) -> Unit, // New callback    onNotesChanged: (String) -> Unit,    onAwayColorSelected: (Color) -> Unit,    onHomeColorSelected: (Color) -> Unit,    onMaxSubstitutionsChange: (String) -> Unit,    onExtraTimeHalfDurationChange: (String) -> Unit,    onHalftimeDurationChange: (String) -> Unit,    onHalfDurationChange: (String) -> Unit,    onGameDateTimeChange: (Long) -> Unit,    onCompetitionChange: (String) -> Unit,    onVenueChange: (String) -> Unit,    onRefereeAssignmentChange: (String) -> Unit,    onRemoveAwayPlayer: (Player) -> Unit,    onAddAwayPlayer: (String, String) -> Unit,    onRemoveHomePlayer: (Player) -> Unit,    onAddHomePlayer: (String, String) -> Unit,    onSaveGame: () -> Unit,    onAwayScoreChange: (String) -> Unit, // New callback    onHomeScoreChange: (String) -> Unit, // New callback    onNotesChanged: (String) -> Unit,








    onAwayColorSelected: (Color) -> Unit,    onHomeColorSelected: (Color) -> Unit,    onMaxSubstitutionsChange: (String) -> Unit,    onExtraTimeHalfDurationChange: (String) -> Unit,    onHalftimeDurationChange: (String) -> Unit,    onHalfDurationChange: (String) -> Unit,    onGameDateTimeChange: (Long) -> Unit,    onCompetitionChange: (String) -> Unit,    onVenueChange: (String) -> Unit,    onRefereeAssignmentChange: (String) -> Unit,    onRemoveAwayPlayer: (Player) -> Unit,    onAddAwayPlayer: (String, String) -> Unit,    onRemoveHomePlayer: (Player) -> Unit,    onAddHomePlayer: (String, String) -> Unit,    onSaveGame: () -> Unit,    onAwayScoreChange: (String) -> Unit, // New callback    onHomeScoreChange: (String) -> Unit, // New callback    onNotesChanged: (String) -> Unit,    onAwayColorSelected: (Color) -> Unit,    onHomeColorSelected: (Color) -> Unit,    onMaxSubstitutionsChange: (String) -> Unit,    onExtraTimeHalfDurationChange: (String) -> Unit,    onHalftimeDurationChange: (String) -> Unit,    onHalfDurationChange: (String) -> Unit,    onGameDateTimeChange: (Long) -> Unit,    onCompetitionChange: (String) -> Unit,    onVenueChange: (String) -> Unit,    onRefereeAssignmentChange: (String) -> Unit,    onRemoveAwayPlayer: (Player) -> Unit,    onAddAwayPlayer: (String, String) -> Unit,    onRemoveHomePlayer: (Player) -> Unit,    onAddHomePlayer: (String, String) -> Unit,    onSaveGame: () -> Unit,    onAwayScoreChange: (String) -> Unit, // New callback    onHomeScoreChange: (String) -> Unit, // New callback    onNotesChanged: (String) -> Unit,    onAwayColorSelected: (Color) -> Unit,    onHomeColorSelected: (Color) -> Unit,    onMaxSubstitutionsChange: (String) -> Unit,    onExtraTimeHalfDurationChange: (String) -> Unit,    onHalftimeDurationChange: (String) -> Unit,    onHalfDurationChange: (String) -> Unit,    onGameDateTimeChange: (Long) -> Unit,    onCompetitionChange: (String) -> Unit,    onVenueChange: (String) -> Unit,    onRefereeAssignmentChange: (String) -> Unit,    onRemoveAwayPlayer: (Player) -> Unit,    onAddAwayPlayer: (String, String) -> Unit,    onRemoveHomePlayer: (Player) -> Unit,    onAddHomePlayer: (String, String) -> Unit,    onSaveGame: () -> Unit,    onAwayScoreChange: (String) -> Unit, // New callback    onHomeScoreChange: (String) -> Unit, // New callback    onNotesChanged: (String) -> Unit,    onAwayColorSelected: (Color) -> Unit,    onHomeColorSelected: (Color) -> Unit,    onMaxSubstitutionsChange: (String) -> Unit,    onExtraTimeHalfDurationChange: (String) -> Unit,    onHalftimeDurationChange: (String) -> Unit,    onHalfDurationChange: (String) -> Unit,    onGameDateTimeChange: (Long) -> Unit,    onCompetitionChange: (String) -> Unit,    onVenueChange: (String) -> Unit,    onRefereeAssignmentChange: (String) -> Unit,    onRemoveAwayPlayer: (Player) -> Unit,    onAddAwayPlayer: (String, String) -> Unit,    onRemoveHomePlayer: (Player) -> Unit,    onAddHomePlayer: (String, String) -> Unit,    onSaveGame: () -> Unit,    onAwayScoreChange: (String) -> Unit, // New callback    onHomeScoreChange: (String) -> Unit, // New callback    onNotesChanged: (String) -> Unit,    onAwayColorSelected: (Color) -> Unit,    onHomeColorSelected: (Color) -> Unit,    onMaxSubstitutionsChange: (String) -> Unit,    onExtraTimeHalfDurationChange: (String) -> Unit,    onHalftimeDurationChange: (String) -> Unit,    onHalfDurationChange: (String) -> Unit,    onGameDateTimeChange: (Long) -> Unit,    onCompetitionChange: (String) -> Unit,    onVenueChange: (String) -> Unit,    onRefereeAssignmentChange: (String) -> Unit,    onRemoveAwayPlayer: (Player) -> Unit,    onAddAwayPlayer: (String, String) -> Unit,    onRemoveHomePlayer: (Player) -> Unit,    onAddHomePlayer: (String, String) -> Unit,    onSaveGame: () -> Unit,    onAwayScoreChange: (String) -> Unit, // New callback    onHomeScoreChange: (String) -> Unit, // New callback    onNotesChanged: (String) -> Unit,    onAwayColorSelected: (Color) -> Unit,    onHomeColorSelected: (Color) -> Unit,    onMaxSubstitutionsChange: (String) -> Unit,    onExtraTimeHalfDurationChange: (String) -> Unit,    onHalftimeDurationChange: (String) -> Unit,    onHalfDurationChange: (String) -> Unit,    onGameDateTimeChange: (Long) -> Unit,    onCompetitionChange: (String) -> Unit,    onVenueChange: (String) -> Unit,    onRefereeAssignmentChange: (String) -> Unit,    onRemoveAwayPlayer: (Player) -> Unit,    onAddAwayPlayer: (String, String) -> Unit,    onRemoveHomePlayer: (Player) -> Unit,    onAddHomePlayer: (String, String) -> Unit,    onSaveGame: () -> Unit,    onAwayScoreChange: (String) -> Unit, // New callback    onHomeScoreChange: (String) -> Unit, // New callback    onNotesChanged: (String) -> Unit,    onAwayColorSelected: (Color) -> Unit,    onHomeColorSelected: (Color) -> Unit,    onMaxSubstitutionsChange: (String) -> Unit,    onExtraTimeHalfDurationChange: (String) -> Unit,    onHalftimeDurationChange: (String) -> Unit,    onHalfDurationChange: (String) -> Unit,    onGameDateTimeChange: (Long) -> Unit,    onCompetitionChange: (String) -> Unit,    onVenueChange: (String) -> Unit,    onRefereeAssignmentChange: (String) -> Unit,    onRemoveAwayPlayer: (Player) -> Unit,    onAddAwayPlayer: (String, String) -> Unit,    onRemoveHomePlayer: (Player) -> Unit,    onAddHomePlayer: (String, String) -> Unit,    onSaveGame: () -> Unit,    onAwayScoreChange: (String) -> Unit, // New callback    onHomeScoreChange: (String) -> Unit, // New callback    onNotesChanged: (String) -> Unit,    onAwayColorSelected: (Color) -> Unit,    onHomeColorSelected: (Color) -> Unit,    onMaxSubstitutionsChange: (String) -> Unit,    onExtraTimeHalfDurationChange: (String) -> Unit,    onHalftimeDurationChange: (String) -> Unit,    onHalfDurationChange: (String) -> Unit,    onGameDateTimeChange: (Long) -> Unit,    onCompetitionChange: (String) -> Unit,    onVenueChange: (String) -> Unit,    onRefereeAssignmentChange: (String) -> Unit,    onRemoveAwayPlayer: (Player) -> Unit,    onAddAwayPlayer: (String, String) -> Unit,    onRemoveHomePlayer: (Player) -> Unit,    onAddHomePlayer: (String, String) -> Unit,    onSaveGame: () -> Unit,    onAwayScoreChange: (String) -> Unit, // New callback    onHomeScoreChange: (String) -> Unit, // New callback    onNotesChanged: (String) -> Unit,    onAwayColorSelected: (Color) -> Unit,    onHomeColorSelected: (Color) -> Unit,    onMaxSubstitutionsChange: (String) -> Unit,    onExtraTimeHalfDurationChange: (String) -> Unit,    onHalftimeDurationChange: (String) -> Unit,    onHalfDurationChange: (String) -> Unit,    onGameDateTimeChange: (Long) -> Unit,    onCompetitionChange: (String) -> Unit,    onVenueChange: (String) -> Unit,    onRefereeAssignmentChange: (String) -> Unit,    onRemoveAwayPlayer: (Player) -> Unit,    onAddAwayPlayer: (String, String) -> Unit,    onRemoveHomePlayer: (Player) -> Unit,    onAddHomePlayer: (String, String) -> Unit,    onSaveGame: () -> Unit,    onAwayScoreChange: (String) -> Unit, // New callback    onHomeScoreChange: (String) -> Unit, // New callback    onNotesChanged: (String) -> Unit,    onAwayColorSelected: (Color) -> Unit,    onHomeColorSelected: (Color) -> Unit,    onMaxSubstitutionsChange: (String) -> Unit,    onExtraTimeHalfDurationChange: (String) -> Unit,    onHalftimeDurationChange: (String) -> Unit,    onHalfDurationChange: (String) -> Unit,    onGameDateTimeChange: (Long) -> Unit,    onCompetitionChange: (String) -> Unit,    onVenueChange: (String) -> Unit,    onRefereeAssignmentChange: (String) -> Unit,    onRemoveAwayPlayer: (Player) -> Unit,    onAddAwayPlayer: (String, String) -> Unit,    onRemoveHomePlayer: (Player) -> Unit,    onAddHomePlayer: (String, String) -> Unit,    onSaveGame: () -> Unit,    onAwayScoreChange: (String) -> Unit, // New callback    onHomeScoreChange: (String) -> Unit, // New callback    onNotesChanged: (String) -> Unit,    onAwayColorSelected: (Color) -> Unit,    onHomeColorSelected: (Color) -> Unit,    onMaxSubstitutionsChange: (String) -> Unit,    onExtraTimeHalfDurationChange: (String) -> Unit,    onHalftimeDurationChange: (String) -> Unit,    onHalfDurationChange: (String) -> Unit,    onGameDateTimeChange: (Long) -> Unit,    onCompetitionChange: (String) -> Unit,    onVenueChange: (String) -> Unit,    onRefereeAssignmentChange: (String) -> Unit,    onRemoveAwayPlayer: (Player) -> Unit,    onAddAwayPlayer: (String, String) -> Unit,    onRemoveHomePlayer: (Player) -> Unit,    onAddHomePlayer: (String, String) -> Unit,    onSaveGame: () -> Unit,    onAwayScoreChange: (String) -> Unit, // New callback    onHomeScoreChange: (String) -> Unit, // New callback    onNotesChanged: (String) -> Unit,    onAwayColorSelected: (Color) -> Unit,    onHomeColorSelected: (Color) -> Unit,    onMaxSubstitutionsChange: (String) -> Unit,    onExtraTimeHalfDurationChange: (String) -> Unit,    onHalftimeDurationChange: (String) -> Unit,    onHalfDurationChange: (String) -> Unit,    onGameDateTimeChange: (Long) -> Unit,    onCompetitionChange: (String) -> Unit,    onVenueChange: (String) -> Unit,    onRefereeAssignmentChange: (String) -> Unit,    onRemoveAwayPlayer: (Player) -> Unit,    onAddAwayPlayer: (String, String) -> Unit,    onRemoveHomePlayer: (Player) -> Unit,    onAddHomePlayer: (String, String) -> Unit,    onSaveGame: () -> Unit,    onAwayScoreChange: (String) -> Unit, // New callback    onHomeScoreChange: (String) -> Unit, // New callback    onNotesChanged: (String) -> Unit,    onAwayColorSelected: (Color) -> Unit,    onHomeColorSelected: (Color) -> Unit,    onMaxSubstitutionsChange: (String) -> Unit,    onExtraTimeHalfDurationChange: (String) -> Unit,    onHalftimeDurationChange: (String) -> Unit,    onHalfDurationChange: (String) -> Unit,    onGameDateTimeChange: (Long) -> Unit,    onCompetitionChange: (String) -> Unit,    onVenueChange: (String) -> Unit,    onRefereeAssignmentChange: (String) -> Unit,    onRemoveAwayPlayer: (Player) -> Unit,    onAddAwayPlayer: (String, String) -> Unit,    onRemoveHomePlayer: (Player) -> Unit,    onAddHomePlayer: (String, String) -> Unit,    onSaveGame: () -> Unit,    onAwayScoreChange: (String) -> Unit, // New callback    onHomeScoreChange: (String) -> Unit, // New callback    onNotesChanged: (String) -> Unit,    onAwayColorSelected: (Color) -> Unit,    onHomeColorSelected: (Color) -> Unit,    onMaxSubstitutionsChange: (String) -> Unit,    onExtraTimeHalfDurationChange: (String) -> Unit,    onHalftimeDurationChange: (String) -> Unit,    onHalfDurationChange: (String) -> Unit,    onGameDateTimeChange: (Long) -> Unit,    onCompetitionChange: (String) -> Unit,    onVenueChange: (String) -> Unit,    onRefereeAssignmentChange: (String) -> Unit,    onRemoveAwayPlayer: (Player) -> Unit,    onAddAwayPlayer: (String, String) -> Unit,    onRemoveHomePlayer: (Player) -> Unit,    onAddHomePlayer: (String, String) -> Unit,    onSaveGame: () -> Unit,    onAwayScoreChange: (String) -> Unit, // New callback    onHomeScoreChange: (String) -> Unit, // New callback    onNotesChanged: (String) -> Unit,    onAwayColorSelected: (Color) -> Unit,    onHomeColorSelected: (Color) -> Unit,    onMaxSubstitutionsChange: (String) -> Unit,    onExtraTimeHalfDurationChange: (String) -> Unit,    onHalftimeDurationChange: (String) -> Unit,    onHalfDurationChange: (String) -> Unit,    onGameDateTimeChange: (Long) -> Unit,    onCompetitionChange: (String) -> Unit,    onVenueChange: (String) -> Unit,    onRefereeAssignmentChange: (String) -> Unit,    onRemoveAwayPlayer: (Player) -> Unit,    onAddAwayPlayer: (String, String) -> Unit,    onRemoveHomePlayer: (Player) -> Unit,    onAddHomePlayer: (String, String) -> Unit,    onSaveGame: () -> Unit,    onAwayScoreChange: (String) -> Unit, // New callback    onHomeScoreChange: (String) -> Unit, // New callback    onNotesChanged: (String) -> Unit,    onAwayColorSelected: (Color) -> Unit,    onHomeColorSelected: (Color) -> Unit,    onMaxSubstitutionsChange: (String) -> Unit,    onExtraTimeHalfDurationChange: (String) -> Unit,    onHalftimeDurationChange: (String) -> Unit,    onHalfDurationChange: (String) -> Unit,    onGameDateTimeChange: (Long) -> Unit,    onCompetitionChange: (String) -> Unit,    onVenueChange: (String) -> Unit,    onRefereeAssignmentChange: (String) -> Unit,    onRemoveAwayPlayer: (Player) -> Unit,    onAddAwayPlayer: (String, String) -> Unit,    onRemoveHomePlayer: (Player) -> Unit,    onAddHomePlayer: (String, String) -> Unit,    onSaveGame: () -> Unit,    onAwayScoreChange: (String) -> Unit, // New callback    onHomeScoreChange: (String) -> Unit, // New callback    onNotesChanged: (String) -> Unit,    onAwayColorSelected: (Color) -> Unit,    onHomeColorSelected: (Color) -> Unit,    onMaxSubstitutionsChange: (String) -> Unit,    onExtraTimeHalfDurationChange: (String) -> Unit,    onHalftimeDurationChange: (String) -> Unit,    onHalfDurationChange: (String) -> Unit,    onGameDateTimeChange: (Long) -> Unit,    onCompetitionChange: (String) -> Unit,    onVenueChange: (String) -> Unit,    onRefereeAssignmentChange: (String) -> Unit,    onRemoveAwayPlayer: (Player) -> Unit,    onAddAwayPlayer: (String, String) -> Unit,    onRemoveHomePlayer: (Player) -> Unit,    onAddHomePlayer: (String, String) -> Unit,    onSaveGame: () -> Unit,    onAwayScoreChange: (String) -> Unit, // New callback    onHomeScoreChange: (String) -> Unit, // New callback    onNotesChanged: (String) -> Unit,    onAwayColorSelected: (Color) -> Unit,    onHomeColorSelected: (Color) -> Unit,    onMaxSubstitutionsChange: (String) -> Unit,    onExtraTimeHalfDurationChange: (String) -> Unit,    onHalftimeDurationChange: (String) -> Unit,    onHalfDurationChange: (String) -> Unit,    onGameDateTimeChange: (Long) -> Unit,    onCompetitionChange: (String) -> Unit,    onVenueChange: (String) -> Unit,    onRefereeAssignmentChange: (String) -> Unit,    onRemoveAwayPlayer: (Player) -> Unit,    onAddAwayPlayer: (String, String) -> Unit,    onRemoveHomePlayer: (Player) -> Unit,    onAddHomePlayer: (String, String) -> Unit,    onSaveGame: () -> Unit,    onAwayScoreChange: (String) -> Unit, // New callback    onHomeScoreChange: (String) -> Unit, // New callback    onNotesChanged: (String) -> Unit,    onAwayColorSelected: (Color) -> Unit,    onHomeColorSelected: (Color) -> Unit,    onMaxSubstitutionsChange: (String) -> Unit,    onExtraTimeHalfDurationChange: (String) -> Unit,    onHalftimeDurationChange: (String) -> Unit,    onHalfDurationChange: (String) -> Unit,    onGameDateTimeChange: (Long) -> Unit,    onCompetitionChange: (String) -> Unit,    onVenueChange: (String) -> Unit,    onRefereeAssignmentChange: (String) -> Unit,    onRemoveAwayPlayer: (Player) -> Unit,    onAddAwayPlayer: (String, String) -> Unit,    onRemoveHomePlayer: (Player) -> Unit,    onAddHomePlayer: (String, String) -> Unit,    onSaveGame: () -> Unit,    onAwayScoreChange: (String) -> Unit, // New callback    onHomeScoreChange: (String) -> Unit, // New callback    onNotesChanged: (String) -> Unit,    onAwayColorSelected: (Color) -> Unit,    onHomeColorSelected: (Color) -> Unit,    onMaxSubstitutionsChange: (String) -> Unit,    onExtraTimeHalfDurationChange: (String) -> Unit,    onHalftimeDurationChange: (String) -> Unit,    onHalfDurationChange: (String) -> Unit,    onGameDateTimeChange: (Long) -> Unit,    onCompetitionChange: (String) -> Unit,    onVenueChange: (String) -> Unit,    onRefereeAssignmentChange: (String) -> Unit,    onRemoveAwayPlayer: (Player) -> Unit,    onAddAwayPlayer: (String, String) -> Unit,    onRemoveHomePlayer: (Player) -> Unit,    onAddHomePlayer: (String, String) -> Unit,    onSaveGame: () -> Unit,    onAwayScoreChange: (String) -> Unit, // New callback    onHomeScoreChange: (String) -> Unit, // New callback    onNotesChanged: (String) -> Unit,    onAwayColorSelected: (Color) -> Unit,    onHomeColorSelected: (Color) -> Unit,    onMaxSubstitutionsChange: (String) -> Unit,    onExtraTimeHalfDurationChange: (String) -> Unit,    onHalftimeDurationChange: (String) -> Unit,    onHalfDurationChange: (String) -> Unit,    onGameDateTimeChange: (Long) -> Unit,    onCompetitionChange: (String) -> Unit,    onVenueChange: (String) -> Unit,    onRefereeAssignmentChange: (String) -> Unit,    onRemoveAwayPlayer: (Player) -> Unit,    onAddAwayPlayer: (String, String) -> Unit,    onRemoveHomePlayer: (Player) -> Unit,    onAddHomePlayer: (String, String) -> Unit,    onSaveGame: () -> Unit,    onAwayScoreChange: (String) -> Unit, // New callback    onHomeScoreChange: (String) -> Unit, // New callback    onNotesChanged: (String) -> Unit,    onAwayColorSelected: (Color) -> Unit,    onHomeColorSelected: (Color) -> Unit,    onMaxSubstitutionsChange: (String) -> Unit,    onExtraTimeHalfDurationChange: (String) -> Unit,    onHalftimeDurationChange: (String) -> Unit,    onHalfDurationChange: (String) -> Unit,    onGameDateTimeChange: (Long) -> Unit,    onCompetitionChange: (String) -> Unit,    onVenueChange: (String) -> Unit,    onRefereeAssignmentChange: (String) -> Unit,    onRemoveAwayPlayer: (Player) -> Unit,    onAddAwayPlayer: (String, String) -> Unit,    onRemoveHomePlayer: (Player) -> Unit,    onAddHomePlayer: (String, String) -> Unit,    onSaveGame: () -> Unit,    onAwayScoreChange: (String) -> Unit, // New callback    onHomeScoreChange: (String) -> Unit, // New callback    onNotesChanged: (String) -> Unit,    onAwayColorSelected: (Color) -> Unit,    onHomeColorSelected: (Color) -> Unit,    onMaxSubstitutionsChange: (String) -> Unit,    onExtraTimeHalfDurationChange: (String) -> Unit,    onHalftimeDurationChange: (String) -> Unit,    onHalfDurationChange: (String) -> Unit,    onGameDateTimeChange: (Long) -> Unit,    onCompetitionChange: (String) -> Unit,    onVenueChange: (String) -> Unit,    onRefereeAssignmentChange: (String) -> Unit,    onRemoveAwayPlayer: (Player) -> Unit,    onAddAwayPlayer: (String, String) -> Unit,    onRemoveHomePlayer: (Player) -> Unit,    onAddHomePlayer: (String, String) -> Unit,    onSaveGame: () -> Unit,    onAwayScoreChange: (String) -> Unit, // New callback    onHomeScoreChange: (String) -> Unit, // New callback    onNotesChanged: (String) -> Unit,    onAwayColorSelected: (Color) -> Unit,    onHomeColorSelected: (Color) -> Unit,    onMaxSubstitutionsChange: (String) -> Unit,    onExtraTimeHalfDurationChange: (String) -> Unit,    onHalftimeDurationChange: (String) -> Unit,    onHalfDurationChange: (String) -> Unit,    onGameDateTimeChange: (Long) -> Unit,    onCompetitionChange: (String) -> Unit,    onVenueChange: (String) -> Unit,    onRefereeAssignmentChange: (String) -> Unit,    onRemoveAwayPlayer: (Player) -> Unit,    onAddAwayPlayer: (String, String) -> Unit,    onRemoveHomePlayer: (Player) -> Unit,    onAddHomePlayer: (String, String) -> Unit,    onSaveGame: () -> Unit,    onAwayScoreChange: (String) -> Unit, // New callback    onHomeScoreChange: (String) -> Unit, // New callback    onNotesChanged: (String) -> Unit,    onAwayColorSelected: (Color) -> Unit,    onHomeColorSelected: (Color) -> Unit,    onMaxSubstitutionsChange: (String) -> Unit,    onExtraTimeHalfDurationChange: (String) -> Unit,    onHalftimeDurationChange: (String) -> Unit,    onHalfDurationChange: (String) -> Unit,    onGameDateTimeChange: (Long) -> Unit,    onCompetitionChange: (String) -> Unit,    onVenueChange: (String) -> Unit,    onRefereeAssignmentChange: (String) -> Unit,    onRemoveAwayPlayer: (Player) -> Unit,    onAddAwayPlayer: (String, String) -> Unit,    onRemoveHomePlayer: (Player) -> Unit,    onAddHomePlayer: (String, String) -> Unit,    onSaveGame: () -> Unit,    onAwayScoreChange: (String) -> Unit, // New callback    onHomeScoreChange: (String) -> Unit, // New callback    onNotesChanged: (String) -> Unit,    onAwayColorSelected: (Color) -> Unit,    onHomeColorSelected: (Color) -> Unit,    onMaxSubstitutionsChange: (String) -> Unit,    onExtraTimeHalfDurationChange: (String) -> Unit,    onHalftimeDurationChange: (String) -> Unit,    onHalfDurationChange: (String) -> Unit,    onGameDateTimeChange: (Long) -> Unit,    onCompetitionChange: (String) -> Unit,    onVenueChange: (String) -> Unit,    onRefereeAssignmentChange: (String) -> Unit,    onRemoveAwayPlayer: (Player) -> Unit,    onAddAwayPlayer: (String, String) -> Unit,    onRemoveHomePlayer: (Player) -> Unit,    onAddHomePlayer: (String, String) -> Unit,    onSaveGame: () -> Unit,    onAwayScoreChange: (String) -> Unit, // New callback    onHomeScoreChange: (String) -> Unit, // New callback    onNotesChanged: (String) -> Unit,    onAwayColorSelected: (Color) -> Unit,    onHomeColorSelected: (Color) -> Unit,    onMaxSubstitutionsChange: (String) -> Unit,    onExtraTimeHalfDurationChange: (String) -> Unit,    onHalftimeDurationChange: (String) -> Unit,    onHalfDurationChange: (String) -> Unit,    onGameDateTimeChange: (Long) -> Unit,    onCompetitionChange: (String) -> Unit,    onVenueChange: (String) -> Unit,    onRefereeAssignmentChange: (String) -> Unit,    onRemoveAwayPlayer: (Player) -> Unit,    onAddAwayPlayer: (String, String) -> Unit,    onRemoveHomePlayer: (Player) -> Unit,    onAddHomePlayer: (String, String) -> Unit,    onSaveGame: () -> Unit,    onAwayScoreChange: (String) -> Unit, // New callback    onHomeScoreChange: (String) -> Unit, // New callback    onNotesChanged: (String) -> Unit,    onAwayColorSelected: (Color) -> Unit,    onHomeColorSelected: (Color) -> Unit,    onMaxSubstitutionsChange: (String) -> Unit,    onExtraTimeHalfDurationChange: (String) -> Unit,    onHalftimeDurationChange: (String) -> Unit,    onHalfDurationChange: (String) -> Unit,    onGameDateTimeChange: (Long) -> Unit,    onCompetitionChange: (String) -> Unit,    onVenueChange: (String) -> Unit,    onRefereeAssignmentChange: (String) -> Unit,    onRemoveAwayPlayer: (Player) -> Unit,    onAddAwayPlayer: (String, String) -> Unit,    onRemoveHomePlayer: (Player) -> Unit,    onAddHomePlayer: (String, String) -> Unit,    onSaveGame: () -> Unit,    onAwayScoreChange: (String) -> Unit, // New callback    onHomeScoreChange: (String) -> Unit, // New callback    onNotesChanged: (String) -> Unit,    onAwayColorSelected: (Color) -> Unit,    onHomeColorSelected: (Color) -> Unit,    onMaxSubstitutionsChange: (String) -> Unit,    onExtraTimeHalfDurationChange: (String) -> Unit,    onHalftimeDurationChange: (String) -> Unit,    onHalfDurationChange: (String) -> Unit,    onGameDateTimeChange: (Long) -> Unit,    onCompetitionChange: (String) -> Unit,    onVenueChange: (String) -> Unit,    onRefereeAssignmentChange: (String) -> Unit,    onRemoveAwayPlayer: (Player) -> Unit,    onAddAwayPlayer: (String, String) -> Unit,    onRemoveHomePlayer: (Player) -> Unit,    onAddHomePlayer: (String, String) -> Unit,    onSaveGame: () -> Unit,    onAwayScoreChange: (String) -> Unit, // New callback    onHomeScoreChange: (String) -> Unit, // New callback    onNotesChanged: (String) -> Unit,    onAwayColorSelected: (Color) -> Unit,    onHomeColorSelected: (Color) -> Unit,    onMaxSubstitutionsChange: (String) -> Unit,    onExtraTimeHalfDurationChange: (String) -> Unit,    onHalftimeDurationChange: (String) -> Unit,    onHalfDurationChange: (String) -> Unit,    onGameDateTimeChange: (Long) -> Unit,    onCompetitionChange: (String) -> Unit,    onVenueChange: (String) -> Unit,    onRefereeAssignmentChange: (String) -> Unit,    onRemoveAwayPlayer: (Player) -> Unit,    onAddAwayPlayer: (String, String) -> Unit,    onRemoveHomePlayer: (Player) -> Unit,    onAddHomePlayer: (String, String) -> Unit,    onSaveGame: () -> Unit,    onAwayScoreChange: (String) -> Unit, // New callback    onHomeScoreChange: (String) -> Unit, // New callback    onNotesChanged: (String) -> Unit,    onAwayColorSelected: (Color) -> Unit,    onHomeColorSelected: (Color) -> Unit,    onMaxSubstitutionsChange: (String) -> Unit,    onExtraTimeHalfDurationChange: (String) -> Unit,    onHalftimeDurationChange: (String) -> Unit,    onHalfDurationChange: (String) -> Unit,    onGameDateTimeChange: (Long) -> Unit,    onCompetitionChange: (String) -> Unit,    onVenueChange: (String) -> Unit,    onRefereeAssignmentChange: (String) -> Unit,    onRemoveAwayPlayer: (Player) -> Unit,    onAddAwayPlayer: (String, String) -> Unit,    onRemoveHomePlayer: (Player) -> Unit,    onAddHomePlayer: (String, String) -> Unit,    onSaveGame: () -> Unit,    onAwayScoreChange: (String) -> Unit, // New callback    onHomeScoreChange: (String) -> Unit, // New callback    onNotesChanged: (String) -> Unit,    onAwayColorSelected: (Color) -> Unit,    onHomeColorSelected: (Color) -> Unit,    onMaxSubstitutionsChange: (String) -> Unit,    onExtraTimeHalfDurationChange: (String) -> Unit,    onHalftimeDurationChange: (String) -> Unit,    onHalfDurationChange: (String) -> Unit,    onGameDateTimeChange: (Long) -> Unit,    onCompetitionChange: (String) -> Unit,    onVenueChange: (String) -> Unit,    onRefereeAssignmentChange: (String) -> Unit,    onRemoveAwayPlayer: (Player) -> Unit,    onAddAwayPlayer: (String, String) -> Unit,    onRemoveHomePlayer: (Player) -> Unit,    onAddHomePlayer: (String, String) -> Unit,    onSaveGame: () -> Unit,    onAwayScoreChange: (String) -> Unit, // New callback    onHomeScoreChange: (String) -> Unit, // New callback    onNotesChanged: (String) -> Unit,    onAwayColorSelected: (Color) -> Unit,    onHomeColorSelected: (Color) -> Unit,    onMaxSubstitutionsChange: (String) -> Unit,    onExtraTimeHalfDurationChange: (String) -> Unit,    onHalftimeDurationChange: (String) -> Unit,    onHalfDurationChange: (String) -> Unit,    onGameDateTimeChange: (Long) -> Unit,    onCompetitionChange: (String) -> Unit,    onVenueChange: (String) -> Unit,    onRefereeAssignmentChange: (String) -> Unit,    onRemoveAwayPlayer: (Player) -> Unit,    onAddAwayPlayer: (String, String) -> Unit,    onRemoveHomePlayer: (Player) -> Unit,    onAddHomePlayer: (String, String) -> Unit,    onSaveGame: () -> Unit,    onAwayScoreChange: (String) -> Unit, // New callback    onHomeScoreChange: (String) -> Unit, // New callback    onNotesChanged: (String) -> Unit,    onAwayColorSelected: (Color) -> Unit,    onHomeColorSelected: (Color) -> Unit,    onMaxSubstitutionsChange: (String) -> Unit,    onExtraTimeHalfDurationChange: (String) -> Unit,    onHalftimeDurationChange: (String) -> Unit,    onHalfDurationChange: (String) -> Unit,    onGameDateTimeChange: (Long) -> Unit,    onCompetitionChange: (String) -> Unit,    onVenueChange: (String) -> Unit,    onRefereeAssignmentChange: (String) -> Unit,    onRemoveAwayPlayer: (Player) -> Unit,    onAddAwayPlayer: (String, String) -> Unit,    onRemoveHomePlayer: (Player) -> Unit,    onAddHomePlayer: (String, String) -> Unit,    onSaveGame: () -> Unit,    onAwayScoreChange: (String) -> Unit, // New callback    onHomeScoreChange: (String) -> Unit, // New callback    onNotesChanged: (String) -> Unit,    onAwayColorSelected: (Color) -> Unit,    onHomeColorSelected: (Color) -> Unit,    onMaxSubstitutionsChange: (String) -> Unit,    onExtraTimeHalfDurationChange: (String) -> Unit,    onHalftimeDurationChange: (String) -> Unit,    onHalfDurationChange: (String) -> Unit,    onGameDateTimeChange: (Long) -> Unit,    onCompetitionChange: (String) -> Unit,    onVenueChange: (String) -> Unit,    onRefereeAssignmentChange: (String) -> Unit,    onRemoveAwayPlayer: (Player) -> Unit,    onAddAwayPlayer: (String, String) -> Unit,    onRemoveHomePlayer: (Player) -> Unit,    onAddHomePlayer: (String, String) -> Unit,    onSaveGame: () -> Unit,    onAwayScoreChange: (String) -> Unit, // New callback    onHomeScoreChange: (String) -> Unit, // New callback    onNotesChanged: (String) -> Unit,    onAwayColorSelected: (Color) -> Unit,    onHomeColorSelected: (Color) -> Unit,    onMaxSubstitutionsChange: (String) -> Unit,    onExtraTimeHalfDurationChange: (String) -> Unit,    onHalftimeDurationChange: (String) -> Unit,    onHalfDurationChange: (String) -> Unit,    onGameDateTimeChange: (Long) -> Unit,    onCompetitionChange: (String) -> Unit,    onVenueChange: (String) -> Unit,    onRefereeAssignmentChange: (String) -> Unit,    onRemoveAwayPlayer: (Player) -> Unit,    onAddAwayPlayer: (String, String) -> Unit,    onRemoveHomePlayer: (Player) -> Unit,    onAddHomePlayer: (String, String) -> Unit,    onSaveGame: () -> Unit,    onAwayScoreChange: (String) -> Unit, // New callback    onHomeScoreChange: (String) -> Unit, // New callback    onNotesChanged: (String) -> Unit,    onAwayColorSelected: (Color) -> Unit,    onHomeColorSelected: (Color) -> Unit,    onMaxSubstitutionsChange: (String) -> Unit,    onExtraTimeHalfDurationChange: (String) -> Unit,    onHalftimeDurationChange: (String) -> Unit,    onHalfDurationChange: (String) -> Unit,    onGameDateTimeChange: (Long) -> Unit,    onCompetitionChange: (String) -> Unit,    onVenueChange: (String) -> Unit,    onRefereeAssignmentChange: (String) -> Unit,    onRemoveAwayPlayer: (Player) -> Unit,    onAddAwayPlayer: (String, String) -> Unit,    onRemoveHomePlayer: (Player) -> Unit,    onAddHomePlayer: (String, String) -> Unit,    onSaveGame: () -> Unit,    onAwayScoreChange: (String) -> Unit, // New callback    onHomeScoreChange: (String) -> Unit, // New callback    onNotesChanged: (String) -> Unit,    onAwayColorSelected: (Color) -> Unit,    onHomeColorSelected: (Color) -> Unit,    onMaxSubstitutionsChange: (String) -> Unit,    onExtraTimeHalfDurationChange: (String) -> Unit,    onHalftimeDurationChange: (String) -> Unit,    onHalfDurationChange: (String) -> Unit,    onGameDateTimeChange: (Long) -> Unit,    onCompetitionChange: (String) -> Unit,    onVenueChange: (String) -> Unit,    onRefereeAssignmentChange: (String) -> Unit,    onRemoveAwayPlayer: (Player) -> Unit,    onAddAwayPlayer: (String, String) -> Unit,    onRemoveHomePlayer: (Player) -> Unit,    onAddHomePlayer: (String, String) -> Unit,    onSaveGame: () -> Unit,    onAwayScoreChange: (String) -> Unit, // New callback    onHomeScoreChange: (String) -> Unit, // New callback    onNotesChanged: (String) -> Unit,    onAwayColorSelected: (Color) -> Unit,    onHomeColorSelected: (Color) -> Unit,    onMaxSubstitutionsChange: (String) -> Unit,    onExtraTimeHalfDurationChange: (String) -> Unit,    onHalftimeDurationChange: (String) -> Unit,    onHalfDurationChange: (String) -> Unit,    onGameDateTimeChange: (Long) -> Unit,    onCompetitionChange: (String) -> Unit,    onVenueChange: (String) -> Unit,    onRefereeAssignmentChange: (String) -> Unit,    onRemoveAwayPlayer: (Player) -> Unit,    onAddAwayPlayer: (String, String) -> Unit,    onRemoveHomePlayer: (Player) -> Unit,    onAddHomePlayer: (String, String) -> Unit,    onSaveGame: () -> Unit,    onAwayScoreChange: (String) -> Unit, // New callback    onHomeScoreChange: (String) -> Unit, // New callback    onNotesChanged: (String) -> Unit,    onAwayColorSelected: (Color) -> Unit,    onHomeColorSelected: (Color) -> Unit,    onMaxSubstitutionsChange: (String) -> Unit,    onExtraTimeHalfDurationChange: (String) -> Unit,    onHalftimeDurationChange: (String) -> Unit,    onHalfDurationChange: (String) -> Unit,    onGameDateTimeChange: (Long) -> Unit,    onCompetitionChange: (String) -> Unit,    onVenueChange: (String) -> Unit,    onRefereeAssignmentChange: (String) -> Unit,    onRemoveAwayPlayer: (Player) -> Unit,    onAddAwayPlayer: (String, String) -> Unit,    onRemoveHomePlayer: (Player) -> Unit,    onAddHomePlayer: (String, String) -> Unit,    onSaveGame: () -> Unit,    onAwayScoreChange: (String) -> Unit, // New callback    onHomeScoreChange: (String) -> Unit, // New callback    onNotesChanged: (String) -> Unit,    onAwayColorSelected: (Color) -> Unit,    onHomeColorSelected: (Color) -> Unit,    onMaxSubstitutionsChange: (String) -> Unit,    onExtraTimeHalfDurationChange: (String) -> Unit,    onHalftimeDurationChange: (String) -> Unit,    onHalfDurationChange: (String) -> Unit,    onGameDateTimeChange: (Long) -> Unit,    onCompetitionChange: (String) -> Unit,    onVenueChange: (String) -> Unit,    onRefereeAssignmentChange: (String) -> Unit,    onRemoveAwayPlayer: (Player) -> Unit,    onAddAwayPlayer: (String, String) -> Unit,    onRemoveHomePlayer: (Player) -> Unit,    onAddHomePlayer: (String, String) -> Unit,    onSaveGame: () -> Unit,    onAwayScoreChange: (String) -> Unit, // New callback    onHomeScoreChange: (String) -> Unit, // New callback    onNotesChanged: (String) -> Unit,    onAwayColorSelected: (Color) -> Unit,    onHomeColorSelected: (Color) -> Unit,    onMaxSubstitutionsChange: (String) -> Unit,    onExtraTimeHalfDurationChange: (String) -> Unit,    onHalftimeDurationChange: (String) -> Unit,    onHalfDurationChange: (String) -> Unit,    onGameDateTimeChange: (Long) -> Unit,    onCompetitionChange: (String) -> Unit,    onVenueChange: (String) -> Unit,    onRefereeAssignmentChange: (String) -> Unit,    onRemoveAwayPlayer: (Player) -> Unit,    onAddAwayPlayer: (String, String) -> Unit,    onRemoveHomePlayer: (Player) -> Unit,    onAddHomePlayer: (String, String) -> Unit,    onSaveGame: () -> Unit,    onAwayScoreChange: (String) -> Unit, // New callback    onHomeScoreChange: (String) -> Unit, // New callback    onNotesChanged: (String) -> Unit,    onAwayColorSelected: (Color) -> Unit,    onHomeColorSelected: (Color) -> Unit,    onMaxSubstitutionsChange: (String) -> Unit,    onExtraTimeHalfDurationChange: (String) -> Unit,    onHalftimeDurationChange: (String) -> Unit,    onHalfDurationChange: (String) -> Unit,    onGameDateTimeChange: (Long) -> Unit,    onCompetitionChange: (String) -> Unit,    onVenueChange: (String) -> Unit,    onRefereeAssignmentChange: (String) -> Unit,    onRemoveAwayPlayer: (Player) -> Unit,    onAddAwayPlayer: (String, String) -> Unit,    onRemoveHomePlayer: (Player) -> Unit,    onAddHomePlayer: (String, String) -> Unit,    onSaveGame: () -> Unit,    onAwayScoreChange: (String) -> Unit, // New callback    onHomeScoreChange: (String) -> Unit, // New callback    onNotesChanged: (String) -> Unit,    onAwayColorSelected: (Color) -> Unit,    onHomeColorSelected: (Color) -> Unit,    onMaxSubstitutionsChange: (String) -> Unit,    onExtraTimeHalfDurationChange: (String) -> Unit,    onHalftimeDurationChange: (String) -> Unit,    onHalfDurationChange: (String) -> Unit,    onGameDateTimeChange: (Long) -> Unit,    onCompetitionChange: (String) -> Unit,    onVenueChange: (String) -> Unit,    onRefereeAssignmentChange: (String) -> Unit,    onRemoveAwayPlayer: (Player) -> Unit,    onAddAwayPlayer: (String, String) -> Unit,    onRemoveHomePlayer: (Player) -> Unit,    onAddHomePlayer: (String, String) -> Unit,    onSaveGame: () -> Unit,    onAwayScoreChange: (String) -> Unit, // New callback    onHomeScoreChange: (String) -> Unit, // New callback    onNotesChanged: (String) -> Unit,    onAwayColorSelected: (Color) -> Unit,    onHomeColorSelected: (Color) -> Unit,    onMaxSubstitutionsChange: (String) -> Unit,    onExtraTimeHalfDurationChange: (String) -> Unit,    onHalftimeDurationChange: (String) -> Unit,    onHalfDurationChange: (String) -> Unit,    onGameDateTimeChange: (Long) -> Unit,    onCompetitionChange: (String) -> Unit,    onVenueChange: (String) -> Unit,    onRefereeAssignmentChange: (String) -> Unit,    onRemoveAwayPlayer: (Player) -> Unit,    onAddAwayPlayer: (String, String) -> Unit,    onRemoveHomePlayer: (Player) -> Unit,    onAddHomePlayer: (String, String) -> Unit,    onSaveGame: () -> Unit,    onAwayScoreChange: (String) -> Unit, // New callback    onHomeScoreChange: (String) -> Unit, // New callback            onHomeCaptainNumberChange = {},
            onAwayCaptainNumberChange = {},
            onFieldNumberChange = {},
            onRefereeAssignmentChange = {}, // Add dummy handler
            onVenueChange = {},
            onCompetitionChange = {},
            onGameDateTimeChange = {},
            onHalfDurationChange = {},
            onHalftimeDurationChange = {},
            onExtraTimeHalfDurationChange = {},
            onMaxSubstitutionsChange = {},
            onHomeColorSelected = {},
            onAwayColorSelected = {},
            onNotesChanged = {},
            onHomeScoreChange = {}, // Add dummy handler
            onAwayScoreChange = {}, // Add dummy handler
            onSaveGame = {}
        )
    }
}

// Dummy composable for previewing TimePickerDialog if Material 3 doesn't have a direct one yet
@Composable
fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Select Time") },
        text = content,
        confirmButton = confirmButton,
        dismissButton = dismissButton
    )
}
