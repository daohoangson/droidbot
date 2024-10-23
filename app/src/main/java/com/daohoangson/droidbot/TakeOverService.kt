package com.daohoangson.droidbot

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.IBinder
import androidx.core.app.NotificationCompat

class TakeOverService : Service() {
    private object Notification {
        const val CHANNEL_ID = "TakeOverService"
        const val NOTIFICATION_ID = 1
    }

    companion object {
        const val ACTION_STOP_CAPTURE = "STOP_CAPTURE"
        const val EXTRA_INT_MEDIA_PROJECTION_RESULT_CODE = "mediaProjectionResultCode"
        const val EXTRA_PARCELABLE_MEDIA_PROJECTION_RESULT_DATA = "mediaProjectionResultData"
    }

    private var mediaProjection: MediaProjection? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            return START_NOT_STICKY
        }

        val resultCode = intent.getIntExtra(EXTRA_INT_MEDIA_PROJECTION_RESULT_CODE, 0)
        val resultData = intent.getParcelableExtra(
            EXTRA_PARCELABLE_MEDIA_PROJECTION_RESULT_DATA, Intent::class.java
        )

        if (resultCode != 0 && resultData != null) {
            startForeground(
                Notification.NOTIFICATION_ID,
                createNotification(),
                FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )

            val mpm = getSystemService(MediaProjectionManager::class.java)
            mediaProjection = mpm.getMediaProjection(resultCode, resultData)
        } else if (intent.action == ACTION_STOP_CAPTURE) {
            stopSelf()
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not yet implemented")
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaProjection?.stop()
        mediaProjection = null
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            Notification.CHANNEL_ID,
            getString(R.string.take_over),
            NotificationManager.IMPORTANCE_HIGH
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(): android.app.Notification {
        val stopIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, TakeOverService::class.java).setAction(ACTION_STOP_CAPTURE),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, Notification.CHANNEL_ID)
            .setContentTitle(getString(R.string.take_over_notification))
            .setSmallIcon(R.drawable.ic_launcher_foreground).addAction(
                R.drawable.ic_launcher_foreground, getString(R.string.take_over_stop), stopIntent
            ).build()
    }
}
