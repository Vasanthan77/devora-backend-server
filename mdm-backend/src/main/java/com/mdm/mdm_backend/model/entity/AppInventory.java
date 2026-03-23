package com.mdm.mdm_backend.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "app_inventory")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class AppInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String deviceId;

    private String appName;
    private String packageName;
    private String versionName;
    private Long versionCode;
    private String installSource;
    private Boolean isSystemApp;
    @Column(name = "is_suspended")
    private Boolean isSuspended;

    @Column(columnDefinition = "TEXT")
    private String iconBase64;

    private LocalDateTime collectedAt;
}
