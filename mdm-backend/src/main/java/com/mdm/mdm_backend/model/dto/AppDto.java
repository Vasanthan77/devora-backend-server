package com.mdm.mdm_backend.model.dto;

import lombok.Data;

@Data
public class AppDto {
    private String appName;
    private String packageName;
    private String versionName;
    private Long versionCode;
    private String installSource;
    private Boolean isSystemApp;
    private Boolean isSuspended;
    private String iconBase64;
}