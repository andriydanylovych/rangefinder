package com.prostologik.lv12.ui.home

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Size
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
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.prostologik.lv12.R
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

        getSharedPref()

        snippetWidth = homeViewModel.snippetWidth.value ?: 64
        snippetHeight = homeViewModel.snippetHeight.value ?: 64
        snippetLayer = homeViewModel.snippetLayer.value ?: 0

        analyzerOption = homeViewModel.analyzerOption.value ?: 0
        resolutionWidth = homeViewModel.resolutionWidth.value ?: 640
        resolutionHeight = homeViewModel.resolutionHeight.value ?: 480

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions()
        }

        val btnImage = binding.imageCaptureButton
        btnImage.setOnClickListener {
            if (analyzerOption == 1) takePhoto()
            else displayAnalyzer()
        }

        textView = binding.textHome

        val btnInfo = binding.infoButton
        btnInfo.setOnClickListener {
            //textView.text = infoText
            capturePhoto()
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        return root
    }

    private fun getSharedPref() {
        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE)

        val trySnippetWidth = sharedPref?.getInt("snippet_width", 64)
        if (trySnippetWidth != null) homeViewModel.setSnippetWidth(trySnippetWidth)

        val trySnippetHeight = sharedPref?.getInt("snippet_height", 64)
        if (trySnippetHeight != null) homeViewModel.setSnippetHeight(trySnippetHeight)

        val trySnippetLayer = sharedPref?.getInt("saved_layer", 1) // getString(R.string.saved_layer)
        if (trySnippetLayer != null) homeViewModel.setSnippetLayer(trySnippetLayer)

        val tryAnalyzerOption = sharedPref?.getInt("analyzer_option", 0)
        if (tryAnalyzerOption != null) homeViewModel.setAnalyzerOption(tryAnalyzerOption)

        val tryResolutionWidth = sharedPref?.getInt("resolution_width", 640)
        if (tryResolutionWidth != null) homeViewModel.setResolutionWidth(tryResolutionWidth)

        val tryResolutionHeight = sharedPref?.getInt("resolution_height", 480)
        if (tryResolutionHeight != null) homeViewModel.setResolutionHeight(tryResolutionHeight)

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

            //imageCapture = ImageCapture.Builder().build()

            imageCapture = ImageCapture.Builder()
                .apply {
                    //this.setTargetRotation(orientation)

                    val resolutionSelectorBuilder = ResolutionSelector.Builder().apply {
                        setResolutionStrategy(
                            ResolutionStrategy(
                                Size(
                                    resolutionWidth,
                                    resolutionHeight
                                ), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                                //ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER
                            )
                        )
                    }

                    this.setResolutionSelector(resolutionSelectorBuilder.build())

//                    if (actualCameraIsMaxQuality) {
//                        this.setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
//                    }

//                    this.setFlashMode(flashLamp)
                }
                .build()


            val imageAnalyzer = ImageAnalysis.Builder()
                //.setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888) // YUV_420_888
                //.setTargetResolution(Size(1280, 720)) // default (640, 480) // deprecated :(
                //.setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST) // STRATEGY_BLOCK_PRODUCER
                .setImageQueueDepth(1)
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

        val photoFileName = getPhotoFileName()
        homeViewModel.setPhotoFileName(photoFileName)

        val photoFile = File(outputDirectory, "$photoFileName.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // Set up image capture listener, which is triggered after photo has been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(safeContext),
            object : ImageCapture.OnImageSavedCallback { // OnImageSavedCallback
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }
                @RequiresApi(Build.VERSION_CODES.O)
                override fun onImageSaved(output: ImageCapture.OutputFileResults){

                    try {
                        val file = File("$outputDirectory/$photoFileName.csv")
                        file.writeText(infoText)
                    } catch (_: IOException) {}

                    val msg = "Photo capture succeeded: ${output.savedUri}"
                    Toast.makeText(safeContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                }

            }
        )
    }

    private fun displayAnalyzer() {

        textView.text = infoText

    }

    private fun getOutputDirectory(): File {
        val mediaDir = activity?.externalMediaDirs?.firstOrNull()?.let {
            File(it, "image").apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists()) mediaDir else activity?.filesDir!!
    }

    private fun getPhotoFileName(): String {
        return SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis())
    }

    var iii = 0
    private fun capturePhoto() {
        val imageCapture = imageCapture ?: return

        imageCapture.takePicture(
            ContextCompat.getMainExecutor(safeContext),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    super.onCaptureSuccess(image)

                    val capturedImageAnalyzer = CapturedImageAnalysis()
                    val test = capturedImageAnalyzer.analyze(image, 8, 8, 0)
                    textView.text = "$iii: $test"
                    iii++

                }
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        _binding = null
    }

    private class PhotoAnalyzer(private val listener: AnalyzerListener) : ImageAnalysis.Analyzer {

        override fun analyze(image: ImageProxy) {
            if (analyzerOption == 0) {
                val imageAnalyzer = MyImageAnalyzer()
                listener(imageAnalyzer.analyze(image))
            } else {
                val imageAnalyzer = SnippetAnalyzer()
                listener(imageAnalyzer.analyze(image, snippetWidth, snippetHeight, snippetLayer))
            }
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

        private var analyzerOption = 0
        private var resolutionWidth = 640
        private var resolutionHeight = 480
    }

    /**
    * Release memory when the UI becomes hidden or when system resources become low.
    * 'at' param level the memory-related event that is raised.
    */
//    override fun onTrimMemory(level: Int) {
//
//        if (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
//            // Release memory related to UI elements, such as bitmap caches.
//        }
//
//        if (level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) {
//            // Release memory related to background processing, such as by
//            // closing a database connection.
//        }
//    }

    // https://developer.android.com/topic/performance/memory
//    fun doSomethingMemoryIntensive() {
//
//        // Before doing something that requires a lot of memory,
//        // check whether the device is in a low memory state.
//        if (!getAvailableMemory().lowMemory) {
//            // Do memory intensive work.
//        }
//    }
//
//    private fun getAvailableMemory(): ActivityManager.MemoryInfo {
//        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager //  Context.ACTIVITY_SERVICE
//        return ActivityManager.MemoryInfo().also { memoryInfo ->
//            activityManager.getMemoryInfo(memoryInfo)
//        }
//    }
}