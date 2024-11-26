package com.prostologik.lv12.ui.review

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.ViewModelProvider
import com.prostologik.lv12.databinding.FragmentGalleryBinding
import java.io.File

class ReviewFragment : Fragment() {

    private var _binding: FragmentGalleryBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    private var result: String? = "test22"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val galleryViewModel =
            ViewModelProvider(this).get(ReviewViewModel::class.java)

        _binding = FragmentGalleryBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // to get the output directory from HomeFragment
        setFragmentResultListener("requestKey") { requestKey, bundle ->
            // We use a String here, but any type that can be put in a Bundle is supported.
            result = bundle.getString("bundleKey")
            // Do something with the result.
        }

        val btnNext = binding.nextButton
        btnNext.setOnClickListener { nextPhoto() }

        val btnDelete = binding.deleteButton
        btnDelete.setOnClickListener { deletePhoto() }

//        val textView: TextView = binding.textGallery
//        galleryViewModel.text.observe(viewLifecycleOwner) {
//            textView.text = it
//        }

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
        val outputDirectory = "/storage/emulated/0/Android/media/com.prostologik.lv12/image"
        val uriDir = "file://$outputDirectory/"

        val files = File(outputDirectory).listFiles()
        val fileNames = arrayOfNulls<String>(files.size)
        files?.mapIndexed { index, item ->
            fileNames[index] = item?.name
        }

        selectedFile = files[counter]

        val uri = Uri.parse(uriDir + fileNames[counter])
        imageView.setImageURI(uri)

        textView.text = fileNames[counter]
        counter = (counter + 1) % files.size
    }

    private fun deletePhoto() {
        if (selectedFile?.exists() == true) selectedFile!!.delete()
        counter = 0
        nextPhoto()
    }
}