package com.veritas.backend.audit.mapper;

import static org.junit.jupiter.api.Assertions.*;

import com.veritas.backend.audit.dto.AuditLogDto;
import com.veritas.backend.audit.entity.AuditLog;
import com.veritas.backend.requisition.entity.Request;
import com.veritas.backend.user.entity.User;
import com.veritas.backend.workflow.entity.WorkflowStep;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;

class AuditLogMapperUnitTest {

    private AuditLogMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(AuditLogMapper.class);
    }

    @Test
    void jiraSyncLogtoDto_NullLog_ReturnsNull() {
        assertNull(mapper.jiraSyncLogtoDto(null));
    }

    @Test
    void jiraSyncLogtoDto_NullAssociations_MapsCorrectly() {
        AuditLog log = AuditLog.builder()
            .id(1L)
            .action("SYNC")
            .timestamp(LocalDateTime.now())
            .entryHash("hash123")
            .previousHash("prev123")
            .actor(null)
            .request(null)
            .build();

        AuditLogDto dto = mapper.jiraSyncLogtoDto(log);

        assertAll(
            () -> assertEquals("SYNC", dto.action()),
            () -> assertEquals("System", dto.user()),
            () -> assertEquals("hash123", dto.currentHash()),
            () -> assertNull(dto.requestName()),
            () -> assertNull(dto.requestKey()),
            () -> assertNull(dto.jiraIssueUrl())
        );
    }

    @Test
    void jiraSyncLogtoDto_WithAssociations_MapsCorrectly() {
        User actor = new User();
        actor.setEmail("actor@example.com");

        Request request = new Request();
        request.setRequestName("Request 1");
        request.setJiraIssueKey("JIRA-1");
        request.setJiraIssueUrl("http://jira/1");

        AuditLog log = AuditLog.builder()
            .id(2L)
            .action("SYNC")
            .actor(actor)
            .request(request)
            .entryHash("hash123")
            .build();

        AuditLogDto dto = mapper.jiraSyncLogtoDto(log);

        assertAll(
            () -> assertEquals("actor@example.com", dto.user()),
            () -> assertEquals("Request 1", dto.requestName()),
            () -> assertEquals("JIRA-1", dto.requestKey()),
            () -> assertEquals("http://jira/1", dto.jiraIssueUrl())
        );
    }

    @Test
    void toDto_NullLog_ReturnsNull() {
        assertNull(mapper.toDto(null));
    }

    @Test
    void toDto_NullAssociations_MapsCorrectly() {
        AuditLog log = AuditLog.builder()
            .id(3L)
            .action("APPROVE")
            .timestamp(LocalDateTime.now())
            .entryHash("hash456")
            .actor(null)
            .request(null)
            .previousStep(null)
            .newStep(null)
            .build();

        AuditLogDto dto = mapper.toDto(log);

        assertAll(
            () -> assertEquals("System", dto.user()),
            () -> assertNull(dto.requestName()),
            () -> assertNull(dto.requestKey()),
            () -> assertNull(dto.previousStatus()),
            () -> assertNull(dto.newStatus()),
            () -> assertEquals("hash456", dto.currentHash())
        );
    }

    @Test
    void toDto_WithAssociations_MapsCorrectly() {
        User actor = new User();
        actor.setEmail("user@example.com");

        Request request = new Request();
        request.setRequestName("Laptop Request");
        request.setRequestKey("REQ-99");
        request.setJiraIssueUrl("http://jira/99");

        WorkflowStep prevStep = new WorkflowStep();
        prevStep.setName("Draft");

        WorkflowStep newStep = new WorkflowStep();
        newStep.setName("Approved");

        AuditLog log = AuditLog.builder()
            .id(4L)
            .action("APPROVE")
            .actor(actor)
            .request(request)
            .previousStep(prevStep)
            .newStep(newStep)
            .entryHash("hash456")
            .build();

        AuditLogDto dto = mapper.toDto(log);

        assertAll(
            () -> assertEquals("user@example.com", dto.user()),
            () -> assertEquals("Laptop Request", dto.requestName()),
            () -> assertEquals("REQ-99", dto.requestKey()),
            () -> assertEquals("http://jira/99", dto.jiraIssueUrl()),
            () -> assertEquals("Draft", dto.previousStatus()),
            () -> assertEquals("Approved", dto.newStatus())
        );
    }
}
