package com.mdm.mdm_backend.service;

import com.mdm.mdm_backend.model.dto.DeviceInfoRequest;
import com.mdm.mdm_backend.model.entity.Device;
import com.mdm.mdm_backend.model.entity.DeviceInfo;
import com.mdm.mdm_backend.model.entity.Employee;
import com.mdm.mdm_backend.repository.DeviceInfoRepository;
import com.mdm.mdm_backend.repository.DeviceRepository;
import com.mdm.mdm_backend.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceInfoService {

    private final DeviceInfoRepository deviceInfoRepository;
    private final DeviceRepository deviceRepository;
    private final EmployeeRepository employeeRepository;

    public DeviceInfo saveDeviceInfo(DeviceInfoRequest request) {
        Boolean effectiveDeviceOwnerSet = request.getDeviceOwnerSet();
        if (!Boolean.TRUE.equals(effectiveDeviceOwnerSet)) {
            boolean wasEverDeviceOwner = deviceInfoRepository
                    .findFirstByDeviceIdOrderByCollectedAtDesc(request.getDeviceId())
                    .map(DeviceInfo::getDeviceOwnerSet)
                    .map(Boolean.TRUE::equals)
                    .orElse(false);
            if (wasEverDeviceOwner) {
                effectiveDeviceOwnerSet = true;
            }
        }

        DeviceInfo info = DeviceInfo.builder()
                .deviceId(request.getDeviceId())
                .model(request.getModel())
                .manufacturer(request.getManufacturer())
                .osVersion(request.getOsVersion())
                .sdkVersion(request.getSdkVersion() != null ? request.getSdkVersion().toString() : null)
                .serialNumber(request.getSerialNumber())
                .imei(request.getImei())
                .deviceType(request.getDeviceType())
                .deviceOwnerSet(effectiveDeviceOwnerSet)
                .employeeId(request.getEmployeeId())
                .collectedAt(LocalDateTime.now())
                .build();

        // Also update the Device record with model/manufacturer so the admin list is accurate
        deviceRepository.findByDeviceId(request.getDeviceId()).ifPresent(device -> {
            if (request.getModel() != null) device.setDeviceModel(request.getModel());
            if (request.getManufacturer() != null) device.setManufacturer(request.getManufacturer());
            device.setLastSeenAt(LocalDateTime.now());

            if (request.getEmployeeId() != null && !request.getEmployeeId().isBlank()) {
                employeeRepository.findByEmployeeId(request.getEmployeeId()).ifPresent(employee -> {
                    device.setEmployeeId(employee.getEmployeeId());
                    device.setEmployeeName(employee.getEmployeeName());
                });
            }
            deviceRepository.save(device);
        });

        if (request.getEmployeeId() != null && !request.getEmployeeId().isBlank()) {
            employeeRepository.findByEmployeeId(request.getEmployeeId()).ifPresent(employee -> {
                String deviceName = buildDeviceName(request.getManufacturer(), request.getModel());
                employee.setDeviceId(request.getDeviceId());
                employee.setDeviceName(deviceName);
                employee.setUpdatedAt(LocalDateTime.now());
                employeeRepository.save(employee);
            });
        }

        log.info("Saving device info for: {} (employee: {})", request.getDeviceId(), request.getEmployeeId());
        return deviceInfoRepository.save(info);
    }

    public List<DeviceInfo> getDeviceInfo(String deviceId) {
        return deviceInfoRepository.findByDeviceIdOrderByCollectedAtDesc(deviceId);
    }

    private String buildDeviceName(String manufacturer, String model) {
        String value = (manufacturer == null ? "" : manufacturer.trim()) + " " + (model == null ? "" : model.trim());
        String normalized = value.trim();
        return normalized.isBlank() ? "Unknown Device" : normalized;
    }
}