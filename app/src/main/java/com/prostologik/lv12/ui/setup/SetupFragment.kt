package com.prostologik.lv12.ui.setup

import android.content.Context
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.prostologik.lv12.R
import com.prostologik.lv12.Util
import com.prostologik.lv12.databinding.FragmentSetupBinding
import com.prostologik.lv12.ui.home.HomeViewModel
import kotlin.math.absoluteValue

class SetupFragment : Fragment() {
    private var _binding: FragmentSetupBinding? = null
    private val binding get() = _binding!!

    private val homeViewModel: HomeViewModel by activityViewModels()

    private lateinit var safeContext: Context

    override fun onAttach(context: Context) {
        super.onAttach(context)
        safeContext = context
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        var snippetLayer = homeViewModel.snippetLayer
//        var patchWidth = homeViewModel.patchWidth
//        var patchHeight = homeViewModel.patchHeight

        _binding = FragmentSetupBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textHome = binding.textHome
        val textBottom = binding.textBottom

        val radioGroupLayers = binding.radioGroupLayers
        val radioLayerY = binding.radioLayerY
        val radioLayerU = binding.radioLayerU
        val radioLayerV = binding.radioLayerV

        radioGroupLayers.setOnCheckedChangeListener { _, _ -> // _, checkedId ->
            snippetLayer = if (radioLayerU.isChecked) { 1 } else if (radioLayerV.isChecked) { 2 } else { 0 }
            homeViewModel.snippetLayer = snippetLayer
            savePreferences("saved_layer", snippetLayer)
        }

        fun setLayerChecked(y: Boolean, u: Boolean, v: Boolean) {
            radioLayerY.setChecked(y)
            radioLayerU.setChecked(u)
            radioLayerV.setChecked(v)
        }

        when (snippetLayer) {
            0 -> setLayerChecked(true, false, false)
            1 -> setLayerChecked(false, true, false)
            2 -> setLayerChecked(false, false, true)
        }


        ///////////////// camera setup

        var resolutionWidth = homeViewModel.resolutionWidth//.value ?: 800//640
        var resolutionHeight = homeViewModel.resolutionHeight//.value ?: 600//480

        // KILL THE BELOW

        resolutionWidth = Util.limitValue(resolutionWidth, 1, 12800)

        val editResolutionWidth = binding.editResolutionWidth
        val editResolutionHeight = binding.editResolutionHeight

        editResolutionWidth.setText(resolutionWidth.toString())
        editResolutionWidth.addTextChangedListener {
            resolutionWidth = Util.stringToInteger(editResolutionWidth.text.toString())
            resolutionWidth = Util.limitValue(resolutionWidth, 1, 12800)
            homeViewModel.resolutionWidth = resolutionWidth
            savePreferences("resolution_width", resolutionWidth)

            resolutionHeight = (resolutionWidth * 3) / 4
            homeViewModel.resolutionHeight = resolutionHeight
            savePreferences("resolution_height", resolutionHeight)
            editResolutionHeight.setText(resolutionHeight.toString())
        }

        editResolutionHeight.setText(resolutionHeight.toString())
        editResolutionHeight.addTextChangedListener {
            resolutionHeight = Util.stringToInteger(editResolutionHeight.text.toString())
            resolutionHeight = Util.limitValue(resolutionHeight, 1, 9600)
            homeViewModel.resolutionHeight = resolutionHeight
            savePreferences("resolution_height", resolutionHeight)
        }


        // OutputSize spinner:

        //val arrayOfItemNames = arrayOf( "352 x 288", "640 x 480", "800 x 600" )

        val arrayOutputWidth = homeViewModel.arrayOutputWidth
        val arrayOutputHeight = homeViewModel.arrayOutputHeight

        val listOutputSize = mutableListOf<String>()

        for (i in arrayOutputWidth.indices) {
            val t = arrayOutputWidth[i].toString() + " - " + arrayOutputHeight[i].toString()
            listOutputSize.add(t)
        }
        val arrayOutputSize = listOutputSize.toTypedArray()

        // get an index of the width closest to homeViewModel.arrayOutputWidth

        var bestFitWidth = arrayOutputWidth[0]
        arrayOutputWidth.forEach {
            if ((it - resolutionWidth).absoluteValue < (bestFitWidth - resolutionWidth).absoluteValue) {
            bestFitWidth = it
        } }
        val outputSizeIndex = arrayOutputWidth.indexOf(bestFitWidth)



        val spinnerOutputSize: Spinner = binding.spinnerOutputSize

        // Create an ArrayAdapter using a simple spinner layout
        val aa = ArrayAdapter(safeContext, android.R.layout.simple_spinner_item, arrayOutputSize)

        // Set layout to use when the list of choices appear
        aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        // Set Adapter to Spinner
        spinnerOutputSize.setAdapter(aa)
        spinnerOutputSize.setSelection(outputSizeIndex)

        spinnerOutputSize.onItemSelectedListener = object:
            AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                resolutionWidth = arrayOutputWidth[position]
                resolutionHeight = arrayOutputHeight[position]
                homeViewModel.resolutionWidth = resolutionWidth
                homeViewModel.resolutionHeight = resolutionHeight
                savePreferences("resolution_width", resolutionWidth)
                savePreferences("resolution_height", resolutionHeight)
                editResolutionWidth.setText(resolutionWidth.toString())
                editResolutionHeight.setText(resolutionHeight.toString())

                if (resolutionWidth < 1) resolutionWidth = 1
                val ratio2 = Util.limitValue((resolutionHeight * 100) / resolutionWidth, 30, 300)
                //val textHome = binding.textHome
                textHome.text = "$position: W = $resolutionWidth; H = $resolutionHeight; H / W = $ratio2%"
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                homeViewModel.resolutionWidth = 640
                homeViewModel.resolutionHeight = 480
                val wLoc = homeViewModel.resolutionWidth
                val hLoc = homeViewModel.resolutionHeight
                //val textHome = binding.textHome
                textHome.text = "NothingSelected: W x H = $wLoc x $hLoc"
            }

        }


        // Info spinner:

        val spinnerInfoOption: Spinner = binding.spinnerInfoOption

        // Create an ArrayAdapter using the string array and a default spinner layout.
        ArrayAdapter.createFromResource(
            safeContext,
            R.array.info_options,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears.
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner.
            spinnerInfoOption.adapter = adapter

            spinnerInfoOption.onItemSelectedListener = object:
                AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    if (id.toInt() == 0) {
                        textBottom.text = "listCameras(cameraProvider):  " + homeViewModel.cameraInfo
                    } else {
                        textBottom.text = "photoDirectory:  " + homeViewModel.photoDirectory
                    }
                    textBottom.movementMethod = ScrollingMovementMethod()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    textBottom.text = "there is no info to display"
                }

            }
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun savePreferences(key: String, value: Int = 0) {
        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE) ?: return
        with (sharedPref.edit()) {
            putInt(key, value)
            apply()
        }
    }

}