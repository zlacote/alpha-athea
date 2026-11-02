package iatheiya.ayana.ui.common

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
 */
@Composable
fun AyanaLight(modifier: Modifier = Modifier) {
    val light = remember { SpotLight() }
    Canvas(modifier) {
        // The light reaches a little past its square at the ends; drawn at four fifths, it fits.
        val s = size.minDimension * 0.8f
        drawIntoCanvas { light.draw(it.nativeCanvas, (size.width - s) / 2, (size.height - s) / 2, s) }
    }
}

/** How far a spot reaches, in its own radii, before its light is too faint to matter. */
private const val REACH = 3f

/** Stops of exp(-r²/2) from the centre out to [REACH]. */
private val PROFILE_AT = floatArrayOf(0f, 1f / 6, 2f / 6, 3f / 6, 4f / 6, 5f / 6, 1f)
private val PROFILE = floatArrayOf(1f, .8825f, .6065f, .3247f, .1353f, .0439f, 0f)

private class SpotLight {
    private val count = LIGHT_SPOTS.size / LIGHT_SPOT_FIELDS
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { blendMode = BlendMode.PLUS }
    private val shaders: Array<Shader>
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
    }

    fun draw(canvas: android.graphics.Canvas, left: Float, top: Float, side: Float) {
        for (i in 0 until count) {
            val o = i * LIGHT_SPOT_FIELDS
            paint.shader = shaders[i]
            canvas.save()
            canvas.translate(left + LIGHT_SPOTS[o] * side, top + LIGHT_SPOTS[o + 1] * side)
            canvas.rotate(Math.toDegrees(LIGHT_SPOTS[o + 4].toDouble()).toFloat())
            canvas.scale(LIGHT_SPOTS[o + 2] * side, LIGHT_SPOTS[o + 3] * side)
            canvas.drawCircle(0f, 0f, REACH, paint)
            canvas.restore()
        }
    }
}

private fun argb(alpha: Float, r: Float, g: Float, b: Float): Int =
    ((alpha.coerceIn(0f, 1f) * 255 + .5f).toInt() shl 24) or
        ((r * 255 + .5f).toInt() shl 16) or ((g * 255 + .5f).toInt() shl 8) or (b * 255 + .5f).toInt()
