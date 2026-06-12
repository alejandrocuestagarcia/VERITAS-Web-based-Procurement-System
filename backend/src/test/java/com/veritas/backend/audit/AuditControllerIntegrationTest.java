package com.veritas.backend.audit;

import com.veritas.backend.BaseDBIntegrationTest;
import com.veritas.backend.audit.entity.AuditLog;
import com.veritas.backend.audit.repository.AuditLogRepository;
import com.veritas.backend.auth.service.JwtService;
import com.veritas.backend.requisition.entity.Request;
import com.veritas.backend.requisition.repository.RequestRepository;
import com.veritas.backend.requisition.repository.InvoiceRepository;
import com.veritas.backend.requisition.repository.AttachmentRepository;
import com.veritas.backend.requisition.repository.RequestItemRepository;
import com.veritas.backend.team.entity.Team;
import com.veritas.backend.team.repository.TeamRepository;
import com.veritas.backend.user.entity.User;
import com.veritas.backend.user.entity.UserRole;
import com.veritas.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static com.veritas.backend.common.model.AuditActionConstants.REQUISITION_EDITED;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// AI-GENERATED
@SpringBootTest
@AutoConfigureMockMvc
class AuditControllerIntegrationTest extends BaseDBIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private RequestRepository requestRepository;
    @Autowired
    private AuditLogRepository auditLogRepository;
    @Autowired
    private InvoiceRepository invoiceRepository;
    @Autowired
    private AttachmentRepository attachmentRepository;
    @Autowired
    private RequestItemRepository requestItemRepository;

    private String requesterToken;
    private Request testRequest;
    private User requester;

    @BeforeEach
    void setUp() {
        invoiceRepository.deleteAllInBatch();
        attachmentRepository.deleteAllInBatch();
        requestItemRepository.deleteAllInBatch();
        auditLogRepository.deleteAllInBatch();
        requestRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
        teamRepository.deleteAllInBatch();

        Team team = teamRepository.save(Team.builder()
                .name("Engineering")
                .description("Engineering team for testing")
                .isActive(true)
                .build());

        requester = new User();
        requester.setEmail("req-audit@veritas.com");
        requester.setName("Audit Requester");
        requester.setPasswordHash("hashed");
        requester.setRole(UserRole.REQUESTER);
        requester.setIsActive(true);
        requester.setTeam(team);
        requester = userRepository.save(requester);

        requesterToken = jwtService.generateAccessToken(requester);

        testRequest = new Request();
        testRequest.setRequestName("Audit Test Request");
        testRequest.setUser(requester);
        testRequest = requestRepository.save(testRequest);
    }

    @Test
    void GetAuditLogs_ValidRequest_ReturnsChronologicalLogs() throws Exception {
        // Arrange
        AuditLog log1 = AuditLog.builder()
                .request(testRequest)
                .actor(requester)
                .action("SUBMIT")
                .description("Request submitted")
                .timestamp(LocalDateTime.now().minusMinutes(5))
                .entryHash("hash1")
                .build();
        
        AuditLog log2 = AuditLog.builder()
                .request(testRequest)
                .actor(requester)
                .action(REQUISITION_EDITED)
                .description("Request edited")
                .timestamp(LocalDateTime.now().minusMinutes(1))
                .entryHash("hash2")
                .build();

        auditLogRepository.save(log1);
        auditLogRepository.save(log2);

        // Act & Assert
        mockMvc.perform(get("/api/v1/requisitions/" + testRequest.getRequestID() + "/audit")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].action").value(REQUISITION_EDITED))
                .andExpect(jsonPath("$[0].description").value("Request edited"))
                .andExpect(jsonPath("$[0].user").value("req-audit@veritas.com"))
                .andExpect(jsonPath("$[1].action").value("SUBMIT"))
                .andExpect(jsonPath("$[1].description").value("Request submitted"))
                .andExpect(jsonPath("$[1].user").value("req-audit@veritas.com"));
    }

    @Test
    void GetAuditLogs_NoLogs_ReturnsEmptyArray() throws Exception {
        mockMvc.perform(get("/api/v1/requisitions/" + testRequest.getRequestID() + "/audit")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void GetAuditLogs_Unauthenticated_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/requisitions/" + testRequest.getRequestID() + "/audit")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }
}
