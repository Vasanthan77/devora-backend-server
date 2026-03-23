package com.devora.devicemanager.enrollment

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.gson.Gson
import com.google.gson.GsonBuilder

/**
 * Generates QR code payloads for Android Device Owner provisioning.
 *
 * The QR code follows the Android Enterprise provisioning format:
 *   - The JSON payload contains provisioning extras that the Android setup wizard
 *     reads during factory-reset provisioning (tap QR at "Welcome" screen 6 times).
 *   - Keys are the standard `android.app.extra.PROVISIONING_*` extras.
 *
 * @see <a href="https://developer.android.com/work/dpc/build-dpc#qr_code_method">
 *   Android Device Owner QR provisioning</a>
 */
object QrProvisioningHelper {

    private const val KEY_ADMIN_COMPONENT =
        "android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME"
    private const val KEY_PACKAGE_DOWNLOAD_LOCATION =
        "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION"
    private const val KEY_PACKAGE_CHECKSUM =
        "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_CHECKSUM"
    private const val KEY_SKIP_ENCRYPTION =
        "android.app.extra.PROVISIONING_SKIP_ENCRYPTION"
    private const val KEY_WIFI_SSID =
        "android.app.extra.PROVISIONING_WIFI_SSID"
    private const val KEY_WIFI_PASSWORD =
        "android.app.extra.PROVISIONING_WIFI_PASSWORD"
    private const val KEY_WIFI_SECURITY_TYPE =
        "android.app.extra.PROVISIONING_WIFI_SECURITY_TYPE"
    private const val KEY_LEAVE_ALL_SYSTEM_APPS =
        "android.app.extra.PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED"
    private const val KEY_LOCALE =
        "android.app.extra.PROVISIONING_LOCALE"
    private const val KEY_TIME_ZONE =
        "android.app.extra.PROVISIONING_TIME_ZONE"

    // Default values — should be configured by the admin via Settings screen
    private const val DEFAULT_ADMIN_COMPONENT =
        "com.devora.devicemanager/com.devora.devicemanager.AdminReceiver"
    private const val DEFAULT_APK_DOWNLOAD_URL =
        "https://devora-backend-server-production.up.railway.app/downloads/devora-mdm-latest.apk"
    private const val DEFAULT_CHECKSUM =
        "zr4L-_kEAKszSP5zY_epOfpBsBdGCdcGCAcXVo_SHyI"

    /**
     * Builds the JSON payload for a Device Owner provisioning QR code.
     *
     * @param adminComponent   Fully-qualified ComponentName (package/class)
     * @param apkDownloadUrl   URL where the setup wizard downloads the DPC APK
     * @param checksum         SHA-256 checksum of the APK (Base64 URL-safe)
     * @param skipEncryption   Whether to skip forcing device encryption
     * @param wifiSsid         Optional Wi-Fi SSID to auto-connect during provisioning
     * @param wifiPassword     Optional Wi-Fi password
     * @param wifiSecurityType Optional Wi-Fi security (e.g. "WPA", "WEP")
     * @param enrollmentToken  Custom enrollment token embedded in the payload
     */
    fun buildProvisioningPayload(
        adminComponent: String = DEFAULT_ADMIN_COMPONENT,
        apkDownloadUrl: String = DEFAULT_APK_DOWNLOAD_URL,
        checksum: String = DEFAULT_CHECKSUM,
        skipEncryption: Boolean = true,
        wifiSsid: String? = null,
        wifiPassword: String? = null,
        wifiSecurityType: String? = null,
        enrollmentToken: String? = null
    ): String {
        val payload = linkedMapOf<String, Any>()

        // Required: Admin component in "package/class" format
        payload[KEY_ADMIN_COMPONENT] = adminComponent

        // Required: Where to download the DPC APK from
        payload[KEY_PACKAGE_DOWNLOAD_LOCATION] = apkDownloadUrl

        // Required: SHA-256 checksum to verify APK integrity
        payload[KEY_PACKAGE_CHECKSUM] = checksum

        // Optional: Skip encryption to speed up provisioning (development / unmanaged devices)
        payload[KEY_SKIP_ENCRYPTION] = skipEncryption

        // Optional: Pre-configure Wi-Fi so the device can download the DPC APK
        if (!wifiSsid.isNullOrBlank()) {
            payload[KEY_WIFI_SSID] = wifiSsid
            if (!wifiPassword.isNullOrBlank()) {
                payload[KEY_WIFI_PASSWORD] = wifiPassword
                payload[KEY_WIFI_SECURITY_TYPE] = wifiSecurityType ?: "WPA"
            }
        }

        // Keep all system apps enabled during provisioning
        payload[KEY_LEAVE_ALL_SYSTEM_APPS] = true

        // Custom: Include enrollment token so the DPC can read it after provisioning
        if (!enrollmentToken.isNullOrBlank()) {
            payload["com.devora.devicemanager.ENROLLMENT_TOKEN"] = enrollmentToken
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
     * Generates a full Device Owner provisioning QR bitmap.
     * This QR contains the complete JSON payload that Android's setup wizard reads
     * after factory reset (tap "Welcome" screen 6 times to trigger QR scanner).
     *
     * The setup wizard will:
     *  1. Connect to Wi-Fi (if provided)
     *  2. Download the DPC APK from the specified URL
     *  3. Verify the APK checksum
     *  4. Install the APK and set it as Device Owner
     *  5. Launch the app via onProfileProvisioningComplete()
     */
    fun generateDeviceOwnerProvisioningQr(
        apkDownloadUrl: String = DEFAULT_APK_DOWNLOAD_URL,
        checksum: String = DEFAULT_CHECKSUM,
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
