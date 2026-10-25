package iatheiya.ayana.session

import iatheiya.ayana.core.journal.JournalEvent
import iatheiya.ayana.core.journal.SessionJournal
import iatheiya.ayana.core.model.FavoriteCommand
import iatheiya.ayana.core.terminal.EngineEvent
import iatheiya.ayana.core.terminal.TerminalEngine
import iatheiya.ayana.data.AyanaSettings
import iatheiya.ayana.data.AyanaStorage
import iatheiya.ayana.data.CustomKey
import iatheiya.ayana.data.FavoritesIndex
import iatheiya.ayana.data.KeysIndex
import iatheiya.ayana.data.SessionMeta
import iatheiya.ayana.data.SessionsIndex
import iatheiya.ayana.parse.StreamParser
import iatheiya.ayana.parse.applyTo
import iatheiya.ayana.screen.TerminalScreen
import iatheiya.ayana.transcript.TranscriptBuilder
import iatheiya.ayana.transcript.TranscriptSearch
import iatheiya.ayana.util.AyanaLog
import iatheiya.ayana.util.normalizeCommand
import iatheiya.ayana.util.shellEval
import iatheiya.ayana.util.trimCommand
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Owns live sessions end-to-end: transcripts, journals, terminal backends,
 * favourites, custom keys, settings and drafts. Platform-free: the Android
 * view model forwards user intents here and renders [state].
 *
 * History survives restarts: journals are replayed into fresh transcripts at
 * startup, then live shells are attached behind the restored sessions.
 */
class Workspace(
    private val storage: AyanaStorage,
    private val scope: CoroutineScope,
    private val engines: EngineFactory,
    private val defaultSessionName: (Int) -> String,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {

    private val _state = MutableStateFlow(WorkspaceState())
    val state: StateFlow<WorkspaceState> = _state.asStateFlow()

    /**
     * One-shot notifications. Each event is delivered to exactly one collector
     * and waits in the queue until someone collects it, so a rotation does not
     * replay an old toast.
     */
    private val eventQueue = Channel<WorkspaceEvent>(
        capacity = EVENT_QUEUE_CAPACITY,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: Flow<WorkspaceEvent> = eventQueue.receiveAsFlow()

    /** Guards every multi-step session mutation against concurrent engine events. */
    private val lock = Any()

    private val metas = LinkedHashMap<Long, SessionMeta>()
    private val pipes = LinkedHashMap<Long, SessionPipe>()
    private val backends = HashMap<Long, TerminalEngine>()
    private val backendJobs = HashMap<Long, Job>()
    private val rawQueues = HashMap<Long, Channel<EngineEvent>>()
    private val nextCommandSeqs = HashMap<Long, Long>()

    /** Submitted command texts per session, newest last (autocomplete source). */
    private val histories = HashMap<Long, MutableList<String>>()

    /** Sessions with pending transcript refreshes, drained at a fixed rate. */
    private val dirtySessions = ConcurrentHashMap.newKeySet<Long>()

    private var nextSessionIdValue = 1L
    private var favoriteCounter = 0L

    // Both must be declared above init: restore() projects the stored sessions from inside the
    // constructor, and a property declared further down is still unset at that point (a lazy one
    // included, its delegate being a field too). That was a crash on every start with saved data.

    /** The shell's home, resolved through symlinks, for the `~` in the prompt. */
    private val canonicalHome: String =
        runCatching { storage.shellHome().canonicalPath }.getOrDefault(storage.shellHome().absolutePath).trimEnd('/')

    /** Resolving a path touches the disk, and the projection is rebuilt often: each path is resolved once. */
    private val shownDirectories = ConcurrentHashMap<String, String>()

    init {
        restore()
        // Throttled refresh: heavy output produces many batches per second;
        // without throttling each one would trigger a full recomposition.
        scope.launch(dispatcher) {
            while (isActive) {
                delay(REFRESH_INTERVAL_MS)
                if (dirtySessions.isEmpty()) continue
                val toRefresh = dirtySessions.toList()
                dirtySessions.removeAll(toRefresh)
                synchronized(lock) {
                    for (id in toRefresh) refreshSession(id)
                }
            }
        }
    }

    // ----------------------------------------------------------- restoration

    private fun restore() {
        val index = storage.loadIndex()
        val settings = storage.loadSettings()
        val favorites = storage.loadFavorites()
        val keys = storage.loadKeys()
        _state.update {
            it.copy(settings = settings, favorites = favorites.items, customKeys = keys.items)
        }
        favoriteCounter = favorites.nextFavoriteId - 1

        val restored = index.items.map { meta ->
            val journal = storage.journalFor(meta.id)
            val events = journal.readAll()
            val commands = events.filterIsInstance<JournalEvent.CommandSubmitted>()
            RestoredSession(
                meta = meta,
                pipe = SessionPipe(
                    journal = journal,
                    parser = StreamParser(),
                    builder = TranscriptBuilder.replay(events, previewLines = settings.previewLines),
                ),
                history = commands.map { it.text }.toMutableList(),
                nextSeq = (commands.maxOfOrNull { it.seq } ?: 0L) + 1,
            )
        }
        synchronized(lock) {
            for (session in restored) {
                val id = session.meta.id
                metas[id] = session.meta
                pipes[id] = session.pipe
                histories[id] = session.history
                nextCommandSeqs[id] = session.nextSeq
                attachEngine(id, settings.shellPath)
                refreshSession(id)
            }
            nextSessionIdValue = maxOf(index.nextSessionId, (metas.keys.maxOrNull() ?: 0L) + 1)
            if (pipes.isEmpty()) {
                createSessionLocked()
            } else {
                _state.update { it.copy(currentSessionId = pipes.keys.first()) }
            }
        }
    }

    // ---------------------------------------------------------- engine events

    private fun attachEngine(id: Long, shellPath: String) {
        val engine = engines.create(shellPath)
        if (!engine.start(INITIAL_ROWS, INITIAL_COLS)) {
            // The session still appears; submissions to it will be dropped.
            eventQueue.trySend(WorkspaceEvent.ShellStartFailed)
            return
        }
        backends[id] = engine
        if (!engine.reportsCommandEnds) {
            pipes[id]?.screen = TerminalScreen(INITIAL_ROWS, INITIAL_COLS)
        }
        // The collector only forwards into an unbounded queue, which can never
        // drop. A batch worker drains everything queued per iteration: one lock
        // acquisition, one journal append and one refresh per burst.
        val queue = Channel<EngineEvent>(Channel.UNLIMITED)
        rawQueues[id] = queue
        scope.launch(dispatcher) {
            engine.events.collect { queue.send(it) }
        }
        backendJobs[id] = scope.launch(dispatcher) {
            while (isActive) {
                val batch = ArrayList<EngineEvent>(8)
                batch.add(queue.receive())
                while (true) batch.add(queue.tryReceive().getOrNull() ?: break)
                // One dead event must never kill the pipeline forever.
                try {
                    processBatch(id, batch)
                } catch (e: Exception) {
                    AyanaLog.error("pipeline", "processBatch failed", e)
                }
            }
        }
    }

    private fun processBatch(id: Long, batch: List<EngineEvent>) {
        // Pure CPU and disk work, outside the lock. The pipe is removed only in
        // deleteSession, which cancels this worker first.
        val pipe = pipes[id] ?: return
        var shellExited = false
        var exitCode = -1
        val output = ByteArrayOutputStream()
        for (event in batch) {
            when (event) {
                is EngineEvent.Output -> output.write(event.data)
                is EngineEvent.Exited -> {
                    shellExited = true
                    exitCode = event.exitCode
                }
            }
        }
        val bytes = output.toByteArray().takeIf { it.isNotEmpty() }
        val screen = pipe.screen
        val wasFullScreen = screen?.altActive == true
        bytes?.let { screen?.feed(it) }
        val isFullScreen = screen?.altActive == true
        if (wasFullScreen != isFullScreen) AyanaLog.log("screen", "full screen ${if (isFullScreen) "on" else "off"}")
        // A full-screen program's output is shown as its screen, so it is not journaled as transcript text.
        val parsed = if (bytes == null || screen?.altActive == true) {
            emptyList()
        } else {
            // The journal and the parser are per-session with a single writer each.
            pipe.journal.append(JournalEvent.OutputArrived(bytes))
            pipe.parser.feed(bytes)
        }

        synchronized(lock) {
            if (pipes[id] == null) return // deleted while parsing
            for (event in parsed) event.applyTo(pipe.builder)
            if (shellExited) {
                pipe.builder.applyCommandEnd(null)
                eventQueue.trySend(WorkspaceEvent.ShellExited(exitCode))
            }
            dirtySessions.add(id)
        }
    }

    // ---------------------------------------------------------------- sessions

    fun newSession() {
        synchronized(lock) { createSessionLocked() }
    }

    fun selectSession(id: Long) {
        _state.update { it.copy(currentSessionId = id, search = null) }
    }

    fun renameSession(id: Long, name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        mutateMeta(id) { it.copy(name = trimmed) }
    }

    fun togglePin(id: Long) = mutateMeta(id) { it.copy(pinned = !it.pinned) }

    fun deleteSession(id: Long) {
        synchronized(lock) {
            if (id !in pipes) return
            // Pick the successor from the order before removal.
            val order = pipes.keys.toList()
            val current = _state.value.currentSessionId
            val successor = if (current == id) neighbourOf(order, id) else current
            backends.remove(id)?.terminate()
            rawQueues.remove(id)?.cancel()
            backendJobs.remove(id)?.cancel()
            metas.remove(id)
            pipes.remove(id)?.journal?.close()
            nextCommandSeqs.remove(id)
            histories.remove(id)
            storage.deleteSession(id)
            if (pipes.isEmpty()) {
                createSessionLocked()
            } else {
                persistIndexLocked()
                _state.update { it.copy(currentSessionId = successor, search = null) }
            }
            rebuildAllSessions()
        }
    }

    private fun neighbourOf(order: List<Long>, id: Long): Long? {
        val index = order.indexOf(id)
        return order.getOrNull(index + 1) ?: order.getOrNull(index - 1)
    }

    // ------------------------------------------------------------------ input

    fun updateDraft(text: String) {
        val id = _state.value.currentSessionId ?: return
        synchronized(lock) {
            val current = metas[id] ?: return
            // In memory only: a disk write per keystroke is visible as paste jank.
            // Drafts persist on pause, on send and on session switch instead.
            // Only the draft changes: the transcript keeps its block list, so the
            // transcript view does not redo its layout work on every key press.
            metas[id] = current.copy(draft = text)
            _state.update { st ->
                st.copy(sessions = st.sessions.map { if (it.id == id) it.copy(draft = text) else it })
            }
        }
        updateSuggestion(id, text)
    }

    /** Submits the draft. Returns false when there was nothing to send. */
    fun sendDraft(): Boolean {
        val id = _state.value.currentSessionId ?: return false
        synchronized(lock) {
            if (pipes[id]?.screen?.altActive == true) return sendToFullScreen(id)
            val draft = metas[id]?.draft.orEmpty().trimCommand()
            if (draft.isBlank()) return false
            submit(id, draft)
            mutateMeta(id) { it.copy(draft = "") }
        }
        persistTransientState()
        return true
    }

    /** A full-screen program gets the typed text and Enter as raw input: no command bubble, no history. */
    private fun sendToFullScreen(id: Long): Boolean {
        val backend = backends[id] ?: return false
        val text = metas[id]?.draft.orEmpty()
        backend.write((text + "\r").toByteArray(Charsets.UTF_8))
        mutateMeta(id) { it.copy(draft = "") }
        return true
    }

    /** Runs a favourite or key payload straight to the shell. Returns false when there was nothing to run. */
    fun executeDirectly(command: String): Boolean {
        val id = _state.value.currentSessionId ?: return false
        val text = command.trimCommand()
        if (text.isBlank()) return false
        submit(id, text)
        return true
    }

    /** Appends text to the draft, e.g. a favourite that should be edited before running. */
    fun insertIntoDraft(text: String) {
        val id = _state.value.currentSessionId ?: return
        mutateMeta(id) { it.copy(draft = it.draft + text) }
    }

    /** Tab with a visible suggestion: take the whole command. */
    fun acceptSuggestion() {
        val id = _state.value.currentSessionId ?: return
        val suggestion = _state.value.suggestion ?: return
        mutateMeta(id) { it.copy(draft = suggestion) }
        _state.update { it.copy(suggestion = null) }
    }

    /**
     * Clears the shown transcript of the current session. The journal keeps every event, so a
     * restart shows the same cleared screen and nothing is lost from disk.
     */
    fun clearScreen() {
        val id = _state.value.currentSessionId ?: return
        synchronized(lock) {
            val pipe = pipes[id] ?: return
            pipe.journal.append(JournalEvent.ScreenCleared(timestampMs = System.currentTimeMillis()))
            pipe.builder.applyScreenCleared()
            refreshSession(id)
        }
    }

    /** Commands sent in the current session, oldest first, for walking back with the up key. */
    fun commandHistory(): List<String> {
        val id = _state.value.currentSessionId ?: return emptyList()
        return synchronized(lock) { histories[id]?.toList() }.orEmpty()
    }

    /** Raw bytes to the current shell, bypassing the draft (key row). */
    fun sendRaw(payload: String) {
        val id = _state.value.currentSessionId ?: return
        val backend = synchronized(lock) { backends[id] } ?: return
        backend.write(payload.toByteArray(Charsets.UTF_8))
    }

    fun onAreaResized(rows: Int, cols: Int) {
        val id = _state.value.currentSessionId ?: return
        val safeRows = rows.coerceAtLeast(1)
        val safeCols = cols.coerceAtLeast(1)
        val (backend, screen) = synchronized(lock) { backends[id] to pipes[id]?.screen }
        screen?.resize(safeRows, safeCols)
        backend?.resize(safeRows, safeCols)
    }

    /**
     * Ghost-text suggestion: the newest history command that extends the
     * current draft, zsh-autosuggestions style.
     */
    private fun updateSuggestion(id: Long, draft: String) {
        val suggestion = if (!_state.value.settings.autocompleteEnabled || draft.isEmpty()) {
            null
        } else {
            synchronized(lock) {
                histories[id]?.lastOrNull { it != draft && it.startsWith(draft) }
            }
        }
        _state.update { it.copy(suggestion = suggestion) }
    }

    private fun submit(id: Long, text: String) {
        val backend = synchronized(lock) { backends[id] }
        if (backend == null) {
            AyanaLog.error("submit", "no engine for session $id")
            return
        }
        val seq = synchronized(lock) {
            val pipe = pipes[id] ?: return
            val seq = nextCommandSeqs[id] ?: return
            nextCommandSeqs[id] = seq + 1
            if (!backend.reportsCommandEnds) {
                // The output of the previous command ends here, so the new output lands below this command.
                pipe.journal.append(JournalEvent.CommandFinished(exitCode = null))
                pipe.builder.applyCommandEnd(null)
            }
            pipe.journal.append(
                JournalEvent.CommandSubmitted(seq = seq, text = text, timestampMs = System.currentTimeMillis()),
            )
            pipe.builder.applyCommandSubmitted(seq, text)
            val history = histories.getOrPut(id) { mutableListOf() }
            history.add(text)
            if (history.size > HISTORY_CAP) history.removeAt(0)
            refreshSession(id)
            seq
        }
        val payload = text.shellEval()
        AyanaLog.log("submit", "seq=$seq payloadSize=${payload.length}")
        backend.write(payload.toByteArray(Charsets.UTF_8))
    }

    // ------------------------------------------------------------- favourites

    fun addFavorite(text: String) {
        val trimmed = text.trimCommand()
        if (trimmed.isEmpty()) return
        synchronized(lock) {
            val current = _state.value.favorites
            if (current.any { it.text == trimmed }) return
            saveFavorites(current + FavoriteCommand(id = ++favoriteCounter, text = trimmed))
        }
    }

    fun updateFavorite(id: Long, text: String) {
        val trimmed = text.normalizeCommand()
        if (trimmed.isEmpty()) return
        synchronized(lock) {
            val current = _state.value.favorites
            if (current.any { it.id == id && it.text == trimmed }) return
            saveFavorites(current.map { if (it.id == id) it.copy(text = trimmed) else it })
        }
    }

    fun deleteFavorite(id: Long) {
        synchronized(lock) {
            saveFavorites(_state.value.favorites.filterNot { it.id == id })
        }
    }

    private fun saveFavorites(items: List<FavoriteCommand>) {
        storage.saveFavorites(FavoritesIndex(items = items, nextFavoriteId = favoriteCounter + 1))
        _state.update { it.copy(favorites = items) }
    }

    // -------------------------------------------------------------- key row

    fun setCustomKeys(keys: List<CustomKey>) {
        storage.saveKeys(KeysIndex(items = keys))
        _state.update { it.copy(customKeys = keys) }
    }

    // ------------------------------------------------------------- placement

    /**
     * Puts a command the app prepared (a file copied, moved or linked) into the composer on a line
     * of its own, or runs it at once when the setting says so.
     */
    fun placeCommand(command: String) {
        if (_state.value.settings.attachSendsNow) {
            executeDirectly(command)
            return
        }
        val draft = currentDraft()
        insertIntoDraft(if (draft.isEmpty() || draft.endsWith("\n")) command else "\n$command")
    }

    /** Puts an already quoted path into the composer, after a space on the same line. */
    fun placePath(quotedPath: String) {
        val draft = currentDraft()
        insertIntoDraft(if (draft.isEmpty() || draft.last().isWhitespace()) quotedPath else " $quotedPath")
    }

    private fun currentDraft(): String {
        val state = _state.value
        return state.sessions.firstOrNull { it.id == state.currentSessionId }?.draft.orEmpty()
    }

    // ----------------------------------------------------------------- search

    fun enterSearch() {
        _state.update { it.copy(search = SearchState()) }
    }

    fun exitSearch() {
        _state.update { it.copy(search = null) }
    }

    /** Re-runs the search over the current session. Returns the first match to reveal, if any. */
    fun setSearchQuery(query: String): String? {
        val id = _state.value.currentSessionId ?: return null
        val blocks = _state.value.sessions.firstOrNull { it.id == id }?.blocks.orEmpty()
        val matches = TranscriptSearch.matchingBlockIds(blocks, query)
        _state.update { st ->
            st.copy(
                search = (st.search ?: SearchState()).copy(query = query, matchBlockIds = matches, index = 0),
            )
        }
        return matches.firstOrNull()
    }

    /** Advances to the next match, wrapping around. Returns the block to reveal. */
    fun nextSearchMatch(): String? {
        val search = _state.value.search ?: return null
        if (search.matchBlockIds.isEmpty()) return null
        val nextIndex = (search.index + 1) % search.matchBlockIds.size
        _state.update { st -> st.copy(search = st.search?.copy(index = nextIndex)) }
        return search.matchBlockIds[nextIndex]
    }

    // ------------------------------------------------------- block interaction

    fun toggleBlock(blockId: String) {
        val id = _state.value.currentSessionId ?: return
        synchronized(lock) {
            pipes[id]?.builder?.toggleExpanded(blockId)
            refreshSession(id)
        }
    }

    /** Expands a block without collapsing anything (search navigation). */
    fun revealBlock(blockId: String) {
        val id = _state.value.currentSessionId ?: return
        synchronized(lock) {
            pipes[id]?.builder?.reveal(blockId)
            refreshSession(id)
        }
    }

    // --------------------------------------------------------------- settings

    /**
     * Applies a settings change, keeps every live view consistent with it and
     * persists the result. Bounds are enforced here, once, for every caller.
     */
    fun updateSettings(transform: (AyanaSettings) -> AyanaSettings) {
        synchronized(lock) {
            val before = _state.value.settings
            val after = transform(before).withinBounds()
            if (after == before) return
            _state.update {
                it.copy(
                    settings = after,
                    suggestion = if (after.autocompleteEnabled) it.suggestion else null,
                )
            }
            if (after.previewLines != before.previewLines) {
                pipes.values.forEach { it.builder.applyPreviewLines(after.previewLines) }
            }
            if (after.previewLines != before.previewLines || after.rawStream != before.rawStream) {
                for (id in pipes.keys.toList()) refreshSession(id)
            }
            storage.saveSettings(after)
        }
    }

    private fun AyanaSettings.withinBounds(): AyanaSettings = copy(
        outputFontSizeSp = outputFontSizeSp.coerceIn(10, 22),
        previewLines = previewLines.coerceIn(1, 10),
        bubbleFontSizeSp = bubbleFontSizeSp.coerceIn(12, 24),
        wallpaperDimPct = wallpaperDimPct.coerceIn(0, 90),
        wallpaperBlurDp = wallpaperBlurDp.coerceIn(0, 40),
        longCommandSec = longCommandSec.coerceIn(3, 600),
    )

    // ------------------------------------------------------------ persistence

    /** Persists metadata (names, drafts, pins). Call on pause and before shutdown. */
    fun persistTransientState() {
        synchronized(lock) { persistIndexLocked() }
    }

    /** Stops every shell and closes every journal. */
    fun close() {
        synchronized(lock) {
            backends.values.forEach { it.terminate() }
            rawQueues.values.forEach { it.cancel() }
            pipes.values.forEach { it.journal.close() }
        }
    }

    // ------------------------------------------------------------- internals

    private fun createSessionLocked() {
        val settings = _state.value.settings
        val id = nextSessionIdValue++
        metas[id] = SessionMeta(id = id, name = defaultSessionName(pipes.size + 1))
        histories[id] = mutableListOf()
        val journal = storage.journalFor(id)
        pipes[id] = SessionPipe(
            journal = journal,
            parser = StreamParser(),
            builder = TranscriptBuilder(previewLines = settings.previewLines),
        )
        nextCommandSeqs[id] = journal.nextCommandSeq() + 1
        attachEngine(id, settings.shellPath)
        persistIndexLocked()
        refreshSession(id)
        _state.update { it.copy(currentSessionId = id, search = null) }
    }

    private fun mutateMeta(id: Long, persist: Boolean = true, transform: (SessionMeta) -> SessionMeta) {
        synchronized(lock) {
            val current = metas[id] ?: return
            metas[id] = transform(current)
            if (persist) persistIndexLocked()
            refreshSession(id)
        }
    }

    private fun persistIndexLocked() {
        val items = metas.values.sortedBy { it.id }
        val nextId = maxOf(nextSessionIdValue, (items.maxOfOrNull { it.id } ?: 0L) + 1)
        nextSessionIdValue = nextId
        storage.saveIndex(SessionsIndex(items = items, nextSessionId = nextId))
    }

    private fun refreshSession(id: Long) {
        val pipe = pipes[id] ?: return
        val meta = metas[id] ?: return
        val rawStream = _state.value.settings.rawStream
        val snapshot = pipe.builder.snapshot(displayRaw = rawStream)
        val view = SessionView(
            id = meta.id,
            name = meta.name,
            pinned = meta.pinned,
            rawStream = rawStream,
            draft = meta.draft,
            blocks = snapshot.blocks,
            running = snapshot.running,
            rawText = snapshot.rawText,
            fullScreen = pipe.screen?.takeIf { it.altActive }?.snapshot(),
            directory = pipe.builder.workingDirectory?.let(::abbreviateHome),
        )
        _state.update { st ->
            val sessions = st.sessions.toMutableList()
            val index = sessions.indexOfFirst { it.id == id }
            if (index >= 0) sessions[index] = view else sessions.add(view)
            st.copy(sessions = sessions)
        }
    }

    /**
     * `~` for the shell's home folder, as shells show it in their prompt. Both sides are compared
     * resolved: the shell reports its physical directory (/data/data/...), while the home was handed
     * over through a symlinked path (/data/user/0/...), so a plain prefix check never matched.
     */
    private fun abbreviateHome(path: String): String = shownDirectories.getOrPut(path) {
        val resolved = runCatching { File(path).canonicalPath }.getOrDefault(path)
        when {
            resolved == canonicalHome -> "~"
            resolved.startsWith("$canonicalHome/") -> "~" + resolved.substring(canonicalHome.length)
            else -> resolved
        }
    }



    private fun rebuildAllSessions() {
        for (id in pipes.keys.toList()) refreshSession(id)
        _state.update { st -> st.copy(sessions = st.sessions.filter { it.id in pipes }) }
    }

    private class SessionPipe(
        val journal: SessionJournal,
        val parser: StreamParser,
        val builder: TranscriptBuilder,
    ) {
        /** Only for a shell that sends no command marks: it draws a full-screen program's output. */
        var screen: TerminalScreen? = null
    }

    private class RestoredSession(
        val meta: SessionMeta,
        val pipe: SessionPipe,
        val history: MutableList<String>,
        val nextSeq: Long,
    )

    private companion object {
        const val INITIAL_ROWS = 24
        const val INITIAL_COLS = 80
        const val HISTORY_CAP = 500
        const val REFRESH_INTERVAL_MS = 100L
        const val EVENT_QUEUE_CAPACITY = 16
    }
}
