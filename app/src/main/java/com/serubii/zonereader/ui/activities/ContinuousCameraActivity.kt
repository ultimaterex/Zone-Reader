package com.serubii.zonereader.ui.activities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Matrix
import android.os.Bundle
import android.os.HandlerThread
import android.util.DisplayMetrics
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.serubii.zonereader.R
import com.serubii.zonereader.helpers.analyzers.MRZAnalyzer
import java.util.concurrent.Executors

class ContinuousCameraActivity : AppCompatActivity() {

    private val REQUEST_CODE_PERMISSIONS = 10
    private val TAG = this.javaClass.simpleName
    private var width: Int = 0
    private var height: Int = 0
    private lateinit var context: Context

    // This is an array of all the permission specified in the manifest.
    private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_continuous_camera)
        context = this

        viewFinder = findViewById(R.id.back_view_finder)
//        facenumber = findViewById(R.id.facenumber)
//        imageView = findViewById(R.id.FrontCamera)
//        resultLayout = findViewById(R.id.FrontResultLayout)

        // Request camera permissions
        if (allPermissionsGranted()) {
            viewFinder.post { startCamera() }
        } else {
            ActivityCompat.requestPermissions(
                    this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        // Every time the provided texture view changes, recompute layout
        viewFinder.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            updateTransform()
        }
    }

    // Add this after onCreate

    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var viewFinder: TextureView
//    private lateinit var facenumber: TextView
//    private lateinit var imageView: ImageView
//    private lateinit var resultLayout: ConstraintLayout

    private fun calcRatio() {
        val metrics: DisplayMetrics = resources.displayMetrics
        width = metrics.widthPixels
        height = metrics.heightPixels
        val ratio =
                metrics.heightPixels.toFloat() / metrics.widthPixels.toFloat()
        Log.wtf(TAG, ": $ratio at h$width & w$height with $width x $height")
    }

    private fun startCamera() {

        // do aspect ratio calculation
        calcRatio()

        // TODO: Implement CameraX operations


        // Build the viewfinder use case
        val previewConfig = createPreviewConfig()
        val preview = Preview(previewConfig)

        val analyzerConfig = createAnalysisConfig()
        val analyzerUseCase = ImageAnalysis(analyzerConfig).apply {
            setAnalyzer(executor,
                    MRZAnalyzer(context
                    )
            )
        }


        // Every time the viewfinder is updated, recompute layout
        preview.setOnPreviewOutputUpdateListener {

            // To update the SurfaceTexture, we have to remove it and re-add it
            val parent = viewFinder.parent as ViewGroup
            parent.removeView(viewFinder)
            parent.addView(viewFinder, 0)

            viewFinder.surfaceTexture = it.surfaceTexture

            updateTransform()
        }

        // Add this before CameraX.bindToLifecycle

        // Create configuration object for the image capture use case
        ImageCaptureConfig.Builder()
                .apply {
                    // We don't set a resolution for image capture; instead, we
                    // select a capture mode which will infer the appropriate
                    // resolution based on aspect ration and requested mode
                    setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
                }.build()

        CameraX.bindToLifecycle(this, preview, analyzerUseCase)


    }

    override fun onBackPressed() {
        super.onBackPressed()
        val main = Intent(this, MainActivity::class.java)
        // set the new task and clear flags
        main.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(main)
    }

    private fun createPreviewConfig(): PreviewConfig {
        // Create configuration object for the viewfinder use case
        return PreviewConfig.Builder()
                .setTargetRotation(windowManager.defaultDisplay.rotation)
                .setLensFacing(CameraX.LensFacing.BACK)
                .apply {}
//            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
//            .setTargetResolution(Size(1920,1080))
                .build()

    }

    private fun createAnalysisConfig(): ImageAnalysisConfig {

        return ImageAnalysisConfig.Builder().apply {
            // Use a worker thread for image analysis to prevent glitches

            HandlerThread(
                    "FaceAnalysis"
            ).apply { start() }
            setLensFacing(CameraX.LensFacing.BACK)
            setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
            setTargetAspectRatio(AspectRatio.RATIO_16_9)
//            setTargetResolution(Size(1920, 1080))
        }
                .build()
    }


    private fun updateTransform() {
        // TODO: Implement camera viewfinder transformations
        val matrix = Matrix()

        // Compute the center of the view finder
        val centerX = viewFinder.width / 2f
        val centerY = viewFinder.height / 2f

        // Correct preview output to account for display rotation
        val rotationDegrees = when (viewFinder.display.rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> return
        }
        matrix.postRotate(-rotationDegrees.toFloat(), centerX, centerY)

        // Finally, apply transformations to our TextureView
        viewFinder.setTransform(matrix)
    }

    /**
     * Process result from permission request dialog box, has the request
     * been granted? If yes, start Camera. Otherwise display a toast
     */
    override fun onRequestPermissionsResult(
            requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                viewFinder.post { startCamera() }
            } else {
                Toast.makeText(
                        this,
                        "Permissions not granted by the user.",
                        Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    /**
     * Check if all permission specified in the manifest have been granted
     */
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
                baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }


}
