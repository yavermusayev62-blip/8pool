package com.poolmod.menu

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.core.graphics.toColorInt
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ModMenuService : LifecycleService() {

    private var modMenuView: ModMenuView? = null
    private var toggleButton: ModToggleButton? = null
    private var overlayDrawView: OverlayDrawView? = null
    private var windowManager: WindowManager? = null
    private var gamePackage: String? = null
    private val handler = Handler(Looper.getMainLooper())
    
    private var isTrajectoryEnabled = false
    private var isAutoAimEnabled = false
    private var isScreenCaptureRunning = false
    private var currentBalls: List<BallDetector.Ball> = emptyList()
    private var currentTrajectories: List<PhysicsCalculator.BallTrajectory> = emptyList()
    private var currentHoles: List<HoleDetector.Hole> = emptyList()
    private var currentAutoAimTarget: AutoAimEngine.AimTarget? = null
    private var tableBounds: BallDetector.TableBounds? = null
    private lateinit var modConfig: ModMenuConfig
    
    private var menuLayoutParams: WindowManager.LayoutParams? = null
    private var toggleButtonLayoutParams: WindowManager.LayoutParams? = null

    private val screenshotReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            try {
                when (intent?.action) {
                    ScreenCaptureService.ACTION_SCREENSHOT_READY -> {
                        val byteArray = intent.getByteArrayExtra("bitmap_data")
                        val width = intent.getIntExtra("width", 0)
                        val height = intent.getIntExtra("height", 0)
                        
                        if (byteArray != null && byteArray.isNotEmpty() && width > 0 && height > 0) {
                            val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                            if (bitmap != null && !bitmap.isRecycled) {
                                onScreenshotReceived(bitmap)
                            } else {
                                Log.w("ModMenuService", "Bitmap decode edilemedi veya zaten recycle edilmiş")
                            }
                        } else {
                            Log.w("ModMenuService", "Geçersiz bitmap verisi: byteArray=${byteArray != null}, width=$width, height=$height")
                        }
                    }
                    ACTION_SCREEN_CAPTURE_DENIED -> {
                        val oldValue = isScreenCaptureRunning
                        Log.d("ModMenuService", "⚠️ Screen capture permission denied - switch'leri kapatılıyor (isScreenCaptureRunning: $oldValue -> false)")
                        DebugLogger.logInfo("ModMenuService", "Screen capture permission denied - mod'lar kapatılıyor")
                        // İzin verilmediğinde switch'leri kapat
                        handler.post {
                            isScreenCaptureRunning = false
                            disableModsRequiringScreenCapture()
                        }
                    }
                    ScreenCaptureService.ACTION_CAPTURE_STATE_CHANGED -> {
                        val isRunning = intent.getBooleanExtra("is_running", false)
                        val oldValue = isScreenCaptureRunning
                        isScreenCaptureRunning = isRunning
                        Log.d("ModMenuService", "✅ Screen capture state güncellendi: isRunning=$isRunning (önceki değer: $oldValue)")
                        DebugLogger.logInfo("ModMenuService", "Screen capture state değişti: $oldValue -> $isRunning")
                        
                        // Eğer servis beklenmedik şekilde durduysa logla
                        if (oldValue && !isRunning && (isTrajectoryEnabled || isAutoAimEnabled)) {
                            Log.w("ModMenuService", "⚠️ UYARI: Screen capture servisi beklenmedik şekilde durdu! (trajectory=$isTrajectoryEnabled, autoAim=$isAutoAimEnabled)")
                            DebugLogger.logWarning("ModMenuService", "Screen capture servisi beklenmedik şekilde durdu - trajectory veya auto aim aktifken")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ModMenuService", "Broadcast receiver hatası: ${e.message}", e)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        modConfig = ModMenuConfig(this) // Context artık hazır
        
        // State'leri config'den yükle
        isTrajectoryEnabled = modConfig.isModEnabled(ModMenuConfig.MOD_BALL_TRAJECTORY)
        isAutoAimEnabled = modConfig.isModEnabled(ModMenuConfig.MOD_AUTO_AIM)
        
        createNotificationChannel()
        startForeground(1, createNotification())
        
        // Broadcast receiver kaydet
        val filter = IntentFilter().apply {
            addAction(ScreenCaptureService.ACTION_SCREENSHOT_READY)
            addAction(ACTION_SCREEN_CAPTURE_DENIED)
            addAction(ScreenCaptureService.ACTION_CAPTURE_STATE_CHANGED)
        }
        @Suppress("DEPRECATION")
        LocalBroadcastManager.getInstance(this).registerReceiver(screenshotReceiver, filter)
        
        // Global broadcast receiver (ACTION_SCREEN_CAPTURE_DENIED için)
        val globalFilter = IntentFilter(ACTION_SCREEN_CAPTURE_DENIED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ (API 33+) için RECEIVER_NOT_EXPORTED flag'i gerekli
            // Bu sadece uygulama içi broadcast olduğu için NOT_EXPORTED kullanıyoruz
            registerReceiver(screenshotReceiver, globalFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(screenshotReceiver, globalFilter)
        }
        
        // Anti-cheat bypass aktif
        AntiCheatBypass.protectMemory()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("ModMenuService", "onStartCommand çağrıldı - action: ${intent?.action}")
        
        when (intent?.action) {
            ACTION_START -> {
                gamePackage = intent.getStringExtra("game_package")
                Log.d("ModMenuService", "ACTION_START alındı, gamePackage: $gamePackage")
                
                // Overlay izni kontrolü - AppOps ile birlikte
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val hasPermission = AppOpsHelper.checkAndStartOverlayPermission(this)
                    DebugLogger.logOverlayPermissionStatus("ModMenuService", hasPermission, "ACTION_START")
                    Log.d("ModMenuService", "Overlay izni (AppOps ile): $hasPermission")
                    
                    // Detaylı durum logla
                    val detailedStatus = AppOpsHelper.getDetailedOverlayPermissionStatus(this)
                    DebugLogger.logDebug("ModMenuService", detailedStatus)
                    
                    if (!hasPermission) {
                        Log.e("ModMenuService", "Overlay izni yok! Mod menu gösterilemez.")
                        DebugLogger.logAppOpsError("ModMenuService", "Overlay izni yok - Mod menu gösterilemez", 
                            android.os.Process.myUid(), packageName)
                        android.widget.Toast.makeText(this, "Overlay izni gerekli! Ayarlardan izin verin.", android.widget.Toast.LENGTH_LONG).show()
                        return START_STICKY // Service'i çalışır durumda tut
                    }
                }
                
                // Sadece toggle button'ı göster - mod menu kullanıcı tıkladığında açılacak
                handler.post {
                    Log.d("ModMenuService", "Handler.post çalışıyor - toggle button gösteriliyor")
                    showToggleButton()
                    // Mod menu'yu otomatik açma - kullanıcı toggle button'a tıkladığında açılacak
                }
                
                return START_STICKY // Service'i çalışır durumda tut
            }
            ACTION_STOP -> {
                Log.d("ModMenuService", "ACTION_STOP alındı - service durduruluyor")
                hideModMenu()
                hideToggleButton()
                stopScreenCapture()
                stopSelf()
                return START_NOT_STICKY // Service durduruldu
            }
            ACTION_TOGGLE -> {
                Log.d("ModMenuService", "ACTION_TOGGLE alındı")
                toggleMenu()
            }
            ACTION_START_SCREEN_CAPTURE -> {
                val resultCode = intent.getIntExtra("result_code", -1)
                @Suppress("ExplicitTypeArguments")
                val resultData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra("result_data", Intent::class.java)
                } else {
                    @Suppress("DEPRECATION", "ExplicitTypeArguments")
                    intent.getParcelableExtra<Intent>("result_data")
                }
                startScreenCapture(resultCode, resultData)
            }
            ACTION_TOGGLE_TRAJECTORY -> {
                // UI işləri main thread-də olmalıdır
                handler.post {
                    try {
                        toggleTrajectory()
                    } catch (e: Exception) {
                        Log.e("ModMenuService", "toggleTrajectory hatası: ${e.message}", e)
                    }
                }
            }
            ACTION_TOGGLE_AUTO_AIM -> {
                // UI işləri main thread-də olmalıdır
                handler.post {
                    try {
                        toggleAutoAim()
                    } catch (e: Exception) {
                        Log.e("ModMenuService", "toggleAutoAim hatası: ${e.message}", e)
                    }
                }
            }
            ACTION_REQUEST_SCREEN_CAPTURE_PERMISSION -> {
                // Mod menu'dan ekran yakalama izni isteği
                Log.d("ModMenuService", "ACTION_REQUEST_SCREEN_CAPTURE_PERMISSION alındı - mod menu'dan")
                handler.post {
                    try {
                        requestScreenCapturePermission()
                    } catch (e: Exception) {
                        Log.e("ModMenuService", "requestScreenCapturePermission hatası: ${e.message}", e)
                        DebugLogger.logException("ModMenuService", "requestScreenCapturePermission hatası", e)
                    }
                }
            }
            ACTION_HOLE_SETTINGS_CHANGED -> {
                // Delik pozisyon ayarları değişti - overlay'i yenile
                Log.d("ModMenuService", "ACTION_HOLE_SETTINGS_CHANGED alındı - overlay yenileniyor")
                handler.post {
                    try {
                        // Mevcut delik pozisyonlarını yeniden hesapla ve overlay'i güncelle
                        refreshOverlay()
                    } catch (e: Exception) {
                        Log.e("ModMenuService", "refreshOverlay hatası: ${e.message}", e)
                        DebugLogger.logException("ModMenuService", "refreshOverlay hatası", e)
                    }
                }
            }
            null -> {
                Log.d("ModMenuService", "Intent action null - service yeniden başlatılıyor olabilir")
                // Service yeniden başlatıldıysa sadece toggle button'ı göster
                if (toggleButton == null) {
                    handler.postDelayed({
                        showToggleButton()
                        // Mod menu'yu otomatik açma - kullanıcı toggle button'a tıkladığında açılacak
                    }, 500)
                }
            }
        }
        
        // Her durumda service'i çalışır durumda tut
        Log.d("ModMenuService", "onStartCommand tamamlandı - START_STICKY döndürülüyor")
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    /**
     * Küçük toggle butonunu göster
     */
    private fun showToggleButton() {
                // Overlay izni kontrolü - AppOps ile birlikte
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val hasPermission = AppOpsHelper.checkAndStartOverlayPermission(this)
                    DebugLogger.logOverlayPermissionStatus("ModMenuService", hasPermission, "showToggleButton()")
                    if (!hasPermission) {
                        DebugLogger.logAppOpsError("ModMenuService", "Overlay izni yok - Toggle button gösterilemiyor", 
                            android.os.Process.myUid(), packageName)
                        Log.e("ModMenuService", "Overlay izni yok - toggle button gösterilemiyor!")
                        return
                    }
                }
        
        if (toggleButton != null) return

        // Oyun kontrolünü kaldırdık - her zaman göster
        toggleButton = ModToggleButton(this)
        
        // Direkt callback - onTouchEvent'ten çağrılacak
        // setOnClickListener-i silmək lazımdır, çünki iki dəfə toggle olur
        toggleButton?.onClickCallback = {
            android.util.Log.d("ModMenuService", "Toggle button callback çağrıldı!")
            toggleMenu()
        }
        
        // Sürükleme için callback
        toggleButton?.onPositionUpdate = { x, y ->
            android.util.Log.d("ModMenuService", "onPositionUpdate çağrıldı: x=$x, y=$y")
            toggleButtonLayoutParams?.let { params ->
                params.x = x
                params.y = y
                try {
                    windowManager?.updateViewLayout(toggleButton, params)
                    android.util.Log.d("ModMenuService", "Toggle button pozisyonu güncellendi")
                    
                    // Mod menu açıqsa, onun pozisiyasını da yenilə
                    if (modMenuView != null && modMenuView!!.isAttachedToWindow && menuLayoutParams != null) {
                        updateModMenuPosition(x, y)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ModMenuService", "Toggle button pozisyonu güncellenemedi", e)
                }
            }
        }

        val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        
        toggleButtonLayoutParams = WindowManager.LayoutParams(
            80,
            80,
            windowType,
            // Overlay'ler için touch event'lerin çalışması üçün flag-lər
            // FLAG_NOT_TOUCH_MODAL: Butonun dışındaki dokunmalar oyuna geçer, butonun kendisi dokunmaları alır
            // FLAG_NOT_FOCUSABLE: Focus almasın, amma touch event-ləri alır
            // FLAG_WATCH_OUTSIDE_TOUCH: Xaricdəki touch-ları izlə (touch event-lərin işləməsi üçün)
            // FLAG_LAYOUT_IN_SCREEN: Ekranda düzgün konumlanma
            // FLAG_LAYOUT_NO_LIMITS: Ekran sınırları dışına çıkabilme
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.OPAQUE
        ).apply {
            x = 20
            y = 100
            gravity = android.view.Gravity.START or android.view.Gravity.TOP
            alpha = 1.0f
        }
        
        // Callback-in null olmadığını yoxla
        android.util.Log.d("ModMenuService", "Toggle button callback set edildi: ${toggleButton?.onClickCallback != null}")

        try {
            android.util.Log.d("ModMenuService", "=== TOGGLE BUTTON EKLENİYOR ===")
            android.util.Log.d("ModMenuService", "Toggle button: $toggleButton")
            android.util.Log.d("ModMenuService", "Toggle button layout params: $toggleButtonLayoutParams")
            android.util.Log.d("ModMenuService", "Toggle button flags: ${toggleButtonLayoutParams?.flags}")
            android.util.Log.d("ModMenuService", "Toggle button type: ${toggleButtonLayoutParams?.type}")
            android.util.Log.d("ModMenuService", "Toggle button format: ${toggleButtonLayoutParams?.format}")
            android.util.Log.d("ModMenuService", "Toggle button x: ${toggleButtonLayoutParams?.x}, y: ${toggleButtonLayoutParams?.y}")
            android.util.Log.d("ModMenuService", "Toggle button width: ${toggleButtonLayoutParams?.width}, height: ${toggleButtonLayoutParams?.height}")
            android.util.Log.d("ModMenuService", "Toggle button isClickable: ${toggleButton?.isClickable}")
            android.util.Log.d("ModMenuService", "Toggle button isFocusable: ${toggleButton?.isFocusable}")
            android.util.Log.d("ModMenuService", "Toggle button visibility: ${toggleButton?.visibility}")
            android.util.Log.d("ModMenuService", "Toggle button alpha: ${toggleButton?.alpha}")
            
            try {
                windowManager?.addView(toggleButton, toggleButtonLayoutParams)
                DebugLogger.logWindowManagerOperation("ModMenuService", "addView(toggleButton)", true, 
                    "Toggle button başarıyla eklendi")
                android.util.Log.d("ModMenuService", "✅ Toggle button WindowManager'a eklendi")
            } catch (e: SecurityException) {
                DebugLogger.logWindowManagerOperation("ModMenuService", "addView(toggleButton)", false, 
                    "SecurityException: ${e.message}")
                DebugLogger.logException("ModMenuService", "Toggle button eklenirken SecurityException", e)
                throw e
            } catch (e: Exception) {
                DebugLogger.logWindowManagerOperation("ModMenuService", "addView(toggleButton)", false, 
                    "${e.javaClass.simpleName}: ${e.message}")
                DebugLogger.logException("ModMenuService", "Toggle button eklenirken hata", e)
                throw e
            }
            
            // View'ın durumunu kontrol et
            Handler(Looper.getMainLooper()).postDelayed({
                android.util.Log.d("ModMenuService", "=== TOGGLE BUTTON DURUMU (500ms sonra) ===")
                android.util.Log.d("ModMenuService", "Toggle button visibility: ${toggleButton?.visibility}")
                android.util.Log.d("ModMenuService", "Toggle button alpha: ${toggleButton?.alpha}")
                android.util.Log.d("ModMenuService", "Toggle button width: ${toggleButton?.width}, height: ${toggleButton?.height}")
                android.util.Log.d("ModMenuService", "Toggle button measured: ${toggleButton?.measuredWidth}x${toggleButton?.measuredHeight}")
                android.util.Log.d("ModMenuService", "Toggle button parent: ${toggleButton?.parent}")
                android.util.Log.d("ModMenuService", "Toggle button isAttachedToWindow: ${toggleButton?.isAttachedToWindow}")
                android.util.Log.d("ModMenuService", "Toggle button isClickable: ${toggleButton?.isClickable}")
                android.util.Log.d("ModMenuService", "Toggle button isFocusable: ${toggleButton?.isFocusable}")
                android.util.Log.d("ModMenuService", "Toggle button isFocusableInTouchMode: ${toggleButton?.isFocusableInTouchMode}")
                
                // Layout params'ı tekrar kontrol et
                val params = toggleButton?.layoutParams as? WindowManager.LayoutParams
                android.util.Log.d("ModMenuService", "Layout params flags: ${params?.flags}")
                android.util.Log.d("ModMenuService", "Layout params type: ${params?.type}")
                android.util.Log.d("ModMenuService", "Layout params format: ${params?.format}")
            }, 500)
            
            Log.d("ModMenuService", "Toggle button gösterildi")
        } catch (e: Exception) {
            Log.e("ModMenuService", "Toggle button gösterilemedi", e)
            e.printStackTrace()
        }
        
        // Overlay çizim view'ını ekleme - sadece gerektiğinde eklenecek (performans için)
        // showOverlayDrawView() // Lazy loading - sadece trajectory/auto aim aktifken
    }

    /**
     * Menüyü aç/kapat
     */
    private fun toggleMenu() {
        Log.d("ModMenuService", "=== toggleMenu() çağrıldı ===")
        
        // Menu-nun görünür olub olmadığını yoxla
        val isMenuVisible = try {
            modMenuView != null && modMenuView!!.isAttachedToWindow && modMenuView!!.visibility == android.view.View.VISIBLE
        } catch (e: Exception) {
            false
        }
        
        Log.d("ModMenuService", "modMenuView durumu: ${if (modMenuView == null) "null" else "mevcut (isAttached=${modMenuView?.isAttachedToWindow}, visibility=${modMenuView?.visibility})"}")
        Log.d("ModMenuService", "isMenuVisible: $isMenuVisible")
        
        if (isMenuVisible) {
            Log.d("ModMenuService", "Mod menu görünür - kapatılıyor...")
            hideModMenu()
        } else {
            Log.d("ModMenuService", "Mod menu görünmüyor - açılıyor...")
            // Eğer view var ama eklenmemişse, önce temizle
            if (modMenuView != null) {
                Log.d("ModMenuService", "Mod menu view var ama eklenmemiş - temizleniyor...")
                try {
                    windowManager?.removeView(modMenuView)
                } catch (e: Exception) {
                    // View zaten yoksa hata vermez
                    Log.d("ModMenuService", "View kaldırılırken hata (normal olabilir): ${e.message}")
                }
                modMenuView = null
            }
            showModMenu()
        }
    }

    /**
     * Mod menüsünü göster
     */
    private fun showModMenu() {
        Log.d("ModMenuService", "=== showModMenu() çağrıldı ===")
        
        // Overlay izni kontrolü - AppOps ile birlikte
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val hasPermission = AppOpsHelper.checkAndStartOverlayPermission(this)
            DebugLogger.logOverlayPermissionStatus("ModMenuService", hasPermission, "showModMenu()")
            Log.d("ModMenuService", "Overlay izni kontrolü (AppOps ile): $hasPermission")
            if (!hasPermission) {
                DebugLogger.logAppOpsError("ModMenuService", "Overlay izni yok - Mod menu gösterilemiyor", 
                    android.os.Process.myUid(), packageName)
                Log.e("ModMenuService", "❌ Overlay izni yok!")
                android.widget.Toast.makeText(this, "Overlay izni gerekli! Ayarlardan izin verin.", android.widget.Toast.LENGTH_LONG).show()
                return
            }
            Log.d("ModMenuService", "✅ Overlay izni var")
        }
        
        // View var ve eklenmişse, zaten gösteriliyor demektir
        val isAlreadyVisible = try {
            modMenuView != null && modMenuView!!.isAttachedToWindow && modMenuView!!.visibility == android.view.View.VISIBLE
        } catch (e: Exception) {
            false
        }
        
        if (isAlreadyVisible) {
            Log.d("ModMenuService", "Mod menu zaten gösteriliyor ve ekli - toggleMenu çağrılmalı")
            // Əgər menu artıq açıqdırsa, bağla
            hideModMenu()
            return
        }
        
        // View var ama eklenmemişse, temizle
        if (modMenuView != null) {
            Log.d("ModMenuService", "Mod menu view var ama eklenmemiş - temizleniyor")
            try {
                windowManager?.removeView(modMenuView)
            } catch (e: Exception) {
                // View zaten yoksa hata vermez
            }
            modMenuView = null
        }

        if (windowManager == null) {
            Log.e("ModMenuService", "❌ WindowManager null!")
            android.widget.Toast.makeText(this, "WindowManager null!", android.widget.Toast.LENGTH_LONG).show()
            return
        }
        Log.d("ModMenuService", "✅ WindowManager mevcut")

        try {
            Log.d("ModMenuService", "=== MOD MENU GÖSTERME BAŞLIYOR ===")
            DebugLogger.logDebug("ModMenuService", "=== MOD MENU GÖSTERME BAŞLIYOR ===")
            // Test view'ı kaldırdık - direkt gerçek menu'yu göster (performans için)
            showRealModMenu()
        } catch (e: SecurityException) {
            DebugLogger.logException("ModMenuService", "SecurityException - Overlay izni gerekli", e)
            Log.e("ModMenuService", "❌ SecurityException!", e)
            android.widget.Toast.makeText(this, "SecurityException: Overlay izni gerekli! ${e.message}", android.widget.Toast.LENGTH_LONG).show()
        } catch (e: IllegalArgumentException) {
            DebugLogger.logException("ModMenuService", "IllegalArgumentException - WindowManager parametreleri hatalı", e)
            Log.e("ModMenuService", "❌ IllegalArgumentException!", e)
            android.widget.Toast.makeText(this, "IllegalArgumentException: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            DebugLogger.logException("ModMenuService", "Genel hata - Mod menu gösterilemedi", e)
            Log.e("ModMenuService", "❌ Genel hata!", e)
            android.widget.Toast.makeText(this, "Hata: ${e.javaClass.simpleName} - ${e.message}", android.widget.Toast.LENGTH_LONG).show()
        }
    }
    
    /**
     * Gerçek mod menu'yu göster (test view'dan sonra)
     */
    private fun showRealModMenu() {
        Log.d("ModMenuService", "=== GERÇEK MOD MENU GÖSTERİLİYOR ===")
        
        if (modMenuView != null) {
            Log.d("ModMenuService", "Mod menu zaten var - gösteriliyor")
            // Mod menu zaten var ama görünmüyor olabilir, tekrar eklemeyi dene
            try {
                val existingView = modMenuView
                if (existingView != null) {
                    val isAttached = existingView.isAttachedToWindow
                    val parent = existingView.parent
                    Log.d("ModMenuService", "Mod menu durumu: isAttached=$isAttached, parent=$parent")
                    
                    if (!isAttached || parent == null) {
                        Log.d("ModMenuService", "Mod menu eklenmemiş - tekrar ekleniyor")
                        // Eğer menuLayoutParams yoksa, yeni oluştur
                        if (menuLayoutParams == null) {
                            val screenWidth = resources.displayMetrics.widthPixels
                            val screenHeight = resources.displayMetrics.heightPixels
                            // Yığcam menu - kiçik düzbucaqlı qutu (delik pozisyon kontrolleri için genişletildi)
                            val menuWidth = 280
                            val menuHeight = 380
                            val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                            } else {
                                @Suppress("DEPRECATION")
                                WindowManager.LayoutParams.TYPE_PHONE
                            }
                            
                            // Toggle button-un pozisiyasını al
                            val toggleX = toggleButtonLayoutParams?.x ?: 20
                            val toggleY = toggleButtonLayoutParams?.y ?: 100
                            
                            // Menu-nu toggle button-un sağında yerləşdir
                            val menuX = toggleX + 100
                            val menuY = toggleY
                            
                            // Ekran sərhədlərini yoxla
                            val finalX = menuX.coerceIn(0, screenWidth - menuWidth)
                            val finalY = menuY.coerceIn(0, screenHeight - menuHeight)
                            
                            menuLayoutParams = WindowManager.LayoutParams(
                                menuWidth,
                                menuHeight,
                                windowType,
                                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                                PixelFormat.OPAQUE
                            ).apply {
                                x = finalX
                                y = finalY
                                gravity = android.view.Gravity.START or android.view.Gravity.TOP
                                alpha = 1.0f
                            }
                        } else {
                            // Mövcud params varsa, pozisiyasını yenilə
                            val toggleX = toggleButtonLayoutParams?.x ?: 20
                            val toggleY = toggleButtonLayoutParams?.y ?: 100
                            updateModMenuPosition(toggleX, toggleY)
                        }
                        existingView.visibility = android.view.View.VISIBLE
                        existingView.alpha = 1.0f
                        windowManager?.addView(existingView, menuLayoutParams)
                        Log.d("ModMenuService", "✅ Mod menu tekrar eklendi")
                    } else {
                        Log.d("ModMenuService", "Mod menu zaten ekli ve görünür")
                        existingView.visibility = android.view.View.VISIBLE
                        existingView.alpha = 1.0f
                    }
                    toggleButton?.setMenuOpen(true)
                }
            } catch (e: Exception) {
                Log.e("ModMenuService", "Mod menu tekrar eklenirken hata", e)
                e.printStackTrace()
                // Hata varsa yeni bir tane oluştur
                try {
                    windowManager?.removeView(modMenuView)
                } catch (ex: Exception) {
                    // View zaten yoksa hata vermez
                }
                modMenuView = null
                // Yeni bir tane oluşturmak için devam et
            } finally {
                // Eğer hata olmadıysa return et
                if (modMenuView != null && modMenuView?.isAttachedToWindow == true) {
                    return
                }
            }
        }
        
        if (windowManager == null) {
            Log.e("ModMenuService", "❌ WindowManager null - mod menu gösterilemez!")
            android.widget.Toast.makeText(this, "WindowManager null!", android.widget.Toast.LENGTH_LONG).show()
            return
        }
        
        try {
            Log.d("ModMenuService", "ModMenuView oluşturuluyor...")
            modMenuView = ModMenuView(this)
            Log.d("ModMenuService", "✅ ModMenuView oluşturuldu: $modMenuView")
            
            // Switch butonlarının state-ini yenilə - config-dən yüklə
            modMenuView?.updateSwitchStates()
            
            modMenuView?.setOnCloseListener {
                Log.d("ModMenuService", "Mod menu kapatılıyor (close listener)")
                hideModMenu()
            }

            val screenWidth = resources.displayMetrics.widthPixels
            val screenHeight = resources.displayMetrics.heightPixels
            // Yığcam menu - kiçik düzbucaqlı qutu
            val menuWidth = 280
            
            Log.d("ModMenuService", "Ekran genişliği: $screenWidth, Menu genişliği: $menuWidth")
            
            val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }
            
            modMenuView?.visibility = android.view.View.VISIBLE
            modMenuView?.alpha = 1.0f
            
            Log.d("ModMenuService", "Mod menu visibility: ${modMenuView?.visibility}, alpha: ${modMenuView?.alpha}")
            
            // Toggle button-un pozisiyasını al
            val toggleX = toggleButtonLayoutParams?.x ?: 20
            val toggleY = toggleButtonLayoutParams?.y ?: 100
            
            // Menu-nu toggle button-un sağında yerləşdir
            val menuX = toggleX + 100 // Toggle button-dan 100px sağda
            val menuY = toggleY
            
            // Ekran sərhədlərini yoxla
            val finalX = menuX.coerceIn(0, screenWidth - menuWidth)
            val finalY = menuY.coerceIn(0, screenHeight - 200) // Yığcam menu - 200px yüksəklik
            
            menuLayoutParams = WindowManager.LayoutParams(
                menuWidth,
                WindowManager.LayoutParams.WRAP_CONTENT,
                windowType,
                // Toggle button ile aynı flag kombinasyonu - overlay view'ler için gerekli
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.OPAQUE
            ).apply {
                x = finalX
                y = finalY
                gravity = android.view.Gravity.START or android.view.Gravity.TOP
                alpha = 1.0f
            }
            
            Log.d("ModMenuService", "Menu pozisiyası: toggleX=$toggleX, toggleY=$toggleY, menuX=$finalX, menuY=$finalY")
            
            Log.d("ModMenuService", "Menu layout params: width=$menuWidth, x=${menuLayoutParams?.x}, y=${menuLayoutParams?.y}")

            // View'ı ölç ve hazırla
            modMenuView?.measure(
                android.view.View.MeasureSpec.makeMeasureSpec(menuWidth, android.view.View.MeasureSpec.EXACTLY),
                android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED)
            )
            
            Log.d("ModMenuService", "Mod menu ölçüldü: ${modMenuView?.measuredWidth}x${modMenuView?.measuredHeight}")
            
            // View'ın görünürlüğünü ve alpha değerini tekrar ayarla (güvenlik için)
            modMenuView?.visibility = android.view.View.VISIBLE
            modMenuView?.alpha = 1.0f
            modMenuView?.setWillNotDraw(false)
            
            Log.d("ModMenuService", "Gerçek mod menu WindowManager'a ekleniyor...")
            Log.d("ModMenuService", "WindowManager: $windowManager")
            Log.d("ModMenuService", "ModMenuView: $modMenuView")
            Log.d("ModMenuService", "MenuLayoutParams: $menuLayoutParams")
            Log.d("ModMenuService", "WindowType: $windowType")
            Log.d("ModMenuService", "Flags: ${menuLayoutParams?.flags}")
            
            // View'ı ekle
            try {
                windowManager?.addView(modMenuView, menuLayoutParams)
                DebugLogger.logWindowManagerOperation("ModMenuService", "addView(modMenuView)", true, 
                    "Mod menu başarıyla eklendi - Size: ${menuLayoutParams?.width}x${menuLayoutParams?.height}, Position: (${menuLayoutParams?.x}, ${menuLayoutParams?.y})")
                Log.d("ModMenuService", "✅✅✅ GERÇEK MOD MENU EKLENDİ!")
            } catch (e: SecurityException) {
                DebugLogger.logWindowManagerOperation("ModMenuService", "addView(modMenuView)", false, 
                    "SecurityException: ${e.message}")
                DebugLogger.logException("ModMenuService", "Mod menu eklenirken SecurityException", e)
                throw e
            } catch (e: Exception) {
                DebugLogger.logWindowManagerOperation("ModMenuService", "addView(modMenuView)", false, 
                    "${e.javaClass.simpleName}: ${e.message}")
                DebugLogger.logException("ModMenuService", "Mod menu eklenirken hata", e)
                throw e
            }
            
            // View'ın eklenip eklenmediğini kontrol et (hemen ve biraz sonra)
            handler.post {
                val isAttached = modMenuView?.isAttachedToWindow == true
                val parent = modMenuView?.parent
                val visibility = modMenuView?.visibility
                val alpha = modMenuView?.alpha
                Log.d("ModMenuService", "Mod menu durumu (hemen): isAttached=$isAttached, parent=$parent, visibility=$visibility, alpha=$alpha")
                
                if (!isAttached) {
                    Log.e("ModMenuService", "❌ Mod menu eklenemedi - isAttachedToWindow=false, tekrar deneniyor...")
                    // Önce mevcut view'ı kaldırmayı dene (eğer varsa)
                    try {
                        if (modMenuView != null && modMenuView!!.parent != null) {
                            windowManager?.removeView(modMenuView)
                            Log.d("ModMenuService", "Eski mod menu view kaldırıldı")
                        }
                    } catch (e: Exception) {
                        Log.d("ModMenuService", "Eski view kaldırılamadı (normal olabilir): ${e.message}")
                    }
                    
                    // Tekrar dene
                    handler.postDelayed({
                        try {
                            if (modMenuView != null && !modMenuView!!.isAttachedToWindow) {
                                Log.d("ModMenuService", "Mod menu tekrar ekleniyor (retry)...")
                                // View'ı tekrar hazırla
                                modMenuView?.visibility = android.view.View.VISIBLE
                                modMenuView?.alpha = 1.0f
                                windowManager?.addView(modMenuView, menuLayoutParams)
                                
                                handler.postDelayed({
                                    val retryAttached = modMenuView?.isAttachedToWindow == true
                                    val retryParent = modMenuView?.parent
                                    Log.d("ModMenuService", "Mod menu durumu (retry sonrası): isAttached=$retryAttached, parent=$retryParent")
                                    if (!retryAttached) {
                                        Log.e("ModMenuService", "❌ Mod menu hala eklenemedi! View temizleniyor...")
                                        // View'ı temizle, bir sonraki tıklamada tekrar denenecek
                                        try {
                                            windowManager?.removeView(modMenuView)
                                        } catch (ex: Exception) {
                                            // View zaten yoksa hata vermez
                                        }
                                        modMenuView = null
                                        android.widget.Toast.makeText(this@ModMenuService, "Mod menu gösterilemedi! Tekrar deneyin.", android.widget.Toast.LENGTH_LONG).show()
                                    } else {
                                        Log.d("ModMenuService", "✅ Mod menu retry ile başarıyla eklendi!")
                                        toggleButton?.setMenuOpen(true)
                                    }
                                }, 200)
                            }
                        } catch (e: Exception) {
                            Log.e("ModMenuService", "Mod menu retry hatası", e)
                            e.printStackTrace()
                            // Hata varsa view'ı temizle
                            try {
                                windowManager?.removeView(modMenuView)
                            } catch (ex: Exception) {
                                // View zaten yoksa hata vermez
                            }
                            modMenuView = null
                        }
                    }, 300)
                } else {
                    Log.d("ModMenuService", "✅ Mod menu başarıyla eklendi ve görünür!")
                    toggleButton?.setMenuOpen(true)
                    android.widget.Toast.makeText(this@ModMenuService, "Mod menu açıldı!", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: SecurityException) {
            Log.e("ModMenuService", "❌ SecurityException - Gerçek mod menu gösterilemedi", e)
            e.printStackTrace()
            android.widget.Toast.makeText(this, "Güvenlik hatası: Overlay izni gerekli! ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            modMenuView = null
        } catch (e: IllegalArgumentException) {
            Log.e("ModMenuService", "❌ IllegalArgumentException - Gerçek mod menu gösterilemedi", e)
            e.printStackTrace()
            android.widget.Toast.makeText(this, "Parametre hatası: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            modMenuView = null
        } catch (e: Exception) {
            Log.e("ModMenuService", "❌ Genel hata - Gerçek mod menu gösterilemedi", e)
            e.printStackTrace()
            android.widget.Toast.makeText(this, "Mod menu hatası: ${e.javaClass.simpleName} - ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            modMenuView = null
        }
    }

    private fun showOverlayDrawView() {
        // OverlayDrawView sadece trajectory veya auto aim aktifken gerekli
        // Başlangıçta ekleme - performans için lazy loading
        if (overlayDrawView != null) return
        
        // Sadece trajectory veya auto aim aktifse ekle
        if (!isTrajectoryEnabled && !isAutoAimEnabled) {
            Log.d("ModMenuService", "OverlayDrawView gerekli değil - trajectory ve auto aim kapalı")
            return
        }

        val metrics = resources.displayMetrics
        overlayDrawView = OverlayDrawView(this)

        val layoutParams = WindowManager.LayoutParams(
            metrics.widthPixels,
            metrics.heightPixels,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = android.view.Gravity.START or android.view.Gravity.TOP
            x = 0
            y = 0
        }

        try {
            windowManager?.addView(overlayDrawView, layoutParams)
            Log.d("ModMenuService", "OverlayDrawView eklendi")
        } catch (e: Exception) {
            Log.e("ModMenuService", "OverlayDrawView eklenemedi", e)
            e.printStackTrace()
        }
    }

    /**
     * Mod menu pozisiyasını toggle button-un pozisiyasına görə yenilə
     */
    private fun updateModMenuPosition(toggleX: Int, toggleY: Int) {
        menuLayoutParams?.let { params ->
            val screenWidth = resources.displayMetrics.widthPixels
            val screenHeight = resources.displayMetrics.heightPixels
            val menuWidth = params.width
            val menuHeight = modMenuView?.measuredHeight ?: 300
            
            // Toggle button-un sağında yerləşdir
            val menuX = toggleX + 100 // Toggle button-dan 100px sağda
            val menuY = toggleY
            
            // Ekran sərhədlərini yoxla
            val finalX = menuX.coerceIn(0, screenWidth - menuWidth)
            val finalY = menuY.coerceIn(0, screenHeight - menuHeight)
            
            params.x = finalX
            params.y = finalY
            
            try {
                windowManager?.updateViewLayout(modMenuView, params)
                android.util.Log.d("ModMenuService", "Mod menu pozisiyası yeniləndi: x=$finalX, y=$finalY")
            } catch (e: Exception) {
                android.util.Log.e("ModMenuService", "Mod menu pozisiyası yenilənə bilmədi", e)
            }
        }
    }

    /**
     * Mod menüsünü gizle (minimize)
     */
    private fun hideModMenu() {
        Log.d("ModMenuService", "=== hideModMenu() çağrıldı ===")
        
        if (modMenuView == null) {
            Log.d("ModMenuService", "Mod menu view null - zaten kapalı")
            toggleButton?.setMenuOpen(false)
            return
        }
        
        try {
            // View-ın ekli olub olmadığını yoxla
            val isAttached = modMenuView!!.isAttachedToWindow
            Log.d("ModMenuService", "Mod menu isAttachedToWindow: $isAttached")
            
            if (isAttached) {
                windowManager?.removeView(modMenuView)
                Log.d("ModMenuService", "✅ Mod menu view kaldırıldı")
            } else {
                Log.d("ModMenuService", "Mod menu view zaten eklenmemiş")
            }
        } catch (e: Exception) {
            Log.e("ModMenuService", "Mod menu kaldırılırken hata", e)
            e.printStackTrace()
        } finally {
            // Hər halda null et
            modMenuView = null
            menuLayoutParams = null
            toggleButton?.setMenuOpen(false)
            Log.d("ModMenuService", "✅ Mod menu bağlandı, toggle button güncellendi")
        }
    }

    /**
     * Toggle butonunu gizle
     */
    private fun hideToggleButton() {
        toggleButton?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            toggleButton = null
        }
    }


    private fun toggleTrajectory() {
        // Config-dən state-i oxu - Switch butonu artıq state-i dəyişib
        val currentState = modConfig.isModEnabled(ModMenuConfig.MOD_BALL_TRAJECTORY)
        isTrajectoryEnabled = currentState
        
        Log.d("ModMenuService", "toggleTrajectory: currentState=$currentState, isTrajectoryEnabled=$isTrajectoryEnabled")
        
        // Switch butonlarının state-ini yenilə - UI sinxronizasiyası üçün
        if (modMenuView != null) {
            Log.d("ModMenuService", "✅ modMenuView mövcuddur - updateSwitchStates() çağrılır")
            modMenuView?.updateSwitchStates()
        } else {
            Log.w("ModMenuService", "⚠️ modMenuView null - updateSwitchStates() çağrıla bilməz")
        }
        
        if (isTrajectoryEnabled) {
            // OverlayDrawView'ı ekle (eğer yoksa)
            showOverlayDrawView()
            // Ekran yakalama servisi çalışıyorsa yeni izin isteme
            Log.d("ModMenuService", "🔍 toggleTrajectory - isScreenCaptureRunning kontrolü: isScreenCaptureRunning=$isScreenCaptureRunning")
            if (!isScreenCaptureRunning) {
                Log.d("ModMenuService", "⚠️ Screen capture servisi çalışmıyor - izin isteniyor (isScreenCaptureRunning=$isScreenCaptureRunning)")
                DebugLogger.logInfo("ModMenuService", "Trajectory açılırken screen capture servisi çalışmıyor - izin isteniyor")
                requestScreenCapturePermission()
            } else {
                Log.d("ModMenuService", "✅ Screen capture servisi zaten çalışıyor - yeni izin istenmiyor (isScreenCaptureRunning=$isScreenCaptureRunning)")
            }
        } else {
            stopScreenCapture()
            overlayDrawView?.clear()
            // Trajectory verilerini temizle
            currentTrajectories = emptyList()
            // Eğer auto aim de kapalıysa overlayDrawView'ı kaldır
            if (!isAutoAimEnabled) {
                hideOverlayDrawView()
            }
        }
    }

    private fun requestScreenCapturePermission() {
        // MediaProjection izni MainActivity'den alınacak
        // Burada sadece intent gönder
        // MainActivity'yi oyunun üzerine getirmemek için FLAG_ACTIVITY_NO_ANIMATION ve 
        // FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS kullanıyoruz
        val intent = Intent(this, MainActivity::class.java).apply {
            action = MainActivity.ACTION_REQUEST_SCREEN_CAPTURE
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or 
                    Intent.FLAG_ACTIVITY_NO_ANIMATION or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
        }
        startActivity(intent)
    }
    
    /**
     * Ekran yakalama gerektiren mod'ları kapat (izin verilmediğinde)
     */
    private fun disableModsRequiringScreenCapture() {
        Log.d("ModMenuService", "=== disableModsRequiringScreenCapture() çağrıldı ===")
        
        var needUpdate = false
        
        // Trajectory açıksa kapat
        if (isTrajectoryEnabled || modConfig.isModEnabled(ModMenuConfig.MOD_BALL_TRAJECTORY)) {
            Log.d("ModMenuService", "⚠️ Trajectory açık - kapatılıyor")
            modConfig.setModEnabled(ModMenuConfig.MOD_BALL_TRAJECTORY, false)
            isTrajectoryEnabled = false
            needUpdate = true
            stopScreenCapture()
            overlayDrawView?.clear()
            currentTrajectories = emptyList()
        }
        
        // Auto aim açıksa kapat (sadece trajectory kapalıysa)
        if ((isAutoAimEnabled || modConfig.isModEnabled(ModMenuConfig.MOD_AUTO_AIM)) && !isTrajectoryEnabled) {
            Log.d("ModMenuService", "⚠️ Auto Aim açık ve trajectory kapalı - kapatılıyor")
            modConfig.setModEnabled(ModMenuConfig.MOD_AUTO_AIM, false)
            isAutoAimEnabled = false
            needUpdate = true
            currentAutoAimTarget = null
            if (!isTrajectoryEnabled) {
                hideOverlayDrawView()
            }
        }
        
        // UI'yi güncelle
        if (needUpdate && modMenuView != null) {
            Log.d("ModMenuService", "✅ Switch state'leri güncelleniyor")
            modMenuView?.updateSwitchStates()
        }
        
        // Overlay'i güncelle
        overlayDrawView?.updateTrajectories(
            currentTrajectories,
            currentBalls,
            tableBounds,
            currentHoles,
            currentAutoAimTarget,
            isAutoAimEnabled
        )
        
        Log.d("ModMenuService", "✅ Mod'lar kapatıldı - isTrajectoryEnabled=$isTrajectoryEnabled, isAutoAimEnabled=$isAutoAimEnabled")
    }

    fun startScreenCapture(resultCode: Int, resultData: Intent?) {
        Log.d("ModMenuService", "=== startScreenCapture() çağrıldı ===")
        Log.d("ModMenuService", "resultCode: $resultCode, RESULT_OK: ${Activity.RESULT_OK}, resultData: ${resultData != null}")
        
        // RESULT_OK = -1, yəni resultCode == -1 permission verildiyini göstərir
        if (resultCode != Activity.RESULT_OK || resultData == null) {
            val oldValue = isScreenCaptureRunning
            Log.w("ModMenuService", "❌ Screen capture başlatılamadı: resultCode=$resultCode (RESULT_OK=${Activity.RESULT_OK}), resultData=${resultData != null}")
            DebugLogger.logError("ModMenuService", "Screen capture başlatılamadı: resultCode=$resultCode, resultData=${resultData != null}")
            android.widget.Toast.makeText(this, "❌ Ekran yakalama izni verilmedi!", android.widget.Toast.LENGTH_LONG).show()
            isScreenCaptureRunning = false
            Log.d("ModMenuService", "startScreenCapture - isScreenCaptureRunning: $oldValue -> false (hata durumu)")
            return
        }

        try {
            Log.d("ModMenuService", "✅ Screen capture permission var, ScreenCaptureService başlatılıyor...")
            val intent = Intent(this, ScreenCaptureService::class.java).apply {
                action = ScreenCaptureService.ACTION_START
                putExtra("result_code", resultCode)
                putExtra("result_data", resultData)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            // isScreenCaptureRunning flag'i ACTION_CAPTURE_STATE_CHANGED broadcast'inden güncellenecek
            // Bu şekilde ScreenCaptureService başarılı/başarısız durumunu bildirebilir
            Log.d("ModMenuService", "✅ ScreenCaptureService başlatıldı! (flag broadcast'ten güncellenecek, şu anki değer: $isScreenCaptureRunning)")
            DebugLogger.logInfo("ModMenuService", "ScreenCaptureService başlatma intent'i gönderildi - flag broadcast'ten güncellenecek")
            android.widget.Toast.makeText(this, "✅ Ekran yakalama başladı!", android.widget.Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            val oldValue = isScreenCaptureRunning
            isScreenCaptureRunning = false
            Log.e("ModMenuService", "❌ Screen capture servisi başlatılamadı: ${e.message}", e)
            DebugLogger.logException("ModMenuService", "Screen capture servisi başlatılamadı", e)
            Log.d("ModMenuService", "startScreenCapture - isScreenCaptureRunning: $oldValue -> false (exception)")
            android.widget.Toast.makeText(this, "❌ Ekran yakalama hatası: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
        }
    }


    private fun onScreenshotReceived(bitmap: Bitmap) {
        if (!isTrajectoryEnabled && !isAutoAimEnabled) {
            // Bitmap kullanılmadığı için recycle et
            try {
                bitmap.recycle()
            } catch (e: Exception) {
                Log.w("ModMenuService", "Bitmap recycle hatası: ${e.message}")
            }
            return
        }

        // Bitmap geçerliliğini kontrol et (main thread'de hızlı kontrol)
        if (bitmap.isRecycled) {
            Log.w("ModMenuService", "Bitmap zaten recycle edilmiş")
            return
        }

        // DisplayMetrics'i main thread'de al (background thread'de erişim ANR'e neden olabilir)
        val metrics = resources.displayMetrics
        val screenWidth = metrics.widthPixels.toFloat()
        val screenHeight = metrics.heightPixels.toFloat()

        // Ağır işlemleri background thread'de yap
        lifecycleScope.launch(Dispatchers.Default) {
            try {
                // Topları tespit et (ağır işlem - background thread'de)
                val detectedBalls = BallDetector.detectBalls(bitmap)

                // Masa sınırlarını bul (ağır işlem - background thread'de)
                val detectedTableBounds = BallDetector.detectTableBounds(bitmap)

                // Delikleri tespit et (ağır işlem - background thread'de)
                // Delik pozisyon ayarlarını config'den al
                val holeOffsetX = modConfig.getHoleOffsetX()
                val holeOffsetY = modConfig.getHoleOffsetY()
                val holeScale = modConfig.getHoleScale()
                val detectedHoles = HoleDetector.detectHoles(
                    bitmap, 
                    detectedTableBounds,
                    offsetX = holeOffsetX,
                    offsetY = holeOffsetY,
                    scale = holeScale
                )

                // Auto Aim aktifse hedefi hesapla (ağır işlem - background thread'de)
                var calculatedAutoAimTarget: AutoAimEngine.AimTarget? = null
                if (isAutoAimEnabled) {
                    val whiteBall = detectedBalls.find { it.number == 0 }
                    val targetBalls = detectedBalls.filter { it.number > 0 }
                    
                    if (whiteBall != null && targetBalls.isNotEmpty() && detectedHoles.isNotEmpty()) {
                        calculatedAutoAimTarget = AutoAimEngine.calculateBestAim(
                            whiteBall = whiteBall,
                            targetBalls = targetBalls,
                            allBalls = detectedBalls,
                            holes = detectedHoles,
                            tableWidth = screenWidth,
                            tableHeight = screenHeight
                        )
                    }
                }

                // Trajectory hesapla (ağır işlem - background thread'de)
                var calculatedTrajectories: List<PhysicsCalculator.BallTrajectory> = emptyList()
                if (isTrajectoryEnabled) {
                    val whiteBall = detectedBalls.find { it.number == 0 }
                    if (whiteBall != null && detectedBalls.size > 1) {
                        calculatedTrajectories = PhysicsCalculator.calculateTrajectories(
                            whiteBall = whiteBall,
                            cueDirection = calculatedAutoAimTarget?.aimAngle ?: 45f,
                            cuePower = calculatedAutoAimTarget?.aimPower ?: 0.8f,
                            allBalls = detectedBalls,
                            tableWidth = screenWidth,
                            tableHeight = screenHeight
                        )
                    }
                }

                // State'leri güncelle ve UI'ı main thread'de güncelle
                withContext(Dispatchers.Main) {
                    try {
                        currentBalls = detectedBalls
                        tableBounds = detectedTableBounds
                        currentHoles = detectedHoles
                        currentAutoAimTarget = calculatedAutoAimTarget
                        currentTrajectories = calculatedTrajectories

                        // Overlay'i güncelle (UI işlemi - main thread'de yapılmalı)
                        overlayDrawView?.updateTrajectories(
                            currentTrajectories,
                            currentBalls,
                            tableBounds,
                            currentHoles,
                            currentAutoAimTarget,
                            isAutoAimEnabled
                        )
                    } catch (e: Exception) {
                        Log.e("ModMenuService", "UI güncelleme hatası: ${e.message}", e)
                        DebugLogger.logException("ModMenuService", "UI güncelleme hatası", e)
                    }
                }
            } catch (e: Exception) {
                Log.e("ModMenuService", "Ekran işleme hatası: ${e.message}", e)
                DebugLogger.logException("ModMenuService", "Ekran işleme hatası", e)
            } finally {
                // Bitmap'i temizle (background thread'de yapılabilir)
                try {
                    if (!bitmap.isRecycled) {
                        bitmap.recycle()
                    }
                } catch (e: Exception) {
                    Log.w("ModMenuService", "Bitmap recycle hatası: ${e.message}")
                }
            }
        }
    }

    /**
     * Overlay'i yenile - delik pozisyon ayarları değiştiğinde çağrılır
     */
    private fun refreshOverlay() {
        Log.d("ModMenuService", "refreshOverlay() çağrıldı")
        // Mevcut overlay'i güncelle - bir sonraki screenshot'ta yeni delik pozisyonları uygulanacak
        // Şimdilik sadece overlay'i invalidate et, böylece bir sonraki screenshot ile güncellenecek
        if (overlayDrawView != null && isScreenCaptureRunning) {
            Log.d("ModMenuService", "Overlay invalidate ediliyor - yeni delik pozisyonları bir sonraki screenshot'ta uygulanacak")
            overlayDrawView?.invalidate()
        } else {
            Log.d("ModMenuService", "Overlay görünmüyor veya screen capture çalışmıyor - overlay güncellenmiyor")
        }
    }

    /**
     * Auto Aim'i aç/kapat
     */
    private fun toggleAutoAim() {
        // Config-dən state-i oxu - Switch butonu artıq state-i dəyişib
        val currentState = modConfig.isModEnabled(ModMenuConfig.MOD_AUTO_AIM)
        isAutoAimEnabled = currentState
        
        Log.d("ModMenuService", "toggleAutoAim: currentState=$currentState, isAutoAimEnabled=$isAutoAimEnabled")
        
        // Switch butonlarının state-ini yenilə - UI sinxronizasiyası üçün
        if (modMenuView != null) {
            Log.d("ModMenuService", "✅ modMenuView mövcuddur - updateSwitchStates() çağrılır")
            modMenuView?.updateSwitchStates()
        } else {
            Log.w("ModMenuService", "⚠️ modMenuView null - updateSwitchStates() çağrıla bilməz")
        }
        
        if (isAutoAimEnabled) {
            // OverlayDrawView'ı ekle (eğer yoksa)
            showOverlayDrawView()
            if (!isTrajectoryEnabled) {
                // Ekran yakalama gerekli - ama servis çalışıyorsa yeni izin isteme
                Log.d("ModMenuService", "🔍 toggleAutoAim - isScreenCaptureRunning kontrolü: isScreenCaptureRunning=$isScreenCaptureRunning")
                if (!isScreenCaptureRunning) {
                    Log.d("ModMenuService", "⚠️ Screen capture servisi çalışmıyor - izin isteniyor (isScreenCaptureRunning=$isScreenCaptureRunning)")
                    DebugLogger.logInfo("ModMenuService", "Auto aim açılırken screen capture servisi çalışmıyor - izin isteniyor")
                    requestScreenCapturePermission()
                } else {
                    Log.d("ModMenuService", "✅ Screen capture servisi zaten çalışıyor - yeni izin istenmiyor (isScreenCaptureRunning=$isScreenCaptureRunning)")
                }
            }
        } else {
            // Auto aim kapatıldığında hedefi temizle
            currentAutoAimTarget = null
            // Eğer trajectory de kapalıysa overlayDrawView'ı kaldır
            if (!isTrajectoryEnabled) {
                hideOverlayDrawView()
            }
        }
        
        // Overlay'i güncelle
        overlayDrawView?.updateTrajectories(
            currentTrajectories,
            currentBalls,
            tableBounds,
            currentHoles,
            currentAutoAimTarget,
            isAutoAimEnabled
        )
    }
    
    /**
     * OverlayDrawView'ı kaldır
     */
    private fun hideOverlayDrawView() {
        overlayDrawView?.let {
            try {
                windowManager?.removeView(it)
                Log.d("ModMenuService", "OverlayDrawView kaldırıldı")
            } catch (e: Exception) {
                Log.e("ModMenuService", "OverlayDrawView kaldırılamadı", e)
            }
            overlayDrawView = null
        }
    }

    private fun stopScreenCapture() {
        val oldValue = isScreenCaptureRunning
        try {
            val intent = Intent(this, ScreenCaptureService::class.java).apply {
                action = ScreenCaptureService.ACTION_STOP
            }
            stopService(intent)
            // isScreenCaptureRunning flag'i ACTION_CAPTURE_STATE_CHANGED broadcast'inden güncellenecek
            // Ancak stopService() çağrıldıktan hemen sonra false yapmak da mantıklı (zamanlama sorunu olmasın)
            isScreenCaptureRunning = false
            Log.d("ModMenuService", "✅ Screen capture durduruldu. isScreenCaptureRunning: $oldValue -> false")
            DebugLogger.logInfo("ModMenuService", "Screen capture durduruldu: $oldValue -> false")
            // State'i değiştirme - sadece servisi durdur
            // State toggleTrajectory() tarafından yönetiliyor
        } catch (e: Exception) {
            isScreenCaptureRunning = false
            Log.e("ModMenuService", "Screen capture durdurulamadı: ${e.message}", e)
            DebugLogger.logException("ModMenuService", "Screen capture durdurulurken hata", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        @Suppress("DEPRECATION")
        LocalBroadcastManager.getInstance(this).unregisterReceiver(screenshotReceiver)
        try {
            unregisterReceiver(screenshotReceiver)
        } catch (e: Exception) {
            // Receiver zaten unregister edilmiş olabilir
            Log.d("ModMenuService", "Receiver unregister hatası (normal olabilir): ${e.message}")
        }
        hideModMenu()
        hideToggleButton()
        hideOverlayDrawView()
        stopScreenCapture()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Mod Menu Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "8 Ball Pool Mod Menu Overlay"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🎮 Mod Menu Aktif")
            .setContentText("8 Ball Pool mod menu çalışıyor")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        const val ACTION_START = "com.poolmod.menu.START"
        const val ACTION_STOP = "com.poolmod.menu.STOP"
        const val ACTION_TOGGLE = "com.poolmod.menu.TOGGLE"
        const val ACTION_START_SCREEN_CAPTURE = "com.poolmod.menu.START_SCREEN_CAPTURE"
        const val ACTION_TOGGLE_TRAJECTORY = "com.poolmod.menu.TOGGLE_TRAJECTORY"
        const val ACTION_TOGGLE_AUTO_AIM = "com.poolmod.menu.TOGGLE_AUTO_AIM"
        const val ACTION_SCREEN_CAPTURE_DENIED = "com.poolmod.menu.SCREEN_CAPTURE_DENIED"
        const val ACTION_REQUEST_SCREEN_CAPTURE_PERMISSION = "com.poolmod.menu.REQUEST_SCREEN_CAPTURE_PERMISSION"
        const val ACTION_HOLE_SETTINGS_CHANGED = "com.poolmod.menu.HOLE_SETTINGS_CHANGED"
        private const val CHANNEL_ID = "mod_menu_service_channel"
    }
}
