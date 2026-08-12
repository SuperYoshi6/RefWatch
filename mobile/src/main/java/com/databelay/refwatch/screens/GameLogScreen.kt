package com.databelay.refwatch.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.databelay.refwatch.R
import com.databelay.refwatch.common.CardIssuedEvent
import com.databelay.refwatch.common.Game
import com.databelay.refwatch.common.GameEvent
import com.databelay.refwatch.common.GoalScoredEvent
import com.databelay.refwatch.common.SubstitutionEvent
import com.databelay.refwatch.common.shouldBeLogged
import com.databelay.refwatch.common.theme.AccentGreen
import com.databelay.refwatch.common.theme.Border
import com.databelay.refwatch.common.theme.PitchBackground
import com.databelay.refwatch.common.theme.Surface
import com.databelay.refwatch.common.theme.TextMuted
import com.databelay.refwatch.common.theme.TextPrimary
import com.databelay.refwatch.data.GameLogViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameLogScreen(
    game: Game?,
    navController: NavController,
    viewModel: GameLogViewModel = hiltViewModel()
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val isDfbNetEnabled = viewModel.isDfbNetEnabled

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.game_log_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { navController.popBackStack() },
                            colors = IconButtonDefaults.iconButtonColors(contentColor = TextPrimary)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = TextPrimary,
                        navigationIconContentColor = TextPrimary
                    )
                )
                if (isDfbNetEnabled && game != null) {
                    SecondaryTabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = Color.Transparent,
                        contentColor = AccentGreen,
                        indicator = {
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(selectedTabIndex),
                                color = AccentGreen
                            )
                        }
                    ) {
                        Tab(
                            selected = selectedTabIndex == 0,
                            onClick = { selectedTabIndex = 0 },
                            text = { Text(stringResource(R.string.tab_log)) }
                        )
                        Tab(
                            selected = selectedTabIndex == 1,
                            onClick = { selectedTabIndex = 1 },
                            text = { Text(stringResource(R.string.tab_dfbnet)) }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        PitchBackground(modifier = Modifier.fillMaxSize()) {
            if (game == null) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.game_not_found), color = TextPrimary)
                }
                return@PitchBackground
            }

            Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                if (selectedTabIndex == 0) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        // Hero / score block — matches the website scoreboard mockup
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.tab_log),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = AccentGreen,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "${game.homeTeamName} – ${game.awayTeamName}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Spacer(Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = game.homeScore.toString(),
                                        style = MaterialTheme.typography.displaySmall,
                                        color = AccentGreen,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(
                                        text = "  :  ",
                                        style = MaterialTheme.typography.displaySmall,
                                        color = TextMuted,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(
                                        text = game.awayScore.toString(),
                                        style = MaterialTheme.typography.displaySmall,
                                        color = AccentGreen,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.final_score, game.homeScore.toString(), game.awayScore.toString()),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }
                            HorizontalDivider(color = Border)
                        }

                        // List of all game events
                        items(game.events.filter { it.phase?.shouldBeLogged() != false }) { event ->
                            GameLogItem(event = event)
                            HorizontalDivider(color = Border)
                        }
                    }
                } else {
                    DFBnetTabContent(game)
                }
            }
        }
    }
}

@Composable
private fun DFBnetTabContent(game: Game) {
    var showSummary by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    Box(modifier = Modifier.fillMaxSize()) {
        DFBnetWebView(modifier = Modifier.fillMaxSize())

        // Floating Summary Toggle
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Column(horizontalAlignment = Alignment.End) {
                if (showSummary) {
                    ReportSummaryCard(
                        game = game,
                        onClose = { showSummary = false },
                        onCopy = {
                            val summary = formatReportSummary(game)
                            clipboardManager.setText(AnnotatedString(summary))
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                }
                FloatingActionButton(
                    onClick = { showSummary = !showSummary },
                    containerColor = AccentGreen,
                    contentColor = Color(0xFF0A1A0A),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = if (showSummary) Icons.Default.Info else Icons.Default.Info, // Use better icon if needed
                        contentDescription = stringResource(R.string.report_summary)
                    )
                }
            }
        }
    }
}

@Composable
private fun ReportSummaryCard(
    game: Game,
    onClose: () -> Unit,
    onCopy: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(280.dp)
            .fillMaxHeight(0.6f),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, Border)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.report_summary),
                    style = MaterialTheme.typography.titleSmall,
                    color = AccentGreen,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = TextMuted)
                }
            }
            Spacer(Modifier.height(8.dp))
            
            Text(text = "Spiel-Nr: ${game.gameNumber}", style = MaterialTheme.typography.bodySmall, color = TextMuted)
            Text(text = "${game.homeTeamName} vs ${game.awayTeamName}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
            
            Spacer(Modifier.height(12.dp))
            
            SummarySection(title = "Tore", events = game.events.filterIsInstance<GoalScoredEvent>())
            SummarySection(title = "Karten", events = game.events.filterIsInstance<CardIssuedEvent>())
            SummarySection(title = "Wechsel", events = game.events.filterIsInstance<SubstitutionEvent>())

            Spacer(Modifier.height(16.dp))
            
            Button(
                onClick = onCopy,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = Color(0xFF0A1A0A)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.copy_report), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun SummarySection(title: String, events: List<GameEvent>) {
    if (events.isEmpty()) return
    
    Text(text = title, style = MaterialTheme.typography.labelSmall, color = AccentGreen, fontWeight = FontWeight.Bold)
    events.forEach { event ->
        Text(
            text = event.displayString,
            style = MaterialTheme.typography.bodySmall,
            color = TextPrimary,
            modifier = Modifier.padding(vertical = 2.dp)
        )
    }
    Spacer(Modifier.height(8.dp))
}

private fun formatReportSummary(game: Game): String {
    val sb = StringBuilder()
    sb.append("Spielbericht: ${game.homeTeamName} vs ${game.awayTeamName}\n")
    sb.append("Ergebnis: ${game.homeScore}:${game.awayScore}\n\n")
    
    val goals = game.events.filterIsInstance<GoalScoredEvent>()
    if (goals.isNotEmpty()) {
        sb.append("TORE:\n")
        goals.forEach { sb.append("- ${it.displayString}\n") }
        sb.append("\n")
    }
    
    val cards = game.events.filterIsInstance<CardIssuedEvent>()
    if (cards.isNotEmpty()) {
        sb.append("KARTEN:\n")
        cards.forEach { sb.append("- ${it.displayString}\n") }
        sb.append("\n")
    }
    
    val subs = game.events.filterIsInstance<SubstitutionEvent>()
    if (subs.isNotEmpty()) {
        sb.append("WECHSEL:\n")
        subs.forEach { sb.append("- ${it.displayString}\n") }
    }
    
    return sb.toString()
}

@Composable
private fun GameLogItem(event: GameEvent) {
    val sdf = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val formattedTimestamp = remember(event.timestamp) { sdf.format(Date(event.timestamp.toLong())) }

    ListItem(
        headlineContent = { Text(event.displayString, fontWeight = FontWeight.Medium, color = TextPrimary) },
        supportingContent = { Text(stringResource(R.string.event_time, formattedTimestamp), color = TextMuted) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}