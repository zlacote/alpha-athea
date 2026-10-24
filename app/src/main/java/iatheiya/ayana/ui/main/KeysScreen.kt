package iatheiya.ayana.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
 * The key grid in its own room. Everything scrolls, so the on/off switch at the top
 * moves out of the way when the keyboard is up. While the grid is off, the controls,
 * the input and the preview below the switch are greyed out and inert.
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
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var buffer by remember { mutableStateOf("") }
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
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            AyanaScrollHeader(stringResource(R.string.keys_title))
            SettingsCard {
                SwitchRow(
                    title = stringResource(R.string.settings_key_row),
                    subtitle = stringResource(R.string.settings_key_row_desc),
                    checked = keyRowVisible,
                    onChange = onKeyRowVisibleChange,
                )
            }

            Column(Modifier.fillMaxWidth().then(locked)) {
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

                // One add area: type the sequence by hand or tap the keyboard below it. Without
                // "send now" the key drops its text into the composer; with it, it runs at once.
                OutlinedTextField(
                    value = buffer,
                    onValueChange = { buffer = it },
                    label = { Text(stringResource(R.string.settings_keys_payload)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    FilterChip(
                        selected = sendNow,
                        onClick = { sendNow = !sendNow },
                        label = { Text(stringResource(R.string.keys_send_now)) },
                    )
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = {
                            val key = if (sendNow) {
                                CustomKey(label = visibleSequence(buffer).take(12), payload = buffer + "\r", kind = KeyKind.SEND)
                            } else {
                                CustomKey(label = visibleSequence(buffer).take(12), payload = buffer, kind = KeyKind.INSERT)
                            }
                            onKeysChange(keys + key)
                            buffer = ""
                        },
                        enabled = buffer.isNotEmpty(),
                    ) {
                        Text(stringResource(R.string.keys_add))
                    }
                }
                KeyBuilderPad(
                    onAppend = { buffer += it },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                )
            }

            // Default button at the very end, centred and bare, like the Glass screen.
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
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
