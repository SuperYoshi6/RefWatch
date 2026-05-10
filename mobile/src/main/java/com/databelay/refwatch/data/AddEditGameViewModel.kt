package com.databelay.refwatch.data // Or a subpackage like com.databelay.refwatch.games.addedit

import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.databelay.refwatch.auth.AuthRepository
import com.databelay.refwatch.common.AgeGroup
import com.databelay.refwatch.common.Game
import com.databelay.refwatch.common.Team
import com.databelay.refwatch.common.theme.DefaultAwayJerseyColor
import com.databelay.refwatch.common.theme.DefaultHomeJerseyColor
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
    val halftimeDurationMinutes: Int = 0,
    val extraTimeHalfDurationMinutes: Int = 15,
    val maxSubstitutionsAllowed: Int = 5,
    val homeTeamColorArgb: Int = DefaultHomeJerseyColor.toArgb(),
    val awayTeamColorArgb: Int = DefaultAwayJerseyColor.toArgb(),
    val kickOffTeam: Team = Team.HOME,
    val notes: String = "",
    val ageGroup: AgeGroup? = null,
    val homeScore: String = "0",
    val awayScore: String = "0",
    val errorMessage: String? = null,
    val isEditing: Boolean = false
)

@HiltViewModel
class AddEditGameViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val firestore: FirebaseFirestore,
) : ViewModel() {
    private val gameRepository: GameStorageMobile = GameStorageMobile(firestore)
    private val _uiState = MutableStateFlow(AddEditGameUiState())
    val uiState: StateFlow<AddEditGameUiState> = _uiState.asStateFlow()
    private val tag = "AddEditGameViewModel"
    private var editingGameId: String? = null

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
                _uiState.update { it.copy(errorMessage = "User not authenticated.") }
                return@launch
            }
            Log.d(tag, "Initializing for gameId: $gameId, UserID: $currentUserId")
            if (gameId != null) {
                _uiState.update { it.copy(gameId = gameId, errorMessage = null) }
                val gameToEdit = gameRepository.getGamesFlow(currentUserId).firstOrNull()
                    ?.find { it.id == gameId }

                if (gameToEdit != null) {
                    Log.i(tag, "Game found: ${gameToEdit.homeTeamName} for user $currentUserId")
                    Log.d(tag, "initializeForm: gameToEdit=$gameToEdit")
                    editingGameId = gameToEdit.id
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
                            extraTimeHalfDurationMinutes = gameToEdit.extraTimeHalfDurationMinutes,
                            maxSubstitutionsAllowed = gameToEdit.maxSubstitutionsAllowed,
                            homeTeamColorArgb = gameToEdit.homeTeamColorArgb,
                            awayTeamColorArgb = gameToEdit.awayTeamColorArgb,
                            kickOffTeam = gameToEdit.kickOffTeam,
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
                            errorMessage = "Game not found.",
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
    fun onHomeTeamNameChange(name: String) {
        _uiState.value = _uiState.value.copy(homeTeamName = name)
    }

    fun onAwayTeamNameChange(name: String) {
        _uiState.value = _uiState.value.copy(awayTeamName = name)
    }

    fun onHomeTeamAbbrChange(abbr: String) {
        val sanitized = abbr.take(6) // Allow up to 6 chars, no forced uppercase or filtering
        _uiState.value = _uiState.value.copy(homeTeamAbbr = sanitized)
    }

    fun onAwayTeamAbbrChange(abbr: String) {
        val sanitized = abbr.take(6) // Allow up to 6 chars, no forced uppercase or filtering
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

    fun onGameDateTimeChange(epochMillis: Long?) {
        _uiState.value = _uiState.value.copy(gameDateTimeEpochMillis = epochMillis)
    }

    fun onHalfDurationChange(minutes: String) {
        _uiState.value = _uiState.value.copy(halfDurationMinutes = minutes.toIntOrNull() ?: 45)
    }

    fun onHalftimeDurationChange(minutes: String) {
        _uiState.value =
            _uiState.value.copy(halftimeDurationMinutes = minutes.toIntOrNull() ?: 0)
    }

    fun onExtraTimeHalfDurationChange(minutes: String) {
        _uiState.value =
            _uiState.value.copy(extraTimeHalfDurationMinutes = minutes.toIntOrNull() ?: 15)
    }

    fun onMaxSubstitutionsChange(max: String) {
        _uiState.value =
            _uiState.value.copy(maxSubstitutionsAllowed = max.toIntOrNull()?.coerceIn(0, 20) ?: 5)
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

    fun onNotesChanged(newNotes: String) {
        _uiState.value = _uiState.value.copy(notes = newNotes)
    }

    fun onHomeScoreChange(score: String) {
        _uiState.value = _uiState.value.copy(homeScore = score)
    }

    fun onAwayScoreChange(score: String) {
        _uiState.value = _uiState.value.copy(awayScore = score)
    }


    /**
     * Validates the current UI state and constructs a Game object,
     * then passes it to the onGameSaved callback.
     */
    fun onSaveGame() {
        val currentState = _uiState.value
        viewModelScope.launch {
            val currentUserId = authRepository.getCurrentUserId()

            if (currentUserId == null) {
                Log.e(tag, "Cannot save: User not authenticated.")
                _uiState.update { it.copy(errorMessage = "Cannot save: User not authenticated.") }
                return@launch
            }

            _uiState.update { it.copy(errorMessage = null) }

            val gameToSave: Game

            if (currentState.gameId != null && currentState.isEditing) {
                // --- EDITING AN EXISTING GAME ---
                Log.d(tag, "Attempting to save changes to existing game: ${currentState.gameId}")
                val existingGame = gameRepository.getGamesFlow(currentUserId)
                    .firstOrNull()
                    ?.find { it.id == currentState.gameId }

                if (existingGame == null) {
                    Log.e(
                        tag,
                        "Failed to fetch existing game (${currentState.gameId}) for update. Cannot save."
                    )
                    _uiState.update { it.copy(errorMessage = "Error: Original game not found. Cannot save.") }
                    return@launch
                }

                gameToSave = existingGame.copy(
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
                    extraTimeHalfDurationMinutes = currentState.extraTimeHalfDurationMinutes,
                    maxSubstitutionsAllowed = currentState.maxSubstitutionsAllowed,
                    homeTeamColorArgb = currentState.homeTeamColorArgb,
                    awayTeamColorArgb = currentState.awayTeamColorArgb,
                    kickOffTeam = currentState.kickOffTeam,
                    notes = currentState.notes.takeIf { it.isNotBlank() },
                    ageGroup = currentState.ageGroup,
                    homeScore = currentState.homeScore.toIntOrNull() ?: existingGame.homeScore,
                    awayScore = currentState.awayScore.toIntOrNull() ?: existingGame.awayScore,
                    lastUpdated = System.currentTimeMillis()
                )
                Log.d(tag, "Updated game object prepared: $gameToSave")

            } else {
                // --- CREATING A NEW GAME ---
                Log.d(tag, "Attempting to save a new game.")
                val newGameId = UUID.randomUUID().toString()
                gameToSave = Game(
                    id = newGameId,
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
                    extraTimeHalfDurationMinutes = currentState.extraTimeHalfDurationMinutes,
                    maxSubstitutionsAllowed = currentState.maxSubstitutionsAllowed,
                    homeTeamColorArgb = currentState.homeTeamColorArgb,
                    awayTeamColorArgb = currentState.awayTeamColorArgb,
                    kickOffTeam = currentState.kickOffTeam,
                    notes = currentState.notes.takeIf { it.isNotBlank() },
                    ageGroup = currentState.ageGroup,
                    homeScore = currentState.homeScore.toIntOrNull() ?: 0,
                    awayScore = currentState.awayScore.toIntOrNull() ?: 0,
                    lastUpdated = System.currentTimeMillis()
                )
                Log.d(tag, "New game object prepared: $gameToSave")
            }

            val result = gameRepository.addOrUpdateGame(currentUserId, gameToSave)
            if (result.isSuccess) {
                Log.i(
                    tag,
                    "Game saved/updated successfully for user $currentUserId. Game ID: ${gameToSave.id}"
                )
            } else {
                Log.e(
                    tag,
                    "Failed to save game for user $currentUserId: ${result.exceptionOrNull()?.message}"
                )
                _uiState.update {
                    it.copy(errorMessage = "Failed to save game: ${result.exceptionOrNull()?.localizedMessage}")
                }
            }
        }
    }
}
