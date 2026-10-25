package iatheiya.ayana.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import iatheiya.ayana.ui.theme.AccentColor
import iatheiya.ayana.ui.theme.Ui
import iatheiya.ayana.ui.theme.contentOn
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp


/** A card of its own, without a title: one entry or switch group drawn as one rounded block. */
@Composable
internal fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Ui.screenGutter, vertical = Ui.sectionGap),
    ) {
        // No padding of its own: the first and last rows reach the rounded edges, so pressing
        // them lights the whole row, corners included (the rows carry their own padding).
        Column(content = content)
    }
}

@Composable
internal fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    /** Off greys the row out and ignores taps, for a setting that cannot apply right now. */
    enabled: Boolean = true,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onChange(!checked) }
            .padding(horizontal = Ui.spaceL, vertical = Ui.spaceM)
            .alpha(if (enabled) 1f else Ui.ghostAlpha),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The text always keeps clear of the switch: it wraps to the next line before it gets close.
        Column(
            Modifier
                .weight(1f)
                .padding(end = Ui.controlGap),
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            enabled = enabled,
            // Off-state thumb must stay visible on the dark track.
            // A switch that is on is lit with the accent colour, when one is chosen.
            colors = SwitchDefaults.colors(
                checkedTrackColor = AccentColor.shown ?: MaterialTheme.colorScheme.primary,
                checkedThumbColor = AccentColor.shown?.let(::contentOn) ?: MaterialTheme.colorScheme.onPrimary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.outline,
                uncheckedBorderColor = Color.Transparent,
            ),
        )
    }
}

@Composable
internal fun SliderRow(
    title: String,
    value: Int,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Int) -> Unit,
    enabled: Boolean = true,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .alpha(if (enabled) 1f else Ui.ghostAlpha),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "$value",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.toInt()) },
            enabled = enabled,
            valueRange = range,
        )
    }
}

/** A group of entries drawn as one rounded card; the caller puts a [SettingsDivider] between rows. */
@Composable
internal fun SettingsEntryGroup(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Ui.screenGutter, vertical = Ui.sectionGap),
    ) {
        Column(content = content)
    }
}

/**
 * One entry in a settings group: an icon on the left, a title, and a trailing
 * control. The trailing control defaults to a chevron (the entry opens its own
 * room); pass [trailing] to show something else, e.g. a copy action.
 */
@Composable
internal fun SettingsEntryRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit = {
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    },
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f)
                .padding(start = Ui.spaceL, end = Ui.controlGap),
        )
        trailing()
    }
}

/**
 * The split between two entries in the same group: a full-width band of the page
 * background, so the card reads as cut through to what is behind it rather than
 * drawn with a line.
 */
@Composable
internal fun SettingsDivider() {
    HorizontalDivider(
        thickness = 2.dp,
        color = MaterialTheme.colorScheme.background,
    )
}

/**
 * A setting chosen from a few options: its name, and its current value with a down arrow (the
 * options come up from the bottom, they do not open a page). Tapping it raises them (see [iatheiya.ayana.ui.common.AyanaChoiceSheet]).
 */
@Composable
internal fun ChoiceSettingRow(title: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Ui.spaceL, vertical = Ui.spaceL),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f)
                .padding(end = Ui.controlGap),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Icon(
            Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
