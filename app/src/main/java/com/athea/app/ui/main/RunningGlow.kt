package com.athea.app.ui.main

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.athea.app.data.AtheaSettings
import com.athea.app.ui.theme.Ui

/**
 * A soft light that rises from the bottom edge while a command runs. Its strength, height, width
 * and pulse come from the settings. It is not drawn at all when the glow is switched off.
 */
@Composable
fun RunningGlow(running: Boolean, settings: AtheaSettings, modifier: Modifier = Modifier) {
    val presence by animateFloatAsState(
        targetValue = if (running && settings.glowEnabled) 1f else 0f,
        animationSpec = tween(durationMillis = Ui.glowFadeMs),
        label = "glowPresence",
    )
    if (presence > 0f) GlowPulse(presence = presence, settings = settings, modifier = modifier)
}

@Composable
private fun GlowPulse(presence: Float, settings: AtheaSettings, modifier: Modifier) {
    val period = settings.glowPeriodMs
    val transition = rememberInfiniteTransition(label = "glowPulse")
    // One pulse: it flares quickly over the first eighth of the period, then fades slowly.
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes<Float> {
                durationMillis = period
                0f at 0
                1f at period / 8 using FastOutSlowInEasing
                0f at period using FastOutSlowInEasing
            },
        ),
        label = "glowPulse",
    )
    val glow = MaterialTheme.colorScheme.primary
    val peak = settings.glowStrengthPct / 100f
    Box(
        modifier
            .fillMaxWidth(settings.glowWidthPct / 100f)
            .height(settings.glowHeightDp.dp)
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
                alpha = presence * pulse * peak
            }
            // Fades the ends out sideways, so the light has no hard edges.
            .drawWithContent {
                drawContent()
                drawRect(
                    brush = Brush.horizontalGradient(
                        0f to Color.Transparent,
                        0.25f to Color.Black,
                        0.75f to Color.Black,
                        1f to Color.Transparent,
                    ),
                    blendMode = BlendMode.DstIn,
                )
            }
            .background(Brush.verticalGradient(0f to Color.Transparent, 1f to glow)),
    )
}
