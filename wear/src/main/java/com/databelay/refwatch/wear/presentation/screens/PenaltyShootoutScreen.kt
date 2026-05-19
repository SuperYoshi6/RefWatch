package com.databelay.refwatch.wear.presentation.screens

import androidx.compose.ui.text.style.TextAlign

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.databelay.refwatch.common.homeTeamColor
import com.databelay.refwatch.common.awayTeamColor
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.ui.tooling.preview.WearPreviewFontScales
import com.databelay.refwatch.common.Game
import com.databelay.refwatch.common.GamePhase
import com.databelay.refwatch.common.Team
import com.databelay.refwatch.common.isPlayablePhase
import com.databelay.refwatch.common.readable
import com.databelay.refwatch.common.theme.RefWatchWearTheme
import com.databelay.refwatch.wear.presentation.components.ColorIndicator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PenaltyShootoutScreen(
    game: Game, // Pass the necessary game state
    onPenaltyAttemptRecorded: (scored: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    ScreenScaffold(
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp), // Added some padding for the dedicated screen
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceAround // Center content for this dedicated screen
        ) {
//            Spacer(modifier = Modifier.height(1.dp))
            // Time display if TimeText isn't sufficient or for specific styling
            androidx.wear.compose.material3.Text(
                text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
            )

            // Score and Team Colors
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                // ... content of score row (ColorIndicator, Score Text) ...
                val homeHasKickOff =
                    game.kickOffTeam == Team.HOME && game.currentPhase.isPlayablePhase()
                ColorIndicator(
                    color = game.homeTeamColor,
                    hasKickOffBorder = homeHasKickOff,
                )
                androidx.wear.compose.material3.Text(
                    "${game.homeScore} - ${game.awayScore}",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                val awayHasKickOff =
                    game.kickOffTeam == Team.AWAY && game.currentPhase.isPlayablePhase()
                ColorIndicator(
                    color = game.awayTeamColor,
                    hasKickOffBorder = awayHasKickOff
                )
            }
            // Current Phase
            androidx.wear.compose.material3.Text(
                text = "${game.currentPhase.readable()}: ${game.penaltiesTakenHome} - ${game.penaltiesTakenAway}",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
            )


            val takerName =
                if (game.kickOffTeam == Team.HOME) game.homeTeamName else game.awayTeamName
            val takerAbbreviation = takerName.take(3).uppercase()
            Spacer(modifier = Modifier.padding(1.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { onPenaltyAttemptRecorded(true) },
                    modifier = Modifier
                        .weight(1f)
                        .height(ButtonDefaults.LargeIconSize),

                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        "Y",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                }
                Text(
                    text = "Scored?",
                    modifier = Modifier.padding(horizontal = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Button(
                    onClick = { onPenaltyAttemptRecorded(false) },
                    modifier = Modifier
                        .weight(1f)
                        .height(ButtonDefaults.LargeIconSize), // Use standard Wear button size
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        "N",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                }
            }
            Spacer(modifier = Modifier.padding(1.dp))
            // Display current penalty count (taken by each team)
            Text(
                text = "Taken: ${game.penaltiesTakenHome} | ${game.penaltiesTakenAway}",
                style = MaterialTheme.typography.bodySmall, // Slightly larger than caption
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}

// --------------------------------------- Previews ----------------------------------------
// -----------------------------------------------------------------------------------------

@Preview(device = "id:wearos_small_round", showBackground = true)
@Preview(device = "id:wearos_large_round", showBackground = true)
@Preview(device = "id:wearos_square", showBackground = true)
@WearPreviewFontScales
@Composable
fun Preview_PenaltiShootout() {
    RefWatchWearTheme {
        PenaltyShootoutScreen(
            game = Game.defaults().copy(
                currentPhase = GamePhase.PENALTIES,
                homeTeamName = "Red Team",
                awayTeamName = "Blue Team",
                homeScore = 3,
                awayScore = 1,
                penaltiesTakenHome = 2,
                penaltiesTakenAway = 1,
            ),
            onPenaltyAttemptRecorded = {}
        )
    }
}