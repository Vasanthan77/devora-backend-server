package com.mdm.mdm_backend.repository;

import com.mdm.mdm_backend.model.entity.DeviceCommand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface DeviceCommandRepository extends JpaRepository<DeviceCommand, Long> {
    List<DeviceCommand> findByDeviceIdAndExecutedFalse(String deviceId);
    List<DeviceCommand> findByDeviceIdAndExecutedFalseOrderByCreatedAtAsc(String deviceId);
    @Transactional
    void deleteByDeviceId(String deviceId);
}
