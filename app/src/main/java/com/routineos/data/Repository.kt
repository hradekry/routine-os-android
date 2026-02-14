package com.routineos.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.routineos.data.models.AppSettings
import com.routineos.data.models.Event
import com.routineos.data.models.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class Repository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("routine_os", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    companion object {
        private const val KEY_EVENTS = "routine-os-events"
        private const val KEY_TASKS = "routine-os-tasks"
        private const val KEY_SETTINGS = "routine-os-settings"
        private const val KEY_SELECTED_DATE = "routine-os-selected-date"
        private const val KEY_CURRENT_CONTEXT = "routine-os-context"
    }
    
    // Events
    suspend fun getEvents(): List<Event> = withContext(Dispatchers.IO) {
        val json = prefs.getString(KEY_EVENTS, "[]") ?: "[]"
        val type = object : TypeToken<List<Event>>() {}.type
        gson.fromJson(json, type) ?: emptyList()
    }
    
    suspend fun saveEvents(events: List<Event>) = withContext(Dispatchers.IO) {
        val json = gson.toJson(events)
        prefs.edit().putString(KEY_EVENTS, json).apply()
    }
    
    // Tasks
    suspend fun getTasks(): List<Task> = withContext(Dispatchers.IO) {
        val json = prefs.getString(KEY_TASKS, "[]") ?: "[]"
        val type = object : TypeToken<List<Task>>() {}.type
        gson.fromJson(json, type) ?: emptyList()
    }
    
    suspend fun saveTasks(tasks: List<Task>) = withContext(Dispatchers.IO) {
        val json = gson.toJson(tasks)
        prefs.edit().putString(KEY_TASKS, json).apply()
    }
    
    // Settings
    suspend fun getSettings(): AppSettings = withContext(Dispatchers.IO) {
        val json = prefs.getString(KEY_SETTINGS, null)
        if (json != null) {
            gson.fromJson(json, AppSettings::class.java)
        } else {
            AppSettings()
        }
    }
    
    suspend fun saveSettings(settings: AppSettings) = withContext(Dispatchers.IO) {
        val json = gson.toJson(settings)
        prefs.edit().putString(KEY_SETTINGS, json).apply()
    }
    
    // Selected Date
    fun getSelectedDate(): Date {
        val dateStr = prefs.getString(KEY_SELECTED_DATE, null)
        return if (dateStr != null) {
            dateFormat.parse(dateStr) ?: Date()
        } else {
            Date()
        }
    }
    
    fun saveSelectedDate(date: Date) {
        prefs.edit().putString(KEY_SELECTED_DATE, dateFormat.format(date)).apply()
    }
    
    // Current Context
    fun getCurrentContext(): String {
        return prefs.getString(KEY_CURRENT_CONTEXT, "calendar") ?: "calendar"
    }
    
    fun saveCurrentContext(context: String) {
        prefs.edit().putString(KEY_CURRENT_CONTEXT, context).apply()
    }
    
    // Utility
    fun formatDateKey(date: Date): String {
        return dateFormat.format(date)
    }
    
    fun parseDateKey(dateStr: String): Date {
        return dateFormat.parse(dateStr) ?: Date()
    }
    
    // Export/Import
    suspend fun exportData(): String = withContext(Dispatchers.IO) {
        val events = getEvents()
        val tasks = getTasks()
        val settings = getSettings()
        val today = formatDateKey(Date())
        
        val exportData = mapOf(
            "events" to events,
            "tasks" to tasks,
            "settings" to settings,
            "timestamp" to Date().toISOString(),
            "exportDate" to today
        )
        
        gson.toJson(exportData)
    }
    
    suspend fun importData(jsonData: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val type = object : TypeToken<Map<String, Any>>() {}.type
            val data: Map<String, Any> = gson.fromJson(jsonData, type)
            
            val eventsJson = gson.toJson(data["events"])
            val tasksJson = gson.toJson(data["tasks"])
            val settingsJson = gson.toJson(data["settings"])
            
            prefs.edit()
                .putString(KEY_EVENTS, eventsJson)
                .putString(KEY_TASKS, tasksJson)
                .putString(KEY_SETTINGS, settingsJson)
                .apply()
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        prefs.edit()
            .remove(KEY_EVENTS)
            .remove(KEY_TASKS)
            .putString(KEY_CURRENT_CONTEXT, "calendar")
            .apply()
    }
}
