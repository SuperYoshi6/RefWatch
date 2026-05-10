package com.databelay.refwatch.wear.presentation.screens

import android.util.Log
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.wear.compose.material3.ConfirmationDialog
import com.databelay.refwatch.common.theme.PredefinedJerseyColors
import com.databelay.refwatch.wear.WearGameViewModel
import com.databelay.refwatch.wear.navigation.WearNavRoutes
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.databelay.refwatch.R

private const val TAG = "PreGameSetupRoute"

@Composable
fun PreGameSetupRoute(
    navController: NavController,
    gameViewModel: WearGameViewModel = hiltViewModel()
) {
    val activeGame by gameViewModel.activeGame.collectAsState()

    var showHomeTeamEditDialog by remember { mutableStateOf(false) }
    var showAwayTeamEditDialog by remember { mutableStateOf(false) }
    var showHomeTeamAbbrEditDialog by remember { mutableStateOf(false) }
    var showAwayTeamAbbrEditDialog by remember { mutableStateOf(false) }
    var showHomeCaptainEditDialog by remember { mutableStateOf(false) }
    var showAwayCaptainEditDialog by remember { mutableStateOf(false) }
    var showHomeColorPickerDialog by remember { mutableStateOf(false) }
    var showAwayColorPickerDialog by remember { mutableStateOf(false) }

    PreGameSetupScreen(
        game = activeGame,
        onEditHomeTeamNameClick = { showHomeTeamEditDialog = true },
        onEditAwayTeamNameClick = { showAwayTeamEditDialog = true },
        onEditHomeTeamAbbrClick = { showHomeTeamAbbrEditDialog = true },
        onEditAwayTeamAbbrClick = { showAwayTeamAbbrEditDialog = true },
        onEditHomeCaptainClick = { showHomeCaptainEditDialog = true },
        onEditAwayCaptainClick = { showAwayCaptainEditDialog = true },
        onHomeColorPickerClick = { showHomeColorPickerDialog = true },
        onAwayColorPickerClick = { showAwayColorPickerDialog = true },
        onSetHalfDuration = { duration -> gameViewModel.setHalfDuration(duration) },
        onSetHalftimeDuration = { duration -> gameViewModel.setHalftimeDuration(duration) },
        onSetExtraTimeDuration = { duration -> gameViewModel.setExtraTimeDuration(duration) },
        onSetMaxSubstitutions = { max -> gameViewModel.updateMaxSubstitutions(max) },
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
            teamLabel = stringResource(R.string.edit_team_name) + " (${stringResource(R.string.home)})",
            initialValue = activeGame?.homeTeamName ?: "",
            onSave = {
                gameViewModel.updateHomeTeamName(it)
                showHomeTeamEditDialog = false
            },
            onDismiss = { showHomeTeamEditDialog = false }
        )
    }

    if (showAwayTeamEditDialog) {
        TeamNameEditDialog(
            teamLabel = stringResource(R.string.edit_team_name) + " (${stringResource(R.string.away)})",
            initialValue = activeGame?.awayTeamName ?: "",
            onSave = {
                gameViewModel.updateAwayTeamName(it)
                showAwayTeamEditDialog = false
            },
            onDismiss = { showAwayTeamEditDialog = false }
        )
    }

    if (showHomeTeamAbbrEditDialog) {
        TeamNameEditDialog(
            teamLabel = stringResource(R.string.edit_team_abbr) + " (${stringResource(R.string.home)})",
            initialValue = activeGame?.homeTeamAbbr ?: "",
            onSave = {
                gameViewModel.updateHomeTeamAbbr(it)
                showHomeTeamAbbrEditDialog = false
            },
            onDismiss = { showHomeTeamAbbrEditDialog = false }
        )
    }

    if (showAwayTeamAbbrEditDialog) {
        TeamNameEditDialog(
            teamLabel = stringResource(R.string.edit_team_abbr) + " (${stringResource(R.string.away)})",
            initialValue = activeGame?.awayTeamAbbr ?: "",
            onSave = {
                gameViewModel.updateAwayTeamAbbr(it)
                showAwayTeamAbbrEditDialog = false
            },
            onDismiss = { showAwayTeamAbbrEditDialog = false }
        )
    }

    if (showHomeCaptainEditDialog) {
        NumberEditDialog(
            label = stringResource(R.string.edit_home_captain),
            initialValue = activeGame?.homeCaptainNumber?.toString() ?: "",
            onSave = {
                gameViewModel.updateHomeCaptainNumber(it)
                showHomeCaptainEditDialog = false
            },
            onDismiss = { showHomeCaptainEditDialog = false }
        )
    }

    if (showAwayCaptainEditDialog) {
        NumberEditDialog(
            label = stringResource(R.string.edit_away_captain),
            initialValue = activeGame?.awayCaptainNumber?.toString() ?: "",
            onSave = {
                gameViewModel.updateAwayCaptainNumber(it)
                showAwayCaptainEditDialog = false
            },
            onDismiss = { showAwayCaptainEditDialog = false }
        )
    }

    if (showHomeColorPickerDialog) {
        SimpleColorPickerDialog(
            title = stringResource(R.string.team_color),
            availableColors = PredefinedJerseyColors,
            selectedColor = activeGame?.homeTeamColor ?: Color.Gray,
            onColorSelected = { color ->
                gameViewModel.updateHomeTeamColor(color)
                showHomeColorPickerDialog = false
            },
            onDismiss = { showHomeColorPickerDialog = false }
        )
    }

    if (showAwayColorPickerDialog) {
        SimpleColorPickerDialog(
            title = stringResource(R.string.team_color),
            availableColors = PredefinedJerseyColors,
            selectedColor = activeGame?.awayTeamColor ?: Color.LightGray,
            onColorSelected = { color ->
                gameViewModel.updateAwayTeamColor(color)
                showAwayColorPickerDialog = false
            },
            onDismiss = { showAwayColorPickerDialog = false }
        )
    }
}
