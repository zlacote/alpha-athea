package iatheiya.ayana.ui.common

import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import iatheiya.ayana.ui.liquid.LocalLiquidBackdrop
import iatheiya.ayana.ui.liquid.liquidBackdrop
import iatheiya.ayana.ui.liquid.rememberLiquidBackdrop
import iatheiya.ayana.ui.theme.Ui

/**
 * A screen with a floating glass back button. The back button stays in its corner;
 * the screen's content, which starts with [AyanaScrollHeader], scrolls under it.
 */
@Composable
fun AyanaScaffold(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    backIcon: ImageVector = Icons.AutoMirrored.Filled.ArrowBack,
    /** Drawn above the content and below the back button (which refracts it), e.g. a key being dragged. */
    overlay: @Composable BoxScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val backdrop = rememberLiquidBackdrop()
    val background = MaterialTheme.colorScheme.background

    Surface(color = background, modifier = modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalLiquidBackdrop provides backdrop) {
            Box(Modifier.fillMaxSize()) {
                // The overlay is recorded together with the content, so whatever it draws (a key
                // being dragged) is refracted by the back button when it passes beneath it.
                Box(
                    Modifier
                        .fillMaxSize()
                        .liquidBackdrop(backdrop, background),
                ) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(
                                WindowInsets.safeDrawing.only(
                                    WindowInsetsSides.Start + WindowInsetsSides.End + WindowInsetsSides.Bottom,
                                ),
                            ),
                    ) {
                        content()
                    }
                    overlay()
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = Ui.topBarHorizontalPadding, top = Ui.topBarTopPadding),
                ) {
                    AyanaRoundButton(onClick = onBack) {
                        Icon(
                            backIcon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

/**
 * The screen title as plain text at the top of the scrolling content, in the row the
 * back button leaves free. It has no outline and scrolls away with the content.
 */
@Composable
fun AyanaScrollHeader(title: String) {
    Column(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = Ui.topBarTopPadding)
                .height(Ui.topButtonSize)
                .padding(horizontal = Ui.topButtonSize + Ui.topBarHorizontalPadding * 2),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(Ui.topBarBottomPadding))
    }
}

/**
 * A settings-like page: its title, and its content under it. When everything fits on the screen,
 * the title and the content stand together in the middle of the space below the back button, the
 * title just above the first card; when it does not fit, the page scrolls from the top. One rule
 * for every such page, so none of them crowds the top and leaves the bottom empty.
 */
@Composable
fun AyanaPage(
    title: String,
    scrollState: ScrollState = rememberScrollState(),
    content: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val page = maxHeight
        val density = LocalDensity.current
        val top = WindowInsets.statusBars.getTop(density).let { with(density) { it.toDp() } } + PAGE_TOP_CLEAR
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = page)
                    .padding(top = top, bottom = PAGE_BOTTOM_ROOM),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(start = Ui.pageTextInset, end = Ui.pageTextInset, bottom = Ui.spaceS),
                )
                content()
            }
        }
    }
}

/** Room kept clear at the top of a page for the floating back button. */
private val PAGE_TOP_CLEAR = Ui.topBarTopPadding + Ui.topButtonSize + Ui.topBarBottomPadding

/** Room left under the last card of a page. */
private val PAGE_BOTTOM_ROOM = Ui.spaceXl
