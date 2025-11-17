package com.poolmod.menu

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
            if (result.resultCode == RESULT_OK) {
                val data = result.data
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
                requestScreenCapturePermission()
            }
        } catch (e: Exception) {
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
            android.util.Log.e("MainActivity", "❌ SecurityException!", e)
            e.printStackTrace()
            Toast.makeText(this, "❌ SecurityException: ${e.message}", Toast.LENGTH_LONG).show()
        } catch (e: IllegalArgumentException) {
            android.util.Log.e("MainActivity", "❌ IllegalArgumentException!", e)
            e.printStackTrace()
            Toast.makeText(this, "❌ IllegalArgumentException: ${e.message}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "❌ Genel hata!", e)
            e.printStackTrace()
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

    override fun onResume() {
        super.onResume()
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
        val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val intent = projectionManager.createScreenCaptureIntent()
        screenCaptureLauncher.launch(intent)
    }

    companion object {
        private const val REQUEST_OVERLAY_PERMISSION = 1001
        const val ACTION_REQUEST_SCREEN_CAPTURE = "com.poolmod.menu.REQUEST_SCREEN_CAPTURE"
    }
}

