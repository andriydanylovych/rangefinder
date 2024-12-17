package com.prostologik.lv12.ui.setup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.prostologik.lv12.Util
import com.prostologik.lv12.databinding.FragmentSetupBinding
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

        var snippetWidth = 64
        var snippetHeight = 64
        var snippetLayer = 0
        textView.text = textWidthHeight(snippetWidth, snippetHeight, snippetLayer)

        val editSnippetWidth = binding.editSnippetWidth
        editSnippetWidth.setText(snippetWidth.toString())
        editSnippetWidth.addTextChangedListener {
            snippetWidth = Util.stringToInteger(editSnippetWidth.text.toString(), 64)
            if (snippetWidth > 256) snippetWidth = 256
            if (snippetWidth < 1) snippetWidth = 1
            homeViewModel.setSnippetWidth(snippetWidth)
            textView.text = textWidthHeight(snippetWidth, snippetHeight, snippetLayer)
            OverlayView.snippetWidth = snippetWidth
        }

        val editSnippetHeight = binding.editSnippetHeight
        editSnippetHeight.setText(snippetHeight.toString())
        editSnippetHeight.addTextChangedListener {
            snippetHeight = Util.stringToInteger(editSnippetHeight.text.toString(), 64)
            if (snippetHeight > 256) snippetHeight = 256
            if (snippetHeight < 1) snippetHeight = 1
            homeViewModel.setSnippetHeight(snippetHeight)
            textView.text = textWidthHeight(snippetWidth, snippetHeight, snippetLayer)
            OverlayView.snippetHeight = snippetHeight
        }

        val editSnippetLayer = binding.editSnippetLayer
        editSnippetLayer.setText(snippetLayer.toString())
        editSnippetLayer.addTextChangedListener {
            snippetLayer = Util.stringToInteger(editSnippetLayer.text.toString())
            if (snippetLayer > 2) snippetLayer = 2
            if (snippetLayer < 0) snippetLayer = 0
            homeViewModel.setSnippetLayer(snippetLayer)
            textView.text = textWidthHeight(snippetWidth, snippetHeight, snippetLayer)
        }

        return root
    }

    private fun textWidthHeight(w: Int, h: Int, l: Int): String {
        return "W x H = $w x $h  L = $l"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}