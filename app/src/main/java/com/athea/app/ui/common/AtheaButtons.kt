package com.athea.app.ui.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import com.athea.app.ui.liquid.LocalLiquidBackdrop
import com.athea.app.ui.liquid.liquidGlass
import com.athea.app.ui.theme.Ui

/** Half the control height: the lens hugs a fully round outline. */
private val ROUND_RADIUS = Ui.topButtonSize * 0.5f

/**
 * The one floating round button (TopBar drawer toggle, scaffold back, ...):
 * a circle of [Ui.topButtonSize] that refracts whatever is behind it.
 */
@Composable
fun AtheaRoundButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    Surface(
        shape = CircleShape,
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.liquidGlass(LocalLiquidBackdrop.current, CircleShape, ROUND_RADIUS),
    ) {
        Box(
            Modifier
                .size(Ui.topButtonSize)
                .clip(CircleShape)
                .background(pressWash(source))
                .clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

/**
 * The one merged pill hosting a row of icon buttons (TopBar right,
 * drawer bottom). Callers only place [AtheaPillIcon]s inside.
 */
@Composable
fun AtheaPill(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Surface(
        shape = Ui.pillShape,
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.liquidGlass(LocalLiquidBackdrop.current, Ui.pillShape, ROUND_RADIUS),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, content = content)
    }
}

/**
 * The one icon button for pills. Its only press feedback is the wash, on the same circle every
 * time: no material ripple is drawn on top of it, so the press looks the same everywhere.
 */
@Composable
fun AtheaPillIcon(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val source = remember { MutableInteractionSource() }
    Box(
        modifier
            .size(Ui.topButtonSize)
            .clip(CircleShape)
            .background(pressWash(source))
            .clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * Press feedback for the glass controls: a soft wash that fades in while the control is held
 * and out again on release.
 */
@Composable
fun pressWash(source: InteractionSource): Color {
    val pressed by source.collectIsPressedAsState()
    val alpha by animateFloatAsState(
        targetValue = if (pressed) Ui.pressWashAlpha else 0f,
        animationSpec = tween(durationMillis = 90),
        label = "pressWash",
    )
    return MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
}
