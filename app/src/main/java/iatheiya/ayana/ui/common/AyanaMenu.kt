package iatheiya.ayana.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import iatheiya.ayana.ui.liquid.LocalLiquidBackdrop
import iatheiya.ayana.ui.liquid.liquidGlass
import iatheiya.ayana.ui.theme.ChromeTint
import iatheiya.ayana.ui.theme.Ui

/**
 * The one menu connector for the whole app: owns the open/close state and
 * the "close, then dispatch" ordering. Callers never write
 * `menuOpen = false; onX()` by hand — that one-shape pattern lives here.
 */
class AyanaMenu {
    var expanded by mutableStateOf(false)
        private set

    fun open() {
        expanded = true
    }

    fun close() {
        expanded = false
    }

    fun toggle() {
        expanded = !expanded
    }

    /** Close the menu, then run the action — the single place this order lives. */
    fun dispatch(action: () -> Unit) {
        close()
        action()
    }
}

@Composable
fun rememberAyanaMenu(): AyanaMenu = remember { AyanaMenu() }

/**
 * Receiver for menu content: every [item] closes the menu on its own,
 * the caller only describes the action.
 */
class AyanaMenuScope internal constructor(private val menu: AyanaMenu) {

    @Composable
    fun item(
        icon: ImageVector,
        text: String,
        tinted: Boolean = false,
        crossed: Boolean = false,
        action: () -> Unit,
    ) {
        AyanaDropdownItem(
            icon = icon,
            text = text,
            tinted = tinted,
            crossed = crossed,
            onClick = { menu.dispatch(action) },
        )
    }
}

/**
 * Renders [AyanaMenu]'s content: popup DropdownMenu (message/session menus)
 * or inline Surface (TopBar) to avoid the Popup window offset.
 */
@Composable
fun AyanaMenuHost(
    menu: AyanaMenu,
    isPopup: Boolean = true,
    offset: DpOffset = DpOffset.Zero,
    content: @Composable AyanaMenuScope.() -> Unit,
) {
    AyanaDropdownMenu(
        expanded = menu.expanded,
        onDismissRequest = menu::close,
        offset = offset,
        isPopup = isPopup,
    ) {
        with(AyanaMenuScope(menu)) { content() }
    }
}

/**
 * The one overflow menu style used everywhere (top bar, message
 * long-press, session long-press): large, rounded, iconed - like chat
 * apps render theirs.
 * @param isPopup true=Popup DropdownMenu (message/session), false=inline Surface (TopBar) to avoid Popup window offset/rounded peek.
 */
@Composable
fun AyanaDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    offset: DpOffset = DpOffset.Zero,
    isPopup: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (isPopup) {
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissRequest,
            offset = offset,
            shape = Ui.menuShape,
            // A popup window cannot see the screen under it, so it cannot be glass; it takes the
            // solid tone, with the chosen colour mixed in like every other surface.
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.let { base ->
                ChromeTint.color?.let { lerp(base, it, ChromeTint.SOLID_MIX) } ?: base
            },
            modifier = Modifier.widthIn(min = Ui.menuMinWidth),
            content = content,
        )
    } else {
        // A quick fade, no scale: a growing scale would also move the sampled position every
        // frame, so the lens would slowly "zoom into place" over the animation. Appearing at
        // full size lets the lens refract correctly from the first frame.
        androidx.compose.animation.AnimatedVisibility(
            visible = expanded,
            enter = androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(90)),
            exit = androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(90)),
        ) {
            // The same chrome material as the buttons: liquid glass when glass is on,
            // the one solid tone when it is off. Drawn straight onto a clipped Box (no
            // wrapping Surface, which left a square under the rounded corners), with a
            // shadow to lift it off the content behind.
            androidx.compose.foundation.layout.Box(
                Modifier
                    .shadow(Ui.menuElevation, Ui.menuShape)
                    .clip(Ui.menuShape)
                    .widthIn(min = Ui.menuMinWidth)
                    .liquidGlass(LocalLiquidBackdrop.current, Ui.menuShape, Ui.menuRadius)
                    // The panel itself holds every touch on it: a tap between or beside the items
                    // must not fall through to the scrim below and close the menu.
                    .pointerInput(Unit) { awaitEachGesture { awaitFirstDown(requireUnconsumed = false) } },
            ) {
                // As wide as its widest item, and at least as wide as the panel, so the items reach
                // its right edge (over the button that opened it) instead of leaving a dead strip.
                androidx.compose.foundation.layout.Column(
                    Modifier
                        .width(IntrinsicSize.Max)
                        .widthIn(min = Ui.menuMinWidth),
                    content = content,
                )
            }
        }
    }
}

private val CROSS_STROKE: Dp = 2.dp
private const val CROSS_INSET = 0.15f

@Composable
fun AyanaDropdownItem(
    icon: ImageVector,
    text: String,
    tinted: Boolean = false,
    crossed: Boolean = false,
    onClick: () -> Unit,
) {
    val color = if (tinted) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    DropdownMenuItem(
        leadingIcon = {
            Box(Modifier.heightIn(min = Ui.menuIconSize), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = color)
                if (crossed) {
                    // A diagonal stroke marks the item as switched off, like a crossed-out pin.
                    Canvas(Modifier.matchParentSize()) {
                        val inset = size.width * CROSS_INSET
                        drawLine(
                            color = color,
                            start = Offset(inset, inset),
                            end = Offset(size.width - inset, size.height - inset),
                            strokeWidth = CROSS_STROKE.toPx(),
                            cap = StrokeCap.Round,
                        )
                    }
                }
            }
        },
        text = {
            Text(
                text,
                style = MaterialTheme.typography.bodyLarge,
                color = if (tinted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
        },
        modifier = Modifier.heightIn(min = Ui.menuItemMinHeight),
        onClick = onClick,
    )
}
