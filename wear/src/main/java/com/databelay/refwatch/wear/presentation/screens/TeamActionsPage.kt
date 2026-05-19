// In TeamActionsPage.kt
package com.databelay.refwatch.wear.presentation.screens

// Remove ScalingLazyColumn related imports
// import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
// import androidx.wear.compose.foundation.lazy.ScalingLazyListAnchorType
// import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import com.databelay.refwatch.common.homeTeamColor
import com.databelay.refwatch.common.awayTeamColor
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.ui.tooling.preview.WearPreviewFontScales
import androidx.wear.tooling.preview.devices.WearDevices
import com.databelay.refwatch.common.CardType
import com.databelay.refwatch.common.Game
import com.databelay.refwatch.common.GamePhase
import com.databelay.refwatch.common.Team
import com.databelay.refwatch.common.GoalType
import com.databelay.refwatch.common.isDark
import com.databelay.refwatch.common.isPlayablePhase
import com.databelay.refwatch.common.shortName
import com.databelay.refwatch.common.theme.RefWatchWearTheme
import androidx.wear.compose.material3.Dialog
import androidx.compose.ui.res.stringResource
import com.databelay.refwatch.R
import com.databelay.refwatch.wear.presentation.utils.localizedName
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults

@Composable
fun TeamActionsPage(
    team: Team,
    game: Game,
    onNavigateToLogGoal: (Team, GoalType) -> Unit,
    onNavigateToLogCard: (team: Team, cardType: CardType) -> Unit,
    onNavigateToLogSubstitution: (Team) -> Unit,
    modifier: Modifier = Modifier
) {
    var showGoalTypeDialog by remember { mutableStateOf(false) }

    val teamColor = if (team == Team.HOME) game.homeTeamColor else game.awayTeamColor
    val teamName =
        if (team == Team.HOME) shortName(game.homeTeamName) else shortName(game.awayTeamName)
//    val score = if (team == Team.HOME) game.homeScore else game.awayScore
    ScreenScaffold() {
        Spacer(modifier = Modifier.height(1.dp))

        Column(
            modifier = modifier // Use the passed modifier
                .fillMaxSize(),
            // Add your own general horizontal padding for the content
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceAround
        ) {
            // Text: Team Name and Score
            Text(
                text = teamName,
                style = MaterialTheme.typography.bodyMedium,
                color = if (teamColor.isDark()) Color.White else teamColor.let {
                    Color(
                        it.red,
                        it.green,
                        it.blue,
                        it.alpha
                    )
                },
                textAlign = TextAlign.Center, // Center text if it wraps
//                    modifier = Modifier.fillMaxWidth()/*.transformedHeight( (0, transformationSpec))*/,
//                        transformation = SurfaceTransformation(transformationSpec)
            )

            // Goal Button
            if (game.currentPhase.isPlayablePhase()) {
                Button(
                    onClick = { showGoalTypeDialog = true },
                    shape = CircleShape,
                    modifier = Modifier
                        .size(72.dp)
//                        .wrapContentWidth()
//                        .padding(horizontal = 32.dp),
                ) {
                    Text(
                        "+1",
                        style = MaterialTheme.typography.displayLarge,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                    )
                }
            } else {
                // Optional: Show a placeholder or disabled button if not in a playable phase
                // Or simply omit it, and the space will be taken up by other elements
                // For example, a Spacer to maintain height:
                Spacer(modifier = Modifier.height(ButtonDefaults.LargeIconSize)) // Standard Wear Button height
            }

            // Card Buttons in a Row
            Row(
                modifier = Modifier.fillMaxWidth(.5f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically // Align card buttons vertically
            ) {
                // Yellow Card Button
                CardShapedButton(
                    onClick = { onNavigateToLogCard(team, CardType.YELLOW) },
                    text = CardType.YELLOW.localizedName(),
                    backgroundColor = Color.Yellow,
                    contentColor = Color.Black,
                    modifier = Modifier.weight(1f) // Distribute space equally
                )

                // Red Card Button
                CardShapedButton(
                    onClick = { onNavigateToLogCard(team, CardType.RED) },
                    text = CardType.RED.localizedName(),
                    backgroundColor = Color.Red,
                    contentColor = Color.White,
                    modifier = Modifier.weight(1f) // Distribute space equally
                )
            }

            // Substitution Button
            val subsCount = game.events.filterIsInstance<com.databelay.refwatch.common.SubstitutionEvent>().count { it.team == team }
            val subsRemaining = (game.maxSubstitutionsAllowed - subsCount).coerceAtLeast(0)
            
            Button(
                onClick = { onNavigateToLogSubstitution(team) },
                enabled = subsRemaining > 0,
                modifier = Modifier.fillMaxWidth(0.6f).height(32.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Text(
                    text = "${stringResource(R.string.substitution)} ($subsRemaining)",
                    style = MaterialTheme.typography.labelSmall
                )
            }

            Spacer(modifier = Modifier.height(1.dp))

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
                    colors = ChipDefaults.primaryChipColors(),
                    label = { Text(stringResource(R.string.goal_regular)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Chip(
                    onClick = { onGoalTypeSelected(GoalType.PENALTY) },
                    colors = ChipDefaults.primaryChipColors(),
                    label = { Text(stringResource(R.string.goal_penalty)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Chip(
                    onClick = { onGoalTypeSelected(GoalType.OWN_GOAL) },
                    colors = ChipDefaults.primaryChipColors(),
                    label = { Text(stringResource(R.string.goal_own_goal)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// CardShapedButton Composable remains the same
@Composable
fun CardShapedButton(
    onClick: () -> Unit,
    text: String,
    backgroundColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(
            topStart = 8.dp,
            topEnd = 8.dp,
            bottomStart = 8.dp,
            bottomEnd = 8.dp
        ),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = contentColor
        ),
        modifier = modifier
            .aspectRatio(.90f)
            .border(
                1.dp,
                contentColor.copy(alpha = 0.5f),
                RoundedCornerShape(
                    topStart = 8.dp,
                    topEnd = 8.dp,
                    bottomStart = 8.dp,
                    bottomEnd = 8.dp
                )
            )
    ) {

        Text(
            text,
            style = MaterialTheme.typography.bodyExtraSmall,
            textAlign = TextAlign.Center
        )
    }
}


// -------------------------------- Previews -----------------------------------------------
// -----------------------------------------------------------------------------------------
@Preview(device = WearDevices.LARGE_ROUND, showBackground = true)
@Preview(device =  WearDevices.SMALL_ROUND, showBackground = true)
@Preview(device =  WearDevices.SQUARE, showBackground = true)
@WearPreviewFontScales

@Composable
fun TeamActionsPagePreview() {
    RefWatchWearTheme { // Wrap in MaterialTheme for previews
        TeamActionsPage(
            team = Team.HOME,
            game = Game.defaults().copy( // Use your Game.defaults() or a sample game
                id = "previewGame",
                currentPhase = GamePhase.FIRST_HALF,
                homeTeamName = "Red Team",
                homeTeamColorArgb = android.graphics.Color.BLACK,
                awayTeamColorArgb = android.graphics.Color.YELLOW,
                homeScore = 2
            ),
            onNavigateToLogGoal = { _, _ -> },
            onNavigateToLogCard = { _, _ -> },
            onNavigateToLogSubstitution = {}
        )
    }
}
