package iatheiya.ayana.ui.main

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.zIndex
import iatheiya.ayana.session.Attachments
import iatheiya.ayana.ui.common.AyanaDrawer
import iatheiya.ayana.ui.common.rememberAyanaDrawerState
import iatheiya.ayana.util.toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import kotlin.math.abs
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntOffset
import iatheiya.ayana.ui.common.pressWash
import kotlin.math.roundToInt
import android.graphics.Typeface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import iatheiya.ayana.data.AWAKE_ALWAYS
import iatheiya.ayana.data.AWAKE_RUNNING
import iatheiya.ayana.data.FONT_CUSTOM
import iatheiya.ayana.ui.theme.withFamily
import iatheiya.ayana.data.FONT_MONO
import iatheiya.ayana.data.FONT_SERIF
import iatheiya.ayana.ui.liquid.GlassTuning
import iatheiya.ayana.ui.theme.AccentColor
import iatheiya.ayana.ui.theme.SearchHighlight
import iatheiya.ayana.ui.theme.LocalTranscriptFont
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.InsertLink
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import iatheiya.ayana.R
import iatheiya.ayana.ui.MainViewModel
import iatheiya.ayana.session.WorkspaceEvent
import iatheiya.ayana.ui.UiState
import iatheiya.ayana.ui.common.AyanaDialogContainer
import iatheiya.ayana.ui.common.ContextMenuLayer
import iatheiya.ayana.ui.common.ContextMenuState
import iatheiya.ayana.ui.common.LocalContextMenu
import iatheiya.ayana.ui.common.LocalImmersive
import iatheiya.ayana.ui.common.setStatusBarHidden
import iatheiya.ayana.ui.liquid.LiquidBackdrop
import iatheiya.ayana.ui.liquid.LocalLiquidBackdrop
import iatheiya.ayana.ui.liquid.liquidBackdrop
import iatheiya.ayana.ui.liquid.backdropGap
import iatheiya.ayana.ui.liquid.liquidGlass
import iatheiya.ayana.ui.liquid.rememberLiquidBackdrop
import iatheiya.ayana.ui.theme.LocalMessageFontSize
import iatheiya.ayana.ui.theme.LocalOutputFontSize
import iatheiya.ayana.ui.theme.Ui
import iatheiya.ayana.util.copyToClipboard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val settings = state.workspace.settings
    // The chosen font is the app's font, on every screen, not only in the transcript.
    val font = remember(settings.font, settings.customFontVersion) {
        when (settings.font) {
            FONT_MONO -> FontFamily.Monospace
            FONT_SERIF -> FontFamily.Serif
            FONT_CUSTOM -> runCatching { FontFamily(Typeface.createFromFile(viewModel.customFontFile())) }.getOrNull()
            else -> null
        }
    }
    val baseTypography = MaterialTheme.typography
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme,
        typography = remember(font, baseTypography) { baseTypography.withFamily(font) },
    ) {
        // A press lights up in the accent colour, when one is chosen.
        val ripple = AccentColor.shown?.let { RippleConfiguration(color = it) } ?: LocalRippleConfiguration.current
        CompositionLocalProvider(
            LocalTranscriptFont provides font,
            LocalRippleConfiguration provides ripple,
            LocalImmersive provides settings.immersiveMode,
        ) {
            MainScreenRoutes(viewModel, state)
        }
    }
}

/** Which screen is up (settings, its sub-screens, favourites, or the chat), and the app-wide effects. */
@Composable
private fun MainScreenRoutes(viewModel: MainViewModel, state: UiState) {

    // Settings keep their scroll position while a sub-page is open, and start at the top again
    // once Settings is left.
    val settingsScroll = rememberScrollState()
    // The open settings room and its scroll live here too: Glass and Glow replace the settings on
    // screen, and going back from them must land in the room they were opened from.
    var settingsPane by rememberSaveable { mutableStateOf(SettingsPane.ROOT) }
    val appearanceScroll = rememberScrollState()
    val scope = rememberCoroutineScope()

    val settings = state.workspace.settings
    val appContext = LocalContext.current.applicationContext

    // Appearance, applied here above the early returns so it holds on whichever screen is open.
    val view = LocalView.current
    val anyRunning = state.workspace.sessions.any { it.running }
    val awake = when (settings.screenAwake) {
        AWAKE_ALWAYS -> true
        AWAKE_RUNNING -> anyRunning
        else -> false
    }
    DisposableEffect(view, awake) {
        view.keepScreenOn = awake
        onDispose { view.keepScreenOn = false }
    }
    // The glass switch lives in GlassTuning (the tuning screen flips it too); it is stored in the
    // settings so it survives a restart: the stored value is applied, and every change is stored.
    LaunchedEffect(settings.glassEnabled) { GlassTuning.enabled = settings.glassEnabled }
    LaunchedEffect(Unit) { snapshotFlow { GlassTuning.enabled }.collect { viewModel.setGlassEnabled(it) } }
    // The same for tinting the glass with the accent (a switch on the Glass screen).
    LaunchedEffect(settings.glassTinted) { AccentColor.tintsGlass = settings.glassTinted }
    LaunchedEffect(Unit) { snapshotFlow { AccentColor.tintsGlass }.collect { viewModel.setGlassTinted(it) } }
    val systemTint = systemAccent()
    SideEffect {
        systemTint?.let { SearchHighlight.background = it }
        AccentColor.softened = settings.tintSoft
        AccentColor.color = resolveTint(settings.tintMode, settings.tintArgb, systemTint)
    }

    // Full screen hides the status bar on every screen; a swipe from the top brings it back for
    // a moment.
    val activity = LocalContext.current as? android.app.Activity
    val immersive = settings.immersiveMode
    LaunchedEffect(activity, immersive) {
        val window = activity?.window ?: return@LaunchedEffect
        setStatusBarHidden(window, window.decorView, hidden = immersive)
    }
    fun closeSettings() {
        scope.launch {
            settingsScroll.scrollTo(0)
            appearanceScroll.scrollTo(0)
        }
        settingsPane = SettingsPane.ROOT
        viewModel.setShowSettings(false)
    }

    if (state.showGlassTuning) {
        BackHandler { viewModel.setShowGlassTuning(false) }
        GlassTuningScreen(
            onBack = { viewModel.setShowGlassTuning(false) },
            onEnableAccent = {
                val (mode, argb) = tintWhenEnabled(settings.tintArgb)
                viewModel.setTint(mode, argb)
            },
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
            onBack = { viewModel.setShowKeys(false) },
        )
        return
    }

    if (state.showSettings) {
        BackHandler { closeSettings() }
        SettingsScreen(
            scrollState = settingsScroll,
            onResetAll = viewModel::resetAllSettings,
            pane = settingsPane,
            // Going back out of a room forgets where it was scrolled; going deeper (Appearance into
            // Glass) keeps it, since this screen stays put underneath.
            onPaneChange = { next ->
                if (next == SettingsPane.ROOT && settingsPane == SettingsPane.APPEARANCE) {
                    scope.launch { appearanceScroll.scrollTo(0) }
                }
                settingsPane = next
            },
            appearanceScroll = appearanceScroll,
            appearance = AppearanceState(
                immersiveMode = settings.immersiveMode,
                screenAwake = settings.screenAwake,
                tintMode = settings.tintMode,
                tintArgb = settings.tintArgb,
                tintSoft = settings.tintSoft,
                font = settings.font,
                hasCustomFont = settings.customFontVersion != 0L,
                hasWallpaper = settings.wallpaperVersion != 0L,
                wallpaperDimPct = settings.wallpaperDimPct,
                wallpaperBlurDp = settings.wallpaperBlurDp,
                pinchZoomEnabled = settings.pinchZoomEnabled,
                greetingAnimated = settings.greetingAnimated,
                outputFontSizeSp = settings.outputFontSizeSp,
                bubbleFontSizeSp = settings.bubbleFontSizeSp,
            ),
            appearanceActions = AppearanceActions(
                onImmersiveModeChange = viewModel::setImmersiveMode,
                onScreenAwakeChange = viewModel::setScreenAwake,
                onTintChange = viewModel::setTint,
                onTintSoftChange = viewModel::setTintSoft,
                onFontChange = viewModel::setFont,
                onFontPicked = { uri -> viewModel.importCustomFont(uri, appContext) },
                onWallpaperPicked = { uri -> viewModel.importWallpaper(uri, appContext) },
                onRemoveWallpaper = viewModel::removeWallpaper,
                onWallpaperDimChange = viewModel::setWallpaperDim,
                onWallpaperBlurChange = viewModel::setWallpaperBlur,
                onPinchZoomEnabledChange = viewModel::setPinchZoomEnabled,
                onGreetingAnimatedChange = viewModel::setGreetingAnimated,
                onOutputFontSizeChange = viewModel::setOutputFontSize,
                onBubbleFontSizeChange = viewModel::setBubbleFontSize,
                onOpenGlassTuning = { viewModel.setShowGlassTuning(true) },
            ),
            glowCard = {
                GlowCard(
                    glowEnabled = settings.glowEnabled,
                    glowStrengthPct = settings.glowStrengthPct,
                    glowHeightDp = settings.glowHeightDp,
                    glowWidthPct = settings.glowWidthPct,
                    glowPeriodMs = settings.glowPeriodMs,
                    onGlowEnabledChange = viewModel::setGlowEnabled,
                    onGlowStrengthChange = viewModel::setGlowStrengthPct,
                    onGlowHeightChange = viewModel::setGlowHeightDp,
                    onGlowWidthChange = viewModel::setGlowWidthPct,
                    onGlowPeriodChange = viewModel::setGlowPeriodMs,
                )
            },
            enterSends = state.workspace.settings.enterSends,
            autoScrollOnSend = state.workspace.settings.autoScrollOnSend,
            rawStream = state.workspace.settings.rawStream,
            signalLongCommands = settings.signalLongCommands,
            longCommandSec = settings.longCommandSec,
            onSignalLongCommandsChange = viewModel::setSignalLongCommands,
            notificationsAsked = settings.notificationsAsked,
            onNotificationsAsked = viewModel::markNotificationsAsked,
            onWaitForNotificationSettings = viewModel::waitForNotificationSettings,
            onLongCommandSecChange = viewModel::setLongCommandSec,
            autocompleteEnabled = state.workspace.settings.autocompleteEnabled,
            downSetsDraftAside = settings.downSetsDraftAside,
            onDownSetsDraftAsideChange = viewModel::setDownSetsDraftAside,
            virtualizeLargeOutput = state.workspace.settings.virtualizeLargeOutput,
            previewLines = state.workspace.settings.previewLines,
            onEnterSendsChange = viewModel::setEnterSends,
            onAutoScrollOnSendChange = viewModel::setAutoScrollOnSend,
            onRawStreamChange = viewModel::setRawStream,
            onAutocompleteEnabledChange = viewModel::setAutocompleteEnabled,
            onVirtualizeLargeOutputChange = viewModel::setVirtualizeLargeOutput,
            onPreviewLinesChange = viewModel::setPreviewLines,
            onOpenKeys = { viewModel.setShowKeys(true) },
            attachSendsNow = state.workspace.settings.attachSendsNow,
            onAttachSendsNowChange = viewModel::setAttachSendsNow,
            favoritesEnabled = state.workspace.settings.favoritesEnabled,
            onFavoritesEnabledChange = viewModel::setFavoritesEnabled,
            favoriteReturnsHome = state.workspace.settings.favoriteReturnsHome,
            favoriteSendsImmediately = state.workspace.settings.favoriteSendsImmediately,
            onFavoriteReturnsHomeChange = viewModel::setFavoriteReturnsHome,
            onFavoriteSendsImmediatelyChange = viewModel::setFavoriteSendsImmediately,
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
            onAdd = viewModel::addFavorite,
            onEdit = viewModel::updateFavorite,
            onTogglePin = viewModel::togglePinFavorite,
            onDelete = viewModel::deleteFavorite,
        )
        return
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
    val contextMenu = remember { ContextMenuState() }
    // Every glass surface on this screen refracts the same transcript backdrop.
    CompositionLocalProvider(LocalLiquidBackdrop provides composerBackdrop, LocalContextMenu provides contextMenu) {
        MainScreenBody(viewModel, state, composerBackdrop)
    }
}

@Composable
private fun MainScreenBody(viewModel: MainViewModel, state: UiState, composerBackdrop: LiquidBackdrop) {
    val context = LocalContext.current
    val drawerState = rememberAyanaDrawerState()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    // Measured height of the composer and key row: the transcript keeps its last line above them.
    var bottomBarHeight by remember { mutableStateOf(0.dp) }
    // The same height in pixels, read by the transcript while it is laid out (see BottomBar).
    val barHeightPx = remember { mutableIntStateOf(0) }
    // The transcript is scrolled up from its end: the composer offers a jump back down.
    var jumpAvailable by remember { mutableStateOf(false) }
    // Where the send circle is, and where this screen's box is, both in window coordinates.
    var sendBounds by remember { mutableStateOf(Rect.Zero) }
    var panelBounds by remember { mutableStateOf(Rect.Zero) }
    var screenOrigin by remember { mutableStateOf(Offset.Zero) }
    // The + panel, and the system picker it opens (several files can be picked at once).
    var attachOpen by remember { mutableStateOf(false) }
    var attachAction by remember { mutableStateOf<String?>(null) }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        val action = attachAction
        attachAction = null
        if (uris.isNotEmpty() && action != null) viewModel.importAttachments(uris, action, context)
    }
    fun pickFiles(action: String) {
        attachAction = action
        filePicker.launch(arrayOf("*/*"))
    }

    // Native toasts: the system renders them, no custom surfaces needed.
    LaunchedEffect(viewModel) {
        // Several shells ended together (the system stopping them all while Ayana was away) say
        // so once, not once each.
        var lastExitToast = 0L
        viewModel.events.collect { event ->
            val message = when (event) {
                WorkspaceEvent.ShellStartFailed ->
                    context.getString(R.string.shell_start_failed)

                is WorkspaceEvent.ShellExited -> {
                    val now = android.os.SystemClock.uptimeMillis()
                    if (now - lastExitToast < EXIT_TOAST_QUIET_MS) return@collect
                    lastExitToast = now
                    if (event.restarted) {
                        context.getString(R.string.shell_restarted, event.exitCode)
                    } else {
                        context.getString(R.string.shell_exited, event.exitCode)
                    }
                }
            }
            context.toast(message)
        }
    }

    val current = state.workspace.sessions.firstOrNull { it.id == state.workspace.currentSessionId }

    // The composer's caret handle lives in its own popup window, above the drawer. As soon as the
    // drawer starts to open, the composer lets go of focus, so the handle (and the keyboard) go away
    // instead of floating over the sessions list.
    val focusManager = LocalFocusManager.current
    LaunchedEffect(drawerState) {
        snapshotFlow { drawerState.isVisible }.collect { opening -> if (opening) focusManager.clearFocus() }
    }

    AyanaDrawer(
        state = drawerState,
        // Drawer swipes must not fight with fullscreen overlays.
        enabled = !state.editorExpanded && state.selectTextPayload == null,
        drawerContent = {
            SessionsDrawerContent(
                sessions = state.workspace.sessions,
                currentSessionId = state.workspace.currentSessionId,
                onSelectSession = { id ->
                    viewModel.selectSession(id)
                    drawerState.close()
                },
                onRenameSession = viewModel::requestRename,
                onTogglePin = viewModel::togglePin,
                onDeleteSession = viewModel::requestDelete,
                onOpenFavorites = if (state.workspace.settings.favoritesEnabled) {
                    { viewModel.setShowFavorites(true) }
                } else {
                    null
                },
                onSettings = { viewModel.setShowSettings(true) },
                pulsePeriodMs = state.workspace.settings.glowPeriodMs,
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
                    .background(MaterialTheme.colorScheme.background)
                    .onGloballyPositioned { screenOrigin = it.positionInRoot() },
            ) {
                // The bottom bar is measured first, so the transcript's room under it is known in
                // the same layout that changes it: a send that shrinks the composer moves the
                // newest message down with it at once, not a frame later.
                BottomBar(
                    viewModel = viewModel,
                    state = state,
                    draft = if (current != null) current.draft.orEmpty() else "",
                    suggestion = if (state.workspace.search == null) state.workspace.suggestion else null,
                    composerBackdrop = composerBackdrop,
                    onSendBounds = { sendBounds = it },
                    onPanelBounds = { panelBounds = it },
                    onAttach = { attachOpen = true },
                    // The caret and its handle (which lives above everything) step aside while the
                    // + panel is open; the keyboard stays.
                    caretHidden = attachOpen,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        // Drawn over the transcript, though measured before it.
                        .zIndex(1f)
                        .onSizeChanged {
                            barHeightPx.intValue = it.height
                            bottomBarHeight = with(density) { it.height.toDp() }
                        },
                )

                // Transcript fills the available area; the bottom bar
                // (InputBar + key row) floats above it for both the
                // session and the empty state, wired in exactly one place.
                if (current != null) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .liquidBackdrop(composerBackdrop, MaterialTheme.colorScheme.background),
                    ) {
                        // Inside the backdrop, so the glass refracts the wallpaper like any content.
                        Wallpaper(
                            file = viewModel.wallpaperFile(),
                            version = state.workspace.settings.wallpaperVersion,
                            dimPct = state.workspace.settings.wallpaperDimPct,
                            blurDp = state.workspace.settings.wallpaperBlurDp,
                        )
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
                                contentBottomPaddingNow = { with(density) { barHeightPx.intValue.toDp() } + Ui.contentPaddingV },
                                onJumpAvailableChange = { jumpAvailable = it },
                                pinchZoomEnabled = state.workspace.settings.pinchZoomEnabled,
                                greetingAnimated = state.workspace.settings.greetingAnimated || current.id == state.workspace.settings.introSessionId,
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
                                onAddToFavorites = if (state.workspace.settings.favoritesEnabled) viewModel::addFavorite else null,
                                onAreaResized = viewModel::onTranscriptAreaResized,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        // The top shade and the glow of a running command are part of what the glass
                        // sees, so a panel over them (the drawer) shows them through it, dimmed as
                        // it dims the rest; they stay under the top buttons.
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
                            running = current.running,
                            settings = state.workspace.settings,
                            modifier = Modifier.align(Alignment.TopCenter),
                        )
                    }
                }

                // Back to the newest output: in the send button's column, centred on it and the
                // same size, a fixed distance above the top of the panel (composer or search),
                // so a tall draft pushes it up rather than covering it. Placed from measured
                // positions, not by hand.
                Box(Modifier.zIndex(2f)) {
                    JumpDownButton(
                        visible = jumpAvailable && sendBounds != Rect.Zero && panelBounds != Rect.Zero,
                        centreX = sendBounds.center.x,
                        panelTop = panelBounds.top,
                        origin = screenOrigin,
                        onClick = viewModel::requestJumpToBottom,
                    )
                }

                // TopBar overlay — its internal menu scrim is now full-screen
                // (covers transcript + composer + key row) because TopBar
                // fills the outer Box, not just the transcript Box.
                TopBar(
                    modifier = Modifier.align(Alignment.TopCenter).zIndex(2f),
                    hasMessages = current?.blocks?.isNotEmpty() == true,
                    pinned = current?.pinned ?: false,
                    onOpenDrawer = { drawerState.open() },
                    onNewSession = viewModel::newSession,
                    onSearch = viewModel::enterSearch,
                    onRename = { current?.let { viewModel.requestRename(it.id) } },
                    onTogglePin = { current?.let { viewModel.togglePin(it.id) } },
                    onDelete = { current?.let { viewModel.requestDelete(it.id) } },
                )

                // Context menus (a long-pressed message) open in the screen, above everything.
                LocalContextMenu.current?.let { Box(Modifier.fillMaxSize().zIndex(2f)) { ContextMenuLayer(it) } }

                // The + panel grows up from the composer's top left corner.
                Box(Modifier.fillMaxSize().zIndex(2f)) {
                    AttachPanel(
                        open = attachOpen,
                        anchor = panelBounds,
                        origin = screenOrigin,
                        choices = listOf(
                            AttachChoice(Icons.Filled.ContentCopy, stringResource(R.string.attach_copy)) { pickFiles(Attachments.COPY) },
                            AttachChoice(Icons.Filled.InsertLink, stringResource(R.string.attach_link)) { pickFiles(Attachments.LINK) },
                            AttachChoice(Icons.Filled.TextFields, stringResource(R.string.attach_path)) { pickFiles(Attachments.PATH) },
                        ),
                        onDismiss = { attachOpen = false },
                    )
                }
            }
        }
    }

    // ---- Fullscreen overlays -------------------------------------------

    if (state.editorExpanded && current != null) {
        EditorOverlay(
            draft = current.draft,
            onDraftChange = viewModel::onComposerTextChange,
            selection = state.composerSelection,
            onSelectionChange = viewModel::setComposerSelection,
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
    onSendBounds: (Rect) -> Unit,
    onPanelBounds: (Rect) -> Unit,
    onAttach: () -> Unit,
    caretHidden: Boolean,
    modifier: Modifier = Modifier,
) {
    // One column aligned to the bottom edge: the composer sits directly above
    // the key row. Without this modifier both children fell to the top-start
    // corner of the screen.
    val keyRowShown = state.workspace.settings.keyRowVisible && state.workspace.search == null
    val current = state.workspace.sessions.firstOrNull { it.id == state.workspace.currentSessionId }
    // With the key row shown, the panel and the keys are one glass container:
    // rounded on top, flat at the bottom, one lens over both, so they refract as one surface.
    // The search has no key row under it: it floats on its own, like the composer without keys.
    val docked = keyRowShown
    // It spills just past the left, right and bottom screen edges, so its rim lies off screen there
    // and only the rounded top edge shows one. Only just: the further out, the less of the bend at
    // its ends is on screen.
    val container = if (docked) {
        Modifier
            .padding(top = 10.dp)
            .bleedPastEdges(DOCK_BLEED)
            .liquidGlass(composerBackdrop, DockedPanelShape, Ui.panelRadius, bottomCornerRadius = 0.dp)
            .padding(start = DOCK_BLEED, end = DOCK_BLEED, bottom = DOCK_BLEED)
    } else {
        Modifier
    }
    Column(modifier = modifier.then(container)) {
        InputBar(
            draft = draft,
            suggestion = suggestion,
            onDraftChange = viewModel::onComposerTextChange,
            onSend = viewModel::sendDraft,
            onExpandEditor = { viewModel.setEditorExpanded(true) },
            onAddClick = { onAttach() },
            search = state.workspace.search,
            onSearchQueryChange = viewModel::updateSearchQuery,
            onSearchNext = viewModel::nextSearchMatch,
            onSearchPrevious = viewModel::previousSearchMatch,
            onExitSearch = viewModel::exitSearch,
            enterSends = state.workspace.settings.enterSends,
            selection = state.composerSelection,
            onSelectionChange = viewModel::setComposerSelection,
            composerBackdrop = composerBackdrop,
            directory = current?.directory,
            running = current?.running == true,
            docked = keyRowShown,
            caretHidden = caretHidden,
            onSendBounds = onSendBounds,
            onPanelBounds = onPanelBounds,
        )
        if (keyRowShown) {
            // The line between the composer and the keys, like the dividers in the settings cards,
            // made of strong blur rather than a cut, so nothing behind it can be read.
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(DOCK_GAP)
                    .backdropGap(composerBackdrop),
            )
            val keys = remember(state.workspace.customKeys) {
                effectiveKeyRow(state.workspace.customKeys).map { it.toTerminalKey() }
            }
            var fnOn by remember { mutableStateOf(false) }
            KeyRow(
                keys = keys,
                fnOn = fnOn,
                onToggleFn = { fnOn = !fnOn },
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
                // The navigation bar's room is part of the row, so its separators reach the edge.
                // While the keyboard is up it covers the bar, and the screen already moved up.
                bottomTail = navigationBarTail(),
                rows = state.workspace.settings.keyRows,
                cols = state.workspace.settings.keyCols,
                glass = false,
            )
        }
    }
}

/** How long after one shell's end the ends of others go unannounced. */
private const val EXIT_TOAST_QUIET_MS = 5_000L

private val DOCK_BLEED = 2.dp
private val DOCK_GAP = 3.dp

/** Widens the element by [bleed] on the left, right and bottom without moving its content area in the layout. */
private fun Modifier.bleedPastEdges(bleed: Dp): Modifier = layout { measurable, constraints ->
    val extra = bleed.roundToPx()
    val placeable = measurable.measure(
        constraints.copy(
            minWidth = constraints.minWidth + extra * 2,
            maxWidth = if (constraints.hasBoundedWidth) constraints.maxWidth + extra * 2 else constraints.maxWidth,
        ),
    )
    layout(placeable.width - extra * 2, (placeable.height - extra).coerceAtLeast(0)) {
        placeable.place(-extra, 0)
    }
}

/** The navigation bar's height not already taken by the keyboard's inset. */
@Composable
private fun navigationBarTail(): Dp {
    val density = LocalDensity.current
    val bar = WindowInsets.navigationBars.getBottom(density)
    val keyboard = WindowInsets.ime.getBottom(density)
    return with(density) { (bar - keyboard).coerceAtLeast(0).toDp() }
}

/** Room between the jump-down button and the top of the panel below it. */
private val JUMP_GAP = 12.dp

@Composable
private fun JumpDownButton(visible: Boolean, centreX: Float, panelTop: Float, origin: Offset, onClick: () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(160)) + scaleIn(tween(200), initialScale = 0.7f),
        exit = fadeOut(tween(120)) + scaleOut(tween(160), targetScale = 0.7f),
        modifier = Modifier.offset {
            val size = Ui.sendCircleSize.roundToPx()
            IntOffset(
                (centreX - origin.x - size / 2f).roundToInt(),
                (panelTop - origin.y - JUMP_GAP.toPx() - size).roundToInt(),
            )
        },
    ) {
        val source = remember { MutableInteractionSource() }
        Box(
            Modifier
                .size(Ui.sendCircleSize)
                .clip(CircleShape)
                .liquidGlass(LocalLiquidBackdrop.current, CircleShape, Ui.sendCircleSize / 2)
                .background(pressWash(source))
                .clickable(interactionSource = source, indication = null, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = stringResource(R.string.cd_scroll_down),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

