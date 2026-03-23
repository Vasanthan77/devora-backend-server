package com.mdm.mdm_backend.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "device_commands")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class DeviceCommand {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String deviceId;

    @Column(nullable = false)
    private String commandType; // LOCK, WIPE, CAMERA_ENABLE, CAMERA_DISABLE

    @Column
    private String packageName;

    @Column(nullable = false)
    private boolean executed;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime executedAt;
}
