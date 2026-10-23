package com.athea.app.ui.main

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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import com.athea.app.R
import com.athea.app.data.CustomKey
import com.athea.app.data.KeyKind
import com.athea.app.ui.common.AtheaScaffold
import com.athea.app.ui.common.AtheaScrollHeader
import com.athea.app.ui.liquid.LocalLiquidBackdrop
import com.athea.app.ui.theme.Ui

/**
 * The key row in its own room. The switch stays on top, so the row can always be turned back on.
 * While the row is off, the live row, the input and the list below the switch cannot be used.
 */
@Composable
fun KeysScreen(
    keys: List<CustomKey>,
    keyRowVisible: Boolean,
    onKeyRowVisibleChange: (Boolean) -> Unit,
    onKeysChange: (List<CustomKey>) -> Unit,
    onResetKeys: () -> Unit,
    onOpenKeyBuilder: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var draft by remember { mutableStateOf("") }
    var sendNow by remember { mutableStateOf(false) }
    val locked = if (keyRowVisible) {
        Modifier
    } else {
        Modifier.alpha(Ui.ghostAlpha).pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) awaitPointerEvent().changes.forEach { it.consume() }
            }
        }
    }

    AtheaScaffold(onBack = onBack, modifier = modifier) {
        AtheaScrollHeader(stringResource(R.string.keys_title))
        SettingsCard {
            SwitchRow(
                title = stringResource(R.string.settings_key_row),
                subtitle = stringResource(R.string.settings_key_row_desc),
                checked = keyRowVisible,
                onChange = onKeyRowVisibleChange,
            )
        }

        Column(Modifier.fillMaxWidth().then(locked)) {
            // The live row shows the glass as plain glass: it must not sample this screen.
            CompositionLocalProvider(LocalLiquidBackdrop provides null) {
                KeyRow(
                    keys = keys.map { it.toTerminalKey() },
                    stickyCtrl = false,
                    suggestionActive = false,
                    onInsert = {},
                    onSendBytes = {},
                    onCursor = {},
                    onAcceptSuggestion = {},
                    onToggleStickyCtrl = {},
                    onConsumeStickyCtrl = {},
                )
            }

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
                        // Without "send now" the text lands in the composer, where it is visible.
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
        }

        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .then(locked)
                .verticalScroll(rememberScrollState()),
        ) {
            SettingsSection(title = stringResource(R.string.keys_list_title)) {
                keys.forEach { key ->
                    KeyEditRow(
                        key = key,
                        onEdit = { edited ->
                            onKeysChange(keys.map { if (it == key) edited else it })
                        },
                        onDelete = {
                            onKeysChange(keys.filterNot { it == key })
                        },
                    )
                }
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
}
