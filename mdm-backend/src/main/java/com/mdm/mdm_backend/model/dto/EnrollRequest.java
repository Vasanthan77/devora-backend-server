package com.mdm.mdm_backend.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EnrollRequest {

    @NotBlank(message = "deviceId is required")
    private String deviceId;

    private String enrollmentToken;

    @NotBlank(message = "enrollmentMethod is required")
    private String enrollmentMethod;
}