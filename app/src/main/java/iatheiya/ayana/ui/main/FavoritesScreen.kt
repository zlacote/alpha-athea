package iatheiya.ayana.ui.main

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import iatheiya.ayana.R
import iatheiya.ayana.core.model.FavoriteCommand
import iatheiya.ayana.ui.common.AyanaMenuHost
import iatheiya.ayana.ui.common.AyanaScaffold
import iatheiya.ayana.ui.common.AyanaScrollHeader
import iatheiya.ayana.ui.common.AyanaTextFieldDialog
import iatheiya.ayana.ui.common.rememberAyanaMenu
import iatheiya.ayana.ui.theme.AccentColor
import iatheiya.ayana.ui.theme.Ui
import iatheiya.ayana.ui.theme.contentOn
import iatheiya.ayana.ui.theme.glass
import iatheiya.ayana.ui.theme.messageStyle

/**
 * The kept commands, each in a bubble like a sent message: a tap uses it (as Settings say), a
 * long press offers run, edit, pin and delete. Pinned ones stay at the top. A long command shows
 * its first lines and opens to the rest. A command can also be written here directly.
 */
@Composable
fun FavoritesScreen(
    favorites: List<FavoriteCommand>,
    onBack: () -> Unit,
    onInsert: (String) -> Unit,
    onRun: (String) -> Unit,
    onAdd: (String) -> Unit,
    onEdit: (Long, String) -> Unit,
    onTogglePin: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var editTarget by remember { mutableStateOf<FavoriteCommand?>(null) }
    var adding by remember { mutableStateOf(false) }
    val title = stringResource(R.string.favorites)
    // Pinned first, each part in the order the commands were kept.
    val ordered = remember(favorites) { favorites.sortedByDescending { it.pinned } }

    AyanaScaffold(onBack = onBack, modifier = modifier) {
        LazyColumn(Modifier.fillMaxSize()) {
            item { AyanaScrollHeader(title) }
            item { AddCommandRow(onClick = { adding = true }) }
            if (ordered.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.favorites_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp, vertical = Ui.spaceXl),
                    )
                }
            }
            items(ordered, key = { it.id }) { favorite ->
                FavoriteBubble(
                    favorite = favorite,
                    onUse = { onInsert(favorite.text) },
                    onRun = { onRun(favorite.text) },
                    onEdit = { editTarget = favorite },
                    onTogglePin = { onTogglePin(favorite.id) },
                    onDelete = { onDelete(favorite.id) },
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
    if (adding) {
        AyanaTextFieldDialog(
            title = stringResource(R.string.favorites_add_title),
            initialValue = "",
            onDismiss = { adding = false },
            onSave = { text ->
                adding = false
                onAdd(text)
            },
            singleLine = false,
        )
    }
}

/** The row that writes a new favorite, at the top of the list, like an entry in the settings. */
@Composable
private fun AddCommandRow(onClick: () -> Unit) {
    SettingsCard {
        SettingsEntryRow(
            icon = Icons.Default.Add,
            title = stringResource(R.string.favorites_add),
            onClick = onClick,
            trailing = {},
        )
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
    onRun: () -> Unit,
    onEdit: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit,
) {
    val menu = rememberAyanaMenu()
    val fill = AccentColor.shown
    val ink = fill?.let(::contentOn) ?: MaterialTheme.colorScheme.onSecondaryContainer
    var open by remember(favorite.id) { mutableStateOf(false) }
    var overflows by remember(favorite.text) { mutableStateOf(false) }
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Ui.screenGutter, vertical = Ui.spaceXs),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(Ui.bubbleShape)
                .then(if (fill != null) Modifier.background(fill) else Modifier.glass(Ui.bubbleShape))
                .combinedClickable(onClick = onUse, onLongClick = menu::open)
                .animateContentSize()
                .padding(horizontal = Ui.bubblePaddingH, vertical = Ui.spaceM),
        ) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(Ui.spaceS)) {
                Text(
                    text = favorite.text,
                    style = messageStyle(),
                    color = ink,
                    maxLines = if (open) Int.MAX_VALUE else FAVORITE_PREVIEW_LINES,
                    overflow = TextOverflow.Ellipsis,
                    onTextLayout = { layout: TextLayoutResult -> if (!open) overflows = layout.hasVisualOverflow },
                    modifier = Modifier.weight(1f),
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
                        .clickable { open = !open },
                )
            }
        }
        AyanaMenuHost(menu) {
            item(icon = Icons.Default.PlayArrow, text = stringResource(R.string.favorites_run)) { onRun() }
            item(icon = Icons.Default.Edit, text = stringResource(R.string.favorites_edit)) { onEdit() }
            item(
                icon = Icons.Default.PushPin,
                text = stringResource(if (favorite.pinned) R.string.menu_unpin else R.string.menu_pin),
                crossed = favorite.pinned,
            ) { onTogglePin() }
            item(icon = Icons.Default.Delete, text = stringResource(R.string.menu_delete), tinted = true) { onDelete() }
        }
    }
}

/** How many lines of a kept command show before it folds. */
private const val FAVORITE_PREVIEW_LINES = 4
private val PIN_SIZE = 15.dp
