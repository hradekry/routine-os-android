package com.routineos

import android.app.Application
import com.routineos.services.AlarmService

class RoutineOSApp : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Start the alarm service to ensure it can receive broadcasts
        AlarmService.startService(this)
    }
}
