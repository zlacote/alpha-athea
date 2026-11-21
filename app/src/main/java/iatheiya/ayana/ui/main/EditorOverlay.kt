package iatheiya.ayana.ui.main

import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloseFullscreen
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import iatheiya.ayana.R
import iatheiya.ayana.ui.theme.Ui
import iatheiya.ayana.ui.theme.MessageStyle

/**
 * Full-screen editing surface, chat-app style: plain text over the whole canvas, a collapse
 * control top-right, send bottom-right. It opens with the cursor (or the selection) where it was
 * in the composer. Enter always starts a new line here, whatever it does in the composer: this
 * is where long commands are written.
 */
@Composable
fun EditorOverlay(
    draft: String,
    onDraftChange: (String) -> Unit,
    /** The composer's own cursor or selection, shared with it both ways. */
    selection: TextRange?,
    onSelectionChange: (TextRange) -> Unit,
    onSend: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        // The text runs the whole height and reaches the left edge. On the right a lane is kept
        // free, the whole height too: the collapse control at its top, the send button (the
        // composer's own, at its size) at its bottom.
        Box(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
        ) {
            val focus = remember { FocusRequester() }
            // Focused at once, so the keyboard is up and the cursor shows where it was.
            LaunchedEffect(Unit) { focus.requestFocus() }
            BasicTextField(
                value = TextFieldValue(
                    text = draft,
                    selection = selection?.let {
                        TextRange(it.start.coerceIn(0, draft.length), it.end.coerceIn(0, draft.length))
                    } ?: TextRange(draft.length),
                ),
                onValueChange = { value ->
                    if (value.text != draft) onDraftChange(value.text)
                    onSelectionChange(value.selection)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = Ui.spaceL, end = EDITOR_LANE, top = Ui.spaceS, bottom = Ui.spaceS)
                    .focusRequester(focus),
                textStyle = MessageStyle.copy(color = MaterialTheme.colorScheme.onBackground),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.None,
                ),
            )
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = Ui.sendButtonInset - Ui.spaceXs, top = Ui.spaceXs)
                    .size(Ui.composerButtonSize + Ui.spaceS),
            ) {
                Icon(
                    Icons.Default.CloseFullscreen,
                    contentDescription = stringResource(R.string.cd_collapse),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            SendCircleButton(
                enabled = draft.isNotBlank(),
                onClick = onSend,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = Ui.sendButtonInset, bottom = Ui.spaceM),
            )
        }
    }
}

/** The right-hand lane the text leaves free for the collapse and send buttons. */
private val EDITOR_LANE = Ui.sendButtonInset + Ui.composerButtonSize + Ui.spaceL
