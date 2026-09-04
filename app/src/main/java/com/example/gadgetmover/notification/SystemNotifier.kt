package com.example.gadgetmover.notification

import android.Manifest
import android.app.PendingIntent
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.gadgetmover.R
import com.example.gadgetmover.MainActivity
import com.example.gadgetmover.data.AuthRepository
import com.example.gadgetmover.model.Notification
import com.example.gadgetmover.model.NotificationType
import com.example.gadgetmover.model.Order
import com.example.gadgetmover.model.RentalOrder
import com.example.gadgetmover.util.formatMoney
import java.time.Instant

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
    const val EXTRA_ORDER_ID = "notification_order_id"
    const val EXTRA_FROM_NOTIFICATION = "from_order_notification"
    const val EXTRA_RECIPIENT_USER_ID = "notification_recipient_user_id"

    fun post(context: Context, notification: Notification) {
        // Realtime is RLS filtered, but keep the final OS-facing boundary fail-closed as well:
        // a stale callback from an account switch must never alert the newly signed-in user.
        if (notification.recipientUserId != AuthRepository.currentUser.value?.id) return
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
        notification.relatedOrderId?.let { orderId ->
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_ORDER_ID, orderId)
                putExtra(EXTRA_FROM_NOTIFICATION, true)
                putExtra(EXTRA_RECIPIENT_USER_ID, notification.recipientUserId)
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                notification.id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.setContentIntent(pendingIntent)
        }
        val systemId = (notification.milestoneKey ?: notification.id).hashCode()
        NotificationManagerCompat.from(context).notify(systemId, builder.build())
    }

    /**
     * Immediate seller-side confirmation after the release RPC succeeds. The matching persisted
     * server notification uses the same milestone key, so its realtime arrival replaces this OS
     * entry instead of producing a duplicate.
     */
    fun notifyDepositRefunded(context: Context, order: Order) {
        val rental = order as? RentalOrder ?: return
        val recipientId = AuthRepository.currentUser.value?.id ?: return
        post(
            context,
            Notification(
                id = "deposit-refund-${rental.id}",
                recipientUserId = recipientId,
                type = NotificationType.ORDER_UPDATE,
                title = "Security deposit released",
                message = "${formatMoney(rental.deposit)} was refunded for rental #${rental.id.take(8).uppercase()}.",
                timestamp = Instant.now().toString(),
                relatedOrderId = rental.id,
                milestoneKey = "order:${rental.id}:deposit-refunded:seller"
            )
        )
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
