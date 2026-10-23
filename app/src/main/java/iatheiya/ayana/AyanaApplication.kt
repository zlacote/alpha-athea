package iatheiya.ayana

import android.app.Application
import android.util.Log
import iatheiya.ayana.util.AyanaLog
import iatheiya.ayana.util.LogSink

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
    }

    private companion object {
        const val LOG_TAG = "Ayana"
    }
}
