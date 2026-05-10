package com.databelay.refwatch.navigation


// Define your navigation routes if you haven't already
object MobileNavRoutes {
    const val LOADING_SCREEN = "loading"
    const val AUTH_SCREEN = "auth"
    const val GAME_LIST_SCREEN = "game_list"
    const val ADD_EDIT_GAME_SCREEN = "add_edit_game_screen" // For navigating to Add/Edit
    const val SETTINGS_SCREEN = "mobile_settings"
    const val GAME_LOG_SCREEN = "game_log"
    const val MATCH_SCREEN = "match_screen"
    fun addEditGameRoute(gameId: String? = null): String {
        return if (gameId != null) "$ADD_EDIT_GAME_SCREEN?gameId=$gameId" else ADD_EDIT_GAME_SCREEN
    }

    fun gameLogRoute(gameId: String? = null): String {
        return if (gameId != null) "$GAME_LOG_SCREEN?gameId=$gameId" else GAME_LOG_SCREEN
    }

    fun matchRoute(gameId: String): String {
        return "$MATCH_SCREEN?gameId=$gameId"
    }
}