package iatheiya.ayana.ui.common

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * How far the drawer is out, from 0 (shut) to 1 (open). It follows the finger while a swipe
 * carries it, and settles to an end when the finger lets go.
 */
@Stable
class AyanaDrawerState internal constructor(private val scope: CoroutineScope) {
    internal val progress = Animatable(0f)
    internal var widthPx by mutableFloatStateOf(1f)

    /** Out at all: opening, open or closing. */
    val isVisible: Boolean get() = progress.value > 0f || progress.isRunning
    val isOpen: Boolean get() = progress.value >= 1f && !progress.isRunning

    fun open() { scope.launch { progress.animateTo(1f, SETTLE) } }
    fun close() { scope.launch { progress.animateTo(0f, SETTLE) } }

    // Where the finger has the drawer, kept here so quick moves add up before each snap lands.
    private var dragAt = 0f

    internal fun startDrag() { dragAt = progress.value }

    internal fun dragBy(dx: Float) {
        dragAt = (dragAt + dx / widthPx).coerceIn(0f, 1f)
        val to = dragAt
        scope.launch { progress.snapTo(to) }
    }

    /**
     * Where a swipe lands when the finger lets go. A slow pull opens once it has brought the
     * drawer a third of the way, and shuts once it has taken it back a third. A quick flick
     * counts only after the drawer has come a fair way too, so a twitch of a few pixels, however
     * fast, puts it back instead of throwing it open.
     */
    internal fun settle(velocityPx: Float, opening: Boolean) {
        val flick = FLICK_DP_PER_S * densityScale
        val moved = if (opening) progress.value else 1f - progress.value
        val target = when {
            moved >= FLICK_MIN_TRAVEL && velocityPx > flick -> 1f
            moved >= FLICK_MIN_TRAVEL && velocityPx < -flick -> 0f
            opening -> if (progress.value > ONE_THIRD) 1f else 0f
            else -> if (progress.value < 1f - ONE_THIRD) 0f else 1f
        }
        scope.launch { progress.animateTo(target, SETTLE) }
    }

    internal var densityScale = 1f

    private companion object {
        const val ONE_THIRD = 1f / 3f
        const val FLICK_DP_PER_S = 900f
        /** How far a flick must have carried the drawer before its speed decides. */
        const val FLICK_MIN_TRAVEL = 0.15f
        /** The rest of the way, at an even, unhurried pace. */
        val SETTLE = tween<Float>(durationMillis = 300, easing = FastOutSlowInEasing)
    }
}

@Composable
fun rememberAyanaDrawerState(): AyanaDrawerState {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current.density
    return remember { AyanaDrawerState(scope) }.also { it.densityScale = density }
}

/**
 * A side drawer that a swipe to the right pulls out from anywhere on the content, and a swipe to
 * the left (or a tap on the dimmed content, or back) puts away. The swipe is watched on the
 * initial pass, so a message or an output under the finger cannot keep it; it lets go of vertical
 * scrolls, a second finger and a press held into a long press.
 */
@Composable
fun AyanaDrawer(
    state: AyanaDrawerState,
    enabled: Boolean,
    drawerContent: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    BackHandler(enabled = state.isVisible) { state.close() }
    Box(
        Modifier
            .fillMaxSize()
            .then(if (enabled) Modifier.drawerSwipes(state) else Modifier),
    ) {
        content()
        val p = state.progress.value
        if (p > 0f) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = SCRIM_ALPHA * p))
                    .pointerInput(state) {
                        awaitEachGesture {
                            awaitFirstDown()
                            val up = awaitPointerEventUp()
                            if (up) state.close()
                        }
                    },
            )
        }
        Box(
            Modifier
                .onSizeChanged { state.widthPx = it.width.toFloat().coerceAtLeast(1f) }
                .offset { IntOffset(((p - 1f) * state.widthPx).roundToInt(), 0) },
        ) {
            if (p > 0f) drawerContent()
        }
    }
}

private const val SCRIM_ALPHA = 0.4f

/** Waits for the finger to lift; true when it lifted without being taken by a drag. */
private suspend fun androidx.compose.ui.input.pointer.AwaitPointerEventScope.awaitPointerEventUp(): Boolean {
    while (true) {
        val event = awaitPointerEvent()
        val change = event.changes.firstOrNull() ?: return false
        if (change.isConsumed) return false
        if (!change.pressed) return true
    }
}

private fun Modifier.drawerSwipes(state: AyanaDrawerState): Modifier = pointerInput(state) {
    val slop = viewConfiguration.touchSlop
    val longPress = viewConfiguration.longPressTimeoutMillis
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val opening = state.progress.value < 0.5f
        var travel = Offset.Zero
        var dragging = false
        val tracker = VelocityTracker()
        tracker.addPosition(down.uptimeMillis, down.position)
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            if (event.changes.size > 1 && !dragging) break
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            tracker.addPosition(change.uptimeMillis, change.position)
            if (!change.pressed) {
                if (dragging) state.settle(tracker.calculateVelocity().x, opening)
                break
            }
            val delta = change.positionChange()
            if (!dragging) {
                if (change.uptimeMillis - down.uptimeMillis > longPress) break
                travel += delta
                val horizontal = abs(travel.x) > slop && abs(travel.x) > abs(travel.y) * 2
                // Shut, only a swipe to the right starts it; open, only one to the left.
                if (horizontal && (if (opening) travel.x > 0 else travel.x < 0)) {
                    dragging = true
                    state.startDrag()
                    state.dragBy(travel.x - (if (travel.x > 0) slop else -slop))
                } else if (abs(travel.y) > slop || (opening && travel.x < -slop) || (!opening && travel.x > slop)) {
                    break
                }
            } else {
                state.dragBy(delta.x)
            }
            // Once it is the drawer's swipe, nothing under the finger sees it.
            if (dragging) event.changes.forEach { it.consume() }
        }
    }
}
