package com.mdm.mdm_backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminLoginResponse {

    private boolean success;
    private String name;
    private String message;
}
