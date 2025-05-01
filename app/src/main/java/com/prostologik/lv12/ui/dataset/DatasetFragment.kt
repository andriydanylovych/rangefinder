package com.prostologik.lv12.ui.dataset

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.prostologik.lv12.Util
import com.prostologik.lv12.databinding.FragmentDatasetBinding
import com.prostologik.lv12.ui.home.HomeViewModel
import java.io.File
import java.io.IOException
import kotlin.math.max

class DatasetFragment : Fragment() {

    private var _binding: FragmentDatasetBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    private lateinit var safeContext: Context

    private val homeViewModel: HomeViewModel by activityViewModels()

    private var photoDirectory: String = "/storage/emulated/0/Android/media/com.prostologik.lv12/image"
    private lateinit var fileNamesArray: Array<String>
    private var fileName: String = "default"
    private lateinit var mImageView: ImageView
    private lateinit var bitmap: Bitmap

    private val scaleFactor = 8

    override fun onAttach(context: Context) {
        super.onAttach(context)
        safeContext = context
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        photoDirectory = homeViewModel.photoDirectory
        fileNamesArray = getFileNames(photoDirectory)

        OverlayView.scaleFactor = scaleFactor

        homeViewModel.photoFileName.observe(viewLifecycleOwner) {
            val temp: String = it ?: ""
            if (temp != "") {
                fileName = temp
                renderPhoto(photoDirectory, fileName)
            }
        }

        _binding = FragmentDatasetBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textDataset

        mImageView = binding.imageSnippet

        val btnDelete = binding.deleteButton
        val btnNext = binding.nextButton
        val btnPrev = binding.prevButton

        btnDelete.setOnClickListener { textView.text = "clicked btnDelete" }

        fun renderImage(imageIndex: Int) {
            fileName = fileNamesArray[imageIndex]
            renderPhoto(photoDirectory, fileName)
            textView.text = fileName
        }

        fun renderNextImage() {
            val currentFileIndex = fileNamesArray.indexOf(fileName)
            var nextItemIndex = 0
            if (currentFileIndex >= 0 && currentFileIndex < fileNamesArray.size - 1) nextItemIndex = currentFileIndex + 1
            renderImage(nextItemIndex)
        }

        renderNextImage()

        btnNext.setOnClickListener { renderNextImage() }

        btnPrev.setOnClickListener {
            val currentFileIndex = fileNamesArray.indexOf(fileName)
            var nextItemIndex = currentFileIndex - 1
            if (nextItemIndex < 0) nextItemIndex = fileNamesArray.size - 1
            renderImage(nextItemIndex)
        }

        // PatchSize spinner:

        var patchSize: Int

        val arrayOfPatchSizes = arrayOf(16,18,20,22,24,26,28,30,32)

        val listOfPatchSizes = mutableListOf<String>()

        for (i in arrayOfPatchSizes.indices) {
            val t = arrayOfPatchSizes[i].toString()
            listOfPatchSizes.add("patch size: $t x $t")
        }
        val arrayOfPatchSizeNames = listOfPatchSizes.toTypedArray()

        val spinnerOutputSize: Spinner = binding.spinnerPatchSize

        // Create an ArrayAdapter using a simple spinner layout
        val patchSizeAdapter = ArrayAdapter(safeContext, android.R.layout.simple_spinner_item, arrayOfPatchSizeNames)

        // Set layout to use when the list of choices appear
        patchSizeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        // Set Adapter to Spinner
        spinnerOutputSize.setAdapter(patchSizeAdapter)
        spinnerOutputSize.setSelection(arrayOfPatchSizes.indexOf(28))

        spinnerOutputSize.onItemSelectedListener = object:
            AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                patchSize = arrayOfPatchSizes[position]
                OverlayView.patchSize = patchSize
                val overlayView = binding.overlayview
                overlayView.invalidate()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                patchSize = 28
            }

        }




        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun renderPhoto(dir: String, file: String) {

        var schemaBlueRed = true
        if (file.startsWith("0")) schemaBlueRed = false

        val linesOfPixels = mutableListOf<String>()



        try {
            val fileCsv = File("$dir/$file.csv")

            if(fileCsv.exists()) {
                fileCsv.forEachLine { line ->
                    linesOfPixels.add(line)
                }
            }
        } catch (_: IOException) {}

        // to get snippet dimensions
        var snippetY = 2
        var snippetX = 3
        if (linesOfPixels.size > 0) {
            val firstLine = linesOfPixels[0].split(",")
            snippetY = firstLine.size
            snippetX = linesOfPixels.size
        }
        var snippetUV = snippetX
        if (schemaBlueRed) snippetUV = snippetX * 2 / 3

        val step = scaleFactor

        bitmap = Bitmap.createBitmap(snippetUV * step, snippetY * step, Bitmap.Config.ARGB_8888)

        for ((x, line) in linesOfPixels.withIndex()) {
            val pixels = line.split(",")

            if (x == snippetUV) break

            for (y in 0..< snippetY) {

                val color = Util.stringByteToColor(pixels[y], 0)
                val size = step * step
                val intArray = IntArray(size) { color }

                bitmap.setPixels(intArray,0,step,(snippetUV - 1 - x) * step, y * step, step, step)

            }
        }

        mImageView.setImageBitmap(bitmap)

    }

    private fun getFileNames(dir: String, fileNameExtension: String = "jpg", fileNamePrefix: String = ""): Array<String> {
        val filesAll = File(dir).listFiles()
        filesAll?.sort()
        val selectedExtensions = filesAll?.filter { it.name.substringAfter(".") == fileNameExtension }
        val selectedFileNames = ArrayList<String>()
        selectedExtensions?.forEach { f -> selectedFileNames.add(f.name.substringBefore(".")) }
        return selectedFileNames.toTypedArray()
    }

}