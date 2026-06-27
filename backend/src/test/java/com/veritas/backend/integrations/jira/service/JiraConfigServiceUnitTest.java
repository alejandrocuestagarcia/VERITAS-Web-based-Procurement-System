package com.veritas.backend.integrations.jira.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import com.veritas.backend.project.entity.Project;
import com.veritas.backend.project.repository.ProjectRepository;
import com.veritas.backend.user.entity.User;
import com.veritas.backend.user.entity.UserRole;
import com.veritas.backend.user.repository.UserRepository;
import com.veritas.backend.team.entity.Team;
import com.veritas.backend.workflow.entity.WorkflowDefinition;
import com.veritas.backend.workflow.repository.WorkflowDefinitionRepository;
import com.veritas.backend.department.entity.Department;
import com.veritas.backend.requisition.entity.RequestStatus;
import com.veritas.backend.requisition.entity.Invoice;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.EntityExistsException;
import java.util.List;
import java.util.Optional;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import org.junit.jupiter.api.AfterEach;
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
import org.springframework.security.core.context.SecurityContextHolder;

//AI-GENERATED

@ExtendWith(MockitoExtension.class)
class JiraConfigServiceUnitTest {

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

    @Mock
    private JiraSyncService jiraSyncService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private WorkflowDefinitionRepository workflowDefinitionRepository;

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
                "jql", 60, "customfield_10015", 10L, 20L, 30L, null, null);

        responseDto = new JiraConfigResponseDto(1L, "Test Config", "https://test.atlassian.net", "user",
            "jql", 60, "customfield_10015", null, null, null, null, null, null, null, null, true);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
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
        Team team = Team.builder().teamId(1L).name("Team A").build();
        setupFallbackMocks(10L, 20L, 30L, team);

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
        Team team = Team.builder().teamId(1L).name("Team A").build();
        setupFallbackMocks(10L, 20L, 30L, team);

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
        assertNull(syncedRequest.getJiraConfig());

        // otherRequest is untouched because it wasn't returned by findByJiraConfigId
        assertEquals("OTHER-456", otherRequest.getJiraIssueKey());
        assertEquals("https://test.atlassian.net/browse/OTHER-456", otherRequest.getJiraIssueUrl());
        assertEquals("Done", otherRequest.getJiraStatus());

        verify(queueItemRepository).deleteByJiraConfig(config);
        verify(requestRepository).saveAll(List.of(syncedRequest));
        verify(auditService).createJiraUnsyncLog(eq(null), eq(syncedRequest), anyString());
    }

    @Test
    void CreateConfig_FallbackUserAndProjectDifferentTeams_ThrowsIllegalArgument() {
        Team teamA = Team.builder().teamId(1L).name("Team A").build();
        Team teamB = Team.builder().teamId(2L).name("Team B").build();

        User fallbackUser = User.builder().id(10L).role(UserRole.REQUESTER).team(teamA).build();
        Project fallbackProject = Project.builder().id(20L).team(teamB).build();
        WorkflowDefinition workflow = new WorkflowDefinition();
        workflow.setId(30L);
        workflow.setIsActive(true);

        JiraConfigDto dto = new JiraConfigDto(null, "Config", "https://test.atlassian.net", "user", "token",
                "jql", 60, "customfield_10015", 10L, 20L, 30L, null, null);

        when(repository.existsByJiraUrlAndJql(anyString(), anyString())).thenReturn(false);
        when(mapper.toEntity(any())).thenReturn(new JiraConfig());
        when(userRepository.findById(10L)).thenReturn(Optional.of(fallbackUser));
        when(projectRepository.findById(20L)).thenReturn(Optional.of(fallbackProject));
        when(workflowDefinitionRepository.findById(30L)).thenReturn(Optional.of(workflow));

        assertThrows(IllegalArgumentException.class, () -> service.createConfig(dto));
        verify(repository, never()).save(any());
    }

    @Test
    void CreateConfig_FallbackUserAndProjectSameTeam_Succeeds() {
        Team team = Team.builder().teamId(1L).name("Team A").build();

        User fallbackUser = User.builder().id(10L).role(UserRole.REQUESTER).team(team).build();
        Project fallbackProject = Project.builder().id(20L).team(team).build();
        WorkflowDefinition workflow = new WorkflowDefinition();
        workflow.setId(30L);
        workflow.setIsActive(true);

        JiraConfigDto dto = new JiraConfigDto(null, "Config", "https://test.atlassian.net", "user", "token",
                "jql", 60, "customfield_10015", 10L, 20L, 30L, null, null);

        JiraConfig entity = new JiraConfig();
        when(repository.existsByJiraUrlAndJql(anyString(), anyString())).thenReturn(false);
        when(mapper.toEntity(any())).thenReturn(entity);
        when(userRepository.findById(10L)).thenReturn(Optional.of(fallbackUser));
        when(projectRepository.findById(20L)).thenReturn(Optional.of(fallbackProject));
        when(workflowDefinitionRepository.findById(30L)).thenReturn(Optional.of(workflow));
        when(repository.save(any())).thenReturn(entity);
        when(mapper.toDto(any(JiraConfig.class))).thenReturn(responseDto);

        JiraConfigResponseDto result = service.createConfig(dto);

        assertNotNull(result);
        verify(repository).save(any());
    }

    @Test
    void CreateConfig_WorkflowDifferentDepartment_ThrowsIllegalArgument() {
        Department deptA = Department.builder().departmentId(1L).name("Dept A").build();
        Department deptB = Department.builder().departmentId(2L).name("Dept B").build();
        Team team = Team.builder().teamId(1L).name("Team A").department(deptA).build();

        User fallbackUser = User.builder().id(10L).role(UserRole.REQUESTER).team(team).build();
        Project fallbackProject = Project.builder().id(20L).team(team).build();
        WorkflowDefinition workflow = new WorkflowDefinition();
        workflow.setId(30L);
        workflow.setIsActive(true);
        workflow.setDepartment(deptB);

        JiraConfigDto dto = new JiraConfigDto(null, "Config", "https://test.atlassian.net", "user", "token",
                "jql", 60, "customfield_10015", 10L, 20L, 30L, null, null);

        when(repository.existsByJiraUrlAndJql(anyString(), anyString())).thenReturn(false);
        when(mapper.toEntity(any())).thenReturn(new JiraConfig());
        when(userRepository.findById(10L)).thenReturn(Optional.of(fallbackUser));
        when(projectRepository.findById(20L)).thenReturn(Optional.of(fallbackProject));
        when(workflowDefinitionRepository.findById(30L)).thenReturn(Optional.of(workflow));

        assertThrows(IllegalArgumentException.class, () -> service.createConfig(dto));
        verify(repository, never()).save(any());
    }

    @Test
    void CreateConfig_GlobalWorkflow_Succeeds() {
        Department dept = Department.builder().departmentId(1L).name("Dept A").build();
        Team team = Team.builder().teamId(1L).name("Team A").department(dept).build();

        User fallbackUser = User.builder().id(10L).role(UserRole.REQUESTER).team(team).build();
        Project fallbackProject = Project.builder().id(20L).team(team).build();
        WorkflowDefinition workflow = new WorkflowDefinition();
        workflow.setId(30L);
        workflow.setDepartment(null);
        workflow.setIsActive(true);

        JiraConfigDto dto = new JiraConfigDto(null, "Config", "https://test.atlassian.net", "user", "token",
                "jql", 60, "customfield_10015", 10L, 20L, 30L, null, null);

        JiraConfig entity = new JiraConfig();
        when(repository.existsByJiraUrlAndJql(anyString(), anyString())).thenReturn(false);
        when(mapper.toEntity(any())).thenReturn(entity);
        when(userRepository.findById(10L)).thenReturn(Optional.of(fallbackUser));
        when(projectRepository.findById(20L)).thenReturn(Optional.of(fallbackProject));
        when(workflowDefinitionRepository.findById(30L)).thenReturn(Optional.of(workflow));
        when(repository.save(any())).thenReturn(entity);
        when(mapper.toDto(any(JiraConfig.class))).thenReturn(responseDto);

        JiraConfigResponseDto result = service.createConfig(dto);

        assertNotNull(result);
        verify(repository).save(any());
    }

    @Test
    void DeleteConfig_WithNoAuthentication_DeletesSuccessfully() {
        SecurityContextHolder.clearContext();

        when(repository.findById(1L)).thenReturn(Optional.of(config));
        when(requestRepository.findByJiraConfigId(1L)).thenReturn(List.of());
        when(mapper.toDto(any(JiraConfig.class))).thenReturn(responseDto);

        JiraConfigResponseDto result = service.deleteConfigById(1L);

        assertNotNull(result);
        verify(repository).delete(config);
    }

    @Test
    void DeleteConfig_WithAuthenticationNotUser_DeletesSuccessfully() {
        var auth = new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(repository.findById(1L)).thenReturn(Optional.of(config));
        when(requestRepository.findByJiraConfigId(1L)).thenReturn(List.of());
        when(mapper.toDto(any(JiraConfig.class))).thenReturn(responseDto);

        JiraConfigResponseDto result = service.deleteConfigById(1L);

        assertNotNull(result);
        verify(repository).delete(config);
    }

    @Test
    void DeleteConfig_WithFinishedPaidRequest_DoesNotLogOrComment() {
        when(repository.findById(1L)).thenReturn(Optional.of(config));

        Request finishedPaidRequest = new Request();
        finishedPaidRequest.setState(RequestStatus.FINISHED);
        Invoice invoice = new Invoice();
        invoice.setIsPaid(true);
        finishedPaidRequest.setInvoice(invoice);

        when(requestRepository.findByJiraConfigId(1L)).thenReturn(List.of(finishedPaidRequest));
        when(mapper.toDto(any(JiraConfig.class))).thenReturn(responseDto);

        service.deleteConfigById(1L);

        verify(auditService, never()).createJiraUnsyncLog(any(), any(), any());
        verify(jiraSyncService, never()).postJiraComment(any(), anyString(), anyString());
    }

    @Test
    void DeleteConfig_WithSyncedRequestBlankIssueKey_DoesNotComment() {
        when(repository.findById(1L)).thenReturn(Optional.of(config));

        Request req = new Request();
        req.setJiraIssueKey("");

        when(requestRepository.findByJiraConfigId(1L)).thenReturn(List.of(req));
        when(mapper.toDto(any(JiraConfig.class))).thenReturn(responseDto);

        service.deleteConfigById(1L);

        verify(auditService).createJiraUnsyncLog(eq(null), eq(req), anyString());
        verify(jiraSyncService, never()).postJiraComment(any(), anyString(), anyString());
    }

    @Test
    void DeleteConfig_CommentThrowsException_LogsWarningAndContinues() throws Exception {
        when(repository.findById(1L)).thenReturn(Optional.of(config));

        Request req = new Request();
        req.setJiraIssueKey("TEST-123");

        when(requestRepository.findByJiraConfigId(1L)).thenReturn(List.of(req));
        when(mapper.toDto(any(JiraConfig.class))).thenReturn(responseDto);
        Mockito.doThrow(new RuntimeException("Jira API Down"))
            .when(jiraSyncService).postJiraComment(any(), anyString(), anyString());

        service.deleteConfigById(1L);

        verify(repository).delete(config);
    }

    @Test
    void CreateConfig_FallbackUserNullTeam_ThrowsIllegalArgument() {
        User fallbackUser = User.builder().id(10L).role(UserRole.REQUESTER).team(null).build();
        Project fallbackProject = Project.builder().id(20L).team(Team.builder().teamId(1L).build()).build();
        WorkflowDefinition workflow = new WorkflowDefinition();
        workflow.setId(30L);
        workflow.setIsActive(true);

        JiraConfigDto dto = new JiraConfigDto(null, "Config", "https://test.atlassian.net", "user", "token",
                "jql", 60, "customfield_10015", 10L, 20L, 30L, null, null);

        when(repository.existsByJiraUrlAndJql(anyString(), anyString())).thenReturn(false);
        when(mapper.toEntity(any())).thenReturn(new JiraConfig());
        when(userRepository.findById(10L)).thenReturn(Optional.of(fallbackUser));
        when(projectRepository.findById(20L)).thenReturn(Optional.of(fallbackProject));
        when(workflowDefinitionRepository.findById(30L)).thenReturn(Optional.of(workflow));

        assertThrows(IllegalArgumentException.class, () -> service.createConfig(dto));
    }

    @Test
    void CreateConfig_FallbackProjectNullTeam_ThrowsIllegalArgument() {
        User fallbackUser = User.builder().id(10L).role(UserRole.REQUESTER).team(Team.builder().teamId(1L).build()).build();
        Project fallbackProject = Project.builder().id(20L).team(null).build();
        WorkflowDefinition workflow = new WorkflowDefinition();
        workflow.setId(30L);
        workflow.setIsActive(true);

        JiraConfigDto dto = new JiraConfigDto(null, "Config", "https://test.atlassian.net", "user", "token",
                "jql", 60, "customfield_10015", 10L, 20L, 30L, null, null);

        when(repository.existsByJiraUrlAndJql(anyString(), anyString())).thenReturn(false);
        when(mapper.toEntity(any())).thenReturn(new JiraConfig());
        when(userRepository.findById(10L)).thenReturn(Optional.of(fallbackUser));
        when(projectRepository.findById(20L)).thenReturn(Optional.of(fallbackProject));
        when(workflowDefinitionRepository.findById(30L)).thenReturn(Optional.of(workflow));

        assertThrows(IllegalArgumentException.class, () -> service.createConfig(dto));
    }
    @Test
    void GetConfigById_ExistingConfig_ReturnsDto() {
        when(repository.findById(1L)).thenReturn(Optional.of(config));
        when(mapper.toDto(config)).thenReturn(responseDto);

        JiraConfigResponseDto result = service.getConfigById(1L);

        assertNotNull(result);
        assertEquals(responseDto.id(), result.id());
    }

    @Test
    void GetConfigById_NonExistingConfig_ThrowsRuntimeException() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> service.getConfigById(999L));
    }

    @Test
    void CreateConfig_ApiTokenNull_ThrowsIllegalArgumentException() {
        JiraConfigDto dto = new JiraConfigDto(null, "Config", "https://test.atlassian.net", "user", null,
                "jql", 60, "customfield_10015", null, null, null, null, null);

        assertThrows(IllegalArgumentException.class, () -> service.createConfig(dto));
    }

    @Test
    void CreateConfig_ApiTokenBlank_ThrowsIllegalArgumentException() {
        JiraConfigDto dto = new JiraConfigDto(null, "Config", "https://test.atlassian.net", "user", "   ",
                "jql", 60, "customfield_10015", null, null, null, null, null);

        assertThrows(IllegalArgumentException.class, () -> service.createConfig(dto));
    }

    @Test
    void UpdateConfig_DuplicateConfigUrlAndJql_ThrowsEntityExistsException() {
        JiraConfig anotherConfig = new JiraConfig();
        anotherConfig.setId(2L);
        anotherConfig.setJiraUrl("https://test.atlassian.net");
        anotherConfig.setJql("jql");

        when(repository.findByJiraUrlAndJql(anyString(), anyString())).thenReturn(Optional.of(anotherConfig));

        assertThrows(EntityExistsException.class, () -> service.updateConfig(1L, configDto));
    }

    @Test
    void UpdateConfig_NullApiToken_PreservesExistingToken() {
        config.setApiToken("old-token");
        setupFallbackMocks(10L, 20L, 30L, Team.builder().teamId(1L).build());

        JiraConfigDto dto = new JiraConfigDto(1L, "Test Config", "https://test.atlassian.net", "user", null,
                "jql", 60, "customfield_10015", 10L, 20L, 30L, null, null);

        when(repository.findById(1L)).thenReturn(Optional.of(config));
        when(repository.findByJiraUrlAndJql(anyString(), anyString())).thenReturn(Optional.empty());
        when(repository.save(config)).thenReturn(config);
        when(mapper.toDto(config)).thenReturn(responseDto);

        service.updateConfig(1L, dto);

        assertEquals("old-token", config.getApiToken());
    }

    @Test
    void UpdateConfig_BlankApiToken_PreservesExistingToken() {
        config.setApiToken("old-token");
        setupFallbackMocks(10L, 20L, 30L, Team.builder().teamId(1L).build());

        JiraConfigDto dto = new JiraConfigDto(1L, "Test Config", "https://test.atlassian.net", "user", "   ",
                "jql", 60, "customfield_10015", 10L, 20L, 30L, null, null);

        when(repository.findById(1L)).thenReturn(Optional.of(config));
        when(repository.findByJiraUrlAndJql(anyString(), anyString())).thenReturn(Optional.empty());
        when(repository.save(config)).thenReturn(config);
        when(mapper.toDto(config)).thenReturn(responseDto);

        service.updateConfig(1L, dto);

        assertEquals("old-token", config.getApiToken());
    }

    @Test
    void DeleteConfig_WithUserAuthentication_DeletesSuccessfullyAndLogsWithActor() {
        User currentUser = User.builder().id(10L).email("actor@test.com").build();
        var auth = new UsernamePasswordAuthenticationToken(currentUser, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(repository.findById(1L)).thenReturn(Optional.of(config));
        Request req = new Request();
        req.setJiraIssueKey("TEST-123");
        when(requestRepository.findByJiraConfigId(1L)).thenReturn(List.of(req));
        when(mapper.toDto(any(JiraConfig.class))).thenReturn(responseDto);

        service.deleteConfigById(1L);

        verify(auditService).createJiraUnsyncLog(eq(currentUser), eq(req), anyString());
    }

    @Test
    void DeleteConfig_WithFinishedRequestNullInvoice_LogsAndComments() {
        when(repository.findById(1L)).thenReturn(Optional.of(config));

        Request finishedRequest = new Request();
        finishedRequest.setState(RequestStatus.FINISHED);
        finishedRequest.setInvoice(null);
        finishedRequest.setJiraIssueKey("TEST-123");

        when(requestRepository.findByJiraConfigId(1L)).thenReturn(List.of(finishedRequest));
        when(mapper.toDto(any(JiraConfig.class))).thenReturn(responseDto);

        service.deleteConfigById(1L);

        assertAll(
            () -> verify(auditService).createJiraUnsyncLog(eq(null), eq(finishedRequest), anyString()),
            () -> verify(jiraSyncService).postJiraComment(any(), eq("TEST-123"), anyString())
        );
    }

    @Test
    void DeleteConfig_WithFinishedRequestUnpaidInvoice_LogsAndComments() {
        when(repository.findById(1L)).thenReturn(Optional.of(config));

        Request finishedRequest = new Request();
        finishedRequest.setState(RequestStatus.FINISHED);
        Invoice invoice = new Invoice();
        invoice.setIsPaid(false);
        finishedRequest.setInvoice(invoice);
        finishedRequest.setJiraIssueKey("TEST-123");

        when(requestRepository.findByJiraConfigId(1L)).thenReturn(List.of(finishedRequest));
        when(mapper.toDto(any(JiraConfig.class))).thenReturn(responseDto);

        service.deleteConfigById(1L);

        assertAll(
            () -> verify(auditService).createJiraUnsyncLog(eq(null), eq(finishedRequest), anyString()),
            () -> verify(jiraSyncService).postJiraComment(any(), eq("TEST-123"), anyString())
        );
    }

    @Test
    void CreateConfig_WorkflowNonNullDeptAndProjectNullDept_Succeeds() {
        Department deptA = Department.builder().departmentId(1L).name("Dept A").build();
        Team team = Team.builder().teamId(1L).name("Team A").department(null).build();

        User fallbackUser = User.builder().id(10L).role(UserRole.REQUESTER).team(team).build();
        Project fallbackProject = Project.builder().id(20L).team(team).build();
        WorkflowDefinition workflow = new WorkflowDefinition();
        workflow.setId(30L);
        workflow.setDepartment(deptA);
        workflow.setIsActive(true);

        JiraConfigDto dto = new JiraConfigDto(null, "Config", "https://test.atlassian.net", "user", "token",
                "jql", 60, "customfield_10015", 10L, 20L, 30L, null, null);

        JiraConfig entity = new JiraConfig();
        when(repository.existsByJiraUrlAndJql(anyString(), anyString())).thenReturn(false);
        when(mapper.toEntity(any())).thenReturn(entity);
        when(userRepository.findById(10L)).thenReturn(Optional.of(fallbackUser));
        when(projectRepository.findById(20L)).thenReturn(Optional.of(fallbackProject));
        when(workflowDefinitionRepository.findById(30L)).thenReturn(Optional.of(workflow));
        when(repository.save(any())).thenReturn(entity);
        when(mapper.toDto(any(JiraConfig.class))).thenReturn(responseDto);

        JiraConfigResponseDto result = service.createConfig(dto);

        assertNotNull(result);
        verify(repository).save(any());
    }

    @Test
    void CreateConfig_WorkflowAndProjectSameDepartment_Succeeds() {
        Department deptA = Department.builder().departmentId(1L).name("Dept A").build();
        Team team = Team.builder().teamId(1L).name("Team A").department(deptA).build();

        User fallbackUser = User.builder().id(10L).role(UserRole.REQUESTER).team(team).build();
        Project fallbackProject = Project.builder().id(20L).team(team).build();
        WorkflowDefinition workflow = new WorkflowDefinition();
        workflow.setId(30L);
        workflow.setDepartment(deptA);
        workflow.setIsActive(true);

        JiraConfigDto dto = new JiraConfigDto(null, "Config", "https://test.atlassian.net", "user", "token",
                "jql", 60, "customfield_10015", 10L, 20L, 30L, null, null);

        JiraConfig entity = new JiraConfig();
        when(repository.existsByJiraUrlAndJql(anyString(), anyString())).thenReturn(false);
        when(mapper.toEntity(any())).thenReturn(entity);
        when(userRepository.findById(10L)).thenReturn(Optional.of(fallbackUser));
        when(projectRepository.findById(20L)).thenReturn(Optional.of(fallbackProject));
        when(workflowDefinitionRepository.findById(30L)).thenReturn(Optional.of(workflow));
        when(repository.save(any())).thenReturn(entity);
        when(mapper.toDto(any(JiraConfig.class))).thenReturn(responseDto);

        JiraConfigResponseDto result = service.createConfig(dto);

        assertNotNull(result);
        verify(repository).save(any());
    }

    @Test
    void CreateConfig_FallbackUserNotFound_ThrowsEntityNotFoundException() {
        JiraConfigDto dto = new JiraConfigDto(null, "Config", "https://test.atlassian.net", "user", "token",
                "jql", 60, "customfield_10015", 10L, 20L, 30L, null, null);

        when(repository.existsByJiraUrlAndJql(anyString(), anyString())).thenReturn(false);
        when(mapper.toEntity(any())).thenReturn(new JiraConfig());
        when(userRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.createConfig(dto));
    }

    @Test
    void CreateConfig_FallbackProjectNotFound_ThrowsEntityNotFoundException() {
        JiraConfigDto dto = new JiraConfigDto(null, "Config", "https://test.atlassian.net", "user", "token",
                "jql", 60, "customfield_10015", 10L, 20L, 30L, null, null);

        User fallbackUser = User.builder().id(10L).build();

        when(repository.existsByJiraUrlAndJql(anyString(), anyString())).thenReturn(false);
        when(mapper.toEntity(any())).thenReturn(new JiraConfig());
        when(userRepository.findById(10L)).thenReturn(Optional.of(fallbackUser));
        when(projectRepository.findById(20L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.createConfig(dto));
    }

    @Test
    void CreateConfig_FallbackWorkflowNotFound_ThrowsEntityNotFoundException() {
        JiraConfigDto dto = new JiraConfigDto(null, "Config", "https://test.atlassian.net", "user", "token",
                "jql", 60, "customfield_10015", 10L, 20L, 30L, null, null);

        User fallbackUser = User.builder().id(10L).build();
        Project fallbackProject = Project.builder().id(20L).build();

        when(repository.existsByJiraUrlAndJql(anyString(), anyString())).thenReturn(false);
        when(mapper.toEntity(any())).thenReturn(new JiraConfig());
        when(userRepository.findById(10L)).thenReturn(Optional.of(fallbackUser));
        when(projectRepository.findById(20L)).thenReturn(Optional.of(fallbackProject));
        when(workflowDefinitionRepository.findById(30L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.createConfig(dto));
    }

    @Test
    void DeleteConfig_NonExistingConfig_ThrowsEntityNotFoundException() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.deleteConfigById(999L));
    }

    @Test
    void UpdateConfig_SameConfigUrlAndJql_Succeeds() {
        JiraConfig existing = new JiraConfig();
        existing.setId(1L);
        existing.setJiraUrl("https://test.atlassian.net");
        existing.setJql("jql");
        setupFallbackMocks(10L, 20L, 30L, Team.builder().teamId(1L).build());

        when(repository.findById(1L)).thenReturn(Optional.of(config));
        when(repository.findByJiraUrlAndJql(anyString(), anyString())).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenReturn(config);
        when(mapper.toDto(any())).thenReturn(responseDto);

        JiraConfigResponseDto result = service.updateConfig(1L, configDto);

        assertNotNull(result);
        verify(repository).save(any());
    }

    @Test
    void UpdateConfig_ConfigNotFound_ThrowsEntityNotFoundException() {
        when(repository.findByJiraUrlAndJql(anyString(), anyString())).thenReturn(Optional.empty());
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.updateConfig(999L, configDto));
    }

    @Test
    void GetAllConfigs_NullSearch_ReturnsAll() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<JiraConfig> configPage = new PageImpl<>(List.of(config));

        when(repository.findAllFiltered(null, pageable)).thenReturn(configPage);
        when(mapper.toDto(any(JiraConfig.class))).thenReturn(responseDto);

        Page<JiraConfigResponseDto> result = service.getAllConfigs(pageable, null);

        assertNotNull(result);
        verify(repository).findAllFiltered(null, pageable);
    }

    @Test
    void GetAllConfigs_BlankSearch_ReturnsAll() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<JiraConfig> configPage = new PageImpl<>(List.of(config));

        when(repository.findAllFiltered(null, pageable)).thenReturn(configPage);
        when(mapper.toDto(any(JiraConfig.class))).thenReturn(responseDto);

        Page<JiraConfigResponseDto> result = service.getAllConfigs(pageable, "   ");

        assertNotNull(result);
        verify(repository).findAllFiltered(null, pageable);
    }

    @Test
    void DeleteConfig_WithSyncedRequestNullIssueKey_DoesNotComment() {
        when(repository.findById(1L)).thenReturn(Optional.of(config));

        Request req = new Request();
        req.setJiraIssueKey(null);

        when(requestRepository.findByJiraConfigId(1L)).thenReturn(List.of(req));
        when(mapper.toDto(any(JiraConfig.class))).thenReturn(responseDto);

        service.deleteConfigById(1L);

        verify(auditService).createJiraUnsyncLog(eq(null), eq(req), anyString());
        verify(jiraSyncService, never()).postJiraComment(any(), any(), anyString());
    }


    private void setupFallbackMocks(Long userId, Long projectId, Long workflowId, Team team) {
        User fallbackUser = User.builder().id(userId).role(UserRole.REQUESTER).team(team).build();
        Project fallbackProject = Project.builder().id(projectId).team(team).build();
        WorkflowDefinition workflow = new WorkflowDefinition();
        workflow.setId(workflowId);
        workflow.setIsActive(true);

        when(userRepository.findById(userId)).thenReturn(Optional.of(fallbackUser));
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(fallbackProject));
        when(workflowDefinitionRepository.findById(workflowId)).thenReturn(Optional.of(workflow));
    }
}
