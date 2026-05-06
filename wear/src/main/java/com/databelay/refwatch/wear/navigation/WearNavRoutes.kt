package com.databelay.refwatch.wear.navigation

import android.util.Log
import com.databelay.refwatch.common.CardType
import com.databelay.refwatch.common.Team

/**
 * A singleton object that holds all the navigation route constants and
 * helper functions for the Wear OS app, mirroring the mobile app's style.
 */
object WearNavRoutes {

    // --- Argument Keys ---
    // Define argument names as constants to avoid typos
    const val GAME_ID_ARG = "gameId"
    const val TEAM_ARG = "team"
    const val CARD_TYPE_ARG = "cardType"
    const val GOAL_TYPE_ARG = "goalType"

    // --- Route Definitions ---
    // Base routes are simple constants
    const val GAME_LIST_SCREEN = "game_list"
    const val PRE_GAME_SETUP_SCREEN = "pre_game_setup"
    const val KICK_OFF_SELECTION_SCREEN = "kick_off_selection"
    const val GAME_IN_PROGRESS_SCREEN = "game_in_progress"
    const val GAME_LOG_SCREEN = "game_log"
    const val LOG_CARD_SCREEN = "log_card"
    const val LOG_GOAL_SCREEN = "log_goal"
    const val LOG_SUBSTITUTION_SCREEN = "log_substitution"
    // --- Route Helper Functions ---

    /**
     * Creates the navigation route for the active game screen.
     * This route requires a gameId.
     */
    fun gameInProgressRoute(gameId: String): String {
        return "$GAME_IN_PROGRESS_SCREEN?$GAME_ID_ARG=$gameId"
    }

    /**
     * Creates the navigation route for the game log screen.
     * This route requires a gameId.
     */
    fun gameLogRoute(gameId: String): String {
        Log.d(TAG, "Game log route created: $GAME_LOG_SCREEN/${gameId}")
        return "$GAME_LOG_SCREEN/${gameId}"
    }

    /**
     * Creates the navigation route for the log card screen.
     * This route requires a team to be pre-selected.
     */
    fun logCardRoute(team: Team, cardType: CardType): String {
        return "$LOG_CARD_SCREEN/${team.name}/${cardType.name}" // Use path segments
    }

    fun logGoalRoute(team: Team, goalType: com.databelay.refwatch.common.GoalType): String {
        return "$LOG_GOAL_SCREEN/${team.name}/${goalType.name}"
    }

    fun logSubstitutionRoute(team: Team): String {
        return "$LOG_SUBSTITUTION_SCREEN/${team.name}"
    }

}