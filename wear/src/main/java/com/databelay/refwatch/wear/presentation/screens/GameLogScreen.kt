package com.databelay.refwatch.wear.presentation.screens

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.ScrollIndicator
import androidx.wear.compose.material3.Text
import androidx.wear.compose.ui.tooling.preview.WearPreviewFontScales
import androidx.compose.ui.res.stringResource
import com.databelay.refwatch.R
import com.databelay.refwatch.wear.presentation.utils.localizedDisplayString
import com.databelay.refwatch.wear.presentation.utils.getMatchMinute
import com.databelay.refwatch.common.theme.RefWatchWearTheme
import com.databelay.refwatch.common.Game
import com.databelay.refwatch.common.GameEvent
import com.databelay.refwatch.common.PreviewTools.createFirstHalfSampleGame

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GameLogScreen(
    game: Game,
    onDismiss: () -> Unit,
    onRemoveEvent: (event: GameEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeDialogInfo: ConfirmationDialogInfo? by remember { mutableStateOf(null) }

    val listState = rememberScalingLazyListState()
    ScreenScaffold(
        scrollIndicator = {
            ScrollIndicator(
                modifier = Modifier.align(Alignment.CenterEnd),
                state = listState
            )
        },
        modifier = modifier
            .fillMaxSize()
            .padding(2.dp),
        contentPadding = PaddingValues(vertical = 0.dp, horizontal = 2.dp),
    ) { contentPadding ->
        ScalingLazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                ListHeader {
                    Text(
                        stringResource(R.string.game_log),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 8.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            if (game.events.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.no_events),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                items(
                    game.events.asReversed(),
                    key = { event -> event.id }) { event ->
                    EventLogItem(
                        event = event,
                        halfDurationMinutes = game.halfDurationMinutes,
                        onLongClick = {
                            activeDialogInfo = ConfirmationDialogInfo.RemoveLogEvent(
                                onConfirm = { onRemoveEvent(event)},
                                onDialogClose = onDismiss
                            )
                        }
                    )
                }
            }
            item {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .padding(top = 10.dp, bottom = 10.dp)
                        .fillMaxWidth(0.7f)
                ) {
                    Text(stringResource(R.string.back),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }

    activeDialogInfo?.let { dialogInfo ->
        UnifiedConfirmationDialog(dialogInfo = dialogInfo)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EventLogItem(
    event: GameEvent,
    halfDurationMinutes: Int,
    onLongClick: () -> Unit
) {
    val matchMinute = remember(event, halfDurationMinutes) {
        event.getMatchMinute(halfDurationMinutes)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.shapes.medium
            )
            .clip(MaterialTheme.shapes.medium)
            .combinedClickable(
                onClick = { },
                onLongClick = onLongClick,
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 8.dp)
        ) {
            Text(
                text = event.localizedDisplayString(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary
            )
            if (matchMinute.isNotEmpty()) {
                Text(
                    text = "Minute: $matchMinute",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                )
            }
        }
    }
}
