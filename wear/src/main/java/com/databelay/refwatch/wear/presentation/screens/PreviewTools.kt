package com.databelay.refwatch.wear.presentation.screens

import android.graphics.Color
import com.databelay.refwatch.common.Game
import com.databelay.refwatch.common.GamePhase
import com.databelay.refwatch.common.GameStatus
import com.databelay.refwatch.common.Team
import com.databelay.refwatch.wear.IWearGameViewModel
import com.databelay.refwatch.wear.data.DataFetchStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow


class PreviewWearGameViewModel (
    initialGames: List<Game> = emptyList(),
    initialFetchStatus: DataFetchStatus = DataFetchStatus.SUCCESS,
    initialActiveGame: Game? = null
) : IWearGameViewModel {
    override val gamesList: StateFlow<List<Game>> = MutableStateFlow(initialGames)
    override val dataFetchStatus: StateFlow<DataFetchStatus> = MutableStateFlow(initialFetchStatus)
    override val isOnline: StateFlow<Boolean> = MutableStateFlow(true)
    override val activeGame: StateFlow<Game> = MutableStateFlow(initialActiveGame) as StateFlow<Game>

    // Helper to update the list for preview variations
    fun setGames(games: List<Game>) {
        (this.gamesList as MutableStateFlow).value = games
    }
    fun setFetchStatus(status: DataFetchStatus) {
        (this.dataFetchStatus as MutableStateFlow).value = status
    }
    fun setActiveGame(game: Game) {
        (this.activeGame as MutableStateFlow).value = game
    }
}

object PreviewTools {
    fun createFirstHalfSampleGame(): Game {
        return Game.defaults().copy(
            id = "scheduledGame1",
            homeTeamName = "Alpha FC",
            awayTeamName = "Beta United",
            currentPhase = GamePhase.FIRST_HALF,
            gameDateTimeEpochMillis = System.currentTimeMillis() + (2 * 60 * 60 * 1000L), // 2 hours from now
            venue = "Stadium One",
        )
    }

    fun createExtraFirstHalfSampleGame(): Game {
        return Game.defaults().copy(
            id = "scheduledGame1",
            homeTeamName = "Alpha FC",
            awayTeamName = "Beta United",
            currentPhase = GamePhase.EXTRA_TIME_FIRST_HALF,
            gameDateTimeEpochMillis = System.currentTimeMillis() + (2 * 60 * 60 * 1000L), // 2 hours from now
            venue = "Stadium One",
        )
    }

    fun createPenaltiesSampleGame(): Game {
        return Game.defaults().copy(
            id = "scheduledGame1",
            homeTeamName = "Alpha FC",
            awayTeamName = "Beta United",
            currentPhase = GamePhase.PENALTIES,
            gameDateTimeEpochMillis = System.currentTimeMillis() + (2 * 60 * 60 * 1000L), // 2 hours from now
            venue = "Stadium One",
        )
    }

    fun createSampleGames(): List<Game> {
        return listOf(
            // --- Scheduled Games ---
            Game.defaults().copy(
                id = "scheduledGame1",
                homeTeamName = "Alpha FC",
                awayTeamName = "Beta United",
                status = GameStatus.SCHEDULED,
                gameDateTimeEpochMillis = System.currentTimeMillis() + (2 * 60 * 60 * 1000L), // 2 hours from now
                venue = "Stadium One",
            ),
            Game.defaults().copy(
                id = "scheduledGame2",
                homeTeamName = "Gamma Rovers",
                awayTeamName = "Delta City",
                status = GameStatus.SCHEDULED,
                homeTeamColorArgb = Color.parseColor("#3F51B5"), // Indigo
                awayTeamColorArgb = Color.parseColor("#FFC107"), // Amber
                gameDateTimeEpochMillis = System.currentTimeMillis() + (26 * 60 * 60 * 1000L), // 26 hours from now
                venue = "Community Park",
            ),

            // --- In-Progress Game Example (for potential "Resume Game" chip) ---
            Game.defaults().copy(
                id = "inProgressGame1",
                homeTeamName = "Red Warriors",
                awayTeamName = "Blue Thunder",
                status = GameStatus.IN_PROGRESS, // Or any active status
                currentPhase = GamePhase.FIRST_HALF,
                isTimerRunning = true,
                displayedTimeMillis = 15 * 60 * 1000L, // 15:00 on the clock
                actualTimeElapsedInPeriodMillis = 15 * 60 * 1000L + (30 * 1000L), // 15m 30s actual
                halfDurationMinutes = 40,
                homeScore = 1,
                awayScore = 0,
                kickOffTeam = Team.HOME,
                homeTeamColorArgb = Color.RED,
                awayTeamColorArgb = Color.BLUE,
            ),

            // --- Completed Games ---
            Game.defaults().copy(
                id = "completedGame1",
                homeTeamName = "Green Hornets",
                awayTeamName = "Purple Haze",
                status = GameStatus.COMPLETED,
                currentPhase = GamePhase.GAME_ENDED,
                isTimerRunning = false,
                gameDateTimeEpochMillis = System.currentTimeMillis() - (1 * 24 * 60 * 60 * 1000L), // 1 day ago
                homeScore = 3,
                awayScore = 2,
                homeTeamColorArgb = Color.GREEN,
                awayTeamColorArgb = Color.MAGENTA, // Using MAGENTA for Purple
                venue = "Old Trafford (simulated)",
            ),
            Game.defaults().copy(
                id = "completedGame2",
                homeTeamName = "Black Cats",
                awayTeamName = "White Knights",
                status = GameStatus.COMPLETED,
                currentPhase = GamePhase.GAME_ENDED,
                gameDateTimeEpochMillis = System.currentTimeMillis() - (5 * 24 * 60 * 60 * 1000L), // 5 days ago
                homeScore = 0,
                awayScore = 0,
                homeTeamColorArgb = Color.BLACK,
                awayTeamColorArgb = Color.WHITE,
                venue = "The Den",
            ),
            // Your example item (slightly adjusted if `halfDurationMinutes` affects display directly)
            Game.defaults().copy(
                id = "previewGameYourExample",
                status = GameStatus.COMPLETED, // Assuming if it has score and time, it's active
                currentPhase = GamePhase.GAME_ENDED,
                homeTeamName = "Red Team Example",
                awayTeamName = "Blue Team Example",
                homeTeamColorArgb = Color.BLACK, // As per your example
                awayTeamColorArgb = Color.YELLOW,
                kickOffTeam = Team.AWAY,
                actualTimeElapsedInPeriodMillis = (5 * 60000L), // 5 minutes (assuming your (5*L)+(2*L) was an example)
                displayedTimeMillis = (45 * 60000L) - (5 * 60000L), // If half is 45m, and 5m elapsed, 40m displayed (countdown)
                halfDurationMinutes = 45,
                homeScore = 2,
                isTimerRunning = true // Implied if it's FIRST_HALF with time elapsed
            )
        )
    }
}