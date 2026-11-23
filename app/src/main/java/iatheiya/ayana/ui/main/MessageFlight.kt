package iatheiya.ayana.ui.main

import android.os.SystemClock
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntSize
import iatheiya.ayana.ui.liquid.drawContentInto
import kotlin.math.roundToInt

/**
 * A sent message flying out of the composer, as in a messenger: the words just written become the
 * message where they stand in the field, its bubble filling in around them, and it rises from there
 * to its place in the chat, over the composer, while the chat makes room by scrolling up.
 *
 * The message itself is the real item of the list, laid out in its place; while it flies it is not
 * drawn there but recorded into [layer], which [MessageFlightLayer] draws above the composer, moved
 * from where the words stood in the field to where the item is. Positions are read from layout
 * coordinates at draw time, so the flying message and the scrolling chat move in the same frame.
 */
internal class MessageFlight {
    /** Where the composer's words start, in the window; kept by the composer. */
    internal var composerText: LayoutCoordinates? = null

    /** The words' start at the moment of sending, and when that was. */
    private var takeoff: Offset? = null
    private var takeoffAt = 0L

    /** From sending until the message lands: the composer shrinks on the flight's own spring. */
    var launching by mutableStateOf(false)
        private set

    /** The list item that flies, while it does. */
    var key: Any? by mutableStateOf(null)
        private set

    /** 0 at the composer, 1 in place. */
    var progress by mutableFloatStateOf(1f)
        internal set

    /** How opaque the bubble is: it fills in over the first part of the flight. */
    val bubbleAlpha: Float get() = (progress / BUBBLE_FILL_SHARE).coerceIn(0f, 1f)

    internal var layer: GraphicsLayer? = null
    internal var item: LayoutCoordinates? = null
    internal var bubble: LayoutCoordinates? = null
    internal var textInset: Offset = Offset.Zero

    /** From where the words stood to where the bubble's words are laid out, fixed at boarding. */
    internal var shift: Offset? = null

    /** Flights so far; each one is animated once. */
    var count by mutableIntStateOf(0)
        private set

    /** The composer sends: remember where its words stood. */
    fun takeOff() {
        takeoff = composerText?.takeIf { it.isAttached }?.positionInRoot()
        takeoffAt = SystemClock.uptimeMillis()
        launching = takeoff != null
    }

    /** A sent message has come into the list as [itemKey]: it flies, if it was just sent from the composer. */
    fun board(itemKey: Any) {
        val from = takeoff ?: return
        takeoff = null
        if (SystemClock.uptimeMillis() - takeoffAt > BOARDING_WINDOW_MS) {
            launching = false
            return
        }
        start = from
        shift = null
        item = null
        bubble = null
        progress = 0f
        key = itemKey
        count++
    }

    /** No flight for what comes in next (a message sent by other means, or not followed). */
    fun cancelTakeoff() {
        takeoff = null
        launching = false
    }

    internal fun land() {
        key = null
        progress = 1f
        launching = false
    }

    private var start: Offset = Offset.Zero

    /** The words' place in the bubble now, when the bubble is laid out. */
    private fun textNow(): Offset? = bubble?.takeIf { it.isAttached }?.positionInRoot()?.plus(textInset)

    /** Fixes the shift on first sight of the laid-out bubble; afterwards returns it. */
    internal fun shiftNow(): Offset? {
        shift?.let { return it }
        val text = textNow() ?: return null
        return (start - text).also { shift = it }
    }
}

/** The flight the screen's composer and chat share; null where nothing flies. */
internal val LocalMessageFlight = staticCompositionLocalOf<MessageFlight?> { null }

/**
 * For the list item [key]: while it flies, it is recorded for [MessageFlightLayer] instead of being
 * drawn in place; it still takes its place in the layout, so the chat scrolls as if it were there.
 */
internal fun Modifier.flyingItem(flight: MessageFlight?, key: Any): Modifier {
    if (flight == null) return this
    return this
        .onGloballyPositioned { if (flight.key == key) flight.item = it }
        .drawWithContent {
            val layer = flight.layer
            // Drawn in place unless the flight can draw it: it needs the bubble's place for that.
            if (flight.key != key || layer == null || flight.bubble == null) {
                drawContent()
                return@drawWithContent
            }
            val pixels = IntSize(size.width.roundToInt(), size.height.roundToInt())
            layer.record(this, layoutDirection, pixels) { drawContentInto(this@drawWithContent) }
        }
}

/** The bubble of the flying item reports where it is and where its words start inside it. */
internal fun Modifier.flyingBubble(flight: MessageFlight?, key: Any, textInset: Offset): Modifier {
    if (flight == null) return this
    return onGloballyPositioned {
        if (flight.key == key) {
            flight.bubble = it
            flight.textInset = textInset
        }
    }
}

/**
 * Draws the flying message above the composer and runs the flight. Put it over the composer, in
 * the same window as the chat.
 */
@Composable
internal fun MessageFlightLayer(flight: MessageFlight, modifier: Modifier = Modifier) {
    val layer = rememberGraphicsLayer()
    flight.layer = layer
    LaunchedEffect(flight.count) {
        if (flight.count == 0) return@LaunchedEffect
        val mine = flight.key
        try {
            animate(0f, 1f, animationSpec = FLIGHT_SPRING) { value, _ -> flight.progress = value }
        } finally {
            // A newer flight may have taken over; it lands on its own.
            if (flight.key == mine) flight.land()
        }
    }
    val origin = remember { arrayOfNulls<LayoutCoordinates>(1) }
    Box(
        modifier
            .fillMaxSize()
            .onGloballyPositioned { origin[0] = it }
            .drawBehind {
                if (flight.key == null) return@drawBehind
                val p = flight.progress
                val item = flight.item?.takeIf { it.isAttached } ?: return@drawBehind
                val shift = flight.shiftNow() ?: return@drawBehind
                val here = origin[0]?.takeIf { it.isAttached }?.positionInRoot() ?: Offset.Zero
                // Across faster than up, as in a messenger: the bubble reaches its side early and
                // settles into its place on the way up.
                val across = 1f - (1f - p) * (1f - p) * (1f - p)
                val at = item.positionInRoot() + Offset(shift.x * (1f - across), shift.y * (1f - p)) - here
                translate(at.x, at.y) { drawLayer(layer) }
            },
    )
}

/**
 * One spring for the message's flight, for the chat making room and for the composer shrinking
 * after it, so they move as one. About as quick as a messenger's: mostly there in a tenth of a second.
 */
internal val FLIGHT_SPRING = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMedium,
)

/** [FLIGHT_SPRING] for the composer's size. */
internal val FLIGHT_RESIZE = spring(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMedium,
    visibilityThreshold = IntSize.VisibilityThreshold,
)

/** The share of the flight over which the bubble fills in around the words. */
private const val BUBBLE_FILL_SHARE = 0.35f

/** A message that comes into the list later than this after sending does not fly. */
private const val BOARDING_WINDOW_MS = 1500L
