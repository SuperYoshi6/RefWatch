package com.databelay.refwatch.wear.presentation.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CompactButton
import androidx.wear.compose.material.ButtonDefaults as M2ButtonDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material3.*
import androidx.wear.compose.ui.tooling.preview.WearPreviewFontScales
import androidx.wear.tooling.preview.devices.WearDevices
import com.databelay.refwatch.R
import com.databelay.refwatch.common.*
import com.databelay.refwatch.common.theme.RefWatchWearTheme
import com.databelay.refwatch.wear.presentation.utils.localizedName
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*

@Composable
fun TeamActionsPage(
    team: Team,
    game: Game,
    onNavigateToLogGoal: (Team, GoalType) -> Unit,
    onNavigateToLogCard: (team: Team, cardType: CardType) -> Unit,
    onNavigateToLogSubstitution: (Team) -> Unit,
    onQuickSubstitution: (Team, Int, Int) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    var showGoalTypeDialog by remember { mutableStateOf(false) }
    var showQuickSubDialog by remember { mutableStateOf(false) }
    var showRosterDialog by remember { mutableStateOf(false) }

    val teamColor = if (team == Team.HOME) game.homeTeamColor else game.awayTeamColor
    val teamAbbr = if (team == Team.HOME) game.homeTeamAbbr else game.awayTeamAbbr
    val fullTeamName = if (team == Team.HOME) game.homeTeamName else game.awayTeamName
    val teamName = (teamAbbr?.takeIf { it.isNotBlank() }
        ?: fullTeamName.uppercase().filter { it.isLetterOrDigit() }.take(3))
        .ifBlank { stringResource(if (team == Team.HOME) R.string.home else R.string.away).take(3).uppercase() }

    ScreenScaffold() {
        // FIXED COLUMN (No Scrolling)
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(top = 16.dp, bottom = 4.dp, start = 8.dp, end = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween // Spread elements evenly
        ) {
            // Header: Team Name
            Text(
                text = teamName,
                style = MaterialTheme.typography.titleMedium,
                color = if (teamColor.isDark()) Color.White else teamColor,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Black,
                modifier = Modifier.pointerInput(team) {
                    detectTapGestures(
                        onDoubleTap = { showQuickSubDialog = true },
                        onLongPress = { onNavigateToLogGoal(team, GoalType.REGULAR) }
                    )
                }
            )

            Spacer(Modifier.height(8.dp)) // Small spacer instead of Kader button

            // Goal Button (Compacted diameter)
            if (game.currentPhase.isPlayablePhase()) {
                Button(
                    onClick = { showGoalTypeDialog = true },
                    shape = CircleShape,
                    modifier = Modifier.size(60.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "+1",
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Cards Row (Larger)
            Row(
                modifier = Modifier.fillMaxWidth(0.9f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Yellow Card
                ActionSquareButton(
                    onClick = { onNavigateToLogCard(team, CardType.YELLOW) },
                    label = "GELB",
                    backgroundColor = Color.Yellow,
                    contentColor = Color.Black,
                    modifier = Modifier.width(62.dp).height(42.dp)
                )
                Spacer(Modifier.width(10.dp))
                // Red Card
                ActionSquareButton(
                    onClick = { onNavigateToLogCard(team, CardType.RED) },
                    label = "ROT",
                    backgroundColor = Color.Red,
                    contentColor = Color.White,
                    modifier = Modifier.width(62.dp).height(42.dp)
                )
            }

            // Substitution Button (Flat Wide Balken)
            val subsCount = game.events.filterIsInstance<SubstitutionEvent>().count { it.team == team }
            val subsRemaining = (game.maxSubstitutionsAllowed - subsCount).coerceAtLeast(0)
            
            Button(
                onClick = { showQuickSubDialog = true },
                enabled = subsRemaining > 0,
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(38.dp)
                    .pointerInput(team) {
                        detectTapGestures(
                            onLongPress = { onNavigateToLogSubstitution(team) }
                        )
                    },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2962FF), // Vibrant blue
                    contentColor = Color.White
                ),
                shape = CircleShape
            ) {
                Text(
                    text = "Wechsel ($subsRemaining)",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black)
                )
            }
        }
    }

    if (showQuickSubDialog) {
        QuickSubstitutionDialog(
            team = team,
            roster = if (team == Team.HOME) game.homeRoster else game.awayRoster,
            onConfirm = { outNum, inNum ->
                onQuickSubstitution(team, outNum, inNum)
                showQuickSubDialog = false
            },
            onDismiss = { showQuickSubDialog = false }
        )
    }

    if (showRosterDialog) {
        val roster = if (team == Team.HOME) game.homeRoster else game.awayRoster
        Dialog(visible = true, onDismissRequest = { showRosterDialog = false }) {
            val rosterListState = rememberScalingLazyListState()
            ScalingLazyColumn(
                state = rosterListState,
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(top = 32.dp, bottom = 32.dp, start = 8.dp, end = 8.dp)
            ) {
                item {
                    Text(
                        text = "Kader: $teamName",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                items(roster) { player ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = player.number.toString(),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = player.name.ifBlank { "Spieler" },
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (player.isCaptain) {
                            Spacer(Modifier.width(4.dp))
                            Text("(C)", style = MaterialTheme.typography.labelSmall, color = Color.Yellow)
                        }
                    }
                }
                item {
                    Button(
                        onClick = { showRosterDialog = false },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        Text("Schließen")
                    }
                }
            }
        }
    }

    if (showGoalTypeDialog) {
        GoalTypeSelectionDialog(
            team = team,
            onGoalTypeSelected = { goalType ->
                showGoalTypeDialog = false
                onNavigateToLogGoal(team, goalType)
            },
            onDismiss = { showGoalTypeDialog = false }
        )
    }
}

@Composable
fun ActionSquareButton(
    onClick: () -> Unit,
    label: String,
    backgroundColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = contentColor
        ),
        contentPadding = PaddingValues(0.dp),
        modifier = modifier
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun GoalTypeSelectionDialog(
    team: Team,
    onGoalTypeSelected: (GoalType) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        visible = true,
        onDismissRequest = onDismiss
    ) {
        val listState = rememberScalingLazyListState()
        val chipColors = ChipDefaults.primaryChipColors(
            backgroundColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
        ScalingLazyColumn(
            state = listState,
            contentPadding = PaddingValues(vertical = 16.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                ListHeader {
                    Text(
                        stringResource(R.string.goal_type),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
            item {
                Chip(
                    onClick = { onGoalTypeSelected(GoalType.REGULAR) },
                    colors = chipColors,
                    label = { Text(stringResource(R.string.goal_regular)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Chip(
                    onClick = { onGoalTypeSelected(GoalType.PENALTY) },
                    colors = chipColors,
                    label = { Text(stringResource(R.string.goal_penalty)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Chip(
                    onClick = { onGoalTypeSelected(GoalType.OWN_GOAL) },
                    colors = chipColors,
                    label = { Text(stringResource(R.string.goal_own_goal)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Preview(device = WearDevices.LARGE_ROUND, showBackground = true)
@WearPreviewFontScales
@Composable
fun TeamActionsPagePreview() {
    RefWatchWearTheme {
        TeamActionsPage(
            team = Team.HOME,
            game = Game.defaults().copy(
                homeTeamName = "Warriors",
                homeTeamColorArgb = android.graphics.Color.BLUE
            ),
            onNavigateToLogGoal = { _, _ -> },
            onNavigateToLogCard = { _, _ -> },
            onNavigateToLogSubstitution = {}
        )
    }
}
