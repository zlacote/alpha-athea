package com.athea.app.ui.liquid

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asComposeRenderEffect
import com.athea.app.util.AtheaLog

/**
 * One lens: a RuntimeShader that bends the recorded backdrop inside a rounded
 * rectangle. Each glass surface owns its own instance, so uniforms never collide.
 */
internal class LensEffect(
    private val shader: RuntimeShader,
    val composeEffect: androidx.compose.ui.graphics.RenderEffect,
) {

    /** Sets every uniform the shader declares; they are all written on every draw. */
    fun update(size: Size, cornerRadiusPx: Float) {
        shader.setFloatUniform("lensCenter", size.width / 2f, size.height / 2f)
        shader.setFloatUniform("lensSize", size.width, size.height)
        shader.setFloatUniform("cornerRadius", cornerRadiusPx)
        shader.setFloatUniform("refraction", REFRACTION)
        shader.setFloatUniform("dispersion", DISPERSION)
        shader.setFloatUniform("tint", TINT[0], TINT[1], TINT[2], TINT[3])
        shader.setFloatUniform("lightDir", LIGHT_DIR[0], LIGHT_DIR[1])
        shader.setFloatUniform("rimStrength", RIM_STRENGTH)
    }
}

/** The lens for this surface, or null below Android 13 where RuntimeShader does not exist. */
@Composable
internal fun rememberLensEffect(): LensEffect? = remember {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        null
    } else {
        createLensEffect()
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun createLensEffect(): LensEffect? = try {
    val shader = RuntimeShader(LENS_AGSL)
    LensEffect(
        shader,
        RenderEffect.createRuntimeShaderEffect(shader, "content").asComposeRenderEffect(),
    )
} catch (e: Exception) {
    AtheaLog.error("lens", "lens shader failed to compile; glass is shown without refraction", e)
    null
}

private const val REFRACTION = 1.0f
private const val DISPERSION = 0.6f
private const val RIM_STRENGTH = 0.35f
private val TINT = floatArrayOf(0.02f, 0.02f, 0.03f, 0.10f)
private val LIGHT_DIR = floatArrayOf(-0.5f, -0.87f)

/**
 * Lens in AGSL. Inside a rounded rectangle the backdrop is sampled from closer to
 * the centre near the rim, which bends it toward the edge. Each colour channel is
 * sampled at a slightly different offset, which gives the prismatic fringe.
 * Outside the lens the backdrop passes through untouched.
 */
private const val LENS_AGSL = """
uniform shader content;
uniform float2 lensCenter;
uniform float2 lensSize;
uniform float cornerRadius;
uniform float refraction;
uniform float dispersion;
uniform float4 tint;
uniform float2 lightDir;
uniform float rimStrength;

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

half4 main(float2 xy) {
    float2 halfSize = lensSize * 0.5;
    float r = min(cornerRadius, min(halfSize.x, halfSize.y));
    float2 p = xy - lensCenter;
    float sdf = roundBoxSdf(p, halfSize, r);
    half4 backdrop = content.eval(xy);
    if (sdf > 1.0) {
        return backdrop;
    }
    float minHalf = min(halfSize.x, halfSize.y);
    float2 n = outwardNormal(p, halfSize, r);

    // 1 at the rim, 0 half a lens-half inside: the bend covers the whole lens and is
    // strongest at the edge, where a real glass slab bends light the most.
    float depth = clamp(-sdf / (minHalf * 0.5), 0.0, 1.0);
    float bend = refraction * minHalf * 0.5 * (1.0 - depth);

    float k = dispersion * 0.12;
    half rChannel = content.eval(xy - n * bend * (1.0 + k)).r;
    half gChannel = content.eval(xy - n * bend).g;
    half bChannel = content.eval(xy - n * bend * (1.0 - k)).b;
    half3 refracted = half3(rChannel, gChannel, bChannel);
    refracted = mix(refracted, half3(tint.rgb), tint.a);

    // Faces turned toward the light catch a soft highlight along the rim.
    float facing = max(dot(n, -normalize(lightDir)), 0.0);
    float rimBand = 1.0 - smoothstep(0.0, minHalf * 0.12, -sdf);
    refracted += half3(facing * rimBand * rimStrength);

    float alpha = 1.0 - smoothstep(-0.75, 0.75, sdf);
    return half4(mix(backdrop.rgb, refracted, alpha), backdrop.a);
}
"""
