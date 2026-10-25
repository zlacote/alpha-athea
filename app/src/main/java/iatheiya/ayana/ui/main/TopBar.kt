package iatheiya.ayana.ui.main

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.alpha
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import iatheiya.ayana.R
import iatheiya.ayana.ui.common.AyanaMenuHost
import iatheiya.ayana.ui.common.AyanaPill
import iatheiya.ayana.ui.common.AyanaPillIcon
import iatheiya.ayana.ui.common.AyanaRoundButton
import iatheiya.ayana.ui.common.AyanaScrim
import iatheiya.ayana.ui.common.rememberAyanaMenu
import iatheiya.ayana.ui.theme.Ui

/**
 * Floating top controls, chat-app style: one standalone round button on
 * the left, one merged pill on the right. The overflow menu is a full-
 * screen overlay that covers the buttons and the content behind them.
 */
@Composable
fun TopBar(
    hasMessages: Boolean,
    pinned: Boolean,
    onOpenDrawer: () -> Unit,
    onNewSession: () -> Unit,
    onSearch: () -> Unit,
    onRename: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val menu = rememberAyanaMenu()

    Box(modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(
                    start = Ui.topBarHorizontalPadding,
                    end = Ui.topBarHorizontalPadding,
                    top = Ui.topBarTopPadding,
                    bottom = Ui.topBarBottomPadding,
                ),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AyanaRoundButton(onClick = onOpenDrawer) {
                Icon(
                    Icons.Default.Menu,
                    contentDescription = stringResource(R.string.cd_open_drawer),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }

            // The pill fades out while the menu is open, so its buttons do not sit uselessly
            // behind the panel; it fades back once the menu closes, so neither is abrupt.
            val pillAlpha by animateFloatAsState(
                targetValue = if (menu.expanded) 0f else 1f,
                animationSpec = tween(durationMillis = 140),
                label = "pillAlpha",
            )
            AyanaPill(modifier = Modifier.alpha(pillAlpha)) {
                if (hasMessages) {
                    AyanaPillIcon(
                        icon = Icons.Default.Add,
                        contentDescription = stringResource(R.string.cd_new_session),
                        onClick = onNewSession,
                        enabled = !menu.expanded,
                    )
                }
                AyanaPillIcon(
                    icon = Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.cd_more),
                    onClick = menu::toggle,
                    // The open menu covers this button; taps there belong to the menu's items.
                    enabled = !menu.expanded,
                )
            }
        }

        // Scrim sits above the buttons: with the menu open, every tap
        // outside the menu panel — including the pill itself — closes it.
        AyanaScrim(menu.expanded, menu::close)

        // Menu panel: inline (not Popup) so it exactly covers the pill — Popup has own window + 8dp offset.
        // Anchored at the pill's own top so it opens OVER the pill (covering the +/⋮ buttons),
        // not below it. Above the scrim, so its items stay tappable while the scrim is live.
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = Ui.topBarTopPadding, end = Ui.topBarHorizontalPadding),
        ) {
            AyanaMenuHost(menu, isPopup = false) {
                if (hasMessages) {
                    item(
                        icon = Icons.Default.Search,
                        text = stringResource(R.string.menu_search),
                    ) { onSearch() }
                }
                item(
                    icon = Icons.Default.Edit,
                    text = stringResource(R.string.menu_rename),
                ) { onRename() }
                item(
                    icon = Icons.Default.PushPin,
                    text = stringResource(
                        if (pinned) R.string.menu_unpin else R.string.menu_pin
                    ),
                    crossed = pinned,
                ) { onTogglePin() }
                if (hasMessages) {
                    item(
                        icon = Icons.Default.Delete,
                        text = stringResource(R.string.menu_delete),
                        tinted = true,
                    ) { onDelete() }
                }
            }
        }
    }
}
