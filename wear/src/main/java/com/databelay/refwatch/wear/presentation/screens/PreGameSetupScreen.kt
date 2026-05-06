package com.databelay.refwatch.wear.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.databelay.refwatch.R
import com.databelay.refwatch.common.theme.RefWatchWearTheme
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.CompactButton
import androidx.wear.compose.material.OutlinedChip
import androidx.wear.compose.material3.AlertDialogDefaults
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Dialog
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.ScrollIndicator
import androidx.wear.compose.material3.Text
import androidx.wear.compose.ui.tooling.preview.WearPreviewFontScales
import com.databelay.refwatch.common.Game

@Composable
fun PreGameSetupScreen(
    game: Game?,
    onEditHomeTeamNameClick: () -> Unit,
    onEditAwayTeamNameClick: () -> Unit,
    onEditHomeTeamAbbrClick: () -> Unit,
    onEditAwayTeamAbbrClick: () -> Unit,
    onEditHomeCaptainClick: () -> Unit,
    onEditAwayCaptainClick: () -> Unit,
    onHomeColorPickerClick: () -> Unit,
    onAwayColorPickerClick: () -> Unit,
    onSetHalfDuration: (Int) -> Unit,
    onSetHalftimeDuration: (Int) -> Unit,
    onSetMaxSubstitutions: (Int) -> Unit,
    onCreateMatchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberScalingLazyListState()
    val homeTeamName = game?.homeTeamName ?: stringResource(R.string.home)
    val awayTeamName = game?.awayTeamName ?: stringResource(R.string.away)
    val homeTeamAbbr = game?.homeTeamAbbr ?: "HOM"
    val awayTeamAbbr = game?.awayTeamAbbr ?: "AWA"
    val homeCaptain = game?.homeCaptainNumber?.toString() ?: stringResource(R.string.none)
    val awayCaptain = game?.awayCaptainNumber?.toString() ?: stringResource(R.string.none)
    val homeTeamColor = game?.homeTeamColor ?: Color.Gray
    val awayTeamColor = game?.awayTeamColor ?: Color.LightGray
    val halfDurationMinutes = game?.halfDurationMinutes ?: 45
    val halftimeDurationMinutes = game?.halftimeDurationMinutes ?: 15
    val maxSubstitutionsAllowed = game?.maxSubstitutionsAllowed ?: 5

    ScreenScaffold(
        scrollIndicator = {
            ScrollIndicator(
                modifier = Modifier.align(Alignment.CenterEnd),
                state = listState
            )
        },
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 0.dp, horizontal = 2.dp),

        ) { contentPadding ->
        ScalingLazyColumn(
            state = listState, contentPadding = contentPadding,

            ) {
            item {
                ListHeader(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        stringResource(R.string.match_setup),
                        style = MaterialTheme.typography.titleSmall,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            // Team Names Section
            item {
                Text(
                    stringResource(R.string.home_away_teams),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceAround,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    OutlinedChip(
                        onClick = onEditHomeTeamNameClick,
                        label = {
                            Text(
                                homeTeamName,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        icon = {
                            Icon(
                                Icons.Default.Edit,
                                modifier = Modifier.size(12.dp),
                                contentDescription = stringResource(R.string.edit_team_name)
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
//                        Spacer(modifier = Modifier.width(8.dp))
                    OutlinedChip(
                        onClick = onEditAwayTeamNameClick,
                        label = {
                            Text(
                                awayTeamName,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        icon = {
                            Icon(
                                Icons.Default.Edit,
                                modifier = Modifier.size(12.dp),
                                contentDescription = stringResource(R.string.edit_team_name)
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Team Abbr Editors
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceAround,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    OutlinedChip(
                        onClick = onEditHomeTeamAbbrClick,
                        label = {
                            Text(
                                homeTeamAbbr,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        icon = {
                            Icon(
                                Icons.Default.Edit,
                                modifier = Modifier.size(12.dp),
                                contentDescription = stringResource(R.string.edit_team_abbr)
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedChip(
                        onClick = onEditAwayTeamAbbrClick,
                        label = {
                            Text(
                                awayTeamAbbr,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        icon = {
                            Icon(
                                Icons.Default.Edit,
                                modifier = Modifier.size(12.dp),
                                contentDescription = stringResource(R.string.edit_team_abbr)
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Captain Editors
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceAround,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    OutlinedChip(
                        onClick = onEditHomeCaptainClick,
                        label = {
                            Text(
                                "C: $homeCaptain",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        icon = {
                            Icon(
                                Icons.Default.Edit,
                                modifier = Modifier.size(12.dp),
                                contentDescription = stringResource(R.string.edit_home_captain)
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedChip(
                        onClick = onEditAwayCaptainClick,
                        label = {
                            Text(
                                "C: $awayCaptain",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        icon = {
                            Icon(
                                Icons.Default.Edit,
                                modifier = Modifier.size(12.dp),
                                contentDescription = stringResource(R.string.edit_away_captain)
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Jersey Colors
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    ColorPickerButton(
                        stringResource(R.string.home),
                        homeTeamColor,
                        onClick = onHomeColorPickerClick
                    )
                    ColorPickerButton(
                        stringResource(R.string.away),
                        awayTeamColor,
                        onClick = onAwayColorPickerClick
                    )
                }
            }

            // Half Duration
            item {
                DurationSettingStepper(
                    label = stringResource(R.string.half_duration),
                    currentValue = halfDurationMinutes,
                    onValueChange = onSetHalfDuration,
                    valueRange = 1..120,
                    step = 1
                )
            }

            // Halftime Duration
            item {
                DurationSettingStepper(
                    label = stringResource(R.string.halftime_duration),
                    currentValue = halftimeDurationMinutes,
                    onValueChange = onSetHalftimeDuration,
                    valueRange = 1..60,
                    step = 1
                )
            }

            // Max Substitutions
            item {
                DurationSettingStepper(
                    label = stringResource(R.string.max_substitutions),
                    currentValue = maxSubstitutionsAllowed,
                    onValueChange = onSetMaxSubstitutions,
                    valueRange = 1..11,
                    step = 1
                )
            }

            // Create Match Button
            item {
                EdgeButton(
                    onClick = onCreateMatchClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.create_match))
                }
            }
        }
    }
}


@Composable
fun TeamNameEditDialogContent(
    teamLabel: String,
    text: String,
    onTextChange: (String) -> Unit,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 12.dp)
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            teamLabel,
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        TextField(
            value = text,
            onValueChange = onTextChange,
            label = { Text(stringResource(R.string.edit_team_name)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = "Save",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable {
                        val cleaned = text.trim()
                        if (cleaned.isNotBlank()) onSave(cleaned)
                    }
                )
            },
            colors = androidx.compose.material3.TextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.outline
            ),
            keyboardOptions = KeyboardOptions.Default.copy(
                capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Words,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    val cleaned = text.trim()
                    if (cleaned.isNotBlank()) {
                        onSave(cleaned)
                    }
                }
            )
        )
        Spacer(modifier = Modifier.weight(1f))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            modifier = Modifier.fillMaxWidth()
        ) {
            AlertDialogDefaults.DismissButton(onClick = onDismiss)
            AlertDialogDefaults.ConfirmButton(
                onClick = {
                    val cleaned = text.trim()
                    if (cleaned.isNotBlank()) onSave(cleaned)
                }
            )
        }
    }
}

@Composable
fun NumberEditDialogContent(
    label: String,
    text: String,
    onTextChange: (String) -> Unit,
    onSave: (Int?) -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 12.dp)
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        TextField(
            value = text,
            onValueChange = { onTextChange(it.filter { char -> char.isDigit() }) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = androidx.compose.material3.TextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.outline
            ),
            trailingIcon = {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = "Save",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onSave(text.toIntOrNull()) }
                )
            },
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    onSave(text.toIntOrNull())
                }
            )
        )
        Spacer(modifier = Modifier.weight(1f))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            modifier = Modifier.fillMaxWidth()
        ) {
            AlertDialogDefaults.DismissButton(onClick = onDismiss)
            AlertDialogDefaults.ConfirmButton(onClick = { onSave(text.toIntOrNull()) })
        }
    }
}


/**
 * A dialog Composable for editing a team's name.
 * This remains available for the parent composable to use.
 */
@Composable
fun TeamNameEditDialog(
    teamLabel: String,
    initialValue: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember(initialValue) { mutableStateOf(initialValue) }
    Dialog(
        visible = true,
        onDismissRequest = onDismiss,
    ) {
        TeamNameEditDialogContent(
            teamLabel = teamLabel,
            text = text,
            onTextChange = { text = it },
            onSave = onSave,
            onDismiss = onDismiss,
        )
    }
}


@Composable
fun NumberEditDialog(
    label: String,
    initialValue: String,
    onSave: (Int?) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember(initialValue) { mutableStateOf(initialValue) }
    Dialog(
        visible = true,
        onDismissRequest = {
            onSave(text.toIntOrNull())
        },
    ) {
        NumberEditDialogContent(
            label = label,
            text = text,
            onTextChange = { text = it },
            onSave = onSave,
            onDismiss = onDismiss,
        )
    }
}

@Composable
fun ColorPickerButton(label: String, currentColor: Color, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(currentColor)
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    CircleShape
                )
        )
    }
}

/**
 * A dialog Composable for picking a color.
 * This remains available for the parent composable to use.
 */
@Composable
fun SimpleColorPickerDialog(
    title: String,
    availableColors: List<Color>,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        true,
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            val listState = rememberScalingLazyListState()
            ScalingLazyColumn(
                state = listState,
                contentPadding = PaddingValues(vertical = 8.dp, horizontal = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(availableColors.chunked(3)) { rowColors ->
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        rowColors.forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .padding(4.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        CircleShape
                                    )
                                    .clickable { onColorSelected(color) }
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth(0.7f)) {
                Text(stringResource(R.string.dismiss))
            }
        }
    }
}

@Composable
fun DurationSettingStepper(
    label: String,
    currentValue: Int,
    onValueChange: (Int) -> Unit,
    valueRange: IntRange = 1..60,
    step: Int = 5
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleSmall
        ) // Consider a smaller style if too large e.g. titleMedium
        Spacer(Modifier.height(2.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            CompactButton(
                onClick = {
                    if (currentValue - step >= valueRange.first) onValueChange(
                        currentValue - step
                    )
                },
                modifier = Modifier.size(40.dp)
            ) { Text("-", fontSize = 18.sp) }

            Text(
                text = "$currentValue",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .defaultMinSize(minWidth = 60.dp),
                textAlign = TextAlign.Center
            )
            CompactButton(
                onClick = {
                    if (currentValue + step <= valueRange.last) onValueChange(
                        currentValue + step
                    )
                },
                modifier = Modifier.size(40.dp)
            ) { Text("+", fontSize = 18.sp) }
        }
    }
}

// --------------------------------------- Previews ----------------------------------------
@Preview(device = "id:wearos_small_round", showBackground = true)
@Preview(device = "id:wearos_square", showBackground = true)
@Preview(device = "id:wearos_large_round", showBackground = true)
@WearPreviewFontScales
@Composable
fun PreviewPreGameSetupScreen() {
    RefWatchWearTheme {
        // Create a sample Game object for the preview
        val sampleGame = Game.defaults().copy(
            homeTeamName = "Spartans",
            awayTeamName = "Vikings",
            halfDurationMinutes = 40,
            halftimeDurationMinutes = 15
        )
        PreGameSetupScreen(
            game = sampleGame,
            onEditHomeTeamNameClick = {},
            onEditAwayTeamNameClick = {},
            onEditHomeTeamAbbrClick = {},
            onEditAwayTeamAbbrClick = {},
            onEditHomeCaptainClick = {},
            onEditAwayCaptainClick = {},
            onHomeColorPickerClick = {},
            onAwayColorPickerClick = {},
            onSetHalfDuration = {},
            onSetHalftimeDuration = {},
            onSetMaxSubstitutions = {},
            onCreateMatchClick = {}
        )
    }
}
/*

@Preview(device = "id:wearos_small_round",showBackground = true)
@Preview(device = "id:wearos_square", showBackground = true)
@Preview(device = "id:wearos_large_round",showBackground = true)
@WearPreviewFontScales
@Composable
fun PreviewTeamNameEditDialog_Home() {
    RefWatchWearTheme {
        AppScaffold() {
            TeamNameEditDialogContent(
                teamLabel = "Home",
                initialValue = "Warriors",
                onSave = { },
                onDismiss = { }
            )
        }
    }
}*/
