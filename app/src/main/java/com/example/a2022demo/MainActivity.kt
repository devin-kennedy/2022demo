package com.example.a2022demo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.view.PreviewView
import java.util.concurrent.ExecutorService
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Build
import android.util.AttributeSet
import android.util.Size
import android.widget.TextView
import android.widget.Toast
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.toRectF
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabelerOptionsBase
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.custom.CustomImageLabelerOptions
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions
import java.lang.Exception
import java.util.concurrent.Executor
import java.util.concurrent.Executors


class MainActivity : AppCompatActivity() {
    private var cameraSelector: CameraSelector? = null
    private lateinit var previewView: PreviewView
    private var previewUseCase: Preview? = null
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private lateinit var cameraExecutor: ExecutorService
    private var graphicOverlay: GraphicOverlay? = null
    private lateinit var testOverlay: TestView1
    private var textOverlay: TextView? = null
    private lateinit var noString: String
    private lateinit var boundBoxView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        setContentView(R.layout.activity_main)
        previewView = findViewById(R.id.previewView)
        //graphicOverlay = findViewById(R.id.graphicOverlayView)
        testOverlay = findViewById(R.id.testView)
        textOverlay = findViewById(R.id.resultTextView)
        noString = getString(R.string.nothing)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setTargetResolution(Size(1280, 720))
                .build().apply {
                    setAnalyzer(
                        Executors.newSingleThreadExecutor(),
                        TestAnalyzer(
                            textOverlay,
                            noString,
                            testOverlay,
                            previewView.height.toFloat(),
                            previewView.width.toFloat()
                        )
                    )
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(
                    this as LifecycleOwner, cameraSelector, imageAnalyzer, preview)
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CameraX2022Demo"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }

    // Once complete, move to analyzer file
    private class TestAnalyzer(
        private var graphicOverlay: TextView?,
        private var noString: String,
        private var testOverlay: TestView1,
        private var viewHeight: Float,
        private var viewWidth: Float) : ImageAnalysis.Analyzer {

        private val localModel = LocalModel.Builder()
            .setAssetFilePath("custom_models/object_labeler.tflite")
            .build()
        private val customObjectDetectorOptions = CustomObjectDetectorOptions.Builder(localModel)
            .setDetectorMode(CustomObjectDetectorOptions.STREAM_MODE)
            .enableClassification()
            .setClassificationConfidenceThreshold(0.5f)
            .enableMultipleObjects()
            .build()
        private val objectDetector = ObjectDetection.getClient(customObjectDetectorOptions)

        private val paint = Paint().apply {
            style = Paint.Style.STROKE
            color = Color.RED
            strokeWidth = 6f
        }
        private var _scaleY = 0f
        private var _scaleX = 0f

        private fun translateX(x: Float): Float = x * _scaleX
        private fun translateY(y: Float): Float = y * _scaleY

        private fun translateRect(rect: Rect) = RectF(
            translateX(rect.left.toFloat()),
            translateY(rect.top.toFloat()),
            translateX(rect.right.toFloat()),
            translateY(rect.bottom.toFloat())
        )

        @SuppressLint("UnsafeOptInUsageError", "SetTextI18n")
        override fun analyze(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                _scaleY = viewHeight / image.width.toFloat()
                _scaleX = viewWidth / image.height.toFloat()

                objectDetector
                    .process(image)
                    .addOnFailureListener { e ->
                        println("Failure on object detection")
                        println(e.toString())
                    }
                    .addOnSuccessListener { results ->
                        for (detectedObject in results) {
                            if (detectedObject.labels.size >= 1) {
                                graphicOverlay?.text = detectedObject.labels.first().text
                                testOverlay.setRectBounds(translateRect(detectedObject.boundingBox))
                                testOverlay.invalidate()
                            } else {
                                graphicOverlay?.text = noString
                                testOverlay.setRectBounds(RectF(0f, 0f, 0f, 0f))
                                testOverlay.invalidate()
                            }
                        }
                    }
                    .addOnCompleteListener {
                        mediaImage.close()
                        imageProxy.close()
                    }
            }
        }
    }
}

data class DetectionResult(val boundingBox: RectF, val text: String)
