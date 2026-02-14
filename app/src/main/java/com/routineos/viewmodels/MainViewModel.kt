package com.routineos.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.routineos.data.Repository
import com.routineos.data.models.AppSettings
import com.routineos.data.models.Event
import com.routineos.data.models.Task
import com.routineos.services.AlarmScheduler
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = Repository(application)
    private val context = application.applicationContext
    
    private val _events = MutableLiveData<List<Event>>()
    val events: LiveData<List<Event>> = _events
    
    private val _tasks = MutableLiveData<List<Task>>()
    val tasks: LiveData<List<Task>> = _tasks
    
    private val _settings = MutableLiveData<AppSettings>()
    val settings: LiveData<AppSettings> = _settings
    
    private val _currentContext = MutableLiveData<String>()
    val currentContext: LiveData<String> = _currentContext
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error
    
    init {
        loadData()
    }
    
    private fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _events.value = ArrayList(repository.getEvents())
                _tasks.value = ArrayList(repository.getTasks())
                _settings.value = repository.getSettings()
                _currentContext.value = repository.getCurrentContext()
            } catch (e: Exception) {
                _error.value = "Failed to load data: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun onPermissionsGranted() {
        viewModelScope.launch {
            try {
                AlarmScheduler.scheduleEventAlarms(context, _events.value ?: emptyList())
            } catch (e: Exception) {
                _error.value = "Failed to schedule alarms: ${e.message}"
            }
        }
    }
    
    fun saveEvents(events: List<Event>) {
        viewModelScope.launch {
            try {
                repository.saveEvents(events)
                _events.value = ArrayList(events)
                
                // Schedule alarms only for future events
                val settings = _settings.value ?: AppSettings()
                if (settings.alarmsEnabled) {
                    val now = System.currentTimeMillis()
                    events.filter { it.alarmEnabled && it.alarmTimestamp != null && it.alarmTimestamp > now }
                        .forEach { event ->
                            AlarmScheduler.scheduleAlarm(
                                context, event.title, event.description,
                                event.alarmTimestamp!!, false, eventId = event.id
                            )
                        }
                }
            } catch (e: Exception) {
                _error.value = "Failed to save events: ${e.message}"
            }
        }
    }
    
    fun saveTasks(tasks: List<Task>) {
        viewModelScope.launch {
            try {
                repository.saveTasks(tasks)
                _tasks.value = ArrayList(tasks)
            } catch (e: Exception) {
                _error.value = "Failed to save tasks: ${e.message}"
            }
        }
    }
    
    fun saveSettings(settings: AppSettings) {
        viewModelScope.launch {
            try {
                repository.saveSettings(settings)
                _settings.value = settings
                
                if (settings.alarmsEnabled) {
                    AlarmScheduler.scheduleEventAlarms(context, _events.value ?: emptyList())
                } else {
                    _events.value?.forEach { event ->
                        if (event.alarmEnabled) {
                            AlarmScheduler.cancelAlarm(context, eventId = event.id)
                        }
                    }
                }
            } catch (e: Exception) {
                _error.value = "Failed to save settings: ${e.message}"
            }
        }
    }
    
    fun setCurrentContext(context: String) {
        repository.saveCurrentContext(context)
        _currentContext.value = context
    }
    
    fun clearError() {
        _error.value = null
    }
    
    suspend fun exportData(): String {
        return repository.exportData()
    }
    
    suspend fun importData(jsonData: String): Boolean {
        return try {
            val success = repository.importData(jsonData)
            if (success) {
                loadData()
            }
            success
        } catch (e: Exception) {
            _error.value = "Failed to import data: ${e.message}"
            false
        }
    }
    
    suspend fun clearAllData() {
        repository.clearAllData()
        loadData()
    }
}
