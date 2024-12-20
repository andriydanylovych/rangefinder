package com.prostologik.lv12.ui.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.prostologik.lv12.databinding.FragmentHomeBinding
import com.prostologik.lv12.ui.review.SavedPhotoAnalyzer
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

typealias AnalyzerListener = (infoString: String) -> Unit

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    private val binding get() = _binding!!

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService

    private lateinit var safeContext: Context

    private lateinit var outputDirectory: File
    private lateinit var textView: TextView
    private var infoText: String = "no input"


    private val homeViewModel: HomeViewModel by activityViewModels()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        safeContext = context
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreate(savedInstanceState)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        outputDirectory = getOutputDirectory()
        homeViewModel.setPhotoDirectory(outputDirectory.toString())

        snippetWidth = 64
        homeViewModel.snippetWidth.observe(viewLifecycleOwner) {
            val temp: Int? = it
            if (temp != null && temp != 0) {
                snippetWidth = temp
            }
        }

        snippetHeight = 1
        homeViewModel.snippetHeight.observe(viewLifecycleOwner) {
            val temp: Int? = it
            if (temp != null && temp != 0) {
                snippetHeight = temp
            }
        }

        snippetLayer = 0
        homeViewModel.snippetLayer.observe(viewLifecycleOwner) {
            val temp: Int? = it
            if (temp != null && temp != 0) {
                snippetLayer = temp
            }
        }

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions()
        }

        val btnImage = binding.imageCaptureButton
        btnImage.setOnClickListener { takePhoto() }

        textView = binding.textHome

        val btnInfo = binding.infoButton
        btnInfo.setOnClickListener {
            //textView.text = infoText
            capturePhoto()
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        return root
    }

    private fun requestPermissions() {
        activityResultLauncher.launch(REQUIRED_PERMISSIONS)
    }

    private val activityResultLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions())
        { permissions ->
            var permissionGranted = true
            permissions.entries.forEach {
                if (it.key in REQUIRED_PERMISSIONS && !it.value)
                    permissionGranted = false
            }
            if (!permissionGranted) {
                Toast.makeText(safeContext,
                    "Permission request denied",
                    Toast.LENGTH_SHORT).show()
            } else {
                startCamera()
            }
        }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        context?.let { it1 -> ContextCompat.checkSelfPermission(it1, it) } == PackageManager.PERMISSION_GRANTED
    }

    private fun startCamera() {

        val cameraProviderFuture = ProcessCameraProvider.getInstance(safeContext)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.surfaceProvider = binding.viewFinder.surfaceProvider
                }

            imageCapture = ImageCapture.Builder().build()

            val imageAnalyzer = ImageAnalysis.Builder()
                //.setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                //.setTargetResolution(Size(1280, 720))
                //.setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor,
                        PhotoAnalyzer { infoString ->
                            infoText = infoString
                        })
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll() // Unbind use cases before rebinding
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, imageAnalyzer)
            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(safeContext))
    }

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        val photoFileName: String = SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis()) //+ ".jpg"
        homeViewModel.setPhotoFileName(photoFileName)
        val photoFileNameJpg = "$photoFileName.jpg"

        val photoFile = File(outputDirectory, photoFileNameJpg)
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // Set up image capture listener, which is triggered after photo has been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(safeContext),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }
                @RequiresApi(Build.VERSION_CODES.O)
                override fun onImageSaved(output: ImageCapture.OutputFileResults){

                    //textView.text = infoText

                    try {
                        val filePath = "$outputDirectory/$photoFileName.csv"
                        val file = File(filePath)
                        file.writeText(infoText)
                    } catch (_: IOException) {}

                    val msg = "Photo capture succeeded: ${output.savedUri}"
                    Toast.makeText(safeContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                }
            }
        )
    }

    private fun getOutputDirectory(): File {
        val mediaDir = activity?.externalMediaDirs?.firstOrNull()?.let {
            File(it, "image").apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists()) mediaDir else activity?.filesDir!!
    }

    var iii = 0
    private fun capturePhoto() {
        val imageCapture = imageCapture ?: return

        imageCapture.takePicture(
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    super.onCaptureSuccess(image)

//                    val bufferY = image.planes[0].buffer
//                    val dataY = bufferY.toByteArray() // dataY.size = 2760180
//                    val test0 = dataY[0]
//                    val test1 = dataY[1]
//                    val test2 = dataY[2]
//                    val test3 = dataY[3]
//                    val test4 = dataY[4]
//                    val dd = dataY.size
//                    textView.text = "0:$test0, 1:$test1, 2:$test2, 3:$test3, 4:$test4, data:$dd"

                    val capturedImageAnalyzer = CapturedImageAnalysis()
                    val test = capturedImageAnalyzer.analyze(image, 8, 8, 0)
                    textView.text = "$iii: $test"
                    iii++

                    //val w = image.width // 2448
                    //val h = image.height // 3264
                    //textView.text = "w:$w h:$h"
//                    parentFragmentManager.beginTransaction()
//                        .replace(R.id.container, ImageViewFragment.newInstance(image))
//                        .addToBackStack(null)
//                        .commit()
                }
            }
        )
    }

    private fun ByteBuffer.toByteArray(): ByteArray {
        rewind()    // Rewind the buffer to zero
        val data = ByteArray(remaining())
        get(data)   // Copy the buffer into a byte array [cannot be dropped!]
        return data // Return the byte array
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        _binding = null
    }

    private class PhotoAnalyzer(private val listener: AnalyzerListener) : ImageAnalysis.Analyzer {

        override fun analyze(image: ImageProxy) {
            val myImageAnalyzer = MyImageAnalyzer()
            listener(myImageAnalyzer.analyze(image, snippetWidth, snippetHeight, snippetLayer))
            image.close()
        }
    }

    companion object {
        private const val TAG = "CameraXApp"
        const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private val REQUIRED_PERMISSIONS =
            mutableListOf (
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()

        private var snippetWidth = 64
        private var snippetHeight = 64
        private var snippetLayer = 0
    }


}