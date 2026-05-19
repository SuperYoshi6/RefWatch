package com.databelay.refwatch.data // Or a subpackage like com.databelay.refwatch.games.addedit
package com.databelay.refwatch.data // Or a subpackage like com.databelay.refwatch.games.addedit
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel.toArgb
import androidx.lifecycle.viewModelScope
import com.databelay.refwatch.auth.AuthRepository
import com.databelay.refwatch.common.AgeGrouptory
import com.databelay.refwatch.common.Gameroup
import com.databelay.refwatch.common.Player
import com.databelay.refwatch.common.Teamer
import com.databelay.refwatch.common.theme.DefaultAwayJerseyColor
import com.databelay.refwatch.common.theme.DefaultHomeJerseyColor
import com.google.firebase.firestore.FirebaseFirestoreJerseyColor
import dagger.hilt.android.lifecycle.HiltViewModeltore
import kotlinx.coroutines.flow.MutableStateFlowdel
import kotlinx.coroutines.flow.StateFlowateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.updaterNull
import kotlinx.coroutines.launchpdate
import java.util.UUIDines.launch
import javax.inject.Inject
import javax.inject.Inject

// Data class to hold the UI state of the form
data class AddEditGameUiState(tate of the form
    val gameId: String? = null, // To know if editing or adding
    val gameNumber: String = "XXXX",o know if editing or adding
    val fieldNumber: String = "",X",
    val homeTeamName: String = "",
    val awayTeamName: String = "",
    val homeTeamAbbr: String = "",
    val awayTeamAbbr: String = "",
    val homeCaptainNumber: String = "",
    val awayCaptainNumber: String = "",
    val homeRoster: List<Player> = emptyList(),
    val homeRoster: List<Player> = emptyList(),
    val awayRoster: List<Player> = emptyList(),
    val refereeAssignment: String = "",
    val venue: String = "", = "",
    val competition: String = "",Long? = null,
    val gameDateTimeEpochMillis: Long? = null,
    val halftimeDurationMinutes: Int = 0,
    val extraTimeHalfDurationMinutes: Int = 15,
    val maxSubstitutionsAllowed: Int = 5, = 15,
    val homeTeamColorArgb: Int = DefaultHomeJerseyColor.toArgb(),
    val awayTeamColorArgb: Int = DefaultAwayJerseyColor.toArgb(),
    val kickOffTeam: Team = Team.HOME,ltAwayJerseyColor.toArgb(),
    val notes: String = "", Team.HOME,
    val ageGroup: AgeGroup? = null,
    val homeScore: String = "0",ll,
    val awayScore: String = "0",
    val homeRoster: List<Player> = emptyList(),
    val awayRoster: List<Player> = emptyList(),
    val errorMessage: String? = null,ptyList(),
    val isEditing: Boolean = falsell,
)   val isEditing: Boolean = false
)
@HiltViewModel
class AddEditGameViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val firestore: FirebaseFirestore,y,
) : ViewModel() {irestore: FirebaseFirestore,
    private val gameRepository: GameStorageMobile = GameStorageMobile(firestore)
    private val _uiState = MutableStateFlow(AddEditGameUiState())bile(firestore)
    val uiState: StateFlow<AddEditGameUiState> = _uiState.asStateFlow()
    private val tag = "AddEditGameViewModel"e> = _uiState.asStateFlow()
    private var editingGameId: String? = null
    private var editingGameId: String? = null
    /**
     * Populates the form with data from an existing game for editing,
     * or sets default values for a new game.xisting game for editing,
     */or sets default values for a new game.
    fun initializeForm(gameId: String?) {
        viewModelScope.launch {String?) {
            val currentUserId = authRepository.getCurrentUserId()
            if (currentUserId == null) {sitory.getCurrentUserId()
                Log.e(tUserId == null) {
                    tag,
                    "Cannot initialize AddEditGameViewModel: User not authenticated (userId is null)."
                )   "Cannot initialize AddEditGameViewModel: User not authenticated (userId is null)."
                _uiState.update { it.copy(errorMessage = "User not authenticated.") }
                return@launchte { it.copy(errorMessage = "User not authenticated.") }
            }   return@launch
            Log.d(tag, "Initializing for gameId: $gameId, UserID: $currentUserId")
            if (gameId != null) {ing for gameId: $gameId, UserID: $currentUserId")
                _uiState.update { it.copy(gameId = gameId, errorMessage = null) }
                val gameToEdit = gameRepository.getGamesFlow(currentUserId).firstOrNull()
                    ?.find { it.id == gameId }y.getGamesFlow(currentUserId).firstOrNull()
                    ?.find { it.id == gameId }
                if (gameToEdit != null) {
                    Log.i(tag, "Game found: ${gameToEdit.homeTeamName} for user $currentUserId")
                    Log.d(tag, "initializeForm: gameToEdit=$gameToEdit")or user $currentUserId")
                    editingGameId = gameToEdit.idameToEdit=$gameToEdit")
                    _uiState.update {ameToEdit.id
                        it.copy(ate {
                            gameId = gameToEdit.id,
                            gameNumber = gameToEdit.gameNumber,
                            homeTeamName = gameToEdit.homeTeamName,
                            awayTeamName = gameToEdit.awayTeamName,
                            homeTeamAbbr = gameToEdit.homeTeamAbbr ?: "",
                            awayTeamAbbr = gameToEdit.awayTeamAbbr ?: "",
                            homeCaptainNumber = gameToEdit.homeCaptainNumber?.toString() ?: "",
                            awayCaptainNumber = gameToEdit.awayCaptainNumber?.toString() ?: "",
                            homeRoster = gameToEdit.homeRoster,CaptainNumber?.toString() ?: "",
                            awayRoster = gameToEdit.awayRoster,
                            fieldNumber = gameToEdit.fieldNumber ?: "",
                            refereeAssignment = gameToEdit.refereeAssignment ?: "",
                            venue = gameToEdit.venue ?: "",refereeAssignment ?: "",
                            competition = gameToEdit.competition ?: "",
                            gameDateTimeEpochMillis = gameToEdit.gameDateTimeEpochMillis,
                            halfDurationMinutes = gameToEdit.halfDurationMinutes,hMillis,
                            halftimeDurationMinutes = gameToEdit.halftimeDurationMinutes,
                            extraTimeHalfDurationMinutes = gameToEdit.extraTimeHalfDurationMinutes,
                            maxSubstitutionsAllowed = gameToEdit.maxSubstitutionsAllowed,onMinutes,
                            homeTeamColorArgb = gameToEdit.homeTeamColorArgb,ionsAllowed,
                            awayTeamColorArgb = gameToEdit.awayTeamColorArgb,
                            kickOffTeam = gameToEdit.kickOffTeam,amColorArgb,
                            notes = gameToEdit.notes ?: "",fTeam,
                            ageGroup = gameToEdit.ageGroup,
                            homeScore = gameToEdit.homeScore.toString(),
                            awayScore = gameToEdit.awayScore.toString(),
                            homeRoster = gameToEdit.homeRoster,String(),
                            awayRoster = gameToEdit.awayRoster,
                            isEditing = trueeToEdit.awayRoster,
                        )   isEditing = true
                    }   )
                } else {
                    Log.w(tag, "Game with ID $gameId not found for user $currentUserId.")
                    _uiState.update {with ID $gameId not found for user $currentUserId.")
                        it.copy(ate {
                            errorMessage = "Game not found.",
                            isEditing = false,me not found.",
                            gameId = gameIdse,
                        )   gameId = gameId
                    }   )
                }   }
            } else {
                Log.d(tag, "Initializing for a new game for user $currentUserId.")
                _uiState.value = AddEditGameUiState(ame for user $currentUserId.")
                    isEditing = false,itGameUiState(
                    gameId = null,lse,
                    gameDateTimeEpochMillis = System.currentTimeMillis()
                )   gameDateTimeEpochMillis = System.currentTimeMillis()
            }   )
        }   }
    }   }
    }
    // --- Event Handlers for UI Inputs ---
    fun onHomeTeamNameChange(name: String) {
        _uiState.value = _uiState.value.copy(homeTeamName = name)
    }   _uiState.value = _uiState.value.copy(homeTeamName = name)
    }
    fun onAwayTeamNameChange(name: String) {
        _uiState.value = _uiState.value.copy(awayTeamName = name)
    }   _uiState.value = _uiState.value.copy(awayTeamName = name)
    }
    fun onHomeTeamAbbrChange(abbr: String) {
        val sanitized = abbr.take(6) // Allow up to 6 chars, no forced uppercase or filtering
        _uiState.value = _uiState.value.copy(homeTeamAbbr = sanitized) uppercase or filtering
    }   _uiState.value = _uiState.value.copy(homeTeamAbbr = sanitized)
    }
    fun onAwayTeamAbbrChange(abbr: String) {
        val sanitized = abbr.take(6) // Allow up to 6 chars, no forced uppercase or filtering
        _uiState.value = _uiState.value.copy(awayTeamAbbr = sanitized) uppercase or filtering
    }   _uiState.value = _uiState.value.copy(awayTeamAbbr = sanitized)
    }
    fun onHomeCaptainNumberChange(number: String) {
        _uiState.value = _uiState.value.copy(homeCaptainNumber = number)
    }   _uiState.value = _uiState.value.copy(homeCaptainNumber = number)
    }
    fun onAwayCaptainNumberChange(number: String) {
        _uiState.value = _uiState.value.copy(awayCaptainNumber = number)
    }   _uiState.value = _uiState.value.copy(awayCaptainNumber = number)
    }
    fun onHomeRosterChange(roster: List<Player>) {
        _uiState.value = _uiState.value.copy(homeRoster = roster)
    }   _uiState.value = _uiState.value.copy(homeRoster = roster)
    }
    fun onAwayRosterChange(roster: List<Player>) {
        _uiState.value = _uiState.value.copy(awayRoster = roster)
    }   _uiState.value = _uiState.value.copy(awayRoster = roster)
    }
    fun onFieldNumberChange(newFieldNumber: String) {
        _uiState.value = _uiState.value.copy(fieldNumber = newFieldNumber)
    }   _uiState.value = _uiState.value.copy(fieldNumber = newFieldNumber)
    }
    fun onRefereeAssignmentChange(assignment: String) {
        _uiState.value = _uiState.value.copy(refereeAssignment = assignment)
    }   _uiState.value = _uiState.value.copy(refereeAssignment = assignment)
    }
    fun onVenueChange(venue: String) {
        _uiState.value = _uiState.value.copy(venue = venue)
    }   _uiState.value = _uiState.value.copy(venue = venue)
    }
    fun onCompetitionChange(newCompetition: String) {
        _uiState.value = _uiState.value.copy(competition = newCompetition)
    }   _uiState.value = _uiState.value.copy(competition = newCompetition)
    }
    fun onGameDateTimeChange(epochMillis: Long?) {
        _uiState.value = _uiState.value.copy(gameDateTimeEpochMillis = epochMillis)
    }   _uiState.value = _uiState.value.copy(gameDateTimeEpochMillis = epochMillis)
    }
    fun onHalfDurationChange(minutes: String) {
        _uiState.value = _uiState.value.copy(halfDurationMinutes = minutes.toIntOrNull() ?: 45)
    }   _uiState.value = _uiState.value.copy(halfDurationMinutes = minutes.toIntOrNull() ?: 45)
    }
    fun onHalftimeDurationChange(minutes: String) {
        _uiState.value =onChange(minutes: String) {
            _uiState.value.copy(halftimeDurationMinutes = minutes.toIntOrNull() ?: 0)
    }       _uiState.value.copy(halftimeDurationMinutes = minutes.toIntOrNull() ?: 0)
    }
    fun onExtraTimeHalfDurationChange(minutes: String) {
        _uiState.value =urationChange(minutes: String) {
            _uiState.value.copy(extraTimeHalfDurationMinutes = minutes.toIntOrNull() ?: 15)
    }       _uiState.value.copy(extraTimeHalfDurationMinutes = minutes.toIntOrNull() ?: 15)
    }
    fun onMaxSubstitutionsChange(max: String) {
        _uiState.value =nsChange(max: String) {
            _uiState.value.copy(maxSubstitutionsAllowed = max.toIntOrNull()?.coerceIn(0, 20) ?: 5)
    }       _uiState.value.copy(maxSubstitutionsAllowed = max.toIntOrNull()?.coerceIn(0, 20) ?: 5)
    }
    fun onHomeColorSelected(color: Color) {
        _uiState.value = _uiState.value.copy(homeTeamColorArgb = color.toArgb())
    }   _uiState.value = _uiState.value.copy(homeTeamColorArgb = color.toArgb())
    }
    fun onAwayColorSelected(color: Color) {
        _uiState.value = _uiState.value.copy(awayTeamColorArgb = color.toArgb())
    }   _uiState.value = _uiState.value.copy(awayTeamColorArgb = color.toArgb())
    }
    fun onKickOffTeamSelected(team: Team) {
        _uiState.value = _uiState.value.copy(kickOffTeam = team)
    }   _uiState.value = _uiState.value.copy(kickOffTeam = team)
    }
    fun onNotesChanged(newNotes: String) {
        _uiState.value = _uiState.value.copy(notes = newNotes)
    }   _uiState.value = _uiState.value.copy(notes = newNotes)
    }
    fun onHomeScoreChange(score: String) {
        _uiState.value = _uiState.value.copy(homeScore = score)
    }   _uiState.value = _uiState.value.copy(homeScore = score)
    }
    fun onAwayScoreChange(score: String) {
        _uiState.value = _uiState.value.copy(awayScore = score)
    }   _uiState.value = _uiState.value.copy(awayScore = score)
    }
    fun addHomePlayer(name: String, number: String) {
        val num = number.toIntOrNull() ?: returnng) {
        val newPlayer = Player(name, num, false)
        _uiState.value = _uiState.value.copy(homeRoster = _uiState.value.homeRoster + newPlayer)
    }   _uiState.value = _uiState.value.copy(homeRoster = _uiState.value.homeRoster + newPlayer)
    }
    fun removeHomePlayer(player: Player) {
        _uiState.value = _uiState.value.copy(homeRoster = _uiState.value.homeRoster - player)
    }   _uiState.value = _uiState.value.copy(homeRoster = _uiState.value.homeRoster - player)
    }
    fun addAwayPlayer(name: String, number: String) {
        val num = number.toIntOrNull() ?: returnng) {
        val newPlayer = Player(name, num, false)
        _uiState.value = _uiState.value.copy(awayRoster = _uiState.value.awayRoster + newPlayer)
    }   _uiState.value = _uiState.value.copy(awayRoster = _uiState.value.awayRoster + newPlayer)
    }
    fun removeAwayPlayer(player: Player) {
        _uiState.value = _uiState.value.copy(awayRoster = _uiState.value.awayRoster - player)
    }   _uiState.value = _uiState.value.copy(awayRoster = _uiState.value.awayRoster - player)
    }

    /**
     * Validates the current UI state and constructs a Game object,
     * then passes it to the onGameSaved callback.ts a Game object,
     */then passes it to the onGameSaved callback.
    fun onSaveGame() {
        val currentState = _uiState.value
        viewModelScope.launch {tate.value
            val currentUserId = authRepository.getCurrentUserId()
            val currentUserId = authRepository.getCurrentUserId()
            if (currentUserId == null) {
                Log.e(tag, "Cannot save: User not authenticated.")
                _uiState.update { it.copy(errorMessage = "Cannot save: User not authenticated.") }
                return@launchte { it.copy(errorMessage = "Cannot save: User not authenticated.") }
            }   return@launch
            }
            _uiState.update { it.copy(errorMessage = null) }
            _uiState.update { it.copy(errorMessage = null) }
            val gameToSave: Game
            val gameToSave: Game
            if (currentState.gameId != null && currentState.isEditing) {
                // --- EDITING AN EXISTING GAME ---entState.isEditing) {
                Log.d(tag, "Attempting to save changes to existing game: ${currentState.gameId}")
                val existingGame = gameRepository.getGamesFlow(currentUserId)rrentState.gameId}")
                    .firstOrNull() gameRepository.getGamesFlow(currentUserId)
                    ?.find { it.id == currentState.gameId }
                    ?.find { it.id == currentState.gameId }
                if (existingGame == null) {
                    Log.e(ngGame == null) {
                        tag,
                        "Failed to fetch existing game (${currentState.gameId}) for update. Cannot save."
                    )   "Failed to fetch existing game (${currentState.gameId}) for update. Cannot save."
                    _uiState.update { it.copy(errorMessage = "Error: Original game not found. Cannot save.") }
                    return@launchte { it.copy(errorMessage = "Error: Original game not found. Cannot save.") }
                }   return@launch
                }
                gameToSave = existingGame.copy(
                    gameNumber = currentState.gameNumber,
                    homeTeamName = currentState.homeTeamName,
                    awayTeamName = currentState.awayTeamName,
                    homeTeamAbbr = currentState.homeTeamAbbr.ifBlank { null },
                    awayTeamAbbr = currentState.awayTeamAbbr.ifBlank { null },
                    homeCaptainNumber = currentState.homeCaptainNumber.toIntOrNull(),
                    awayCaptainNumber = currentState.awayCaptainNumber.toIntOrNull(),
                    homeRoster = currentState.homeRoster,CaptainNumber.toIntOrNull(),
                    awayRoster = currentState.awayRoster,
                    fieldNumber = currentState.fieldNumber.takeIf { it.isNotBlank() },
                    refereeAssignment = currentState.refereeAssignment.takeIf { it.isNotBlank() },
                    venue = currentState.venue.takeIf { it.isNotBlank() },eIf { it.isNotBlank() },
                    competition = currentState.competition.takeIf { it.isNotBlank() },
                    gameDateTimeEpochMillis = currentState.gameDateTimeEpochMillis, },
                    halfDurationMinutes = currentState.halfDurationMinutes,hMillis,
                    halftimeDurationMinutes = currentState.halftimeDurationMinutes,
                    extraTimeHalfDurationMinutes = currentState.extraTimeHalfDurationMinutes,
                    maxSubstitutionsAllowed = currentState.maxSubstitutionsAllowed,onMinutes,
                    homeTeamColorArgb = currentState.homeTeamColorArgb,ionsAllowed,
                    awayTeamColorArgb = currentState.awayTeamColorArgb,
                    kickOffTeam = currentState.kickOffTeam,amColorArgb,
                    notes = currentState.notes.takeIf { it.isNotBlank() },
                    ageGroup = currentState.ageGroup, { it.isNotBlank() },
                    homeScore = currentState.homeScore.toIntOrNull() ?: existingGame.homeScore,
                    awayScore = currentState.awayScore.toIntOrNull() ?: existingGame.awayScore,
                    lastUpdated = System.currentTimeMillis()OrNull() ?: existingGame.awayScore,
                )   lastUpdated = System.currentTimeMillis()
                Log.d(tag, "Updated game object prepared: $gameToSave")
                Log.d(tag, "Updated game object prepared: $gameToSave")
            } else {
                // --- CREATING A NEW GAME ---
                Log.d(tag, "Attempting to save a new game.")
                val newGameId = UUID.randomUUID().toString()
                gameToSave = Game(ID.randomUUID().toString()
                    id = newGameId,
                    gameNumber = currentState.gameNumber,
                    homeTeamName = currentState.homeTeamName,
                    awayTeamName = currentState.awayTeamName,
                    homeTeamAbbr = currentState.homeTeamAbbr.ifBlank { null },
                    awayTeamAbbr = currentState.awayTeamAbbr.ifBlank { null },
                    homeCaptainNumber = currentState.homeCaptainNumber.toIntOrNull(),
                    awayCaptainNumber = currentState.awayCaptainNumber.toIntOrNull(),
                    homeRoster = currentState.homeRoster,CaptainNumber.toIntOrNull(),
                    awayRoster = currentState.awayRoster,
                    fieldNumber = currentState.fieldNumber.takeIf { it.isNotBlank() },
                    refereeAssignment = currentState.refereeAssignment.takeIf { it.isNotBlank() },
                    venue = currentState.venue.takeIf { it.isNotBlank() },eIf { it.isNotBlank() },
                    competition = currentState.competition.takeIf { it.isNotBlank() },
                    gameDateTimeEpochMillis = currentState.gameDateTimeEpochMillis, },
                    halfDurationMinutes = currentState.halfDurationMinutes,hMillis,
                    halftimeDurationMinutes = currentState.halftimeDurationMinutes,
                    extraTimeHalfDurationMinutes = currentState.extraTimeHalfDurationMinutes,
                    maxSubstitutionsAllowed = currentState.maxSubstitutionsAllowed,onMinutes,
                    homeTeamColorArgb = currentState.homeTeamColorArgb,ionsAllowed,
                    awayTeamColorArgb = currentState.awayTeamColorArgb,
                    kickOffTeam = currentState.kickOffTeam,amColorArgb,
                    notes = currentState.notes.takeIf { it.isNotBlank() },
                    ageGroup = currentState.ageGroup, { it.isNotBlank() },
                    homeScore = currentState.homeScore.toIntOrNull() ?: 0,
                    awayScore = currentState.awayScore.toIntOrNull() ?: 0,
                    lastUpdated = System.currentTimeMillis()OrNull() ?: 0,
                )   lastUpdated = System.currentTimeMillis()
                Log.d(tag, "New game object prepared: $gameToSave")
            }   Log.d(tag, "New game object prepared: $gameToSave")
            }
            val result = gameRepository.addOrUpdateGame(currentUserId, gameToSave)
            if (result.isSuccess) {tory.addOrUpdateGame(currentUserId, gameToSave)
                Log.i(.isSuccess) {
                    tag,
                    "Game saved/updated successfully for user $currentUserId. Game ID: ${gameToSave.id}"
                )   "Game saved/updated successfully for user $currentUserId. Game ID: ${gameToSave.id}"
            } else {
                Log.e(
                    tag,
                    "Failed to save game for user $currentUserId: ${result.exceptionOrNull()?.message}"
                )   "Failed to save game for user $currentUserId: ${result.exceptionOrNull()?.message}"
                _uiState.update {
                    it.copy(errorMessage = "Failed to save game: ${result.exceptionOrNull()?.localizedMessage}")
                }   it.copy(errorMessage = "Failed to save game: ${result.exceptionOrNull()?.localizedMessage}")
            }   }
        }   }
    }   }
   }}
    fun onAddHomePlayer: (String, String) -> Unit,}
    fun onAddHomePlayer: (String, String) -> Unit,




}    onRemoveAwayPlayer: (Player) -> Unit,    onAddAwayPlayer: (String, String) -> Unit,    onRemoveHomePlayer: (Player) -> Unit,
}    onRemoveAwayPlayer: (Player) -> Unit,    onAddAwayPlayer: (String, String) -> Unit,    onRemoveHomePlayer: (Player) -> Unit,



}    onRemoveAwayPlayer: (Player) -> Unit,    onAddAwayPlayer: (String, String) -> Unit,    onRemoveHomePlayer: (Player) -> Unit,