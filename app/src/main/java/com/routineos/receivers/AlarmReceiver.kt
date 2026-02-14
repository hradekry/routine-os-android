package com.routineos.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.routineos.services.AlarmSoundService

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "Routine OS"
        val description = intent.getStringExtra("description") ?: ""

        // Start the looping alarm sound service — it shows its own
        // foreground notification with a Dismiss button.
        AlarmSoundService.start(context, title, description)
    }
}
