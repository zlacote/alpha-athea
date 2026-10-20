package com.athea.app.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.athea.app.R
import com.athea.app.data.CustomKey
import com.athea.app.data.KeyKind
import com.athea.app.ui.common.AtheaScaffold
import com.athea.app.ui.common.AtheaScrollHeader
import com.athea.app.util.isCtrlCombinable
import com.athea.app.util.toControlChar

/** What a key of the builder does when pressed. */
private sealed interface BuilderAction {
    /** Types [raw]; with CTRL armed, a single letter becomes its control character. */
    data class Type(val raw: String) : BuilderAction

    data object Ctrl : BuilderAction

    data object Backspace : BuilderAction
}

/** One key cap. [weight] is its width in units of a row of [GRID_UNITS]. */
private data class BuilderKey(
    val label: String,
    val action: BuilderAction,
    val weight: Float = 1f,
)

private const val GRID_UNITS = 10f

private fun typed(label: String, raw: String = label) = BuilderKey(label, BuilderAction.Type(raw))

/** One character per key, as a phone keyboard lays letters out. */
private fun chars(row: String): List<BuilderKey> = row.map { typed(it.toString()) }

/**
 * The keyboard, row by row, laid out like a phone keyboard on a grid of ten units.
 * Shorter rows are centred. No key appears twice. Function keys and the
 * navigation block follow the xterm sequences.
 */
private val BUILDER_ROWS: List<List<BuilderKey>> = listOf(
    listOf(
        typed("ESC", "\u001B"), typed("TAB", "\t"),
        typed("F1", "\u001BOP"), typed("F2", "\u001BOQ"), typed("F3", "\u001BOR"), typed("F4", "\u001BOS"),
        typed("F5", "\u001B[15~"), typed("F6", "\u001B[17~"),
    ),
    listOf(
        typed("F7", "\u001B[18~"), typed("F8", "\u001B[19~"), typed("F9", "\u001B[20~"), typed("F10", "\u001B[21~"),
        typed("F11", "\u001B[23~"), typed("F12", "\u001B[24~"), typed("INS", "\u001B[2~"), typed("DEL", "\u001B[3~"),
    ),
    chars("1234567890"),
    chars("qwertyuiop"),
    chars("asdfghjkl"),
    listOf(BuilderKey("CTRL", BuilderAction.Ctrl)) + chars("zxcvbnm") +
        listOf(BuilderKey("⌫", BuilderAction.Backspace)),
    chars("-=[]\\;:'\"/"),
    chars("`~|\$&*()!?"),
    chars("@#%^+_{}<>"),
    listOf(
        typed("HOME", "\u001B[H"), typed("END", "\u001B[F"), typed("PGUP", "\u001B[5~"), typed("PGDN", "\u001B[6~"),
        typed("↑", "\u001B[A"), typed("↓", "\u001B[B"), typed("←", "\u001B[D"), typed("→", "\u001B[C"),
    ),
    listOf(
        typed(","),
        typed("."),
        BuilderKey("SP", BuilderAction.Type(" "), weight = 6f),
        BuilderKey("⏎", BuilderAction.Type("\r"), weight = 2f),
    ),
)

/**
 * Visual key builder: tap keys on a keyboard to build a sequence. CTRL arms a
 * control combination for the next letter, ⏎ puts a real Enter into the sequence.
 * Save turns the sequence into a button of the key row.
 */
@Composable
fun KeyBuilderScreen(
    onAdd: (CustomKey) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var buffer by remember { mutableStateOf("") }
    var ctrlArmed by remember { mutableStateOf(false) }

    fun press(action: BuilderAction) {
        when (action) {
            BuilderAction.Ctrl -> ctrlArmed = !ctrlArmed
            BuilderAction.Backspace -> buffer = buffer.dropLast(1)
            is BuilderAction.Type -> {
                val raw = action.raw
                buffer += if (ctrlArmed && raw.length == 1 && raw[0].isCtrlCombinable()) {
                    ctrlArmed = false
                    raw[0].toControlChar().toString()
                } else {
                    raw
                }
            }
        }
    }

    AtheaScaffold(onBack = onBack, modifier = modifier) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 8.dp),
        ) {
            AtheaScrollHeader(stringResource(R.string.builder_title))

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
            ) {
                Text(
                    text = if (buffer.isEmpty()) stringResource(R.string.builder_hint) else visible(buffer),
                    style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                    color = if (buffer.isEmpty()) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { buffer = "" }, enabled = buffer.isNotEmpty()) {
                    Text(stringResource(R.string.builder_clear))
                }
            }

            BUILDER_ROWS.forEach { keys ->
                KeyLine(
                    keys = keys,
                    ctrlArmed = ctrlArmed,
                    onPress = { press(it) },
                )
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    onAdd(CustomKey(label = visible(buffer).take(12), payload = buffer, kind = KeyKind.SEND))
                    onBack()
                },
                enabled = buffer.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
            ) {
                Text(stringResource(R.string.builder_save))
            }
        }
    }
}

/** One row of the keyboard, centred on the grid. */
@Composable
private fun KeyLine(
    keys: List<BuilderKey>,
    ctrlArmed: Boolean,
    onPress: (BuilderAction) -> Unit,
) {
    val used = keys.fold(0f) { sum, key -> sum + key.weight }
    val margin = (GRID_UNITS - used) / 2f
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
    ) {
        if (margin > 0f) Spacer(Modifier.weight(margin))
        keys.forEach { key ->
            KeyCap(
                key = key,
                selected = key.action == BuilderAction.Ctrl && ctrlArmed,
                onClick = { onPress(key.action) },
            )
        }
        if (margin > 0f) Spacer(Modifier.weight(margin))
    }
}

@Composable
private fun RowScope.KeyCap(
    key: BuilderKey,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        modifier = Modifier
            .weight(key.weight)
            .padding(horizontal = 2.dp)
            .height(44.dp),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = key.label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = if (key.label.length > 2) 10.sp else 13.sp,
                ),
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )
        }
    }
}

/** The sequence with control characters drawn as symbols, so it reads as typed. */
private fun visible(text: String): String = text
    .replace("\u001B", "␛")
    .replace("\t", "⇥")
    .replace("\r", "⏎")
