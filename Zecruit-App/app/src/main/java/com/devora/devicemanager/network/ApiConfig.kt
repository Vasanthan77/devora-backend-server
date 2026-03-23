package com.devora.devicemanager.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Centralized API configuration for both emulator and physical device.
 * - Emulator:  uses 10.0.2.2 which maps to the host machine's localhost.
 * - Phone:     uses the laptop's WiFi IP (replace X with your actual IP).
 */
object ApiConfig {

    /** Emulator → host localhost (10.0.2.2 maps to host machine) */
    const val EMULATOR_BASE_URL = "http://10.0.2.2:8080/"

    /** Production backend hosted on Railway */
    const val PHONE_BASE_URL = "https://devora-backend-server-production.up.railway.app/"

    /** Active base URL — change this when switching between emulator and phone */
    var BASE_URL: String = PHONE_BASE_URL
        private set

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private var retrofit: Retrofit = buildRetrofit(BASE_URL)

    private fun buildRetrofit(baseUrl: String): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /** Get the sync API service using the current BASE_URL */
    val syncApi: SyncApiService by lazy {
        retrofit.create(SyncApiService::class.java)
    }

    /**
     * Switch to a different base URL at runtime (e.g. from Settings screen).
     * Returns a new SyncApiService instance pointing to that URL.
     */
    fun createSyncApiWithUrl(baseUrl: String): SyncApiService {
        val url = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return buildRetrofit(url).create(SyncApiService::class.java)
    }
}
