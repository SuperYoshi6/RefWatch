package com.databelay.refwatch.screens

import android.content.res.Configuration
import android.widget.Toast
import android.util.Log
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chair
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SportsFootball
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.databelay.refwatch.R
import com.databelay.refwatch.common.Player
import com.databelay.refwatch.common.Team
import com.databelay.refwatch.common.TeamOfficial
import com.databelay.refwatch.common.theme.AccentGreen
import com.databelay.refwatch.common.theme.Border
import com.databelay.refwatch.common.theme.PitchBackground
import com.databelay.refwatch.common.theme.Surface
import com.databelay.refwatch.common.theme.TextMuted
import com.databelay.refwatch.common.theme.TextPrimary
import com.databelay.refwatch.common.theme.RefWatchMobileTheme
import com.databelay.refwatch.data.AddEditGameUiState
import com.databelay.refwatch.data.AddEditGameViewModel
import com.databelay.refwatch.common.luminance
import com.databelay.refwatch.common.theme.PredefinedJerseyColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditGameScreen(
    uiState: AddEditGameUiState,
    onNavigateBack: () -> Unit,
    onHomeTeamNameChange: (String) -> Unit,
    onAwayTeamNameChange: (String) -> Unit,
    onHomeTeamAbbrChange: (String) -> Unit,
    onAwayTeamAbbrChange: (String) -> Unit,
    onHomeCaptainNumberChange: (String) -> Unit,
    onAwayCaptainNumberChange: (String) -> Unit,
    onFieldNumberChange: (String) -> Unit,
    onRefereeAssignmentChange: (String) -> Unit,
    onVenueChange: (String) -> Unit,
    onCompetitionChange: (String) -> Unit,
    onMainRefereeChange: (String) -> Unit,
    onAssistantReferee1Change: (String) -> Unit,
    onAssistantReferee2Change: (String) -> Unit,
    onFourthOfficialChange: (String) -> Unit,
    onObserverChange: (String) -> Unit,
    onGameDateTimeChange: (Long) -> Unit,
    onHalfDurationChange: (String) -> Unit,
    onHalftimeDurationChange: (String) -> Unit,
    onHasExtraTimeChange: (Boolean) -> Unit,
    onExtraTimeHalfDurationChange: (String) -> Unit,
    onMaxSubstitutionsChange: (String) -> Unit,
    onHasTemporaryDismissalsChange: (Boolean) -> Unit,
    onTemporaryDismissalChange: (String) -> Unit,
    onHasPenaltiesChange: (Boolean) -> Unit,
    onPenaltyKicksPerTeamChange: (String) -> Unit,
    onHomeColorSelected: (Color) -> Unit,
    onAwayColorSelected: (Color) -> Unit,
    onNotesChanged: (String) -> Unit,
    onHomeScoreChange: (String) -> Unit,
    onAwayScoreChange: (String) -> Unit,
    onApplyTemplate: (Team, Int, Int) -> Unit,
    onAddPlayer: (Team, Int, String) -> Unit,
    onRemovePlayer: (Team, String) -> Unit,
    onToggleCaptain: (Team, String) -> Unit,
    onToggleOnField: (Team, String) -> Unit,
    onUpdatePlayerNumber: (Team, String, Int) -> Unit,
    onUpdatePlayerName: (Team, String, String) -> Unit,
    onAddOfficial: (Team, String, String) -> Unit,
    onRemoveOfficial: (Team, String) -> Unit,
    onSaveGame: () -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val scrollState = rememberScrollState()

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = uiState.gameDateTimeEpochMillis ?: System.currentTimeMillis()
    )

    val calendar = Calendar.getInstance()
    uiState.gameDateTimeEpochMillis?.let { calendar.timeInMillis = it }
    val timePickerState = rememberTimePickerState(
        initialHour = calendar.get(Calendar.HOUR_OF_DAY),
        initialMinute = calendar.get(Calendar.MINUTE),
        is24Hour = true
    )
    var showTimePicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showHomeColorPicker by remember { mutableStateOf(false) }
    var showAwayColorPicker by remember { mutableStateOf(false) }
    var selectedTeamColorTarget by remember { mutableStateOf("HOME") }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = if (uiState.isEditing) stringResource(R.string.edit_game) else stringResource(R.string.add_game),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onNavigateBack,
                            colors = IconButtonDefaults.iconButtonColors(contentColor = TextPrimary)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = TextPrimary,
                        navigationIconContentColor = TextPrimary
                    )
                )
                SecondaryTabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color.Transparent,
                    contentColor = AccentGreen,
                    indicator = {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(selectedTabIndex),
                            color = AccentGreen
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = { Text(stringResource(R.string.tab_details)) }
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = { Text(stringResource(R.string.tab_roster)) }
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { 
                    Log.d("AddEditGameScreen", "Save FAB clicked. isSaving: ${uiState.isSaving}")
                    if (!uiState.isSaving) onSaveGame() 
                },
                containerColor = AccentGreen,
                contentColor = Color(0xFF0A1A0A),
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp
                ),
                shape = CircleShape
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color(0xFF0A1A0A),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Filled.Save, contentDescription = stringResource(R.string.save_game))
                }
            }
        }
    ) { paddingValues ->
        PitchBackground(modifier = Modifier.padding(paddingValues)) {
            if (selectedTabIndex == 0) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HeroHeader(
                        title = if (uiState.isEditing) stringResource(R.string.hero_edit_game) else stringResource(R.string.hero_new_game),
                        subtitle = stringResource(R.string.hero_sync_subtitle)
                    )
                    Spacer(Modifier.height(4.dp))

                    FormCard(title = stringResource(R.string.section_teams)) {
                        OutlinedTextField(
                            value = uiState.homeTeamName,
                            onValueChange = onHomeTeamNameChange,
                            label = { Text(stringResource(R.string.home_team_name)) },
                            placeholder = { Text(stringResource(R.string.placeholder_home_team) + (if (uiState.homeTeamAbbr.isNotBlank()) " (${uiState.homeTeamAbbr})" else "")) },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            singleLine = true,
                            colors = darkFieldColors()
                        )
                        OutlinedTextField(
                            value = uiState.awayTeamName,
                            onValueChange = onAwayTeamNameChange,
                            label = { Text(stringResource(R.string.away_team_name)) },
                            placeholder = { Text(stringResource(R.string.placeholder_away_team) + (if (uiState.awayTeamAbbr.isNotBlank()) " (${uiState.awayTeamAbbr})" else "")) },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            singleLine = true,
                            colors = darkFieldColors()
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = uiState.homeTeamAbbr,
                                onValueChange = onHomeTeamAbbrChange,
                                label = { Text(stringResource(R.string.home_abbr)) },
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = darkFieldColors()
                            )
                            OutlinedTextField(
                                value = uiState.awayTeamAbbr,
                                onValueChange = onAwayTeamAbbrChange,
                                label = { Text(stringResource(R.string.away_abbr)) },
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = darkFieldColors()
                            )
                        }
                    }

                    FormCard(title = stringResource(R.string.section_score)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = uiState.homeScore,
                                onValueChange = onHomeScoreChange,
                                label = { Text(stringResource(R.string.home_score)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = darkFieldColors()
                            )
                            OutlinedTextField(
                                value = uiState.awayScore,
                                onValueChange = onAwayScoreChange,
                                label = { Text(stringResource(R.string.away_score)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = darkFieldColors()
                            )
                        }
                    }

                    FormCard(title = stringResource(R.string.section_jersey_colors)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    selectedTeamColorTarget = "HOME"
                                    showHomeColorPicker = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Surface, contentColor = TextPrimary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .background(Color(uiState.homeTeamColorArgb), RoundedCornerShape(4.dp))
                                            .padding(2.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(R.string.home_color))
                                }
                            }
                            Button(
                                onClick = {
                                    selectedTeamColorTarget = "AWAY"
                                    showAwayColorPicker = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Surface, contentColor = TextPrimary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .background(Color(uiState.awayTeamColorArgb), RoundedCornerShape(4.dp))
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(R.string.away_color))
                                }
                            }
                        }
                    }

                    FormCard(title = stringResource(R.string.referee_team)) {
                        OutlinedTextField(
                            value = uiState.mainReferee,
                            onValueChange = onMainRefereeChange,
                            label = { Text(stringResource(R.string.main_referee)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = darkFieldColors()
                        )
                        OutlinedTextField(
                            value = uiState.assistantReferee1,
                            onValueChange = onAssistantReferee1Change,
                            label = { Text(stringResource(R.string.assistant_referee_1)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = darkFieldColors()
                        )
                        OutlinedTextField(
                            value = uiState.assistantReferee2,
                            onValueChange = onAssistantReferee2Change,
                            label = { Text(stringResource(R.string.assistant_referee_2)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = darkFieldColors()
                        )
                        OutlinedTextField(
                            value = uiState.fourthOfficial,
                            onValueChange = onFourthOfficialChange,
                            label = { Text(stringResource(R.string.fourth_official)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = darkFieldColors()
                        )
                        OutlinedTextField(
                            value = uiState.observer,
                            onValueChange = onObserverChange,
                            label = { Text(stringResource(R.string.observer)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = darkFieldColors()
                        )
                    }

                    FormCard(title = stringResource(R.string.section_location)) {
                        OutlinedTextField(
                            value = uiState.refereeAssignment,
                            onValueChange = onRefereeAssignmentChange,
                            label = { Text(stringResource(R.string.assignment)) },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                            singleLine = true,
                            colors = darkFieldColors()
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = uiState.venue,
                                onValueChange = onVenueChange,
                                label = { Text(stringResource(R.string.venue_optional)) },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                                singleLine = true,
                                colors = darkFieldColors()
                            )
                            OutlinedTextField(
                                value = uiState.fieldNumber,
                                onValueChange = onFieldNumberChange,
                                label = { Text(stringResource(R.string.field_number_optional)) },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                colors = darkFieldColors()
                            )
                        }
                        OutlinedTextField(
                            value = uiState.competition,
                            onValueChange = onCompetitionChange,
                            label = { Text(stringResource(R.string.competition_optional)) },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                            singleLine = true,
                            colors = darkFieldColors()
                        )
                    }

                    val selectedDateTimeString = uiState.gameDateTimeEpochMillis?.let {
                        val sdfDate = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
                        val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault())
                        stringResource(R.string.date_time_format, sdfDate.format(Date(it)), sdfTime.format(Date(it)))
                    } ?: stringResource(R.string.select_date_time)

                    FormCard(title = stringResource(R.string.section_schedule)) {
                        Box {
                            OutlinedTextField(
                                value = selectedDateTimeString,
                                onValueChange = {},
                                label = { Text(stringResource(R.string.game_date_time)) },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = false,
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = TextPrimary,
                                    disabledBorderColor = Border,
                                    disabledPlaceholderColor = TextMuted,
                                    disabledLabelColor = TextMuted,
                                    disabledTrailingIconColor = TextMuted
                                ),
                                readOnly = true,
                                trailingIcon = { Icon(Icons.Filled.DateRange, contentDescription = stringResource(R.string.select_date)) }
                            )
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { showDatePicker = true }
                            )
                        }
                    }

                    FormCard(title = stringResource(R.string.section_timing)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = if (uiState.halfDurationMinutes == 0) "" else uiState.halfDurationMinutes.toString(),
                                onValueChange = onHalfDurationChange,
                                label = { Text(stringResource(R.string.half_minutes)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                colors = darkFieldColors()
                            )
                            OutlinedTextField(
                                value = if (uiState.halftimeDurationMinutes == 0) "" else uiState.halftimeDurationMinutes.toString(),
                                onValueChange = onHalftimeDurationChange,
                                label = { Text(stringResource(R.string.halftime_minutes)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                colors = darkFieldColors()
                            )
                        }

                        HorizontalDivider(Modifier.padding(vertical = 4.dp), color = Border.copy(alpha = 0.5f))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.extra_time_possible),
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = uiState.hasExtraTime,
                                onCheckedChange = onHasExtraTimeChange,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = TextPrimary,
                                    checkedTrackColor = AccentGreen,
                                    checkedBorderColor = AccentGreen,
                                    uncheckedThumbColor = TextMuted,
                                    uncheckedTrackColor = Surface,
                                    uncheckedBorderColor = Border
                                )
                            )
                        }

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = if (uiState.extraTimeHalfDurationMinutes == 0) "" else uiState.extraTimeHalfDurationMinutes.toString(),
                                onValueChange = onExtraTimeHalfDurationChange,
                                label = { Text(stringResource(R.string.extra_time_minutes)) },
                                enabled = uiState.hasExtraTime,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                colors = if (uiState.hasExtraTime) darkFieldColors() else disabledFieldColors()
                            )
                            OutlinedTextField(
                                value = if (uiState.maxSubstitutionsAllowed == 0) "" else uiState.maxSubstitutionsAllowed.toString(),
                                onValueChange = onMaxSubstitutionsChange,
                                label = { Text(stringResource(R.string.max_substitutions_mobile)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = darkFieldColors()
                            )
                        }
                    }

                    FormCard(title = stringResource(R.string.temporary_dismissal_enabled)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.temporary_dismissal_enabled),
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = uiState.hasTemporaryDismissals,
                                onCheckedChange = onHasTemporaryDismissalsChange,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = TextPrimary,
                                    checkedTrackColor = AccentGreen,
                                    checkedBorderColor = AccentGreen,
                                    uncheckedThumbColor = TextMuted,
                                    uncheckedTrackColor = Surface,
                                    uncheckedBorderColor = Border
                                )
                            )
                        }

                        OutlinedTextField(
                            value = if (uiState.temporaryDismissalMinutes == 0) "" else uiState.temporaryDismissalMinutes.toString(),
                            onValueChange = onTemporaryDismissalChange,
                            label = { Text(stringResource(R.string.temporary_dismissal_minutes)) },
                            enabled = uiState.hasTemporaryDismissals,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = if (uiState.hasTemporaryDismissals) darkFieldColors() else disabledFieldColors()
                        )
                    }

                    FormCard(title = stringResource(R.string.penalty_shootout_enabled)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.penalty_shootout_enabled),
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = uiState.hasPenalties,
                                onCheckedChange = onHasPenaltiesChange,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = TextPrimary,
                                    checkedTrackColor = AccentGreen,
                                    checkedBorderColor = AccentGreen,
                                    uncheckedThumbColor = TextMuted,
                                    uncheckedTrackColor = Surface,
                                    uncheckedBorderColor = Border
                                )
                            )
                        }

                        OutlinedTextField(
                            value = if (uiState.penaltyKicksPerTeam == 0) "" else uiState.penaltyKicksPerTeam.toString(),
                            onValueChange = onPenaltyKicksPerTeamChange,
                            label = { Text(stringResource(R.string.penalty_kicks_per_team)) },
                            placeholder = { Text("5") },
                            enabled = uiState.hasPenalties,
                            readOnly = !uiState.hasPenalties,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = if (uiState.hasPenalties) darkFieldColors() else disabledFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    OutlinedTextField(
                        value = uiState.notes,
                        onValueChange = onNotesChanged,
                        label = { Text(stringResource(R.string.notes_optional)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 80.dp),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        colors = darkFieldColors()
                    )

                    uiState.errorMessage?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }

                    Spacer(Modifier.height(60.dp))
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    RosterManagementSection(
                        teamName = uiState.homeTeamName.ifBlank { stringResource(R.string.home) },
                        team = Team.HOME,
                        roster = uiState.homeRoster,
                        officials = uiState.homeOfficials,
                        onApplyTemplate = onApplyTemplate,
                        onAddPlayer = onAddPlayer,
                        onRemovePlayer = onRemovePlayer,
                        onToggleCaptain = onToggleCaptain,
                        onToggleOnField = onToggleOnField,
                        onUpdatePlayerNumber = onUpdatePlayerNumber,
                        onUpdatePlayerName = onUpdatePlayerName,
                        onAddOfficial = onAddOfficial,
                        onRemoveOfficial = onRemoveOfficial
                    )

                    RosterManagementSection(
                        teamName = uiState.awayTeamName.ifBlank { stringResource(R.string.away) },
                        team = Team.AWAY,
                        roster = uiState.awayRoster,
                        officials = uiState.awayOfficials,
                        onApplyTemplate = onApplyTemplate,
                        onAddPlayer = onAddPlayer,
                        onRemovePlayer = onRemovePlayer,
                        onToggleCaptain = onToggleCaptain,
                        onToggleOnField = onToggleOnField,
                        onUpdatePlayerNumber = onUpdatePlayerNumber,
                        onUpdatePlayerName = onUpdatePlayerName,
                        onAddOfficial = onAddOfficial,
                        onRemoveOfficial = onRemoveOfficial
                    )
                    Spacer(Modifier.height(60.dp))
                }
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showDatePicker = false
                    datePickerState.selectedDateMillis?.let { selectedDate ->
                        val cal = Calendar.getInstance()
                        uiState.gameDateTimeEpochMillis?.let { cal.timeInMillis = it }
                        cal.timeInMillis = selectedDate
                        showTimePicker = true
                    }
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel)) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        TimePickerDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showTimePicker = false
                    val cal = Calendar.getInstance()
                    datePickerState.selectedDateMillis?.let { cal.timeInMillis = it }
                    cal.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                    cal.set(Calendar.MINUTE, timePickerState.minute)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    onGameDateTimeChange(cal.timeInMillis)
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text(stringResource(R.string.cancel)) }
            }
        ) {
            TimePicker(state = timePickerState, modifier = Modifier.padding(16.dp))
        }
    }

    if (showHomeColorPicker) {
        AdvancedColorPickerDialog(
            initialColor = Color(uiState.homeTeamColorArgb),
            onColorSelected = { onHomeColorSelected(it) },
            onDismiss = { showHomeColorPicker = false }
        )
    }
    if (showAwayColorPicker) {
        AdvancedColorPickerDialog(
            initialColor = Color(uiState.awayTeamColorArgb),
            onColorSelected = { onAwayColorSelected(it) },
            onDismiss = { showAwayColorPicker = false }
        )
    }
}

@Composable
private fun RosterManagementSection(
    teamName: String,
    team: Team,
    roster: List<Player>,
    officials: List<TeamOfficial>,
    onApplyTemplate: (Team, Int, Int) -> Unit,
    onAddPlayer: (Team, Int, String) -> Unit,
    onRemovePlayer: (Team, String) -> Unit,
    onToggleCaptain: (Team, String) -> Unit,
    onToggleOnField: (Team, String) -> Unit,
    onUpdatePlayerNumber: (Team, String, Int) -> Unit,
    onUpdatePlayerName: (Team, String, String) -> Unit,
    onAddOfficial: (Team, String, String) -> Unit,
    onRemoveOfficial: (Team, String) -> Unit
) {
    var showAddPlayerDialog by remember { mutableStateOf(false) }
    var showAddOfficialDialog by remember { mutableStateOf(false) }

    FormCard(title = "${stringResource(R.string.tab_roster)}: $teamName") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { onApplyTemplate(team, 11, 5) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Surface, contentColor = TextPrimary),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Border)
            ) {
                Text(stringResource(R.string.template_11_5), style = MaterialTheme.typography.bodySmall)
            }
            Button(
                onClick = { onApplyTemplate(team, 7, 6) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Surface, contentColor = TextPrimary),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Border)
            ) {
                Text(stringResource(R.string.template_7_6), style = MaterialTheme.typography.bodySmall)
            }
        }

        Spacer(Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            roster.filter { it.onField }.forEach { player ->
                PlayerRow(
                    player = player,
                    onRemove = { onRemovePlayer(team, player.id) },
                    onToggleCaptain = { onToggleCaptain(team, player.id) },
                    onToggleOnField = { onToggleOnField(team, player.id) },
                    onUpdateNumber = { onUpdatePlayerNumber(team, player.id, it) },
                    onUpdateName = { onUpdatePlayerName(team, player.id, it) }
                )
            }
        }
        
        Spacer(Modifier.height(8.dp))

        // Bench Section
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.on_bench_header), style = MaterialTheme.typography.labelLarge, color = AccentGreen, modifier = Modifier.weight(1f))
            IconButton(onClick = { showAddPlayerDialog = true }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Add, contentDescription = null, tint = AccentGreen)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            roster.filter { !it.onField }.forEach { player ->
                PlayerRow(
                    player = player,
                    onRemove = { onRemovePlayer(team, player.id) },
                    onToggleCaptain = { onToggleCaptain(team, player.id) },
                    onToggleOnField = { onToggleOnField(team, player.id) },
                    onUpdateNumber = { onUpdatePlayerNumber(team, player.id, it) },
                    onUpdateName = { onUpdatePlayerName(team, player.id, it) }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.officials), style = MaterialTheme.typography.labelLarge, color = AccentGreen, modifier = Modifier.weight(1f))
            IconButton(onClick = { showAddOfficialDialog = true }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Add, contentDescription = null, tint = AccentGreen)
            }
        }

        if (officials.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                officials.forEach { official ->
                    OfficialRow(
                        official = official,
                        onRemove = { onRemoveOfficial(team, official.id) }
                    )
                }
            }
        }
    }

    if (showAddPlayerDialog) {
        var numStr by remember { mutableStateOf("") }
        var nameStr by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddPlayerDialog = false },
            title = { Text(stringResource(R.string.add_player)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = numStr,
                        onValueChange = { 
                            val filtered = it.filter { c -> c.isDigit() }
                            if (filtered != "0") numStr = filtered 
                        },
                        label = { Text(stringResource(R.string.player_number_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = nameStr,
                        onValueChange = { nameStr = it },
                        label = { Text(stringResource(R.string.player_name_label)) },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    numStr.toIntOrNull()?.let { onAddPlayer(team, it, nameStr) }
                    showAddPlayerDialog = false
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showAddPlayerDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (showAddOfficialDialog) {
        var nameStr by remember { mutableStateOf("") }
        var roleStr by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddOfficialDialog = false },
            title = { Text(stringResource(R.string.add_official)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = roleStr,
                        onValueChange = { roleStr = it },
                        label = { Text(stringResource(R.string.official_role)) },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = nameStr,
                        onValueChange = { nameStr = it },
                        label = { Text(stringResource(R.string.official_name)) },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onAddOfficial(team, nameStr, roleStr)
                    showAddOfficialDialog = false
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showAddOfficialDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@Composable
private fun PlayerRow(
    player: Player,
    onRemove: () -> Unit,
    onToggleCaptain: () -> Unit,
    onToggleOnField: () -> Unit,
    onUpdateNumber: (Int) -> Unit,
    onUpdateName: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Player Number - In-line editable
        var numberText by remember(player.number) { mutableStateOf(player.number.toString()) }
        androidx.compose.foundation.text.BasicTextField(
            value = numberText,
            onValueChange = {
                val filtered = it.filter { c -> c.isDigit() }
                if (filtered.length <= 2) { // Max 99
                    numberText = filtered
                    filtered.toIntOrNull()?.let { num ->
                        if (num in 1..99) onUpdateNumber(num)
                    }
                }
            },
            modifier = Modifier.width(36.dp),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Bold,
                color = AccentGreen,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            cursorBrush = androidx.compose.ui.graphics.SolidColor(AccentGreen)
        )
        
        Spacer(Modifier.width(8.dp))

        // Player Name - In-line editable
        var nameText by remember(player.name) { mutableStateOf(player.name) }
        androidx.compose.foundation.text.BasicTextField(
            value = nameText,
            onValueChange = {
                nameText = it
                onUpdateName(it)
            },
            modifier = Modifier.weight(1f),
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            singleLine = true,
            cursorBrush = androidx.compose.ui.graphics.SolidColor(AccentGreen),
            decorationBox = { innerTextField ->
                if (nameText.isEmpty()) {
                    Text(
                        text = stringResource(R.string.player_name_label),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
                innerTextField()
            }
        )
        
        IconButton(onClick = onToggleOnField, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = if (player.onField) Icons.Default.SportsFootball else Icons.Default.Chair,
                contentDescription = null,
                tint = if (player.onField) AccentGreen else TextMuted,
                modifier = Modifier.size(18.dp)
            )
        }

        TextButton(onClick = onToggleCaptain, modifier = Modifier.width(32.dp), contentPadding = PaddingValues(0.dp)) {
            Text(text = stringResource(R.string.captain_short), color = if (player.captain) AccentGreen else TextMuted, fontWeight = if (player.captain) FontWeight.Bold else FontWeight.Normal, fontSize = 16.sp)
        }

        IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun OfficialRow(
    official: TeamOfficial,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = official.role, style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Text(text = official.name, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
        }
        IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun HeroHeader(title: String, subtitle: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp)) {
        Text(text = "RefWatch", style = MaterialTheme.typography.labelMedium, color = AccentGreen, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = title, style = MaterialTheme.typography.headlineMedium, color = TextPrimary, fontWeight = FontWeight.ExtraBold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
    }
}

@Composable
private fun FormCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface), border = androidx.compose.foundation.BorderStroke(1.dp, Border)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = title, style = MaterialTheme.typography.labelLarge, color = AccentGreen, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun darkFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
    focusedContainerColor = Surface, unfocusedContainerColor = Surface,
    focusedBorderColor = AccentGreen, unfocusedBorderColor = Border,
    focusedLabelColor = AccentGreen, unfocusedLabelColor = TextMuted,
    focusedPlaceholderColor = TextMuted, unfocusedPlaceholderColor = TextMuted,
    cursorColor = AccentGreen, disabledTextColor = TextMuted,
    disabledContainerColor = Surface, disabledBorderColor = Border,
    disabledLabelColor = TextMuted, disabledPlaceholderColor = TextMuted
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun disabledFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextMuted, unfocusedTextColor = TextMuted,
    focusedContainerColor = Surface.copy(alpha = 0.5f), unfocusedContainerColor = Surface.copy(alpha = 0.5f),
    focusedBorderColor = Border, unfocusedBorderColor = Border,
    focusedLabelColor = TextMuted, unfocusedLabelColor = TextMuted,
    focusedPlaceholderColor = TextMuted, unfocusedPlaceholderColor = TextMuted,
    cursorColor = TextMuted, disabledTextColor = TextMuted,
    disabledContainerColor = Surface.copy(alpha = 0.5f), disabledBorderColor = Border,
    disabledLabelColor = TextMuted, disabledPlaceholderColor = TextMuted
)

@Composable
fun AddEditGameRoute(
    navController: NavController,
    addEditViewModel: AddEditGameViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by addEditViewModel.uiState.collectAsStateWithLifecycle()
    
    // Use the navigation backstack entry directly to get the current arguments
    val currentEntry by navController.currentBackStackEntryAsState()
    val gameId = currentEntry?.arguments?.getString("gameId")

    LaunchedEffect(gameId) {
        Log.d("AddEditGameRoute", "Initializing form for gameId: $gameId")
        addEditViewModel.initializeForm(gameId)
    }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            Log.d("AddEditGameRoute", "Save success detected. Navigating back.")
            Toast.makeText(context, "Spiel erfolgreich gespeichert!", Toast.LENGTH_SHORT).show()
            navController.popBackStack()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            Log.e("AddEditGameRoute", "Error detected: $it")
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

    AddEditGameScreen(
        uiState = uiState,
        onNavigateBack = { navController.popBackStack() },
        onHomeTeamNameChange = addEditViewModel::onHomeTeamNameChange,
        onAwayTeamNameChange = addEditViewModel::onAwayTeamNameChange,
        onHomeTeamAbbrChange = addEditViewModel::onHomeTeamAbbrChange,
        onAwayTeamAbbrChange = addEditViewModel::onAwayTeamAbbrChange,
        onHomeCaptainNumberChange = addEditViewModel::onHomeCaptainNumberChange,
        onAwayCaptainNumberChange = addEditViewModel::onAwayCaptainNumberChange,
        onFieldNumberChange = addEditViewModel::onFieldNumberChange,
        onRefereeAssignmentChange = addEditViewModel::onRefereeAssignmentChange,
        onVenueChange = addEditViewModel::onVenueChange,
        onCompetitionChange = addEditViewModel::onCompetitionChange,
        onMainRefereeChange = addEditViewModel::onMainRefereeChange,
        onAssistantReferee1Change = addEditViewModel::onAssistantReferee1Change,
        onAssistantReferee2Change = addEditViewModel::onAssistantReferee2Change,
        onFourthOfficialChange = addEditViewModel::onFourthOfficialChange,
        onObserverChange = addEditViewModel::onObserverChange,
        onGameDateTimeChange = addEditViewModel::onGameDateTimeChange,
        onHalfDurationChange = addEditViewModel::onHalfDurationChange,
        onHalftimeDurationChange = addEditViewModel::onHalftimeDurationChange,
        onHasExtraTimeChange = addEditViewModel::onHasExtraTimeChange,
        onExtraTimeHalfDurationChange = addEditViewModel::onExtraTimeHalfDurationChange,
        onMaxSubstitutionsChange = addEditViewModel::onMaxSubstitutionsChange,
        onHasTemporaryDismissalsChange = addEditViewModel::onHasTemporaryDismissalsChange,
        onTemporaryDismissalChange = addEditViewModel::onTemporaryDismissalChange,
        onHasPenaltiesChange = addEditViewModel::onHasPenaltiesChange,
        onPenaltyKicksPerTeamChange = addEditViewModel::onPenaltyKicksPerTeamChange,
        onHomeColorSelected = addEditViewModel::onHomeColorSelected,
        onAwayColorSelected = addEditViewModel::onAwayColorSelected,
        onNotesChanged = addEditViewModel::onNotesChanged,
        onHomeScoreChange = addEditViewModel::onHomeScoreChange,
        onAwayScoreChange = addEditViewModel::onAwayScoreChange,
        onApplyTemplate = addEditViewModel::applyRosterTemplate,
        onAddPlayer = addEditViewModel::addPlayer,
        onRemovePlayer = addEditViewModel::removePlayer,
        onToggleCaptain = addEditViewModel::toggleCaptain,
        onToggleOnField = addEditViewModel::toggleOnField,
        onUpdatePlayerNumber = addEditViewModel::updatePlayerNumber,
        onUpdatePlayerName = addEditViewModel::updatePlayerName,
        onAddOfficial = addEditViewModel::addOfficial,
        onRemoveOfficial = addEditViewModel::removeOfficial,
            onSaveGame = {
            Log.d("AddEditGameRoute", "onSaveGame triggered in Route.")
            Toast.makeText(context, "Speichere Spiel...", Toast.LENGTH_SHORT).show()
            addEditViewModel.onSaveGame()
        }
    )
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AddEditGameScreen_Preview() {
    RefWatchMobileTheme {
        AddEditGameScreen(
            uiState = AddEditGameUiState(isEditing = false),
            onNavigateBack = {},
            onHomeTeamNameChange = {}, onAwayTeamNameChange = {},
            onHomeTeamAbbrChange = {}, onAwayTeamAbbrChange = {},
            onHomeCaptainNumberChange = {}, onAwayCaptainNumberChange = {},
            onFieldNumberChange = {}, onRefereeAssignmentChange = {},
            onVenueChange = {}, onCompetitionChange = {},
            onMainRefereeChange = {}, onAssistantReferee1Change = {},
            onAssistantReferee2Change = {}, onFourthOfficialChange = {},
            onObserverChange = {},
            onGameDateTimeChange = {}, onHalfDurationChange = {},
            onHalftimeDurationChange = {},
            onHasExtraTimeChange = {},
            onExtraTimeHalfDurationChange = {},
            onMaxSubstitutionsChange = {},
            onHasTemporaryDismissalsChange = {},
            onTemporaryDismissalChange = {},
            onHasPenaltiesChange = {},
            onPenaltyKicksPerTeamChange = {}, onHomeColorSelected = {},
            onAwayColorSelected = {}, onNotesChanged = {},
            onHomeScoreChange = {}, onAwayScoreChange = {},
            onApplyTemplate = { _, _, _ -> }, onAddPlayer = { _, _, _ -> },
            onRemovePlayer = { _, _ -> }, onToggleCaptain = { _, _ -> },
            onToggleOnField = { _, _ -> }, onUpdatePlayerNumber = { _, _, _ -> },
            onUpdatePlayerName = { _, _, _ -> },
            onAddOfficial = { _, _, _ -> }, onRemoveOfficial = { _, _ -> },
            onSaveGame = {}
        )
    }
}

@Composable
fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    AlertDialog(onDismissRequest = onDismissRequest, title = { Text(stringResource(R.string.select_time)) }, text = content, confirmButton = confirmButton, dismissButton = dismissButton)
}
