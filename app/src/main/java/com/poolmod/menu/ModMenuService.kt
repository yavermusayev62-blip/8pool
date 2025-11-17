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
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class ModMenuService : Service() {

    private var modMenuView: ModMenuView? = null
    private var toggleButton: ModToggleButton? = null
    private var overlayDrawView: OverlayDrawView? = null
    private var windowManager: WindowManager? = null
    private var gamePackage: String? = null
    private val handler = Handler(Looper.getMainLooper())
    
    private var isTrajectoryEnabled = false
    private var isAutoAimEnabled = false
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
            if (intent?.action == ScreenCaptureService.ACTION_SCREENSHOT_READY) {
                val byteArray = intent.getByteArrayExtra("bitmap_data")
                val width = intent.getIntExtra("width", 0)
                val height = intent.getIntExtra("height", 0)
                
                if (byteArray != null && width > 0 && height > 0) {
                    val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                    if (bitmap != null) {
                        onScreenshotReceived(bitmap)
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        modConfig = ModMenuConfig(this) // Context artık hazır
        createNotificationChannel()
        startForeground(1, createNotification())
        
        // Broadcast receiver kaydet
        val filter = IntentFilter(ScreenCaptureService.ACTION_SCREENSHOT_READY)
        @Suppress("DEPRECATION")
        LocalBroadcastManager.getInstance(this).registerReceiver(screenshotReceiver, filter)
        
        // Anti-cheat bypass aktif
        AntiCheatBypass.protectMemory()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("ModMenuService", "onStartCommand çağrıldı - action: ${intent?.action}")
        
        when (intent?.action) {
            ACTION_START -> {
                gamePackage = intent.getStringExtra("game_package")
                Log.d("ModMenuService", "ACTION_START alındı, gamePackage: $gamePackage")
                
                // Overlay izni kontrolü
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val hasPermission = android.provider.Settings.canDrawOverlays(this)
                    Log.d("ModMenuService", "Overlay izni: $hasPermission")
                    if (!hasPermission) {
                        Log.e("ModMenuService", "Overlay izni yok! Mod menu gösterilemez.")
                        android.widget.Toast.makeText(this, "Overlay izni gerekli!", android.widget.Toast.LENGTH_LONG).show()
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
                toggleTrajectory()
            }
            ACTION_TOGGLE_AUTO_AIM -> {
                toggleAutoAim()
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

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Küçük toggle butonunu göster
     */
    private fun showToggleButton() {
        // Overlay izni kontrolü
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!android.provider.Settings.canDrawOverlays(this)) {
                Log.e("ModMenuService", "Overlay izni yok - toggle button gösterilemiyor!")
                return
            }
        }
        
        if (toggleButton != null) return

        // Oyun kontrolünü kaldırdık - her zaman göster
        toggleButton = ModToggleButton(this)
        toggleButton?.setOnClickListener {
            android.util.Log.d("ModMenuService", "Toggle button onClick tıklandı!")
            toggleMenu()
        }
        
        // Direkt callback - onTouchEvent'ten çağrılacak
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
                } catch (e: Exception) {
                    android.util.Log.e("ModMenuService", "Toggle button pozisyonu güncellenemedi", e)
                }
            }
        }

        toggleButtonLayoutParams = WindowManager.LayoutParams(
            80,
            80,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            // Overlay'ler için touch event'lerin çalışması için doğru flag kombinasyonu
            // FLAG_NOT_TOUCH_MODAL: Butonun dışındaki dokunmalar oyuna geçer, butonun kendisi dokunmaları alır
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.OPAQUE
        ).apply {
            x = 20
            y = 100
            gravity = android.view.Gravity.START or android.view.Gravity.TOP
            alpha = 1.0f
        }

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
            
            windowManager?.addView(toggleButton, toggleButtonLayoutParams)
            
            android.util.Log.d("ModMenuService", "✅ Toggle button WindowManager'a eklendi")
            
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
        
        val isMenuVisible = modMenuView != null && modMenuView!!.isAttachedToWindow
        Log.d("ModMenuService", "modMenuView durumu: ${if (modMenuView == null) "null" else "mevcut (isAttached=${modMenuView?.isAttachedToWindow})"}")
        
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
        
        // Overlay izni kontrolü
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val hasPermission = android.provider.Settings.canDrawOverlays(this)
            Log.d("ModMenuService", "Overlay izni kontrolü: $hasPermission")
            if (!hasPermission) {
                Log.e("ModMenuService", "❌ Overlay izni yok!")
                android.widget.Toast.makeText(this, "Overlay izni gerekli! Ayarlardan izin verin.", android.widget.Toast.LENGTH_LONG).show()
                return
            }
            Log.d("ModMenuService", "✅ Overlay izni var")
        }
        
        // View var ve eklenmişse, zaten gösteriliyor demektir
        if (modMenuView != null && modMenuView!!.isAttachedToWindow) {
            Log.d("ModMenuService", "Mod menu zaten gösteriliyor ve ekli")
            toggleButton?.setMenuOpen(true)
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
            // Test view'ı kaldırdık - direkt gerçek menu'yu göster (performans için)
            showRealModMenu()
        } catch (e: SecurityException) {
            Log.e("ModMenuService", "❌ SecurityException!", e)
            e.printStackTrace()
            android.widget.Toast.makeText(this, "SecurityException: Overlay izni gerekli! ${e.message}", android.widget.Toast.LENGTH_LONG).show()
        } catch (e: IllegalArgumentException) {
            Log.e("ModMenuService", "❌ IllegalArgumentException!", e)
            e.printStackTrace()
            android.widget.Toast.makeText(this, "IllegalArgumentException: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e("ModMenuService", "❌ Genel hata!", e)
            e.printStackTrace()
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
                            val menuWidth = (screenWidth * 0.8).toInt().coerceAtMost(500).coerceAtLeast(350)
                            val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                            } else {
                                @Suppress("DEPRECATION")
                                WindowManager.LayoutParams.TYPE_PHONE
                            }
                            menuLayoutParams = WindowManager.LayoutParams(
                                menuWidth,
                                WindowManager.LayoutParams.WRAP_CONTENT,
                                windowType,
                                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                                PixelFormat.OPAQUE
                            ).apply {
                                x = (screenWidth - menuWidth) / 2
                                y = 100
                                gravity = android.view.Gravity.START or android.view.Gravity.TOP
                                alpha = 1.0f
                            }
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
            
            modMenuView?.setOnCloseListener {
                Log.d("ModMenuService", "Mod menu kapatılıyor (close listener)")
                hideModMenu()
            }

            val screenWidth = resources.displayMetrics.widthPixels
            val menuWidth = (screenWidth * 0.8).toInt().coerceAtMost(500).coerceAtLeast(350)
            
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
                x = (screenWidth - menuWidth) / 2
                y = 100
                gravity = android.view.Gravity.START or android.view.Gravity.TOP
                alpha = 1.0f
            }
            
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
            windowManager?.addView(modMenuView, menuLayoutParams)
            Log.d("ModMenuService", "✅✅✅ GERÇEK MOD MENU EKLENDİ!")
            
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
     * Mod menüsünü gizle (minimize)
     */
    private fun hideModMenu() {
        modMenuView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            modMenuView = null
            toggleButton?.setMenuOpen(false)
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
        isTrajectoryEnabled = !isTrajectoryEnabled
        
        if (isTrajectoryEnabled) {
            // OverlayDrawView'ı ekle (eğer yoksa)
            showOverlayDrawView()
            // Ekran yakalama izni iste
            requestScreenCapturePermission()
        } else {
            stopScreenCapture()
            overlayDrawView?.clear()
            // Eğer auto aim de kapalıysa overlayDrawView'ı kaldır
            if (!isAutoAimEnabled) {
                hideOverlayDrawView()
            }
        }
    }

    private fun requestScreenCapturePermission() {
        // MediaProjection izni MainActivity'den alınacak
        // Burada sadece intent gönder
        val intent = Intent(this, MainActivity::class.java).apply {
            action = MainActivity.ACTION_REQUEST_SCREEN_CAPTURE
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
    }

    fun startScreenCapture(resultCode: Int, resultData: Intent?) {
        if (resultCode == -1 || resultData == null) return

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
    }


    private fun onScreenshotReceived(bitmap: Bitmap) {
        if (!isTrajectoryEnabled && !isAutoAimEnabled) return

        handler.post {
            try {
                // Topları tespit et
                val detectedBalls = BallDetector.detectBalls(bitmap)
                currentBalls = detectedBalls

                // Masa sınırlarını bul
                tableBounds = BallDetector.detectTableBounds(bitmap)

                // Delikleri tespit et
                currentHoles = HoleDetector.detectHoles(bitmap, tableBounds)

                // Auto Aim aktifse hedefi hesapla
                if (isAutoAimEnabled) {
                    val whiteBall = detectedBalls.find { it.number == 0 }
                    val targetBalls = detectedBalls.filter { it.number > 0 }
                    
                    if (whiteBall != null && targetBalls.isNotEmpty() && currentHoles.isNotEmpty()) {
                        val metrics = resources.displayMetrics
                        currentAutoAimTarget = AutoAimEngine.calculateBestAim(
                            whiteBall = whiteBall,
                            targetBalls = targetBalls,
                            allBalls = detectedBalls,
                            holes = currentHoles,
                            tableWidth = metrics.widthPixels.toFloat(),
                            tableHeight = metrics.heightPixels.toFloat()
                        )
                    } else {
                        currentAutoAimTarget = null
                    }
                }

                // Trajectory hesapla (top yolu gösterimi aktifse)
                if (isTrajectoryEnabled) {
                    val whiteBall = detectedBalls.find { it.number == 0 }
                    if (whiteBall != null && detectedBalls.size > 1) {
                        val metrics = resources.displayMetrics
                        val trajectories = PhysicsCalculator.calculateTrajectories(
                            whiteBall = whiteBall,
                            cueDirection = currentAutoAimTarget?.aimAngle ?: 45f,
                            cuePower = currentAutoAimTarget?.aimPower ?: 0.8f,
                            allBalls = detectedBalls,
                            tableWidth = metrics.widthPixels.toFloat(),
                            tableHeight = metrics.heightPixels.toFloat()
                        )
                        currentTrajectories = trajectories
                    } else {
                        currentTrajectories = emptyList()
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
            } catch (e: Exception) {
                Log.e("ModMenuService", "Ekran işleme hatası: ${e.message}", e)
            }
        }
    }

    /**
     * Auto Aim'i aç/kapat
     */
    private fun toggleAutoAim() {
        isAutoAimEnabled = modConfig.isModEnabled(ModMenuConfig.MOD_AUTO_AIM)
        
        if (isAutoAimEnabled) {
            // OverlayDrawView'ı ekle (eğer yoksa)
            showOverlayDrawView()
            if (!isTrajectoryEnabled) {
                // Ekran yakalama gerekli
                requestScreenCapturePermission()
            }
        } else {
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
        val intent = Intent(this, ScreenCaptureService::class.java).apply {
            action = ScreenCaptureService.ACTION_STOP
        }
        stopService(intent)
        isTrajectoryEnabled = false
    }

    override fun onDestroy() {
        super.onDestroy()
        @Suppress("DEPRECATION")
        LocalBroadcastManager.getInstance(this).unregisterReceiver(screenshotReceiver)
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
        private const val CHANNEL_ID = "mod_menu_service_channel"
    }
}
