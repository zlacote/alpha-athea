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
    val light = remember { SpotLight(along = false) }
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
    // It starts where a shimmer that had always been running would be now, not at its beginning,
    // so opening a screen with the light finds it already under way.
    val cycle = remember { mutableFloatStateOf((System.nanoTime() % CYCLE_NANOS).toFloat() / CYCLE_NANOS) }
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
 * Fills what this draws (a name, say) with the icon's own light: the light is turned to lie along
 * it, its length across the whole width, and shows only where it draws, so its colours, its fibres
 * and its swell flow through the letters from one end to the other. Not stretched: stretched to a
 * word's flat shape its fibres smeared into a plain gradient. [cycle] shimmers it; null keeps it
 * still.
 */
fun Modifier.filledWithLight(cycle: FloatState?): Modifier = composed {
    val light = remember { SpotLight(along = true) }
    val atop = remember { Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP) } }
    graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen).drawWithContent {
        drawContent()
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@drawWithContent
        drawIntoCanvas { canvas ->
            val c = canvas.nativeCanvas
            val layer = c.saveLayer(0f, 0f, w, h, atop)
            // Read while drawing, so each frame of the shimmer redraws the letters directly.
            light.draw(c, w.roundToInt().coerceAtLeast(1), h.roundToInt().coerceAtLeast(1), cycle?.floatValue)
            c.restoreToCount(layer)
        }
    }
}

/** How much of the light's length a word spans, in the light's own units (its square is 1). */
private const val ALONG_SPAN = 0.8f
/** How much brighter the light is laid into letters, which are thin next to the emblem. */
private const val ALONG_GAIN = 1.6f

/** One full shimmer, after which every motion is back where it began. */
private const val CYCLE_NANOS = 4_500_000_000L

/** How far a spot reaches, in its own radii, before its light is too faint to matter. */
private const val REACH = 3f

/** Stops of exp(-r²/2) from the centre out to [REACH]. */
private val PROFILE_AT = floatArrayOf(0f, 1f / 6, 2f / 6, 3f / 6, 4f / 6, 5f / 6, 1f)
private val PROFILE = floatArrayOf(1f, .8825f, .6065f, .3247f, .1353f, .0439f, 0f)

/**
 * The light, drawn from its spots. As an emblem ([along] false) it fills a square in the middle of
 * the box it is given; that picture is the same at any box, so it is painted once for the whole
 * app, off the main thread when [warmLight] is called ahead. Laid [along] a word it is turned so
 * its length runs across the box, and its swell runs from the word's start to its end.
 */
private class SpotLight(private val along: Boolean) {
    private var picture: Bitmap? = null
    private val layPaint = Paint(Paint.FILTER_BITMAP_FLAG).apply { blendMode = BlendMode.PLUS }
    private val layerPaint = Paint().apply { blendMode = BlendMode.PLUS }
    private val swellPaint = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN) }
    private val hue = ColorMatrix()
    private val gain = if (along) ALONG_GAIN else 1f
    private val dst = android.graphics.RectF()

    fun draw(canvas: android.graphics.Canvas, w: Int, h: Int, cycle: Float?) {
        val image: Bitmap
        if (along) {
            image = picture?.takeIf { it.width == w && it.height == h } ?: Spots.paintAlong(w, h).also { picture = it }
            dst.set(0f, 0f, w.toFloat(), h.toFloat())
        } else {
            // The picture is a square as wide as the box, the emblem in its middle; it reaches past
            // a box shorter than it is wide, so it is cut to the box, as it was painted before.
            image = Spots.emblem(w)
            dst.set(0f, (h - w) / 2f, w.toFloat(), (h + w) / 2f)
            canvas.save()
            canvas.clipRect(0, 0, w, h)
        }
        drawLight(canvas, image, w, h, cycle)
        if (!along) canvas.restore()
    }

    private fun drawLight(canvas: android.graphics.Canvas, image: Bitmap, w: Int, h: Int, cycle: Float?) {
        if (cycle == null) {
            hue.reset()
            hue.setScale(1f, 1f, 1f, gain)
            layPaint.colorFilter = ColorMatrixColorFilter(hue)
            layPaint.alpha = 255
            canvas.drawBitmap(image, null, dst, layPaint)
            return
        }
        val phase = (2 * PI * cycle).toFloat()
        hue.setHueRotation(HUE_DRIFT * sin(phase), gain)
        layPaint.colorFilter = ColorMatrixColorFilter(hue)
        // The dim pass: all of the light, at the floor it never goes under.
        layPaint.alpha = (255 * SWELL_FLOOR).toInt()
        canvas.drawBitmap(image, null, dst, layPaint)
        // The bright pass, seen only through a band that travels along the light: from the lower
        // end to the upper on the emblem, from the start of the word to its end along one.
        swellPaint.shader = if (along) {
            val band = w / ALONG_WAVES
            val shift = band * cycle
            LinearGradient(shift, 0f, shift + band, 0f, SWELL_COLOURS, SWELL_AT, Shader.TileMode.REPEAT)
        } else {
            val band = w / SWELL_WAVES
            val shift = band * cycle
            LinearGradient(shift, h - shift, shift + band, h - shift - band, SWELL_COLOURS, SWELL_AT, Shader.TileMode.REPEAT)
        }
        val layer = canvas.saveLayer(0f, 0f, w.toFloat(), h.toFloat(), layerPaint)
        layPaint.alpha = (255 * (1 - SWELL_FLOOR)).toInt()
        canvas.drawBitmap(image, null, dst, layPaint)
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), swellPaint)
        canvas.restoreToCount(layer)
    }
}

/** The emblem's square, as a share of the width of the box it is shown in. */
private const val EMBLEM_SHARE = 0.8f
private const val EMBLEM_MARGIN = (1 - EMBLEM_SHARE) / 2
/** How many swells lie along a word at once. */
private const val ALONG_WAVES = 1.04f

/**
 * Paints the light ahead, off the main thread, for an emblem [widthPx] wide (the width of the box
 * it will be shown in), so the screen that shows it opens on its first frame without stopping to
 * paint it.
 */
fun warmLight(widthPx: Int) {
    if (widthPx > 0) Spots.emblem(widthPx)
}

/** The spots, and the pictures painted from them. */
private object Spots {
    private val count = LIGHT_SPOTS.size / LIGHT_SPOT_FIELDS
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

    /** The light's middle and the direction it runs in, weighted by how much light each spot gives. */
    private val axis: FloatArray by lazy {
        var sw = 0.0; var mx = 0.0; var my = 0.0
        val wt = DoubleArray(count) { i ->
            val o = i * LIGHT_SPOT_FIELDS
            (LIGHT_SPOTS[o + 5] + LIGHT_SPOTS[o + 6] + LIGHT_SPOTS[o + 7]).toDouble() * LIGHT_SPOTS[o + 2] * LIGHT_SPOTS[o + 3]
        }
        for (i in 0 until count) {
            sw += wt[i]; mx += wt[i] * LIGHT_SPOTS[i * LIGHT_SPOT_FIELDS]; my += wt[i] * LIGHT_SPOTS[i * LIGHT_SPOT_FIELDS + 1]
        }
        mx /= sw; my /= sw
        var cxx = 0.0; var cyy = 0.0; var cxy = 0.0
        for (i in 0 until count) {
            val dx = LIGHT_SPOTS[i * LIGHT_SPOT_FIELDS] - mx
            val dy = LIGHT_SPOTS[i * LIGHT_SPOT_FIELDS + 1] - my
            cxx += wt[i] * dx * dx; cyy += wt[i] * dy * dy; cxy += wt[i] * dx * dy
        }
        // The direction of the greatest spread, pointing rightwards.
        var angle = 0.5 * kotlin.math.atan2(2 * cxy, cxx - cyy)
        if (cos(angle) < 0) angle += PI
        floatArrayOf(mx.toFloat(), my.toFloat(), angle.toFloat())
    }

    @Volatile private var emblemPicture: Bitmap? = null

    /**
     * The emblem for a box [width] pixels wide: a square that wide, the emblem's own square in its
     * middle ([EMBLEM_SHARE] of it). Painted once; one already painted serves while it is at least
     * as large, and close enough in size to stay sharp when laid down smaller.
     */
    fun emblem(width: Int): Bitmap = synchronized(this) {
        emblemPicture?.takeIf { it.width >= width && it.width <= width * 3 / 2 } ?: paint(width, width) { c ->
            c.translate(width * EMBLEM_MARGIN, width * EMBLEM_MARGIN)
            c.scale(width * EMBLEM_SHARE, width * EMBLEM_SHARE)
        }.also { emblemPicture = it }
    }

    /** The light turned to run along a [w]×[h] box, [ALONG_SPAN] of its length across the width. */
    fun paintAlong(w: Int, h: Int): Bitmap = paint(w, h) { c ->
        val (mx, my, angle) = axis
        val k = w / ALONG_SPAN
        c.translate(w / 2f, h / 2f)
        c.scale(k, k)
        c.rotate(-Math.toDegrees(angle.toDouble()).toFloat())
        c.translate(-mx, -my)
    }

    private inline fun paint(w: Int, h: Int, place: (android.graphics.Canvas) -> Unit): Bitmap {
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = android.graphics.Canvas(bitmap)
        place(c)
        val spotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { blendMode = BlendMode.PLUS }
        for (i in 0 until count) {
            val o = i * LIGHT_SPOT_FIELDS
            spotPaint.shader = shaders[i]
            c.save()
            c.translate(LIGHT_SPOTS[o], LIGHT_SPOTS[o + 1])
            c.rotate(Math.toDegrees(LIGHT_SPOTS[o + 4].toDouble()).toFloat())
            c.scale(LIGHT_SPOTS[o + 2], LIGHT_SPOTS[o + 3])
            c.drawCircle(0f, 0f, REACH, spotPaint)
            c.restore()
        }
        return bitmap
    }
}

/** Turns a colour matrix into a rotation of hue by [degrees], keeping brightness; [alphaScale] strengthens it. */
private fun ColorMatrix.setHueRotation(degrees: Float, alphaScale: Float = 1f) {
    val r = Math.toRadians(degrees.toDouble())
    val c = cos(r).toFloat()
    val s = sin(r).toFloat()
    set(
        floatArrayOf(
            .213f + c * .787f - s * .213f, .715f - c * .715f - s * .715f, .072f - c * .072f + s * .928f, 0f, 0f,
            .213f - c * .213f + s * .143f, .715f + c * .285f + s * .140f, .072f - c * .072f - s * .283f, 0f, 0f,
            .213f - c * .213f - s * .787f, .715f - c * .715f + s * .715f, .072f + c * .928f + s * .072f, 0f, 0f,
            0f, 0f, 0f, alphaScale, 0f,
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
