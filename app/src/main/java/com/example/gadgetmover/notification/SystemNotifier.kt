package com.example.gadgetmover.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.gadgetmover.R
import com.example.gadgetmover.model.Notification

private const val CHANNEL_ID = "gadgetmover_activity"

/**
 * Posts [notification] to the Android system tray — the counterpart to the in-app Notifications
 * screen, which only shows what's happened while the user has that screen open. Called from
 * [com.example.gadgetmover.data.NotificationRepository]'s realtime subscription whenever a new
 * row streams in from `public.notifications` while the app process is alive.
 *
 * This can't wake the app process up from being fully killed or deliver anything while offline —
 * that needs a real push service (Firebase Cloud Messaging) plus a server-side sender, which is
 * a separate, heavier piece of infrastructure this app doesn't have set up.
 */
object SystemNotifier {
    fun post(context: Context, notification: Notification) {
        ensureChannel(context)
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(notification.title)
            .setContentText(notification.message)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        NotificationManagerCompat.from(context).notify(notification.id.hashCode(), builder.build())
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Activity updates",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Order status changes, new messages, and rental requests"
        }
        manager.createNotificationChannel(channel)
    }
}
