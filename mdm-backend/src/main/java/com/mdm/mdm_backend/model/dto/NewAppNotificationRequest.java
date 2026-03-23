package com.mdm.mdm_backend.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class NewAppNotificationRequest {

    @NotBlank
    private String deviceId;

    @NotBlank
    private String appName;

    @NotBlank
    private String packageName;

    private String versionName;
    private Long versionCode;
    private Boolean isSystemApp;

    @NotBlank
    private String action; // INSTALLED, UPDATED
}
