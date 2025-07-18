package com.waqas028.strange_call

import android.app.Activity
import android.app.Service
import android.content.Intent
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

class CallService : Service() {

    private lateinit var mediaProjection: MediaProjection

    override fun onCreate() {
        super.onCreate()

        val notification = NotificationCompat.Builder(this, "media_projection_channel")
            .setContentTitle("Screen Capture")
            .setContentText("Capturing your screen...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()

        startForeground(1, notification)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val resultCode = intent.getIntExtra("result_code", Activity.RESULT_CANCELED)
        val data = intent.getParcelableExtra<Intent>("data_intent")

        if (resultCode == Activity.RESULT_OK && data != null) {
            val mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)!!

            Log.d("YourForegroundService", "MediaProjection started successfully")
        } else {
            Log.e("YourForegroundService", "Failed to obtain MediaProjection.")
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("YourForegroundService", "Service destroyed")
    }
}

