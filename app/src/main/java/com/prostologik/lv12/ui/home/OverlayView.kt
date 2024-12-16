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
        //val scaleFactor = height / imageWidth
        val scaleFactor = width.toFloat() / imageHeight
        val sh = snippetWidth * scaleFactor
        val sw = snippetHeight * scaleFactor
        canvas.drawRect(
            0.5f * (width - sw),
            0.5f * (height - sh),
            0.5f * (width + sw),
            0.5f * (height + sh),
            myRedPaint
        )

        myRedPaint.setTextSize(40F);
        canvas.drawText("$height x $width", 100F, 150F, myRedPaint);
    }

    companion object {
        // height = 1680 // viewHeight
        // width = 1080 // viewWidth
        var imageWidth: Int = 640
        var imageHeight: Int = 480
        var snippetWidth: Int = 64
        var snippetHeight: Int = 64
    }
}
