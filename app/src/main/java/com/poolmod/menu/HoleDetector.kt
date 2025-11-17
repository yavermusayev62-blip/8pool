package com.poolmod.menu

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Delik tespiti (8 Ball Pool masasındaki 6 delik)
 */
object HoleDetector {

    data class Hole(
        val x: Float,
        val y: Float,
        val radius: Float,
        val number: Int // 1-6 (köşeler ve kenarlar)
    )

    /**
     * Ekran görüntüsünden delikleri tespit et
     */
    fun detectHoles(bitmap: Bitmap, tableBounds: BallDetector.TableBounds?): List<Hole> {
        val holes = mutableListOf<Hole>()
        
        try {
            val width = bitmap.width
            val height = bitmap.height
            
            val bounds = tableBounds ?: BallDetector.TableBounds(0, 0, width, height)
            
            // 8 Ball Pool masasında 6 delik var:
            // 4 köşe + 2 kenar (üst ve alt ortada)
            val tableWidth = bounds.right - bounds.left
            val tableHeight = bounds.bottom - bounds.top
            
            // Delik pozisyonları (masa boyutuna göre)
            val cornerRadius = minOf(tableWidth, tableHeight) / 25f
            val edgeRadius = minOf(tableWidth, tableHeight) / 30f
            
            // Köşe delikleri
            holes.add(Hole(
                x = bounds.left + cornerRadius,
                y = bounds.top + cornerRadius,
                radius = cornerRadius,
                number = 1
            ))
            
            holes.add(Hole(
                x = bounds.right - cornerRadius,
                y = bounds.top + cornerRadius,
                radius = cornerRadius,
                number = 2
            ))
            
            holes.add(Hole(
                x = bounds.left + cornerRadius,
                y = bounds.bottom - cornerRadius,
                radius = cornerRadius,
                number = 3
            ))
            
            holes.add(Hole(
                x = bounds.right - cornerRadius,
                y = bounds.bottom - cornerRadius,
                radius = cornerRadius,
                number = 4
            ))
            
            // Kenar delikleri (üst ve alt ortada)
            holes.add(Hole(
                x = bounds.left + tableWidth / 2f,
                y = bounds.top + edgeRadius,
                radius = edgeRadius,
                number = 5
            ))
            
            holes.add(Hole(
                x = bounds.left + tableWidth / 2f,
                y = bounds.bottom - edgeRadius,
                radius = edgeRadius,
                number = 6
            ))
            
            // Görsel doğrulama için delikleri kontrol et
            val verifiedHoles = verifyHoles(bitmap, holes, bounds)
            
            Log.d(TAG, "${verifiedHoles.size} delik tespit edildi")
            return verifiedHoles
            
        } catch (e: Exception) {
            Log.e(TAG, "Delik tespit hatası: ${e.message}", e)
        }
        
        return holes
    }

    /**
     * Delikleri görsel olarak doğrula (koyu renkli alanlar)
     */
    private fun verifyHoles(
        bitmap: Bitmap,
        holes: List<Hole>,
        bounds: BallDetector.TableBounds
    ): List<Hole> {
        val verifiedHoles = mutableListOf<Hole>()
        
        holes.forEach { hole ->
            if (isValidHole(bitmap, hole, bounds)) {
                verifiedHoles.add(hole)
            }
        }
        
        return verifiedHoles.ifEmpty { holes } // Eğer hiçbiri doğrulanamazsa varsayılan pozisyonları kullan
    }

    /**
     * Delik geçerli mi kontrol et (koyu renkli alan)
     */
    private fun isValidHole(
        bitmap: Bitmap,
        hole: Hole,
        bounds: BallDetector.TableBounds
    ): Boolean {
        val x = hole.x.toInt().coerceIn(0, bitmap.width - 1)
        val y = hole.y.toInt().coerceIn(0, bitmap.height - 1)
        
        if (x < bounds.left || x > bounds.right || y < bounds.top || y > bounds.bottom) {
            return false
        }
        
        // Delik merkezinden örnekler al
        var darkPixelCount = 0
        val sampleCount = 10
        
        for (i in 0 until sampleCount) {
            val angle = (i * 360f / sampleCount) * Math.PI / 180
            val sampleX = (x + hole.radius * Math.cos(angle)).toInt().coerceIn(0, bitmap.width - 1)
            val sampleY = (y + hole.radius * Math.sin(angle)).toInt().coerceIn(0, bitmap.height - 1)
            
            val pixel = bitmap.getPixel(sampleX, sampleY)
            val brightness = getBrightness(pixel)
            
            // Delikler çok koyu olmalı (siyah)
            if (brightness < 30) {
                darkPixelCount++
            }
        }
        
        // En az %70'i koyu olmalı
        return darkPixelCount >= sampleCount * 0.7
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
     * En yakın deliği bul
     */
    fun findNearestHole(ball: BallDetector.Ball, holes: List<Hole>): Hole? {
        if (holes.isEmpty()) return null
        
        var nearestHole: Hole? = null
        var minDistance = Float.MAX_VALUE
        
        holes.forEach { hole ->
            val distance = sqrt(
                (ball.x - hole.x).pow(2) + (ball.y - hole.y).pow(2)
            )
            if (distance < minDistance) {
                minDistance = distance
                nearestHole = hole
            }
        }
        
        return nearestHole
    }

    /**
     * Top için en uygun deliği bul (açı ve mesafe göz önünde bulundurarak)
     */
    fun findBestHoleForBall(
        ball: BallDetector.Ball,
        allBalls: List<BallDetector.Ball>,
        holes: List<Hole>,
        tableWidth: Float,
        tableHeight: Float
    ): Hole? {
        if (holes.isEmpty()) return null
        
        var bestHole: Hole? = null
        var bestScore = 0f
        
        holes.forEach { hole ->
            val score = calculateHoleScore(ball, hole, allBalls, tableWidth, tableHeight)
            if (score > bestScore) {
                bestScore = score
                bestHole = hole
            }
        }
        
        return bestHole
    }

    /**
     * Delik skorunu hesapla (mesafe, açı, engeller)
     */
    private fun calculateHoleScore(
        ball: BallDetector.Ball,
        hole: Hole,
        allBalls: List<BallDetector.Ball>,
        tableWidth: Float,
        tableHeight: Float
    ): Float {
        // Mesafe skoru (yakın delikler daha iyi)
        val distance = sqrt(
            (ball.x - hole.x).pow(2) + (ball.y - hole.y).pow(2)
        )
        val maxDistance = sqrt(tableWidth * tableWidth + tableHeight * tableHeight)
        val distanceScore = 1f - (distance / maxDistance)
        
        // Açı skoru (düz çizgi daha iyi)
        val angle = Math.atan2(
            (hole.y - ball.y).toDouble(),
            (hole.x - ball.x).toDouble()
        )
        val angleScore = 1f // Basit versiyon, daha sonra geliştirilebilir
        
        // Engel skoru (diğer toplar engel oluşturuyor mu?)
        var obstacleScore = 1f
        allBalls.forEach { otherBall ->
            if (otherBall.number != ball.number) {
                val ballToHoleDistance = distance
                val ballToOtherDistance = sqrt(
                    (ball.x - otherBall.x).pow(2) + (ball.y - otherBall.y).pow(2)
                )
                val otherToHoleDistance = sqrt(
                    (otherBall.x - hole.x).pow(2) + (otherBall.y - hole.y).pow(2)
                )
                
                // Eğer diğer top yol üzerindeyse skor düşer
                if (ballToOtherDistance + otherToHoleDistance < ballToHoleDistance * 1.2f) {
                    obstacleScore *= 0.5f
                }
            }
        }
        
        return distanceScore * 0.4f + angleScore * 0.3f + obstacleScore * 0.3f
    }

    private const val TAG = "HoleDetector"
}

