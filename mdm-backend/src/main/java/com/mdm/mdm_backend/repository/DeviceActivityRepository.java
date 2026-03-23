package com.mdm.mdm_backend.repository;

import com.mdm.mdm_backend.model.entity.DeviceActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface DeviceActivityRepository extends JpaRepository<DeviceActivity, Long> {
    List<DeviceActivity> findAllByOrderByCreatedAtDesc();
    List<DeviceActivity> findByDeviceIdOrderByCreatedAtDesc(String deviceId);
    @Transactional
    void deleteByDeviceId(String deviceId);
}
