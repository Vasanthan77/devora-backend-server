package com.devora.devicemanager.network

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import okhttp3.OkHttpClient
import okhttp3.Credentials
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

// ══════════════════════════════════════
// DTOs — match the mdm-backend models
// ══════════════════════════════════════

data class EnrollRequest(
    @SerializedName("deviceId") val deviceId: String,
    @SerializedName("enrollmentToken") val enrollmentToken: String,
    @SerializedName("enrollmentMethod") val enrollmentMethod: String  // "QR_CODE" or "TOKEN"
)

data class EnrollResponse(
    @SerializedName("id") val id: Long?,
    @SerializedName("deviceId") val deviceId: String?,
    @SerializedName("enrollmentToken") val enrollmentToken: String?,
    @SerializedName("enrollmentMethod") val enrollmentMethod: String?,
    @SerializedName("enrolledAt") val enrolledAt: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("employeeId") val employeeId: String? = null,
    @SerializedName("employeeName") val employeeName: String? = null
)

data class DeviceInfoRequest(
    @SerializedName("deviceId") val deviceId: String,
    @SerializedName("model") val model: String,
    @SerializedName("manufacturer") val manufacturer: String,
    @SerializedName("osVersion") val osVersion: String,
    @SerializedName("sdkVersion") val sdkVersion: Int,
    @SerializedName("serialNumber") val serialNumber: String?,
    @SerializedName("imei") val imei: String?,
    @SerializedName("deviceType") val deviceType: String?,
    @SerializedName("deviceOwnerSet") val deviceOwnerSet: Boolean? = null,
    @SerializedName("employeeId") val employeeId: String? = null
)

data class AppInventoryRequest(
    @SerializedName("deviceId") val deviceId: String,
    @SerializedName("appName") val appName: String,
    @SerializedName("packageName") val packageName: String,
    @SerializedName("versionName") val versionName: String,
    @SerializedName("versionCode") val versionCode: Long,
    @SerializedName("installSource") val installSource: String,
    @SerializedName("isSystemApp") val isSystemApp: Boolean
)

data class AppInventoryItem(
    @SerializedName("id") val id: Long,
    @SerializedName("deviceId") val deviceId: String,
    @SerializedName("appName") val appName: String,
    @SerializedName("packageName") val packageName: String,
    @SerializedName("versionName") val versionName: String?,
    @SerializedName("versionCode") val versionCode: Long?,
    @SerializedName("installSource") val installSource: String?,
    @SerializedName("isSystemApp") val isSystemApp: Boolean?,
    @SerializedName("iconBase64") val iconBase64: String?,
    @SerializedName("collectedAt") val collectedAt: String?
)

data class NewAppNotification(
    @SerializedName("deviceId") val deviceId: String,
    @SerializedName("appName") val appName: String,
    @SerializedName("packageName") val packageName: String,
    @SerializedName("versionName") val versionName: String,
    @SerializedName("versionCode") val versionCode: Long,
    @SerializedName("isSystemApp") val isSystemApp: Boolean,
    @SerializedName("action") val action: String // "INSTALLED" or "UPDATED"
)

data class AdminNotification(
    @SerializedName("id") val id: Long,
    @SerializedName("deviceId") val deviceId: String,
    @SerializedName("type") val type: String,
    @SerializedName("title") val title: String,
    @SerializedName("message") val message: String?,
    @SerializedName("read") val read: Boolean,
    @SerializedName("createdAt") val createdAt: String?
)

data class RestrictedApp(
    @SerializedName("id") val id: Long,
    @SerializedName("deviceId") val deviceId: String,
    @SerializedName("packageName") val packageName: String,
    @SerializedName("appName") val appName: String?,
    @SerializedName("restrictedAt") val restrictedAt: String?
)

data class RestrictAppRequest(
    @SerializedName("packageName") val packageName: String,
    @SerializedName("appName") val appName: String
)

// ── New DTOs for MDM features ──

data class DeviceAppRestrictionResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("deviceId") val deviceId: String,
    @SerializedName("packageName") val packageName: String,
    @SerializedName("appName") val appName: String?,
    @SerializedName("installSource") val installSource: String?,
    @SerializedName("restricted") val restricted: Boolean,
    @SerializedName("appliedAt") val appliedAt: String?,
    @SerializedName("appliedBy") val appliedBy: String?
)

data class RestrictAppRequestNew(
    @SerializedName("packageName") val packageName: String,
    @SerializedName("appName") val appName: String,
    @SerializedName("installSource") val installSource: String?,
    @SerializedName("restricted") val restricted: Boolean
)

data class DeviceActivityResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("deviceId") val deviceId: String?,
    @SerializedName("employeeName") val employeeName: String?,
    @SerializedName("activityType") val activityType: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("severity") val severity: String?,
    @SerializedName("createdAt") val createdAt: String?
)

data class MdmAlertResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("deviceId") val deviceId: String?,
    @SerializedName("employeeName") val employeeName: String?,
    @SerializedName("alertType") val alertType: String?,
    @SerializedName("message") val message: String?,
    @SerializedName("isRead") val isRead: Boolean,
    @SerializedName("severity") val severity: String?,
    @SerializedName("createdAt") val createdAt: String?
)

data class DevicePolicyResponse(
    @SerializedName("id") val id: Long?,
    @SerializedName("deviceId") val deviceId: String?,
    @SerializedName("cameraDisabled") val cameraDisabled: Boolean,
    @SerializedName("screenLockRequired") val screenLockRequired: Boolean,
    @SerializedName("installBlocked") val installBlocked: Boolean,
    @SerializedName("uninstallBlocked") val uninstallBlocked: Boolean,
    @SerializedName("locationTrackingEnabled") val locationTrackingEnabled: Boolean,
    @SerializedName("appliedAt") val appliedAt: String?
)

data class PolicyUpdateRequest(
    @SerializedName("cameraDisabled") val cameraDisabled: Boolean? = null,
    @SerializedName("screenLockRequired") val screenLockRequired: Boolean? = null,
    @SerializedName("installBlocked") val installBlocked: Boolean? = null,
    @SerializedName("uninstallBlocked") val uninstallBlocked: Boolean? = null,
    @SerializedName("locationTrackingEnabled") val locationTrackingEnabled: Boolean? = null
)

data class DeviceLocationResponse(
    @SerializedName("id") val id: Long?,
    @SerializedName("deviceId") val deviceId: String?,
    @SerializedName("latitude") val latitude: Double?,
    @SerializedName("longitude") val longitude: Double?,
    @SerializedName("accuracy") val accuracy: Float?,
    @SerializedName("altitude") val altitude: Double? = null,
    @SerializedName("bearing") val bearing: Float? = null,
    @SerializedName("speed") val speed: Float? = null,
    @SerializedName("address") val address: String?,
    @SerializedName("recordedAt") val recordedAt: String?
)

data class LocationRequest(
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("accuracy") val accuracy: Float,
    @SerializedName("timestamp") val timestamp: Long
)

data class LocationResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("deviceId") val deviceId: String,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("accuracy") val accuracy: Float,
    @SerializedName("altitude") val altitude: Double? = null,
    @SerializedName("bearing") val bearing: Float? = null,
    @SerializedName("speed") val speed: Float? = null,
    @SerializedName("address") val address: String?,
    @SerializedName("recordedAt") val recordedAt: String
)

data class LocationReportRequest(
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("accuracy") val accuracy: Float,
    @SerializedName("altitude") val altitude: Double? = null,
    @SerializedName("bearing") val bearing: Float? = null,
    @SerializedName("speed") val speed: Float? = null
)

data class DeviceCommandResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("deviceId") val deviceId: String?,
    @SerializedName("commandType") val commandType: String?,
    @SerializedName("executed") val executed: Boolean,
    @SerializedName("createdAt") val createdAt: String?,
    @SerializedName("executedAt") val executedAt: String?
)

data class CommandRequest(
    @SerializedName("type") val type: String,
    @SerializedName("packageName") val packageName: String? = null
)

data class CommandQueueResponse(
    @SerializedName("message") val message: String?,
    @SerializedName("commandId") val commandId: Long?,
    @SerializedName("status") val status: String?,
    @SerializedName("commandType") val commandType: String?
)

data class CommandStatusResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("deviceId") val deviceId: String?,
    @SerializedName("commandType") val commandType: String?,
    @SerializedName("executed") val executed: Boolean,
    @SerializedName("status") val status: String?,
    @SerializedName("createdAt") val createdAt: String?,
    @SerializedName("executedAt") val executedAt: String?
)

data class UnreadCountResponse(
    @SerializedName("count") val count: Int
)

data class MarkAlertsReadRequest(
    @SerializedName("alertIds") val alertIds: List<Long>
)

data class DashboardStats(
    @SerializedName("totalDevices") val totalDevices: Int,
    @SerializedName("activeDevices") val activeDevices: Int,
    @SerializedName(value = "violations", alternate = ["inactiveDevices"]) val violations: Int,
    @SerializedName("totalApps") val totalApps: Int
)

data class DeviceResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("deviceId") val deviceId: String,
    @SerializedName("enrollmentToken") val enrollmentToken: String?,
    @SerializedName("enrollmentMethod") val enrollmentMethod: String,
    @SerializedName("enrolledAt") val enrolledAt: String,
    @SerializedName("status") val status: String,
    @SerializedName("employeeId") val employeeId: String? = null,
    @SerializedName("employeeName") val employeeName: String? = null,
    @SerializedName("deviceModel") val deviceModel: String? = null,
    @SerializedName("manufacturer") val manufacturer: String? = null,
    @SerializedName("osVersion") val osVersion: String? = null,
    @SerializedName("sdkVersion") val sdkVersion: String? = null,
    @SerializedName("serialNumber") val serialNumber: String? = null,
    @SerializedName("deviceOwnerSet") val deviceOwnerSet: Boolean? = null
)

data class TokenValidationRequest(
    @SerializedName("token") val token: String,
    @SerializedName("deviceId") val deviceId: String
)

data class TokenValidationResponse(
    @SerializedName("valid") val valid: Boolean,
    @SerializedName("message") val message: String?
)

data class DeleteDeviceResponse(
    @SerializedName("message") val message: String?,
    @SerializedName("deviceId") val deviceId: String?
)

data class AdminRegisterRequest(
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class AdminLoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class AdminLoginResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("name") val name: String?,
    @SerializedName("message") val message: String?
)

data class AdminRegisterResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?
)

data class GenerateEnrollmentTokenRequest(
    @SerializedName("employeeId") val employeeId: String,
    @SerializedName("employeeName") val employeeName: String,
    @SerializedName("type") val type: String
)

data class GenerateEnrollmentTokenResponse(
    @SerializedName("token") val token: String,
    @SerializedName("employeeId") val employeeId: String?,
    @SerializedName("expiresAt") val expiresAt: String?,
    @SerializedName("qrCode") val qrCode: String?
)

data class EnrollmentTokenResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("token") val token: String,
    @SerializedName("employeeId") val employeeId: String,
    @SerializedName("employeeName") val employeeName: String,
    @SerializedName("createdAt") val createdAt: String?,
    @SerializedName("expiresAt") val expiresAt: String?,
    @SerializedName("status") val status: String,
    @SerializedName("deviceId") val deviceId: String?
)

// ══════════════════════════════════════
// RETROFIT API INTERFACE
// ══════════════════════════════════════

interface EnrollmentApiService {

    @POST("api/enrollment/generate")
    suspend fun generateEnrollmentToken(
        @Body request: GenerateEnrollmentTokenRequest
    ): Response<GenerateEnrollmentTokenResponse>

    @POST("api/enroll")
    suspend fun enrollDevice(@Body request: EnrollRequest): Response<EnrollResponse>

    @GET("api/devices/{deviceId}")
    suspend fun getDevice(@Path("deviceId") deviceId: String): Response<EnrollResponse>

    @GET("api/devices")
    suspend fun getAllDevices(): Response<List<EnrollResponse>>

    @GET("api/devices")
    suspend fun getDeviceList(): Response<List<DeviceResponse>>

    @POST("api/device-info")
    suspend fun uploadDeviceInfo(@Body request: DeviceInfoRequest): Response<Unit>

    @POST("api/app-inventory")
    suspend fun uploadAppInventory(@Body request: AppInventoryRequest): Response<Unit>

    @GET("api/app-inventory/{deviceId}")
    suspend fun getAppInventory(@Path("deviceId") deviceId: String): Response<List<AppInventoryItem>>

    @POST("api/app-inventory/notify")
    suspend fun notifyNewApp(@Body notification: NewAppNotification): Response<Unit>

    @GET("api/notifications")
    suspend fun getNotifications(): Response<List<AdminNotification>>

    @POST("api/devices/{deviceId}/heartbeat")
    suspend fun sendHeartbeat(@Path("deviceId") deviceId: String): Response<Unit>

    @POST("api/devices/{deviceId}/sign-out")
    suspend fun signOutDevice(@Path("deviceId") deviceId: String): Response<Unit>

    @GET("api/dashboard/stats")
    suspend fun getDashboardStats(): Response<DashboardStats>

    @POST("api/admin/register")
    suspend fun registerAdmin(@Body request: AdminRegisterRequest): Response<AdminRegisterResponse>

    @POST("api/admin/login")
    suspend fun loginAdmin(@Body request: AdminLoginRequest): Response<AdminLoginResponse>

    @DELETE("api/devices/{deviceId}")
    suspend fun deleteDevice(@Path("deviceId") deviceId: String): Response<Map<String, String>>

    @GET("api/devices/check/{deviceId}")
    suspend fun checkDevice(@Path("deviceId") deviceId: String): Response<DeviceResponse>

    @GET("api/enrollment/active")
    suspend fun getActiveEnrollments(): Response<List<EnrollmentTokenResponse>>

    @DELETE("api/enrollment/{tokenId}")
    suspend fun revokeEnrollmentToken(@Path("tokenId") tokenId: Long): Response<Map<String, String>>

    @DELETE("api/activities/{activityId}")
    suspend fun deleteActivity(@Path("activityId") activityId: Long): Response<Unit>

    @POST("api/devices/{deviceId}/restrict-app")
    suspend fun restrictApp(
        @Path("deviceId") deviceId: String,
        @Body request: RestrictAppRequestNew
    ): Response<Map<String, String>>

    @GET("api/devices/{deviceId}/restricted-apps")
    suspend fun getRestrictedApps(
        @Path("deviceId") deviceId: String
    ): Response<List<DeviceAppRestrictionResponse>>

    @GET("api/devices/{deviceId}/app-restrictions")
    suspend fun getAllAppRestrictions(
        @Path("deviceId") deviceId: String
    ): Response<List<DeviceAppRestrictionResponse>>

    // ── Activities ──

    @GET("api/activities")
    suspend fun getActivities(
        @Query("limit") limit: Int = 10
    ): Response<List<DeviceActivityResponse>>

    @GET("api/activities/device/{deviceId}")
    suspend fun getDeviceActivities(
        @Path("deviceId") deviceId: String
    ): Response<List<DeviceActivityResponse>>

    // ── Alerts ──

    @GET("api/alerts/unread")
    suspend fun getUnreadAlerts(): Response<List<MdmAlertResponse>>

    @GET("api/alerts/unread-count")
    suspend fun getUnreadAlertCount(): Response<UnreadCountResponse>

    @POST("api/alerts/mark-read")
    suspend fun markAlertsRead(
        @Body request: MarkAlertsReadRequest
    ): Response<Map<String, String>>

    // ── Policies ──

    @GET("api/devices/{deviceId}/policies")
    suspend fun getDevicePolicies(
        @Path("deviceId") deviceId: String
    ): Response<DevicePolicyResponse>

    @POST("api/devices/{deviceId}/policy")
    suspend fun updateDevicePolicy(
        @Path("deviceId") deviceId: String,
        @Body request: PolicyUpdateRequest
    ): Response<DevicePolicyResponse>

    // ── Commands ──

    @POST("api/devices/{deviceId}/lock")
    suspend fun lockDevice(
        @Path("deviceId") deviceId: String
    ): Response<CommandQueueResponse>

    @POST("api/devices/{deviceId}/wipe")
    suspend fun wipeDevice(
        @Path("deviceId") deviceId: String
    ): Response<CommandQueueResponse>

    @POST("api/devices/{deviceId}/command")
    suspend fun createDeviceCommand(
        @Path("deviceId") deviceId: String,
        @Body request: CommandRequest
    ): Response<CommandQueueResponse>

    @GET("api/devices/{deviceId}/commands/{commandId}")
    suspend fun getCommandStatus(
        @Path("deviceId") deviceId: String,
        @Path("commandId") commandId: Long
    ): Response<CommandStatusResponse>

    @GET("api/devices/{deviceId}/pending-commands")
    suspend fun getPendingCommands(
        @Path("deviceId") deviceId: String
    ): Response<List<DeviceCommandResponse>>

    @POST("api/devices/{deviceId}/commands/{commandId}/ack")
    suspend fun ackCommand(
        @Path("deviceId") deviceId: String,
        @Path("commandId") commandId: Long
    ): Response<Map<String, String>>

    // ── Location ──

    @POST("api/devices/{deviceId}/accurate-location")
    suspend fun updateLocation(
        @Path("deviceId") deviceId: String,
        @Body request: LocationRequest
    ): Response<Unit>

    @POST("api/devices/{deviceId}/accurate-location")
    suspend fun reportLocation(
        @Path("deviceId") deviceId: String,
        @Body request: LocationReportRequest
    ): Response<Map<String, String>>

    @GET("api/devices/{deviceId}/accurate-location")
    suspend fun getDeviceLocation(
        @Path("deviceId") deviceId: String
    ): Response<DeviceLocationResponse>

    @GET("api/devices/{deviceId}/accurate-location/history")
    suspend fun getLocationHistory(
        @Path("deviceId") deviceId: String,
        @Query("limit") limit: Int = 5
    ): Response<List<DeviceLocationResponse>>

    // ── Direct AMAPI Management ──

    @GET("api/amapi/devices")
    suspend fun getAmapiDevices(
        @Query("enterpriseName") enterpriseName: String? = null
    ): Response<String>

    @GET("api/amapi/devices/{deviceId}")
    suspend fun getAmapiDevice(
        @Path("deviceId") deviceId: String,
        @Query("enterpriseName") enterpriseName: String? = null
    ): Response<String>

    @POST("api/amapi/devices/{deviceId}/lock")
    suspend fun lockDeviceAmapi(
        @Path("deviceId") deviceId: String,
        @Query("enterpriseName") enterpriseName: String? = null
    ): Response<String>

    @POST("api/amapi/devices/{deviceId}/wipe")
    suspend fun wipeDeviceAmapi(
        @Path("deviceId") deviceId: String,
        @Query("enterpriseName") enterpriseName: String? = null
    ): Response<String>
}

// ══════════════════════════════════════
// RETROFIT CLIENT SINGLETON
// ══════════════════════════════════════

object RetrofitClient {

    // Default base URL — Railway hosted backend
    private const val DEFAULT_BASE_URL = "https://devora-backend-server-production.up.railway.app/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private const val API_USERNAME = "mdm-device"
    private const val API_PASSWORD = "SecurePass123"

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val requestWithAuth = chain.request().newBuilder()
                .header("Authorization", Credentials.basic(API_USERNAME, API_PASSWORD))
                .build()
            chain.proceed(requestWithAuth)
        }
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val api: EnrollmentApiService by lazy {
        Retrofit.Builder()
            .baseUrl(DEFAULT_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(EnrollmentApiService::class.java)
    }

    /** Creates a client with a custom base URL (e.g. from Settings screen). */
    fun createWithBaseUrl(baseUrl: String): EnrollmentApiService {
        val url = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return Retrofit.Builder()
            .baseUrl(url)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(EnrollmentApiService::class.java)
    }
}
