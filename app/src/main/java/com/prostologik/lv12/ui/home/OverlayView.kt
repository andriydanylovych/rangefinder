package com.prostologik.lv12.ui.home

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
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
        myRedPaint = Paint().apply { color = Color.RED; strokeWidth = 3.0f; style = Paint.Style.STROKE }
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = 0.5f * width
        val h = 0.5f * height
        val a = 32f
        canvas.drawRect(w - a, h - a,  w + a,h + a, myRedPaint)
    }

    companion object {
        var height: Int = 30
        var width: Int = 30
    }
}

// canvas.drawBitmap(bitmap, null, rect, paint)
// https://medium.com/over-engineering/getting-started-with-drawing-on-the-android-canvas-621cf512f4c7
