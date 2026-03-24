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

import java.util.Map;

@RestController
@RequestMapping("/api/amapi")
@RequiredArgsConstructor
public class AmapiController {

    private final AmapiService amapiService;

    @Value("${amapi.enterprise-name:enterprises/LC01oh6rj0}")
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
            @RequestParam String topicName) {
        try {
            String result = amapiService.updateEnterprisePubsubTopic(enterpriseName, projectId, topicName);
            return ResponseEntity.ok(result);
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
}
