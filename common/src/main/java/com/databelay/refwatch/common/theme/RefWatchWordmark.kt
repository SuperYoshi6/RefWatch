package com.databelay.refwatch.common.theme

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle

/**
 * The RefWatch wordmark used on the landing page. The site uses
 *   <span class="ref">Ref</span><span class="watch">Watch</span>
 * with the "Watch" portion colored. We mirror that two-tone pattern.
 *
 * By default the wordmark is constrained to a single line with no soft wrap,
 * so it can never blow past the available width in a TopAppBar / hero header
 * and shove the action icons off-screen.
 */
@Composable
fun RefWatchWordmark(
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    refColor: Color = Color(0xFFE2E8F0),   // text-primary on dark
    watchColor: Color = AccentGreen,        // matches the site logo accent
    refWeight: FontWeight = FontWeight.ExtraBold,
    watchWeight: FontWeight = FontWeight.Black,
    maxLines: Int = 1
) {
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = refColor, fontWeight = refWeight)) {
                append("Ref")
            }
            withStyle(SpanStyle(color = watchColor, fontWeight = watchWeight)) {
                append("Watch")
            }
        },
        modifier = modifier,
        style = style,
        maxLines = maxLines,
        softWrap = false,
        overflow = TextOverflow.Clip
    )
}
