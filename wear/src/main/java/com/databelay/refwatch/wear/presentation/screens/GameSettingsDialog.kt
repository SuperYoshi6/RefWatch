package com.databelay.refwatch.wear.presentation.screens // Or your chosen package

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PauseCircleFilled
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.dialog.Confirmation
import androidx.wear.compose.material.dialog.Dialog
import com.databelay.refwatch.BuildConfig
import com.databelay.refwatch.common.Game
import com.databelay.refwatch.common.GamePhase
import com.databelay.refwatch.common.Team
import com.databelay.refwatch.common.hasTimer
import com.databelay.refwatch.common.readable
import androidx.wear.compose.material.dialog.Confirmation // For simple Yes/No
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue // if not already there
import androidx.compose.runtime.getValue // if not already there
import androidx.compose.runtime.saveable.rememberSaveable // To keep dialog state on config changes

@Composable
fun GameSettingsDialog(
    game: Game,
    onDismiss: () -> Unit,
    onFinishGame: () -> Unit,
    onResetPeriodTimer: () -> Unit,
    onResetGame: () -> Unit,
    onViewLog: () -> Unit,
    onToggleTimer: () -> Unit,
    onEndPhase: () -> Unit,
) {
    Dialog(
        showDialog = true,
        onDismissRequest = onDismiss
    ) {
        // State to control the visibility of the reset confirmation dialog
        var showResetConfirmationDialog by rememberSaveable { mutableStateOf(false) }
        val listState = rememberScalingLazyListState()
        ScalingLazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 16.dp, horizontal = 16.dp), // Adjusted padding
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically)
        ) {
            item {
                Text("Menu", style = MaterialTheme.typography.title3)
            }

            // Play/Pause Button - only if game is active (not PRE_GAME or FULL_TIME)
            if (game.currentPhase.hasTimer()) {
                item {
                    // Start/Pause Button
                    Button(
                        onClick = onToggleTimer,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color.Green
                        ),
                        modifier = Modifier.size(ButtonDefaults.LargeButtonSize)
                    ) {
                        Icon(
                            imageVector = if (game.isTimerRunning) Icons.Filled.PauseCircleFilled else Icons.Filled.PlayCircleFilled,
                            contentDescription = if (game.isTimerRunning) "Pause Timer" else "Start Timer",
                            modifier = Modifier.size(ButtonDefaults.LargeIconSize)
                        )
                    }
                }
            // End Phase Early Button
                item {
                    Button(
                        onClick = onEndPhase,
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red), // Or a distinct color
                        modifier = Modifier.fillMaxWidth(),
                        ) {
                        Text(
                            text = "End ${game.currentPhase.readable()}",
                            textAlign = TextAlign.Center
                        ) // Adding text
                    }
                }
            }
            item { // Finish Game Button
                Button(
                    onClick = onFinishGame,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Finish Game")}
            }

            item { // Reset/End Game Button
                Button(
                    onClick = onResetPeriodTimer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Reset Period Timer")}
            }
            item { // "Reset Game" Button
                Button(
                    onClick = { showResetConfirmationDialog = true }, // Show confirmation dialog
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = MaterialTheme.colors.error,
                        contentColor = MaterialTheme.colors.onError
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PriorityHigh,
                            contentDescription = "Warning",
                            // Tint will be MaterialTheme.colors.onError due to Button's contentColor
                        )
//                        Spacer(Modifier.size(ButtonDefaults.SmallIconSize))
                        Text("Reset Game")
                    }
                }
            }
            item {
                Button(
                    onClick = onViewLog,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("View Game Log")}
            }
            item {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Close Menu")
                }
            }
        }

        // --- Reset Confirmation Dialog ---
        if (showResetConfirmationDialog) {
            Confirmation(
                onTimeout = { showResetConfirmationDialog = false }, // Or onCancel lambda
                icon = {
                    Icon(
                        Icons.Filled.PriorityHigh,
                        contentDescription = "Warning",
                        tint = MaterialTheme.colors.error, // Make icon red
                        modifier = Modifier.size(48.dp) // Adjust size as needed
                    )
                },
            ) {
                // Use a Column to arrange message and buttons vertically
                Column(
                    modifier = Modifier.fillMaxWidth(), // Fill width for centering content
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp) // Space between message and buttons
                ) {
                    Text( // This is the message
                        text = "Are you sure ? Scores and game logs will be erased. ",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.body2.copy(fontSize = 16.sp),
                        color = MaterialTheme.colors.onBackground, // Ensure good contrast
                        modifier = Modifier.padding(horizontal = 8.dp) // Add some horizontal padding
                    )

                    Spacer(Modifier.height(12.dp)) // Extra space before buttons

                    // Button for "Yes"
                    Button(
                        onClick = {
                            showResetConfirmationDialog = false // Dismiss confirmation
                            onResetGame()                   // Execute the actual reset
                        },
                        colors = ButtonDefaults.primaryButtonColors(), // Standard confirm color
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Yes, Reset")
                    }

                    // Button for "No"
                    Button(
                        onClick = {
                            showResetConfirmationDialog = false // Just dismiss confirmation
                        },
                        colors = ButtonDefaults.secondaryButtonColors(), // Standard cancel color
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("No, Cancel")
                    }
                }
            }
        }
    }
}
// ---------------------- Preview --------------------------------------

@Preview(device = "id:wearos_large_round", showSystemUi = true, backgroundColor = 0xff000000, showBackground = true)
@Composable
fun GameSettingsDialogPreview_TimerRunning() {
    MaterialTheme {
        GameSettingsDialog(
            game = Game.defaults().copy( // Use your Game.defaults() or a sample game
                id = "previewGame",
                currentPhase = GamePhase.FIRST_HALF,
                homeTeamName = "Red Team",
                awayTeamName = "Blue Team",
                homeTeamColorArgb = android.graphics.Color.BLACK,
                awayTeamColorArgb = android.graphics.Color.YELLOW,
                kickOffTeam = Team.AWAY,
                actualTimeElapsedInPeriodMillis = (5 * 60000L) + (2 * 60000L),
                halfDurationMinutes = 45,
                homeScore = 2
            ),
            onDismiss = {}, // Empty lambda for preview
            onFinishGame = {},
            onResetPeriodTimer = {},
            onResetGame = {},
            onViewLog = {},
            onToggleTimer = {},
            onEndPhase = {}
        )
    }
}
