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
        val step = 1

        val sb: StringBuilder = StringBuilder("")

        val startRaw = (imageHeight - snippetHeight * step) / 2
        val startCol = (imageWidth - snippetWidth * step) / 2

        val bufferY = image.planes[0].buffer
        val dataY = bufferY.toByteArray() // dataY.count() => 307200
        var j = 0
        while (j < snippetHeight) {
            val startPx: Int = imageWidth * (startRaw + j * step) + startCol
            var i = 0
            while (i < snippetWidth - 1) {
                val d = (dataY[startPx + i * step])
                sb.append("$d,")
                i++
            }
            val dLast = (dataY[startPx + (snippetWidth - 1) * step])
            sb.append("$dLast\n")
            j++
        }

        if (snippetLayer > 0) {
            val bufferUV = image.planes[snippetLayer].buffer
            val dataUV = bufferUV.toByteArray() // dataUV.count() => 153599
            j = 0
            while (j < snippetHeight / 2) {
                val startPx: Int = imageWidth * (startRaw / 2 + j * step) + startCol
                val sbV: StringBuilder = StringBuilder("")
                var i = 0
                while (i < snippetWidth - 1) {
                    val d = (dataUV[startPx + i * step])
                    if (i % 2 == 0) sb.append("$d,")
                    else sbV.append("$d,")
                    i++
                }
                sb.append(sbV)
                val dLast = (dataUV[startPx + (snippetWidth - 1) * step])
                sb.append("$dLast\n")
                j++
            }

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
