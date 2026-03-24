package com.devora.devicemanager.data.remote

import com.devora.devicemanager.network.AdminLoginRequest
import com.devora.devicemanager.network.AdminRegisterRequest
import com.devora.devicemanager.network.DeviceInfoRequest
import com.devora.devicemanager.network.GenerateEnrollmentTokenRequest
import com.devora.devicemanager.network.LocationReportRequest
import com.devora.devicemanager.network.MarkAlertsReadRequest
import com.devora.devicemanager.network.PolicyUpdateRequest
import com.devora.devicemanager.network.RestrictAppRequestNew
import com.devora.devicemanager.network.RetrofitClient

object RemoteDataSource {

    suspend fun getDashboardStats() = RetrofitClient.api.getDashboardStats()

    suspend fun getActivities(limit: Int = 10) = RetrofitClient.api.getActivities(limit)

    suspend fun getDeviceList() = RetrofitClient.api.getDeviceList()

    suspend fun getDeviceActivities(deviceId: String) = RetrofitClient.api.getDeviceActivities(deviceId)

    suspend fun getDevicePolicies(deviceId: String) = RetrofitClient.api.getDevicePolicies(deviceId)

    suspend fun getDeviceLocation(deviceId: String) = RetrofitClient.api.getDeviceLocation(deviceId)

    suspend fun updateDevicePolicy(deviceId: String, request: PolicyUpdateRequest) =
        RetrofitClient.api.updateDevicePolicy(deviceId, request)

    suspend fun getAppInventory(deviceId: String) = RetrofitClient.api.getAppInventory(deviceId)

    suspend fun getRestrictedApps(deviceId: String) = RetrofitClient.api.getRestrictedApps(deviceId)

    suspend fun restrictApp(deviceId: String, request: RestrictAppRequestNew) =
        RetrofitClient.api.restrictApp(deviceId, request)

    suspend fun lockDevice(deviceId: String) = RetrofitClient.api.lockDevice(deviceId)

    suspend fun wipeDevice(deviceId: String) = RetrofitClient.api.wipeDevice(deviceId)

    suspend fun deleteDevice(deviceId: String) = RetrofitClient.api.deleteDevice(deviceId)

    suspend fun checkDevice(deviceId: String) = RetrofitClient.api.checkDevice(deviceId)

    suspend fun signOutDevice(deviceId: String) = RetrofitClient.api.signOutDevice(deviceId)

    suspend fun loginAdmin(request: AdminLoginRequest) = RetrofitClient.api.loginAdmin(request)

    suspend fun registerAdmin(request: AdminRegisterRequest) = RetrofitClient.api.registerAdmin(request)

    suspend fun getActiveEnrollments() = RetrofitClient.api.getActiveEnrollments()

    suspend fun revokeEnrollmentToken(tokenId: Long) = RetrofitClient.api.revokeEnrollmentToken(tokenId)

    suspend fun generateEnrollmentToken(request: GenerateEnrollmentTokenRequest) =
        RetrofitClient.api.generateEnrollmentToken(request)

    suspend fun deleteActivity(activityId: Long) = RetrofitClient.api.deleteActivity(activityId)

    suspend fun uploadDeviceInfo(request: DeviceInfoRequest) =
        RetrofitClient.api.uploadDeviceInfo(request)

    suspend fun reportLocation(deviceId: String, request: LocationReportRequest) =
        RetrofitClient.api.reportLocation(deviceId, request)

    suspend fun markAlertsRead(request: MarkAlertsReadRequest) =
        RetrofitClient.api.markAlertsRead(request)

    suspend fun getAmapiDevices(enterpriseName: String? = null) =
        RetrofitClient.api.getAmapiDevices(enterpriseName)

    suspend fun getAmapiDevice(deviceId: String, enterpriseName: String? = null) =
        RetrofitClient.api.getAmapiDevice(deviceId, enterpriseName)

    suspend fun lockDeviceAmapi(deviceId: String, enterpriseName: String? = null) =
        RetrofitClient.api.lockDeviceAmapi(deviceId, enterpriseName)

    suspend fun wipeDeviceAmapi(deviceId: String, enterpriseName: String? = null) =
        RetrofitClient.api.wipeDeviceAmapi(deviceId, enterpriseName)
}
