package com.databelay.refwatch.wear.presentation.screens
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.ChipDefaults.chipColors
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.ToggleButton
import androidx.wear.compose.material.ToggleButtonDefaults
import com.databelay.refwatch.BuildConfig
import com.databelay.refwatch.common.Game
import com.databelay.refwatch.common.GameStatus
import com.databelay.refwatch.common.IWearGameViewModel
import com.databelay.refwatch.common.getAppVersionCode
import com.databelay.refwatch.common.getAppVersionName
import com.databelay.refwatch.common.PreviewTools.createSampleGames
import com.databelay.refwatch.common.PreviewWearGameViewModel
import com.databelay.refwatch.wear.data.DataFetchStatus

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
    // Using Material 3 OutlinedButton for toggle effect, customize as needed
    // For Wear, you might use Chip or Button with custom styling.
    Row(
        modifier = modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Define filters with their corresponding standard icons
        val filters = listOf(
            GameListFilterState.UPCOMING to Icons.Filled.Event,   // Standard icon for scheduled events
            GameListFilterState.PAST to Icons.Filled.History      // Standard icon for history
        )

        filters.forEach { (filterEnum, iconVector) ->
            val isSelected = selectedFilter == filterEnum
            val contentDescription = when (filterEnum) {
                GameListFilterState.UPCOMING -> "Upcoming games ($upcomingCount)"
                GameListFilterState.PAST -> "Past games ($pastCount)"
            }

            ToggleButton(
                checked = isSelected,
                onCheckedChange = { if (it) onFilterSelected(filterEnum) },
                modifier = Modifier.size(ToggleButtonDefaults.SmallToggleButtonSize),
                colors = ToggleButtonDefaults.toggleButtonColors(
                    checkedBackgroundColor = Color.Green.copy(alpha = .5f),)
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
    viewModel: IWearGameViewModel,
    onGameSelected: (Game) -> Unit,
    onViewLog: (String) -> Unit,
    onNavigateToNewGame: () -> Unit,
    onNavigateToGameScreen: () -> Unit
) {
    val TAG = "GameListScreen"
    val allGames by viewModel.gamesList.collectAsStateWithLifecycle()
    LaunchedEffect(allGames) { // Or just log within the composable body (less efficient)
        Log.d(TAG, "Games list updated. Number of games: ${allGames.size}")
    }
    val isOnline by viewModel.isOnline.collectAsState()
    val activeGame by viewModel.activeGame.collectAsState()
    var selectedFilterState by remember { mutableStateOf(GameListFilterState.UPCOMING) }
    var appVersionName by remember { mutableStateOf("Loading...") } // State for version name
    var appVersionNumber by remember { mutableLongStateOf(0L) } // State for version name
    val buildDateString = BuildConfig.BUILD_TIME
    val context = LocalContext.current // Get context

    // LaunchedEffect to get version name (it's a synchronous call but good practice
    // if it were asynchronous, and keeps UI responsive during initial composition)
    LaunchedEffect(Unit) {
        appVersionName = getAppVersionName(context)
        appVersionNumber = getAppVersionCode(context)
    }

    val (upcomingGames, pastGames) = remember(allGames) {
        val (scheduled, completed) = allGames.partition { it.status == GameStatus.SCHEDULED }
        Pair(
            scheduled.sortedBy { it.gameDateTimeEpochMillis },
            completed.sortedByDescending { it.gameDateTimeEpochMillis }
        )
    }
    val gamesToDisplay = if (selectedFilterState == GameListFilterState.UPCOMING) upcomingGames else pastGames

    LaunchedEffect(Unit) {
        // viewModel.performConnectivityCheck()
    }

    Box(modifier = Modifier.fillMaxSize()) { // Use Box as the root for layering
        // Layer 1: The scrollable list
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(), // List takes the full space of the Box
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
            // Content padding might be needed at the top of the list
            // to prevent content from appearing under the filter bar initially.
            // Adjust this padding based on the actual height of your filter bar.
            // contentPadding = PaddingValues(top = 56.dp) // Example: 48dp for buttons + 8dp padding
        ) {
            // Add a spacer at the top of the list if you don't use contentPadding
            // to prevent the first item from being hidden underneath the filter.
            // The height of this spacer should be roughly the height of your CompactGameFilter.
            item { Spacer(modifier = Modifier.padding(top = 1.dp)) } // Adjust this height

            // "Resume Game" Chip - This will now scroll with the list if placed inside
            // If it should ALSO be fixed, it needs to be a separate layer in the Box too.
            // For simplicity, let's assume it can scroll OR you handle its fixed position separately.

            // "New Ad-Hoc Game" Chip
            if (selectedFilterState == GameListFilterState.UPCOMING) {
                item {
                    Chip(
                        onClick = onNavigateToNewGame,
                        label = { Text("New Game") },
                        icon = { Icon(Icons.Filled.Add, contentDescription = "Start New Ad-Hoc Game") },
                        modifier = Modifier
                            .fillMaxWidth(0.9f), // Adjust padding
                        colors = chipColors(
                            backgroundColor = MaterialTheme.colorScheme.secondary,
                            contentColor = Color.White)
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
            // Internet connectivity status
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    val emptyMessage =
                        if (isOnline) "Online." else "Not online."
//                        when (dataFetchStatus) {
//                            DataFetchStatus.INITIAL, DataFetchStatus.ERROR_PHONE_UNREACHABLE -> "Can't fetch games.\nPlease connect to RefWatch on the phone."
//                            DataFetchStatus.FETCHING -> "Loading games..."
//                            DataFetchStatus.ERROR_PARSING -> "Error: Could not read game data from phone."
//                            DataFetchStatus.ERROR_UNKNOWN -> "An error occurred while loading games."
//                            DataFetchStatus.NO_DATA_AVAILABLE -> "No games on your phone."
//                            DataFetchStatus.SUCCESS -> "Fetching successful." // Should be NO_DATA_AVAILABLE if list is empty
//                            DataFetchStatus.LOADED_FROM_CACHE -> "Loaded from cache. Connect to phone to update." // Or show cached if any
//                        }
                    Text(
                        text = emptyMessage,
                        color = MaterialTheme.colorScheme.onSecondary,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            // --- ADD BUILD INFO TEXT HERE ---
            item {
                Spacer(modifier = Modifier.height(8.dp))
                androidx.compose.material3.Text(
                    text = "Version: $appVersionName $buildDateString", // Display version name
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            // --- END BUILD INFO TEXT ---
        }

        // Layer 2: The CompactGameFilter on top
        // This Column is for potentially stacking other fixed elements like the "Resume Game" chip
        Column(modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.TopCenter)) {
            CompactGameFilter(
                selectedFilter = selectedFilterState,
                onFilterSelected = { newFilter -> selectedFilterState = newFilter },
                upcomingCount = upcomingGames.size,
                pastCount = pastGames.size,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent) // Make the Row background transparent
                    .padding(horizontal = 16.dp, vertical = 4.dp) // Keep its own padding
                // .zIndex(1f) // Usually not needed in a simple Box structure like this
            )
        }
    }
}
@Composable
fun ScheduledGameItem(game: Game, onClick: () -> Unit) {
    Chip(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(0.95f),
        colors = ChipDefaults.primaryChipColors(),
        label = {
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = "${game.homeTeamName} vs ${game.awayTeamName}",
                    maxLines = 2, // Allow two lines for long names
                    style = MaterialTheme.typography.labelMedium
                )
                // Show score for completed games
                if (game.status == GameStatus.COMPLETED) {
                    Text(
                        text = "Final: ${game.homeScore} - ${game.awayScore}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            }

            // Conditionally display the icon
            if (game.needsSyncWithPhone) {
                Spacer(Modifier.width(8.dp)) // Add some spacing before the icon
                Icon(
                    imageVector = Icons.Filled.SyncProblem, // Your chosen icon
                    contentDescription = "Needs sync with phone", // Accessibility
                    modifier = Modifier.size(ChipDefaults.IconSize), // Standard chip icon size
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) // Or a warning color
                )
            }
        },
        secondaryLabel = {
            Column(horizontalAlignment = Alignment.Start) {
                val dateTimeString = game.formattedGameDateTime ?: "No time set"
                val venueString = game.venue?.takeIf { it.isNotBlank() }
                val fieldNumberString = game.fieldNumber?.takeIf { it.isNotBlank() } // Get field number

                Text(text = dateTimeString)

                // Combine Venue and Field Number if they exist
                val locationDetails = mutableListOf<String>()
                venueString?.let { locationDetails.add(it) }
                fieldNumberString?.let { locationDetails.add("Field: $it") }

                if (locationDetails.isNotEmpty()) {
                    Text(
                        text = locationDetails.joinToString(" - "), // e.g., "Main Stadium - Field: 3"
                        maxLines = 1,
                        style = MaterialTheme.typography.labelSmall // Or MaterialTheme.typography.bodySmall for M3
                    )
                }
            }
        },
        icon = { // Example: If you want an icon specifically if field number exists
            game.fieldNumber?.let {
                Icon(
                    imageVector = Icons.Filled.LocationOn, // Example icon, choose something appropriate
                    contentDescription = "Field Number available",
                    modifier = Modifier.size(ChipDefaults.IconSize)
                )
            }
        }
    )
}


// ---------------------- PREVIEWS ---------------------------
// -----------------------------------------------------------
// Helper to create sample games for previews


@Preview(
    device = "id:wearos_large_round",
    showSystemUi = true,
    backgroundColor = 0xff000000,
    showBackground = true,
    name = "GameList - Empty Scheduled"
)
@Composable
fun GameListScreenPreview_EmptyScheduled() {
    val mockViewModel = PreviewWearGameViewModel(
        initialGames = emptyList(),
    )
    // RefWatchTheme { // Wrap in your app's theme
    GameListScreen(
        viewModel = mockViewModel,
        onGameSelected = {},
        onViewLog = {},
        onNavigateToNewGame = {},
        onNavigateToGameScreen = {}
    )
    // }
}

@Preview(
    device = "id:wearos_large_round",
    showSystemUi = true,
    backgroundColor = 0xff000000,
    showBackground = true,
    name = "GameList - Loading State"
)
@Composable
fun GameListScreenPreview_Loading() {
    val mockViewModel = PreviewWearGameViewModel(
        initialGames = emptyList(),
    )
    // RefWatchTheme {
    GameListScreen(
        viewModel = mockViewModel,
        onGameSelected = {},
        onViewLog = {},
        onNavigateToNewGame = {},
        onNavigateToGameScreen = {}
    )
    // }
}

@Preview(
    device = "id:wearos_large_round",
    showSystemUi = true,
    backgroundColor = 0xff000000,
    showBackground = true,
    name = "GameList - Error State"
)
@Composable
fun GameListScreenPreview_Error() {
    val mockViewModel = PreviewWearGameViewModel(
        initialGames = emptyList(),
    )
    // RefWatchTheme {
    GameListScreen(
        viewModel = mockViewModel,
        onGameSelected = {},
        onViewLog = {},
        onNavigateToNewGame = {},
        onNavigateToGameScreen = {}
    )
    // }
}

@Preview(
    device = "id:wearos_small_round",
    showSystemUi = true,
    backgroundColor = 0xff000000,
    showBackground = true,
    name = "GameList - With Scheduled Games"
)
@Composable
fun GameListScreenPreview_WithScheduledGames() {
    val mockViewModel =
        PreviewWearGameViewModel(initialGames = createSampleGames().filter { it.status == GameStatus.SCHEDULED })
    // RefWatchTheme {
    GameListScreen(
        viewModel = mockViewModel,
        onGameSelected = {},
        onViewLog = {},
        onNavigateToNewGame = {},
        onNavigateToGameScreen = {}
    )
    // }
}

@Preview(
    device = "id:wearos_large_round",
    showSystemUi = true,
    backgroundColor = 0xff000000,
    showBackground = true,
    name = "GameList - With Past Games Tab"
)
@Composable
fun GameListScreenPreview_WithPastGames() {
    // To see the "Past" tab selected, we'd ideally need a way to control
    // the internal 'selectedTab' state of GameListScreen from the preview,
    // or the FakeWearGameViewModel could expose a way to hint the initial tab.
    // For simplicity, this preview will show the games, but the "Scheduled" tab will be selected by default.
    // To preview the "Past" tab selected, you'd need to modify GameListScreen or its state handling for previews.
    val mockViewModel = PreviewWearGameViewModel(initialGames = createSampleGames())
    // RefWatchTheme {
    GameListScreen(
        viewModel = mockViewModel,
        onGameSelected = {},
        onViewLog = {},
        onNavigateToNewGame = {},
        onNavigateToGameScreen = {}
    )
    // }
}

@Preview(
    device = "id:wearos_large_round",
    showSystemUi = true,
    backgroundColor = 0xff000000,
    showBackground = true,
    name = "GameList - With Resumable Game"
)
@Composable
fun GameListScreenPreview_WithResumableGame() {
    val resumableGame = Game(
        id = "active123",
        homeTeamName = "Active Team A",
        awayTeamName = "Active Team B",
        isTimerRunning = true,
        displayedTimeMillis = 120000 // 2 minutes in
    )
    val mockViewModel = PreviewWearGameViewModel(
        initialGames = createSampleGames(),
        initialActiveGame = resumableGame
    )
    // RefWatchTheme {
    GameListScreen(
        viewModel = mockViewModel,
        onGameSelected = {},
        onViewLog = {},
        onNavigateToNewGame = {},
        onNavigateToGameScreen = {}
    )
    // }
}

