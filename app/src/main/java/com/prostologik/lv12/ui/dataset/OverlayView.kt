package com.prostologik.lv12.ui.dataset

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View


class OverlayView : View {

    private lateinit var myRedPaint: Paint
    constructor(c: Context?) : super(c) {
        init()
    }
    constructor(c: Context?, attr: AttributeSet?) : super(c, attr) {
        init()
    }
    constructor(c: Context?, attr: AttributeSet?, defStyle: Int) : super(c, attr, defStyle) {
        init()
    }
    private fun init() {
        myRedPaint = Paint().apply {
            color = Color.RED
            strokeWidth = 3.0f
            style = Paint.Style.STROKE
            textSize = 40F
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

            val s = patchSize * scaleFactor

            if (clickX < 0) {
                clickX = 0.5f * width
                clickY = 0.5f * height
            }

            if (patchSize > 1) {
                canvas.drawRect(
                    clickX - s / 2,
                    clickY - s / 2,
                    clickX + s / 2,
                    clickY + s / 2,
                    myRedPaint
                )
            } else {
                canvas.drawRect(
                    clickX,
                    clickY,
                    clickX + s,
                    clickY + s,
                    myRedPaint
                )
            }


    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        super.onTouchEvent(event)

        fun roundToScale(f: Float, scale: Int): Float {
            return ((f / scale).toInt() * scale).toFloat()
        }

        when (event.action) {

            MotionEvent.ACTION_DOWN -> return true

            MotionEvent.ACTION_UP -> {
                var x = event.x
                var y = event.y

                val s = ((patchSize * scaleFactor) / 2).toFloat()

                if (x < s) x = s
                if (x > width - s) x = width - s
                if (y < s) y = s
                if (y > height - s) y = height - s
                clickX = roundToScale(x, scaleFactor)
                clickY = roundToScale(y, scaleFactor)

                super.invalidate()

                performClick()

                return true
            }
        }
        return false
    }

    // Because we call this from onTouchEvent, this code will be executed for both
    // normal touch events and for when the system calls this using Accessibility
    override fun performClick(): Boolean {
        super.performClick()
        //doSomething()
        return true
    }

//    private fun doSomething() {
//        Toast.makeText(context, "x=$clickX y=$clickY", Toast.LENGTH_SHORT).show()
//    }


    companion object {
        var patchSize: Int = 28
        var scaleFactor = 8

        var clickX: Float = -1f
        var clickY: Float = 0f
    }
}