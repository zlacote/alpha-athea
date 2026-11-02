package iatheiya.ayana.ui.common

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.res.painterResource
import iatheiya.ayana.R

/**
 * The light of the app icon, alive: a bundle of soft veils of light woven about one diagonal, a warm
 * and a cool ribbon twisted together with lighter sheets and hairs around them. Where they cross,
 * their light adds up to white. The bundle turns about its axis, the nearer veils brighter and
 * wider. The icon is one moment of the same light.
 *
 * It is drawn by a shader (Android 13 and later); before that the icon's still light stands in.
 */
@Composable
fun AyanaLight(modifier: Modifier = Modifier) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        LivingLight(modifier)
    } else {
        Image(painterResource(R.drawable.ic_launcher_foreground), contentDescription = null, modifier = modifier)
    }
}

/**
 * Why the light's shader does not compile on this device, or null when it does (or the device has
 * no shaders). A debug build checks it at start, so the emulator run catches a broken shader.
 */
fun lightShaderError(): String? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        runCatching { RuntimeShader(LIGHT_AGSL) }.exceptionOrNull()?.let { it.message ?: it.toString() }
    } else {
        null
    }

/** One turn of the light, after which every motion is back where it began. */
private const val CYCLE_NANOS = 20_000_000_000L

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun LivingLight(modifier: Modifier) {
    val shader = remember { runCatching { RuntimeShader(LIGHT_AGSL) }.getOrNull() }
    if (shader == null) {
        Image(painterResource(R.drawable.ic_launcher_foreground), contentDescription = null, modifier = modifier)
        return
    }
    val brush = remember(shader) { ShaderBrush(shader) }
    var cycle by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (true) {
            withFrameNanos { now -> cycle = ((now - start) % CYCLE_NANOS).toFloat() / CYCLE_NANOS }
        }
    }
    Canvas(modifier) {
        shader.setFloatUniform("size", size.width, size.height)
        shader.setFloatUniform("cycle", cycle)
        drawRect(brush)
    }
}

/**
 * The light as one field over the square: every pixel asks how far it lies along the diagonal (t)
 * and across it (d), and adds up the veils and a common haze.
 */
private const val LIGHT_AGSL = """
uniform float2 size;
uniform float cycle;

const float TAU = 6.2831853;

float sq(float x) { return x * x; }

// One veil: a bright thin middle in a wide soft halo, waving about the axis with its own phase and
// turning with time; nearer (facing you) it is brighter and wider. Its light gathers about [mid].
float veil(float t, float d, float tc, float spindle, float ends, float amp, float cyc, float ph,
           float wc, float wh, float bright, float spin, float mid, float spread, float tilt, float line) {
    float theta = TAU * cyc * t + ph + TAU * spin * cycle;
    float off = amp * spindle * sin(theta) + tilt * (t - 0.5);
    float z = cos(theta);
    float dd = d - off;
    float along = exp(-sq((t - mid) / spread)) * ends;
    float w1 = 1.25 * wc * (0.55 + 0.45 * spindle) * (1.0 + 0.35 * z);
    float w2 = 0.8 * wh * (0.35 + 0.65 * spindle) * (1.0 + 0.25 * z);
    float lum = bright * (0.7 + 0.3 * z) * along;
    return lum * (line * exp(-sq(dd / w1)) + 0.45 * exp(-sq(dd / w2)));
}

half4 main(float2 frag) {
    float side0 = min(size.x, size.y);
    float2 p = (frag - 0.5 * (size - side0)) / side0;
    float2 A = float2(0.13, 0.89);
    float2 B = float2(0.87, 0.11);
    float L = length(B - A);
    float2 u = (B - A) / L;
    float2 n = float2(-u.y, u.x);
    float t = dot(p - A, u) / L;
    float d = dot(p - A, n);
    float tc = clamp(t, 0.0, 1.0);
    float spindle = 0.18 + 0.82 * pow(sin(3.14159265 * tc), 0.7);
    float ends = smoothstep(-0.06, 0.24, t) * smoothstep(-0.06, 0.24, 1.0 - t);

    float3 img = float3(0.0);
    // the white heart, near the axis
    img += veil(t, d, tc, spindle, ends, 0.006, 1.0, 0.0, 0.0035, 0.016, 0.85, 1.0, 0.48, 0.42, 0.0, 1.0) * float3(1.000, 0.957, 0.925);
    // the warm main ribbon
    img += veil(t, d, tc, spindle, ends, 0.048, 1.2, 0.3, 0.0055, 0.04, 1.0, 1.0, 0.48, 0.46, 0.0, 0.8) * float3(1.000, 0.588, 0.361);
    // the cool main ribbon, half a turn behind it
    img += veil(t, d, tc, spindle, ends, 0.04, 1.2, 3.44, 0.0055, 0.042, 1.0, 1.0, 0.52, 0.44, 0.0, 0.8) * float3(0.267, 0.529, 1.000);
    // violet, between them
    img += veil(t, d, tc, spindle, ends, 0.046, 1.25, 1.9, 0.004, 0.045, 0.6, 1.0, 0.46, 0.44, 0.01, 0.55) * float3(0.502, 0.306, 0.933);
    // lavender
    img += veil(t, d, tc, spindle, ends, 0.03, 1.6, 5.0, 0.003, 0.035, 0.45, 2.0, 0.55, 0.4, -0.01, 0.45) * float3(0.627, 0.490, 1.000);
    // apricot
    img += veil(t, d, tc, spindle, ends, 0.028, 1.0, 4.2, 0.0035, 0.03, 0.45, 1.0, 0.4, 0.36, 0.02, 0.5) * float3(1.000, 0.463, 0.306);
    // cyan
    img += veil(t, d, tc, spindle, ends, 0.022, 1.8, 2.4, 0.0025, 0.022, 0.4, 2.0, 0.64, 0.3, 0.0, 0.55) * float3(0.412, 0.765, 1.000);
    // pink
    img += veil(t, d, tc, spindle, ends, 0.034, 1.4, 0.8, 0.003, 0.045, 0.4, 1.0, 0.44, 0.4, 0.0, 0.3) * float3(0.910, 0.384, 0.737);
    // blue
    img += veil(t, d, tc, spindle, ends, 0.036, 1.1, 2.0, 0.003, 0.05, 0.4, 2.0, 0.58, 0.38, 0.01, 0.3) * float3(0.267, 0.529, 1.000);
    // a faint hair
    img += veil(t, d, tc, spindle, ends, 0.075, 0.9, 0.9, 0.0012, 0.03, 0.18, 1.0, 0.36, 0.36, 0.03, 0.35) * float3(0.910, 0.384, 0.737);
    // a faint hair
    img += veil(t, d, tc, spindle, ends, 0.085, 0.8, 2.6, 0.001, 0.028, 0.15, 1.0, 0.62, 0.34, -0.03, 0.35) * float3(0.627, 0.490, 1.000);
    // a faint hair
    img += veil(t, d, tc, spindle, ends, 0.095, 1.1, 4.4, 0.001, 0.026, 0.12, 1.0, 0.3, 0.32, 0.02, 0.35) * float3(0.502, 0.306, 0.933);

    // a common haze along the bundle: blue toward the top, violet toward the bottom
    float hz = exp(-sq(d / 0.06)) * spindle * ends * 0.05;
    img += hz * mix(float3(0.667, 0.431, 0.902), float3(0.471, 0.588, 1.0), tc);

    float3 rgb = 1.0 - exp(-img * 2.0);
    float alpha = max(rgb.r, max(rgb.g, rgb.b));
    return half4(half3(rgb), half(alpha));
}
"""

