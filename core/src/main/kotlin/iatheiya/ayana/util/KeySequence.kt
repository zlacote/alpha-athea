package iatheiya.ayana.util

/**
 * The text form of a key's bytes, so a sequence can be typed by hand: `\e` (or `\E`) is Escape,
 * `\t` Tab, `\r` Enter, `\n` a newline, `\\` a backslash, `\xHH` and `\uHHHH` any character by
 * its code. Home, for example, is `\e[H`. Anything else is taken literally.
 */
fun unescapeKeySequence(text: String): String {
    val out = StringBuilder(text.length)
    var i = 0
    while (i < text.length) {
        val ch = text[i]
        if (ch != '\\' || i + 1 >= text.length) {
            out.append(ch)
            i++
            continue
        }
        when (val next = text[i + 1]) {
            'e', 'E' -> out.append('\u001B')
            't' -> out.append('\t')
            'r' -> out.append('\r')
            'n' -> out.append('\n')
            '\\' -> out.append('\\')
            'x', 'u' -> {
                val digits = if (next == 'x') 2 else 4
                val code = text.substring(i + 2, minOf(i + 2 + digits, text.length))
                    .takeIf { it.length == digits }
                    ?.toIntOrNull(16)
                if (code == null) {
                    out.append(ch).append(next)
                } else {
                    out.append(code.toChar())
                    i += digits
                }
            }
            else -> out.append(ch).append(next)
        }
        i += 2
    }
    return out.toString()
}

/** The inverse of [unescapeKeySequence]: control characters back to their typed form. */
fun escapeKeySequence(raw: String): String = buildString {
    for (ch in raw) {
        when {
            ch == '\u001B' -> append("\\e")
            ch == '\t' -> append("\\t")
            ch == '\r' -> append("\\r")
            ch == '\n' -> append("\\n")
            ch == '\\' -> append("\\\\")
            ch.code < 0x20 || ch.code == 0x7F -> append("\\x%02X".format(ch.code))
            else -> append(ch)
        }
    }
}

/** True for a plain command line, false once it holds control characters (a key sequence). */
fun String.isPlainText(): Boolean = none { it.code < 0x20 || it.code == 0x7F }
