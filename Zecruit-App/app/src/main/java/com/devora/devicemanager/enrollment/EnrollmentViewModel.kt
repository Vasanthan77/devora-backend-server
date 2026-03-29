package com.devora.devicemanager.enrollment

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.devora.devicemanager.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Represents the full UI state for the enrollment screen.
 */
data class EnrollmentUiState(
    val status: EnrollmentStatus = EnrollmentStatus.IDLE,
    val enrollmentResult: EnrollmentResult? = null,
    val tokenInput: String = "",
    val tokenValid: Boolean = false,
    val errorMessage: String? = null,
    val isDeviceOwner: Boolean = false,
    val stepIndex: Int = -1, // -1 = pre-enrollment, 0-4 = in-progress, 5 = success
    val qrBitmap: Bitmap? = null,
    val provisioningPayload: String? = null,
    val employeeName: String? = null,
    val employeeId: String? = null
)

/**
 * ViewModel for the enrollment flow.
 *
 * Manages enrollment state as a [StateFlow] consumed by Compose UI.
 * Coordinates between [EnrollmentRepository], [QrProvisioningHelper],
 * and [DevicePolicyHelper].
 */
class EnrollmentViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = EnrollmentRepository(application)
    private val policyHelper = DevicePolicyHelper(application)

    private val _uiState = MutableStateFlow(EnrollmentUiState(
        isDeviceOwner = policyHelper.isDeviceOwner
    ))
    val uiState: StateFlow<EnrollmentUiState> = _uiState.asStateFlow()

    init {
        val forceReEnroll = SessionManager.isForceReEnroll(getApplication())

        // Check if already enrolled
        if (repository.isEnrolled() && !forceReEnroll) {
            _uiState.value = _uiState.value.copy(
                status = EnrollmentStatus.SUCCESS,
                stepIndex = 5
            )
        } else {
            viewModelScope.launch {
                val recovered = repository.recoverEnrollmentFromServerIfNeeded()
                if (recovered != null) {
                    _uiState.value = _uiState.value.copy(
                        status = EnrollmentStatus.SUCCESS,
                        stepIndex = 5,
                        enrollmentResult = recovered,
                        employeeName = recovered.employeeName,
                        employeeId = recovered.employeeId,
                        isDeviceOwner = policyHelper.isDeviceOwner
                    )
                }
            }
        }
    }

    /**
     * Updates the token input and validates format in real-time.
     */
    fun onTokenChanged(raw: String) {
        _uiState.value = _uiState.value.copy(
            tokenInput = raw,
            tokenValid = repository.validateTokenFormat(raw),
            errorMessage = null
        )
    }

    /**
     * Starts enrollment using a manually-entered token.
     */
    fun enrollWithToken() {
        val token = _uiState.value.tokenInput.trim()
        if (token.isBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Please enter an enrollment token"
            )
            return
        }
        // Accept both legacy DEV-XXXX format and AMAPI tokens
        startEnrollment(token, "TOKEN")
    }

    /**
     * Starts enrollment using a token extracted from a scanned QR code.
     *
     * @param qrContent Raw content string from the QR scanner
     */
    fun enrollWithQrCode(qrContent: String) {
        val token = QrProvisioningHelper.parseEnrollmentQr(qrContent)
        if (token == null) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Invalid QR code — no enrollment token found"
            )
            return
        }
        _uiState.value = _uiState.value.copy(tokenInput = token)
        startEnrollment(token, "QR_CODE")
    }

    /**
     * Simulates a QR scan for demo purposes (uses a generated demo token).
     */
    fun simulateQrScan() {
        val demoToken = "DEV-" + (1..3).joinToString("-") {
            (1..4).map { "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".random() }.joinToString("")
        }
        _uiState.value = _uiState.value.copy(tokenInput = demoToken)
        startEnrollment(demoToken, "QR_CODE")
    }

    /**
     * Generates a provisioning QR code payload and bitmap for admin use.
     */
    fun generateProvisioningQr(
        wifiSsid: String? = null,
        wifiPassword: String? = null,
        enrollmentToken: String? = null
    ) {
        val payload = QrProvisioningHelper.buildProvisioningPayload(
            wifiSsid = wifiSsid,
            wifiPassword = wifiPassword,
            enrollmentToken = enrollmentToken
        )
        val bitmap = QrProvisioningHelper.generateQrBitmap(payload)
        _uiState.value = _uiState.value.copy(
            provisioningPayload = payload,
            qrBitmap = bitmap
        )
    }

    /**
     * Generates a simple enrollment token QR for an employee to scan.
     */
    fun generateTokenQr(token: String) {
        val bitmap = QrProvisioningHelper.generateEnrollmentTokenQr(token)
        _uiState.value = _uiState.value.copy(qrBitmap = bitmap)
    }

    /**
     * Resets enrollment state to allow re-enrollment.
     */
    fun resetEnrollment() {
        repository.clearEnrollmentState()
        _uiState.value = EnrollmentUiState(
            isDeviceOwner = policyHelper.isDeviceOwner
        )
    }

    /**
     * Returns the current device management status summary.
     */
    fun getManagementStatus(): Map<String, Any> = policyHelper.getManagementStatus()

    /**
     * Deletes a device from the backend
     * Removes all employee data and allows re-enrollment
     */
    fun deleteDevice(deviceId: String) {
        viewModelScope.launch {
            val result = repository.deleteDevice(deviceId)
            if (result) {
                // Device deleted successfully, reset enrollment state
                resetEnrollment()
            } else {
                // Deletion failed
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to delete device. Please try again."
                )
            }
        }
    }

    // ─────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────

    private fun startEnrollment(token: String, method: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                status = EnrollmentStatus.PENDING,
                stepIndex = 0,
                errorMessage = null
            )

            val result = repository.enroll(
                token = token,
                method = method,
                onStepChanged = { step ->
                    val stepIndex = when (step) {
                        EnrollmentStatus.VALIDATING_TOKEN -> 0
                        EnrollmentStatus.CONNECTING -> 1
                        EnrollmentStatus.INSTALLING_POLICIES -> 2
                        EnrollmentStatus.CONFIGURING_DEVICE_OWNER -> 3
                        EnrollmentStatus.FINALIZING -> 4
                        EnrollmentStatus.SUCCESS -> 5
                        else -> _uiState.value.stepIndex
                    }
                    _uiState.value = _uiState.value.copy(
                        status = step,
                        stepIndex = stepIndex
                    )
                }
            )

            if (result.errorMessage != null) {
                _uiState.value = _uiState.value.copy(
                    status = EnrollmentStatus.FAILED,
                    enrollmentResult = result,
                    errorMessage = result.errorMessage,
                    stepIndex = -1
                )
            } else {
                // Apply Device Owner lock screen info if we are the Device Owner
                if (policyHelper.isDeviceOwner) {
                    policyHelper.setDeviceOwnerLockScreenInfo(
                        "Managed by DEVORA MDM\nDevice ID: ${result.deviceId}"
                    )
                }

                _uiState.value = _uiState.value.copy(
                    status = EnrollmentStatus.SUCCESS,
                    enrollmentResult = result,
                    stepIndex = 5,
                    isDeviceOwner = policyHelper.isDeviceOwner,
                    employeeName = result.employeeName,
                    employeeId = result.employeeId
                )
            }
        }
    }

    private fun formatToken(input: String): String {
        val clean = input.replace("-", "")
            .filter { it.isLetterOrDigit() }
            .uppercase()
            .take(16)
        return clean.chunked(4).joinToString("-")
    }
}
