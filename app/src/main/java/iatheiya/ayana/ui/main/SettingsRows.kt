package iatheiya.ayana.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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


/** A card without a title: one entry, or a group of rows with a [SettingsDivider] between them. */
@Composable
internal fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = Ui.cardShape,
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
    /** A line under the title; left out where the title says enough. */
    subtitle: String? = null,
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
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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

/** A whole-number setting on a slider, its value shown at the right. */
@Composable
internal fun SliderRow(
    title: String,
    value: Int,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Int) -> Unit,
    enabled: Boolean = true,
) = SliderRow(
    title = title,
    value = value.toFloat(),
    valueText = "$value",
    range = range,
    onChange = { onChange(it.toInt()) },
    reason = null,
    enabled = enabled,
)

/**
 * A setting on a slider: its name, its value at the right, and the slider under them. With a
 * [reason] the row is greyed out and says why the setting does nothing right now.
 */
@Composable
internal fun SliderRow(
    title: String,
    value: Float,
    valueText: String,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
    reason: String? = null,
    enabled: Boolean = reason == null,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Ui.spaceL, vertical = Ui.spaceS),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .alpha(if (enabled) 1f else Ui.ghostAlpha),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (reason != null) {
            Text(
                text = reason,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(
            value = value,
            onValueChange = onChange,
            enabled = enabled,
            valueRange = range,
            modifier = Modifier.alpha(if (enabled) 1f else Ui.ghostAlpha),
        )
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
            .padding(Ui.spaceL),
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

/**
 * A setting with three choices on one switch: a wider track whose thumb rests at the left, in the
 * middle or at the right, with the chosen choice's name under it. A tap on a third of the track
 * picks that choice; the thumb can also be dragged and settles where it is let go. Left is the
 * off position: the track is lit with the accent only while the thumb is off it.
 */
@Composable
internal fun <T> ThreeWaySwitchRow(
    title: String,
    subtitle: String? = null,
    /** The three choices, left to right, with their names. */
    choices: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    require(choices.size == 3)
    val index = choices.indexOfFirst { it.first == selected }.coerceAtLeast(0)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Ui.spaceL, vertical = Ui.spaceM),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            Modifier
                .weight(1f)
                .padding(end = Ui.controlGap),
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            ThreeWaySwitch(index = index, onIndex = { onSelect(choices[it].first) })
            Text(
                text = choices[index].second,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Ui.spaceXs),
            )
        }
    }
}

@Composable
private fun ThreeWaySwitch(index: Int, onIndex: (Int) -> Unit) {
    val density = LocalDensity.current
    val accent = AccentColor.shown ?: MaterialTheme.colorScheme.primary
    val travel = with(density) { (TRI_TRACK_W - TRI_TRACK_H).toPx() }
    // Where the thumb is, 0 (left) to 2 (right). The finger moves it directly; a choice glides it.
    val position = remember { Animatable(index.toFloat()) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(index) { if (!position.isRunning) position.animateTo(index.toFloat(), TRI_GLIDE) }
    val lit = position.value > 0.5f
    val track by animateColorAsState(if (lit) accent else MaterialTheme.colorScheme.outline, label = "triTrack")
    val thumb by animateColorAsState(
        if (lit) contentOn(accent) else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "triThumb",
    )
    val thumbSize by animateDpAsState(if (lit) TRI_THUMB_ON else TRI_THUMB_OFF, label = "triThumbSize")
    fun settle(to: Int) {
        scope.launch { position.animateTo(to.toFloat(), TRI_GLIDE) }
        if (to != index) onIndex(to)
    }
    Box(
        Modifier
            .size(TRI_TRACK_W, TRI_TRACK_H)
            .clip(CircleShape)
            .background(track)
            .pointerInput(index) {
                detectTapGestures { at -> settle((at.x / size.width * 3).toInt().coerceIn(0, 2)) }
            }
            .pointerInput(index) {
                detectHorizontalDragGestures(
                    onDragEnd = { settle(position.value.roundToInt().coerceIn(0, 2)) },
                    onDragCancel = { settle(index) },
                ) { change, dx ->
                    change.consume()
                    scope.launch { position.snapTo((position.value + dx / travel * 2).coerceIn(0f, 2f)) }
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .offset { IntOffset((position.value / 2 * travel).roundToInt(), 0) }
                .size(TRI_TRACK_H),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(thumbSize)
                    .clip(CircleShape)
                    .background(thumb),
            )
        }
    }
}

private val TRI_TRACK_W = 84.dp
private val TRI_TRACK_H = 32.dp
private val TRI_THUMB_ON = 24.dp
private val TRI_THUMB_OFF = 16.dp
private val TRI_GLIDE = spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
