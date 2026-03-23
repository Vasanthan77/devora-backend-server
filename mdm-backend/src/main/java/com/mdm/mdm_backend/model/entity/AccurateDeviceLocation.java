package com.mdm.mdm_backend.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "accurate_device_locations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccurateDeviceLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false)
    private String deviceId;

    private Double latitude;
    private Double longitude;
    private Float accuracy;
    private Double altitude;
    private Float bearing;
    private Float speed;
    private String address;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;
}
