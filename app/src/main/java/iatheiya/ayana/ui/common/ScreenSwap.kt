package iatheiya.ayana.ui.common

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize

/**
 * Swaps whole screens (settings, a settings room, favourites) with a short, small move: the screen
 * going deeper grows in from slightly smaller and fades in while the one it covers fades out,
 * growing a little; going back plays the same the other way. No slide across the width: the
 * screens stay where they are, only their depth changes.
 *
 * [depth] orders the screens: a target deeper than the current one is a step in. A [still] screen
 * only fades, without the scale: its glass finds what it refracts by its place in the window, and
 * a scaled screen would bend the wrong part of itself for as long as the swap lasts.
 */
@Composable
internal fun <T> ScreenSwap(
    target: T,
    depth: (T) -> Int,
    modifier: Modifier = Modifier,
    still: (T) -> Boolean = { false },
    content: @Composable (T) -> Unit,
) {
    AnimatedContent(
        targetState = target,
        modifier = modifier.fillMaxSize(),
        transitionSpec = {
            screenSwap(
                forward = depth(targetState) >= depth(initialState),
                scaleIn = !still(targetState),
                scaleOut = !still(initialState),
            )
        },
        label = "screen",
    ) { shown -> content(shown) }
}

private fun screenSwap(forward: Boolean, scaleIn: Boolean, scaleOut: Boolean): ContentTransform {
    var enter = fadeIn(tween(SWAP_IN_MS, delayMillis = SWAP_IN_DELAY_MS, easing = LinearOutSlowInEasing))
    if (scaleIn) {
        enter += scaleIn(tween(SWAP_MS, easing = FastOutSlowInEasing), initialScale = if (forward) SWAP_NEAR else SWAP_FAR)
    }
    var exit = fadeOut(tween(SWAP_OUT_MS, easing = FastOutSlowInEasing))
    if (scaleOut) {
        exit += scaleOut(tween(SWAP_MS, easing = FastOutSlowInEasing), targetScale = if (forward) SWAP_FAR else SWAP_NEAR)
    }
    // The screen coming in is drawn above the one leaving, either way.
    return ContentTransform(enter, exit, targetContentZIndex = 1f, sizeTransform = SizeTransform(clip = false))
}

/** How long a swap takes, for whatever must wait until the leaving screen is gone. */
internal const val SCREEN_SWAP_MS = 260
private const val SWAP_MS = SCREEN_SWAP_MS
private const val SWAP_IN_MS = 200
private const val SWAP_IN_DELAY_MS = 40
private const val SWAP_OUT_MS = 140
private const val SWAP_NEAR = 0.94f
private const val SWAP_FAR = 1.04f
