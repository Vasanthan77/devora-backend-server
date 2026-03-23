package com.mdm.mdm_backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceResponse {

    private Long id;
    private String deviceId;
    private String employeeId;
    private String employeeName;
    private String enrollmentMethod;
    private String deviceModel;
    private String manufacturer;
    private String osVersion;
    private String sdkVersion;
    private String serialNumber;
    private Boolean deviceOwnerSet;
    private LocalDateTime enrolledAt;
    private String status;
}
