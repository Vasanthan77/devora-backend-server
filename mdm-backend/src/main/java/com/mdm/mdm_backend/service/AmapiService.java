package com.mdm.mdm_backend.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

@Service
public class AmapiService {

    @Value("classpath:service-account.json")
    private Resource serviceAccountResource;

    @Value("${GOOGLE_SERVICE_ACCOUNT_JSON:}")
    private String serviceAccountJson;

    @Value("${GOOGLE_SERVICE_ACCOUNT_JSON_BASE64:}")
    private String serviceAccountJsonBase64;

    @Value("${GOOGLE_APPLICATION_CREDENTIALS:}")
    private String googleApplicationCredentialsPath;

    private static final String AMAPI_SCOPES = "https://www.googleapis.com/auth/androidmanagement";

    private final RestTemplate restTemplate = new RestTemplate();

    private InputStream getServiceAccountInputStream() throws Exception {
        if (serviceAccountJson != null && !serviceAccountJson.isBlank()) {
            return new ByteArrayInputStream(serviceAccountJson.getBytes(StandardCharsets.UTF_8));
        }

        if (serviceAccountJsonBase64 != null && !serviceAccountJsonBase64.isBlank()) {
            byte[] decoded = Base64.getDecoder().decode(serviceAccountJsonBase64);
            return new ByteArrayInputStream(decoded);
        }

        if (googleApplicationCredentialsPath != null && !googleApplicationCredentialsPath.isBlank()) {
            Path keyPath = Paths.get(googleApplicationCredentialsPath.trim());
            if (Files.exists(keyPath)) {
                return Files.newInputStream(keyPath);
            }
            throw new RuntimeException("GOOGLE_APPLICATION_CREDENTIALS file not found: " + keyPath);
        }

        return serviceAccountResource.getInputStream();
    }

    private String getAccessToken() throws Exception {
        try (InputStream in = getServiceAccountInputStream()) {
            GoogleCredentials credentials = GoogleCredentials.fromStream(in)
                    .createScoped(Collections.singleton(AMAPI_SCOPES));
            credentials.refreshIfExpired();
            return credentials.getAccessToken().getTokenValue();
        }
    }

    private String getProjectId() throws Exception {
        try (InputStream in = getServiceAccountInputStream()) {
            ServiceAccountCredentials credentials = (ServiceAccountCredentials) GoogleCredentials.fromStream(in);
            return credentials.getProjectId();
        }
    }

    public String createEnterprise() throws Exception {
        String accessToken = getAccessToken();
        String projectId = getProjectId();

        if (projectId == null || projectId.isEmpty()) {
            throw new RuntimeException("Could not find project_id in service-account.json");
        }

        String url = "https://androidmanagement.googleapis.com/v1/enterprises?projectId=" + projectId
                + "&agreementAccepted=true";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

                // Use a minimal payload to create a managed Google Play enterprise.
                String requestBody = """
                                {
                                    "enterpriseDisplayName": "Devora MDM"
                                }
                                """;

        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody();
        } else {
            throw new RuntimeException("Failed to create enterprise: " + response.getBody());
        }
    }

    public String updateEnterprisePubsubTopic(String enterpriseName, String projectId, String topicName)
            throws Exception {
        return updateEnterprisePubsubTopic(enterpriseName, projectId, topicName, null);
        }

        public String updateEnterprisePubsubTopic(String enterpriseName, String projectId, String topicName,
            List<String> enabledNotificationTypes)
            throws Exception {
        String accessToken = getAccessToken();
        String url = "https://androidmanagement.googleapis.com/v1/" + enterpriseName;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-HTTP-Method-Override", "PATCH");

        String fullTopicName = "projects/" + projectId + "/topics/" + topicName;
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode rootNode = mapper.createObjectNode();
        rootNode.put("pubsubTopic", fullTopicName);

        if (enabledNotificationTypes != null && !enabledNotificationTypes.isEmpty()) {
            ArrayNode notifications = rootNode.putArray("enabledNotificationTypes");
            for (String type : enabledNotificationTypes) {
                if (type != null && !type.isBlank()) {
                    notifications.add(type.trim());
                }
            }
        }

        String requestBody = mapper.writeValueAsString(rootNode);

        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody();
        } else {
            throw new RuntimeException("Failed to update enterprise: " + response.getBody());
        }
    }

    public String getEnterprise(String enterpriseName) throws Exception {
        String accessToken = getAccessToken();
        String url = "https://androidmanagement.googleapis.com/v1/" + enterpriseName;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody();
        } else {
            throw new RuntimeException("Failed to get enterprise: " + response.getBody());
        }
    }

    public String generateEnterpriseUpgradeUrl(String enterpriseName, List<String> allowedDomains, String adminEmail)
            throws Exception {
        String accessToken = getAccessToken();
        String url = "https://androidmanagement.googleapis.com/v1/" + enterpriseName + ":generateEnterpriseUpgradeUrl";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode body = mapper.createObjectNode();

        if (allowedDomains != null && !allowedDomains.isEmpty()) {
            ArrayNode domains = body.putArray("allowedDomains");
            for (String domain : allowedDomains) {
                if (domain != null && !domain.isBlank()) {
                    domains.add(domain.trim());
                }
            }
        }

        if (adminEmail != null && !adminEmail.isBlank()) {
            body.put("adminEmail", adminEmail.trim());
        }

        String requestBody = mapper.writeValueAsString(body);
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody();
        } else {
            throw new RuntimeException("Failed to generate enterprise upgrade URL: " + response.getBody());
        }
    }

    public String createPolicy(String enterpriseName, String policyId) throws Exception {
        String accessToken = getAccessToken();
        String url = "https://androidmanagement.googleapis.com/v1/" + enterpriseName + "/policies/" + policyId;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        // AMAPI policy updates are PATCH operations; using POST + override avoids HTTP client limitations.
        headers.set("X-HTTP-Method-Override", "PATCH");

        String requestBody = """
                {
                    "passwordRequirements": {
                        "passwordQuality": "NUMERIC",
                        "passwordMinimumLength": 6
                    },
                    "cameraDisabled": false,
                    "screenCaptureDisabled": true,
                    "factoryResetDisabled": true,
                    "playStoreMode": "BLACKLIST",
                    "applications": [
                        {
                            "packageName": "com.android.chrome",
                            "installType": "FORCE_INSTALLED",
                            "defaultPermissionPolicy": "GRANT"
                        },
                        {
                            "packageName": "com.devora.devicemanager",
                            "installType": "FORCE_INSTALLED",
                            "defaultPermissionPolicy": "GRANT"
                        }
                    ]
                }
                """;

        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody();
        } else {
            throw new RuntimeException("Failed to create policy: " + response.getBody());
        }
    }

    public String patchDevicePolicy(String enterpriseName, String policyId,
            com.mdm.mdm_backend.model.entity.DevicePolicy dbPolicy,
            java.util.List<com.mdm.mdm_backend.model.entity.DeviceAppRestriction> apps) throws Exception {
        String accessToken = getAccessToken();
        String url = "https://androidmanagement.googleapis.com/v1/" + enterpriseName + "/policies/" + policyId;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-HTTP-Method-Override", "PATCH");

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode rootNode = mapper.createObjectNode();

        boolean cameraDisabled = (dbPolicy != null) && dbPolicy.isCameraDisabled();
        boolean screenLockRequired = (dbPolicy != null) && dbPolicy.isScreenLockRequired();
        boolean installBlocked = (dbPolicy != null) && dbPolicy.isInstallBlocked();

        rootNode.put("cameraDisabled", cameraDisabled);
        rootNode.put("screenCaptureDisabled", true);
        rootNode.put("factoryResetDisabled", true);

        // Map password requirements
        ObjectNode passwordNode = rootNode.putObject("passwordRequirements");
        passwordNode.put("passwordQuality", "NUMERIC");
        if (screenLockRequired) {
            passwordNode.put("passwordMinimumLength", 6);
        } else {
            passwordNode.put("passwordMinimumLength", 4);
        }

        // Map Play Store settings
        if (installBlocked) {
            rootNode.put("playStoreMode", "WHITELIST");
        } else {
            rootNode.put("playStoreMode", "BLACKLIST");
        }

        // Map applications
        ArrayNode appsArray = rootNode.putArray("applications");

        // Ensure Chrome and Devora are installed
        ObjectNode chromeApp = appsArray.addObject();
        chromeApp.put("packageName", "com.android.chrome");
        chromeApp.put("installType", "FORCE_INSTALLED");
        chromeApp.put("defaultPermissionPolicy", "GRANT");

        ObjectNode devoraApp = appsArray.addObject();
        devoraApp.put("packageName", "com.devora.devicemanager");
        devoraApp.put("installType", "FORCE_INSTALLED");
        devoraApp.put("defaultPermissionPolicy", "GRANT");

        if (apps != null) {
            for (var app : apps) {
                if (app.getPackageName() != null && !app.getPackageName().equals("com.android.chrome")) {
                    ObjectNode appNode = appsArray.addObject();
                    appNode.put("packageName", app.getPackageName());
                    if (app.isRestricted()) {
                        appNode.put("installType", "BLOCKED");
                    } else {
                        appNode.put("installType", "FORCE_INSTALLED");
                        appNode.put("defaultPermissionPolicy", "GRANT");
                    }
                }
            }
        }

        String requestBody = mapper.writeValueAsString(rootNode);

        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody();
        } else {
            throw new RuntimeException("Failed to patch policy: " + response.getBody());
        }
    }

    public String createEnrollmentToken(String enterpriseName, String policyId) throws Exception {
        return createEnrollmentToken(enterpriseName, policyId, "QR_CODE");
    }

    public String createEnrollmentToken(String enterpriseName, String policyId, String type) throws Exception {
        String accessToken = getAccessToken();
        String url = "https://androidmanagement.googleapis.com/v1/" + enterpriseName + "/enrollmentTokens";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        String policyName = enterpriseName + "/policies/" + policyId;

        String requestBody = String.format("""
                {
                  "policyName": "%s",
                  "duration": "86400s",
                                    "allowPersonalUsage": "PERSONAL_USAGE_DISALLOWED",
                  "oneTimeOnly": false
                }
                """, policyName);

        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody();
        } else {
            throw new RuntimeException("Failed to create token: " + response.getBody());
        }
    }

    // -------------------------------------------------------------
    // AMAPI Device Operations (Step 6 replacements)
    // -------------------------------------------------------------

    public String issueCommand(String enterpriseName, String deviceId, String commandType) throws Exception {
        String accessToken = getAccessToken();
        // deviceId in AMAPI usually includes the enterprise, e.g.
        // "enterprises/LC01oh6rj0/devices/12345"
        // But if frontend only sends "12345", we construct the full name.
        String deviceName = deviceId.startsWith("enterprises/") ? deviceId : enterpriseName + "/devices/" + deviceId;
        String url = "https://androidmanagement.googleapis.com/v1/" + deviceName + ":issueCommand";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        String requestBody = String.format("""
                {
                  "type": "%s"
                }
                """, commandType);

        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody();
        } else {
            throw new RuntimeException("Failed to issue " + commandType + " command: " + response.getBody());
        }
    }

    public String getDevice(String enterpriseName, String deviceId) throws Exception {
        String accessToken = getAccessToken();
        String deviceName = deviceId.startsWith("enterprises/") ? deviceId : enterpriseName + "/devices/" + deviceId;
        String url = "https://androidmanagement.googleapis.com/v1/" + deviceName;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody();
        } else {
            throw new RuntimeException("Failed to get device info: " + response.getBody());
        }
    }

    public String listDevices(String enterpriseName) throws Exception {
        String accessToken = getAccessToken();
        String url = "https://androidmanagement.googleapis.com/v1/" + enterpriseName + "/devices";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody();
        } else {
            throw new RuntimeException("Failed to list devices: " + response.getBody());
        }
    }

    public String listEnterprises() throws Exception {
        String accessToken = getAccessToken();
        String projectId = getProjectId();
        if (projectId == null || projectId.isEmpty()) {
            throw new RuntimeException("Could not find project_id to list enterprises");
        }
        String url = "https://androidmanagement.googleapis.com/v1/enterprises?projectId=" + projectId;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody();
        } else {
            throw new RuntimeException("Failed to list enterprises: " + response.getBody());
        }
    }

    public void deleteEnterprise(String enterpriseName) throws Exception {
        String accessToken = getAccessToken();
        String url = "https://androidmanagement.googleapis.com/v1/" + enterpriseName;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        restTemplate.exchange(url, HttpMethod.DELETE, entity, Void.class);
    }

    public void deleteDevice(String enterpriseName, String deviceId) throws Exception {
        String accessToken = getAccessToken();
        // deviceId in AMAPI usually includes the enterprise, e.g. "enterprises/LC01oh6rj0/devices/12345"
        String deviceName = deviceId.startsWith("enterprises/") ? deviceId : enterpriseName + "/devices/" + deviceId;
        String url = "https://androidmanagement.googleapis.com/v1/" + deviceName;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        // Use DELETE method directly to remove from Google AMAPI records and free up quota
        restTemplate.exchange(url, HttpMethod.DELETE, entity, Void.class);
    }
}
