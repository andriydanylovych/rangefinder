package com.prostologik.lv12.ui.home

import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer

class MyImageAnalyzer {

    var setImageSize: Boolean = false

    fun analyze(image: ImageProxy): String {
        val buffer0 = image.planes[0].buffer
        val data = buffer0.toByteArray()
        val count = data.count()// 307200
        if (!setImageSize) {
            OverlayView.height = image.height // 640
            OverlayView.width = image.width // 480
            OverlayView.text = "data=$count"
            setImageSize = true
        }

        val startPx = 153832
        val sb: StringBuilder = StringBuilder("start=$startPx: ")
        var i = 0
        while (i < 16) {
            val d = byteToPixel(data[startPx + i])
            sb.append("$d.")
            i++
        }
        val viewBag = sb.toString()

        return viewBag
    }

    private fun byteToPixel(b: Byte): Int {
        return if (b < 0) (256 + b.toInt()) else b.toInt()
    }

    private fun ByteBuffer.toByteArray(): ByteArray {
        rewind()    // Rewind the buffer to zero
        val data = ByteArray(remaining())
        get(data)   // Copy the buffer into a byte array
        return data // Return the byte array
    }

}
