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

        val SNIPPET_SIZE = 16
        val STEP = 20
        val startPx = 307200 / 2 + 640 / 2 - SNIPPET_SIZE / 2 * STEP // 153760
        val sb: StringBuilder = StringBuilder(">") // "start=$startPx: "
        var i = 0
        while (i < SNIPPET_SIZE) {
            val d = byteToPixel(data[startPx + i * STEP])
            sb.append("$d.")
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
