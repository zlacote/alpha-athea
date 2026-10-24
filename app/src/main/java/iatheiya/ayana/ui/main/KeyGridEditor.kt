package iatheiya.ayana.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import iatheiya.ayana.R
import iatheiya.ayana.data.CustomKey
import iatheiya.ayana.ui.theme.Ui
import kotlin.math.roundToInt

/**
 * The key grid as an editable preview: it lays the keys out exactly as the live row
 * shows them (column-major, [rows] per column) and scrolls horizontally. Hold a key
 * and drag to reorder it; tap to rename; double-tap to arm a red cross, then tap the
 * cross to remove.
 */
@Composable
fun KeyGridEditor(
    keys: List<CustomKey>,
    rows: Int,
    cols: Int,
    onKeysChange: (List<CustomKey>) -> Unit,
    onRename: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rowCount = rows.coerceAtLeast(1)
    val colsVisible = cols.coerceAtLeast(1)
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var armedDelete by remember { mutableStateOf<Int?>(null) }

    val sep = Ui.keySeparatorWidth
    val cellH = Ui.keyMinHeight

    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .padding(horizontal = Ui.keyRowPaddingH, vertical = 8.dp),
    ) {
        val width = maxWidth.value
        val viewport = if (width.isFinite() && width > 0f) maxWidth else 0.dp
        val fixed = sep * (colsVisible - 1).coerceAtLeast(0)
        val cellW = if (viewport <= fixed) 0.dp else (viewport - fixed) / colsVisible
        val pitchXpx = with(density) { (cellW + sep).toPx() }
        val pitchYpx = with(density) { cellH.toPx() }

        // Column-major, exactly as the live row fills.
        val columns = keys.chunked(rowCount)
        androidx.compose.foundation.layout.Row(Modifier.horizontalScroll(rememberScrollState())) {
            columns.forEachIndexed { columnIndex, columnKeys ->
                if (columnIndex > 0) Spacer(Modifier.width(sep))
                Column(Modifier.width(cellW)) {
                    for (rowIndex in 0 until rowCount) {
                        val index = columnIndex * rowCount + rowIndex
                        val key = columnKeys.getOrNull(rowIndex)
                        Box(
                            Modifier
                                .width(cellW)
                                .height(cellH)
                                .padding(2.dp),
                        ) {
                            if (key != null) {
                                EditableKeyCell(
                                    key = key,
                                    dragging = draggedIndex == index,
                                    dragOffset = if (draggedIndex == index) dragOffset else Offset.Zero,
                                    armed = armedDelete == index,
                                    onTap = {
                                        if (armedDelete == index) armedDelete = null else onRename(index)
                                    },
                                    onDoubleTap = {
                                        armedDelete = if (armedDelete == index) null else index
                                    },
                                    onConfirmDelete = {
                                        armedDelete = null
                                        onKeysChange(keys.filterIndexed { i, _ -> i != index })
                                    },
                                    onDragStart = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        draggedIndex = index
                                        dragOffset = Offset.Zero
                                        armedDelete = null
                                    },
                                    onDrag = { delta -> dragOffset += delta },
                                    onDragEnd = {
                                        draggedIndex?.let { from ->
                                            val target = targetIndex(
                                                from, dragOffset, pitchXpx, pitchYpx, rowCount, keys.size,
                                            )
                                            if (target != from) onKeysChange(moveItem(keys, from, target))
                                        }
                                        draggedIndex = null
                                        dragOffset = Offset.Zero
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

/** Where a key dragged from [from] by [offset] lands, in the column-major grid. */
private fun targetIndex(
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

private fun <T> moveItem(list: List<T>, from: Int, to: Int): List<T> {
    val mutable = list.toMutableList()
    val item = mutable.removeAt(from)
    mutable.add(to.coerceIn(0, mutable.size), item)
    return mutable
}

@Composable
private fun EditableKeyCell(
    key: CustomKey,
    dragging: Boolean,
    dragOffset: Offset,
    armed: Boolean,
    onTap: () -> Unit,
    onDoubleTap: () -> Unit,
    onConfirmDelete: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .zIndex(if (dragging) 1f else 0f)
            .graphicsLayer {
                translationX = dragOffset.x
                translationY = dragOffset.y
                val s = if (dragging) 1.06f else 1f
                scaleX = s
                scaleY = s
            }
            .clip(Ui.smallShape)
            .background(
                if (dragging) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
            )
            // Long-press to pick up and drag; a quick tap or double-tap is handled below.
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
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = { onDoubleTap() },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = key.label.ifEmpty { stringResource(R.string.settings_keys_unnamed) },
            style = MaterialTheme.typography.labelLarge.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
        )
        if (armed) {
            // The red cross: tap it to confirm removal; tapping the cell body disarms.
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error)
                    .clickable { onConfirmDelete() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.keys_remove),
                    tint = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}
