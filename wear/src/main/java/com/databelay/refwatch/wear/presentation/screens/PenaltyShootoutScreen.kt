package com.databelay.refwatch.wear.presentation.screens

import androidx.compose.ui.text.style.TextAlign
import android.widget.Toast

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.databelay.refwatch.common.homeTeamColor
import com.databelay.refwatch.common.awayTeamColor
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.KeyboardType
import androidx.wear.compose.material3.AlertDialogDefaults
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Dialog
import androidx.wear.compose.ui.tooling.preview.WearPreviewFontScales
import com.databelay.refwatch.R
import com.databelay.refwatch.common.Game
import com.databelay.refwatch.common.GamePhase
import com.databelay.refwatch.common.Team
import com.databelay.refwatch.common.isPlayablePhase
import com.databelay.refwatch.common.Player
import com.databelay.refwatch.wear.presentation.components.PlayerPicker
import com.databelay.refwatch.wear.presentation.utils.localizedName
import com.databelay.refwatch.common.theme.RefWatchWearTheme
import com.databelay.refwatch.wear.presentation.components.ColorIndicator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PenaltyShootoutScreen(
    game: Game,
    onPenaltyAttemptRecorded: (scored: Boolean, kickerNumber: Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var pendingKicker by remember { mutableStateOf<Int?>(null) }
    var showKickerDialog by remember { mutableStateOf(false) }
    var isManualEntry by remember { mutableStateOf(false) }

    val currentRoster = remember(game.kickOffTeam, game.homeRoster, game.awayRoster) {
        if (game.kickOffTeam == Team.HOME) game.homeRoster.filter { it.isOnField }
        else game.awayRoster.filter { it.isOnField }
    }

    ScreenScaffold {
        if (showKickerDialog && currentRoster.isNotEmpty() && !isManualEntry) {
            Column(
                modifier = Modifier.fillMaxSize().padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.penalty_kicker_title),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                PlayerPicker(
                    players = currentRoster,
                    onPlayerSelected = { 
                        pendingKicker = it.number
                        showKickerDialog = false
                    },
                    onManualEntry = { isManualEntry = true }
                )
            }
        } else {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceAround
            ) {
                Text(
                    text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val homeHasKickOff =
                        game.kickOffTeam == Team.HOME && game.currentPhase.isPlayablePhase()
                    ColorIndicator(
                        color = game.homeTeamColor,
                        hasKickOffBorder = homeHasKickOff,
                    )
                    Text(
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

                Text(
                    text = "${game.currentPhase.localizedName()}: ${game.penaltiesTakenHome} - ${game.penaltiesTakenAway}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                val takerName =
                    if (game.kickOffTeam == Team.HOME) game.homeTeamName else game.awayTeamName
                Spacer(modifier = Modifier.padding(2.dp))
                Button(
                    onClick = { showKickerDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ButtonDefaults.LargeIconSize),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Text(
                        text = pendingKicker?.let { "#$it" }
                            ?: stringResource(R.string.penalty_kicker_prompt, takerName),
                        style = MaterialTheme.typography.titleSmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.padding(2.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.CenterHorizontally),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            val kicker = pendingKicker
                            if (kicker != null) {
                                onPenaltyAttemptRecorded(true, kicker)
                                pendingKicker = null
                            }
                        },
                        enabled = pendingKicker != null,
                        modifier = Modifier
                            .weight(1f)
                            .height(ButtonDefaults.LargeIconSize),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            stringResource(R.string.penalty_scored),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )
                    }
                    Text(
                        text = stringResource(R.string.goal_increment),
                        modifier = Modifier.padding(horizontal = 6.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Button(
                        onClick = {
                            val kicker = pendingKicker
                            if (kicker != null) {
                                onPenaltyAttemptRecorded(false, kicker)
                                pendingKicker = null
                            }
                        },
                        enabled = pendingKicker != null,
                        modifier = Modifier
                            .weight(1f)
                            .height(ButtonDefaults.LargeIconSize),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(
                            stringResource(R.string.penalty_missed),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                Spacer(modifier = Modifier.padding(2.dp))
                Text(
                    text = stringResource(
                        R.string.taken_score_inline,
                        game.penaltiesTakenHome,
                        game.penaltiesTakenAway
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    if (showKickerDialog && (currentRoster.isEmpty() || isManualEntry)) {
        KickerNumberDialog(
            initialNumber = pendingKicker?.toString() ?: "",
            onConfirm = { number ->
                pendingKicker = number
                showKickerDialog = false
                isManualEntry = false
            },
            onDismiss = { 
                showKickerDialog = false
                isManualEntry = false
            }
        )
    }
}

@Composable
private fun KickerNumberDialog(
    initialNumber: String,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val enterNumberPrompt = stringResource(R.string.enter_valid_player_number)
    var number by remember { mutableStateOf(initialNumber) }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    Dialog(visible = true, onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.penalty_kicker_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = stringResource(R.string.penalty_kicker_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            LaunchedEffect(Unit) {
                keyboard?.show()
                focusRequester.requestFocus()
            }
            OutlinedTextField(
                value = number,
                onValueChange = { number = it.filter { c -> c.isDigit() }.take(3) },
                label = { Text(stringResource(R.string.enter_number)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                keyboardActions = KeyboardActions(
                    onDone = {
                        val n = number.toIntOrNull()
                        if (n != null && n > 0) onConfirm(n) else
                            Toast.makeText(context, enterNumberPrompt, Toast.LENGTH_SHORT).show()
                    }
                ),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
                Button(onClick = {
                    val n = number.toIntOrNull()
                    if (n != null && n > 0) onConfirm(n) else
                        Toast.makeText(context, enterNumberPrompt, Toast.LENGTH_SHORT).show()
                }) {
                    Text(stringResource(R.string.confirm))
                }
            }
        }
    }
}

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
            onPenaltyAttemptRecorded = { _, _ -> }
        )
    }
}
