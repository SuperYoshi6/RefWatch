package com.databelay.refwatch.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.databelay.refwatch.common.Game
import com.databelay.refwatch.common.GamePhase
import com.databelay.refwatch.common.GoalScoredEvent
import com.databelay.refwatch.common.GoalType
import com.databelay.refwatch.common.SubstitutionEvent
import com.databelay.refwatch.common.Team
import com.databelay.refwatch.common.formatTime
import com.databelay.refwatch.common.hasTimer
import com.databelay.refwatch.common.isPlayablePhase
import com.databelay.refwatch.common.readable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchScreen(
    game: Game,
    onNavigateBack: () -> Unit,
    onTakeCurrentTime: () -> Unit,
    onHalfTime: () -> Unit,
    onRecordGoal: (team: Team, goalType: GoalType) -> Unit,
    onLogSubstitution: (team: Team, outgoingPlayerNumber: Int, incomingPlayerNumber: Int) -> Unit
) {
    val view = LocalView.current
    val keepScreenOn = game.currentPhase.hasTimer() || game.isTimerRunning || game.isStoppageTimerRunning
    DisposableEffect(view, keepScreenOn) {
        view.keepScreenOn = keepScreenOn
        onDispose { view.keepScreenOn = false }
    }

    var kickoffCountdownSeconds by remember { mutableStateOf<Int?>(null) }
    var showGoalDialog by remember { mutableStateOf(false) }
    var activeGoalTeam by remember { mutableStateOf<Team?>(null) }
    var showSubstitutionDialog by remember { mutableStateOf(false) }
    var activeSubstitutionTeam by remember { mutableStateOf<Team?>(null) }

    // Handle kickoff countdown
    LaunchedEffect(game.actualTimeElapsedInPeriodMillis, game.isTimerRunning) {
        if (game.actualTimeElapsedInPeriodMillis == 0L && !game.isTimerRunning && game.currentPhase.isPlayablePhase()) {
            // Start countdown if needed
            for (i in 5 downTo 1) {
                kickoffCountdownSeconds = i
                delay(1000)
            }
            kickoffCountdownSeconds = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${game.homeTeamName} vs ${game.awayTeamName}") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // Score Display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = game.homeTeamAbbr ?: game.homeTeamName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            activeGoalTeam = Team.HOME
                            showGoalDialog = true
                        },
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "${game.homeScore} : ${game.awayScore}",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Text(
                    text = game.awayTeamAbbr ?: game.awayTeamName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            activeGoalTeam = Team.AWAY
                            showGoalDialog = true
                        },
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Team antippen für Torart, Wechsel über die Buttons",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Schnell hinzufügen: Team + Torart wählen",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            TeamGoalQuickSelectRow(
                homeTeamName = game.homeTeamAbbr ?: game.homeTeamName,
                awayTeamName = game.awayTeamAbbr ?: game.awayTeamName,
                onTeamGoalTypeSelected = { team, type -> onRecordGoal(team, type) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = {
                        activeSubstitutionTeam = Team.HOME
                        showSubstitutionDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text("Wechsel H")
                }
                Button(
                    onClick = {
                        activeSubstitutionTeam = Team.AWAY
                        showSubstitutionDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text("Wechsel A")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Timer Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (kickoffCountdownSeconds != null) {
                        Text(
                            text = kickoffCountdownSeconds.toString(),
                            style = MaterialTheme.typography.displayLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text("Anstoß", style = MaterialTheme.typography.labelLarge)
                    } else {
                        val regulationDuration = remember(game.currentPhase) { game.regulationPeriodDurationMillis() }
                        val isPlayablePhaseAndInAddedTime = game.currentPhase.isPlayablePhase() &&
                                game.actualTimeElapsedInPeriodMillis >= regulationDuration &&
                                regulationDuration > 0

                        val mainTimerText = if (isPlayablePhaseAndInAddedTime) {
                            game.actualTimeElapsedInPeriodMillis.formatTime()
                        } else {
                            (regulationDuration - game.actualTimeElapsedInPeriodMillis).coerceAtLeast(0).formatTime()
                        }

                        if (game.actualTimeElapsedInPeriodMillis == 0L && !game.isTimerRunning && game.currentPhase.isPlayablePhase()) {
                            Button(
                                onClick = { /* TODO: onKickOff */ },
                                modifier = Modifier.size(100.dp),
                                shape = androidx.compose.foundation.shape.CircleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text(
                                    "Anstoß",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            Text(
                                text = mainTimerText,
                                style = MaterialTheme.typography.displayLarge,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    isPlayablePhaseAndInAddedTime -> Color.Red
                                    game.isTimerRunning -> Color.White
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }

                        if (game.stoppageTimeMillis > 0) {
                            Text(
                                text = "+${game.stoppageTimeMillis.formatTime()}",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color(0xFF00E676)
                            )
                        }
                    }
                }
            }

            // Control Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = onTakeCurrentTime,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text("Take Current")
                }

                Button(
                    onClick = onHalfTime,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary
                    )
                ) {
                    Text("Half Time")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Phase Info
            Text(
                text = game.currentPhase.readable(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }

    if (showGoalDialog && activeGoalTeam != null) {
        GoalInputDialog(
            teamName = if (activeGoalTeam == Team.HOME) game.homeTeamName else game.awayTeamName,
            team = activeGoalTeam!!,
            onGoalTypeSelected = { goalType ->
                onRecordGoal(activeGoalTeam!!, goalType)
                showGoalDialog = false
                activeGoalTeam = null
            },
            onDismiss = {
                showGoalDialog = false
                activeGoalTeam = null
            }
        )
    }

    if (showSubstitutionDialog && activeSubstitutionTeam != null) {
        SubstitutionInputDialog(
            teamName = if (activeSubstitutionTeam == Team.HOME) game.homeTeamName else game.awayTeamName,
            onLogSubstitution = { outgoing, incoming ->
                onLogSubstitution(activeSubstitutionTeam!!, outgoing, incoming)
                showSubstitutionDialog = false
                activeSubstitutionTeam = null
            },
            onDismiss = {
                showSubstitutionDialog = false
                activeSubstitutionTeam = null
            }
        )
    }
}

@Composable
private fun TeamGoalQuickSelectRow(
    homeTeamName: String,
    awayTeamName: String,
    onTeamGoalTypeSelected: (team: Team, goalType: GoalType) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Schnelltor Home",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf(
                GoalType.OPEN_PLAY to "Feldtor",
                GoalType.PENALTY to "Strafstoß",
                GoalType.OWN_GOAL to "Eigentor"
            ).forEach { (goalType, label) ->
                Card(
                    modifier = Modifier
                        .clickable { onTeamGoalTypeSelected(Team.HOME, goalType) }
                        .padding(4.dp),
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Schnelltor Away",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf(
                GoalType.OPEN_PLAY to "Feldtor",
                GoalType.PENALTY to "Strafstoß",
                GoalType.OWN_GOAL to "Eigentor"
            ).forEach { (goalType, label) ->
                Card(
                    modifier = Modifier
                        .clickable { onTeamGoalTypeSelected(Team.AWAY, goalType) }
                        .padding(4.dp),
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun SubstitutionInputDialog(
    teamName: String,
    onLogSubstitution: (outgoing: Int, incoming: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var outgoingNumber by remember { mutableStateOf("") }
    var incomingNumber by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Wechsel für $teamName") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = outgoingNumber,
                    onValueChange = { outgoingNumber = it.filter { char -> char.isDigit() } },
                    label = { Text("Auswechslender Spieler #") },
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true
                )
                OutlinedTextField(
                    value = incomingNumber,
                    onValueChange = { incomingNumber = it.filter { char -> char.isDigit() } },
                    label = { Text("Eingesetzter Spieler #") },
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val outgoing = outgoingNumber.toIntOrNull()
                    val incoming = incomingNumber.toIntOrNull()
                    if (outgoing != null && incoming != null) {
                        onLogSubstitution(outgoing, incoming)
                    }
                },
                enabled = outgoingNumber.isNotBlank() && incomingNumber.isNotBlank()
            ) {
                Text("Speichern")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}
