package iatheiya.ayana.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import iatheiya.ayana.R
import iatheiya.ayana.data.CustomKey
import iatheiya.ayana.data.KeyKind
import iatheiya.ayana.ui.common.AyanaScaffold
import iatheiya.ayana.ui.common.AyanaScrollHeader
import iatheiya.ayana.ui.theme.Ui

/**
 * The key grid in its own room. The switch stays on top, so the grid can always be
 * turned back on. While the grid is off, the controls, the input and the preview
 * below the switch cannot be used.
 */
@Composable
fun KeysScreen(
    keys: List<CustomKey>,
    keyRowVisible: Boolean,
    keyRows: Int,
    keyCols: Int,
    onKeyRowVisibleChange: (Boolean) -> Unit,
    onKeyRowsChange: (Int) -> Unit,
    onKeyColsChange: (Int) -> Unit,
    onKeysChange: (List<CustomKey>) -> Unit,
    onResetKeys: () -> Unit,
    onOpenKeyBuilder: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var draft by remember { mutableStateOf("") }
    var sendNow by remember { mutableStateOf(false) }
    var editIndex by remember { mutableStateOf<Int?>(null) }
    val locked = if (keyRowVisible) {
        Modifier
    } else {
        Modifier.alpha(Ui.ghostAlpha).pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) awaitPointerEvent().changes.forEach { it.consume() }
            }
        }
    }

    AyanaScaffold(onBack = onBack, modifier = modifier) {
        AyanaScrollHeader(stringResource(R.string.keys_title))
        SettingsCard {
            SwitchRow(
                title = stringResource(R.string.settings_key_row),
                subtitle = stringResource(R.string.settings_key_row_desc),
                checked = keyRowVisible,
                onChange = onKeyRowVisibleChange,
            )
        }

        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .then(locked)
                .verticalScroll(rememberScrollState()),
        ) {
            SettingsCard {
                SliderRow(
                    title = stringResource(R.string.keys_rows),
                    value = keyRows,
                    range = 1f..4f,
                    onChange = onKeyRowsChange,
                )
                SettingsDivider()
                SliderRow(
                    title = stringResource(R.string.keys_cols),
                    value = keyCols,
                    range = 3f..10f,
                    onChange = onKeyColsChange,
                )
            }

            // The editable preview: the grid exactly as the composer shows it.
            Text(
                text = stringResource(R.string.keys_edit_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
            KeyGridEditor(
                keys = keys,
                rows = keyRows,
                cols = keyCols,
                onKeysChange = onKeysChange,
                onRename = { editIndex = it },
            )

            // Add a key: without "send now" the text lands in the composer, where it is visible.
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.settings_keys_payload)) },
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = {
                        val key = if (sendNow) {
                            CustomKey(label = draft.take(12), payload = draft + "\r", kind = KeyKind.SEND)
                        } else {
                            CustomKey(label = draft.take(12), payload = draft, kind = KeyKind.INSERT)
                        }
                        onKeysChange(keys + key)
                        draft = ""
                    },
                    enabled = draft.isNotEmpty(),
                ) {
                    Text(stringResource(R.string.keys_add))
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                FilterChip(
                    selected = sendNow,
                    onClick = { sendNow = !sendNow },
                    label = { Text(stringResource(R.string.keys_send_now)) },
                )
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(onClick = onOpenKeyBuilder) {
                    Text(stringResource(R.string.builder_open))
                }
                TextButton(onClick = onResetKeys) {
                    Text(stringResource(R.string.settings_keys_reset))
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    editIndex?.let { index ->
        val key = keys.getOrNull(index)
        if (key == null) {
            editIndex = null
        } else {
            KeyEditorDialog(
                initial = key,
                onDismiss = { editIndex = null },
                onSave = { edited ->
                    editIndex = null
                    onKeysChange(keys.mapIndexed { i, existing -> if (i == index) edited else existing })
                },
                onDelete = {
                    editIndex = null
                    onKeysChange(keys.filterIndexed { i, _ -> i != index })
                },
            )
        }
    }
}
