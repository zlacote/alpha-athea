package com.athea.app.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import com.athea.app.screen.ScreenSnapshot
import com.athea.app.ui.theme.codeStyle

/**
 * The screen of a full-screen program (vim, top, less), drawn in place of the transcript. Its size
 * is reported the same way the transcript reports it, so the program lays itself out to this area.
 * Colours are not shown yet: the screen keeps plain characters.
 */
@Composable
fun FullScreenView(
    screen: ScreenSnapshot,
    contentTopPadding: Dp,
    contentBottomPadding: Dp,
    onAreaResized: (rows: Int, cols: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(top = contentTopPadding, bottom = contentBottomPadding),
    ) {
        val measurer = rememberTextMeasurer()
        val cellStyle = codeStyle()
        LaunchedEffect(constraints.maxWidth, constraints.maxHeight, cellStyle) {
            val layout = measurer.measure(AnnotatedString("M"), cellStyle)
            val cols = (constraints.maxWidth / layout.size.width.coerceAtLeast(1)).coerceAtLeast(1)
            val rows = (constraints.maxHeight / layout.size.height.coerceAtLeast(1)).coerceAtLeast(1)
            onAreaResized(rows, cols)
        }

        val cursorStyle = SpanStyle(
            color = MaterialTheme.colorScheme.background,
            background = MaterialTheme.colorScheme.onBackground,
        )
        Column(Modifier.fillMaxSize().clipToBounds()) {
            screen.rows.forEachIndexed { row, text ->
                val line = if (screen.cursorVisible && row == screen.cursorRow) {
                    withCursor(text, screen.cursorCol, cursorStyle)
                } else {
                    AnnotatedString(text)
                }
                Text(text = line, style = cellStyle, softWrap = false, maxLines = 1)
            }
        }
    }
}

/** The row text with the cell under the cursor drawn inverted. */
private fun withCursor(text: String, col: Int, style: SpanStyle): AnnotatedString {
    val padded = text.padEnd(col + 1)
    return buildAnnotatedString {
        append(padded.substring(0, col))
        pushStyle(style)
        append(padded[col])
        pop()
        append(padded.substring(col + 1))
    }
}
