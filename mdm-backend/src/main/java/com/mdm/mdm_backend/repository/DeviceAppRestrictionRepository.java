package com.mdm.mdm_backend.repository;

import com.mdm.mdm_backend.model.entity.DeviceAppRestriction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

public interface DeviceAppRestrictionRepository extends JpaRepository<DeviceAppRestriction, Long> {
    List<DeviceAppRestriction> findByDeviceId(String deviceId);
    List<DeviceAppRestriction> findByDeviceIdAndRestricted(String deviceId, boolean restricted);
    Optional<DeviceAppRestriction> findByDeviceIdAndPackageName(String deviceId, String packageName);
    @Transactional
    void deleteByDeviceId(String deviceId);
}
