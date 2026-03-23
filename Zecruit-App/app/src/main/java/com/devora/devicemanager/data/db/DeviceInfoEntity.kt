package com.devora.devicemanager.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "device_info")
data class DeviceInfoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deviceId: String,
    val model: String,
    val manufacturer: String,
    val brand: String,
    val board: String,
    val osVersion: String,
    val sdkVersion: Int,
    val serialNumber: String?,
    val imei: String?,
    val deviceType: String,
    val serialRestricted: Boolean,
    val imeiRestricted: Boolean,
    val collectedAt: String
)
