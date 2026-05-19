package com.databelay.refwatch.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.databelay.refwatch.common.GoalType
import com.databelay.refwatch.common.Team

/**
 * Dialog zum Auswählen der Torart nach Drücken auf Team-Namen
 */
@Composable
fun GoalInputDialog(
    teamName: String,
    team: Team,
    onGoalTypeSelected: (goalType: GoalType) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedGoalType by remember { mutableStateOf<GoalType?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Torart für $teamName")
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Schließen")
                }
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Überschrift
                Text(
                    "Bitte wählen Sie die Torart:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Radio Button Optionen
                val goalTypes = listOf(
                    GoalType.OPEN_PLAY to "Feldtor",
                    GoalType.PENALTY to "Strafstoß",
                    GoalType.OWN_GOAL to "Eigentor"
                )

                goalTypes.forEach { (goalType, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedGoalType = goalType
                            }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RadioButton(
                            selected = selectedGoalType == goalType,
                            onClick = { selectedGoalType = goalType }
                        )
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedGoalType?.let {
                        onGoalTypeSelected(it)
                        onDismiss()
                    }
                },
                enabled = selectedGoalType != null
            ) {
                Text("Bestätigen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

/**
 * Compact Version für schnelle Eingabe während des Spiels
 */
@Composable
fun GoalTypeQuickSelect(
    onGoalTypeSelected: (goalType: GoalType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        listOf(
            GoalType.OPEN_PLAY to "⚽",
            GoalType.PENALTY to "⚡",
            GoalType.OWN_GOAL to "❌"
        ).forEach { (type, emoji) ->
            Card(
                modifier = Modifier
                    .clickable { onGoalTypeSelected(type) }
                    .padding(4.dp),
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    emoji,
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        }
    }
}
