package iatheiya.ayana.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import iatheiya.ayana.ui.theme.AccentColor
import kotlin.math.roundToInt
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import iatheiya.ayana.ui.theme.Ui
import kotlinx.coroutines.launch

/** One option of an [AyanaChoiceSheet]. */
data class SheetChoice<T>(val value: T, val label: String, val icon: ImageVector? = null)

private val SHEET_SHAPE = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
private val OPTION_SHAPE = RoundedCornerShape(24.dp)

/**
 * The one way a value is chosen from a few options: a sheet that rises from the bottom with the
 * options as large rows, the current one filled and ticked. It is as tall as its options, never the
 * whole screen.
 *
 * It follows the finger: dragged down it goes down with it, and let go past a third of its height
 * (or flung) it slides away; dragged up it gives a little and springs back, as a list does at its
 * end. A tap outside, or picking an option, slides it away too.
 */
@Composable
fun <T> AyanaChoiceSheet(
    title: String,
    choices: List<SheetChoice<T>>,
    selected: T?,
    onPick: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    // The sheet's distance below its resting place, in pixels; it starts fully below.
    val offset = remember { Animatable(Float.MAX_VALUE) }
    var sheetHeight by remember { mutableStateOf(0f) }
    var closing by remember { mutableStateOf(false) }
    val accent = AccentColor.color

    fun close(then: () -> Unit = {}) {
        if (closing) return
        closing = true
        scope.launch {
            offset.animateTo(sheetHeight, tween(SLIDE_MS, easing = FastOutSlowInEasing))
            then()
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = { close() },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        // The window's own dim is replaced by a scrim that fades with the sheet.
        (LocalView.current.parent as? DialogWindowProvider)?.window?.setDimAmount(0f)
        val shown = if (sheetHeight > 0f) (1f - (offset.value / sheetHeight)).coerceIn(0f, 1f) else 0f
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = SCRIM_ALPHA * shown))
                .pointerInput(Unit) { detectTapGestures { close() } },
        ) {
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .onSizeChanged { size ->
                        if (sheetHeight == 0f) {
                            sheetHeight = size.height.toFloat()
                            scope.launch {
                                offset.snapTo(sheetHeight)
                                offset.animateTo(0f, tween(SLIDE_MS, easing = FastOutSlowInEasing))
                            }
                        }
                    }
                    .offset { IntOffset(0, offset.value.coerceAtMost(if (sheetHeight > 0f) sheetHeight else OFFSCREEN_PX).roundToInt()) }
                    .clip(SHEET_SHAPE)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    // Taps on the sheet stay on it; only taps on the scrim close it.
                    .pointerInput(Unit) { detectTapGestures { } }
                    .draggable(
                        orientation = Orientation.Vertical,
                        state = rememberDraggableState { delta ->
                            if (closing) return@rememberDraggableState
                            val next = offset.value + delta
                            // Upward, past its resting place, it only gives a little.
                            val resisted = if (next < 0f) offset.value + delta * PULL_RESISTANCE else next
                            scope.launch { offset.snapTo(resisted.coerceAtLeast(-MAX_PULL_PX)) }
                        },
                        onDragStopped = { velocity ->
                            if (offset.value > sheetHeight * DISMISS_FRACTION || velocity > FLING_VELOCITY) {
                                close()
                            } else {
                                offset.animateTo(0f, spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow))
                            }
                        },
                    )
                    .navigationBarsPadding(),
            ) {
                // The grip, as sheets show one.
                Box(
                    Modifier
                        .padding(top = Ui.spaceM, bottom = Ui.spaceL)
                        .align(Alignment.CenterHorizontally)
                        .size(width = 32.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)),
                )
                Text(
                    title,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(start = Ui.spaceXl, end = Ui.spaceXl, bottom = Ui.spaceL),
                )
                Column(
                    Modifier
                        .padding(horizontal = Ui.spaceL)
                        .padding(bottom = Ui.spaceXl),
                    verticalArrangement = Arrangement.spacedBy(Ui.spaceS),
                ) {
                    choices.forEach { choice ->
                        val current = choice.value == selected
                        // The chosen option is lit with the accent colour when one is set.
                        val fill = when {
                            !current -> Color.Transparent
                            accent != null -> accent.copy(alpha = ACCENT_FILL_ALPHA)
                            else -> MaterialTheme.colorScheme.surfaceContainerHigh
                        }
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(OPTION_SHAPE)
                                .background(fill)
                                .border(
                                    BorderStroke(1.dp, if (current) Color.Transparent else MaterialTheme.colorScheme.outline),
                                    OPTION_SHAPE,
                                )
                                .clickable { close { onPick(choice.value) } }
                                .padding(horizontal = Ui.spaceXl, vertical = 18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            choice.icon?.let {
                                Icon(it, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.width(Ui.spaceL))
                            }
                            Text(
                                choice.label,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f),
                            )
                            if (current) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }
        }
    }
}

private const val SLIDE_MS = 260

/** Where the sheet waits, below the screen, until it has been measured. */
private const val OFFSCREEN_PX = 100_000f
private const val SCRIM_ALPHA = 0.5f
private const val DISMISS_FRACTION = 0.33f
private const val FLING_VELOCITY = 1800f
private const val PULL_RESISTANCE = 0.25f
private const val MAX_PULL_PX = 120f
private const val ACCENT_FILL_ALPHA = 0.45f
