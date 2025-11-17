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

    init {
        setupView()
    }

    private fun setupView() {
        orientation = VERTICAL
        
        // Minimum boyut ayarla - önce bunu ayarla
        minimumWidth = 350
        minimumHeight = 250
        
        // Görünürlük ayarları - kesinlikle görünür olmalı
        visibility = View.VISIBLE
        alpha = 1.0f
        
        // Arka plan daha görünür yap - parlak kırmızı kenarlık ile
        val drawable = android.graphics.drawable.GradientDrawable().apply {
            setColor(Color.parseColor("#FF000000")) // Tam opak siyah - daha görünür
            cornerRadius = 15f
            setStroke(8, Color.parseColor("#FF0000")) // Çok kalın kırmızı kenarlık
        }
        background = drawable
        
        setPadding(30, 25, 30, 25)
        
        // Zorla görünür yap
        setWillNotDraw(false)
        
        android.util.Log.d("ModMenuView", "ModMenuView setupView() tamamlandı")
        android.util.Log.d("ModMenuView", "Visibility: $visibility (VISIBLE=${View.VISIBLE}), Alpha: $alpha")
        android.util.Log.d("ModMenuView", "MinWidth: $minimumWidth, MinHeight: $minimumHeight")
        android.util.Log.d("ModMenuView", "Background: ${background != null}")

        // Başlık
        val title = TextView(context).apply {
            text = "🎱 8 Ball Pool Mod"
            textSize = 16f
            setTextColor(Color.parseColor("#FFD700"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 15)
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        addView(title)

        // Minimize butonu
        val btnMinimize = Button(context).apply {
            text = "▼ Küçült"
            setBackgroundColor(Color.parseColor("#FF9800"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                onCloseListener?.invoke() // Bu minimize yapacak
            }
        }
        addView(btnMinimize, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

        // Ayırıcı
        addSeparator()

        // Mod özellikleri
        addModOption("🎯 Auto Aim", ModMenuConfig.MOD_AUTO_AIM)
        addModOption("📊 Top Yolu Göster", ModMenuConfig.MOD_BALL_TRAJECTORY)
    }

    private fun addModOption(name: String, modKey: String) {
        val container = LinearLayout(context).apply {
            orientation = HORIZONTAL
            setPadding(0, 8, 0, 8)
        }

        val textView = TextView(context).apply {
            text = name
            setTextColor(Color.WHITE)
            textSize = 13f
            layoutParams = LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        }

        val switch = Switch(context).apply {
            isChecked = modConfig.isModEnabled(modKey)
            setOnCheckedChangeListener { _, isChecked ->
                modConfig.setModEnabled(modKey, isChecked)
                // Mod aktifleştirildiğinde hook'u uygula
                applyModHook(modKey, isChecked)
            }
        }

        container.addView(textView)
        container.addView(switch)
        addView(container)
    }

    private fun addSeparator() {
        val separator = View(context).apply {
            setBackgroundColor(Color.parseColor("#33FFFFFF"))
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 2)
        }
        addView(separator)
        val margin = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            setMargins(0, 10, 0, 10)
        }
        separator.layoutParams = margin
    }

    private fun applyModHook(modKey: String, enabled: Boolean) {
        // Mod hook'larını uygula (native hook sistemi ile)
        ModHookManager.applyHook(modKey, enabled)
        
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
    }

    fun setOnCloseListener(listener: () -> Unit) {
        this.onCloseListener = listener
    }
}

