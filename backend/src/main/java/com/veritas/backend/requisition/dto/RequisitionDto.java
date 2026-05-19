package com.veritas.backend.requisition.dto;

import com.veritas.backend.requisition.entity.Priority;
import java.time.LocalDateTime;
import java.util.List;

public record RequisitionDto(
    Long id,
    String requestName,
    String requestKey,
    String status,
    Boolean isClosed,
    Priority priority,
    String projectName,
    String projectKey,
    String workflowName,
    String teamName,
    String requesterName,
    String responsibleRole,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    String description,
    String jiraIssueKey,
    String jiraIssueUrl,
    Long vendorId,
    String vendorName,
    Boolean isEvaluated,
    List<RequisitionItemDto> items,
    List<AttachmentDto> attachments,
    String state
) {}
