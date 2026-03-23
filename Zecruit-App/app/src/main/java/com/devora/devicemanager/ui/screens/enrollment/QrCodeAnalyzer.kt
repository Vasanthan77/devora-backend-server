package com.devora.devicemanager.ui.screens.enrollment

import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

/**
 * CameraX [ImageAnalysis.Analyzer] that uses ML Kit Barcode Scanning
 * to detect QR codes in the camera feed.
 *
 * [onQrCodeDetected] is invoked exactly once (the first successful detection).
 * After that the analyzer stops processing frames to avoid duplicate callbacks.
 */
class QrCodeAnalyzer(
    private val onQrCodeDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient()
    private var hasDetected = false

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        if (hasDetected) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val inputImage = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        scanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    if (barcode.format == Barcode.FORMAT_QR_CODE) {
                        val rawValue = barcode.rawValue
                        if (!rawValue.isNullOrEmpty() && !hasDetected) {
                            hasDetected = true
                            Log.d("QrCodeAnalyzer", "QR detected: $rawValue")
                            onQrCodeDetected(rawValue)
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("QrCodeAnalyzer", "Barcode scan failed: ${e.message}")
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }
}
