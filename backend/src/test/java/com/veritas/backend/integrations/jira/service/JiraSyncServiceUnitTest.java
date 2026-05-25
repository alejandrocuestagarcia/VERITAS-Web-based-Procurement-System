package com.veritas.backend.integrations.jira.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.veritas.backend.audit.service.impl.AuditServiceImpl;
import com.veritas.backend.integrations.jira.dto.*;
import com.veritas.backend.integrations.jira.entity.JiraConfig;
import com.veritas.backend.integrations.jira.entity.JiraSyncQueueItem;
import com.veritas.backend.integrations.jira.mapper.JiraIssueMapper;
import com.veritas.backend.integrations.jira.repository.JiraConfigRepository;
import com.veritas.backend.integrations.jira.repository.JiraSyncQueueItemRepository;
import com.veritas.backend.integrations.jira.service.impl.JiraSyncServiceImpl;
import com.veritas.backend.project.entity.Project;
import com.veritas.backend.project.repository.ProjectRepository;
import com.veritas.backend.requisition.entity.Request;
import com.veritas.backend.requisition.entity.RequestItem;
import com.veritas.backend.requisition.entity.RequestStatus;
import com.veritas.backend.workflow.entity.WorkflowStep;
import com.veritas.backend.requisition.repository.RequestItemRepository;
import com.veritas.backend.requisition.repository.RequestRepository;
import com.veritas.backend.user.entity.User;
import com.veritas.backend.user.repository.UserRepository;
import java.lang.reflect.Field;
import java.util.List;
import java.util.ArrayList;
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

    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private RequestItemRepository requestItemRepository;
    @Mock
    private JiraSyncQueueItemRepository queueItemRepository;
    @Mock
    private com.veritas.backend.workflow.repository.WorkflowDefinitionRepository workflowDefinitionRepository;
    @Mock
    private com.veritas.backend.workflow.repository.WorkflowStepRepository workflowStepRepository;
    @Mock
    private com.veritas.backend.requisition.service.RequisitionService requisitionService;
    @Mock
    private com.veritas.backend.requisition.repository.AttachmentRepository attachmentRepository;

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
                "field", null, null, null, null, null);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class),
            eq(JsonNode.class)))
            .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        boolean result = service.testConnection(dto);

        assertTrue(result);
    }

    @Test
    void RunManualSync_ExistingIssues_ProcessesAndSaves() {
        when(configRepository.findById(1L)).thenReturn(Optional.of(config));

        JiraSearchResponseRecord response = new JiraSearchResponseRecord(List.of(
            new JiraIssueRecord("10001", "TEST-1", "https://api/1",
                new JiraFieldsRecord("Summary", null,
                    null, "2026-05-01T16:06:19.433+02:00",
                    null, null, null, null))));

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
            new JiraIssueRecord("10001", "TEST-1", "https://api/1", new JiraFieldsRecord("Summary", null, null, "2026-05-01T16:06:19.433+02:00", null, reporter, null, null))));

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

    //AI-GENERATED
    @Test
    void MapDescription_ValidADF_ParsesToPlainText() throws Exception {
        String adfJson = "{\"type\":\"doc\",\"version\":1,\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"This is a test description.\"}]}]}";
        ObjectMapper mapper = new ObjectMapper();
        JsonNode adfNode = mapper.readTree(adfJson);

        String result = service.mapDescription(adfNode);

        assertEquals("This is a test description.", result);
    }

    //AI-GENERATED
    @Test
    void MapDescription_WithTableAndText_SkipsTableContent() throws Exception {
        String adfJson = "{\"type\":\"doc\",\"version\":1,\"content\":[" +
                "{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"Actual description text.\"}]}," +
                "{\"type\":\"table\",\"content\":[{\"type\":\"tableRow\",\"content\":[" +
                "{\"type\":\"tableCell\",\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"macbook\"}]}]}" +
                "]}]}" +
                "]}";
        ObjectMapper mapper = new ObjectMapper();
        JsonNode adfNode = mapper.readTree(adfJson);

        String result = service.mapDescription(adfNode);

        assertEquals("Actual description text.", result);
    }

    //AI-GENERATED
    @Test
    void RunManualSync_WithProjectCode_MatchesAndLinksProject() {
        when(configRepository.findById(1L)).thenReturn(Optional.of(config));

        JiraProjectRecord projectRecord = new JiraProjectRecord("SCRUM", "Scrum Project");
        JiraSearchResponseRecord response = new JiraSearchResponseRecord(List.of(
            new JiraIssueRecord("10001", "TEST-1", "https://api/1", new JiraFieldsRecord("Summary", null, null, "2026-05-01T16:06:19.433+02:00", null, null, projectRecord, null))));

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class),
            eq(JiraSearchResponseRecord.class))).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        when(requestRepository.findByJiraIssueKey(anyString())).thenReturn(Optional.empty());
        Request request = new Request();
        request.setRequestID(101L);
        when(issueMapper.toRequest(any())).thenReturn(request);
        when(requestRepository.saveAndFlush(any())).thenReturn(request);

        Project mockProject = new Project();
        mockProject.setId(10L);
        mockProject.setProjectKey("SCRUM");
        mockProject.setRequestCounter(0);
        when(projectRepository.findByProjectKey("SCRUM")).thenReturn(Optional.of(mockProject));

        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class),
            eq(String.class))).thenReturn(new ResponseEntity<>(HttpStatus.NO_CONTENT));

        service.runManualSync(1L);

        verify(projectRepository).findByProjectKey("SCRUM");
        assertEquals(mockProject, request.getProjectID());
        assertEquals("SCRUM-1", request.getRequestKey());
    }

    //AI-GENERATED
    @Test
    void ProcessQueue_SyncJiraItem_UpdatesJiraDescriptionAndTransitions() {
        Request request = new Request();
        request.setRequestID(101L);
        request.setJiraIssueKey("TEST-1");
        request.setJiraIssueUrl("https://test.atlassian.net/browse/TEST-1");
        request.setDescription("Req description");

        WorkflowStep step = new WorkflowStep();
        step.setName("Technical Review");
        request.setCurrentStepID(step);

        JiraSyncQueueItem queueItem = JiraSyncQueueItem.builder()
                .id(1L)
                .request(request)
                .jiraIssueKey("TEST-1")
                .actionType("SYNC_JIRA")
                .status("PENDING")
                .build();

        when(queueItemRepository.findByStatus("PENDING")).thenReturn(List.of(queueItem));
        when(configRepository.findAll()).thenReturn(List.of(config));

        // Mock PUT to update description
        when(restTemplate.exchange(contains("/issue/TEST-1"), eq(HttpMethod.PUT), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        // Mock GET attachments
        when(restTemplate.exchange(contains("fields=attachment"), eq(HttpMethod.GET), any(HttpEntity.class), eq(JsonNode.class)))
                .thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

        // Mock GET available transitions
        String transitionsJson = "{\"transitions\":[{\"id\":\"11\",\"name\":\"Technical Review\",\"to\":{\"name\":\"Technical Review\"}}]}";
        ObjectMapper mapper = new ObjectMapper();
        JsonNode transNode = null;
        try {
            transNode = mapper.readTree(transitionsJson);
        } catch (Exception e) {}
        when(restTemplate.exchange(contains("/transitions"), eq(HttpMethod.GET), any(HttpEntity.class), eq(JsonNode.class)))
                .thenReturn(new ResponseEntity<>(transNode, HttpStatus.OK));

        // Mock POST to execute transition
        when(restTemplate.exchange(contains("/transitions"), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        service.processQueue();

        assertEquals("COMPLETED", queueItem.getStatus());
        verify(queueItemRepository).save(queueItem);
    }

    //AI-GENERATED
    @Test
    void BuildJiraDescriptionPayload_WithLineItems_GeneratesADFTable() {
        Request request = new Request();
        request.setDescription("Requisition for office supplies.");
        
        List<RequestItem> items = new ArrayList<>();
        RequestItem item1 = new RequestItem();
        item1.setName("Notebooks");
        item1.setQuantity(10);
        item1.setUnit("pcs");
        items.add(item1);
        request.setItems(items);

        JsonNode adfPayload = service.buildJiraDescriptionPayload(request);

        assertNotNull(adfPayload);
        assertEquals("doc", adfPayload.path("type").asText());
        JsonNode content = adfPayload.path("content");
        assertTrue(content.isArray());
        
        // Find table node
        boolean foundTable = false;
        for (JsonNode node : content) {
            if ("table".equals(node.path("type").asText())) {
                foundTable = true;
                JsonNode rows = node.path("content");
                assertEquals(2, rows.size()); // Header + 1 data row
            }
        }
        assertTrue(foundTable);
    }
}
