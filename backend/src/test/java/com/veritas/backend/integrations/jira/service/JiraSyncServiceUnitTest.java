package com.veritas.backend.integrations.jira.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.veritas.backend.audit.service.impl.AuditServiceImpl;
import com.veritas.backend.integrations.jira.dto.*;
import com.veritas.backend.integrations.jira.entity.JiraConfig;
import com.veritas.backend.integrations.jira.mapper.JiraIssueMapper;
import com.veritas.backend.integrations.jira.repository.JiraConfigRepository;
import com.veritas.backend.integrations.jira.service.impl.JiraSyncServiceImpl;
import com.veritas.backend.requisition.entity.Request;
import com.veritas.backend.requisition.repository.RequestRepository;
import com.veritas.backend.user.entity.User;
import com.veritas.backend.user.repository.UserRepository;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

//AI-GENERATED

@ExtendWith(MockitoExtension.class)
public class JiraSyncServiceUnitTest {

    @Mock
    private JiraConfigRepository configRepository;
    @Mock
    private RequestRepository requestRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private JiraIssueMapper issueMapper;
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private AuditServiceImpl auditService;

    @InjectMocks
    private JiraSyncServiceImpl service;

    private JiraConfig config;

    @BeforeEach
    void setUp() throws Exception {
        config = new JiraConfig();
        config.setId(1L);
        config.setJiraUrl("https://test.atlassian.net");
        config.setUsername("user");
        config.setApiToken("token");
        config.setJql("project = TEST");
        config.setCustomFieldId("customfield_10001");


        Field restTemplateField = JiraSyncServiceImpl.class.getDeclaredField("restTemplate");
        restTemplateField.setAccessible(true);
        restTemplateField.set(service, restTemplate);
    }

    @Test
    void TestConnection_SuccessfulResponse_ReturnsTrue() {
        JiraConfigDto dto =
            new JiraConfigDto(1L, "Test", "https://test.atlassian.net", "user", "token", "jql", 60,
                "field", null, null);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class),
            eq(com.fasterxml.jackson.databind.JsonNode.class)))
            .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        boolean result = service.testConnection(dto);

        assertTrue(result);
    }

    @Test
    void RunManualSync_ExistingIssues_ProcessesAndSaves() {
        when(configRepository.findById(1L)).thenReturn(Optional.of(config));

        JiraSearchResponseRecord response = new JiraSearchResponseRecord(List.of(
            new JiraIssueRecord("10001", "TEST-1", "https://api/1",
                new com.veritas.backend.integrations.jira.dto.JiraFieldsRecord("Summary", null,
                    null, "2026-05-01T16:06:19.433+02:00",
                    null, null, null))));

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class),
            eq(JiraSearchResponseRecord.class)))
            .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        when(requestRepository.findByJiraIssueKey(anyString())).thenReturn(Optional.empty());
        Request request = new Request();
        request.setRequestID(101L);
        when(issueMapper.toRequest(any())).thenReturn(request);
        when(requestRepository.saveAndFlush(any())).thenReturn(request);

        // Mocking the update Jira call
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class),
            eq(String.class)))
            .thenReturn(new ResponseEntity<>(HttpStatus.NO_CONTENT));

        service.runManualSync(1L);

        verify(requestRepository, atLeastOnce()).saveAndFlush(any());
        verify(configRepository).save(any());
    }

    @Test
    void RunManualSync_WithReporterEmail_MatchesAndLinksUser() {
        when(configRepository.findById(1L)).thenReturn(Optional.of(config));

        JiraUserRecord reporter = new JiraUserRecord("reporter@veritas.com", "Reporter Name");

        JiraSearchResponseRecord response = new JiraSearchResponseRecord(List.of(
            new JiraIssueRecord("10001", "TEST-1", "https://api/1", new JiraFieldsRecord("Summary", null, null, "2026-05-01T16:06:19.433+02:00", null, reporter, null))));

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class),
            eq(JiraSearchResponseRecord.class))).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        when(requestRepository.findByJiraIssueKey(anyString())).thenReturn(Optional.empty());
        Request request = new Request();
        request.setRequestID(101L);
        when(issueMapper.toRequest(any())).thenReturn(request);
        when(requestRepository.saveAndFlush(any())).thenReturn(request);

        User mockUser = new User();
        mockUser.setId(42L);
        mockUser.setEmail("reporter@veritas.com");
        when(userRepository.findByEmail("reporter@veritas.com")).thenReturn(Optional.of(mockUser));

        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class),
            eq(String.class))).thenReturn(new ResponseEntity<>(HttpStatus.NO_CONTENT));

        service.runManualSync(1L);

        verify(userRepository).findByEmail("reporter@veritas.com");
        verify(requestRepository, atLeastOnce()).saveAndFlush(any());
        verify(configRepository).save(any());
    }
}
