package com.prostologik.lv12.ui.dataset

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
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
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.prostologik.lv12.R
import com.prostologik.lv12.Util
import com.prostologik.lv12.Util.limitValue
import com.prostologik.lv12.Util.stringToInteger
import com.prostologik.lv12.databinding.FragmentDatasetBinding
import com.prostologik.lv12.ui.home.HomeViewModel
import java.io.File
import java.io.IOException
import kotlin.math.roundToInt

class DatasetFragment : Fragment() {

    private var _binding: FragmentDatasetBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    private lateinit var safeContext: Context

    private val homeViewModel: HomeViewModel by activityViewModels()

    private var photoDirectory: String = "/storage/emulated/0/Android/media/com.prostologik.lv12/image"
    private lateinit var fileNamesArray: Array<String>
    private var fileName: String = "default"
    private var datasetName: String = "ds_01_s28_m255"
    private lateinit var mImageView: ImageView
    private lateinit var bitmap: Bitmap
    private var patchSize: Int = 0
    private var maxLabel: Int = 99999
    private lateinit var linesOfPixels: Array<String>
    private lateinit var linesOfPatches: Array<String>
    private var patchIndex = 0
    private var dsSnippetOption = 1 // 0 - DataSets, 1 - Snippets

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

        patchSize = getPreferencesInt("patch_size", 28)
        datasetName = getPreferencesString("dataset_name", "ds01_s28_m255")

        photoDirectory = homeViewModel.photoDirectory
        fileNamesArray = getFileNames(photoDirectory)

        OverlayView.scaleFactor = scaleFactor

        homeViewModel.photoFileName.observe(viewLifecycleOwner) {
            val temp: String = it ?: ""
            if (temp != "") {
                fileName = temp
                linesOfPixels = renderSnippet(photoDirectory, fileName)
            }
        }

        _binding = FragmentDatasetBinding.inflate(inflater, container, false)
        val root: View = binding.root
        val overlayView = binding.overlayview

        mImageView = binding.imageSnippet

        val editLabel = binding.editLabel
        editLabel.setText("0")

        val btnSave = binding.saveButton
        val btnOption = binding.optionButton
        val btnNext = binding.nextButton
        val btnPrev = binding.prevButton

        btnSave.setOnClickListener {
            val x = (OverlayView.clickX / scaleFactor - patchSize * 0.5f).roundToInt()
            val y = (OverlayView.clickY / scaleFactor - patchSize * 0.5f).roundToInt()

            val label = limitValue(stringToInteger(editLabel.text.toString()), -1, maxLabel)
            editLabel.setText(label.toString())
            val sb: StringBuilder = StringBuilder()
            sb.append(label)

            var size = linesOfPixels.size // includes UV!!
            if (!fileName.startsWith("0")) size = size * 2 / 3

            for (i in 0..< patchSize) {
                val line = linesOfPixels[size - patchSize - x + i] // sizeI - patchSize- // 40-x + i
                val pixels = line.split(",")
                for (j in 0..< patchSize) {
                    val d = pixels[y + j] // sizeJ - patchSize-
                    sb.append(",$d")
                }
            }
            sb.append("\n")

            patchIndex = 0
            try {
                val file = File("$photoDirectory/$datasetName.csv")
                file.forEachLine { patchIndex++ } // to reference the freshly saved patch
                file.appendText(sb.toString())
            } catch (_: IOException) {}

            val msg = "record=$patchIndex x=$x y=$y + patch=$patchSize"
            Toast.makeText(safeContext, msg, Toast.LENGTH_SHORT).show()
        }

        fun renderNextImage(step: Int = 1) {
            val size = fileNamesArray.size
            val currentFileIndex = fileNamesArray.indexOf(fileName)
            val nextItemIndex = (currentFileIndex + size + step) % size
            fileName = fileNamesArray[nextItemIndex]
            linesOfPixels = renderSnippet(photoDirectory, fileName)
            OverlayView.clickX = -1f
            overlayView.invalidate()
        }

        renderNextImage()

        fun getPatchesFromDataset(file: File): Array<String> {
            val temp = mutableListOf<String>()
            file.forEachLine { line -> temp.add(line) }
            return temp.toTypedArray()
        }

        fun renderNextPatch(step: Int = 1) {
            try {
                val file = File("$photoDirectory/$datasetName.csv")
                if(file.exists()) linesOfPatches = getPatchesFromDataset(file)
                val size = linesOfPatches.size
                patchIndex = (patchIndex + size + step) % size
                renderPatch(linesOfPatches[patchIndex])
            } catch (_: IOException) {}
        }

        fun toastXY() {
            val x = (OverlayView.clickX / scaleFactor - patchSize * 0.5f).roundToInt()
            val y = (OverlayView.clickY / scaleFactor - patchSize * 0.5f).roundToInt()
            val msg = "x=$x y=$y + patch=$patchSize"
            Toast.makeText(safeContext, msg, Toast.LENGTH_SHORT).show()
        }

        val spinnerPatchSize: Spinner = binding.spinnerPatchSize

        btnOption.setOnClickListener {
            dsSnippetOption = (dsSnippetOption + 1) % 2
            //var tint: Int
            var src: Int
            if (dsSnippetOption == 0) { // 0=Dataset
                //tint = R.color.purple_500
                src = R.drawable.crop_24
                buildDatasetSpinner(0)
                renderNextPatch(0)
                OverlayView.patchSize = 1
                spinnerPatchSize.visibility = View.INVISIBLE
            }
            else { // if (dsSnippetOption == 1) // 1=Snippet
                //tint = R.color.teal_700
                src = R.drawable.edit_24
                renderNextImage(0)
                OverlayView.patchSize = patchSize
                spinnerPatchSize.visibility = View.VISIBLE
                buildDatasetSpinner(patchSize)
                val textNew = binding.textNew
                textNew.text = getString(R.string.label) + ":"
            }

            toastXY()

            OverlayView.clickX = -1f
            overlayView.invalidate()
            //btnOption.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(tint, null)))
            btnOption.setImageResource(src)
            // Color.RED 0xFFFF0000 btnOption.setBackgroundColor(white)
        }

        btnNext.setOnClickListener {
            if (dsSnippetOption == 1) renderNextImage(1)
            else renderNextPatch(1)
        }
        btnPrev.setOnClickListener {
            if (dsSnippetOption == 1) renderNextImage(-1)
            else renderNextPatch(-1)
        }

        // PatchSize spinner:
        buildPatchSizeSpinner()

        // Dataset spinner:
        buildDatasetSpinner(patchSize)

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun buildPatchSizeSpinner() {
        val arrayOfPatchSizes = arrayOf(16,18,20,22,24,26,28,30,32)

        val listOfPatchSizes = mutableListOf<String>()

        for (i in arrayOfPatchSizes.indices) {
            val t = arrayOfPatchSizes[i].toString()
            listOfPatchSizes.add("patch size: $t x $t")
        }
        val arrayOfPatchSizeNames = listOfPatchSizes.toTypedArray()

        val spinnerPatchSize: Spinner = binding.spinnerPatchSize

        // Create an ArrayAdapter using a simple spinner layout
        val patchSizeAdapter = ArrayAdapter(safeContext, android.R.layout.simple_spinner_item, arrayOfPatchSizeNames)

        // Set layout to use when the list of choices appear
        patchSizeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        // Set Adapter to Spinner
        spinnerPatchSize.setAdapter(patchSizeAdapter)
        spinnerPatchSize.setSelection(arrayOfPatchSizes.indexOf(patchSize))

        spinnerPatchSize.onItemSelectedListener = object:
            AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                patchSize = arrayOfPatchSizes[position]
                savePreferencesInt("patch_size", patchSize)
                OverlayView.patchSize = patchSize
                OverlayView.clickX = -1f
                val overlayView = binding.overlayview
                overlayView.invalidate()

                buildDatasetSpinner(patchSize)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                //patchSize = 28
            }

        }
    }

    private fun buildDatasetSpinner(patchSize: Int) {
        // to list all datasets without NEW --> patchSize = 0 i.e. buildDatasetSpinner(0)
        val allDatasets = getFileNames(photoDirectory, "csv", "ds")
        val listOfDatasets = mutableListOf<String>()
        val listOfSeqNumbers = mutableListOf<Int>()
        if (patchSize > 0) listOfDatasets.add("ds_99_28") // a placeholder for a NEW file name
        for (ds in allDatasets) {
            val bitsOfDatasetName = ds.split("_")
            if (bitsOfDatasetName.size < 3) continue
            if (patchSize > 0 && bitsOfDatasetName[2] != patchSize.toString()) continue
            listOfDatasets.add(ds)
            listOfSeqNumbers.add(stringToInteger(bitsOfDatasetName[1], 0))
        }
        listOfSeqNumbers.sort()
        var nextSeqNumber = 1
        for (num in listOfSeqNumbers) {
            if (num == nextSeqNumber) nextSeqNumber++
        }
        if (patchSize > 0) listOfDatasets[0] = "ds_$nextSeqNumber" + "_" + patchSize // a NEW file name

        // if (patchSize == 0 && arrayOfDatasets.size < 1) ??????????

        //val arrayOfDatasets = getFileNames(photoDirectory, "csv", "ds")
        val arrayOfDatasets = listOfDatasets.toTypedArray()

        if (patchSize == 0 && arrayOfDatasets.indexOf(datasetName) < 0) datasetName = arrayOfDatasets[0]

        val spinnerDataset: Spinner = binding.spinnerDataset
        val textNew = binding.textNew

        // Create an ArrayAdapter using a simple spinner layout
        val datasetAdapter = ArrayAdapter(safeContext, android.R.layout.simple_spinner_item, arrayOfDatasets)

        // Set layout to use when the list of choices appear
        datasetAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        // Set Adapter to Spinner
        spinnerDataset.setAdapter(datasetAdapter)
        spinnerDataset.setSelection(arrayOfDatasets.indexOf(datasetName))

        spinnerDataset.onItemSelectedListener = object:
            AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                datasetName = arrayOfDatasets[position]
                savePreferencesString("dataset_name", datasetName) // "ds01_s28_m255"
                if (patchSize > 0 && position == 0) textNew.text = getString(R.string.NEW) else textNew.text = ""
                patchIndex = 0
                val bitsOfDatasetName = datasetName.split("_")
                maxLabel = if (bitsOfDatasetName.size > 3) stringToInteger(bitsOfDatasetName[3], 99999) else 99999
                // android.R.id.NEW android.R.layout.simple_spinner_item
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                val temp = "NEW file"
            }

        }

    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun renderSnippet(dir: String, file: String): Array<String> {

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

        return linesOfPixels.toTypedArray()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun renderPatch(patch: String): Array<String> {

        val pixels = patch.split(",")

        //val patchSize = 28

        val step = scaleFactor

        bitmap = Bitmap.createBitmap(patchSize * step, patchSize * step, Bitmap.Config.ARGB_8888)

        for (x in 0..< patchSize) {

            for (y in 0..< patchSize) {

                val color = Util.stringByteToColor(pixels[1 + x * patchSize + y], 0)
                val size = step * step
                val intArray = IntArray(size) { color }

                bitmap.setPixels(intArray,0,step,(patchSize - 1 - x) * step, y * step, step, step)

            }
        }

        mImageView.setImageBitmap(bitmap)

        return pixels.toTypedArray()
    }

    private fun getFileNames(dir: String, fileNameExtension: String = "jpg", fileNamePrefix: String = ""): Array<String> {
        val filesAll = File(dir).listFiles()
        filesAll?.sort()

        val selectedExtensions = filesAll?.filter { it.name.substringAfter(".") == fileNameExtension }

        var selectedPrefixes = selectedExtensions
        if (fileNamePrefix != "") {
            selectedPrefixes = selectedExtensions?.filter { it.name.substring(0,fileNamePrefix.length) == fileNamePrefix }
        }

        val selectedFileNames = ArrayList<String>()
        //selectedExtensions?.forEach { f -> selectedFileNames.add(f.name.substringBefore(".")) }
        selectedPrefixes?.forEach { f -> selectedFileNames.add(f.name.substringBefore(".")) }
        return selectedFileNames.toTypedArray()
    }

    private fun getPreferencesInt(key: String, value: Int = 0): Int {
        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE)
        return sharedPref?.getInt(key, value) ?: value
    }

    private fun savePreferencesInt(key: String, value: Int = 0) {
        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE) ?: return
        with (sharedPref.edit()) {
            putInt(key, value)
            apply()
        }
    }

    private fun getPreferencesString(key: String, value: String = "ds01_s28_m255"): String {
        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE)
        return sharedPref?.getString(key, value) ?: value
    }

    private fun savePreferencesString(key: String, value: String) {
        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE) ?: return
        with (sharedPref.edit()) {
            putString(key, value)
            apply()
        }
    }


}