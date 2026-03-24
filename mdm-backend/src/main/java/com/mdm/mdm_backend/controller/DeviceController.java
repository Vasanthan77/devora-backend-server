
package com.mdm.mdm_backend.controller;

import com.mdm.mdm_backend.model.dto.DeviceResponse;
import com.mdm.mdm_backend.model.entity.AccurateDeviceLocation;
import com.mdm.mdm_backend.model.entity.Device;
import com.mdm.mdm_backend.model.entity.DeviceActivity;
import com.mdm.mdm_backend.model.entity.DeviceAppRestriction;
import com.mdm.mdm_backend.model.entity.DeviceCommand;
import com.mdm.mdm_backend.model.entity.DevicePolicy;
import com.mdm.mdm_backend.model.entity.MdmAlert;
import com.mdm.mdm_backend.repository.AccurateDeviceLocationRepository;
import com.mdm.mdm_backend.repository.DeviceActivityRepository;
import com.mdm.mdm_backend.repository.DeviceAppRestrictionRepository;
import com.mdm.mdm_backend.repository.DeviceCommandRepository;
import com.mdm.mdm_backend.repository.DevicePolicyRepository;
import com.mdm.mdm_backend.repository.DeviceRepository;
import com.mdm.mdm_backend.repository.MdmAlertRepository;
import com.mdm.mdm_backend.service.EnrollmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mdm.mdm_backend.service.AmapiService;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class DeviceController {

        private final EnrollmentService enrollmentService;
        private final DeviceAppRestrictionRepository appRestrictionRepo;
        private final DevicePolicyRepository policyRepo;
        private final DeviceCommandRepository commandRepo;
        private final AccurateDeviceLocationRepository accurateLocationRepo;
        private final DeviceActivityRepository activityRepo;
        private final MdmAlertRepository alertRepo;
        private final DeviceRepository deviceRepo;
        private final AmapiService amapiService;
        
        // Hardcoded for your new enterprise
        private static final String ENTERPRISE_NAME = "enterprises/LC01oh6rj0";

        @GetMapping("/devices")
        public ResponseEntity<?> getAllDevices() {
                try {
                        String amapiResponse = amapiService.listDevices(ENTERPRISE_NAME);
                        return ResponseEntity.ok(amapiResponse);
                } catch (Exception e) {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                             .body(Map.of("message", e.getMessage()));
                }
        }

        @GetMapping("/devices/{deviceId}")
        public ResponseEntity<?> getDevice(@PathVariable String deviceId) {
                try {
                        String amapiResponse = amapiService.getDevice(ENTERPRISE_NAME, deviceId);
                        return ResponseEntity.ok(amapiResponse);
                } catch (Exception e) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                             .body(Map.of("message", "Device not found in AMAPI"));
                }
        }

        @PostMapping("/devices/{deviceId}/heartbeat")
        public ResponseEntity<Map<String, String>> heartbeat(@PathVariable String deviceId) {
                return enrollmentService.recordHeartbeat(deviceId)
                                .map(device -> ResponseEntity.ok(Map.of("status", "ok")))
                                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                                                .body(Map.of("message", "Device not found")));
        }

        @DeleteMapping("/devices/{deviceId}")
        public ResponseEntity<Map<String, String>> deleteDevice(@PathVariable String deviceId) {
                try {
                        String employeeName = deviceRepo.findByDeviceId(deviceId)
                                        .map(Device::getEmployeeName).orElse("Unknown");
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
                                log.warn("Device {} deleted but audit logging failed: {}", deviceId,
                                                logEx.getMessage());
                        }

                        return ResponseEntity.ok(Map.of("message", "Device deleted successfully"));
                } catch (Exception ex) {
                        log.error("Failed to delete device {}", deviceId, ex);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(Map.of("message", "Failed to delete device due to server error"));
                }
        }

        @GetMapping("/devices/check/{deviceId}")
        public ResponseEntity<DeviceResponse> checkDevice(@PathVariable String deviceId) {
                return enrollmentService.getDeviceAsResponse(deviceId)
                                .map(ResponseEntity::ok)
                                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
        }

        // ════════════════════════════════════════
        // APP RESTRICTION
        // ════════════════════════════════════════

        @PostMapping("/devices/{deviceId}/restrict-app")
        public ResponseEntity<?> restrictApp(@PathVariable String deviceId, @RequestBody Map<String, Object> body) {
                String packageName = (String) body.get("packageName");
                String appName = (String) body.getOrDefault("appName", packageName);
                String installSource = (String) body.getOrDefault("installSource", "");
                Boolean restricted = body.get("restricted") != null
                                ? Boolean.valueOf(body.get("restricted").toString())
                                : true;

                if (packageName == null || packageName.isBlank())
                        return ResponseEntity.badRequest().body(Map.of("message", "packageName is required"));

                var existing = appRestrictionRepo.findByDeviceIdAndPackageName(deviceId, packageName);
                DeviceAppRestriction restriction;
                if (existing.isPresent()) {
                        restriction = existing.get();
                        restriction.setRestricted(restricted);
                        restriction.setAppliedAt(LocalDateTime.now());
                } else {
                        restriction = DeviceAppRestriction.builder()
                                        .deviceId(deviceId).packageName(packageName).appName(appName)
                                        .installSource(installSource).restricted(restricted)
                                        .appliedAt(LocalDateTime.now()).build();
                }
                appRestrictionRepo.save(restriction);

                String employeeName = deviceRepo.findByDeviceId(deviceId)
                                .map(Device::getEmployeeName).orElse("Unknown");
                String action = restricted ? "APP_RESTRICTED" : "APP_ALLOWED";
                String desc = restricted
                                ? appName + " restricted on " + employeeName + "'s Device"
                                : appName + " allowed on " + employeeName + "'s Device";
                String sourceInfo = (installSource != null && !installSource.isBlank())
                                ? " (" + installSource + ")"
                                : "";
                activityRepo.save(DeviceActivity.builder()
                                .deviceId(deviceId).employeeName(employeeName)
                                .activityType(action).description(desc + sourceInfo)
                                .severity(restricted ? "WARNING" : "INFO").createdAt(LocalDateTime.now()).build());
                if (restricted) {
                        alertRepo.save(MdmAlert.builder()
                                        .deviceId(deviceId).employeeName(employeeName)
                                        .alertType("APP_RESTRICTED")
                                        .message(appName + sourceInfo + " blocked on " + employeeName + "'s Device")
                                        .isRead(false).severity("WARNING").createdAt(LocalDateTime.now()).build());
                }

                }

                // Sync AMAPI dynamically!
                try {
                        DevicePolicy policy = policyRepo.findByDeviceId(deviceId).orElse(null);
                        amapiService.patchDevicePolicy(ENTERPRISE_NAME, "policy1", policy, appRestrictionRepo.findByDeviceId(deviceId));
                } catch(Exception e) {
                        log.error("Failed to sync app restriction to AMAPI", e);
                }

                log.info("{} app {} on device {}", restricted ? "Restricted" : "Allowed", packageName, deviceId);
                return ResponseEntity.ok(Map.of("message",
                                "App " + (restricted ? "restricted" : "allowed") + " successfully"));
        }

        @GetMapping("/devices/{deviceId}/restricted-apps")
        public ResponseEntity<List<DeviceAppRestriction>> getRestrictedApps(@PathVariable String deviceId) {
                return ResponseEntity.ok(appRestrictionRepo.findByDeviceIdAndRestricted(deviceId, true));
        }

        @GetMapping("/devices/{deviceId}/app-restrictions")
        public ResponseEntity<List<DeviceAppRestriction>> getAllAppRestrictions(@PathVariable String deviceId) {
                return ResponseEntity.ok(appRestrictionRepo.findByDeviceId(deviceId));
        }

        // ════════════════════════════════════════
        // DEVICE POLICIES
        // ════════════════════════════════════════

        @GetMapping("/devices/{deviceId}/policies")
        public ResponseEntity<DevicePolicy> getPolicies(@PathVariable String deviceId) {
                DevicePolicy policy = policyRepo.findByDeviceId(deviceId)
                                .orElse(DevicePolicy.builder()
                                                .deviceId(deviceId).cameraDisabled(false)
                                                .screenLockRequired(false).installBlocked(false)
                                                .uninstallBlocked(false).locationTrackingEnabled(false)
                                                .appliedAt(LocalDateTime.now()).build());
                return ResponseEntity.ok(policy);
        }

        @PostMapping("/devices/{deviceId}/policy")
        public ResponseEntity<?> updatePolicy(@PathVariable String deviceId,
                        @RequestBody Map<String, Object> body) {
                DevicePolicy policy = policyRepo.findByDeviceId(deviceId)
                                .orElse(DevicePolicy.builder().deviceId(deviceId)
                                                .cameraDisabled(false).screenLockRequired(false)
                                                .installBlocked(false).uninstallBlocked(false)
                                                .locationTrackingEnabled(false).build());

                String employeeName = deviceRepo.findByDeviceId(deviceId)
                                .map(Device::getEmployeeName).orElse("Unknown");

                if (body.containsKey("cameraDisabled")) {
                        boolean val = Boolean.parseBoolean(body.get("cameraDisabled").toString());
                        policy.setCameraDisabled(val);
                        String desc = (val ? "Camera disabled" : "Camera enabled")
                                        + " on " + employeeName + "'s Device";
                        activityRepo.save(DeviceActivity.builder()
                                        .deviceId(deviceId).employeeName(employeeName)
                                        .activityType(val ? "CAMERA_DISABLED" : "CAMERA_ENABLED")
                                        .description(desc).severity("WARNING").createdAt(LocalDateTime.now()).build());
                        alertRepo.save(MdmAlert.builder()
                                        .deviceId(deviceId).employeeName(employeeName)
                                        .alertType(val ? "CAMERA_DISABLED" : "CAMERA_ENABLED").message(desc)
                                        .isRead(false).severity("WARNING").createdAt(LocalDateTime.now()).build());
                }
                if (body.containsKey("installBlocked"))
                        policy.setInstallBlocked(Boolean.parseBoolean(body.get("installBlocked").toString()));
                if (body.containsKey("uninstallBlocked"))
                        policy.setUninstallBlocked(Boolean.parseBoolean(body.get("uninstallBlocked").toString()));
                if (body.containsKey("locationTrackingEnabled"))
                        policy.setLocationTrackingEnabled(
                                        Boolean.parseBoolean(body.get("locationTrackingEnabled").toString()));

                policy.setAppliedAt(LocalDateTime.now());
                policyRepo.save(policy);

                // Sync AMAPI dynamically!
                try {
                        amapiService.patchDevicePolicy(ENTERPRISE_NAME, "policy1", policy, appRestrictionRepo.findByDeviceId(deviceId));
                } catch(Exception e) {
                        log.error("Failed to sync policy to AMAPI", e);
                }

                return ResponseEntity.ok(Map.of("message", "Policy updated"));
        }

        // ════════════════════════════════════════
        // DEVICE COMMANDS (lock, wipe)
        // ════════════════════════════════════════

        @PostMapping("/devices/{deviceId}/lock")
        public ResponseEntity<?> lockDevice(@PathVariable String deviceId) {
                try {
                        amapiService.issueCommand(ENTERPRISE_NAME, deviceId, "LOCK");
                } catch(Exception e) {
                        log.error("AMAPI Lock failed", e);
                        return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
                }

                DeviceCommand command = commandRepo.save(DeviceCommand.builder()
                                .deviceId(deviceId).commandType("LOCK")
                                .executed(true).createdAt(LocalDateTime.now()).build());
                String employeeName = deviceRepo.findByDeviceId(deviceId)
                                .map(Device::getEmployeeName).orElse("Unknown");
                activityRepo.save(DeviceActivity.builder()
                                .deviceId(deviceId).employeeName(employeeName)
                                .activityType("DEVICE_LOCKED").description(employeeName + "'s Device locked via AMAPI")
                                .severity("WARNING").createdAt(LocalDateTime.now()).build());
                alertRepo.save(MdmAlert.builder()
                                .deviceId(deviceId).employeeName(employeeName)
                                .alertType("DEVICE_LOCKED").message(employeeName + "'s Device locked via AMAPI")
                                .isRead(false).severity("WARNING").createdAt(LocalDateTime.now()).build());
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Lock command issued via AMAPI");
                response.put("commandId", command.getId());
                response.put("status", "EXECUTED");
                response.put("commandType", "LOCK");
                return ResponseEntity.ok(response);
        }

        @PostMapping("/devices/{deviceId}/wipe")
        @Transactional
        public ResponseEntity<?> wipeDevice(@PathVariable String deviceId) {
                try {
                        amapiService.issueCommand(ENTERPRISE_NAME, deviceId, "WIPE_DATA");
                } catch(Exception e) {
                        log.error("AMAPI Wipe failed", e);
                        return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
                }

                DeviceCommand command = commandRepo.save(DeviceCommand.builder()
                                .deviceId(deviceId).commandType("WIPE")
                                .executed(true).createdAt(LocalDateTime.now()).build());
                String employeeName = deviceRepo.findByDeviceId(deviceId)
                                .map(Device::getEmployeeName).orElse("Unknown");

                markDeviceOfflineForWipe(deviceId);

                activityRepo.save(DeviceActivity.builder()
                                .deviceId(deviceId).employeeName(employeeName)
                                .activityType("WIPE_INITIATED").description(employeeName + "'s Device wiped via AMAPI")
                                .severity("CRITICAL").createdAt(LocalDateTime.now()).build());
                alertRepo.save(MdmAlert.builder()
                                .deviceId(deviceId).employeeName(employeeName)
                                .alertType("WIPE_INITIATED").message(employeeName + "'s Device wipe executed via AMAPI")
                                .isRead(false).severity("CRITICAL").createdAt(LocalDateTime.now()).build());
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Wipe command issued via AMAPI");
                response.put("commandId", command.getId());
                response.put("status", "EXECUTED");
                response.put("commandType", "WIPE");
                return ResponseEntity.ok(response);
        }

        @PostMapping("/devices/{deviceId}/command")
        public ResponseEntity<?> createCommand(
                        @PathVariable String deviceId,
                        @RequestBody Map<String, Object> body) {
                String type = body.get("type") != null ? body.get("type").toString() : null;
                String packageName = body.get("packageName") != null ? body.get("packageName").toString() : null;

                if (type == null || type.isBlank()) {
                        return ResponseEntity.badRequest().body(Map.of("message", "type is required"));
                }

                String commandType = type;
                if ("CLEAR_APP_DATA".equals(type) && packageName != null && !packageName.isBlank()) {
                        commandType = "CLEAR_APP_DATA:" + packageName;
                }

                DeviceCommand command = commandRepo.save(DeviceCommand.builder()
                                .deviceId(deviceId)
                                .commandType(commandType)
                                .packageName(packageName)
                                .executed(false)
                                .createdAt(LocalDateTime.now())
                                .build());

                String employeeName = deviceRepo.findByDeviceId(deviceId)
                                .map(Device::getEmployeeName).orElse("Unknown");
                activityRepo.save(DeviceActivity.builder()
                                .deviceId(deviceId)
                                .employeeName(employeeName)
                                .activityType(type)
                                .description(type + " command queued for " + employeeName + "'s Device")
                                .severity("WARNING")
                                .createdAt(LocalDateTime.now())
                                .build());

                Map<String, Object> response = new HashMap<>();
                response.put("message", type + " command queued");
                response.put("commandId", command.getId());
                response.put("status", "QUEUED");
                response.put("commandType", commandType);
                return ResponseEntity.ok(response);
        }

        @GetMapping("/devices/{deviceId}/commands/{commandId}")
        public ResponseEntity<?> getCommandStatus(
                        @PathVariable String deviceId,
                        @PathVariable Long commandId) {
                return commandRepo.findById(commandId)
                                .filter(cmd -> deviceId.equals(cmd.getDeviceId()))
                                .map(cmd -> {
                                        Map<String, Object> response = new HashMap<>();
                                        response.put("id", cmd.getId());
                                        response.put("deviceId", cmd.getDeviceId());
                                        response.put("commandType", cmd.getCommandType());
                                        response.put("executed", cmd.isExecuted());
                                        response.put("status", cmd.isExecuted() ? "EXECUTED" : "QUEUED");
                                        response.put("createdAt", cmd.getCreatedAt());
                                        response.put("executedAt", cmd.getExecutedAt());
                                        return ResponseEntity.ok(response);
                                })
                                .orElse(ResponseEntity.notFound().build());
        }

        @GetMapping("/devices/{deviceId}/pending-commands")
        public ResponseEntity<List<DeviceCommand>> getPendingCommands(@PathVariable String deviceId) {
                return ResponseEntity.ok(
                                commandRepo.findByDeviceIdAndExecutedFalseOrderByCreatedAtAsc(deviceId));
        }

        @PostMapping("/devices/{deviceId}/commands/{commandId}/ack")
        public ResponseEntity<?> ackCommand(@PathVariable String deviceId,
                        @PathVariable Long commandId) {
                return commandRepo.findById(commandId).map(cmd -> {
                        cmd.setExecuted(true);
                        cmd.setExecutedAt(LocalDateTime.now());
                        commandRepo.save(cmd);

                        if ("WIPE".equalsIgnoreCase(cmd.getCommandType())) {
                                markDeviceOfflineForWipe(deviceId);
                        }

                        return ResponseEntity.ok(Map.of("message", "Command acknowledged"));
                }).orElse(ResponseEntity.notFound().build());
        }

        @PostMapping("/commands/{commandId}/executed")
        public ResponseEntity<?> markCommandExecuted(@PathVariable Long commandId) {
                return commandRepo.findById(commandId).map(cmd -> {
                        cmd.setExecuted(true);
                        cmd.setExecutedAt(LocalDateTime.now());
                        commandRepo.save(cmd);

                        if ("WIPE".equalsIgnoreCase(cmd.getCommandType())) {
                                markDeviceOfflineForWipe(cmd.getDeviceId());
                        }

                        return ResponseEntity.ok(Map.of("message", "Command marked as executed"));
                }).orElse(ResponseEntity.notFound().build());
        }

        private void markDeviceOfflineForWipe(String deviceId) {
                deviceRepo.findByDeviceId(deviceId).ifPresent(device -> {
                        device.setStatus("OFFLINE");
                        device.setLastSeenAt(LocalDateTime.now().minusHours(1));
                        deviceRepo.save(device);
                });
        }

        // ════════════════════════════════════════
        // GPS LOCATION
        // ════════════════════════════════════════

        @PostMapping("/devices/{deviceId}/location")
        public ResponseEntity<?> reportLocation(@PathVariable String deviceId,
                        @RequestBody Map<String, Object> body) {
                Double lat = body.get("latitude") != null
                                ? Double.parseDouble(body.get("latitude").toString())
                                : null;
                Double lng = body.get("longitude") != null
                                ? Double.parseDouble(body.get("longitude").toString())
                                : null;
                Float accuracy = body.get("accuracy") != null
                                ? Float.parseFloat(body.get("accuracy").toString())
                                : null;
                Double altitude = body.get("altitude") != null
                                ? Double.parseDouble(body.get("altitude").toString())
                                : null;
                Float bearing = body.get("bearing") != null
                                ? Float.parseFloat(body.get("bearing").toString())
                                : null;
                Float speed = body.get("speed") != null
                                ? Float.parseFloat(body.get("speed").toString())
                                : null;
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

                // Keep only latest 50 points per device.
                accurateLocationRepo.deleteOldLocations(deviceId);

                activityRepo.save(DeviceActivity.builder()
                                .deviceId(deviceId)
                                .employeeName(employeeName)
                                .activityType("LOCATION_UPDATED")
                                .description("Location updated")
                                .severity("INFO")
                                .createdAt(LocalDateTime.now())
                                .build());

                return ResponseEntity.ok(Map.of("message", "Location recorded"));
        }

        @GetMapping("/devices/{deviceId}/location")
        public ResponseEntity<?> getLocation(@PathVariable String deviceId) {
                return accurateLocationRepo.findTopByDeviceIdOrderByRecordedAtDesc(deviceId)
                                .map(ResponseEntity::ok)
                                .orElse(ResponseEntity.notFound().build());
        }

        @GetMapping("/devices/{deviceId}/location/history")
        public ResponseEntity<List<AccurateDeviceLocation>> getLocationHistory(
                        @PathVariable String deviceId,
                        @RequestParam(defaultValue = "5") int limit) {
                int safeLimit = Math.max(1, Math.min(limit, 10));
                List<AccurateDeviceLocation> locations = accurateLocationRepo
                                .findTop10ByDeviceIdOrderByRecordedAtDesc(deviceId)
                                .stream()
                                .limit(safeLimit)
                                .toList();

                return ResponseEntity.ok(locations);
        }

        @PostMapping("/devices/{deviceId}/accurate-location")
        public ResponseEntity<?> reportAccurateLocation(@PathVariable String deviceId,
                        @RequestBody Map<String, Object> body) {
                return reportLocation(deviceId, body);
        }

        @GetMapping("/devices/{deviceId}/accurate-location")
        public ResponseEntity<?> getAccurateLocation(@PathVariable String deviceId) {
                return getLocation(deviceId);
        }

        @GetMapping("/devices/{deviceId}/accurate-location/history")
        public ResponseEntity<List<AccurateDeviceLocation>> getAccurateLocationHistory(
                        @PathVariable String deviceId,
                        @RequestParam(defaultValue = "5") int limit) {
                return getLocationHistory(deviceId, limit);
        }

        @PostMapping("/devices/{deviceId}/sign-out")
        public ResponseEntity<?> signOutDevice(@PathVariable String deviceId) {
                // 1. Find the device and set status to OFFLINE.
                // Also backdate lastSeenAt so heartbeat reconciler doesn't immediately restore ACTIVE.
                Device device = deviceRepo.findByDeviceId(deviceId)
                                .orElse(null);
                if (device == null) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                        .body(Map.of("message", "Device not found"));
                }
                device.setStatus("OFFLINE");
                device.setLastSeenAt(LocalDateTime.now().minusHours(1));
                deviceRepo.save(device);

                // 2. Log the activity for the Admin Dashboard
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

}
