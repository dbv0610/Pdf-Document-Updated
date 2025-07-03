package com.azg.pdf8.ui.main.camera;

import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.azg.pdf8.R
import com.azg.pdf8.app.toastShort
import com.azg.pdf8.databinding.ActivityCameraBinding
import com.azg.pdf8.ui.main.create.CreateActivity
import com.azg.pdf8.utils.Constant
import com.dong.baselib.base.BaseActivity
import com.dong.baselib.widget.click
import java.io.File

class CameraActivity : BaseActivity<ActivityCameraBinding>(ActivityCameraBinding::inflate) {
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private lateinit var imageCapture: ImageCapture
    private var screenAction = ""

    override fun backPressed() {
        finish()
    }

    override fun initialize() {
        screenAction = getData<String>(Constant.SCREEN_ACTION).toString()
        binding.previewView.post { startCamera() }
    }

    override fun ActivityCameraBinding.setData() = Unit

    override fun ActivityCameraBinding.onClick() {
        icCaptureImage.setOnClickListener {
            takePhoto()
        }
        icBack.click {
            finish()
        }
        icStateFlash.setOnClickListener {
            val camera = cameraProvider?.bindToLifecycle(
                this@CameraActivity,
                CameraSelector.Builder().requireLensFacing(lensFacing).build(),
                imageCapture
            )
            camera?.cameraControl?.enableTorch(!isTorchOn).also {
                isTorchOn = !isTorchOn
                icStateFlash.setImageResource(
                    if (isTorchOn) R.drawable.ic_flash_on else R.drawable.ic_flash_off
                )
            }
        }
        icFlipCamera.setOnClickListener {
            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK)
                CameraSelector.LENS_FACING_FRONT
            else
                CameraSelector.LENS_FACING_BACK
            startCamera()
        }
    }

    private var cameraProvider: ProcessCameraProvider? = null
    private var isTorchOn = false

    private fun startCamera() {
        val previewView = binding.previewView
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .build()
                .also { it.surfaceProvider = previewView.surfaceProvider }
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetRotation(previewView.display.rotation)
                .build()
            cameraProvider?.unbindAll()
            cameraProvider?.bindToLifecycle(
                this,
                CameraSelector.Builder().requireLensFacing(lensFacing).build(),
                preview,
                imageCapture
            )
            isTorchOn = false
            binding.icStateFlash.setImageResource(R.drawable.ic_flash_off)
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val photoFile = File(cacheDir, "JPEG_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    cameraProvider?.unbindAll()
                    Handler(Looper.getMainLooper()).postDelayed({
                        if (screenAction == "mainSc") {
                            launchActivity<CreateActivity>(
                                hashMapOf(
                                    Constant.KEY_ACTION to "addNew",
                                    Constant.IMAGE_PATH to photoFile.absolutePath
                                )
                            )
                        } else if (screenAction == Constant.CAPTURE_ADD) {
                            val intentData = Intent()
                            intentData.putExtra(
                                Constant.CAPTURE_ADD,
                                photoFile.absolutePath
                            )
                            setResult(RESULT_OK, intentData)
                            finish()
                        }
                    }, 3000L)
                }

                override fun onError(exc: ImageCaptureException) {
                    toastShort(
                        "Capture failed: ${exc.message}"
                    )
                }
            }
        )
    }
}
