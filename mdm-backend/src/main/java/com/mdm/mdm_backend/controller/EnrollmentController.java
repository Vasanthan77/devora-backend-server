package com.mdm.mdm_backend.controller;

import com.mdm.mdm_backend.model.dto.DeviceResponse;
import com.mdm.mdm_backend.model.dto.EnrollRequest;
import com.mdm.mdm_backend.model.dto.EnrollmentRequest;
import com.mdm.mdm_backend.model.dto.EnrollmentTokenResponse;
import com.mdm.mdm_backend.model.entity.Device;
import com.mdm.mdm_backend.service.AmapiService;
import com.mdm.mdm_backend.service.EnrollmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class EnrollmentController {

    private final EnrollmentService enrollmentService;
    private final AmapiService amapiService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${amapi.enterprise-name:enterprises/LC01uq3ykm}")
    private String enterpriseName;

    @Value("${amapi.policy-id:policy1}")
    private String defaultPolicyId;

    /**
     * Enroll a device with its details.
     * If enrollmentToken is provided, employee details are fetched from the token.
     */
    @PostMapping("/enroll")
    public ResponseEntity<DeviceResponse> enroll(@Valid @RequestBody EnrollRequest request) {
        Device device = enrollmentService.enrollDevice(request);
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(
                DeviceResponse.builder()
                        .id(device.getId())
                        .deviceId(device.getDeviceId())
                        .employeeId(device.getEmployeeId())
                        .employeeName(device.getEmployeeName())
                        .enrollmentMethod(device.getEnrollmentMethod())
                        .deviceModel(device.getDeviceModel())
                        .manufacturer(device.getManufacturer())
                        .enrolledAt(device.getEnrolledAt())
                        .status(device.getStatus())
                        .build());
    }

    /**
     * Generate AMAPI enrollment token. PostgreSQL is used for audit/history, not token storage.
     */
    @PostMapping("/enrollment/generate")
    public ResponseEntity<Map<String, Object>> generateToken(@Valid @RequestBody EnrollmentRequest request) {
        try {
            String raw = amapiService.createEnrollmentToken(enterpriseName, defaultPolicyId);
            JsonNode node = objectMapper.readTree(raw);
            String token = node.path("value").asText("");
            String expiresAt = node.path("expirationTimestamp").asText(null);
            String tokenName = node.path("name").asText(null);
            String qrCode = node.path("qrCode").asText(null);

                Map<String, Object> payload = new HashMap<>();
                payload.put("token", token);
                payload.put("tokenName", tokenName);
                payload.put("employeeId", request.getEmployeeId());
                payload.put("employeeName", request.getEmployeeName());
                payload.put("expiresAt", expiresAt);
                payload.put("qrCode", qrCode);
                payload.put("status", "PENDING");
                return ResponseEntity.ok(payload);
        } catch (Exception e) {
            log.error("Failed to generate AMAPI enrollment token", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "message", "Failed to generate AMAPI enrollment token",
                    "error", e.getMessage()));
        }
    }

    @DeleteMapping("/enrollment/{tokenId}")
    public ResponseEntity<Map<String, String>> revokeEnrollment(@PathVariable Long tokenId) {
        return ResponseEntity.ok(Map.of(
                "message", "AMAPI tokens are managed by Google; local token revocation is not supported in this endpoint"));
    }

    @GetMapping("/enrollment/active")
    public ResponseEntity<List<EnrollmentTokenResponse>> getActiveEnrollments() {
        return ResponseEntity.ok(List.of());
    }
}