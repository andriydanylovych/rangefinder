package com.prostologik.lv12.ui.objects

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
//import com.prostologik.lv12.Manifest
import com.prostologik.lv12.databinding.FragmentSlideshowBinding

class ObjectsFragment : Fragment() {

    private var _binding: FragmentSlideshowBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val slideshowViewModel =
            ViewModelProvider(this).get(ObjectsViewModel::class.java)

        _binding = FragmentSlideshowBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textSlideshow
        slideshowViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    //@Composable
//    fun CurrentLocationScreen() {
//        val permissions = listOf(
//            Manifest.permission.ACCESS_COARSE_LOCATION,
//            Manifest.permission.ACCESS_FINE_LOCATION,
//        )
//        PermissionBox(
//            permissions = permissions,
//            requiredPermissions = listOf(permissions.first()),
//            onGranted = {
//                CurrentLocationContent(
//                    usePreciseLocation = it.contains(Manifest.permission.ACCESS_FINE_LOCATION),
//                )
//            },
//        )
//    }
}