package iatheiya.ayana.util

import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** Receives every log line. The Android app forwards them to logcat. */
fun interface LogSink {
    fun write(isError: Boolean, tag: String, message: String, error: Throwable?)
}

/**
 * In-memory diagnostic ring buffer. Everything around the shell pipeline
 * (spawn, write, read, parse, journal) reports here, so failures can be
 * exported from Settings and inspected instead of being guessed at.
 */
object AyanaLog {

    private const val CAPACITY = 600
    private val lines = ArrayDeque<String>()
    private val lock = Any()
    private val clock = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")

    @Volatile
    var sink: LogSink? = null

    fun log(tag: String, message: String) {
        enqueue("${LocalTime.now().format(clock)} [$tag] $message")
        sink?.write(isError = false, tag = tag, message = message, error = null)
    }

    fun error(tag: String, message: String, error: Throwable? = null) {
        enqueue(
            "${LocalTime.now().format(clock)} [$tag] ERROR: $message" +
                (error?.let { " :: ${it::class.java.simpleName}: ${it.message}" } ?: ""),
        )
        sink?.write(isError = true, tag = tag, message = message, error = error)
    }

    private fun enqueue(line: String) = synchronized(lock) {
        lines.addLast(line)
        while (lines.size > CAPACITY) lines.removeFirst()
    }

    fun dump(): String = synchronized(lock) { lines.joinToString("\n") }

    fun clear() = synchronized(lock) { lines.clear() }
}
