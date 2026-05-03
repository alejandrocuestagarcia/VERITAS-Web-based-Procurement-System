package com.veritas.backend.workflow.service;

import com.veritas.backend.workflow.dto.WorkflowDto;
import com.veritas.backend.workflow.dto.WorkflowEditDto;
import com.veritas.backend.workflow.dto.WorkflowSaveDto;

public interface WorkflowService {

    public WorkflowDto createWorkflow(WorkflowSaveDto workflowSaveDto);

    public WorkflowDto getWorkflow(Long id);

    public WorkflowDto editWorkflow(Long id, WorkflowEditDto workflowEditDto);
}
