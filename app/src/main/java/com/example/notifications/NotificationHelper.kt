package com.example.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.util.DateHelpers

object NotificationHelper {
    const val CHANNEL_REMINDERS = "goal_reminders"
    const val CHANNEL_BIRTHDAY = "birthday_surprise"

    const val ACTION_GOAL_REMINDER = "com.example.ACTION_GOAL_REMINDER"
    const val ACTION_BIRTHDAY_SURPRISE = "com.example.ACTION_BIRTHDAY_SURPRISE"

    const val EXTRA_GOAL_ID = "extra_goal_id"
    const val EXTRA_GOAL_TITLE = "extra_goal_title"
    const val EXTRA_OPEN_BIRTHDAY = "extra_open_birthday"
    const val EXTRA_OPEN_GOALS = "extra_open_goals"

    private const val BIRTHDAY_REQUEST_CODE = 90306

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDERS,
                context.getString(R.string.channel_reminders_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.channel_reminders_desc)
            }
            val birthdayChannel = NotificationChannel(
                CHANNEL_BIRTHDAY,
                context.getString(R.string.channel_birthday_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.channel_birthday_desc)
            }
            manager.createNotificationChannel(reminderChannel)
            manager.createNotificationChannel(birthdayChannel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun scheduleGoalReminder(
        context: Context,
        goalId: Long,
        title: String,
        triggerAtMillis: Long,
    ) {
        if (triggerAtMillis <= System.currentTimeMillis()) return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = goalPendingIntent(context, goalId, title)
        scheduleSafeAlarm(alarmManager, triggerAtMillis, pendingIntent)
    }

    fun cancelGoalReminder(context: Context, goalId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = goalPendingIntent(context, goalId, "")
        alarmManager.cancel(pendingIntent)
    }

    fun scheduleAnnualBirthdayAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val nextTrigger = DateHelpers.nextMarch6MorningMillis()
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ACTION_BIRTHDAY_SURPRISE
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            BIRTHDAY_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        scheduleSafeAlarm(alarmManager, nextTrigger, pendingIntent)
    }

    private fun scheduleSafeAlarm(
        alarmManager: AlarmManager,
        triggerAtMillis: Long,
        pendingIntent: PendingIntent,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent,
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent,
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent,
            )
        }
    }

    private fun goalPendingIntent(context: Context, goalId: Long, title: String): PendingIntent {
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ACTION_GOAL_REMINDER
            putExtra(EXTRA_GOAL_ID, goalId)
            putExtra(EXTRA_GOAL_TITLE, title)
        }
        return PendingIntent.getBroadcast(
            context,
            (goalId.toInt() and 0x7FFFFFFF).coerceAtLeast(1),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    fun showGoalNotification(context: Context, goalId: Long, title: String) {
        if (!hasNotificationPermission(context)) return
        createChannels(context)
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_GOALS, true)
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            (goalId.toInt() and 0x7FFFFFFF).coerceAtLeast(10),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle("Goal Reminder")
            .setContentText(title.ifBlank { "Time to check off your study goal!" })
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(goalId.toInt(), notification)
        }
    }

    fun showBirthdayNotification(context: Context, nickname: String) {
        if (!hasNotificationPermission(context)) return
        createChannels(context)
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_BIRTHDAY, true)
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            BIRTHDAY_REQUEST_CODE,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_BIRTHDAY)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle("Happy Birthday, $nickname!")
            .setContentText("A birthday message and a free Mystery Gift Box are waiting inside Pastel Paws.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(BIRTHDAY_REQUEST_CODE, notification)
        }
    }
}
