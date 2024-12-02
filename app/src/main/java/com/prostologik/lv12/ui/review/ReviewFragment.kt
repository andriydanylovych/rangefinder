package com.prostologik.lv12.ui.review

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.prostologik.lv12.databinding.FragmentReviewBinding
import com.prostologik.lv12.ui.home.HomeViewModel
import java.io.File

class ReviewFragment : Fragment() {

    private var _binding: FragmentReviewBinding? = null

    private val binding get() = _binding!!

    private var photoDirectory: String = "/storage" // "/storage/emulated/0/Android/media/com.prostologik.lv12/image"

    private val homeViewModel: HomeViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        homeViewModel.photoDirectory.observe(viewLifecycleOwner) {
            val temp: String? = it
            if (temp != null) photoDirectory = temp
        }

        _binding = FragmentReviewBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val btnNext = binding.nextButton
        btnNext.setOnClickListener { nextPhoto() }

        val btnDelete = binding.deleteButton
        btnDelete.setOnClickListener { deletePhoto() }

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        nextPhoto()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private var counter = 0
    private var selectedFile: File? = null

    private fun nextPhoto() {

        val imageView: ImageView = binding.imageView
        val textView: TextView = binding.textGallery
        val outputDirectory = photoDirectory // "/storage/emulated/0/Android/media/com.prostologik.lv12/image"
        val uriDir = "file://$outputDirectory/"

        val files = File(outputDirectory).listFiles()

        if (files != null) {
            val fileNames = arrayOfNulls<String>(files.size)
            files.mapIndexed { index, item ->
                fileNames[index] = item?.name
            }

            selectedFile = files[counter]

            val uri = Uri.parse(uriDir + fileNames[counter])
            imageView.setImageURI(uri)

            textView.text = fileNames[counter]
            counter = (counter + 1) % files.size
        }

    }

    private fun deletePhoto() {
        if (selectedFile?.exists() == true) selectedFile!!.delete()
        counter = 0
        nextPhoto()
    }
}