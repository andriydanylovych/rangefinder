package com.prostologik.lv12.ui.dataset

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.prostologik.lv12.R
import com.prostologik.lv12.Util
import com.prostologik.lv12.Util.byteToPixel
import com.prostologik.lv12.Util.limitValue
import com.prostologik.lv12.Util.pixelToByteAsInt
import com.prostologik.lv12.Util.stringToInteger
import com.prostologik.lv12.databinding.FragmentDatasetBinding
import com.prostologik.lv12.ui.home.HomeViewModel
import java.io.File
import java.io.IOException
import kotlin.math.roundToInt


class DatasetFragment : Fragment() {

    private var _binding: FragmentDatasetBinding? = null
    private val binding get() = _binding!!

    private lateinit var safeContext: Context

    private val homeViewModel: HomeViewModel by activityViewModels()

    private lateinit var mImageView: ImageView
    private lateinit var overlayView: OverlayView
    private lateinit var textName: TextView
    private lateinit var textLabel: TextView
    private lateinit var textNew: TextView
    private lateinit var editLabel: EditText
    private lateinit var spinnerPatchSize: Spinner
    private lateinit var spinnerDataset: Spinner

    private lateinit var btnSave: ImageButton
    private lateinit var btnDelete: ImageButton
    private lateinit var btnOption: ImageButton

    private lateinit var bitmap: Bitmap
    private lateinit var fileNamesArray: Array<String>
    private lateinit var linesOfPixels: Array<String>
    private lateinit var linesOfPatches: Array<String>

    private var photoDirectory: String = "/storage/emulated/0/Android/media/com.prostologik.lv12/image"
    private var fileName: String = "default"
    private var datasetName: String = "ds_01_s28_m255"
    private var patchSize: Int = 0
    private var maxLabel: Int = 99999
    private var patchIndex = 0
    private var pixelIndex = 0
    private var dsSnippetOption = 1 // 0 - DataSets, 1 - Snippets
    private val scaleFactor = 8
    private val scalePatch = 24

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

        _binding = FragmentDatasetBinding.inflate(inflater, container, false)
        val root: View = binding.root
        mImageView = binding.imageSnippet
        overlayView = binding.overlayView
        textName = binding.textName
        textLabel = binding.textLabel
        textNew = binding.textNew
        editLabel = binding.editLabel
        spinnerPatchSize = binding.spinnerPatchSize
        spinnerDataset = binding.spinnerDataset
        btnSave = binding.saveButton
        btnDelete = binding.deleteButton
        btnOption = binding.optionButton
        val btnNext = binding.nextButton
        val btnPrev = binding.prevButton

        dsSnippetOption = 1 // 0 - DataSets, 1 - Snippets
        btnSave.visibility = View.VISIBLE
        btnDelete.visibility = View.INVISIBLE

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

        textLabel.text = getString(R.string.label)
        editLabel.setText("0")

//        editLabel.setOnClickListener { // setOnClickListener
//            btnSave.visibility = View.VISIBLE
//            btnDelete.visibility = View.INVISIBLE
//        }

        editLabel.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                if (dsSnippetOption == 0) {
                    btnSave.visibility = View.VISIBLE
                    btnDelete.visibility = View.INVISIBLE
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        })

        btnSave.setOnClickListener {
            if (dsSnippetOption == 1) {
                val x = (OverlayView.clickX / scaleFactor - patchSize * 0.5f).roundToInt()
                val y = (OverlayView.clickY / scaleFactor - patchSize * 0.5f).roundToInt()

                val label = limitValue(stringToInteger(editLabel.text.toString()), -1, maxLabel)
                editLabel.setText(label.toString())
                val sb: StringBuilder = StringBuilder()
                sb.append(label)

                var size = linesOfPixels.size // includes UV!!
                if (!fileName.startsWith("0")) size = size * 2 / 3

                for (i in 0..< patchSize) {
                    val line = linesOfPixels[size - patchSize - x + i]
                    val pixels = line.split(",")
                    for (j in 0..< patchSize) {
                        val d = pixels[y + j]
                        sb.append(",$d")
                    }
                }
                sb.append("\n")

                var lineCount = 0
                var msg: String
                try {
                    val file = File("$photoDirectory/$datasetName.csv")
                    if (file.exists()) {
                        file.forEachLine { lineCount++ } // to reference the freshly saved patch
                        patchIndex = lineCount
                        file.appendText(sb.toString())
                    } else {
                        file.writeText(sb.toString())
                    }
                    textNew.text = "" // to suppress NEW if it was there
                    btnOption.visibility = View.VISIBLE
                    msg = "record=$patchIndex x=$x y=$y + size=$patchSize"
                } catch (_: IOException) {
                    msg = "not posted"
                }
                Toast.makeText(safeContext, msg, Toast.LENGTH_SHORT).show()
            } else { // if (dsSnippetOption == 0)
                // edit label or pixel
                var value = stringToInteger(editLabel.text.toString())

                // check if value is in the allowed range
                if (pixelIndex == 0 && value < -1) value = -1

                // pixelToByteAsInt(value) // convert 255 to -128
                if (pixelIndex > 0) value = pixelToByteAsInt(value)

                editDataset(photoDirectory, datasetName, patchIndex, pixelIndex, value)

                if (pixelIndex > 0) renderNextPatch(0)

                Toast.makeText(safeContext, "saved value $value", Toast.LENGTH_SHORT).show()
                btnSave.visibility = View.INVISIBLE
                btnDelete.visibility = View.VISIBLE


            }
        }

        btnDelete.setOnClickListener {
            deletePatch(photoDirectory, datasetName, patchIndex)
        }

        fun renderNextImage(step: Int = 1) {
            val size = fileNamesArray.size
            val currentFileIndex = fileNamesArray.indexOf(fileName)
            val nextItemIndex = if (size > 0) (currentFileIndex + size + step) % size else 0
            //val nextItemIndex = (currentFileIndex + size + step) % size
            fileName = fileNamesArray[nextItemIndex]
            linesOfPixels = renderSnippet(photoDirectory, fileName)
            OverlayView.clickX = -1f
            overlayView.invalidate()
        }

        renderNextImage()

        btnOption.setOnClickListener {
            dsSnippetOption = (dsSnippetOption + 1) % 2
            //var tint: Int
            val src: Int
            if (dsSnippetOption == 0) { // 0=Dataset
                // tint = R.color.purple_500
                src = R.drawable.crop_24
                OverlayView.scaleFactor = scalePatch
                OverlayView.patchSize = 0 // scalePatch
                spinnerPatchSize.visibility = View.INVISIBLE
                spinnerDataset.visibility = View.INVISIBLE
                renderNextPatch(0)
            }
            else { // if (dsSnippetOption == 1) // 1=Snippet
                // tint = R.color.teal_700
                src = R.drawable.edit_24
                OverlayView.scaleFactor = scaleFactor
                OverlayView.patchSize = patchSize
                buildDatasetSpinner(patchSize)
                spinnerPatchSize.visibility = View.VISIBLE
                spinnerDataset.visibility = View.VISIBLE
                btnSave.visibility = View.VISIBLE
                btnDelete.visibility = View.INVISIBLE
                textLabel.text = getString(R.string.label)
                renderNextImage(0)
            }

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

        buildPatchSizeSpinner()

        buildDatasetSpinner(patchSize)

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun renderNextPatch(step: Int = 1) {
        try {
            val file = File("$photoDirectory/$datasetName.csv")
            if (file.exists()) {
                val temp = mutableListOf<String>()
                file.forEachLine { line -> temp.add(line) }
                linesOfPatches = temp.toTypedArray() //getPatchesFromDataset(file)
            } else arrayOf("")
            val size = linesOfPatches.size
            if (size > 0) {
                patchIndex = (patchIndex + size + step) % size
                renderPatch(linesOfPatches[patchIndex])
            } else {
                renderPatch("")
            }

            textName.text = getString(R.string.dataset_patch_index, datasetName, patchIndex)
            textLabel.text = getString(R.string.label)
            btnSave.visibility = View.INVISIBLE
            btnDelete.visibility = View.VISIBLE
            pixelIndex = 0
        } catch (_: IOException) {}
    }

    private fun buildPatchSizeSpinner() {
        val arrayOfPatchSizes = arrayOf(16,18,20,22,24,26,28,30,32)

        val listOfPatchSizes = mutableListOf<String>()

        for (i in arrayOfPatchSizes.indices) {
            val t = arrayOfPatchSizes[i].toString()
            listOfPatchSizes.add("patch size: $t x $t")
        }
        val arrayOfPatchSizeNames = listOfPatchSizes.toTypedArray()

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
                overlayView.invalidate()

                buildDatasetSpinner(patchSize)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}

        }
    }

    private fun buildDatasetSpinner(patchSize: Int) {
        // to list all datasets without NEW --> patchSize = 0 i.e. buildDatasetSpinner(0)

        val allDatasets = getFileNames(photoDirectory, "csv", "ds")
        val listOfDatasets = mutableListOf<String>()
        val listOfSeqNumbers = mutableListOf<Int>()
        listOfDatasets.add("ds_99_28") // a placeholder for a NEW file name if (patchSize > 0)
        for (ds in allDatasets) {
            val bitsOfDatasetName = ds.split("_")
            if (bitsOfDatasetName.size < 3) continue
            if (bitsOfDatasetName[2] != patchSize.toString()) continue // && patchSize > 0
            listOfDatasets.add(ds)
            listOfSeqNumbers.add(stringToInteger(bitsOfDatasetName[1], 0))
        }
        listOfSeqNumbers.sort()
        var nextSeqNumber = 1
        for (num in listOfSeqNumbers) {
            if (num == nextSeqNumber) nextSeqNumber++
        }
        listOfDatasets[0] = "ds_$nextSeqNumber" + "_" + patchSize // a NEW file name if (patchSize > 0)

        val arrayOfDatasets = listOfDatasets.toTypedArray()

        //if (patchSize == 0 && arrayOfDatasets.indexOf(datasetName) < 0) datasetName = arrayOfDatasets[0]

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
                savePreferencesString("dataset_name", datasetName) // "ds_1_28_255"
                if (position == 0) {
                    textNew.text = getString(R.string.NEW)
                    btnOption.visibility = View.INVISIBLE
                } else {
                    textNew.text = ""
                    btnOption.visibility = View.VISIBLE
                } // patchSize > 0 &&
                patchIndex = 0
                val bitsOfDatasetName = datasetName.split("_")
                maxLabel = if (bitsOfDatasetName.size > 3) stringToInteger(bitsOfDatasetName[3], 99999) else 99999
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}

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

        textName.text = file

        overlayView.setOnClickListener {} // to silence the listener set in renderPatch

        return linesOfPixels.toTypedArray()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun renderPatch(patch: String): Array<String> {

        val temp = patchSize * patchSize + 1
        var pixels = (Array(temp) { "0" }).toList()
        try {
            if (patch != "") pixels = patch.split(",")
        } catch (_: IOException) {}

        Toast.makeText(safeContext, "pixels.size = ${pixels.size}", Toast.LENGTH_SHORT).show()

        val step = scalePatch

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

        editLabel.setText(pixels[0])

        OverlayView.patchSize = 0
        overlayView.invalidate()

        overlayView.setOnClickListener {
            val x = (OverlayView.clickX / scalePatch - 0.5f).roundToInt()
            val y = (OverlayView.clickY / scalePatch - 0.5f).roundToInt()

            pixelIndex = limitValue((patchSize - 1 - x) * patchSize + y + 1, 1, pixels.size - 1)

            val pxValue = byteToPixel(stringToInteger(pixels[pixelIndex]).toByte()).toString()
            editLabel.setText(pxValue)
            textLabel.text = getString(R.string.pixel, pixelIndex)
        }

        return pixels.toTypedArray()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun deletePatch(fileDir: String, dsName: String, lineIndex: Int) {
        try {
            val file = File("$fileDir/$dsName.csv")
            if (file.exists()) {
                val sb: StringBuilder = StringBuilder()
                var i = 0
                file.forEachLine { line ->
                    run {
                        if (i != lineIndex) sb.append("$line\n")
                        i++
                    }
                }
                if (true) { // i > 1
                    file.writeText(sb.toString()) // to avoid ending without any patches
                    renderNextPatch(0)
                } else {
                    file.delete()
                    // go back to snippet view
                }
            }
        } catch (_: IOException) {}
    }

    private fun editDataset(fileDir: String, dsName: String, lineIndex: Int, pixelIndex: Int, newValue: Int) {
        try {
            val file = File("$fileDir/$dsName.csv")
            val sb: StringBuilder = StringBuilder()
            var i = 0
            file.forEachLine { line -> run {
                    if (i == lineIndex) {
                        val pixels = line.split(",")
                        for (j in pixels.indices) {
                            if (j > 0) sb.append(",")
                            if (j == pixelIndex) sb.append(newValue)
                            else sb.append(pixels[j])
                        }
                        sb.append("\n")
                    }
                    else sb.append("$line\n")
                    i++
                }
            }
            file.writeText(sb.toString())
        } catch (_: IOException) {}

    }

//    private fun editDataset(fileDir: String, fileName: String, lineIndex: Int, newPatch: String) {
//    }

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

    private fun savePreferencesInt(key: String, value: Int) {
        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE) ?: return
        with (sharedPref.edit()) {
            putInt(key, value)
            apply()
        }
    }

    private fun getPreferencesString(key: String, value: String = "ds_01_s28"): String {
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