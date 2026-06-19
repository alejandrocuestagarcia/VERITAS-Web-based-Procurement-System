package com.veritas.backend.workflow.entity;

import com.veritas.backend.user.entity.UserRole;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "workflow_steps")
@Data
public class WorkflowStep {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_definition_id")
    private WorkflowDefinition workflowDefinition;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkflowComponent workflowComponent;

    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_role")
    private UserRole role;

    @Column(name = "is_team_leader", nullable = false)
    private boolean isTeamLeader = false;

    @Column(name = "is_automated_approval", nullable = false)
    private boolean isAutomatedApproval = false;

    public boolean getIsTeamLeader() {
        return this.isTeamLeader;
    }

    public boolean getIsAutomatedApproval() {
        return this.isAutomatedApproval;
    }

    public void setIsTeamLeader(boolean isTeamLeader) {
        this.isTeamLeader = isTeamLeader;
    }

    public void setIsAutomatedApproval(boolean isAutomatedApproval) {
        this.isAutomatedApproval = isAutomatedApproval;
    }
}