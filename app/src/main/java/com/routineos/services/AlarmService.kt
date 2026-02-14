package com.routineos.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import com.routineos.MainActivity
import com.routineos.R

class AlarmService : Service() {
    
    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "alarm_channel"
        private const val CHANNEL_NAME = "Routine OS Alarms"
        
        fun startService(context: Context) {
            val intent = Intent(context, AlarmService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stopService(context: Context) {
            val intent = Intent(context, AlarmService::class.java)
            context.stopService(intent)
        }
    }
    
    private var notificationManager: NotificationManager? = null
    private var vibrator: Vibrator? = null
    
    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "SHOW_ALARM" -> {
                val title = intent.getStringExtra("title") ?: "Routine OS"
                val description = intent.getStringExtra("description") ?: ""
                showAlarmNotification(title, description)
                playAlarmSound()
                vibrate()
            }
            "STOP_ALARM" -> {
                stopAlarmSound()
                stopVibration()
            }
        }
        
        // Keep service running with a persistent notification
        startForeground(NOTIFICATION_ID, createPersistentNotification())
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for Routine OS alarms and reminders"
                enableLights(true)
                enableVibration(true)
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }
    
    private fun createPersistentNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Routine OS")
            .setContentText("Alarm service is running")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }
    
    private fun showAlarmNotification(title: String, description: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val stopIntent = Intent(this, AlarmService::class.java).apply {
            action = "STOP_ALARM"
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(description)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "Stop", stopPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .build()
        
        notificationManager?.notify(System.currentTimeMillis().toInt(), notification)
    }
    
    private fun playAlarmSound() {
        try {
            val ringtone = RingtoneManager.getRingtone(
                this,
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            )
            ringtone?.play()
        } catch (e: Exception) {
            // Fallback to system default sound
        }
    }
    
    private fun stopAlarmSound() {
        // Stop any playing alarm sounds
        // This would need to track the ringtone instance
    }
    
    private fun vibrate() {
        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(
                    VibrationEffect.createWaveform(
                        longArrayOf(0, 500, 200, 500, 200, 500, 200, 500),
                        2 // Repeat
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(longArrayOf(0, 500, 200, 500, 200, 500, 200, 500), 2)
            }
        }
    }
    
    private fun stopVibration() {
        vibrator?.cancel()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopVibration()
    }
}
