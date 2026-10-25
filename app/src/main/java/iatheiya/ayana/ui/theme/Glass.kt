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
import androidx.compose.ui.graphics.lerp
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
 * The colour the user mixed into the base of buttons and panels (Appearance), or null for none.
 * Glass surfaces lay it over their fill as a light wash; with glass off it tints the solid tone.
 */
object ChromeTint {
    var color by mutableStateOf<Color?>(null)

    /** How strongly the colour washes over a glass surface. */
    internal const val GLASS_ALPHA = 0.22f

    /** How far the solid tone moves toward the colour with glass off. */
    internal const val SOLID_MIX = 0.35f
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
            ChromeTint.color?.let { drawPath(path, color = it.copy(alpha = ChromeTint.GLASS_ALPHA)) }
            if (lit) drawPath(path, color = Glass.RIM, style = Stroke(width = rimWidth))
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
        drawPath(path, color = ChromeTint.color?.let { lerp(color, it, ChromeTint.SOLID_MIX) } ?: color)
        if (lit) drawPath(path, color = Glass.RIM, style = Stroke(width = rimWidth))
    }
}

internal fun Outline.toPath(): Path = when (this) {
    is Outline.Rectangle -> Path().apply { addRect(rect) }
    is Outline.Rounded -> Path().apply { addRoundRect(roundRect) }
    is Outline.Generic -> path
}
