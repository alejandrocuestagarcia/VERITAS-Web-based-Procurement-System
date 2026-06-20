package com.veritas.backend.integrations.jira.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.veritas.backend.audit.dto.AuditLogDto;
import com.veritas.backend.audit.service.impl.AuditServiceImpl;
import com.veritas.backend.integrations.jira.dto.JiraConfigDto;
import com.veritas.backend.integrations.jira.dto.JiraConfigResponseDto;
import com.veritas.backend.integrations.jira.service.JiraConfigService;
import com.veritas.backend.integrations.jira.service.JiraSyncService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class JiraConfigControllerUnitTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private JiraConfigService service;

    @Mock
    private JiraSyncService syncService;

    @Mock
    private AuditServiceImpl auditService;

    @InjectMocks
    private JiraConfigController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void getAllConfigs_ValidRequest_ReturnsPage() throws Exception {
        Page<JiraConfigResponseDto> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        when(service.getAllConfigs(any(Pageable.class), any())).thenReturn(page);

        mockMvc.perform(get("/jira-configs")
                .param("page", "0")
                .param("size", "10")
                .param("search", "test"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE));

        verify(service).getAllConfigs(any(Pageable.class), eq("test"));
    }

    @Test
    void getConfigById_ValidRequest_ReturnsDto() throws Exception {
        JiraConfigResponseDto dto = new JiraConfigResponseDto(1L, "name", "http://jira", "user", "jql", 5, "customField", null, null, null, null, null, null, null, null, true);
        when(service.getConfigById(1L)).thenReturn(dto);

        mockMvc.perform(get("/jira-configs/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.id").value(1L));

        verify(service).getConfigById(1L);
    }

    @Test
    void createConfig_ValidRequest_ReturnsDto() throws Exception {
        JiraConfigDto dto = new JiraConfigDto(1L, "name", "http://jira", "user", "token", "jql", 5, "customField", 1L, 1L, 1L, null, null);
        JiraConfigResponseDto response = new JiraConfigResponseDto(1L, "name", "http://jira", "user", "jql", 5, "customField", null, null, null, null, null, null, null, null, true);
        when(service.createConfig(any(JiraConfigDto.class))).thenReturn(response);

        mockMvc.perform(post("/jira-configs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));

        verify(service).createConfig(any(JiraConfigDto.class));
    }

    @Test
    void updateConfig_ValidRequest_ReturnsDto() throws Exception {
        JiraConfigDto dto = new JiraConfigDto(1L, "name", "http://jira", "user", "token", "jql", 5, "customField", 1L, 1L, 1L, null, null);
        JiraConfigResponseDto response = new JiraConfigResponseDto(1L, "name", "http://jira", "user", "jql", 5, "customField", null, null, null, null, null, null, null, null, true);
        when(service.updateConfig(eq(1L), any(JiraConfigDto.class))).thenReturn(response);

        mockMvc.perform(put("/jira-configs/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));

        verify(service).updateConfig(eq(1L), any(JiraConfigDto.class));
    }

    @Test
    void deleteConfig_ValidRequest_ReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/jira-configs/{id}", 1L))
                .andExpect(status().isNoContent());

        verify(service).deleteConfigById(1L);
    }

    @Test
    void triggerSync_ValidRequest_ReturnsOk() throws Exception {
        mockMvc.perform(post("/jira-configs/{id}/sync", 1L))
                .andExpect(status().isOk());

        verify(syncService).runManualSync(1L);
    }

    @Test
    void triggerAllSyncs_ValidRequest_ReturnsOk() throws Exception {
        mockMvc.perform(post("/jira-configs/sync-all"))
                .andExpect(status().isOk());

        verify(syncService).runAllSyncs();
    }

    @Test
    void testConnection_ValidRequest_ReturnsOk() throws Exception {
        JiraConfigDto dto = new JiraConfigDto(1L, "name", "http://jira", "user", "token", "jql", 5, "customField", 1L, 1L, 1L, null, null);
        when(syncService.testConnection(any(JiraConfigDto.class))).thenReturn(true);

        mockMvc.perform(post("/jira-configs/test")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(syncService).testConnection(any(JiraConfigDto.class));
    }

    @Test
    void getJiraSyncAudit_ValidRequest_ReturnsPage() throws Exception {
        Page<AuditLogDto> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        when(auditService.getJiraIssueLogsByActions(any(), any(), anyString())).thenReturn(page);

        mockMvc.perform(get("/jira-configs/audit")
                .param("page", "0")
                .param("size", "10")
                .param("search", "test"))
                .andExpect(status().isOk());

        verify(auditService).getJiraIssueLogsByActions(any(), any(), eq("test"));
    }
}
