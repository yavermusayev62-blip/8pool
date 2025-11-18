package com.poolmod.menu

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat

class ModMenuView(context: Context) : LinearLayout(context) {

    private var onCloseListener: (() -> Unit)? = null
    private val modConfig = ModMenuConfig(context)
    private val switchMap = mutableMapOf<String, Switch>()
    private val switchListenerMap = mutableMapOf<String, android.widget.CompoundButton.OnCheckedChangeListener>()

    init {
        setupView()
    }

    private fun setupView() {
        orientation = VERTICAL
        
        // Yığcam boyut - kiçik düzbucaqlı qutu (delik pozisyon kontrolleri için genişletildi)
        minimumWidth = 280
        minimumHeight = 380
        
        // Görünürlük ayarları - kesinlikle görünür olmalı
        visibility = View.VISIBLE
        alpha = 1.0f
        
        // Arka plan - yığcam və gözəl
        val drawable = android.graphics.drawable.GradientDrawable().apply {
            setColor(Color.parseColor("#E6000000")) // Yarı şəffaf qara
            cornerRadius = 12f
            setStroke(3, Color.parseColor("#FFD700")) // Altın rəngli kenarlık
        }
        background = drawable
        
        // Kiçik padding - yığcam görünsün
        setPadding(20, 15, 20, 15)
        
        // Zorla görünür yap
        setWillNotDraw(false)
        
        android.util.Log.d("ModMenuView", "ModMenuView setupView() tamamlandı")
        android.util.Log.d("ModMenuView", "Visibility: $visibility (VISIBLE=${View.VISIBLE}), Alpha: $alpha")
        android.util.Log.d("ModMenuView", "MinWidth: $minimumWidth, MinHeight: $minimumHeight")
        android.util.Log.d("ModMenuView", "Background: ${background != null}")

        // Kiçik başlık
        val title = TextView(context).apply {
            text = "🎱 Mod Menu"
            textSize = 14f
            setTextColor(Color.parseColor("#FFD700"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 10)
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        addView(title)

        // Ayırıcı - daha incə
        val separator = View(context).apply {
            setBackgroundColor(Color.parseColor("#33FFD700"))
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 1).apply {
                setMargins(0, 5, 0, 5)
            }
        }
        addView(separator)

        // Mod özellikleri - yığcam
        addModOption("🎯 Auto Aim", ModMenuConfig.MOD_AUTO_AIM)
        addModOption("📊 Top Yolu", ModMenuConfig.MOD_BALL_TRAJECTORY)
        
        // Ayırıcı
        val separator2 = View(context).apply {
            setBackgroundColor(Color.parseColor("#33FFD700"))
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 1).apply {
                setMargins(0, 5, 0, 5)
            }
        }
        addView(separator2)
        
        // Ekran yakalama izni butonu
        val permissionButton = Button(context).apply {
            text = "📸 Ekran Yakalama İzni Ver"
            textSize = 11f
            setTextColor(Color.WHITE)
            setPadding(10, 8, 10, 8)
            
            // Buton stil
            val buttonDrawable = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#FF4CAF50")) // Yeşil
                cornerRadius = 8f
                setStroke(2, Color.parseColor("#FFD700")) // Altın kenarlık
            }
            background = buttonDrawable
            
            setOnClickListener {
                android.util.Log.d("ModMenuView", "📸 Ekran yakalama izni butonu tıklandı")
                // ModMenuService'e intent gönder
                val intent = android.content.Intent(context, ModMenuService::class.java).apply {
                    action = ModMenuService.ACTION_REQUEST_SCREEN_CAPTURE_PERMISSION
                }
                try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        context.startForegroundService(intent)
                    } else {
                        context.startService(intent)
                    }
                    android.util.Log.d("ModMenuView", "✅ Ekran yakalama izni isteği gönderildi")
                } catch (e: Exception) {
                    android.util.Log.e("ModMenuView", "❌ Intent gönderme hatası: ${e.message}", e)
                    android.widget.Toast.makeText(context, "İzin isteği gönderilemedi: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
        addView(permissionButton)

        // Ayırıcı
        val separator3 = View(context).apply {
            setBackgroundColor(Color.parseColor("#33FFD700"))
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 1).apply {
                setMargins(0, 5, 0, 5)
            }
        }
        addView(separator3)

        // Delik pozisyon ayarları başlığı
        val holeTitle = TextView(context).apply {
            text = "🔧 Delik Pozisyonu"
            textSize = 11f
            setTextColor(Color.parseColor("#FFD700"))
            gravity = Gravity.CENTER
            setPadding(0, 5, 0, 5)
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        addView(holeTitle)

        // Delik pozisyon bilgisi (mevcut offset ve scale)
        val holeInfo = TextView(context).apply {
            textSize = 9f
            setTextColor(Color.parseColor("#CCCCCC"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 5)
        }
        addView(holeInfo)

        // Delik pozisyon ayarlarını güncelleme fonksiyonu
        fun updateHoleInfo() {
            val offsetX = modConfig.getHoleOffsetX()
            val offsetY = modConfig.getHoleOffsetY()
            val scale = modConfig.getHoleScale()
            holeInfo.text = "X: ${String.format("%.1f", offsetX)} Y: ${String.format("%.1f", offsetY)} Zoom: ${String.format("%.2f", scale)}x"
        }
        updateHoleInfo()

        // Zoom kontrolleri (yatay layout)
        val zoomContainer = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 5, 0, 5)
        }

        // Zoom out butonu
        val zoomOutButton = Button(context).apply {
            text = "➖ Küçült"
            textSize = 10f
            setTextColor(Color.WHITE)
            setPadding(8, 6, 8, 6)
            minWidth = 0
            minimumWidth = 0
            val buttonDrawable = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#FF444444"))
                cornerRadius = 6f
                setStroke(1, Color.parseColor("#FFD700"))
            }
            background = buttonDrawable
            
            setOnClickListener {
                val currentScale = modConfig.getHoleScale()
                val newScale = (currentScale - 0.1f).coerceAtLeast(0.5f)
                modConfig.setHoleScale(newScale)
                updateHoleInfo()
                // ModMenuService'e ayar güncellendiğini bildir
                notifyHoleSettingsChanged()
            }
            layoutParams = LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(2, 0, 2, 0)
            }
        }
        zoomContainer.addView(zoomOutButton)

        // Zoom in butonu
        val zoomInButton = Button(context).apply {
            text = "➕ Büyüt"
            textSize = 10f
            setTextColor(Color.WHITE)
            setPadding(8, 6, 8, 6)
            minWidth = 0
            minimumWidth = 0
            val buttonDrawable = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#FF444444"))
                cornerRadius = 6f
                setStroke(1, Color.parseColor("#FFD700"))
            }
            background = buttonDrawable
            
            setOnClickListener {
                val currentScale = modConfig.getHoleScale()
                val newScale = (currentScale + 0.1f).coerceAtMost(2.0f)
                modConfig.setHoleScale(newScale)
                updateHoleInfo()
                notifyHoleSettingsChanged()
            }
            layoutParams = LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(2, 0, 2, 0)
            }
        }
        zoomContainer.addView(zoomInButton)

        addView(zoomContainer)

        // Hareket kontrolleri (3x3 grid)
        val movementContainer = LinearLayout(context).apply {
            orientation = VERTICAL
            gravity = Gravity.CENTER
            setPadding(0, 5, 0, 5)
        }

        // Üst satır (yukarı)
        val topRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
        }
        
        val spacer1 = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        }
        topRow.addView(spacer1)

        val upButton = Button(context).apply {
            text = "⬆️"
            textSize = 12f
            setTextColor(Color.WHITE)
            setPadding(8, 6, 8, 6)
            minWidth = 0
            minimumWidth = 0
            val buttonDrawable = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#FF444444"))
                cornerRadius = 6f
                setStroke(1, Color.parseColor("#FFD700"))
            }
            background = buttonDrawable
            
            setOnClickListener {
                val currentOffsetY = modConfig.getHoleOffsetY()
                val newOffsetY = currentOffsetY - 10f
                modConfig.setHoleOffsetY(newOffsetY)
                updateHoleInfo()
                notifyHoleSettingsChanged()
            }
            layoutParams = LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        }
        topRow.addView(upButton)

        val spacer2 = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        }
        topRow.addView(spacer2)

        movementContainer.addView(topRow)

        // Orta satır (sol, sağ)
        val middleRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
        }

        val leftButton = Button(context).apply {
            text = "⬅️"
            textSize = 12f
            setTextColor(Color.WHITE)
            setPadding(8, 6, 8, 6)
            minWidth = 0
            minimumWidth = 0
            val buttonDrawable = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#FF444444"))
                cornerRadius = 6f
                setStroke(1, Color.parseColor("#FFD700"))
            }
            background = buttonDrawable
            
            setOnClickListener {
                val currentOffsetX = modConfig.getHoleOffsetX()
                val newOffsetX = currentOffsetX - 10f
                modConfig.setHoleOffsetX(newOffsetX)
                updateHoleInfo()
                notifyHoleSettingsChanged()
            }
            layoutParams = LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                setMargins(5, 5, 5, 5)
            }
        }
        middleRow.addView(leftButton)

        val rightButton = Button(context).apply {
            text = "➡️"
            textSize = 12f
            setTextColor(Color.WHITE)
            setPadding(8, 6, 8, 6)
            minWidth = 0
            minimumWidth = 0
            val buttonDrawable = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#FF444444"))
                cornerRadius = 6f
                setStroke(1, Color.parseColor("#FFD700"))
            }
            background = buttonDrawable
            
            setOnClickListener {
                val currentOffsetX = modConfig.getHoleOffsetX()
                val newOffsetX = currentOffsetX + 10f
                modConfig.setHoleOffsetX(newOffsetX)
                updateHoleInfo()
                notifyHoleSettingsChanged()
            }
            layoutParams = LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                setMargins(5, 5, 5, 5)
            }
        }
        middleRow.addView(rightButton)

        movementContainer.addView(middleRow)

        // Alt satır (aşağı)
        val bottomRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
        }
        
        val spacer3 = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        }
        bottomRow.addView(spacer3)

        val downButton = Button(context).apply {
            text = "⬇️"
            textSize = 12f
            setTextColor(Color.WHITE)
            setPadding(8, 6, 8, 6)
            minWidth = 0
            minimumWidth = 0
            val buttonDrawable = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#FF444444"))
                cornerRadius = 6f
                setStroke(1, Color.parseColor("#FFD700"))
            }
            background = buttonDrawable
            
            setOnClickListener {
                val currentOffsetY = modConfig.getHoleOffsetY()
                val newOffsetY = currentOffsetY + 10f
                modConfig.setHoleOffsetY(newOffsetY)
                updateHoleInfo()
                notifyHoleSettingsChanged()
            }
            layoutParams = LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        }
        bottomRow.addView(downButton)

        val spacer4 = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        }
        bottomRow.addView(spacer4)

        movementContainer.addView(bottomRow)

        addView(movementContainer)

        // Sıfırla butonu
        val resetButton = Button(context).apply {
            text = "🔄 Sıfırla"
            textSize = 10f
            setTextColor(Color.WHITE)
            setPadding(8, 6, 8, 6)
            val buttonDrawable = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#FFFF6B6B"))
                cornerRadius = 6f
                setStroke(1, Color.parseColor("#FFD700"))
            }
            background = buttonDrawable
            
            setOnClickListener {
                modConfig.setHoleOffsetX(0f)
                modConfig.setHoleOffsetY(0f)
                modConfig.setHoleScale(1f)
                updateHoleInfo()
                notifyHoleSettingsChanged()
            }
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 5, 0, 0)
            }
        }
        addView(resetButton)
    }

    /**
     * Delik ayarları değiştiğinde ModMenuService'e bildir
     */
    private fun notifyHoleSettingsChanged() {
        try {
            val intent = android.content.Intent(context, ModMenuService::class.java).apply {
                action = ModMenuService.ACTION_HOLE_SETTINGS_CHANGED
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            android.util.Log.d("ModMenuView", "✅ Delik ayarları güncellendi bildirimi gönderildi")
        } catch (e: Exception) {
            android.util.Log.e("ModMenuView", "❌ Intent gönderme hatası: ${e.message}", e)
        }
    }

    private fun addModOption(name: String, modKey: String) {
        val container = LinearLayout(context).apply {
            orientation = HORIZONTAL
            setPadding(0, 5, 0, 5)
        }

        val textView = TextView(context).apply {
            text = name
            setTextColor(Color.WHITE)
            textSize = 12f
            layoutParams = LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        }

        // Listener-i əvvəlcə yarat və map-ə əlavə et
        val listener = android.widget.CompoundButton.OnCheckedChangeListener { _, isChecked ->
            try {
                android.util.Log.d("ModMenuView", "🔵🔵🔵 Switch listener çağrıldı: $modKey = $isChecked")
                // Config-i dəyişdir - blocking deyil
                modConfig.setModEnabled(modKey, isChecked)
                android.util.Log.d("ModMenuView", "✅ Config yeniləndi: $modKey = $isChecked")
                // Mod aktifleştirildiğinde hook'u uygula - background thread-də (blocking deyil)
                applyModHook(modKey, isChecked)
                android.util.Log.d("ModMenuView", "✅ applyModHook çağrıldı: $modKey = $isChecked")
            } catch (e: Exception) {
                android.util.Log.e("ModMenuView", "❌ Switch listener hatası: ${e.message}", e)
                e.printStackTrace()
            }
        }
        
        // Listener-i əvvəlcə map-ə əlavə et
        switchListenerMap[modKey] = listener
        
        val switch = Switch(context).apply {
            isChecked = modConfig.isModEnabled(modKey)
            setOnCheckedChangeListener(listener)
            android.util.Log.d("ModMenuView", "✅ Switch yaradıldı: $modKey, isChecked=$isChecked, listener=${listener != null}")
        }
        
        // Switch-i map-ə əlavə et - sonra state-i yeniləmək üçün
        switchMap[modKey] = switch

        container.addView(textView)
        container.addView(switch)
        addView(container)
    }
    
    /**
     * Switch butonlarının state-ini yenilə
     */
    fun updateSwitchStates() {
        android.util.Log.d("ModMenuView", "🔵 updateSwitchStates() çağrıldı - switchMap size: ${switchMap.size}, listenerMap size: ${switchListenerMap.size}")
        switchMap.forEach { (modKey, switch) ->
            val currentState = modConfig.isModEnabled(modKey)
            val listener = switchListenerMap[modKey]
            android.util.Log.d("ModMenuView", "🔵 Switch kontrolü: $modKey - currentState=$currentState, switch.isChecked=${switch.isChecked}, listener=${listener != null}")
            
            if (switch.isChecked != currentState) {
                android.util.Log.d("ModMenuView", "🔵 Switch state yenilənir: $modKey = $currentState (köhnə: ${switch.isChecked})")
                // Listener-i müvəqqəti olaraq sil - sonsuz döngüyü qarşısını almaq üçün
                switch.setOnCheckedChangeListener(null)
                android.util.Log.d("ModMenuView", "🔵 Listener silindi: $modKey")
                
                // State-i dəyişdir
                switch.isChecked = currentState
                android.util.Log.d("ModMenuView", "🔵 Switch state dəyişdirildi: $modKey = $currentState")
                
                // Listener-i yenidən əlavə et
                if (listener != null) {
                    switch.setOnCheckedChangeListener(listener)
                    android.util.Log.d("ModMenuView", "✅ Listener yenidən əlavə edildi: $modKey")
                } else {
                    android.util.Log.e("ModMenuView", "❌ Listener tapılmadı: $modKey - yeni listener yaradılır")
                    // Listener yoxdursa, yeni bir tane yarat
                    val newListener = android.widget.CompoundButton.OnCheckedChangeListener { _, isChecked ->
                        try {
                            android.util.Log.d("ModMenuView", "🔵🔵🔵 Switch listener çağrıldı: $modKey = $isChecked")
                            modConfig.setModEnabled(modKey, isChecked)
                            applyModHook(modKey, isChecked)
                        } catch (e: Exception) {
                            android.util.Log.e("ModMenuView", "❌ Switch listener hatası: ${e.message}", e)
                            e.printStackTrace()
                        }
                    }
                    switchListenerMap[modKey] = newListener
                    switch.setOnCheckedChangeListener(newListener)
                    android.util.Log.d("ModMenuView", "✅ Yeni listener əlavə edildi: $modKey")
                }
            } else {
                // State eynidir, amma listener-in mövcud olduğunu yoxla
                // Listener-i həmişə yenidən əlavə et - təhlükəsizlik üçün
                if (listener != null) {
                    switch.setOnCheckedChangeListener(listener)
                    android.util.Log.d("ModMenuView", "🔵 Listener yenidən təyin edildi: $modKey (state eynidir)")
                }
                android.util.Log.d("ModMenuView", "🔵 Switch state eynidir: $modKey = $currentState, listener=${listener != null}")
            }
        }
    }


    private fun applyModHook(modKey: String, enabled: Boolean) {
        // Background thread-də işlə - main thread-i bloklama
        Thread {
            try {
                // Mod hook'larını uygula (native hook sistemi ile) - background thread-də
                ModHookManager.applyHook(modKey, enabled)
                
                // Service çağrılarını main thread-də et
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    try {
                        // Top yolu modu için özel işlem
                        if (modKey == ModMenuConfig.MOD_BALL_TRAJECTORY) {
                            val intent = android.content.Intent(context, ModMenuService::class.java).apply {
                                action = ModMenuService.ACTION_TOGGLE_TRAJECTORY
                            }
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                context.startForegroundService(intent)
                            } else {
                                context.startService(intent)
                            }
                        }
                        
                        // Auto Aim modu için özel işlem
                        if (modKey == ModMenuConfig.MOD_AUTO_AIM) {
                            val intent = android.content.Intent(context, ModMenuService::class.java).apply {
                                action = ModMenuService.ACTION_TOGGLE_AUTO_AIM
                            }
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                context.startForegroundService(intent)
                            } else {
                                context.startService(intent)
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("ModMenuView", "Service çağrısı hatası: ${e.message}", e)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ModMenuView", "applyModHook hatası: ${e.message}", e)
            }
        }.start()
    }

    fun setOnCloseListener(listener: () -> Unit) {
        this.onCloseListener = listener
    }
}



