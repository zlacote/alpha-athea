package com.athea.app.session

import com.athea.app.util.shellQuote

/** Turns a file copied into Athea's folder into the shell command that uses it in the current folder. */
object Attachments {

    /** The action that inserts the path itself, with no command. */
    const val PATH = "path"

    private val UNSAFE_NAME_CHARS = Regex("[^A-Za-z0-9._-]")

    fun safeName(raw: String): String = raw.replace(UNSAFE_NAME_CHARS, "_")

    /** [action] is copy, move or link. Anything else behaves like copy. */
    fun command(action: String, absolutePath: String, safeName: String): String {
        val quoted = absolutePath.shellQuote()
        return when (action) {
            "move" -> "mv $quoted ./"
            "link" -> "ln -s $quoted ./'$safeName'"
            else -> "cp $quoted ./"
        }
    }
}
