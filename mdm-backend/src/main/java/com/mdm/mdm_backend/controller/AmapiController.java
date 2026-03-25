package com.mdm.mdm_backend.controller;

import com.mdm.mdm_backend.service.AmapiService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/amapi")
@RequiredArgsConstructor
public class AmapiController {

    private final AmapiService amapiService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${amapi.enterprise-name:enterprises/LC01uq3ykm}")
    private String defaultEnterpriseName;

    @Value("${amapi.policy-id:policy1}")
    private String defaultPolicyId;

    @PostMapping("/enterprise")
    public ResponseEntity<?> createEnterprise() {
        try {
            String result = amapiService.createEnterprise();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/policy")
    public ResponseEntity<?> createPolicy(@RequestParam String enterpriseName,
            @RequestParam String policyId) {
        try {
            String result = amapiService.createPolicy(enterpriseName, policyId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/token")
    public ResponseEntity<?> createEnrollmentToken(@RequestParam String enterpriseName,
            @RequestParam String policyId) {
        try {
            String result = amapiService.createEnrollmentToken(enterpriseName, policyId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/enterprise/pubsub")
    public ResponseEntity<?> setPubSubTopic(
            @RequestParam String enterpriseName,
            @RequestParam String projectId,
            @RequestParam String topicName,
            @RequestParam(required = false) String notificationTypes) {
        try {
            List<String> parsedTypes = null;
            if (notificationTypes != null && !notificationTypes.isBlank()) {
                parsedTypes = Arrays.stream(notificationTypes.split(","))
                        .map(String::trim)
                        .filter(type -> !type.isBlank())
                        .toList();
            }

            String result = amapiService.updateEnterprisePubsubTopic(enterpriseName, projectId, topicName, parsedTypes);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/enterprise")
    public ResponseEntity<?> getEnterprise(@RequestParam(required = false) String enterpriseName) {
        try {
            String resolvedEnterprise = enterpriseName != null && !enterpriseName.isBlank()
                    ? enterpriseName
                    : defaultEnterpriseName;
            String result = amapiService.getEnterprise(resolvedEnterprise);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/enterprise/upgrade/eligibility")
    public ResponseEntity<?> getUpgradeEligibility(@RequestParam(required = false) String enterpriseName) {
        try {
            String resolvedEnterprise = enterpriseName != null && !enterpriseName.isBlank()
                    ? enterpriseName
                    : defaultEnterpriseName;

            String rawEnterprise = amapiService.getEnterprise(resolvedEnterprise);
            String enterpriseType = extractEnterpriseType(rawEnterprise);
            boolean eligible = "MANAGED_GOOGLE_PLAY_ACCOUNTS_ENTERPRISE".equals(enterpriseType);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("enterpriseName", resolvedEnterprise);
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
            @RequestParam(required = false) String enterpriseName,
            @RequestBody(required = false) Map<String, Object> body) {
        try {
            String resolvedEnterprise = enterpriseName != null && !enterpriseName.isBlank()
                    ? enterpriseName
                    : defaultEnterpriseName;

            String adminEmail = body != null && body.get("adminEmail") != null
                    ? body.get("adminEmail").toString()
                    : null;

            List<String> allowedDomains = new ArrayList<>();
            if (body != null && body.get("allowedDomains") instanceof List<?> domains) {
                for (Object domain : domains) {
                    if (domain != null) {
                        String asText = domain.toString().trim();
                        if (!asText.isBlank()) {
                            allowedDomains.add(asText);
                        }
                    }
                }
            }

            String result = amapiService.generateEnterpriseUpgradeUrl(
                    resolvedEnterprise,
                    allowedDomains.isEmpty() ? Collections.emptyList() : allowedDomains,
                    adminEmail);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/enterprise/upgrade/status")
    public ResponseEntity<?> getUpgradeStatus(@RequestParam(required = false) String enterpriseName) {
        try {
            String resolvedEnterprise = enterpriseName != null && !enterpriseName.isBlank()
                    ? enterpriseName
                    : defaultEnterpriseName;

            String rawEnterprise = amapiService.getEnterprise(resolvedEnterprise);
            String enterpriseType = extractEnterpriseType(rawEnterprise);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("enterpriseName", resolvedEnterprise);
            response.put("enterpriseType", enterpriseType == null ? "UNKNOWN" : enterpriseType);
            response.put("upgradeCompleted", "MANAGED_GOOGLE_DOMAIN".equals(enterpriseType));
            response.put("rawEnterprise", rawEnterprise);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/devices")
    public ResponseEntity<?> listDevices(@RequestParam(required = false) String enterpriseName) {
        try {
            String result = amapiService.listDevices(
                    enterpriseName != null && !enterpriseName.isBlank() ? enterpriseName : defaultEnterpriseName);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/devices/{deviceId}")
    public ResponseEntity<?> getDevice(@org.springframework.web.bind.annotation.PathVariable String deviceId,
            @RequestParam(required = false) String enterpriseName) {
        try {
            String result = amapiService.getDevice(
                    enterpriseName != null && !enterpriseName.isBlank() ? enterpriseName : defaultEnterpriseName,
                    deviceId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/devices/{deviceId}/lock")
    public ResponseEntity<?> lockDevice(@org.springframework.web.bind.annotation.PathVariable String deviceId,
            @RequestParam(required = false) String enterpriseName) {
        try {
            String result = amapiService.issueCommand(
                    enterpriseName != null && !enterpriseName.isBlank() ? enterpriseName : defaultEnterpriseName,
                    deviceId,
                    "LOCK");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/devices/{deviceId}/wipe")
    public ResponseEntity<?> wipeDevice(@org.springframework.web.bind.annotation.PathVariable String deviceId,
            @RequestParam(required = false) String enterpriseName) {
        try {
            String result = amapiService.issueCommand(
                    enterpriseName != null && !enterpriseName.isBlank() ? enterpriseName : defaultEnterpriseName,
                    deviceId,
                    "WIPE_DATA");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/devices/{deviceId}/command")
    public ResponseEntity<?> issueCommand(@org.springframework.web.bind.annotation.PathVariable String deviceId,
            @RequestBody Map<String, Object> body,
            @RequestParam(required = false) String enterpriseName) {
        try {
            String commandType = body.get("type") != null ? body.get("type").toString() : "";
            if (commandType.isBlank()) {
                return ResponseEntity.badRequest().body("Error: command type is required");
            }

            String result = amapiService.issueCommand(
                    enterpriseName != null && !enterpriseName.isBlank() ? enterpriseName : defaultEnterpriseName,
                    deviceId,
                    commandType);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/policy/sync")
    public ResponseEntity<?> syncDefaultPolicy(@RequestParam(required = false) String enterpriseName,
            @RequestParam(required = false) String policyId) {
        try {
            String resolvedEnterprise = enterpriseName != null && !enterpriseName.isBlank()
                    ? enterpriseName
                    : defaultEnterpriseName;
            String resolvedPolicy = policyId != null && !policyId.isBlank()
                    ? policyId
                    : defaultPolicyId;

            String result = amapiService.createPolicy(resolvedEnterprise, resolvedPolicy);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    private String extractEnterpriseType(String enterpriseJson) {
        if (enterpriseJson == null || enterpriseJson.isBlank()) {
            return null;
        }

        try {
            JsonNode root = objectMapper.readTree(enterpriseJson);
            JsonNode enterpriseTypeNode = root.path("enterpriseType");
            if (enterpriseTypeNode.isMissingNode() || enterpriseTypeNode.isNull()) {
                return null;
            }
            String value = enterpriseTypeNode.asText();
            return value == null || value.isBlank() ? null : value;
        } catch (Exception ex) {
            return null;
        }
    }
}
