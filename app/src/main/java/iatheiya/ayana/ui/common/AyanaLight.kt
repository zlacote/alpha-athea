package iatheiya.ayana.ui.common

import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.PI
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import android.graphics.ColorMatrixColorFilter
import android.graphics.ColorMatrix
import android.graphics.BlendMode
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas

/**
 * The light of the app icon, drawn from the same spots ([LIGHT_SPOTS]), added together, at the
 * size it is given: the emblem of the About room, crisp at any size because it is drawn, not scaled.
 * Its shape stays still; its light shimmers: a soft swell runs along it, and its hues drift a little
 * warmer and cooler and back.
 */
@Composable
fun AyanaLight(modifier: Modifier = Modifier) {
    val light = remember { SpotLight() }
    var cycle by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (true) {
            withFrameNanos { now -> cycle = ((now - start) % CYCLE_NANOS).toFloat() / CYCLE_NANOS }
        }
    }
    Canvas(modifier) {
        // The light reaches a little past its square at the ends; drawn at four fifths, it fits.
        val s = size.minDimension * 0.8f
        drawIntoCanvas { light.draw(it.nativeCanvas, (size.width - s) / 2, (size.height - s) / 2, s, cycle) }
    }
}

/** One full shimmer, after which every motion is back where it began. */
private const val CYCLE_NANOS = 9_000_000_000L

/** How far a spot reaches, in its own radii, before its light is too faint to matter. */
private const val REACH = 3f

/** Stops of exp(-r²/2) from the centre out to [REACH]. */
private val PROFILE_AT = floatArrayOf(0f, 1f / 6, 2f / 6, 3f / 6, 4f / 6, 5f / 6, 1f)
private val PROFILE = floatArrayOf(1f, .8825f, .6065f, .3247f, .1353f, .0439f, 0f)

private class SpotLight {
    private val count = LIGHT_SPOTS.size / LIGHT_SPOT_FIELDS
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { blendMode = BlendMode.PLUS }
    private val shaders: Array<Shader>
    /** Where each spot lies along the light, from one end (0) to the other (1). */
    private val along = FloatArray(count)
    private val hue = ColorMatrix()

    init {
        // The light runs corner to corner; a spot's place along it is its share of that diagonal.
        for (i in 0 until count) {
            val o = i * LIGHT_SPOT_FIELDS
            along[i] = ((LIGHT_SPOTS[o] + 1f - LIGHT_SPOTS[o + 1]) / 2f).coerceIn(0f, 1f)
        }
        shaders = Array(count) { i ->
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
    }

    fun draw(canvas: android.graphics.Canvas, left: Float, top: Float, side: Float, cycle: Float) {
        val phase = (2 * PI * cycle).toFloat()
        hue.setHueRotation(HUE_DRIFT * sin(phase))
        paint.colorFilter = ColorMatrixColorFilter(hue)
        for (i in 0 until count) {
            val o = i * LIGHT_SPOT_FIELDS
            paint.shader = shaders[i]
            // The swell: a broad band of extra light travelling from end to end.
            val swell = 0.5f + 0.5f * cos(phase - along[i] * SWELL_WAVES * 2 * PI.toFloat())
            paint.alpha = (255 * (SWELL_FLOOR + (1 - SWELL_FLOOR) * swell)).toInt()
            canvas.save()
            canvas.translate(left + LIGHT_SPOTS[o] * side, top + LIGHT_SPOTS[o + 1] * side)
            canvas.rotate(Math.toDegrees(LIGHT_SPOTS[o + 4].toDouble()).toFloat())
            canvas.scale(LIGHT_SPOTS[o + 2] * side, LIGHT_SPOTS[o + 3] * side)
            canvas.drawCircle(0f, 0f, REACH, paint)
            canvas.restore()
        }
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

/** How many swells lie along the light at once. */
private const val SWELL_WAVES = 0.8f
/** The dimmest a part of the light gets between swells. */
private const val SWELL_FLOOR = 0.72f
/** How far the hues drift either way, in degrees. */
private const val HUE_DRIFT = 14f

private fun argb(alpha: Float, r: Float, g: Float, b: Float): Int =
    ((alpha.coerceIn(0f, 1f) * 255 + .5f).toInt() shl 24) or
        ((r * 255 + .5f).toInt() shl 16) or ((g * 255 + .5f).toInt() shl 8) or (b * 255 + .5f).toInt()
