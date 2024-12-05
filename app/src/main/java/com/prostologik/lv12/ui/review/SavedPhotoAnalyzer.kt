package com.prostologik.lv12.ui.review

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.get
import androidx.core.net.toFile

class SavedPhotoAnalyzer {

    @RequiresApi(Build.VERSION_CODES.P)
    public fun analyze(uri: Uri): String {
        val source: ImageDecoder.Source = ImageDecoder.createSource(uri.toFile())
        val drawable = ImageDecoder.decodeDrawable(source)
        val bm: Bitmap = drawable.toBitmap()
        val w = bm.width
        val h = bm.height
        val size = "$w x $h"
        //val bm00 = bm[0, 0]
        // channel '49c68f com.prostologik.lv12/com.prostologik.lv12.MainActivity (server)' ~ Channel is unrecoverably broken and will be disposed!

        return "source: $source size: $size bm00: 0"
    }
}

//        val source: ImageDecoder.Source = ImageDecoder.createSource(uri.toFile())
//        val drawable = ImageDecoder.decodeDrawable(source)
//        imageView.setImageDrawable(drawable)

//        val bm: Bitmap = drawable.toBitmap()
//        val w = bm.width
//        val h = bm.height
//        val size = "$w x $h"
//        val colorint: IntArray = intArrayOf(0)

//        val pixels = IntArray(w * h)
//        // Get the pixels from the bitmap
//        bm.getPixels(pixels, 0, w, 0, 0, w, h)
//        // Process the pixel data (e.g., print the first pixel value)
//        val firstPixel = pixels[0]

//        try {
//            val pixel = bm.getPixels(colorint, 0, 1, 0, 0, 1, 1) //bm.getPixel(0, 0)
//            displaytext = pixel.toString()
//        } catch (e: ArithmeticException) {
//            displaytext = source.toString()
//        }
//displaytext = bm[0, 0].toString()
//px = bm[0, 0]
//val savedPhotoAnalyzer = SavedPhotoAnalyzer()