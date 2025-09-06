// In TeamActionsPage.kt
package com.databelay.refwatch.wear.presentation.screens

// Remove ScalingLazyColumn related imports
// import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
// import androidx.wear.compose.foundation.lazy.ScalingLazyListAnchorType
// import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.*
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import androidx.wear.compose.ui.tooling.preview.WearPreviewFontScales
import com.databelay.refwatch.common.CardType
import com.databelay.refwatch.common.Game
import com.databelay.refwatch.common.GamePhase
import com.databelay.refwatch.common.Team
import com.databelay.refwatch.common.isDark
import com.databelay.refwatch.common.isPlayablePhase
import com.databelay.refwatch.common.shortName
import com.databelay.refwatch.common.theme.RefWatchWearTheme
import com.google.android.horologist.compose.layout.ColumnItemType
import com.google.android.horologist.compose.layout.rememberResponsiveColumnPadding

@Composable
fun TeamActionsPage(
    team: Team,
    game: Game,
    onAddGoal: (Team) -> Unit,
    onNavigateToLogCard: (team: Team, cardType: CardType) -> Unit,
    modifier: Modifier = Modifier
) {
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
                    onClick = { onAddGoal(team) },
                    modifier = Modifier
                        .wrapContentWidth()
                        .padding(horizontal = 32.dp),
//                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Add Goal")
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
                    text = "Yellow",
                    backgroundColor = Color.Yellow,
                    contentColor = Color.Black,
                    modifier = Modifier.weight(1f) // Distribute space equally
                )

                // Red Card Button
                CardShapedButton(
                    onClick = { onNavigateToLogCard(team, CardType.RED) },
                    text = "Red",
                    backgroundColor = Color.Red,
                    contentColor = Color.White,
                    modifier = Modifier.weight(1f) // Distribute space equally
                )
            }
            Spacer(modifier = Modifier.height(1.dp))

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
            .aspectRatio(.80f)
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
@Preview(device = "id:wearos_small_round", showBackground = true)
@Preview(device = "id:wearos_large_round", showBackground = true)
@Preview(device = "id:wearos_square", showBackground = true)
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
            onAddGoal = {},
            onNavigateToLogCard = { _, _ -> }
        )
    }
}
