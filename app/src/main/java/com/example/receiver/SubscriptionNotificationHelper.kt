package com.example.receiver

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.data.Subscription
import java.util.Calendar

object SubscriptionNotificationHelper {
    private const val TAG = "SubscriptionNotif"
    const val CHANNEL_ID = "subscription_reminders"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Recordatorios de Suscripciones"
            val descriptionText = "Notificaciones para recordarte los vencimientos de tus suscripciones"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun scheduleReminder(context: Context, subscription: Subscription) {
        if (!subscription.isNotificationEnabled) {
            cancelReminder(context, subscription.id)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, SubscriptionAlarmReceiver::class.java).apply {
            putExtra("subscription_id", subscription.id)
            putExtra("subscription_name", subscription.name)
            putExtra("subscription_amount", subscription.amount)
        }

        // Use a unique requestCode for each subscription to avoid overwriting alarms
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            subscription.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = getNextReminderTimeInMillis(subscription.dueDateDay)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
            Log.d(TAG, "Scheduled reminder for ${subscription.name} at: ${java.util.Date(triggerTime)}")
        } catch (e: SecurityException) {
            Log.e(TAG, "Could not schedule exact/idle alarm due to security restrictions", e)
        }
    }

    fun cancelReminder(context: Context, subscriptionId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, SubscriptionAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            subscriptionId,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Canceled reminder for subscription ID: $subscriptionId")
        }
    }

    /**
     * Calculates the millisecond timestamp for the next reminder.
     * Reminders are scheduled 1 day before the due date at 9:00 AM.
     * If that time has already passed for this month's cycle, it schedules for next month's cycle.
     */
    fun getNextReminderTimeInMillis(dueDateDay: Int): Long {
        val calendar = Calendar.getInstance()
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        // Set to 9:00 AM on the day BEFORE the due date
        val reminderCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        var targetMonth = currentMonth
        var targetYear = currentYear
        
        // Let's determine the target day of the month for the reminder.
        // If the due date day is e.g. 1 (first of the month), 1 day before is the last of the previous/current month.
        // It's cleaner to set the calendar to the due date day, then subtract 1 day.
        val dueCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            
            // Adjust day. If due date day is greater than actual maximum days in target month,
            // set to maximum day of that month.
            val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
            set(Calendar.DAY_OF_MONTH, if (dueDateDay > maxDay) maxDay else dueDateDay)
        }

        // Subtract 1 day for reminder
        dueCalendar.add(Calendar.DAY_OF_MONTH, -1)

        val now = Calendar.getInstance()
        if (dueCalendar.before(now)) {
            // If the reminder for this month's due date is already in the past, schedule it for the next month
            dueCalendar.add(Calendar.MONTH, 1)
            
            // Re-adjust day for the next month
            val nextMonthMaxDay = dueCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            dueCalendar.set(Calendar.DAY_OF_MONTH, if (dueDateDay > nextMonthMaxDay) nextMonthMaxDay else dueDateDay)
            dueCalendar.add(Calendar.DAY_OF_MONTH, -1)
        }

        return dueCalendar.timeInMillis
    }

    /**
     * Calculates the exact next payment date for display purposes.
     */
    fun getNextPaymentDate(dueDateDay: Int): Calendar {
        val now = Calendar.getInstance()
        val currentDay = now.get(Calendar.DAY_OF_MONTH)
        
        val dueCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            
            val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
            set(Calendar.DAY_OF_MONTH, if (dueDateDay > maxDay) maxDay else dueDateDay)
        }

        if (dueCalendar.before(now) && dueCalendar.get(Calendar.DAY_OF_MONTH) != currentDay) {
            dueCalendar.add(Calendar.MONTH, 1)
            val maxDay = dueCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            dueCalendar.set(Calendar.DAY_OF_MONTH, if (dueDateDay > maxDay) maxDay else dueDateDay)
        }

        return dueCalendar
    }

    /**
     * Helper to format calendar into readable Spanish date, e.g., "15 de Julio"
     */
    fun formatSpanishDate(calendar: Calendar): String {
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val monthNames = arrayOf(
            "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
        )
        val monthName = monthNames[calendar.get(Calendar.MONTH)]
        return "$day de $monthName"
    }

    /**
     * Calculates how many days are left until the next payment.
     */
    fun getDaysRemaining(dueDateDay: Int): Int {
        val now = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val nextDue = getNextPaymentDate(dueDateDay).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        val diffInMillis = nextDue.timeInMillis - now.timeInMillis
        return (diffInMillis / (1000 * 60 * 60 * 24)).toInt()
    }
}
