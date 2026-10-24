package iatheiya.ayana.ui.main

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import iatheiya.ayana.R
import iatheiya.ayana.ui.common.AyanaRoundButton
import iatheiya.ayana.ui.liquid.GlassTuning
import iatheiya.ayana.ui.liquid.LocalLiquidBackdrop
import iatheiya.ayana.ui.liquid.liquidBackdrop
import iatheiya.ayana.ui.liquid.liquidGlass
import iatheiya.ayana.ui.liquid.rememberLiquidBackdrop
import iatheiya.ayana.ui.theme.Ui

/** Height of the pinned sample, measured below the status bar. */
private val SAMPLE_HEIGHT = 260.dp

/** Extra scene drawn below the sample so it can drift up as the controls scroll. */
private val PARALLAX_EXTRA = 140.dp
private const val PARALLAX_FACTOR = 0.35f

/**
 * Live tuning of the glass. A generated colour scene stays pinned at the top and
 * drifts under the glass as the controls scroll (parallax); every [GlassTuning]
 * parameter is a slider below it, grouped so each setting sits next to the one it
 * depends on. Glass is one master switch — on is the refracting lens, off is a solid
 * surface — and a slider whose setting is off is greyed out with the reason.
 */
@Composable
fun GlassTuningScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sampleBackdrop = rememberLiquidBackdrop()
    val background = MaterialTheme.colorScheme.background
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val scroll = rememberScrollState()
    val extraPx = with(LocalDensity.current) { PARALLAX_EXTRA.toPx() }

    Surface(color = background, modifier = modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            // Pinned: nothing scrolls above or behind the sample.
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(statusBarTop + SAMPLE_HEIGHT),
            ) {
                // The backdrop the sample glass refracts. It is recorded on its own, so the
                // sample glass never records itself, and it is clipped to the sample area.
                Box(
                    Modifier
                        .matchParentSize()
                        .clipToBounds()
                        .liquidBackdrop(sampleBackdrop, background),
                ) {
                    GradientScene(
                        Modifier
                            .fillMaxWidth()
                            .height(statusBarTop + SAMPLE_HEIGHT + PARALLAX_EXTRA)
                            // The scene drifts up as the controls scroll, but never past its extra.
                            .graphicsLayer {
                                translationY = -(scroll.value.toFloat() * PARALLAX_FACTOR)
                                    .coerceIn(0f, extraPx)
                            },
                    )
                }
                // The sample glass sits outside any recorded node, centred below the status bar.
                Box(
                    Modifier
                        .matchParentSize()
                        .padding(top = statusBarTop),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .size(width = 240.dp, height = 120.dp)
                            .liquidGlass(sampleBackdrop, RoundedCornerShape(32.dp), 32.dp),
                    )
                }
                // The back button sits where it does on every screen and refracts the sample.
                CompositionLocalProvider(LocalLiquidBackdrop provides sampleBackdrop) {
                    Box(
                        Modifier
                            .statusBarsPadding()
                            .padding(start = Ui.topBarHorizontalPadding, top = Ui.topBarTopPadding),
                    ) {
                        AyanaRoundButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }

            // The controls scroll below the pinned sample.
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scroll)
                    .navigationBarsPadding(),
            ) {
                // Title and the master switch on one line; no description below it.
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.glass_tuning_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Switch(
                        checked = GlassTuning.enabled,
                        onCheckedChange = { GlassTuning.enabled = it },
                        colors = SwitchDefaults.colors(
                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            uncheckedTrackColor = MaterialTheme.colorScheme.outline,
                            uncheckedBorderColor = Color.Transparent,
                        ),
                    )
                }
                // The default button rides just under the title, not buried at the bottom.
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = { GlassTuning.reset() }) {
                        Text(stringResource(R.string.glass_reset))
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

                Spacer(Modifier.height(24.dp))
            }
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
    Canvas(modifier) {
        drawRect(
            Brush.verticalGradient(
                listOf(Color(0xFF1A1030), Color(0xFF05070E)),
            ),
        )
        fun blob(color: Color, fx: Float, fy: Float, fr: Float) {
            val centre = Offset(size.width * fx, size.height * fy)
            val radius = size.minDimension * fr
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = 0.9f), color.copy(alpha = 0f)),
                    center = centre,
                    radius = radius,
                ),
                radius = radius,
                center = centre,
            )
        }
        blob(Color(0xFFFF3D7F), 0.20f, 0.22f, 0.55f)
        blob(Color(0xFF00E0FF), 0.82f, 0.18f, 0.50f)
        blob(Color(0xFFFFC24D), 0.72f, 0.66f, 0.52f)
        blob(Color(0xFF7A5CFF), 0.28f, 0.80f, 0.60f)
        blob(Color(0xFF34FFB0), 0.55f, 0.48f, 0.38f)

        // Fine diagonals give the lens crisp edges to bend and fringe.
        val step = size.width / 7f
        val stroke = 1.dp.toPx()
        var x = -size.height
        while (x < size.width + size.height) {
            drawLine(
                color = Color.White.copy(alpha = 0.10f),
                start = Offset(x, 0f),
                end = Offset(x + size.height, size.height),
                strokeWidth = stroke,
            )
            x += step
        }
    }
}
