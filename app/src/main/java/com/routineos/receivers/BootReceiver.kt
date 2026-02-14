package com.routineos.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.routineos.services.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON" ||
            intent.action == "com.htc.intent.action.QUICKBOOT_POWERON") {
            
            Log.d("BootReceiver", "Boot completed, rescheduling alarms")
            
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    AlarmScheduler.rescheduleAllAlarms(context)
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Error rescheduling alarms", e)
                }
            }
        }
    }
}
