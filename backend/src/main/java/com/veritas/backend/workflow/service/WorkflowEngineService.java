package com.veritas.backend.workflow.service;

import com.veritas.backend.workflow.dto.WorkflowDto;
import com.veritas.backend.workflow.dto.WorkflowEditDto;
import com.veritas.backend.workflow.dto.WorkflowSaveDto;
import com.veritas.backend.user.entity.User;

import com.veritas.backend.requisition.entity.Request;

public interface WorkflowEngineService {

    public void moveToNextStep(Request requisition, User actor);

    public void revertToPreviousStep(Request request, User actor, String reason);

    void startWorkflow(Request request, User actor);

}
