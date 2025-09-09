package com.databelay.refwatch.wear.presentation.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.Check
import androidx.wear.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.*
import androidx.wear.compose.ui.tooling.preview.WearPreviewFontScales
import com.databelay.refwatch.common.CardType
import com.databelay.refwatch.common.Team
import com.databelay.refwatch.common.theme.RefWatchWearTheme
import kotlinx.coroutines.delay

@Composable
fun LogCardScreen(
    preselectedTeam: Team?,
    cardType: CardType,
    onLogCard: (team: Team, playerNumber: Int, cardType: CardType) -> Unit,
    onCancel: () -> Unit
) {
    var selectedTeam by remember { mutableStateOf(preselectedTeam) }
//    var selectedCardType by remember { mutableStateOf<CardType?>(CardType.YELLOW) }
    var playerNumberString by remember { mutableStateOf("") }
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    ScreenScaffold(
//        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceAround
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            Text("Log Card", style = MaterialTheme.typography.titleSmall)
            preselectedTeam?.let {
                Text(
                    "For Team: ${it.name}",
                    style = MaterialTheme.typography.bodySmall,
//                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            // Player Number
            OutlinedTextField( // Using M3 OutlinedTextField
                value = playerNumberString,
                onValueChange = {
                    if (it.length <= 2 && it.all { char -> char.isDigit() }) {
                        playerNumberString = it
                    }
                },
                label = { Text("") }, // M3 Text
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .padding(horizontal = 32.dp),
                colors = TextFieldDefaults.colors(
                    // Focused colors
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedContainerColor = Color.Transparent, // Or MaterialTheme.colorScheme.surface

                    // Unfocused colors
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unfocusedContainerColor = Color.Transparent, // Or MaterialTheme.colorScheme.surface

                    // Disabled colors (optional, but good to define)
                    disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    disabledContainerColor = Color.Transparent,

                    // Cursor color
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )


            // Action Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
/*                IconButton(
                    onClick = onCancel,
                ) {
                    Icon(imageVector = Icons.Filled.Close, contentDescription = "Cancel")
                }
                IconButton (
                    onClick = {
                        val playerNum = playerNumberString.toIntOrNull()
                        // Read selectedTeam into a local immutable variable
                        val currentSelectedTeam =
                            selectedTeam // selectedTeam is MutableState<Team?>
                        if (currentSelectedTeam != null && playerNum != null && playerNum > 0) {
                            // Now currentSelectedTeam can be smart-cast to Team
                            onLogCard(currentSelectedTeam, playerNum, cardType)
                        } else {
                            if (currentSelectedTeam == null) {
                                Toast.makeText(context, "No team selected", Toast.LENGTH_SHORT)
                                    .show()
                            } else { // playerNum is null or not > 0
                                Toast.makeText(
                                    context,
                                    "Enter a valid player number",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    },
                    enabled = selectedTeam != null && playerNumberString.isNotBlank(),
                ) {
//                    AlertDialogDefaults.ConfirmIcon
                    Icon(imageVector = Icons.Filled.Check, contentDescription = "Log card")

                }*/
                AlertDialogDefaults.DismissButton(
                    onClick = onCancel,
                )
                if (selectedTeam != null && playerNumberString.isNotBlank())
                    AlertDialogDefaults.ConfirmButton(
                        onClick = {
                            val playerNum = playerNumberString.toIntOrNull()
                            // Read selectedTeam into a local immutable variable
                            val currentSelectedTeam =
                                selectedTeam // selectedTeam is MutableState<Team?>
                            if (currentSelectedTeam != null && playerNum != null && playerNum > 0) {
                                // Now currentSelectedTeam can be smart-cast to Team
                                onLogCard(currentSelectedTeam, playerNum, cardType)
                            } else {
                                if (currentSelectedTeam == null) {
                                    Toast.makeText(context, "No team selected", Toast.LENGTH_SHORT)
                                        .show()
                                } else { // playerNum is null or not > 0
                                    Toast.makeText(
                                        context,
                                        "Enter a valid player number",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        },
                    )
                else
                    AlertDialogDefaults.ConfirmButton(
                        onClick = {},
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                        )
                    )

            }
            Spacer(modifier = Modifier.height(12.dp))

        }
    }
}

// --------------------------------------- Previews ----------------------------------------
// -----------------------------------------------------------------------------------------
@Preview(device = "id:wearos_small_round", name = "LogCard SmRnd", showBackground = true)
@Preview(device = "id:wearos_large_round", name = "LogCard LrgRnd", showBackground = true)
@Preview(device = "id:wearos_square", name = "LogCard Sqr", showBackground = true)
@WearPreviewFontScales
@Composable
fun LogCardScreenPreview_Yellow_Home() {
    RefWatchWearTheme {
        LogCardScreen(
            preselectedTeam = Team.HOME,
            cardType = CardType.YELLOW,
            onLogCard = { _, _, _ -> },
            onCancel = {}
        )
    }
}


