package com.databelay.refwatch.wear.presentation.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.*
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import com.databelay.refwatch.R
import com.databelay.refwatch.common.Team
import com.databelay.refwatch.common.Player
import com.databelay.refwatch.common.theme.RefWatchWearTheme
import com.databelay.refwatch.wear.presentation.components.PlayerPicker
import com.databelay.refwatch.wear.presentation.utils.localizedName
import kotlinx.coroutines.delay

@Composable
fun LogSubstitutionScreen(
    team: Team,
    roster: List<Player> = emptyList(),
    onLogSubstitution: (outgoing: Int, incoming: Int) -> Unit,
    onCancel: () -> Unit
) {
    var outgoingNumber by remember { mutableStateOf("") }
    var incomingNumber by remember { mutableStateOf("") }
    var isEnteringIncoming by remember { mutableStateOf(false) }
    var isManualEntry by remember { mutableStateOf(roster.isEmpty()) }
    
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }

    val currentRosterSource = remember(isEnteringIncoming, roster) {
        if (!isEnteringIncoming) {
            roster.filter { it.isOnField }
        } else {
            roster.filter { !it.isOnField }
        }
    }

    LaunchedEffect(isEnteringIncoming, isManualEntry) {
        if (isManualEntry) {
            delay(100)
            focusRequester.requestFocus()
        }
    }

    ScreenScaffold { _ ->
        if (!isManualEntry && roster.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (!isEnteringIncoming) "RAUS (FELD)" else "REIN (BANK)",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (!isEnteringIncoming) Color.White else Color.Green
                )
                
                PlayerPicker(
                    players = currentRosterSource,
                    onPlayerSelected = { player ->
                        if (!isEnteringIncoming) {
                            outgoingNumber = player.number.toString()
                            isEnteringIncoming = true
                        } else {
                            onLogSubstitution(outgoingNumber.toInt(), player.number)
                        }
                    },
                    onManualEntry = { isManualEntry = true }
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (!isEnteringIncoming) "${stringResource(R.string.outgoing_player)} (FELD)" else "${stringResource(R.string.incoming_player)} (BANK)",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (!isEnteringIncoming) Color.White else Color.Green
                )
                
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = if (!isEnteringIncoming) outgoingNumber else incomingNumber,
                    onValueChange = { newValue ->
                        if (newValue.length <= 3 && newValue.all { char -> char.isDigit() }) {
                            if (!isEnteringIncoming) outgoingNumber = newValue else incomingNumber = newValue
                        }
                    },
                    modifier = Modifier.focusRequester(focusRequester).width(80.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onDone = {
                            if (!isEnteringIncoming) {
                                if (outgoingNumber.isNotBlank()) {
                                    isEnteringIncoming = true
                                    if (roster.isNotEmpty()) isManualEntry = false
                                }
                            } else {
                                val outNum = outgoingNumber.toIntOrNull()
                                val inNum = incomingNumber.toIntOrNull()
                                if (outNum != null && inNum != null) {
                                    onLogSubstitution(outNum, inNum)
                                }
                            }
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AlertDialogDefaults.DismissButton(onClick = {
                        if (isEnteringIncoming) {
                            isEnteringIncoming = false
                            if (roster.isNotEmpty()) isManualEntry = false
                        } else if (roster.isNotEmpty() && isManualEntry) {
                            isManualEntry = false
                        } else {
                            onCancel()
                        }
                    })
                    
                    AlertDialogDefaults.ConfirmButton(
                        onClick = {
                            if (!isEnteringIncoming) {
                                if (outgoingNumber.isNotBlank()) {
                                    isEnteringIncoming = true
                                    if (roster.isNotEmpty()) isManualEntry = false
                                } else {
                                    Toast.makeText(context, context.getString(R.string.enter_number_toast), Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                if (incomingNumber.isNotBlank()) {
                                    val outNum = outgoingNumber.toIntOrNull()
                                    val inNum = incomingNumber.toIntOrNull()
                                    if (outNum != null && inNum != null) {
                                        onLogSubstitution(outNum, inNum)
                                    }
                                } else {
                                    Toast.makeText(context, context.getString(R.string.enter_number_toast), Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
