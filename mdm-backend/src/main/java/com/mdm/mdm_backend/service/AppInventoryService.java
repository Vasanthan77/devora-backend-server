package com.mdm.mdm_backend.service;

import com.mdm.mdm_backend.model.dto.AppInventoryRequest;
import com.mdm.mdm_backend.model.entity.AppInventory;
import com.mdm.mdm_backend.repository.AppInventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppInventoryService {

    private final AppInventoryRepository appInventoryRepository;

    @Transactional
    public List<AppInventory> saveInventory(AppInventoryRequest request) {

        appInventoryRepository.deleteByDeviceId(request.getDeviceId());

        List<AppInventory> apps = request.getApps().stream()
                .map(dto -> AppInventory.builder()
                        .deviceId(request.getDeviceId())
                        .appName(dto.getAppName())
                        .packageName(dto.getPackageName())
                        .versionName(dto.getVersionName())
                        .versionCode(dto.getVersionCode())
                        .installSource(dto.getInstallSource())
                        .isSystemApp(dto.getIsSystemApp())
                    .isSuspended(Boolean.TRUE.equals(dto.getIsSuspended()))
                        .iconBase64(dto.getIconBase64())
                        .collectedAt(LocalDateTime.now())
                        .build())
                .collect(Collectors.toList());

        log.info("Saving {} apps for device: {}", apps.size(), request.getDeviceId());
        return appInventoryRepository.saveAll(apps);
    }

    public List<AppInventory> getInventory(String deviceId) {
        return appInventoryRepository.findByDeviceId(deviceId);
    }

    public long countApps() {
        return appInventoryRepository.count();
    }
}
