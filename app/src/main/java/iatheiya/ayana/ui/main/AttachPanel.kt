package iatheiya.ayana.ui.main

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import iatheiya.ayana.ui.liquid.LocalLiquidBackdrop
import iatheiya.ayana.ui.liquid.liquidGlass
import iatheiya.ayana.ui.theme.Ui
import kotlin.math.roundToInt

/** One choice in the attach panel: its icon, its words, and what it does. */
class AttachChoice(val icon: ImageVector, val label: String, val onPick: () -> Unit)

/**
 * The panel the composer's + opens: a glass card that grows out of the composer's lower left
 * corner, over it, in the same glass as the other menus (tinted with them), with a large row
 * per choice. A tap anywhere else
 * puts it away; there is no Cancel.
 *
 * [anchor] is the composer panel's bounds in the root, [origin] this screen's own position there.
 */
@Composable
fun AttachPanel(open: Boolean, anchor: Rect, origin: Offset, choices: List<AttachChoice>, onDismiss: () -> Unit) {
    if (open) {
        Box(
            Modifier
                .fillMaxSize()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        )
    }
    var height by remember { mutableIntStateOf(0) }
    val inset = with(LocalDensity.current) { Ui.spaceS.roundToPx() }
    AnimatedVisibility(
        visible = open,
        enter = fadeIn(tween(140)) + scaleIn(tween(180), initialScale = .85f, transformOrigin = TransformOrigin(0f, 1f)),
        exit = fadeOut(tween(120)) + scaleOut(tween(140), targetScale = .9f, transformOrigin = TransformOrigin(0f, 1f)),
        modifier = Modifier.offset {
            IntOffset(
                (anchor.left - origin.x).roundToInt() + inset,
                (anchor.bottom - origin.y).roundToInt() - inset - height,
            )
        },
    ) {
        Column(
            Modifier
                .onSizeChanged { height = it.height }
                .widthIn(min = PANEL_MIN_WIDTH)
                .liquidGlass(LocalLiquidBackdrop.current, Ui.menuShape, Ui.menuRadius)
                // Rows reach the panel's edges, so a pressed row lights up to them and no further.
                .clip(Ui.menuShape)
                .width(IntrinsicSize.Max),
        ) {
            choices.forEach { choice ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            onDismiss()
                            choice.onPick()
                        }
                        .padding(horizontal = Ui.spaceXl, vertical = ROW_PADDING_V),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // A bare icon, as everywhere else.
                    Icon(choice.icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = choice.label,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = Ui.spaceL, end = Ui.spaceL),
                    )
                }
            }
        }
    }
}

private val PANEL_MIN_WIDTH = 240.dp
private val ROW_PADDING_V = 18.dp
