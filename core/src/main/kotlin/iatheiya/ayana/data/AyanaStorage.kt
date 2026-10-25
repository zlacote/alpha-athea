package iatheiya.ayana.data

import iatheiya.ayana.core.journal.SessionJournal
import iatheiya.ayana.core.model.FavoriteCommand
import iatheiya.ayana.core.model.PREVIEW_LINES
import iatheiya.ayana.util.writeTextAtomic
import java.io.File
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Persistable per-session metadata (the transcript itself lives in the
 * journal).
 *
 * The raw-stream view is deliberately absent: it is a single global
 * preference in [AyanaSettings], and storing a per-session copy of it here
 * only created a second source of truth to keep in sync.
 */
/** Written once the first-run setup is finished. */

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

const val AWAKE_OFF = "off"
const val AWAKE_RUNNING = "running"
const val AWAKE_ALWAYS = "always"

const val TINT_DEFAULT = "default"
const val TINT_SYSTEM = "system"
const val TINT_COLOR = "color"

const val FONT_SYSTEM = "system"
const val FONT_MONO = "mono"
const val FONT_SERIF = "serif"
const val FONT_CUSTOM = "custom"

/** Default rows and columns of the key grid above the composer. */
const val DEFAULT_KEY_ROWS = 2
const val DEFAULT_KEY_COLS = 7

@Serializable
data class AyanaSettings(
    val keyRowVisible: Boolean = true,
    /** Rows of the key grid shown above the composer. */
    val keyRows: Int = DEFAULT_KEY_ROWS,
    /** Columns of the key grid visible before it scrolls. */
    val keyCols: Int = DEFAULT_KEY_COLS,
    val enterSends: Boolean = true,
    val autoScrollOnSend: Boolean = true,
    val rawStream: Boolean = false,
    /** Full screen: the status bar with the clock is hidden until swiped in. */
    val immersiveMode: Boolean = false,
    /** When the screen is kept from going dark: [AWAKE_OFF], [AWAKE_RUNNING] or [AWAKE_ALWAYS]. */
    val screenAwake: String = AWAKE_OFF,
    /** A vibration, or a notification when Ayana is in the background, as a long command ends. */
    val signalLongCommands: Boolean = false,
    /**
     * The signal was switched off because notifications are not allowed; it comes back on by
     * itself once they are.
     */
    val signalAwaitingPermission: Boolean = false,
    /** How long a command must run, in seconds, to count as long. */
    val longCommandSec: Int = 15,
    /** Glass surfaces on; off, every surface is one solid tone. */
    val glassEnabled: Boolean = true,
    /** Where the colour of buttons and panels comes from: [TINT_DEFAULT], [TINT_SYSTEM] or [TINT_COLOR]. */
    val tintMode: String = TINT_DEFAULT,
    /** The colour for [TINT_COLOR], as ARGB. */
    val tintArgb: Long? = null,
    /** The transcript font: one of [FONT_SYSTEM], [FONT_MONO], [FONT_SERIF], [FONT_CUSTOM]. */
    val font: String = FONT_SYSTEM,
    /** Bumped each time a custom font file is picked; 0 while there is none. */
    val customFontVersion: Long = 0,
    /** Bumped each time a wallpaper is picked; 0 while there is none. */
    val wallpaperVersion: Long = 0,
    /** How much the wallpaper is darkened under the text, in percent. */
    val wallpaperDimPct: Int = 45,
    /** How much the wallpaper is blurred, in dp; 0 keeps it sharp. */
    val wallpaperBlurDp: Int = 0,
    val outputFontSizeSp: Int = 13,
    val previewLines: Int = PREVIEW_LINES,
    val bubbleFontSizeSp: Int = 16,
    val autocompleteEnabled: Boolean = true,
    val pinchZoomEnabled: Boolean = true,
    val shellPath: String = DEFAULT_SHELL_PATH,
    val virtualizeLargeOutput: Boolean = false,
    /** Commands made from picked files run at once; off, they wait in the composer on a new line. */
    val attachSendsNow: Boolean = false,
    /** Tapping a favorite returns to the chat after placing it. */
    val favoriteReturnsHome: Boolean = true,
    /** Tapping a favorite runs it at once instead of inserting it into the composer. */
    val favoriteSendsImmediately: Boolean = false,
    /** The light that glows down from the top edge while a command runs. */
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
 * Owns every byte Ayana keeps on disk: session index, favorites, journals
 * and the shell integration rc. Pure file operations; orchestration lives
 * in the ViewModel, so this class stays trivially testable.
 */
class AyanaStorage(private val root: File) {

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

    fun loadSettings(): AyanaSettings =
        runCatching {
            json.decodeFromString(
                AyanaSettings.serializer(),
                File(sessionsDir, "settings.json").readText(),
            )
        }.getOrDefault(AyanaSettings())

    fun saveSettings(settings: AyanaSettings) {
        sessionsDir.mkdirs()
        File(sessionsDir, "settings.json")
            .writeTextAtomic(json.encodeToString(AyanaSettings.serializer(), settings))
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

    /** Files the user picked to dress the app: the wallpaper and a custom font. */
    fun wallpaperFile(): File = File(appearanceDir, "wallpaper")

    fun customFontFile(): File = File(appearanceDir, "font")

    private val appearanceDir get() = File(root, "appearance").apply { mkdirs() }
}
