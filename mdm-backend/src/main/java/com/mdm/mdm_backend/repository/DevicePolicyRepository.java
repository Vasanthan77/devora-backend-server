package com.mdm.mdm_backend.repository;

import com.mdm.mdm_backend.model.entity.DevicePolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

public interface DevicePolicyRepository extends JpaRepository<DevicePolicy, Long> {
    Optional<DevicePolicy> findByDeviceId(String deviceId);
    @Transactional
    void deleteByDeviceId(String deviceId);
}
