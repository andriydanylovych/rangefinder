package com.prostologik.lv12.ui.setup

import android.app.Activity
import android.content.Context
import android.os.Bundle
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
import com.prostologik.lv12.ui.home.HomeFragment
import com.prostologik.lv12.ui.home.HomeFragment.Companion
import com.prostologik.lv12.ui.home.HomeViewModel
import com.prostologik.lv12.ui.home.OverlayView

class SetupFragment : Fragment(), AdapterView.OnItemSelectedListener {
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

        val textView: TextView = binding.textSetup

        var snippetWidth = homeViewModel.snippetWidth.value ?: 64
        var snippetHeight = homeViewModel.snippetHeight.value ?: 64
        var snippetLayer = homeViewModel.snippetLayer.value ?: 0

        var analyzerOption = homeViewModel.analyzerOption.value ?: 0
        var resolutionWidth = homeViewModel.resolutionWidth.value ?: 640
        var resolutionHeight = homeViewModel.resolutionHeight.value ?: 480

        textView.text = textWidthHeight(snippetWidth, snippetHeight, snippetLayer)

        val editSnippetWidth = binding.editSnippetWidth
        editSnippetWidth.setText(snippetWidth.toString())
        editSnippetWidth.addTextChangedListener {
            snippetWidth = Util.stringToInteger(editSnippetWidth.text.toString(), 64)
            snippetWidth = Util.limitValue(snippetWidth, 1, 256)
            homeViewModel.setSnippetWidth(snippetWidth)
            textView.text = textWidthHeight(snippetWidth, snippetHeight, snippetLayer)
            OverlayView.snippetWidth = snippetWidth
            savePreferences("snippet_width", snippetWidth)
        }

        val editSnippetHeight = binding.editSnippetHeight
        editSnippetHeight.setText(snippetHeight.toString())
        editSnippetHeight.addTextChangedListener {
            snippetHeight = Util.stringToInteger(editSnippetHeight.text.toString(), 64)
            snippetHeight = Util.limitValue(snippetHeight, 1, 256)
            homeViewModel.setSnippetHeight(snippetHeight)
            textView.text = textWidthHeight(snippetWidth, snippetHeight, snippetLayer)
            OverlayView.snippetHeight = snippetHeight
            savePreferences("snippet_height", snippetHeight)
        }

        val editSnippetLayer = binding.editSnippetLayer
        editSnippetLayer.setText(snippetLayer.toString())
        editSnippetLayer.addTextChangedListener {
            snippetLayer = Util.stringToInteger(editSnippetLayer.text.toString())
            snippetLayer = Util.limitValue(snippetLayer, 0, 2)
            homeViewModel.setSnippetLayer(snippetLayer)
            textView.text = textWidthHeight(snippetWidth, snippetHeight, snippetLayer)
            savePreferences("saved_layer", snippetLayer) // getString(R.string.saved_layer)
        }

        val editAnalyzerOption = binding.editAnalyzerOption
        editAnalyzerOption.setText(analyzerOption.toString())
        editAnalyzerOption.addTextChangedListener {
            analyzerOption = Util.stringToInteger(editAnalyzerOption.text.toString())
            analyzerOption = Util.limitValue(analyzerOption, 0, 2)
            homeViewModel.setAnalyzerOption(analyzerOption)
            savePreferences("analyzer_option", analyzerOption)
        }

        val editResolutionWidth = binding.editResolutionWidth
        editResolutionWidth.setText(resolutionWidth.toString())
        editResolutionWidth.addTextChangedListener {
            resolutionWidth = Util.stringToInteger(editResolutionWidth.text.toString())
            resolutionWidth = Util.limitValue(resolutionWidth, 1, 12800)
            homeViewModel.setResolutionWidth(resolutionWidth)
            savePreferences("resolution_width", resolutionWidth)
        }

        val editResolutionHeight = binding.editResolutionHeight
        editResolutionHeight.setText(resolutionHeight.toString())
        editResolutionHeight.addTextChangedListener {
            resolutionHeight = Util.stringToInteger(editResolutionHeight.text.toString())
            resolutionHeight = Util.limitValue(resolutionHeight, 1, 9600)
            homeViewModel.setResolutionHeight(resolutionHeight)
            savePreferences("resolution_height", resolutionHeight)
        }

        val spinnerAnalyzerOption: Spinner = binding.spinnerAnalyzerOption
        // Create an ArrayAdapter using the string array and a default spinner layout.
        ArrayAdapter.createFromResource(
            safeContext,
            R.array.analyzer_options,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears.
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner.
            spinnerAnalyzerOption.adapter = adapter
        }

        spinnerAnalyzerOption.onItemSelectedListener = this

        return root
    }

    private fun textWidthHeight(w: Int? = 64, h: Int? = 64, l: Int? = 0): String {
        return "W x H = $w x $h  L = $l"
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

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
//        TODO("Not yet implemented")
        // An item is selected. You can retrieve the selected item using
        // parent.getItemAtPosition(pos).
        val editAnalyzerOption = binding.editAnalyzerOption
        editAnalyzerOption.setText(id.toString())

        homeViewModel.setAnalyzerOption(id.toInt())
        savePreferences("analyzer_option", id.toInt())
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
//        TODO("Not yet implemented")
    }

}