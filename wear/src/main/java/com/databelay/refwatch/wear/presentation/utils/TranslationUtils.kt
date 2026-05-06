package com.databelay.refwatch.wear.presentation.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.databelay.refwatch.common.GamePhase
import com.databelay.refwatch.common.GameEvent
import com.databelay.refwatch.common.CardIssuedEvent
import com.databelay.refwatch.common.GoalScoredEvent
import com.databelay.refwatch.common.PenaltyEvent
import com.databelay.refwatch.common.GenericLogEvent
import com.databelay.refwatch.common.CardType
import com.databelay.refwatch.common.GoalType
import com.databelay.refwatch.common.Team
import com.databelay.refwatch.common.formatTime
import com.databelay.refwatch.R

@Composable
fun GamePhase.localizedName(): String {
    val resId = when (this) {
        GamePhase.NOT_STARTED -> R.string.phase_not_started
        GamePhase.PRE_GAME -> R.string.phase_pre_game
        GamePhase.KICK_OFF_SELECTION_FIRST_HALF,
        GamePhase.KICK_OFF_SELECTION_EXTRA_TIME,
        GamePhase.KICK_OFF_SELECTION_PENALTIES -> R.string.phase_kick_off_selection
        GamePhase.FIRST_HALF -> R.string.phase_first_half
        GamePhase.HALF_TIME -> R.string.phase_half_time
        GamePhase.SECOND_HALF -> R.string.phase_second_half
        GamePhase.EXTRA_TIME_FIRST_HALF -> R.string.phase_extra_time_first_half
        GamePhase.EXTRA_TIME_HALF_TIME -> R.string.phase_extra_time_half_time
        GamePhase.EXTRA_TIME_SECOND_HALF -> R.string.phase_extra_time_second_half
        GamePhase.PENALTIES -> R.string.phase_penalties
        GamePhase.GAME_ENDED -> R.string.phase_game_ended
    }
    return stringResource(resId)
}

@Composable
fun CardType.localizedName(): String {
    val resId = when (this) {
        CardType.YELLOW -> R.string.card_yellow
        CardType.RED -> R.string.card_red
    }
    return stringResource(resId)
}

@Composable
fun GoalType.localizedName(): String {
    val resId = when (this) {
        GoalType.REGULAR -> R.string.goal_regular
        GoalType.PENALTY -> R.string.goal_penalty
        GoalType.OWN_GOAL -> R.string.goal_own_goal
    }
    return stringResource(resId)
}

@Composable
fun Team.localizedName(): String {
    val resId = when (this) {
        Team.HOME -> R.string.home
        Team.AWAY -> R.string.away
    }
    return stringResource(resId)
}

@Composable
fun GameEvent.localizedDisplayString(): String {
    return when (this) {
        is GoalScoredEvent -> {
            stringResource(
                R.string.goal_event_template,
                goalType.localizedName(),
                team.localizedName(),
                homeScoreAtTime,
                awayScoreAtTime,
                gameTimeMillis.toLong().formatTime()
            )
        }
        is PenaltyEvent -> {
            stringResource(
                R.string.penalty_event_template,
                team.localizedName(),
                if (scored) stringResource(R.string.scored) else stringResource(R.string.missed_saved)
            )
        }
        is CardIssuedEvent -> {
            stringResource(
                R.string.card_event_template,
                cardType.localizedName(),
                team.localizedName(),
                playerNumber,
                gameTimeMillis.toLong().formatTime()
            )
        }
        is GenericLogEvent -> message
        else -> displayString // Fallback for phase changes etc.
    }
}
