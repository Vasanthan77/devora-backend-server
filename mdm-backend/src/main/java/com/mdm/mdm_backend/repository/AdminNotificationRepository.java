package com.mdm.mdm_backend.repository;

import com.mdm.mdm_backend.model.entity.AdminNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AdminNotificationRepository extends JpaRepository<AdminNotification, Long> {
    List<AdminNotification> findByReadFalseOrderByCreatedAtDesc();
    List<AdminNotification> findAllByOrderByCreatedAtDesc();
    List<AdminNotification> findByDeviceIdOrderByCreatedAtDesc(String deviceId);
    long countByReadFalse();

    @org.springframework.transaction.annotation.Transactional
    void deleteByDeviceId(String deviceId);
}
