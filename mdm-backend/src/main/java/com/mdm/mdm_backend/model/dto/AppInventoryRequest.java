package com.mdm.mdm_backend.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class AppInventoryRequest {

    @NotBlank(message = "deviceId is required")
    private String deviceId;

    @NotNull(message = "apps list is required")
    private List<AppDto> apps;
}
