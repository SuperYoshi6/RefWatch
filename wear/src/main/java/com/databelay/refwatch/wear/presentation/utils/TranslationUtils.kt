package com.databelay.refwatch.wear.presentation.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.databelay.refwatch.common.GamePhase
import com.databelay.refwatch.common.GameEvent
import com.databelay.refwatch.common.CardIssuedEvent
import com.databelay.refwatch.common.GoalScoredEvent
import com.databelay.refwatch.common.PenaltyEvent
import com.databelay.refwatch.common.GenericLogEvent
import com.databelay.refwatch.common.PhaseChangedEvent
import com.databelay.refwatch.common.SubstitutionEvent
import com.databelay.refwatch.common.CardType
import com.databelay.refwatch.common.GoalType
import com.databelay.refwatch.common.Team
import com.databelay.refwatch.common.formatTime
import com.databelay.refwatch.R
import com.databelay.refwatch.common.shouldBeLogged

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
        GamePhase.ABORTED -> R.string.phase_aborted
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
        GoalType.OPEN_PLAY -> R.string.goal_open_play
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
    if (phase?.shouldBeLogged() == false) {
        return ""
    }
    return when (this) {
        is GoalScoredEvent -> {
            val teamLabel = teamDisplayName?.takeIf { it.isNotBlank() } ?: team.localizedName()
            val scorerStr = playerNumber?.toString() ?: "--"
            val assistStr = if (assistantNumber != null) " (Assist: #$assistantNumber)" else ""
            
            "$teamLabel: #$scorerStr$assistStr [$homeScoreAtTime:$awayScoreAtTime]"
        }
        is PenaltyEvent -> {
            val teamLabel = team.localizedName()
            val outcome = if (scored) "GETROFFEN" else "VERGEBEN"
            val kicker = if (kickerNumber != null) " #$kickerNumber" else ""
            "Elfer $teamLabel$kicker: $outcome"
        }
        is CardIssuedEvent -> {
            if (isOfficial) {
                val type = cardType.localizedName()
                val teamLabel = team.localizedName()
                "$type (${officialName ?: "Offizieller"} $teamLabel)"
            } else {
                stringResource(
                    R.string.card_event_template,
                    cardType.localizedName(),
                    team.localizedName(),
                    playerNumber,
                    gameTimeMillis.toLong().formatTime()
                )
            }
        }
        is GenericLogEvent -> message
        is PhaseChangedEvent -> newPhase.localizedName()
        is SubstitutionEvent -> {
            val name = teamDisplayName?.takeIf { it.isNotBlank() } ?: team.localizedName()
            "$name: #$outgoingPlayerNumber ➡️ #$incomingPlayerNumber"
        }
        else -> displayString
    }
}
