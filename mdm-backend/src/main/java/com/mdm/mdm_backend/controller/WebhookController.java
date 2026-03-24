package com.mdm.mdm_backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.Map;

@RestController
@RequestMapping("/webhook")
@Slf4j
public class WebhookController {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/amapi")
    public ResponseEntity<String> receiveAmapiPubSubEvent(@RequestBody Map<String, Object> payload) {
        try {
            // Google Pub/Sub wrapper format
            @SuppressWarnings("unchecked")
            Map<String, Object> message = (Map<String, Object>) payload.get("message");
            if (message == null || !message.containsKey("data")) {
                log.warn("Invalid Pub/Sub payload received: {}", payload);
                return ResponseEntity.badRequest().body("Invalid payload");
            }

            // The actual device event is Base64 encoded in the "data" field
            String base64Data = (String) message.get("data");
            String decodedEventStr = new String(Base64.getDecoder().decode(base64Data));
            
            JsonNode amapiEvent = objectMapper.readTree(decodedEventStr);
            String eventType = amapiEvent.path("type").asText("UNKNOWN");
            String deviceId = amapiEvent.path("deviceId").asText("UNKNOWN");
            
            log.info("Received AMAPI Event [Type: {}] for Device [{}]", eventType, deviceId);

            // Log detailed status if it's a STATUS_REPORT
            if ("STATUS_REPORT".equals(eventType)) {
                log.info("- Battery: {}%", amapiEvent.path("batteryLevel").asInt());
                log.info("- Network: {}", amapiEvent.path("networkType").asText());
                log.info("- Applied Policy: {}", amapiEvent.path("appliedPolicyName").asText());
                // Here you can actively update your local DeviceRepository based on this heart-beat style data!
            } else if ("ENROLLMENT_REPORT".equals(eventType)) {
                log.info("Device {} successfully ENROLLED!", deviceId);
            }

            // Google Pub/Sub requires you to return a 200 OK immediately
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            log.error("Failed to parse AMAPI Event", e);
            // Must return 2xx even on error or Google will indefinitely retry
            return ResponseEntity.ok().build();
        }
    }
}
