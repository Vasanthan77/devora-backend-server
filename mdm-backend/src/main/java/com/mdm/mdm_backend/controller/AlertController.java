package com.mdm.mdm_backend.controller;

import com.mdm.mdm_backend.model.entity.MdmAlert;
import com.mdm.mdm_backend.repository.MdmAlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AlertController {

    private final MdmAlertRepository alertRepository;

    @GetMapping("/alerts/unread")
    public ResponseEntity<List<MdmAlert>> getUnreadAlerts() {
        return ResponseEntity.ok(alertRepository.findByIsReadFalseOrderByCreatedAtDesc());
    }

    @GetMapping("/alerts/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        return ResponseEntity.ok(Map.of("count", alertRepository.countByIsReadFalse()));
    }

    @PostMapping("/alerts/mark-read")
    public ResponseEntity<Map<String, String>> markRead(@RequestBody Map<String, List<Long>> body) {
        List<Long> alertIds = body.getOrDefault("alertIds", List.of());
        for (Long id : alertIds) {
            alertRepository.findById(id).ifPresent(alert -> {
                alert.setRead(true);
                alertRepository.save(alert);
            });
        }
        return ResponseEntity.ok(Map.of("message", "Marked " + alertIds.size() + " alerts as read"));
    }
}
