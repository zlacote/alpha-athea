package iatheiya.ayana.ui.main

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import iatheiya.ayana.data.AyanaSettings
import iatheiya.ayana.ui.theme.Ui

/**
 * A soft light that glows down from the top edge while a command runs. Its strength, height, width
 * and pulse come from the settings. It is not drawn at all when the glow is switched off.
 */
@Composable
fun RunningGlow(running: Boolean, settings: AyanaSettings, modifier: Modifier = Modifier) {
    val presence by animateFloatAsState(
        targetValue = if (running && settings.glowEnabled) 1f else 0f,
        animationSpec = tween(durationMillis = Ui.glowFadeMs),
        label = "glowPresence",
    )
    if (presence > 0f) GlowPulse(presence = presence, settings = settings, modifier = modifier)
}

@Composable
private fun GlowPulse(presence: Float, settings: AyanaSettings, modifier: Modifier) {
    val pulse by rememberCommandPulse(settings.glowPeriodMs)
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
            .background(Brush.verticalGradient(0f to glow, 1f to Color.Transparent)),
    )
}

/** How long one pulse lasts, flare and fade, however far apart the pulses come. */
private const val PULSE_MS = 1200

/**
 * The pulse of a running command, 0 to 1: a quick flare, a slower fade, and dark until the next,
 * one every [periodMs]. It is read from the frame clock, the same for everything on screen, so the
 * glow at the top and the dot of the running session in the drawer pulse together.
 */
@Composable
fun rememberCommandPulse(periodMs: Int): State<Float> {
    val pulse = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(periodMs) {
        val period = periodMs.coerceAtLeast(1).toLong()
        val length = minOf(periodMs, PULSE_MS).toFloat()
        while (true) {
            withFrameMillis { now ->
                val t = (now % period).toFloat()
                val flare = length / 8
                pulse.floatValue = when {
                    t < flare -> FastOutSlowInEasing.transform(t / flare)
                    t < length -> 1f - FastOutSlowInEasing.transform((t - flare) / (length - flare))
                    else -> 0f
                }
            }
        }
    }
    return pulse
}
