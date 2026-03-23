package com.mdm.mdm_backend.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "device_info")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class DeviceInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String deviceId;

    private String model;
    private String manufacturer;
    private String osVersion;
    private String sdkVersion;
    private String serialNumber;
    private String imei;
    private String deviceType;
    private Boolean deviceOwnerSet;
    private String employeeId;

    private LocalDateTime collectedAt;
}