package com.mdm.mdm_backend.controller;

import com.mdm.mdm_backend.service.AmapiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/amapi")
public class AmapiController {

    @Autowired
    private AmapiService amapiService;

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
    public ResponseEntity<?> createPolicy(@org.springframework.web.bind.annotation.RequestParam String enterpriseName, 
                                          @org.springframework.web.bind.annotation.RequestParam String policyId) {
        try {
            String result = amapiService.createPolicy(enterpriseName, policyId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/token")
    public ResponseEntity<?> createEnrollmentToken(@org.springframework.web.bind.annotation.RequestParam String enterpriseName, 
                                                   @org.springframework.web.bind.annotation.RequestParam String policyId) {
        try {
            String result = amapiService.createEnrollmentToken(enterpriseName, policyId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/enterprise/pubsub")
    public ResponseEntity<?> setPubSubTopic(
            @org.springframework.web.bind.annotation.RequestParam String enterpriseName,
            @org.springframework.web.bind.annotation.RequestParam String projectId,
            @org.springframework.web.bind.annotation.RequestParam String topicName) {
        try {
            String result = amapiService.updateEnterprisePubsubTopic(enterpriseName, projectId, topicName);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}
