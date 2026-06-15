package com.veritas.backend.audit;

import com.veritas.backend.BaseDBIntegrationTest;
import com.veritas.backend.audit.dto.AuditLogDto;
import com.veritas.backend.audit.entity.AuditLog;
import com.veritas.backend.audit.repository.AuditLogRepository;
import com.veritas.backend.audit.service.AuditService;
import com.veritas.backend.auth.repository.RefreshTokenRepository;
import com.veritas.backend.requisition.entity.Request;
import com.veritas.backend.requisition.repository.RequestRepository;
import com.veritas.backend.team.repository.TeamRepository;
import com.veritas.backend.user.entity.User;
import com.veritas.backend.user.entity.UserRole;
import com.veritas.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static com.veritas.backend.common.model.AuditActionConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.data.domain.Sort;

@SpringBootTest
class AuditServiceIntegrationTest extends BaseDBIntegrationTest {

    @Autowired
    AuditService auditService;

    @Autowired
    AuditLogRepository auditLogRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    RefreshTokenRepository refreshTokenRepository;

    @Autowired
    RequestRepository requestRepository;

    @Autowired
    TeamRepository teamRepository;

    @Autowired
    PasswordEncoder encoder;

    User testActor;
    Request testRequest;
    @BeforeEach
    void setup() {
        auditLogRepository.deleteAll();
        requestRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        testActor = userRepository.save(User.builder()
                .name("Test Actor")
                .email("actor@yahoo.com")
                .passwordHash(encoder.encode("password123"))
                .role(UserRole.PROCUREMENT_OFFICER)
                .isActive(true)
                .build());

        testRequest = new Request();
        testRequest.setRequestName("Test Request");
        testRequest = requestRepository.save(testRequest);
    }

    @Test
    void CreateJiraSyncLog_ValidInput_SavesLogToDatabase() {
        String details = "Synced issue SCRUM-42";

        auditService.createJiraSyncLog(testActor, testRequest, details);

        List<AuditLog> logs = auditLogRepository.findAll();
        assertEquals(1, logs.size());

        AuditLog saved = logs.getFirst();
        assertAll(
            () -> assertEquals(JIRA_SYNC, saved.getAction()),
            () -> assertEquals(details, saved.getDescription()),
            () -> assertEquals(testActor.getId(), saved.getActor().getId()),
            () -> assertEquals(testRequest.getRequestID(), saved.getRequest().getRequestID()),
            () -> assertNotNull(saved.getEntryHash()),
            () -> assertNotNull(saved.getTimestamp())
        );
    }

    @Test
    void CreateJiraSyncLog_ValidInput_GeneratesUniqueHashes() {
        auditService.createJiraSyncLog(testActor, testRequest, "First sync");
        auditService.createJiraSyncLog(testActor, testRequest, "Second sync");

        List<AuditLog> logs = auditLogRepository.findAll();
        assertEquals(2, logs.size());
        assertNotEquals(logs.get(0).getEntryHash(), logs.get(1).getEntryHash());
    }

    @Test
    void GetJiraIssueLogsByAction_ValidInput_ReturnsMappedDtos() {
        auditService.createJiraSyncLog(testActor, testRequest, "Synced issue SCRUM-1");
        auditService.createJiraSyncLog(testActor, testRequest, "Synced issue SCRUM-2");

        Page<AuditLogDto> result = auditService.getJiraIssueLogsByAction(JIRA_SYNC, PageRequest.of(0, 10), "");

        assertEquals(2, result.getContent().size());
    }

    @Test
    void GetJiraIssueLogsByAction_NoLogsExist_ReturnsEmptyList() {
        Page<AuditLogDto> result = auditService.getJiraIssueLogsByAction(JIRA_SYNC, PageRequest.of(0, 10), "");

        assertTrue(result.getContent().isEmpty());
    }

    @Test
    void GetJiraIssueLogsByAction_WithUnrelatedAction_ReturnsEmptyList() {
        auditService.createJiraSyncLog(testActor, testRequest, "Some sync");

        Page<AuditLogDto> result = auditService.getJiraIssueLogsByAction("SOME_OTHER_ACTION", PageRequest.of(0, 10), "");

        assertTrue(result.getContent().isEmpty());
    }

    @Test
    void CreateRequisitionChangeLog_ValidInput_SavesLogToDatabase() {
        String details = "Field 'requestName' changed from 'Old' to 'New'";

        auditService.createRequisitionChangeLog(testActor, testRequest, details);

        List<AuditLog> logs = auditLogRepository.findAll();
        assertEquals(1, logs.size());

        AuditLog saved = logs.getFirst();
        assertAll(
            () -> assertEquals(REQUISITION_EDITED, saved.getAction()),
            () -> assertEquals(details, saved.getDescription()),
            () -> assertEquals(testActor.getId(), saved.getActor().getId()),
            () -> assertEquals(testRequest.getRequestID(), saved.getRequest().getRequestID()),
            () -> assertNotNull(saved.getEntryHash()),
            () -> assertNotNull(saved.getTimestamp())
        );
    }

    @Test
    void GetJiraIssueLogsByActions_MultipleActions_ReturnsSortedAndFilteredLogs() {
        auditService.createJiraSyncLog(testActor, testRequest, "Syncing 1");
        auditService.createJiraUnsyncLog(testActor, testRequest, "Unsyncing 2");

        Page<AuditLogDto> result = auditService.getJiraIssueLogsByActions(
                List.of(JIRA_SYNC, JIRA_UNSYNC),
                PageRequest.of(0, 10, Sort.by("timestamp").descending()),
                ""
        );

        assertEquals(2, result.getContent().size());
    }

    // AI-GENERATED
    @Test
    void CreateNotificationLog_ValidInput_SavesLogToDatabase() {
        String details = "Notifications sent for approval at step 'Review': actor@yahoo.com (APPROVED), assignee@test.com (ASSIGNED)";

        auditService.createNotificationLog(testActor, testRequest, details);

        List<AuditLog> logs = auditLogRepository.findAll();
        assertEquals(1, logs.size());

        AuditLog saved = logs.getFirst();
        assertAll(
            () -> assertEquals(NOTIFICATION_SENT, saved.getAction()),
            () -> assertEquals(details, saved.getDescription()),
            () -> assertEquals(testActor.getId(), saved.getActor().getId()),
            () -> assertEquals(testRequest.getRequestID(), saved.getRequest().getRequestID()),
            () -> assertNotNull(saved.getEntryHash()),
            () -> assertNotNull(saved.getTimestamp())
        );
    }

    @Test
    void CreateJiraCommentLog_ValidInput_SavesLogToDatabase() {
        String details = "Jira comment posted to issue SCRUM-42: Transition to step 'Procurement Review'";

        auditService.createJiraCommentLog(testActor, testRequest, details);

        List<AuditLog> logs = auditLogRepository.findAll();
        assertEquals(1, logs.size());

        AuditLog saved = logs.getFirst();
        assertAll(
            () -> assertEquals(JIRA_COMMENT_POSTED, saved.getAction()),
            () -> assertEquals(details, saved.getDescription()),
            () -> assertEquals(testActor.getId(), saved.getActor().getId()),
            () -> assertEquals(testRequest.getRequestID(), saved.getRequest().getRequestID()),
            () -> assertNotNull(saved.getEntryHash()),
            () -> assertNotNull(saved.getTimestamp())
        );
    }
}