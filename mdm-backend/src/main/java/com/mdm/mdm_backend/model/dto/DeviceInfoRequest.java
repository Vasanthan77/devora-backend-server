package com.mdm.mdm_backend.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DeviceInfoRequest {

    @NotBlank(message = "deviceId is required")
    private String deviceId;

    private String model;
    private String manufacturer;
    private String osVersion;
    private Integer sdkVersion;
    private String serialNumber;
    private String imei;
    private String deviceType;
    private Boolean deviceOwnerSet;
    private String employeeId;
}