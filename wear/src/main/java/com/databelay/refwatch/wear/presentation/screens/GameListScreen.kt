package com.databelay.refwatch.wear.presentation.screens

import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.foundation.background
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
import androidx.compose.runtime.LaunchedEffect
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
import android.util.Log
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.ChipDefaults.chipColors
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
import com.databelay.refwatch.common.GameStatus
import com.databelay.refwatch.common.PreviewTools.createSampleGames
import com.databelay.refwatch.common.getAppVersionName

// Assuming GameStatus.SCHEDULED and GameStatus.COMPLETED
enum class GameListFilterState { UPCOMING, PAST }

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
                GameListFilterState.UPCOMING -> stringResource(R.string.upcoming_games) + " ($upcomingCount)"
                GameListFilterState.PAST -> stringResource(R.string.past_games) + " ($pastCount)"
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
    modifier: Modifier = Modifier
) {
    val tag = "GameListScreen"
    LaunchedEffect(allGames) {
        Log.d(tag, "Games list updated. Number of games: ${allGames.size}")
    }
    var selectedFilterState by remember { mutableStateOf(GameListFilterState.UPCOMING) }
    var appVersionName by remember { mutableStateOf("Loading...") }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        appVersionName = getAppVersionName(context)
    }

    val (upcomingGames, pastGames) = remember(allGames) {
        val (scheduled, completed) = allGames.partition { it.status == GameStatus.SCHEDULED }
        Pair(
            scheduled.sortedBy { it.gameDateTimeEpochMillis },
            completed.sortedByDescending { it.gameDateTimeEpochMillis }
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
//            modifier = Modifier.fillMaxSize(),
//            horizontalAlignment = Alignment.CenterHorizontally,
//            verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { Spacer(modifier = Modifier.padding(top = 0.dp)) }

                if (selectedFilterState == GameListFilterState.UPCOMING) {
                    item {
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
                            if (game.status == GameStatus.SCHEDULED) {
                                onGameSelected(game)
                            } else {
                                onViewLog(game.id)
                            }
                        }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        val emptyMessage =
                            if (isOnline) stringResource(R.string.online) else stringResource(R.string.not_online)
                        Text(
                            text = emptyMessage,
                            color = MaterialTheme.colorScheme.onSecondary,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Version: $appVersionName",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
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
                    .background(Color.Transparent)
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
                    text = "${game.homeTeamName} vs ${game.awayTeamName}",
                    maxLines = 2,
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelMedium
                )
                if (game.status == GameStatus.COMPLETED) {
                    Text(
                        text = "Final: ${game.homeScore} - ${game.awayScore}",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelMedium,
//                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            }

            if (game.needsSyncWithPhone) {
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Filled.SyncProblem,
                    contentDescription = "Needs sync with phone",
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
                    contentDescription = "Field Number available",
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
            onNavigateToNewGame = {}
        )
    }
}


