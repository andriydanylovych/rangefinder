package com.prostologik.lv12.ui.home

import androidx.camera.core.ImageProxy
import com.prostologik.lv12.Util.byteToPixel
import java.nio.ByteBuffer

class MyImageAnalyzer {

    fun analyze(image: ImageProxy): String {

        val imageWidth = image.width
        val imageHeight = image.height

        val startRaw = imageHeight / 2
        val startCol = imageWidth / 2

        val bufferY = image.planes[0].buffer
        val dataY = bufferY.toByteArray()

        val centralPx: Int = imageWidth * startRaw + startCol

        val d = byteToPixel(dataY[centralPx])

//        val info = image.imageInfo.toString()
//        val format = image.format.toString()
//
//        //image.setCropRect(Rect(1, 1, 959, 719))
//        val cropRect = image.cropRect.toString()

        //return "central px = $d\nw $imageWidth x h $imageHeight\n\ninfo: $info\n\nformat: $format\n\ncropRect: $cropRect"
        return "central px = $d"
    }

    private fun ByteBuffer.toByteArray(): ByteArray {
        rewind()    // Rewind the buffer to zero
        val data = ByteArray(remaining())
        get(data)   // Copy the buffer into a byte array [cannot be dropped!]
        return data // Return the byte array
    }

}
