package iatheiya.ayana.ui.common

import androidx.compose.runtime.FloatState
import androidx.compose.runtime.MutableFloatState
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.composed
import android.graphics.Bitmap
import android.graphics.BlendMode
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * The light of the app icon, drawn from the same spots ([LIGHT_SPOTS]), added together, at the
 * size it is given: crisp at any size because it is drawn, not scaled. Its shape stays still; its
 * light shimmers: a soft swell runs along it, and its hues drift a little warmer and cooler.
 *
 * The spots are drawn once, into a picture of the light; each frame only lays that picture down
 * twice (a dim pass, and a bright pass seen through the moving swell) with its hues turned. So the
 * shimmer costs a couple of draws a frame, not hundreds.
 */
@Composable
fun AyanaLight(modifier: Modifier = Modifier, shimmering: Boolean = true) {
    val light = remember { SpotLight() }
    val cycle = rememberShimmerCycle(shimmering)
    Canvas(modifier) {
        val w = size.width.roundToInt()
        val h = size.height.roundToInt()
        if (w <= 0 || h <= 0) return@Canvas
        // Read here, while drawing, so each new frame of the shimmer redraws the light directly.
        val now = if (shimmering) cycle.floatValue else null
        drawIntoCanvas { light.draw(it.nativeCanvas, w, h, now) }
    }
}

/**
 * Where the shimmer is in its cycle, from 0 to 1. Its clock runs only while frames are drawn and
 * steps over any pause (the app in the background, a menu over it): after one, the shimmer goes
 * on from where it stopped, instead of leaping or racing to catch up.
 */
@Composable
fun rememberShimmerCycle(running: Boolean = true): MutableFloatState {
    val cycle = remember { mutableFloatStateOf(0f) }
    if (running) {
        LaunchedEffect(Unit) {
            var last = withFrameNanos { it }
            while (true) {
                withFrameNanos { now ->
                    val step = (now - last).coerceIn(0L, MAX_STEP_NANOS)
                    last = now
                    cycle.floatValue = (cycle.floatValue + step.toFloat() / CYCLE_NANOS) % 1f
                }
            }
        }
    }
    return cycle
}

/** The longest step the shimmer's clock takes between two frames; anything longer was a pause. */
private const val MAX_STEP_NANOS = 50_000_000L

/**
 * Fills what this draws (a name, say) with the icon's own light: the light is laid over it,
 * stretched to run corner to corner across it, and shows only where it draws, so its colours, its
 * fibres and its swell flow through the letters. [cycle] shimmers it; null keeps it still.
 */
fun Modifier.filledWithLight(cycle: FloatState?): Modifier = composed {
    val light = remember { SpotLight() }
    val atop = remember { Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP) } }
    graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen).drawWithContent {
        drawContent()
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@drawWithContent
        drawIntoCanvas { canvas ->
            val c = canvas.nativeCanvas
            // The light's own square, a little larger than the letters so its ends reach their
            // corners, squeezed to their height.
            val padX = w * TEXT_PAD_X
            val padY = h * TEXT_PAD_Y
            val side = (w + 2 * padX).roundToInt().coerceAtLeast(1)
            val layer = c.saveLayer(0f, 0f, w, h, atop)
            c.translate(-padX, -padY)
            c.scale(1f, (h + 2 * padY) / side)
            // Read while drawing, so each frame of the shimmer redraws the letters directly.
            light.draw(c, side, side, cycle?.floatValue)
            c.restoreToCount(layer)
        }
    }
}

private const val TEXT_PAD_X = 0.12f
private const val TEXT_PAD_Y = 0.35f

/** One full shimmer, after which every motion is back where it began. */
private const val CYCLE_NANOS = 4_500_000_000L

/** How far a spot reaches, in its own radii, before its light is too faint to matter. */
private const val REACH = 3f

/** Stops of exp(-r²/2) from the centre out to [REACH]. */
private val PROFILE_AT = floatArrayOf(0f, 1f / 6, 2f / 6, 3f / 6, 4f / 6, 5f / 6, 1f)
private val PROFILE = floatArrayOf(1f, .8825f, .6065f, .3247f, .1353f, .0439f, 0f)

private class SpotLight {
    private val count = LIGHT_SPOTS.size / LIGHT_SPOT_FIELDS
    private val spotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { blendMode = BlendMode.PLUS }
    private val shaders: Array<Shader> = Array(count) { i ->
        val o = i * LIGHT_SPOT_FIELDS
        val r = LIGHT_SPOTS[o + 5]
        val g = LIGHT_SPOTS[o + 6]
        val b = LIGHT_SPOTS[o + 7]
        // The colour at full strength, and its strength carried in the alpha, so that added
        // together the spots give exactly the light they were fitted to.
        val m = maxOf(r, g, b, 1e-4f).coerceAtMost(1f)
        val colours = IntArray(PROFILE.size) { k ->
            argb(m * PROFILE[k], (r / m).coerceAtMost(1f), (g / m).coerceAtMost(1f), (b / m).coerceAtMost(1f))
        }
        RadialGradient(0f, 0f, REACH, colours, PROFILE_AT, Shader.TileMode.CLAMP)
    }

    private var picture: Bitmap? = null
    private val layPaint = Paint(Paint.FILTER_BITMAP_FLAG).apply { blendMode = BlendMode.PLUS }
    private val layerPaint = Paint().apply { blendMode = BlendMode.PLUS }
    private val swellPaint = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN) }
    private val hue = ColorMatrix()

    fun draw(canvas: android.graphics.Canvas, w: Int, h: Int, cycle: Float?) {
        val image = picture?.takeIf { it.width == w && it.height == h } ?: paint(w, h).also { picture = it }
        if (cycle == null) {
            layPaint.colorFilter = null
            layPaint.alpha = 255
            canvas.drawBitmap(image, 0f, 0f, layPaint)
            return
        }
        val phase = (2 * PI * cycle).toFloat()
        hue.setHueRotation(HUE_DRIFT * sin(phase))
        layPaint.colorFilter = ColorMatrixColorFilter(hue)
        // The dim pass: all of the light, at the floor it never goes under.
        layPaint.alpha = (255 * SWELL_FLOOR).toInt()
        canvas.drawBitmap(image, 0f, 0f, layPaint)
        // The bright pass, seen only through a band that travels from the lower end to the upper.
        val band = w / SWELL_WAVES
        val shift = band * cycle
        swellPaint.shader = LinearGradient(
            shift, h - shift, shift + band, h - shift - band,
            SWELL_COLOURS, SWELL_AT, Shader.TileMode.REPEAT,
        )
        val layer = canvas.saveLayer(0f, 0f, w.toFloat(), h.toFloat(), layerPaint)
        layPaint.alpha = (255 * (1 - SWELL_FLOOR)).toInt()
        canvas.drawBitmap(image, 0f, 0f, layPaint)
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), swellPaint)
        canvas.restoreToCount(layer)
    }

    /** The light drawn once at [w]×[h], sized by the width: it runs corner to corner. */
    private fun paint(w: Int, h: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = android.graphics.Canvas(bitmap)
        val side = w * 0.8f
        val left = (w - side) / 2
        val top = (h - side) / 2
        for (i in 0 until count) {
            val o = i * LIGHT_SPOT_FIELDS
            spotPaint.shader = shaders[i]
            c.save()
            c.translate(left + LIGHT_SPOTS[o] * side, top + LIGHT_SPOTS[o + 1] * side)
            c.rotate(Math.toDegrees(LIGHT_SPOTS[o + 4].toDouble()).toFloat())
            c.scale(LIGHT_SPOTS[o + 2] * side, LIGHT_SPOTS[o + 3] * side)
            c.drawCircle(0f, 0f, REACH, spotPaint)
            c.restore()
        }
        return bitmap
    }
}

/** Turns a colour matrix into a rotation of hue by [degrees], keeping brightness. */
private fun ColorMatrix.setHueRotation(degrees: Float) {
    val r = Math.toRadians(degrees.toDouble())
    val c = cos(r).toFloat()
    val s = sin(r).toFloat()
    set(
        floatArrayOf(
            .213f + c * .787f - s * .213f, .715f - c * .715f - s * .715f, .072f - c * .072f + s * .928f, 0f, 0f,
            .213f - c * .213f + s * .143f, .715f + c * .285f + s * .140f, .072f - c * .072f - s * .283f, 0f, 0f,
            .213f - c * .213f - s * .787f, .715f - c * .715f + s * .715f, .072f + c * .928f + s * .072f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f,
        ),
    )
}

/** The swell's profile over one period: a smooth rise and fall, (1 - cos)/2, as alpha on white. */
private val SWELL_AT = FloatArray(13) { it / 12f }
private val SWELL_COLOURS = IntArray(13) { k ->
    val a = (1 - cos(2 * PI * k / 12)) / 2
    ((a * 255 + .5).toInt() shl 24) or 0xFFFFFF
}

/** How many swells lie along the light at once. */
private const val SWELL_WAVES = 1.3f
/** The dimmest a part of the light gets between swells. */
private const val SWELL_FLOOR = 0.45f
/** How far the hues drift either way, in degrees. */
private const val HUE_DRIFT = 24f

private fun argb(alpha: Float, r: Float, g: Float, b: Float): Int =
    ((alpha.coerceIn(0f, 1f) * 255 + .5f).toInt() shl 24) or
        ((r * 255 + .5f).toInt() shl 16) or ((g * 255 + .5f).toInt() shl 8) or (b * 255 + .5f).toInt()
