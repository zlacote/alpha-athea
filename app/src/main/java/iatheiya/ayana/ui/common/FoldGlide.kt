package iatheiya.ayana.ui.common

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntSize

/**
 * A change of height that glides only when made by hand ([byHand]: a fold or an opening the user
 * asked for) and snaps otherwise, so what Ayana folds by itself (the previous answer, as a new
 * command goes out), or a folded item first shown, never plays a fold. [onSettled] runs once the
 * change is done, to put [byHand] back.
 */
fun Modifier.foldGlide(byHand: Boolean, onSettled: () -> Unit): Modifier = animateContentSize(
    animationSpec = if (byHand) {
        spring(stiffness = Spring.StiffnessMediumLow, visibilityThreshold = IntSize.VisibilityThreshold)
    } else {
        snap()
    },
    finishedListener = { _, _ -> onSettled() },
)
