package com.devora.devicemanager.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "device_info_sync_log")
data class DeviceInfoSyncLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deviceId: String,
    val syncedAt: String,
    val status: String,       // SUCCESS, FAILED, PENDING
    val httpCode: Int?,
    val errorMessage: String?
)
