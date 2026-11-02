package iatheiya.ayana.ui.common

import android.graphics.BlendMode
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos

/**
 * The light of the app icon, alive. It is drawn from the same spots as the icon ([LIGHT_SPOTS]),
 * added together. Slowly, the bundle turns about its own axis: a twist runs along it, so in turn
 * each part narrows toward the axis and opens out again, and the part facing you is brighter.
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
        val s = size.minDimension
        drawIntoCanvas { light.draw(it.nativeCanvas, (size.width - s) / 2, (size.height - s) / 2, s, cycle) }
    }
}

/** One turn, after which every motion is back where it began. */
private const val CYCLE_NANOS = 16_000_000_000L

/** How far a spot reaches, in its own radii, before its light is too faint to matter. */
private const val REACH = 3f

/** Stops of exp(-r²/2) from the centre out to [REACH]. */
private val PROFILE_AT = floatArrayOf(0f, 1f / 6, 2f / 6, 3f / 6, 4f / 6, 5f / 6, 1f)
private val PROFILE = floatArrayOf(1f, .8825f, .6065f, .3247f, .1353f, .0439f, 0f)

private class SpotLight {
    private val count = LIGHT_SPOTS.size / LIGHT_SPOT_FIELDS
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { blendMode = BlendMode.PLUS }
    private val shaders: Array<Shader>
    // The bundle's own axis, so the turn can run along it.
    private val cx: Float
    private val cy: Float
    private val ax: Float
    private val ay: Float
    private val axisDegrees: Float

    init {
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
        // Light-weighted centre and principal direction of the spots.
        var w = 0f; var sx = 0f; var sy = 0f
        for (i in 0 until count) {
            val o = i * LIGHT_SPOT_FIELDS
            val wi = LIGHT_SPOTS[o + 5] + LIGHT_SPOTS[o + 6] + LIGHT_SPOTS[o + 7]
            w += wi; sx += wi * LIGHT_SPOTS[o]; sy += wi * LIGHT_SPOTS[o + 1]
        }
        cx = sx / w; cy = sy / w
        var cxx = 0f; var cyy = 0f; var cxy = 0f
        for (i in 0 until count) {
            val o = i * LIGHT_SPOT_FIELDS
            val wi = LIGHT_SPOTS[o + 5] + LIGHT_SPOTS[o + 6] + LIGHT_SPOTS[o + 7]
            val dx = LIGHT_SPOTS[o] - cx; val dy = LIGHT_SPOTS[o + 1] - cy
            cxx += wi * dx * dx; cyy += wi * dy * dy; cxy += wi * dx * dy
        }
        val angle = 0.5f * atan2(2 * cxy, cxx - cyy)
        ax = cos(angle); ay = kotlin.math.sin(angle)
        axisDegrees = Math.toDegrees(angle.toDouble()).toFloat()
    }

    fun draw(canvas: android.graphics.Canvas, left: Float, top: Float, side: Float, cycle: Float) {
        val phase = (2 * PI * cycle).toFloat()
        for (i in 0 until count) {
            val o = i * LIGHT_SPOT_FIELDS
            val dx = LIGHT_SPOTS[o] - cx
            val dy = LIGHT_SPOTS[o + 1] - cy
            val t = dx * ax + dy * ay                  // along the axis
            val d = -dx * ay + dy * ax                 // across it
            // The twist: how open the bundle is here and now, and how much this part faces you.
            val turn = phase - t * TWIST
            val c = cos(turn)
            val open = OPEN_MIN + (1 - OPEN_MIN) * (0.5f + 0.5f * c)
            val x = cx + t * ax - d * open * ay
            val y = cy + t * ay + d * open * ax
            paint.shader = shaders[i]
            paint.alpha = (255 * (FACING_MIN + (1 - FACING_MIN) * (0.5f + 0.5f * c))).toInt()
            canvas.save()
            canvas.translate(left + x * side, top + y * side)
            // Squeezed across the axis with the bundle, then the spot's own turn and radii.
            canvas.rotate(axisDegrees)
            canvas.scale(1f, open)
            canvas.rotate(-axisDegrees)
            canvas.rotate(Math.toDegrees(LIGHT_SPOTS[o + 4].toDouble()).toFloat())
            canvas.scale(LIGHT_SPOTS[o + 2] * side, LIGHT_SPOTS[o + 3] * side)
            canvas.drawCircle(0f, 0f, REACH, paint)
            canvas.restore()
        }
    }

    private companion object {
        /** Radians of twist per unit of length along the axis. */
        const val TWIST = 5f
        /** The narrowest the bundle closes to, as a share of its full width. */
        const val OPEN_MIN = 0.45f
        /** The dimmest a part turned away gets. */
        const val FACING_MIN = 0.7f
    }
}

private fun argb(alpha: Float, r: Float, g: Float, b: Float): Int =
    ((alpha.coerceIn(0f, 1f) * 255 + .5f).toInt() shl 24) or
        ((r * 255 + .5f).toInt() shl 16) or ((g * 255 + .5f).toInt() shl 8) or (b * 255 + .5f).toInt()
