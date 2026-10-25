package iatheiya.ayana.ui.liquid

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import android.os.Build
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
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
import iatheiya.ayana.ui.theme.ChromeTint
import iatheiya.ayana.ui.theme.glass
import iatheiya.ayana.ui.theme.glassFill
import iatheiya.ayana.ui.theme.solidChrome
import iatheiya.ayana.ui.theme.toPath
import iatheiya.ayana.util.AyanaLog
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

    /**
     * Window-root position of the recorded content. Glass surfaces subtract it to align. It is
     * snapshot state so a glass surface redraws the moment its position is known — without it the
     * first draw used a stale zero offset (sampling empty pixels) and nothing short of an unrelated
     * redraw, such as leaving and re-entering the app, ever corrected it.
     */
    internal var rootOrigin: Offset by mutableStateOf(Offset.Zero)
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

/** Where a glass surface sits in window coordinates, updated during layout. Snapshot state so the
 *  draw re-runs with the right offset as soon as layout reports the position. */
private class GlassPlacement {
    var rootOrigin: Offset by mutableStateOf(Offset.Zero)
}

/**
 * Glass surface that refracts [backdrop] inside [shape]. With a null backdrop it is
 * the plain glass of [iatheiya.ayana.ui.theme.glass]. Below Android 13 the backdrop
 * is shown without the bend, still with the base and the rim. [flat] surfaces, such as
 * bars along a screen edge, are blurred but not bent or highlighted; see [LensEffect.effectFor].
 */
@Composable
fun Modifier.liquidGlass(
    backdrop: LiquidBackdrop?,
    shape: Shape,
    cornerRadius: Dp,
    lit: Boolean = true,
    strong: Boolean = false,
    clear: Boolean = false,
    flat: Boolean = false,
    /** The radius of the two bottom corners, when they differ from the top ones. */
    bottomCornerRadius: Dp = cornerRadius,
): Modifier {
    // A render effect is only drawn through an offscreen buffer; without it the lens is dropped.
    val lensLayer = rememberGraphicsLayer().apply {
        compositingStrategy = CompositingStrategy.Offscreen
    }
    val lensEffect = rememberLensEffect()
    val placement = remember { GlassPlacement() }
    // Glass switched off: the whole interface falls back to one solid, opaque
    // tone (the tone the menus use) rather than going translucent.
    if (!GlassTuning.enabled) {
        return solidChrome(shape, MaterialTheme.colorScheme.surfaceContainerHigh, lit)
    }
    // Glass on, but this surface has no backdrop to sample: non-refracting glass.
    if (backdrop == null) return glass(shape, lit, strong, clear)
    val fill = glassFill(strong, clear)
    // The fill under the lens is a white sheen over the lensed backdrop.
    val sheen = Color.White.copy(alpha = GlassTuning.fillAlpha)
    val rim = Color.White.copy(alpha = GlassTuning.rimAlpha)
    return this
        .onGloballyPositioned { placement.rootOrigin = it.positionInRoot() }
        .drawWithCache {
            val path = shape.createOutline(size, layoutDirection, this).toPath()
            val rimWidth = 1.dp.toPx()
            val radiusPx = cornerRadius.toPx()
            val bottomRadiusPx = bottomCornerRadius.toPx()
            val pixels = IntSize(size.width.roundToInt(), size.height.roundToInt())
            var logged = false
            onDrawWithContent {
                // Record the backdrop, moved so that it lines up with this surface.
                val offset = placement.rootOrigin - backdrop.rootOrigin
                if (!logged) {
                    logged = true
                    AyanaLog.log("lens", "surface size=$pixels offset=$offset shaded=${lensEffect != null}")
                }
                lensLayer.record(this@onDrawWithContent, layoutDirection, pixels) {
                    translate(-offset.x, -offset.y) {
                        drawLayer(backdrop.layer)
                    }
                }
                if (lensEffect != null) {
                    val effect = lensEffect.effectFor(size, radiusPx, flat, bottomRadiusPx)
                    if (lensLayer.renderEffect != effect) lensLayer.renderEffect = effect
                }
                clipPath(path) { drawLayer(lensLayer) }
                drawPath(path, color = fill)
                ChromeTint.color?.let { drawPath(path, color = it.copy(alpha = ChromeTint.GLASS_ALPHA)) }
                drawPath(path, color = sheen)
                if (lit) drawPath(path, color = rim, style = Stroke(width = rimWidth))
                drawContent()
            }
        }
}

/**
 * A gap inside a glass surface, such as the line between the composer and the key row or between
 * two keys. It is not cut out to the page (text behind would show through it, sharp) and not
 * left as glass: it shows the backdrop under a much stronger blur, without the glass fill. So the
 * surface reads as parts of one panel, while nothing behind it is legible.
 *
 * With glass off, or where the blur cannot be drawn (before Android 12), the gap is a plain cutout
 * in the page colour, like the dividers in the settings cards.
 */
@Composable
fun Modifier.backdropGap(backdrop: LiquidBackdrop?, blur: Dp = GAP_BLUR): Modifier {
    val layer = rememberGraphicsLayer()
    val placement = remember { GlassPlacement() }
    val cutout = MaterialTheme.colorScheme.background
    if (!GlassTuning.enabled || backdrop == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        return drawBehind { drawRect(cutout) }
    }
    return this
        .onGloballyPositioned { placement.rootOrigin = it.positionInRoot() }
        .drawBehind {
            val radius = blur.toPx()
            if (layer.renderEffect == null) layer.renderEffect = BlurEffect(radius, radius, TileMode.Clamp)
            // Recorded with a margin of the blur radius on every side, so the blur at the gap's
            // edges is made of the real neighbouring backdrop, not of clamped edge pixels.
            val margin = radius * 2
            val offset = placement.rootOrigin - backdrop.rootOrigin
            val pixels = IntSize((size.width + margin * 2).roundToInt(), (size.height + margin * 2).roundToInt())
            layer.record(this, layoutDirection, pixels) {
                translate(margin - offset.x, margin - offset.y) { drawLayer(backdrop.layer) }
            }
            clipRect { translate(-margin, -margin) { drawLayer(layer) } }
        }
}

private val GAP_BLUR = 24.dp
