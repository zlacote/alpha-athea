package com.athea.app.ui.liquid

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.athea.app.ui.theme.glass
import com.athea.app.ui.theme.glassFill
import com.athea.app.ui.theme.toPath
import com.athea.app.util.AtheaLog
import kotlin.math.roundToInt

/**
 * The content that glass surfaces refract. Content marked with [liquidBackdrop] is
 * recorded into [layer]; every [liquidGlass] surface draws that recording through
 * its own lens, so several glass elements can bend the same content.
 *
 * Architecture follows KMPLiquidGlass (Apache-2.0, see NOTICE): a layer backdrop
 * that glass surfaces sample. It is rebuilt on public Compose APIs.
 */
class LiquidBackdrop internal constructor(internal val layer: GraphicsLayer) {

    /** Window-root position of the recorded content. Glass surfaces subtract it to align. */
    internal var rootOrigin: Offset = Offset.Zero
}

/** The backdrop that glass surfaces below this point refract. Null means plain glass. */
val LocalLiquidBackdrop = compositionLocalOf<LiquidBackdrop?> { null }

@Composable
fun rememberLiquidBackdrop(): LiquidBackdrop {
    val layer = rememberGraphicsLayer()
    return remember(layer) { LiquidBackdrop(layer) }
}

/**
 * Marks this subtree as backdrop content: it is drawn normally and recorded for glass.
 * [background] is painted inside the recording too. A background set on a parent is
 * not part of this subtree, so glass would sample transparent pixels and show nothing.
 */
fun Modifier.liquidBackdrop(backdrop: LiquidBackdrop, background: Color = Color.Unspecified): Modifier =
    onGloballyPositioned { backdrop.rootOrigin = it.positionInRoot() }
        .drawWithContent {
            val pixels = IntSize(size.width.roundToInt(), size.height.roundToInt())
            backdrop.layer.record(this@drawWithContent, layoutDirection, pixels) {
                if (background != Color.Unspecified) drawRect(background)
                drawContentInto(this@drawWithContent)
            }
            drawLayer(backdrop.layer)
        }

/**
 * Draws the children of [content] into the canvas this recording draws to. A plain
 * `drawContent()` called inside a record goes to the screen canvas instead, so the
 * recording stayed empty and glass sampled nothing.
 */
private fun DrawScope.drawContentInto(content: ContentDrawScope) {
    val screen = content.drawContext.canvas
    content.drawContext.canvas = drawContext.canvas
    try {
        content.drawContent()
    } finally {
        content.drawContext.canvas = screen
    }
}

/** Where a glass surface sits in window coordinates, updated during layout. */
private class GlassPlacement {
    var rootOrigin: Offset = Offset.Zero
}

/**
 * Glass surface that refracts [backdrop] inside [shape]. With a null backdrop it is
 * the plain glass of [com.athea.app.ui.theme.glass]. Below Android 13 the backdrop
 * is shown without the bend, still with the base and the rim.
 */
@Composable
fun Modifier.liquidGlass(
    backdrop: LiquidBackdrop?,
    shape: Shape,
    cornerRadius: Dp,
    lit: Boolean = true,
    strong: Boolean = false,
    clear: Boolean = false,
): Modifier {
    // A render effect is only drawn through an offscreen buffer; without it the lens is dropped.
    val lensLayer = rememberGraphicsLayer().apply {
        compositingStrategy = CompositingStrategy.Offscreen
    }
    val lensEffect = rememberLensEffect()
    val placement = remember { GlassPlacement() }
    if (backdrop == null || GlassTuning.mode == GlassMode.OFF) return glass(shape, lit, strong, clear)
    val fill = if (clear) Color.White.copy(alpha = GlassTuning.fillAlpha) else glassFill(strong, clear)
    val rim = Color.White.copy(alpha = GlassTuning.rimAlpha)
    return this
        .onGloballyPositioned { placement.rootOrigin = it.positionInRoot() }
        .drawWithCache {
            val path = shape.createOutline(size, layoutDirection, this).toPath()
            val rimWidth = 1.dp.toPx()
            val radiusPx = cornerRadius.toPx()
            val pixels = IntSize(size.width.roundToInt(), size.height.roundToInt())
            var logged = false
            onDrawWithContent {
                // Record the backdrop, moved so that it lines up with this surface.
                val offset = placement.rootOrigin - backdrop.rootOrigin
                if (!logged) {
                    logged = true
                    AtheaLog.log("lens", "surface size=$pixels offset=$offset shaded=${lensEffect != null}")
                }
                lensLayer.record(this@onDrawWithContent, layoutDirection, pixels) {
                    translate(-offset.x, -offset.y) {
                        drawLayer(backdrop.layer)
                    }
                }
                if (lensEffect != null) {
                    lensEffect.update(size, radiusPx, placement.rootOrigin)
                    if (lensLayer.renderEffect != lensEffect.composeEffect) {
                        lensLayer.renderEffect = lensEffect.composeEffect
                    }
                }
                clipPath(path) { drawLayer(lensLayer) }
                drawPath(path, color = fill)
                if (lit) drawPath(path, color = rim, style = Stroke(width = rimWidth))
                drawContent()
            }
        }
}
