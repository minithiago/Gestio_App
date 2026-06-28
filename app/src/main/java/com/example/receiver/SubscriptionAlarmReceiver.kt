package com.example.receiver

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity

class SubscriptionAlarmReceiver : BroadcastReceiver() {

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        val subscriptionId = intent.getIntExtra("subscription_id", 0)
        val subscriptionName = intent.getStringExtra("subscription_name") ?: "Suscripción"
        val subscriptionAmount = intent.getDoubleExtra("subscription_amount", 0.0)

        // Intent to open the app when clicking the notification
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            subscriptionId,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification
        val formattedAmount = String.format("%.2f", subscriptionAmount)
        val builder = NotificationCompat.Builder(context, SubscriptionNotificationHelper.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder) // System default icon, looks solid and safe
            .setContentTitle("Próximo vencimiento")
            .setContentText("Tu suscripción a $subscriptionName por $$formattedAmount vence pronto.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = NotificationManagerCompat.from(context)
        
        // We trigger the notification. If the POST_NOTIFICATIONS runtime permission is missing,
        // Android 13+ will naturally ignore the call without crashing, which is correct and safe.
        notificationManager.notify(subscriptionId, builder.build())
    }
}
