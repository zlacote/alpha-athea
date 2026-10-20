package com.athea.app.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.athea.app.R
import com.athea.app.core.terminal.TerminalEngine
import com.athea.app.data.AtheaStorage
import com.athea.app.data.CustomKey
import com.athea.app.data.DEFAULT_SHELL_PATH
import com.athea.app.data.SshTarget
import com.athea.app.engine.NativeShellEngine
import com.athea.app.engine.SshIdentity
import com.athea.app.engine.SshShellEngine
import com.athea.app.engine.probeSsh
import com.athea.app.session.Attachments
import com.athea.app.session.CommandRecall
import com.athea.app.session.EngineFactory
import com.athea.app.session.Workspace
import com.athea.app.session.WorkspaceEvent
import com.athea.app.session.WorkspaceState
import com.athea.app.ui.main.CursorKey
import com.athea.app.ui.main.FORM_FEED
import com.athea.app.ui.main.effectiveKeyRow
import com.athea.app.util.AtheaLog
import com.athea.app.util.dropOldestSharedFlow
import com.athea.app.util.shellQuote
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
    /** Caret in the composer; -1 means the end of the draft. */
    val composerCaret: Int = -1,
    /** The setup page showing, if any: the first-run welcome, or the shell choice and Termux page. */
    val setup: SetupPage? = null,
    /** True when the setup was opened from Settings, so Back closes it instead of returning to the welcome. */
    val setupFromSettings: Boolean = false,
    /** This app's SSH public key, once it exists. */
    val sshPublicKey: String = "",
    /** Why the key could not be created, if it could not. */
    val sshKeyError: String? = null,
    val sshCheck: SshCheck = SshCheck.Idle,
    val showSettings: Boolean = false,
    val showFavorites: Boolean = false,
    val showAttachChooser: Boolean = false,
    val showKeys: Boolean = false,
    val showKeyBuilder: Boolean = false,
    val showGlassTuning: Boolean = false,
    val renameTargetId: Long? = null,
    val deleteTargetId: Long? = null,
    val selectTextPayload: String? = null,
)

/**
 * Android face of [Workspace]. Forwards user intents to the core, owns the
 * presentation-only flags (dialogs, overlays, sticky modifier) and mirrors
 * the workspace into [state] for Compose.
 */
/** The shell setup pages: the first-run welcome, the choice of shell, and the Termux connection. */
enum class SetupPage { WELCOME, SHELL, TERMUX }

/** The state of the check that the Termux server accepts this app's key. */
sealed interface SshCheck {
    data object Idle : SshCheck

    data object Checking : SshCheck

    data class Ok(val target: SshTarget) : SshCheck

    data class Failed(val message: String) : SshCheck
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val storage = AtheaStorage(application.filesDir)

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
        if (storage.needsOnboarding()) _state.update { it.copy(setup = SetupPage.WELCOME) }
        viewModelScope.launch {
            workspace.state.collect { projection -> _state.update { it.copy(workspace = projection) } }
        }
    }

    private fun createEngine(app: Application, shellPath: String): TerminalEngine {
        val ssh = SshTarget.fromShellPath(shellPath)
        return if (ssh != null) {
            SshShellEngine(ssh, SshIdentity.privateKey(app.filesDir))
        } else {
            NativeShellEngine(
                homeDir = storage.shellHome().absolutePath,
                rcPath = storage.ensureShellRc(readShellAsset(app)).absolutePath,
                shellPath = shellPath,
            )
        }
    }

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
        _state.update { it.copy(composerCaret = -1) }
    }

    fun acceptSuggestion() {
        workspace.acceptSuggestion()
        _state.update { it.copy(composerCaret = -1) }
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

    fun setComposerCaret(caret: Int) = _state.update { it.copy(composerCaret = caret) }

    private fun recallCommand(older: Boolean) {
        val state = _state.value.workspace
        val session = state.sessions.firstOrNull { it.id == state.currentSessionId } ?: return
        val text = recall.step(workspace.commandHistory(), session.draft.orEmpty(), older) ?: return
        workspace.updateDraft(text)
        _state.update { it.copy(composerCaret = -1) }
    }

    private fun moveCaret(delta: Int) {
        val state = _state.value.workspace
        val draft = state.sessions.firstOrNull { it.id == state.currentSessionId }?.draft.orEmpty()
        val caret = _state.value.composerCaret.takeIf { it >= 0 } ?: draft.length
        _state.update { it.copy(composerCaret = (caret + delta).coerceIn(0, draft.length)) }
    }

    fun onTranscriptAreaResized(rows: Int, cols: Int) = workspace.onAreaResized(rows, cols)

    fun toggleStickyCtrl() = _state.update { it.copy(stickyCtrl = !it.stickyCtrl) }

    fun consumeStickyCtrl() = _state.update { it.copy(stickyCtrl = false) }

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

                AtheaLog.log("attach", "imported $safeName action=$action")
                withContext(Dispatchers.Main) {
                    if (action == Attachments.PATH) {
                        workspace.placePath(target.absolutePath.shellQuote())
                    } else {
                        workspace.placeCommand(Attachments.command(action, target.absolutePath, safeName))
                    }
                }
            } catch (e: Exception) {
                AtheaLog.error("attach", "import failed", e)
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

    // ------------------------------------------------------------------ setup

    /** Opens a setup page. [fromSettings] makes Back close the flow instead of returning to the welcome. */
    fun openSetup(page: SetupPage, fromSettings: Boolean = false) =
        _state.update { it.copy(setup = page, setupFromSettings = fromSettings, sshCheck = SshCheck.Idle) }

    /** Back on the Termux page returns to the shell choice; on the choice it returns to the welcome, or closes. */
    fun backSetup() {
        val state = _state.value
        when (state.setup) {
            SetupPage.TERMUX -> openSetup(SetupPage.SHELL, state.setupFromSettings)
            SetupPage.SHELL -> if (state.setupFromSettings) finishSetup() else openSetup(SetupPage.WELCOME)
            SetupPage.WELCOME, null -> Unit
        }
    }

    /** The built-in shell needs nothing installed, so the setup ends here. */
    fun chooseLocalShell() {
        workspace.updateSettings { it.copy(shellPath = DEFAULT_SHELL_PATH) }
        finishSetup()
    }

    /** Opens the Termux page, with this app's key created and ready to show. */
    fun openTermuxSetup() {
        openSetup(SetupPage.TERMUX, _state.value.setupFromSettings)
        viewModelScope.launch(Dispatchers.IO) {
            // Anything thrown here would end the app, so the failure is shown on the page instead.
            val result = runCatching { SshIdentity.publicKeyLine(getApplication<Application>().filesDir) }
            result.onFailure { AtheaLog.error("ssh", "could not create the key", it) }
            _state.update {
                it.copy(
                    sshPublicKey = result.getOrDefault(""),
                    sshKeyError = result.exceptionOrNull()?.let { error -> error.message ?: error.javaClass.simpleName },
                )
            }
        }
    }

    /** Connects to Termux on this phone as [user] on [port], with this app's key. */
    fun checkTermux(user: String, port: Int) {
        val target = SshTarget(user.trim(), LOOPBACK, port)
        _state.update { it.copy(sshCheck = SshCheck.Checking) }
        val identity = SshIdentity.privateKey(getApplication<Application>().filesDir)
        viewModelScope.launch(Dispatchers.IO) {
            val failure = probeSsh(target, identity)
            _state.update {
                it.copy(sshCheck = if (failure == null) SshCheck.Ok(target) else SshCheck.Failed(failure))
            }
        }
    }

    /** Makes Termux the shell of new sessions, and opens one. */
    fun useTermux(user: String, port: Int) {
        val target = SshTarget(user.trim(), LOOPBACK, port)
        workspace.updateSettings { it.copy(shellPath = target.toShellPath()) }
        workspace.newSession()
        finishSetup()
    }

    private fun finishSetup() {
        storage.markOnboarded()
        _state.update { it.copy(setup = null, setupFromSettings = false, sshCheck = SshCheck.Idle) }
    }

    fun setKeyRowVisible(visible: Boolean) = workspace.updateSettings { it.copy(keyRowVisible = visible) }

    fun setAttachSendsNow(value: Boolean) = workspace.updateSettings { it.copy(attachSendsNow = value) }

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

        /** Termux listens on the phone itself; the setup never targets another host. */
        const val LOOPBACK = "127.0.0.1"
    }

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = MainViewModel(app) as T
    }
}
