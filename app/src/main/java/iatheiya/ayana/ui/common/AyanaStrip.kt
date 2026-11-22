package iatheiya.ayana.ui.common

import kotlin.math.roundToInt
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.geometry.Rect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A horizontally scrolling grid of equal-width cells with thin separators between
 * columns.
 *
 * The one "one-shape" place for the key grid: the cell width is derived from the
 * measured viewport, so exactly [columnsVisible] columns fill it and extra columns
 * scroll. Cells fill column-major — each column holds [rows] items top to bottom —
 * so adding keys grows the grid to the right. With [rows] = 1 it is a plain strip.
 *
 * The viewport division is guarded: while the viewport is unknown or unbounded
 * (first layout pass, Infinity constraint) the grid degrades to zero-width cells
 * instead of propagating Infinity into the scroll state — that is what made the old
 * unguarded variant hang.
 */
@Composable
fun <T> EqualCellGrid(
    items: List<T>,
    rows: Int,
    columnsVisible: Int,
    cellHeight: Dp,
    separatorWidth: Dp,
    contentPaddingH: Dp,
    contentPaddingV: Dp,
    modifier: Modifier = Modifier,
    separator: @Composable () -> Unit,
    /** Drawn between the rows of a column, [separatorWidth] tall; none when null. */
    rowSeparator: (@Composable () -> Unit)? = null,
    /**
     * Empty room under the cells (the navigation bar's height, for a grid on the bottom edge).
     * The column separators run on through it, down to the very edge.
     */
    tail: Dp = 0.dp,
    cell: @Composable (T) -> Unit,
) {
    val scrollState = rememberScrollState()
    val rowCount = rows.coerceAtLeast(1)
    val cols = columnsVisible.coerceAtLeast(1)
    // Turned off its first screen, a swipe to the right is the grid's own way back, not the
    // drawer's: it says so for the place it covers while it is away from its start.
    val drawer = LocalAyanaDrawer.current
    val owner = remember { Any() }
    var bounds by remember { mutableStateOf(Rect.Zero) }
    val away = scrollState.value > 0
    LaunchedEffect(drawer, away, bounds) {
        if (drawer == null) return@LaunchedEffect
        if (away) drawer.swipeOwners[owner] = bounds else drawer.swipeOwners.remove(owner)
    }
    DisposableEffect(drawer) { onDispose { drawer?.swipeOwners?.remove(owner) } }
    BoxWithConstraints(modifier.fillMaxWidth().onGloballyPositioned { bounds = it.boundsInRoot() }) {
        // maxWidth.value is a plain Float member, and Float.isFinite() is a
        // stdlib function. Compose's Dp.isFinite is a top-level extension
        // property and would need its own import; the stdlib route needs
        // nothing and says exactly what is being checked.
        val width = maxWidth.value
        val viewport = if (width.isFinite() && width > 0f) maxWidth else 0.dp
        val fixed = contentPaddingH * 2 + separatorWidth * (cols - 1).coerceAtLeast(0)
        val cellWidth = if (viewport <= fixed) 0.dp else (viewport - fixed) / cols
        // Column-major: each column is a stack of [rowCount] cells.
        val columns = items.chunked(rowCount)
        val rowGaps = if (rowSeparator != null) separatorWidth * (rowCount - 1) else 0.dp
        val columnHeight = cellHeight * rowCount + rowGaps + tail
        // A swipe turns whole screens of [cols] columns, settling on the nearest one when the
        // finger lets go, as the pages of a keyboard do.
        val pageWidthPx = with(LocalDensity.current) { ((cellWidth + separatorWidth) * cols).toPx() }
        LaunchedEffect(scrollState, pageWidthPx) {
            if (pageWidthPx <= 0f) return@LaunchedEffect
            snapshotFlow { scrollState.isScrollInProgress }.collect { moving ->
                if (!moving) {
                    val page = (scrollState.value / pageWidthPx).roundToInt()
                    val target = (page * pageWidthPx).roundToInt().coerceIn(0, scrollState.maxValue)
                    if (target != scrollState.value) scrollState.animateScrollTo(target)
                }
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = contentPaddingH, vertical = contentPaddingV),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            columns.forEachIndexed { columnIndex, columnItems ->
                if (columnIndex > 0) {
                    // The grid owns separator geometry so callers only describe its
                    // appearance; it spans the full height of the column.
                    Box(
                        Modifier
                            .width(separatorWidth)
                            .height(columnHeight),
                        contentAlignment = Alignment.Center,
                    ) {
                        separator()
                    }
                }
                Column(Modifier.width(cellWidth)) {
                    for (rowIndex in 0 until rowCount) {
                        if (rowIndex > 0 && rowSeparator != null) {
                            Box(Modifier.width(cellWidth).height(separatorWidth)) { rowSeparator() }
                        }
                        Box(
                            Modifier
                                .width(cellWidth)
                                .height(cellHeight),
                            contentAlignment = Alignment.Center,
                        ) {
                            columnItems.getOrNull(rowIndex)?.let { cell(it) }
                        }
                    }
                    if (tail > 0.dp) Spacer(Modifier.height(tail))
                }
            }
        }
    }
}
