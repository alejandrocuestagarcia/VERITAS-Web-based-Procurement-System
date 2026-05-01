package com.veritas.backend.requisition.dto;

import com.veritas.backend.requisition.entity.Priority;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RequisitionDto {
    private Long id;
    private String requestName;
    private String requestKey;
    private String status;
    private Priority priority;
    private String projectName;
    private String teamName;
    private String requesterName;
    private LocalDateTime createdAt;
}
