package com.prostologik.lv12.ui.home

import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer

class MyImageAnalyzer {

    //lateinit var _image: ImageProxy
    var setImageSize: Boolean = false;

    fun analyze(image: ImageProxy): String {
        val buffer = image.planes[0].buffer
        val data = buffer.toByteArray()
        val pixels = data.map { it.toInt() and 0xFF }
        val luma = pixels.average()
        val xx = pixels.get(0)
        val count = pixels.count()
        val rotation = image.imageInfo.rotationDegrees
        if (!setImageSize) {
            OverlayView.height = image.height //image.getHeight()
            OverlayView.width = image.width //image.getWidth()
            setImageSize = true;
        }
        val s = String.format("%.1f", luma)
        val viewBag = "luma=$s; count=$count; px(0)=$xx; rot=$rotation" //h=$height; w=$width
        OverlayView.text = viewBag
        return viewBag
    }

    private fun ByteBuffer.toByteArray(): ByteArray {
        rewind()    // Rewind the buffer to zero
        val data = ByteArray(remaining())
        get(data)   // Copy the buffer into a byte array
        return data // Return the byte array
    }

}
