package com.mdm.mdm_backend.repository;

import com.mdm.mdm_backend.model.entity.AppInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface AppInventoryRepository extends JpaRepository<AppInventory, Long> {

    List<AppInventory> findByDeviceId(String deviceId);

    @Transactional
    void deleteByDeviceId(String deviceId);
}