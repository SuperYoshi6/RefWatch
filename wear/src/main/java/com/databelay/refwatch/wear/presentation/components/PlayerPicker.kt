package com.databelay.refwatch.wear.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.*
import com.databelay.refwatch.R
import com.databelay.refwatch.common.Player

@Composable
fun PlayerPicker(
    players: List<Player>,
    onPlayerSelected: (Player) -> Unit,
    onManualEntry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberScalingLazyListState()

    ScalingLazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(top = 32.dp, bottom = 32.dp, start = 8.dp, end = 8.dp)
    ) {
        item {
            Button(
                onClick = onManualEntry,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Keyboard,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface, // Brighter icon
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.manual_entry_button))
                }
            }
        }

        items(players) { player ->
            TitleCard(
                onClick = { onPlayerSelected(player) },
                title = { 
                    Text(
                        text = player.number.toString(),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    ) 
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                if (player.name.isNotBlank()) {
                    Text(
                        text = player.name,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
