package com.databelay.refwatch.games

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.* // Keep specific imports if preferred
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.databelay.refwatch.common.Game // Assuming Game is needed for onSave
import com.databelay.refwatch.common.predefinedColors // If AddEditGameUiState doesn't hold them
import com.databelay.refwatch.common.theme.RefWatchMobileTheme // Your app's theme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// --- Stateless Composable ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditGameScreen(
    uiState: AddEditGameUiState,
    onNavigateBack: () -> Unit,
    onHomeTeamNameChange: (String) -> Unit,
    onAwayTeamNameChange: (String) -> Unit,
    onVenueChange: (String) -> Unit,
    onCompetitionChange: (String) -> Unit,
    onGameDateTimeChange: (Long) -> Unit, // Pass epoch millis
    onHalfDurationChange: (String) -> Unit, // Pass string for TextField
    onHalftimeDurationChange: (String) -> Unit, // Pass string for TextField
    onHomeColorSelected: (Color) -> Unit, // Pass compose Color
    onAwayColorSelected: (Color) -> Unit,
    onNotesChanged: (String) -> Unit,
    onSaveGame: () -> Unit, // ViewModel handles constructing Game object
    // You might also need specific event handlers for date/time picker interactions
    // if they are complex and managed by the ViewModel.
    // For simplicity here, we assume basic state updates.
) {
    val scrollState = rememberScrollState()

    // Date Picker State (derived from uiState or managed via callbacks)
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = uiState.gameDateTimeEpochMillis ?: System.currentTimeMillis()
    )
    var showDatePicker by remember { mutableStateOf(false) }

    // Time Picker State
    val calendar = Calendar.getInstance()
    uiState.gameDateTimeEpochMillis?.let { calendar.timeInMillis = it }
    val timePickerState = rememberTimePickerState(
        initialHour = calendar.get(Calendar.HOUR_OF_DAY),
        initialMinute = calendar.get(Calendar.MINUTE),
        is24Hour = true // Or based on locale
    )
    var showTimePicker by remember { mutableStateOf(false) }

    // Color Picker States
    var showHomeColorPicker by remember { mutableStateOf(false) }
    var showAwayColorPicker by remember { mutableStateOf(false) }
    // var showKickOffPicker by remember { mutableStateOf(false) } // Removed if not used

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditing) "Edit Game" else "Add New Game") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onSaveGame) { // Call the passed-in onSaveGame
                Icon(Icons.Filled.Save, contentDescription = "Save Game")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = uiState.homeTeamName,
                onValueChange = onHomeTeamNameChange,
                label = { Text("Home Team Name") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                singleLine = true
            )
            OutlinedTextField(
                value = uiState.awayTeamName,
                onValueChange = onAwayTeamNameChange,
                label = { Text("Away Team Name") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                singleLine = true
            )
            OutlinedTextField(
                value = uiState.venue,
                onValueChange = onVenueChange,
                label = { Text("Venue / Location (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                singleLine = true
            )
            OutlinedTextField(
                value = uiState.competition,
                onValueChange = onCompetitionChange,
                label = { Text("Competition (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                singleLine = true
            )

            val sdfDate = remember { SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()) }
            val sdfTime = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
            val selectedDateTimeString = uiState.gameDateTimeEpochMillis?.let {
                "${sdfDate.format(Date(it))} at ${sdfTime.format(Date(it))}"
            } ?: "Select Date & Time"

            OutlinedTextField(
                value = selectedDateTimeString,
                onValueChange = {}, // Not directly editable
                label = { Text("Game Date & Time") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true },
                readOnly = true,
                trailingIcon = { Icon(Icons.Filled.DateRange, contentDescription = "Select Date") }
            )

            if (showDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            showDatePicker = false
                            datePickerState.selectedDateMillis?.let { selectedDate ->
                                val cal = Calendar.getInstance()
                                uiState.gameDateTimeEpochMillis?.let { cal.timeInMillis = it }
                                cal.timeInMillis = selectedDate // Set only date part, preserve time
                                // Update timePickerState to reflect new date's existing time (or default)
                                timePickerState.hour = cal.get(Calendar.HOUR_OF_DAY)
                                timePickerState.minute = cal.get(Calendar.MINUTE)
                                showTimePicker = true // Show time picker immediately after date
                            }
                        }) { Text("OK") }
                    },
                    dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
                ) {
                    DatePicker(state = datePickerState)
                }
            }

            if (showTimePicker) {
                TimePickerDialog( // Ensure you have this custom dialog or use a Material 3 alternative
                    onDismissRequest = { showTimePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            showTimePicker = false
                            val cal = Calendar.getInstance()
                            // Start with date from datePickerState
                            datePickerState.selectedDateMillis?.let { cal.timeInMillis = it }
                            cal.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                            cal.set(Calendar.MINUTE, timePickerState.minute)
                            cal.set(Calendar.SECOND, 0)
                            cal.set(Calendar.MILLISECOND, 0)
                            onGameDateTimeChange(cal.timeInMillis) // Update ViewModel
                        }) { Text("OK") }
                    },
                    dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } }
                ) {
                    TimePicker(state = timePickerState, modifier = Modifier.padding(16.dp))
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = uiState.halfDurationMinutes.toString(),
                    onValueChange = onHalfDurationChange,
                    label = { Text("Half (min)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = uiState.halftimeDurationMinutes.toString(),
                    onValueChange = onHalftimeDurationChange,
                    label = { Text("Halftime (min)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { showHomeColorPicker = true }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(20.dp).background(Color(uiState.homeTeamColorArgb)))
                        Spacer(Modifier.width(4.dp))
                        Text("Home Color")
                    }
                }
                Button(onClick = { showAwayColorPicker = true }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(20.dp).background(Color(uiState.awayTeamColorArgb)))
                        Spacer(Modifier.width(4.dp))
                        Text("Away Color")
                    }
                }
            }

            if (showHomeColorPicker) {
                ColorPickerDialog(
                    title = "Select Home Color",
                    availableColors = predefinedColors,
                    selectedColor = Color(uiState.homeTeamColorArgb),
                    onColorSelected = { color:Color ->
                        onHomeColorSelected(color) // Call ViewModel update
                        showHomeColorPicker = false
                    },
                    onDismiss = { showHomeColorPicker = false }
                )
            }
            if (showAwayColorPicker) {
                ColorPickerDialog(
                    title = "Select Away Color",
                    availableColors = predefinedColors,
                    selectedColor = Color(uiState.awayTeamColorArgb),
                    onColorSelected = { color:Color ->
                        onAwayColorSelected(color) // Call ViewModel update
                        showAwayColorPicker = false
                    },
                    onDismiss = { showAwayColorPicker = false }
                )
            }

            OutlinedTextField(
                value = uiState.notes,
                onValueChange = onNotesChanged,
                label = { Text("Notes (Optional)") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )

            uiState.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(60.dp)) // Space for FAB
        }
    }
}


// --- ViewModel-Driven Route Composable ---
@Composable
fun AddEditGameRoute(
    navController: NavController, // For navigation
    addEditViewModel: AddEditGameViewModel = hiltViewModel(),
    mobileGameViewModel: MobileGameViewModel = hiltViewModel() // For fetching existing game if needed for edit, and for saving
) {
    val uiState by addEditViewModel.uiState.collectAsStateWithLifecycle()

    // Extract gameId from NavController's arguments if you're navigating with gameId
    // This part depends on how you set up your navigation route for editing
    val gameId = navController.currentBackStackEntry?.arguments?.getString("gameId") // Example

    LaunchedEffect(gameId) { // Load game data if gameId is present (for editing)
        if (gameId != null && !uiState.isEditing) { // Only load if not already editing this game
            // Fetch the game from MobileGameViewModel's list or a repository
            val gameToEdit = mobileGameViewModel.gamesList.value.find { it.id == gameId }
            addEditViewModel.initializeForm(gameToEdit)
        } else if (gameId == null) {
            addEditViewModel.initializeForm(null) // New game
        }
    }

    AddEditGameScreen(
        uiState = uiState,
        onNavigateBack = { navController.popBackStack() },
        onHomeTeamNameChange = addEditViewModel::onHomeTeamNameChange,
        onAwayTeamNameChange = addEditViewModel::onAwayTeamNameChange,
        onVenueChange = addEditViewModel::onVenueChange,
        onCompetitionChange = addEditViewModel::onCompetitionChange,
        onGameDateTimeChange = addEditViewModel::onGameDateTimeChange,
        onHalfDurationChange = addEditViewModel::onHalfDurationChange,
        onHalftimeDurationChange = addEditViewModel::onHalftimeDurationChange,
        onHomeColorSelected = addEditViewModel::onHomeColorSelected, // Assuming ViewModel takes Color
        onAwayColorSelected = addEditViewModel::onAwayColorSelected, // Assuming ViewModel takes Color
        onNotesChanged = addEditViewModel::onNotesChanged,
        onSaveGame = {
            addEditViewModel.onSaveGame { gameWithUpdates ->
                // gameWithUpdates is the Game object constructed by AddEditGameViewModel
                mobileGameViewModel.addOrUpdateGame(gameWithUpdates)
                navController.popBackStack()
            }
        }
    )
}

// --- Previews ---

@Preview(name = "Add New Game (Light)", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Add New Game (Dark)", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AddEditGameScreen_AddNewPreview() {
    RefWatchMobileTheme { // Use your app's theme
        AddEditGameScreen(
            uiState = AddEditGameUiState(isEditing = false),
            onNavigateBack = {},
            onHomeTeamNameChange = {},
            onAwayTeamNameChange = {},
            onVenueChange = {},
            onCompetitionChange = {},
            onGameDateTimeChange = {},
            onHalfDurationChange = {},
            onHalftimeDurationChange = {},
            onHomeColorSelected = {},
            onAwayColorSelected = {},
            onNotesChanged = {},
            onSaveGame = {}
        )
    }
}

@Preview(name = "Edit Game (Light)", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Edit Game (Dark)", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AddEditGameScreen_EditPreview() {
    RefWatchMobileTheme {
        AddEditGameScreen(
            uiState = AddEditGameUiState(
                gameId = "previewGame123",
                homeTeamName = "Preview Home Team",
                awayTeamName = "Preview Away Team",
                venue = "Preview Venue",
                competition = "Preview League",
                gameDateTimeEpochMillis = System.currentTimeMillis() + (3 * 24 * 60 * 60 * 1000), // 3 days from now
                halfDurationMinutes = 40,
                halftimeDurationMinutes = 10,
                homeTeamColorArgb = Color.Blue.toArgb(),
                awayTeamColorArgb = Color.Red.toArgb(),
                notes = "This is a note for the preview of an edited game.",
                isEditing = true,
                errorMessage = null
            ),
            onNavigateBack = {},
            onHomeTeamNameChange = {},
            onAwayTeamNameChange = {},
            onVenueChange = {},
            onCompetitionChange = {},
            onGameDateTimeChange = {},
            onHalfDurationChange = {},
            onHalftimeDurationChange = {},
            onHomeColorSelected = {},
            onAwayColorSelected = {},
            onNotesChanged = {},
            onSaveGame = {}
        )
    }
}

// Dummy composable for previewing TimePickerDialog if Material 3 doesn't have a direct one yet
// Or implement a more complete one based on AlertDialog
@Composable
fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Select Time") },
        text = content,
        confirmButton = confirmButton,
        dismissButton = dismissButton
    )
}
