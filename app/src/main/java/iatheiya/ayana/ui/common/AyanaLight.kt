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
 * The light of the app icon, alive. A ribbon of white light turns about its own axis: it narrows
 * and widens, its warm and cool edges trade sides as it turns, a glint runs along it, and fans of
 * the spectrum turn around it, passing in front of it and behind it, while its lower end comes
 * apart into a breathing trail. The icon is one moment of the same light.
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
 * The light as one field over the square: every pixel asks how far it lies along the line (t)
 * and across it (d), and adds up the body, its edges, its glow, the fans and the trail.
 */
private const val LIGHT_AGSL = """
uniform float2 size;
uniform float cycle;

const float PI = 3.14159265;
const float H = 0.026;
const float TWIST = 0.9;
const float3 WARM = float3(1.0, 0.588, 0.392);
const float3 COOL = float3(0.392, 0.627, 1.0);

float sm(float a, float b, float x) { return smoothstep(a, b, x); }
float sq(float x) { return x * x; }

float3 specAt(int i) {
    if (i == 0) return float3(1.0, 0.412, 0.373);
    if (i == 1) return float3(1.0, 0.647, 0.333);
    if (i == 2) return float3(1.0, 0.882, 0.471);
    if (i == 3) return float3(0.549, 0.902, 0.588);
    if (i == 4) return float3(0.392, 0.804, 1.0);
    if (i == 5) return float3(0.392, 0.529, 1.0);
    return float3(0.647, 0.431, 1.0);
}

// A colour along the spectrum, wrapping around, blended between neighbours.
float3 spec(float x) {
    float w = mod(x, 7.0);
    float i = floor(w);
    return mix(specAt(int(i)), specAt(int(mod(i + 1.0, 7.0))), w - i);
}

half4 main(float2 frag) {
    float side0 = min(size.x, size.y);
    float2 p = (frag - 0.5 * (size - side0)) / side0;
    float2 A = float2(0.2, 0.86);
    float2 B = float2(0.8, 0.14);
    float L = length(B - A);
    float2 u = (B - A) / L;
    float2 n = float2(-u.y, u.x);
    float t = dot(p - A, u) / L;
    float d = dot(p - A, n);
    float ph = 2.0 * PI * cycle;

    float inside = (t > 0.0 && t < 1.0) ? 1.0 : 0.0;
    float tc = clamp(t, 0.0, 1.0);
    float env = pow(sm(0.0, 0.3, tc), 1.7) * pow(sm(0.0, 0.26, 1.0 - tc), 1.7) * inside;
    float c = cos(2.0 * PI * TWIST * t + ph);
    float ac = abs(c);
    float hw = H * (0.42 + 0.58 * ac) * (0.6 + 0.4 * min(sm(0.0, 0.3, tc), sm(0.0, 0.26, 1.0 - tc)));
    float e = d / hw;

    float core = exp(-pow(abs(e / 0.8), 3.5)) * env;
    float k = sm(-0.3, 0.3, c);
    float3 left = mix(COOL, WARM, k);
    float3 right = mix(WARM, COOL, k);
    float fl = exp(-sq((e + 1.0) / 0.3)) * env;
    float fr = exp(-sq((e - 1.0) / 0.3)) * env;
    float tg = cycle * 1.4 - 0.2;
    float glint = exp(-sq((t - tg) / 0.1)) * pow(ac, 4.0) * env;
    float3 tint = spec(t * 2.2 + cycle * 7.0) * 0.14 + 0.86;

    float3 img = core * (0.72 + 0.35 * glint) * tint;
    img += exp(-sq(d / (H * 0.16))) * env * (0.55 + 0.5 * glint);
    img += fl * (0.55 + 0.4 * (1.0 - ac)) * left;
    img += fr * (0.55 + 0.4 * (1.0 - ac)) * right;
    img += exp(-pow(abs(d) / (hw * 1.8), 1.6)) * env * (0.12 + 0.3 * glint) * float3(0.8, 0.86, 1.0);
    img += exp(-abs(d) / (H * 7.0)) * env * 0.035 * float3(0.65, 0.7, 1.0);
    float cover = clamp(core * 1.4 + (fl + fr) * 0.4, 0.0, 1.0);

    // Fans: rays from one point, each a colour of the spectrum, turning about the line.
    for (int fi = 0; fi < 3; fi++) {
        float t0 = fi == 0 ? 0.70 : (fi == 1 ? 0.52 : 0.44);
        float t1 = fi == 0 ? 0.50 : (fi == 1 ? 0.38 : 0.22);
        float sideF = fi == 2 ? -1.0 : 1.0;
        float rays = fi == 0 ? 4.0 : (fi == 1 ? 3.0 : 5.0);
        float c0 = fi == 0 ? 1.0 : (fi == 1 ? 3.0 : 0.0);
        float spin = fi == 1 ? 2.0 : 1.0;
        float ffi = float(fi);
        float psi = ph * spin + ffi * 2.1;
        float proj = cos(psi);
        float depth = sin(psi);
        float span = abs(t1 - t0);
        float q = (t - t0) / (t1 - t0);
        float qc = clamp(q, 0.0, 1.0);
        float inq = (q > 0.0 && q < 1.0) ? 1.0 : 0.0;
        float along = qc * span * L;
        for (int kk = 0; kk < 5; kk++) {
            float fk = float(kk);
            if (fk < rays) {
                float f = fk / max(1.0, rays - 1.0);
                float ang = radians(6.0 + 16.0 * f) * (1.0 + 0.25 * sin(ph * 2.0 + ffi + fk));
                float off = sideF * (hw * 0.2 + along * tan(ang) * (0.35 + 0.65 * proj));
                float life = 0.7 + 0.3 * mod(fk * 37.0 + ffi * 11.0, 10.0) / 10.0;
                float vis = sm(0.0, 0.2, qc) * (1.0 - sm(0.25, life, qc)) * inq * clamp(env * 1.8, 0.0, 1.0);
                float w = H * (0.2 - 0.06 * f) * (1.0 + 1.2 * qc);
                float3 col = spec(c0 + fk + 0.7 * sin(ph + ffi)) * 0.8 + 0.2;
                float a = 1.6 * (1.0 - 0.3 * f) * (0.7 + 0.3 * depth);
                float m = (exp(-sq((d - off) / w)) + 0.25 * exp(-sq((d - off) / (w * 3.5)))) * vis * a;
                m *= 1.0 - cover * (depth < 0.0 ? 1.0 : 0.8);
                img += m * col;
            }
        }
    }

    // The trail the lower end comes apart into, breathing.
    for (int kk = 0; kk < 6; kk++) {
        float fk = float(kk);
        float f = (fk + 0.5) / 6.0;
        float t0 = 0.62 - 0.05 * fk;
        float t1 = -0.12;
        float q = (t0 - t) / (t0 - t1);
        float qc = clamp(q, 0.0, 1.0);
        float inq = (q > 0.0 && q < 1.0) ? 1.0 : 0.0;
        float spread = 0.07 * f * (1.0 + 0.3 * sin(ph * 3.0 + fk * 1.3));
        float off = hw * 0.2 + spread * pow(qc, 1.4);
        float vis = sm(0.0, 0.25, qc) * (1.0 - sm(0.45, 0.95, qc)) * inq * sm(0.0, 0.3, 1.0 - tc);
        float w = H * (0.16 + 0.08 * (1.0 - f)) * (1.0 + 0.8 * qc);
        float3 col = spec(4.0 + 2.0 * f + 0.5 * sin(ph)) * 0.75 + 0.25;
        img += exp(-sq((d - off) / w)) * vis * (0.8 - 0.35 * f) * (1.0 - cover) * col;
    }

    float3 rgb = 1.0 - exp(-img * 1.2);
    float alpha = max(rgb.r, max(rgb.g, rgb.b));
    return half4(half3(rgb), half(alpha));
}
"""
