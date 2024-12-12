package com.prostologik.lv12.ui.setup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.prostologik.lv12.databinding.FragmentSetupBinding
import com.prostologik.lv12.ui.home.HomeViewModel

class SetupFragment : Fragment() {
    private var _binding: FragmentSetupBinding? = null
    private val binding get() = _binding!!

    private val homeViewModel: HomeViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        //val slideshowViewModel = ViewModelProvider(this).get(ObjectsViewModel::class.java)

        _binding = FragmentSetupBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textSetup

        var snippetWidth = 64
        var snippetHeight = 1
        textView.text = textWidthHeight(snippetWidth, snippetHeight)

        val editSnippetWidth = binding.editSnippetWidth
        //editSnippetWidth.setText(snippetWidth)
        editSnippetWidth.addTextChangedListener {
            snippetWidth = stringToInteger(editSnippetWidth.text.toString())
            if (snippetWidth > 256) snippetWidth = 256
            if (snippetWidth < 1) snippetWidth = 1
            homeViewModel.setSnippetWidth(snippetWidth)
            textView.text = textWidthHeight(snippetWidth, snippetHeight)
        }

        val editSnippetHeight = binding.editSnippetHeight
        //editSnippetHeight.setText(snippetHeight)
        editSnippetHeight.addTextChangedListener {
            snippetHeight = stringToInteger(editSnippetHeight.text.toString())
            if (snippetHeight > 256) snippetHeight = 256
            if (snippetHeight < 1) snippetHeight = 1
            homeViewModel.setSnippetHeight(snippetHeight)
            textView.text = textWidthHeight(snippetWidth, snippetHeight)
        }

        return root
    }

    private fun textWidthHeight(w: Int, h: Int): String {
        return "W x H = $w x $h"
    }

    private fun stringToInteger(s: String): Int {
        try {
            return s.toInt()
        } catch (nfe: NumberFormatException) {
            return 32
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}