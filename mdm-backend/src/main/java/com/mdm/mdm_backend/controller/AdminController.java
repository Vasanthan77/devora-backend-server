package com.mdm.mdm_backend.controller;

import com.mdm.mdm_backend.model.dto.AdminLoginRequest;
import com.mdm.mdm_backend.model.dto.AdminLoginResponse;
import com.mdm.mdm_backend.model.dto.AdminRegisterRequest;
import com.mdm.mdm_backend.model.entity.Admin;
import com.mdm.mdm_backend.repository.AdminRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;  // Inject from Spring

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody AdminRegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        if (adminRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("success", false, "message", "Email already registered"));
        }

        Admin admin = Admin.builder()
            .name(request.getName().trim())
            .email(normalizedEmail)
                .password(passwordEncoder.encode(request.getPassword()))
                .createdAt(LocalDateTime.now())
                .build();

        adminRepository.save(admin);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("success", true, "message", "Admin registered successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<AdminLoginResponse> login(@Valid @RequestBody AdminLoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        String rawPassword = request.getPassword();
        log.info("Login attempt for email: {}", normalizedEmail);
        return adminRepository.findByEmailIgnoreCase(normalizedEmail)
                .map(admin -> {
                    String storedPassword = admin.getPassword() == null ? "" : admin.getPassword();
                    boolean passwordMatches = passwordEncoder.matches(rawPassword, storedPassword);

                    // Backward compatibility: support legacy rows that accidentally stored
                    // plain passwords before encoder enforcement, then migrate to hash.
                    if (!passwordMatches && storedPassword.equals(rawPassword)) {
                        admin.setPassword(passwordEncoder.encode(rawPassword));
                        adminRepository.save(admin);
                        passwordMatches = true;
                        log.info("Migrated legacy plaintext password to encoded hash for {}", normalizedEmail);
                    }

                    if (passwordMatches) {
                        log.info("Login successful for: {}", normalizedEmail);
                        return ResponseEntity.ok(
                                new AdminLoginResponse(true, admin.getName(), "Login successful"));
                    } else {
                        log.warn("Password mismatch for: {}", normalizedEmail);
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(new AdminLoginResponse(false, null, "Invalid email or password"));
                    }
                })
                .orElseGet(() -> {
                    log.warn("No admin found for email: {}", normalizedEmail);
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(new AdminLoginResponse(false, null, "Invalid email or password"));
                });
    }
}