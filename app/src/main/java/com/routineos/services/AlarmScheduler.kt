package com.routineos.services

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.routineos.data.Repository
import com.routineos.data.formatDateKey
import com.routineos.receivers.AlarmReceiver
import com.routineos.data.models.Event
import com.routineos.data.models.Task
import java.text.SimpleDateFormat
import java.util.*

object AlarmScheduler {
    
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    
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
        
        val today = formatDateKey(Date())
        
        events.forEach { event ->
            if (event.alarmEnabled && event.time != null && event.alarmTimestamp != null) {
                // Only schedule if the event is today or in the future
                if (event.date >= today) {
                    scheduleAlarm(
                        context = context,
                        title = event.title,
                        description = event.description,
                        alarmTime = event.alarmTimestamp,
                        isRecurring = event.type == "recurring",
                        eventId = event.id
                    )
                }
            }
        }
    }
    
    suspend fun scheduleTaskAlarms(context: Context, tasks: List<Task>) {
        val repository = Repository(context)
        val settings = repository.getSettings()
        
        if (!settings.alarmsEnabled) return
        
        val today = formatDateKey(Date())
        
        tasks.forEach { task ->
            // Schedule task reminders at 8 AM for daily tasks
            if (task.type == "recurring") {
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 8)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    
                    // If it's already past 8 AM, schedule for tomorrow
                    if (timeInMillis <= System.currentTimeMillis()) {
                        add(Calendar.DAY_OF_YEAR, 1)
                    }
                }
                
                scheduleAlarm(
                    context = context,
                    title = "Task Reminder: ${task.title}",
                    description = task.description.ifEmpty { "Don't forget to complete this task today!" },
                    alarmTime = calendar.timeInMillis,
                    isRecurring = true,
                    taskId = task.id
                )
            }
        }
    }
    
    suspend fun rescheduleAllAlarms(context: Context) {
        val repository = Repository(context)
        val events = repository.getEvents()
        val tasks = repository.getTasks()
        
        // Cancel all existing alarms first
        cancelAllAlarms(context, events, tasks)
        
        // Reschedule
        scheduleEventAlarms(context, events)
        scheduleTaskAlarms(context, tasks)
    }
    
    private fun cancelAllAlarms(context: Context, events: List<Event>, tasks: List<Task>) {
        events.forEach { event ->
            if (event.alarmEnabled) {
                cancelAlarm(context, eventId = event.id)
            }
        }
        
        tasks.forEach { task ->
            if (task.type == "recurring") {
                cancelAlarm(context, taskId = task.id)
            }
        }
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
