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
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.prostologik.lv12.databinding.FragmentReviewBinding
import com.prostologik.lv12.ui.home.HomeViewModel
import kotlinx.coroutines.launch
import java.io.File

class ReviewFragment : Fragment() {

    private var _binding: FragmentReviewBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    private var result: String? = "test22"

    private var dir: File? = null
    private var test: String? = "111"

    private val homeViewModel: HomeViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
//        val galleryViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentReviewBinding.inflate(inflater, container, false)
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
//        homeViewModel.text.observe(viewLifecycleOwner) {
//            test = it //textView.text = it
//        }

        homeViewModel.photoDirectory.observe(viewLifecycleOwner) {
                x -> dir = x
        }
        homeViewModel.test.observe(viewLifecycleOwner) {
            test = it
        }
//        homeViewModel.photoDirectory.observe(viewLifecycleOwner, Observer {
//            x -> dir = x
//        })

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
        val outputDirectory = "/storage/emulated/0/Android/media/com.prostologik.lv12/image" // dir.toString() //
        val uriDir = "file://$outputDirectory/"

        val files = File(outputDirectory).listFiles()
        val fileNames = arrayOfNulls<String>(files.size)
        files?.mapIndexed { index, item ->
            fileNames[index] = item?.name
        }

        selectedFile = files[counter]

        val uri = Uri.parse(uriDir + fileNames[counter])
        imageView.setImageURI(uri)

        textView.text = test // fileNames[counter] // dir.toString()
        counter = (counter + 1) % files.size
    }

    private fun deletePhoto() {
        if (selectedFile?.exists() == true) selectedFile!!.delete()
        counter = 0
        nextPhoto()
    }
}