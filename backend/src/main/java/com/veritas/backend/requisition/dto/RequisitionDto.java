package com.veritas.backend.requisition.dto;

import com.veritas.backend.requisition.entity.Priority;
import java.time.LocalDateTime;

public record RequisitionDto(
    Long id,
    String requestName,
    String requestKey,
    String status,
    Priority priority,
    String projectName,
    String teamName,
    String requesterName,
    LocalDateTime createdAt
) {}
