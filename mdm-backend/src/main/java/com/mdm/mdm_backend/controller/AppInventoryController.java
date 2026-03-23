package com.mdm.mdm_backend.controller;

import com.mdm.mdm_backend.model.dto.AppInventoryRequest;
import com.mdm.mdm_backend.model.dto.NewAppNotificationRequest;
import com.mdm.mdm_backend.model.entity.AdminNotification;
import com.mdm.mdm_backend.model.entity.AppInventory;
import com.mdm.mdm_backend.repository.AdminNotificationRepository;
import com.mdm.mdm_backend.repository.DeviceRepository;
import com.mdm.mdm_backend.repository.AppInventoryRepository;
import com.mdm.mdm_backend.service.AppInventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class AppInventoryController {

    private final AppInventoryService appInventoryService;
    private final AdminNotificationRepository notificationRepository;
    private final DeviceRepository deviceRepository;
    private final AppInventoryRepository appInventoryRepository;

    @PostMapping("/app-inventory")
    public ResponseEntity<?> saveInventory(@Valid @RequestBody AppInventoryRequest request) {
        List<AppInventory> saved = appInventoryService.saveInventory(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                        "message", "App inventory saved successfully",
                        "appsCount", saved.size()));
    }

    @GetMapping("/app-inventory/{deviceId}")
    public ResponseEntity<List<AppInventory>> getInventory(@PathVariable String deviceId) {
        List<AppInventory> apps = appInventoryService.getInventory(deviceId);
        if (apps.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(apps);
    }

    @PostMapping("/app-inventory/notify")
    public ResponseEntity<?> notifyNewApp(@Valid @RequestBody NewAppNotificationRequest request) {
        // Look up employee name from device
        String employeeName = deviceRepository.findByDeviceId(request.getDeviceId())
                .map(d -> d.getEmployeeName() != null ? d.getEmployeeName() : request.getDeviceId().substring(0, 8))
                .orElse(request.getDeviceId().substring(0, 8));

        String title = request.getAction().equals("INSTALLED")
                ? "New app installed"
                : "App updated";

        // Enhanced message with more details
        String message = String.format(
                "%s %s app:\n- Name: %s\n- Package: %s\n- Version: %s (%d)\n- System App: %s",
                employeeName,
                request.getAction().equals("INSTALLED") ? "installed" : "updated",
                request.getAppName(),
                request.getPackageName(),
                request.getVersionName() != null ? request.getVersionName() : "",
                request.getVersionCode() != null ? request.getVersionCode() : 0,
                request.getIsSystemApp() != null && request.getIsSystemApp() ? "Yes" : "No");

        AdminNotification notification = AdminNotification.builder()
                .deviceId(request.getDeviceId())
                .type("APP_" + request.getAction())
                .title(title)
                .message(message)
                .read(false)
                .createdAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();
        notificationRepository.save(notification);

        // Also add/update the app in inventory
        AppInventory app = AppInventory.builder()
                .deviceId(request.getDeviceId())
                .appName(request.getAppName())
                .packageName(request.getPackageName())
                .versionName(request.getVersionName())
                .versionCode(request.getVersionCode())
                .isSystemApp(request.getIsSystemApp() != null ? request.getIsSystemApp() : false)
                .collectedAt(LocalDateTime.now())
                .build();
        appInventoryRepository.save(app);

        log.info("App {} notification: {} on device {}", request.getAction(), request.getAppName(),
                request.getDeviceId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Notification recorded"));
    }

    @GetMapping("/notifications")
    public ResponseEntity<List<AdminNotification>> getNotifications() {
        return ResponseEntity.ok(notificationRepository.findAllByOrderByCreatedAtDesc());
    }

    @GetMapping("/notifications/unread")
    public ResponseEntity<?> getUnreadCount() {
        long count = notificationRepository.countByReadFalse();
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PutMapping("/notifications/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long id) {
        return notificationRepository.findById(id)
                .map(n -> {
                    n.setRead(true);
                    notificationRepository.save(n);
                    return ResponseEntity.ok(Map.of("message", "Marked as read"));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
