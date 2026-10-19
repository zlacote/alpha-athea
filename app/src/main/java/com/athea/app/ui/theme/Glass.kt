package com.athea.app.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** Alpha levels of the glass surfaces, all white over the black canvas. */
object Glass {
    const val FILL_TOP = 0.14f
    const val FILL_BOTTOM = 0.05f
    const val FILL_STRONG_TOP = 0.22f
    const val FILL_STRONG_BOTTOM = 0.10f
    const val EDGE_TOP = 0.30f
    const val EDGE_BOTTOM = 0.06f

    /** The glass fill composited over the black canvas: where a translucent surface fades into its background. */
    val CANVAS_TINT = Color(0xFF242424)
}

/**
 * A translucent panel with a lit top edge. Background blur needs a
 * RenderEffect pipeline (API 31+), so this is the part that works on every
 * supported API level; the blur can be layered on top later.
 *
 * @param lit false drops the edge stroke, for full-width bars.
 * @param strong a denser fill, for panels that sit over busy content.
 */
fun Modifier.glass(shape: Shape, lit: Boolean = true, strong: Boolean = false): Modifier {
    val top = if (strong) Glass.FILL_STRONG_TOP else Glass.FILL_TOP
    val bottom = if (strong) Glass.FILL_STRONG_BOTTOM else Glass.FILL_BOTTOM
    val fill = Brush.verticalGradient(
        listOf(Color.White.copy(alpha = top), Color.White.copy(alpha = bottom)),
    )
    val edge = Brush.verticalGradient(
        listOf(Color.White.copy(alpha = Glass.EDGE_TOP), Color.White.copy(alpha = Glass.EDGE_BOTTOM)),
    )
    val surface = background(fill, shape)
    return if (lit) surface.border(BorderStroke(1.dp, edge), shape) else surface
}
