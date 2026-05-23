package com.veritas.backend.requisition.entity;

import com.veritas.backend.user.entity.User;
import com.veritas.backend.team.entity.Team;
import com.veritas.backend.project.entity.Project;
import com.veritas.backend.budget.entity.InternalBudget;
import com.veritas.backend.vendor.entity.Quote;
import com.veritas.backend.vendor.entity.VendorEvaluation;
import com.veritas.backend.workflow.entity.WorkflowDefinition;
import com.veritas.backend.workflow.entity.WorkflowStep;
import com.veritas.backend.requisition.entity.RequestStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "requests")
@Data
public class Request {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_id")
    private Long requestID;

    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RequestItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Attachment> attachments = new ArrayList<>();

    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Quote> quotes = new ArrayList<>();

    @OneToOne(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private VendorEvaluation vendorEvaluation;

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

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Enumerated(EnumType.STRING)
    private Priority priority;

    @Column(name = "jira_issue_key", unique = true)
    private String jiraIssueKey;

    @Column(name = "jira_issue_url")
    private String jiraIssueUrl;

    @Column(name = "jira_status")
    private String jiraStatus;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    private RequestStatus state = RequestStatus.DRAFT;

    @Column(name = "rejection_reason",columnDefinition = "TEXT")
    private String rejectionReason;
}