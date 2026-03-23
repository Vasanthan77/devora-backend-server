package com.devora.devicemanager.ui.screens.employee

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EmployeeFormState(
    val fullName: String = "",
    val employeeId: String = "",
    val email: String = "",
    val department: String = "",
    val designation: String = "",
    val deviceType: String = "",
    val reason: String = "",
    val priority: String = "Normal",
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)

class EmployeeRegisterViewModel : ViewModel() {

    private val _formState = MutableStateFlow(EmployeeFormState())
    val formState: StateFlow<EmployeeFormState> = _formState.asStateFlow()

    val departments = listOf("Engineering", "Design", "Marketing", "Sales", "Finance", "HR", "Operations", "Support")
    val deviceTypes = listOf("Laptop", "Desktop", "Tablet", "Phone")
    val priorities = listOf("Low", "Normal", "High", "Urgent")

    fun updateFullName(value: String) {
        _formState.update { it.copy(fullName = value, errorMessage = null) }
    }

    fun updateEmployeeId(value: String) {
        _formState.update { it.copy(employeeId = value, errorMessage = null) }
    }

    fun updateEmail(value: String) {
        _formState.update { it.copy(email = value, errorMessage = null) }
    }

    fun updateDepartment(value: String) {
        _formState.update { it.copy(department = value, errorMessage = null) }
    }

    fun updateDesignation(value: String) {
        _formState.update { it.copy(designation = value, errorMessage = null) }
    }

    fun updateDeviceType(value: String) {
        _formState.update { it.copy(deviceType = value, errorMessage = null) }
    }

    fun updateReason(value: String) {
        if (value.length <= 200) {
            _formState.update { it.copy(reason = value, errorMessage = null) }
        }
    }

    fun updatePriority(value: String) {
        _formState.update { it.copy(priority = value, errorMessage = null) }
    }

    fun validate(): Boolean {
        val state = _formState.value
        val error = when {
            state.fullName.isBlank() -> "Full name is required"
            state.employeeId.isBlank() -> "Employee ID is required"
            state.email.isBlank() -> "Work email is required"
            !state.email.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) -> "Enter a valid email address"
            state.department.isBlank() -> "Select a department"
            state.designation.isBlank() -> "Designation is required"
            state.deviceType.isBlank() -> "Select a device type"
            state.reason.isBlank() -> "Purpose / reason is required"
            else -> null
        }
        if (error != null) {
            _formState.update { it.copy(errorMessage = error) }
            return false
        }
        return true
    }

    fun submitRegistration(onSuccess: () -> Unit) {
        if (!validate()) return
        viewModelScope.launch {
            _formState.update { it.copy(isLoading = true, errorMessage = null) }
            // TODO: Replace with real API call
            delay(1500)
            _formState.update { it.copy(isLoading = false, isSuccess = true) }
            onSuccess()
        }
    }
}
