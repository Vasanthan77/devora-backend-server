package com.devora.devicemanager.session

import android.content.Context
import android.content.SharedPreferences

object SessionManager {

    private const val PREF_NAME = "devora_session"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_ADMIN_NAME = "admin_name"
    private const val KEY_ADMIN_EMAIL = "admin_email"
    private const val KEY_FORCE_REENROLL = "force_reenroll"
    private const val ENROLLMENT_PREF = "devora_enrollment"
    private const val KEY_EMPLOYEE_SIGNED_OUT = "employee_signed_out"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun saveSession(context: Context, name: String, email: String) {
        prefs(context).edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_ADMIN_NAME, name)
            .putString(KEY_ADMIN_EMAIL, email)
            .apply()
    }

    fun isLoggedIn(context: Context): Boolean =
        prefs(context).getBoolean(KEY_IS_LOGGED_IN, false)

    fun getAdminName(context: Context): String =
        prefs(context).getString(KEY_ADMIN_NAME, "") ?: ""

    fun getAdminEmail(context: Context): String =
        prefs(context).getString(KEY_ADMIN_EMAIL, "") ?: ""

    fun logout(context: Context) {
        prefs(context).edit().clear().apply()
    }

    fun clearDeviceEnrollment(context: Context) {
        context.getSharedPreferences(ENROLLMENT_PREF, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }

    fun setEmployeeSignedOut(context: Context, signedOut: Boolean) {
        context.getSharedPreferences(ENROLLMENT_PREF, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_EMPLOYEE_SIGNED_OUT, signedOut)
            .apply()
    }

    fun isEmployeeSignedOut(context: Context): Boolean {
        return context.getSharedPreferences(ENROLLMENT_PREF, Context.MODE_PRIVATE)
            .getBoolean(KEY_EMPLOYEE_SIGNED_OUT, false)
    }

    fun setForceReEnroll(context: Context, force: Boolean) {
        prefs(context).edit()
            .putBoolean(KEY_FORCE_REENROLL, force)
            .apply()
    }

    fun isForceReEnroll(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_FORCE_REENROLL, false)
    }
}
