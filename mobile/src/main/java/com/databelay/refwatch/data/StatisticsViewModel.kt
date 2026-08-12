package com.databelay.refwatch.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.databelay.refwatch.auth.AuthRepository
import com.databelay.refwatch.common.CardIssuedEvent
import com.databelay.refwatch.common.CardType
import com.databelay.refwatch.common.GameStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StatisticsUiState(
    val totalGames: Int = 0,
    val totalGoals: Int = 0,
    val totalYellowCards: Int = 0,
    val totalRedCards: Int = 0,
    val isLoading: Boolean = true
)

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val gameRepository: GameStorageMobile
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    init {
        loadStatistics()
    }

    private fun loadStatistics() {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId() ?: return@launch
            gameRepository.getGamesFlow(userId).collectLatest { games ->
                val completedGames = games.filter { it.status == GameStatus.COMPLETED }
                
                var goals = 0
                var yellows = 0
                var reds = 0
                
                completedGames.forEach { game ->
                    goals += game.homeScore + game.awayScore
                    game.events.filterIsInstance<CardIssuedEvent>().forEach { event ->
                        when (event.cardType) {
                            CardType.YELLOW -> yellows++
                            CardType.RED -> reds++
                        }
                    }
                }
                
                _uiState.value = StatisticsUiState(
                    totalGames = completedGames.size,
                    totalGoals = goals,
                    totalYellowCards = yellows,
                    totalRedCards = reds,
                    isLoading = false
                )
            }
        }
    }
}
