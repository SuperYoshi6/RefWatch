package com.databelay.refwatch.games

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.databelay.refwatch.BuildConfig
import com.databelay.refwatch.auth.AuthState
import com.databelay.refwatch.common.Game
import com.databelay.refwatch.common.GameStatus
import com.databelay.refwatch.common.PreviewTools.createSampleGames
import com.databelay.refwatch.common.getAppVersionCode
import com.databelay.refwatch.common.getAppVersionName
import com.databelay.refwatch.common.theme.RefWatchMobileTheme
import com.google.firebase.auth.FirebaseUser
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameListScreen(
    authStateValue: AuthState, // << Pass AuthState directly
    games: List<Game>, // << Pass the list of games to display directly
    selectedTab: GameStatus,    // << Pass current tab
    onTabSelected: (GameStatus) -> Unit, // << Callback to change tab
    onAddGame: () -> Unit,
    onEditGame: (Game) -> Unit,
    onViewLog: (Game) -> Unit, // <-- Ensure this is passed
    onDeleteGame: (Game) -> Unit,
    onSignOut: () -> Unit, // This might be handled by the calling composable via AuthViewModel
    onImportGames: () -> Unit, // Callback for importing
    onNavigateToSettings: () -> Unit
) {
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

    Log.d("GameListScreen", "Received games: ${games.map { it.id + " -> " + it.status }}") // Log input games

    // Filter and sort the lists, just like on the watch
    val (upcomingGames, pastGames) = remember(games) {
        val (scheduled, completed) = games.partition { it.status == GameStatus.SCHEDULED }
        Pair(
            scheduled.sortedBy { it.gameDateTimeEpochMillis },
            completed.sortedByDescending { it.gameDateTimeEpochMillis }
        )
    }
    val gamesToDisplay = if (selectedTab == GameStatus.SCHEDULED) upcomingGames else pastGames

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Games") },
                actions = {
                    // Conditionally display Settings Button based on AuthState
                    if (authStateValue is AuthState.Authenticated) {
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = "Settings"
                            )
                        }
                    }
                    IconButton(onClick = onImportGames) {
                        Icon(Icons.Default.UploadFile, contentDescription = "Import ICS")
                    }
                    IconButton(onClick = onSignOut) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Sign Out")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddGame) {
                Icon(Icons.Filled.Add, "Add Game")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            // --- TABS FOR FILTERING ---
            TabRow(selectedTabIndex = if (selectedTab == GameStatus.SCHEDULED) 0 else 1) { // Determine index based on selectedTab
                Tab(
                    selected = selectedTab == GameStatus.SCHEDULED,
                    onClick = { onTabSelected(GameStatus.SCHEDULED) },
                    text = { Text("Upcoming (${upcomingGames.size})") }
                )
                Tab(
                    selected = selectedTab == GameStatus.COMPLETED,
                    onClick = { onTabSelected(GameStatus.COMPLETED) },
                    text = { Text("Past (${pastGames.size})") }
                )
            }

            // --- GAME LIST or EMPTY MESSAGE ---
            if (gamesToDisplay.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No games scheduled. Add one or import ICS.")
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(gamesToDisplay , key = { it.id }) { game ->
                        GameListItem(
                            game = game,
                            onEditGame = onEditGame,
                            onViewLog = onViewLog, // <-- Pass it down to the item
                            onDelete = { onDeleteGame(game) }
                        )
                    }
                }
            }
            // --- ADD BUILD INFO TEXT HERE ---
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Version: $appVersionName $buildDateString", // Display version name
                color = androidx.wear.compose.material.MaterialTheme.colors.primary,
                style = androidx.wear.compose.material.MaterialTheme.typography.caption1.copy(fontSize = 14.sp),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            // --- END BUILD INFO TEXT ---
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameListItem(
    game: Game,
    onEditGame: (Game) -> Unit,
    onViewLog: (Game) -> Unit, // The callback to view the log
    onDelete: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("EEE, MMM d, yyyy 'at' HH:mm", Locale.getDefault()) }
    val context = LocalContext.current // Get context for launching Intent
    Card(
        // Add conditional logic to the onClick lambda
        onClick = {
            if (game.status == GameStatus.SCHEDULED) {
                // Otherwise (if it's upcoming or in-progress), call the function to edit
                onEditGame(game)
            } else {
                // If the game is finished, call the function to view the log
                onViewLog(game)
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column( modifier = Modifier
                .weight(1f) // <<<< KEY CHANGE: Makes this column flexible
                .padding(end = 8.dp) // Optional: Add some padding between text and button
            ) {
                Text("${game.homeTeamName} vs ${game.awayTeamName}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color =  MaterialTheme.colorScheme.error)
                Text("#${game.gameNumber}", style = MaterialTheme.typography.bodySmall,
                    color =  MaterialTheme.colorScheme.surfaceTint)
                game.ageGroup?.let {
                    Text("Age Group: ${it.displayName}", style = MaterialTheme.typography.bodySmall,
                        color =  MaterialTheme.colorScheme.tertiary)
                }
                game.formattedGameDateTime?.let {
                    Text("Time: $it", style = MaterialTheme.typography.bodyMedium)
                }
                // --- Display Venue and/or Field Number ---
                val locationDetails = mutableListOf<String>()
                game.venue?.takeIf { it.isNotBlank() }?.let { venueText ->
                    locationDetails.add(venueText) // Add venue if it exists
                }
                game.fieldNumber?.takeIf { it.isNotBlank() }?.let { fieldNum ->
                    locationDetails.add("Field: $fieldNum") // Add field number if it exists
                }

                if (locationDetails.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = locationDetails.joinToString(" - "), // e.g., "City Park - Field: 3" or just "Field: 3" or just "City Park"
                            style = MaterialTheme.typography.bodyMedium,
                            color =  MaterialTheme.colorScheme.tertiary,
                            maxLines = 2
                        )

                    }
                }
                // --- End Display Venue and/or Field Number ---
                Text(
                    "H: ${game.homeScore} - A: ${game.awayScore}",
                    /*(${game.currentPhase.readable()})*/
                    style = MaterialTheme.typography.bodySmall
                )
                // You can add more details from GameSettings here
                // Row(verticalAlignment = Alignment.CenterVertically) {
                //     Box(modifier = Modifier.size(16.dp).background(game.homeTeamColor))
                //     Spacer(Modifier.width(4.dp))
                //     Text("Home", style = MaterialTheme.typography.bodySmall)
                //     Spacer(Modifier.width(8.dp))
                //     Box(modifier = Modifier.size(16.dp).background(game.awayTeamColor))
                //     Spacer(Modifier.width(4.dp))
                //     Text("Away", style = MaterialTheme.typography.bodySmall)
                // }
            }
            // Right side: Location IconButton (if location exists) and Delete IconButton
            Column(horizontalAlignment = Alignment.End) { // Aligns buttons to the right
                val fullLocationQuery = remember(game.venue, game.fieldNumber) {
                    // Construct a query string. Prioritize venue if available.
                    // If only field number, it might not be enough for a good map search.
                    // You might want to combine with city/state if you have that info elsewhere.
                    val venuePart = game.venue?.takeIf { it.isNotBlank() } ?: ""
                    val fieldPart = game.fieldNumber?.takeIf { it.isNotBlank() }?.let { "Field $it" } ?: ""

                    if (venuePart.isNotBlank() && fieldPart.isNotBlank() && venuePart.contains(fieldPart, ignoreCase = true)) {
                        venuePart // If venue already contains field info, just use venue
                    } else if (venuePart.isNotBlank() && fieldPart.isNotBlank()) {
                        "$venuePart, $fieldPart" // Combine them
                    } else {
                        venuePart.ifBlank { fieldPart } // Use whichever is not blank
                    }
                }
                // Define a consistent height for the icon button area
                val iconButtonHeight = 40.dp // Or 48.dp if you prefer standard touch target directly

                if (fullLocationQuery.isNotBlank()) {
                    IconButton(
                        onClick = {
                            // Create an Intent to open Google Maps
                            // Use geo:0,0?q=lat,lng(label) or geo:0,0?q=address
                            val mapQuery = Uri.encode(fullLocationQuery)
                            val gmmIntentUri = Uri.parse("geo:0,0?q=$mapQuery")
                            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                            mapIntent.setPackage("com.google.android.apps.maps") // Specific to Google Maps app

                            // Verify that the intent will resolve to an activity
                            if (mapIntent.resolveActivity(context.packageManager) != null) {
                                context.startActivity(mapIntent)
                            } else {
                                // Optionally, handle the case where Google Maps is not installed
                                // You could try a generic geo intent without setPackage,
                                // or show a toast message.
                                val genericMapIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$mapQuery"))
                                try {
                                    context.startActivity(genericMapIntent)
                                } catch (e: Exception) {
                                    Log.e("MapLink", "No map app found to handle: $fullLocationQuery", e)
                                    // Toast.makeText(context, "No map application found.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .height(iconButtonHeight) // Ensure it takes up consistent height
                            .size(iconButtonHeight) // You can also use .size()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn, // Or Icons.Filled.Navigation or other map-related icon
                            contentDescription = "Open in Maps",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                } else {
                    // Placeholder to maintain vertical alignment when location icon is absent
                    Box(modifier = Modifier.height(iconButtonHeight)) {
                        // You can leave this Box empty, or put a transparent Spacer if you prefer
                        // Spacer(Modifier.size(iconButtonHeight)) // Alternative if Box feels wrong
                    }
                }

                Spacer(modifier = Modifier.height(8.dp)) // Adjust 8.dp to your desired spacing

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .height(iconButtonHeight) // Ensure it takes up consistent height
                        .size(iconButtonHeight) // Match the size/height
                ) {
                    Icon(Icons.Filled.Delete, "Delete Game", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}




// ---------------------- PREVIEWS ---------------------------
// -----------------------------------------------------------
// Helper to create sample games for previews
@Preview(device = "id:Nexus 7 2013", showSystemUi = true, showBackground = true, name = "GameList - Unauthenticated",  uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(device = "id:pixel_c", showSystemUi = true, showBackground = true, name = "GameList - Unauthenticated",  uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(device = "id:pixel_9", showSystemUi = true, showBackground = true, name = "GameList - Unauthenticated",  uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun GameListScreenPreview_Unauthenticated() {
    RefWatchMobileTheme {
        GameListScreen(
            authStateValue = AuthState.Unauthenticated,
            games = createSampleGames(),
            selectedTab = GameStatus.SCHEDULED, // Default, not really visible
            onTabSelected = {},
            onAddGame = {},
            onEditGame = {},
            onViewLog = {},
            onDeleteGame = {},
            onSignOut = {},
            onImportGames = {},
            onNavigateToSettings = {}
        )
    }
}

@Preview(device = "id:pixel_9", showSystemUi = true, showBackground = true, name = "GameList - Loading",  uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun GameListScreenPreview_Loading() {
    RefWatchMobileTheme {
        GameListScreen(
            authStateValue = AuthState.Unauthenticated,
            games = createSampleGames(),
            selectedTab = GameStatus.COMPLETED, // Default
            onTabSelected = {},
            onAddGame = {},
            onEditGame = {},
            onViewLog = {},
            onDeleteGame = {},
            onSignOut = {},
            onImportGames = {},
            onNavigateToSettings = {}
        )
    }
}
/*

@Preview(
    device = "id:pixel_9",
    showSystemUi = true,
    backgroundColor = 0xff000000,
    showBackground = true,
    name = "GameList - Loading State"
)
@Composable
fun GameListScreenPreview_Loading() {
    val mockViewModel = PreviewMobileGameViewModel(
        initialGames = emptyList(),
    )
    // RefWatchTheme {
    GameListScreen(
        viewModel = mockViewModel,
        onAddGame = {},
        onEditGame = {},
        onViewLog = {},
        onDeleteGame = {},
        onImportGames = {},
        onNavigateToSettings = {}
    )
    // }
}

@Preview(
    device = "id:pixel_9",
    showSystemUi = true,
    backgroundColor = 0xff000000,
    showBackground = true,
    name = "GameList - Error State"
)
@Composable
fun GameListScreenPreview_Error() {
    val mockViewModel = PreviewMobileGameViewModel(
        initialGames = emptyList(),
    )
    // RefWatchTheme {
    GameListScreen(
        viewModel = mockViewModel,
        onAddGame = {},
        onEditGame = {},
        onViewLog = {},
        onDeleteGame = {},
        onImportGames = {},
        onNavigateToSettings = {}
    )
    // }
}

@Preview(
    device = "id:pixel_9",
    showSystemUi = true,
    backgroundColor = 0xff000000,
    showBackground = true,
    name = "GameList - With Scheduled Games"
)
@Composable
fun GameListScreenPreview_WithScheduledGames() {
    val mockViewModel =
        PreviewMobileGameViewModel(initialGames = createSampleGames().filter { it.status == GameStatus.SCHEDULED })
    // RefWatchTheme {
    GameListScreen(
        viewModel = mockViewModel,
        onAddGame = {},
        onEditGame = {},
        onViewLog = {},
        onDeleteGame = {},
        onImportGames = {},
        onNavigateToSettings = {}
    )
    // }
}

@Preview(
    device = "id:pixel_9",
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
    val mockViewModel = PreviewMobileGameViewModel(initialGames = createSampleGames())
    // RefWatchTheme {
    GameListScreen(
        viewModel = mockViewModel,
        onAddGame = {},
        onEditGame = {},
        onViewLog = {},
        onDeleteGame = {},
        onImportGames = {},
        onNavigateToSettings = {}
    )
    // }
}

@Preview(
    device = "id:pixel_9",
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
    val mockViewModel = PreviewMobileGameViewModel(initialGames = createSampleGames(),)
    // RefWatchTheme {
    GameListScreen(
        viewModel = mockViewModel,
        onAddGame = {},
        onEditGame = {},
        onViewLog = {},
        onDeleteGame = {},
        onImportGames = {},
        onNavigateToSettings = {}
    )
    // }
}

*/
