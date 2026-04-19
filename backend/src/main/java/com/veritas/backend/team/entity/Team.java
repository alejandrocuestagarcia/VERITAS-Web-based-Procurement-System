package com.veritas.backend.team.entity;

import com.veritas.backend.common.model.Department;
import com.veritas.backend.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "teams")
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Team {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long teamId;

    private String name;
    private String description;

    @OneToOne
    @JoinColumn(name = "leader_id")
    private User leader;

    private Boolean isActive = true;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    private Department department;
}