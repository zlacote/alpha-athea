package com.athea.app.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.athea.app.R
import com.athea.app.data.CustomKey
import com.athea.app.data.KeyKind
import com.athea.app.ui.common.AtheaDialogContainer

/** A titled group of rows, drawn as one rounded card. */
@Composable
internal fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(vertical = 6.dp), content = content)
        }
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
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
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
            valueRange = range,
        )
    }
}

@Composable
internal fun ActionRow(
    title: String,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
internal fun KeyEditRow(
    key: CustomKey,
    onEdit: (CustomKey) -> Unit,
    onDelete: () -> Unit,
) {
    var editing by remember { mutableStateOf(false) }

    Row(
        Modifier
            .fillMaxWidth()
            .clickable { editing = true }
            .padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = key.label.ifEmpty { stringResource(R.string.settings_keys_unnamed) },
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = key.payload,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.Delete,
                contentDescription = stringResource(R.string.menu_delete),
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }

    if (editing) {
        KeyEditorDialog(
            initial = key,
            onDismiss = { editing = false },
            onSave = {
                editing = false
                onEdit(it)
            },
        )
    }
}

@Composable
private fun KeyEditorDialog(
    initial: CustomKey,
    onDismiss: () -> Unit,
    onSave: (CustomKey) -> Unit,
) {
    var label by remember { mutableStateOf(initial.label) }
    var payload by remember { mutableStateOf(initial.payload) }
    var kind by remember { mutableStateOf(initial.kind) }

    AtheaDialogContainer(
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
                    text = stringResource(R.string.settings_keys_kind),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
                )
                Row {
                    listOf(
                        KeyKind.SEND to stringResource(R.string.settings_keys_kind_send),
                        KeyKind.INSERT to stringResource(R.string.settings_keys_kind_insert),
                        KeyKind.CTRL to stringResource(R.string.settings_keys_kind_ctrl),
                    ).forEach { (candidate, text) ->
                        FilterChip(
                            selected = kind == candidate,
                            onClick = { kind = candidate },
                            label = { Text(text) },
                            modifier = Modifier.padding(end = 6.dp),
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.settings_keys_esc_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        },
        confirm = {
            TextButton(onClick = {
                onSave(
                    initial.copy(
                        label = label.trim(),
                        payload = payload,
                        kind = kind,
                    )
                )
            }) {
                Text(stringResource(R.string.dialog_save))
            }
        },
    )
}
