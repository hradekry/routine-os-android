package com.routineos.services

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.routineos.data.Repository
import com.routineos.receivers.AlarmReceiver
import com.routineos.data.models.Event

object AlarmScheduler {
    
    fun scheduleAlarm(context: Context, title: String, description: String, alarmTime: Long, isRecurring: Boolean, eventId: String? = null, taskId: String? = null) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("title", title)
            putExtra("description", description)
            putExtra("eventId", eventId)
            putExtra("taskId", taskId)
            putExtra("recurring", isRecurring)
            putExtra("alarmTime", alarmTime)
        }
        
        val requestCode = (eventId ?: taskId ?: System.currentTimeMillis()).hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        alarmTime,
                        pendingIntent
                    )
                } else {
                    // Fallback to inexact alarm
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        alarmTime,
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    alarmTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    alarmTime,
                    pendingIntent
                )
            }
            
            Log.d("AlarmScheduler", "Scheduled alarm for $title at $alarmTime")
        } catch (e: SecurityException) {
            Log.e("AlarmScheduler", "Permission denied for exact alarm", e)
        }
    }
    
    fun cancelAlarm(context: Context, eventId: String? = null, taskId: String? = null) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        
        val requestCode = (eventId ?: taskId ?: System.currentTimeMillis()).hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        alarmManager.cancel(pendingIntent)
        Log.d("AlarmScheduler", "Cancelled alarm for ${eventId ?: taskId}")
    }
    
    suspend fun scheduleEventAlarms(context: Context, events: List<Event>) {
        val repository = Repository(context)
        val settings = repository.getSettings()
        
        if (!settings.alarmsEnabled) return
        
        val now = System.currentTimeMillis()
        
        events.forEach { event ->
            if (event.alarmEnabled && event.time != null && event.alarmTimestamp != null) {
                if (event.alarmTimestamp > now) {
                    scheduleAlarm(
                        context = context,
                        title = event.title,
                        description = event.description,
                        alarmTime = event.alarmTimestamp,
                        isRecurring = false,
                        eventId = event.id
                    )
                }
            }
        }
    }
    
    suspend fun rescheduleAllAlarms(context: Context) {
        val repository = Repository(context)
        val events = repository.getEvents()
        
        // Cancel existing event alarms first
        events.forEach { event ->
            if (event.alarmEnabled) {
                cancelAlarm(context, eventId = event.id)
            }
        }
        
        // Reschedule future event alarms only
        scheduleEventAlarms(context, events)
    }
    
    fun requestExactAlarmPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true // Permission not required on older versions
        }
    }
    
    fun requestNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Check if permission is granted
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Permission not required on older versions
        }
    }
}
