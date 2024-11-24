package com.prostologik.lv12.ui.home

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class OverlayView : View {
    //public var height = 30;
    //public var width = 30

    private val textPaint = Paint().apply {
        color = Color.BLACK
        textSize = 50f
        isAntiAlias = true
    }
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
        var w = 0.5f * width
        var h = 0.5f * height
        var a = 32f
        canvas.drawRect(w - a, h - a,  w + a,h + a, myRedPaint)
        canvas.drawText(text, 60f, 160f, textPaint)
    }

    companion object {
        var height: Int = 30
        var width: Int = 30
        var text: String = "default text"
    }
}

// canvas.drawBitmap(bitmap, null, rect, paint)
// https://medium.com/over-engineering/getting-started-with-drawing-on-the-android-canvas-621cf512f4c7
