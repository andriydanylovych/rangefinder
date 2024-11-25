package com.prostologik.lv12.ui.review

import android.media.ThumbnailUtils
import android.os.Bundle
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.ViewModelProvider
import com.prostologik.lv12.R
import com.prostologik.lv12.databinding.FragmentGalleryBinding
import com.prostologik.lv12.ui.home.HomeFragment.Companion.FILENAME_FORMAT
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class ReviewFragment : Fragment() {

    private var _binding: FragmentGalleryBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
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

        val textView: TextView = binding.textGallery
        galleryViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Photo capture succeeded: file:///storage/emulated/0/Android/media/com.prostologik.lv12/image/2024-11-25-22-02-17-480.jpg

        //val outputDirectory = activity?.externalMediaDirs?.firstOrNull()
        val outputDirectory = "/storage/emulated/0/Android/media/com.prostologik.lv12/image"
        val photoFile = File(outputDirectory, "cup.jpg")
        val bitmap = ThumbnailUtils.createImageThumbnail(photoFile, Size(640, 480), null)
        val imageView: ImageView = binding.imageView
        imageView.setImageBitmap(bitmap)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}