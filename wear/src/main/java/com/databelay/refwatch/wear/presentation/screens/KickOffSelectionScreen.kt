package com.databelay.refwatch.wear.presentation.screens // Or your package

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
// Removed: import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Text
import com.databelay.refwatch.common.Team
import com.databelay.refwatch.common.readable
import com.databelay.refwatch.wear.IWearGameViewModel
import com.databelay.refwatch.wear.presentation.screens.PreviewTools.createExtraFirstHalfSampleGame
import com.databelay.refwatch.wear.presentation.screens.PreviewTools.createFirstHalfSampleGame
import com.databelay.refwatch.wear.presentation.screens.PreviewTools.createPenaltiesSampleGame


@Composable
fun KickOffSelectionScreen(
    gameViewModel: IWearGameViewModel,
    onConfirm: () -> Unit,
    onSetKickOffTeam: (Team) -> Unit
) {
    val activeGame by gameViewModel.activeGame.collectAsState()

    Column( // Replaced ScalingLazyColumn with Column
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp), // Overall padding for the screen
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center // Center content vertically on the screen
    ) {
        Text(
            // Use elvis operator for safety, though activeGame should ideally not be null here
            text = "Select ${activeGame?.currentPhase?.readable() ?: ""} Kick-Off Team",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth(0.85f) // Take 85% of width
                .padding(bottom = 8.dp) // Space below the title
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center, // Center chips in the Row
            verticalAlignment = Alignment.CenterVertically
        ) {
            Chip(
                onClick = { onSetKickOffTeam(Team.HOME) },
                label = { Text("Home", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                colors = ChipDefaults.chipColors(
                    backgroundColor = if (activeGame?.kickOffTeam == Team.HOME) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 2.dp) // Adjust padding as needed
            )

            Spacer(Modifier.width(8.dp)) // Increased spacer for better visual separation

            Chip(
                onClick = { onSetKickOffTeam(Team.AWAY) },
                label = { Text("Away", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                colors = ChipDefaults.chipColors(
                    backgroundColor = if (activeGame?.kickOffTeam == Team.AWAY) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 2.dp) // Adjust padding as needed
            )
        }

        Spacer(Modifier.height(16.dp)) // Space before the confirm button

        Button(
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth(0.8f), // Button takes 80% of width
            colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.colorScheme.tertiary // Or primary, depending on your theme
            )
        ) {
            Text("Select")
        }
    }
}


// ------------------------ Previews -----------------------------
// ---------------------------------------------------------------
@Preview(device = "id:wearos_large_round", showSystemUi = true, backgroundColor = 0xff000000, showBackground = true)
@Composable
fun KickOffSelectionScreenPreview_FirstHalf() {
    MaterialTheme {
        KickOffSelectionScreen(
            PreviewWearGameViewModel(initialActiveGame  = createFirstHalfSampleGame()),
            onConfirm = {}, // Empty lambda for preview
            onSetKickOffTeam = {} // Empty lambda for preview
        )
    }
}

@Preview(device = "id:wearos_large_round", showSystemUi = true, backgroundColor = 0xff000000, showBackground = true)
@Composable
fun KickOffSelectionScreenPreview_ExtraFirstHalf() {
    MaterialTheme {
        KickOffSelectionScreen(
            PreviewWearGameViewModel(initialActiveGame  = createExtraFirstHalfSampleGame()),
            onConfirm = {}, // Empty lambda for preview
            onSetKickOffTeam = {} // Empty lambda for preview
        )
    }
}

@Preview(device = "id:wearos_large_round", showSystemUi = true, backgroundColor = 0xff000000, showBackground = true)
@Composable
fun KickOffSelectionScreenPreview_Penalties() {
    MaterialTheme {
        KickOffSelectionScreen(
            PreviewWearGameViewModel(initialActiveGame  = createPenaltiesSampleGame()),
            onConfirm = {}, // Empty lambda for preview
            onSetKickOffTeam = {} // Empty lambda for preview
        )
    }
}