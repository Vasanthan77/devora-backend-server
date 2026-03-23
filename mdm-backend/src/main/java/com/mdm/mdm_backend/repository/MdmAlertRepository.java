package com.mdm.mdm_backend.repository;

import com.mdm.mdm_backend.model.entity.MdmAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface MdmAlertRepository extends JpaRepository<MdmAlert, Long> {
    List<MdmAlert> findByIsReadFalseOrderByCreatedAtDesc();
    List<MdmAlert> findAllByOrderByCreatedAtDesc();
    long countByIsReadFalse();
    @Transactional
    void deleteByDeviceId(String deviceId);
}
