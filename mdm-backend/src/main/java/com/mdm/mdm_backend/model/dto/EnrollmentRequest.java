package com.mdm.mdm_backend.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EnrollmentRequest {

    @NotBlank(message = "employeeId is required")
    private String employeeId;

    @NotBlank(message = "employeeName is required")
    private String employeeName;

    @NotBlank(message = "type is required")
    private String type;
}
