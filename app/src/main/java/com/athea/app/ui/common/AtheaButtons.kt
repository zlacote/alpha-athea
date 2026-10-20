package com.athea.app.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.liquidGlass(LocalLiquidBackdrop.current, CircleShape, ROUND_RADIUS),
    ) {
        Box(Modifier.size(Ui.topButtonSize), contentAlignment = Alignment.Center) {
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

/** The one icon button for pills. */
@Composable
fun AtheaPillIcon(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}
