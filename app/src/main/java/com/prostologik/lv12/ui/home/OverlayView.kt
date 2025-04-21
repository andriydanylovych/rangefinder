package com.prostologik.lv12.ui.home

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
        myRedPaint = Paint().apply { color = Color.RED; strokeWidth = 3.0f; style = Paint.Style.STROKE }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val scaleFactor = height.toFloat() / imageWidth // width.toFloat() / imageHeight

        if (analyzerOption == 0) { // Analyzer
            val sh = patchWidth * scaleFactor
            val sw = patchHeight * scaleFactor
            //val myRedPaint = Paint().apply { color = Color.RED; strokeWidth = 3.0f; style = Paint.Style.STROKE }
            canvas.drawRect(
                0.5f * (width - sw),
                0.5f * (height - sh),
                0.5f * (width + sw),
                0.5f * (height + sh),
                myRedPaint
            )
            myRedPaint.textSize = 40F
            val textToDraw = "h$height x w$width : image $imageWidth x $imageHeight : patch $patchWidth x $patchHeight"
            canvas.drawText(textToDraw, 100F, 150F, myRedPaint)
        } else { // Snippet (analyzerOption == 0)
            val sh = snippetWidth * scaleFactor
            val sw = snippetHeight * scaleFactor
            canvas.drawRect(
                0.5f * (width - sw),
                0.5f * (height - sh),
                0.5f * (width + sw),
                0.5f * (height + sh),
                myRedPaint
            )
        }

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

        var snippetWidth: Int = 64
        var snippetHeight: Int = 64

        var patchWidth: Int = 28
        var patchHeight: Int = 28
    }
}
