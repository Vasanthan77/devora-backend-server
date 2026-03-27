
package com.mdm.mdm_backend.controller;

import com.mdm.mdm_backend.model.dto.DeviceResponse;
import com.mdm.mdm_backend.model.entity.AccurateDeviceLocation;
import com.mdm.mdm_backend.model.entity.Device;
import com.mdm.mdm_backend.model.entity.DeviceActivity;
import com.mdm.mdm_backend.model.entity.MdmAlert;
import com.mdm.mdm_backend.repository.AccurateDeviceLocationRepository;
import com.mdm.mdm_backend.repository.DeviceActivityRepository;
import com.mdm.mdm_backend.repository.DeviceRepository;
import com.mdm.mdm_backend.repository.MdmAlertRepository;
import com.mdm.mdm_backend.service.AmapiService;
import com.mdm.mdm_backend.service.EnrollmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DeviceController — "Inventory & Location" only.
 *
 * All device MANAGEMENT (lock, wipe, policy, app-restrict) is in AmapiController.
 * This controller handles:
 *   - Listing / getting / checking / deleting devices from the local DB
 *   - Heartbeat from the Android app
 *   - GPS location reporting & history
 *   - Sign-out
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class DeviceController {

    private final EnrollmentService enrollmentService;
    private final AccurateDeviceLocationRepository accurateLocationRepo;
    private final DeviceActivityRepository activityRepo;
    private final MdmAlertRepository alertRepo;
    private final DeviceRepository deviceRepo;
    private final AmapiService amapiService;

    @Value("${amapi.enterprise-name:enterprises/LC03patpnu}")
    private String enterpriseName;

    // ════════════════════════════════════════
    // DEVICE INVENTORY
    // ════════════════════════════════════════

    @GetMapping("/devices")
    public ResponseEntity<List<DeviceResponse>> getAllDevices() {
        return ResponseEntity.ok(enrollmentService.getAllDevicesAsResponse());
    }

    @GetMapping("/devices/{deviceId}")
    public ResponseEntity<?> getDevice(@PathVariable(name = "deviceId") String deviceId) {
        return enrollmentService.getDeviceAsResponse(deviceId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Device not found")));
    }

    @GetMapping("/devices/check/{deviceId}")
    public ResponseEntity<DeviceResponse> checkDevice(@PathVariable(name = "deviceId") String deviceId) {
        return enrollmentService.getDeviceAsResponse(deviceId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PostMapping("/devices/{deviceId}/heartbeat")
    public ResponseEntity<Map<String, String>> heartbeat(@PathVariable(name = "deviceId") String deviceId) {
        return enrollmentService.recordHeartbeat(deviceId)
                .map(device -> ResponseEntity.ok(Map.of("status", "ok")))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Device not found")));
    }

    @DeleteMapping("/devices/{deviceId}")
    public ResponseEntity<Map<String, String>> deleteDevice(@PathVariable(name = "deviceId") String deviceId) {
        try {
            String employeeName = deviceRepo.findByDeviceId(deviceId)
                    .map(Device::getEmployeeName).orElse("Unknown");

            // Sync deletion with Google AMAPI to free up project quota
            try {
                amapiService.deleteDevice(enterpriseName, deviceId);
                log.info("Successfully deleted device {} from AMAPI", deviceId);
            } catch (Exception amapiEx) {
                log.warn("Device {} deleted locally but AMAPI deletion failed (might already be gone): {}",
                        deviceId, amapiEx.getMessage());
            }

            boolean deleted = enrollmentService.deleteDeviceWithAllData(deviceId);
            if (!deleted) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Device not found"));
            }

            try {
                activityRepo.save(DeviceActivity.builder()
                        .deviceId(deviceId).employeeName(employeeName)
                        .activityType("DELETED").description(employeeName + "'s device removed")
                        .severity("CRITICAL").createdAt(LocalDateTime.now()).build());
                alertRepo.save(MdmAlert.builder()
                        .deviceId(deviceId).employeeName(employeeName)
                        .alertType("DEVICE_DELETED")
                        .message(employeeName + "'s device was deleted")
                        .isRead(false).severity("CRITICAL").createdAt(LocalDateTime.now())
                        .build());
            } catch (Exception logEx) {
                log.warn("Device {} deleted but audit logging failed: {}", deviceId, logEx.getMessage());
            }

            return ResponseEntity.ok(Map.of("message", "Device deleted successfully"));
        } catch (Exception ex) {
            log.error("Failed to delete device {}", deviceId, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to delete device due to server error"));
        }
    }

    @PostMapping("/devices/{deviceId}/sign-out")
    public ResponseEntity<?> signOutDevice(@PathVariable(name = "deviceId") String deviceId) {
        Device device = deviceRepo.findByDeviceId(deviceId).orElse(null);
        if (device == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Device not found"));
        }
        device.setStatus("OFFLINE");
        device.setLastSeenAt(LocalDateTime.now().minusHours(1));
        deviceRepo.save(device);

        activityRepo.save(DeviceActivity.builder()
                .deviceId(deviceId)
                .employeeName(device.getEmployeeName())
                .activityType("SIGN_OUT")
                .description("Employee signed out from the device")
                .severity("INFO")
                .createdAt(LocalDateTime.now())
                .build());

        return ResponseEntity.ok().build();
    }

    // ════════════════════════════════════════
    // GPS LOCATION
    // ════════════════════════════════════════

    @PostMapping("/devices/{deviceId}/location")
    public ResponseEntity<?> reportLocation(@PathVariable(name = "deviceId") String deviceId,
            @RequestBody Map<String, Object> body) {
        Double lat = body.get("latitude") != null ? Double.parseDouble(body.get("latitude").toString()) : null;
        Double lng = body.get("longitude") != null ? Double.parseDouble(body.get("longitude").toString()) : null;
        Float accuracy = body.get("accuracy") != null ? Float.parseFloat(body.get("accuracy").toString()) : null;
        Double altitude = body.get("altitude") != null ? Double.parseDouble(body.get("altitude").toString()) : null;
        Float bearing = body.get("bearing") != null ? Float.parseFloat(body.get("bearing").toString()) : null;
        Float speed = body.get("speed") != null ? Float.parseFloat(body.get("speed").toString()) : null;
        String address = (String) body.getOrDefault("address", "");

        if (lat == null || lng == null)
            return ResponseEntity.badRequest().body(Map.of("message", "latitude and longitude required"));

        String employeeName = deviceRepo.findByDeviceId(deviceId)
                .map(Device::getEmployeeName).orElse("Unknown");

        accurateLocationRepo.save(AccurateDeviceLocation.builder()
                .deviceId(deviceId).latitude(lat).longitude(lng)
                .accuracy(accuracy).altitude(altitude).bearing(bearing).speed(speed)
                .address(address)
                .recordedAt(LocalDateTime.now()).build());

        accurateLocationRepo.deleteOldLocations(deviceId);

        activityRepo.save(DeviceActivity.builder()
                .deviceId(deviceId).employeeName(employeeName)
                .activityType("LOCATION_UPDATED").description("Location updated")
                .severity("INFO").createdAt(LocalDateTime.now()).build());

        return ResponseEntity.ok(Map.of("message", "Location recorded"));
    }

    @GetMapping("/devices/{deviceId}/location")
    public ResponseEntity<?> getLocation(@PathVariable(name = "deviceId") String deviceId) {
        return accurateLocationRepo.findTopByDeviceIdOrderByRecordedAtDesc(deviceId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/devices/{deviceId}/location/history")
    public ResponseEntity<List<AccurateDeviceLocation>> getLocationHistory(
            @PathVariable(name = "deviceId") String deviceId,
            @RequestParam(name = "limit", defaultValue = "5") int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 10));
        List<AccurateDeviceLocation> locations = accurateLocationRepo
                .findTop10ByDeviceIdOrderByRecordedAtDesc(deviceId)
                .stream().limit(safeLimit).toList();
        return ResponseEntity.ok(locations);
    }

    @PostMapping("/devices/{deviceId}/accurate-location")
    public ResponseEntity<?> reportAccurateLocation(@PathVariable(name = "deviceId") String deviceId,
            @RequestBody Map<String, Object> body) {
        return reportLocation(deviceId, body);
    }

    @GetMapping("/devices/{deviceId}/accurate-location")
    public ResponseEntity<?> getAccurateLocation(@PathVariable(name = "deviceId") String deviceId) {
        return getLocation(deviceId);
    }

    @GetMapping("/devices/{deviceId}/accurate-location/history")
    public ResponseEntity<List<AccurateDeviceLocation>> getAccurateLocationHistory(
            @PathVariable(name = "deviceId") String deviceId,
            @RequestParam(name = "limit", defaultValue = "5") int limit) {
        return getLocationHistory(deviceId, limit);
    }
}
