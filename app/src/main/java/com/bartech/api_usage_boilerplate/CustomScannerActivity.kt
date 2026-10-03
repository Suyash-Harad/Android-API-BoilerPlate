package com.bartech.api_usage_boilerplate

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import com.bartech.api_usage_boilerplate.databinding.ActivityCustomScannerBinding

/**
 * A self-contained barcode/QR scanner Activity built on CameraX + ML Kit.
 *
 * Usage from caller Activity/Fragment:
 *
 *   private val scanLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
 *       if (result.resultCode == Activity.RESULT_OK) {
 *           val code = result.data?.getStringExtra(CustomScannerActivity.EXTRA_SCANNED_VALUE)
 *           // use code
 *       }
 *   }
 *   scanLauncher.launch(Intent(this, CustomScannerActivity::class.java))
 */
class CustomScannerActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "CustomScannerActivity"
        private const val PERMISSION_REQUEST_CODE = 101

        /** Key used to return the scanned value to the calling Activity. */
        const val EXTRA_SCANNED_VALUE = "scanned_barcode"
    }

    private var torchEnabled = false
    private var cameraControl: CameraControl? = null
    private var cameraInfo: CameraInfo? = null
    private var cameraProvider: ProcessCameraProvider? = null

    private lateinit var cameraExecutor: ExecutorService

    private lateinit var binding: ActivityCustomScannerBinding

    // Created once and reused across frames instead of per-frame allocation.
    private val barcodeScanner: BarcodeScanner by lazy {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_PDF417
            )
            .build()
        BarcodeScanning.getClient(options)
    }

    // Prevents multiple concurrent finish() calls / duplicate results from overlapping frames.
    private val resultDelivered = AtomicBoolean(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()


        binding.btnTorch.setImageResource(R.drawable.ic_flash_off)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestRequiredPermissions()
        }

        binding.btnTorch.setOnClickListener { toggleTorch() }
    }

    private fun toggleTorch() {
        val control = cameraControl ?: return
        torchEnabled = !torchEnabled
        control.enableTorch(torchEnabled)
        binding.btnTorch.setImageResource(
            if (torchEnabled) R.drawable.ic_flash_on else R.drawable.ic_flash_off
        )
    }

    private fun requestRequiredPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            // CAMERA must be granted; storage permission failure is non-fatal (only blocks gallery upload).
            val cameraGranted = permissions.indexOf(Manifest.permission.CAMERA).let { idx ->
                idx != -1 && grantResults.getOrNull(idx) == PackageManager.PERMISSION_GRANTED
            }
            if (cameraGranted) {
                startCamera()
            } else {
                Toast.makeText(this, "Camera permission is required to scan.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val provider = cameraProviderFuture.get()
            cameraProvider = provider

            val preview = Preview.Builder()
                .build()
                .also { it.setSurfaceProvider(binding.previewView.surfaceProvider) }

            val analyzer = ImageAnalysis.Builder()
                // Drop stale frames instead of queueing — keeps latency low.
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        processImageProxy(imageProxy)
                    }
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                provider.unbindAll()
                val camera = provider.bindToLifecycle(this, cameraSelector, preview, analyzer)
                cameraControl = camera.cameraControl
                cameraInfo = camera.cameraInfo
            } catch (e: Exception) {
                Log.e(TAG, "Camera bind failed", e)
                Toast.makeText(this, "Unable to start camera.", Toast.LENGTH_SHORT).show()
                finish()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processImageProxy(imageProxy: ImageProxy) {
        // Skip work entirely once we already have a result in flight.
        if (resultDelivered.get()) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        barcodeScanner.process(image)
            .addOnSuccessListener { barcodes ->
                val value = barcodes.firstOrNull()?.rawValue
                if (value != null && resultDelivered.compareAndSet(false, true)) {
                    deliverResult(value)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Barcode scan failed: ${e.message}", e)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    private fun deliverResult(value: String) {
        val intent = Intent().putExtra(EXTRA_SCANNED_VALUE, value)
        setResult(RESULT_OK, intent)
        finish()
    }

    /** Optional: call this from an "upload from gallery" button if you re-enable that flow. */
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val uri: Uri? = result.data?.data
            uri?.let { handleGalleryImage(it) }
        }
    }

    private fun handleGalleryImage(uri: Uri) {
        try {
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
            val image = InputImage.fromBitmap(bitmap, 0)

            barcodeScanner.process(image)
                .addOnSuccessListener { barcodes ->
                    val value = barcodes.firstOrNull()?.rawValue
                    if (value != null) {
                        if (resultDelivered.compareAndSet(false, true)) {
                            deliverResult(value)
                        }
                    } else {
                        Toast.makeText(
                            this,
                            "No barcode found in the image. Please try another.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to process image.", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading image from gallery", e)
            Toast.makeText(this, "Error loading image from gallery.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun allPermissionsGranted(): Boolean {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(Manifest.permission.CAMERA, Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            listOf(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        return permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraProvider?.unbindAll()
        cameraExecutor.shutdown()
        barcodeScanner.close()
    }
}