package com.devora.devicemanager

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.UserHandle
import android.util.Log

/**
 * Device Admin / Device Owner receiver for DEVORA MDM.
 *
 * This receiver handles all Device Owner lifecycle callbacks:
 *  - onEnabled:  Device admin privilege granted
 *  - onDisabled: Device admin privilege revoked
 *  - onProfileProvisioningComplete: Device Owner provisioning finished (QR / NFC)
 *  - onLockTaskModeEntering / Exiting: Lock task (kiosk) mode transitions
 *
 * After provisioning completes, initial enterprise policies are applied and the
 * main activity is launched so the employee lands on the enrollment success screen.
 */
class AdminReceiver : DeviceAdminReceiver() {

    companion object {
        private const val TAG = "DevoraAdminReceiver"

        /** Returns the ComponentName used to identify this receiver with DevicePolicyManager. */
        fun getComponentName(context: Context): ComponentName =
            ComponentName(context.applicationContext, AdminReceiver::class.java)

        /** Checks whether this app is currently the Device Owner. */
        fun isDeviceOwner(context: Context): Boolean {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            return dpm.isDeviceOwnerApp(context.packageName)
        }
    }

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.d(TAG, "Device Admin Enabled")
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.d(TAG, "Device Admin Disabled")
    }

    override fun onProfileProvisioningComplete(context: Context, intent: Intent) {
        super.onProfileProvisioningComplete(context, intent)
        Log.d(TAG, "Profile provisioning complete — applying initial policies")

        // Apply initial Device Owner policies after provisioning
        applyInitialPolicies(context)

        // Launch the main activity so the user sees the enrollment result
        val launch = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("enrollment_complete", true)
        }
        context.startActivity(launch)
    }

    override fun onLockTaskModeEntering(context: Context, intent: Intent, pkg: String) {
        super.onLockTaskModeEntering(context, intent, pkg)
        Log.d(TAG, "Entering lock task mode for package: $pkg")
    }

    override fun onLockTaskModeExiting(context: Context, intent: Intent) {
        super.onLockTaskModeExiting(context, intent)
        Log.d(TAG, "Exiting lock task mode")
    }

    // ─────────────────────────────────────────────
    // Initial policy enforcement after provisioning
    // ─────────────────────────────────────────────

    private fun applyInitialPolicies(context: Context) {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = getComponentName(context)

        if (!dpm.isDeviceOwnerApp(context.packageName)) {
            Log.w(TAG, "Not Device Owner — skipping policy enforcement")
            return
        }

        try {
            // 1. Set this app as a lock task package (kiosk mode allowlist)
            dpm.setLockTaskPackages(admin, arrayOf(context.packageName))
            Log.d(TAG, "Lock task packages set")

            // 2. Configure keyguard features — disable trust agents & fingerprint on lockscreen
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                dpm.setKeyguardDisabledFeatures(
                    admin,
                    DevicePolicyManager.KEYGUARD_DISABLE_TRUST_AGENTS
                            or DevicePolicyManager.KEYGUARD_DISABLE_FINGERPRINT
                )
                Log.d(TAG, "Keyguard features configured")
            }

            // 3. Set password quality to at least NUMERIC
            @Suppress("DEPRECATION")
            dpm.setPasswordQuality(admin, DevicePolicyManager.PASSWORD_QUALITY_NUMERIC)
            dpm.setPasswordMinimumLength(admin, 6)
            Log.d(TAG, "Password policy set: NUMERIC, min length 6")

            // 4. Disable status bar (API 23+ Device Owner only) — prevents pulling notification shade
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                dpm.setStatusBarDisabled(admin, false) // keep enabled by default; toggle via policy
                Log.d(TAG, "Status bar policy set")
            }

            // 5. Mark enrollment complete in shared prefs
            context.getSharedPreferences("devora_enrollment", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("is_device_owner", true)
                .putLong("provisioning_timestamp", System.currentTimeMillis())
                .apply()

            Log.d(TAG, "Initial Device Owner policies applied successfully")
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to apply policies — insufficient privileges", e)
        }
    }
}