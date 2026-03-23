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
public class EnrollmentTokenResponse {
    private Long id;
    private String token;
    private String employeeId;
    private String employeeName;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private String status;
    private String deviceId;
}
