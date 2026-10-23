package com.athea.app.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.athea.app.R
import com.athea.app.session.SearchState
import com.athea.app.ui.theme.Ui
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.athea.app.ui.liquid.LiquidBackdrop
import com.athea.app.ui.liquid.liquidGlass
import com.athea.app.ui.theme.glass
import com.athea.app.ui.theme.messageStyle




/** Space the buttons occupy at the tail of the field, shared by every
 *  layout that reserves room for them. */


@Composable
fun InputBar(
    draft: String,
    suggestion: String?,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onExpandEditor: () -> Unit,
    onAddClick: () -> Unit,
    search: SearchState?,
    onSearchQueryChange: (String) -> Unit,
    onSearchNext: () -> Unit,
    onExitSearch: () -> Unit,
    enterSends: Boolean,
    caret: Int,
    onCaretChange: (Int) -> Unit,
    composerBackdrop: LiquidBackdrop? = null,
    modifier: Modifier = Modifier,
) {
    if (search == null) {
        DraftPanel(
            draft = draft,
            suggestion = suggestion,
            onDraftChange = onDraftChange,
            onSend = onSend,
            onExpandEditor = onExpandEditor,
            onAddClick = onAddClick,
            enterSends = enterSends,
            caret = caret,
            onCaretChange = onCaretChange,
            backdrop = composerBackdrop,
            modifier = modifier,
        )
    } else {
        SearchPanel(
            query = search.query,
            matchIndex = search.index,
            matchCount = search.matchBlockIds.size,
            onQueryChange = onSearchQueryChange,
            onNext = onSearchNext,
            onExit = onExitSearch,
            backdrop = composerBackdrop,
            modifier = modifier,
        )
    }
}

/** Floating gap — visibly lifted above key row, no solid black stripe; side margins 16dp let transcript peek with dimming */
@Composable
private fun Modifier.barPadding(): Modifier =
    this
        .padding(horizontal = 16.dp)
        .padding(top = 10.dp, bottom = 8.dp)

/**
 * One morphing panel, chat-app style:
 *  - short text: slim single row, buttons vertically centered beside it;
 *  - growing text: the field rises to full width, buttons stay on their
 *    own line at the tail, so no dead space appears beside the text.
 * The field is a single instance, so focus survives the morph.
 */
@Composable
private fun DraftPanel(
    draft: String,
    suggestion: String?,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onExpandEditor: () -> Unit,
    onAddClick: () -> Unit,
    enterSends: Boolean,
    caret: Int,
    onCaretChange: (Int) -> Unit,
    backdrop: LiquidBackdrop?,
    modifier: Modifier = Modifier,
) {
    var lineCount by remember { mutableStateOf(1) }
    var grown by remember { mutableStateOf(false) }
    if (draft.isEmpty()) grown = false
    if (lineCount > 1) grown = true
    // Sticky growth: a mid-length draft wraps in the narrow width but
    // fits in the wide one - without the latch the panel flip-flops.
    val compact = !grown

    Surface(
        shape = Ui.panelShape,
        color = Color.Transparent,
        modifier = modifier
            .fillMaxWidth()
            .barPadding()
            .liquidGlass(backdrop, Ui.panelShape, Ui.panelRadius, strong = true),
    ) {
        Column(Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
            Box {
                BasicTextField(
                    value = TextFieldValue(draft, TextRange(if (caret in 0..draft.length) caret else draft.length)),
                    onValueChange = { value ->
                        if (value.text != draft) onDraftChange(value.text)
                        onCaretChange(value.selection.end)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = if (compact) 50.dp else 16.dp,
                            end = if (compact) Ui.composerButtonSpace else Ui.composerFieldEndGrown,
                            top = if (compact) 4.dp else Ui.composerFieldTopGrown,
                            bottom = if (compact) 0.dp else Ui.composerButtonRowHeight,
                        ),
                    textStyle = messageStyle().copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    maxLines = 6,
                    onTextLayout = { lineCount = it.lineCount },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        keyboardType = KeyboardType.Ascii,
                        // None keeps the IME newline key: multi-line drafts stay possible.
                        imeAction = if (enterSends) ImeAction.Send else ImeAction.None,
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = { onSend() },
                    ),
                    decorationBox = { inner ->
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .then(if (compact) Modifier.heightIn(min = 40.dp) else Modifier),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            if (draft.isEmpty()) {
                                Text(
                                    stringResource(R.string.input_hint),
                                    style = messageStyle(),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    // Optically centered — 2dp lift is clearly visible
                                    modifier = Modifier.align(Alignment.CenterStart).padding(bottom = 2.dp),
                                )
                            }
                            // Ghost suggestion: the opaque draft text covers the
                            // prefix, the gray tail sticks out past the cursor.
                            if (suggestion != null && suggestion.startsWith(draft)) {
                                Text(
                                    suggestion,
                                    style = messageStyle(),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                        .copy(alpha = Ui.ghostAlpha),
                                    maxLines = 1,
                                )
                            }
                            Box(Modifier.align(Alignment.CenterStart)) { inner() }
                        }
                    },
                )
                if (compact) {
                    IconButton(
                        onClick = onAddClick,
                        modifier = Modifier.align(Alignment.CenterStart).size(Ui.composerButtonSize),
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = stringResource(R.string.attach_add),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                Row(
                    Modifier
                        .align(if (compact) Alignment.CenterEnd else Alignment.BottomEnd)
                        .padding(end = 4.dp, bottom = if (compact) 0.dp else 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (!compact) {
                        IconButton(onClick = onExpandEditor, modifier = Modifier.size(Ui.composerButtonSize)) {
                            Icon(
                                Icons.Default.OpenInFull,
                                contentDescription = stringResource(R.string.cd_expand_editor),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    val hasText = draft.isNotBlank()
                    FilledIconButton(
                        onClick = onSend,
                        enabled = hasText,
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (hasText) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceContainer
                            },
                            contentColor = if (hasText) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                        modifier = Modifier.size(Ui.composerButtonSize),
                    ) {
                        Icon(
                            Icons.Default.ArrowUpward,
                            contentDescription = stringResource(R.string.cd_send),
                        )
                    }
                }
                if (!compact) {
                    IconButton(
                        onClick = onAddClick,
                        modifier = Modifier.align(Alignment.BottomStart).size(Ui.composerButtonSize),
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = stringResource(R.string.attach_add),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchPanel(
    query: String,
    matchIndex: Int,
    matchCount: Int,
    onQueryChange: (String) -> Unit,
    onNext: () -> Unit,
    onExit: () -> Unit,
    backdrop: LiquidBackdrop?,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = Ui.panelShape,
        color = Color.Transparent,
        modifier = modifier
            .fillMaxWidth()
            .barPadding()
            .liquidGlass(backdrop, Ui.panelShape, Ui.panelRadius, strong = true),
    ) {
        Row(
            Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 6.dp),
            )
            // Same height and type as the composer. A long hint shrinks; the field never widens.
            Box(Modifier.weight(1f).heightIn(min = 40.dp), contentAlignment = Alignment.CenterStart) {
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = messageStyle().copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Search,
                    ),
                    keyboardActions = KeyboardActions(onSearch = { onNext() }),
                    decorationBox = { inner ->
                        if (query.isEmpty()) {
                            FittingText(
                                text = stringResource(R.string.search_in_session_hint),
                                style = messageStyle(),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        inner()
                    },
                )
            }
            Text(
                text = "${matchIndex + 1}/$matchCount",
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            IconButton(onClick = onNext) {
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onExit) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.cd_close),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** One line of text that shrinks its font until it fits the width, so it never widens the field beside it. */
@Composable
private fun FittingText(text: String, style: TextStyle, color: Color) {
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        Text(
            text,
            style = fittedStyle(measurer, text, style, constraints.maxWidth),
            color = color,
            maxLines = 1,
            softWrap = false,
        )
    }
}

/** The largest font size, down to [MIN_FIT_SP], at which [text] fits [maxWidth] pixels on one line. */
private fun fittedStyle(measurer: TextMeasurer, text: String, style: TextStyle, maxWidth: Int): TextStyle {
    var size = if (style.fontSize.isSpecified) style.fontSize.value else MIN_FIT_SP
    while (size > MIN_FIT_SP && measurer.measure(text, style.copy(fontSize = size.sp)).size.width > maxWidth) {
        size -= FIT_STEP_SP
    }
    return style.copy(fontSize = size.sp)
}

private const val MIN_FIT_SP = 11f
private const val FIT_STEP_SP = 0.5f
