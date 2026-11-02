package iatheiya.ayana.session

import iatheiya.ayana.util.shellQuote

/** Turns a file copied into Ayana's folder into the shell command that uses it in the current folder. */
object Attachments {

    /** The action that inserts the path itself, with no command. */
    const val PATH = "path"

    /** Copy the file into the shell's working directory. */
    const val COPY = "copy"

    /** Give the working directory a symlink to the file. */
    const val LINK = "link"

    private val UNSAFE_NAME_CHARS = Regex("[^A-Za-z0-9._-]")

    fun safeName(raw: String): String = raw.replace(UNSAFE_NAME_CHARS, "_")

    /**
     * [action] is copy or link. Anything else behaves like copy. The original file is never
     * touched: the picker grants read access only, so this is a copy, not a move.
     */
    fun command(action: String, absolutePath: String, safeName: String): String {
        val quoted = absolutePath.shellQuote()
        return when (action) {
            LINK -> "ln -s $quoted ./'$safeName'"
            else -> "cp $quoted ./"
        }
    }

    /**
     * What several picked files put into the composer: their paths side by side for [PATH], or
     * one command per file joined with `&&`, so they run in turn and stop at the first failure.
     */
    fun combined(action: String, files: List<Pair<String, String>>): String = if (action == PATH) {
        files.joinToString(" ") { (path, _) -> path.shellQuote() }
    } else {
        files.joinToString(" && ") { (path, name) -> command(action, path, name) }
    }
}
