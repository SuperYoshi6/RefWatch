package com.databelay.refwatch.wear.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.compose.ui.tooling.preview.WearPreviewFontScales
import com.databelay.refwatch.R
import com.databelay.refwatch.common.Game
import com.databelay.refwatch.common.PreviewTools.createFirstHalfSampleGame
import com.databelay.refwatch.common.Team
import com.databelay.refwatch.common.theme.RefWatchWearTheme
import com.databelay.refwatch.wear.presentation.components.ColorIndicator

@Composable
fun KickOffSelectionScreen(
    game: Game,
    onSetKickOffTeam: (Team) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.select_kick_off_team),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(MaterialTheme.shapes.medium)
                    .clickable { onSetKickOffTeam(Team.HOME) }
                    .padding(8.dp)
            ) {
                val homeName = if (game.homeTeamName == "Home") stringResource(R.string.home) else game.homeTeamName
                Text(
                    text = homeName,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(0.42f),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                ColorIndicator(
                    color = game.homeTeamColor,
                    indicatorSize = 56.dp,
                    outlineWidth = 2.dp,
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(MaterialTheme.shapes.medium)
                    .clickable { onSetKickOffTeam(Team.AWAY) }
                    .padding(8.dp)
            ) {
                val awayName = if (game.awayTeamName == "Away") stringResource(R.string.away) else game.awayTeamName
                Text(
                    text = awayName,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(0.42f),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                ColorIndicator(
                    color = game.awayTeamColor,
                    indicatorSize = 56.dp,
                    outlineWidth = 2.dp,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Preview(device = "id:wearos_small_round", name = "KickOffSelection SmRnd", showBackground = true)
@Preview(device = "id:wearos_large_round", name = "KickOffSelection LrgRnd", showBackground = true)
@Preview(device = "id:wearos_square", name = "KickOffSelection Sqr", showBackground = true)
@WearPreviewFontScales
@Composable
fun KickOffSelectionScreenPreview_FirstHalf() {
    RefWatchWearTheme {
        KickOffSelectionScreen(
            game = createFirstHalfSampleGame(),
            onSetKickOffTeam = {}
        )
    }
}
