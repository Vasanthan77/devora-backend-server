package com.mdm.mdm_backend.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "device_app_restrictions", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"deviceId", "packageName"})
})
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class DeviceAppRestriction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String deviceId;

    @Column(nullable = false)
    private String packageName;

    private String appName;

    private String installSource;

    @Column(nullable = false)
    private boolean restricted;

    private LocalDateTime appliedAt;

    private String appliedBy;
}
