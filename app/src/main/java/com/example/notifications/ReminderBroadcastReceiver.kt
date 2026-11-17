package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.PastelPawsApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ReminderBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            NotificationHelper.ACTION_GOAL_REMINDER -> {
                val goalId = intent.getLongExtra(NotificationHelper.EXTRA_GOAL_ID, 0L)
                val title = intent.getStringExtra(NotificationHelper.EXTRA_GOAL_TITLE).orEmpty()
                NotificationHelper.showGoalNotification(context, goalId, title)
            }

            NotificationHelper.ACTION_BIRTHDAY_SURPRISE -> {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val app = context.applicationContext as? PastelPawsApp
                        val nickname = app?.container?.repository?.prefsFlow?.first()?.nickname ?: "Doc"
                        NotificationHelper.showBirthdayNotification(context, nickname)
                        NotificationHelper.scheduleAnnualBirthdayAlarm(context)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}
