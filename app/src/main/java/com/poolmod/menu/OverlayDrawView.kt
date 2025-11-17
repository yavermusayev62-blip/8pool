package com.poolmod.menu

import android.content.Context
import android.graphics.*
import android.view.View
import android.util.Log

/**
 * Top yollarını çizen overlay view
 * Her top için çizgi ve numara gösterir
 */
class OverlayDrawView(context: Context) : View(context) {

    private val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val textPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        textSize = 24f
        typeface = Typeface.DEFAULT_BOLD
        color = Color.WHITE
    }

    private val ballPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    
    // Geçici paint'ler için
    private val tempPaint = Paint()

    private var trajectories: List<PhysicsCalculator.BallTrajectory> = emptyList()
    private var balls: List<BallDetector.Ball> = emptyList()
    private var tableBounds: BallDetector.TableBounds? = null
    private var holes: List<HoleDetector.Hole> = emptyList()
    private var autoAimTarget: AutoAimEngine.AimTarget? = null
    private var isAutoAimEnabled = false

    // Her top için farklı renk
    private val ballColors = mapOf(
        0 to Color.WHITE,      // Beyaz top
        -1 to Color.BLACK,     // Siyah top
        1 to Color.RED,        // Kırmızı
        2 to Color.parseColor("#FFA500"), // Turuncu
        3 to Color.YELLOW,     // Sarı
        4 to Color.GREEN,      // Yeşil
        5 to Color.CYAN,       // Cyan
        6 to Color.BLUE,       // Mavi
        7 to Color.MAGENTA,    // Mor
        8 to Color.parseColor("#8B4513"), // Kahverengi
        9 to Color.parseColor("#FF1493"), // Pembe
        10 to Color.parseColor("#00CED1"), // Koyu turkuaz
        11 to Color.parseColor("#FF4500"), // Turuncu-kırmızı
        12 to Color.parseColor("#4169E1"), // Kraliyet mavisi
        13 to Color.parseColor("#FF6347"), // Domates kırmızısı
        14 to Color.parseColor("#32CD32"), // Açık yeşil
        15 to Color.parseColor("#FFD700")  // Altın
    )

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (trajectories.isEmpty() && balls.isEmpty()) {
            return
        }

        // Masa sınırlarını çiz
        tableBounds?.let { bounds ->
            val tablePaint = Paint().apply {
                color = Color.parseColor("#3300FF00")
                style = Paint.Style.STROKE
                strokeWidth = 2f
            }
            canvas.drawRect(
                bounds.left.toFloat(),
                bounds.top.toFloat(),
                bounds.right.toFloat(),
                bounds.bottom.toFloat(),
                tablePaint
            )
        }

        // Delikleri çiz
        holes.forEach { hole ->
            val holePaint = Paint().apply {
                color = Color.parseColor("#80000000")
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            val holeStrokePaint = Paint().apply {
                color = Color.YELLOW
                style = Paint.Style.STROKE
                strokeWidth = 3f
                isAntiAlias = true
            }
            
            // Delik çemberi
            canvas.drawCircle(hole.x, hole.y, hole.radius, holePaint)
            canvas.drawCircle(hole.x, hole.y, hole.radius, holeStrokePaint)
            
            // Delik numarası
            val holeText = hole.number.toString()
            val textBounds = Rect()
            textPaint.getTextBounds(holeText, 0, holeText.length, textBounds)
            val textX = hole.x - textBounds.width() / 2f
            val textY = hole.y + textBounds.height() / 2f
            
            // Arka plan
            val holeBgPaint = Paint().apply {
                color = Color.parseColor("#CC000000")
                style = Paint.Style.FILL
            }
            canvas.drawRect(
                textX - 5,
                textY - textBounds.height() - 5,
                textX + textBounds.width() + 5,
                textY + 5,
                holeBgPaint
            )
            
            textPaint.color = Color.YELLOW
            canvas.drawText(holeText, textX, textY, textPaint)
        }

        // Auto Aim hedefini çiz
        if (isAutoAimEnabled && autoAimTarget != null) {
            val aimTarget = autoAimTarget!!
            
            // Hedef delik vurgusu
            val targetHolePaint = Paint().apply {
                color = Color.parseColor("#80FF0000")
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            val targetHoleStrokePaint = Paint().apply {
                color = Color.RED
                style = Paint.Style.STROKE
                strokeWidth = 5f
                isAntiAlias = true
            }
            
            canvas.drawCircle(
                aimTarget.targetHole.x,
                aimTarget.targetHole.y,
                aimTarget.targetHole.radius * 1.5f,
                targetHolePaint
            )
            canvas.drawCircle(
                aimTarget.targetHole.x,
                aimTarget.targetHole.y,
                aimTarget.targetHole.radius * 1.5f,
                targetHoleStrokePaint
            )
            
            // Aim çizgisi (beyaz top'tan hedefe)
            val whiteBall = balls.find { it.number == 0 }
            if (whiteBall != null && aimTarget.trajectory.isNotEmpty()) {
                val aimLinePaint = Paint().apply {
                    color = Color.parseColor("#FFFF00")
                    style = Paint.Style.STROKE
                    strokeWidth = 5f
                    isAntiAlias = true
                    pathEffect = android.graphics.DashPathEffect(floatArrayOf(20f, 10f), 0f)
                }
                
                val aimPath = Path()
                aimPath.moveTo(whiteBall.x, whiteBall.y)
                
                // Trajectory'nin ilk birkaç noktasını çiz
                val maxPoints = minOf(aimTarget.trajectory.size, 50)
                for (i in 0 until maxPoints) {
                    aimPath.lineTo(
                        aimTarget.trajectory[i].x,
                        aimTarget.trajectory[i].y
                    )
                }
                
                canvas.drawPath(aimPath, aimLinePaint)
                
                // Aim bilgisi
                val aimInfo = AutoAimEngine.getAimInfo(aimTarget)
                val infoTextBounds = Rect()
                textPaint.getTextBounds(aimInfo, 0, aimInfo.length, infoTextBounds)
                val infoX = whiteBall.x - infoTextBounds.width() / 2f
                val infoY = whiteBall.y - 40f
                
                val infoBgPaint = Paint().apply {
                    color = Color.parseColor("#CC000000")
                    style = Paint.Style.FILL
                }
                canvas.drawRect(
                    infoX - 10,
                    infoY - infoTextBounds.height() - 10,
                    infoX + infoTextBounds.width() + 10,
                    infoY + 10,
                    infoBgPaint
                )
                
                textPaint.color = Color.YELLOW
                textPaint.textSize = 18f
                canvas.drawText(aimInfo, infoX, infoY, textPaint)
                textPaint.textSize = 24f // Geri al
            }
        }

        // Topları çiz
        balls.forEach { ball ->
            val color = ballColors[ball.number] ?: Color.GRAY
            ballPaint.color = color
            
            // Top çemberi
            canvas.drawCircle(ball.x, ball.y, ball.radius, ballPaint)
            
            // Top numarası
            val numberText = when (ball.number) {
                0 -> "W" // White
                -1 -> "B" // Black
                else -> ball.number.toString()
            }
            
            // Numarayı ortala
            val textBounds = Rect()
            textPaint.getTextBounds(numberText, 0, numberText.length, textBounds)
            val textX = ball.x - textBounds.width() / 2f
            val textY = ball.y + textBounds.height() / 2f
            
            // Arka plan (okunabilirlik için)
            val ballBgPaint = Paint().apply {
                this.color = Color.parseColor("#80000000")
                style = Paint.Style.FILL
            }
            canvas.drawRect(
                textX - 5,
                textY - textBounds.height() - 5,
                textX + textBounds.width() + 5,
                textY + 5,
                ballBgPaint
            )
            
            textPaint.color = if (ball.number == -1) Color.WHITE else Color.BLACK
            canvas.drawText(numberText, textX, textY, textPaint)
        }

        // Top yollarını çiz
        trajectories.forEach { trajectory ->
            val color = ballColors[trajectory.ballNumber] ?: Color.WHITE
            paint.color = color
            paint.alpha = 200
            
            // Yolu çiz
            if (trajectory.path.size > 1) {
                val path = Path()
                path.moveTo(trajectory.path[0].x, trajectory.path[0].y)
                
                for (i in 1 until trajectory.path.size) {
                    path.lineTo(trajectory.path[i].x, trajectory.path[i].y)
                }
                
                canvas.drawPath(path, paint)
            }
            
            // Başlangıç noktası
            paint.style = Paint.Style.FILL
            paint.alpha = 255
            canvas.drawCircle(
                trajectory.path.first().x,
                trajectory.path.first().y,
                8f,
                paint
            )
            
            // Bitiş noktası (duracağı yer)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 3f
            paint.alpha = 255
            canvas.drawCircle(
                trajectory.finalPosition.x,
                trajectory.finalPosition.y,
                12f,
                paint
            )
            
            // Top numarasını bitiş noktasına yaz
            val numberText = when (trajectory.ballNumber) {
                0 -> "W"
                -1 -> "B"
                else -> trajectory.ballNumber.toString()
            }
            
            val textBounds = Rect()
            textPaint.getTextBounds(numberText, 0, numberText.length, textBounds)
            val textX = trajectory.finalPosition.x - textBounds.width() / 2f
            val textY = trajectory.finalPosition.y - 20f
            
            // Arka plan
            val trajectoryBgPaint = Paint().apply {
                this.color = Color.parseColor("#CC000000")
                style = Paint.Style.FILL
            }
            canvas.drawRect(
                textX - 8,
                textY - textBounds.height() - 8,
                textX + textBounds.width() + 8,
                textY + 8,
                trajectoryBgPaint
            )
            
            textPaint.color = color
            canvas.drawText(numberText, textX, textY, textPaint)
            
            // Çarpışma noktalarını işaretle
            trajectory.collisions.forEach { collision ->
                val collisionPaint = Paint().apply {
                    this.color = Color.YELLOW
                    style = Paint.Style.FILL
                    alpha = 200
                }
                canvas.drawCircle(
                    collision.point.x,
                    collision.point.y,
                    6f,
                    collisionPaint
                )
            }
            
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 4f
        }
    }

    /**
     * Trajectory'leri güncelle ve yeniden çiz
     */
    fun updateTrajectories(
        newTrajectories: List<PhysicsCalculator.BallTrajectory>,
        newBalls: List<BallDetector.Ball>,
        newTableBounds: BallDetector.TableBounds?,
        newHoles: List<HoleDetector.Hole> = emptyList(),
        newAutoAimTarget: AutoAimEngine.AimTarget? = null,
        autoAimEnabled: Boolean = false
    ) {
        this.trajectories = newTrajectories
        this.balls = newBalls
        this.tableBounds = newTableBounds
        this.holes = newHoles
        this.autoAimTarget = newAutoAimTarget
        this.isAutoAimEnabled = autoAimEnabled
        invalidate()
    }

    /**
     * Temizle
     */
    fun clear() {
        trajectories = emptyList()
        balls = emptyList()
        tableBounds = null
        holes = emptyList()
        autoAimTarget = null
        isAutoAimEnabled = false
        invalidate()
    }
}

