package com.prostologik.lv12.ui.dataset

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
        myRedPaint = Paint().apply {
            color = Color.RED;
            strokeWidth = 3.0f;
            style = Paint.Style.STROKE;
            textSize = 40F
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        //val scaleFactor = height.toFloat() / imageWidth // width.toFloat() / imageHeight
        val scaleFactor = height.toFloat() / snippetWidth


            val sh = 280//snippetWidth * scaleFactor
            val sw = 280//snippetHeight * scaleFactor

            val s = patchSize * scaleFactor
            canvas.drawRect(
                0.5f * (width - s),
                0.5f * (height - s),
                0.5f * (width + s),
                0.5f * (height + s),
                myRedPaint
            )


    }

//    override fun onTouchEvent( event: MotionEvent): Boolean {
//        return true
//    }

    companion object {
        var analyzerOption = 0

        // var viewHeight = 1680
        // var viewWidth = 1080

        var imageWidth: Int = 640
        var imageHeight: Int = 480

        var patchSize: Int = 28

        var snippetWidth: Int = 64
        var snippetHeight: Int = 64
    }
}