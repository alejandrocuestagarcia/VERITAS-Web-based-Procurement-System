package com.veritas.backend.audit.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.veritas.backend.audit.dto.AuditLogDto;
import com.veritas.backend.audit.service.AuditPdfService;
import com.veritas.backend.audit.service.AuditService;
import com.veritas.backend.requisition.service.RequisitionService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AuditControllerUnitTest {

    private MockMvc mockMvc;

    @Mock
    private AuditService auditService;

    @Mock
    private AuditPdfService auditPdfService;

    @Mock
    private RequisitionService requisitionService;

    @InjectMocks
    private AuditController auditController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(auditController).build();
    }

    @Test
    void getAuditLogs_ValidRequest_ReturnsLogs() throws Exception {
        Long requestId = 1L;
        AuditLogDto dto = new AuditLogDto("name", "key", "url", "user", LocalDateTime.now(), "SUBMIT", "detail", "prevStatus", "newStatus", "hash", "prevHash");
        when(auditService.getAuditLogsByRequestId(requestId)).thenReturn(List.of(dto));

        mockMvc.perform(get("/requisitions/{requestId}/audit", requestId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$[0].action").value("SUBMIT"));

        verify(requisitionService).checkRequestAccess(eq(requestId), any());
        verify(auditService).getAuditLogsByRequestId(requestId);
    }

    @Test
    void exportAuditPdf_ValidRequest_ReturnsPdfResource() throws Exception {
        Long requestId = 1L;
        byte[] pdfContent = new byte[]{1, 2, 3};
        when(auditPdfService.generateAuditReport(requestId)).thenReturn(pdfContent);

        mockMvc.perform(get("/requisitions/{requestId}/audit/export", requestId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", "attachment; filename=audit_report_req_1.pdf"))
                .andExpect(content().bytes(pdfContent));

        verify(requisitionService).checkRequestAccess(eq(requestId), any());
        verify(auditPdfService).generateAuditReport(requestId);
    }

    @Test
    void getAuditTrace_ValidRequest_ReturnsEmptyList() throws Exception {
        Long requestId = 1L;

        mockMvc.perform(get("/requisitions/{requestId}/audit/trace", requestId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$").isEmpty());

        verify(requisitionService).checkRequestAccess(eq(requestId), any());
    }
}
