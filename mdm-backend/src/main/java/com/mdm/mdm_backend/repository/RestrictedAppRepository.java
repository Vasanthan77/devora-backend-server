package com.mdm.mdm_backend.repository;

import com.mdm.mdm_backend.model.entity.RestrictedApp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface RestrictedAppRepository extends JpaRepository<RestrictedApp, Long> {

    List<RestrictedApp> findByDeviceId(String deviceId);

    Optional<RestrictedApp> findByDeviceIdAndPackageName(String deviceId, String packageName);

    boolean existsByDeviceIdAndPackageName(String deviceId, String packageName);

    @Transactional
    void deleteByDeviceIdAndPackageName(String deviceId, String packageName);

    @Transactional
    void deleteByDeviceId(String deviceId);
}
