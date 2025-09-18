package com.databelay.refwatch.games

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.databelay.refwatch.auth.AuthState
import com.databelay.refwatch.common.Game
import com.databelay.refwatch.common.GameStatus
import com.databelay.refwatch.common.PreviewTools.createSampleGames
import com.databelay.refwatch.common.theme.RefWatchMobileTheme
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.*


// Data class to define the structure and behavior of context menu items
data class ContextMenuItemAction(
    val label: String,
    val action: (game: Game) -> Unit // The action lambda now takes the specific Game as a parameter
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameListScreen(
    authStateValue: AuthState, // << Pass AuthState directly
    games: List<Game>, // << Pass the list of games to display directly
    selectedTab: GameStatus,    // << Pass current tab
    scrollToTopSignal: SharedFlow<Unit>?, // Event stream passed in
    onTabSelected: (GameStatus) -> Unit, // << Callback to change tab
    onAddGame: () -> Unit,
    onEditGame: (Game) -> Unit,
    onViewLog: (Game) -> Unit, // <-- Ensure this is passed
    onDeleteGame: (Game) -> Unit,
    onSignOut: () -> Unit, // This might be handled by the calling composable via AuthViewModel
    onImportGames: () -> Unit, // Callback for importing
    onNavigateToSettings: () -> Unit
) {

    Log.d(
        "GameListScreen",
        "Received games: ${games.map { it.id + " -> " + it.status }}"
    ) // Log input games

    // Filter and sort the lists, just like on the watch
    val (upcomingGames, pastGames) = remember(games) {
        val (scheduled, completed) = games.partition { it.status == GameStatus.SCHEDULED }
        Pair(
            scheduled.sortedBy { it.gameDateTimeEpochMillis },
            completed.sortedByDescending { it.gameDateTimeEpochMillis }
        )
    }
    val gamesToDisplay = if (selectedTab == GameStatus.SCHEDULED) upcomingGames else pastGames
    val lazyListState = rememberLazyListState()

    // --- Collect UI events ---
    LaunchedEffect(key1 = scrollToTopSignal) { // Key on the signal itself in case it could change
        scrollToTopSignal?.collectLatest {
            Log.d("GameListScreen", "ScrollToTop event received in UI.")
            if (gamesToDisplay.isNotEmpty()) {
                lazyListState.animateScrollToItem(index = 0)
            }
        }
    }

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
                    items(gamesToDisplay, key = { it.id }) { game ->
                        GameListItem(
                            game = game,
                            onEditGame = { onEditGame(game) },
                            onViewLog = onViewLog, // <-- Pass it down to the item
                            onDeleteGame = { onDeleteGame(game) }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GameListItem(
    game: Game,
    onEditGame: (Game) -> Unit,
    onViewLog: (Game) -> Unit,
    onDeleteGame: (Game) -> Unit
) {
    val dateFormat =
        remember { SimpleDateFormat("EEE, MMM d, yyyy 'at' HH:mm", Locale.getDefault()) }
    val context = LocalContext.current // Get context for launching Intent
    var showContextMenu by remember { mutableStateOf(false) }

    Box {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .combinedClickable(
                    onClick = {
                        showContextMenu = false // Dismiss context menu on click
                        if (game.status == GameStatus.SCHEDULED) {
                            onEditGame(game)
                        } else {
                            onViewLog(game)
                        }
                    },
                    onLongClick = {
                        showContextMenu = true
                    }
                )
        ) {
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f) // <<<< KEY CHANGE: Makes this column flexible
                        .padding(end = 8.dp) // Optional: Add some padding between text and button
                ) {
                    Text(
                        "${game.homeTeamName} vs ${game.awayTeamName}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        "#${game.gameNumber}", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.surfaceTint
                    )
                    game.ageGroup?.let {
                        Text(
                            "Age Group: ${it.displayName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
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
                                color = MaterialTheme.colorScheme.tertiary,
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
                }
                // Right side: Location IconButton (if location exists) and Delete IconButton
                Column(horizontalAlignment = Alignment.End) { // Aligns buttons to the right
                    val fullLocationQuery = remember(game.venue, game.fieldNumber) {
                        val venuePart = game.venue?.takeIf { it.isNotBlank() } ?: ""
                        val fieldPart =
                            game.fieldNumber?.takeIf { it.isNotBlank() }?.let { "Field $it" } ?: ""

                        if (venuePart.isNotBlank() && fieldPart.isNotBlank() && venuePart.contains(
                                fieldPart,
                                ignoreCase = true
                            )
                        ) {
                            venuePart
                        } else if (venuePart.isNotBlank() && fieldPart.isNotBlank()) {
                            "$venuePart, $fieldPart"
                        } else {
                            venuePart.ifBlank { fieldPart }
                        }
                    }
                    val iconButtonHeight = 40.dp

                    if (fullLocationQuery.isNotBlank()) {
                        IconButton(
                            onClick = {
                                val mapQuery = Uri.encode(fullLocationQuery)
                                val gmmIntentUri = Uri.parse("geo:0,0?q=$mapQuery")
                                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                mapIntent.setPackage("com.google.android.apps.maps")
                                if (mapIntent.resolveActivity(context.packageManager) != null) {
                                    context.startActivity(mapIntent)
                                } else {
                                    val genericMapIntent =
                                        Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$mapQuery"))
                                    try {
                                        context.startActivity(genericMapIntent)
                                    } catch (e: Exception) {
                                        Log.e(
                                            "MapLink",
                                            "No map app found to handle: $fullLocationQuery",
                                            e
                                        )
                                    }
                                }
                            },
                            modifier = Modifier
                                .height(iconButtonHeight)
                                .size(iconButtonHeight)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = "Open in Maps",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    } else {
                        Box(modifier = Modifier.height(iconButtonHeight))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    IconButton(
                        onClick = { onDeleteGame(game) }, // This IconButton is now outside the context menu logic
                        modifier = Modifier
                            .height(iconButtonHeight)
                            .size(iconButtonHeight)
                    ) {
                        Icon(
                            Icons.Filled.Delete,
                            "Delete Game",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        // 1. Define the list of actions for the context menu.
        //    This list is remembered. The `action` lambdas here just define WHAT to do.
        val contextMenuActions =
            remember(game.status) { // Re-calculate if available actions change based on game status
                Log.d(
                    "GameListItem",
                    "Defining context menu actions for game: ${game.id}, status: ${game.status}"
                )
                listOfNotNull(
                    ContextMenuItemAction("Edit Game") { gameParam ->
                        Log.d(
                            "GameListItem",
                            "Action 'Edit Game' invoked for game: ${gameParam.id}"
                        )
                        onEditGame(gameParam)
                    },
                    // Conditionally add "View Log" if applicable
                    ContextMenuItemAction("View Log") { gameParam ->
                        Log.d("GameListItem", "Action 'View Log' invoked for game: ${gameParam.id}")
                        onViewLog(gameParam)
                    },
                    ContextMenuItemAction("Delete Game") { gameParam -> //
                        Log.d("GameListItem", "Action 'Delete Game' invoked")
                        onDeleteGame(gameParam)
                    }
                )
            }

        // 2. Build the DropdownMenu using these actions.
        DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = { showContextMenu = false }
        ) {
            contextMenuActions.forEach { itemDefinition ->
                DropdownMenuItem(
                    text = { Text(itemDefinition.label) },
                    // 3. The onClick for the DropdownMenuItem itself.
                    //    This lambda captures the CURRENT 'game' from GameListItem's scope.
                    onClick = {
                        Log.d(
                            "GameListItem",
                            "Context Menu Item '${itemDefinition.label}' CLICKED. Game to use: ${game.id} (${game.homeTeamName})"
                        )
                        itemDefinition.action(game) // Pass the CURRENT 'game' to the defined action
                        showContextMenu = false
                    }
                    // You could add leadingIcon here if ContextMenuItemAction had an icon property
                )
            }
        }
    }
}


// -------------------------------- PREVIEWS ----------------------------------------------------
// ----------------------------------------------------------------------------------------------
// Helper to create sample games for previews
@Preview(
    device = "id:medium_phone",
    showSystemUi = true,
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Preview(
    device = "id:small_phone",
    showSystemUi = true,
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun GameListScreenPreview_Unauthenticated() {
    RefWatchMobileTheme {
        GameListScreen(
            authStateValue = AuthState.Unauthenticated,
            games = createSampleGames(),
            selectedTab = GameStatus.SCHEDULED, // Default, not really visible
            scrollToTopSignal = null,
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

@Preview(
    device = "id:pixel_9",
    showSystemUi = true,
    showBackground = true,
    name = "GameList - Loading",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun GameListScreenPreview_Loading() {
    RefWatchMobileTheme {
        GameListScreen(
            authStateValue = AuthState.Unauthenticated,
            games = createSampleGames(),
            selectedTab = GameStatus.COMPLETED, // Default
            scrollToTopSignal = null,
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



