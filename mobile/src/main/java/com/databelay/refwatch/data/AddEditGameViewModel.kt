package com.databelay.refwatch.data // Or a subpackage like com.databelay.refwatch.games.addedit

import android.app.Application
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.databelay.refwatch.R
import com.databelay.refwatch.auth.AuthRepository
import com.databelay.refwatch.common.AgeGroup
import com.databelay.refwatch.common.Game
import com.databelay.refwatch.common.Player
import com.databelay.refwatch.common.Team
import com.databelay.refwatch.common.TeamOfficial
import com.databelay.refwatch.common.theme.DefaultAwayJerseyColor
import com.databelay.refwatch.common.theme.DefaultHomeJerseyColor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject


// Data class to hold the UI state of the form
data class AddEditGameUiState(
    val gameId: String? = null, // To know if editing or adding
    val gameNumber: String = "XXXX",
    val fieldNumber: String = "",
    val homeTeamName: String = "",
    val awayTeamName: String = "",
    val homeTeamAbbr: String = "",
    val awayTeamAbbr: String = "",
    val homeCaptainNumber: String = "",
    val awayCaptainNumber: String = "",
    val refereeAssignment: String = "",
    val venue: String = "",
    val competition: String = "",
    val gameDateTimeEpochMillis: Long? = null,
    val halfDurationMinutes: Int = 45,
    val halftimeDurationMinutes: Int = 15,
    val hasExtraTime: Boolean = false,
    val extraTimeHalfDurationMinutes: Int = 0,
    val maxSubstitutionsAllowed: Int = 5,
    val hasTemporaryDismissals: Boolean = false,
    val temporaryDismissalMinutes: Int = 0,
    val homeTeamColorArgb: Int = DefaultHomeJerseyColor.toArgb(),
    val awayTeamColorArgb: Int = DefaultAwayJerseyColor.toArgb(),
    val kickOffTeam: Team = Team.HOME,
    // True if penalty shootout is allowed if the game ends tied after (optional) extra time.
    // Surfaced in AddEditGameScreen as a switch next to the Extra Time controls.
    // Drives the phase transition in WearGameViewModel: EXTRA_TIME_SECOND_HALF -> KICK_OFF_SELECTION_PENALTIES
    // (only if score is tied AND hasPenalties is true); otherwise the game ends.
    val hasPenalties: Boolean = false,
    /**
     * Number of kicks each team gets in the penalty shootout before sudden
     * death. Surfaced in AddEditGameScreen as a numeric field that is only
     * enabled when [hasPenalties] is true. Range 1..20; 0 means "use default 5"
     * and is rendered as a blank input field by the screen.
     */
    val penaltyKicksPerTeam: Int = 5,
    val homeRoster: List<Player> = emptyList(),
    val awayRoster: List<Player> = emptyList(),
    val homeOfficials: List<TeamOfficial> = emptyList(),
    val awayOfficials: List<TeamOfficial> = emptyList(),
    val fourthOfficial: String = "",
    val observer: String = "",
    val mainReferee: String = "",
    val assistantReferee1: String = "",
    val assistantReferee2: String = "",
    val notes: String = "",
    val ageGroup: AgeGroup? = null,
    val homeScore: String = "0",
    val awayScore: String = "0",
    val homeFieldLimit: Int = 11,
    val awayFieldLimit: Int = 11,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null,
    val isEditing: Boolean = false
)

@HiltViewModel
class AddEditGameViewModel @Inject constructor(
    application: Application,
    private val authRepository: AuthRepository,
    private val gameRepository: GameStorageMobile,
) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(AddEditGameUiState())
    val uiState: StateFlow<AddEditGameUiState> = _uiState.asStateFlow()
    private val tag = "AddEditGameViewModel"

    /**
     * Populates the form with data from an existing game for editing,
     * or sets default values for a new game.
     */
    fun initializeForm(gameId: String?) {
        viewModelScope.launch {
            val currentUserId = authRepository.getCurrentUserId()
            if (currentUserId == null) {
                Log.e(
                    tag,
                    "Cannot initialize AddEditGameViewModel: User not authenticated (userId is null)."
                )
                _uiState.update { it.copy(errorMessage = getApplication<Application>().getString(R.string.error_user_not_authenticated)) }
                return@launch
            }
            Log.d(tag, "Initializing for gameId: $gameId, UserID: $currentUserId")
            if (gameId != null) {
                _uiState.update { it.copy(gameId = gameId, errorMessage = null) }
                val gameToEdit = gameRepository.getGameById(currentUserId, gameId)

                if (gameToEdit != null) {
                    Log.i(tag, "Game found: ${gameToEdit.homeTeamName} for user $currentUserId")
                    Log.d(tag, "initializeForm: gameToEdit=$gameToEdit")
                    _uiState.update {
                        it.copy(
                            gameId = gameToEdit.id,
                            gameNumber = gameToEdit.gameNumber,
                            homeTeamName = gameToEdit.homeTeamName,
                            awayTeamName = gameToEdit.awayTeamName,
                            homeTeamAbbr = gameToEdit.homeTeamAbbr ?: "",
                            awayTeamAbbr = gameToEdit.awayTeamAbbr ?: "",
                            homeCaptainNumber = gameToEdit.homeCaptainNumber?.toString() ?: "",
                            awayCaptainNumber = gameToEdit.awayCaptainNumber?.toString() ?: "",
                            fieldNumber = gameToEdit.fieldNumber ?: "",
                            refereeAssignment = gameToEdit.refereeAssignment ?: "",
                            venue = gameToEdit.venue ?: "",
                            competition = gameToEdit.competition ?: "",
                            gameDateTimeEpochMillis = gameToEdit.gameDateTimeEpochMillis,
                            halfDurationMinutes = gameToEdit.halfDurationMinutes,
                            halftimeDurationMinutes = gameToEdit.halftimeDurationMinutes,
                            hasExtraTime = gameToEdit.hasExtraTime,
                            extraTimeHalfDurationMinutes = gameToEdit.extraTimeHalfDurationMinutes,
                            maxSubstitutionsAllowed = gameToEdit.maxSubstitutionsAllowed,
                            hasTemporaryDismissals = gameToEdit.hasTemporaryDismissals,
                            temporaryDismissalMinutes = gameToEdit.temporaryDismissalMinutes,
                            homeTeamColorArgb = gameToEdit.homeTeamColorArgb,
                            awayTeamColorArgb = gameToEdit.awayTeamColorArgb,
                            kickOffTeam = gameToEdit.kickOffTeam,
                            hasPenalties = gameToEdit.hasPenalties,
                            penaltyKicksPerTeam = gameToEdit.penaltyKicksPerTeam,
                            homeRoster = gameToEdit.homeRoster,
                            awayRoster = gameToEdit.awayRoster,
                            homeOfficials = gameToEdit.homeOfficials,
                            awayOfficials = gameToEdit.awayOfficials,
                            homeFieldLimit = if (gameToEdit.homeRoster.count { it.onField } <= 7) 7 else 11,
                            awayFieldLimit = if (gameToEdit.awayRoster.count { it.onField } <= 7) 7 else 11,
                            fourthOfficial = gameToEdit.fourthOfficial ?: "",
                            observer = gameToEdit.observer ?: "",
                            mainReferee = gameToEdit.mainReferee ?: "",
                            assistantReferee1 = gameToEdit.assistantReferee1 ?: "",
                            assistantReferee2 = gameToEdit.assistantReferee2 ?: "",
                            notes = gameToEdit.notes ?: "",
                            ageGroup = gameToEdit.ageGroup,
                            homeScore = gameToEdit.homeScore.toString(),
                            awayScore = gameToEdit.awayScore.toString(),
                            isEditing = true
                        )
                    }
                } else {
                    Log.w(tag, "Game with ID $gameId not found for user $currentUserId.")
                    _uiState.update {
                        it.copy(
                            errorMessage = getApplication<Application>().getString(R.string.error_game_not_found),
                            isEditing = false,
                            gameId = gameId
                        )
                    }
                }
            } else {
                Log.d(tag, "Initializing for a new game for user $currentUserId.")
                _uiState.value = AddEditGameUiState(
                    isEditing = false,
                    gameId = null,
                    gameDateTimeEpochMillis = System.currentTimeMillis()
                )
            }
        }
    }

    // --- Event Handlers for UI Inputs ---
    private fun sanitizeName(name: String): String {
        return name.filter { it.isLetterOrDigit() || it.isWhitespace() || it == '/' }
    }

    fun onHomeTeamNameChange(name: String) {
        _uiState.value = _uiState.value.copy(homeTeamName = sanitizeName(name))
    }

    fun onAwayTeamNameChange(name: String) {
        _uiState.value = _uiState.value.copy(awayTeamName = sanitizeName(name))
    }

    fun onHomeTeamAbbrChange(abbr: String) {
        val sanitized = sanitizeName(abbr).take(3).uppercase()
        _uiState.value = _uiState.value.copy(homeTeamAbbr = sanitized)
    }

    fun onAwayTeamAbbrChange(abbr: String) {
        val sanitized = sanitizeName(abbr).take(3).uppercase()
        _uiState.value = _uiState.value.copy(awayTeamAbbr = sanitized)
    }

    fun onHomeCaptainNumberChange(number: String) {
        _uiState.value = _uiState.value.copy(homeCaptainNumber = number)
    }

    fun onAwayCaptainNumberChange(number: String) {
        _uiState.value = _uiState.value.copy(awayCaptainNumber = number)
    }

    fun onFieldNumberChange(newFieldNumber: String) {
        _uiState.value = _uiState.value.copy(fieldNumber = newFieldNumber)
    }

    fun onRefereeAssignmentChange(assignment: String) {
        _uiState.value = _uiState.value.copy(refereeAssignment = assignment)
    }

    fun onVenueChange(venue: String) {
        _uiState.value = _uiState.value.copy(venue = venue)
    }

    fun onCompetitionChange(newCompetition: String) {
        _uiState.value = _uiState.value.copy(competition = newCompetition)
    }

    fun onMainRefereeChange(name: String) {
        _uiState.value = _uiState.value.copy(mainReferee = name)
    }

    fun onAssistantReferee1Change(name: String) {
        _uiState.value = _uiState.value.copy(assistantReferee1 = name)
    }

    fun onAssistantReferee2Change(name: String) {
        _uiState.value = _uiState.value.copy(assistantReferee2 = name)
    }

    fun onFourthOfficialChange(name: String) {
        _uiState.value = _uiState.value.copy(fourthOfficial = name)
    }

    fun onObserverChange(name: String) {
        _uiState.value = _uiState.value.copy(observer = name)
    }

    fun onGameDateTimeChange(epochMillis: Long?) {
        _uiState.value = _uiState.value.copy(gameDateTimeEpochMillis = epochMillis)
    }

    fun onHalfDurationChange(minutes: String) {
        _uiState.value = _uiState.value.copy(halfDurationMinutes = minutes.toIntOrNull() ?: 0)
    }

    fun onHalftimeDurationChange(minutes: String) {
        _uiState.value =
            _uiState.value.copy(halftimeDurationMinutes = minutes.toIntOrNull() ?: 0)
    }

    fun onHasExtraTimeChange(enabled: Boolean) {
        _uiState.update { 
            it.copy(
                hasExtraTime = enabled,
                extraTimeHalfDurationMinutes = if (enabled) (it.extraTimeHalfDurationMinutes.takeIf { m -> m > 0 } ?: 15) else 0
            )
        }
    }

    fun onExtraTimeHalfDurationChange(minutes: String) {
        _uiState.value =
            _uiState.value.copy(extraTimeHalfDurationMinutes = minutes.toIntOrNull() ?: 0)
    }

    fun onMaxSubstitutionsChange(max: String) {
        _uiState.value =
            _uiState.value.copy(maxSubstitutionsAllowed = max.toIntOrNull()?.coerceIn(0, 20) ?: 0)
    }

    fun onHasTemporaryDismissalsChange(enabled: Boolean) {
        _uiState.update { 
            it.copy(
                hasTemporaryDismissals = enabled,
                temporaryDismissalMinutes = if (enabled) it.temporaryDismissalMinutes else 0
            )
        }
    }

    fun onTemporaryDismissalChange(minutes: String) {
        _uiState.value =
            _uiState.value.copy(temporaryDismissalMinutes = minutes.toIntOrNull()?.coerceIn(0, 60) ?: 0)
    }

    fun onHomeColorSelected(color: Color) {
        _uiState.value = _uiState.value.copy(homeTeamColorArgb = color.toArgb())
    }

    fun onAwayColorSelected(color: Color) {
        _uiState.value = _uiState.value.copy(awayTeamColorArgb = color.toArgb())
    }

    fun onKickOffTeamSelected(team: Team) {
        _uiState.value = _uiState.value.copy(kickOffTeam = team)
    }

    fun onHasPenaltiesChange(enabled: Boolean) {
        _uiState.update {
            it.copy(
                hasPenalties = enabled,
                penaltyKicksPerTeam = if (enabled) it.penaltyKicksPerTeam else 5
            )
        }
    }

    fun onPenaltyKicksPerTeamChange(value: String) {
        if (!_uiState.value.hasPenalties) return
        val parsed = value.toIntOrNull()
        _uiState.value = _uiState.value.copy(
            penaltyKicksPerTeam = parsed?.coerceIn(1, 20) ?: 0
        )
    }

    fun onNotesChanged(newNotes: String) {
        _uiState.value = _uiState.value.copy(notes = newNotes)
    }

    fun onHomeScoreChange(score: String) {
        _uiState.value = _uiState.value.copy(homeScore = score)
    }

    fun onAwayScoreChange(score: String) {
        _uiState.value = _uiState.value.copy(awayScore = score)
    }

    // --- Roster Management ---

    fun onHomeRosterChange(roster: List<Player>) {
        _uiState.value = _uiState.value.copy(homeRoster = roster)
        // Automatically sync captain number if designated
        roster.find { it.captain }?.let {
            _uiState.value = _uiState.value.copy(homeCaptainNumber = it.number.toString())
        }
    }

    fun onAwayRosterChange(roster: List<Player>) {
        _uiState.value = _uiState.value.copy(awayRoster = roster)
        // Automatically sync captain number if designated
        roster.find { it.captain }?.let {
            _uiState.value = _uiState.value.copy(awayCaptainNumber = it.number.toString())
        }
    }

    fun onHomeOfficialsChange(officials: List<TeamOfficial>) {
        _uiState.value = _uiState.value.copy(homeOfficials = officials)
    }

    fun onAwayOfficialsChange(officials: List<TeamOfficial>) {
        _uiState.value = _uiState.value.copy(awayOfficials = officials)
    }

    fun applyRosterTemplate(team: Team, starters: Int, subs: Int) {
        val roster = (1..starters).map { number ->
            Player(name = "", number = number, captain = number == 1, onField = true)
        } + ((starters + 1)..(starters + subs)).map { number ->
            Player(name = "", number = number, captain = false, onField = false)
        }
        if (team == Team.HOME) {
            _uiState.update { it.copy(homeFieldLimit = starters) }
            onHomeRosterChange(roster)
        } else {
            _uiState.update { it.copy(awayFieldLimit = starters) }
            onAwayRosterChange(roster)
        }
    }

    fun toggleCaptain(team: Team, playerId: String) {
        if (team == Team.HOME) {
            val updated = _uiState.value.homeRoster.map {
                it.copy(captain = it.id == playerId)
            }
            onHomeRosterChange(updated)
        } else {
            val updated = _uiState.value.awayRoster.map {
                it.copy(captain = it.id == playerId)
            }
            onAwayRosterChange(updated)
        }
    }

    fun toggleOnField(team: Team, playerId: String) {
        val roster = if (team == Team.HOME) _uiState.value.homeRoster else _uiState.value.awayRoster
        val player = roster.find { it.id == playerId } ?: return
        val limit = if (team == Team.HOME) _uiState.value.homeFieldLimit else _uiState.value.awayFieldLimit
        
        if (!player.onField) {
            // Moving to field. Check if we need to swap.
            val currentOnField = roster.filter { it.onField }
            if (currentOnField.size >= limit) {
                // Limit reached, move first on-field player to bench
                val firstOnField = currentOnField.first()
                if (team == Team.HOME) {
                    val updated = _uiState.value.homeRoster.map {
                        when (it.id) {
                            playerId -> it.copy(onField = true)
                            firstOnField.id -> it.copy(onField = false)
                            else -> it
                        }
                    }
                    onHomeRosterChange(updated)
                } else {
                    val updated = _uiState.value.awayRoster.map {
                        when (it.id) {
                            playerId -> it.copy(onField = true)
                            firstOnField.id -> it.copy(onField = false)
                            else -> it
                        }
                    }
                    onAwayRosterChange(updated)
                }
                return
            }
        }

        if (team == Team.HOME) {
            val updated = _uiState.value.homeRoster.map {
                if (it.id == playerId) it.copy(onField = !it.onField) else it
            }
            onHomeRosterChange(updated)
        } else {
            val updated = _uiState.value.awayRoster.map {
                if (it.id == playerId) it.copy(onField = !it.onField) else it
            }
            onAwayRosterChange(updated)
        }
    }

    fun addPlayer(team: Team, number: Int, name: String = "") {
        if (number == 0) return // Block number 0
        val limit = if (team == Team.HOME) _uiState.value.homeFieldLimit else _uiState.value.awayFieldLimit
        if (team == Team.HOME) {
            val current = _uiState.value.homeRoster
            if (current.none { it.number == number }) {
                val shouldBeOnField = current.count { it.onField } < limit
                onHomeRosterChange(current + Player(name = name, number = number, onField = shouldBeOnField))
            }
        } else {
            val current = _uiState.value.awayRoster
            if (current.none { it.number == number }) {
                val shouldBeOnField = current.count { it.onField } < limit
                onAwayRosterChange(current + Player(name = name, number = number, onField = shouldBeOnField))
            }
        }
    }

    fun removePlayer(team: Team, playerId: String) {
        if (team == Team.HOME) {
            onHomeRosterChange(_uiState.value.homeRoster.filterNot { it.id == playerId })
        } else {
            onAwayRosterChange(_uiState.value.awayRoster.filterNot { it.id == playerId })
        }
    }

    fun updatePlayerNumber(team: Team, playerId: String, newNumber: Int) {
        if (newNumber == 0) return
        if (team == Team.HOME) {
            val updated = _uiState.value.homeRoster.map {
                if (it.id == playerId) it.copy(number = newNumber) else it
            }
            onHomeRosterChange(updated)
        } else {
            val updated = _uiState.value.awayRoster.map {
                if (it.id == playerId) it.copy(number = newNumber) else it
            }
            onAwayRosterChange(updated)
        }
    }

    fun updatePlayerName(team: Team, playerId: String, newName: String) {
        if (team == Team.HOME) {
            val updated = _uiState.value.homeRoster.map {
                if (it.id == playerId) it.copy(name = newName) else it
            }
            onHomeRosterChange(updated)
        } else {
            val updated = _uiState.value.awayRoster.map {
                if (it.id == playerId) it.copy(name = newName) else it
            }
            onAwayRosterChange(updated)
        }
    }

    fun addOfficial(team: Team, name: String, role: String) {
        if (name.isBlank()) return
        if (team == Team.HOME) {
            val current = _uiState.value.homeOfficials
            onHomeOfficialsChange(current + TeamOfficial(name = name, role = role))
        } else {
            val current = _uiState.value.awayOfficials
            onAwayOfficialsChange(current + TeamOfficial(name = name, role = role))
        }
    }

    fun removeOfficial(team: Team, id: String) {
        if (team == Team.HOME) {
            onHomeOfficialsChange(_uiState.value.homeOfficials.filterNot { it.id == id })
        } else {
            onAwayOfficialsChange(_uiState.value.awayOfficials.filterNot { it.id == id })
        }
    }



    /**
     * Validates the current UI state and constructs a Game object,
     * then passes it to the onGameSaved callback.
     */
    fun onSaveGame() {
        val currentState = _uiState.value
        Log.d(tag, "onSaveGame called. isSaving: ${currentState.isSaving}, gameId: ${currentState.gameId}")
        
        viewModelScope.launch {
            val currentUserId = authRepository.getCurrentUserId()

            if (currentUserId == null) {
                Log.e(tag, "Cannot save: User not authenticated.")
                _uiState.update { it.copy(errorMessage = getApplication<Application>().getString(R.string.error_cannot_save_user)) }
                return@launch
            }

            _uiState.update { it.copy(isSaving = true, saveSuccess = false, errorMessage = null) }

            // --- VALIDATION ---
            if (currentState.homeTeamName.trim().equals(currentState.awayTeamName.trim(), ignoreCase = true)) {
                _uiState.update { it.copy(isSaving = false, errorMessage = getApplication<Application>().getString(R.string.error_identical_teams)) }
                return@launch
            }

            if (currentState.homeTeamAbbr.isNotBlank() && currentState.homeTeamAbbr.equals(currentState.awayTeamAbbr, ignoreCase = true)) {
                _uiState.update { it.copy(isSaving = false, errorMessage = getApplication<Application>().getString(R.string.error_identical_abbr)) }
                return@launch
            }

            if (currentState.homeTeamColorArgb == currentState.awayTeamColorArgb) {
                _uiState.update { it.copy(isSaving = false, errorMessage = getApplication<Application>().getString(R.string.error_identical_colors)) }
                return@launch
            }

            val gameToSave: Game

            if (currentState.gameId != null && currentState.isEditing) {
                // --- EDITING AN EXISTING GAME ---
                Log.d(tag, "Attempting to save changes to existing game: ${currentState.gameId}")
                val existingGame = gameRepository.getGameById(currentUserId, currentState.gameId)

                if (existingGame == null) {
                    Log.e(
                        tag,
                        "Failed to fetch existing game (${currentState.gameId}) for update. Cannot save."
                    )
                    _uiState.update { it.copy(errorMessage = getApplication<Application>().getString(R.string.error_original_game_not_found)) }
                    return@launch
                }

                gameToSave = existingGame.copy(
                    userId = currentUserId, // Ensure userId is present
                    gameNumber = currentState.gameNumber,
                    homeTeamName = currentState.homeTeamName,
                    awayTeamName = currentState.awayTeamName,
                    homeTeamAbbr = currentState.homeTeamAbbr.ifBlank { null },
                    awayTeamAbbr = currentState.awayTeamAbbr.ifBlank { null },
                    homeCaptainNumber = currentState.homeCaptainNumber.toIntOrNull(),
                    awayCaptainNumber = currentState.awayCaptainNumber.toIntOrNull(),
                    fieldNumber = currentState.fieldNumber.takeIf { it.isNotBlank() },
                    refereeAssignment = currentState.refereeAssignment.takeIf { it.isNotBlank() },
                    venue = currentState.venue.takeIf { it.isNotBlank() },
                    competition = currentState.competition.takeIf { it.isNotBlank() },
                    gameDateTimeEpochMillis = currentState.gameDateTimeEpochMillis,
                    halfDurationMinutes = currentState.halfDurationMinutes,
                    halftimeDurationMinutes = currentState.halftimeDurationMinutes,
                    hasExtraTime = currentState.hasExtraTime,
                    extraTimeHalfDurationMinutes = currentState.extraTimeHalfDurationMinutes,
                    maxSubstitutionsAllowed = currentState.maxSubstitutionsAllowed,
                    hasTemporaryDismissals = currentState.hasTemporaryDismissals,
                    temporaryDismissalMinutes = currentState.temporaryDismissalMinutes,
                    homeTeamColorArgb = currentState.homeTeamColorArgb,
                    awayTeamColorArgb = currentState.awayTeamColorArgb,
                    kickOffTeam = currentState.kickOffTeam,
                    hasPenalties = currentState.hasPenalties,
                    penaltyKicksPerTeam = currentState.penaltyKicksPerTeam,
                    homeRoster = currentState.homeRoster,
                    awayRoster = currentState.awayRoster,
                    homeOfficials = currentState.homeOfficials,
                    awayOfficials = currentState.awayOfficials,
                    fourthOfficial = currentState.fourthOfficial.takeIf { it.isNotBlank() },
                    observer = currentState.observer.takeIf { it.isNotBlank() },
                    mainReferee = currentState.mainReferee.takeIf { it.isNotBlank() },
                    assistantReferee1 = currentState.assistantReferee1.takeIf { it.isNotBlank() },
                    assistantReferee2 = currentState.assistantReferee2.takeIf { it.isNotBlank() },
                    notes = currentState.notes.takeIf { it.isNotBlank() },
                    ageGroup = currentState.ageGroup,
                    homeScore = currentState.homeScore.toIntOrNull() ?: existingGame.homeScore,
                    awayScore = currentState.awayScore.toIntOrNull() ?: existingGame.awayScore,
                    lastUpdated = System.currentTimeMillis()
                )
                Log.d(tag, "Updated game object prepared: $gameToSave")

            } else {
                // --- CREATING A NEW GAME ---
                Log.d(tag, "Attempting to save a new game for user $currentUserId.")
                val newGameId = UUID.randomUUID().toString()
                gameToSave = Game(
                    id = newGameId,
                    userId = currentUserId, // CRITICAL: Assign userId
                    gameNumber = currentState.gameNumber,
                    homeTeamName = currentState.homeTeamName,
                    awayTeamName = currentState.awayTeamName,
                    homeTeamAbbr = currentState.homeTeamAbbr.ifBlank { null },
                    awayTeamAbbr = currentState.awayTeamAbbr.ifBlank { null },
                    homeCaptainNumber = currentState.homeCaptainNumber.toIntOrNull(),
                    awayCaptainNumber = currentState.awayCaptainNumber.toIntOrNull(),
                    fieldNumber = currentState.fieldNumber.takeIf { it.isNotBlank() },
                    refereeAssignment = currentState.refereeAssignment.takeIf { it.isNotBlank() },
                    venue = currentState.venue.takeIf { it.isNotBlank() },
                    competition = currentState.competition.takeIf { it.isNotBlank() },
                    gameDateTimeEpochMillis = currentState.gameDateTimeEpochMillis,
                    halfDurationMinutes = currentState.halfDurationMinutes,
                    halftimeDurationMinutes = currentState.halftimeDurationMinutes,
                    hasExtraTime = currentState.hasExtraTime,
                    extraTimeHalfDurationMinutes = currentState.extraTimeHalfDurationMinutes,
                    maxSubstitutionsAllowed = currentState.maxSubstitutionsAllowed,
                    hasTemporaryDismissals = currentState.hasTemporaryDismissals,
                    temporaryDismissalMinutes = currentState.temporaryDismissalMinutes,
                    homeTeamColorArgb = currentState.homeTeamColorArgb,
                    awayTeamColorArgb = currentState.awayTeamColorArgb,
                    kickOffTeam = currentState.kickOffTeam,
                    hasPenalties = currentState.hasPenalties,
                    penaltyKicksPerTeam = currentState.penaltyKicksPerTeam,
                    homeRoster = currentState.homeRoster,
                    awayRoster = currentState.awayRoster,
                    homeOfficials = currentState.homeOfficials,
                    awayOfficials = currentState.awayOfficials,
                    fourthOfficial = currentState.fourthOfficial.takeIf { it.isNotBlank() },
                    observer = currentState.observer.takeIf { it.isNotBlank() },
                    mainReferee = currentState.mainReferee.takeIf { it.isNotBlank() },
                    assistantReferee1 = currentState.assistantReferee1.takeIf { it.isNotBlank() },
                    assistantReferee2 = currentState.assistantReferee2.takeIf { it.isNotBlank() },
                    notes = currentState.notes.takeIf { it.isNotBlank() },
                    ageGroup = currentState.ageGroup,
                    homeScore = currentState.homeScore.toIntOrNull() ?: 0,
                    awayScore = currentState.awayScore.toIntOrNull() ?: 0,
                    lastUpdated = System.currentTimeMillis()
                )
                Log.d(tag, "New game object prepared: $gameToSave")
            }

            val result = gameRepository.addOrUpdateGame(currentUserId, gameToSave)
            Log.d(tag, "gameRepository.addOrUpdateGame result: ${result.isSuccess}")
            if (result.isSuccess) {
                Log.i(
                    tag,
                    "Game saved/updated successfully for user $currentUserId. Game ID: ${gameToSave.id}"
                )
                _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
            } else {
                Log.e(
                    tag,
                    "Failed to save game for user $currentUserId: ${result.exceptionOrNull()?.message}"
                )
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = getApplication<Application>().getString(R.string.error_save_game_failed, result.exceptionOrNull()?.localizedMessage ?: "")
                    )
                }
            }
        }
    }
}