package com.veritas.backend.audit.entity;

import com.veritas.backend.requisition.entity.Request;
import com.veritas.backend.user.entity.User;
import com.veritas.backend.workflow.entity.WorkflowStep;
import com.veritas.backend.workflow.entity.WorkflowTransition;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id")
    private Request request;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;

    @Column(nullable = false)
    private String action;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "previous_step_id")
    private WorkflowStep previousStep;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "new_step_id")
    private WorkflowStep newStep;

    @Column(name = "entry_hash", nullable = false)
    private String entryHash;

    @Column(name = "previous_hash")
    private String previousHash;

    @Column(name = "timestamp")
    private LocalDateTime timestamp = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transition_id")
    private WorkflowTransition transition;


}