package iatheiya.ayana.ui.common

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/**
 * The one full-screen tap-catcher: any tap outside the overlay dismisses
 * it. No ripple — matches the drawer behaviour (no white flash).
 * Every hand-rolled "scrim Box" in the app goes through this.
 */
@Composable
fun AyanaScrim(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!visible) return
    // Any touch closes what the scrim guards: a tap, but also a swipe or a drag in any direction.
    // The whole gesture is taken, so it does not also scroll or press what lies under the scrim.
    Box(
        modifier
            .fillMaxSize()
            .pointerInput(onDismiss) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false).consume()
                    onDismiss()
                    do {
                        val event = awaitPointerEvent()
                        event.changes.forEach { it.consume() }
                    } while (event.changes.any { it.pressed })
                }
            },
    )
}
