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
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

//AI-GENERATED

@ExtendWith(MockitoExtension.class)
public class JiraConfigServiceUnitTest {

    @Mock
    private JiraConfigRepository repository;

    @Mock
    private JiraConfigMapper mapper;

    @Mock
    private DynamicJiraScheduler scheduler;

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
                "project = TEST", 60, "field_123", null, null);

        responseDto = new JiraConfigResponseDto(1L, "Test Config", "https://test.atlassian.net", "user",
            "project = TEST", 60, "field_123", null, null, true);
    }

    @Test
    void GetAllConfigs_ExistingConfigs_ReturnsList() {
        when(repository.findAll()).thenReturn(List.of(config));
        when(mapper.toDto(any())).thenReturn(responseDto);

        List<JiraConfigResponseDto> result = service.getAllConfigs();

        assertEquals(1, result.size());
        verify(repository).findAll();
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

}
