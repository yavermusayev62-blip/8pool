package com.poolmod.menu

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

class ScreenCaptureService : Service() {

    private var mediaProjection: MediaProjection? = null
    private var imageReader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var screenWidth = 0
    private var screenHeight = 0
    private var densityDpi = 0
    
    private val handler = Handler(Looper.getMainLooper())
    
    private var isCapturing = false
    private val captureInterval = 500L // 500ms (2 FPS - performans için)

    override fun onCreate() {
        super.onCreate()
        val metrics = resources.displayMetrics
        screenWidth = metrics.widthPixels
        screenHeight = metrics.heightPixels
        densityDpi = metrics.densityDpi
        
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val resultCode = intent.getIntExtra("result_code", -1)
                @Suppress("ExplicitTypeArguments")
                val resultData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra("result_data", Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra<Intent>("result_data")
                }
                
                if (resultCode != -1 && resultData != null) {
                    startCapture(resultCode, resultData)
                }
            }
            ACTION_STOP -> {
                stopCapture()
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startCapture(resultCode: Int, resultData: Intent) {
        if (isCapturing) return
        
        try {
            val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = projectionManager.getMediaProjection(resultCode, resultData)
            
            imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2)
            imageReader?.setOnImageAvailableListener({ reader ->
                var image: Image? = null
                try {
                    image = reader.acquireLatestImage()
                    if (image != null) {
                        processImage(image)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Image işleme hatası: ${e.message}", e)
                } finally {
                    // Image'i mutlaka kapat
                    image?.close()
                }
            }, handler)
            
            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "ScreenCapture",
                screenWidth,
                screenHeight,
                densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface,
                null,
                handler
            )
            
            isCapturing = true
            startForeground(1, createNotification())
            
            // Periyodik yakalama başlat
            startPeriodicCapture()
            
            Log.d(TAG, "Ekran yakalama başlatıldı")
        } catch (e: Exception) {
            Log.e(TAG, "Ekran yakalama hatası: ${e.message}", e)
        }
    }

    private fun startPeriodicCapture() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (isCapturing) {
                    // ImageReader otomatik olarak yeni görüntüleri gönderecek
                    handler.postDelayed(this, captureInterval)
                }
            }
        }, captureInterval)
    }

    private fun processImage(image: Image) {
        var bitmap: Bitmap? = null
        var croppedBitmap: Bitmap? = null
        try {
            val planes = image.planes
            if (planes.isEmpty()) {
                Log.w(TAG, "Image planes boş")
                return
            }
            
            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * screenWidth
            
            if (pixelStride == 0) {
                Log.w(TAG, "Pixel stride 0, görüntü işlenemiyor")
                return
            }
            
            bitmap = Bitmap.createBitmap(
                screenWidth + rowPadding / pixelStride,
                screenHeight,
                Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(buffer)
            
            // Kareyi kırp
            croppedBitmap = Bitmap.createBitmap(bitmap, 0, 0, screenWidth, screenHeight)
            bitmap.recycle()
            bitmap = null
            
            // Broadcast gönder
            sendBitmapBroadcast(croppedBitmap)
            croppedBitmap = null // sendBitmapBroadcast içinde recycle edilecek
            
        } catch (e: Exception) {
            Log.e(TAG, "Görüntü işleme hatası: ${e.message}", e)
        } finally {
            // Güvenli temizlik
            bitmap?.recycle()
            croppedBitmap?.recycle()
        }
    }
    
    private fun sendBitmapBroadcast(bitmap: Bitmap) {
        try {
            // Bitmap'i byte array'e çevir (küçük boyut için)
            val outputStream = ByteArrayOutputStream()
            val compressed = bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            
            if (!compressed) {
                Log.w(TAG, "Bitmap sıkıştırılamadı")
                bitmap.recycle()
                return
            }
            
            val byteArray = outputStream.toByteArray()
            
            if (byteArray.isEmpty()) {
                Log.w(TAG, "Bitmap byte array boş")
                bitmap.recycle()
                return
            }
            
            val intent = Intent(ACTION_SCREENSHOT_READY).apply {
                putExtra("bitmap_data", byteArray)
                putExtra("width", bitmap.width)
                putExtra("height", bitmap.height)
            }
            
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
            
            bitmap.recycle()
        } catch (e: Exception) {
            Log.e(TAG, "Broadcast gönderme hatası: ${e.message}", e)
            // Hata durumunda da bitmap'i temizle
            try {
                bitmap.recycle()
            } catch (ex: Exception) {
                Log.e(TAG, "Bitmap recycle hatası: ${ex.message}")
            }
        }
    }

    private fun stopCapture() {
        isCapturing = false
        
        virtualDisplay?.release()
        virtualDisplay = null
        
        imageReader?.close()
        imageReader = null
        
        mediaProjection?.stop()
        mediaProjection = null
        
        Log.d(TAG, "Ekran yakalama durduruldu")
    }

    override fun onDestroy() {
        super.onDestroy()
        stopCapture()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Screen Capture",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ekran yakalama servisi"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("📸 Ekran Yakalama Aktif")
            .setContentText("8 Ball Pool analiz ediliyor")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        const val ACTION_START = "com.poolmod.menu.SCREEN_CAPTURE_START"
        const val ACTION_STOP = "com.poolmod.menu.SCREEN_CAPTURE_STOP"
        const val ACTION_SCREENSHOT_READY = "com.poolmod.menu.SCREENSHOT_READY"
        private const val CHANNEL_ID = "screen_capture_channel"
        private const val TAG = "ScreenCaptureService"
    }
}

