package com.mdm.mdm_backend.controller;

import com.mdm.mdm_backend.model.entity.DeviceActivity;
import com.mdm.mdm_backend.repository.DeviceActivityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ActivityController {

    private final DeviceActivityRepository activityRepository;

    @GetMapping("/activities")
    public ResponseEntity<List<DeviceActivity>> getActivities(
            @RequestParam(defaultValue = "10") int limit) {
        List<DeviceActivity> all = activityRepository.findAllByOrderByCreatedAtDesc();
        List<DeviceActivity> result = all.stream().limit(limit).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

        @GetMapping("/activities/device/{deviceId}")
        public ResponseEntity<List<DeviceActivity>> getDeviceActivities(
            @PathVariable String deviceId,
            @RequestParam(defaultValue = "20") int limit
        ) {
        List<DeviceActivity> activities = activityRepository.findByDeviceIdOrderByCreatedAtDesc(deviceId);
        return ResponseEntity.ok(
                activities.stream()
                .limit(limit)
                        .collect(Collectors.toList()));
    }

    @DeleteMapping("/activities/{activityId}")
    public ResponseEntity<Void> deleteActivity(@PathVariable Long activityId) {
        if (!activityRepository.existsById(activityId)) {
            return ResponseEntity.notFound().build();
        }
        activityRepository.deleteById(activityId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/activities/device/{deviceId}")
    public ResponseEntity<Void> deleteAllDeviceActivities(@PathVariable String deviceId) {
        activityRepository.deleteByDeviceId(deviceId);
        return ResponseEntity.noContent().build();
    }
}
