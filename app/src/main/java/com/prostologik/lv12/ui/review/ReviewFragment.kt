package com.prostologik.lv12.ui.review

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
import com.prostologik.lv12.databinding.FragmentReviewBinding
import com.prostologik.lv12.ui.home.HomeViewModel
import java.io.File


class ReviewFragment : Fragment() {

    private var _binding: FragmentReviewBinding? = null

    private val binding get() = _binding!!

    private var photoDirectory: String = "/storage/emulated/0/Android/media/com.prostologik.lv12/image"
    private var fileName: String = "default"

    private val homeViewModel: HomeViewModel by activityViewModels()

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val outputDirectory = getOutputDirectory()
        photoDirectory = outputDirectory.toString()

//        homeViewModel.photoDirectory.observe(viewLifecycleOwner) {
//            val temp: String? = it
//            if (temp != null) photoDirectory = temp
//        }

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

        fileName = getNextFileName(fileName, photoDirectory)
        renderPhoto(photoDirectory, fileName)

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun renderPhoto(dir: String, file: String) {

        val uri = Uri.parse("file://$dir/$file.jpg")

        val imageView: ImageView = binding.imageView
        imageView.setImageURI(uri)

        val textView: TextView = binding.textReview
        textView.text = file
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

}