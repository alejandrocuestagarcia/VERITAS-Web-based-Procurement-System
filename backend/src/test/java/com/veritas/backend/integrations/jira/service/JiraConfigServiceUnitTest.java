package com.veritas.backend.integrations.jira.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.veritas.backend.integrations.jira.dto.JiraConfigDto;
import com.veritas.backend.integrations.jira.dto.JiraConfigResponseDto;
import com.veritas.backend.integrations.jira.entity.JiraConfig;
import com.veritas.backend.integrations.jira.mapper.JiraConfigMapper;
import com.veritas.backend.integrations.jira.repository.JiraConfigRepository;
import com.veritas.backend.integrations.jira.service.impl.JiraConfigServiceImpl;
import com.veritas.backend.integrations.jira.repository.JiraSyncQueueItemRepository;
import com.veritas.backend.requisition.repository.RequestRepository;
import com.veritas.backend.audit.service.AuditService;
import com.veritas.backend.requisition.entity.Request;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.eq;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

//AI-GENERATED

@ExtendWith(MockitoExtension.class)
public class JiraConfigServiceUnitTest {

    @Mock
    private JiraConfigRepository repository;

    @Mock
    private JiraConfigMapper mapper;

    @Mock
    private DynamicJiraScheduler scheduler;

    @Mock
    private JiraSyncQueueItemRepository queueItemRepository;

    @Mock
    private RequestRepository requestRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private JiraConfigServiceImpl service;

    private JiraConfig config;
    private JiraConfigDto configDto;
    private JiraConfigResponseDto responseDto;

    @BeforeEach
    void setUp() {
        config = new JiraConfig();
        config.setId(1L);
        config.setName("Test Config");
        config.setJiraUrl("https://test.atlassian.net");
        config.setJql("project = TEST");

        configDto =
            new JiraConfigDto(1L, "Test Config", "https://test.atlassian.net", "user", "token",
                "jql", 60, "customfield_10015", null, null, null, null, null, true);

        responseDto = new JiraConfigResponseDto(1L, "Test Config", "https://test.atlassian.net", "user",
            "jql", 60, "customfield_10015", null, null, null, null, null, null, null, null, true, true);
    }

    @Test
    void GetAllConfigs_ExistingConfigs_ReturnsPage() {
        // 1. Setup
        Pageable pageable = PageRequest.of(0, 10);
        String search = "test-search";
        Page<JiraConfig> configPage = new PageImpl<>(List.of(config));

        // 2. Mocking
        // We match the specific method name in your repository: findAllFiltered
        when(repository.findAllFiltered(anyString(), eq(pageable))).thenReturn(configPage);
        when(mapper.toDto(any(JiraConfig.class))).thenReturn(responseDto);

        // 3. Execution
        Page<JiraConfigResponseDto> result = service.getAllConfigs(pageable, search);

        // 4. Assertions
        assertEquals(1, result.getContent().size());
        // Use .id() (record syntax) instead of .getId()
        assertEquals(responseDto.id(), result.getContent().get(0).id());

        // Verify using the correct repository method name
        verify(repository).findAllFiltered(anyString(), eq(pageable));
    }

    @Test
    void CreateConfig_ValidInput_SavesAndSchedules() {
        when(repository.existsByJiraUrlAndJql(anyString(), anyString())).thenReturn(false);
        when(mapper.toEntity(any())).thenReturn(config);
        when(repository.save(any())).thenReturn(config);
        when(mapper.toDto(any())).thenReturn(responseDto);

        JiraConfigResponseDto result = service.createConfig(configDto);

        assertNotNull(result);
        verify(repository).save(any());
        verify(scheduler).scheduleConfig(any());
    }

    @Test
    void CreateConfig_DuplicateConfig_ThrowsException() {
        when(repository.existsByJiraUrlAndJql(anyString(), anyString())).thenReturn(true);

        assertThrows(RuntimeException.class, () -> service.createConfig(configDto));
        verify(repository, never()).save(any());
    }

    @Test
    void UpdateConfig_ValidInput_SavesAndSchedules() {
        when(repository.findById(1L)).thenReturn(Optional.of(config));
        when(repository.findByJiraUrlAndJql(anyString(), anyString())).thenReturn(Optional.empty());
        when(repository.save(any())).thenReturn(config);
        when(mapper.toDto(any())).thenReturn(responseDto);

        JiraConfigResponseDto result = service.updateConfig(1L, configDto);

        assertNotNull(result);
        verify(repository).save(any());
        verify(scheduler).scheduleConfig(any());
    }

    @Test
    void getAllConfigs_WithSearchString_ReturnsFilteredPage() {
        // Setup
        Pageable pageable = PageRequest.of(0, 10);
        String search = "Jira";
        Page<JiraConfig> configPage = new PageImpl<>(List.of(config));

        // Mocking the specific repository method you created
        when(repository.findAllFiltered(eq("%" + search.toLowerCase() + "%"), eq(pageable)))
                .thenReturn(configPage);
        when(mapper.toDto(any(JiraConfig.class))).thenReturn(responseDto);

        // Execute
        Page<JiraConfigResponseDto> result = service.getAllConfigs(pageable, search);

        // Verify
        assertEquals(1, result.getTotalElements());
        assertEquals(responseDto.name(), result.getContent().get(0).name());
        verify(repository).findAllFiltered(eq("%" + search.toLowerCase() + "%"), eq(pageable));
    }

    @Test
    void getAllConfigs_SearchNoMatch_ReturnsEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        String search = "nonexistent";

        when(repository.findAllFiltered(anyString(), eq(pageable)))
                .thenReturn(Page.empty());

        Page<JiraConfigResponseDto> result = service.getAllConfigs(pageable, search);

        assertEquals(true,result.isEmpty());
        assertEquals(0, result.getTotalElements());
    }

    @Test
    void DeleteConfig_ExistingConfig_DeletesAndCancels() {
        config.setJiraUrl("https://test.atlassian.net");
        when(repository.findById(1L)).thenReturn(Optional.of(config));
        when(requestRepository.findByJiraConfigId(1L)).thenReturn(List.of());
        when(mapper.toDto(any(JiraConfig.class))).thenReturn(responseDto);

        JiraConfigResponseDto result = service.deleteConfigById(1L);

        assertNotNull(result);
        verify(repository).delete(config);
        verify(queueItemRepository).deleteByJiraConfig(config);
        verify(requestRepository).findByJiraConfigId(1L);
        verify(scheduler).cancelConfig(1L);
    }

    @Test
    void DeleteConfig_WithSyncedRequests_ClearsJiraFieldsOnRequests() {
        config.setJiraUrl("https://test.atlassian.net");
        when(repository.findById(1L)).thenReturn(Optional.of(config));
        
        Request syncedRequest = new Request();
        syncedRequest.setJiraIssueKey("TEST-123");
        syncedRequest.setJiraIssueUrl("https://test.atlassian.net/browse/TEST-123");
        syncedRequest.setJiraStatus("Done");
        syncedRequest.setJiraConfig(config);

        Request otherRequest = new Request();
        otherRequest.setJiraIssueKey("OTHER-456");
        otherRequest.setJiraIssueUrl("https://test.atlassian.net/browse/OTHER-456");
        otherRequest.setJiraStatus("Done");
        
        when(requestRepository.findByJiraConfigId(1L)).thenReturn(List.of(syncedRequest));
        when(mapper.toDto(any(JiraConfig.class))).thenReturn(responseDto);

        service.deleteConfigById(1L);

        assertEquals("TEST-123", syncedRequest.getJiraIssueKey());
        assertEquals("https://test.atlassian.net/browse/TEST-123", syncedRequest.getJiraIssueUrl());
        assertEquals("NOT_SYNCED", syncedRequest.getJiraStatus());
        org.junit.jupiter.api.Assertions.assertNull(syncedRequest.getJiraConfig());

        // otherRequest is untouched because it wasn't returned by findByJiraConfigId
        assertEquals("OTHER-456", otherRequest.getJiraIssueKey());
        assertEquals("https://test.atlassian.net/browse/OTHER-456", otherRequest.getJiraIssueUrl());
        assertEquals("Done", otherRequest.getJiraStatus());

        verify(queueItemRepository).deleteByJiraConfig(config);
        verify(requestRepository).saveAll(List.of(syncedRequest));
        verify(auditService).createJiraUnsyncLog(eq(null), eq(syncedRequest), anyString());
    }
}
