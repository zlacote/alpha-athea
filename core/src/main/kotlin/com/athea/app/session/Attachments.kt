package com.athea.app.session

import com.athea.app.util.shellQuote

/** Turns a staged file into the shell command that places it in the session folder. */
object Attachments {

    private val UNSAFE_NAME_CHARS = Regex("[^A-Za-z0-9._-]")

    fun safeName(raw: String): String = raw.replace(UNSAFE_NAME_CHARS, "_")

    /** [action] is copy, move, link or name. Anything else behaves like copy. */
    fun command(action: String, absolutePath: String, safeName: String): String {
        val quoted = absolutePath.shellQuote()
        return when (action) {
            "move" -> "mv $quoted ./"
            "link" -> "ln -s $quoted ./'$safeName'"
            "name" -> quoted
            else -> "cp $quoted ./"
        }
    }
}
