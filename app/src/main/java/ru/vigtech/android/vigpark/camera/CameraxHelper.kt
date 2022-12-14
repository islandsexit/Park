package ru.vigtech.android.vigpark.camera
import android.annotation.SuppressLint
import android.app.Activity
import android.net.Uri
import android.util.DisplayMetrics
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.maps.model.LatLng
import ru.vigtech.android.vigpark.api.ApiClient
import ru.vigtech.android.vigpark.tools.PicturesUtils
import java.io.File
import java.math.BigDecimal
import java.util.*
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min



class CameraxHelper(
    private val caller: Any,
    private val previewView: PreviewView,
    private val imageAnalizer: ImageAnalysis.Analyzer? = null,
    private val filesDirectory: File? = null,
    private val onPictureTaken: ((File, Uri?) -> Unit)? = null,
    private val builderPreview: Preview.Builder? = null,
    var builderImageCapture: ImageCapture.Builder? = null,
    private val onError: ((Throwable) -> Unit)? = null,
    var zone: Int = 1,
    var latLng: LatLng,





) {
    private val RATIO_4_3_VALUE = 4.0 / 3.0
    private val RATIO_16_9_VALUE = 16.0 / 9.0
    var cameraControl: CameraControl? = null
    var cameraInfo: CameraInfo? = null
    private val context by lazy {
        when (caller) {
            is Activity -> caller
            is Fragment -> caller.activity
                ?: throw Exception("Fragment is not attached to Activity")
            else -> throw Exception("Can't get a context from a caller")
        }
    }

    private lateinit var imagePreview: Preview
    private lateinit var imageCapture: ImageCapture
    private var imageAnalysis: ImageAnalysis? = null

    private var lensFacing = CameraSelector.LENS_FACING_FRONT
    private val executor = Executors.newSingleThreadExecutor()

    fun start() {
        if (caller !is LifecycleOwner) throw Exception("Caller is not a lifecycle owner")
        previewView.post { startCamera() }
    }


    private fun createImagePreview() =
        (builderPreview ?: Preview.Builder()
            .setTargetAspectRatio(aspectRatio()))
            .setTargetRotation(previewView.display.rotation)
            .build()
            .apply { setSurfaceProvider(previewView.surfaceProvider) }

    private fun createImageAnalysis() =
        ImageAnalysis.Builder()
            .setImageQueueDepth(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .apply { imageAnalizer?.let { setAnalyzer(executor, imageAnalizer) } }

    @SuppressLint("RestrictedApi")
    fun createImageCapture() =
        (builderImageCapture ?: ImageCapture.Builder()
            .setTargetAspectRatio(aspectRatio()))
            .setTargetResolution(android.util.Size(1024,768))
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()

    fun changeCamera() {
        lensFacing =
            if (lensFacing == CameraSelector.LENS_FACING_FRONT) CameraSelector.LENS_FACING_BACK
            else CameraSelector.LENS_FACING_FRONT

        startCamera()
    }

    private fun startCamera() {
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener(Runnable {
            try {
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
                imagePreview = createImagePreview()
                imagePreview.setSurfaceProvider(previewView.surfaceProvider)

                imageCapture = createImageCapture()
                imageAnalysis = createImageAnalysis()


                cameraProvider.unbindAll()
               val camera = cameraProvider.bindToLifecycle(
                    caller as LifecycleOwner,
                    cameraSelector,
                    imagePreview,
                    imageCapture,
                    imageAnalysis
                )

                cameraControl = camera.cameraControl
                cameraInfo = camera.cameraInfo



            } catch (exc: Exception) {
                onError?.invoke(exc)
            }


        }, ContextCompat.getMainExecutor(context))

    }

    private fun aspectRatio(): Int {
        val metrics = DisplayMetrics().also { previewView.display.getRealMetrics(it) }
        val width = metrics.widthPixels
        val height = metrics.heightPixels

        val previewRatio = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    fun takePicture() {
        val dir = filesDirectory ?: context.cacheDir
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, UUID.randomUUID().toString() + ".jpg")
        val metadata = ImageCapture.Metadata().apply {
            // Mirror image when using the front camera

            isReversedHorizontal = lensFacing == CameraSelector.LENS_FACING_FRONT
        }

        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(file)
            .setMetadata(metadata)
            .build()

        imageCapture.takePicture(
            outputFileOptions,
            executor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    onPictureTaken?.invoke(
                        file,
                        outputFileResults.savedUri
                    )
                    val img64 = PicturesUtils.img64FromFile(file.path)
                    ApiClient.POST_img64(img64,img_path =  file.path, img_plate_path = "None", zone = zone, long = latLng.longitude, lat=latLng.latitude  )
                    Log.i("Photo Taken", "${latLng.latitude} ${latLng.longitude}")
                }

                override fun onError(exception: ImageCaptureException) {
                    onError?.invoke(exception)
                }
            })
    }


}