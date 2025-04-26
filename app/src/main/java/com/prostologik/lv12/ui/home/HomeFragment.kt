package com.prostologik.lv12.ui.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
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
import androidx.core.util.component1
import androidx.core.util.component2
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
        homeViewModel.photoDirectory = outputDirectory.toString() // photoDirectory

        if (!homeViewModel.modelAlreadyPopulated){
            getSharedPref()
            homeViewModel.modelAlreadyPopulated = true
        }

        snippetLayer = homeViewModel.snippetLayer
        snippetWidth = homeViewModel.snippetWidth
        snippetHeight = homeViewModel.snippetHeight
        analyzerOption = homeViewModel.analyzerOption
        resolutionWidth = homeViewModel.resolutionWidth
        resolutionHeight = homeViewModel.resolutionHeight

        OverlayView.snippetWidth = snippetWidth
        OverlayView.snippetHeight = snippetHeight
        OverlayView.analyzerOption = analyzerOption

        val patchWidth = homeViewModel.patchWidth
        val patchHeight = homeViewModel.patchHeight
        OverlayView.patchWidth = patchWidth
        OverlayView.patchHeight = patchHeight

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions()
        }

        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        cameraExecutor = Executors.newSingleThreadExecutor()

        textView = binding.textHome

        val btnImage = binding.imageCaptureButton
        val btnOption = binding.optionButton

        val sliderWidth = binding.sliderWidth
        val sliderHeight = binding.sliderHeight

        fun renderView() {
            if (analyzerOption == 0) {
                sliderWidth.visibility = View.INVISIBLE
                sliderHeight.visibility = View.INVISIBLE
            } else {
                sliderWidth.value = snippetWidth.toFloat()
                sliderWidth.addOnChangeListener { slider, _, _ ->
                    snippetWidth = slider.value.toInt()
                    processWidthHeight()
                }
                sliderHeight.value = snippetHeight.toFloat()
                sliderHeight.addOnChangeListener { slider, _, _ ->
                    snippetHeight = slider.value.toInt()
                    processWidthHeight()
                }

                btnImage.setOnClickListener {
                    takePhoto()
                }

                btnOption.visibility = View.VISIBLE
                sliderWidth.visibility = View.VISIBLE
                sliderHeight.visibility = View.VISIBLE

                setSnippetWidthHeight()
            }
        }

        renderView()

        btnOption.setOnClickListener {
            analyzerOption = if (analyzerOption == 0) 1 else 0
            homeViewModel.analyzerOption = analyzerOption
            OverlayView.analyzerOption = analyzerOption
            val overlayView = binding.overlayview
            overlayView.invalidate()
            renderView()
        }

        val root: View = binding.root

        return root
    }

    private fun setSnippetWidthHeight() {
        val temp = snippetWidth * snippetHeight
        textView.text = "WxH = $snippetWidth x $snippetHeight = $temp"
    }

    private fun processWidthHeight() {
        homeViewModel.snippetWidth = snippetWidth
        homeViewModel.snippetHeight = snippetHeight
        OverlayView.snippetWidth = snippetWidth
        OverlayView.snippetHeight = snippetHeight

        setSnippetWidthHeight()

        val overlayView = binding.overlayview
        overlayView.invalidate()

    }

    private fun getSharedPref() {
        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE)

        val tryPatchWidth = sharedPref?.getInt("patch_width", 28)
        if (tryPatchWidth != null) homeViewModel.patchWidth = tryPatchWidth

        val tryPatchHeight = sharedPref?.getInt("patch_height", 28)
        if (tryPatchHeight != null) homeViewModel.patchHeight = tryPatchHeight

        val trySnippetLayer = sharedPref?.getInt("saved_layer", 1)
        if (trySnippetLayer != null) homeViewModel.snippetLayer = trySnippetLayer

        val tryResolutionWidth = sharedPref?.getInt("resolution_width", 640)
        if (tryResolutionWidth != null) homeViewModel.resolutionWidth = tryResolutionWidth

        val tryResolutionHeight = sharedPref?.getInt("resolution_height", 480)
        if (tryResolutionHeight != null) homeViewModel.resolutionHeight = tryResolutionHeight

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
                                    // to prevent the entire bitmap leaking from analyzerOption = 1:
                                    if (infoText.length < 99) textView.text = infoText
                                }
                            }
                        }
                    )
                }

            try {
                cameraProvider.unbindAll() // Unbind use cases before rebinding
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, imageAnalyzer)

                listCameras(cameraProvider)

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

    private fun getOutputDirectory(): File {
        val mediaDir = activity?.externalMediaDirs?.firstOrNull()?.let {
            File(it, "image").apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists()) mediaDir else activity?.filesDir!!
    }

    private fun getPhotoFileName(): String {
        return SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis())
    }

    @OptIn(ExperimentalCamera2Interop::class)
    fun listCameras(provider: ProcessCameraProvider):String {

        val sb: StringBuilder = StringBuilder("")

        val cam2Infos = provider.availableCameraInfos.map {
            Camera2CameraInfo.from(it)
        }.sortedByDescending {
            it.getCameraCharacteristic(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
        }

        var iii = 0
        for (c in cam2Infos) {
            iii++
            sb.append("\nCAMERA $iii")
            sb.append("\n$c")
            val focalLength = c.getCameraCharacteristic(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.firstOrNull() //?: return
            val sensorSize = c.getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE) //?: return
            val lensFacing = c.getCameraCharacteristic(CameraCharacteristics.LENS_FACING)
            sb.append("\nfocalLength: $focalLength  sensorSize: $sensorSize")
            sb.append("\nlensFacing: $lensFacing (Front, Back, External)")
            if (focalLength != null && focalLength != 0f && sensorSize != null) {
                val horizontalAngle = (2f * atan((sensorSize.width / (focalLength * 2f)).toDouble())) * 180.0 / Math.PI
                val verticalAngle = (2f * atan((sensorSize.height / (focalLength * 2f)).toDouble())) * 180.0 / Math.PI
                sb.append("\nhorizontalAngle: $horizontalAngle verticalAngle: $verticalAngle")
            }
            val d = c.getCameraCharacteristic(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            sb.append("\n$d\n")
            val e = d?.getOutputSizes(35)
            e.isNullOrEmpty()
            if (e.isNullOrEmpty()) {
                sb.append("\n outputSize[0]=null\n")
            } else {
                homeViewModel.arrayOutputWidth = (e.map { it.component1() }).toTypedArray()
                homeViewModel.arrayOutputHeight = (e.map { it.component2() }).toTypedArray()
            }
        }
        Log.d(TAG, sb.toString())

        homeViewModel.cameraInfo = sb.toString().replace("[", "\n[")
        return sb.toString().replace("[", "\n[")

    }

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

        private var snippetLayer = 0
        private var snippetWidth = 64
        private var snippetHeight = 64

        private var analyzerOption = 0
        private var resolutionWidth = 640
        private var resolutionHeight = 480

        private var imageWidth = 640
        private var imageHeight = 480

        var homeFragmentInfo = " Layer: $snippetLayer  WxH: $resolutionWidth x $resolutionHeight  :  $snippetWidth x $snippetHeight"
    }

}