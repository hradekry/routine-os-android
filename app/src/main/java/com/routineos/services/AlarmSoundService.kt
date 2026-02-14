package com.routineos.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.routineos.MainActivity
import com.routineos.R
import kotlin.math.exp
import kotlin.math.sin

class AlarmSoundService : Service() {

    companion object {
        const val ACTION_DISMISS = "com.routineos.DISMISS_ALARM"
        private const val CHANNEL_ID = "routine_os_alarm_sound"
        private const val CHANNEL_NAME = "Routine OS Alarm"
        private const val NOTIFICATION_ID = 2001
        private const val SAMPLE_RATE = 44100

        fun start(context: Context, title: String, description: String) {
            val intent = Intent(context, AlarmSoundService::class.java).apply {
                putExtra("title", title)
                putExtra("description", description)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AlarmSoundService::class.java))
        }
    }

    private var audioTrack: AudioTrack? = null
    private var isPlaying = false
    private var playThread: Thread? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_DISMISS) {
            stopPlayback()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val title = intent?.getStringExtra("title") ?: "Routine OS Alarm"
        val description = intent?.getStringExtra("description") ?: ""

        startForeground(NOTIFICATION_ID, buildNotification(title, description))
        startDrumLoop()
        startVibrationLoop()

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Active alarm sound"
                    setSound(null, null) // We handle sound ourselves
                    enableVibration(false) // We handle vibration ourselves
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    private fun buildNotification(title: String, description: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPending = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(this, AlarmSoundService::class.java).apply {
            action = ACTION_DISMISS
        }
        val dismissPending = PendingIntent.getService(
            this, 1, dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(description)
            .setContentIntent(openPending)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .addAction(R.drawable.ic_check, "Dismiss", dismissPending)
            .build()
    }

    /**
     * Generates a short kick-drum sound as PCM samples.
     * Low-frequency sine wave (70 Hz) with fast exponential decay (~200 ms).
     */
    private fun generateDrumHit(): ShortArray {
        val durationMs = 220
        val numSamples = SAMPLE_RATE * durationMs / 1000
        val samples = ShortArray(numSamples)
        val freq = 70.0
        val twoPiF = 2.0 * Math.PI * freq / SAMPLE_RATE

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val envelope = exp(-t * 12.0) // fast decay
            val sample = (sin(twoPiF * i) * envelope * Short.MAX_VALUE * 0.8).toInt()
            samples[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return samples
    }

    /**
     * Generates silence of given duration in ms.
     */
    private fun generateSilence(durationMs: Int): ShortArray {
        val numSamples = SAMPLE_RATE * durationMs / 1000
        return ShortArray(numSamples) // all zeros = silence
    }

    private fun startDrumLoop() {
        if (isPlaying) return
        isPlaying = true

        val drumHit = generateDrumHit()
        val silence = generateSilence(900) // 900ms pause between beats

        // Build one cycle: hit + silence
        val cycle = ShortArray(drumHit.size + silence.size)
        drumHit.copyInto(cycle, 0)
        silence.copyInto(cycle, drumHit.size)

        val bufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(cycle.size * 2)

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack?.play()

        playThread = Thread {
            try {
                while (isPlaying) {
                    audioTrack?.write(cycle, 0, cycle.size)
                }
            } catch (_: Exception) {}
        }
        playThread?.start()
    }

    private fun startVibrationLoop() {
        val pattern = longArrayOf(0, 150, 950) // vibrate 150ms, pause 950ms — matches drum rhythm
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, 0)
        }
    }

    private fun stopPlayback() {
        isPlaying = false
        try {
            playThread?.join(500)
        } catch (_: Exception) {}
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null

        // Stop vibration
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        vibrator.cancel()
    }

    override fun onDestroy() {
        stopPlayback()
        super.onDestroy()
    }
}
