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
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt


class DatasetFragment : Fragment() {

    private var _binding: FragmentDatasetBinding? = null
    private val binding get() = _binding!!

    private lateinit var safeContext: Context

    private val homeViewModel: HomeViewModel by activityViewModels()

    private lateinit var mImageView: ImageView
    private lateinit var overlayView: OverlayView
    private lateinit var textName: TextView
    private lateinit var textLabelTag: TextView
    private lateinit var editLabelTag: EditText
    private lateinit var textLabel: TextView
    private lateinit var textNew: TextView
    private lateinit var editLabel: EditText
    private lateinit var spinnerPatchSize: Spinner
    private lateinit var spinnerDataset: Spinner
    private lateinit var spinnerLabel: Spinner

    private lateinit var btnSave: ImageButton
    private lateinit var btnDelete: ImageButton
    private lateinit var btnOption: ImageButton

    private lateinit var fileNamesArray: Array<String>
    private lateinit var lineOfSnippetPixels: Array<String>
    private lateinit var listOfPatches: Array<String>
    private lateinit var listOfLabels: MutableList<String>

    private var photoDirectory: String = "/storage/emulated/0/Android/media/com.prostologik.lv12/image"
    private var fileName: String = "default"
    private var datasetName: String = "ds_1_28"
    private var textLabels: String = ""
    private var patchSize: Int = 0
    private var maxLabel: Int = 99999
    private var patchIndex = 0
    private var pixelIndex = 0
    private var dsSnippetOption = 1 // 0 - DataSets, 1 - Snippets
    private var scaleFactor = 8 // will be recalculated
    private var scalePatch = 24 // will be recalculated

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
        textLabelTag = binding.textLabelTag
        editLabelTag = binding.editLabelTag
        textLabel = binding.textLabel
        textNew = binding.textNew
        editLabel = binding.editLabel
        spinnerPatchSize = binding.spinnerPatchSize
        spinnerDataset = binding.spinnerDataset
        spinnerLabel = binding.spinnerLabel
        btnSave = binding.saveButton
        btnDelete = binding.deleteButton
        btnOption = binding.optionButton
        val btnNext = binding.nextButton
        val btnPrev = binding.prevButton

        dsSnippetOption = 1 // 0 - DataSets, 1 - Snippets
        showSaveHideDelete(true)
        showSpinnersHideLabelTitle(true)

        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE)
        patchSize = sharedPref?.getInt("patch_size", 28) ?: 28
                //getPreferencesInt("patch_size", 28)
        datasetName = sharedPref?.getString("dataset_name", "ds_1_28") ?: "ds_1_28"

        val mnistLabels = "ds_784_28_mnist,zero,one,two,three,four,five,six,seven,eight,nine"
        textLabels = sharedPref?.getString("dataset_labels", mnistLabels) ?: mnistLabels
        listOfLabels = getListOfLabels(textLabels, datasetName).toMutableList()

        editLabelTag.setOnClickListener {
            if (dsSnippetOption == 0 && pixelIndex == 0) showSaveHideDelete(true)
        }

        photoDirectory = homeViewModel.photoDirectory
        fileNamesArray = getFileNames(photoDirectory)
        //Toast.makeText(safeContext, "fileNamesArray = $fileNamesArray", Toast.LENGTH_SHORT).show()

        homeViewModel.photoFileName.observe(viewLifecycleOwner) {
            val temp: String = it ?: ""
            if (temp != "") {
                val file = File("$photoDirectory/$temp.jpg")
                if (file.exists()) {
                    fileName = temp
                    lineOfSnippetPixels = renderSnippet(photoDirectory, fileName)
                }
            }
        }

        buildPatchSizeSpinner()
        buildDatasetSpinner(patchSize)
        buildLabelSpinner(listOfLabels)

        textLabel.text = getString(R.string.label)
        editLabel.setText("0")

        editLabel.setOnClickListener {
            if (dsSnippetOption == 0) showSaveHideDelete(true)
        }

        editLabel.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {

                if (s.isNotEmpty() && pixelIndex == 0) { // Label but not Pixel
                    var editLabelValue = -1
                    try {
                        editLabelValue = limitValue(s.toString().toInt(), 0, maxLabel)
                    } catch (_: IOException) {}

                    val spinnerLabelValue = spinnerLabel.getSelectedItem()
                    if (spinnerLabelValue == editLabelValue) return
                    if (editLabelValue > -1 && editLabelValue < listOfLabels.size) spinnerLabel.setSelection(editLabelValue)
                    if (editLabelValue >= listOfLabels.size) spinnerLabel.setSelection(listOfLabels.size) // "Other"
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        })

        fun scaleBack(click: Float): Int {
            return (click / scaleFactor - patchSize * 0.5f).roundToInt()
        }

        createMnist()
        textLabels = saveListOfLabels(textLabels, datasetName,
            arrayOf("zero","one","two","tree","four","five","six","seven","eight","nine"))

        btnSave.setOnClickListener {
            if (dsSnippetOption == 1) {
                val x = scaleBack(OverlayView.clickX)
                val y = scaleBack(OverlayView.clickY)

                val label = limitValue(stringToInteger(editLabel.text.toString()), -1, maxLabel)

                editLabel.setText(label.toString())
                val sb: StringBuilder = StringBuilder()
                sb.append(label)

                val size = yLayerLines(lineOfSnippetPixels)

                for (i in 0..< patchSize) {
                    val line = lineOfSnippetPixels[size - patchSize - x + i]
                    val pixels = line.split(",")
                    for (j in 0..< patchSize) {
                        val d = pixels[y + j]
                        sb.append(",$d")
                    }
                }
                sb.append("\n")

                var lineCount = 0
                try {
                    val file = File("$photoDirectory/$datasetName.csv")
                    if (file.exists()) {
                        file.forEachLine { lineCount++ } // to reference the freshly saved patch
                        patchIndex = lineCount
                        file.appendText(sb.toString())
                    } else {
                        file.writeText(sb.toString())
                        buildDatasetSpinner(patchSize) // to add a "NEW file" to the menu
                    }
                    textNew.text = "" // to suppress NEW if it was there
                    btnOption.visibility = View.VISIBLE
                    val msg = "record=$patchIndex x=$x y=$y + size=$patchSize"
                    Toast.makeText(safeContext, msg, Toast.LENGTH_SHORT).show()
                } catch (_: IOException) {}
            } else { // if (dsSnippetOption == 0)
                // edit label or pixel
                var value = stringToInteger(editLabel.text.toString())

                val tag = editLabelTag.text.toString()

                if (pixelIndex == 0 && value >= 0) { // Label but not Pixel
                    if (value < listOfLabels.size) {
                        listOfLabels[value] = tag
                    } else if (value == listOfLabels.size) {
                        listOfLabels.add(value,tag)
                    }
                    if (value < listOfLabels.size) { // listOfLabels.size increased by added element
                        buildLabelSpinner(listOfLabels)
                        spinnerLabel.setSelection(value)
                        textLabels = saveListOfLabels(textLabels, datasetName, listOfLabels.toTypedArray())
                    }
                }

                // check if value is in the allowed range
                if (pixelIndex == 0 && value < 0) value = 0

                // pixelToByteAsInt(value) // convert 255 to -128
                if (pixelIndex > 0) value = pixelToByteAsInt(value)

                editDataset(photoDirectory, datasetName, patchIndex, pixelIndex, value)

                if (pixelIndex > 0) renderNextPatch(0)

                //Toast.makeText(safeContext, "saved value $value", Toast.LENGTH_SHORT).show()
                showSaveHideDelete(false)
            }
        }

        btnDelete.setOnClickListener {
            deletePatch(photoDirectory, datasetName, patchIndex)
        }

        fun renderNextImage(step: Int = 1) {
            val size = fileNamesArray.size
            if (size > 0) {
                val currentFileIndex = fileNamesArray.indexOf(fileName)
                val nextItemIndex = (currentFileIndex + size + step) % size
                fileName = fileNamesArray[nextItemIndex]
                lineOfSnippetPixels = renderSnippet(photoDirectory, fileName)
                OverlayView.clickX = -1f
                overlayView.invalidate()
            }
        }

        renderNextImage()

        btnOption.setOnClickListener {
            dsSnippetOption = (dsSnippetOption + 1) % 2
            //var tint: Int
            if (dsSnippetOption == 0) { // 0=Dataset
                // tint = R.color.purple_500
                btnOption.setImageResource(R.drawable.crop_24)
                OverlayView.patchSize = 0 // scalePatch
                renderNextPatch(0)
            }
            else { // if (dsSnippetOption == 1) // 1=Snippet
                // tint = R.color.teal_700
                btnOption.setImageResource(R.drawable.edit_24)
                OverlayView.patchSize = patchSize
                textLabel.text = getString(R.string.label)
                renderNextImage(0)
            }
            showSaveHideDelete(dsSnippetOption == 1)
            showSpinnersHideLabelTitle(dsSnippetOption == 1)

            OverlayView.clickX = -1f
            overlayView.invalidate()
            //btnOption.setBackgroundTintList(ColorStateList.valueOf(resources.getColor(tint, null)))
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

        return root
    }

    private fun showSaveHideDelete(b: Boolean) {
        if (b) {
            btnSave.visibility = View.VISIBLE
            btnDelete.visibility = View.GONE
        } else {
            btnSave.visibility = View.GONE
            btnDelete.visibility = View.VISIBLE
        }
    }

    private fun showSpinnersHideLabelTitle(b: Boolean) {
        if (b) {
            spinnerPatchSize.visibility = View.VISIBLE
            spinnerDataset.visibility = View.VISIBLE
            textLabelTag.visibility = View.GONE
            editLabelTag.visibility = View.GONE
        } else {
            spinnerPatchSize.visibility = View.GONE
            spinnerDataset.visibility = View.GONE
            textLabelTag.visibility = View.VISIBLE
            editLabelTag.visibility = View.VISIBLE
        }
    }

    private fun showLabelTag(b: Boolean) {
        if (b) {
            textLabelTag.visibility = View.VISIBLE
            editLabelTag.visibility = View.VISIBLE
        } else {
            textLabelTag.visibility = View.INVISIBLE
            editLabelTag.visibility = View.INVISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun getListOfLabels(datasetLabels: String, datasetName: String): List<String> {
        // datasetLabels -> all datasets split by semicolons. Inndividual labels split by comas.
        // ds_1_28,zero,one;ds_784_28_mnist,zero,one,two,three,four,five,six,seven,eight,nine
        val dsList = datasetLabels.split(";")
        for (ds in dsList) {
            val dsNameLabelList = ds.split(",")
            if (dsNameLabelList[0] == datasetName) return dsNameLabelList.subList(1,dsNameLabelList.size)
        }
        return listOf("zero","one") //splitLabels(default).toMutableList()
    }

    private fun saveListOfLabels(datasetLabels: String, datasetName: String, listOfLabels: Array<String>): String {
        val sb: StringBuilder = StringBuilder()
        sb.append(datasetName)
        for (label in listOfLabels) sb.append(",$label")
        val dsList = datasetLabels.split(";")
        for (ds in dsList) {
            val dsNameLabelList = ds.split(",")
            if (dsNameLabelList[0] != datasetName) sb.append(";$ds")
        }
        val textLabels = sb.toString()
        savePreferencesString("dataset_labels", textLabels)
        return textLabels
    }

    private fun yLayerLines(lineOfSnippetPixels: Array<String>): Int {
        var size = lineOfSnippetPixels.size // potentially includes UV!!
        if (!fileName.startsWith("0")) size = size * 2 / 3
        return size
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun renderNextPatch(step: Int = 1) {
        try {
            val file = File("$photoDirectory/$datasetName.csv")
            if (file.exists()) {
                val temp = mutableListOf<String>()
                file.forEachLine { line -> temp.add(line) }
                listOfPatches = temp.toTypedArray() //getPatchesFromDataset(file)
            } else arrayOf("")
            val size = listOfPatches.size
            if (size > 0) {
                patchIndex = (patchIndex + size + step) % size
                renderPatch(listOfPatches[patchIndex])
            } else {
                renderPatch("")
            }

            textName.text = getString(R.string.dataset_patch_index, datasetName, patchIndex)
            textLabel.text = getString(R.string.label)
            showSaveHideDelete(false)
            spinnerLabel.visibility = View.VISIBLE
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

        val patchSizeAdapter = ArrayAdapter(safeContext, android.R.layout.simple_spinner_item, arrayOfPatchSizeNames)

        patchSizeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

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
                //savePreferencesInt("patch_size", patchSize)

                val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE) ?: return
                with (sharedPref.edit()) {
                    putInt("patch_size", patchSize)
                    apply()
                }

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
        listOfDatasets.add("") // a placeholder for a NEW file name if (patchSize > 0)
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
        // if there are some datasets other than NEW, offer one of those as default
        var index = arrayOfDatasets.indexOf(datasetName)
        val size = arrayOfDatasets.size
        if (index < 0 && size > 1) index = 1
        spinnerDataset.setSelection(index)

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

                listOfLabels = getListOfLabels(textLabels, datasetName).toMutableList()
                buildLabelSpinner(listOfLabels)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun buildLabelSpinner(lst: MutableList<String>) {

        val temp = lst.mapIndexed { idx, value -> "$idx $value" }.toMutableList()
        val ttt: MutableList<String> = temp
        ttt.add("Other")
        val arrayOfLabels = ttt.toTypedArray()

        // Create an ArrayAdapter using a simple spinner layout
        val labelAdapter =
            ArrayAdapter(safeContext, android.R.layout.simple_spinner_item, arrayOfLabels)

        // Set layout to use when the list of choices appear
        labelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        // Set Adapter to Spinner
        spinnerLabel.setAdapter(labelAdapter)

        spinnerLabel.onItemSelectedListener = object :
            AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val editLabelValue = stringToInteger(editLabel.text.toString())

                if (position == editLabelValue) return

                if (position < ttt.size - 1 || editLabelValue < ttt.size - 1) {
                    editLabel.setText(position.toString())
                    if (position < lst.size) {
                        editLabelTag.setText(lst[position])
                    } else {
                        editLabelTag.setText("")
                    }
                    if (dsSnippetOption == 0) showSaveHideDelete(true)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun renderSnippet(dir: String, file: String): Array<String> {

        val lineOfSnippetPixels = mutableListOf<String>()

        try {
            val fileCsv = File("$dir/$file.csv")

            if(fileCsv.exists()) {
                fileCsv.forEachLine { line ->
                    lineOfSnippetPixels.add(line)
                }
            }
        } catch (_: IOException) {}

        if (lineOfSnippetPixels.size < 1) return createDummySnippet()

        // to get snippet dimensions:
        val snippetY = lineOfSnippetPixels[0].split(",").size
        val snippetX = yLayerLines(lineOfSnippetPixels.toTypedArray())

        scaleFactor = calculateImageScale(snippetX, snippetY) //scale
        val step = scaleFactor
        OverlayView.scaleFactor = step

        val bitmap = Bitmap.createBitmap(snippetX * step, snippetY * step, Bitmap.Config.ARGB_8888)

        for ((x, line) in lineOfSnippetPixels.withIndex()) {
            val pixels = line.split(",")

            if (x == snippetX) break

            for (y in 0..< snippetY) {

                val color = Util.stringByteToColor(pixels[y], 0)
                //val size = step * step
                val intArray = IntArray(step * step) { color }

                bitmap.setPixels(intArray,0,step,(snippetX - 1 - x) * step, y * step, step, step)
            }
        }

        mImageView.setImageBitmap(bitmap)

        textName.text = file

        overlayView.setOnClickListener {} // to silence the listener set in renderPatch

        return lineOfSnippetPixels.toTypedArray()
    }

    private fun calculateImageScale(x: Int, y: Int): Int {
        val width: Int = context?.resources?.displayMetrics?.widthPixels ?: 0
        val height: Int = context?.resources?.displayMetrics?.heightPixels ?: 0
        val minDim = min(width, height)
        val maxSnippetDim = max(x, y)
        return if (maxSnippetDim > 0) (minDim * 4 / 5) / maxSnippetDim else 1
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun renderPatch(patch: String): Array<String> {

        val temp = patchSize * patchSize + 1
        var pixels = (Array(temp) { "0" }).toList()
        try {
            if (patch != "") pixels = patch.split(",")
        } catch (_: IOException) {}

        //Toast.makeText(safeContext, "pixels.size = ${pixels.size}", Toast.LENGTH_SHORT).show()

        scalePatch = calculateImageScale(patchSize, patchSize) //scale
        val step = scalePatch
        OverlayView.scaleFactor = step

        val bitmap = Bitmap.createBitmap(patchSize * step, patchSize * step, Bitmap.Config.ARGB_8888)

        for (x in 0..< patchSize) {

            for (y in 0..< patchSize) {

                val color = Util.stringByteToColor(pixels[1 + x * patchSize + y], 0)
                //val size = step * step
                val intArray = IntArray(step * step) { color }

                bitmap.setPixels(intArray,0,step,(patchSize - 1 - x) * step, y * step, step, step)
            }
        }

        mImageView.setImageBitmap(bitmap)

        editLabel.setText(pixels[0])

        val labelValue: Int = stringToInteger(pixels[0], 0)
        if (labelValue < listOfLabels.size) {
            editLabelTag.setText(listOfLabels[labelValue])
            //showLabelTag(true)
        } else if (labelValue == listOfLabels.size) {
            editLabelTag.setText("")
            //showLabelTag(true)
        } //else {
            //showLabelTag(false)
        //}
        showLabelTag(labelValue <= listOfLabels.size)

        OverlayView.patchSize = 0
        overlayView.invalidate()

        overlayView.setOnClickListener {
            spinnerLabel.visibility = View.GONE
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
                file.writeText(sb.toString())
                renderNextPatch(0)
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

    private fun getFileNames(dir: String, fileNameExtension: String = "jpg", fileNamePrefix: String = ""): Array<String> {
        val filesAll = File(dir).listFiles() ?: return arrayOf() //arrayOf("")
        filesAll.sort()

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

//    private fun getPreferencesInt(key: String, value: Int = 0): Int {
//        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE)
//        return sharedPref?.getInt(key, value) ?: value
//    }

//    private fun savePreferencesInt(key: String, value: Int) {
//        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE) ?: return
//        with (sharedPref.edit()) {
//            putInt(key, value)
//            apply()
//        }
//    }

//    private fun getPreferencesString(key: String, value: String = "ds_01_s28"): String {
//        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE)
//        return sharedPref?.getString(key, value) ?: value
//    }

    private fun savePreferencesString(key: String, value: String) {
        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE) ?: return
        with (sharedPref.edit()) {
            putString(key, value)
            apply()
        }
    }

    private fun createDummySnippet(): Array<String> {
        val s = "0,0,0,0"
        return arrayOf(s, s, s, s)
    }

    private fun createMnist() {
        val sb: StringBuilder = StringBuilder()
        try {
            val file = File("$photoDirectory/ds_784_28_mnist.csv")
            if (!file.exists()) {
                sb.append(transpose("7,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,84,185,159,151,60,36,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,222,254,254,254,254,241,198,198,198,198,198,198,198,198,170,52,0,0,0,0,0,0,0,0,0,0,0,0,67,114,72,114,163,227,254,225,254,254,254,250,229,254,254,140,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,17,66,14,67,67,67,59,21,236,254,106,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,83,253,209,18,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,22,233,255,83,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,129,254,238,44,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,59,249,254,62,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,133,254,187,5,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,9,205,248,58,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,126,254,182,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,75,251,240,57,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,19,221,254,166,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,3,203,254,219,35,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,38,254,254,77,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,31,224,254,115,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,133,254,254,52,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,61,242,254,254,52,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,121,254,254,219,40,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,121,254,207,18,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0"))
                sb.append("\n")
                sb.append(transpose("2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,116,125,171,255,255,150,93,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,169,253,253,253,253,253,253,218,30,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,169,253,253,253,213,142,176,253,253,122,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,52,250,253,210,32,12,0,6,206,253,140,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,77,251,210,25,0,0,0,122,248,253,65,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,31,18,0,0,0,0,209,253,253,65,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,117,247,253,198,10,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,76,247,253,231,63,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,128,253,253,144,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,176,246,253,159,12,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,25,234,253,233,35,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,198,253,253,141,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,78,248,253,189,12,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,19,200,253,253,141,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,134,253,253,173,12,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,248,253,253,25,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,248,253,253,43,20,20,20,20,5,0,5,20,20,37,150,150,150,147,10,0,0,0,0,0,0,0,0,0,248,253,253,253,253,253,253,253,168,143,166,253,253,253,253,253,253,253,123,0,0,0,0,0,0,0,0,0,174,253,253,253,253,253,253,253,253,253,253,253,249,247,247,169,117,117,57,0,0,0,0,0,0,0,0,0,0,118,123,123,123,166,253,253,253,155,123,123,41,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0"))
                sb.append("\n")
                sb.append(transpose("1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,38,254,109,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,87,252,82,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,135,241,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,45,244,150,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,84,254,63,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,202,223,11,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,32,254,216,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,95,254,195,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,140,254,77,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,57,237,205,8,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,124,255,165,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,171,254,81,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,24,232,215,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,120,254,159,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,151,254,142,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,228,254,66,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,61,251,254,66,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,141,254,205,3,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,10,215,254,121,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,5,198,176,10,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0"))
                sb.append("\n")
                sb.append(transpose("0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,11,150,253,202,31,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,37,251,251,253,107,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,21,197,251,251,253,107,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,110,190,251,251,251,253,169,109,62,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,253,251,251,251,251,253,251,251,220,51,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,182,255,253,253,253,253,234,222,253,253,253,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,63,221,253,251,251,251,147,77,62,128,251,251,105,0,0,0,0,0,0,0,0,0,0,0,0,0,0,32,231,251,253,251,220,137,10,0,0,31,230,251,243,113,5,0,0,0,0,0,0,0,0,0,0,0,0,37,251,251,253,188,20,0,0,0,0,0,109,251,253,251,35,0,0,0,0,0,0,0,0,0,0,0,0,37,251,251,201,30,0,0,0,0,0,0,31,200,253,251,35,0,0,0,0,0,0,0,0,0,0,0,0,37,253,253,0,0,0,0,0,0,0,0,32,202,255,253,164,0,0,0,0,0,0,0,0,0,0,0,0,140,251,251,0,0,0,0,0,0,0,0,109,251,253,251,35,0,0,0,0,0,0,0,0,0,0,0,0,217,251,251,0,0,0,0,0,0,21,63,231,251,253,230,30,0,0,0,0,0,0,0,0,0,0,0,0,217,251,251,0,0,0,0,0,0,144,251,251,251,221,61,0,0,0,0,0,0,0,0,0,0,0,0,0,217,251,251,0,0,0,0,0,182,221,251,251,251,180,0,0,0,0,0,0,0,0,0,0,0,0,0,0,218,253,253,73,73,228,253,253,255,253,253,253,253,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,113,251,251,253,251,251,251,251,253,251,251,251,147,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,31,230,251,253,251,251,251,251,253,230,189,35,10,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,62,142,253,251,251,251,251,253,107,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,72,174,251,173,71,72,30,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0"))
                sb.append("\n")
                sb.append(transpose("4,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,50,224,0,0,0,0,0,0,0,70,29,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,121,231,0,0,0,0,0,0,0,148,168,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,4,195,231,0,0,0,0,0,0,0,96,210,11,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,69,252,134,0,0,0,0,0,0,0,114,252,21,0,0,0,0,0,0,0,0,0,0,0,0,0,0,45,236,217,12,0,0,0,0,0,0,0,192,252,21,0,0,0,0,0,0,0,0,0,0,0,0,0,0,168,247,53,0,0,0,0,0,0,0,18,255,253,21,0,0,0,0,0,0,0,0,0,0,0,0,0,84,242,211,0,0,0,0,0,0,0,0,141,253,189,5,0,0,0,0,0,0,0,0,0,0,0,0,0,169,252,106,0,0,0,0,0,0,0,32,232,250,66,0,0,0,0,0,0,0,0,0,0,0,0,0,15,225,252,0,0,0,0,0,0,0,0,134,252,211,0,0,0,0,0,0,0,0,0,0,0,0,0,0,22,252,164,0,0,0,0,0,0,0,0,169,252,167,0,0,0,0,0,0,0,0,0,0,0,0,0,0,9,204,209,18,0,0,0,0,0,0,22,253,253,107,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,169,252,199,85,85,85,85,129,164,195,252,252,106,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,41,170,245,252,252,252,252,232,231,251,252,252,9,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,49,84,84,84,84,0,0,161,252,252,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,127,252,252,45,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,128,253,253,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,127,252,252,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,135,252,244,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,232,236,111,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,179,66,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0\n"))
                sb.append("\n")

//                sb.append("7,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,52,140,106,18,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,170,254,254,209,83,44,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,198,254,236,253,255,238,62,5,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,198,229,21,83,233,254,254,187,58,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,198,250,59,0,22,129,249,254,248,182,57,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,198,254,67,0,0,0,59,133,205,254,240,166,35,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,198,254,67,0,0,0,0,0,9,126,251,254,219,77,1,0,0,0,0,0,0,0,0,0,0,0,0,0,198,254,67,0,0,0,0,0,0,0,75,221,254,254,115,52,52,40,0,0,0,0,0,0,0,0,0,0,198,225,14,0,0,0,0,0,0,0,0,19,203,254,254,254,254,219,18,0,0,0,0,0,0,0,0,0,198,254,66,0,0,0,0,0,0,0,0,0,3,38,224,254,254,254,207,0,0,0,0,0,0,0,0,36,241,227,17,0,0,0,0,0,0,0,0,0,0,0,31,133,242,254,254,0,0,0,0,0,0,0,0,60,254,163,0,0,0,0,0,0,0,0,0,0,0,0,0,0,61,121,121,0,0,0,0,0,0,0,0,151,254,114,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,159,254,72,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,185,254,114,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,84,222,67,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0\n")
//                sb.append("2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,10,123,57,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,147,253,117,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,150,253,117,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,150,253,169,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,150,253,247,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,37,253,247,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,20,253,249,41,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,20,253,253,123,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,5,166,253,123,0,0,0,0,0,0,0,0,0,30,122,140,65,65,10,0,0,0,0,0,0,0,0,0,0,143,253,155,0,0,0,0,0,0,0,0,93,218,253,253,253,253,198,63,0,0,0,0,0,0,0,0,5,168,253,253,0,0,0,0,0,0,0,0,150,253,253,206,248,253,253,231,144,12,0,0,0,0,0,0,20,253,253,253,0,0,0,0,0,0,0,0,255,253,176,6,122,209,247,253,253,159,35,0,0,0,0,0,20,253,253,253,0,0,0,0,0,0,0,0,255,253,142,0,0,0,117,247,253,253,233,141,12,0,0,0,20,253,253,166,0,0,0,0,0,0,0,0,171,253,213,12,0,0,0,76,128,246,253,253,189,141,12,0,20,253,253,123,0,0,0,0,0,0,0,0,125,253,253,32,0,0,0,0,0,176,234,253,253,253,173,25,43,253,253,123,0,0,0,0,0,0,0,0,116,253,253,210,25,0,0,0,0,0,25,198,248,253,253,253,253,253,253,123,0,0,0,0,0,0,0,0,0,169,253,253,210,18,0,0,0,0,0,0,78,200,253,253,253,253,253,118,0,0,0,0,0,0,0,0,0,0,169,250,251,31,0,0,0,0,0,0,0,19,134,248,248,248,174,0,0,0,0,0,0,0,0,0,0,0,0,52,77,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0\n")
//                sb.append("1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,109,82,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,254,252,241,150,63,11,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,38,87,135,244,254,223,216,195,77,8,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,45,84,202,254,254,254,205,165,81,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,32,95,140,237,255,254,215,159,142,66,66,3,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,57,124,171,232,254,254,254,254,205,121,10,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,24,120,151,228,251,254,254,176,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,61,141,215,198,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,10,5,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0\n")
//                sb.append("0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,5,35,35,164,35,30,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,113,251,251,253,251,230,61,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,105,243,253,253,255,253,253,221,180,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,51,253,251,251,251,200,202,251,251,251,251,253,147,10,0,0,0,0,0,0,0,0,0,0,0,0,0,62,220,253,251,230,109,31,32,109,231,251,251,253,251,35,0,0,0,0,0,0,0,0,0,0,0,0,0,109,251,253,128,31,0,0,0,0,63,251,251,253,251,189,0,0,0,0,0,0,0,0,0,0,31,107,107,169,251,222,62,0,0,0,0,0,21,144,221,253,251,230,107,30,0,0,0,0,0,0,0,0,202,253,253,253,253,234,77,0,0,0,0,0,0,0,182,255,253,253,253,72,0,0,0,0,0,0,0,0,253,251,251,251,251,253,147,10,0,0,0,0,0,0,0,253,251,251,251,71,0,0,0,0,0,0,0,0,150,251,251,251,251,253,251,137,0,0,0,0,0,0,0,253,251,251,251,173,0,0,0,0,0,0,0,0,11,37,197,251,251,253,251,220,20,0,0,0,0,0,0,228,251,251,251,251,0,0,0,0,0,0,0,0,0,0,21,190,251,253,251,251,188,30,0,0,0,0,0,73,251,251,251,174,0,0,0,0,0,0,0,0,0,0,0,110,253,255,253,253,253,201,0,0,0,0,0,73,253,253,253,72,0,0,0,0,0,0,0,0,0,0,0,0,0,182,221,251,251,251,253,251,251,251,251,253,251,251,142,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,63,231,251,251,253,251,251,251,251,253,251,230,62,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,32,37,37,37,140,217,217,217,218,113,31,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0\n")
                file.writeText(sb.toString())
                //savePreferencesString("dataset_labels", "ds_784_28_mnist,zero,one,two,three,four,five,six,seven,eight,nine")
                Toast.makeText(safeContext, "mnist created", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(safeContext, "mnist exists", Toast.LENGTH_SHORT).show()
            }
        } catch (_: IOException) {}
    }

    private fun transpose(s: String): String {
        val sList = s.split(",")
        val sb: StringBuilder = StringBuilder()
        sb.append(sList[0])
        for (i in 27 downTo 0) {//27 downTo 0
            for (j in 0..27) {
                val index = limitValue(1 + j * 28 + i, 1, 784)
                sb.append(",${ sList[index] }") // (27 - j)
            }
        }
        return sb.toString()
    }

}