package com.athea.app.ui.theme

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.Dp
import com.athea.app.util.AtheaLog

/**
 * Where one refracting panel sits inside its [LiquidLayer], in pixels.
 * Written by the panel's layout, read at draw time by the layer's shader.
 */
class LensRegion(val cornerRadius: Dp) {

    /** Panel bounds relative to the owning layer. */
    var rect by mutableStateOf(Rect.Zero)

    /** Owning layer's origin in window-root coordinates. */
    internal var origin = Offset.Zero

    /** True on Android 13+, the only level where the layer refracts. */
    val supported: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    /** Set after the first applied frame, so the diagnostic line is written once. */
    internal var reported = false
}

@Composable
fun rememberLensRegion(cornerRadius: Dp): LensRegion =
    remember(cornerRadius) { LensRegion(cornerRadius) }

/** Records where a panel sits, so its [LiquidLayer] can bend the backdrop behind it. */
fun Modifier.lensAnchor(lens: LensRegion): Modifier = onGloballyPositioned { coordinates ->
    val topLeft = coordinates.positionInRoot() - lens.origin
    lens.rect = Rect(
        topLeft,
        Size(coordinates.size.width.toFloat(), coordinates.size.height.toFloat()),
    )
}

/**
 * Renders [content], then bends the part of it that lies inside [lens].
 *
 * Panels drawn above this layer (not inside it) keep their text sharp and see
 * only the bent backdrop. Below API 33 the layer is a plain box: the glass
 * panel above it still draws its tint, sheen and rim.
 */
@Composable
fun LiquidLayer(
    lens: LensRegion,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val lensShader = rememberLensShader(lens)
    Box(
        modifier
            .onGloballyPositioned { lens.origin = it.positionInRoot() }
            .graphicsLayer {
                val shader = lensShader ?: return@graphicsLayer
                val rect = lens.rect
                if (rect.width <= 0f || rect.height <= 0f) return@graphicsLayer
                if (!lens.reported) {
                    lens.reported = true
                    AtheaLog.log("lens", "applied rect=$rect origin=${lens.origin}")
                }
                shader.shader.setFloatUniform("lensCenter", rect.center.x, rect.center.y)
                shader.shader.setFloatUniform("lensSize", rect.width, rect.height)
                shader.shader.setFloatUniform("cornerRadius", lens.cornerRadius.toPx())
                shader.shader.setFloatUniform("refraction", LENS_REFRACTION)
                shader.shader.setFloatUniform("dispersion", LENS_DISPERSION)
                shader.shader.setFloatUniform("tint", LENS_TINT[0], LENS_TINT[1], LENS_TINT[2], LENS_TINT[3])
                shader.shader.setFloatUniform("lightDir", LENS_LIGHT_DIR[0], LENS_LIGHT_DIR[1])
                shader.shader.setFloatUniform("rimStrength", LENS_RIM_STRENGTH)
                renderEffect = shader.composeEffect
            },
        content = content,
    )
}

private class LensShader(val shader: RuntimeShader, val composeEffect: androidx.compose.ui.graphics.RenderEffect)

@Composable
private fun rememberLensShader(lens: LensRegion): LensShader? = remember {
    if (!lens.supported) {
        AtheaLog.log("lens", "refraction off: needs Android 13+, this device is SDK ${Build.VERSION.SDK_INT}")
        null
    } else {
        createLensShader()?.also { AtheaLog.log("lens", "shader ready") }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun createLensShader(): LensShader? = try {
    val shader = RuntimeShader(LENS_AGSL)
    val effect = RenderEffect.createRuntimeShaderEffect(shader, "content")
    LensShader(shader, effect.asComposeRenderEffect())
} catch (e: Exception) {
    AtheaLog.error("lens", "lens shader failed to compile; falling back to plain glass", e)
    null
}

private const val LENS_REFRACTION = 1.0f
private const val LENS_DISPERSION = 0.6f
private const val LENS_RIM_STRENGTH = 0.35f
private val LENS_TINT = floatArrayOf(0.02f, 0.02f, 0.03f, 0.10f)
private val LENS_LIGHT_DIR = floatArrayOf(-0.5f, -0.87f)

/**
 * Lens shader in AGSL. Inside a rounded rectangle the backdrop is sampled from
 * closer to the centre near the rim, which bends it toward the edge. Each colour
 * channel is sampled at a slightly different offset, which gives the prismatic
 * fringe. Outside the lens the backdrop passes through untouched.
 *
 * The approach follows skydoves/Cloudy (Apache-2.0), reimplemented here.
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
