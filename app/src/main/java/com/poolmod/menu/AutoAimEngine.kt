package com.poolmod.menu

import android.util.Log
import kotlin.math.*

/**
 * Otomatik nişan alma motoru
 * Delikleri hedefleyerek otomatik aim yapar
 */
object AutoAimEngine {

    data class AimTarget(
        val targetHole: HoleDetector.Hole,
        val aimAngle: Float, // Derece
        val aimPower: Float, // 0-1
        val trajectory: List<PhysicsCalculator.Point>
    )

    /**
     * Beyaz top için en iyi hedefi hesapla
     */
    fun calculateBestAim(
        whiteBall: BallDetector.Ball,
        targetBalls: List<BallDetector.Ball>,
        allBalls: List<BallDetector.Ball>,
        holes: List<HoleDetector.Hole>,
        tableWidth: Float,
        tableHeight: Float
    ): AimTarget? {
        if (holes.isEmpty() || targetBalls.isEmpty()) return null

        try {
            var bestTarget: AimTarget? = null
            var bestScore = 0f

            // Her hedef top için en iyi deliği bul
            targetBalls.forEach { targetBall ->
                val bestHole = HoleDetector.findBestHoleForBall(
                    targetBall,
                    allBalls,
                    holes,
                    tableWidth,
                    tableHeight
                )

                if (bestHole != null) {
                    val aimTarget = calculateAimForTarget(
                        whiteBall,
                        targetBall,
                        bestHole,
                        allBalls,
                        tableWidth,
                        tableHeight
                    )

                    if (aimTarget != null) {
                        val score = calculateAimScore(aimTarget, whiteBall, targetBall, bestHole)
                        if (score > bestScore) {
                            bestScore = score
                            bestTarget = aimTarget
                        }
                    }
                }
            }

            return bestTarget
        } catch (e: Exception) {
            Log.e(TAG, "Aim hesaplama hatası: ${e.message}", e)
            return null
        }
    }

    /**
     * Belirli bir hedef için aim hesapla
     */
    private fun calculateAimForTarget(
        whiteBall: BallDetector.Ball,
        targetBall: BallDetector.Ball,
        targetHole: HoleDetector.Hole,
        allBalls: List<BallDetector.Ball>,
        tableWidth: Float,
        tableHeight: Float
    ): AimTarget? {
        try {
            // Hedef topun deliğe gitmesi için gerekli açı
            val targetToHoleAngle = Math.atan2(
                (targetHole.y - targetBall.y).toDouble(),
                (targetHole.x - targetBall.x).toDouble()
            )

            // Beyaz topun hedef topa çarpması için gerekli açı
            // Hedef topun arkasından vurmalıyız (cut angle)
            val cutAngle = calculateCutAngle(whiteBall, targetBall, targetToHoleAngle)

            // Aim açısı
            val aimAngle = Math.toDegrees(cutAngle).toFloat()

            // Güç hesaplama (mesafeye göre)
            val distance = sqrt(
                (whiteBall.x - targetBall.x).pow(2) + (whiteBall.y - targetBall.y).pow(2)
            )
            val maxDistance = sqrt(tableWidth * tableWidth + tableHeight * tableHeight)
            val aimPower = (distance / maxDistance * 0.8f + 0.2f).coerceIn(0.3f, 1f)

            // Trajectory hesapla
            val trajectory = calculateTrajectory(
                whiteBall,
                aimAngle,
                aimPower,
                allBalls,
                tableWidth,
                tableHeight
            )

            return AimTarget(
                targetHole = targetHole,
                aimAngle = aimAngle,
                aimPower = aimPower,
                trajectory = trajectory
            )
        } catch (e: Exception) {
            Log.e(TAG, "Aim hesaplama hatası: ${e.message}", e)
            return null
        }
    }

    /**
     * Cut angle hesapla (beyaz topun hedef topa çarpması için açı)
     */
    private fun calculateCutAngle(
        whiteBall: BallDetector.Ball,
        targetBall: BallDetector.Ball,
        targetToHoleAngle: Double
    ): Double {
        // Beyaz top ve hedef top arasındaki açı
        val whiteToTargetAngle = Math.atan2(
            (targetBall.y - whiteBall.y).toDouble(),
            (targetBall.x - whiteBall.x).toDouble()
        )

        // Cut angle = target to hole angle - white to target angle
        // Ama biraz offset eklemeliyiz (top çarpışması için)
        val cutAngle = targetToHoleAngle - whiteToTargetAngle

        // Normalize açı (-180 ile 180 arası)
        return normalizeAngle(cutAngle)
    }

    /**
     * Açıyı normalize et
     */
    private fun normalizeAngle(angle: Double): Double {
        var normalized = angle
        while (normalized > Math.PI) normalized -= 2 * Math.PI
        while (normalized < -Math.PI) normalized += 2 * Math.PI
        return normalized
    }

    /**
     * Trajectory hesapla
     */
    private fun calculateTrajectory(
        whiteBall: BallDetector.Ball,
        aimAngle: Float,
        aimPower: Float,
        allBalls: List<BallDetector.Ball>,
        tableWidth: Float,
        tableHeight: Float
    ): List<PhysicsCalculator.Point> {
        val trajectory = PhysicsCalculator.calculateTrajectories(
            whiteBall = whiteBall,
            cueDirection = aimAngle,
            cuePower = aimPower,
            allBalls = allBalls.filter { it.number != 0 },
            tableWidth = tableWidth,
            tableHeight = tableHeight
        )

        return trajectory.firstOrNull()?.path ?: emptyList()
    }

    /**
     * Aim skorunu hesapla
     */
    private fun calculateAimScore(
        aimTarget: AimTarget,
        whiteBall: BallDetector.Ball,
        targetBall: BallDetector.Ball,
        targetHole: HoleDetector.Hole
    ): Float {
        // Mesafe skoru
        val whiteToTargetDistance = sqrt(
            (whiteBall.x - targetBall.x).pow(2) + (whiteBall.y - targetBall.y).pow(2)
        )
        val targetToHoleDistance = sqrt(
            (targetBall.x - targetHole.x).pow(2) + (targetBall.y - targetHole.y).pow(2)
        )
        val totalDistance = whiteToTargetDistance + targetToHoleDistance

        // Kısa mesafe daha iyi
        val distanceScore = 1f / (1f + totalDistance / 1000f)

        // Trajectory uzunluğu (kısa trajectory daha iyi)
        val trajectoryLength = aimTarget.trajectory.size
        val trajectoryScore = 1f / (1f + trajectoryLength / 100f)

        // Güç skoru (orta güç daha iyi)
        val powerScore = 1f - abs(aimTarget.aimPower - 0.7f) * 2f

        return distanceScore * 0.4f + trajectoryScore * 0.3f + powerScore * 0.3f
    }

    /**
     * Aim bilgisini al (açı ve güç)
     */
    fun getAimInfo(aimTarget: AimTarget?): String {
        return if (aimTarget != null) {
            "Açı: ${aimTarget.aimAngle.toInt()}° | Güç: ${(aimTarget.aimPower * 100).toInt()}%"
        } else {
            "Hedef bulunamadı"
        }
    }

    private const val TAG = "AutoAimEngine"
}

