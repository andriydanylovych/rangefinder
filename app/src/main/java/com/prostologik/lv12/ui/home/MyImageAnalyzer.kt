package com.prostologik.lv12.ui.home

import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer

class MyImageAnalyzer {

    private var setImageSize: Boolean = false
    private var imageHeight: Int = 480
    private var imageWidth: Int = 640

    fun analyze(image: ImageProxy, snippetWidth: Int = 64, snippetHeight: Int = 1, snippetLayer: Int = 0): String {
        val buffer0 = image.planes[0].buffer // snippetLayer
        val data = buffer0.toByteArray()
        //val count = data.count() // 307200
        if (!setImageSize) { // landscape
            imageHeight = image.height
            imageWidth = image.width
            OverlayView.height = imageHeight
            OverlayView.width = imageWidth
            setImageSize = true
        }

        val step = 1
        //        val t1: String = "size=" + image.planes.size + "::"

        val sb: StringBuilder = StringBuilder("")

        var j = 0
        while (j < snippetHeight) {
            val startPx: Int = imageWidth * (imageHeight - j * step) / 2 + imageWidth / 2 - snippetWidth / 2 * step
            var i = 0
            while (i < snippetWidth - 1) {
                val d = byteToPixel(data[startPx + i * step])
                sb.append("$d,")
                i++
            }
            val dLast = byteToPixel(data[startPx + (snippetWidth - 1) * step])
            sb.append("$dLast\n")
            //sb.append("\n")
            j++
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
