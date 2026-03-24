package com.devora.devicemanager.enrollment

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.gson.Gson
import com.google.gson.GsonBuilder

/**
 * Generates QR payloads for AMAPI enrollment token flows.
 */
object QrProvisioningHelper {

    /**
     * Builds an AMAPI enrollment QR payload that includes the enrollment token.
     */
    fun buildProvisioningPayload(
        adminComponent: String = "",
        apkDownloadUrl: String = "",
        checksum: String = "",
        skipEncryption: Boolean = true,
        wifiSsid: String? = null,
        wifiPassword: String? = null,
        wifiSecurityType: String? = null,
        enrollmentToken: String? = null
    ): String {
        val payload = linkedMapOf<String, Any>(
            "provider" to "AMAPI",
            "type" to "ENROLLMENT_TOKEN"
        )

        if (!enrollmentToken.isNullOrBlank()) {
            payload["enrollmentToken"] = enrollmentToken.trim()
        }

        return GsonBuilder().setPrettyPrinting().create().toJson(payload)
    }

    /**
     * Generates a Bitmap QR code from the provisioning JSON payload.
     *
     * @param payload   JSON string (from [buildProvisioningPayload])
     * @param size      Width and height in pixels (QR codes are square)
     * @return [Bitmap] containing the rendered QR code, or null on error
     */
    fun generateQrBitmap(payload: String, size: Int = 512): Bitmap? {
        return try {
            val hints = mapOf(
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.MARGIN to 1
            )

            val bitMatrix = QRCodeWriter().encode(
                payload,
                BarcodeFormat.QR_CODE,
                size,
                size,
                hints
            )

            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            for (x in 0 until size) {
                for (y in 0 until size) {
                    bitmap.setPixel(
                        x, y,
                        if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
                    )
                }
            }
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Generates a QR bitmap for enrollment token display (not Device Owner provisioning).
     * This is a simpler QR containing just the enrollment token for the employee app to scan.
     *
     * @param token  Enrollment token string
     * @param size   QR bitmap dimension in pixels
     */
    fun generateEnrollmentTokenQr(token: String, size: Int = 512): Bitmap? {
        val payload = token.trim().uppercase() // Send raw token string for robustness
        return generateQrBitmap(payload, size)
    }

    /**
      * Generates an AMAPI enrollment payload QR.
     */
    fun generateDeviceOwnerProvisioningQr(
          apkDownloadUrl: String = "",
          checksum: String = "",
        wifiSsid: String? = null,
        wifiPassword: String? = null,
        wifiSecurityType: String? = null,
        enrollmentToken: String? = null,
        size: Int = 512
    ): Bitmap? {
        val payload = buildProvisioningPayload(
            apkDownloadUrl = apkDownloadUrl,
            checksum = checksum,
            wifiSsid = wifiSsid,
            wifiPassword = wifiPassword,
            wifiSecurityType = wifiSecurityType,
            enrollmentToken = enrollmentToken
        )
        return generateQrBitmap(payload, size)
    }

    /**
     * Parses a scanned QR code payload and extracts the enrollment token.
     *
     * @param qrContent  Raw string from QR scanner
     * @return enrollment token if found, null otherwise
     */
    fun parseEnrollmentQr(qrContent: String): String? {
        val normalizedContent = qrContent.trim().uppercase()
        
        // Strategy 1: Look for raw token string directly (DEV-XXXX-XXXX-XXXX)
        val tokenRegex = Regex("DEV-[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}")
        tokenRegex.find(normalizedContent)?.value?.let { return it }

        // Strategy 2: Look for JSON format
        return try {
            val map = Gson().fromJson(qrContent, Map::class.java)
            val extracted = ((map["enrollment_token"] as? String)
                ?: (map["com.devora.devicemanager.ENROLLMENT_TOKEN"] as? String))
            
            extracted?.trim()?.uppercase()?.let { 
                if (tokenRegex.matches(it)) it else null
            }
        } catch (e: Exception) {
            null
        }
    }
}
