package iatheiya.ayana.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import iatheiya.ayana.ui.theme.Ui
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import iatheiya.ayana.R
import iatheiya.ayana.data.CustomKey
import iatheiya.ayana.data.KeyKind
import iatheiya.ayana.ui.common.AyanaDialogContainer

/** A card of its own, without a title: one entry or switch group drawn as one rounded block. */
@Composable
internal fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Column(Modifier.padding(vertical = 4.dp), content = content)
    }
}

@Composable
internal fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
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
            // Off-state thumb must stay visible on the dark track.
            colors = SwitchDefaults.colors(
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
            .padding(horizontal = 16.dp, vertical = 8.dp),
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
                .padding(start = 16.dp),
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
 * What a key does, as a single choice the user picks from one list — instead of a "kind" plus
 * separate sticky flags. Four behaviours: drop text into the editor, send at once, arm as a
 * one-shot modifier (sticky), or latch until pressed again (lock).
 */
internal enum class KeyMode { INSERT, SEND, STICKY, LOCK }

@Composable
internal fun keyModeLabel(mode: KeyMode): String = when (mode) {
    KeyMode.INSERT -> stringResource(R.string.settings_keys_kind_insert)
    KeyMode.SEND -> stringResource(R.string.settings_keys_kind_send)
    KeyMode.STICKY -> stringResource(R.string.keys_sticky)
    KeyMode.LOCK -> stringResource(R.string.keys_lock)
}

/** The mode a stored key represents, read back from its kind + sticky/lock flags. */
internal fun CustomKey.keyMode(): KeyMode = when {
    sticky && lock -> KeyMode.LOCK
    sticky -> KeyMode.STICKY
    kind == KeyKind.INSERT -> KeyMode.INSERT
    else -> KeyMode.SEND
}

/** The same key with its behaviour set to [mode]: the one place mode maps onto kind + flags. */
internal fun CustomKey.withMode(mode: KeyMode): CustomKey = when (mode) {
    KeyMode.INSERT -> copy(kind = KeyKind.INSERT, sticky = false, lock = false)
    KeyMode.SEND -> copy(kind = KeyKind.SEND, sticky = false, lock = false)
    KeyMode.STICKY -> copy(kind = KeyKind.SEND, sticky = true, lock = false)
    KeyMode.LOCK -> copy(kind = KeyKind.SEND, sticky = true, lock = true)
}

/** The one place a key's behaviour is chosen: tap the field, a list flows out, pick one. */
@Composable
internal fun KeyModeDropdown(
    mode: KeyMode,
    onModeChange: (KeyMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        Row(
            Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable { open = true }
                .padding(start = 14.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(keyModeLabel(mode), style = MaterialTheme.typography.bodyLarge)
            Icon(
                Icons.Filled.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            shape = Ui.menuShape,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            KeyMode.entries.forEach { candidate ->
                DropdownMenuItem(
                    text = { Text(keyModeLabel(candidate)) },
                    onClick = {
                        onModeChange(candidate)
                        open = false
                    },
                )
            }
        }
    }
}

@Composable
internal fun KeyEditorDialog(
    initial: CustomKey,
    onDismiss: () -> Unit,
    onSave: (CustomKey) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    var label by remember { mutableStateOf(initial.label) }
    var payload by remember { mutableStateOf(initial.payload) }
    var mode by remember { mutableStateOf(initial.keyMode()) }

    AyanaDialogContainer(
        title = stringResource(R.string.settings_keys_edit_title),
        onDismiss = onDismiss,
        content = {
            Column {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text(stringResource(R.string.settings_keys_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = payload,
                    onValueChange = { payload = it },
                    label = { Text(stringResource(R.string.settings_keys_payload)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = stringResource(R.string.keys_mode),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
                )
                KeyModeDropdown(mode = mode, onModeChange = { mode = it })
                Text(
                    text = stringResource(R.string.settings_keys_esc_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        },
        // The dialog lays out its own buttons so Delete can sit on the far left,
        // away from Cancel and Save on the right. The container's slots are emptied.
        dismiss = {},
        confirm = {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text(
                            stringResource(R.string.menu_delete),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.dialog_cancel))
                }
                TextButton(onClick = {
                    onSave(
                        initial.copy(label = label.trim(), payload = payload).withMode(mode)
                    )
                }) {
                    Text(stringResource(R.string.dialog_save))
                }
            }
        },
    )
}
