package com.prostologik.lv12.ui.review

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.graphics.toColor
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
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
    private lateinit var canvas: Canvas
    private lateinit var paint: Paint

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        // 4 lines only needed because default fragment is ReviewFragment, not HomeFragment
        var tempDir: String? = null
        homeViewModel.photoDirectory.observe(viewLifecycleOwner) { tempDir = it }
        photoDirectory = if (tempDir != null) tempDir!!
                        else getOutputDirectory().toString()

        homeViewModel.photoFileName.observe(viewLifecycleOwner) {
            val temp: String? = it
            if (temp != null) {
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


        var csvtext = "csv not available"
        try {
            bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
            val fileCsv = File("$dir/$file.csv")
            var y = 50
            if(fileCsv.exists()) {
                fileCsv.forEachLine { line ->
                    val pixels = line.split(",")
                    val listSize = pixels.size - 1
                    csvtext = "$listSize"
                    for (x in 0..listSize) {
                        val color = stringToInteger(pixels[x]) * -65793 //Color.GRAY
                        //bitmap.setPixel(x, y, color)
                        bitmap.setPixel(x*2, y*2, color)
                        bitmap.setPixel(x*2+1, y*2, color)
                        bitmap.setPixel(x*2, y*2+1, color)
                        bitmap.setPixel(x*2+1, y*2+1, color)
                    }
                    y++
                    mImageView.setImageBitmap(bitmap)
                }
            }
        } catch (_: IOException) {}

        val textView: TextView = binding.textReview

        "$file :: $csvtext".also { textView.text = it }

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
        val uri = Uri.parse("file://$photoDirectory/$fileName.jpg")
        textView.text = savedPhotoAnalyzer.analyze(uri)
    }

    private fun getOutputDirectory(): File {
        val mediaDir = activity?.externalMediaDirs?.firstOrNull()?.let {
            File(it, "image").apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists()) mediaDir else activity?.filesDir!!
    }

    private fun stringToInteger(s: String): Int {
        return try {
            s.toInt()
        } catch (nfe: NumberFormatException) {
            32
        }
    }

}