package com.mdm.mdm_backend.repository;

import com.mdm.mdm_backend.model.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, Long> {

    Optional<Device> findByDeviceId(String deviceId);

    boolean existsByDeviceId(String deviceId);

    boolean existsByDeviceIdAndStatus(String deviceId, String status);

    long countByStatus(String status);
}