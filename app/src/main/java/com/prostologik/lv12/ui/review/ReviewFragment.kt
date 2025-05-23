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
import com.prostologik.lv12.ui.dataset.OverlayView
import com.prostologik.lv12.ui.home.HomeViewModel
import java.io.File
import java.io.IOException
import kotlin.math.max
import kotlin.math.min

class ReviewFragment : Fragment() {

    private var _binding: FragmentReviewBinding? = null

    private val binding get() = _binding!!

    private val homeViewModel: HomeViewModel by activityViewModels()

    private var photoDirectory: String = "/storage/emulated/0/Android/media/com.prostologik.lv12/image"
        // "/storage/emulated/0/Android/media/com.prostologik.lv12/image"
        // homeViewModel.photoDirectory.toString()
    private lateinit var fileNamesArray: Array<String>
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
        fileNamesArray = getFileNames(photoDirectory)

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

        fun renderNextImage(step: Int = 1) {
            val size = fileNamesArray.size
            val currentFileIndex = fileNamesArray.indexOf(fileName)
            // if (currentFileIndex == -1) ????????
            val nextItemIndex = if (size > 0) (currentFileIndex + size + step) % size else 0
            renderImage(nextItemIndex)
        }

        renderNextImage()

        btnNext.setOnClickListener { renderNextImage() }

        btnPrev.setOnClickListener { renderNextImage(-1) }

        val root: View = binding.root
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun renderPhoto(dir: String, file: String) {

        var includesBlueRed = true
        if (file.startsWith("0")) includesBlueRed = false

        if (File("$dir/$file.jpg").exists()) {
            try {
                val uri = Uri.parse("file://$dir/$file.jpg")
                // uri.toString() --> "file:///storage/emulated/0/Android/media/com.prostologik.lv12/image"
                val imageView: ImageView = binding.imageView
                imageView.setImageURI(uri)
            } catch (_: IOException) {}
        }

        val linesOfPixels = mutableListOf<String>()
        //val step = 6 // to set size of the snippet

        try {
            val fileCsv = File("$dir/$file.csv")

            if(fileCsv.exists()) {
                fileCsv.forEachLine { line ->
                    linesOfPixels.add(line)
                }
            }
        } catch (_: IOException) {}

        if (linesOfPixels.size < 1) {
            linesOfPixels.add("0,0,0")
            linesOfPixels.add("0,0,0")
            linesOfPixels.add("0,0,0")
        }
        // to get snippet dimensions
        val firstLine = linesOfPixels[0].split(",")
        val snippetY = firstLine.size
        val snippetX = linesOfPixels.size
        val snippetUV = if (!file.startsWith("0")) (snippetX * 2 / 3) else snippetX
        val step = calculateImageScale(snippetX, snippetY)

        bitmap = Bitmap.createBitmap(snippetX * step, snippetY * step, Bitmap.Config.ARGB_8888)
        var schemaBlueRed = false
        var layer = 0
        for ((x, line) in linesOfPixels.withIndex()) {
            val pixels = line.split(",")

            if (x == snippetUV && includesBlueRed) schemaBlueRed = true

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

    @RequiresApi(Build.VERSION_CODES.P)
    private fun deletePhoto() {
        val currentFileIndex = fileNamesArray.indexOf(fileName)

        val files = File(photoDirectory).listFiles()
        for (file in files!!) {
            if (file.name.substringBefore(".") == fileName) file.delete()
        }

        fileNamesArray = getFileNames(photoDirectory)
        var nextItemIndex = 0
        if (currentFileIndex >= 0 && currentFileIndex < fileNamesArray.size - 1) nextItemIndex = currentFileIndex + 1
        renderImage(nextItemIndex)

    }

    private fun getFileNames(dir: String, fileNameExtension: String = "jpg", fileNamePrefix: String = ""): Array<String> {
        val filesAll = File(dir).listFiles()
        filesAll?.sort()
        val selectedExtensions = filesAll?.filter { it.name.substringAfter(".") == fileNameExtension }
        val selectedFileNames = ArrayList<String>()
        selectedExtensions?.forEach { f -> selectedFileNames.add(f.name.substringBefore(".")) }
        return selectedFileNames.toTypedArray()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun renderImage(imageIndex: Int) {
        val size = fileNamesArray.size
        if (imageIndex in 0..<size) {
            fileName = fileNamesArray[imageIndex]
            renderPhoto(photoDirectory, fileName)
        }
    }

    private fun calculateImageScale(x: Int, y: Int): Int {
        val width: Int = context?.resources?.displayMetrics?.widthPixels ?: 0
        val height: Int = context?.resources?.displayMetrics?.heightPixels ?: 0
        val minDim = min(width, height)
        val maxSnippetDim = max(x, y)
        return if (maxSnippetDim > 0) minDim / (maxSnippetDim * 2) else 1
    }
}