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
    const val EDGE_TOP = 0.30f
    const val EDGE_BOTTOM = 0.06f
}

/**
 * A translucent panel with a lit top edge. Background blur needs a
 * RenderEffect pipeline (API 31+), so this is the part that works on every
 * supported API level; the blur can be layered on top later.
 *
 * @param lit false drops the edge stroke, for full-width bars.
 */
fun Modifier.glass(shape: Shape, lit: Boolean = true): Modifier {
    val fill = Brush.verticalGradient(
        listOf(Color.White.copy(alpha = Glass.FILL_TOP), Color.White.copy(alpha = Glass.FILL_BOTTOM)),
    )
    val edge = Brush.verticalGradient(
        listOf(Color.White.copy(alpha = Glass.EDGE_TOP), Color.White.copy(alpha = Glass.EDGE_BOTTOM)),
    )
    val surface = background(fill, shape)
    return if (lit) surface.border(BorderStroke(1.dp, edge), shape) else surface
}
