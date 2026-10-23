package iatheiya.ayana.ui.liquid

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.platform.LocalDensity
import iatheiya.ayana.util.AyanaLog
import kotlin.math.cos
import kotlin.math.sin

/**
 * One lens: a RuntimeShader that bends the recorded backdrop inside a rounded rectangle.
 * Each glass surface owns its own instance, so uniforms never collide.
 *
 * Android copies a RuntimeShader's uniforms into the RenderEffect when the effect is
 * created. Setting them afterwards left every parameter at zero, so the lens never drew.
 * The effect is therefore rebuilt whenever one of the parameters changes.
 */
internal class LensEffect(
    private val shader: RuntimeShader,
    private val density: Float,
) {
    private var applied: FloatArray? = null
    private var effect: androidx.compose.ui.graphics.RenderEffect? = null

    /**
     * The render effect for a surface of [size], rebuilt only when a parameter changed.
     * A [flat] surface, such as a bar that spans an edge, keeps its blur but is neither bent
     * nor lit: along a straight edge the bend and the highlight read as a bright line.
     */
    fun effectFor(size: Size, cornerRadiusPx: Float, flat: Boolean = false): androidx.compose.ui.graphics.RenderEffect {
        val bent = GlassTuning.mode == GlassMode.LENS && !flat
        val angle = GlassTuning.lightAngleDeg * DEGREES_TO_RADIANS
        val values = floatArrayOf(
            size.width,
            size.height,
            cornerRadiusPx,
            GlassTuning.bezelDp * density,
            if (bent) GlassTuning.bendDp * density else 0f,
            if (bent) GlassTuning.chroma else 0f,
            GlassTuning.highlightWidthDp * density,
            if (bent) GlassTuning.highlightStrength else 0f,
            cos(angle),
            sin(angle),
            GlassTuning.blurDp * density,
        )
        val previous = applied
        val cached = effect
        if (cached != null && previous != null && values.contentEquals(previous)) return cached

        shader.setFloatUniform("lensSize", values[0], values[1])
        shader.setFloatUniform("cornerRadius", values[2])
        shader.setFloatUniform("bezel", values[3])
        shader.setFloatUniform("bend", values[4])
        shader.setFloatUniform("chroma", values[5])
        shader.setFloatUniform("highlightWidth", values[6])
        shader.setFloatUniform("highlightStrength", values[7])
        shader.setFloatUniform("lightDir", values[8], values[9])
        val blurPx = values[10].coerceAtLeast(0.01f)
        // The backdrop is blurred first, then the lens bends the blurred image.
        val built = RenderEffect.createChainEffect(
            RenderEffect.createRuntimeShaderEffect(shader, "content"),
            RenderEffect.createBlurEffect(blurPx, blurPx, Shader.TileMode.CLAMP),
        ).asComposeRenderEffect()
        applied = values
        effect = built
        return built
    }
}

/**
 * The lens for this surface, or null below Android 13 where RuntimeShader does not exist.
 * The density is needed to turn the dp parameters into pixels.
 */
@Composable
internal fun rememberLensEffect(): LensEffect? {
    val density = LocalDensity.current.density
    return remember(density) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) null else createLensEffect(density)
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun createLensEffect(density: Float): LensEffect? = try {
    val effect = LensEffect(RuntimeShader(LENS_AGSL), density)
    AyanaLog.log("lens", "shader ready")
    effect
} catch (e: Exception) {
    AyanaLog.error("lens", "lens shader failed to compile; glass is shown without refraction", e)
    null
}

private const val DEGREES_TO_RADIANS = 0.017453292f

/**
 * Lens in AGSL. Inside a rounded rectangle the backdrop is sampled from nearer the
 * centre, so it is magnified toward the rim. The bend follows a circular profile:
 * strongest at the rim and falling to zero across the bezel. Each colour channel
 * is sampled at a slightly different offset, which gives the prismatic fringe.
 * A thin highlight catches the rim on the side that faces the light.
 * Outside the lens the backdrop passes through untouched.
 */
private const val LENS_AGSL = """
uniform shader content;
uniform float2 lensSize;
uniform float cornerRadius;
uniform float bezel;
uniform float bend;
uniform float chroma;
uniform float highlightWidth;
uniform float highlightStrength;
uniform float2 lightDir;

float roundBoxSdf(float2 p, float2 halfSize, float r) {
    float2 d = abs(p) - halfSize + float2(r);
    return length(max(d, float2(0.0))) + min(max(d.x, d.y), 0.0) - r;
}

float2 outwardNormal(float2 p, float2 halfSize, float r) {
    float2 d = abs(p) - halfSize + float2(r);
    float2 s = float2(p.x >= 0.0 ? 1.0 : -1.0, p.y >= 0.0 ? 1.0 : -1.0);
    if (max(d.x, d.y) > 0.0) {
        return s * normalize(max(d, float2(0.0)));
    }
    return d.x > d.y ? float2(s.x, 0.0) : float2(0.0, s.y);
}

// 1 at the rim, 0 at the inner edge of the bezel, along a quarter circle.
float circleMap(float x) {
    return 1.0 - sqrt(max(1.0 - x * x, 0.0));
}

half4 main(float2 xy) {
    float2 halfSize = lensSize * 0.5;
    float r = min(cornerRadius, min(halfSize.x, halfSize.y));
    float2 p = xy - halfSize;
    float sdf = roundBoxSdf(p, halfSize, r);
    half4 backdrop = content.eval(xy);
    if (sdf > 1.0) {
        return backdrop;
    }
    float minHalf = min(halfSize.x, halfSize.y);
    float bezelWidth = min(bezel, minHalf);
    float2 n = outwardNormal(p, halfSize, r);

    float depth = clamp(-sdf / bezelWidth, 0.0, 1.0);
    float shift = bend * circleMap(1.0 - depth);

    float spread = shift * chroma * 0.2;
    half rChannel = content.eval(xy - n * (shift + spread)).r;
    half gChannel = content.eval(xy - n * shift).g;
    half bChannel = content.eval(xy - n * (shift - spread)).b;
    half3 refracted = half3(rChannel, gChannel, bChannel);

    // The highlight is brightest where the rim faces the light and fades around the edge.
    float2 light = normalize(lightDir);
    float facing = max(dot(n, -light), 0.0);
    float rimBand = 1.0 - smoothstep(0.0, highlightWidth, -sdf);
    refracted += half3(facing * facing * rimBand * highlightStrength);

    float alpha = 1.0 - smoothstep(-0.75, 0.75, sdf);
    return half4(mix(backdrop.rgb, refracted, alpha), backdrop.a);
}
"""
