package iatheiya.ayana.ui.main

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.StarBorder
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
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

/**
 * Full list of favorite commands: tap inserts into the editor, long
 * press opens per-item actions (run, edit, delete).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FavoritesScreen(
    favorites: List<FavoriteCommand>,
    onBack: () -> Unit,
    onInsert: (String) -> Unit,
    onRun: (String) -> Unit,
    onEdit: (Long, String) -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var editTarget by remember { mutableStateOf<FavoriteCommand?>(null) }
    val title = stringResource(R.string.favorites)

    AyanaScaffold(onBack = onBack, modifier = modifier) {
        if (favorites.isEmpty()) {
            Column(Modifier.fillMaxSize()) {
                AyanaScrollHeader(title)
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.favorites_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp),
                    )
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                item { AyanaScrollHeader(title) }
                items(favorites, key = { it.id }) { favorite ->
                    val menu = rememberAyanaMenu()
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { onInsert(favorite.text) },
                                onLongClick = menu::open,
                            )
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Outlined.StarBorder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = favorite.text,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 14.dp),
                        )
                        AyanaMenuHost(menu) {
                            item(
                                icon = Icons.Default.PlayArrow,
                                text = stringResource(R.string.favorites_run),
                            ) { onRun(favorite.text) }
                            item(
                                icon = Icons.Default.Edit,
                                text = stringResource(R.string.favorites_edit),
                            ) { editTarget = favorite }
                            item(
                                icon = Icons.Default.Delete,
                                text = stringResource(R.string.menu_delete),
                                tinted = true,
                            ) { onDelete(favorite.id) }
                        }
                    }
                }
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
        )
    }
}
