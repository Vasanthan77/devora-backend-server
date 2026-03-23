package com.mdm.mdm_backend.controller;

import com.mdm.mdm_backend.model.dto.DeviceInfoRequest;
import com.mdm.mdm_backend.model.entity.DeviceInfo;
import com.mdm.mdm_backend.service.DeviceInfoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DeviceInfoController {

    private final DeviceInfoService deviceInfoService;

    @PostMapping("/device-info")
    public ResponseEntity<?> saveDeviceInfo(@Valid @RequestBody DeviceInfoRequest request) {
        DeviceInfo info = deviceInfoService.saveDeviceInfo(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                        "message", "Device info saved successfully",
                        "id", info.getId()
                ));
    }

    @GetMapping("/device-info/{deviceId}")
    public ResponseEntity<List<DeviceInfo>> getDeviceInfo(@PathVariable String deviceId) {
        List<DeviceInfo> infoList = deviceInfoService.getDeviceInfo(deviceId);
        if (infoList.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(infoList);
    }
}