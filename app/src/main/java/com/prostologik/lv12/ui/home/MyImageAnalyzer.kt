package com.prostologik.lv12.ui.home

import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer

class MyImageAnalyzer {

    private var setImageSize: Boolean = false

    fun analyze(image: ImageProxy): String {
        val buffer0 = image.planes[0].buffer
        val data = buffer0.toByteArray()
        //val count = data.count() // 307200
        if (!setImageSize) { // landscape
            OverlayView.height = image.height // 480
            OverlayView.width = image.width // 640
            setImageSize = true
        }

        val snippetSize = 64
        val step = 1
        val startPx = 307200 / 2 + 640 / 2 - snippetSize / 2 * step
        val sb: StringBuilder = StringBuilder("")
        var i = 0
        while (i < snippetSize) {
            val d = byteToPixel(data[startPx + i * step])
            sb.append("$d,")
            i++
        }

        return sb.toString()
    }

    private fun byteToPixel(b: Byte): Int {
        return if (b < 0) (256 + b.toInt()) else b.toInt()
    }

    private fun ByteBuffer.toByteArray(): ByteArray {
        rewind()    // Rewind the buffer to zero
        val data = ByteArray(remaining())
        get(data)   // Copy the buffer into a byte array [cannot be dropped!]
        return data // Return the byte array
    }

}
