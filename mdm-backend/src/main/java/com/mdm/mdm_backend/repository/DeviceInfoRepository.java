package com.mdm.mdm_backend.repository;

import com.mdm.mdm_backend.model.entity.DeviceInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DeviceInfoRepository extends JpaRepository<DeviceInfo, Long> {

    List<DeviceInfo> findByDeviceIdOrderByCollectedAtDesc(String deviceId);

    Optional<DeviceInfo> findFirstByDeviceIdOrderByCollectedAtDesc(String deviceId);

        @Query("""
                        SELECT COUNT(DISTINCT di.deviceId)
                        FROM DeviceInfo di
                        WHERE di.collectedAt >= :cutoff
                            AND di.deviceId IN (
                                SELECT d.deviceId
                                FROM Device d
                                WHERE UPPER(COALESCE(d.status, 'ACTIVE')) <> 'OFFLINE'
                            )
                        """)
        long countRecentlySyncedActiveDevices(@Param("cutoff") LocalDateTime cutoff);

    void deleteByDeviceId(String deviceId);
}