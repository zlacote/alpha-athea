package iatheiya.ayana.session

import iatheiya.ayana.core.model.FavoriteCommand
import iatheiya.ayana.core.terminal.TerminalEngine
import iatheiya.ayana.data.AyanaSettings
import iatheiya.ayana.data.CustomKey
import iatheiya.ayana.screen.ScreenSnapshot
import iatheiya.ayana.transcript.BlockView

/** Everything one session shows. The draft lives in the session's metadata, not in the transcript. */
data class SessionView(
    val id: Long,
    val name: String,
    val pinned: Boolean,
    val rawStream: Boolean,
    val draft: String,
    val blocks: List<BlockView>,
    val running: Boolean,
    val rawText: String,
    /** The screen of a full-screen program while it runs; null otherwise. */
    val fullScreen: ScreenSnapshot? = null,
    /** The shell's working directory with the home folder shown as `~`; null until reported. */
    val directory: String? = null,
)

/** Search within one session transcript. */
data class SearchState(
    val query: String = "",
    val matchBlockIds: List<String> = emptyList(),
    val index: Int = 0,
)

/** Everything the workspace publishes. Presentation-only flags are not part of it. */
data class WorkspaceState(
    val sessions: List<SessionView> = emptyList(),
    val currentSessionId: Long? = null,
    val favorites: List<FavoriteCommand> = emptyList(),
    val customKeys: List<CustomKey> = emptyList(),
    val settings: AyanaSettings = AyanaSettings(),
    val search: SearchState? = null,
    val suggestion: String? = null,
)

/** One-shot notifications for the user. */
sealed interface WorkspaceEvent {
    data object ShellStartFailed : WorkspaceEvent

    data class ShellExited(val exitCode: Int) : WorkspaceEvent
}

/** Creates the terminal backend for a new session. The workspace starts it. */
fun interface EngineFactory {
    fun create(shellPath: String): TerminalEngine
}
