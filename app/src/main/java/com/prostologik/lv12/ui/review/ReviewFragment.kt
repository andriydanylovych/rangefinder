package com.prostologik.lv12.ui.review

import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.prostologik.lv12.Util
import com.prostologik.lv12.databinding.FragmentReviewBinding
import com.prostologik.lv12.ui.home.HomeViewModel
import java.io.File
import java.io.IOException

class ReviewFragment : Fragment() {

    private var _binding: FragmentReviewBinding? = null

    private val binding get() = _binding!!

    private var photoDirectory: String = "/storage/emulated/0/Android/media/com.prostologik.lv12/image"
    private var fileName: String = "default"

    private val homeViewModel: HomeViewModel by activityViewModels()

    private lateinit var mImageView: ImageView
    private lateinit var bitmap: Bitmap
    //private lateinit var canvas: Canvas
    //private lateinit var paint: Paint

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        homeViewModel.photoFileName.observe(viewLifecycleOwner) {
            val temp: String = it ?: ""
            if (temp != "") {
                fileName = temp
                renderPhoto(photoDirectory, fileName)
            }
        }

        _binding = FragmentReviewBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val btnNext = binding.nextButton
        btnNext.setOnClickListener {
            fileName = getNextFileName(fileName, photoDirectory)
            renderPhoto(photoDirectory, fileName)
        }

        val btnDelete = binding.deleteButton
        btnDelete.setOnClickListener { deletePhoto() }

        val btnSave = binding.saveButton
        btnSave.setOnClickListener { processPhoto() }

        mImageView = binding.imageSnippet //findViewById(R.id.imageSnippet)
//        bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
//        canvas = Canvas(bitmap)
//        mImageView.setImageBitmap(bitmap)

        fileName = getNextFileName(fileName, photoDirectory)
        renderPhoto(photoDirectory, fileName)

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun renderPhoto(dir: String, file: String) {

        val uri = Uri.parse("file://$dir/$file.jpg")
        // uri.toString() --> "file:///storage/emulated/0/Android/media/com.prostologik.lv12/image"

        val imageView: ImageView = binding.imageView
        imageView.setImageURI(uri)

        val linesOfPixels = mutableListOf<String>()
        val step = 6 // to set size of the snippet

        try {
            val fileCsv = File("$dir/$file.csv")

            if(fileCsv.exists()) {
                fileCsv.forEachLine { line ->
                    linesOfPixels.add(line)
                }
            }
        } catch (_: IOException) {}

        // to get snippet dimensions
        val firstLine = linesOfPixels[0].split(",")
        val snippetY = firstLine.size
        val snippetX = linesOfPixels.size
        val snippetUV = snippetX - snippetY / 2

        bitmap = Bitmap.createBitmap(snippetX * step, snippetY * step, Bitmap.Config.ARGB_8888)
        var schemaBlueRed = false
        var layer = 0
        for ((x, line) in linesOfPixels.withIndex()) {
            val pixels = line.split(",")

            if (x == snippetUV) {
                val y2 = Util.stringByteToPixel(pixels[snippetY / 2 - 2])
                val y0 = Util.stringByteToPixel(pixels[snippetY / 2 - 1])
                val y1 = Util.stringByteToPixel(pixels[snippetY / 2])
                val y3 = Util.stringByteToPixel(pixels[snippetY / 2 + 1])
                val d01 = y0 - y1
                val d02 = y0 - y2
                val d13 = y1 - y3
                if (d01 * d01 > d02 * d02 + d13 * d13 && snippetX > snippetY) schemaBlueRed = true
            }
            for (y in 0..< snippetY) {

                if (schemaBlueRed) layer = if (y < snippetY / 2) { 1 } else { 2 }

                val color = Util.stringByteToColor(pixels[y], layer)
                val size = step * step
                val intArray = IntArray(size) { color }

                bitmap.setPixels(intArray,0,step,(snippetX - 1 - x) * step, y * step, step, step)
            }
        }

        mImageView.setImageBitmap(bitmap)

        val textView: TextView = binding.textReview

        "file: $file".also { textView.text = it }

    }

    private fun getNextFileName(currentFileName: String, dir: String): String {
        val currentFileNameJpg = "$currentFileName.jpg"
        val filesAll = File(dir).listFiles()
        filesAll?.sort()
        val filesJpg = filesAll?.filter { it.name.substringAfter(".") == "jpg" }
        var nextFileNameJpg = filesJpg?.get(0)?.name
        val followingFiles = filesJpg?.filter { it.name > currentFileNameJpg }
        if(followingFiles?.isNotEmpty() == true) nextFileNameJpg = followingFiles[0].name

        return nextFileNameJpg?.substringBefore(".") ?: "default"
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun deletePhoto() {
        val files = File(photoDirectory).listFiles()
        for (file in files!!) {
            if (file.name.substringBefore(".") == fileName) file.delete()
        }

        fileName = getNextFileName(fileName, photoDirectory)
        renderPhoto(photoDirectory, fileName)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun processPhoto() {

        val textView: TextView = binding.textReview
        val savedPhotoAnalyzer = SavedPhotoAnalyzer()
        //val uri = Uri.parse("file://$photoDirectory/$fileName.jpg")
        textView.text = savedPhotoAnalyzer.analyze()
    }

}