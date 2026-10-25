package iatheiya.ayana.ui.main

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.InsertLink
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import iatheiya.ayana.R
import iatheiya.ayana.ui.MainViewModel
import iatheiya.ayana.session.WorkspaceEvent
import iatheiya.ayana.ui.UiState
import iatheiya.ayana.ui.common.AyanaDialogContainer
import iatheiya.ayana.ui.liquid.LiquidBackdrop
import iatheiya.ayana.ui.liquid.LocalLiquidBackdrop
import iatheiya.ayana.ui.liquid.liquidBackdrop
import iatheiya.ayana.ui.liquid.rememberLiquidBackdrop
import iatheiya.ayana.ui.theme.LocalMessageFontSize
import iatheiya.ayana.ui.theme.LocalOutputFontSize
import iatheiya.ayana.ui.theme.Ui
import iatheiya.ayana.util.copyToClipboard
import kotlinx.coroutines.launch

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Settings keep their scroll position while a sub-page is open, and start at the top again
    // once Settings is left.
    val settingsScroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    fun closeSettings() {
        scope.launch { settingsScroll.scrollTo(0) }
        viewModel.setShowSettings(false)
    }

    if (state.showGlassTuning) {
        BackHandler { viewModel.setShowGlassTuning(false) }
        GlassTuningScreen(onBack = { viewModel.setShowGlassTuning(false) })
        return
    }

    if (state.showGlow) {
        BackHandler { viewModel.setShowGlow(false) }
        GlowScreen(
            glowEnabled = state.workspace.settings.glowEnabled,
            glowStrengthPct = state.workspace.settings.glowStrengthPct,
            glowHeightDp = state.workspace.settings.glowHeightDp,
            glowWidthPct = state.workspace.settings.glowWidthPct,
            glowPeriodMs = state.workspace.settings.glowPeriodMs,
            onGlowEnabledChange = viewModel::setGlowEnabled,
            onGlowStrengthChange = viewModel::setGlowStrengthPct,
            onGlowHeightChange = viewModel::setGlowHeightDp,
            onGlowWidthChange = viewModel::setGlowWidthPct,
            onGlowPeriodChange = viewModel::setGlowPeriodMs,
            onBack = { viewModel.setShowGlow(false) },
        )
        return
    }

    if (state.showKeys) {
        BackHandler { viewModel.setShowKeys(false) }
        KeysScreen(
            keys = effectiveKeyRow(state.workspace.customKeys),
            keyRowVisible = state.workspace.settings.keyRowVisible,
            keyRows = state.workspace.settings.keyRows,
            keyCols = state.workspace.settings.keyCols,
            onKeyRowVisibleChange = viewModel::setKeyRowVisible,
            onKeyRowsChange = viewModel::setKeyRows,
            onKeyColsChange = viewModel::setKeyCols,
            onKeysChange = viewModel::setCustomKeys,
            onResetKeys = viewModel::resetKeysToDefaults,
            onBack = { viewModel.setShowKeys(false) },
        )
        return
    }

    if (state.showSettings) {
        BackHandler { closeSettings() }
        SettingsScreen(
            scrollState = settingsScroll,
            enterSends = state.workspace.settings.enterSends,
            autoScrollOnSend = state.workspace.settings.autoScrollOnSend,
            rawStream = state.workspace.settings.rawStream,
            autocompleteEnabled = state.workspace.settings.autocompleteEnabled,
            pinchZoomEnabled = state.workspace.settings.pinchZoomEnabled,
            virtualizeLargeOutput = state.workspace.settings.virtualizeLargeOutput,
            outputFontSizeSp = state.workspace.settings.outputFontSizeSp,
            previewLines = state.workspace.settings.previewLines,
            bubbleFontSizeSp = state.workspace.settings.bubbleFontSizeSp,
            onEnterSendsChange = viewModel::setEnterSends,
            onAutoScrollOnSendChange = viewModel::setAutoScrollOnSend,
            onRawStreamChange = viewModel::setRawStream,
            onAutocompleteEnabledChange = viewModel::setAutocompleteEnabled,
            onPinchZoomEnabledChange = viewModel::setPinchZoomEnabled,
            onVirtualizeLargeOutputChange = viewModel::setVirtualizeLargeOutput,
            onOutputFontSizeChange = viewModel::setOutputFontSize,
            onPreviewLinesChange = viewModel::setPreviewLines,
            onBubbleFontSizeChange = viewModel::setBubbleFontSize,
            onOpenKeys = { viewModel.setShowKeys(true) },
            onOpenGlow = { viewModel.setShowGlow(true) },
            attachSendsNow = state.workspace.settings.attachSendsNow,
            onAttachSendsNowChange = viewModel::setAttachSendsNow,
            favoriteReturnsHome = state.workspace.settings.favoriteReturnsHome,
            favoriteSendsImmediately = state.workspace.settings.favoriteSendsImmediately,
            onFavoriteReturnsHomeChange = viewModel::setFavoriteReturnsHome,
            onFavoriteSendsImmediatelyChange = viewModel::setFavoriteSendsImmediately,
            onOpenGlassTuning = { viewModel.setShowGlassTuning(true) },
            onBack = { closeSettings() },
        )
        return
    }

    if (state.showFavorites) {
        BackHandler { viewModel.setShowFavorites(false) }
        FavoritesScreen(
            favorites = state.workspace.favorites,
            onBack = { viewModel.setShowFavorites(false) },
            onInsert = { text ->
                if (state.workspace.settings.favoriteSendsImmediately) {
                    viewModel.executeDirectly(text)
                } else {
                    viewModel.insertIntoDraft(text)
                }
                if (state.workspace.settings.favoriteReturnsHome) {
                    viewModel.setShowFavorites(false)
                }
            },
            onRun = viewModel::executeDirectly,
            onEdit = viewModel::updateFavorite,
            onDelete = viewModel::deleteFavorite,
        )
        return
    }

    // ---- Attachment picker flow -----------------------------------------

    val pickerContext = LocalContext.current
    var attachAction by remember { mutableStateOf<String?>(null) }
    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        val action = attachAction
        attachAction = null
        if (uri != null && action != null) {
            viewModel.importAttachment(uri, action, pickerContext)
        }
    }
    if (state.showAttachChooser) {
        val attachOptions: List<Triple<String, String, androidx.compose.ui.graphics.vector.ImageVector>> =
            listOf(
                Triple("copy", stringResource(R.string.attach_copy), Icons.Filled.ContentCopy),
                Triple("link", stringResource(R.string.attach_link), Icons.Filled.InsertLink),
                Triple("path", stringResource(R.string.attach_path), Icons.Filled.TextFields),
            )
        AyanaDialogContainer(
            title = stringResource(R.string.attach_chooser_title),
            onDismiss = { viewModel.setShowAttachChooser(false) },
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    attachOptions.forEach { (action, label, icon) ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(Ui.attachmentShape)
                                .clickable {
                                    attachAction = action
                                    viewModel.setShowAttachChooser(false)
                                    filePicker.launch(arrayOf("*/*"))
                                }
                                .padding(horizontal = 14.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp),
                            )
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(start = 16.dp),
                            )
                        }
                    }
                }
            },
            confirm = {},
        )
    }

    CompositionLocalProvider(
        LocalOutputFontSize provides state.workspace.settings.outputFontSizeSp,
        LocalMessageFontSize provides state.workspace.settings.bubbleFontSizeSp,
    ) {
        MainScreenContent(viewModel, state)
    }
}

@Composable
private fun MainScreenContent(viewModel: MainViewModel, state: UiState) {
    val composerBackdrop = rememberLiquidBackdrop()
    // Every glass surface on this screen refracts the same transcript backdrop.
    CompositionLocalProvider(LocalLiquidBackdrop provides composerBackdrop) {
        MainScreenBody(viewModel, state, composerBackdrop)
    }
}

@Composable
private fun MainScreenBody(viewModel: MainViewModel, state: UiState, composerBackdrop: LiquidBackdrop) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    // Measured height of the composer and key row: the transcript keeps its last line above them.
    var bottomBarHeight by remember { mutableStateOf(0.dp) }

    // Native toasts: the system renders them, no custom surfaces needed.
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            val message = when (event) {
                WorkspaceEvent.ShellStartFailed ->
                    context.getString(R.string.shell_start_failed)

                is WorkspaceEvent.ShellExited ->
                    context.getString(R.string.shell_exited, event.exitCode)
            }
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT)
                .show()
        }
    }

    val current = state.workspace.sessions.firstOrNull { it.id == state.workspace.currentSessionId }

    // The composer's caret handle lives in its own popup window, above the drawer. As soon as the
    // drawer starts to open, the composer lets go of focus, so the handle (and the keyboard) go away
    // instead of floating over the sessions list.
    val focusManager = LocalFocusManager.current
    LaunchedEffect(drawerState) {
        snapshotFlow {
            drawerState.currentValue != DrawerValue.Closed ||
                drawerState.targetValue != DrawerValue.Closed ||
                drawerState.isAnimationRunning
        }.collect { opening -> if (opening) focusManager.clearFocus() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        // Drawer swipes must not fight with fullscreen overlays.
        gesturesEnabled = !state.editorExpanded && state.selectTextPayload == null,
        drawerContent = {
            SessionsDrawerContent(
                sessions = state.workspace.sessions,
                currentSessionId = state.workspace.currentSessionId,
                onSelectSession = { id ->
                    viewModel.selectSession(id)
                    scope.launch { drawerState.close() }
                },
                onRenameSession = viewModel::requestRename,
                onTogglePin = viewModel::togglePin,
                onDeleteSession = viewModel::requestDelete,
                onOpenFavorites = { viewModel.setShowFavorites(true) },
                onSettings = { viewModel.setShowSettings(true) },
            )
        },
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            // Full-bleed: the scrim and floating top buttons must reach
            // the status bar; insets are handled per-component instead.
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
        ) { padding ->
            val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            val topScrimHeight = statusBarTop + Ui.topBarScrimHeight
            // Outer Box overlays TopBar/scrim above the Column so the menu
            // scrim covers the whole screen (transcript + composer + key row),
            // not just the transcript area — dismiss works from any tap.
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
                    .background(MaterialTheme.colorScheme.background),
            ) {
                // Transcript fills the available area; the bottom bar
                // (InputBar + key row) floats above it for both the
                // session and the empty state, wired in exactly one place.
                if (current != null) {
                    Box(Modifier.fillMaxSize().liquidBackdrop(composerBackdrop, MaterialTheme.colorScheme.background)) {
                        val overlayBottom = bottomBarHeight + Ui.contentPaddingV
                        val fullScreen = current.fullScreen
                        if (fullScreen != null) {
                            FullScreenView(
                                screen = fullScreen,
                                contentTopPadding = statusBarTop + Ui.topBarContentTop,
                                contentBottomPadding = overlayBottom,
                                onAreaResized = viewModel::onTranscriptAreaResized,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            TranscriptView(
                                session = current,
                                search = state.workspace.search,
                                scrollRequests = viewModel.scrollRequests,
                                jumpToBottom = viewModel.jumpToBottom,
                                pageScroll = viewModel.pageScroll,
                                previewLines = state.workspace.settings.previewLines,
                                contentTopPadding = statusBarTop + Ui.topBarContentTop,
                                contentBottomPadding = overlayBottom,
                                pinchZoomEnabled = state.workspace.settings.pinchZoomEnabled,
                                virtualizeLargeOutput = state.workspace.settings.virtualizeLargeOutput,
                                onTranscriptFontZoom = viewModel::onTranscriptFontZoom,
                                onToggleBlock = viewModel::toggleBlockCollapsed,
                                onRevealBlock = viewModel::revealBlock,
                                onLocateBlock = { blockId ->
                                    current.blocks.indexOfFirst { it.block.id == blockId }
                                },
                                onCopyCommand = { text ->
                                    context.copyToClipboard(text, "ayana-command")
                                },
                                onCopyOutput = { text ->
                                    context.copyToClipboard(text, "ayana-output")
                                },
                                onSelectCommandText = viewModel::showSelectText,
                                onAddToFavorites = viewModel::addFavorite,
                                onAreaResized = viewModel::onTranscriptAreaResized,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }

                BottomBar(
                    viewModel = viewModel,
                    state = state,
                    draft = if (current != null) current.draft.orEmpty() else "",
                    suggestion = if (state.workspace.search == null) state.workspace.suggestion else null,
                    composerBackdrop = composerBackdrop,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .onSizeChanged { bottomBarHeight = with(density) { it.height.toDp() } },
                )

                // Top scrim: same gradient as before, now sibling of Column
                // so it sits above transcript but below TopBar.
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(topScrimHeight)
                        .align(Alignment.TopStart)
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Black,
                                1f to Color.Transparent,
                            )
                        ),
                )

                // The light of a running command comes down from the top edge, over the scrim and
                // under the top buttons.
                RunningGlow(
                    running = current?.running == true,
                    settings = state.workspace.settings,
                    modifier = Modifier.align(Alignment.TopCenter),
                )

                // TopBar overlay — its internal menu scrim is now full-screen
                // (covers transcript + composer + key row) because TopBar
                // fills the outer Box, not just the transcript Box.
                TopBar(
                    modifier = Modifier.align(Alignment.TopCenter),
                    hasMessages = current?.blocks?.isNotEmpty() == true,
                    pinned = current?.pinned ?: false,
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onNewSession = viewModel::newSession,
                    onSearch = viewModel::enterSearch,
                    onRename = { current?.let { viewModel.requestRename(it.id) } },
                    onTogglePin = { current?.let { viewModel.togglePin(it.id) } },
                    onDelete = { current?.let { viewModel.requestDelete(it.id) } },
                )
            }
        }
    }

    // ---- Fullscreen overlays -------------------------------------------

    if (state.editorExpanded && current != null) {
        EditorOverlay(
            draft = current.draft,
            onDraftChange = viewModel::onComposerTextChange,
            onSend = viewModel::sendFromExpandedEditor,
            onClose = { viewModel.setEditorExpanded(false) },
        )
    }

    state.selectTextPayload?.let { payload ->
        SelectTextScreen(text = payload, onClose = viewModel::dismissSelectText)
    }

    // ---- Dialogs ---------------------------------------------------------

    state.renameTargetId?.let { id ->
        val target = state.workspace.sessions.firstOrNull { it.id == id }
        if (target != null) {
            RenameDialog(
                initialName = target.name,
                onDismiss = viewModel::cancelRename,
                onSave = { name -> viewModel.renameSession(id, name) },
            )
        }
    }

    state.deleteTargetId?.let { id ->
        val target = state.workspace.sessions.firstOrNull { it.id == id }
        if (target != null) {
            DeleteConfirmDialog(
                sessionName = target.name,
                onDismiss = viewModel::cancelDelete,
                onConfirm = viewModel::confirmDelete,
            )
        }
    }
}

/**
 * The single wiring for the bottom panel (InputBar + key row). Used by both
 * the active-session and the empty states — there is exactly one place that
 * binds the composer to the ViewModel here, instead of the old copy-paste
 * of the whole block.
 */
@Composable
private fun BottomBar(
    viewModel: MainViewModel,
    state: UiState,
    draft: String,
    suggestion: String?,
    composerBackdrop: LiquidBackdrop?,
    modifier: Modifier = Modifier,
) {
    // One column aligned to the bottom edge: the composer sits directly above
    // the key row. Without this modifier both children fell to the top-start
    // corner of the screen.
    val keyRowShown = state.workspace.settings.keyRowVisible && state.workspace.search == null
    val current = state.workspace.sessions.firstOrNull { it.id == state.workspace.currentSessionId }
    Column(modifier = modifier) {
        InputBar(
            draft = draft,
            suggestion = suggestion,
            onDraftChange = viewModel::onComposerTextChange,
            onSend = viewModel::sendDraft,
            onExpandEditor = { viewModel.setEditorExpanded(true) },
            onAddClick = { viewModel.setShowAttachChooser(true) },
            search = state.workspace.search,
            onSearchQueryChange = viewModel::updateSearchQuery,
            onSearchNext = viewModel::nextSearchMatch,
            onExitSearch = viewModel::exitSearch,
            enterSends = state.workspace.settings.enterSends,
            selection = state.composerSelection,
            onSelectionChange = viewModel::setComposerSelection,
            composerBackdrop = composerBackdrop,
            directory = current?.directory,
            running = current?.running == true,
            docked = keyRowShown,
        )
        if (keyRowShown) {
            val keys = remember(state.workspace.customKeys) {
                effectiveKeyRow(state.workspace.customKeys).map { it.toTerminalKey() }
            }
            KeyRow(
                keys = keys,
                stickyCtrl = state.stickyCtrl,
                stickyAlt = state.stickyAlt,
                stickyPrefix = state.stickyPrefix,
                suggestionActive = suggestion != null,
                onInsert = viewModel::insertIntoDraft,
                onSendBytes = viewModel::sendDirectText,
                onCursor = viewModel::pressCursorKey,
                onAcceptSuggestion = viewModel::acceptSuggestion,
                onToggleStickyCtrl = viewModel::toggleStickyCtrl,
                onConsumeStickyCtrl = viewModel::consumeStickyCtrl,
                onToggleStickyAlt = viewModel::toggleStickyAlt,
                onConsumeStickyAlt = viewModel::consumeStickyAlt,
                onToggleStickyPrefix = viewModel::toggleStickyPrefix,
                onConsumeStickyPrefix = viewModel::consumeStickyPrefix,
                modifier = Modifier.navigationBarsPadding(),
                rows = state.workspace.settings.keyRows,
                cols = state.workspace.settings.keyCols,
            )
        }
    }
}



