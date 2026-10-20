package com.athea.app.data

import com.athea.app.core.journal.SessionJournal
import com.athea.app.core.model.FavoriteCommand
import com.athea.app.core.model.PREVIEW_LINES
import com.athea.app.util.writeTextAtomic
import java.io.File
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Persistable per-session metadata (the transcript itself lives in the
 * journal).
 *
 * The raw-stream view is deliberately absent: it is a single global
 * preference in [AtheaSettings], and storing a per-session copy of it here
 * only created a second source of truth to keep in sync.
 */
/** Written once the first-run setup is finished. */
private const val ONBOARDED_MARKER = "onboarded"

/** Shell used when the user has not picked one. */
const val DEFAULT_SHELL_PATH = "/system/bin/sh"

@Serializable
data class SessionMeta(
    val id: Long,
    val name: String,
    val pinned: Boolean = false,
    val draft: String = "",
)

@Serializable
internal data class SessionsIndex(
    val items: List<SessionMeta> = emptyList(),
    val nextSessionId: Long = 1,
)

@Serializable
internal data class FavoritesIndex(
    val items: List<FavoriteCommand> = emptyList(),
    val nextFavoriteId: Long = 1,
)

/** User-facing application preferences. */
@Serializable
data class AtheaSettings(
    val keyRowVisible: Boolean = true,
    val enterSends: Boolean = true,
    val autoScrollOnSend: Boolean = true,
    val rawStream: Boolean = false,
    val outputFontSizeSp: Int = 13,
    val previewLines: Int = PREVIEW_LINES,
    val bubbleFontSizeSp: Int = 16,
    val autocompleteEnabled: Boolean = true,
    val pinchZoomEnabled: Boolean = true,
    val shellPath: String = DEFAULT_SHELL_PATH,
    val virtualizeLargeOutput: Boolean = false,
    /** Commands made from picked files run at once; off, they wait in the composer on a new line. */
    val attachSendsNow: Boolean = false,
    /** The light that rises from the bottom edge while a command runs. */
    val glowEnabled: Boolean = true,
    /** Peak brightness of the glow, in percent. */
    val glowStrengthPct: Int = 50,
    /** Height of the glow, in dp. */
    val glowHeightDp: Int = 28,
    /** Width of the glow, as a percentage of the screen width. */
    val glowWidthPct: Int = 86,
    /** One pulse, in milliseconds. */
    val glowPeriodMs: Int = 2400,
)

/**
 * Owns every byte Athea keeps on disk: session index, favorites, journals
 * and the shell integration rc. Pure file operations; orchestration lives
 * in the ViewModel, so this class stays trivially testable.
 */
class AtheaStorage(root: File) {

    private val json = Json { ignoreUnknownKeys = true }

    private val sessionsDir = File(root, "sessions")
    private val shellDir = File(root, "shell")
    private val homeDir = File(root, "home")

    private val indexFile get() = File(sessionsDir, "index.json")
    private val favoritesFile get() = File(sessionsDir, "favorites.json")

    init {
        sessionsDir.mkdirs()
        homeDir.mkdirs()
    }

    // ----------------------------------------------------------------- index

    internal fun loadIndex(): SessionsIndex =
        runCatching {
            json.decodeFromString(SessionsIndex.serializer(), indexFile.readText())
        }.getOrDefault(SessionsIndex())

    internal fun saveIndex(index: SessionsIndex) {
        sessionsDir.mkdirs()
        indexFile.writeTextAtomic(json.encodeToString(SessionsIndex.serializer(), index))
    }

    // -------------------------------------------------------------- journals

    fun journalFor(id: Long): SessionJournal =
        SessionJournal(File(sessionsDir, id.toString()).resolve("journal.log"))

    fun deleteSession(id: Long) {
        File(sessionsDir, id.toString()).deleteRecursively()
    }

    // ------------------------------------------------------------- favorites

    internal fun loadFavorites(): FavoritesIndex =
        runCatching {
            json.decodeFromString(FavoritesIndex.serializer(), favoritesFile.readText())
        }.getOrDefault(FavoritesIndex())

    internal fun saveFavorites(favorites: FavoritesIndex) {
        sessionsDir.mkdirs()
        favoritesFile.writeTextAtomic(json.encodeToString(FavoritesIndex.serializer(), favorites))
    }

    // -------------------------------------------------------------- settings

    /** True until the first-run setup is finished, or until any setting has been saved. */
    fun needsOnboarding(): Boolean =
        !File(sessionsDir, ONBOARDED_MARKER).exists() && !File(sessionsDir, "settings.json").exists()

    fun markOnboarded() {
        sessionsDir.mkdirs()
        File(sessionsDir, ONBOARDED_MARKER).writeText("")
    }

    fun loadSettings(): AtheaSettings =
        runCatching {
            json.decodeFromString(
                AtheaSettings.serializer(),
                File(sessionsDir, "settings.json").readText(),
            )
        }.getOrDefault(AtheaSettings())

    fun saveSettings(settings: AtheaSettings) {
        sessionsDir.mkdirs()
        File(sessionsDir, "settings.json")
            .writeTextAtomic(json.encodeToString(AtheaSettings.serializer(), settings))
    }

    // ------------------------------------------------------------- key row

    fun loadKeys(): KeysIndex =
        runCatching {
            json.decodeFromString(KeysIndex.serializer(), File(sessionsDir, "keys.json").readText())
        }.getOrDefault(KeysIndex())

    fun saveKeys(keys: KeysIndex) {
        sessionsDir.mkdirs()
        File(sessionsDir, "keys.json")
            .writeTextAtomic(json.encodeToString(KeysIndex.serializer(), keys))
    }

    // ----------------------------------------------------------------- files

    fun ensureShellRc(assetBytes: ByteArray): File {
        shellDir.mkdirs()
        val rc = File(shellDir, "mkshrc")
        if (!rc.exists() || !rc.readBytes().contentEquals(assetBytes)) {
            rc.writeBytes(assetBytes)
        }
        return rc
    }

    fun shellHome(): File = homeDir
}
