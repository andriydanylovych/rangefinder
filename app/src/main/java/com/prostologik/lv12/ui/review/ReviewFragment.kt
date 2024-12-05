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

    private val homeViewModel: HomeViewModel by activityViewModels()

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        homeViewModel.photoDirectory.observe(viewLifecycleOwner) {
            val temp: String? = it
            if (temp != null) photoDirectory = temp
        }

        homeViewModel.photoFileName.observe(viewLifecycleOwner) {
            val temp: String? = it
            if (temp != null) photoFileName = temp
        }

        _binding = FragmentReviewBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val btnNext = binding.nextButton
        btnNext.setOnClickListener { nextPhoto() }

        val btnDelete = binding.deleteButton
        btnDelete.setOnClickListener { deletePhoto() }

        val btnSave = binding.saveButton
        btnSave.setOnClickListener { processPhoto() }

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //if (photoDirectory != "/storage") nextPhoto()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private var counter = -1
    private var selectedFileName = photoFileName
    private var newImage = true

    @RequiresApi(Build.VERSION_CODES.P)
    private fun nextPhoto() {
        val imageView: ImageView = binding.imageView
        val textView: TextView = binding.textReview

        if (newImage && photoFileName != "default.jpg") {
            newImage = false
            selectedFileName = photoFileName
        } else {
            val files = File(photoDirectory).listFiles()
            if (files != null) {
                counter = (counter + 1) % files.size

                val fileNames = arrayOfNulls<String>(files.size)
                files.mapIndexed { index, item ->
                    fileNames[index] = item?.name
                }
                selectedFileName = fileNames[counter]!!
            }
        }
        val uri = Uri.parse("file://$photoDirectory/$selectedFileName")
        imageView.setImageURI(uri)

        textView.text = selectedFileName
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun deletePhoto() {
        val files = File(photoDirectory).listFiles()
        for (file in files!!) {
            if (file.name == selectedFileName) {
                file.delete()
                if (counter > -1) counter -= 1
                break
            }
        }
        nextPhoto()
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun processPhoto() {
        val textView: TextView = binding.textReview
        val savedPhotoAnalyzer = SavedPhotoAnalyzer()
        val uri = Uri.parse("file://$photoDirectory/$selectedFileName")
        textView.text = savedPhotoAnalyzer.analyze(uri)
    }

}