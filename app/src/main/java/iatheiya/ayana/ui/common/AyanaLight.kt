package iatheiya.ayana.ui.common

import android.graphics.BlendMode
import android.graphics.BlurMaskFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin

/**
 * The light of the app icon, alive: a slash of white light with a warm and a cool fringe, soft
 * at both ends, with small fans of the spectrum leaking out from under it and a trail its lower
 * end comes apart into. The colours drift, and the fans and the trail slowly change their shape.
 * It loops seamlessly; every motion runs a whole number of times per cycle.
 */
@Composable
fun AyanaLight(modifier: Modifier = Modifier) {
    val phase by rememberInfiniteTransition(label = "light").animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(CYCLE_MS, easing = LinearEasing)),
        label = "phase",
    )
    val paint = remember { Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND } }
    Canvas(modifier) { drawLight(phase, paint) }
}

private const val CYCLE_MS = 24_000

/** The colours the fans and the trail are drawn from, red to violet, as RGB. */
private val SPECTRUM = arrayOf(
    intArrayOf(255, 105, 95), intArrayOf(255, 165, 85), intArrayOf(255, 225, 120), intArrayOf(140, 230, 150),
    intArrayOf(100, 205, 255), intArrayOf(100, 135, 255), intArrayOf(165, 110, 255),
)

/** How bright the body is along its length: from nothing, to full, and back to nothing. */
private val ENDS_AT = floatArrayOf(0f, .08f, .15f, .22f, .3f, .74f, .8f, .86f, .93f, 1f)
private val ENDS_ALPHA = floatArrayOf(0f, .04f, .22f, .58f, 1f, 1f, .58f, .22f, .04f, 0f)

/** Where a fan leaves the line, where it heads, to which side, how many rays, its first colour. */
private class Fan(val from: Float, val to: Float, val side: Int, val rays: Int, val firstColour: Int)

private val FANS = listOf(Fan(.70f, .50f, 1, 4, 1), Fan(.52f, .38f, 1, 3, 3), Fan(.44f, .22f, -1, 5, 0))

private fun DrawScope.drawLight(phase: Float, paint: Paint) {
    val s = size.minDimension
    val ox = (size.width - s) / 2
    val oy = (size.height - s) / 2
    val a = Offset(ox + .28f * s, oy + .84f * s)
    val b = Offset(ox + .72f * s, oy + .16f * s)
    val length = hypot(b.x - a.x, b.y - a.y)
    val ux = (b.x - a.x) / length
    val uy = (b.y - a.y) / length
    val nx = -uy
    val ny = ux
    val h = .021f * s
    fun at(t: Float) = Offset(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)
    fun wave(times: Int, shift: Float = 0f) = sin(phase * times + shift)

    drawIntoCanvas { canvas ->
        val c = canvas.nativeCanvas
        fun line(p: Offset, q: Offset, width: Float, rgb: IntArray, at: FloatArray, alpha: FloatArray, blur: Float, add: Boolean) {
            val colours = IntArray(at.size) { argb(alpha[it], rgb) }
            paint.shader = LinearGradient(p.x, p.y, q.x, q.y, colours, at, Shader.TileMode.CLAMP)
            paint.strokeWidth = width
            // A blur mask's radius spreads about 1.7 times its sigma.
            paint.maskFilter = if (blur > 0f) BlurMaskFilter(blur * 1.7f, BlurMaskFilter.Blur.NORMAL) else null
            paint.blendMode = if (add) BlendMode.PLUS else BlendMode.SRC_OVER
            c.drawLine(p.x, p.y, q.x, q.y, paint)
        }

        // The trail the lower end comes apart into.
        for (k in 0 until 6) {
            val f = (k + .5f) / 6
            val from = at(.62f - .04f * k)
            val spread = s * (.012f + .045f * f) * (1 + .25f * wave(3, k * 1.3f))
            val to = Offset(a.x - ux * length * .1f + nx * spread, a.y - uy * length * .1f + ny * spread)
            line(
                from, to, h * (.16f + .1f * (1 - f)), spectrum(4 + f * 2 + .5f * wave(1)),
                floatArrayOf(0f, .35f, .75f, 1f), floatArrayOf(0f, .7f - .35f * f, .25f, 0f), .8f, add = true,
            )
        }

        // Fans of light leaking out from under the line, each ray its own colour; they breathe.
        FANS.forEachIndexed { i, fan ->
            val origin = at(fan.from + .015f * wave(2, i * 2f))
            val reach = abs(fan.to - fan.from) * length * (1 + .15f * wave(3, i.toFloat()))
            val dir = if (fan.to < fan.from) -1 else 1
            for (k in 0 until fan.rays) {
                val f = k.toFloat() / max(1, fan.rays - 1)
                val angle = ((4 + 11 * f) * (1 + .35f * wave(3, i * 1.7f + k * .6f)) * PI / 180 * fan.side).toFloat()
                val dx = ux * dir
                val dy = uy * dir
                val ex = dx * cos(angle) - dy * sin(angle)
                val ey = dx * sin(angle) + dy * cos(angle)
                val tip = Offset(origin.x + ex * reach, origin.y + ey * reach)
                line(
                    origin, tip, h * (.24f - .08f * f), towardWhite(spectrum(fan.firstColour + k + .6f * wave(1, i.toFloat())), .15f),
                    floatArrayOf(0f, .1f, .55f, 1f), floatArrayOf(0f, 1f, .5f, 0f), .5f, add = true,
                )
            }
        }

        // The body: a wide cool glow, a warm and a cool fringe, a white core over the fans.
        val p = at(0f)
        val q = at(1f)
        val pulse = 1 + .06f * wave(2)
        line(p, q, h * 5 * pulse, intArrayOf(190, 205, 255), ENDS_AT, scaled(.16f), h * 1.6f, add = true)
        val off = h * .42f
        val sway = .5f + .5f * wave(2)
        line(
            Offset(p.x + nx * off, p.y + ny * off), Offset(q.x + nx * off, q.y + ny * off), h * .45f,
            mix(intArrayOf(255, 140, 95), intArrayOf(255, 190, 120), sway), ENDS_AT, scaled(.8f), h * .25f, add = true,
        )
        line(
            Offset(p.x - nx * off, p.y - ny * off), Offset(q.x - nx * off, q.y - ny * off), h * .45f,
            mix(intArrayOf(100, 145, 255), intArrayOf(110, 210, 255), 1 - sway), ENDS_AT, scaled(.85f), h * .25f, add = true,
        )
        line(p, q, h * 1.05f, intArrayOf(238, 243, 255), ENDS_AT, scaled(.95f), h * .35f, add = false)
        line(p, q, h * .45f, intArrayOf(255, 255, 255), ENDS_AT, ENDS_ALPHA, 0f, add = false)
    }
}

private fun scaled(k: Float) = FloatArray(ENDS_ALPHA.size) { ENDS_ALPHA[it] * k }

private fun argb(alpha: Float, rgb: IntArray): Int =
    ((alpha.coerceIn(0f, 1f) * 255).toInt() shl 24) or (rgb[0] shl 16) or (rgb[1] shl 8) or rgb[2]

private fun mix(from: IntArray, to: IntArray, f: Float) = IntArray(3) { (from[it] + (to[it] - from[it]) * f).toInt() }

private fun towardWhite(rgb: IntArray, f: Float) = mix(rgb, intArrayOf(255, 255, 255), f)

/** A colour along [SPECTRUM], wrapping around, blended between its neighbours. */
private fun spectrum(x: Float): IntArray {
    val n = SPECTRUM.size
    val w = ((x % n) + n) % n
    val i = floor(w).toInt()
    return mix(SPECTRUM[i], SPECTRUM[(i + 1) % n], w - i)
}
