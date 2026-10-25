package iatheiya.ayana.ui.main

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import iatheiya.ayana.R
import iatheiya.ayana.data.CustomKey
import iatheiya.ayana.ui.liquid.backdropGap
import iatheiya.ayana.ui.theme.Ui
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * The key a finger is carrying in [KeyGridEditor]. The grid only leaves a gap for it; the key itself
 * is drawn by [KeyDragGhostOverlay] at the screen level, so it floats over every other panel
 * instead of being clipped by the grid's scroll area or hidden under the cards around it.
 */
@Stable
class KeyDragGhost {
    var key by mutableStateOf<CustomKey?>(null)
        internal set

    /** Window position of the picked-up cell, where [offset] is measured from. */
    internal var origin by mutableStateOf(Offset.Zero)
    internal var size by mutableStateOf(IntSize.Zero)
    internal val offset = Animatable(Offset.Zero, Offset.VectorConverter)

    /** Dragged well away from the grid: letting go here puts the key back where it was. */
    internal var outside by mutableStateOf(false)
}

/**
 * The key grid as an editable preview: it lays the keys out exactly as the live row
 * shows them (column-major, [rows] per column) and scrolls horizontally. Hold a key
 * and drag to reorder it — the other keys slide to make room — and tap a key to open
 * its editor (where it can also be removed). Dropped far outside the grid, the key
 * returns to its place.
 */
@Composable
fun KeyGridEditor(
    keys: List<CustomKey>,
    rows: Int,
    cols: Int,
    ghost: KeyDragGhost,
    onKeysChange: (List<CustomKey>) -> Unit,
    onRename: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rowCount = rows.coerceAtLeast(1)
    val colsVisible = cols.coerceAtLeast(1)
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var targetIndex by remember { mutableStateOf<Int?>(null) }

    val sep = Ui.keySeparatorWidth
    val cellH = Ui.keyMinHeight

    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .padding(horizontal = Ui.keyRowPaddingH),
    ) {
        val width = maxWidth.value
        val viewport = if (width.isFinite() && width > 0f) maxWidth else 0.dp
        val fixed = sep * (colsVisible - 1).coerceAtLeast(0)
        val cellW = if (viewport <= fixed) 0.dp else (viewport - fixed) / colsVisible
        val pitchXpx = with(density) { (cellW + sep).toPx() }
        // A row is a key plus the gap under it, as in the live row.
        val pitchYpx = with(density) { (cellH + sep).toPx() }

        // The pixel position of slot [k] in the column-major grid.
        fun slotOffset(k: Int) = Offset((k / rowCount) * pitchXpx, (k % rowCount) * pitchYpx)

        val columns = keys.chunked(rowCount)
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            columns.forEachIndexed { columnIndex, columnKeys ->
                if (columnIndex > 0) {
                    Box(
                        Modifier
                            .width(sep)
                            .height(cellH * rowCount + sep * (rowCount - 1))
                            .backdropGap(null),
                    )
                }
                Column(Modifier.width(cellW)) {
                    for (rowIndex in 0 until rowCount) {
                        if (rowIndex > 0) {
                            Box(Modifier.width(cellW).height(sep).backdropGap(null))
                        }
                        val index = columnIndex * rowCount + rowIndex
                        val key = columnKeys.getOrNull(rowIndex)
                        // Flat cells side by side, with the same separators as the live row.
                        Box(
                            Modifier
                                .width(cellW)
                                .height(cellH),
                        ) {
                            if (key != null) {
                                val dragging = draggedIndex == index
                                // Non-dragged keys slide to the slot they take while a drag is live.
                                val slide = if (draggedIndex != null && targetIndex != null && !dragging) {
                                    val display = displayPosition(index, draggedIndex!!, targetIndex!!)
                                    slotOffset(display) - slotOffset(index)
                                } else {
                                    Offset.Zero
                                }
                                // While dragging, the make-room shift springs; on release the list has
                                // already reordered into these exact slots, so the slide snaps to zero
                                // instead of replaying the animation on top of the reorder (the jitter).
                                val animatedSlide by animateOffsetAsState(
                                    targetValue = slide,
                                    animationSpec = if (draggedIndex != null) {
                                        spring(stiffness = Spring.StiffnessMediumLow)
                                    } else {
                                        snap()
                                    },
                                    label = "slot",
                                )
                                var cellOrigin by remember { mutableStateOf(Offset.Zero) }
                                var cellSize by remember { mutableStateOf(IntSize.Zero) }
                                EditableKeyCell(
                                    key = key,
                                    // The carried key is drawn by the overlay; its own cell is the gap.
                                    hidden = dragging,
                                    translation = animatedSlide,
                                    modifier = Modifier.onGloballyPositioned {
                                        cellOrigin = it.positionInRoot()
                                        cellSize = it.size
                                    },
                                    onTap = { onRename(index) },
                                    onDragStart = {
                                        if (ghost.key != null) return@EditableKeyCell
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        draggedIndex = index
                                        targetIndex = index
                                        ghost.origin = cellOrigin
                                        ghost.size = cellSize
                                        ghost.outside = false
                                        ghost.key = key
                                        scope.launch { ghost.offset.snapTo(Offset.Zero) }
                                    },
                                    onDrag = { delta ->
                                        if (draggedIndex != index) return@EditableKeyCell
                                        val moved = ghost.offset.value + delta
                                        scope.launch { ghost.offset.snapTo(moved) }
                                        val outside = isFarOutside(index, moved, pitchXpx, pitchYpx, rowCount, columns.size)
                                        ghost.outside = outside
                                        // Out there, the others slide back: the drop would change nothing.
                                        targetIndex = if (outside) {
                                            index
                                        } else {
                                            targetFor(index, moved, pitchXpx, pitchYpx, rowCount, keys.size)
                                        }
                                    },
                                    onDragEnd = {
                                        val from = draggedIndex ?: return@EditableKeyCell
                                        val to = targetIndex ?: from
                                        // The key glides into the slot it lands in (its own, when the drop
                                        // was far outside), and only then does the list reorder.
                                        scope.launch {
                                            ghost.offset.animateTo(
                                                slotOffset(to) - slotOffset(from),
                                                tween(durationMillis = 160),
                                            )
                                            if (to != from) onKeysChange(moveItem(keys, from, to))
                                            draggedIndex = null
                                            targetIndex = null
                                            ghost.key = null
                                            ghost.outside = false
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Draws the key a finger carries, above everything under it on the screen. Placed by the screen
 * between its content and its back button, so the key passes over the cards and stays under the
 * back button.
 */
@Composable
fun KeyDragGhostOverlay(ghost: KeyDragGhost, modifier: Modifier = Modifier) {
    var overlayOrigin by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current
    Box(
        modifier
            .fillMaxSize()
            .onGloballyPositioned { overlayOrigin = it.positionInRoot() },
    ) {
        val key = ghost.key ?: return@Box
        KeyCellFace(
            key = key,
            highlighted = true,
            modifier = Modifier
                .offset {
                    val at = ghost.origin + ghost.offset.value - overlayOrigin
                    IntOffset(at.x.roundToInt(), at.y.roundToInt())
                }
                .size(with(density) { ghost.size.width.toDp() }, with(density) { ghost.size.height.toDp() })
                .graphicsLayer {
                    scaleX = GHOST_SCALE
                    scaleY = GHOST_SCALE
                    alpha = if (ghost.outside) GHOST_OUTSIDE_ALPHA else 1f
                },
        )
    }
}

private const val GHOST_SCALE = 1.08f
private const val GHOST_OUTSIDE_ALPHA = 0.55f

/** More than one whole cell beyond the grid's edge, on any side. */
private fun isFarOutside(
    from: Int,
    offset: Offset,
    pitchX: Float,
    pitchY: Float,
    rows: Int,
    columns: Int,
): Boolean {
    if (pitchX <= 0f || pitchY <= 0f) return false
    val row = from % rows + offset.y / pitchY
    val column = from / rows + offset.x / pitchX
    return row < -1f || row > rows || column < -1f || column > columns
}

/** Where a key dragged from [from] by [offset] lands, in the column-major grid. */
private fun targetFor(
    from: Int,
    offset: Offset,
    pitchX: Float,
    pitchY: Float,
    rows: Int,
    size: Int,
): Int {
    val column = from / rows
    val row = from % rows
    val columnDelta = if (pitchX > 0f) (offset.x / pitchX).roundToInt() else 0
    val rowDelta = if (pitchY > 0f) (offset.y / pitchY).roundToInt() else 0
    val newColumn = (column + columnDelta).coerceAtLeast(0)
    val newRow = (row + rowDelta).coerceIn(0, rows - 1)
    return (newColumn * rows + newRow).coerceIn(0, size - 1)
}

/** The display slot a key at [index] takes while [from] is being dragged to [to]. */
private fun displayPosition(index: Int, from: Int, to: Int): Int {
    val withoutDragged = if (index < from) index else index - 1
    return if (withoutDragged >= to) withoutDragged + 1 else withoutDragged
}

private fun <T> moveItem(list: List<T>, from: Int, to: Int): List<T> {
    val mutable = list.toMutableList()
    val item = mutable.removeAt(from)
    mutable.add(to.coerceIn(0, mutable.size), item)
    return mutable
}

@Composable
private fun EditableKeyCell(
    key: CustomKey,
    hidden: Boolean,
    translation: Offset,
    onTap: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    KeyCellFace(
        key = key,
        highlighted = false,
        modifier = modifier
            .graphicsLayer {
                translationX = translation.x
                translationY = translation.y
                alpha = if (hidden) 0f else 1f
            }
            // Long-press to pick up and drag; a single tap opens the editor at once (no
            // double-tap, so the tap never waits to see whether a second one is coming).
            .pointerInput(key) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { onDragStart() },
                    onDrag = { change, amount ->
                        change.consume()
                        onDrag(amount)
                    },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() },
                )
            }
            .pointerInput(key) {
                detectTapGestures(onTap = { onTap() })
            },
    )
}

/**
 * How one key looks in the editor, in its cell or carried by a finger: the live row's own label
 * on the row's material, and the armed accent while it is carried.
 */
@Composable
private fun KeyCellFace(key: CustomKey, highlighted: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxSize()
            .background(if (highlighted) keyArmedFill() else Color.Transparent),
        contentAlignment = Alignment.Center,
    ) {
        KeyFaceText(
            label = key.label.ifEmpty { stringResource(R.string.settings_keys_unnamed) },
            selected = highlighted,
        )
    }
}
