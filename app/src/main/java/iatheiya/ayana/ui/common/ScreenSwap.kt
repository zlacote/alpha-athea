package iatheiya.ayana.ui.common

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Swaps whole screens (settings and their rooms, favourites, the chat) with a short move from the
 * right, as the arrows on the rows that open them point: the screen going deeper comes in a short
 * way from the right while the one it covers leaves a short way to the left, both fading; back runs
 * the other way. A short step, not a slide across the whole width.
 *
 * [depth] orders the screens: a target deeper than the current one is a step in.
 */
@Composable
internal fun <T> ScreenSwap(
    target: T,
    depth: (T) -> Int,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit,
) {
    AnimatedContent(
        targetState = target,
        modifier = modifier.fillMaxSize(),
        transitionSpec = { sidewaysSwap(forward = depth(targetState) >= depth(initialState)) },
        label = "screen",
    ) { shown -> content(shown) }
}

private fun sidewaysSwap(forward: Boolean): ContentTransform {
    val side = if (forward) 1 else -1
    val enter = fadeIn(tween(SWAP_IN_MS, delayMillis = SWAP_IN_DELAY_MS, easing = LinearOutSlowInEasing)) +
        slideInHorizontally(tween(SCREEN_SWAP_MS, easing = FastOutSlowInEasing)) { width -> side * width / SLIDE_SHARE }
    val exit = fadeOut(tween(SWAP_OUT_MS, easing = FastOutSlowInEasing)) +
        slideOutHorizontally(tween(SCREEN_SWAP_MS, easing = FastOutSlowInEasing)) { width -> -side * width / SLIDE_SHARE }
    // The screen coming in is drawn above the one leaving, either way.
    return ContentTransform(enter, exit, targetContentZIndex = 1f, sizeTransform = SizeTransform(clip = false))
}

/** How long a swap takes, for whatever must wait until the leaving screen is gone. */
internal const val SCREEN_SWAP_MS = 260
private const val SWAP_IN_MS = 200
private const val SWAP_IN_DELAY_MS = 40
private const val SWAP_OUT_MS = 140
/** Each screen moves by this share of the width: a short step, not a full slide. */
private const val SLIDE_SHARE = 5
