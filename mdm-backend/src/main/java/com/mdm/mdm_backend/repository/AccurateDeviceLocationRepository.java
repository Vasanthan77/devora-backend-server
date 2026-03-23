package com.mdm.mdm_backend.repository;

import com.mdm.mdm_backend.model.entity.AccurateDeviceLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface AccurateDeviceLocationRepository extends JpaRepository<AccurateDeviceLocation, Long> {

    Optional<AccurateDeviceLocation> findTopByDeviceIdOrderByRecordedAtDesc(String deviceId);

    List<AccurateDeviceLocation> findTop10ByDeviceIdOrderByRecordedAtDesc(String deviceId);

    @Modifying
    @Transactional
    @Query(
        value = "DELETE FROM accurate_device_locations WHERE device_id = :deviceId " +
                "AND id NOT IN (SELECT id FROM (SELECT id FROM accurate_device_locations WHERE device_id = :deviceId " +
                "ORDER BY recorded_at DESC LIMIT 50) t)",
        nativeQuery = true
    )
    void deleteOldLocations(@Param("deviceId") String deviceId);

    @Transactional
    void deleteByDeviceId(String deviceId);
}
