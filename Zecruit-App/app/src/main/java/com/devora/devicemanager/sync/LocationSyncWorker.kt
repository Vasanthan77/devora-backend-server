package com.devora.devicemanager.sync

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.devora.devicemanager.network.LocationReportRequest
import com.devora.devicemanager.network.RetrofitClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Periodic worker (every 5 min) that reports GPS location to the backend.
 * Only reports if locationTrackingEnabled in device policies and location permission granted.
 */
class LocationSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "LocationSyncWorker"
        private const val WORK_NAME = "devora_location_sync"
        private const val WORK_NOW_NAME = "devora_location_sync_now"
        private const val INPUT_FORCE_ONCE = "input_force_once"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<LocationSyncWorker>(5, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
            Log.d(TAG, "Location sync worker scheduled")
        }

        fun scheduleNow(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<LocationSyncWorker>()
                .setInputData(
                    Data.Builder()
                        .putBoolean(INPUT_FORCE_ONCE, true)
                        .build()
                )
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NOW_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
            Log.d(TAG, "Immediate location sync scheduled")
        }
    }

    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("devora_enrollment", Context.MODE_PRIVATE)
        val deviceId = prefs.getString("device_id", null) ?: return Result.success()
        val forceOnce = inputData.getBoolean(INPUT_FORCE_ONCE, false)

        try {
            val policyResponse = RetrofitClient.api.getDevicePolicies(deviceId)
            if (policyResponse.isSuccessful) {
                val policy = policyResponse.body()
                if (!forceOnce && policy != null && !policy.locationTrackingEnabled) {
                    Log.d(TAG, "Location tracking disabled by policy")
                    return Result.success()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to check policy: ${e.message}")
            return Result.retry()
        }

        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Location permission not granted")
            return Result.success()
        }

        try {
            val location = getCurrentLocation() ?: run {
                Log.w(TAG, "Failed to get location")
                return Result.retry()
            }

            val request = LocationReportRequest(
                latitude = location.latitude,
                longitude = location.longitude,
                accuracy = location.accuracy,
                altitude = location.altitude,
                bearing = location.bearing,
                speed = location.speed
            )

            val response = RetrofitClient.api.reportLocation(deviceId, request)
            if (response.isSuccessful) {
                Log.d(TAG, "Location reported: ${location.latitude}, ${location.longitude}, accuracy=${location.accuracy}m")
            } else {
                Log.w(TAG, "Location report failed: ${response.code()}")
                return Result.retry()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Location sync failed: ${e.message}")
            return Result.retry()
        }

        return Result.success()
    }

    @Suppress("MissingPermission")
    private suspend fun getCurrentLocation(): Location? {
        return try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(applicationContext)

            if (ActivityCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return null
            }

            // Try to get last known location (if recent and reasonably accurate)
            val lastLocation = fusedClient.lastLocation.await()
            if (lastLocation != null && System.currentTimeMillis() - lastLocation.time < 120_000L && lastLocation.accuracy < 100f) {
                Log.d(TAG, "Using last known location: accuracy=${lastLocation.accuracy}m")
                return lastLocation
            }

            // Request HIGH_ACCURACY location with extended timeout for better GPS fix
            val bestLocation = requestHighAccuracyLocation(fusedClient)
            if (bestLocation != null && bestLocation.accuracy < 100f) {
                Log.d(TAG, "Got high accuracy location: accuracy=${bestLocation.accuracy}m")
                return bestLocation
            }

            // Fallback to BALANCED_POWER_ACCURACY if HIGH_ACCURACY failed or accuracy is poor
            val balancedLocation = requestBalancedLocation(fusedClient)
            Log.d(TAG, "Using fallback location: accuracy=${balancedLocation?.accuracy}m")
            return balancedLocation
        } catch (e: Exception) {
            Log.e(TAG, "Error getting location: ${e.message}")
            null
        }
    }

    @Suppress("MissingPermission")
    private suspend fun requestHighAccuracyLocation(fusedClient: com.google.android.gms.location.FusedLocationProviderClient): Location? {
        return try {
            val bestLocation = mutableListOf<Location>()
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2_000L)
                .setMinUpdateIntervalMillis(1_000L)
                .setMaxUpdates(15)  // Allow up to 15 updates to find best location
                .build()

            suspendCancellableCoroutine { continuation ->
                val callback = object : LocationCallback() {
                    override fun onLocationResult(result: LocationResult) {
                        for (location in result.locations) {
                            bestLocation.add(location)
                            // If we have a location with good accuracy, we can stop early
                            if (location.accuracy < 50f && bestLocation.size >= 3) {
                                fusedClient.removeLocationUpdates(this)
                                continuation.resume(location)
                                return
                            }
                        }
                        // If max updates reached, return the best location so far
                        if (bestLocation.size >= 15) {
                            fusedClient.removeLocationUpdates(this)
                            val best = bestLocation.minByOrNull { it.accuracy }
                            continuation.resume(best)
                        }
                    }
                }

                fusedClient.requestLocationUpdates(locationRequest, callback, Looper.getMainLooper())
                
                // Timeout after 30 seconds
                val timeoutTask = android.os.Handler(Looper.getMainLooper()).postDelayed({
                    fusedClient.removeLocationUpdates(callback)
                    val best = bestLocation.minByOrNull { it.accuracy }
                    if (best != null && !continuation.isActive) {
                        continuation.resume(best)
                    } else if (continuation.isActive) {
                        continuation.resume(best)
                    }
                }, 30_000L)

                continuation.invokeOnCancellation {
                    fusedClient.removeLocationUpdates(callback)
                    android.os.Handler(Looper.getMainLooper()).removeCallbacks { }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "High accuracy location request failed: ${e.message}")
            null
        }
    }

    @Suppress("MissingPermission")
    private suspend fun requestBalancedLocation(fusedClient: com.google.android.gms.location.FusedLocationProviderClient): Location? {
        return try {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 5_000L)
                .setMaxUpdates(1)
                .build()

            suspendCancellableCoroutine { continuation ->
                val callback = object : LocationCallback() {
                    override fun onLocationResult(result: LocationResult) {
                        fusedClient.removeLocationUpdates(this)
                        continuation.resume(result.lastLocation)
                    }
                }

                fusedClient.requestLocationUpdates(locationRequest, callback, Looper.getMainLooper())
                
                // Timeout after 15 seconds
                val timeoutTask = android.os.Handler(Looper.getMainLooper()).postDelayed({
                    fusedClient.removeLocationUpdates(callback)
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                }, 15_000L)

                continuation.invokeOnCancellation {
                    fusedClient.removeLocationUpdates(callback)
                    android.os.Handler(Looper.getMainLooper()).removeCallbacks { }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Balanced location request failed: ${e.message}")
            null
        }
    }
}
