package com.devora.devicemanager.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class AuthViewModel : ViewModel() {
    var isAdminLoggedIn by mutableStateOf(false)
        private set
    var isEmployeeLoggedIn by mutableStateOf(false)
        private set
    var currentUserRole by mutableStateOf("")
        private set

    fun loginAdmin() {
        isAdminLoggedIn = true
        isEmployeeLoggedIn = false
        currentUserRole = "ADMIN"
    }

    fun loginEmployee() {
        isAdminLoggedIn = false
        isEmployeeLoggedIn = true
        currentUserRole = "EMPLOYEE"
    }

    fun signOut() {
        isAdminLoggedIn = false
        isEmployeeLoggedIn = false
        currentUserRole = ""
    }
}
