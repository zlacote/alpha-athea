package com.athea.app.ui.liquid

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.platform.LocalDensity
import com.athea.app.util.AtheaLog
import kotlin.math.cos
import kotlin.math.sin

/**
 * One lens: a RuntimeShader that bends the recorded backdrop inside a rounded
 * rectangle. Each glass surface owns its own instance, so uniforms never collide.
 */
internal class LensEffect(
    private val shader: RuntimeShader,
    val composeEffect: androidx.compose.ui.graphics.RenderEffect,
    private val density: Float,
) {

    /** Sets every uniform the shader declares from [GlassTuning]; written on every draw. */
    fun update(size: Size, cornerRadiusPx: Float, originPx: Offset) {
        val lens = GlassTuning.mode == GlassMode.LENS
        val angle = GlassTuning.lightAngleDeg * DEGREES_TO_RADIANS
        shader.setFloatUniform("lensSize", size.width, size.height)
        shader.setFloatUniform("origin", originPx.x, originPx.y)
        shader.setFloatUniform("cornerRadius", cornerRadiusPx)
        shader.setFloatUniform("bezel", GlassTuning.bezelDp * density)
        shader.setFloatUniform("bend", if (lens) GlassTuning.bendDp * density else 0f)
        shader.setFloatUniform("chroma", GlassTuning.chroma)
        shader.setFloatUniform("highlightWidth", GlassTuning.highlightWidthDp * density)
        shader.setFloatUniform("highlightStrength", if (lens) GlassTuning.highlightStrength else 0f)
        shader.setFloatUniform("lightDir", cos(angle), sin(angle))
    }
}

/**
 * The lens for this surface, or null below Android 13 where RuntimeShader does not exist.
 * The blur radius is baked into the render effect, so the effect is rebuilt when it changes.
 */
@Composable
internal fun rememberLensEffect(): LensEffect? {
    val density = LocalDensity.current.density
    val blurDp = GlassTuning.blurDp
    return remember(density, blurDp) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            null
        } else {
            createLensEffect(blurPx = (blurDp * density).coerceAtLeast(0.01f), density = density)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun createLensEffect(blurPx: Float, density: Float): LensEffect? = try {
    val shader = RuntimeShader(LENS_AGSL)
    // The backdrop is blurred first, then the lens bends the blurred image.
    val blur = RenderEffect.createBlurEffect(blurPx, blurPx, Shader.TileMode.CLAMP)
    val lens = RenderEffect.createRuntimeShaderEffect(shader, "content")
    AtheaLog.log("lens", "shader ready")
    LensEffect(
        shader = shader,
        composeEffect = RenderEffect.createChainEffect(lens, blur).asComposeRenderEffect(),
        density = density,
    )
} catch (e: Exception) {
    AtheaLog.error("lens", "lens shader failed to compile; glass is shown without refraction", e)
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
uniform float2 origin;
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
    // xy arrives in window space, so the lens is measured from the element's own origin.
    float2 local = xy - origin;
    float2 halfSize = lensSize * 0.5;
    float r = min(cornerRadius, min(halfSize.x, halfSize.y));
    float2 p = local - halfSize;
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
