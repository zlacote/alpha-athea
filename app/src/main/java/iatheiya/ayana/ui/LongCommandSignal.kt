package iatheiya.ayana.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import iatheiya.ayana.MainActivity
import iatheiya.ayana.R

/**
 * Tells the user that a long command has ended: a short double vibration, and, while Ayana is in
 * the background, a notification that brings it back when tapped.
 */
class LongCommandSignal(private val context: Context) {

    fun fire(sessionName: String, seconds: Long, foreground: Boolean) {
        vibrate()
        if (!foreground) notify(sessionName, seconds)
    }

    private fun vibrate() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            context.getSystemService(Vibrator::class.java)
        }
        vibrator?.vibrate(VibrationEffect.createWaveform(VIBRATION, -1))
    }

    @SuppressLint("MissingPermission") // checked just below for the versions that need it
    private fun notify(sessionName: String, seconds: Long) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName(context.getString(R.string.signal_channel))
                .build(),
        )
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_done)
            .setContentTitle(context.getString(R.string.signal_title))
            .setContentText(context.getString(R.string.signal_text, sessionName, seconds.toInt()))
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
    }

    private companion object {
        const val CHANNEL = "finished_commands"
        const val NOTIFICATION_ID = 1
        val VIBRATION = longArrayOf(0, 60, 90, 60)
    }
}
