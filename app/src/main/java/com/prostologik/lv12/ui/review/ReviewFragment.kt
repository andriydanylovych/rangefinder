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

    private var photoDirectory: String = "/storage/emulated/0/Android/media/com.prostologik.lv12/image" // "/storage" //
    private var photoFileName: String = "default.jpg"
    private var newImage = false

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
                photoFileName = temp
                newImage = true
                nextPhoto()
            }
        }

        _binding = FragmentReviewBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val btnNext = binding.nextButton
        btnNext.setOnClickListener { nextPhoto() }

        val btnDelete = binding.deleteButton
        btnDelete.setOnClickListener { deletePhoto() }

        val btnSave = binding.saveButton
        btnSave.setOnClickListener { processPhoto() }

        nextPhoto()

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun nextPhoto() {

        if (newImage) {
            newImage = false
        } else {
            photoFileName = getNextFileName(photoFileName, photoDirectory).toString()
        }

        val uri = Uri.parse("file://$photoDirectory/$photoFileName")

        val imageView: ImageView = binding.imageView
        imageView.setImageURI(uri)

        val textView: TextView = binding.textReview
        textView.text = photoFileName
    }

    private fun getNextFileName(currentFileName: String, dir: String): String? {
        val files = File(dir).listFiles()
        files?.sort()
        var nextFileName = files?.get(0)?.name
        var graterNameExists = false
        for (file in files!!) {
            if(currentFileName < file.name) {
                nextFileName = file.name
                graterNameExists = true
                break
            }
        }
        if (graterNameExists) {
            for (file in files) {
                if (nextFileName != null) {
                    if(currentFileName < file.name && nextFileName > file.name) {
                        nextFileName = file.name
                    }
                }
            }
        } else {
            for (file in files) {
                if (nextFileName != null) {
                    if(nextFileName > file.name) {
                        nextFileName = file.name
                    }
                }
            }
        }
        return nextFileName
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun deletePhoto() {
        val files = File(photoDirectory).listFiles()
        for (file in files!!) {
            if (file.name == photoFileName) {
                file.delete()
                break
            }
        }
        nextPhoto()
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun processPhoto() {
        val textView: TextView = binding.textReview
        val savedPhotoAnalyzer = SavedPhotoAnalyzer()
        val uri = Uri.parse("file://$photoDirectory/$photoFileName")
        textView.text = savedPhotoAnalyzer.analyze(uri)
    }

    private fun getOutputDirectory(): File {
        val mediaDir = activity?.externalMediaDirs?.firstOrNull()?.let {
            File(it, "image").apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists()) mediaDir else activity?.filesDir!!
    }

}