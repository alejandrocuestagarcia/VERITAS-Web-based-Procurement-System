package com.veritas.backend.auth.entity;

import com.veritas.backend.user.entity.User;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
@Data
public class RefreshToken {
    @Id
    private UUID tokenId;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    private LocalDateTime expiresAt;
    private LocalDateTime createdAt = LocalDateTime.now();
}