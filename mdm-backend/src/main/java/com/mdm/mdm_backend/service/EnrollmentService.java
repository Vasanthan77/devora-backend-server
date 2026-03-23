package com.mdm.mdm_backend.service;

import com.mdm.mdm_backend.model.dto.DeviceResponse;
import com.mdm.mdm_backend.model.dto.EnrollRequest;
import com.mdm.mdm_backend.model.entity.Device;
import com.mdm.mdm_backend.model.entity.DeviceActivity;
import com.mdm.mdm_backend.model.entity.DeviceInfo;
import com.mdm.mdm_backend.model.entity.Employee;
import com.mdm.mdm_backend.model.entity.MdmAlert;
import com.mdm.mdm_backend.model.entity.EnrollmentToken;
import com.mdm.mdm_backend.repository.AppInventoryRepository;
import com.mdm.mdm_backend.repository.AccurateDeviceLocationRepository;
import com.mdm.mdm_backend.repository.AdminNotificationRepository;
import com.mdm.mdm_backend.repository.DeviceActivityRepository;
import com.mdm.mdm_backend.repository.DeviceAppRestrictionRepository;
import com.mdm.mdm_backend.repository.DeviceCommandRepository;
import com.mdm.mdm_backend.repository.DevicePolicyRepository;
import com.mdm.mdm_backend.repository.MdmAlertRepository;
import com.mdm.mdm_backend.repository.RestrictedAppRepository;
import com.mdm.mdm_backend.repository.DeviceInfoRepository;
import com.mdm.mdm_backend.repository.DeviceRepository;
import com.mdm.mdm_backend.repository.EmployeeRepository;
import com.mdm.mdm_backend.repository.EnrollmentTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnrollmentService {

    private final DeviceRepository deviceRepository;
    private final EnrollmentTokenRepository enrollmentTokenRepository;
    private final DeviceInfoRepository deviceInfoRepository;
    private final AppInventoryRepository appInventoryRepository;
    private final AdminNotificationRepository adminNotificationRepository;
    private final RestrictedAppRepository restrictedAppRepository;
    private final DeviceAppRestrictionRepository appRestrictionRepository;
    private final DevicePolicyRepository policyRepository;
    private final DeviceCommandRepository commandRepository;
    private final AccurateDeviceLocationRepository accurateLocationRepository;
    private final DeviceActivityRepository activityRepository;
    private final MdmAlertRepository alertRepository;
    private final EmployeeRepository employeeRepository;

    private static final long DEVICE_OFFLINE_HEARTBEAT_WINDOW_MINUTES = 20L;

    // ════════════════════════════════════════
    // ENROLLMENT TOKEN MANAGEMENT
    // ════════════════════════════════════════

    public EnrollmentToken generateEnrollmentToken(String token, String employeeId, String employeeName) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusHours(1);

        EnrollmentToken enrollmentToken = EnrollmentToken.builder()
                .token(token)
                .employeeId(employeeId)
                .employeeName(employeeName)
                .createdAt(now)
                .expiresAt(expiresAt)
                .status("PENDING")
                .build();

        log.info("Generated enrollment token for employee: {} ({})", employeeName, employeeId);
        upsertEmployee(employeeId, employeeName, null, null);
        return enrollmentTokenRepository.save(enrollmentToken);
    }

    @Transactional
    public boolean revokeEnrollmentToken(Long tokenId) {
        Optional<EnrollmentToken> tokenOptional = enrollmentTokenRepository.findById(tokenId);
        if (tokenOptional.isEmpty()) {
            log.warn("Enrollment token not found for revoke: {}", tokenId);
            return false;
        }

        EnrollmentToken token = tokenOptional.get();
        token.setStatus("REVOKED");
        enrollmentTokenRepository.save(token);
        log.info("Revoked enrollment token id={} token={}", tokenId, token.getToken());
        return true;
    }

    public Optional<EnrollmentToken> getEnrollmentToken(String token) {
        return enrollmentTokenRepository.findByTokenAndStatus(token, "PENDING");
    }

    public void markTokenAsUsed(String token, String deviceId) {
        Optional<EnrollmentToken> enrollmentToken = enrollmentTokenRepository.findByToken(token);
        if (enrollmentToken.isPresent()) {
            EnrollmentToken et = enrollmentToken.get();
            et.setStatus("USED");
            et.setDeviceId(deviceId);
            et.setUsedAt(LocalDateTime.now());
            enrollmentTokenRepository.save(et);
            upsertEmployee(et.getEmployeeId(), et.getEmployeeName(), deviceId, null);
            log.info("Marked token {} as USED for device: {}", token, deviceId);
        }
    }

    // ════════════════════════════════════════
    // DEVICE ENROLLMENT
    // ════════════════════════════════════════

    public Device enrollDevice(EnrollRequest request) {
        // If enrollment token is provided, get employee info from token
        String employeeName = null;
        String employeeId = null;

        if (request.getEnrollmentToken() != null && !request.getEnrollmentToken().isBlank()) {
            EnrollmentToken token = enrollmentTokenRepository.findByToken(request.getEnrollmentToken())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token not found"));

            if (LocalDateTime.now().isAfter(token.getExpiresAt())) {
                token.setStatus("EXPIRED");
                enrollmentTokenRepository.save(token);
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Enrollment token has expired. Please generate a new token.");
            }

            if (!"PENDING".equals(token.getStatus())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Token already used or revoked: " + token.getStatus());
            }

            employeeName = token.getEmployeeName();
            employeeId = token.getEmployeeId();
            markTokenAsUsed(request.getEnrollmentToken(), request.getDeviceId());
        }

        if ((employeeName == null || employeeName.isBlank()) || (employeeId == null || employeeId.isBlank())) {
            Optional<Employee> employee = employeeRepository.findByDeviceId(request.getDeviceId());
            if (employee.isPresent()) {
                Employee existingEmployee = employee.get();
                employeeName = existingEmployee.getEmployeeName();
                employeeId = existingEmployee.getEmployeeId();
            }
        }

        if (deviceRepository.existsByDeviceId(request.getDeviceId())) {
            log.info("Device {} already enrolled, updating...", request.getDeviceId());
            Device existing = deviceRepository.findByDeviceId(request.getDeviceId()).get();
            existing.setStatus("ACTIVE");
            if (employeeName != null) {
                existing.setEmployeeName(employeeName);
                existing.setEmployeeId(employeeId);
            }
            return deviceRepository.save(existing);
        }

        Device device = Device.builder()
                .deviceId(request.getDeviceId())
                .enrollmentToken(request.getEnrollmentToken())
                .enrollmentMethod(request.getEnrollmentMethod())
                .employeeName(employeeName)
                .employeeId(employeeId)
                .enrolledAt(LocalDateTime.now())
                .status("ACTIVE")
                .build();

        log.info("Enrolling new device: {} for employee: {}", request.getDeviceId(), employeeName);
        Device saved = deviceRepository.save(device);

        // Log enrollment activity and alert
        String eName = employeeName != null ? employeeName : "Unknown";
        String model = device.getDeviceModel() != null ? device.getDeviceModel()
                : request.getDeviceId().substring(0, 8);
        activityRepository.save(DeviceActivity.builder()
                .deviceId(request.getDeviceId()).employeeName(eName)
                .activityType("ENROLLED").description(eName + " enrolled " + model)
                .severity("INFO").createdAt(LocalDateTime.now()).build());
        alertRepository.save(MdmAlert.builder()
                .deviceId(request.getDeviceId()).employeeName(eName)
                .alertType("DEVICE_ENROLLED").message(eName + " enrolled a new device")
                .isRead(false).severity("INFO").createdAt(LocalDateTime.now()).build());

        return saved;
    }

    public List<Device> getAllDevices() {
        reconcileDeviceStatusesByHeartbeat();
        return deviceRepository.findAll();
    }

    public List<DeviceResponse> getAllDevicesAsResponse() {
        reconcileDeviceStatusesByHeartbeat();
        return deviceRepository.findAll().stream()
                .map(this::convertToDeviceResponse)
                .collect(Collectors.toList());
    }

    public Optional<Device> getDevice(String deviceId) {
        reconcileDeviceStatusesByHeartbeat();
        return deviceRepository.findByDeviceId(deviceId);
    }

    public Optional<DeviceResponse> getDeviceAsResponse(String deviceId) {
        reconcileDeviceStatusesByHeartbeat();
        return deviceRepository.findByDeviceId(deviceId)
                .map(this::convertToDeviceResponse);
    }

    private DeviceResponse convertToDeviceResponse(Device device) {
        String employeeId = device.getEmployeeId();
        String employeeName = device.getEmployeeName();
        String model = device.getDeviceModel();
        String manufacturer = device.getManufacturer();
        String osVersion = null;
        String sdkVersion = null;
        String serialNumber = null;
        Boolean deviceOwnerSet = null;

        // Enrich from latest device_info if fields are missing
        List<DeviceInfo> infoList = deviceInfoRepository.findByDeviceIdOrderByCollectedAtDesc(device.getDeviceId());
        if (!infoList.isEmpty()) {
            DeviceInfo latest = infoList.get(0);
            if (model == null || model.isBlank())
                model = latest.getModel();
            if (manufacturer == null || manufacturer.isBlank())
                manufacturer = latest.getManufacturer();
            osVersion = latest.getOsVersion();
            sdkVersion = latest.getSdkVersion();
            serialNumber = latest.getSerialNumber();
            deviceOwnerSet = latest.getDeviceOwnerSet();

            if (!Boolean.TRUE.equals(deviceOwnerSet)) {
                boolean hasAnyDeviceOwnerSet = infoList.stream()
                        .anyMatch(info -> Boolean.TRUE.equals(info.getDeviceOwnerSet()));
                if (hasAnyDeviceOwnerSet) {
                    deviceOwnerSet = true;
                }
            }
        }

        if ((employeeName == null || employeeName.isBlank()) || (employeeId == null || employeeId.isBlank())) {
            Optional<Employee> employee = employeeRepository.findByDeviceId(device.getDeviceId());
            if (employee.isPresent()) {
                Employee mappedEmployee = employee.get();
                employeeId = mappedEmployee.getEmployeeId();
                employeeName = mappedEmployee.getEmployeeName();
            }
        }

        return DeviceResponse.builder()
                .id(device.getId())
                .deviceId(device.getDeviceId())
                .employeeId(employeeId)
                .employeeName(employeeName)
                .enrollmentMethod(device.getEnrollmentMethod())
                .deviceModel(model)
                .manufacturer(manufacturer)
                .osVersion(osVersion)
                .sdkVersion(sdkVersion)
                .serialNumber(serialNumber)
                .deviceOwnerSet(deviceOwnerSet)
                .enrolledAt(device.getEnrolledAt())
                .status(device.getStatus())
                .build();
    }

    public long countDevices() {
        return deviceRepository.count();
    }

    public long countByStatus(String status) {
        if ("ACTIVE".equalsIgnoreCase(status) || "OFFLINE".equalsIgnoreCase(status)) {
            reconcileDeviceStatusesByHeartbeat();
        }
        return deviceRepository.countByStatus(status);
    }

    // ════════════════════════════════════════
    // DEVICE DELETION
    // ════════════════════════════════════════

    @Transactional
    public boolean deleteDeviceWithAllData(String deviceId) {
        Optional<Device> device = deviceRepository.findByDeviceId(deviceId);
        if (device.isEmpty()) {
            log.warn("Device not found for deletion: {}", deviceId);
            return false;
        }

        // 1) App inventory
        appInventoryRepository.deleteByDeviceId(deviceId);

        // 2) Device info
        deviceInfoRepository.deleteByDeviceId(deviceId);

        // 3) Admin notifications
        adminNotificationRepository.deleteByDeviceId(deviceId);

        // 4) Old restricted apps table
        restrictedAppRepository.deleteByDeviceId(deviceId);

        // 5) New app restrictions table
        appRestrictionRepository.deleteByDeviceId(deviceId);

        // 6) Policies
        policyRepository.deleteByDeviceId(deviceId);

        // 7) Location
        accurateLocationRepository.deleteByDeviceId(deviceId);

        // 8) Commands
        commandRepository.deleteByDeviceId(deviceId);

        // 9) Activities
        activityRepository.deleteByDeviceId(deviceId);

        // 10) Alerts
        alertRepository.deleteByDeviceId(deviceId);

        // 11) Revoke enrollment tokens
        List<EnrollmentToken> tokens = enrollmentTokenRepository.findByDeviceId(deviceId);
        for (EnrollmentToken token : tokens) {
            token.setStatus("REVOKED");
        }
        if (!tokens.isEmpty()) {
            enrollmentTokenRepository.saveAll(tokens);
        }

        // 12) Delete device
        deviceRepository.delete(device.get());

        // 13) Clear employee link
        employeeRepository.findByDeviceId(deviceId).ifPresent(employee -> {
            employee.setDeviceId(null);
            employee.setDeviceName(null);
            employee.setUpdatedAt(LocalDateTime.now());
            employeeRepository.save(employee);
        });

        log.info("Deleted device and all related data for deviceId={}", deviceId);
        return true;
    }

    public boolean checkDeviceExists(String deviceId) {
        return deviceRepository.existsByDeviceIdAndStatus(deviceId, "ACTIVE");
    }

    public List<EnrollmentToken> getActiveEnrollmentTokens() {
        return enrollmentTokenRepository.findByStatusAndExpiresAtAfterOrderByCreatedAtDesc(
                "PENDING",
                LocalDateTime.now());
    }

    @Transactional
    public Optional<Device> recordHeartbeat(String deviceId) {
        return deviceRepository.findByDeviceId(deviceId).map(device -> {
            LocalDateTime now = LocalDateTime.now();
            device.setLastSeenAt(now);
            if ("OFFLINE".equalsIgnoreCase(device.getStatus())) {
                device.setStatus("ACTIVE");
            }
            return deviceRepository.save(device);
        });
    }

    @Scheduled(fixedDelay = 300_000)
    @Transactional
    public void expireOldTokens() {
        List<EnrollmentToken> pendingTokens = enrollmentTokenRepository.findByStatus("PENDING");
        LocalDateTime now = LocalDateTime.now();
        pendingTokens.forEach(token -> {
            if (now.isAfter(token.getExpiresAt())) {
                token.setStatus("EXPIRED");
                enrollmentTokenRepository.save(token);
            }
        });
    }

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void syncDeviceStatusFromHeartbeat() {
        reconcileDeviceStatusesByHeartbeat();
    }

    @Transactional
    private void reconcileDeviceStatusesByHeartbeat() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(DEVICE_OFFLINE_HEARTBEAT_WINDOW_MINUTES);

        deviceRepository.findAll().forEach(device -> {
            LocalDateTime lastSeen = device.getLastSeenAt();
            if (lastSeen == null) {
                lastSeen = deviceInfoRepository
                        .findFirstByDeviceIdOrderByCollectedAtDesc(device.getDeviceId())
                        .map(DeviceInfo::getCollectedAt)
                        .orElse(null);
                if (lastSeen != null) {
                    device.setLastSeenAt(lastSeen);
                }
            }

            boolean isRecentlySeen = lastSeen != null && !lastSeen.isBefore(cutoff);
            String nextStatus = isRecentlySeen ? "ACTIVE" : "OFFLINE";

            if (!nextStatus.equalsIgnoreCase(device.getStatus())) {
                device.setStatus(nextStatus);
                deviceRepository.save(device);
            }
        });
    }

    private void upsertEmployee(String employeeId, String employeeName, String deviceId, String deviceName) {
        if (employeeId == null || employeeId.isBlank() || employeeName == null || employeeName.isBlank()) {
            return;
        }

        Employee employee = employeeRepository.findByEmployeeId(employeeId)
                .orElse(Employee.builder().employeeId(employeeId).build());

        employee.setEmployeeName(employeeName);
        if (deviceId != null) {
            employee.setDeviceId(deviceId);
        }
        if (deviceName != null && !deviceName.isBlank()) {
            employee.setDeviceName(deviceName);
        }
        employee.setUpdatedAt(LocalDateTime.now());
        employeeRepository.save(employee);
    }
}