package iatheiya.ayana.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.luminance
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

/**
 * The accent the user chose (Appearance → Color), or null for none. It lights what is chosen or
 * switched on: a switch's track, the picked option in a chooser, an armed modifier key. It does
 * not tint surfaces (the composer, the buttons, the menus), which keep their own neutral material.
 */
object AccentColor {
    var color by mutableStateOf<Color?>(null)
}

/** Black or white, whichever reads on [background]. */
fun contentOn(background: Color): Color = if (background.luminance() > 0.5f) Color.Black else Color.White

/**
 * A rim drawn inside [path] only: a stroke centred on the edge would stick out by half its width,
 * making the surface look wider than its neighbours and biting into gaps beside it.
 */
internal fun DrawScope.drawInnerRim(path: Path, color: Color, width: Float) {
    clipPath(path) { drawPath(path, color = color, style = Stroke(width = width * 2)) }
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
            if (lit) drawInnerRim(path, Glass.RIM, rimWidth)
        }
    }
}

/**
 * Opaque chrome fill, drawn when glass is switched off. Instead of going
 * translucent, every chrome surface falls back to this one solid tone — the
 * same [color] the menus already use — so turning glass off reads as a flat,
 * ordinary interface rather than a see-through one.
 *
 * @param lit false drops the rim, for full-width bars.
 */
fun Modifier.solidChrome(
    shape: Shape,
    color: Color,
    lit: Boolean = true,
): Modifier = drawWithCache {
    val path = shape.createOutline(size, layoutDirection, this).toPath()
    val rimWidth = 1.dp.toPx()
    onDrawBehind {
        drawPath(path, color = color)
        if (lit) drawInnerRim(path, Glass.RIM, rimWidth)
    }
}

internal fun Outline.toPath(): Path = when (this) {
    is Outline.Rectangle -> Path().apply { addRect(rect) }
    is Outline.Rounded -> Path().apply { addRoundRect(roundRect) }
    is Outline.Generic -> path
}
