package com.prostologik.lv12.ui.setup

import android.content.Context
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.prostologik.lv12.R
import com.prostologik.lv12.Util
import com.prostologik.lv12.databinding.FragmentSetupBinding
import com.prostologik.lv12.ui.home.HomeViewModel
import com.prostologik.lv12.ui.home.OverlayView

class SetupFragment : Fragment() { // , AdapterView.OnItemSelectedListener
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

        _binding = FragmentSetupBinding.inflate(inflater, container, false)
        val root: View = binding.root






        var analyzerOption = homeViewModel.analyzerOption.value ?: 0
        val textView: TextView = binding.textSetup
        textView.text = "option: $analyzerOption"

        val radioAnalyzer = binding.radioAnalyzer
        val radioSnippet = binding.radioSnippet
        if (analyzerOption == 0) {
            radioAnalyzer.isChecked = true
            radioSnippet.isChecked = false
        } else {
            radioAnalyzer.isChecked = false
            radioSnippet.isChecked = true
        }

        val radioGroupOption = binding.radioGroupOption
        radioGroupOption.setOnCheckedChangeListener { _, checkedId ->
            analyzerOption = if (checkedId == R.id.radioAnalyzer) { 0 } else { 1 }
            homeViewModel.setAnalyzerOption(analyzerOption)
            savePreferences("analyzer_option", analyzerOption)
            textView.text = "option: $analyzerOption"
        }





        var snippetWidth = homeViewModel.snippetWidth.value ?: 64
        var snippetHeight = homeViewModel.snippetHeight.value ?: 64
        var snippetLayer = homeViewModel.snippetLayer.value ?: 0

        val editSnippetWidth = binding.editSnippetWidth
        val editSnippetHeight = binding.editSnippetHeight
        editSnippetWidth.setText(snippetWidth.toString())
        editSnippetWidth.addTextChangedListener {
            snippetWidth = Util.stringToInteger(editSnippetWidth.text.toString(), 64)
            snippetWidth = Util.limitValue(snippetWidth, 1, 256)
            homeViewModel.setSnippetWidth(snippetWidth)
            OverlayView.snippetWidth = snippetWidth
            savePreferences("snippet_width", snippetWidth)

            snippetHeight = snippetWidth
            homeViewModel.setSnippetHeight(snippetHeight)
            OverlayView.snippetHeight = snippetHeight
            savePreferences("snippet_height", snippetHeight)
            editSnippetHeight.setText(snippetHeight.toString())
        }

        editSnippetHeight.setText(snippetHeight.toString())
        editSnippetHeight.addTextChangedListener {
            snippetHeight = Util.stringToInteger(editSnippetHeight.text.toString(), 64)
            snippetHeight = Util.limitValue(snippetHeight, 1, 256)
            homeViewModel.setSnippetHeight(snippetHeight)
            OverlayView.snippetHeight = snippetHeight
            savePreferences("snippet_height", snippetHeight)
        }

        val editSnippetLayer = binding.editSnippetLayer
        editSnippetLayer.setText(snippetLayer.toString())
        editSnippetLayer.addTextChangedListener {
            snippetLayer = Util.stringToInteger(editSnippetLayer.text.toString())
            snippetLayer = Util.limitValue(snippetLayer, 0, 2)
            homeViewModel.setSnippetLayer(snippetLayer)
            savePreferences("saved_layer", snippetLayer)
        }


        var resolutionWidth = homeViewModel.resolutionWidth.value ?: 800//640
        var resolutionHeight = homeViewModel.resolutionHeight.value ?: 600//480
        resolutionWidth = Util.limitValue(resolutionWidth, 1, 12800)
        var ratio = Util.limitValue((resolutionHeight * 100) / resolutionWidth, 30, 300)

        val editResolutionWidth = binding.editResolutionWidth
        val editResolutionHeight = binding.editResolutionHeight
        val editRatio = binding.editRatio
        editRatio.setText(ratio.toString())
        editRatio.addTextChangedListener {
            ratio = Util.stringToInteger(editRatio.text.toString())
            ratio = Util.limitValue(ratio, 30, 300)
        }

        editResolutionWidth.setText(resolutionWidth.toString())
        editResolutionWidth.addTextChangedListener {
            resolutionWidth = Util.stringToInteger(editResolutionWidth.text.toString())
            resolutionWidth = Util.limitValue(resolutionWidth, 1, 12800)
            homeViewModel.setResolutionWidth(resolutionWidth)
            savePreferences("resolution_width", resolutionWidth)

            resolutionHeight = Util.limitValue((resolutionWidth * ratio) / 100, 1, 9600)
            homeViewModel.setResolutionHeight(resolutionHeight)
            savePreferences("resolution_height", resolutionHeight)
            editResolutionHeight.setText(resolutionHeight.toString())
        }

        editResolutionHeight.setText(resolutionHeight.toString())
        editResolutionHeight.addTextChangedListener {
            resolutionHeight = Util.stringToInteger(editResolutionHeight.text.toString())
            resolutionHeight = Util.limitValue(resolutionHeight, 1, 9600)
            homeViewModel.setResolutionHeight(resolutionHeight)
            savePreferences("resolution_height", resolutionHeight)
        }



        var infoOption = 0
        val textBottom = binding.textBottom
        val spinnerInfoOption: Spinner = binding.spinnerInfoOption
        spinnerInfoOption.setSelection(infoOption)
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
                    var info = "dummy text"
                    if (id.toInt() == 0) {
                        info = homeViewModel.info
                    }
                    textBottom.text = "id=$id position=$position info=$info"
                    textBottom.movementMethod = ScrollingMovementMethod()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {

                }

            }
        }



        //textBottom.setText("test TextBottom")


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

//    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
////        TODO("Not yet implemented")
//        // An item is selected. You can retrieve the selected item using
//        // parent.getItemAtPosition(pos).
//        homeViewModel.setAnalyzerOption(id.toInt())
//        savePreferences("analyzer_option", id.toInt())
//
//        val editAnalyzerOption = binding.editAnalyzerOption
//        editAnalyzerOption.setText(id.toString())
//
//    }
//
//    override fun onNothingSelected(parent: AdapterView<*>?) {
////        TODO("Not yet implemented")
//    }

}