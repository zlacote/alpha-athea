package com.athea.app.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import com.athea.app.ui.liquid.LocalLiquidBackdrop
import com.athea.app.ui.liquid.liquidBackdrop
import com.athea.app.ui.liquid.rememberLiquidBackdrop
import com.athea.app.ui.theme.Ui

/**
 * A screen with a floating header: a glass back button and a glass title over
 * the content. The header is not a bar. The content starts under it and scrolls
 * beneath it, so [content] receives [headerInset], the height the header takes:
 * the scrolling column puts that much space first, so the first item sits below
 * the header at rest.
 */
@Composable
fun AtheaScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    backIcon: ImageVector = Icons.AutoMirrored.Filled.ArrowBack,
    content: @Composable ColumnScope.(headerInset: Dp) -> Unit,
) {
    val backdrop = rememberLiquidBackdrop()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val headerInset = statusBarTop + Ui.topBarTopPadding + Ui.topButtonSize + Ui.topBarBottomPadding

    Surface(color = MaterialTheme.colorScheme.background, modifier = modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalLiquidBackdrop provides backdrop) {
            Box(Modifier.fillMaxSize()) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .liquidBackdrop(backdrop, MaterialTheme.colorScheme.background)
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing.only(
                                WindowInsetsSides.Start + WindowInsetsSides.End + WindowInsetsSides.Bottom,
                            ),
                        ),
                ) {
                    content(headerInset)
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(
                            start = Ui.topBarHorizontalPadding,
                            end = Ui.topBarHorizontalPadding,
                            top = Ui.topBarTopPadding,
                            bottom = Ui.topBarBottomPadding,
                        ),
                ) {
                    AtheaRoundButton(
                        onClick = onBack,
                        modifier = Modifier.align(Alignment.CenterStart),
                    ) {
                        Icon(
                            backIcon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    AtheaTitlePill(
                        title = title,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }
        }
    }
}
