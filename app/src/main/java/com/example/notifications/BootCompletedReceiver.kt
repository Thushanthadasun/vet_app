package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.PastelPawsApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                NotificationHelper.scheduleAnnualBirthdayAlarm(context)
                val app = context.applicationContext as? PastelPawsApp ?: return@launch
                val pendingGoals = app.container.repository.getPendingReminderGoals()
                pendingGoals.forEach { goal ->
                    val time = goal.reminderEpochMillis
                    if (time != null && time > System.currentTimeMillis()) {
                        NotificationHelper.scheduleGoalReminder(
                            context = context,
                            goalId = goal.id,
                            title = goal.title,
                            triggerAtMillis = time,
                        )
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
