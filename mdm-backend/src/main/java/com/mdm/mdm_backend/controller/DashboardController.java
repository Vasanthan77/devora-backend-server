package com.mdm.mdm_backend.controller;

import com.mdm.mdm_backend.model.dto.DashboardStats;
import com.mdm.mdm_backend.service.AppInventoryService;
import com.mdm.mdm_backend.service.EnrollmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DashboardController {

    private final EnrollmentService enrollmentService;
    private final AppInventoryService appInventoryService;

    @GetMapping("/dashboard/stats")
    public ResponseEntity<DashboardStats> getStats() {
        long totalDevices = enrollmentService.countDevices();
        long activeDevices = enrollmentService.countByStatus("ACTIVE");
        long inactiveDevices = Math.max(0L, totalDevices - activeDevices);

        DashboardStats stats = DashboardStats.builder()
            .totalDevices(totalDevices)
            .activeDevices(activeDevices)
            .inactiveDevices(inactiveDevices)
                .totalApps(appInventoryService.countApps())
                .build();
        return ResponseEntity.ok(stats);
    }
}
