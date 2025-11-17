package com.poolmod.menu

import android.util.Log
import kotlin.math.*

/**
 * Top yolu hesaplama (fizik simülasyonu)
 * Topların gideceği ve duracağı yerleri tahmin eder
 */
object PhysicsCalculator {

    data class BallTrajectory(
        val ballNumber: Int,
        val path: List<Point>, // Topun izleyeceği yol
        val finalPosition: Point, // Topun duracağı yer
        val collisions: List<Collision> // Çarpışmalar
    )

    data class Point(
        val x: Float,
        val y: Float
    )

    data class Collision(
        val ballNumber: Int, // Çarpışan top numarası
        val point: Point, // Çarpışma noktası
        val time: Float // Çarpışma zamanı
    )

    /**
     * Beyaz topun vurulmasından sonra tüm topların yollarını hesapla
     */
    fun calculateTrajectories(
        whiteBall: BallDetector.Ball,
        cueDirection: Float, // Vuruş yönü (derece)
        cuePower: Float, // Vuruş gücü (0-1)
        allBalls: List<BallDetector.Ball>,
        tableWidth: Float,
        tableHeight: Float
    ): List<BallTrajectory> {
        val trajectories = mutableListOf<BallTrajectory>()
        
        try {
            // Beyaz topun başlangıç hızı
            val initialVelocity = cuePower * 500f // Piksel/saniye
            
            // Beyaz topun yolu
            val whiteBallTrajectory = calculateBallPath(
                whiteBall,
                cueDirection,
                initialVelocity,
                allBalls.filter { it.number != 0 },
                tableWidth,
                tableHeight
            )
            trajectories.add(whiteBallTrajectory)
            
            // Diğer topların yolları (çarpışmalardan sonra)
            val hitBalls = findHitBalls(whiteBallTrajectory, allBalls)
            
            hitBalls.forEach { hitBall ->
                val trajectory = calculateHitBallPath(
                    hitBall,
                    whiteBallTrajectory,
                    allBalls,
                    tableWidth,
                    tableHeight
                )
                trajectories.add(trajectory)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Yol hesaplama hatası: ${e.message}", e)
        }
        
        return trajectories
    }

    /**
     * Bir topun yolunu hesapla
     */
    private fun calculateBallPath(
        ball: BallDetector.Ball,
        direction: Float,
        initialVelocity: Float,
        otherBalls: List<BallDetector.Ball>,
        tableWidth: Float,
        tableHeight: Float
    ): BallTrajectory {
        val path = mutableListOf<Point>()
        val collisions = mutableListOf<Collision>()
        
        val angleRad = Math.toRadians(direction.toDouble())
        var vx = initialVelocity * cos(angleRad).toFloat()
        var vy = initialVelocity * sin(angleRad).toFloat()
        
        var x = ball.x
        var y = ball.y
        var time = 0f
        val dt = 0.01f // Zaman adımı
        val friction = 0.98f // Sürtünme katsayısı
        
        path.add(Point(x, y))
        
        // Simülasyon (top durana kadar)
        while (sqrt(vx * vx + vy * vy) > 1f && time < 10f) {
            time += dt
            
            // Yeni pozisyon
            x += vx * dt
            y += vy * dt
            
            // Duvar çarpışmaları
            val wallCollision = checkWallCollision(x, y, ball.radius, tableWidth, tableHeight)
            if (wallCollision != null) {
                when (wallCollision.type) {
                    WallType.LEFT, WallType.RIGHT -> vx = -vx * 0.8f // Yatay duvar
                    WallType.TOP, WallType.BOTTOM -> vy = -vy * 0.8f // Dikey duvar
                }
                x = wallCollision.x
                y = wallCollision.y
            }
            
            // Top çarpışmaları
            val ballCollision = checkBallCollision(
                Point(x, y),
                ball.radius,
                otherBalls,
                collisions.map { it.ballNumber }
            )
            
            if (ballCollision != null) {
                collisions.add(ballCollision)
                
                // Çarpışma sonrası hız hesaplama (basit fizik)
                val collisionAngle = atan2(
                    (ballCollision.point.y - y).toDouble(),
                    (ballCollision.point.x - x).toDouble()
                )
                
                val speed = sqrt(vx * vx + vy * vy)
                val newAngle = collisionAngle + Math.PI / 2
                
                vx = (speed * 0.7 * cos(newAngle)).toFloat()
                vy = (speed * 0.7 * sin(newAngle)).toFloat()
            }
            
            // Sürtünme
            vx *= friction
            vy *= friction
            
            path.add(Point(x, y))
        }
        
        return BallTrajectory(
            ballNumber = ball.number,
            path = path,
            finalPosition = Point(x, y),
            collisions = collisions
        )
    }

    /**
     * Çarpışan topları bul
     */
    private fun findHitBalls(
        whiteBallTrajectory: BallTrajectory,
        allBalls: List<BallDetector.Ball>
    ): List<BallDetector.Ball> {
        val hitBalls = mutableListOf<BallDetector.Ball>()
        
        whiteBallTrajectory.collisions.forEach { collision ->
            val hitBall = allBalls.find { it.number == collision.ballNumber }
            if (hitBall != null) {
                hitBalls.add(hitBall)
            }
        }
        
        return hitBalls
    }

    /**
     * Çarpışan topun yolunu hesapla
     */
    private fun calculateHitBallPath(
        hitBall: BallDetector.Ball,
        whiteBallTrajectory: BallTrajectory,
        allBalls: List<BallDetector.Ball>,
        tableWidth: Float,
        tableHeight: Float
    ): BallTrajectory {
        // Çarpışma noktasını bul
        val collision = whiteBallTrajectory.collisions.find { it.ballNumber == hitBall.number }
            ?: return BallTrajectory(
                hitBall.number,
                listOf(Point(hitBall.x, hitBall.y)),
                Point(hitBall.x, hitBall.y),
                emptyList()
            )
        
        // Çarpışma açısını hesapla
        val collisionPoint = collision.point
        val angle = atan2(
            (collisionPoint.y - hitBall.y).toDouble(),
            (collisionPoint.x - hitBall.x).toDouble()
        )
        
        // Çarpışma sonrası hız (beyaz topun hızının bir kısmı)
        val initialVelocity = 300f
        
        return calculateBallPath(
            hitBall,
            Math.toDegrees(angle).toFloat(),
            initialVelocity,
            allBalls.filter { it.number != hitBall.number },
            tableWidth,
            tableHeight
        )
    }

    /**
     * Duvar çarpışması kontrolü
     */
    private fun checkWallCollision(
        x: Float,
        y: Float,
        radius: Float,
        tableWidth: Float,
        tableHeight: Float
    ): WallCollision? {
        val margin = radius
        
        return when {
            x - radius < 0 -> WallCollision(WallType.LEFT, radius, y)
            x + radius > tableWidth -> WallCollision(WallType.RIGHT, tableWidth - radius, y)
            y - radius < 0 -> WallCollision(WallType.TOP, x, radius)
            y + radius > tableHeight -> WallCollision(WallType.BOTTOM, x, tableHeight - radius)
            else -> null
        }
    }

    /**
     * Top çarpışması kontrolü
     */
    private fun checkBallCollision(
        position: Point,
        radius: Float,
        otherBalls: List<BallDetector.Ball>,
        excludedBalls: List<Int>
    ): Collision? {
        for (ball in otherBalls) {
            if (excludedBalls.contains(ball.number)) continue
            
            val distance = sqrt(
                (position.x - ball.x).pow(2) + (position.y - ball.y).pow(2)
            )
            
            if (distance < radius + ball.radius) {
                return Collision(
                    ballNumber = ball.number,
                    point = Point(
                        (position.x + ball.x) / 2,
                        (position.y + ball.y) / 2
                    ),
                    time = 0f
                )
            }
        }
        
        return null
    }

    enum class WallType {
        LEFT, RIGHT, TOP, BOTTOM
    }

    data class WallCollision(
        val type: WallType,
        val x: Float,
        val y: Float
    )

    private const val TAG = "PhysicsCalculator"
}

