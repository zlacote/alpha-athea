package com.athea.app

import android.app.Application
import android.util.Log
import com.athea.app.util.AtheaLog
import com.athea.app.util.LogSink

class AtheaApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        AtheaLog.sink = LogSink { isError, tag, message, error ->
            if (isError) {
                Log.e(LOG_TAG, "[$tag] $message", error)
            } else {
                Log.i(LOG_TAG, "[$tag] $message")
            }
        }
    }

    private companion object {
        const val LOG_TAG = "Athea"
    }
}
