package com.daohoangson.droidbot

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import kotlin.math.roundToInt

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
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    private var handler: Handler? = null
    private var screenCaptureRunnable: Runnable? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        handler = Handler(Looper.getMainLooper())
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
                .apply {
                    // callback must be registered first
                    registerCallback(object : MediaProjection.Callback() {
                        override fun onStop() {
                            virtualDisplay?.release()
                            virtualDisplay = null

                            imageReader?.close()
                            imageReader = null
                        }
                    }, null)
                }.apply {
                    val metrics = resources.displayMetrics
                    imageReader = ImageReader.newInstance(
                        metrics.widthPixels, metrics.heightPixels, PixelFormat.RGBA_8888, 2
                    )

                    virtualDisplay = createVirtualDisplay(
                        "TakeOverService",
                        metrics.widthPixels,
                        metrics.heightPixels,
                        metrics.densityDpi,
                        DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                        imageReader?.surface,
                        null,
                        null
                    )
                }

            screenCaptureRunnable = object : Runnable {
                override fun run() {
                    captureScreen()
                    handler?.postDelayed(this, 1000)
                }
            }
            handler?.post(screenCaptureRunnable!!)
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

        val runnable = screenCaptureRunnable
        if (runnable != null) {
            handler?.removeCallbacks(runnable)
        }

        mediaProjection?.stop()
        mediaProjection = null
    }

    private fun captureScreen() {
        imageReader?.acquireLatestImage()?.apply {
            try {
                val plane0 = planes[0]
                val buffer = plane0.buffer
                val pixelStride = plane0.pixelStride
                val rowStride = plane0.rowStride
                val rowPadding = rowStride - pixelStride * width
                val bitmap = Bitmap.createBitmap(
                    width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888
                )
                bitmap.copyPixelsFromBuffer(buffer)

                val resized = resizeBitmapIfNeeded(bitmap)
                if (resized != bitmap) resized.recycle()

                bitmap.recycle()
            } finally {
                close()
            }
        }
    }

    private fun resizeBitmapIfNeeded(bitmap: Bitmap): Bitmap {
        val preferredWidth = 1280
        val preferredHeight = 800
        val ratio = bitmap.width / bitmap.height.toFloat()

        var resizedWidth = preferredWidth
        var resizedHeight = preferredHeight
        if (ratio > preferredWidth / preferredHeight) {
            resizedHeight = (preferredWidth / ratio).roundToInt()
        } else {
            resizedWidth = (preferredHeight * ratio).roundToInt()
        }

        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, true)
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
