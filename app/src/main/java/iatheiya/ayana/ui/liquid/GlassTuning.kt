package iatheiya.ayana.ui.liquid

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Live parameters of every glass surface, edited on the glass tuning screen. They
 * live in memory only and start from the values the lens ships with.
 *
 * Glass is one master switch, not a set of modes: on, surfaces are the refracting
 * lens (a blur-only look is just the lens with its bezel and bend zeroed); off,
 * surfaces fall back to one solid opaque tone.
 */
object GlassTuning {
    var enabled by mutableStateOf(true)
    var blurDp by mutableStateOf(DEFAULT_BLUR_DP)
    var bezelDp by mutableStateOf(DEFAULT_BEZEL_DP)
    var bendDp by mutableStateOf(DEFAULT_BEND_DP)
    var chroma by mutableStateOf(DEFAULT_CHROMA)
    var highlightWidthDp by mutableStateOf(DEFAULT_HIGHLIGHT_WIDTH_DP)
    var highlightStrength by mutableStateOf(DEFAULT_HIGHLIGHT_STRENGTH)
    var lightAngleDeg by mutableStateOf(DEFAULT_LIGHT_ANGLE_DEG)
    var fillAlpha by mutableStateOf(DEFAULT_FILL_ALPHA)
    var rimAlpha by mutableStateOf(DEFAULT_RIM_ALPHA)

    fun reset() {
        enabled = true
        blurDp = DEFAULT_BLUR_DP
        bezelDp = DEFAULT_BEZEL_DP
        bendDp = DEFAULT_BEND_DP
        chroma = DEFAULT_CHROMA
        highlightWidthDp = DEFAULT_HIGHLIGHT_WIDTH_DP
        highlightStrength = DEFAULT_HIGHLIGHT_STRENGTH
        lightAngleDeg = DEFAULT_LIGHT_ANGLE_DEG
        fillAlpha = DEFAULT_FILL_ALPHA
        rimAlpha = DEFAULT_RIM_ALPHA
    }
}

private const val DEFAULT_BLUR_DP = 3f
private const val DEFAULT_BEZEL_DP = 20f
private const val DEFAULT_BEND_DP = 12f
private const val DEFAULT_CHROMA = 0.6f
private const val DEFAULT_HIGHLIGHT_WIDTH_DP = 1.5f
private const val DEFAULT_HIGHLIGHT_STRENGTH = 0.55f
private const val DEFAULT_LIGHT_ANGLE_DEG = 240f
private const val DEFAULT_FILL_ALPHA = 0f
private const val DEFAULT_RIM_ALPHA = 0.15f
