package com.devora.devicemanager.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DeviceInfoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(info: DeviceInfoEntity)

    @Query("SELECT * FROM device_info ORDER BY id DESC LIMIT 1")
    suspend fun getLatest(): DeviceInfoEntity?

    @Query("SELECT * FROM device_info ORDER BY id DESC")
    suspend fun getAll(): List<DeviceInfoEntity>

    // Sync log operations

    @Insert
    suspend fun insertSyncLog(log: DeviceInfoSyncLogEntity)

    @Query("SELECT * FROM device_info_sync_log ORDER BY id DESC LIMIT 20")
    suspend fun getSyncHistory(): List<DeviceInfoSyncLogEntity>

    @Query("SELECT * FROM device_info_sync_log WHERE status = 'SUCCESS' ORDER BY id DESC LIMIT 1")
    suspend fun getLastSuccessfulSync(): DeviceInfoSyncLogEntity?
}
