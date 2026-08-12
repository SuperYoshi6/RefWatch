package com.databelay.refwatch.wear.presentation.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.AlertDialogDefaults
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.Dialog
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.databelay.refwatch.R
import com.databelay.refwatch.common.Team
import com.databelay.refwatch.common.Player
import com.databelay.refwatch.wear.presentation.components.PlayerPicker

/**
 * Two-window substitution flow.
 *
 * The referee double-taps a team to open this dialog. The flow is split into
 * two distinct windows so the watch UI gives a clear visual confirmation that
 * the first step was accepted before the second prompt appears:
 *
 *   Window 1 — "Outgoing Player": type the number of the player leaving the
 *              field, press the confirm button (or Enter). On submit, the
 *              outgoing number is captured and window 1 closes.
 *
 *   Window 2 — "Incoming Player": opens immediately after window 1 is
 *              confirmed. The incoming number is captured and the dialog
 *              commits the substitution.
 *
 * Both windows use the same OutlinedTextField + confirm/dismiss layout so
 * the muscle memory is identical. The internal state in the wrapper Composable
 * (outgoingNumber + step) decides which window is currently shown.
 *
 * Auto-focus + soft-keyboard are reset every time a new window appears so
 * the referee never has to tap the field again.
 */
@Composable
fun QuickSubstitutionDialog(
    team: Team,
    roster: List<Player> = emptyList(),
    onConfirm: (outgoing: Int, incoming: Int) -> Unit,
    onDismiss: () -> Unit
) {
    // The outgoing number captured in window 1, reused when window 2 submits.
    var outgoingNumber by remember { mutableStateOf<Int?>(null) }
    var isManualEntry by remember { mutableStateOf(roster.isEmpty()) }

    val outgoing = outgoingNumber
    if (outgoing == null) {
        OutgoingPlayerDialog(
            team = team,
            roster = roster.filter { it.isOnField },
            isManualEntry = isManualEntry,
            onSetManual = { isManualEntry = true },
            onSubmit = { outNum ->
                // Capture and switch to window 2.
                outgoingNumber = outNum
                if (roster.isNotEmpty()) isManualEntry = false
            },
            onDismiss = onDismiss
        )
    } else {
        IncomingPlayerDialog(
            team = team,
            roster = roster.filter { !it.isOnField },
            isManualEntry = isManualEntry,
            onSetManual = { isManualEntry = true },
            onSubmit = { inNum ->
                onConfirm(outgoing, inNum)
            },
            onDismiss = {
                outgoingNumber = null
                if (roster.isNotEmpty()) isManualEntry = false
            }
        )
    }
}

/**
 * Window 1: prompt for the outgoing (leaving) player number.
 */
@Composable
private fun OutgoingPlayerDialog(
    team: Team,
    roster: List<Player>,
    isManualEntry: Boolean,
    onSetManual: () -> Unit,
    onSubmit: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val enterNumberPrompt = stringResource(R.string.enter_valid_player_number)
    var number by remember { mutableStateOf("") }

    Dialog(visible = true, onDismissRequest = onDismiss) {
        if (!isManualEntry && roster.isNotEmpty()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.outgoing_player), style = MaterialTheme.typography.labelMedium)
                PlayerPicker(
                    players = roster,
                    onPlayerSelected = { onSubmit(it.number) },
                    onManualEntry = onSetManual
                )
            }
        } else {
            SubstitutionEntryColumn(
                team = team,
                title = stringResource(R.string.substitution),
                prompt = stringResource(R.string.outgoing_player),
                value = number,
                onValueChange = { number = it.filter { c -> c.isDigit() }.take(3) },
                onSubmit = {
                    val n = number.toIntOrNull()
                    if (n != null) {
                        onSubmit(n)
                    } else {
                        Toast.makeText(context, enterNumberPrompt, Toast.LENGTH_SHORT).show()
                    }
                },
                onDismiss = onDismiss
            )
        }
    }
}

/**
 * Window 2: prompt for the incoming (entering) player number.
 * Identical layout to the outgoing dialog — only the prompt text differs.
 */
@Composable
private fun IncomingPlayerDialog(
    team: Team,
    roster: List<Player>,
    isManualEntry: Boolean,
    onSetManual: () -> Unit,
    onSubmit: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val enterNumberPrompt = stringResource(R.string.enter_valid_player_number)
    var number by remember { mutableStateOf("") }

    Dialog(visible = true, onDismissRequest = onDismiss) {
        if (!isManualEntry && roster.isNotEmpty()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.incoming_player), style = MaterialTheme.typography.labelMedium)
                PlayerPicker(
                    players = roster,
                    onPlayerSelected = { onSubmit(it.number) },
                    onManualEntry = onSetManual
                )
            }
        } else {
            SubstitutionEntryColumn(
                team = team,
                title = stringResource(R.string.substitution),
                prompt = stringResource(R.string.incoming_player),
                value = number,
                onValueChange = { number = it.filter { c -> c.isDigit() }.take(3) },
                onSubmit = {
                    val n = number.toIntOrNull()
                    if (n != null) {
                        onSubmit(n)
                    } else {
                        Toast.makeText(context, enterNumberPrompt, Toast.LENGTH_SHORT).show()
                    }
                },
                onDismiss = onDismiss
            )
        }
    }
}

/**
 * Shared column layout for both windows. Auto-focuses the field and shows
 * the soft keyboard as soon as the dialog enters composition, so the referee
 * can start typing right away — no need to tap the field.
 */
@Composable
private fun SubstitutionEntryColumn(
    team: Team,
    title: String,
    prompt: String,
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = prompt,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        val focusRequester = remember { FocusRequester() }
        val keyboard = LocalSoftwareKeyboardController.current
        LaunchedEffect(Unit) {
            // Show the soft keyboard and focus the field automatically so the
            // referee can start typing the player number right away.
            keyboard?.show()
            focusRequester.requestFocus()
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(stringResource(R.string.enter_number)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AlertDialogDefaults.DismissButton(onClick = onDismiss)
            AlertDialogDefaults.ConfirmButton(onClick = onSubmit)
        }
    }
}
