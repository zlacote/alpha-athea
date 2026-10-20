package com.athea.app.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.athea.app.R
import com.athea.app.data.CustomKey
import com.athea.app.data.KeyKind
import com.athea.app.ui.common.AtheaDialogContainer
import com.athea.app.ui.common.AtheaScaffold
import com.athea.app.util.AtheaLog
import com.athea.app.util.copyToClipboard

@Composable
fun SettingsScreen(
    keyRowVisible: Boolean,
    enterSends: Boolean,
    autoScrollOnSend: Boolean,
    rawStream: Boolean,
    autocompleteEnabled: Boolean,
    pinchZoomEnabled: Boolean,
    virtualizeLargeOutput: Boolean,
    outputFontSizeSp: Int,
    previewLines: Int,
    bubbleFontSizeSp: Int,
    customKeys: List<CustomKey>,
    onKeyRowVisibleChange: (Boolean) -> Unit,
    onEnterSendsChange: (Boolean) -> Unit,
    onAutoScrollOnSendChange: (Boolean) -> Unit,
    onRawStreamChange: (Boolean) -> Unit,
    onAutocompleteEnabledChange: (Boolean) -> Unit,
    onPinchZoomEnabledChange: (Boolean) -> Unit,
    onVirtualizeLargeOutputChange: (Boolean) -> Unit,
    onOutputFontSizeChange: (Int) -> Unit,
    onPreviewLinesChange: (Int) -> Unit,
    onBubbleFontSizeChange: (Int) -> Unit,
    onCustomKeysChange: (List<CustomKey>) -> Unit,
    onResetKeys: () -> Unit,
    onOpenKeyBuilder: () -> Unit,
    onOpenGlassTuning: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AtheaScaffold(
        title = stringResource(R.string.settings),
        onBack = onBack,
        modifier = modifier,
    ) { headerInset ->
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(headerInset))

            SettingsSection(title = stringResource(R.string.settings_section_input)) {
                SwitchRow(
                    title = stringResource(R.string.settings_enter_sends),
                    subtitle = stringResource(R.string.settings_enter_sends_desc),
                    checked = enterSends,
                    onChange = onEnterSendsChange,
                )
                SwitchRow(
                    title = stringResource(R.string.settings_autocomplete),
                    subtitle = stringResource(R.string.settings_autocomplete_desc),
                    checked = autocompleteEnabled,
                    onChange = onAutocompleteEnabledChange,
                )
                SwitchRow(
                    title = stringResource(R.string.settings_auto_scroll),
                    subtitle = stringResource(R.string.settings_auto_scroll_desc),
                    checked = autoScrollOnSend,
                    onChange = onAutoScrollOnSendChange,
                )
            }

            SettingsSection(title = stringResource(R.string.settings_section_output)) {
                SwitchRow(
                    title = stringResource(R.string.settings_raw_stream),
                    subtitle = stringResource(R.string.settings_raw_stream_desc),
                    checked = rawStream,
                    onChange = onRawStreamChange,
                )
                SwitchRow(
                    title = stringResource(R.string.settings_pinch_zoom),
                    subtitle = stringResource(R.string.settings_pinch_zoom_desc),
                    checked = pinchZoomEnabled,
                    onChange = onPinchZoomEnabledChange,
                )
                SwitchRow(
                    title = stringResource(R.string.settings_virtualize),
                    subtitle = stringResource(R.string.settings_virtualize_desc),
                    checked = virtualizeLargeOutput,
                    onChange = onVirtualizeLargeOutputChange,
                )
                SliderRow(
                    title = stringResource(R.string.settings_font_size),
                    value = outputFontSizeSp,
                    range = 10f..22f,
                    onChange = onOutputFontSizeChange,
                )
                SliderRow(
                    title = stringResource(R.string.settings_preview_lines),
                    value = previewLines,
                    range = 1f..10f,
                    onChange = onPreviewLinesChange,
                )
                SliderRow(
                    title = stringResource(R.string.settings_bubble_size),
                    value = bubbleFontSizeSp,
                    range = 12f..24f,
                    onChange = onBubbleFontSizeChange,
                )
            }

            SettingsSection(title = stringResource(R.string.settings_keys_section)) {
                SwitchRow(
                    title = stringResource(R.string.settings_key_row),
                    subtitle = stringResource(R.string.settings_key_row_desc),
                    checked = keyRowVisible,
                    onChange = onKeyRowVisibleChange,
                )
                customKeys.forEach { key ->
                    KeyEditRow(
                        key = key,
                        onEdit = { edited ->
                            onCustomKeysChange(customKeys.map { if (it == key) edited else it })
                        },
                        onDelete = {
                            onCustomKeysChange(customKeys.filterNot { it == key })
                        },
                    )
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TextButton(onClick = {
                        onCustomKeysChange(
                            customKeys + CustomKey(label = "", payload = "", kind = KeyKind.SEND)
                        )
                    }) {
                        Text(stringResource(R.string.settings_keys_add))
                    }
                    TextButton(onClick = onResetKeys) {
                        Text(stringResource(R.string.settings_keys_reset))
                    }
                }
                ActionRow(
                    title = stringResource(R.string.builder_open),
                    onClick = onOpenKeyBuilder,
                )
            }

            SettingsSection(title = stringResource(R.string.glass_section)) {
                ActionRow(
                    title = stringResource(R.string.glass_open),
                    onClick = onOpenGlassTuning,
                )
            }

            SettingsSection(title = stringResource(R.string.settings_diag_section)) {
                val context = LocalContext.current
                ActionRow(title = stringResource(R.string.settings_diag_copy)) {
                    context.copyToClipboard(AtheaLog.dump(), "athea-logs")
                    android.widget.Toast.makeText(
                        context,
                        context.getString(R.string.settings_diag_copied),
                        android.widget.Toast.LENGTH_SHORT,
                    ).show()
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

/** A titled group of settings rows, drawn as one rounded card. */
@Composable
private fun SettingsSection(
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
private fun SliderRow(
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
private fun ActionRow(
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
private fun KeyEditRow(
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
                    ).forEach { (candidate, label) ->
                        FilterChip(
                            selected = kind == candidate,
                            onClick = { kind = candidate },
                            label = { Text(label) },
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

@Composable
private fun SwitchRow(
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
