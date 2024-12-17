package com.prostologik.lv12.ui.home

import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer

class MyImageAnalyzer {

    private var setImageSize: Boolean = false
    private var imageHeight: Int = 480
    private var imageWidth: Int = 640

    fun analyze(image: ImageProxy, snippetWidth: Int = 64, snippetHeight: Int = 64, snippetLayer: Int = 0): String {

        if (!setImageSize) { // landscape
            imageHeight = image.height
            imageWidth = image.width
            OverlayView.imageHeight = imageHeight
            OverlayView.imageWidth = imageWidth
            setImageSize = true
        }

        var uv = 1
        if (snippetLayer > 0) uv = 2

        val buffer0 = image.planes[snippetLayer].buffer
        val data = buffer0.toByteArray() // data.count() => 307200, 153599

        val step = 1

        val sb: StringBuilder = StringBuilder("")

        var j = 0
        val startRaw = (imageHeight - snippetHeight * step) / 2 / uv
        val startCol = (imageWidth - snippetWidth * step) / 2
        while (j < snippetHeight / uv) {
            val startPx: Int = imageWidth * (startRaw + j * step) + startCol
            var i = 0
            while (i < snippetWidth - 1) {
                val d = byteToPixel(data[startPx + i * step])
                sb.append("$d,")
                i += uv//i++
            }
            val dLast = byteToPixel(data[startPx + (snippetWidth - 1) * step])
            sb.append("$dLast\n")
            j++
        }

        return sb.toString()

//        val r = d0 + (1.370705 * d2);
//        val g = d0 - (0.698001 * d2) - (0.337633 * d1);
//        val b = d0 + (1.732446 * d1);
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
