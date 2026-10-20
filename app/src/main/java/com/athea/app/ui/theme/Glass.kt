package com.athea.app.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/** Glass palette: a dark translucent base and a bright rim that catches light from above. */
object Glass {

    /** Base of a regular glass panel, top to bottom. */
    val BASE_TOP = Color(0x8C16161C)
    val BASE_BOTTOM = Color(0xB80C0C10)

    /** Denser base for panels over busy content: drawer and menus. */
    val BASE_STRONG_TOP = Color(0xD916161C)
    val BASE_STRONG_BOTTOM = Color(0xF00A0A0D)

    /** Almost clear base under a refracting lens: the bend has to show through. */
    val CLEAR_TOP = Color(0x1F16161C)
    val CLEAR_BOTTOM = Color(0x2A0C0C10)

    /** The rim is bright at the top, where light falls, and fades down the sides. */
    val RIM_TOP = Color(0x9AFFFFFF)
    val RIM_BOTTOM = Color(0x1EFFFFFF)

    /** Fill of a selected row inside a glass panel. */
    val SELECTED = Color(0x1FFFFFFF)

    /** The base composited over black: where a glass bubble fades into the canvas. */
    val CANVAS_TINT = Color(0xFF101014)
}

/**
 * Glass surface: a dark translucent base and a bright rim. Under a working liquid
 * lens the base is clear, so the bent backdrop shows through the panel.
 *
 * @param lit false drops the rim, for full-width bars.
 * @param strong a denser base, for panels over busy content.
 * @param clear an almost transparent base, for panels with a refracting lens behind them.
 */
fun Modifier.glass(
    shape: Shape,
    lit: Boolean = true,
    strong: Boolean = false,
    clear: Boolean = false,
): Modifier {
    val base = when {
        clear -> Brush.verticalGradient(listOf(Glass.CLEAR_TOP, Glass.CLEAR_BOTTOM))
        strong -> Brush.verticalGradient(listOf(Glass.BASE_STRONG_TOP, Glass.BASE_STRONG_BOTTOM))
        else -> Brush.verticalGradient(listOf(Glass.BASE_TOP, Glass.BASE_BOTTOM))
    }
    val rim = Brush.verticalGradient(listOf(Glass.RIM_TOP, Glass.RIM_BOTTOM))
    return drawWithCache {
        val path = shape.createOutline(size, layoutDirection, this).toPath()
        val rimWidth = 1.dp.toPx()
        onDrawBehind {
            drawPath(path, brush = base)
            if (lit) drawPath(path, brush = rim, style = Stroke(width = rimWidth))
        }
    }
}

private fun Outline.toPath(): Path = when (this) {
    is Outline.Rectangle -> Path().apply { addRect(rect) }
    is Outline.Rounded -> Path().apply { addRoundRect(roundRect) }
    is Outline.Generic -> path
}
