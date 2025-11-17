package com.poolmod.menu

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.widget.TextView

/**
 * Çok basit test view - sadece görünürlüğü test etmek için
 */
class SimpleTestView(context: Context) : TextView(context) {
    
    init {
        text = "🎮 MOD MENU TEST - GÖRÜNÜYOR MU?"
        textSize = 24f
        setTextColor(Color.WHITE)
        gravity = Gravity.CENTER
        setBackgroundColor(Color.parseColor("#FF0000")) // Parlak kırmızı arka plan
        setPadding(50, 50, 50, 50)
        visibility = android.view.View.VISIBLE
        alpha = 1.0f
        
        android.util.Log.d("SimpleTestView", "SimpleTestView oluşturuldu")
    }
}

