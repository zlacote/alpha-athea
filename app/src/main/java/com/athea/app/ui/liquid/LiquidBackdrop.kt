package com.athea.app.ui.liquid

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.athea.app.ui.theme.glassBase
import com.athea.app.ui.theme.glassRim
import com.athea.app.ui.theme.toPath
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

@Composable
fun rememberLiquidBackdrop(): LiquidBackdrop {
    val layer = rememberGraphicsLayer()
    return remember(layer) { LiquidBackdrop(layer) }
}

/** Marks this subtree as backdrop content: it is drawn normally and recorded for glass. */
fun Modifier.liquidBackdrop(backdrop: LiquidBackdrop): Modifier =
    onGloballyPositioned { backdrop.rootOrigin = it.positionInRoot() }
        .drawWithContent {
            val pixels = IntSize(size.width.roundToInt(), size.height.roundToInt())
            backdrop.layer.record(density, layoutDirection, pixels) {
                this@drawWithContent.drawContent()
            }
            drawLayer(backdrop.layer)
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
    val lensLayer = rememberGraphicsLayer()
    val lensEffect = rememberLensEffect()
    val placement = remember { GlassPlacement() }
    if (backdrop == null) {
        return this.then(
            Modifier.drawWithCache {
                val path = shape.createOutline(size, layoutDirection, this).toPath()
                val base = glassBase(strong, clear)
                val rim = glassRim()
                val rimWidth = 1.dp.toPx()
                onDrawBehind {
                    drawPath(path, brush = base)
                    if (lit) drawPath(path, brush = rim, style = Stroke(width = rimWidth))
                }
            },
        )
    }
    val base = glassBase(strong, clear)
    val rim = glassRim()
    return this
        .onGloballyPositioned { placement.rootOrigin = it.positionInRoot() }
        .drawWithCache {
            val path = shape.createOutline(size, layoutDirection, this).toPath()
            val rimWidth = 1.dp.toPx()
            val radiusPx = cornerRadius.toPx()
            val pixels = IntSize(size.width.roundToInt(), size.height.roundToInt())
            onDrawWithContent {
                // Record the backdrop, moved so that it lines up with this surface.
                val offset = placement.rootOrigin - backdrop.rootOrigin
                lensLayer.record(density, layoutDirection, pixels) {
                    translate(-offset.x, -offset.y) {
                        drawLayer(backdrop.layer)
                    }
                }
                if (lensEffect != null) {
                    lensEffect.update(size, radiusPx)
                    if (lensLayer.renderEffect != lensEffect.composeEffect) {
                        lensLayer.renderEffect = lensEffect.composeEffect
                    }
                }
                clipPath(path) { drawLayer(lensLayer) }
                drawPath(path, brush = base)
                if (lit) drawPath(path, brush = rim, style = Stroke(width = rimWidth))
                drawContent()
            }
        }
}
