package com.databelay.refwatch.common.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * The RefWatch landing page hero uses three stacked backgrounds:
 *   1) a vertical 180° linear-gradient between `--pitch-dark` (#1A3D1A) and `--pitch-light` (#2D5A2D)
 *   2) a horizontal repeating-linear-gradient of 1px grid lines every 50px
 *   3) a vertical repeating-linear-gradient of 1px grid lines every 50px
 *
 * This composable draws the same thing in Compose (without external deps).
 * Drop it at the top of any screen to make the page feel like the website.
 *
 * The grid lines are subtle (alpha=0.03) and the gradient is dark, so the
 * "card on a green field" composition in the rest of the UI still pops.
 */
@Composable
fun PitchBackground(
    modifier: Modifier = Modifier,
    gridSpacing: Float = 50f,
    gridAlpha: Float = 0.03f,
    topColor: Color = PitchDark,
    bottomColor: Color = PitchLight,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PitchBg)
            .drawBehind {
                // (1) Vertical pitch gradient
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to topColor,
                        0.5f to bottomColor,
                        1f to topColor
                    )
                )
                // (2) Vertical grid lines
                var x = 0f
                while (x < size.width) {
                    drawLine(
                        color = Color.White.copy(alpha = gridAlpha),
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 1f
                    )
                    x += gridSpacing
                }
                // (3) Horizontal grid lines
                var y = 0f
                while (y < size.height) {
                    drawLine(
                        color = Color.White.copy(alpha = gridAlpha),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f
                    )
                    y += gridSpacing
                }
            }
    ) {
        content()
    }
}
