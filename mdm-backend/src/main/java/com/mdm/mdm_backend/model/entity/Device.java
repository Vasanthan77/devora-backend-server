package com.mdm.mdm_backend.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "devices")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String deviceId;

    private String enrollmentToken;
    private String enrollmentMethod;

    @Column(nullable = false)
    private LocalDateTime enrolledAt;

    private String status;

    // Employee Information
    private String employeeId;
    private String employeeName;

    // Device Information
    private String deviceModel;
    private String manufacturer;

    private LocalDateTime lastSeenAt;
}