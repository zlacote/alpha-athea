package iatheiya.ayana.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp

/** A context menu to show next to [anchor] (window coordinates), with its items. */
class ContextMenuRequest(
    val anchor: Rect,
    val content: @Composable AyanaMenuScope.() -> Unit,
)

/** The one context menu of a screen: whoever is long-pressed asks for it here. */
class ContextMenuState {
    var request by mutableStateOf<ContextMenuRequest?>(null)
        private set

    fun show(request: ContextMenuRequest) {
        this.request = request
    }

    fun dismiss() {
        request = null
    }
}

val LocalContextMenu = staticCompositionLocalOf<ContextMenuState?> { null }

private val EDGE_MARGIN = 12.dp
private val ANCHOR_GAP = 6.dp

/**
 * Draws the screen's context menu in the screen itself, not in a popup window: a popup cannot see
 * what lies under it, so it could never be glass. Here the menu is the same glass panel, with the
 * same colour, as the menu of the top bar. It opens under its anchor, or above it when there is
 * no room below, right-aligned with it and kept clear of the screen's edges.
 */
@Composable
fun ContextMenuLayer(state: ContextMenuState) {
    val request = state.request ?: return
    val menu = remember(request) { AyanaMenu().apply { open() } }
    LaunchedEffect(menu) { snapshotFlow { menu.expanded }.collect { open -> if (!open) state.dismiss() } }
    var origin by remember { mutableStateOf(Offset.Zero) }
    Box(
        Modifier
            .fillMaxSize()
            .onGloballyPositioned { origin = it.positionInRoot() },
    ) {
        AyanaScrim(true, menu::close)
        Layout(
            content = {
                AyanaDropdownMenu(expanded = true, onDismissRequest = menu::close, isPopup = false) {
                    with(AyanaMenuScope(menu)) { request.content(this) }
                }
            },
            modifier = Modifier.fillMaxSize(),
        ) { measurables, constraints ->
            val panel = measurables.first().measure(constraints.copy(minWidth = 0, minHeight = 0))
            layout(constraints.maxWidth, constraints.maxHeight) {
                val margin = EDGE_MARGIN.roundToPx()
                val gap = ANCHOR_GAP.roundToPx()
                val anchor = request.anchor.translate(-origin)
                val x = (anchor.right.toInt() - panel.width)
                    .coerceAtMost(constraints.maxWidth - panel.width - margin)
                    .coerceAtLeast(margin)
                val below = anchor.bottom.toInt() + gap
                val y = if (below + panel.height <= constraints.maxHeight - margin) {
                    below
                } else {
                    (anchor.top.toInt() - gap - panel.height).coerceAtLeast(margin)
                }
                panel.place(x, y)
            }
        }
    }
}
