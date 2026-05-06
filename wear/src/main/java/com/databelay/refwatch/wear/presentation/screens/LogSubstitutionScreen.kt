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
import androidx.wear.compose.material3.*
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import com.databelay.refwatch.R
import com.databelay.refwatch.common.Team
import com.databelay.refwatch.common.theme.RefWatchWearTheme
import com.databelay.refwatch.wear.presentation.utils.localizedName
import kotlinx.coroutines.delay

@Composable
fun LogSubstitutionScreen(
    team: Team,
    onLogSubstitution: (outgoing: Int, incoming: Int) -> Unit,
    onCancel: () -> Unit
) {
    var outgoingNumber by remember { mutableStateOf("") }
    var incomingNumber by remember { mutableStateOf("") }
    var isEnteringIncoming by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isEnteringIncoming) {
        delay(100)
        focusRequester.requestFocus()
    }

    ScreenScaffold { _ ->
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (!isEnteringIncoming) stringResource(R.string.outgoing_player) else stringResource(R.string.incoming_player),
                style = MaterialTheme.typography.titleSmall
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
                AlertDialogDefaults.DismissButton(onClick = onCancel)
                
                AlertDialogDefaults.ConfirmButton(
                    onClick = {
                        if (!isEnteringIncoming) {
                            if (outgoingNumber.isNotBlank()) {
                                isEnteringIncoming = true
                            } else {
                                Toast.makeText(context, "Nummer eingeben", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            if (incomingNumber.isNotBlank()) {
                                val outNum = outgoingNumber.toIntOrNull()
                                val inNum = incomingNumber.toIntOrNull()
                                if (outNum != null && inNum != null) {
                                    onLogSubstitution(outNum, inNum)
                                }
                            } else {
                                Toast.makeText(context, "Nummer eingeben", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }
        }
    }
}
