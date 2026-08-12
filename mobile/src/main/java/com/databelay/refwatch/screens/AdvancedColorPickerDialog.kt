package com.databelay.refwatch.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.databelay.refwatch.R
import com.databelay.refwatch.common.theme.Border
import com.databelay.refwatch.common.theme.Surface
import com.databelay.refwatch.common.theme.TextMuted
import com.databelay.refwatch.common.theme.TextPrimary
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedColorPickerDialog(
    initialColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedColor by remember { mutableStateOf(initialColor) }
    var selectedTab by remember { mutableIntStateOf(0) }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Colorize, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Colors", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Tabs
                SecondaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    divider = {},
                    indicator = {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(selectedTab),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                ) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                        Text(stringResource(R.string.color_picker_grid), modifier = Modifier.padding(8.dp))
                    }
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                        Text(stringResource(R.string.color_picker_spectrum), modifier = Modifier.padding(8.dp))
                    }
                    Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) {
                        Text(stringResource(R.string.color_picker_sliders), modifier = Modifier.padding(8.dp))
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Content based on tab
                Box(modifier = Modifier.height(280.dp)) {
                    when (selectedTab) {
                        0 -> ColorGrid(selectedColor) { selectedColor = it }
                        1 -> ColorSpectrum(selectedColor) { selectedColor = it }
                        2 -> ColorSliders(selectedColor) { selectedColor = it }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Opacity Slider
                Text(
                    text = stringResource(R.string.opacity).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Slider(
                        value = selectedColor.alpha,
                        onValueChange = { selectedColor = selectedColor.copy(alpha = it) },
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${(selectedColor.alpha * 100).roundToInt()}%",
                        modifier = Modifier.width(48.dp),
                        textAlign = Alignment.End.let { androidx.compose.ui.text.style.TextAlign.End },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Border)

                // Footer: Result & Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(selectedColor)
                            .border(1.dp, Border, RoundedCornerShape(12.dp))
                    )
                    
                    Button(
                        onClick = {
                            onColorSelected(selectedColor)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.confirm))
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorGrid(selectedColor: Color, onColorChange: (Color) -> Unit) {
    val colors = remember {
        listOf(
            Color.Red, Color.Magenta, Color.Blue, Color.Cyan, Color.Green, Color.Yellow,
            Color.Black, Color.White, Color.Gray, Color.DarkGray, Color.LightGray,
            Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF673AB7), Color(0xFF3F51B5),
            Color(0xFF2196F3), Color(0xFF03A9F4), Color(0xFF00BCD4), Color(0xFF009688),
            Color(0xFF4CAF50), Color(0xFF8BC34A), Color(0xFFCDDC39), Color(0xFFFFEB3B),
            Color(0xFFFFC107), Color(0xFFFF9800), Color(0xFFFF5722), Color(0xFF795548)
        )
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(40.dp),
        contentPadding = PaddingValues(4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(colors) { color ->
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color)
                    .border(
                        width = if (color.toArgb() == selectedColor.toArgb()) 2.dp else 0.dp,
                        color = if (color.toArgb() == selectedColor.toArgb()) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { onColorChange(color) }
            )
        }
    }
}

@Composable
private fun ColorSpectrum(selectedColor: Color, onColorChange: (Color) -> Unit) {
    // Basic Spectrum using a Canvas and a custom brush
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    // Logic to extract color from position would go here
                    // For now, simplified representation
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)
                )
            )
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White, Color.Transparent, Color.Black)
                )
            )
        }
    }
}

@Composable
private fun ColorSliders(selectedColor: Color, onColorChange: (Color) -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ColorSliderRow("Red", selectedColor.red) { onColorChange(selectedColor.copy(red = it)) }
        ColorSliderRow("Green", selectedColor.green) { onColorChange(selectedColor.copy(green = it)) }
        ColorSliderRow("Blue", selectedColor.blue) { onColorChange(selectedColor.copy(blue = it)) }
    }
}

@Composable
private fun ColorSliderRow(label: String, value: Float, onValueChange: (Float) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Slider(value = value, onValueChange = onValueChange, modifier = Modifier.weight(1f))
            Text(
                text = (value * 255).toInt().toString(),
                modifier = Modifier.width(32.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.End,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
