package com.sms.studentmanagement.repository;

import com.sms.studentmanagement.entity.RefreshToken;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update RefreshToken r set r.revokedAt = :now where r.user.id = :userId and r.revokedAt is null")
    int revokeAllActive(@Param("userId") Long userId, @Param("now") Instant now);
}