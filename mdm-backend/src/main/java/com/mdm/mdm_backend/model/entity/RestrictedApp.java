package com.mdm.mdm_backend.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "restricted_apps", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"deviceId", "packageName"})
})
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class RestrictedApp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String deviceId;

    @Column(nullable = false)
    private String packageName;

    private String appName;

    private LocalDateTime restrictedAt;
}
