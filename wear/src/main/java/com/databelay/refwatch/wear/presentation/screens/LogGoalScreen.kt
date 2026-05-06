package com.databelay.refwatch.wear.presentation.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.*
import com.databelay.refwatch.R
import com.databelay.refwatch.common.GoalType
import com.databelay.refwatch.common.Team
import com.databelay.refwatch.wear.presentation.utils.localizedName

@Composable
fun LogGoalScreen(
    preselectedTeam: Team?,
    goalType: GoalType,
    onLogGoal: (team: Team, playerNumber: Int?, goalType: GoalType) -> Unit,
    onCancel: () -> Unit
) {
    var playerNumberString by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val context = LocalContext.current

    ScreenScaffold {
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceAround
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${stringResource(R.string.goal_regular)} - ${goalType.localizedName()}",
                style = MaterialTheme.typography.titleSmall
            )
            
            preselectedTeam?.let {
                Text(
                    text = it.localizedName(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            // Player Number
            OutlinedTextField(
                value = playerNumberString,
                onValueChange = {
                    if (it.length <= 2 && it.all { char -> char.isDigit() }) {
                        playerNumberString = it
                    }
                },
                label = { Text("Nr.") },
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
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AlertDialogDefaults.DismissButton(onClick = onCancel)
                
                AlertDialogDefaults.ConfirmButton(
                    onClick = {
                        val playerNum = playerNumberString.toIntOrNull()
                        if (preselectedTeam != null) {
                            onLogGoal(preselectedTeam, playerNum, goalType)
                        } else {
                            Toast.makeText(context, "No team selected", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
