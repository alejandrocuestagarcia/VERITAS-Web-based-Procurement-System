package com.veritas.backend.requisition.entity;

import com.veritas.backend.common.model.Department;
import com.veritas.backend.user.entity.User;
import com.veritas.backend.team.entity.Team;
import com.veritas.backend.project.entity.Project;
import com.veritas.backend.budget.entity.InternalBudget;
import com.veritas.backend.workflow.entity.WorkflowDefinition;
import com.veritas.backend.workflow.entity.WorkflowStep;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "requests")
@Data
public class Request {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_id")
    private Long requestID;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User userID;

    @Column(name = "request_key")
    private String requestKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "budget_id")
    private InternalBudget budgetID;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team teamID;

    @Column(name = "request_name", nullable = false)
    private String requestName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_definition_id")
    private WorkflowDefinition workflowDefinitionID;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_step_id")
    private WorkflowStep currentStepID;

    @Column(name = "total_quantity")
    private Integer totalQuantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project projectID;

    @Enumerated(EnumType.STRING)
    private Department department;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Enumerated(EnumType.STRING)
    private Priority priority;

    @Column(name = "cost_center")
    private String costCenter;

    @Column(name = "jira_issue_key", unique = true)
    private String jiraIssueKey;

    @Column(name = "jira_issue_url")
    private String jiraIssueUrl;

    @Column(name = "jira_status")
    private String jiraStatus;

    @Column(columnDefinition = "TEXT")
    private String description;
}