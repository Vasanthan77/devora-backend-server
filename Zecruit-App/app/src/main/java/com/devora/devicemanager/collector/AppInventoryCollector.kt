package com.devora.devicemanager.collector

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.util.Base64
import android.util.Log
import java.io.ByteArrayOutputStream

data class AppInfo(
    val appName: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val isSystemApp: Boolean,
    val installSource: String,
    val iconBase64: String,
    val isSuspended: Boolean
)

object AppInventoryCollector {

    private const val TAG = "AppInventoryCollector"

    fun collect(context: Context, includeIcons: Boolean = true): List<AppInfo> {
        val pm = context.packageManager
        val restrictedSet = context
            .getSharedPreferences("devora_restrictions", Context.MODE_PRIVATE)
            .getStringSet("restricted_packages", emptySet())
            ?: emptySet()

        val packages: List<PackageInfo> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.getInstalledPackages(0)
        }

        return packages.map { info -> info.toAppInfo(pm, restrictedSet, includeIcons) }
    }

    private fun PackageInfo.toAppInfo(
        pm: PackageManager,
        restrictedSet: Set<String>,
        includeIcons: Boolean
    ): AppInfo {
        val appLabel = applicationInfo?.loadLabel(pm)?.toString().orEmpty()

        val versionCode: Long = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            longVersionCode
        } else {
            @Suppress("DEPRECATION")
            versionCode.toLong()
        }

        val isSystem = applicationInfo?.let {
            (it.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        } ?: false

        val installer = getInstallerPackage(pm, packageName.orEmpty())
        val icon = if (includeIcons) getAppIconBase64(pm, packageName.orEmpty()) else ""
        val suspended = restrictedSet.contains(packageName.orEmpty())

        return AppInfo(
            appName = appLabel,
            packageName = packageName.orEmpty(),
            versionName = versionName.orEmpty(),
            versionCode = versionCode,
            isSystemApp = isSystem,
            installSource = installer,
            iconBase64 = icon,
            isSuspended = suspended
        )
    }

    private fun getAppIconBase64(pm: PackageManager, pkg: String): String {
        return try {
            val drawable = pm.getApplicationIcon(pkg)
            val bitmap = if (drawable is BitmapDrawable) {
                drawable.bitmap
            } else {
                val bmp = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bmp)
                drawable.setBounds(0, 0, 48, 48)
                drawable.draw(canvas)
                bmp
            }
            val scaled = Bitmap.createScaledBitmap(bitmap, 48, 48, true)
            val stream = ByteArrayOutputStream()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                scaled.compress(Bitmap.CompressFormat.WEBP_LOSSY, 60, stream)
            } else {
                @Suppress("DEPRECATION")
                scaled.compress(Bitmap.CompressFormat.WEBP, 60, stream)
            }
            Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.d(TAG, "Could not get icon for $pkg: ${e.message}")
            ""
        }
    }

    private fun getInstallerPackage(pm: PackageManager, pkg: String): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val sourceInfo = pm.getInstallSourceInfo(pkg)
                sourceInfo.installingPackageName
                    ?: sourceInfo.initiatingPackageName
                    ?: ""
            } else {
                @Suppress("DEPRECATION")
                pm.getInstallerPackageName(pkg) ?: ""
            }
        } catch (e: Exception) {
            Log.d(TAG, "Could not get installer for $pkg: ${e.message}")
            ""
        }
    }
}
