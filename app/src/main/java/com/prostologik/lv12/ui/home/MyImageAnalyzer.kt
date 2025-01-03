package com.prostologik.lv12.ui.home

import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer

class MyImageAnalyzer {

    private var setImageSize: Boolean = false
    private var imageHeight: Int = 480
    private var imageWidth: Int = 640

    fun analyze(image: ImageProxy): String {

        if (!setImageSize) { // landscape
            imageHeight = image.height
            imageWidth = image.width
            OverlayView.imageHeight = imageHeight
            OverlayView.imageWidth = imageWidth
            setImageSize = true
        }

        return "w $imageWidth x h $imageHeight"

    }

    private fun ByteBuffer.toByteArray(): ByteArray {
        rewind()    // Rewind the buffer to zero
        val data = ByteArray(remaining())
        get(data)   // Copy the buffer into a byte array [cannot be dropped!]
        return data // Return the byte array
    }

}
