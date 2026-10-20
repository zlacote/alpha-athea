package com.athea.app.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/** Glass palette: a dark translucent base, a slow iridescent sheen and a lit rim. */
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

    /** Colours of the moving sheen: cool, violet, warm. */
    val SHEEN_COOL = Color(0x384FD8FF)
    val SHEEN_VIOLET = Color(0x3AB06BFF)
    val SHEEN_WARM = Color(0x33FFC978)

    val RIM_TOP = Color(0x66FFFFFF)
    val RIM_BOTTOM = Color(0x0FFFFFFF)

    /** Fill of a selected row inside a glass panel. */
    val SELECTED = Color(0x1FFFFFFF)

    /** The base composited over black: where a glass bubble fades into the canvas. */
    val CANVAS_TINT = Color(0xFF101014)

    const val SHEEN_PERIOD_MS = 7000
}

/**
 * Glass surface: a dark translucent base, an optional iridescent sheen that
 * drifts across it, and a lit rim.
 *
 * The sheen is read at draw time only, so it animates without recomposing the
 * screen. Refracting what lies behind needs a RuntimeShader over a captured
 * backdrop (API 33+); this is the part that works on every supported level.
 *
 * @param lit false drops the rim, for full-width bars.
 * @param strong a denser base, for panels over busy content.
 * @param clear an almost transparent base, for panels with a refracting lens behind them.
 * @param animated false keeps the surface static, for text-heavy or numerous surfaces.
 */
@Composable
fun Modifier.glass(
    shape: Shape,
    lit: Boolean = true,
    strong: Boolean = false,
    clear: Boolean = false,
    animated: Boolean = true,
): Modifier {
    val sheen = if (animated) rememberSheenPhase() else null
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
            if (sheen != null) drawPath(path, brush = sheenBrush(size, sheen.value))
            if (lit) drawPath(path, brush = rim, style = Stroke(width = rimWidth))
        }
    }
}

private fun Outline.toPath(): Path = when (this) {
    is Outline.Rectangle -> Path().apply { addRect(rect) }
    is Outline.Rounded -> Path().apply { addRoundRect(roundRect) }
    is Outline.Generic -> path
}

@Composable
private fun rememberSheenPhase(): State<Float> {
    val transition = rememberInfiniteTransition(label = "glass-sheen")
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = Glass.SHEEN_PERIOD_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "glass-sheen-phase",
    )
}

/**
 * A diagonal band of iridescent colour. It starts left of the surface and
 * leaves on the right, so the wrap-around at the end of each cycle is never
 * visible.
 */
private fun sheenBrush(size: Size, phase: Float): Brush {
    val band = size.width * 0.35f
    val travel = size.width + size.height + band
    val start = -band + travel * phase
    return Brush.linearGradient(
        colors = listOf(
            Color.Transparent,
            Glass.SHEEN_COOL,
            Glass.SHEEN_VIOLET,
            Glass.SHEEN_WARM,
            Color.Transparent,
        ),
        start = Offset(start, 0f),
        end = Offset(start + band, size.height),
    )
}
