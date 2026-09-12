package com.databelay.refwatch.wear.presentation.screens

import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import com.databelay.refwatch.common.formattedGameDateTime
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Event
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.ChipDefaults.chipColors
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.ToggleButton
import androidx.wear.compose.material.ToggleButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.ScrollIndicator
import androidx.wear.compose.material3.Text
import androidx.compose.ui.res.stringResource
import com.databelay.refwatch.R
import androidx.wear.tooling.preview.devices.WearDevices
import androidx.wear.compose.ui.tooling.preview.WearPreviewFontScales
import com.databelay.refwatch.common.theme.RefWatchWearTheme
import com.databelay.refwatch.common.Game
import com.databelay.refwatch.common.GamePhase
import com.databelay.refwatch.common.GameStatus
import com.databelay.refwatch.common.PreviewTools.createSampleGames
import com.databelay.refwatch.common.getAppVersionName

// Assuming GameStatus.SCHEDULED and GameStatus.COMPLETED
enum class GameListFilterState { UPCOMING, PAST }

@Composable
fun StatusHeader(
    isOnline: Boolean,
    userId: String?,
    onPairClick: () -> Unit,
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable { if (userId == null) onLoginClick() },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = null,
                tint = if (userId != null) Color.Green else Color.Red,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = userId?.take(10)?.let { "$it..." } ?: "Anmelden",
                style = MaterialTheme.typography.labelSmall,
                color = if (userId != null) MaterialTheme.colorScheme.onSurface else Color.Yellow,
                textDecoration = if (userId == null) androidx.compose.ui.text.style.TextDecoration.Underline else null
            )
        }
    }
}

@Composable
fun CompactGameFilter(
    selectedFilter: GameListFilterState,
    onFilterSelected: (GameListFilterState) -> Unit,
    upcomingCount: Int,
    pastCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val filters = listOf(
            GameListFilterState.UPCOMING to Icons.Filled.Event,
            GameListFilterState.PAST to Icons.Filled.History
        )

        filters.forEach { (filterEnum, iconVector) ->
            val isSelected = selectedFilter == filterEnum
            val contentDescription = when (filterEnum) {
                GameListFilterState.UPCOMING -> stringResource(R.string.upcoming_games)
                GameListFilterState.PAST -> stringResource(R.string.past_games)
            }

            ToggleButton(
                checked = isSelected,
                onCheckedChange = { if (it) onFilterSelected(filterEnum) },
                modifier = Modifier.size(ToggleButtonDefaults.SmallToggleButtonSize),
                colors = ToggleButtonDefaults.toggleButtonColors(
                    checkedBackgroundColor = Color.Green.copy(alpha = .5f),
                )
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = contentDescription,
                    modifier = Modifier.size(ToggleButtonDefaults.SmallIconSize)
                )
            }
        }
    }
}

@Composable
fun GameListScreen(
    allGames: List<Game>,
    activeGame: Game?,
    isOnline: Boolean,
    onGameSelected: (Game) -> Unit,
    onViewLog: (String) -> Unit,
    onNavigateToNewGame: () -> Unit,
    onNavigateToPairing: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedFilterState by remember { mutableStateOf(GameListFilterState.UPCOMING) }
    // Reading the version name from PackageManager is IO, so we hoist it out
    // of recomposition and only read once. Previously this was a `LaunchedEffect
    // (Unit)` that logged every time and ran an extra coroutine.
    val context = LocalContext.current
    val appVersionName = remember(context) {
        runCatching { getAppVersionName(context) }.getOrDefault("?")
    }

    val (upcomingGames, pastGames) = remember(allGames) {
        val now = System.currentTimeMillis()
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        val startOfToday = calendar.timeInMillis

        val (upcoming, past) = allGames.partition { game ->
            val isEnded = game.currentPhase == GamePhase.GAME_ENDED || game.currentPhase == GamePhase.ABORTED
            val isScheduledOrInProgress = game.status == GameStatus.SCHEDULED || game.status == GameStatus.IN_PROGRESS
            
            // Filter: Only show games starting today or in progress/recently started
            val startTime = game.gameDateTimeEpochMillis ?: 0L
            val isTodayOrLater = startTime >= startOfToday
            val isRecent = startTime > (now - 3 * 3600 * 1000L)
            
            !isEnded && isScheduledOrInProgress && isTodayOrLater && isRecent
        }
        Pair(
            upcoming.sortedBy { it.gameDateTimeEpochMillis },
            past.sortedByDescending { it.gameDateTimeEpochMillis }
        )
    }
    val gamesToDisplay =
        if (selectedFilterState == GameListFilterState.UPCOMING) upcomingGames else pastGames

    val listState = rememberScalingLazyListState()

    ScreenScaffold(
        scrollIndicator = { ScrollIndicator(modifier = Modifier.align(Alignment.CenterEnd), state = listState) },
        modifier = modifier
            .fillMaxSize(),
        contentPadding = PaddingValues(2.dp),
    ) { contentPadding ->
            ScalingLazyColumn(
                state = listState, contentPadding = contentPadding,
            ) {
                item(key = "spacer_header") { Spacer(modifier = Modifier.height(32.dp)) }
                
                item(key = "status_header") {
                    StatusHeader(
                        isOnline = isOnline, 
                        userId = activeGame?.userId ?: allGames.firstOrNull()?.userId,
                        onPairClick = onNavigateToPairing,
                        onLoginClick = onNavigateToLogin
                    )
                }

                if (selectedFilterState == GameListFilterState.UPCOMING) {
                    item(key = "new_game_chip") {
                        Chip(
                            onClick = onNavigateToNewGame,
                            label = {
                                Text(
                                    stringResource(R.string.new_game),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            },
                            icon = {
                                Icon(
                                    Icons.Filled.Add,
                                    contentDescription = stringResource(R.string.new_game)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth(0.9f),
                            colors = chipColors(
                                backgroundColor = MaterialTheme.colorScheme.secondaryContainer.copy(
                                    alpha = 0.4f
                                )
                            )
                        )
                    }
                }

                items(items = gamesToDisplay, key = { game -> game.id }) { game ->
                    ScheduledGameItem(
                        game = game,
                        onClick = {
                            if (game.status == GameStatus.SCHEDULED || game.status == GameStatus.IN_PROGRESS) {
                                onGameSelected(game)
                            } else {
                                onViewLog(game.id)
                            }
                        }
                    )
                }
                item(key = "version_footer") {
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Chip(
                            onClick = onLogout,
                            label = { Text(stringResource(R.string.logout), style = MaterialTheme.typography.labelSmall) },
                            icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null) },
                            colors = chipColors(backgroundColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth(0.8f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.version_label, appVersionName),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                    }
                }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
        ) {
            CompactGameFilter(
                selectedFilter = selectedFilterState,
                onFilterSelected = { newFilter -> selectedFilterState = newFilter },
                upcomingCount = upcomingGames.size,
                pastCount = pastGames.size,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun ScheduledGameItem(game: Game, onClick: () -> Unit) {
    Chip(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(0.95f),
        colors = chipColors(
            backgroundColor = MaterialTheme.colorScheme.primary,
        ),
        label = {
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = "${game.homeTeamName} - ${game.awayTeamName}",
                    maxLines = 2,
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelMedium
                )
                if (game.status == GameStatus.COMPLETED) {
                    val statusText = if (game.currentPhase == GamePhase.ABORTED) "Abgebrochen" else stringResource(R.string.final_score_chip, game.homeScore, game.awayScore)
                    Text(
                        text = statusText,
                        color = if (game.currentPhase == GamePhase.ABORTED) Color.Red else MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            if (game.needsSyncWithPhone) {
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Filled.SyncProblem,
                    contentDescription = stringResource(R.string.needs_sync),
                    modifier = Modifier.size(ChipDefaults.IconSize),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        },
        secondaryLabel = {
            Column(horizontalAlignment = Alignment.Start) {
                val dateTimeString = game.formattedGameDateTime ?: stringResource(R.string.no_time_set)
                val venueString = game.venue?.takeIf { it.isNotBlank() }
                val fieldNumberString = game.fieldNumber?.takeIf { it.isNotBlank() }

                Text(
                    text = dateTimeString,
                    color = MaterialTheme.colorScheme.onPrimary,
                )

                val locationDetails = mutableListOf<String>()
                venueString?.let { locationDetails.add(it) }
                fieldNumberString?.let { locationDetails.add(stringResource(R.string.field_label, it)) }

                if (locationDetails.isNotEmpty()) {
                    Text(
                        text = locationDetails.joinToString(" - "),
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        },
        icon = {
            game.fieldNumber?.let {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = stringResource(R.string.field_number_available),
                    modifier = Modifier.size(ChipDefaults.IconSize),
                )
            }
        }
    )
}

// --------------------------------------- Previews ----------------------------------------
// -----------------------------------------------------------------------------------------
@Preview(device = WearDevices.LARGE_ROUND, showBackground = true)
@Preview(device =  WearDevices.SMALL_ROUND, showBackground = true)
@Preview(device =  WearDevices.SQUARE, showBackground = true)
@WearPreviewFontScales

@Composable
fun GameListScreenPreview_WithScheduledGames() {
    val allGames = createSampleGames()
    RefWatchWearTheme {
        GameListScreen(
            allGames = allGames,
            activeGame = allGames.first(),
            isOnline = true,
            onGameSelected = {},
            onViewLog = {},
            onNavigateToNewGame = {},
            onNavigateToPairing = {},
            onNavigateToLogin = {}
        )
    }
}


