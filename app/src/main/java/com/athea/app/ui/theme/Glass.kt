package com.athea.app.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Glass palette: flat translucent fills, so what shows through is the blurred
 * backdrop, not a tint. There are no gradients: a vertical gradient over the
 * glass read as a colour wash instead of a surface.
 */
object Glass {

    /** Fill of a regular glass panel: the blurred backdrop shows through. */
    val FILL = Color(0x40141418)

    /** Denser fill for panels over busy content: drawer and menus. */
    val FILL_STRONG = Color(0xB8141418)

    /** Almost no fill under a refracting lens: the clean look comes from the bent backdrop. */
    val FILL_CLEAR = Color(0x0CFFFFFF)

    /** One thin rim of the same weight on every side. */
    val RIM = Color(0x26FFFFFF)

    /** Fill of a selected row inside a glass panel. */
    val SELECTED = Color(0x1FFFFFFF)

    /** The base composited over black: where a glass bubble fades into the canvas. */
    val CANVAS_TINT = Color(0xFF101014)
}

internal fun glassFill(strong: Boolean, clear: Boolean): Color = when {
    clear -> Glass.FILL_CLEAR
    strong -> Glass.FILL_STRONG
    else -> Glass.FILL
}

/**
 * Glass surface without refraction: a flat translucent fill and a thin rim.
 *
 * @param lit false drops the rim, for full-width bars.
 * @param strong a denser fill, for panels over busy content.
 * @param clear an almost transparent fill, for panels with a refracting lens behind them.
 */
fun Modifier.glass(
    shape: Shape,
    lit: Boolean = true,
    strong: Boolean = false,
    clear: Boolean = false,
): Modifier {
    val fill = glassFill(strong, clear)
    return drawWithCache {
        val path = shape.createOutline(size, layoutDirection, this).toPath()
        val rimWidth = 1.dp.toPx()
        onDrawBehind {
            drawPath(path, color = fill)
            if (lit) drawPath(path, color = Glass.RIM, style = Stroke(width = rimWidth))
        }
    }
}

internal fun Outline.toPath(): Path = when (this) {
    is Outline.Rectangle -> Path().apply { addRect(rect) }
    is Outline.Rounded -> Path().apply { addRoundRect(roundRect) }
    is Outline.Generic -> path
}
