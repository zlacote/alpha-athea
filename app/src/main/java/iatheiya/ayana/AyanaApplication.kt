package iatheiya.ayana

import android.app.Application
import android.util.Log
import iatheiya.ayana.ui.common.lightShaderError
import iatheiya.ayana.util.AyanaLog
import iatheiya.ayana.util.LogSink
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.time.LocalDateTime

class AyanaApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        AyanaLog.sink = LogSink { isError, tag, message, error ->
            if (isError) {
                Log.e(LOG_TAG, "[$tag] $message", error)
            } else {
                Log.i(LOG_TAG, "[$tag] $message")
            }
        }
        installCrashLog()
        if (BuildConfig.DEBUG) {
            Log.i(LOG_TAG, lightShaderError()?.let { "light shader failed: $it" } ?: "light shader ok")
        }
    }

    /**
     * The in-memory log dies with the process, so an uncaught exception is
     * written to a file first. It survives the crash and the diagnostics copy
     * picks it up on the next launch, so a crash can be reported instead of
     * guessed at. The previous handler still runs, so the process ends as the
     * system expects.
     */
    private fun installCrashLog() {
        val crashFile = File(filesDir, CRASH_FILE)
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching {
                val trace = StringWriter().also { error.printStackTrace(PrintWriter(it)) }
                crashFile.writeText(
                    "Ayana crash at ${LocalDateTime.now()}\n" +
                        "thread: ${thread.name}\n\n" +
                        "$trace\n" +
                        "--- recent log ---\n" +
                        AyanaLog.dump(),
                )
            }
            previous?.uncaughtException(thread, error)
        }
    }

    private companion object {
        const val LOG_TAG = "Ayana"
        const val CRASH_FILE = "crash.log"
    }
}
