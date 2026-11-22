package iatheiya.ayana.ui.main

import iatheiya.ayana.ui.common.foldGlide
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import iatheiya.ayana.R
import iatheiya.ayana.core.model.FavoriteCommand
import iatheiya.ayana.ui.common.AyanaRoundButton
import iatheiya.ayana.ui.common.AyanaScaffold
import iatheiya.ayana.ui.common.AyanaScrollHeader
import iatheiya.ayana.ui.common.AyanaTextFieldDialog
import iatheiya.ayana.ui.common.ContextMenuLayer
import iatheiya.ayana.ui.common.ContextMenuRequest
import iatheiya.ayana.ui.common.ContextMenuState
import iatheiya.ayana.ui.liquid.LocalLiquidBackdrop
import iatheiya.ayana.ui.liquid.liquidGlass
import iatheiya.ayana.ui.theme.AccentColor
import iatheiya.ayana.ui.theme.Ui
import iatheiya.ayana.ui.theme.contentOn
import iatheiya.ayana.ui.theme.glass
import iatheiya.ayana.ui.theme.glassInk
import iatheiya.ayana.ui.theme.messageStyle

/**
 * The kept commands, each in a bubble like a sent message: a tap puts it in the composer, a
 * long press offers run, copy, edit, pin and delete in the same glass menu as a message's. Pinned ones
 * stay at the top; a long command shows its first lines and opens to the rest. At the bottom, a
 * field like the composer writes a new one; the magnifier at the top right swaps it for a search
 * that narrows the list as you type, as the search in a chat does.
 */
@Composable
fun FavoritesScreen(
    favorites: List<FavoriteCommand>,
    onBack: () -> Unit,
    onInsert: (String) -> Unit,
    onRun: (String) -> Unit,
    onCopy: (String) -> Unit,
    onAdd: (String) -> Unit,
    onEdit: (Long, String) -> Unit,
    onTogglePin: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var editTarget by remember { mutableStateOf<FavoriteCommand?>(null) }
    var newCommand by remember { mutableStateOf("") }
    var query by remember { mutableStateOf<String?>(null) }
    val contextMenu = remember { ContextMenuState() }
    var barHeightPx by remember { mutableIntStateOf(0) }
    val barHeight = with(LocalDensity.current) { barHeightPx.toDp() }
    val title = stringResource(R.string.favorites)
    // Pinned first, each part in the order the commands were kept; narrowed while searching.
    val ordered = remember(favorites) { favorites.sortedByDescending { it.pinned } }
    val shown = query?.takeIf { it.isNotBlank() }?.let { q -> ordered.filter { it.text.contains(q, ignoreCase = true) } } ?: ordered

    // Back closes the search first, then the screen.
    BackHandler(enabled = query != null) { query = null }

    AyanaScaffold(
        onBack = onBack,
        modifier = modifier,
        floating = {
            // The magnifier, in the corner the back button mirrors.
            AyanaRoundButton(
                onClick = { query = if (query == null) "" else null },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(end = Ui.topBarHorizontalPadding, top = Ui.topBarTopPadding),
            ) {
                Icon(
                    if (query == null) Icons.Default.Search else Icons.Default.Close,
                    contentDescription = stringResource(if (query == null) R.string.cd_search_favorites else R.string.cd_close),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            FloatingBar(onHeight = { barHeightPx = it }) {
                val q = query
                if (q == null) {
                    FavoriteField(
                        value = newCommand,
                        onValueChange = { newCommand = it },
                        hint = stringResource(R.string.favorites_add_hint),
                        trailing = {
                            SendCircleButton(
                                enabled = newCommand.isNotBlank(),
                                onClick = {
                                    onAdd(newCommand)
                                    newCommand = ""
                                },
                            )
                        },
                    )
                } else {
                    FavoriteField(
                        value = q,
                        onValueChange = { query = it },
                        hint = stringResource(R.string.favorites_search_hint),
                        focusNow = true,
                        singleLine = true,
                        trailing = {
                            Text(
                                text = "${shown.size}/${ordered.size}",
                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                                color = glassInk().copy(alpha = Ui.hintAlpha),
                                modifier = Modifier.padding(horizontal = Ui.spaceS),
                            )
                            IconButton(onClick = { query = null }, modifier = Modifier.size(Ui.composerButtonSize)) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cd_close), tint = glassInk())
                            }
                        },
                    )
                }
            }
            ContextMenuLayer(contextMenu)
        },
    ) {
        LazyColumn(
            Modifier.fillMaxSize(),
            // The last bubble can always be scrolled clear of the bar over it.
            contentPadding = PaddingValues(bottom = barHeight + Ui.spaceM),
        ) {
            item { AyanaScrollHeader(title) }
            if (shown.isEmpty()) {
                item {
                    Text(
                        text = stringResource(if (query.isNullOrBlank()) R.string.favorites_empty else R.string.favorites_no_match),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp, vertical = Ui.spaceXl),
                    )
                }
            }
            items(shown, key = { it.id }) { favorite ->
                val runLabel = stringResource(R.string.favorites_run)
                val copyLabel = stringResource(R.string.copy)
                val editLabel = stringResource(R.string.favorites_edit)
                val pinLabel = stringResource(if (favorite.pinned) R.string.menu_unpin else R.string.menu_pin)
                val deleteLabel = stringResource(R.string.menu_delete)
                FavoriteBubble(
                    favorite = favorite,
                    onUse = { onInsert(favorite.text) },
                    onMenu = { bounds ->
                        contextMenu.show(
                            ContextMenuRequest(bounds) {
                                item(icon = Icons.Default.PlayArrow, text = runLabel) { onRun(favorite.text) }
                                item(icon = Icons.Default.ContentCopy, text = copyLabel) { onCopy(favorite.text) }
                                item(icon = Icons.Default.Edit, text = editLabel) { editTarget = favorite }
                                item(icon = Icons.Default.PushPin, text = pinLabel, crossed = favorite.pinned) {
                                    onTogglePin(favorite.id)
                                }
                                item(icon = Icons.Default.Delete, text = deleteLabel, tinted = true) { onDelete(favorite.id) }
                            },
                        )
                    },
                )
            }
        }
    }

    editTarget?.let { target ->
        AyanaTextFieldDialog(
            title = stringResource(R.string.favorites_edit_title),
            initialValue = target.text,
            onDismiss = { editTarget = null },
            onSave = { text ->
                editTarget = null
                onEdit(target.id, text)
            },
            singleLine = false,
        )
    }
}

/**
 * The glass bar along the bottom, over the list and above the keyboard, as the composer floats
 * when the key row is off. It reports its height, so the list can keep clear of it.
 */
@Composable
private fun BoxScope.FloatingBar(onHeight: (Int) -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .onSizeChanged { onHeight(it.height) }
            .padding(horizontal = Ui.floatingMarginH)
            .padding(top = 10.dp, bottom = 8.dp)
            .liquidGlass(LocalLiquidBackdrop.current, Ui.panelShape, Ui.panelRadius)
            .padding(horizontal = Ui.floatingPaddingH, vertical = 4.dp),
    ) {
        content()
    }
}

/**
 * A field in the bar: it grows with what is written, up to a few lines, with [trailing] (the send
 * button, or the search's count and cross) beside it.
 */
@Composable
private fun FavoriteField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    trailing: @Composable () -> Unit,
    focusNow: Boolean = false,
    singleLine: Boolean = false,
) {
    val ink = glassInk()
    val focus = remember { FocusRequester() }
    if (focusNow) LaunchedEffect(Unit) { focus.requestFocus() }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .weight(1f)
                .heightIn(min = Ui.composerFieldMinHeight)
                .padding(start = Ui.spaceM, end = Ui.spaceXs),
            contentAlignment = Alignment.CenterStart,
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focus),
                textStyle = messageStyle().copy(color = ink),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                singleLine = singleLine,
                maxLines = if (singleLine) 1 else FIELD_MAX_LINES,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty()) {
                            Text(hint, style = messageStyle(), color = ink.copy(alpha = Ui.hintAlpha), maxLines = 1)
                        }
                        inner()
                    }
                },
            )
        }
        trailing()
    }
}

/**
 * One kept command, in the bubble a sent message wears (the accent, softened as everywhere, or
 * glass), across the width. Up to [FAVORITE_PREVIEW_LINES] lines show; a longer one opens with
 * the chevron under it.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FavoriteBubble(
    favorite: FavoriteCommand,
    onUse: () -> Unit,
    onMenu: (Rect) -> Unit,
) {
    val fill = AccentColor.shown
    val ink = fill?.let(::contentOn) ?: MaterialTheme.colorScheme.onSecondaryContainer
    var open by remember(favorite.id) { mutableStateOf(false) }
    var overflows by remember(favorite.text) { mutableStateOf(false) }
    var bounds by remember { mutableStateOf(Rect.Zero) }
    // Only an opening or folding by hand glides: a folded command shows folded from the start,
    // with no fold played as the screen opens.
    var foldByHand by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Ui.screenGutter, vertical = Ui.spaceXs)
            .onGloballyPositioned { bounds = it.boundsInRoot() }
            .clip(Ui.bubbleShape)
            .then(if (fill != null) Modifier.background(fill) else Modifier.glass(Ui.bubbleShape))
            .combinedClickable(onClick = onUse, onLongClick = { onMenu(bounds) })
            .foldGlide(foldByHand) { foldByHand = false }
            .padding(horizontal = Ui.bubblePaddingH, vertical = Ui.spaceM),
    ) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(Ui.spaceS)) {
            Text(
                text = favorite.text,
                style = messageStyle(),
                color = ink,
                maxLines = if (open) Int.MAX_VALUE else FAVORITE_PREVIEW_LINES,
                // Folded, its last lines fade out, as a folded message in the chat does.
                overflow = TextOverflow.Clip,
                onTextLayout = { layout: TextLayoutResult -> if (!open) overflows = layout.hasVisualOverflow },
                modifier = Modifier
                    .weight(1f)
                    .then(if (!open && overflows) Modifier.fadeOutBottom(Ui.tailFadeHeight) else Modifier),
            )
            if (favorite.pinned) {
                Icon(
                    Icons.Default.PushPin,
                    contentDescription = null,
                    tint = ink.copy(alpha = Ui.chevronAlphaCollapsedPreview),
                    modifier = Modifier.size(PIN_SIZE),
                )
            }
        }
        if (overflows || open) {
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = stringResource(if (open) R.string.cd_collapse else R.string.cd_expand_editor),
                tint = ink.copy(alpha = Ui.chevronAlphaCollapsedPreview),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = Ui.spaceXs)
                    .size(Ui.chevronExpandSize)
                    .rotate(if (open) 180f else 0f)
                    .clickable {
                        foldByHand = true
                        open = !open
                    },
            )
        }
    }
}

/** How many lines of a kept command show before it folds. */
private const val FAVORITE_PREVIEW_LINES = 4
/** How many lines the field at the bottom grows to before it scrolls. */
private const val FIELD_MAX_LINES = 5
private val PIN_SIZE = 15.dp
