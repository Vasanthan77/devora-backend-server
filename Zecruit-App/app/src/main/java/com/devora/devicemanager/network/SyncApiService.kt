package com.devora.devicemanager.network

import com.devora.devicemanager.network.model.ApiResponse
import com.devora.devicemanager.network.model.BulkAppInventoryRequest
import com.devora.devicemanager.network.model.SyncDeviceInfoRequest
import com.devora.devicemanager.network.model.SyncEnrollRequest
import com.devora.devicemanager.network.model.SyncEnrollResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit API interface dedicated to the device-sync flow.
 * Endpoints match the mdm-backend REST controllers.
 */
interface SyncApiService {

    @POST("api/enroll")
    suspend fun enroll(@Body request: SyncEnrollRequest): Response<SyncEnrollResponse>

    @POST("api/device-info")
    suspend fun uploadDeviceInfo(@Body request: SyncDeviceInfoRequest): Response<ApiResponse>

    @POST("api/app-inventory")
    suspend fun uploadAppInventory(@Body request: BulkAppInventoryRequest): Response<ApiResponse>
}
