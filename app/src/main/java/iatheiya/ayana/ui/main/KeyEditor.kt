package iatheiya.ayana.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import iatheiya.ayana.R
import iatheiya.ayana.data.CustomKey
import iatheiya.ayana.data.KeyKind
import iatheiya.ayana.ui.common.AyanaChoiceSheet
import iatheiya.ayana.ui.common.AyanaDialogContainer
import iatheiya.ayana.ui.common.SheetChoice
import iatheiya.ayana.util.escapeKeySequence
import iatheiya.ayana.util.isPlainText
import iatheiya.ayana.util.unescapeKeySequence

/** Where the key codes are listed: the terminal input sequences of the ANSI escape code article. */
private const val KEY_CODES_URL = "https://en.wikipedia.org/wiki/ANSI_escape_code#Terminal_input_sequences"

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

/**
 * A key's bytes from what the user typed: escapes become control characters, and a plain command
 * that is sent at once gets its Enter. A key sequence (Home, an arrow...) is sent exactly as typed.
 */
internal fun keyPayload(typed: String, mode: KeyMode): String {
    val raw = unescapeKeySequence(typed)
    // Only "send now" runs a command; a sticky or lock key is a prefix and must never carry Enter.
    val runsAsCommand = mode == KeyMode.SEND && raw.isPlainText() && raw.isNotEmpty()
    return if (runsAsCommand) raw + "\r" else raw
}

/** The typed form of a stored payload, for editing: the Enter a command got is not shown. */
internal fun typedPayload(key: CustomKey): String {
    val withoutEnter = if (key.kind == KeyKind.SEND && key.payload.endsWith("\r") &&
        key.payload.dropLast(1).isPlainText()
    ) {
        key.payload.dropLast(1)
    } else {
        key.payload
    }
    return escapeKeySequence(withoutEnter)
}

/** A short, readable label for a sequence: control characters as symbols. */
internal fun visibleSequence(text: String): String = text
    .replace("\u001B", "␛")
    .replace("\t", "⇥")
    .replace("\r", "⏎")
    .replace("\u007F", "⌫")

/**
 * The key's action, as an outlined field like the others. Tapping it raises the four actions from
 * the bottom, the same chooser every option in the app uses.
 */
@Composable
internal fun KeyModeField(
    mode: KeyMode,
    onModeChange: (KeyMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    var choosing by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedTextField(
            value = keyModeLabel(mode),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(stringResource(R.string.keys_mode)) },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
        )
        // A read-only field still takes the tap for its caret; this cover opens the chooser instead.
        Box(
            Modifier
                .matchParentSize()
                .padding(top = FIELD_LABEL_ROOM)
                .clip(MaterialTheme.shapes.extraSmall)
                .clickable { choosing = true },
        )
    }
    if (choosing) {
        AyanaChoiceSheet(
            title = stringResource(R.string.keys_mode),
            choices = KeyMode.entries.map { SheetChoice(it, keyModeLabel(it)) },
            selected = mode,
            onPick = onModeChange,
            onDismiss = { choosing = false },
        )
    }
}

/** The outlined field's box starts below the room its floating label takes. */
private val FIELD_LABEL_ROOM = 8.dp

/** How to type special keys, and where to look up any other key's code. */
@Composable
internal fun KeyEscapeHelp(modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    Column(modifier) {
        Text(
            text = stringResource(R.string.settings_keys_esc_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(
            onClick = { uriHandler.openUri(KEY_CODES_URL) },
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 0.dp, vertical = 4.dp),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.keys_reference), style = MaterialTheme.typography.labelLarge)
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
    var payload by remember { mutableStateOf(typedPayload(initial)) }
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
                KeyModeField(
                    mode = mode,
                    onModeChange = { mode = it },
                    modifier = Modifier.padding(top = 4.dp),
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
                        initial.copy(label = label.trim(), payload = keyPayload(payload, mode)).withMode(mode)
                    )
                }) {
                    Text(stringResource(R.string.dialog_save))
                }
            }
        },
    )
}
