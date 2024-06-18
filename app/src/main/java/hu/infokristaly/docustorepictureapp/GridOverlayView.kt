package hu.infokristaly.docustorepictureapp

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.SurfaceView

class GridOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : SurfaceView(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private val gridSize = 10 // Number of grid lines

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()

        val cellWidth = width / gridSize
        val cellHeight = height / gridSize

        // Draw vertical grid lines
        for (i in 0..gridSize) {
            val x = i * cellWidth
            canvas.drawLine(x, 0f, x, height, paint)
        }

        // Draw horizontal grid lines
        for (i in 0..gridSize) {
            val y = i * cellHeight
            canvas.drawLine(0f, y, width, y, paint)
        }
    }
}