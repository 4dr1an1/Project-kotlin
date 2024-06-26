package com.example.licznikkrokow

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

class PieChartView : View {
    private var paint: Paint? = null
    private var steps = 0f
    private var goal = 0f
    private var textPaint: Paint? = null

    constructor(context: Context?) : super(context) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init()
    }

    private fun init() {
        paint = Paint()
        paint!!.isAntiAlias = true
        paint!!.style = Paint.Style.FILL
        steps = 0f
        goal = 5000f

        textPaint = Paint()
        textPaint!!.isAntiAlias = true
        textPaint!!.color = -0x7800
        textPaint!!.textAlign = Paint.Align.CENTER
        textPaint!!.textSize = 60f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val width = width.toFloat()
        val height = height.toFloat()
        val radius = (min(width.toDouble(), height.toDouble()) / 2).toFloat()

        paint!!.color = -0x333334
        canvas.drawCircle(width / 2, height / 2, radius, paint!!)

        if (steps > 0) {
            paint!!.color = -0x7800
            val sweepAngle = (steps / goal) * 360
            canvas.drawArc(0f, 0f, width, height, -90f, sweepAngle, true, paint!!)
        }

        canvas.drawText(
            "${steps.toInt()} / ${goal.toInt()}",
            width / 2,
            height / 2 - (textPaint!!.descent() + textPaint!!.ascent()) / 2,
            textPaint!!
        )
    }

}
