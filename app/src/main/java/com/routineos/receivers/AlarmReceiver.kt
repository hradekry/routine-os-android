package com.routineos.receivers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.routineos.services.AlarmService

class AlarmReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "Routine OS"
        val description = intent.getStringExtra("description") ?: ""
        val eventId = intent.getStringExtra("eventId")
        val taskId = intent.getStringExtra("taskId")
        
        // Start the alarm service to show notification and play sound
        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            action = "SHOW_ALARM"
            putExtra("title", title)
            putExtra("description", description)
            putExtra("eventId", eventId)
            putExtra("taskId", taskId)
        }
        
        AlarmService.startService(context)
        context.startService(serviceIntent)
        
        // Schedule next alarm if this is a recurring event/task
        if (intent.getBooleanExtra("recurring", false)) {
            scheduleNextRecurringAlarm(context, intent)
        }
    }
    
    private fun scheduleNextRecurringAlarm(context: Context, originalIntent: Intent) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmTime = originalIntent.getLongExtra("alarmTime", 0)
        
        // Schedule for next day
        val nextAlarmTime = alarmTime + (24 * 60 * 60 * 1000)
        
        val newIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = originalIntent.action
            putExtra("title", originalIntent.getStringExtra("title"))
            putExtra("description", originalIntent.getStringExtra("description"))
            putExtra("eventId", originalIntent.getStringExtra("eventId"))
            putExtra("taskId", originalIntent.getStringExtra("taskId"))
            putExtra("recurring", true)
            putExtra("alarmTime", nextAlarmTime)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            originalIntent.getIntExtra("requestCode", 0) + 1000, // Different code for next day
            newIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        nextAlarmTime,
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextAlarmTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    nextAlarmTime,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            // Handle permission denied
        }
    }
}
