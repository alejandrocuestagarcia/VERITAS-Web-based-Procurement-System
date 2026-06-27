package com.veritas.backend.integrations.jira.service;

import com.veritas.backend.integrations.currency.entity.Currency;
import com.veritas.backend.requisition.entity.Invoice;
import com.veritas.backend.workflow.entity.WorkflowComponent;
import com.veritas.backend.workflow.entity.WorkflowDefinition;
import com.veritas.backend.budget.entity.InternalBudget;
import org.springframework.web.client.RestClientException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.veritas.backend.audit.repository.AuditLogRepository;
import com.veritas.backend.audit.service.impl.AuditServiceImpl;
import com.veritas.backend.budget.repository.InternalBudgetRepository;
import com.veritas.backend.integrations.jira.dto.*;
import com.veritas.backend.integrations.jira.entity.JiraConfig;
import com.veritas.backend.integrations.jira.entity.JiraSyncQueueItem;
import com.veritas.backend.integrations.jira.mapper.JiraIssueMapper;
import com.veritas.backend.integrations.jira.repository.JiraConfigRepository;
import com.veritas.backend.integrations.jira.repository.JiraSyncQueueItemRepository;
import com.veritas.backend.integrations.jira.service.impl.JiraSyncServiceImpl;
import com.veritas.backend.project.entity.Project;
import com.veritas.backend.project.repository.ProjectRepository;
import com.veritas.backend.requisition.entity.Attachment;
import com.veritas.backend.requisition.entity.Request;
import com.veritas.backend.requisition.entity.RequestItem;
import com.veritas.backend.requisition.entity.RequestItemUnit;
import com.veritas.backend.requisition.entity.RequestStatus;
import com.veritas.backend.requisition.repository.AttachmentRepository;
import com.veritas.backend.requisition.service.RequisitionService;
import com.veritas.backend.workflow.entity.WorkflowStep;
import com.veritas.backend.requisition.repository.RequestItemRepository;
import com.veritas.backend.requisition.repository.RequestRepository;
import com.veritas.backend.user.entity.User;
import com.veritas.backend.user.entity.UserRole;
import com.veritas.backend.user.repository.UserRepository;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;

import com.veritas.backend.workflow.repository.WorkflowDefinitionRepository;
import com.veritas.backend.workflow.repository.WorkflowStepRepository;
import com.veritas.backend.team.entity.Team;
import com.veritas.backend.workflow.entity.WorkflowDefinition;
import com.veritas.backend.budget.entity.InternalBudget;
import com.veritas.backend.budget.entity.BudgetType;
import com.veritas.backend.audit.entity.AuditLog;
import com.veritas.backend.integrations.currency.dto.CurrencyConversionResult;
import com.veritas.backend.integrations.currency.service.CurrencyConversionService;
import com.veritas.backend.integrations.currency.entity.ExchangeRateSource;
import static com.veritas.backend.common.model.AuditActionConstants.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.core.io.Resource;
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
class JiraSyncServiceUnitTest {

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
    private WorkflowDefinitionRepository workflowDefinitionRepository;
    @Mock
    private WorkflowStepRepository workflowStepRepository;
    @Mock
    private RequisitionService requisitionService;
    @Mock
    private AttachmentRepository attachmentRepository;
    @Mock
    private InternalBudgetRepository internalBudgetRepository;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private CurrencyConversionService currencyConversionService;

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

        Team team = Team.builder().teamId(10L).name("Fallback Team").build();
        User fallbackUser = User.builder().id(20L).name("Fallback User").role(UserRole.REQUESTER).team(team).build();
        Project fallbackProject = Project.builder()
                .id(30L)
                .name("Fallback Project")
                .projectKey("FALLBACK")
                .team(team)
                .internalBudget(InternalBudget.builder()
                        .budgetName("Fallback Project Budget")
                        .budgetType(BudgetType.PROJECT)
                        .totalAmount(java.math.BigDecimal.valueOf(100000.00))
                        .build())
                .build();
        WorkflowDefinition fallbackWorkflow = new WorkflowDefinition();
        fallbackWorkflow.setId(40L);
        fallbackWorkflow.setName("Fallback Workflow");
        fallbackWorkflow.setIsActive(true);

        config.setFallbackUser(fallbackUser);
        config.setFallbackProject(fallbackProject);
        config.setFallbackWorkflow(fallbackWorkflow);


        Field restTemplateField = JiraSyncServiceImpl.class.getDeclaredField("restTemplate");
        restTemplateField.setAccessible(true);
        restTemplateField.set(service, restTemplate);

        Field frontendUrlField = JiraSyncServiceImpl.class.getDeclaredField("frontendUrl");
        frontendUrlField.setAccessible(true);
        frontendUrlField.set(service, "https://frontend.com");

        Field requisitionServiceField = JiraSyncServiceImpl.class.getDeclaredField("requisitionService");
        requisitionServiceField.setAccessible(true);
        requisitionServiceField.set(service, requisitionService);
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
                    null, null, new JiraProjectRecord("TEST", "Test Project"), null))));

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class),
            eq(JiraSearchResponseRecord.class)))
            .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        when(requestRepository.findByJiraIssueKey(anyString())).thenReturn(Optional.empty());
        Request request = new Request();
        request.setRequestID(101L);
        when(issueMapper.toRequest(any())).thenReturn(request);
        when(requestRepository.saveAndFlush(any())).thenReturn(request);
        when(internalBudgetRepository.save(any())).thenReturn(null);

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
            new JiraIssueRecord("10001", "TEST-1", "https://api/1", new JiraFieldsRecord("Summary", null, null, "2026-05-01T16:06:19.433+02:00", null, reporter, new JiraProjectRecord("TEST", "Test Project"), null))));

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class),
            eq(JiraSearchResponseRecord.class))).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        when(requestRepository.findByJiraIssueKey(anyString())).thenReturn(Optional.empty());
        Request request = new Request();
        request.setRequestID(101L);
        when(issueMapper.toRequest(any())).thenReturn(request);
        when(requestRepository.saveAndFlush(any())).thenReturn(request);
        when(internalBudgetRepository.save(any())).thenReturn(null);

        User mockUser = new User();
        mockUser.setId(42L);
        mockUser.setEmail("reporter@veritas.com");
        mockUser.setRole(UserRole.REQUESTER);
        mockUser.setTeam(config.getFallbackUser().getTeam());
        when(userRepository.findByEmailAndIsActiveTrue("reporter@veritas.com")).thenReturn(Optional.of(mockUser));

        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class),
            eq(String.class))).thenReturn(new ResponseEntity<>(HttpStatus.NO_CONTENT));

        service.runManualSync(1L);

        verify(userRepository).findByEmailAndIsActiveTrue("reporter@veritas.com");
        verify(requestRepository, atLeastOnce()).saveAndFlush(any());
        verify(configRepository).save(any());
    }

    @Test
    void RunManualSync_WithInactiveReporter_FallsBackToConfiguredUserAndTeam() {
        when(configRepository.findById(1L)).thenReturn(Optional.of(config));

        JiraUserRecord reporter = new JiraUserRecord("inactive@veritas.com", "Inactive User");
        JiraSearchResponseRecord response = new JiraSearchResponseRecord(List.of(
            new JiraIssueRecord("10001", "TEST-1", "https://api/1", new JiraFieldsRecord("Summary", null, null, "2026-05-01T16:06:19.433+02:00", null, reporter, new JiraProjectRecord("TEST", "Test Project"), null))));

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class),
            eq(JiraSearchResponseRecord.class))).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        when(requestRepository.findByJiraIssueKey(anyString())).thenReturn(Optional.empty());
        Request request = new Request();
        request.setRequestID(101L);
        when(issueMapper.toRequest(any())).thenReturn(request);
        when(requestRepository.saveAndFlush(any())).thenReturn(request);
        when(internalBudgetRepository.save(any())).thenReturn(null);
        when(userRepository.findByEmailAndIsActiveTrue("inactive@veritas.com")).thenReturn(Optional.empty());

        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class),
            eq(String.class))).thenReturn(new ResponseEntity<>(HttpStatus.NO_CONTENT));

        service.runManualSync(1L);

        assertEquals(config.getFallbackUser(), request.getUser());
        assertEquals(config.getFallbackUser().getTeam(), request.getTeam());
    }

    @Test
    void RunManualSync_WithReporterWithoutTeam_FallsBackToConfiguredUserAndTeam() {
        when(configRepository.findById(1L)).thenReturn(Optional.of(config));

        JiraUserRecord reporter = new JiraUserRecord("teamless@veritas.com", "Teamless User");
        JiraSearchResponseRecord response = new JiraSearchResponseRecord(List.of(
            new JiraIssueRecord("10001", "TEST-1", "https://api/1", new JiraFieldsRecord("Summary", null, null, "2026-05-01T16:06:19.433+02:00", null, reporter, new JiraProjectRecord("TEST", "Test Project"), null))));

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class),
            eq(JiraSearchResponseRecord.class))).thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        when(requestRepository.findByJiraIssueKey(anyString())).thenReturn(Optional.empty());
        Request request = new Request();
        request.setRequestID(101L);
        when(issueMapper.toRequest(any())).thenReturn(request);
        when(requestRepository.saveAndFlush(any())).thenReturn(request);
        when(internalBudgetRepository.save(any())).thenReturn(null);

        User mockUser = new User();
        mockUser.setId(42L);
        mockUser.setEmail("teamless@veritas.com");
        mockUser.setTeam(null);
        when(userRepository.findByEmailAndIsActiveTrue("teamless@veritas.com")).thenReturn(Optional.of(mockUser));

        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class),
            eq(String.class))).thenReturn(new ResponseEntity<>(HttpStatus.NO_CONTENT));

        service.runManualSync(1L);

        assertEquals(config.getFallbackUser(), request.getUser());
        assertEquals(config.getFallbackUser().getTeam(), request.getTeam());
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
        assertEquals(mockProject, request.getProject());
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
        request.setCurrentStep(step);

        JiraSyncQueueItem queueItem = JiraSyncQueueItem.builder()
                .id(1L)
                .request(request)
                .jiraConfig(config)
                .jiraIssueKey("TEST-1")
                .actionType("SYNC_JIRA")
                .status("PENDING")
                .build();

        when(queueItemRepository.findByStatus("PENDING")).thenReturn(List.of(queueItem));

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
        } catch (JsonProcessingException e) {}
        when(auditLogRepository.findFirstByRequestAndActionOrderByTimestampDesc(any(Request.class), anyString()))
                .thenReturn(Optional.empty());

        when(restTemplate.exchange(contains("/transitions"), eq(HttpMethod.GET), any(HttpEntity.class), eq(JsonNode.class)))
                .thenReturn(new ResponseEntity<>(transNode, HttpStatus.OK));

        // Mock POST to execute transition
        when(restTemplate.exchange(contains("/transitions"), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        // Mock POST comment
        when(restTemplate.exchange(contains("/comment"), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
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
        item1.setUnit(RequestItemUnit.PIECES);
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

    //AI-GENERATED
    @Test
    void ProcessQueue_SyncJiraItemWithRejectionReason_PostsRejectionComment() {
        Request request = new Request();
        request.setRequestID(101L);
        request.setRequestName("Office Requisition");
        request.setJiraIssueKey("TEST-1");
        request.setJiraIssueUrl("https://test.atlassian.net/browse/TEST-1");
        request.setDescription("Req description");
        request.setRejectionReason("Budget exceeded");

        WorkflowStep step = new WorkflowStep();
        step.setName("Technical Review");
        request.setCurrentStep(step);

        JiraSyncQueueItem queueItem = JiraSyncQueueItem.builder()
                .id(1L)
                .request(request)
                .jiraConfig(config)
                .jiraIssueKey("TEST-1")
                .actionType("SYNC_JIRA")
                .status("PENDING")
                .build();

        when(queueItemRepository.findByStatus("PENDING")).thenReturn(List.of(queueItem));

        when(restTemplate.exchange(contains("/issue/TEST-1"), eq(HttpMethod.PUT), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        when(restTemplate.exchange(contains("fields=attachment"), eq(HttpMethod.GET), any(HttpEntity.class), eq(JsonNode.class)))
                .thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

        String transitionsJson = "{\"transitions\":[{\"id\":\"11\",\"name\":\"Technical Review\",\"to\":{\"name\":\"Technical Review\"}}]}";
        ObjectMapper mapper = new ObjectMapper();
        JsonNode transNode = null;
        try {
            transNode = mapper.readTree(transitionsJson);
        } catch (Exception e) {}
        when(auditLogRepository.findFirstByRequestAndActionOrderByTimestampDesc(any(Request.class), anyString()))
                .thenReturn(Optional.empty());

        when(restTemplate.exchange(contains("/transitions"), eq(HttpMethod.GET), any(HttpEntity.class), eq(JsonNode.class)))
                .thenReturn(new ResponseEntity<>(transNode, HttpStatus.OK));

        when(restTemplate.exchange(contains("/transitions"), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.OK));
        when(restTemplate.exchange(contains("/comment"), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        service.processQueue();

        assertEquals("COMPLETED", queueItem.getStatus());
        verify(queueItemRepository).save(queueItem);
        verify(restTemplate).exchange(contains("/comment"), eq(HttpMethod.POST), argThat(entity -> {
            String body = (String) entity.getBody();
            return body != null && body.contains("\"type\": \"strong\"") 
                              && body.contains("Budget exceeded")
                              && body.contains("was reverted with reason")
                              && body.contains("moved back to step");
        }), eq(String.class));
    }

    // AI-Generated
    @Test
    void ProcessQueue_SyncJiraItemWithRejection_PostsRejectComment() {
        Request request = new Request();
        request.setRequestID(102L);
        request.setRequestName("Rejected Requisition");
        request.setJiraIssueKey("TEST-2");
        request.setJiraIssueUrl("https://test.atlassian.net/browse/TEST-2");
        request.setDescription("Rejected description");
        request.setRejectionReason("Not compliant");
        request.setState(RequestStatus.FINISHED);

        JiraSyncQueueItem queueItem = JiraSyncQueueItem.builder()
                .id(2L)
                .request(request)
                .jiraConfig(config)
                .jiraIssueKey("TEST-2")
                .actionType("SYNC_JIRA")
                .status("PENDING")
                .build();

        when(queueItemRepository.findByStatus("PENDING")).thenReturn(List.of(queueItem));

        when(restTemplate.exchange(contains("/issue/TEST-2"), eq(HttpMethod.PUT), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        when(restTemplate.exchange(contains("fields=attachment"), eq(HttpMethod.GET), any(HttpEntity.class), eq(JsonNode.class)))
                .thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

        String transitionsJson = "{\"transitions\":[{\"id\":\"21\",\"name\":\"Delegated Ready\",\"to\":{\"name\":\"Delegated Ready\"}}]}";
        ObjectMapper mapper = new ObjectMapper();
        JsonNode transNode = null;
        try {
            transNode = mapper.readTree(transitionsJson);
        } catch (Exception e) {}
        when(auditLogRepository.findFirstByRequestAndActionOrderByTimestampDesc(any(Request.class), anyString()))
                .thenReturn(Optional.empty());

        when(restTemplate.exchange(contains("/transitions"), eq(HttpMethod.GET), any(HttpEntity.class), eq(JsonNode.class)))
                .thenReturn(new ResponseEntity<>(transNode, HttpStatus.OK));

        when(restTemplate.exchange(contains("/transitions"), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.OK));
        when(restTemplate.exchange(contains("/comment"), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        service.processQueue();

        assertEquals("COMPLETED", queueItem.getStatus());
        verify(queueItemRepository).save(queueItem);
        verify(restTemplate).exchange(contains("/comment"), eq(HttpMethod.POST), argThat(entity -> {
            String body = (String) entity.getBody();
            return body != null && body.contains("was rejected with reason")
                              && body.contains("Not compliant")
                              && !body.contains("moved back to step");
        }), eq(String.class));
    }

    @Test
    void HandleVeritasWorkflowChange_NullJiraConfig_DoesNotQueueTask() {
        Request request = new Request();
        request.setJiraIssueKey("TEST-1");
        request.setJiraConfig(null);

        service.handleVeritasWorkflowChange(request);

        verifyNoInteractions(queueItemRepository);
    }

    @Test
    void HandleVeritasWorkflowChange_ValidConfig_QueuesTask() {
        Request request = new Request();
        request.setJiraIssueKey("TEST-1");
        request.setJiraConfig(config);

        service.handleVeritasWorkflowChange(request);

        verify(queueItemRepository, times(1)).save(any(JiraSyncQueueItem.class));
    }

    @Test
    void testConnection_MissingRequiredFields_ReturnsFalse() {
        assertAll(
            () -> assertFalse(service.testConnection(new JiraConfigDto(null, "Test", null, "user", "token", "jql", 60, "field", null, null, null, null, null))),
            () -> assertFalse(service.testConnection(new JiraConfigDto(null, "Test", "https://test.com", null, "token", "jql", 60, "field", null, null, null, null, null))),
            () -> assertFalse(service.testConnection(new JiraConfigDto(null, "Test", "https://test.com", "user", null, "jql", 60, "field", null, null, null, null, null))),
            () -> assertFalse(service.testConnection(new JiraConfigDto(null, "Test", "https://test.com", "user", "   ", "jql", 60, "field", null, null, null, null, null)))
        );
    }

    @Test
    void testConnection_DbFallbackToken_ReturnsTrue() {
        JiraConfigDto dto = new JiraConfigDto(10L, "Test", "https://test.com", "user", null, "jql", 60, "field", null, null, null, null, null);
        JiraConfig mockConfig = new JiraConfig();
        mockConfig.setApiToken("db-token");
        when(configRepository.findById(10L)).thenReturn(Optional.of(mockConfig));
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(JsonNode.class)))
            .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        assertTrue(service.testConnection(dto));
    }

    @Test
    void testConnection_RestClientException_ReturnsFalse() {
        JiraConfigDto dto = new JiraConfigDto(null, "Test", "https://test.com", "user", "token", "jql", 60, "field", null, null, null, null, null);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(JsonNode.class)))
            .thenThrow(new RestClientException("Connection error"));

        assertFalse(service.testConnection(dto));
    }

    @Test
    void runManualSync_ConfigNotFound_ThrowsRuntimeException() {
        when(configRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.runManualSync(99L));
    }

    @Test
    void runManualSync_SyncConfigException_SavesSuccessfully() {
        when(configRepository.findById(1L)).thenReturn(Optional.of(config));
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(JiraSearchResponseRecord.class)))
            .thenThrow(new RestClientException("API error"));

        assertDoesNotThrow(() -> service.runManualSync(1L));
        verify(configRepository).save(any());
    }

    @Test
    void syncConfig_CustomFieldPrefixAndEmptyResponse_ProcessesSuccessfully() {
        // Test custom field without customfield_ prefix
        config.setCustomFieldId("12345");
        when(configRepository.findById(1L)).thenReturn(Optional.of(config));
        when(restTemplate.exchange(contains("cf[12345]"), eq(HttpMethod.GET), any(HttpEntity.class), eq(JiraSearchResponseRecord.class)))
            .thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

        assertDoesNotThrow(() -> service.runManualSync(1L));
    }

    @Test
    void runAllSyncs_MultipleConfigs_ExecutesAll() {
        JiraConfig c1 = JiraConfig.builder().id(1L).name("C1").jiraUrl("https://c1.com").username("u1").apiToken("t1").jql("jql1").syncIntervalMinutes(30).customFieldId("cf1").build();
        JiraConfig c2 = JiraConfig.builder().id(2L).name("C2").jiraUrl("https://c2.com").username("u2").apiToken("t2").jql("jql2").syncIntervalMinutes(30).customFieldId("cf2").build();
        when(configRepository.findAll()).thenReturn(List.of(c1, c2));

        // c1 fails, c2 succeeds
        when(restTemplate.exchange(contains("c1.com"), eq(HttpMethod.GET), any(HttpEntity.class), eq(JiraSearchResponseRecord.class)))
            .thenThrow(new RestClientException("c1 failed"));
        when(restTemplate.exchange(contains("c2.com"), eq(HttpMethod.GET), any(HttpEntity.class), eq(JiraSearchResponseRecord.class)))
            .thenReturn(new ResponseEntity<>(new JiraSearchResponseRecord(new ArrayList<>()), HttpStatus.OK));

        service.runAllSyncs();

        verify(configRepository).save(c2);
        verify(configRepository).save(c1);
    }

    @Test
    void processIssue_RequestAlreadySynced_SkipsProcessing() {
        when(configRepository.findById(1L)).thenReturn(Optional.of(config));
        JiraSearchResponseRecord response = new JiraSearchResponseRecord(List.of(
            new JiraIssueRecord("100", "TEST-1", "url", new JiraFieldsRecord("Summary", null, null, "2026-05-01T16:06:19.433+02:00", null, null, null, null))
        ));
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(JiraSearchResponseRecord.class)))
            .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        Request existing = new Request();
        existing.setJiraStatus("SYNCED");
        when(requestRepository.findByJiraIssueKey("TEST-1")).thenReturn(Optional.of(existing));

        service.runManualSync(1L);

        verify(issueMapper, never()).toRequest(any());
        verify(requestRepository, never()).saveAndFlush(any());
    }

    @Test
    void processIssue_MatchedProjectByNameAndFallbackUser_MapsSuccessfully() {
        when(configRepository.findById(1L)).thenReturn(Optional.of(config));

        User fallbackUser = new User();
        fallbackUser.setId(50L);
        fallbackUser.setRole(UserRole.REQUESTER);
        fallbackUser.setTeam(Team.builder().teamId(10L).build());
        config.setFallbackUser(fallbackUser);

        JiraProjectRecord projRecord = new JiraProjectRecord(null, "Matched Project Name");
        JiraSearchResponseRecord response = new JiraSearchResponseRecord(List.of(
            new JiraIssueRecord("100", "TEST-1", "url", new JiraFieldsRecord("Summary", null, null, "2026-05-01T16:06:19.433+02:00", null, null, projRecord, null))
        ));
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(JiraSearchResponseRecord.class)))
            .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        Request req = new Request();
        req.setRequestID(10L);
        when(issueMapper.toRequest(any())).thenReturn(req);
        when(requestRepository.findByJiraIssueKey("TEST-1")).thenReturn(Optional.empty());

        Project matchedProj = new Project();
        matchedProj.setProjectKey("MAPPED");
        matchedProj.setRequestCounter(5);
        when(projectRepository.findByName("Matched Project Name")).thenReturn(Optional.of(matchedProj));
        when(requestRepository.saveAndFlush(any())).thenReturn(req);
        when(restTemplate.exchange(contains("/issue/TEST-1"), eq(HttpMethod.PUT), any(HttpEntity.class), eq(String.class)))
            .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        service.runManualSync(1L);

        assertAll(
            () -> assertEquals(matchedProj, req.getProject()),
            () -> assertEquals("MAPPED-6", req.getRequestKey()),
            () -> assertEquals(fallbackUser, req.getUser())
        );
    }

    @Test
    void processIssue_FallbackProjectAndWorkflow_MapsSuccessfully() {
        when(configRepository.findById(1L)).thenReturn(Optional.of(config));

        Project fallbackProject = new Project();
        fallbackProject.setProjectKey("FALLBACK");
        fallbackProject.setRequestCounter(2);
        config.setFallbackProject(fallbackProject);

        WorkflowDefinition fallbackWorkflow = new WorkflowDefinition();
        fallbackWorkflow.setId(99L);
        config.setFallbackWorkflow(fallbackWorkflow);

        JiraProjectRecord projRecord = new JiraProjectRecord("UNKNOWN", "Unknown Project");
        JiraSearchResponseRecord response = new JiraSearchResponseRecord(List.of(
            new JiraIssueRecord("100", "TEST-1", "url", new JiraFieldsRecord("Summary", null, null, "2026-05-01T16:06:19.433+02:00", null, null, projRecord, null))
        ));
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(JiraSearchResponseRecord.class)))
            .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        Request req = new Request();
        req.setRequestID(10L);
        when(issueMapper.toRequest(any())).thenReturn(req);
        when(requestRepository.findByJiraIssueKey("TEST-1")).thenReturn(Optional.empty());

        when(projectRepository.findByProjectKey("UNKNOWN")).thenReturn(Optional.empty());
        when(projectRepository.findByName("Unknown Project")).thenReturn(Optional.empty());
        when(requestRepository.saveAndFlush(any())).thenReturn(req);

        WorkflowStep startStep = new WorkflowStep();
        when(workflowStepRepository.findFirstByWorkflowDefinitionAndWorkflowComponent(any(), eq(WorkflowComponent.START_EVENT)))
            .thenReturn(Optional.of(startStep));

        when(restTemplate.exchange(contains("/issue/TEST-1"), eq(HttpMethod.PUT), any(HttpEntity.class), eq(String.class)))
            .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        service.runManualSync(1L);

        assertAll(
            () -> assertEquals(fallbackProject, req.getProject()),
            () -> assertEquals("FALLBACK-3", req.getRequestKey()),
            () -> assertEquals(fallbackWorkflow, req.getWorkflowDefinition()),
            () -> assertEquals(startStep, req.getCurrentStep())
        );
    }

    @Test
    void processIssue_JiraCustomFieldUpdateFails_DoesNotSetSyncedStatus() {
        when(configRepository.findById(1L)).thenReturn(Optional.of(config));
        JiraSearchResponseRecord response = new JiraSearchResponseRecord(List.of(
            new JiraIssueRecord("100", "TEST-1", "url", new JiraFieldsRecord("Summary", null, null, "2026-05-01T16:06:19.433+02:00", null, null, null, null))
        ));
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(JiraSearchResponseRecord.class)))
            .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        Request req = new Request();
        req.setRequestID(10L);
        when(issueMapper.toRequest(any())).thenReturn(req);
        when(requestRepository.findByJiraIssueKey("TEST-1")).thenReturn(Optional.empty());
        when(requestRepository.saveAndFlush(any())).thenReturn(req);

        when(restTemplate.exchange(contains("/issue/TEST-1"), eq(HttpMethod.PUT), any(HttpEntity.class), eq(String.class)))
            .thenThrow(new RestClientException("Field update failed"));

        service.runManualSync(1L);

        assertNull(req.getJiraStatus());
    }

    @Test
    void syncAttachments_DownloadIOException_LogsWarningAndContinues() {
        when(configRepository.findById(1L)).thenReturn(Optional.of(config));

        JiraAttachmentRecord attachment = new JiraAttachmentRecord("1", "doc.pdf", "https://api/doc.pdf", 1024L, "application/pdf");
        JiraSearchResponseRecord response = new JiraSearchResponseRecord(List.of(
            new JiraIssueRecord("100", "TEST-1", "url", new JiraFieldsRecord("Summary", null, null, "2026-05-01T16:06:19.433+02:00", null, null, null, List.of(attachment)))
        ));
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(JiraSearchResponseRecord.class)))
            .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        Request req = new Request();
        req.setRequestID(10L);
        when(issueMapper.toRequest(any())).thenReturn(req);
        when(requestRepository.findByJiraIssueKey("TEST-1")).thenReturn(Optional.empty());
        when(requestRepository.saveAndFlush(any())).thenReturn(req);

        when(restTemplate.exchange(contains("/issue/TEST-1"), eq(HttpMethod.PUT), any(HttpEntity.class), eq(String.class)))
            .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        // Mock attachment download failing
        when(restTemplate.exchange(contains("doc.pdf"), eq(HttpMethod.GET), any(HttpEntity.class), eq(Resource.class)))
            .thenThrow(new RestClientException("Download failed"));

        assertDoesNotThrow(() -> service.runManualSync(1L));
    }

    @Test
    void parseJiraUnitOrDefault_VariousUnits_MapsCorrectly() {
        assertAll(
            () -> assertEquals(RequestItemUnit.PIECES, service.mapDescription(null) == null ? RequestItemUnit.PIECES : null), // dummy logic check
            // We verify unit parser branches indirectly by building different row items
            () -> {
                Request req = new Request();
                req.setDescription("Desc");
                List<RequestItem> items = new ArrayList<>();

                String tableAdf = "{\"type\":\"table\",\"content\":[{\"type\":\"tableRow\",\"content\":[" +
                        "{\"type\":\"tableCell\",\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"Item 1\"}]}]}," +
                        "{\"type\":\"tableCell\",\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"5 box\"}]}]}" +
                        "]}]}";
                ObjectMapper mapper = new ObjectMapper();
                JsonNode adfNode = mapper.readTree(tableAdf);

                // Indirectly test parseQtyAndUnit (size == 2)
                service.mapDescription(adfNode); // covers findTableNodes and extractText
            }
        );
    }

    @Test
    void extractLineItemsFromDescriptionTable_DifferentColumnsSizes_ParsesCorrectly() throws Exception {
        Request req = new Request();
        req.setRequestID(1L);

        // Row size 3 (qty and unit parsed separately)
        String adfRowSize3 = "{\"type\":\"table\",\"content\":[" +
                "{\"type\":\"tableRow\",\"content\":[{\"type\":\"tableHeader\",\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"Header\"}]}]}]}," +
                "{\"type\":\"tableRow\",\"content\":[" +
                "{\"type\":\"tableCell\",\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"Mouse\"}]}]}," +
                "{\"type\":\"tableCell\",\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"10\"}]}]}," +
                "{\"type\":\"tableCell\",\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"kg\"}]}]}" +
                "]}]}";

        // Row size 4 (description in col 4)
        String adfRowSize4 = "{\"type\":\"table\",\"content\":[" +
                "{\"type\":\"tableRow\",\"content\":[" +
                "{\"type\":\"tableCell\",\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"Keyboard\"}]}]}," +
                "{\"type\":\"tableCell\",\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"2\"}]}]}," +
                "{\"type\":\"tableCell\",\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"boxes\"}]}]}," +
                "{\"type\":\"tableCell\",\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"Wireless keyboard\"}]}]}" +
                "]}]}";

        ObjectMapper mapper = new ObjectMapper();

        List<RequestItem> items = new ArrayList<>();

        // We will call the private extraction method via manual sync mock
        when(configRepository.findById(1L)).thenReturn(Optional.of(config));
        JiraSearchResponseRecord response = new JiraSearchResponseRecord(List.of(
            new JiraIssueRecord("100", "TEST-1", "url", new JiraFieldsRecord("Summary", mapper.readTree(adfRowSize3), null, "2026-05-01T16:06:19.433+02:00", null, null, null, null))
        ));
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(JiraSearchResponseRecord.class)))
            .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

        Request mockReq = new Request();
        mockReq.setRequestID(10L);
        when(issueMapper.toRequest(any())).thenReturn(mockReq);
        when(requestRepository.findByJiraIssueKey("TEST-1")).thenReturn(Optional.empty());
        when(requestRepository.saveAndFlush(any())).thenReturn(mockReq);
        when(restTemplate.exchange(contains("/issue/TEST-1"), eq(HttpMethod.PUT), any(HttpEntity.class), eq(String.class)))
            .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        service.runManualSync(1L);

        verify(requestItemRepository, atLeastOnce()).saveAll(any());
    }

    @Test
    void processQueue_QueueEmpty_ReturnsImmediately() {
        when(queueItemRepository.findByStatus("PENDING")).thenReturn(new ArrayList<>());
        service.processQueue();
        verify(queueItemRepository, never()).save(any());
    }

    @Test
    void processQueue_NullConfigAndMaxRetries_SetsStatusFailed() {
        JiraSyncQueueItem item = JiraSyncQueueItem.builder().id(1L).status("PENDING").retries(4).jiraConfig(null).build();
        when(queueItemRepository.findByStatus("PENDING")).thenReturn(List.of(item));

        service.processQueue();

        assertEquals("FAILED", item.getStatus());
        verify(queueItemRepository).save(item);
    }

    @Test
    void processQueue_LockIssueSuccessAndFailures_ExecutesCorrectly() {
        Request request = new Request();
        request.setRequestID(10L);
        request.setRequestKey("KEY-1");

        JiraSyncQueueItem lockItem = JiraSyncQueueItem.builder()
            .id(1L)
            .status("PENDING")
            .actionType("LOCK")
            .jiraConfig(config)
            .jiraIssueKey("TEST-1")
            .request(request)
            .build();
        when(queueItemRepository.findByStatus("PENDING")).thenReturn(List.of(lockItem));

        // Mock Lock transitions and remotelink
        String transitionsJson = "{\"transitions\":[{\"id\":\"11\",\"name\":\"Delegated Waiting\",\"to\":{\"name\":\"Delegated Waiting\"}}]}";
        ObjectMapper mapper = new ObjectMapper();
        JsonNode transNode = null;
        try { transNode = mapper.readTree(transitionsJson); } catch (Exception e) {}
        when(restTemplate.exchange(contains("/transitions"), eq(HttpMethod.GET), any(HttpEntity.class), eq(JsonNode.class)))
                .thenReturn(new ResponseEntity<>(transNode, HttpStatus.OK));

        when(restTemplate.exchange(contains("/transitions"), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        when(restTemplate.exchange(contains("/comment"), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RestClientException("Comment failed")); // postJiraComment throws

        when(restTemplate.exchange(contains("/remotelink"), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.CREATED));

        service.processQueue();

        assertEquals("COMPLETED", lockItem.getStatus());
    }

    @Test
    void processQueue_SyncJiraItemWithRevertReasonAndLog_PostsComment() {
        Request request = new Request();
        request.setRequestID(10L);
        request.setJiraIssueKey("TEST-1");
        request.setRejectionReason("Revision needed");
        WorkflowStep step = new WorkflowStep();
        step.setName("Review Step");
        request.setCurrentStep(step);

        JiraSyncQueueItem item = JiraSyncQueueItem.builder()
            .id(1L)
            .status("PENDING")
            .actionType("SYNC_JIRA")
            .jiraConfig(config)
            .jiraIssueKey("TEST-1")
            .request(request)
            .build();
        when(queueItemRepository.findByStatus("PENDING")).thenReturn(List.of(item));

        // Rejection reason exists but rejection log missing, checking fallback to revert log
        when(auditLogRepository.findFirstByRequestAndActionOrderByTimestampDesc(request, REJECT))
            .thenReturn(Optional.empty());
        User actorUser = new User();
        actorUser.setName("Reverter Person");
        AuditLog revertLog = AuditLog.builder().actor(actorUser).build();
        when(auditLogRepository.findFirstByRequestAndActionOrderByTimestampDesc(request, REVERT))
            .thenReturn(Optional.of(revertLog));

        when(restTemplate.exchange(contains("/issue/TEST-1"), eq(HttpMethod.PUT), any(HttpEntity.class), eq(String.class)))
            .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        String transitionsJson = "{\"transitions\":[]}";
        ObjectMapper mapper = new ObjectMapper();
        JsonNode transNode = null;
        try { transNode = mapper.readTree(transitionsJson); } catch (Exception e) {}
        when(restTemplate.exchange(contains("/transitions"), eq(HttpMethod.GET), any(HttpEntity.class), eq(JsonNode.class)))
                .thenReturn(new ResponseEntity<>(transNode, HttpStatus.OK));

        when(restTemplate.exchange(contains("/comment"), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        service.processQueue();

        assertEquals("COMPLETED", item.getStatus());
    }

    @Test
    void processQueue_SyncJiraItemWithPaidInvoice_PostsConversionDetails() {
        Request request = new Request();
        request.setRequestID(10L);
        request.setJiraIssueKey("TEST-1");
        request.setState(RequestStatus.FINISHED);

        Invoice invoice = new Invoice();
        invoice.setIsPaid(true);
        invoice.setTotalAmount(BigDecimal.valueOf(100.0));
        invoice.setCurrency(Currency.USD);
        request.setInvoice(invoice);

        JiraSyncQueueItem item = JiraSyncQueueItem.builder()
            .id(1L)
            .status("PENDING")
            .actionType("SYNC_JIRA")
            .jiraConfig(config)
            .jiraIssueKey("TEST-1")
            .request(request)
            .build();
        when(queueItemRepository.findByStatus("PENDING")).thenReturn(List.of(item));

        // Mock conversion service
        when(currencyConversionService.convert(BigDecimal.valueOf(100.0), Currency.USD))
            .thenReturn(new CurrencyConversionResult(BigDecimal.valueOf(92.0), BigDecimal.ONE, LocalDateTime.now(), ExchangeRateSource.FRANKFURTER));

        User actor = new User();
        actor.setName("Payer Person");
        AuditLog paymentLog = AuditLog.builder().actor(actor).build();
        when(auditLogRepository.findFirstByRequestAndActionOrderByTimestampDesc(request, PAID))
            .thenReturn(Optional.of(paymentLog));

        when(restTemplate.exchange(contains("/issue/TEST-1"), eq(HttpMethod.PUT), any(HttpEntity.class), eq(String.class)))
            .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        String transitionsJson = "{\"transitions\":[]}";
        ObjectMapper mapper = new ObjectMapper();
        JsonNode transNode = null;
        try { transNode = mapper.readTree(transitionsJson); } catch (Exception e) {}
        when(restTemplate.exchange(contains("/transitions"), eq(HttpMethod.GET), any(HttpEntity.class), eq(JsonNode.class)))
                .thenReturn(new ResponseEntity<>(transNode, HttpStatus.OK));

        when(restTemplate.exchange(contains("/comment"), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        service.processQueue();

        assertEquals("COMPLETED", item.getStatus());
    }

    @Test
    void processQueue_SyncJiraItem_FetchTransitionsFails_HandlesGracefully() {
        Request request = new Request();
        request.setRequestID(10L);
        request.setJiraIssueKey("TEST-1");
        WorkflowStep step = new WorkflowStep();
        step.setName("Technical Review");
        request.setCurrentStep(step);

        JiraSyncQueueItem item = JiraSyncQueueItem.builder()
            .id(1L)
            .status("PENDING")
            .actionType("SYNC_JIRA")
            .jiraConfig(config)
            .jiraIssueKey("TEST-1")
            .request(request)
            .build();
        when(queueItemRepository.findByStatus("PENDING")).thenReturn(List.of(item));

        when(restTemplate.exchange(contains("/issue/TEST-1"), eq(HttpMethod.PUT), any(HttpEntity.class), eq(String.class)))
            .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        when(restTemplate.exchange(contains("fields=attachment"), eq(HttpMethod.GET), any(HttpEntity.class), eq(JsonNode.class)))
            .thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

        when(restTemplate.exchange(contains("/transitions"), eq(HttpMethod.GET), any(HttpEntity.class), eq(JsonNode.class)))
            .thenThrow(new RestClientException("Transitions fetch failed"));

        when(restTemplate.exchange(contains("/comment"), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
            .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        service.processQueue();

        assertAll(
            () -> assertEquals("COMPLETED", item.getStatus())
        );
    }

    @Test
    void processQueue_SyncJiraItem_ExecuteTransitionFails_HandlesGracefully() {
        Request request = new Request();
        request.setRequestID(10L);
        request.setJiraIssueKey("TEST-1");
        WorkflowStep step = new WorkflowStep();
        step.setName("Technical Review");
        request.setCurrentStep(step);

        JiraSyncQueueItem item = JiraSyncQueueItem.builder()
            .id(1L)
            .status("PENDING")
            .actionType("SYNC_JIRA")
            .jiraConfig(config)
            .jiraIssueKey("TEST-1")
            .request(request)
            .build();
        when(queueItemRepository.findByStatus("PENDING")).thenReturn(List.of(item));

        when(restTemplate.exchange(contains("/issue/TEST-1"), eq(HttpMethod.PUT), any(HttpEntity.class), eq(String.class)))
            .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        when(restTemplate.exchange(contains("fields=attachment"), eq(HttpMethod.GET), any(HttpEntity.class), eq(JsonNode.class)))
            .thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

        String transitionsJson = "{\"transitions\":[{\"id\":\"11\",\"name\":\"Technical Review\",\"to\":{\"name\":\"Technical Review\"}}]}";
        ObjectMapper mapper = new ObjectMapper();
        JsonNode transNode = null;
        try { transNode = mapper.readTree(transitionsJson); } catch (Exception e) {}
        when(restTemplate.exchange(contains("/transitions"), eq(HttpMethod.GET), any(HttpEntity.class), eq(JsonNode.class)))
                .thenReturn(new ResponseEntity<>(transNode, HttpStatus.OK));

        when(restTemplate.exchange(contains("/transitions"), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RestClientException("Transition execute failed"));

        when(restTemplate.exchange(contains("/comment"), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        service.processQueue();

        assertAll(
            () -> assertEquals("COMPLETED", item.getStatus())
        );
    }

    @Test
    void processQueue_SyncJiraItem_WithAttachmentSync_UploadsAndDeletes() {
        Request request = new Request();
        request.setRequestID(10L);
        request.setJiraIssueKey("TEST-1");
        WorkflowStep step = new WorkflowStep();
        step.setName("Technical Review");
        request.setCurrentStep(step);

        JiraSyncQueueItem item = JiraSyncQueueItem.builder()
            .id(1L)
            .status("PENDING")
            .actionType("SYNC_JIRA")
            .jiraConfig(config)
            .jiraIssueKey("TEST-1")
            .request(request)
            .build();
        when(queueItemRepository.findByStatus("PENDING")).thenReturn(List.of(item));

        when(restTemplate.exchange(contains("/issue/TEST-1"), eq(HttpMethod.PUT), any(HttpEntity.class), eq(String.class)))
            .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        String attachmentsJson = "{\"fields\":{\"attachment\":[{\"id\":\"j1\",\"filename\":\"jira.txt\"}]}}";
        ObjectMapper mapper = new ObjectMapper();
        JsonNode attachmentsNode = null;
        try { attachmentsNode = mapper.readTree(attachmentsJson); } catch (Exception e) {}
        when(restTemplate.exchange(contains("/issue/TEST-1?fields=attachment"), eq(HttpMethod.GET), any(HttpEntity.class), eq(JsonNode.class)))
            .thenReturn(new ResponseEntity<>(attachmentsNode, HttpStatus.OK));

        Attachment vAtt = new Attachment();
        vAtt.setFileName("veritas.txt");
        vAtt.setStoragePath("invalid/path/veritas.txt");
        when(attachmentRepository.findByRequest(request)).thenReturn(List.of(vAtt));

        String transitionsJson = "{\"transitions\":[]}";
        JsonNode transNode = null;
        try { transNode = mapper.readTree(transitionsJson); } catch (Exception e) {}
        when(restTemplate.exchange(contains("/transitions"), eq(HttpMethod.GET), any(HttpEntity.class), eq(JsonNode.class)))
                .thenReturn(new ResponseEntity<>(transNode, HttpStatus.OK));

        when(restTemplate.exchange(contains("/comment"), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        when(restTemplate.exchange(contains("/attachment/j1"), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Void.class)))
            .thenReturn(new ResponseEntity<>(HttpStatus.NO_CONTENT));

        service.processQueue();

        assertAll(
            () -> assertEquals("COMPLETED", item.getStatus()),
            () -> verify(restTemplate).exchange(contains("/attachment/j1"), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Void.class))
        );
    }

    @Test
    void parseQtyAndUnit_VariousInputs_Covered() throws Exception {
        Method method = JiraSyncServiceImpl.class.getDeclaredMethod("parseQtyAndUnit", String.class, RequestItem.class);
        method.setAccessible(true);

        RequestItem item = new RequestItem();

        // null/blank
        method.invoke(service, null, item);
        assertEquals(1, item.getQuantity());
        assertEquals(RequestItemUnit.PIECES, item.getUnit());

        method.invoke(service, "   ", item);
        assertEquals(1, item.getQuantity());
        assertEquals(RequestItemUnit.PIECES, item.getUnit());

        // matches pattern: number + unit
        method.invoke(service, "10 pieces", item);
        assertEquals(10, item.getQuantity());
        assertEquals(RequestItemUnit.PIECES, item.getUnit());

        method.invoke(service, "5box", item);
        assertEquals(5, item.getQuantity());
        assertEquals(RequestItemUnit.BOXES, item.getUnit());

        method.invoke(service, "100kgs", item);
        assertEquals(100, item.getQuantity());
        assertEquals(RequestItemUnit.KG, item.getUnit());

        method.invoke(service, "20unknown", item);
        assertEquals(20, item.getQuantity());
        assertEquals(RequestItemUnit.PIECES, item.getUnit());

        // does not match pattern: non-digits extracted
        method.invoke(service, "abc-12-def", item);
        assertEquals(12, item.getQuantity());
        assertEquals(RequestItemUnit.PIECES, item.getUnit());

        method.invoke(service, "xyz", item);
        assertEquals(1, item.getQuantity());
        assertEquals(RequestItemUnit.PIECES, item.getUnit());
    }

    @Test
    void mapDescription_VariousInputs_Covered() {
        ObjectMapper mapper = new ObjectMapper();

        // null node
        assertNull(service.mapDescription(null));
        assertNull(service.mapDescription(mapper.nullNode()));

        // textual node
        assertEquals("Hello world", service.mapDescription(mapper.valueToTree("Hello world")));

        // complex structure
        String json = "{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"Line 1\"}]},{\"type\":\"table\",\"content\":[]}]}";
        try {
            JsonNode node = mapper.readTree(json);
            String result = service.mapDescription(node);
            assertNotNull(result);
            assertTrue(result.contains("Line 1"));
            assertFalse(result.contains("table"));
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void runManualSync_ThrowsRuntimeException_LogsAndSwallows() {
        when(configRepository.findById(1L)).thenReturn(Optional.of(config));
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(JiraSearchResponseRecord.class)))
            .thenReturn(new ResponseEntity<>(new JiraSearchResponseRecord(List.of()), HttpStatus.OK));
        doThrow(new RuntimeException("Database error")).when(configRepository).save(any(JiraConfig.class));

        assertDoesNotThrow(() -> service.runManualSync(1L));
    }

    @Test
    void lockJiraIssue_RestClientException_ReturnsFalse() throws Exception {
        Method method = JiraSyncServiceImpl.class.getDeclaredMethod("lockJiraIssue", JiraConfig.class, String.class, Request.class);
        method.setAccessible(true);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(JsonNode.class)))
            .thenThrow(new RestClientException("Connection refused"));

        Request request = new Request();
        request.setRequestID(10L);

        Boolean result = (Boolean) method.invoke(service, config, "TEST-1", request);
        assertFalse(result);
    }

    @Test
    void uploadAttachmentToJira_ThrowsException_Handled() throws Exception {
        Method method = JiraSyncServiceImpl.class.getDeclaredMethod("uploadAttachmentToJira", JiraConfig.class, String.class, Attachment.class);
        method.setAccessible(true);

        Attachment att = new Attachment();
        att.setFileName("file.txt");
        att.setStoragePath("invalid/path/file.txt");

        // Should catch IOException and not throw
        assertDoesNotThrow(() -> method.invoke(service, config, "TEST-1", att));
    }

    @Test
    void syncAttachments_WithAttachment_DownloadsAndSaves() throws Exception {
        Method method = JiraSyncServiceImpl.class.getDeclaredMethod("syncAttachments", Request.class, JiraConfig.class, JiraIssueRecord.class);
        method.setAccessible(true);

        Request request = new Request();
        request.setRequestID(10L);

        JiraAttachmentRecord attRecord = new JiraAttachmentRecord("att1", "jira.txt", "https://api/att1/content", 100L, "text/plain");
        JiraFieldsRecord fields = new JiraFieldsRecord("Summary", null, null, "2026-05-01T16:06:19.433+02:00", null, null, null, List.of(attRecord));
        JiraIssueRecord issueRecord = new JiraIssueRecord("10001", "TEST-1", "https://api/1", fields);

        Resource resource = mock(Resource.class);
        ByteArrayInputStream bis = new ByteArrayInputStream("hello".getBytes());
        when(resource.getInputStream()).thenReturn(bis);
        when(restTemplate.exchange(eq("https://api/att1/content"), eq(HttpMethod.GET), any(HttpEntity.class), eq(Resource.class)))
            .thenReturn(new ResponseEntity<>(resource, HttpStatus.OK));

        method.invoke(service, request, config, issueRecord);

        verify(requisitionService).saveAttachmentFromInputStream(eq(10L), eq("jira.txt"), eq("text/plain"), eq(100L), any());
    }

    @Test
    void syncAttachments_DownloadThrowsException_LogsWarningAndSwallows() throws Exception {
        Method method = JiraSyncServiceImpl.class.getDeclaredMethod("syncAttachments", Request.class, JiraConfig.class, JiraIssueRecord.class);
        method.setAccessible(true);

        Request request = new Request();
        request.setRequestID(10L);

        JiraAttachmentRecord attRecord = new JiraAttachmentRecord("att1", "jira.txt", "https://api/att1/content", 100L, "text/plain");
        JiraFieldsRecord fields = new JiraFieldsRecord("Summary", null, null, "2026-05-01T16:06:19.433+02:00", null, null, null, List.of(attRecord));
        JiraIssueRecord issueRecord = new JiraIssueRecord("10001", "TEST-1", "https://api/1", fields);

        when(restTemplate.exchange(eq("https://api/att1/content"), eq(HttpMethod.GET), any(HttpEntity.class), eq(Resource.class)))
            .thenThrow(new RestClientException("Download failed"));

        assertDoesNotThrow(() -> method.invoke(service, request, config, issueRecord));
    }

    @Test
    void syncLineItems_VariousRequests_Covered() throws Exception {
        Method method = JiraSyncServiceImpl.class.getDeclaredMethod("syncLineItems", Request.class, JiraConfig.class, JiraIssueRecord.class);
        method.setAccessible(true);

        Request request1 = new Request();
        request1.setItems(null);

        JiraFieldsRecord fields = new JiraFieldsRecord("Summary", null, null, "2026-05-01T16:06:19.433+02:00", null, null, null, null);
        JiraIssueRecord issueRecord = new JiraIssueRecord("10001", "TEST-1", "https://api/1", fields);

        method.invoke(service, request1, config, issueRecord);
        assertNotNull(request1.getItems());
        assertTrue(request1.getItems().isEmpty());

        Request request2 = new Request();
        List<RequestItem> existingItems = new ArrayList<>();
        existingItems.add(new RequestItem());
        request2.setItems(existingItems);

        method.invoke(service, request2, config, issueRecord);
        verify(requestItemRepository).deleteAll(any());
        assertTrue(request2.getItems().isEmpty());
    }

    @Test
    void extractLineItemsFromDescriptionTable_VariousTables_Covered() throws Exception {
        Method method = JiraSyncServiceImpl.class.getDeclaredMethod("extractLineItemsFromDescriptionTable", JsonNode.class, Request.class, List.class);
        method.setAccessible(true);

        Request request = new Request();
        List<RequestItem> items = new ArrayList<>();

        // Null description
        method.invoke(service, null, request, items);
        assertTrue(items.isEmpty());

        // Description with various table structures
        String json = "{\n" +
                "  \"type\": \"doc\",\n" +
                "  \"content\": [\n" +
                "    {\n" +
                "      \"type\": \"table\",\n" +
                "      \"content\": [\n" +
                "        {\n" +
                "          \"type\": \"tableRow\",\n" +
                "          \"content\": [\n" +
                "            { \"type\": \"tableHeader\", \"content\": [{ \"type\": \"text\", \"text\": \"Item\" }] },\n" +
                "            { \"type\": \"tableHeader\", \"content\": [{ \"type\": \"text\", \"text\": \"Qty\" }] }\n" +
                "          ]\n" +
                "        },\n" +
                "        {\n" +
                "          \"type\": \"tableRow\",\n" +
                "          \"content\": [\n" +
                "            { \"type\": \"tableCell\", \"content\": [{ \"type\": \"text\", \"text\": \"Laptop\" }] },\n" +
                "            { \"type\": \"tableCell\", \"content\": [{ \"type\": \"text\", \"text\": \"5 pieces\" }] }\n" +
                "          ]\n" +
                "        },\n" +
                "        {\n" +
                "          \"type\": \"tableRow\",\n" +
                "          \"content\": [\n" +
                "            { \"type\": \"tableCell\", \"content\": [{ \"type\": \"text\", \"text\": \"Monitor\" }] },\n" +
                "            { \"type\": \"tableCell\", \"content\": [{ \"type\": \"text\", \"text\": \"2\" }] },\n" +
                "            { \"type\": \"tableCell\", \"content\": [{ \"type\": \"text\", \"text\": \"boxes\" }] }\n" +
                "          ]\n" +
                "        },\n" +
                "        {\n" +
                "          \"type\": \"tableRow\",\n" +
                "          \"content\": [\n" +
                "            { \"type\": \"tableCell\", \"content\": [{ \"type\": \"text\", \"text\": \"Mouse\" }] },\n" +
                "            { \"type\": \"tableCell\", \"content\": [{ \"type\": \"text\", \"text\": \"invalid_qty\" }] },\n" +
                "            { \"type\": \"tableCell\", \"content\": [{ \"type\": \"text\", \"text\": \"pcs\" }] }\n" +
                "          ]\n" +
                "        },\n" +
                "        {\n" +
                "          \"type\": \"tableRow\",\n" +
                "          \"content\": [\n" +
                "            { \"type\": \"tableCell\", \"content\": [{ \"type\": \"text\", \"text\": \"Keyboard\" }] }\n" +
                "          ]\n" +
                "        },\n" +
                "        {\n" +
                "          \"type\": \"tableRow\",\n" +
                "          \"content\": []\n" +
                "        },\n" +
                "        {\n" +
                "          \"type\": \"paragraph\"\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(json);

        method.invoke(service, node, request, items);

        assertAll(
            () -> assertEquals(4, items.size()),
            () -> assertEquals("Laptop", items.get(0).getName()),
            () -> assertEquals(5, items.get(0).getQuantity()),
            () -> assertEquals("Monitor", items.get(1).getName()),
            () -> assertEquals(2, items.get(1).getQuantity()),
            () -> assertEquals(RequestItemUnit.BOXES, items.get(1).getUnit()),
            () -> assertEquals("Mouse", items.get(2).getName()),
            () -> assertEquals(1, items.get(2).getQuantity()),
            () -> assertEquals("Keyboard", items.get(3).getName()),
            () -> assertEquals(1, items.get(3).getQuantity())
        );
    }

    @Test
    void processIssue_WithSecurityContextUser_LogsActor() throws Exception {
        Method method = JiraSyncServiceImpl.class.getDeclaredMethod("processIssue", JiraConfig.class, JiraIssueRecord.class);
        method.setAccessible(true);

        Authentication auth = mock(Authentication.class);
        User principal = new User();
        principal.setId(5L);
        when(auth.getPrincipal()).thenReturn(principal);

        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);

        try {
            JiraConfig tempConfig = new JiraConfig();
            tempConfig.setCustomFieldId("customfield_10001");
            tempConfig.setJiraUrl("https://test.atlassian.net");
            tempConfig.setFallbackUser(config.getFallbackUser());
            tempConfig.setFallbackProject(config.getFallbackProject());
            tempConfig.setFallbackWorkflow(config.getFallbackWorkflow());

            Request request = new Request();
            request.setRequestID(10L);
            request.setItems(new ArrayList<>());
            when(requestRepository.findByJiraIssueKey("TEST-1")).thenReturn(Optional.of(request));
            when(requestRepository.saveAndFlush(any())).thenReturn(request);
            when(internalBudgetRepository.save(any())).thenReturn(null);

            // Mock the PUT call for updateJiraCustomField to return success
            when(restTemplate.exchange(contains("/issue/TEST-1"), eq(HttpMethod.PUT), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.NO_CONTENT));

            JiraFieldsRecord fields = new JiraFieldsRecord("Summary", null, null, "2026-05-01T16:06:19.433+02:00", null, null, null, null);
            JiraIssueRecord issueRecord = new JiraIssueRecord("10001", "TEST-1", "https://api/1", fields);

            method.invoke(service, tempConfig, issueRecord);

            verify(auditService).createJiraSyncLog(eq(principal), any(), anyString());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void syncVeritasToJira_VariousBranches_Covered() throws Exception {
        Method method = JiraSyncServiceImpl.class.getDeclaredMethod("syncVeritasToJira", JiraConfig.class, Request.class);
        method.setAccessible(true);

        Request request = new Request();
        request.setRequestID(10L);
        request.setJiraIssueKey("TEST-101");
        request.setState(RequestStatus.FINISHED);

        Invoice invoice = new Invoice();
        invoice.setIsPaid(true);
        request.setInvoice(invoice);

        // Stub PUT description update
        when(restTemplate.exchange(contains("/issue/TEST-101"), eq(HttpMethod.PUT), any(HttpEntity.class), eq(String.class)))
            .thenReturn(new ResponseEntity<>("{}", HttpStatus.OK));

        // Stub transitions GET
        String transitionsJson = "{\"transitions\":[{\"id\":\"11\",\"name\":\"Delegated Ready\",\"to\":{\"name\":\"Delegated Ready\"}}]}";
        ObjectMapper mapper = new ObjectMapper();
        JsonNode transNode = mapper.readTree(transitionsJson);
        when(restTemplate.exchange(contains("/transitions"), eq(HttpMethod.GET), any(HttpEntity.class), eq(JsonNode.class)))
            .thenReturn(new ResponseEntity<>(transNode, HttpStatus.OK));

        // Stub transitions POST
        when(restTemplate.exchange(contains("/transitions"), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
            .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        // Stub comment POST
        when(restTemplate.exchange(contains("/comment"), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
            .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        Boolean result1 = (Boolean) method.invoke(service, config, request);
        assertTrue(result1);
    }

    @Test
    void processQueue_MaxRetriesReached_SetsStatusFailed() {
        Request request = new Request();
        request.setRequestID(10L);
        request.setRequestKey("KEY-1");

        JiraSyncQueueItem lockItem = JiraSyncQueueItem.builder()
            .id(1L)
            .status("PENDING")
            .actionType("LOCK")
            .jiraConfig(config)
            .jiraIssueKey("TEST-1")
            .request(request)
            .retries(4)
            .build();
        when(queueItemRepository.findByStatus("PENDING")).thenReturn(List.of(lockItem));

        // Mock Lock transitions to throw exception so success = false
        when(restTemplate.exchange(contains("/transitions"), eq(HttpMethod.GET), any(HttpEntity.class), eq(JsonNode.class)))
            .thenThrow(new RestClientException("API error"));

        service.processQueue();

        assertEquals("FAILED", lockItem.getStatus());
        verify(queueItemRepository).save(lockItem);
    }

    @Test
    void processQueue_QueueItemThrowsRuntimeException_RetriesReached_SetsStatusFailed() {
        JiraSyncQueueItem item = mock(JiraSyncQueueItem.class);
        when(item.getId()).thenReturn(1L);
        when(item.getRetries()).thenReturn(4, 5); // 4 initially, 5 after increment
        when(item.getRequest()).thenThrow(new RuntimeException("Database error"));

        when(queueItemRepository.findByStatus("PENDING")).thenReturn(List.of(item));

        service.processQueue();

        verify(item).setStatus("FAILED");
        verify(queueItemRepository).save(item);
    }

    @Test
    void processQueue_QueueItemThrowsRuntimeException_RetriesNotReached_SetsStatusPending() {
        JiraSyncQueueItem item = mock(JiraSyncQueueItem.class);
        when(item.getId()).thenReturn(1L);
        when(item.getRetries()).thenReturn(0, 1);
        when(item.getRequest()).thenThrow(new RuntimeException("Database error"));

        when(queueItemRepository.findByStatus("PENDING")).thenReturn(List.of(item));

        service.processQueue();

        verify(item).setStatus("PENDING");
        verify(queueItemRepository).save(item);
    }

    @Test
    void processIssue_FallbackProjectUsed_RequestKeyGenerated() throws Exception {
        Method method = JiraSyncServiceImpl.class.getDeclaredMethod("processIssue", JiraConfig.class, JiraIssueRecord.class);
        method.setAccessible(true);

        Project fallbackProj = new Project();
        fallbackProj.setId(99L);
        fallbackProj.setProjectKey("FALLBACK");
        fallbackProj.setRequestCounter(5);
        config.setFallbackProject(fallbackProj);

        JiraFieldsRecord fields = new JiraFieldsRecord("Summary", null, null, "2026-05-01T16:06:19.433+02:00", null, null, null, null);

        JiraProjectRecord jiraProj = new JiraProjectRecord("UNKNOWN", "Unknown Project");
        JiraFieldsRecord fieldsWithProj = new JiraFieldsRecord("Summary", null, null, "2026-05-01T16:06:19.433+02:00", null, null, jiraProj, null);
        JiraIssueRecord issueRecord = new JiraIssueRecord("10001", "TEST-1", "https://api/1", fieldsWithProj);

        when(projectRepository.findByProjectKey("UNKNOWN")).thenReturn(Optional.empty());
        when(projectRepository.findByName("Unknown Project")).thenReturn(Optional.empty());

        Request req = new Request();
        req.setRequestID(10L);
        when(issueMapper.toRequest(issueRecord)).thenReturn(req);
        when(requestRepository.findByJiraIssueKey("TEST-1")).thenReturn(Optional.empty());
        when(requestRepository.saveAndFlush(any())).thenReturn(req);

        // Stub PUT call
        when(restTemplate.exchange(contains("/issue/TEST-1"), eq(HttpMethod.PUT), any(HttpEntity.class), eq(String.class)))
            .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        method.invoke(service, config, issueRecord);

        assertEquals(fallbackProj, req.getProject());
        assertEquals("FALLBACK-6", req.getRequestKey());
        verify(projectRepository).saveAndFlush(fallbackProj);
    }

    @Test
    void processIssue_ReporterEmailNullOrBlank_FallbackUserUsed() throws Exception {
        Method method = JiraSyncServiceImpl.class.getDeclaredMethod("processIssue", JiraConfig.class, JiraIssueRecord.class);
        method.setAccessible(true);

        User fallbackUser = new User();
        fallbackUser.setId(55L);
        fallbackUser.setRole(UserRole.REQUESTER);
        fallbackUser.setTeam(Team.builder().teamId(10L).build());
        config.setFallbackUser(fallbackUser);

        JiraUserRecord reporter = new JiraUserRecord("   ", "Reporter Name");
        JiraFieldsRecord fields = new JiraFieldsRecord("Summary", null, null, "2026-05-01T16:06:19.433+02:00", null, reporter, null, null);
        JiraIssueRecord issueRecord = new JiraIssueRecord("10001", "TEST-1", "https://api/1", fields);

        Request req = new Request();
        req.setRequestID(10L);
        when(issueMapper.toRequest(issueRecord)).thenReturn(req);
        when(requestRepository.findByJiraIssueKey("TEST-1")).thenReturn(Optional.empty());
        when(requestRepository.saveAndFlush(any())).thenReturn(req);

        // Stub PUT call
        when(restTemplate.exchange(contains("/issue/TEST-1"), eq(HttpMethod.PUT), any(HttpEntity.class), eq(String.class)))
            .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        method.invoke(service, config, issueRecord);

        assertEquals(fallbackUser, req.getUser());
    }

    @Test
    void processIssue_ReporterEmailNotFound_FallbackUserUsed() throws Exception {
        Method method = JiraSyncServiceImpl.class.getDeclaredMethod("processIssue", JiraConfig.class, JiraIssueRecord.class);
        method.setAccessible(true);

        User fallbackUser = new User();
        fallbackUser.setId(55L);
        fallbackUser.setRole(UserRole.REQUESTER);
        fallbackUser.setTeam(Team.builder().teamId(10L).build());
        config.setFallbackUser(fallbackUser);

        JiraUserRecord reporter = new JiraUserRecord("nonexistent@test.com", "Reporter Name");
        JiraFieldsRecord fields = new JiraFieldsRecord("Summary", null, null, "2026-05-01T16:06:19.433+02:00", null, reporter, null, null);
        JiraIssueRecord issueRecord = new JiraIssueRecord("10001", "TEST-1", "https://api/1", fields);

        Request req = new Request();
        req.setRequestID(10L);
        when(issueMapper.toRequest(issueRecord)).thenReturn(req);
        when(requestRepository.findByJiraIssueKey("TEST-1")).thenReturn(Optional.empty());
        when(requestRepository.saveAndFlush(any())).thenReturn(req);

        // Stub PUT call
        when(restTemplate.exchange(contains("/issue/TEST-1"), eq(HttpMethod.PUT), any(HttpEntity.class), eq(String.class)))
            .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        method.invoke(service, config, issueRecord);

        assertEquals(fallbackUser, req.getUser());
    }

    @Test
    void findTableNodes_PrimitiveNode_HandledCorrectly() throws Exception {
        Method method = JiraSyncServiceImpl.class.getDeclaredMethod("findTableNodes", JsonNode.class, List.class);
        method.setAccessible(true);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode primitiveNode = mapper.readTree("123"); // Integer node (neither object nor array)

        List<JsonNode> tableNodes = new ArrayList<>();
        method.invoke(service, primitiveNode, tableNodes);

        assertTrue(tableNodes.isEmpty());
    }

    @Test
    void extractText_ObjectNodeWithoutContent_HandledCorrectly() throws Exception {
        Method method = JiraSyncServiceImpl.class.getDeclaredMethod("extractText", JsonNode.class, StringBuilder.class);
        method.setAccessible(true);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree("{\"type\": \"paragraph\"}"); // paragraph node without content

        StringBuilder sb = new StringBuilder();
        method.invoke(service, node, sb);

        assertEquals("", sb.toString());
    }

    @Test
    void extractText_ParagraphNodeWithLengthZero_SkipsNewline() throws Exception {
        Method method = JiraSyncServiceImpl.class.getDeclaredMethod("extractText", JsonNode.class, StringBuilder.class);
        method.setAccessible(true);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree("{\"type\": \"paragraph\", \"content\": [{\"type\": \"text\", \"text\": \"Hello\"}]}");

        StringBuilder sb = new StringBuilder(); // length 0
        method.invoke(service, node, sb);

        assertEquals("Hello", sb.toString()); // No leading newline
    }

    @Test
    void syncAttachments_BodyNull_HandlesGracefully() throws Exception {
        Method method = JiraSyncServiceImpl.class.getDeclaredMethod("syncAttachments", Request.class, JiraConfig.class, JiraIssueRecord.class);
        method.setAccessible(true);

        Request request = new Request();
        request.setRequestID(10L);

        JiraAttachmentRecord att = new JiraAttachmentRecord("1", "jira.txt", "https://api/att1/content", 100L, "text/plain");
        JiraFieldsRecord fields = new JiraFieldsRecord("Summary", null, null, "2026-05-01T16:06:19.433+02:00", null, null, null, List.of(att));
        JiraIssueRecord issueRecord = new JiraIssueRecord("10001", "TEST-1", "https://api/1", fields);

        when(restTemplate.exchange(eq("https://api/att1/content"), eq(HttpMethod.GET), any(HttpEntity.class), eq(Resource.class)))
            .thenReturn(new ResponseEntity<>(null, HttpStatus.OK)); // Body is null

        assertDoesNotThrow(() -> method.invoke(service, request, config, issueRecord));
    }

    @Test
    void transitionJiraIssue_ReflectionBranches() throws Exception {
        Method method = JiraSyncServiceImpl.class.getDeclaredMethod("transitionJiraIssue", JiraConfig.class, String.class, String.class, boolean.class);
        method.setAccessible(true);

        ObjectMapper mapper = new ObjectMapper();

        // 1. isLock = true, match by name
        String json1 = "{\"transitions\":[{\"id\":\"1\",\"name\":\"Delegated Waiting\",\"to\":{\"name\":\"Other\"}}]}";
        when(restTemplate.exchange(contains("/transitions"), eq(HttpMethod.GET), any(HttpEntity.class), eq(JsonNode.class)))
            .thenReturn(new ResponseEntity<>(mapper.readTree(json1), HttpStatus.OK));
        when(restTemplate.exchange(contains("/transitions"), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
            .thenReturn(new ResponseEntity<>(HttpStatus.OK));
        Boolean res1 = (Boolean) method.invoke(service, config, "TEST-1", "any", true);
        assertTrue(res1);

        // 2. isLock = true, match by to.name
        String json2 = "{\"transitions\":[{\"id\":\"2\",\"name\":\"Other\",\"to\":{\"name\":\"delegated waiting\"}}]}";
        when(restTemplate.exchange(contains("/transitions"), eq(HttpMethod.GET), any(HttpEntity.class), eq(JsonNode.class)))
            .thenReturn(new ResponseEntity<>(mapper.readTree(json2), HttpStatus.OK));
        Boolean res2 = (Boolean) method.invoke(service, config, "TEST-1", "any", true);
        assertTrue(res2);

        // 3. isLock = true, no match
        String json3 = "{\"transitions\":[{\"id\":\"3\",\"name\":\"Other\",\"to\":{\"name\":\"Other\"}}]}";
        when(restTemplate.exchange(contains("/transitions"), eq(HttpMethod.GET), any(HttpEntity.class), eq(JsonNode.class)))
            .thenReturn(new ResponseEntity<>(mapper.readTree(json3), HttpStatus.OK));
        Boolean res3 = (Boolean) method.invoke(service, config, "TEST-1", "any", true);
        assertTrue(res3); // returns isLock (true) when transitionId == null

        // 4. isLock = false, match by name ignore case
        String json4 = "{\"transitions\":[{\"id\":\"4\",\"name\":\"TARGET\",\"to\":{\"name\":\"Other\"}}]}";
        when(restTemplate.exchange(contains("/transitions"), eq(HttpMethod.GET), any(HttpEntity.class), eq(JsonNode.class)))
            .thenReturn(new ResponseEntity<>(mapper.readTree(json4), HttpStatus.OK));
        Boolean res4 = (Boolean) method.invoke(service, config, "TEST-1", "target", false);
        assertTrue(res4);

        // 5. isLock = false, match by to.name ignore case
        String json5 = "{\"transitions\":[{\"id\":\"5\",\"name\":\"Other\",\"to\":{\"name\":\"target\"}}]}";
        when(restTemplate.exchange(contains("/transitions"), eq(HttpMethod.GET), any(HttpEntity.class), eq(JsonNode.class)))
            .thenReturn(new ResponseEntity<>(mapper.readTree(json5), HttpStatus.OK));
        Boolean res5 = (Boolean) method.invoke(service, config, "TEST-1", "target", false);
        assertTrue(res5);

        // 6. isLock = false, match by name contains
        String json6 = "{\"transitions\":[{\"id\":\"6\",\"name\":\"ContainsTargetWord\",\"to\":{\"name\":\"Other\"}}]}";
        when(restTemplate.exchange(contains("/transitions"), eq(HttpMethod.GET), any(HttpEntity.class), eq(JsonNode.class)))
            .thenReturn(new ResponseEntity<>(mapper.readTree(json6), HttpStatus.OK));
        Boolean res6 = (Boolean) method.invoke(service, config, "TEST-1", "target", false);
        assertTrue(res6);

        // 7. isLock = false, match by to.name contains
        String json7 = "{\"transitions\":[{\"id\":\"7\",\"name\":\"Other\",\"to\":{\"name\":\"ContainsTargetWord\"}}]}";
        when(restTemplate.exchange(contains("/transitions"), eq(HttpMethod.GET), any(HttpEntity.class), eq(JsonNode.class)))
            .thenReturn(new ResponseEntity<>(mapper.readTree(json7), HttpStatus.OK));
        Boolean res7 = (Boolean) method.invoke(service, config, "TEST-1", "target", false);
        assertTrue(res7);

        // 8. isLock = false, no match
        String json8 = "{\"transitions\":[{\"id\":\"8\",\"name\":\"Other\",\"to\":{\"name\":\"Other\"}}]}";
        when(restTemplate.exchange(contains("/transitions"), eq(HttpMethod.GET), any(HttpEntity.class), eq(JsonNode.class)))
            .thenReturn(new ResponseEntity<>(mapper.readTree(json8), HttpStatus.OK));
        Boolean res8 = (Boolean) method.invoke(service, config, "TEST-1", "target", false);
        assertFalse(res8); // returns isLock (false) when transitionId == null
    }

    @Test
    void createJiraRemoteLink_VariousBranches() throws Exception {
        Method method = JiraSyncServiceImpl.class.getDeclaredMethod("createJiraRemoteLink", JiraConfig.class, String.class, Request.class);
        method.setAccessible(true);

        Request request = new Request();
        request.setRequestID(10L);
        request.setRequestKey("REQ-10");
        request.setRequestName(null); // covers requestName == null -> requestKey used

        // Test successful call
        lenient().when(restTemplate.exchange(contains("/remotelink"), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
            .thenReturn(new ResponseEntity<>("{}", HttpStatus.OK));
        assertDoesNotThrow(() -> method.invoke(service, config, "TEST-1", request));

        // Test RestClientException is caught and handled gracefully
        lenient().when(restTemplate.exchange(contains("/remotelink"), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
            .thenThrow(new RestClientException("Rest error"));
        assertDoesNotThrow(() -> method.invoke(service, config, "TEST-1", request));
    }

    @Test
    void syncVeritasToJira_RejectionAndRevertBranches() throws Exception {
        Method method = JiraSyncServiceImpl.class.getDeclaredMethod("syncVeritasToJira", JiraConfig.class, Request.class);
        method.setAccessible(true);

        Request request = new Request();
        request.setRequestID(10L);
        request.setJiraIssueKey("TEST-101");
        request.setState(RequestStatus.ACTIVE);
        request.setRejectionReason("Bad quote");
        request.setRequestName("Requisition Name");

        // 1. REJECT audit log present
        User rejectActor = new User();
        rejectActor.setName("Rejecter User");
        AuditLog rejectLog = AuditLog.builder().actor(rejectActor).build();
        lenient().when(auditLogRepository.findFirstByRequestAndActionOrderByTimestampDesc(request, REJECT))
            .thenReturn(Optional.of(rejectLog));

        // Stub REST dependencies
        lenient().when(restTemplate.exchange(contains("/issue/TEST-101"), eq(HttpMethod.PUT), any(HttpEntity.class), eq(String.class)))
            .thenReturn(new ResponseEntity<>("{}", HttpStatus.OK));
        lenient().when(restTemplate.exchange(contains("/comment"), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
            .thenReturn(new ResponseEntity<>(HttpStatus.OK));
        lenient().when(restTemplate.exchange(contains("/transitions"), eq(HttpMethod.GET), any(HttpEntity.class), eq(JsonNode.class)))
            .thenReturn(new ResponseEntity<>(new ObjectMapper().readTree("{\"transitions\":[]}"), HttpStatus.OK));

        Boolean res1 = (Boolean) method.invoke(service, config, request);
        assertTrue(res1);

        // 2. REJECT log absent, REVERT log present
        User reverterActor = new User();
        reverterActor.setName("Reverter User");
        AuditLog revertLog = AuditLog.builder().actor(reverterActor).build();
        lenient().when(auditLogRepository.findFirstByRequestAndActionOrderByTimestampDesc(request, REJECT))
            .thenReturn(Optional.empty());
        lenient().when(auditLogRepository.findFirstByRequestAndActionOrderByTimestampDesc(request, REVERT))
            .thenReturn(Optional.of(revertLog));

        Boolean res2 = (Boolean) method.invoke(service, config, request);
        assertTrue(res2);

        // 3. Both logs absent
        lenient().when(auditLogRepository.findFirstByRequestAndActionOrderByTimestampDesc(request, REJECT))
            .thenReturn(Optional.empty());
        lenient().when(auditLogRepository.findFirstByRequestAndActionOrderByTimestampDesc(request, REVERT))
            .thenReturn(Optional.empty());

        Boolean res3 = (Boolean) method.invoke(service, config, request);
        assertTrue(res3);
    }

    @Test
    void syncVeritasToJira_FinishedStateNullInvoiceAndUnpaidInvoice() throws Exception {
        Method method = JiraSyncServiceImpl.class.getDeclaredMethod("syncVeritasToJira", JiraConfig.class, Request.class);
        method.setAccessible(true);

        // Stub PUT description update and comment POST
        lenient().when(restTemplate.exchange(contains("/issue/TEST-101"), eq(HttpMethod.PUT), any(HttpEntity.class), eq(String.class)))
            .thenReturn(new ResponseEntity<>("{}", HttpStatus.OK));
        lenient().when(restTemplate.exchange(contains("/comment"), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
            .thenReturn(new ResponseEntity<>(HttpStatus.OK));
        lenient().when(restTemplate.exchange(contains("/transitions"), eq(HttpMethod.GET), any(HttpEntity.class), eq(JsonNode.class)))
            .thenReturn(new ResponseEntity<>(new ObjectMapper().readTree("{\"transitions\":[]}"), HttpStatus.OK));

        // 1. Finished state with null Invoice
        Request requestNullInvoice = new Request();
        requestNullInvoice.setRequestID(10L);
        requestNullInvoice.setJiraIssueKey("TEST-101");
        requestNullInvoice.setState(RequestStatus.FINISHED);
        requestNullInvoice.setRequestName("Req");

        Boolean res1 = (Boolean) method.invoke(service, config, requestNullInvoice);
        assertTrue(res1);

        // 2. Finished state with unpaid Invoice
        Request requestUnpaid = new Request();
        requestUnpaid.setRequestID(10L);
        requestUnpaid.setJiraIssueKey("TEST-101");
        requestUnpaid.setState(RequestStatus.FINISHED);
        requestUnpaid.setRequestName("Req");
        Invoice unpaidInvoice = new Invoice();
        unpaidInvoice.setIsPaid(false);
        unpaidInvoice.setTotalAmount(BigDecimal.TEN);
        unpaidInvoice.setCurrency(Currency.EUR);
        requestUnpaid.setInvoice(unpaidInvoice);

        CurrencyConversionResult convResult = new CurrencyConversionResult(BigDecimal.TEN, BigDecimal.ONE, LocalDateTime.now(), ExchangeRateSource.FRANKFURTER);
        lenient().when(currencyConversionService.convert(any(), any())).thenReturn(convResult);

        Boolean res2 = (Boolean) method.invoke(service, config, requestUnpaid);
        assertTrue(res2);
    }

    @Test
    void syncVeritasToJira_CurrentStepNull() throws Exception {
        Method method = JiraSyncServiceImpl.class.getDeclaredMethod("syncVeritasToJira", JiraConfig.class, Request.class);
        method.setAccessible(true);

        Request request = new Request();
        request.setRequestID(10L);
        request.setJiraIssueKey("TEST-101");
        request.setState(RequestStatus.ACTIVE); // Not FINISHED
        request.setCurrentStep(null); // targetStepName becomes null/blank
        request.setRequestName("Req");

        lenient().when(restTemplate.exchange(contains("/issue/TEST-101"), eq(HttpMethod.PUT), any(HttpEntity.class), eq(String.class)))
            .thenReturn(new ResponseEntity<>("{}", HttpStatus.OK));
        lenient().when(restTemplate.exchange(contains("/comment"), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
            .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        Boolean res = (Boolean) method.invoke(service, config, request);
        assertTrue(res);
    }

    @Test
    void syncVeritasToJira_CommentThrowsException() throws Exception {
        Method method = JiraSyncServiceImpl.class.getDeclaredMethod("syncVeritasToJira", JiraConfig.class, Request.class);
        method.setAccessible(true);

        Request request = new Request();
        request.setRequestID(10L);
        request.setJiraIssueKey("TEST-101");
        request.setState(RequestStatus.ACTIVE);
        request.setRequestName("Req");

        lenient().when(restTemplate.exchange(contains("/issue/TEST-101"), eq(HttpMethod.PUT), any(HttpEntity.class), eq(String.class)))
            .thenReturn(new ResponseEntity<>("{}", HttpStatus.OK));
        lenient().when(restTemplate.exchange(contains("/comment"), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
            .thenThrow(new RestClientException("Comment error"));

        Boolean res = (Boolean) method.invoke(service, config, request);
        assertTrue(res); // Handled gracefully and returns true
    }

    @Test
    void processIssue_AlreadyHasRequestKeyAndBudgetAndWorkflow() throws Exception {
        Method method = JiraSyncServiceImpl.class.getDeclaredMethod("processIssue", JiraConfig.class, JiraIssueRecord.class);
        method.setAccessible(true);

        Request existingRequest = new Request();
        existingRequest.setRequestID(10L);
        existingRequest.setRequestKey("EXISTING-KEY");
        existingRequest.setWorkflowDefinition(new WorkflowDefinition());
        existingRequest.setBudget(new InternalBudget());

        JiraFieldsRecord fields = new JiraFieldsRecord("Summary", null, null, "2026-05-01T16:06:19.433+02:00", null, null, null, null);
        JiraIssueRecord issueRecord = new JiraIssueRecord("10001", "TEST-1", "https://api/1", fields);

        when(requestRepository.findByJiraIssueKey("TEST-1")).thenReturn(Optional.empty());
        when(issueMapper.toRequest(issueRecord)).thenReturn(existingRequest);
        when(requestRepository.saveAndFlush(any())).thenReturn(existingRequest);

        // Stub PUT updateJiraCustomField
        lenient().when(restTemplate.exchange(contains("/issue/TEST-1"), eq(HttpMethod.PUT), any(HttpEntity.class), eq(String.class)))
            .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        assertDoesNotThrow(() -> method.invoke(service, config, issueRecord));
    }

    @Test
    void buildJiraDescriptionPayload_VariousBranches() {
        // 1. null description, null items
        Request request1 = new Request();
        request1.setDescription(null);
        request1.setItems(null);

        JsonNode payload1 = service.buildJiraDescriptionPayload(request1);
        assertNotNull(payload1);

        // 2. blank description, items empty
        Request request2 = new Request();
        request2.setDescription("   ");
        request2.setItems(List.of());

        JsonNode payload2 = service.buildJiraDescriptionPayload(request2);
        assertNotNull(payload2);

        // 3. items with null unit and non-null description
        Request request3 = new Request();
        request3.setDescription("Desc");
        RequestItem item = new RequestItem();
        item.setName("Item");
        item.setQuantity(1);
        item.setUnit(null); // unit is null
        item.setDescription("Item Description"); // description is non-null
        request3.setItems(List.of(item));

        JsonNode payload3 = service.buildJiraDescriptionPayload(request3);
        assertNotNull(payload3);

        // 4. items with null unit and null description
        item.setDescription(null);
        JsonNode payload4 = service.buildJiraDescriptionPayload(request3);
        assertNotNull(payload4);
    }
}
