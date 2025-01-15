package com.prostologik.lv12.ui.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
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
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.prostologik.lv12.databinding.FragmentHomeBinding
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.atan

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
    private var infoText2: String = "dummy text"


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

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val btnImage = binding.imageCaptureButton
        btnImage.setOnClickListener {
            if (analyzerOption == 1) takePhoto()
            else displayAnalyzer()
        }

        textView = binding.textHome

        val btnInfo = binding.infoButton
        btnInfo.setOnClickListener {
            homeViewModel.info = "Camera info: $infoText2"
            homeViewModel.setResolutionWidth(imageWidth)
            homeViewModel.setResolutionHeight(imageHeight)
            textView.text = "camera info available"
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

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            val preview = Preview.Builder()
                .build()
                .also {
                    it.surfaceProvider = binding.viewFinder.surfaceProvider
                }

            imageCapture = ImageCapture.Builder().build()

            val resolutionSelectorForAnalyzer = ResolutionSelector.Builder()
                .setAllowedResolutionMode(ResolutionSelector.PREFER_HIGHER_RESOLUTION_OVER_CAPTURE_RATE)
                //.setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                //.setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
                .setResolutionStrategy(
                    ResolutionStrategy(
                        Size(
                            resolutionWidth,
                            resolutionHeight
                        ), ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                        // FALLBACK_RULE_NONE
                    )
                )
                .build()

            val imageAnalyzer = ImageAnalysis.Builder()
                //.setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888) // YUV_420_888
                //.setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST) // STRATEGY_BLOCK_PRODUCER
                .setImageQueueDepth(1)
                .setResolutionSelector(resolutionSelectorForAnalyzer)
                .build()
                .also {
                    it.setAnalyzer(
                        cameraExecutor,
                        PhotoAnalyzer {
                            infoString ->
                            infoText = infoString

                            if (analyzerOption == 0 && activity != null) {
                                activity?.runOnUiThread {
                                    textView.text = infoText
                                }
                            }

                        }
                    )
                }

            val cameraManager = context?.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraCharacteristics = cameraManager.getCameraCharacteristics("0")
            val focalLength = cameraCharacteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.firstOrNull() //?: return
            val sensorSize = cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE) //?: return
            infoText2 = "\nfocalLength: $focalLength sensorSize: $sensorSize"
            if (focalLength != null && focalLength != 0f && sensorSize != null) {
                val horizontalAngle = (2f * atan((sensorSize.width / (focalLength * 2f)).toDouble())) * 180.0 / Math.PI
                val verticalAngle = (2f * atan((sensorSize.height / (focalLength * 2f)).toDouble())) * 180.0 / Math.PI
                infoText2 += "\nhorizontalAngle: $horizontalAngle verticalAngle: $verticalAngle \n"
            }

            try {
                cameraProvider.unbindAll() // Unbind use cases before rebinding
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, imageAnalyzer)

                infoText2 += listCameras(cameraProvider).replace("[", "\n[")
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

        homeViewModel.setResolutionWidth(imageWidth) // does not work
        homeViewModel.setResolutionHeight(imageHeight) // does not work
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

//    private var iii = 0
//    private fun capturePhoto() {
//        textView.text = "$iii: $infoText2"
//        textView.movementMethod = ScrollingMovementMethod()
//        iii++
//    }

    @OptIn(ExperimentalCamera2Interop::class)
    fun listCameras(provider: ProcessCameraProvider):String {

        val sb: StringBuilder = StringBuilder("")

        val cam2Infos = provider.availableCameraInfos.map {
            Camera2CameraInfo.from(it)
        }.sortedByDescending {
            it.getCameraCharacteristic(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
        }

        for (c in cam2Infos) {
            sb.append("$c\n")
            val d = c.getCameraCharacteristic(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            sb.append("$d\n")
//            val msg = "Camera: $c Characteristics: $d"
//            Log.d(TAG, msg)
        }
        Log.d(TAG, sb.toString())

        return sb.toString()

    }

//    @OptIn(ExperimentalCamera2Interop::class)
//    fun selectExternalOrBestCamera(provider: ProcessCameraProvider):CameraSelector? {
//        val cam2Infos = provider.availableCameraInfos.map {
//            Camera2CameraInfo.from(it)
//        }.sortedByDescending {
//            // HARDWARE_LEVEL is Int type, with the order of:
//            // LEGACY < LIMITED < FULL < LEVEL_3 < EXTERNAL
//            it.getCameraCharacteristic(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
//        }
//
//        return when {
//            cam2Infos.isNotEmpty() -> {
//                CameraSelector.Builder()
//                    .addCameraFilter {
//                        it.filter { camInfo ->
//                            // cam2Infos[0] is either EXTERNAL or best built-in camera
//                            val thisCamId = Camera2CameraInfo.from(camInfo).cameraId
//                            thisCamId == cam2Infos[0].cameraId
//                        }
//                    }.build()
//            }
//            else -> null
//        }
//    }

//    @RequiresApi(Build.VERSION_CODES.M)
//    fun getPossibleOutputSizes(id: String) {
//        val cameraManager = context?.getSystemService(Context.CAMERA_SERVICE) as CameraManager
//        val sizes = cameraManager?.getCameraCharacteristics(id)?.get(SCALER_STREAM_CONFIGURATION_MAP)?.getHighResolutionOutputSizes(ImageFormat.JPEG)
//        if (sizes != null) {
//            println("got possible resolutions:")
//            println(sizes.joinToString("\n"))
//        }
//    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        _binding = null
    }

    private class PhotoAnalyzer(private val listener: AnalyzerListener) : ImageAnalysis.Analyzer {

        override fun analyze(image: ImageProxy) {
            if (imageWidth != image.width) {
                imageWidth = image.width
                OverlayView.imageWidth = imageWidth
            }
            if (imageHeight != image.height) {
                imageHeight = image.height
                OverlayView.imageHeight = imageHeight
            }
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
        private var resolutionWidth = 800//640
        private var resolutionHeight = 600//480

        private var imageWidth = 800
        private var imageHeight = 600
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