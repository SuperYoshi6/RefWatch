package com.databelay.refwatch.wear.presentation.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.Check
import androidx.wear.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.*
import androidx.wear.compose.ui.tooling.preview.WearPreviewFontScales
import com.databelay.refwatch.common.CardType
import com.databelay.refwatch.common.Team
import com.databelay.refwatch.common.Player
import com.databelay.refwatch.common.TeamOfficial
import com.databelay.refwatch.common.theme.RefWatchWearTheme
import com.databelay.refwatch.R
import com.databelay.refwatch.wear.presentation.components.PlayerPicker
import com.databelay.refwatch.wear.presentation.utils.localizedName
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.delay

@Composable
fun LogCardScreen(
    preselectedTeam: Team?,
    cardType: CardType,
    roster: List<Player> = emptyList(),
    officials: List<TeamOfficial> = emptyList(),
    hasTemporaryDismissals: Boolean = false,
    temporaryDismissalMinutes: Int = 0,
    onLogCard: (team: Team, playerNumber: Int, cardType: CardType, applyDismissal: Boolean, isOfficial: Boolean, officialName: String?) -> Unit,
    onCancel: () -> Unit
) {
    var selectedTeam by remember { mutableStateOf(preselectedTeam) }
    var playerNumberString by remember { mutableStateOf("") }
    var isManualEntry by remember { mutableStateOf(roster.isEmpty()) }
    var showDismissalPrompt by remember { mutableStateOf(false) }
    var confirmedPlayerNumber by remember { mutableIntStateOf(0) }

    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    
    if (showDismissalPrompt) {
        AlertDialog(
            visible = true,
            onDismissRequest = { showDismissalPrompt = false },
            title = { Text(stringResource(R.string.apply_temporary_dismissal_q)) },
            text = { Text("${temporaryDismissalMinutes} min") },
            confirmButton = {
                AlertDialogDefaults.ConfirmButton(
                    onClick = {
                        if (preselectedTeam != null) {
                            onLogCard(preselectedTeam, confirmedPlayerNumber, cardType, true, false, null)
                        }
                        showDismissalPrompt = false
                    }
                )
            },
            dismissButton = {
                AlertDialogDefaults.DismissButton(
                    onClick = {
                        if (preselectedTeam != null) {
                            onLogCard(preselectedTeam, confirmedPlayerNumber, cardType, false, false, null)
                        }
                        showDismissalPrompt = false
                    }
                )
            }
        )
    }

    ScreenScaffold {
        if (!isManualEntry && roster.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                val title = if (cardType == CardType.YELLOW) stringResource(R.string.card_yellow_title) else stringResource(R.string.card_red_title)
                Text(title, style = MaterialTheme.typography.labelMedium)
                preselectedTeam?.let {
                    Text(
                        text = it.localizedName(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                val combinedList = remember(roster, officials) {
                    roster + officials.map { 
                        Player(name = "[TR] ${it.name}", number = it.number ?: 0, isOnField = false) 
                    }
                }

                PlayerPicker(
                    players = combinedList,
                    onPlayerSelected = { selection ->
                        if (preselectedTeam != null) {
                            val isOfficial = selection.name.startsWith("[TR]")
                            if (!isOfficial && hasTemporaryDismissals && cardType == CardType.YELLOW && temporaryDismissalMinutes > 0) {
                                confirmedPlayerNumber = selection.number
                                showDismissalPrompt = true
                            } else {
                                onLogCard(preselectedTeam, selection.number, cardType, false, isOfficial, selection.name.removePrefix("[TR] ").trim())
                            }
                        }
                    },
                    onManualEntry = { isManualEntry = true }
                )
            }
        } else {
            // Standard manual entry UI
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }

            Column(
                modifier = Modifier
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceAround
            ) {
                Spacer(modifier = Modifier.height(4.dp))

                val title = if (cardType == CardType.YELLOW) stringResource(R.string.card_yellow_title) else stringResource(R.string.card_red_title)
                Text(title, style = MaterialTheme.typography.titleSmall)
                preselectedTeam?.let {
                    Text(
                        it.localizedName(),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                // Player Number
                OutlinedTextField(
                    value = playerNumberString,
                    onValueChange = {
                        if (it.length <= 3 && it.all { char -> char.isDigit() }) {
                            playerNumberString = it
                        }
                    },
                    label = { Text("") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .padding(horizontal = 32.dp),

                    colors = TextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedContainerColor = Color.Transparent,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unfocusedContainerColor = Color.Transparent,
                        disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        disabledContainerColor = Color.Transparent,
                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )


                // Action Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AlertDialogDefaults.DismissButton(
                        onClick = {
                            if (roster.isNotEmpty()) isManualEntry = false else onCancel()
                        },
                    )
                    if (selectedTeam != null && playerNumberString.isNotBlank())
                        AlertDialogDefaults.ConfirmButton(
                            onClick = {
                                val playerNum = playerNumberString.toIntOrNull()
                                val currentSelectedTeam = selectedTeam 
                                if (currentSelectedTeam != null && playerNum != null && playerNum > 0) {
                                    if (hasTemporaryDismissals && cardType == CardType.YELLOW && temporaryDismissalMinutes > 0) {
                                        confirmedPlayerNumber = playerNum
                                        showDismissalPrompt = true
                                    } else {
                                        onLogCard(currentSelectedTeam, playerNum, cardType, false, false, null)
                                    }
                                } else {
                                    if (currentSelectedTeam == null) {
                                        Toast.makeText(context, context.getString(R.string.no_team_selected), Toast.LENGTH_SHORT)
                                            .show()
                                    } else {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.enter_valid_player_number),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            },
                        )
                    else
                        AlertDialogDefaults.ConfirmButton(
                            onClick = {},
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                            )
                        )

                }
                Spacer(modifier = Modifier.height(12.dp))

            }
        }
    }
}

// --------------------------------------- Previews ----------------------------------------
// -----------------------------------------------------------------------------------------
@Preview(device = "id:wearos_small_round", name = "LogCard SmRnd", showBackground = true)
@Preview(device = "id:wearos_large_round", name = "LogCard LrgRnd", showBackground = true)
@Preview(device = "id:wearos_square", name = "LogCard Sqr", showBackground = false)
@WearPreviewFontScales
@Composable
fun LogCardScreenPreview_Yellow_Home() {
    RefWatchWearTheme {
        LogCardScreen(
            preselectedTeam = Team.HOME,
            cardType = CardType.YELLOW,
            onLogCard = { _, _, _, _, _, _ -> },
            onCancel = {}
        )
    }
}


