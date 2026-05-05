package com.veritas.backend.auth.repository;

import com.veritas.backend.auth.entity.PasswordResetToken;
import com.veritas.backend.user.entity.User;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByTokenHashAndUsedAtIsNull(String tokenHash);

    @Modifying
    void deleteByUser(User user);

    @Modifying
    void deleteByUserAndIdNot(User user, Long id);

    @Modifying
    void deleteByExpiresAtBefore(Instant instant);
}
