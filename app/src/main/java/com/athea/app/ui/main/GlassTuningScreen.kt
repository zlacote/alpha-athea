package com.athea.app.ui.main

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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.athea.app.R
import com.athea.app.ui.common.AtheaRoundButton
import com.athea.app.ui.liquid.GlassMode
import com.athea.app.ui.liquid.GlassTuning
import com.athea.app.ui.liquid.LocalLiquidBackdrop
import com.athea.app.ui.liquid.liquidBackdrop
import com.athea.app.ui.liquid.liquidGlass
import com.athea.app.ui.liquid.rememberLiquidBackdrop
import com.athea.app.ui.theme.Ui

/** Height of the pinned sample, measured below the status bar. */
private val SAMPLE_HEIGHT = 260.dp
private const val SAMPLE_LINES = 12

/**
 * Live tuning of the glass. The sample stays pinned at the top of the screen and every
 * parameter of [GlassTuning] is a slider below it, grouped so each setting sits next to the
 * one it depends on. A slider whose setting is off is greyed out and says what it needs.
 */
@Composable
fun GlassTuningScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sampleBackdrop = rememberLiquidBackdrop()
    val background = MaterialTheme.colorScheme.background
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

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
                    SamplePattern()
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
                        AtheaRoundButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }

            // The controls scroll below the pinned sample, title first, so nothing shows above it.
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding(),
            ) {
                Text(
                    text = stringResource(R.string.glass_tuning_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                )
                Text(
                    text = stringResource(R.string.glass_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                ModeRow()

                val mode = GlassTuning.mode
                val lens = mode == GlassMode.LENS
                val lensReason = if (lens) null else stringResource(R.string.glass_needs_lens)

                SectionTitle(stringResource(R.string.glass_group_blur))
                TuningSlider(
                    label = stringResource(R.string.glass_blur),
                    value = GlassTuning.blurDp,
                    range = 0f..24f,
                    reason = if (mode == GlassMode.OFF) stringResource(R.string.glass_needs_glass) else null,
                    onChange = { GlassTuning.blurDp = it },
                )

                SectionTitle(stringResource(R.string.glass_group_lens))
                TuningSlider(
                    label = stringResource(R.string.glass_bezel),
                    value = GlassTuning.bezelDp,
                    range = 0f..80f,
                    reason = lensReason,
                    onChange = { GlassTuning.bezelDp = it },
                )
                TuningSlider(
                    label = stringResource(R.string.glass_bend),
                    value = GlassTuning.bendDp,
                    range = 0f..60f,
                    reason = lensReason ?: if (GlassTuning.bezelDp <= 0f) {
                        stringResource(R.string.glass_needs_bezel)
                    } else {
                        null
                    },
                    onChange = { GlassTuning.bendDp = it },
                )
                TuningSlider(
                    label = stringResource(R.string.glass_chroma),
                    value = GlassTuning.chroma,
                    range = 0f..1f,
                    reason = lensReason ?: if (GlassTuning.bendDp <= 0f) {
                        stringResource(R.string.glass_needs_bend)
                    } else {
                        null
                    },
                    onChange = { GlassTuning.chroma = it },
                )
                TuningSlider(
                    label = stringResource(R.string.glass_fill),
                    value = GlassTuning.fillAlpha,
                    range = 0f..0.4f,
                    reason = lensReason,
                    onChange = { GlassTuning.fillAlpha = it },
                )

                SectionTitle(stringResource(R.string.glass_group_highlight))
                TuningSlider(
                    label = stringResource(R.string.glass_highlight),
                    value = GlassTuning.highlightStrength,
                    range = 0f..1f,
                    reason = lensReason,
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
                    reason = lensReason ?: noHighlight,
                    onChange = { GlassTuning.highlightWidthDp = it },
                )
                TuningSlider(
                    label = stringResource(R.string.glass_light_angle),
                    value = GlassTuning.lightAngleDeg,
                    range = 0f..360f,
                    reason = lensReason ?: noHighlight,
                    onChange = { GlassTuning.lightAngleDeg = it },
                )
                TuningSlider(
                    label = stringResource(R.string.glass_rim),
                    value = GlassTuning.rimAlpha,
                    range = 0f..0.6f,
                    reason = if (mode == GlassMode.OFF) stringResource(R.string.glass_needs_glass) else null,
                    onChange = { GlassTuning.rimAlpha = it },
                )

                TextButton(
                    onClick = { GlassTuning.reset() },
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    Text(stringResource(R.string.glass_reset))
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ModeRow() {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf(
            GlassMode.OFF to R.string.glass_mode_off,
            GlassMode.BLUR to R.string.glass_mode_blur,
            GlassMode.LENS to R.string.glass_mode_lens,
        ).forEach { (mode, label) ->
            FilterChip(
                selected = GlassTuning.mode == mode,
                onClick = { GlassTuning.mode = mode },
                label = { Text(stringResource(label)) },
            )
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

/** Coloured discs and text lines: what the sample glass refracts, so the bend shows. */
@Composable
private fun SamplePattern() {
    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            // Centres are kept inside the area, so no disc spills past the sample's edge.
            val radius = size.minDimension * 0.2f
            fun centre(fx: Float, fy: Float) = Offset(
                (size.width * fx).coerceIn(radius, size.width - radius),
                (size.height * fy).coerceIn(radius, size.height - radius),
            )
            drawCircle(Color(0xFFFF4D4D), radius, centre(0.2f, 0.45f))
            drawCircle(Color(0xFF4DFF88), radius, centre(0.75f, 0.3f))
            drawCircle(Color(0xFF4D8BFF), radius, centre(0.55f, 0.85f))
        }
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            repeat(SAMPLE_LINES) { index ->
                Text(
                    text = stringResource(R.string.glass_sample_line, index + 1),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
    }
}
