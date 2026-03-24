package com.mdm.mdm_backend.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.ObjectMapper;

import java.util.Base64;
import java.util.Map;

@RestController
@RequestMapping("/webhook")
@Slf4j
public class WebhookController {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static String textOrDefault(Map<String, Object> payload, String fieldName, String fallback) {
        Object value = payload.get(fieldName);
        if (value == null) {
            return fallback;
        }
        String text = value.toString();
        return text.isBlank() ? fallback : text;
    }

    private static int intOrDefault(Map<String, Object> payload, String fieldName, int fallback) {
        Object value = payload.get(fieldName);
        if (value == null) {
            return fallback;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

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

            @SuppressWarnings("unchecked")
            Map<String, Object> amapiEvent = objectMapper.readValue(decodedEventStr, Map.class);
            String eventType = textOrDefault(amapiEvent, "type", "UNKNOWN");
            String deviceId = textOrDefault(amapiEvent, "deviceId", "UNKNOWN");

            log.info("Received AMAPI Event [Type: {}] for Device [{}]", eventType, deviceId);

            // Log detailed status if it's a STATUS_REPORT
            if ("STATUS_REPORT".equals(eventType)) {
                log.info("- Battery: {}%", intOrDefault(amapiEvent, "batteryLevel", 0));
                log.info("- Network: {}", textOrDefault(amapiEvent, "networkType", "UNKNOWN"));
                log.info("- Applied Policy: {}", textOrDefault(amapiEvent, "appliedPolicyName", "UNKNOWN"));
                // Here you can actively update your local DeviceRepository based on this
                // heart-beat style data!
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
