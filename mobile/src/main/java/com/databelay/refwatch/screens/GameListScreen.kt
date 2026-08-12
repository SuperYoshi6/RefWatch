package com.databelay.refwatch.screens

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.databelay.refwatch.R
import com.databelay.refwatch.auth.AuthState
import com.databelay.refwatch.common.Game
import com.databelay.refwatch.common.GamePhase
import com.databelay.refwatch.common.GameStatus
import com.databelay.refwatch.common.theme.AccentGreen
import com.databelay.refwatch.common.theme.Border
import com.databelay.refwatch.common.theme.PitchBackground
import com.databelay.refwatch.common.theme.PitchLight
import com.databelay.refwatch.common.theme.RefWatchWordmark
import com.databelay.refwatch.common.theme.Surface
import com.databelay.refwatch.common.theme.TextMuted
import com.databelay.refwatch.common.theme.TextPrimary
import com.databelay.refwatch.data.ExplanationArea
import kotlinx.coroutines.flow.collectLatest
import com.databelay.refwatch.common.formattedGameDateTime
import java.text.SimpleDateFormat
import java.util.Locale

// Data class to define the structure and behavior of context menu items
data class ContextMenuItemAction(
    val label: String,
    val action: (game: Game) -> Unit
)

val tag = "GameListScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameListScreen(
    authStateValue: com.databelay.refwatch.auth.AuthState,
    games: kotlin.collections.List<com.databelay.refwatch.common.Game>,
    selectedTab: com.databelay.refwatch.common.GameStatus,
    scrollToTopSignal: kotlinx.coroutines.flow.SharedFlow<kotlin.Unit>?,
    onTabSelected: (com.databelay.refwatch.common.GameStatus) -> kotlin.Unit,
    onAddGame: () -> kotlin.Unit,
    onEditGame: (com.databelay.refwatch.common.Game) -> kotlin.Unit = {},
    onViewLog: (com.databelay.refwatch.common.Game) -> kotlin.Unit,
    onDeleteGame: (com.databelay.refwatch.common.Game) -> kotlin.Unit,
    onSignOut: () -> kotlin.Unit,
    onImportGames: () -> kotlin.Unit,
    onNavigateToSettings: () -> kotlin.Unit,
    onboardingStep: com.databelay.refwatch.data.OnboardingStep?,
    onNextTooltip: () -> kotlin.Unit,
    onDismissTooltip: () -> kotlin.Unit
) {
    Log.d(tag, "Received games: ${games.map { it.id + " -> " + it.status }}")

    val (upcomingGames, pastGames) = remember(games) {
        val now = System.currentTimeMillis()
        val (upcoming, past) = games.partition { game ->
            // A game is upcoming if:
            // 1. It's NOT in the GAME_ENDED phase
            // AND (2. It's NOT explicitly completed OR 3. It's recent enough)
            val isEnded = game.currentPhase == GamePhase.GAME_ENDED
            val isScheduledOrInProgress = game.status == GameStatus.SCHEDULED || game.status == GameStatus.IN_PROGRESS
            val isRecent = (game.gameDateTimeEpochMillis ?: 0L) > (now - 3 * 3600 * 1000L)
            
            !isEnded && isScheduledOrInProgress && isRecent
        }
        Pair(
            upcoming.sortedBy { it.gameDateTimeEpochMillis },
            past.sortedByDescending { it.gameDateTimeEpochMillis }
        )
    }
    val gamesToDisplay = if (selectedTab == GameStatus.SCHEDULED) upcomingGames else pastGames
    val lazyListState = rememberLazyListState()

    LaunchedEffect(key1 = scrollToTopSignal) {
        scrollToTopSignal?.collectLatest {
            Log.d(tag, "ScrollToTop event received in UI.")
            if (gamesToDisplay.isNotEmpty()) {
                lazyListState.animateScrollToItem(index = 0)
            }
        }
    }

    // PitchBackground is the OUTERMOST container. It draws the pitch gradient
    // edge-to-edge so the system status bar area (which is now transparent
    // thanks to enableEdgeToEdge in MainActivity) shows the same dark green
    // as the rest of the screen, instead of a flat white/black system band.
    PitchBackground(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        // Compact wordmark — keep it at the same visual weight as a
                        // normal app-bar title (the landing page is a hero, the app
                        // bar isn't). A bespoke 18sp ExtraBold style with no
                        // line-height multiplication keeps the bar one line tall
                        // and leaves room for the action icons on the right.
                        RefWatchWordmark(
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 18.sp,
                                lineHeight = 22.sp,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            refWeight = FontWeight.ExtraBold,
                            watchWeight = FontWeight.Black
                        )
                    },
                    actions = {
                        if (authStateValue is AuthState.Authenticated) {
                            IconButton(
                                onClick = onNavigateToSettings,
                                colors = IconButtonDefaults.iconButtonColors(
                                    contentColor = TextPrimary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Settings,
                                    contentDescription = "Settings"
                                )
                            }
                        }
                        ExplanationArea(
                            tag = "import_ics_button",
                            onboardingStep = onboardingStep,
                            onNext = onNextTooltip,
                            onDismiss = onDismissTooltip
                        ) {
                            IconButton(
                                onClick = onImportGames,
                                colors = IconButtonDefaults.iconButtonColors(
                                    contentColor = TextPrimary
                                )
                            ) {
                                Icon(Icons.Default.UploadFile, contentDescription = "Import ICS")
                            }
                        }
                        ExplanationArea(
                            tag = "sign_out_button",
                            onboardingStep = onboardingStep,
                            onNext = onNextTooltip,
                            onDismiss = onDismissTooltip
                        ) {
                            IconButton(
                                onClick = onSignOut,
                                colors = IconButtonDefaults.iconButtonColors(
                                    contentColor = TextPrimary
                                )
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ExitToApp,
                                    contentDescription = "Abmelden"
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = TextPrimary,
                        actionIconContentColor = TextPrimary
                    ),
                    windowInsets = TopAppBarDefaults.windowInsets
                )
            },
            floatingActionButton = {
                ExplanationArea(
                    tag = "add_game_fab",
                    onboardingStep = onboardingStep,
                    onNext = onNextTooltip,
                    onDismiss = onDismissTooltip
                ) {
                    FloatingActionButton(
                        onClick = onAddGame,
                        containerColor = AccentGreen,
                        contentColor = Color(0xFF0A1A0A),
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 0.dp
                        ),
                        shape = CircleShape
                    ) {
                        Icon(Icons.Filled.Add, "Neues Spiel")
                    }
                }
            }
        ) { paddingValues ->
            Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                // Hero header — mirrors the website's hero section. "Meine Spiele"
                // subtitle, the tagline is a German translation of the site's
                // "Match timer, goals, cards, substitutions and match log..." line.
                HeroHeader(
                    title = "Meine Spiele",
                    subtitle = "Spielzeit, Tore, Karten, Wechsel und Spielprotokoll – alles synchronisiert mit deiner Uhr."
                )

                // --- TABS ---
                val scheduledSelected = selectedTab == GameStatus.SCHEDULED
                PrimaryTabRow(
                    selectedTabIndex = if (scheduledSelected) 0 else 1,
                    containerColor = Color.Transparent,
                    contentColor = TextPrimary
                ) {
                    Tab(
                        selected = scheduledSelected,
                        onClick = { onTabSelected(GameStatus.SCHEDULED) },
                        text = { Text("Anstehend (${upcomingGames.size})", fontWeight = FontWeight.SemiBold) },
                        selectedContentColor = TextPrimary,
                        unselectedContentColor = TextMuted
                    )
                    Tab(
                        selected = !scheduledSelected,
                        onClick = { onTabSelected(GameStatus.COMPLETED) },
                        text = { Text("Vergangen (${pastGames.size})", fontWeight = FontWeight.SemiBold) },
                        selectedContentColor = TextPrimary,
                        unselectedContentColor = TextMuted
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // --- LIST OR EMPTY MESSAGE ---
                if (gamesToDisplay.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SportsSoccer,
                                contentDescription = null,
                                tint = AccentGreen,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                "Keine Spiele eingetragen.",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                "Tippe + um ein Spiel anzulegen oder importiere eine ICS-Datei.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        state = lazyListState,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp)
                    ) {
                        items(gamesToDisplay, key = { it.id }) { game ->
                            GameListItem(
                                game = game,
                                onViewLog = onViewLog,
                                onEditGame = { onEditGame(game) },
                                onDeleteGame = { onDeleteGame(game) }
                            )
                        }
                    }
                }
            }

            // Onboarding tooltip trigger
            val tooltipState = rememberTooltipState(isPersistent = true, initialIsVisible = true)
            LaunchedEffect(onboardingStep) {
                onboardingStep?.let { step ->
                    Log.d(tag, "LaunchedEffect for step: ${step.title}.")
                    step.tooltipState.show()
                }
            }
        }
    }
}

/**
 * Hero block at the top of the game list — mirrors the site hero's full-bleed
 * pitch background, big bold title, and one-line subtitle. Uses the
 * [PitchBackground] gradient that's already behind the screen.
 */
@Composable
private fun HeroHeader(title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = "Willkommen zurück",
            style = MaterialTheme.typography.labelMedium,
            color = AccentGreen,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.displaySmall,
            color = TextPrimary,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GameListItem(
    game: Game,
    onViewLog: (Game) -> Unit,
    onEditGame: (Game) -> Unit = {},
    onDeleteGame: (Game) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("EEE, MMM d, yyyy 'at' HH:mm", Locale.getDefault()) }
    val context = LocalContext.current
    var showContextMenu by remember { mutableStateOf(false) }

    val isCompleted = game.status == GameStatus.COMPLETED

    Box {
        // Card styled like the website's .feature card: dark surface, thin
        // border, a colored top-edge accent (green for scheduled, blue for
        // completed — picking the two "branded" colors the site uses).
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {
                        showContextMenu = false
                        if (isCompleted) onViewLog(game)
                    },
                    onLongClick = { showContextMenu = true }
                ),
            colors = CardDefaults.cardColors(containerColor = Surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, Border),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Top accent bar — gives the card that "card has a stripe" feel
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = if (isCompleted) listOf(AccentGreen, PitchLight)
                                else listOf(PitchLight, AccentGreen)
                            )
                        )
                )

                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        // Title row with date chip
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${game.homeTeamName} – ${game.awayTeamName}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))

                        // Sub-tag row mirroring the website's feature card tags
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Tag(text = "#${game.gameNumber}")
                            game.ageGroup?.let { Tag(text = it.displayName) }
                            when {
                                game.currentPhase == GamePhase.ABORTED -> Tag(text = "Abgebrochen", accent = Color.Red)
                                isCompleted -> Tag(text = "Beendet", accent = AccentGreen)
                                else -> Tag(text = "Anstehend", accent = PitchLight)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        game.formattedGameDateTime?.let {
                            Text("Uhrzeit: $it", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        }

                        // Location
                        val locationDetails = mutableListOf<String>()
                        game.venue?.takeIf { it.isNotBlank() }?.let { locationDetails.add(it) }
                        game.fieldNumber?.takeIf { it.isNotBlank() }?.let { locationDetails.add(stringResource(R.string.field_label_inline, it)) }
                        if (locationDetails.isNotEmpty()) {
                            Text(
                                text = locationDetails.joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                maxLines = 2
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Score line — boxed, like the website's "2 – 1" score
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(
                                    R.string.score_inline,
                                    game.homeScore.toString(),
                                    game.awayScore.toString()
                                ),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black,
                                color = AccentGreen
                            )
                        }
                    }

                    // Right side action column
                    Column(horizontalAlignment = Alignment.End) {
                        val fieldInlineTemplate = stringResource(R.string.field_inline)
                        val fullLocationQuery = remember(game.venue, game.fieldNumber, fieldInlineTemplate) {
                            val venuePart = game.venue?.takeIf { it.isNotBlank() } ?: ""
                            val fieldPart = game.fieldNumber?.takeIf { it.isNotBlank() }
                                ?.let { fieldInlineTemplate.format(it) } ?: ""
                            when {
                                venuePart.isNotBlank() && fieldPart.isNotBlank() &&
                                        venuePart.contains(fieldPart, ignoreCase = true) -> venuePart
                                venuePart.isNotBlank() && fieldPart.isNotBlank() -> "$venuePart, $fieldPart"
                                else -> venuePart.ifBlank { fieldPart }
                            }
                        }
                        val iconButtonHeight = 40.dp

                        if (fullLocationQuery.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    val mapQuery = Uri.encode(fullLocationQuery)
                                    val gmmIntentUri = Uri.parse("geo:0,0?q=$mapQuery")
                                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                    mapIntent.setPackage("com.google.android.apps.maps")
                                    if (mapIntent.resolveActivity(context.packageManager) != null) {
                                        context.startActivity(mapIntent)
                                    } else {
                                        try {
                                            context.startActivity(
                                                Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$mapQuery"))
                                            )
                                        } catch (e: Exception) {
                                            Log.e("MapLink", "Keine Karten-App gefunden", e)
                                        }
                                    }
                                },
                                modifier = Modifier.height(iconButtonHeight).size(iconButtonHeight)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.LocationOn,
                                    contentDescription = stringResource(R.string.open_in_maps),
                                    tint = AccentGreen
                                )
                            }
                        } else {
                            Box(modifier = Modifier.height(iconButtonHeight))
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        IconButton(
                            onClick = { onDeleteGame(game) },
                            modifier = Modifier.height(iconButtonHeight).size(iconButtonHeight)
                        ) {
                            Icon(
                                Icons.Filled.Delete,
                                stringResource(R.string.delete_game),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }

        // Context menu
        val editLabel = stringResource(R.string.edit_game_menu)
        val gameLogLabel = stringResource(R.string.game_log)
        val deleteLabel = stringResource(R.string.delete_game)
        val contextMenuActions = remember(game.status, editLabel, gameLogLabel, deleteLabel) {
            listOfNotNull(
                ContextMenuItemAction(editLabel) { onEditGame(it) },
                if (isCompleted) ContextMenuItemAction(gameLogLabel) { onViewLog(it) } else null,
                ContextMenuItemAction(deleteLabel) { onDeleteGame(it) }
            )
        }

        DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = { showContextMenu = false }
        ) {
            contextMenuActions.forEach { itemDefinition ->
                DropdownMenuItem(
                    text = { Text(itemDefinition.label) },
                    onClick = {
                        itemDefinition.action(game)
                        showContextMenu = false
                    }
                )
            }
        }
    }
}

/**
 * Small chip used in the cards. The website has tiny accent pills next to
 * each feature title; this mirrors that pattern in Compose.
 */
@Composable
private fun Tag(text: String, accent: Color = AccentGreen) {
    Box(
        modifier = Modifier
            .padding(end = 6.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(accent.copy(alpha = 0.15f))
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = accent,
            fontWeight = FontWeight.SemiBold
        )
    }
}
