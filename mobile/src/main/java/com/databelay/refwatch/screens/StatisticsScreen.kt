package com.databelay.refwatch.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.databelay.refwatch.R
import com.databelay.refwatch.common.theme.AccentGreen
import com.databelay.refwatch.common.theme.Border
import com.databelay.refwatch.common.theme.PitchBackground
import com.databelay.refwatch.common.theme.Surface
import com.databelay.refwatch.common.theme.TextMuted
import com.databelay.refwatch.common.theme.TextPrimary
import com.databelay.refwatch.data.StatisticsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    onNavigateBack: () -> Unit,
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Statistiken",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
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
        }
    ) { paddingValues ->
        PitchBackground(modifier = Modifier.padding(paddingValues)) {
            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentGreen)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Hero
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Text(
                            text = "Übersicht",
                            style = MaterialTheme.typography.labelMedium,
                            color = AccentGreen,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Deine Karriere",
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    StatCard(
                        title = "Spiele gepfiffen",
                        value = uiState.totalGames.toString(),
                        icon = Icons.Filled.BarChart,
                        color = AccentGreen
                    )

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard(
                            title = "Tore gesamt",
                            value = uiState.totalGoals.toString(),
                            icon = Icons.Filled.SportsSoccer,
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        // Placeholder for something else? Maybe avg goals per game
                        val avg = if (uiState.totalGames > 0) "%.1f".format(uiState.totalGoals.toFloat() / uiState.totalGames) else "0.0"
                        StatCard(
                            title = "Ø Tore / Spiel",
                            value = avg,
                            icon = Icons.Filled.SportsSoccer,
                            color = TextMuted,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard(
                            title = "Gelbe Karten",
                            value = uiState.totalYellowCards.toString(),
                            icon = Icons.Filled.Style,
                            color = Color.Yellow,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Rote Karten",
                            value = uiState.totalRedCards.toString(),
                            icon = Icons.Filled.Block,
                            color = Color.Red,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, Border)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                Text(title, style = MaterialTheme.typography.labelMedium, color = TextMuted)
            }
            Text(
                text = value,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                color = TextPrimary
            )
        }
    }
}
