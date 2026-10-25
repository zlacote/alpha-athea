package iatheiya.ayana

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import iatheiya.ayana.util.AyanaLog

/**
 * Keeps Ayana's process alive while a command runs. Without it Android freezes an app soon after
 * it leaves the screen, and the shell with it: a long command would simply stop in the background
 * and never end. A quiet ongoing notification says a command is running; it goes away with it.
 */
class CommandKeepAliveService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        NotificationManagerCompat.from(this).createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL, NotificationManagerCompat.IMPORTANCE_LOW)
                .setName(getString(R.string.keepalive_channel))
                .build(),
        )
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_running)
            .setContentTitle(getString(R.string.keepalive_title))
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(open)
            .build()
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
        return START_NOT_STICKY
    }

    companion object {
        private const val CHANNEL = "running_commands"
        private const val NOTIFICATION_ID = 2

        /** Starts keeping the process alive; Android allows it only while Ayana is in front. */
        fun start(context: Context) {
            try {
                ContextCompat.startForegroundService(context, Intent(context, CommandKeepAliveService::class.java))
            } catch (e: Exception) {
                AyanaLog.error("keepalive", "could not start", e)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, CommandKeepAliveService::class.java))
        }
    }
}
