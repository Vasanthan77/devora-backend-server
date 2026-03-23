package com.mdm.mdm_backend.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URI;

/**
 * Serves the MDM DPC APK for Device Owner provisioning.
 *
 * During factory-reset QR provisioning, Android's setup wizard downloads the
 * DPC APK from the URL specified in the provisioning payload. This controller
 * serves that APK from the classpath (static/downloads/).
 */
@RestController
@RequestMapping("/downloads")
public class ApkDownloadController {

    private static final String APK_PATH = "static/downloads/devora-mdm-latest.apk";

    @Value("${app.apk.external-url:}")
    private String externalApkUrl;

    @GetMapping(value = "/devora-mdm-latest.apk", produces = "application/vnd.android.package-archive")
    public ResponseEntity<Resource> downloadApk() throws IOException {
        if (externalApkUrl != null && !externalApkUrl.isBlank()) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(externalApkUrl.trim()))
                    .build();
        }

        Resource apk = new ClassPathResource(APK_PATH);

        if (!apk.exists()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"devora-mdm-latest.apk\"")
                .contentType(MediaType.parseMediaType("application/vnd.android.package-archive"))
                .contentLength(apk.contentLength())
                .body(apk);
    }
}
