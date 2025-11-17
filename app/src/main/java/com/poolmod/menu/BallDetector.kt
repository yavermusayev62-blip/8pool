package com.poolmod.menu

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Top tespiti ve numaralandırma
 * Ekran görüntüsünden topları bulur ve numaralandırır
 */
object BallDetector {

    data class Ball(
        val x: Float,
        val y: Float,
        val radius: Float,
        val number: Int, // Top numarası (1-15, 0=beyaz top, -1=siyah top)
        val color: Int // Top rengi
    )

    /**
     * Ekran görüntüsünden topları tespit et
     */
    fun detectBalls(bitmap: Bitmap): List<Ball> {
        val balls = mutableListOf<Ball>()
        
        try {
            val width = bitmap.width
            val height = bitmap.height
            
            // Masanın yaklaşık konumunu bul (yeşil alan)
            val tableBounds = detectTableBounds(bitmap)
            if (tableBounds == null) {
                Log.d(TAG, "Masa tespit edilemedi")
                return balls
            }

            // Topları tespit et
            val detectedCircles = detectCircles(bitmap, tableBounds)
            
            // Topları numaralandır ve renklendir
            detectedCircles.forEachIndexed { index, circle ->
                val ballNumber = identifyBallNumber(bitmap, circle)
                val ballColor = getBallColor(bitmap, circle)
                
                balls.add(Ball(
                    x = circle.x,
                    y = circle.y,
                    radius = circle.radius,
                    number = ballNumber,
                    color = ballColor
                ))
            }
            
            Log.d(TAG, "${balls.size} top tespit edildi")
        } catch (e: Exception) {
            Log.e(TAG, "Top tespit hatası: ${e.message}", e)
        }
        
        return balls
    }

    /**
     * Masa sınırlarını tespit et (yeşil alan)
     */
    fun detectTableBounds(bitmap: Bitmap): TableBounds? {
        val width = bitmap.width
        val height = bitmap.height
        
        // Ekranın ortasından başla
        val centerX = width / 2
        val centerY = height / 2
        
        // Yeşil renk aralığı (masa rengi)
        val greenThreshold = 50
        var left = centerX
        var right = centerX
        var top = centerY
        var bottom = centerY
        
        // Yatay tarama
        for (x in 0 until width step 10) {
            val pixel = bitmap.getPixel(x, centerY)
            val green = Color.green(pixel)
            if (green > greenThreshold) {
                if (x < left) left = x
                if (x > right) right = x
            }
        }
        
        // Dikey tarama
        for (y in 0 until height step 10) {
            val pixel = bitmap.getPixel(centerX, y)
            val green = Color.green(pixel)
            if (green > greenThreshold) {
                if (y < top) top = y
                if (y > bottom) bottom = y
            }
        }
        
        if (right - left > 100 && bottom - top > 100) {
            return TableBounds(left, top, right, bottom)
        }
        
        return null
    }

    /**
     * Dairesel şekilleri tespit et (toplar)
     */
    private fun detectCircles(bitmap: Bitmap, bounds: TableBounds): List<Circle> {
        val circles = mutableListOf<Circle>()
        val width = bitmap.width
        val height = bitmap.height
        
        // Top yarıçapı tahmini (ekran boyutuna göre)
        val minRadius = minOf(width, height) / 50f
        val maxRadius = minOf(width, height) / 20f
        
        // Grid tarama
        val step = minRadius.toInt()
        for (y in bounds.top until bounds.bottom step step) {
            for (x in bounds.left until bounds.right step step) {
                if (x < 0 || x >= width || y < 0 || y >= height) continue
                
                val pixel = bitmap.getPixel(x, y)
                val brightness = getBrightness(pixel)
                
                // Parlak veya koyu renkli alanları kontrol et (toplar)
                if (brightness > 100 || brightness < 50) {
                    val circle = detectCircleAt(bitmap, x, y, minRadius, maxRadius)
                    if (circle != null && !isDuplicate(circles, circle)) {
                        circles.add(circle)
                    }
                }
            }
        }
        
        return circles
    }

    /**
     * Belirli bir noktada daire tespit et
     */
    private fun detectCircleAt(
        bitmap: Bitmap,
        centerX: Int,
        centerY: Int,
        minRadius: Float,
        maxRadius: Float
    ): Circle? {
        val width = bitmap.width
        val height = bitmap.height
        
        var bestRadius = minRadius
        var bestScore = 0f
        
        // Farklı yarıçapları dene
        for (radius in minRadius.toInt()..maxRadius.toInt() step 2) {
            var score = 0f
            var total = 0
            
            // Daire çevresini kontrol et
            for (angle in 0..360 step 10) {
                val rad = Math.toRadians(angle.toDouble())
                val x = (centerX + radius * Math.cos(rad)).toInt()
                val y = (centerY + radius * Math.sin(rad)).toInt()
                
                if (x in 0 until width && y in 0 until height) {
                    val pixel = bitmap.getPixel(x, y)
                    val brightness = getBrightness(pixel)
                    
                    // Daire kenarı parlak veya koyu olmalı
                    if (brightness > 150 || brightness < 50) {
                        score++
                    }
                    total++
                }
            }
            
            val ratio = if (total > 0) score / total else 0f
            if (ratio > 0.6f && ratio > bestScore) {
                bestScore = ratio
                bestRadius = radius.toFloat()
            }
        }
        
        return if (bestScore > 0.6f) {
            Circle(centerX.toFloat(), centerY.toFloat(), bestRadius)
        } else {
            null
        }
    }

    /**
     * Top numarasını belirle (renk analizi ile)
     */
    private fun identifyBallNumber(bitmap: Bitmap, circle: Circle): Int {
        val x = circle.x.toInt()
        val y = circle.y.toInt()
        val radius = circle.radius.toInt()
        
        if (x < 0 || x >= bitmap.width || y < 0 || y >= bitmap.height) {
            return -1
        }
        
        // Topun merkezinden renk örnekleri al
        val colors = mutableListOf<Int>()
        for (dx in -radius/2..radius/2 step 5) {
            for (dy in -radius/2..radius/2 step 5) {
                val px = (x + dx).coerceIn(0, bitmap.width - 1)
                val py = (y + dy).coerceIn(0, bitmap.height - 1)
                colors.add(bitmap.getPixel(px, py))
            }
        }
        
        // Renk analizi
        val avgColor = getAverageColor(colors)
        val r = Color.red(avgColor)
        val g = Color.green(avgColor)
        val b = Color.blue(avgColor)
        val brightness = getBrightness(avgColor)
        
        // Beyaz top (çok parlak)
        if (brightness > 200) {
            return 0
        }
        
        // Siyah top (çok koyu)
        if (brightness < 30) {
            return -1
        }
        
        // Renkli toplar (basit renk tespiti)
        val ballNumber = identifyColorBall(r, g, b)
        return ballNumber
    }

    /**
     * Renkli top numarasını belirle
     */
    private fun identifyColorBall(r: Int, g: Int, b: Int): Int {
        // Basit renk eşleştirme (gerçek implementasyon daha karmaşık olabilir)
        val hue = getHue(r, g, b)
        
        return when {
            hue in 0f..30f || hue in 330f..360f -> 1 // Kırmızı
            hue in 30f..60f -> 2 // Turuncu
            hue in 60f..90f -> 3 // Sarı
            hue in 90f..150f -> 4 // Yeşil
            hue in 150f..210f -> 5 // Cyan/Mavi
            hue in 210f..270f -> 6 // Mavi
            hue in 270f..330f -> 7 // Mor
            else -> 8 // Varsayılan
        }
    }

    /**
     * Top rengini al
     */
    private fun getBallColor(bitmap: Bitmap, circle: Circle): Int {
        val x = circle.x.toInt().coerceIn(0, bitmap.width - 1)
        val y = circle.y.toInt().coerceIn(0, bitmap.height - 1)
        return bitmap.getPixel(x, y)
    }

    /**
     * Parlaklık hesapla
     */
    private fun getBrightness(color: Int): Int {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        return ((r + g + b) / 3).toInt()
    }

    /**
     * Ortalama renk hesapla
     */
    private fun getAverageColor(colors: List<Int>): Int {
        if (colors.isEmpty()) return Color.BLACK
        
        var r = 0
        var g = 0
        var b = 0
        
        colors.forEach { color ->
            r += Color.red(color)
            g += Color.green(color)
            b += Color.blue(color)
        }
        
        val count = colors.size
        return Color.rgb(r / count, g / count, b / count)
    }

    /**
     * Hue hesapla
     */
    private fun getHue(r: Int, g: Int, b: Int): Float {
        val rNorm = r / 255f
        val gNorm = g / 255f
        val bNorm = b / 255f
        
        val max = maxOf(rNorm, gNorm, bNorm)
        val min = minOf(rNorm, gNorm, bNorm)
        val delta = max - min
        
        if (delta == 0f) return 0f
        
        val hue = when (max) {
            rNorm -> ((gNorm - bNorm) / delta) % 6
            gNorm -> ((bNorm - rNorm) / delta) + 2
            else -> ((rNorm - gNorm) / delta) + 4
        }
        
        return (hue * 60).coerceIn(0f, 360f)
    }

    /**
     * Duplicate kontrolü
     */
    private fun isDuplicate(circles: List<Circle>, newCircle: Circle): Boolean {
        return circles.any { existing ->
            val distance = sqrt(
                (existing.x - newCircle.x).pow(2) + 
                (existing.y - newCircle.y).pow(2)
            )
            distance < existing.radius + newCircle.radius
        }
    }

    data class Circle(
        val x: Float,
        val y: Float,
        val radius: Float
    )

    data class TableBounds(
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int
    )

    private const val TAG = "BallDetector"
}

