package com.prostologik.lv12.ui.setup

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

class SetupFragment : Fragment() {
    private var _binding: FragmentSetupBinding? = null
    private val binding get() = _binding!!

    private val homeViewModel: HomeViewModel by activityViewModels()

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

}