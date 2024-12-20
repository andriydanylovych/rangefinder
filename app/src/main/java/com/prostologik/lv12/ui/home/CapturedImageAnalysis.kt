package com.prostologik.lv12.ui.home

import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer

class CapturedImageAnalysis {

    private var imageHeight: Int = 3264
    private var imageWidth: Int = 2448

    fun analyze(image: ImageProxy, snippetWidth: Int = 64, snippetHeight: Int = 64, snippetLayer: Int = 0): String {

        imageHeight = image.height
        imageWidth = image.width

        val sb: StringBuilder = StringBuilder("")

        val buffer = image.planes[0].buffer
        val data = buffer.toByteArray() // data.size = 2-3mm
        //val dataSize = data.size
        val dataCount = data.count()
        sb.append("dataCount=$dataCount:: ")
        var j = 200
        var notNull = 0

        while (notNull < 200 && j < dataCount) {
            val d = data[j].toInt()
            if (d != 0) {
                sb.append("$j=$d,")
                notNull++
            }
            j++
        }

        return sb.toString()
    }

    private fun ByteBuffer.toByteArray(): ByteArray {
        rewind()    // Rewind the buffer to zero
        val data = ByteArray(remaining())
        get(data)   // Copy the buffer into a byte array [cannot be dropped!]
        return data // Return the byte array
    }

}