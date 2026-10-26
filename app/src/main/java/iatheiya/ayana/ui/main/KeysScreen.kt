package iatheiya.ayana.ui.main

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.SolidColor
import iatheiya.ayana.ui.theme.messageStyle
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import iatheiya.ayana.R
import iatheiya.ayana.data.CustomKey
import iatheiya.ayana.util.unescapeKeySequence
import iatheiya.ayana.ui.common.AyanaScaffold
import iatheiya.ayana.ui.common.AyanaScrollHeader
import iatheiya.ayana.ui.liquid.liquidGlass
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
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var buffer by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(KeyMode.SEND) }
    var editIndex by remember { mutableStateOf<Int?>(null) }
    val ghost = remember { KeyDragGhost() }
    val locked = if (keyRowVisible) {
        Modifier
    } else {
        Modifier.alpha(Ui.ghostAlpha).pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) awaitPointerEvent().changes.forEach { it.consume() }
            }
        }
    }

    AyanaScaffold(
        onBack = onBack,
        modifier = modifier,
        overlay = { KeyDragGhostOverlay(ghost) },
    ) {
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

                Text(
                    text = stringResource(R.string.keys_edit_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Ui.pageTextInset, vertical = Ui.spaceXs),
                )
                // The editable grid is drawn as the live row itself: the same material (glass, or
                // the solid tone with glass off, with the chosen colour), the same labels and
                // separators, across the full width.
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = Ui.sectionGap)
                        .liquidGlass(null, RectangleShape, 0.dp, lit = false, flat = true),
                ) {
                    KeyGridEditor(
                        keys = keys,
                        rows = keyRows,
                        cols = keyCols,
                        ghost = ghost,
                        onKeysChange = onKeysChange,
                        onRename = { editIndex = it },
                    )
                }

                // One add block, built like the composer: the sequence is typed in a field with
                // the send button inside it (it grows with the text), and under a dividing line
                // the action is chosen from the same sheet every option uses.
                KeyAddBlock(
                    text = buffer,
                    onTextChange = { buffer = it },
                    mode = mode,
                    onModeChange = { mode = it },
                    onAdd = {
                        val key = CustomKey(
                            label = visibleSequence(unescapeKeySequence(buffer)).take(12),
                            payload = keyPayload(buffer, mode),
                        ).withMode(mode)
                        onKeysChange(keys + key)
                        buffer = ""
                    },
                )
                KeyEscapeHelp(Modifier.padding(horizontal = Ui.pageTextInset, vertical = Ui.spaceS))
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

private val KEY_BLOCK_RADIUS = Ui.cardRadius

private val KeyFieldShape = RoundedCornerShape(topStart = KEY_BLOCK_RADIUS, topEnd = KEY_BLOCK_RADIUS)
private val KeyActionShape = RoundedCornerShape(bottomStart = KEY_BLOCK_RADIUS, bottomEnd = KEY_BLOCK_RADIUS)

@Composable
private fun KeyAddBlock(
    text: String,
    onTextChange: (String) -> Unit,
    mode: KeyMode,
    onModeChange: (KeyMode) -> Unit,
    onAdd: () -> Unit,
) {
    var choosing by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Ui.screenGutter, vertical = Ui.sectionGap),
    ) {
        // The field: rounded on top like a card, in the main composer's material.
        Box(
            Modifier
                .fillMaxWidth()
                .liquidGlass(null, KeyFieldShape, KEY_BLOCK_RADIUS)
                .padding(start = Ui.spaceL, end = Ui.sendButtonInset, top = Ui.spaceXs, bottom = Ui.spaceXs),
        ) {
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                textStyle = messageStyle().copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                maxLines = 6,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = Ui.composerButtonSize + Ui.spaceXs)
                    .heightIn(min = Ui.composerFieldMinHeight)
                    .align(Alignment.CenterStart),
                decorationBox = { inner ->
                    Box(Modifier.heightIn(min = Ui.composerFieldMinHeight), contentAlignment = Alignment.CenterStart) {
                        if (text.isEmpty()) {
                            Text(
                                stringResource(R.string.settings_keys_payload),
                                style = messageStyle(),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        inner()
                    }
                },
            )
            SendCircleButton(
                enabled = text.isNotEmpty(),
                onClick = onAdd,
                // Centred on a one-line field; on a taller one it stays by the last line.
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = (Ui.composerFieldMinHeight - Ui.composerButtonSize) / 2),
            )
        }
        SettingsDivider()
        // The action: rounded at the bottom, with the chevron that opens the chooser.
        Surface(shape = KeyActionShape, color = MaterialTheme.colorScheme.surfaceContainer) {
            ChoiceSettingRow(
                title = stringResource(R.string.keys_mode),
                value = keyModeLabel(mode),
                onClick = { choosing = true },
            )
        }
    }
    if (choosing) KeyModeSheet(mode = mode, onPick = onModeChange, onDismiss = { choosing = false })
}
