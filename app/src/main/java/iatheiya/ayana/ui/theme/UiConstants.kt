package iatheiya.ayana.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Single source of truth for shared UI dimensions and shapes.
 * Every "similar" element references the same constant — no duplicates
 * that can drift apart.
 */
object Ui {
    // ---- Shapes -----------------------------------------------------------
    val menuRadius = 24.dp
    val menuShape = RoundedCornerShape(menuRadius)
    val bubbleShape = RoundedCornerShape(22.dp)
    val panelRadius = 28.dp
    val panelShape = RoundedCornerShape(panelRadius)
    val attachmentShape = RoundedCornerShape(14.dp)
    val dialogShape = RoundedCornerShape(20.dp)
    val pillShape = RoundedCornerShape(50.dp)
    val smallShape = RoundedCornerShape(8.dp)

    // ---- Menus ------------------------------------------------------------
    val menuMinWidth = 190.dp
    val menuItemMinHeight = 56.dp
    val menuIconSize = 24.dp
    val menuElevation = 3.dp

    // ---- Top bar ----------------------------------------------------------
    val topButtonSize = 48.dp
    val topBarHorizontalPadding = 10.dp
    val topBarTopPadding = 14.dp
    val topBarBottomPadding = 6.dp
    val topBarScrimHeight = 30.dp
    val topBarContentTop = 62.dp

    /** Top of the menu under the top-right pill: the pill's bottom edge plus a gap. */

    // ---- Composer ---------------------------------------------------------
    val composerButtonSize = 38.dp
    val composerButtonSpace = 52.dp
    val composerButtonRowHeight = 44.dp
    val composerFieldEndGrown = 8.dp
    val composerFieldTopGrown = 10.dp

    // ---- Key row ----------------------------------------------------------
    val keyMinHeight = 44.dp
    val keyPaddingH = 8.dp
    val keyVisibleCount = 7
    val keySeparatorWidth = 1.dp
    val keyRowPaddingH = 2.dp
    val keyRowPaddingV = 2.dp

    // ---- Bubbles ----------------------------------------------------------
    val bubblePaddingH = 16.dp
    val bubblePaddingTop = 12.dp
    val bubblePaddingBottom = 14.dp
    val bubbleMaxWidthFraction = 0.85f
    val outputPaddingH = 12.dp

    // ---- Drawer -----------------------------------------------------------
    val drawerWidthFraction = 0.72f

    // ---- Common paddings --------------------------------------------------
    val screenPadding = 16.dp
    val headerPaddingH = 8.dp
    val headerPaddingV = 4.dp
    val contentPaddingV = 8.dp
    val dividerPadding = 8.dp

    // ---- Fades & chevrons -------------------------------------------------
    val fadeHeight = 24.dp
    val tailFadeHeight = 26.dp
    val chevronExpandSize = 20.dp
    val chevronCollapseSize = 16.dp
    val spacerMedium = 12.dp

    // ---- Misc -------------------------------------------------------------
    val scrollbarButtonSize = 36.dp

    // ---- Alphas -----------------------------------------------------------
    const val highlightAlpha = 0.08f
    const val pressWashAlpha = 0.12f
    const val glowFadeMs = 500
    const val drawerDarkenAlpha = 0.45f
    const val virtualizedBgAlpha = 0.06f
    const val chevronAlpha = 0.5f
    const val chevronAlphaCollapsedPreview = 0.6f
    const val ghostAlpha = 0.45f
    const val dividerAlpha = 0.28f
    const val drawerScrimA = 0.28f
    const val drawerScrimB = 0.62f
    const val drawerScrimC = 0.92f
    const val dotAlpha = 0.85f

    // ---- Virtualization ---------------------------------------------------
    const val virtualizedMaxFraction = 0.5f
    const val chunkSize = 4000
    const val chunkSlack = 400
    const val hugeCharsThreshold = 8000
    const val hugeLinesThreshold = 20 // TAIL_LINES 5 * 4

    // ---- Throttle / caps --------------------------------------------------
}
