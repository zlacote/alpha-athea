package iatheiya.ayana.ui.main

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import iatheiya.ayana.data.CustomKey
import iatheiya.ayana.data.KeyKind
import iatheiya.ayana.ui.common.EqualCellGrid
import iatheiya.ayana.ui.common.pressWash
import iatheiya.ayana.ui.liquid.LocalLiquidBackdrop
import iatheiya.ayana.ui.liquid.liquidGlass
import iatheiya.ayana.ui.theme.Ui
import iatheiya.ayana.util.isCtrlCombinable
import iatheiya.ayana.util.toControlChar

enum class KeyActionKind {
    /** Plain text appended to the editor draft. */
    INSERT_INTO_DRAFT,

    /** Bytes sent straight to the terminal, bypassing the editor entirely. */
    SEND_TO_TERMINAL,

    /** Arms the sticky Ctrl modifier for the next combinable key. */
    TOGGLE_STICKY_CTRL,

    /** Arms the sticky Alt modifier (ESC prefix) for the next key. */
    TOGGLE_STICKY_ALT,
}

/**
 * The four cursor keys. A running program gets their bytes; an idle prompt uses them to walk the
 * sent commands and to move the caret in the composer.
 */
enum class CursorKey(val sequence: String) {
    UP("\u001B[A"),
    DOWN("\u001B[B"),
    RIGHT("\u001B[C"),
    LEFT("\u001B[D");

    companion object {
        /** The cursor key whose bytes are [payload], or null for any other key. */
        fun of(payload: String): CursorKey? = entries.firstOrNull { it.sequence == payload }
    }
}

/**
 * Ctrl+L. While no command runs it clears the screen; a running program gets it as its redraw key.
 */
const val FORM_FEED = "\u000C"

data class TerminalKey(
    val label: String,
    val kind: KeyActionKind,
    val payload: String = "",
)

object DefaultKeys {
    val CTRL = TerminalKey("CTRL", KeyActionKind.TOGGLE_STICKY_CTRL)
    val ALT = TerminalKey("ALT", KeyActionKind.TOGGLE_STICKY_ALT)
    val ESC = TerminalKey("ESC", KeyActionKind.SEND_TO_TERMINAL, "\u001B")
    val TAB = TerminalKey("TAB", KeyActionKind.SEND_TO_TERMINAL, "\t")
    val UP = TerminalKey("↑", KeyActionKind.SEND_TO_TERMINAL, CursorKey.UP.sequence)
    val DOWN = TerminalKey("↓", KeyActionKind.SEND_TO_TERMINAL, CursorKey.DOWN.sequence)
    val LEFT = TerminalKey("←", KeyActionKind.SEND_TO_TERMINAL, CursorKey.LEFT.sequence)
    val RIGHT = TerminalKey("→", KeyActionKind.SEND_TO_TERMINAL, CursorKey.RIGHT.sequence)
    val SLASH = TerminalKey("/", KeyActionKind.INSERT_INTO_DRAFT, "/")
    val PIPE = TerminalKey("|", KeyActionKind.INSERT_INTO_DRAFT, "|")
    val DASH = TerminalKey("-", KeyActionKind.INSERT_INTO_DRAFT, "-")

    // Ctrl and Alt are modifiers, not dedicated ^C/^L caps: hold Ctrl (or Alt), then
    // the next key — from the row or the keyboard — combines (e.g. Ctrl then c = ^C).
    val row = listOf(CTRL, ALT, ESC, TAB, UP, DOWN, LEFT, RIGHT, SLASH, PIPE, DASH)
}

/** The key row as the user sees it: their stored keys, or the built-in row until they change it. */
fun effectiveKeyRow(stored: List<CustomKey>): List<CustomKey> =
    stored.ifEmpty { DefaultKeys.row.map { it.toCustomKey() } }

fun CustomKey.toTerminalKey(): TerminalKey = TerminalKey(
    label = label,
    kind = when (kind) {
        KeyKind.INSERT -> KeyActionKind.INSERT_INTO_DRAFT
        KeyKind.SEND -> KeyActionKind.SEND_TO_TERMINAL
        KeyKind.CTRL -> KeyActionKind.TOGGLE_STICKY_CTRL
        KeyKind.ALT -> KeyActionKind.TOGGLE_STICKY_ALT
    },
    payload = payload,
)

private fun TerminalKey.toCustomKey(): CustomKey = CustomKey(
    label = label,
    payload = payload,
    kind = when (kind) {
        KeyActionKind.INSERT_INTO_DRAFT -> KeyKind.INSERT
        KeyActionKind.SEND_TO_TERMINAL -> KeyKind.SEND
        KeyActionKind.TOGGLE_STICKY_CTRL -> KeyKind.CTRL
    },
)

@Composable
fun KeyRow(
    keys: List<TerminalKey>,
    stickyCtrl: Boolean,
    stickyAlt: Boolean,
    suggestionActive: Boolean,
    onInsert: (String) -> Unit,
    onSendBytes: (String) -> Unit,
    onCursor: (CursorKey) -> Unit,
    onAcceptSuggestion: () -> Unit,
    onToggleStickyCtrl: () -> Unit,
    onConsumeStickyCtrl: () -> Unit,
    onToggleStickyAlt: () -> Unit,
    onConsumeStickyAlt: () -> Unit,
    modifier: Modifier = Modifier,
    rows: Int = 1,
    cols: Int = Ui.keyVisibleCount,
) {
    // Fixed bar with thin equal-width cells — not floating chips. The cell
    // width is owned by EqualCellGrid, so KeyRow carries no viewport logic
    // itself. One grid, one scroll state, one place to reason about.
    Surface(
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
            .fillMaxWidth()
            .liquidGlass(LocalLiquidBackdrop.current, RectangleShape, 0.dp, lit = false, flat = true),
    ) {
        EqualCellGrid(
            items = keys,
            rows = rows,
            columnsVisible = cols,
            cellHeight = Ui.keyMinHeight,
            separatorWidth = Ui.keySeparatorWidth,
            contentPaddingH = Ui.keyRowPaddingH,
            contentPaddingV = Ui.keyRowPaddingV,
            modifier = Modifier.fillMaxWidth(),
            separator = {
                Box(
                    Modifier.background(
                        MaterialTheme.colorScheme.outline.copy(alpha = Ui.dividerAlpha),
                    ),
                )
            },
            cell = { key ->
                val selected = (key.kind == KeyActionKind.TOGGLE_STICKY_CTRL && stickyCtrl) ||
                    (key.kind == KeyActionKind.TOGGLE_STICKY_ALT && stickyAlt)
                KeyCell(key = key, selected = selected) {
                    when (key.kind) {
                        KeyActionKind.INSERT_INTO_DRAFT -> onInsert(key.payload)

                        KeyActionKind.SEND_TO_TERMINAL -> {
                            if (key == DefaultKeys.TAB && suggestionActive) {
                                onAcceptSuggestion()
                                return@KeyCell
                            }
                            val cursor = CursorKey.of(key.payload)
                            if (cursor != null) {
                                onCursor(cursor)
                                return@KeyCell
                            }
                            // Ctrl folds a letter to its control char; Alt prefixes ESC. Both
                            // modifiers are consumed once they have combined with this key.
                            val ch = key.payload.singleOrNull()
                            val combinable = ch?.isCtrlCombinable() == true
                            val base = if (stickyCtrl && combinable && ch != null) {
                                ch.toControlChar().toString()
                            } else {
                                key.payload
                            }
                            onSendBytes(if (stickyAlt) "\u001B" + base else base)
                            if (stickyCtrl && combinable && ch != null) onConsumeStickyCtrl()
                            if (stickyAlt) onConsumeStickyAlt()
                        }

                        KeyActionKind.TOGGLE_STICKY_CTRL -> onToggleStickyCtrl()

                        KeyActionKind.TOGGLE_STICKY_ALT -> onToggleStickyAlt()
                    }
                }
            },
        )
    }
}

/**
 * One off-canvas key cell (rendered inside [EqualCellGrid]): autoshrinks to
 * fit 4-5 chars on a line, never wraps.
 */
@Composable
private fun KeyCell(
    key: TerminalKey,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val fontSize = when (key.label.length) {
        0, 1, 2 -> 15.sp
        3 -> 13.sp
        4 -> 12.sp
        else -> 11.sp
    }
    // Two layers, one per state: the armed wash fades with the sticky Ctrl state, the press wash
    // with the finger. No material ripple, so a press never replays on top of the armed state.
    val source = remember { MutableInteractionSource() }
    val haptic = LocalHapticFeedback.current
    val armed by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(durationMillis = 160),
        label = "armed",
    )
    Box(
        Modifier
            .fillMaxSize()
            .clip(Ui.smallShape)
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = armed))
            .background(pressWash(source))
            .clickable(
                interactionSource = source,
                indication = null,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
            )
            .padding(horizontal = Ui.keyPaddingH),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = key.label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = fontSize,
                lineHeight = 16.sp,
            ),
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            overflow = androidx.compose.ui.text.style.TextOverflow.Clip,
        )
    }
}
