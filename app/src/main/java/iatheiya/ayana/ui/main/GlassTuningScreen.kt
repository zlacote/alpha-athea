package iatheiya.ayana.ui.main

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.cos
import kotlin.math.sin
import iatheiya.ayana.R
import iatheiya.ayana.ui.common.AyanaScaffold
import iatheiya.ayana.ui.common.AyanaScrollHeader
import iatheiya.ayana.ui.liquid.GlassTuning
import iatheiya.ayana.ui.liquid.liquidBackdrop
import iatheiya.ayana.ui.liquid.liquidGlass
import iatheiya.ayana.ui.liquid.rememberLiquidBackdrop
import iatheiya.ayana.ui.theme.Ui

/** Height of the photo sample. */
private val SAMPLE_HEIGHT = 240.dp

/** Rounded top and bottom of the preview card; its sides stay flush with the screen. */
private val SAMPLE_CORNER = 28.dp

/** One full loop of the perpetual scene, in milliseconds. */
private const val SCENE_PERIOD_MS = 16000

/**
 * Live tuning of the glass. The screen reads top to bottom like the others: the "Glass"
 * title, then the master on/off, then the photo. The photo stays below the switch at the
 * top, but as the controls scroll it rises and pins to the top so it never leaves the
 * screen, while the colour scene inside it drifts at its own, slower rate (parallax).
 */
@Composable
fun GlassTuningScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sampleBackdrop = rememberLiquidBackdrop()
    val background = MaterialTheme.colorScheme.background
    val scroll = rememberScrollState()
    var aboveHeightPx by remember { mutableStateOf(0f) }

    AyanaScaffold(onBack = onBack, modifier = modifier) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scroll),
        ) {
            // Title and master switch scroll away; their height is the photo's resting anchor.
            Column(Modifier.onSizeChanged { aboveHeightPx = it.height.toFloat() }) {
                AyanaScrollHeader(stringResource(R.string.glass_tuning_title))
                SettingsCard {
                    SwitchRow(
                        title = stringResource(R.string.glass_toggle),
                        subtitle = stringResource(R.string.glass_toggle_desc),
                        checked = GlassTuning.enabled,
                        onChange = { GlassTuning.enabled = it },
                    )
                }
            }

            // The photo: pinned to the top once the switch scrolls past, and always on top
            // of the sliders that slide under it.
            Box(
                Modifier
                    .zIndex(1f)
                    .graphicsLayer {
                        translationY = (scroll.value.toFloat() - aboveHeightPx).coerceAtLeast(0f)
                    }
                    .fillMaxWidth()
                    .height(SAMPLE_HEIGHT)
                    .clip(RoundedCornerShape(SAMPLE_CORNER)),
            ) {
                // The scene the sample glass refracts; recorded on its own. It animates forever on
                // its own clock — the frame moves with the scroll, the scene inside it does not.
                Box(
                    Modifier
                        .matchParentSize()
                        .liquidBackdrop(sampleBackdrop, background),
                ) {
                    GradientScene(Modifier.matchParentSize())
                }
                Box(Modifier.matchParentSize(), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier
                            .size(width = 240.dp, height = 120.dp)
                            .liquidGlass(sampleBackdrop, RoundedCornerShape(32.dp), 32.dp),
                    )
                }
            }

            val offReason = if (!GlassTuning.enabled) {
                stringResource(R.string.glass_needs_glass)
            } else {
                null
            }

            // The standalone knobs: each one changes the look on its own.
            SectionTitle(stringResource(R.string.glass_group_surface))
            TuningSlider(
                label = stringResource(R.string.glass_blur),
                value = GlassTuning.blurDp,
                range = 0f..24f,
                reason = offReason,
                onChange = { GlassTuning.blurDp = it },
            )
            TuningSlider(
                label = stringResource(R.string.glass_chroma),
                value = GlassTuning.chroma,
                range = 0f..1f,
                reason = offReason,
                onChange = { GlassTuning.chroma = it },
            )
            TuningSlider(
                label = stringResource(R.string.glass_fill),
                value = GlassTuning.fillAlpha,
                range = 0f..0.4f,
                reason = offReason,
                onChange = { GlassTuning.fillAlpha = it },
            )
            TuningSlider(
                label = stringResource(R.string.glass_rim),
                value = GlassTuning.rimAlpha,
                range = 0f..0.6f,
                reason = offReason,
                onChange = { GlassTuning.rimAlpha = it },
            )

            // The lens shape: the bend grows out of the bezel.
            SectionTitle(stringResource(R.string.glass_group_lens))
            TuningSlider(
                label = stringResource(R.string.glass_bezel),
                value = GlassTuning.bezelDp,
                range = 0f..80f,
                reason = offReason,
                onChange = { GlassTuning.bezelDp = it },
            )
            TuningSlider(
                label = stringResource(R.string.glass_bend),
                value = GlassTuning.bendDp,
                range = 0f..60f,
                reason = offReason ?: if (GlassTuning.bezelDp <= 0f) {
                    stringResource(R.string.glass_needs_bezel)
                } else {
                    null
                },
                onChange = { GlassTuning.bendDp = it },
            )

            // The rim light: its width and angle only matter once it has strength.
            SectionTitle(stringResource(R.string.glass_group_highlight))
            TuningSlider(
                label = stringResource(R.string.glass_highlight),
                value = GlassTuning.highlightStrength,
                range = 0f..1f,
                reason = offReason,
                onChange = { GlassTuning.highlightStrength = it },
            )
            val noHighlight = if (GlassTuning.highlightStrength <= 0f) {
                stringResource(R.string.glass_needs_highlight)
            } else {
                null
            }
            TuningSlider(
                label = stringResource(R.string.glass_highlight_width),
                value = GlassTuning.highlightWidthDp,
                range = 0f..12f,
                reason = offReason ?: noHighlight,
                onChange = { GlassTuning.highlightWidthDp = it },
            )
            TuningSlider(
                label = stringResource(R.string.glass_light_angle),
                value = GlassTuning.lightAngleDeg,
                range = 0f..360f,
                reason = offReason ?: noHighlight,
                onChange = { GlassTuning.lightAngleDeg = it },
            )

            // The default button sits at the very end, centred and bare, like the title.
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                TextButton(onClick = { GlassTuning.reset() }) {
                    Text(stringResource(R.string.glass_reset))
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 2.dp),
    )
}

/**
 * One parameter. A non-null [reason] greys the slider out and says what it waits for; a
 * disabled slider cannot be dragged.
 */
@Composable
private fun TuningSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    reason: String?,
    onChange: (Float) -> Unit,
) {
    val enabled = reason == null
    val labelColor = if (enabled) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = Ui.ghostAlpha)
    }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge, color = labelColor)
            Text(
                text = "%.2f".format(value),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (reason != null) {
            Text(
                text = reason,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(
            value = value,
            onValueChange = onChange,
            enabled = enabled,
            valueRange = range,
        )
    }
}

/**
 * A generated colour scene — overlapping saturated blobs over a deep base, crossed by
 * fine diagonal lines. The smooth blobs show the bend; the crisp lines show the
 * chromatic fringe along their edges. No image file is bundled.
 */
@Composable
private fun GradientScene(modifier: Modifier) {
    val transition = rememberInfiniteTransition(label = "scene")
    // One looping clock. Every motion below is periodic in it (whole cycles only), so the loop
    // has no seam. It runs forever and ignores touch and scroll.
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(SCENE_PERIOD_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "t",
    )
    Canvas(modifier) {
        drawRect(Brush.verticalGradient(listOf(Color(0xFF14091F), Color(0xFF05070E))))

        // Mesh base: saturated blobs that drift on slow whole-cycle orbits and melt together.
        fun blob(color: Color, fx: Float, fy: Float, fr: Float, driftPhase: Float) {
            val angle = TWO_PI * (t + driftPhase)
            val cx = size.width * (fx + 0.05f * cos(angle))
            val cy = size.height * (fy + 0.05f * sin(angle))
            val radius = size.minDimension * fr
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = 0.85f), color.copy(alpha = 0f)),
                    center = Offset(cx, cy),
                    radius = radius,
                ),
                radius = radius,
                center = Offset(cx, cy),
            )
        }
        blob(Color(0xFFFF3D7F), 0.22f, 0.28f, 0.55f, 0.00f)
        blob(Color(0xFF00E0FF), 0.80f, 0.22f, 0.50f, 0.30f)
        blob(Color(0xFFFFC24D), 0.72f, 0.70f, 0.52f, 0.55f)
        blob(Color(0xFF7A5CFF), 0.26f, 0.78f, 0.60f, 0.75f)
        blob(Color(0xFF34FFB0), 0.55f, 0.50f, 0.40f, 0.15f)

        // Constellation: nodes rise in clusters, each cluster born at its own time and height, and
        // thread to their neighbours. Near nodes are larger and brighter, far ones small and dim, so
        // the field has depth. Every node runs whole cycles, so the loop wraps without a seam.
        val nodes = SCENE_NODES.map { n ->
            val life = frac(t * n.rise + n.phase)
            val x = size.width * (n.baseX + n.wobbleAmp * sin(TWO_PI * (t * n.wobbleFreq + n.phase)))
            val y = size.height * (1f - life)
            NodePoint(Offset(x, y), nodeFade(life, n.fadeIn) * (0.35f + 0.65f * n.depth), n.depth)
        }
        val reach = size.minDimension * 0.5f
        for (i in nodes.indices) {
            for (j in i + 1 until nodes.size) {
                val a = nodes[i]
                val b = nodes[j]
                val dist = (a.position - b.position).getDistance()
                if (dist < reach) {
                    // Threads brighten as nodes close and fade as they part, so none is a fixed line;
                    // nearer pairs draw thicker and brighter than far ones.
                    val depthAvg = (a.depth + b.depth) * 0.5f
                    val strength = (1f - dist / reach) * minOf(a.alpha, b.alpha) * (0.28f + 0.42f * depthAvg)
                    drawLine(
                        color = Color.White.copy(alpha = strength),
                        start = a.position,
                        end = b.position,
                        strokeWidth = (0.7f + depthAvg * 0.9f).dp.toPx(),
                    )
                }
            }
        }
        nodes.forEach { node ->
            drawCircle(
                color = Color.White.copy(alpha = node.alpha * (0.6f + 0.4f * node.depth)),
                radius = (1.2f + node.depth * 2.0f).dp.toPx(),
                center = node.position,
            )
        }
    }
}

private const val TWO_PI = (2.0 * Math.PI).toFloat()

private fun frac(v: Float): Float = v - kotlin.math.floor(v)

/** Fades a node in over its own birth band (so births are not on one line) and out at the very top. */
private fun nodeFade(life: Float, fadeIn: Float): Float = when {
    life < fadeIn -> life / fadeIn
    life > 0.82f -> (1f - life) / 0.18f
    else -> 1f
}.coerceIn(0f, 1f)

private class NodePoint(val position: Offset, val alpha: Float, val depth: Float)

private class SceneNode(
    val baseX: Float,
    val phase: Float,
    val rise: Int,
    val wobbleAmp: Float,
    val wobbleFreq: Int,
    val depth: Float,
    val fadeIn: Float,
)

/**
 * Fixed, seeded node field grouped into clusters spread evenly across the width, so the points are
 * not all on the right and each cluster is born in its own place and at its own height. Whole-cycle
 * rise and wobble keep the loop seamless.
 */
private val SCENE_NODES: List<SceneNode> = run {
    val rnd = java.util.Random(11)
    val clusters = 5
    buildList {
        for (c in 0 until clusters) {
            val centreX = (c + 0.5f) / clusters
            val clusterPhase = rnd.nextFloat()
            val clusterRise = 1 + rnd.nextInt(2)
            val count = 3 + rnd.nextInt(2)
            repeat(count) {
                add(
                    SceneNode(
                        baseX = (centreX + (rnd.nextFloat() - 0.5f) * 0.22f).coerceIn(0.04f, 0.96f),
                        phase = frac(clusterPhase + (rnd.nextFloat() - 0.5f) * 0.12f),
                        rise = clusterRise,
                        wobbleAmp = 0.025f + rnd.nextFloat() * 0.06f,
                        wobbleFreq = 1 + rnd.nextInt(2),
                        depth = rnd.nextFloat(),
                        fadeIn = 0.12f + rnd.nextFloat() * 0.3f,
                    ),
                )
            }
        }
    }
}
