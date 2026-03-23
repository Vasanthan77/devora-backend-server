package com.mdm.mdm_backend.repository;

import com.mdm.mdm_backend.model.entity.EnrollmentToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EnrollmentTokenRepository extends JpaRepository<EnrollmentToken, Long> {

    Optional<EnrollmentToken> findByToken(String token);

    List<EnrollmentToken> findByEmployeeId(String employeeId);

    List<EnrollmentToken> findByStatus(String status);

    List<EnrollmentToken> findByExpiresAtBefore(LocalDateTime dateTime);

    Optional<EnrollmentToken> findByTokenAndStatus(String token, String status);

    List<EnrollmentToken> findByDeviceId(String deviceId);

    @Modifying
    @Transactional
    @Query("UPDATE EnrollmentToken et SET et.status = 'REVOKED' WHERE et.deviceId = :deviceId")
    int revokeByDeviceId(@Param("deviceId") String deviceId);

    List<EnrollmentToken> findByStatusAndExpiresAtAfterOrderByCreatedAtDesc(String status, LocalDateTime now);
}
