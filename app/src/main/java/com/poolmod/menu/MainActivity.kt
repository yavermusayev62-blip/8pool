package com.poolmod.menu

import android.app.Activity
import android.content.Intent
import android.hardware.display.DisplayManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var btnDetectGame: Button
    private lateinit var btnLaunchGame: Button
    private lateinit var btnStopMod: Button
    private lateinit var btnTestModMenu: Button
    private lateinit var tvGameInfo: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvVersion: TextView

    private var detectedGame: GameDetector.GameInfo? = null
    private lateinit var screenCaptureLauncher: ActivityResultLauncher<Intent>
    private var screenCapturePermissionRequested = false // Dialog-un bir neçə dəfə açılmasını qarşısını almaq üçün
    private var isScreenCaptureDialogOpen = false // Permission dialog-unun açıq olub olmadığını izlə

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Window ayarları - karanlık ekran sorununu önlemek için
        window.setBackgroundDrawableResource(android.R.color.white)
        window.statusBarColor = resources.getColor(android.R.color.transparent, theme)

        setContentView(R.layout.activity_main)

        // Screen capture launcher
        screenCaptureLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            android.util.Log.d("MainActivity", "=== SCREEN CAPTURE PERMISSION RESULT ===")
            android.util.Log.d("MainActivity", "resultCode: ${result.resultCode}, RESULT_OK: ${Activity.RESULT_OK}, RESULT_CANCELED: ${Activity.RESULT_CANCELED}")
            android.util.Log.d("MainActivity", "result.data: ${result.data != null}")

            // Dialog bağlandı
            isScreenCaptureDialogOpen = false

            // Flag-i reset et ki, növbəti dəfə yenidən işləsin
            screenCapturePermissionRequested = false

            // Intent action-u təmizlə ki, onResume-də yenidən dialog açılmasın
            intent.action = null
            setIntent(Intent().setAction(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER))

            // RESULT_OK = -1, yəni permission verildiyini göstərir
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val data = result.data
                DebugLogger.logScreenCapturePermission("MainActivity", true, null)
                android.util.Log.d("MainActivity", "✅ Screen capture permission verildi!")
                android.util.Log.d("MainActivity", "ModMenuService'e gönderiliyor...")

                // ModMenuService'e gönder
                val intent = Intent(this, ModMenuService::class.java).apply {
                    action = ModMenuService.ACTION_START_SCREEN_CAPTURE
                    putExtra("result_code", result.resultCode)
                    putExtra("result_data", data)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
                android.util.Log.d("MainActivity", "✅ ModMenuService'e intent gönderildi")
                Toast.makeText(this, "✅ Ekran yakalama izni verildi!", Toast.LENGTH_SHORT).show()
            } else {
                val reason = when (result.resultCode) {
                    Activity.RESULT_CANCELED -> "İstifadəçi ləğv etdi (RESULT_CANCELED)"
                    0 -> "İstifadəçi ləğv etdi (0)"
                    else -> "Naməlum səbəb (resultCode: ${result.resultCode})"
                }
                DebugLogger.logScreenCapturePermission("MainActivity", false, reason)
                DebugLogger.logError("MainActivity", "Screen capture permission REDDEDILDI! $reason, data: ${result.data != null}")
                android.util.Log.e("MainActivity", "❌ Screen capture permission REDDEDILDI! $reason, data: ${result.data != null}")
                Toast.makeText(this, "❌ Ekran yakalama izni verilmedi! Mod özellikleri çalışmayacak.", Toast.LENGTH_SHORT).show()

                // ModMenuService'e izin verilmediğini bildir - switch'leri kapatması için
                val intent = Intent(ModMenuService.ACTION_SCREEN_CAPTURE_DENIED).apply {
                    setPackage(packageName)
                }
                sendBroadcast(intent)
                android.util.Log.d("MainActivity", "✅ Screen capture denied broadcast gönderildi")

                // Activity'yi arka plana gönder (finish() yerine - oyunun üzerine gelmesin)
                moveTaskToBack(true)
            }
        }

        try {
            initViews()
            setupClickListeners()
            showVersionInfo()
            checkOverlayPermission()
            detectGameOnStart()

            // Screen capture izni isteği kontrolü
            if (intent.action == ACTION_REQUEST_SCREEN_CAPTURE) {
                android.util.Log.d("MainActivity", "=== ACTION_REQUEST_SCREEN_CAPTURE alındı ===")
                android.util.Log.d("MainActivity", "Screen capture permission dialog-u açılır...")
                requestScreenCapturePermission()
            }
        } catch (e: Exception) {
            DebugLogger.logException("MainActivity", "onCreate hatası", e)
            android.util.Log.e("MainActivity", "Error in onCreate", e)
            Toast.makeText(this, "Hata: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun initViews() {
        btnDetectGame = findViewById(R.id.btnDetectGame)
        btnLaunchGame = findViewById(R.id.btnLaunchGame)
        btnStopMod = findViewById(R.id.btnStopMod)
        btnTestModMenu = findViewById(R.id.btnTestModMenu)
        tvGameInfo = findViewById(R.id.tvGameInfo)
        tvStatus = findViewById(R.id.tvStatus)
        tvVersion = findViewById(R.id.tvVersion)
    }

    /**
     * Versiyon bilgisini göster
     */
    private fun showVersionInfo() {
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val versionName = packageInfo.versionName ?: "Bilinmiyor"
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toString()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toString()
            }

            tvVersion.text = "Versiyon: $versionName (Build: $versionCode)"
            android.util.Log.d("MainActivity", "Versiyon bilgisi gösterildi: $versionName ($versionCode)")
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Versiyon bilgisi alınamadı", e)
            tvVersion.text = "Versiyon: Bilinmiyor"
        }
    }

    private fun setupClickListeners() {
        btnDetectGame.setOnClickListener {
            detectGame()
        }

        btnLaunchGame.setOnClickListener {
            launchGame()
        }

        btnStopMod.setOnClickListener {
            stopModMenu()
        }

        btnTestModMenu.setOnClickListener {
            testModMenu()
        }
    }

    private fun testModMenu() {
        if (!checkOverlayPermission()) {
            Toast.makeText(this, "Önce overlay izni verin!", Toast.LENGTH_LONG).show()
            requestOverlayPermission()
            return
        }

        // Test overlay'i kaldırdık - performans için direkt service'i başlat
        // showTestOverlay() // Kaldırıldı - performans sorununa neden oluyordu

        // Service'i başlat
        val intent = Intent(this, ModMenuService::class.java)
        intent.putExtra("game_package", detectedGame?.packageName ?: "com.miniclip.eightballpool")
        intent.action = ModMenuService.ACTION_START

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        Toast.makeText(this, "Mod Menu başlatılıyor...", Toast.LENGTH_SHORT).show()
    }

    /**
     * MainActivity'den direkt overlay göster - test için
     */
    private fun showTestOverlay() {
        try {
            // Overlay izni kontrolü - çok detaylı
            val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                android.provider.Settings.canDrawOverlays(this)
            } else {
                true
            }

            android.util.Log.d("MainActivity", "=== TEST OVERLAY BAŞLIYOR ===")
            android.util.Log.d("MainActivity", "Overlay izni: $hasPermission")
            android.util.Log.d("MainActivity", "Android SDK: ${Build.VERSION.SDK_INT}")

            if (!hasPermission) {
                android.util.Log.e("MainActivity", "❌ Overlay izni YOK!")
                Toast.makeText(this, "❌ Overlay izni YOK! Ayarlardan izin verin!", Toast.LENGTH_LONG).show()
                return
            }

            val windowManager = getSystemService(WINDOW_SERVICE) as android.view.WindowManager
            android.util.Log.d("MainActivity", "WindowManager: ${windowManager != null}")

            val screenWidth = resources.displayMetrics.widthPixels
            val screenHeight = resources.displayMetrics.heightPixels
            android.util.Log.d("MainActivity", "Ekran boyutu: ${screenWidth}x${screenHeight}")

            // ÇOK BÜYÜK VE PARLAK TEST VIEW
            val testView = android.widget.TextView(this).apply {
                text = "🎮🎮🎮 TEST OVERLAY 🎮🎮🎮\n\nGÖRÜNÜYOR MU?\n\nEğer bu görünüyorsa\noverlay çalışıyor!\n\nBu kutu ekranın\n%80'ini kaplıyor!"
                textSize = 50f
                setTextColor(android.graphics.Color.WHITE)
                gravity = android.view.Gravity.CENTER
                setBackgroundColor(android.graphics.Color.parseColor("#FF0000")) // Parlak kırmızı
                setPadding(100, 100, 100, 100)
                visibility = android.view.View.VISIBLE
                alpha = 1.0f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }

            val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                android.view.WindowManager.LayoutParams.TYPE_PHONE
            }
            android.util.Log.d("MainActivity", "Window type: $windowType")

            // Ekranın %80'ini kaplayacak şekilde büyük yap
            val viewWidth = (screenWidth * 0.9).toInt()
            val viewHeight = (screenHeight * 0.8).toInt()

            val params = android.view.WindowManager.LayoutParams(
                viewWidth,
                viewHeight,
                windowType,
                android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                android.view.WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                android.view.WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                android.graphics.PixelFormat.OPAQUE
            ).apply {
                x = (screenWidth - viewWidth) / 2
                y = (screenHeight - viewHeight) / 2
                gravity = android.view.Gravity.START or android.view.Gravity.TOP
                alpha = 1.0f
            }

            android.util.Log.d("MainActivity", "Layout params: x=${params.x}, y=${params.y}, width=$viewWidth, height=$viewHeight")
            android.util.Log.d("MainActivity", "WindowManager'a view ekleniyor...")

            windowManager.addView(testView, params)

            android.util.Log.d("MainActivity", "✅✅✅ VIEW EKLENDİ! Görünüyor mu?")

            // View'ın durumunu kontrol et
            Handler(Looper.getMainLooper()).postDelayed({
                android.util.Log.d("MainActivity", "=== VIEW DURUMU ===")
                android.util.Log.d("MainActivity", "View visibility: ${testView.visibility}")
                android.util.Log.d("MainActivity", "View alpha: ${testView.alpha}")
                android.util.Log.d("MainActivity", "View width: ${testView.width}, height: ${testView.height}")
                android.util.Log.d("MainActivity", "View measured: ${testView.measuredWidth}x${testView.measuredHeight}")
                android.util.Log.d("MainActivity", "View parent: ${testView.parent}")
                android.util.Log.d("MainActivity", "View isAttachedToWindow: ${testView.isAttachedToWindow}")

                // Zorla görünür yap
                testView.visibility = android.view.View.VISIBLE
                testView.alpha = 1.0f
                testView.invalidate()
                testView.requestLayout()
            }, 500)

            // 15 saniye sonra kaldır
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    windowManager.removeView(testView)
                    android.util.Log.d("MainActivity", "Test view kaldırıldı")
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "Test view kaldırılamadı", e)
                }
            }, 15000)

            Toast.makeText(this, "✅ TEST OVERLAY EKLENDİ!\nEkranda KIRMIZI KUTU görünmeli!", Toast.LENGTH_LONG).show()
        } catch (e: SecurityException) {
            DebugLogger.logException("MainActivity", "SecurityException - Test overlay eklenemedi", e)
            android.util.Log.e("MainActivity", "❌ SecurityException!", e)
            Toast.makeText(this, "❌ SecurityException: ${e.message}", Toast.LENGTH_LONG).show()
        } catch (e: IllegalArgumentException) {
            DebugLogger.logException("MainActivity", "IllegalArgumentException - Test overlay parametreleri hatalı", e)
            android.util.Log.e("MainActivity", "❌ IllegalArgumentException!", e)
            Toast.makeText(this, "❌ IllegalArgumentException: ${e.message}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            DebugLogger.logException("MainActivity", "Genel hata - Test overlay eklenemedi", e)
            android.util.Log.e("MainActivity", "❌ Genel hata!", e)
            Toast.makeText(this, "❌ HATA: ${e.javaClass.simpleName} - ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun detectGameOnStart() {
        Handler(Looper.getMainLooper()).postDelayed({
            detectGame()
        }, 500)
    }

    private fun detectGame() {
        tvStatus.text = "Oyun tespit ediliyor..."

        val gameInfo = GameDetector.detectGame(this)

        if (gameInfo != null) {
            detectedGame = gameInfo
            tvGameInfo.text = """
                ✅ Oyun Bulundu!
                İsim: ${gameInfo.appName}
                Paket: ${gameInfo.packageName}
                Versiyon: ${gameInfo.versionName}
            """.trimIndent()
            tvStatus.text = "Oyun hazır"
            btnLaunchGame.isEnabled = true
        } else {
            tvGameInfo.text = "❌ 8 Ball Pool bulunamadı!\nLütfen oyunu yükleyin."
            tvStatus.text = "Oyun bulunamadı"
            btnLaunchGame.isEnabled = false
            Toast.makeText(this, "8 Ball Pool oyunu bulunamadı!", Toast.LENGTH_LONG).show()
        }
    }

    private fun launchGame() {
        detectedGame?.let { gameInfo ->
            // Overlay izni kontrolü
            if (!checkOverlayPermission()) {
                Toast.makeText(this, "Overlay izni gerekli! Lütfen izin verin.", Toast.LENGTH_LONG).show()
                requestOverlayPermission()
                return
            }

            tvStatus.text = "Oyun başlatılıyor..."

            if (GameLauncher.launchGame(this, gameInfo.packageName)) {
                tvStatus.text = "Oyun başlatıldı, mod menu açılıyor..."

                // Oyun başladıktan sonra mod menu'yu otomatik başlat
                Handler(Looper.getMainLooper()).postDelayed({
                    startModMenu()
                }, 2000)
            } else {
                tvStatus.text = "Oyun başlatılamadı"
                Toast.makeText(this, "Oyun başlatılamadı!", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Toast.makeText(this, "Önce oyunu tespit edin!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                startActivity(intent)
            } else {
                @Suppress("DEPRECATION")
                startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION)
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            if (checkOverlayPermission()) {
                startModMenu()
            } else {
                Toast.makeText(this, "Overlay izni gerekli!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        android.util.Log.d("MainActivity", "=== onNewIntent() çağrıldı ===")
        android.util.Log.d("MainActivity", "Intent action: ${intent?.action}")
        android.util.Log.d("MainActivity", "isScreenCaptureDialogOpen: $isScreenCaptureDialogOpen")

        // Əgər permission dialog-u açıqdırsa, yeni intent-i işlətmə
        // Çünki dialog callback-i zəng edəcək və dialog bağlanacaq
        if (isScreenCaptureDialogOpen) {
            android.util.Log.d("MainActivity", "⚠️ Screen capture dialog açıqdır, yeni intent işlənmir...")
            // Intent-i set etmə - dialog callback-i çağrılana qədər gözlə
            return
        }

        setIntent(intent) // Intent-i set et ki, onResume'da da işləsin

        // Screen capture izni isteği kontrolü
        if (intent?.action == ACTION_REQUEST_SCREEN_CAPTURE && !screenCapturePermissionRequested) {
            android.util.Log.d("MainActivity", "=== ACTION_REQUEST_SCREEN_CAPTURE alındı (onNewIntent) ===")
            android.util.Log.d("MainActivity", "Screen capture permission dialog-u açılır...")
            screenCapturePermissionRequested = true
            // Handler ilə kiçik bir gecikmə əlavə et ki, activity tam hazır olsun
            Handler(Looper.getMainLooper()).postDelayed({
                if (!isScreenCaptureDialogOpen) {
                    requestScreenCapturePermission()
                }
            }, 100)
        }
    }

    override fun onPause() {
        super.onPause()
        android.util.Log.d("MainActivity", "=== onPause() çağrıldı ===")
        android.util.Log.d("MainActivity", "isScreenCaptureDialogOpen: $isScreenCaptureDialogOpen")

        // Əgər permission dialog-u açıqdırsa, onu bağlama
        // Dialog açıq olduqda activity-nin pause olması normaldır
    }

    override fun onResume() {
        super.onResume()
        android.util.Log.d("MainActivity", "=== onResume() çağrıldı ===")
        android.util.Log.d("MainActivity", "Intent action: ${intent?.action}")
        android.util.Log.d("MainActivity", "isScreenCaptureDialogOpen: $isScreenCaptureDialogOpen")
        android.util.Log.d("MainActivity", "screenCapturePermissionRequested: $screenCapturePermissionRequested")

        // Əgər permission dialog-u açıqdırsa, onResume-də heç nə etmə
        // Çünki dialog callback-i zəng edəcək və dialog bağlanacaq
        if (isScreenCaptureDialogOpen) {
            android.util.Log.d("MainActivity", "⚠️ Screen capture dialog açıqdır, callback gözlənilir...")
            android.util.Log.d("MainActivity", "⚠️ Intent action təmizlənmir, dialog callback-i gözlənilir...")
            // Intent action-u təmizləmə - dialog callback-i çağrılana qədər saxla
            return
        }

        // Screen capture izni isteği kontrolü (yalnız bir dəfə və dialog açıq deyilsə)
        if (intent?.action == ACTION_REQUEST_SCREEN_CAPTURE && !screenCapturePermissionRequested) {
            android.util.Log.d("MainActivity", "=== ACTION_REQUEST_SCREEN_CAPTURE alındı (onResume) ===")
            android.util.Log.d("MainActivity", "Screen capture permission dialog-u açılır...")
            screenCapturePermissionRequested = true
            // Handler ilə kiçik bir gecikmə əlavə et ki, activity tam resume olsun
            Handler(Looper.getMainLooper()).postDelayed({
                if (!isScreenCaptureDialogOpen) {
                    requestScreenCapturePermission()
                }
            }, 100)
        }

        // Overlay izni kontrolü
        if (checkOverlayPermission()) {
            tvStatus.text = "Hazır"
        }
    }

    private fun startModMenu() {
        detectedGame?.let { gameInfo ->
            val intent = Intent(this, ModMenuService::class.java)
            intent.putExtra("game_package", gameInfo.packageName)
            intent.action = ModMenuService.ACTION_START

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }

            tvStatus.text = "Mod Menu aktif"
            Toast.makeText(this, "Mod Menu başlatıldı!", Toast.LENGTH_SHORT).show()
        } ?: run {
            Toast.makeText(this, "Önce oyunu tespit edin!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopModMenu() {
        val intent = Intent(this, ModMenuService::class.java)
        intent.action = ModMenuService.ACTION_STOP
        stopService(intent)

        tvStatus.text = "Mod Menu durduruldu"
        Toast.makeText(this, "Mod Menu durduruldu", Toast.LENGTH_SHORT).show()
    }

    private fun requestScreenCapturePermission() {
        android.util.Log.d("MainActivity", "=== requestScreenCapturePermission() çağrıldı ===")
        android.util.Log.d("MainActivity", "Activity state: isFinishing=${isFinishing}, isDestroyed=${if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) isDestroyed else "N/A"}")

        // Əgər dialog artıq açıqdırsa, yenidən açma
        if (isScreenCaptureDialogOpen) {
            android.util.Log.w("MainActivity", "⚠️ Screen capture dialog artıq açıqdır, yenidən açılmır")
            return
        }

        // Əgər activity bağlanıbsa, dialog açma
        if (isFinishing || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && isDestroyed)) {
            android.util.Log.w("MainActivity", "⚠️ Activity bağlanıb, dialog açılmır")
            return
        }

        try {
            val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            if (projectionManager == null) {
                android.util.Log.e("MainActivity", "❌ MediaProjectionManager null!")
                Toast.makeText(this, "❌ MediaProjectionManager null!", Toast.LENGTH_LONG).show()
                return
            }

            val intent = projectionManager.createScreenCaptureIntent()
            if (intent == null) {
                android.util.Log.e("MainActivity", "❌ Screen capture intent null!")
                Toast.makeText(this, "❌ Screen capture intent null!", Toast.LENGTH_LONG).show()
                return
            }

            // Dialog açıldığını qeyd et
            isScreenCaptureDialogOpen = true
            android.util.Log.d("MainActivity", "✅ Screen capture intent yaradıldı, dialog açılır...")
            android.util.Log.d("MainActivity", "✅ Activity state: isFinishing=${isFinishing}, isDestroyed=${if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) isDestroyed else "N/A"}")

            // Dialog-u açmaq üçün kiçik bir gecikmə əlavə et ki, activity tam hazır olsun
            Handler(Looper.getMainLooper()).post {
                if (!isFinishing && (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1 || !isDestroyed)) {
                    try {
                        screenCaptureLauncher.launch(intent)
                        android.util.Log.d("MainActivity", "✅ Screen capture launcher başlatıldı")
                    } catch (e: Exception) {
                        android.util.Log.e("MainActivity", "❌ Screen capture launcher hatası: ${e.message}", e)
                        isScreenCaptureDialogOpen = false
                        Toast.makeText(this@MainActivity, "❌ Ekran yakalama izni hatası: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                } else {
                    android.util.Log.w("MainActivity", "⚠️ Activity bağlanıb, dialog açılmır")
                    isScreenCaptureDialogOpen = false
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "❌ requestScreenCapturePermission hatası: ${e.message}", e)
            isScreenCaptureDialogOpen = false
            Toast.makeText(this, "❌ Ekran yakalama izni hatası: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        private const val REQUEST_OVERLAY_PERMISSION = 1001
        const val ACTION_REQUEST_SCREEN_CAPTURE = "com.poolmod.menu.REQUEST_SCREEN_CAPTURE"
    }
}

