package iatheiya.ayana.util

import java.io.File

/** Crash-safe: temp file first, atomic rename second. */
fun File.writeTextAtomic(text: String) {
    val tmp = File(parentFile, "$name.tmp")
    tmp.writeText(text)
    if (!tmp.renameTo(this)) {
        tmp.delete()
        writeText(text)
    }
}
