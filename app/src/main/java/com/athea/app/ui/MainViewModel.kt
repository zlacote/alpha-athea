package com.athea.app.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.athea.app.R
import com.athea.app.core.model.Attachment
import com.athea.app.core.terminal.TerminalEngine
import com.athea.app.data.AtheaStorage
import com.athea.app.data.CustomKey
import com.athea.app.engine.NativeShellEngine
import com.athea.app.engine.SshShellEngine
import com.athea.app.session.Attachments
import com.athea.app.session.EngineFactory
import com.athea.app.session.Workspace
import com.athea.app.session.WorkspaceEvent
import com.athea.app.session.WorkspaceState
import com.athea.app.util.AtheaLog
import com.athea.app.util.dropOldestSharedFlow
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Everything the main screen renders: the workspace projection plus the
 * presentation-only flags that never leave the view model.
 */
data class UiState(
    val workspace: WorkspaceState = WorkspaceState(),
    val editorExpanded: Boolean = false,
    val stickyCtrl: Boolean = false,
    val showSettings: Boolean = false,
    val showFavorites: Boolean = false,
    val showAttachChooser: Boolean = false,
    val showKeyBuilder: Boolean = false,
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

    private val storage = AtheaStorage(application.filesDir)

    private val workspace = Workspace(
        storage = storage,
        scope = viewModelScope,
        engines = EngineFactory { shellPath -> createEngine(application, shellPath) },
        defaultSessionName = { number -> application.getString(R.string.default_session_name, number) },
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

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
        if (shellPath.startsWith("ssh://")) {
            SshShellEngine(shellPath)
        } else {
            NativeShellEngine(
                homeDir = storage.shellHome().absolutePath,
                rcPath = storage.ensureShellRc(readShellAsset(app)).absolutePath,
                shellPath = shellPath,
            )
        }

    private fun readShellAsset(app: Application): ByteArray =
        runCatching {
            app.assets.open(SHELL_ASSET).use { it.readBytes() }
        }.getOrDefault(ByteArray(0))

    // ----------------------------------------------------------------- sessions

    fun newSession() = workspace.newSession()

    fun selectSession(id: Long) = workspace.selectSession(id)

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

    fun updateDraft(text: String) = workspace.updateDraft(text)

    fun sendDraft() {
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

    fun insertIntoDraft(text: String) = workspace.insertIntoDraft(text)

    fun acceptSuggestion() = workspace.acceptSuggestion()

    fun sendDirectText(payload: String) = workspace.sendRaw(payload)

    fun onTranscriptAreaResized(rows: Int, cols: Int) = workspace.onAreaResized(rows, cols)

    fun toggleStickyCtrl() = _state.update { it.copy(stickyCtrl = !it.stickyCtrl) }

    fun consumeStickyCtrl() = _state.update { it.copy(stickyCtrl = false) }

    // ------------------------------------------------------------ attachments

    /**
     * Copies a picked file into the app attachment folder and stages the
     * command that places it, to run with the next submission.
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
                workspace.stageAttachment(
                    Attachment(
                        name = safeName,
                        command = Attachments.command(action, target.absolutePath, safeName),
                    ),
                )
            } catch (e: Exception) {
                AtheaLog.error("attach", "import failed", e)
            }
        }
    }

    fun removeAttachment(attachment: Attachment) = workspace.removeAttachment(attachment)

    // ------------------------------------------------------------------ favourites

    fun addFavorite(text: String) = workspace.addFavorite(text)

    fun updateFavorite(id: Long, text: String) = workspace.updateFavorite(id, text)

    fun deleteFavorite(id: Long) = workspace.deleteFavorite(id)

    fun setShowFavorites(visible: Boolean) = _state.update { it.copy(showFavorites = visible) }

    fun setShowAttachChooser(visible: Boolean) = _state.update { it.copy(showAttachChooser = visible) }

    fun setShowKeyBuilder(visible: Boolean) = _state.update { it.copy(showKeyBuilder = visible) }

    fun addCustomKey(key: CustomKey) = workspace.setCustomKeys(workspace.state.value.customKeys + key)

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

    fun setKeyRowVisible(visible: Boolean) = workspace.updateSettings { it.copy(keyRowVisible = visible) }

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
