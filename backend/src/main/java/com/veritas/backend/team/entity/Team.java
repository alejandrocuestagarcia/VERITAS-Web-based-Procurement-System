package com.veritas.backend.team.entity;

import com.veritas.backend.common.model.Department;
import com.veritas.backend.user.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

    @NotBlank(message = "Team name is required")
    @Size(max = 120, message = "Team name must be at most 120 characters")
    private String name;

    @NotBlank(message = "Team description is required")
    @Size(max = 500, message = "Team description must be at most 500 characters")
    private String description;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leader_id")
    private User leader;

    @Builder.Default
    @NotNull(message = "Active state is required")
    private Boolean isActive = true;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    private Department department;
 
    /* 
    Safer pattern than inline initialization. It ensures that if a creation date or an active status
    are manually set before saving, the set values won't be accidentally overwritten by the Java 
    object's default constructor logic.
    */
    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }

        if (this.isActive == null) {
            this.isActive = true;
        }
    }
}