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
import com.athea.app.ui.theme.Ui

/**
 * A soft light that rises from the bottom edge while a command runs: one pulse every couple of
 * seconds that flares quickly and fades slowly as it climbs. It fades out when the command ends.
 */
@Composable
fun RunningGlow(running: Boolean, modifier: Modifier = Modifier) {
    val presence by animateFloatAsState(
        targetValue = if (running) 1f else 0f,
        animationSpec = tween(durationMillis = Ui.glowFadeMs),
        label = "glowPresence",
    )
    if (presence > 0f) GlowPulse(presence = presence, modifier = modifier)
}

@Composable
private fun GlowPulse(presence: Float, modifier: Modifier) {
    val transition = rememberInfiniteTransition(label = "glowPulse")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes<Float> {
                durationMillis = Ui.glowPeriodMs
                0f at 0
                1f at Ui.glowPeriodMs / 8 using FastOutSlowInEasing
                0f at Ui.glowPeriodMs using FastOutSlowInEasing
            },
        ),
        label = "glowPulse",
    )
    val glow = MaterialTheme.colorScheme.primary
    Box(
        modifier
            .height(Ui.glowHeight)
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
                alpha = presence * pulse * Ui.glowPeakAlpha
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
