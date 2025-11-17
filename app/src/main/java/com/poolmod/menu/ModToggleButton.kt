package com.poolmod.menu

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.TextView

/**
 * Küçük toggle butonu - Menüyü açmak/kapatmak için
 * Sürüklenebilir
 */

class ModToggleButton(context: Context) : TextView(context) {
    
    init {
        android.util.Log.d("ModToggleButton", "🔵 ModToggleButton oluşturuluyor...")
    }

    private var isMenuOpen = false
    private var initialX = 0f
    private var initialY = 0f
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    var onClickCallback: (() -> Unit)? = null

    init {
        setupButton()
    }

    private fun setupButton() {
        android.util.Log.d("ModToggleButton", "🔵 setupButton() çağrıldı")
        text = "🎮"
        textSize = 24f
        setTextColor(Color.WHITE)
        gravity = Gravity.CENTER
        
        // Arka plan - daha parlak ve görünür
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor("#FF000000")) // Tam opak siyah
            setStroke(4, Color.parseColor("#FFD700")) // Kalın altın kenarlık
        }
        background = drawable
        
        // Padding
        setPadding(20, 20, 20, 20)
        
        // Minimum boyut - daha büyük
        minimumWidth = 80
        minimumHeight = 80
        
        // Tıklanabilir ve görünür - overlay için özel ayarlar
        isClickable = true
        isFocusable = true // Touch event'ler için focusable olmalı
        isFocusableInTouchMode = true
        visibility = View.VISIBLE
        alpha = 1.0f
        setWillNotDraw(false) // Zorla çiz
        elevation = 10f // Gölge ekle
        
        android.util.Log.d("ModToggleButton", "🔵 setupButton() tamamlandı")
        android.util.Log.d("ModToggleButton", "isClickable=$isClickable, isFocusable=$isFocusable, isFocusableInTouchMode=$isFocusableInTouchMode")
        android.util.Log.d("ModToggleButton", "visibility=$visibility, alpha=$alpha")
    }
    
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        android.util.Log.d("ModToggleButton", "🔵🔵🔵 onAttachedToWindow() çağrıldı!")
    }
    
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        android.util.Log.d("ModToggleButton", "🔵🔵🔵 onDetachedFromWindow() çağrıldı!")
    }
    
    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        android.util.Log.d("ModToggleButton", "🔵 onWindowFocusChanged: $hasWindowFocus")
    }

    fun setMenuOpen(open: Boolean) {
        isMenuOpen = open
        updateAppearance()
    }

    private fun updateAppearance() {
        if (isMenuOpen) {
            text = "▼"
            // Menü açıkken küçük göster
            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#CCFF4444"))
                setStroke(3, Color.parseColor("#FFD700"))
            }
            background = drawable
        } else {
            text = "🎮"
            // Menü kapalıyken normal göster
            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#CC000000"))
                setStroke(3, Color.parseColor("#FFD700"))
            }
            background = drawable
        }
    }

    var onPositionUpdate: ((Int, Int) -> Unit)? = null

    override fun onTouchEvent(event: MotionEvent): Boolean {
        android.util.Log.d("ModToggleButton", "🔵🔵🔵 onTouchEvent çağrıldı! action=${event.action}, rawX=${event.rawX}, rawY=${event.rawY}")
        android.util.Log.d("ModToggleButton", "View durumu: visibility=$visibility, alpha=$alpha, width=$width, height=$height")
        android.util.Log.d("ModToggleButton", "View durumu: isClickable=$isClickable, isFocusable=$isFocusable, isFocusableInTouchMode=$isFocusableInTouchMode")
        android.util.Log.d("ModToggleButton", "View durumu: isAttachedToWindow=$isAttachedToWindow, parent=$parent")
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                android.util.Log.d("ModToggleButton", "ACTION_DOWN - Touch başladı!")
                val params = layoutParams as? android.view.WindowManager.LayoutParams
                params?.let {
                    initialX = it.x.toFloat()
                    initialY = it.y.toFloat()
                    android.util.Log.d("ModToggleButton", "Başlangıç pozisyonu: x=$initialX, y=$initialY")
                }
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                android.util.Log.d("ModToggleButton", "Touch pozisyonu: rawX=$initialTouchX, rawY=$initialTouchY")
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                android.util.Log.d("ModToggleButton", "ACTION_MOVE - Sürükleniyor...")
                val params = layoutParams as? android.view.WindowManager.LayoutParams
                params?.let { layoutParams ->
                    val deltaX = event.rawX - initialTouchX
                    val deltaY = event.rawY - initialTouchY
                    val newX = (initialX + deltaX).toInt()
                    val newY = (initialY + deltaY).toInt()
                    android.util.Log.d("ModToggleButton", "Yeni pozisyon: x=$newX, y=$newY (deltaX=$deltaX, deltaY=$deltaY)")
                    layoutParams.x = newX
                    layoutParams.y = newY
                    onPositionUpdate?.invoke(newX, newY)
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                // Tıklama kontrolü
                val deltaX = Math.abs(event.rawX - initialTouchX)
                val deltaY = Math.abs(event.rawY - initialTouchY)
                if (deltaX < 20 && deltaY < 20) {
                    // Sadece tıklama, sürükleme değil
                    android.util.Log.d("ModToggleButton", "Tıklama algılandı! Callback çağrılıyor...")
                    // Overlay view'ler için performClick() çalışmayabilir, direkt callback kullan
                    try {
                        // Önce performClick'i dene (eğer listener varsa)
                        if (hasOnClickListeners()) {
                            performClick()
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("ModToggleButton", "performClick hatası (normal olabilir): ${e.message}")
                    }
                    // Her durumda callback'i çağır (daha güvenilir)
                    onClickCallback?.invoke()
                } else {
                    android.util.Log.d("ModToggleButton", "Sürükleme algılandı (deltaX=$deltaX, deltaY=$deltaY), tıklama değil")
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}

