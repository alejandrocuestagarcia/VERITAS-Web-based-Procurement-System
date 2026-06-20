package com.veritas.backend.workflow.controller;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.veritas.backend.user.entity.User;
import com.veritas.backend.workflow.service.WorkflowService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class WorkflowControllerUnitTest {

    @Mock
    private WorkflowService workflowService;

    @InjectMocks
    private WorkflowController workflowController;

    @Test
    void getAllWorkflows_UserNotNull_CallsServiceAndLogs() {
        Pageable pageable = PageRequest.of(0, 10);
        User user = new User();
        user.setEmail("user@test.com");

        when(workflowService.getAllWorkflows(pageable, "search", true, user)).thenReturn(Page.empty());

        var result = workflowController.getAllWorkflows(pageable, "search", true, user);

        assertNotNull(result);
        verify(workflowService).getAllWorkflows(pageable, "search", true, user);
    }

    @Test
    void getAllWorkflows_UserNull_CallsServiceAndLogsNull() {
        Pageable pageable = PageRequest.of(0, 10);

        when(workflowService.getAllWorkflows(pageable, "search", true, null)).thenReturn(Page.empty());

        var result = workflowController.getAllWorkflows(pageable, "search", true, null);

        assertNotNull(result);
        verify(workflowService).getAllWorkflows(pageable, "search", true, null);
    }
}
