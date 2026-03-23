package com.mdm.mdm_backend.model.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardStats {
    private long totalDevices;
    private long activeDevices;
    private long inactiveDevices;
    private long totalApps;
}
