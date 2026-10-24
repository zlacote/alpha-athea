package iatheiya.ayana.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.compose.ui.text.TextRange
import iatheiya.ayana.R
import iatheiya.ayana.core.terminal.TerminalEngine
import iatheiya.ayana.data.AyanaStorage
import iatheiya.ayana.data.CustomKey
import iatheiya.ayana.engine.NativeShellEngine
import iatheiya.ayana.session.Attachments
import iatheiya.ayana.session.CommandRecall
import iatheiya.ayana.session.EngineFactory
import iatheiya.ayana.session.Workspace
import iatheiya.ayana.session.WorkspaceEvent
import iatheiya.ayana.util.isCtrlCombinable
import iatheiya.ayana.util.toControlChar
import iatheiya.ayana.session.WorkspaceState
import iatheiya.ayana.ui.main.CursorKey
import iatheiya.ayana.ui.main.FORM_FEED
import iatheiya.ayana.ui.main.effectiveKeyRow
import iatheiya.ayana.util.AyanaLog
import iatheiya.ayana.util.dropOldestSharedFlow
import iatheiya.ayana.util.shellQuote
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Everything the main screen renders: the workspace projection plus the
 * presentation-only flags that never leave the view model.
 */
data class UiState(
    val workspace: WorkspaceState = WorkspaceState(),
    val editorExpanded: Boolean = false,
    val stickyCtrl: Boolean = false,
    val stickyAlt: Boolean = false,
    /** Selection in the composer; null means a caret at the end of the draft. */
    val composerSelection: TextRange? = null,
    val showSettings: Boolean = false,
    val showFavorites: Boolean = false,
    val showAttachChooser: Boolean = false,
    val showKeys: Boolean = false,
    val showKeyBuilder: Boolean = false,
    val showGlassTuning: Boolean = false,
    val showGlow: Boolean = false,
    val renameTargetId: Long? = null,
    val deleteTargetId: Long? = null,
    val selectTextPayload: String? = null,
)

/**
 * Android face of [Workspace]. Forwards user intents to the core, owns the
 * presentation-only flags (dialogs, overlays, sticky modifier) and mirrors
 * the workspace into [state] for Compose.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val storage = AyanaStorage(application.filesDir)

    private val workspace = Workspace(
        storage = storage,
        scope = viewModelScope,
        engines = EngineFactory { shellPath -> createEngine(application, shellPath) },
        defaultSessionName = { number -> application.getString(R.string.default_session_name, number) },
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    /** Walks the commands sent in a session with the up and down keys. */
    private val recall = CommandRecall()

    val events: Flow<WorkspaceEvent> = workspace.events

    /** One-shot requests to scroll the transcript to a block (search jumps). */
    private val _scrollRequests = dropOldestSharedFlow<String>(extraBufferCapacity = 8)
    val scrollRequests: SharedFlow<String> = _scrollRequests.asSharedFlow()

    /** One-shot requests to force the transcript to its very bottom. */
    private val _jumpToBottom = dropOldestSharedFlow<Unit>(extraBufferCapacity = 4)
    val jumpToBottom: SharedFlow<Unit> = _jumpToBottom.asSharedFlow()

    init {
        viewModelScope.launch {
            workspace.state.collect { projection -> _state.update { it.copy(workspace = projection) } }
        }
    }

    private fun createEngine(app: Application, shellPath: String): TerminalEngine =
        NativeShellEngine(
            homeDir = storage.shellHome().absolutePath,
            rcPath = storage.ensureShellRc(readShellAsset(app)).absolutePath,
            shellPath = shellPath,
        )

    private fun readShellAsset(app: Application): ByteArray =
        runCatching {
            app.assets.open(SHELL_ASSET).use { it.readBytes() }
        }.getOrDefault(ByteArray(0))

    // ----------------------------------------------------------------- sessions

    fun newSession() {
        recall.reset()
        workspace.newSession()
    }

    fun selectSession(id: Long) {
        recall.reset()
        workspace.selectSession(id)
    }

    fun requestRename(id: Long) = _state.update { it.copy(renameTargetId = id) }

    fun cancelRename() = _state.update { it.copy(renameTargetId = null) }

    fun renameSession(id: Long, name: String) {
        workspace.renameSession(id, name)
        _state.update { it.copy(renameTargetId = null) }
    }

    fun togglePin(id: Long) = workspace.togglePin(id)

    /** Deleting a session with a running command asks first. */
    fun requestDelete(id: Long) {
        val running = _state.value.workspace.sessions.any { it.id == id && it.running }
        if (running) {
            _state.update { it.copy(deleteTargetId = id) }
        } else {
            workspace.deleteSession(id)
        }
    }

    fun confirmDelete() {
        val id = _state.value.deleteTargetId ?: return
        _state.update { it.copy(deleteTargetId = null) }
        workspace.deleteSession(id)
    }

    fun cancelDelete() = _state.update { it.copy(deleteTargetId = null) }

    // ------------------------------------------------------------------- input

    fun updateDraft(text: String) {
        recall.reset()
        workspace.updateDraft(text)
    }

    fun sendDraft() {
        recall.reset()
        if (workspace.sendDraft()) jumpIfEnabled()
    }

    fun setEditorExpanded(expanded: Boolean) = _state.update { it.copy(editorExpanded = expanded) }

    fun sendFromExpandedEditor() {
        sendDraft()
        _state.update { it.copy(editorExpanded = false) }
    }

    /** Runs a favourite or key payload straight to the shell. */
    fun executeDirectly(command: String) {
        if (workspace.executeDirectly(command)) jumpIfEnabled()
    }

    private fun jumpIfEnabled() {
        if (workspace.state.value.settings.autoScrollOnSend) _jumpToBottom.tryEmit(Unit)
    }

    fun insertIntoDraft(text: String) {
        workspace.insertIntoDraft(text)
        _state.update { it.copy(composerSelection = null) }
    }

    fun acceptSuggestion() {
        workspace.acceptSuggestion()
        _state.update { it.copy(composerSelection = null) }
    }

    /** Raw bytes to the shell. Ctrl+L clears the screen while no command runs. */
    fun sendDirectText(payload: String) {
        val state = _state.value.workspace
        val running = state.sessions.firstOrNull { it.id == state.currentSessionId }?.running == true
        if (payload == FORM_FEED && !running) workspace.clearScreen() else workspace.sendRaw(payload)
    }

    /**
     * The cursor keys. A running program gets their bytes. An idle prompt uses them to walk the
     * sent commands (up and down) and to move the caret in the composer (left and right).
     */
    fun pressCursorKey(key: CursorKey) {
        val state = _state.value.workspace
        val session = state.sessions.firstOrNull { it.id == state.currentSessionId } ?: return
        if (session.running) {
            sendDirectText(key.sequence)
            return
        }
        when (key) {
            CursorKey.UP -> recallCommand(older = true)
            CursorKey.DOWN -> recallCommand(older = false)
            CursorKey.LEFT -> moveCaret(-1)
            CursorKey.RIGHT -> moveCaret(1)
        }
    }

    fun setComposerSelection(selection: TextRange) = _state.update { it.copy(composerSelection = selection) }

    private fun recallCommand(older: Boolean) {
        val state = _state.value.workspace
        val session = state.sessions.firstOrNull { it.id == state.currentSessionId } ?: return
        val text = recall.step(workspace.commandHistory(), session.draft.orEmpty(), older) ?: return
        workspace.updateDraft(text)
        _state.update { it.copy(composerSelection = null) }
    }

    private fun moveCaret(delta: Int) {
        val state = _state.value.workspace
        val draft = state.sessions.firstOrNull { it.id == state.currentSessionId }?.draft.orEmpty()
        val caret = _state.value.composerSelection?.end ?: draft.length
        _state.update { it.copy(composerSelection = TextRange((caret + delta).coerceIn(0, draft.length))) }
    }

    fun onTranscriptAreaResized(rows: Int, cols: Int) = workspace.onAreaResized(rows, cols)

    fun toggleStickyCtrl() = _state.update { it.copy(stickyCtrl = !it.stickyCtrl) }

    fun consumeStickyCtrl() = _state.update { it.copy(stickyCtrl = false) }

    fun toggleStickyAlt() = _state.update { it.copy(stickyAlt = !it.stickyAlt) }

    fun consumeStickyAlt() = _state.update { it.copy(stickyAlt = false) }

    /**
     * The composer's text changed. With Ctrl or Alt armed, a single new character is
     * taken as a modifier combination and sent to the terminal instead of entering the
     * draft: Ctrl folds a letter to its control char, Alt prefixes ESC. Otherwise the
     * change is an ordinary draft edit.
     */
    fun onComposerTextChange(new: String) {
        val st = _state.value
        val old = currentDraft()
        if ((st.stickyCtrl || st.stickyAlt) && new.length == old.length + 1) {
            val at = old.commonPrefixWith(new).length
            val ch = new[at]
            val combinable = ch.isCtrlCombinable()
            val bytes = when {
                st.stickyCtrl && combinable -> {
                    val control = ch.toControlChar().toString()
                    if (st.stickyAlt) "\u001B" + control else control
                }
                st.stickyAlt -> "\u001B" + ch
                else -> null
            }
            if (bytes != null) {
                sendDirectText(bytes)
                if (st.stickyCtrl && combinable) consumeStickyCtrl()
                if (st.stickyAlt) consumeStickyAlt()
                return
            }
        }
        updateDraft(new)
    }

    private fun currentDraft(): String {
        val ws = _state.value.workspace
        return ws.sessions.firstOrNull { it.id == ws.currentSessionId }?.draft.orEmpty()
    }

    // ------------------------------------------------------------ attachments

    /**
     * Copies a picked file into the app's attachment folder, then puts the command or the path
     * that uses it into the composer, or runs the command when the setting says so.
     */
    fun importAttachment(uri: Uri, action: String, context: Context) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val resolver = context.contentResolver
                val name = runCatching {
                    resolver.query(
                        uri,
                        arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
                        null,
                        null,
                        null,
                    )?.use { cursor ->
                        val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (cursor.moveToFirst() && idx >= 0) cursor.getString(idx) else null
                    }
                }.getOrNull() ?: "file"

                val safeName = Attachments.safeName(name)
                val dir = File(context.filesDir, "attachments").apply { mkdirs() }
                val target = File(dir, "${System.currentTimeMillis()}_$safeName")
                resolver.openInputStream(uri)?.use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                } ?: throw IllegalStateException("cannot open $uri")

                AyanaLog.log("attach", "imported $safeName action=$action")
                withContext(Dispatchers.Main) {
                    if (action == Attachments.PATH) {
                        workspace.placePath(target.absolutePath.shellQuote())
                    } else {
                        workspace.placeCommand(Attachments.command(action, target.absolutePath, safeName))
                    }
                }
            } catch (e: Exception) {
                AyanaLog.error("attach", "import failed", e)
            }
        }
    }


    // ------------------------------------------------------------------ favourites

    fun addFavorite(text: String) = workspace.addFavorite(text)

    fun updateFavorite(id: Long, text: String) = workspace.updateFavorite(id, text)

    fun deleteFavorite(id: Long) = workspace.deleteFavorite(id)

    fun setShowFavorites(visible: Boolean) = _state.update { it.copy(showFavorites = visible) }

    fun setShowAttachChooser(visible: Boolean) = _state.update { it.copy(showAttachChooser = visible) }

    fun setShowKeys(visible: Boolean) = _state.update { it.copy(showKeys = visible) }

    fun setShowKeyBuilder(visible: Boolean) = _state.update { it.copy(showKeyBuilder = visible) }

    fun addCustomKey(key: CustomKey) = workspace.setCustomKeys(effectiveKeyRow(workspace.state.value.customKeys) + key)

    /** Replaces the whole key row (used by the settings editor). */
    fun setCustomKeys(keys: List<CustomKey>) = workspace.setCustomKeys(keys)

    fun resetKeysToDefaults() = workspace.setCustomKeys(emptyList())

    // ---------------------------------------------------------------- search

    fun enterSearch() = workspace.enterSearch()

    fun exitSearch() = workspace.exitSearch()

    fun updateSearchQuery(query: String) {
        workspace.setSearchQuery(query)?.let { _scrollRequests.tryEmit(it) }
    }

    fun nextSearchMatch() {
        workspace.nextSearchMatch()?.let { _scrollRequests.tryEmit(it) }
    }

    // ------------------------------------------------------ block interactions

    fun toggleBlockCollapsed(blockId: String) = workspace.toggleBlock(blockId)

    /** Expands a block without collapsing anything (search navigation). */
    fun revealBlock(blockId: String) = workspace.revealBlock(blockId)

    fun showSelectText(text: String) = _state.update { it.copy(selectTextPayload = text) }

    fun dismissSelectText() = _state.update { it.copy(selectTextPayload = null) }

    // ------------------------------------------------------------- settings

    fun setShowSettings(visible: Boolean) = _state.update { it.copy(showSettings = visible) }

    fun setShowGlassTuning(visible: Boolean) = _state.update { it.copy(showGlassTuning = visible) }

    fun setShowGlow(visible: Boolean) = _state.update { it.copy(showGlow = visible) }

    fun setKeyRowVisible(visible: Boolean) = workspace.updateSettings { it.copy(keyRowVisible = visible) }

    fun setKeyRows(rows: Int) = workspace.updateSettings { it.copy(keyRows = rows.coerceIn(1, 4)) }

    fun setKeyCols(cols: Int) = workspace.updateSettings { it.copy(keyCols = cols.coerceIn(3, 10)) }

    fun setAttachSendsNow(value: Boolean) = workspace.updateSettings { it.copy(attachSendsNow = value) }

    fun setFavoriteReturnsHome(value: Boolean) =
        workspace.updateSettings { it.copy(favoriteReturnsHome = value) }

    fun setFavoriteSendsImmediately(value: Boolean) =
        workspace.updateSettings { it.copy(favoriteSendsImmediately = value) }

    fun setGlowEnabled(value: Boolean) = workspace.updateSettings { it.copy(glowEnabled = value) }

    fun setGlowStrengthPct(value: Int) = workspace.updateSettings { it.copy(glowStrengthPct = value) }

    fun setGlowHeightDp(value: Int) = workspace.updateSettings { it.copy(glowHeightDp = value) }

    fun setGlowWidthPct(value: Int) = workspace.updateSettings { it.copy(glowWidthPct = value) }

    fun setGlowPeriodMs(value: Int) = workspace.updateSettings { it.copy(glowPeriodMs = value) }

    fun setEnterSends(value: Boolean) = workspace.updateSettings { it.copy(enterSends = value) }

    fun setAutoScrollOnSend(value: Boolean) = workspace.updateSettings { it.copy(autoScrollOnSend = value) }

    /** Global transcript view: raw stream instead of chat blocks. */
    fun setRawStream(enabled: Boolean) = workspace.updateSettings { it.copy(rawStream = enabled) }

    fun setAutocompleteEnabled(value: Boolean) = workspace.updateSettings { it.copy(autocompleteEnabled = value) }

    fun setPinchZoomEnabled(value: Boolean) = workspace.updateSettings { it.copy(pinchZoomEnabled = value) }

    fun setVirtualizeLargeOutput(value: Boolean) =
        workspace.updateSettings { it.copy(virtualizeLargeOutput = value) }

    fun setOutputFontSize(sizeSp: Int) = workspace.updateSettings { it.copy(outputFontSizeSp = sizeSp) }

    fun setPreviewLines(lines: Int) = workspace.updateSettings { it.copy(previewLines = lines) }

    fun setBubbleFontSize(sizeSp: Int) = workspace.updateSettings { it.copy(bubbleFontSizeSp = sizeSp) }

    /** Pinch on the transcript: scale the output font live. */
    fun onOutputFontZoom(factor: Float) {
        if (!workspace.state.value.settings.pinchZoomEnabled) return
        workspace.updateSettings { it.copy(outputFontSizeSp = (it.outputFontSizeSp * factor).toInt()) }
    }

    // ----------------------------------------------------------- lifecycle

    /** Persists metadata (names, drafts, modes); called from onPause too. */
    fun persistTransientState() = workspace.persistTransientState()

    override fun onCleared() {
        workspace.close()
        super.onCleared()
    }

    private companion object {
        const val SHELL_ASSET = "mkshrc"
    }

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = MainViewModel(app) as T
    }
}
