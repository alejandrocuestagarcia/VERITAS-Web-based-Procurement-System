package com.veritas.backend.integrations.jira.mapper;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.veritas.backend.integrations.jira.dto.JiraConfigDto;
import com.veritas.backend.integrations.jira.dto.JiraConfigResponseDto;
import com.veritas.backend.integrations.jira.entity.JiraConfig;
import com.veritas.backend.project.entity.Project;
import com.veritas.backend.user.entity.User;
import com.veritas.backend.workflow.entity.WorkflowDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;

class JiraConfigMapperUnitTest {

    private JiraConfigMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(JiraConfigMapper.class);
    }

    @Test
    void toDto_NullEntity_ReturnsNull() {
        assertNull(mapper.toDto(null));
    }

    @Test
    void toEntity_NullDto_ReturnsNull() {
        assertNull(mapper.toEntity(null));
    }

    @Test
    void updateEntityFromDto_NullDto_DoesNothing() {
        JiraConfig entity = new JiraConfig();
        entity.setId(10L);
        entity.setName("Original Name");

        mapper.updateEntityFromDto(null, entity);
        assertEquals("Original Name", entity.getName());
    }

    @Test
    void toDto_NullAssociations_MapsCorrectly() {
        JiraConfig entity = JiraConfig.builder()
            .id(1L)
            .name("Jira Dev")
            .jiraUrl("https://jira.dev")
            .username("dev-user")
            .apiToken(null)
            .jql("project = PRJ")
            .syncIntervalMinutes(15)
            .customFieldId("customfield_10001")
            .fallbackUser(null)
            .fallbackProject(null)
            .fallbackWorkflow(null)
            .lastSyncTime(LocalDateTime.of(2026, 6, 1, 12, 0))
            .build();

        JiraConfigResponseDto dto = mapper.toDto(entity);

        assertAll(
            () -> assertEquals(1L, dto.id()),
            () -> assertEquals("Jira Dev", dto.name()),
            () -> assertEquals("https://jira.dev", dto.jiraUrl()),
            () -> assertEquals("dev-user", dto.username()),
            () -> assertEquals("project = PRJ", dto.jql()),
            () -> assertEquals(15, dto.syncIntervalMinutes()),
            () -> assertEquals("customfield_10001", dto.customFieldId()),
            () -> assertNull(dto.fallbackUserId()),
            () -> assertNull(dto.fallbackUserName()),
            () -> assertNull(dto.fallbackProjectId()),
            () -> assertNull(dto.fallbackProjectName()),
            () -> assertNull(dto.fallbackWorkflowId()),
            () -> assertNull(dto.fallbackWorkflowName()),
            () -> assertEquals(LocalDateTime.of(2026, 6, 1, 12, 0), dto.lastSyncTime()),
            () -> assertFalse(dto.isTokenSet())
        );
    }

    @Test
    void toDto_WithAssociationsAndToken_MapsCorrectly() {
        User user = new User();
        user.setId(5L);
        user.setName("Fallback User");

        Project project = new Project();
        project.setId(6L);
        project.setName("Fallback Project");

        WorkflowDefinition workflow = new WorkflowDefinition();
        workflow.setId(7L);
        workflow.setName("Fallback Workflow");

        JiraConfig entity = JiraConfig.builder()
            .id(1L)
            .name("Jira Dev")
            .jiraUrl("https://jira.dev")
            .username("dev-user")
            .apiToken("  token_abc  ")
            .jql("project = PRJ")
            .syncIntervalMinutes(15)
            .customFieldId("customfield_10001")
            .fallbackUser(user)
            .fallbackProject(project)
            .fallbackWorkflow(workflow)
            .lastSyncTime(LocalDateTime.of(2026, 6, 1, 12, 0))
            .build();

        JiraConfigResponseDto dto = mapper.toDto(entity);

        assertAll(
            () -> assertEquals(5L, dto.fallbackUserId()),
            () -> assertEquals("Fallback User", dto.fallbackUserName()),
            () -> assertEquals(6L, dto.fallbackProjectId()),
            () -> assertEquals("Fallback Project", dto.fallbackProjectName()),
            () -> assertEquals(7L, dto.fallbackWorkflowId()),
            () -> assertEquals("Fallback Workflow", dto.fallbackWorkflowName()),
            () -> assertTrue(dto.isTokenSet())
        );
    }

    @Test
    void toEntity_MapsCorrectly() {
        JiraConfigDto dto = new JiraConfigDto(
            1L,
            "Jira Dev",
            "https://jira.dev",
            "dev-user",
            "token_abc",
            "project = PRJ",
            15,
            "customfield_10001",
            5L,
            6L,
            7L,
            LocalDateTime.of(2026, 6, 1, 12, 0),
            LocalDateTime.of(2026, 6, 1, 12, 15)
        );

        JiraConfig entity = mapper.toEntity(dto);

        assertAll(
            () -> assertEquals("Jira Dev", entity.getName()),
            () -> assertEquals("https://jira.dev", entity.getJiraUrl()),
            () -> assertEquals("dev-user", entity.getUsername()),
            () -> assertEquals("token_abc", entity.getApiToken()),
            () -> assertEquals("project = PRJ", entity.getJql()),
            () -> assertEquals(15, entity.getSyncIntervalMinutes()),
            () -> assertEquals("customfield_10001", entity.getCustomFieldId()),
            () -> assertNull(entity.getFallbackUser()),
            () -> assertNull(entity.getFallbackProject()),
            () -> assertNull(entity.getFallbackWorkflow())
        );
    }

    @Test
    void updateEntityFromDto_UpdatesCorrectly() {
        User origUser = new User();
        Project origProject = new Project();
        WorkflowDefinition origWorkflow = new WorkflowDefinition();

        JiraConfig entity = JiraConfig.builder()
            .id(10L)
            .name("Old Name")
            .jiraUrl("https://jira.old")
            .username("old-user")
            .apiToken("old-token")
            .jql("project = OLD")
            .syncIntervalMinutes(30)
            .customFieldId("customfield_old")
            .fallbackUser(origUser)
            .fallbackProject(origProject)
            .fallbackWorkflow(origWorkflow)
            .lastSyncTime(LocalDateTime.of(2026, 6, 1, 12, 0))
            .build();

        JiraConfigDto dto = new JiraConfigDto(
            1L,
            "New Name",
            "https://jira.new",
            "new-user",
            "new-token",
            "project = NEW",
            15,
            "customfield_new",
            5L,
            6L,
            7L,
            LocalDateTime.of(2026, 6, 1, 12, 10),
            LocalDateTime.of(2026, 6, 1, 12, 25)
        );

        mapper.updateEntityFromDto(dto, entity);

        assertAll(
            () -> assertEquals(10L, entity.getId()),
            () -> assertEquals("New Name", entity.getName()),
            () -> assertEquals("https://jira.new", entity.getJiraUrl()),
            () -> assertEquals("new-user", entity.getUsername()),
            () -> assertEquals("new-token", entity.getApiToken()),
            () -> assertEquals("project = NEW", entity.getJql()),
            () -> assertEquals(15, entity.getSyncIntervalMinutes()),
            () -> assertEquals("customfield_new", entity.getCustomFieldId()),
            () -> assertEquals(LocalDateTime.of(2026, 6, 1, 12, 0), entity.getLastSyncTime()),
            () -> assertSame(origUser, entity.getFallbackUser()),
            () -> assertSame(origProject, entity.getFallbackProject()),
            () -> assertSame(origWorkflow, entity.getFallbackWorkflow())
        );
    }

    @Test
    void toDto_PartialNullAssociations() {
        User user = new User();
        user.setId(null);
        user.setName(null);

        Project project = new Project();
        project.setId(null);
        project.setName(null);

        WorkflowDefinition workflow = new WorkflowDefinition();
        workflow.setId(null);
        workflow.setName(null);

        JiraConfig entity = JiraConfig.builder()
            .fallbackUser(user)
            .fallbackProject(project)
            .fallbackWorkflow(workflow)
            .apiToken("")
            .build();

        JiraConfigResponseDto dto = mapper.toDto(entity);
        assertAll(
            () -> assertNull(dto.fallbackUserId()),
            () -> assertNull(dto.fallbackUserName()),
            () -> assertNull(dto.fallbackProjectId()),
            () -> assertNull(dto.fallbackProjectName()),
            () -> assertNull(dto.fallbackWorkflowId()),
            () -> assertNull(dto.fallbackWorkflowName()),
            () -> assertFalse(dto.isTokenSet())
        );
    }
}
