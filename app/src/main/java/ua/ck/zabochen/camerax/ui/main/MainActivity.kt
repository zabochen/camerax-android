package ua.ck.zabochen.camerax.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.android.synthetic.main.activity_main.*
import ua.ck.zabochen.camerax.R
import ua.ck.zabochen.camerax.common.showToast
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Codelab: https://codelabs.developers.google.com/codelabs/camerax-getting-started/
 * android/camera-samples: https://github.com/android/camera-samples
 * https://proandroiddev.com/update-android-camerax-4a44c3e4cdcc
 * https://www.raywenderlich.com/6748203-camerax-getting-started
 */
class MainActivity : AppCompatActivity() {

    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private var camera: Camera? = null
    private var imageCapture: ImageCapture? = null

    private val requestAllPermissions =
        registerForActivityResult(RequestMultiplePermissions()) { result ->
            when (result.entries.all { it.value == true }) {
                true -> startCamera()
                false -> {
                    showToast("All permissions not granted by the user")
                    finish()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Check permissions
        when (allPermissionsGranted()) {
            true -> startCamera()
            false -> requestAllPermissions()
        }

        // Init UI Actions
        initUiActions()
    }

    private fun initUiActions() {
        ivTakePhoto.setOnClickListener {
            takePhoto()
        }
    }

    private fun requestAllPermissions() {
        requestAllPermissions.launch(permissions)
    }

    private fun allPermissionsGranted() = permissions.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            // Create folder
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists()) mediaDir else filesDir
    }

    private fun startCamera() {
        this.cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(
            Runnable { bindPreview(cameraProviderFuture.get()) },
            ContextCompat.getMainExecutor(this)
        )
    }

    private fun bindPreview(cameraProvider: ProcessCameraProvider) {
        // Preview
        val preview: Preview = Preview.Builder()
            .build()

        // ImageCapture
        this.imageCapture = ImageCapture.Builder()
            .build()

        // Select back camera
        val cameraSelector = CameraSelector.Builder()
            // LENS_FACING_BACK or LENS_FACING_FRONT
            .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
            .build()

        preview.setSurfaceProvider(vPreviewView.createSurfaceProvider())

        // Bind use cases to camera
        this.camera = cameraProvider.bindToLifecycle(
            this as LifecycleOwner,
            cameraSelector,
            preview,
            imageCapture
        )
    }

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create timestamped output file to hold the image
        val photoFile = File(
            getOutputDirectory(),
            SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis()) + ".jpg"
        )

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        Log.i("MainActivity", "takePhoto: ${getOutputDirectory().absolutePath}")

        // Setup image capture listener which is triggered after photo has been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    Log.i("MainActivity", "onImageSaved: Photo capture succeeded: $savedUri")
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.i("MainActivity", "onImageSaved: Photo capture failed")
                }
            }
        )
    }

    companion object {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }
}