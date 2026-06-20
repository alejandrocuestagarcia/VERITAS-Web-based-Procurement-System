package com.veritas.backend.requisition.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.veritas.backend.requisition.service.RequisitionService;
import com.veritas.backend.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class RequisitionControllerUnitTest {

    @Mock
    private RequisitionService requisitionService;

    @InjectMocks
    private RequisitionController requisitionController;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setEmail("test@veritas.com");
    }

    @Test
    void getNextStepRole_RoleNotNull_ReturnsQuotedRole() {
        when(requisitionService.getNextStepRole(1L, testUser)).thenReturn("ROLE_APPROVER");

        ResponseEntity<String> response = requisitionController.getNextStepRole(1L, testUser);

        assertNotNull(response);
        assertEquals("\"ROLE_APPROVER\"", response.getBody());
        verify(requisitionService).getNextStepRole(1L, testUser);
    }

    @Test
    void getNextStepRole_RoleNull_ReturnsEmptyQuotes() {
        when(requisitionService.getNextStepRole(1L, testUser)).thenReturn(null);

        ResponseEntity<String> response = requisitionController.getNextStepRole(1L, testUser);

        assertNotNull(response);
        assertEquals("\"\"", response.getBody());
        verify(requisitionService).getNextStepRole(1L, testUser);
    }
}
