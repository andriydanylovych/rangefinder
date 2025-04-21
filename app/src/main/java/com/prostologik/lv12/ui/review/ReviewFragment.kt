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

    private val homeViewModel: HomeViewModel by activityViewModels()

    private var photoDirectory: String = "/storage/emulated/0/Android/media/com.prostologik.lv12/image"
        // "/storage/emulated/0/Android/media/com.prostologik.lv12/image"
        // homeViewModel.photoDirectory.toString()
    private var fileName: String = "default"

    private lateinit var mImageView: ImageView
    private lateinit var bitmap: Bitmap

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        photoDirectory = homeViewModel.photoDirectory

        homeViewModel.photoFileName.observe(viewLifecycleOwner) {
            val temp: String = it ?: ""
            if (temp != "") {
                fileName = temp
                renderPhoto(photoDirectory, fileName)
            }
        }

        _binding = FragmentReviewBinding.inflate(inflater, container, false)

        mImageView = binding.imageSnippet //findViewById(R.id.imageSnippet)

        val btnDelete = binding.deleteButton
        val btnNext = binding.nextButton
        val btnPrev = binding.prevButton

        btnDelete.setOnClickListener { deletePhoto() }

        fun renderNext() {
            fileName = getNextFileName(fileName, photoDirectory)
            renderPhoto(photoDirectory, fileName)
        }

        renderNext()

        btnNext.setOnClickListener { renderNext() }

        btnPrev.setOnClickListener {
            fileName = getPrevFileName(fileName, photoDirectory)
            renderPhoto(photoDirectory, fileName)
        }

        val root: View = binding.root
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
        val snippetUV = snippetX * 2 / 3 // bug: snippetX - snippetY / 2

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
                if (d01 * d01 > d02 * d02 + d13 * d13) schemaBlueRed = true //  && snippetX > snippetY
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

        "file: $file  $snippetUV x $snippetY".also { textView.text = it }

    }

    private fun getSortedJpgFiles(dir: String): List<File>? {
        val filesAll = File(dir).listFiles()
        filesAll?.sort()
        return filesAll?.filter { it.name.substringAfter(".") == "jpg" }
    }

    private fun getNextFileName(currentFileName: String, dir: String): String {
        val filesJpg = getSortedJpgFiles(dir)
        var nextFileNameJpg = filesJpg?.first()?.name // in case it is the last one
        val followingFiles = filesJpg?.filter { it.name > "$currentFileName.jpg" }
        if(followingFiles?.isNotEmpty() == true) nextFileNameJpg = followingFiles.first().name

        return nextFileNameJpg?.substringBefore(".") ?: "default"
    }

    private fun getPrevFileName(currentFileName: String, dir: String): String {
        val filesJpg = getSortedJpgFiles(dir)
        var prevFileNameJpg = filesJpg?.last()?.name
        val previousFiles = filesJpg?.filter { it.name < "$currentFileName.jpg" }
        if(previousFiles?.isNotEmpty() == true) prevFileNameJpg = previousFiles.last().name

        return prevFileNameJpg?.substringBefore(".") ?: "default"
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

//    @RequiresApi(Build.VERSION_CODES.P)
//    private fun processPhoto() {
//
//        val textView: TextView = binding.textReview
//        val savedPhotoAnalyzer = SavedPhotoAnalyzer()
//        //val uri = Uri.parse("file://$photoDirectory/$fileName.jpg")
//        textView.text = savedPhotoAnalyzer.analyze()
//    }

}