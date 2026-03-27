package com.mdm.mdm_backend.controller;

import com.mdm.mdm_backend.model.entity.Device;
import com.mdm.mdm_backend.model.entity.DeviceActivity;
import com.mdm.mdm_backend.model.entity.DeviceAppRestriction;
import com.mdm.mdm_backend.model.entity.DeviceCommand;
import com.mdm.mdm_backend.model.entity.DevicePolicy;
import com.mdm.mdm_backend.model.entity.MdmAlert;
import com.mdm.mdm_backend.repository.DeviceActivityRepository;
import com.mdm.mdm_backend.repository.DeviceAppRestrictionRepository;
import com.mdm.mdm_backend.repository.DeviceCommandRepository;
import com.mdm.mdm_backend.repository.DevicePolicyRepository;
import com.mdm.mdm_backend.repository.DeviceRepository;
import com.mdm.mdm_backend.repository.MdmAlertRepository;
import com.mdm.mdm_backend.service.AmapiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AmapiController — the single "Command Center" for all device management.
 */
@RestController
@RequestMapping("/api/amapi")
@RequiredArgsConstructor
@Slf4j
public class AmapiController {

    private final AmapiService amapiService;
    private final DeviceRepository deviceRepo;
    private final DeviceActivityRepository activityRepo;
    private final MdmAlertRepository alertRepo;
    private final DeviceAppRestrictionRepository appRestrictionRepo;
    private final DevicePolicyRepository policyRepo;
    private final DeviceCommandRepository commandRepo;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${amapi.enterprise-name:enterprises/LC03patpnu}")
    private String defaultEnterpriseName;

    @Value("${amapi.policy-id:policy1}")
    private String defaultPolicyId;

    // ════════════════════════════════════════
    // ENTERPRISE MANAGEMENT
    // ════════════════════════════════════════

    @GetMapping("/signup-url")
    public ResponseEntity<?> createSignupUrl(@RequestParam(name = "callbackUrl", defaultValue = "https://localhost:8080/api/amapi/enterprise/callback") String callbackUrl) {
        try {
            String result = amapiService.createSignupUrl(callbackUrl);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to create signup URL", e);
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/enterprise")
    public ResponseEntity<?> createEnterprise(@RequestBody(required = false) Map<String, String> body) {
        try {
            String result;
            if (body != null && body.containsKey("signupToken") && body.containsKey("enterpriseToken")) {
                result = amapiService.createEnterpriseWithToken(body.get("signupToken"), body.get("enterpriseToken"));
            } else if (body != null && body.containsKey("signupToken")) {
                // Fallback if only signupToken exists (though usually both are needed)
                result = amapiService.createEnterpriseWithToken(body.get("signupToken"), ""); 
            } else {
                result = amapiService.createEnterprise();
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to create enterprise", e);
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/enterprise")
    public ResponseEntity<?> getEnterprise(
            @RequestParam(name = "enterpriseName", required = false) String enterpriseName) {
        try {
            String resolved = resolve(enterpriseName);
            String result = amapiService.getEnterprise(resolved);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/projects/enterprises")
    public ResponseEntity<?> listAllEnterprises() {
        try {
            String result = amapiService.listEnterprises();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @DeleteMapping("/projects/enterprises/bulk-cleanup")
    public ResponseEntity<?> cleanupProjectEnterprises() {
        try {
            String raw = amapiService.listEnterprises();
            JsonNode root = objectMapper.readTree(raw);
            JsonNode enterprises = root.path("enterprises");

            int count = 0;
            if (enterprises.isArray()) {
                for (JsonNode ent : enterprises) {
                    String name = ent.path("name").asText();
                    if (name != null && !name.contains("LC03patpnu") && !name.isBlank()) {
                        try {
                            amapiService.deleteEnterprise(name);
                            count++;
                        } catch (Exception ex) {
                            log.warn("Failed to delete enterprise {}: {}", name, ex.getMessage());
                        }
                    }
                }
            }
            return ResponseEntity.ok(Map.of("message", "Cleanup finished", "deletedEnterprises", count));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/enterprise/upgrade/eligibility")
    public ResponseEntity<?> getUpgradeEligibility(
            @RequestParam(name = "enterpriseName", required = false) String enterpriseName) {
        try {
            String resolved = resolve(enterpriseName);
            String rawEnterprise = amapiService.getEnterprise(resolved);
            String enterpriseType = extractEnterpriseType(rawEnterprise);
            boolean eligible = "MANAGED_GOOGLE_PLAY_ACCOUNTS_ENTERPRISE".equals(enterpriseType);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("enterpriseName", resolved);
            response.put("enterpriseType", enterpriseType == null ? "UNKNOWN" : enterpriseType);
            response.put("eligibleForUpgrade", eligible);
            response.put("nextAction", eligible
                    ? "Call /api/amapi/enterprise/upgrade-url and show the URL to authorized IT admins."
                    : "Enterprise already upgraded or created as managed Google domain.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/enterprise/upgrade-url")
    public ResponseEntity<?> generateEnterpriseUpgradeUrl(
            @RequestParam(name = "enterpriseName", required = false) String enterpriseName,
            @RequestBody(required = false) Map<String, Object> body) {
        try {
            String resolved = resolve(enterpriseName);
            String adminEmail = body != null && body.get("adminEmail") != null
                    ? body.get("adminEmail").toString() : null;

            List<String> allowedDomains = new ArrayList<>();
            if (body != null && body.get("allowedDomains") instanceof List<?> domains) {
                for (Object domain : domains) {
                    if (domain != null) {
                        String asText = domain.toString().trim();
                        if (!asText.isBlank()) allowedDomains.add(asText);
                    }
                }
            }

            String result = amapiService.generateEnterpriseUpgradeUrl(
                    resolved, allowedDomains.isEmpty() ? Collections.emptyList() : allowedDomains, adminEmail);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/enterprise/upgrade/status")
    public ResponseEntity<?> getUpgradeStatus(
            @RequestParam(name = "enterpriseName", required = false) String enterpriseName) {
        try {
            String resolved = resolve(enterpriseName);
            String rawEnterprise = amapiService.getEnterprise(resolved);
            String enterpriseType = extractEnterpriseType(rawEnterprise);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("enterpriseName", resolved);
            response.put("enterpriseType", enterpriseType == null ? "UNKNOWN" : enterpriseType);
            response.put("upgradeCompleted", "MANAGED_GOOGLE_DOMAIN".equals(enterpriseType));
            response.put("rawEnterprise", rawEnterprise);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/enterprise/pubsub")
    public ResponseEntity<?> setPubSubTopic(
            @RequestParam(name = "enterpriseName") String enterpriseName,
            @RequestParam(name = "projectId") String projectId,
            @RequestParam(name = "topicName") String topicName,
            @RequestParam(name = "notificationTypes", required = false) String notificationTypes) {
        try {
            List<String> parsedTypes = null;
            if (notificationTypes != null && !notificationTypes.isBlank()) {
                parsedTypes = Arrays.stream(notificationTypes.split(","))
                        .map(String::trim).filter(type -> !type.isBlank()).toList();
            }
            String result = amapiService.updateEnterprisePubsubTopic(enterpriseName, projectId, topicName, parsedTypes);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    // ════════════════════════════════════════
    // POLICY & ENROLLMENT TOKEN
    // ════════════════════════════════════════

    @PostMapping("/policy")
    public ResponseEntity<?> createPolicy(
            @RequestParam(name = "enterpriseName") String enterpriseName,
            @RequestParam(name = "policyId") String policyId) {
        try {
            String result = amapiService.createPolicy(enterpriseName, policyId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/policy/sync")
    public ResponseEntity<?> syncDefaultPolicy(
            @RequestParam(name = "enterpriseName", required = false) String enterpriseName,
            @RequestParam(name = "policyId", required = false) String policyId) {
        try {
            String resolved = resolve(enterpriseName);
            String resolvedPolicy = policyId != null && !policyId.isBlank() ? policyId : defaultPolicyId;
            String result = amapiService.createPolicy(resolved, resolvedPolicy);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/token")
    public ResponseEntity<?> createEnrollmentToken(
            @RequestParam(name = "enterpriseName") String enterpriseName,
            @RequestParam(name = "policyId") String policyId) {
        try {
            String result = amapiService.createEnrollmentToken(enterpriseName, policyId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    // ════════════════════════════════════════
    // DEVICE LISTING
    // ════════════════════════════════════════

    @GetMapping("/devices")
    public ResponseEntity<?> listDevices(
            @RequestParam(name = "enterpriseName", required = false) String enterpriseName) {
        try {
            String result = amapiService.listDevices(resolve(enterpriseName));
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/devices/{deviceId}")
    public ResponseEntity<?> getDevice(
            @PathVariable(name = "deviceId") String deviceId,
            @RequestParam(name = "enterpriseName", required = false) String enterpriseName) {
        try {
            String result = amapiService.getDevice(resolve(enterpriseName), deviceId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @DeleteMapping("/devices/{deviceId}")
    public ResponseEntity<?> deleteDevice(
            @PathVariable(name = "deviceId") String deviceId,
            @RequestParam(name = "enterpriseName", required = false) String enterpriseName) {
        try {
            amapiService.deleteDevice(resolve(enterpriseName), deviceId);
            return ResponseEntity.ok(Map.of("message", "Device deleted successfully from Google AMAPI"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @DeleteMapping("/devices/bulk-delete")
    public ResponseEntity<?> deleteAllDevices(
            @RequestParam(name = "enterpriseName", required = false) String enterpriseName) {
        try {
            String resolved = resolve(enterpriseName);
            String rawDevices = amapiService.listDevices(resolved);
            JsonNode root = objectMapper.readTree(rawDevices);
            JsonNode devices = root.path("devices");

            int count = 0;
            if (devices.isArray()) {
                for (JsonNode device : devices) {
                    String fullDeviceName = device.path("name").asText();
                    if (fullDeviceName != null && !fullDeviceName.isBlank()) {
                        try {
                            amapiService.deleteDevice(resolved, fullDeviceName);
                            count++;
                        } catch (Exception ex) {
                            log.warn("Failed to delete device {}: {}", fullDeviceName, ex.getMessage());
                        }
                    }
                }
            }
            return ResponseEntity.ok(Map.of(
                    "message", "Bulk deletion completed from Google AMAPI quota",
                    "deletedCount", count));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error during bulk delete: " + e.getMessage());
        }
    }

    // ════════════════════════════════════════
    // DEVICE COMMANDS
    // ════════════════════════════════════════

    @PostMapping("/devices/{deviceId}/lock")
    public ResponseEntity<?> lockDevice(
            @PathVariable(name = "deviceId") String deviceId,
            @RequestParam(name = "enterpriseName", required = false) String enterpriseName) {
        try {
            String result = amapiService.issueCommand(resolve(enterpriseName), deviceId, "LOCK");
            String employeeName = getEmployeeName(deviceId);
            logCommand(deviceId, "LOCK", employeeName);
            logActivity(deviceId, employeeName, "DEVICE_LOCKED", employeeName + "'s Device locked via AMAPI", "WARNING");
            logAlert(deviceId, employeeName, "DEVICE_LOCKED", employeeName + "'s Device locked via AMAPI", "WARNING");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lock failed: " + e.getMessage());
        }
    }

    @PostMapping("/devices/{deviceId}/wipe")
    public ResponseEntity<?> wipeDevice(
            @PathVariable(name = "deviceId") String deviceId,
            @RequestParam(name = "enterpriseName", required = false) String enterpriseName) {
        try {
            String result = amapiService.issueCommand(resolve(enterpriseName), deviceId, "WIPE_DATA");
            deviceRepo.findByDeviceId(deviceId).ifPresent(device -> {
                device.setStatus("OFFLINE");
                device.setLastSeenAt(LocalDateTime.now().minusHours(1));
                deviceRepo.save(device);
            });
            String employeeName = getEmployeeName(deviceId);
            logCommand(deviceId, "WIPE_DATA", employeeName);
            logActivity(deviceId, employeeName, "WIPE_INITIATED", employeeName + "'s Device wiped via AMAPI", "CRITICAL");
            logAlert(deviceId, employeeName, "WIPE_INITIATED", employeeName + "'s Device wipe executed via AMAPI", "CRITICAL");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Wipe failed: " + e.getMessage());
        }
    }

    @PostMapping("/devices/{deviceId}/command")
    public ResponseEntity<?> issueCommand(
            @PathVariable(name = "deviceId") String deviceId,
            @RequestBody Map<String, Object> body,
            @RequestParam(name = "enterpriseName", required = false) String enterpriseName) {
        try {
            String commandType = body.get("type") != null ? body.get("type").toString() : "";
            if (commandType.isBlank()) {
                return ResponseEntity.badRequest().body("Error: command type is required");
            }

            String amapiType = switch (commandType.toUpperCase()) {
                case "LOCK" -> "LOCK";
                case "WIPE", "WIPE_DATA" -> "WIPE_DATA";
                case "REBOOT" -> "REBOOT";
                default -> commandType.toUpperCase();
            };

            String result = amapiService.issueCommand(resolve(enterpriseName), deviceId, amapiType);
            String employeeName = getEmployeeName(deviceId);
            logCommand(deviceId, amapiType, employeeName);
            logActivity(deviceId, employeeName, amapiType, amapiType + " command issued on " + employeeName + "'s Device via AMAPI", "WARNING");

            Map<String, Object> response = new HashMap<>();
            response.put("message", amapiType + " command issued via AMAPI");
            response.put("status", "EXECUTED");
            response.put("commandType", amapiType);
            response.put("amapiResponse", result);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    // ════════════════════════════════════════
    // APP RESTRICTIONS
    // ════════════════════════════════════════

    @PostMapping("/devices/{deviceId}/restrict-app")
    public ResponseEntity<?> restrictApp(
            @PathVariable(name = "deviceId") String deviceId,
            @RequestBody Map<String, Object> body) {
        String packageName = (String) body.get("packageName");
        String appName = (String) body.getOrDefault("appName", packageName);
        Boolean restricted = body.get("restricted") != null
                ? Boolean.valueOf(body.get("restricted").toString()) : true;

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
                    .restricted(restricted).appliedAt(LocalDateTime.now()).build();
        }
        appRestrictionRepo.save(restriction);

        try {
            DevicePolicy policy = policyRepo.findByDeviceId(deviceId).orElse(null);
            amapiService.patchDevicePolicy(defaultEnterpriseName, defaultPolicyId, policy,
                    appRestrictionRepo.findByDeviceId(deviceId));
        } catch (Exception e) {
            log.error("Failed to sync app restriction to AMAPI", e);
        }

        String employeeName = getEmployeeName(deviceId);
        String action = restricted ? "APP_RESTRICTED" : "APP_ALLOWED";
        logActivity(deviceId, employeeName, action, (restricted ? appName + " restricted on " : appName + " allowed on ") + employeeName + "'s Device", restricted ? "WARNING" : "INFO");
        return ResponseEntity.ok(Map.of("message", "App " + (restricted ? "restricted" : "allowed") + " successfully via AMAPI"));
    }

    @GetMapping("/devices/{deviceId}/restricted-apps")
    public ResponseEntity<List<DeviceAppRestriction>> getRestrictedApps(
            @PathVariable(name = "deviceId") String deviceId) {
        return ResponseEntity.ok(appRestrictionRepo.findByDeviceIdAndRestricted(deviceId, true));
    }

    // ════════════════════════════════════════
    // HELPERS
    // ════════════════════════════════════════

    private String resolve(String enterpriseName) {
        return (enterpriseName == null || enterpriseName.isBlank()) ? defaultEnterpriseName : enterpriseName;
    }

    private String getEmployeeName(String deviceId) {
        return deviceRepo.findByDeviceId(deviceId).map(Device::getEmployeeName).orElse("Unknown User");
    }

    private void logCommand(String deviceId, String type, String employee) {
        commandRepo.save(DeviceCommand.builder()
                .deviceId(deviceId)
                .commandType(type)
                .executed(true)
                .createdAt(LocalDateTime.now())
                .build());
    }

    private void logActivity(String deviceId, String name, String action, String desc, String severity) {
        activityRepo.save(DeviceActivity.builder()
                .deviceId(deviceId)
                .employeeName(name)
                .activityType(action)
                .description(desc)
                .severity(severity)
                .createdAt(LocalDateTime.now())
                .build());
    }

    private void logAlert(String deviceId, String name, String type, String msg, String severity) {
        alertRepo.save(MdmAlert.builder()
                .deviceId(deviceId)
                .employeeName(name)
                .alertType(type)
                .message(msg)
                .isRead(false)
                .severity(severity)
                .createdAt(LocalDateTime.now())
                .build());
    }

    private String extractEnterpriseType(String raw) {
        try {
            return objectMapper.readTree(raw).path("enterpriseType").asText();
        } catch (Exception e) { return "UNKNOWN"; }
    }
}
