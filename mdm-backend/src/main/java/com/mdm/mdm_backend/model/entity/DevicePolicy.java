package com.mdm.mdm_backend.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "device_policies")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class DevicePolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String deviceId;

    @Column(nullable = false)
    private boolean cameraDisabled;

    @Column(nullable = false)
    private boolean screenLockRequired;

    @Column(nullable = false)
    private boolean installBlocked;

    @Column(nullable = false)
    private boolean uninstallBlocked;

    @Column(nullable = false)
    private boolean locationTrackingEnabled;

    private LocalDateTime appliedAt;
}
