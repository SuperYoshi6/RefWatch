package com.databelay.refwatch.wear.presentation.screens

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.databelay.refwatch.common.theme.PredefinedJerseyColors
import com.databelay.refwatch.wear.WearGameViewModel
import com.databelay.refwatch.wear.navigation.WearNavRoutes

private const val TAG = "PreGameSetupRoute"

@Composable
fun PreGameSetupRoute(
    navController: NavController,
    gameViewModel: WearGameViewModel = hiltViewModel()
) {
    val activeGame by gameViewModel.activeGame.collectAsState()

    var showHomeTeamEditDialog by remember { mutableStateOf(false) }
    var showAwayTeamEditDialog by remember { mutableStateOf(false) }
    var showHomeColorPickerDialog by remember { mutableStateOf(false) }
    var showAwayColorPickerDialog by remember { mutableStateOf(false) }

    PreGameSetupScreen(
        game = activeGame,
        onEditHomeTeamNameClick = { showHomeTeamEditDialog = true },
        onEditAwayTeamNameClick = { showAwayTeamEditDialog = true },
        onHomeColorPickerClick = { showHomeColorPickerDialog = true },
        onAwayColorPickerClick = { showAwayColorPickerDialog = true },
        onSetHalfDuration = { duration -> gameViewModel.setHalfDuration(duration) },
        onSetHalftimeDuration = { duration -> gameViewModel.setHalftimeDuration(duration) },
        onCreateMatchClick = {
            gameViewModel.activeGame.value?.let { game ->
                gameViewModel.proceedToNextPhaseManager(game.copy())
            } ?: Log.w(TAG, "onCreateMatchClick: Cannot proceed, active game is null.")
            navController.navigate(WearNavRoutes.KICK_OFF_SELECTION_SCREEN) {
                popUpTo(WearNavRoutes.GAME_LIST_SCREEN) { inclusive = false }
                launchSingleTop = true
            }
        }
    )

    if (showHomeTeamEditDialog) {
        TeamNameEditDialog(
            teamLabel = "Home",
            initialValue = activeGame?.homeTeamName ?: "Home",
            onSave = {
                gameViewModel.updateHomeTeamName(it)
                showHomeTeamEditDialog = false
            },
            onDismiss = { showHomeTeamEditDialog = false }
        )
    }

    if (showAwayTeamEditDialog) {
        TeamNameEditDialog(
            teamLabel = "Away",
            initialValue = activeGame?.awayTeamName ?: "Away",
            onSave = {
                gameViewModel.updateAwayTeamName(it)
                showAwayTeamEditDialog = false
            },
            onDismiss = { showAwayTeamEditDialog = false }
        )
    }

    if (showHomeColorPickerDialog) {
        SimpleColorPickerDialog(
            title = "Home Color",
            availableColors = PredefinedJerseyColors,
            onColorSelected = {
                gameViewModel.updateHomeTeamColor(it)
                showHomeColorPickerDialog = false
            },
            onDismiss = { showHomeColorPickerDialog = false }
        )
    }

    if (showAwayColorPickerDialog) {
        SimpleColorPickerDialog(
            title = "Away Color",
            availableColors = PredefinedJerseyColors,
            onColorSelected = {
                gameViewModel.updateAwayTeamColor(it)
                showAwayColorPickerDialog = false
            },
            onDismiss = { showAwayColorPickerDialog = false }
        )
    }
}
