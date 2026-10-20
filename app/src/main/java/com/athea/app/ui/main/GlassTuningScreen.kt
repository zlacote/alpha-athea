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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.athea.app.R
import com.athea.app.ui.common.AtheaRoundButton
import com.athea.app.ui.common.AtheaTitlePill
import com.athea.app.ui.liquid.GlassMode
import com.athea.app.ui.liquid.GlassTuning
import com.athea.app.ui.liquid.LocalLiquidBackdrop
import com.athea.app.ui.liquid.liquidBackdrop
import com.athea.app.ui.liquid.liquidGlass
import com.athea.app.ui.liquid.rememberLiquidBackdrop
import com.athea.app.ui.theme.Ui

/** Height of the sample area at the top of the screen. */
private val SAMPLE_HEIGHT = 260.dp
private const val SAMPLE_LINES = 12

/**
 * Live tuning of the glass: every parameter of [GlassTuning] as a slider over a sample
 * glass that follows the sliders. Off and Blur draw the same glass without the lens, so
 * the bend and the highlight can be judged against them.
 */
@Composable
fun GlassTuningScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val headerBackdrop = rememberLiquidBackdrop()
    val sampleBackdrop = rememberLiquidBackdrop()
    val background = MaterialTheme.colorScheme.background
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val headerInset = statusBarTop + Ui.topBarTopPadding + Ui.topButtonSize + Ui.topBarBottomPadding

    Surface(color = background, modifier = modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize()) {
            // The controls scroll under the header; the sample sits over the top of them.
            Column(
                Modifier
                    .fillMaxSize()
                    .liquidBackdrop(headerBackdrop, background)
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding(),
            ) {
                Spacer(Modifier.height(headerInset + SAMPLE_HEIGHT))
                Text(
                    text = stringResource(R.string.glass_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                ModeRow()
                TuningSlider(
                    label = stringResource(R.string.glass_blur),
                    value = GlassTuning.blurDp,
                    range = 0f..24f,
                    onChange = { GlassTuning.blurDp = it },
                )
                TuningSlider(
                    label = stringResource(R.string.glass_bezel),
                    value = GlassTuning.bezelDp,
                    range = 0f..80f,
                    onChange = { GlassTuning.bezelDp = it },
                )
                TuningSlider(
                    label = stringResource(R.string.glass_bend),
                    value = GlassTuning.bendDp,
                    range = 0f..60f,
                    onChange = { GlassTuning.bendDp = it },
                )
                TuningSlider(
                    label = stringResource(R.string.glass_chroma),
                    value = GlassTuning.chroma,
                    range = 0f..1f,
                    onChange = { GlassTuning.chroma = it },
                )
                TuningSlider(
                    label = stringResource(R.string.glass_highlight_width),
                    value = GlassTuning.highlightWidthDp,
                    range = 0f..12f,
                    onChange = { GlassTuning.highlightWidthDp = it },
                )
                TuningSlider(
                    label = stringResource(R.string.glass_highlight),
                    value = GlassTuning.highlightStrength,
                    range = 0f..1f,
                    onChange = { GlassTuning.highlightStrength = it },
                )
                TuningSlider(
                    label = stringResource(R.string.glass_light_angle),
                    value = GlassTuning.lightAngleDeg,
                    range = 0f..360f,
                    onChange = { GlassTuning.lightAngleDeg = it },
                )
                TuningSlider(
                    label = stringResource(R.string.glass_fill),
                    value = GlassTuning.fillAlpha,
                    range = 0f..0.4f,
                    onChange = { GlassTuning.fillAlpha = it },
                )
                TuningSlider(
                    label = stringResource(R.string.glass_rim),
                    value = GlassTuning.rimAlpha,
                    range = 0f..0.6f,
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

            // The backdrop the sample glass refracts. It is recorded on its own, so the
            // sample glass never records itself.
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = headerInset)
                    .height(SAMPLE_HEIGHT)
                    .liquidBackdrop(sampleBackdrop, background),
            ) {
                SamplePattern()
            }
            // The sample glass sits outside any recorded node.
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = headerInset)
                    .height(SAMPLE_HEIGHT),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(width = 240.dp, height = 120.dp)
                        .liquidGlass(sampleBackdrop, RoundedCornerShape(32.dp), 32.dp),
                )
            }
            CompositionLocalProvider(LocalLiquidBackdrop provides headerBackdrop) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(
                            start = Ui.topBarHorizontalPadding,
                            end = Ui.topBarHorizontalPadding,
                            top = Ui.topBarTopPadding,
                            bottom = Ui.topBarBottomPadding,
                        ),
                ) {
                    AtheaRoundButton(
                        onClick = onBack,
                        modifier = Modifier.align(Alignment.CenterStart),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    AtheaTitlePill(
                        title = stringResource(R.string.glass_tuning_title),
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
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
private fun TuningSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "%.2f".format(value),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
        )
    }
}

/** Coloured discs and text lines: what the sample glass refracts, so the bend shows. */
@Composable
private fun SamplePattern() {
    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            val radius = size.minDimension * 0.22f
            drawCircle(Color(0xFFFF4D4D), radius, Offset(size.width * 0.2f, size.height * 0.45f))
            drawCircle(Color(0xFF4DFF88), radius, Offset(size.width * 0.75f, size.height * 0.3f))
            drawCircle(Color(0xFF4D8BFF), radius, Offset(size.width * 0.55f, size.height * 0.85f))
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
