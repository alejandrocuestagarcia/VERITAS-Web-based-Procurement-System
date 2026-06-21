package com.veritas.backend.requisition.service;

import java.util.ArrayList;

import static com.veritas.backend.common.model.AuditActionConstants.CANCEL;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;

import com.veritas.backend.audit.service.AuditService;
import com.veritas.backend.budget.entity.BudgetType;
import com.veritas.backend.budget.entity.InternalBudget;
import com.veritas.backend.budget.repository.InternalBudgetRepository;
import com.veritas.backend.notification.entity.NotificationType;
import com.veritas.backend.notification.service.NotificationService;
import com.veritas.backend.project.entity.Project;
import com.veritas.backend.project.repository.ProjectRepository;
import com.veritas.backend.requisition.dto.InvoiceDto;
import com.veritas.backend.requisition.dto.InvoiceCreateDto;
import com.veritas.backend.requisition.dto.RequisitionCreateDto;
import jakarta.persistence.EntityExistsException;
import org.mockito.*;
import com.veritas.backend.requisition.dto.RequisitionDto;
import com.veritas.backend.requisition.dto.RequisitionItemCreateDto;
import com.veritas.backend.requisition.dto.RequisitionUpdateDto;
import com.veritas.backend.requisition.dto.RequisitionRejectDto;
import com.veritas.backend.requisition.entity.*;
import org.junit.jupiter.api.AfterEach;
import org.springframework.security.access.AccessDeniedException;

import com.veritas.backend.requisition.mapper.InvoiceMapper;
import com.veritas.backend.requisition.mapper.RequisitionMapper;
import com.veritas.backend.requisition.repository.AttachmentRepository;
import com.veritas.backend.requisition.repository.InvoiceRepository;
import com.veritas.backend.requisition.repository.RequestItemRepository;
import com.veritas.backend.requisition.repository.RequestRepository;
import com.veritas.backend.vendor.repository.QuoteLineItemRepository;
import com.veritas.backend.vendor.repository.QuoteRepository;
import com.veritas.backend.vendor.entity.Quote;
import com.veritas.backend.vendor.entity.Vendor;
import com.veritas.backend.requisition.service.impl.RequisitionServiceImpl;
import com.veritas.backend.department.entity.Department;
import com.veritas.backend.team.entity.Team;
import com.veritas.backend.user.entity.User;
import com.veritas.backend.user.entity.UserRole;
import com.veritas.backend.user.repository.UserRepository;
import com.veritas.backend.user.mapper.UserMapper;
import com.veritas.backend.user.dto.UserDto;
import com.veritas.backend.workflow.entity.WorkflowComponent;
import com.veritas.backend.workflow.entity.WorkflowDefinition;
import com.veritas.backend.workflow.entity.WorkflowStep;
import com.veritas.backend.workflow.repository.WorkflowDefinitionRepository;
import com.veritas.backend.workflow.repository.WorkflowStepRepository;
import com.veritas.backend.workflow.service.WorkflowEngineService;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.mockito.Mockito.doThrow;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.PageImpl;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;

import java.nio.file.Path;
import java.nio.file.Files;

import com.veritas.backend.common.exception.WorkflowStateException;
import com.veritas.backend.integrations.currency.dto.CurrencyConversionResult;
import com.veritas.backend.integrations.currency.entity.Currency;
import com.veritas.backend.integrations.currency.entity.ExchangeRateSource;
import com.veritas.backend.integrations.currency.service.CurrencyConversionService;
import com.veritas.backend.integrations.jira.service.JiraSyncService;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class RequisitionServiceUnitTest {

    @Mock
    private RequestRepository requestRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private WorkflowDefinitionRepository workflowDefinitionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RequestItemRepository requestItemRepository;
    @Mock
    private AttachmentRepository attachmentRepository;
    @Mock
    private WorkflowStepRepository workflowStepRepository;
    @Mock
    private RequisitionMapper requisitionMapper;
    @Mock
    private InvoiceMapper invoiceMapper;
    @Mock
    private WorkflowEngineService workflowEngineService;
    @Mock
    private InternalBudgetRepository internalBudgetRepository;
    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private QuoteLineItemRepository quoteLineItemRepository;
    @Mock
    private QuoteRepository quoteRepository;
    @Mock
    private AuditService auditService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private CurrencyConversionService currencyConversionService;
    @Mock
    private JiraSyncService jiraSyncService;
    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private RequisitionServiceImpl requisitionService;

    @Captor
    private ArgumentCaptor<Request> requestCaptor;
    @Captor
    private ArgumentCaptor<RequestItem> itemCaptor;
    @Captor
    private ArgumentCaptor<Attachment> attachmentCaptor;
    @Captor
    private ArgumentCaptor<Invoice> invoiceCaptor;

    private User testUser;
    private Project testProject;
    private WorkflowDefinition testWorkflow;
    private WorkflowStep testStartStep;
    private Team testTeam;

    @BeforeEach
    void setUp() {
        testTeam = new Team();
        testTeam.setTeamId(1L);
        testTeam.setName("Engineering");

        testUser = new User();
        testUser.setId(1L);
        testUser.setName("Test User");
        testUser.setTeam(testTeam);
        testUser.setRole(UserRole.FINANCE_OFFICER);

        testProject = new Project();
        testProject.setId(1L);
        testProject.setProjectKey("PRJ");
        testProject.setName("Test Project");
        testProject.setRequestCounter(10);

        testWorkflow = new WorkflowDefinition();
        testWorkflow.setId(1L);
        testWorkflow.setName("Standard Workflow");

        testStartStep = new WorkflowStep();
        testStartStep.setId(1L);
        testStartStep.setName("Start");
        testStartStep.setWorkflowComponent(WorkflowComponent.START_EVENT);
        testStartStep.setWorkflowDefinition(testWorkflow);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // createRequest tests

    private void stupRepositories() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(workflowDefinitionRepository.findById(1L)).thenReturn(Optional.of(testWorkflow));
        when(workflowStepRepository.findFirstByWorkflowDefinitionAndWorkflowComponent(
                testWorkflow, WorkflowComponent.START_EVENT))
                .thenReturn(Optional.of(testStartStep));
        when(requestRepository.save(any(Request.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(internalBudgetRepository.save(any(InternalBudget.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void CreateRequest_ValidInput_SavesAndReturnsDto() {
        stupRepositories();
        RequisitionDto expectedDto = RequisitionDto.builder()
                .id(1L)
                .requestName("New Laptop")
                .requestKey("PRJ-11")
                .status("Start")
                .isClosed(false)
                .priority(Priority.MEDIUM)
                .projectName("Test Project")
                .projectKey("PRJ")
                .workflowName("Standard Workflow")
                .teamName("Engineering")
                .requesterName("Test User")
                .requesterId(1L)
                .requesterTeamId(1L)
                .state("")
                .isPaid(false)
                .build();
        when(requisitionMapper.toDto(any(Request.class))).thenReturn(expectedDto);

        RequisitionCreateDto createDto = new RequisitionCreateDto(
                "New Laptop", "Need a laptop", 1L, 1L, Priority.MEDIUM,
                List.of(new RequisitionItemCreateDto("MacBook Pro", 2, RequestItemUnit.PIECES, "test")));

        RequisitionDto result = requisitionService.createRequest(createDto, testUser);

        assertNotNull(result);
        assertEquals("New Laptop", result.requestName());
        verify(requestRepository).save(requestCaptor.capture());
        Request saved = requestCaptor.getValue();
        assertEquals("New Laptop", saved.getRequestName());
        assertEquals("Need a laptop", saved.getDescription());
        assertEquals(Priority.MEDIUM, saved.getPriority());
        assertEquals(testStartStep, saved.getCurrentStep());
        assertEquals(testUser, saved.getUser());
        assertEquals(testTeam, saved.getTeam());
        assertEquals("PRJ-11", saved.getRequestKey());
        assertEquals(11, testProject.getRequestCounter());
        verify(projectRepository).save(testProject);
    }

    @Test
    void CreateRequest_WithMultipleItems_SavesAllItems() {
        stupRepositories();
        when(requisitionMapper.toDto(any())).thenReturn(mock(RequisitionDto.class));

        List<RequisitionItemCreateDto> items = List.of(
                new RequisitionItemCreateDto("Monitor", 3, RequestItemUnit.PIECES, "27 inch"),
                new RequisitionItemCreateDto("Keyboard", 5, RequestItemUnit.PIECES, "Mechanical"),
                new RequisitionItemCreateDto("Cable", 10, RequestItemUnit.KG, "USB-C"));

        RequisitionCreateDto createDto = new RequisitionCreateDto(
                "Office Equipment", "Test for employees", 1L, 1L, Priority.MEDIUM, items);

        requisitionService.createRequest(createDto, testUser);

        verify(requestItemRepository, times(3)).save(itemCaptor.capture());
        List<RequestItem> savedItems = itemCaptor.getAllValues();
        assertEquals("Monitor", savedItems.get(0).getName());
        assertEquals(3, savedItems.get(0).getQuantity());
        assertEquals(RequestItemUnit.PIECES, savedItems.get(0).getUnit());
        assertEquals("27 inch", savedItems.get(0).getDescription());
        assertEquals("Cable", savedItems.get(2).getName());
        assertEquals(RequestItemUnit.KG, savedItems.get(2).getUnit());
    }

    @Test
    void CreateRequest_NullOrEmptyItems_SkipsItemCreation() {
        stupRepositories();
        when(requisitionMapper.toDto(any())).thenReturn(mock(RequisitionDto.class));

        requisitionService.createRequest(new RequisitionCreateDto(
                "Request A", null, 1L, 1L, Priority.LOW, null), testUser);
        verify(requestItemRepository, never()).save(any());

        requisitionService.createRequest(new RequisitionCreateDto(
                "Request B", null, 1L, 1L, Priority.LOW, Collections.emptyList()), testUser);
        verify(requestItemRepository, never()).save(any());
    }

    @Test
    void CreateRequest_UserNotFound_ThrowsIllegalArgument() {
        when(userRepository.findById(any())).thenReturn(Optional.empty());

        RequisitionCreateDto createDto = new RequisitionCreateDto(
                "Test", null, 1L, 1L, Priority.LOW,
                List.of(new RequisitionItemCreateDto("Item", 1, RequestItemUnit.PIECES, null)));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> requisitionService.createRequest(createDto, testUser));
        assertTrue(ex.getMessage().contains("Authenticated user not found"));
        verify(requestRepository, never()).save(any());
    }

    @Test
    void CreateRequest_ProjectNotFound_ThrowsIllegalArgument() {
        when(userRepository.findById(any())).thenReturn(Optional.of(testUser));
        when(projectRepository.findById(any())).thenReturn(Optional.empty());

        RequisitionCreateDto createDto = new RequisitionCreateDto(
                "Test", null, 999L, 1L, Priority.LOW,
                List.of(new RequisitionItemCreateDto("Item", 1, RequestItemUnit.PIECES, null)));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> requisitionService.createRequest(createDto, testUser));
        assertTrue(ex.getMessage().contains("Project not found"));
        verify(requestRepository, never()).save(any());
    }

    @Test
    void CreateRequest_WorkflowNotFound_ThrowsIllegalArgument() {
        when(userRepository.findById(any())).thenReturn(Optional.of(testUser));
        when(projectRepository.findById(any())).thenReturn(Optional.of(testProject));
        when(workflowDefinitionRepository.findById(any())).thenReturn(Optional.empty());

        RequisitionCreateDto createDto = new RequisitionCreateDto(
                "Test", null, 1L, 999L, Priority.LOW,
                List.of(new RequisitionItemCreateDto("Item", 1, RequestItemUnit.PIECES, null)));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> requisitionService.createRequest(createDto, testUser));
        assertTrue(ex.getMessage().contains("Workflow not found"));
        verify(requestRepository, never()).save(any());
    }

    @Test
    void CreateRequest_WorkflowMissingStartEvent_ThrowsIllegalState() {
        when(userRepository.findById(any())).thenReturn(Optional.of(testUser));
        when(projectRepository.findById(any())).thenReturn(Optional.of(testProject));
        when(workflowDefinitionRepository.findById(any())).thenReturn(Optional.of(testWorkflow));
        when(workflowStepRepository.findFirstByWorkflowDefinitionAndWorkflowComponent(any(), any()))
                .thenReturn(Optional.empty());

        RequisitionCreateDto createDto = new RequisitionCreateDto(
                "Test", null, 1L, 1L, Priority.LOW,
                List.of(new RequisitionItemCreateDto("Item", 1, RequestItemUnit.PIECES, null)));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> requisitionService.createRequest(createDto, testUser));
        assertTrue(ex.getMessage().contains("START_EVENT"));
        verify(requestRepository, never()).save(any());
    }

    // saveAttachment tests

    @Test
    void SaveAttachment_ValidFile_PersistsAttachmentMetadata() {
        Request request = new Request();
        request.setRequestID(1L);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));

        MockMultipartFile file = new MockMultipartFile(
                "file", "quote.pdf", "application/pdf", "PDF content".getBytes());

        requisitionService.saveAttachment(1L, file, testUser);

        verify(attachmentRepository).save(attachmentCaptor.capture());
        Attachment saved = attachmentCaptor.getValue();
        assertEquals("quote.pdf", saved.getFileName());
        assertEquals("application/pdf", saved.getFileType());
        assertEquals(request, saved.getRequest());
        assertEquals(11L, saved.getFileSize());
        assertNotNull(saved.getStoragePath());
        assertTrue(saved.getStoragePath().endsWith(".pdf"));
    }

    @Test
    void SaveAttachment_FileWithoutExtension_SavesWithoutExtension() {
        Request request = new Request();
        request.setRequestID(2L);
        when(requestRepository.findById(2L)).thenReturn(Optional.of(request));

        MockMultipartFile file = new MockMultipartFile(
                "file", "README", "text/plain", "content".getBytes());

        requisitionService.saveAttachment(2L, file, testUser);

        verify(attachmentRepository).save(attachmentCaptor.capture());
        assertEquals("README", attachmentCaptor.getValue().getFileName());
        assertFalse(attachmentCaptor.getValue().getStoragePath().contains("."));
    }

    @Test
    void SaveAttachment_RequestNotFound_ThrowsIllegalArgument() {
        when(requestRepository.findById(any())).thenReturn(Optional.empty());
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "Hello".getBytes());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> requisitionService.saveAttachment(999L, file, testUser));
        assertTrue(ex.getMessage().contains("Request not found"));
        verify(attachmentRepository, never()).save(any());
    }

    @Test
    void SaveAttachment_IoException_ThrowsRuntimeException() throws IOException {
        Request request = new Request();
        request.setRequestID(1L);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));

        MockMultipartFile file = mock(MockMultipartFile.class);
        when(file.getInputStream()).thenThrow(new IOException("Disk full"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> requisitionService.saveAttachment(1L, file, testUser));
        assertTrue(ex.getMessage().contains("Could not store file"));
        verify(attachmentRepository, never()).save(any());
    }

    @Test
    void GetRequests_WithStatus_FormatsStatusToUpperCaseAndCallsRepository() {
        testUser.setRole(UserRole.ADMINISTRATOR);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        Pageable pageable = PageRequest.of(0, 10);
        when(requestRepository.findFilteredRequests("OPEN", "search", 1L, null, null, null, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of()));

        requisitionService.getRequests("open", "search", 1L, null, null, null, testUser, pageable);

        verify(requestRepository).findFilteredRequests("OPEN", "search", 1L, null, null, null, null, null, null, pageable);
    }

    @Test
    void GetRequests_WithNullStatus_CallsRepositoryWithNull() {
        testUser.setRole(UserRole.ADMINISTRATOR);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        Pageable pageable = PageRequest.of(0, 10);
        when(requestRepository.findFilteredRequests(null, null, null, null, null, null, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of()));

        requisitionService.getRequests("", "", null, null, null, null, testUser, pageable);

        verify(requestRepository).findFilteredRequests(null, null, null, null, null, null, null, null, null, pageable);
    }

    @Test
    void GetRequestById_ValidId_ReturnsMappedDto() {
        Request request = new Request();
        request.setRequestID(1L);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));

        RequisitionDto expectedDto = mock(RequisitionDto.class);
        when(requisitionMapper.toDto(request)).thenReturn(expectedDto);

        RequisitionDto result = requisitionService.getRequestById(1L, testUser);

        assertNotNull(result);
        assertEquals(expectedDto, result);
    }

    @Test
    void GetRequestById_InvalidId_ThrowsEntityNotFoundException() {
        when(requestRepository.findById(999L)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> requisitionService.getRequestById(999L, testUser));
        assertTrue(ex.getMessage().contains("not found"));
    }

    @Test
    void DownloadAttachment_ValidId_ReturnsResponseEntityWithResource() throws IOException {
        Attachment attachment = new Attachment();
        attachment.setAttachmentId(1L);
        attachment.setFileName("test.pdf");
        attachment.setFileType("application/pdf");

        Path tempFile = Files.createTempFile("test", ".pdf");
        attachment.setStoragePath(tempFile.toString());

        when(attachmentRepository.findById(1L)).thenReturn(Optional.of(attachment));

        ResponseEntity<Resource> response = requisitionService.downloadAttachment(1L, testUser);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("application/pdf", response.getHeaders().getContentType().toString());
        assertTrue(response.getHeaders().get(HttpHeaders.CONTENT_DISPOSITION).get(0).contains("filename=\"test.pdf\""));

        Files.deleteIfExists(tempFile);
    }

    @Test
    void DownloadAttachment_InvalidId_ThrowsEntityNotFoundException() {
        when(attachmentRepository.findById(999L)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> requisitionService.downloadAttachment(999L, testUser));
        assertTrue(ex.getMessage().contains("Attachment not found"));
    }

    //AI-Generated
    @Test
    void SubmitRequest_ValidDraft_MovesToFirstStep() {
        // 1. Arrange a mock Request currently in DRAFT status
        Request request = new Request();
        request.setRequestID(100L);
        request.setState(RequestStatus.DRAFT);
        request.setWorkflowDefinition(testWorkflow);

        when(requestRepository.findById(100L)).thenReturn(Optional.of(request));

        // Mock finding the start event scoped specifically to this definition ID

        when(requestRepository.save(any(Request.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RequisitionDto expectedDto = RequisitionDto.builder()
                .id(100L)
                .requestName("Draft Test")
                .requestKey("PRJ-12")
                .status("Start")
                .isClosed(false)
                .priority(Priority.MEDIUM)
                .projectName("Test Project")
                .projectKey("PRJ")
                .workflowName("Standard Workflow")
                .teamName("Engineering")
                .requesterName("Test User")
                .requesterId(1L)
                .requesterTeamId(1L)
                .responsibleRole("ROLE_MANAGER")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .description("Test Description")
                .jiraIssueKey("JIRA-123")
                .jiraIssueUrl("https://jira.com/123")
                .state("ACTIVE")
                .isPaid(false)
                .build();
        when(requisitionMapper.toDto(any(Request.class))).thenReturn(expectedDto);

        doAnswer(invocation -> {
            Request req = invocation.getArgument(0);
            req.setCurrentStep(testStartStep); // Simulates what startWorkflow actually does
            return null;
        }).when(workflowEngineService).startWorkflow(any(Request.class), any(User.class), any());
        // 2. Act
        RequisitionDto result = requisitionService.submitRequest(100L, testUser, null);

        // 3. Assert
        assertNotNull(result);
        verify(requestRepository).save(requestCaptor.capture());
        Request savedRequest = requestCaptor.getValue();

        assertAll("Workflow Initial Submission State Checks",
                () -> assertEquals(RequestStatus.ACTIVE, savedRequest.getState(), "Request state should change to ACTIVE on submit"),
                () -> assertEquals(testStartStep, savedRequest.getCurrentStep(), "Request should advance cleanly to the workflow's START_EVENT node")
        );
    }

    //AI-Generated
    @Test
    void SubmitRequest_AlreadyActive_ThrowsWorkflowStateException() {
        // 1. Arrange an already ACTIVE request
        Request request = new Request();
        request.setRequestID(101L);
        request.setState(RequestStatus.ACTIVE);

        when(requestRepository.findById(101L)).thenReturn(Optional.of(request));

        // 2. Act & Assert
        WorkflowStateException ex = assertThrows(
                WorkflowStateException.class,
                () -> requisitionService.submitRequest(101L, testUser, null)
        );

        assertTrue(ex.getMessage().contains("Only drafts can be submitted."));
        verify(requestRepository, never()).save(any());
    }

    @Test
    void ChangeRequester_ValidInput_UpdatesRequesterAndReturnsDto() {
        Team team = new Team();
        team.setTeamId(10L);

        User currentRequester = new User();
        currentRequester.setId(1L);
        currentRequester.setRole(UserRole.REQUESTER);
        currentRequester.setTeam(team);

        User newRequester = new User();
        newRequester.setId(2L);
        newRequester.setName("New Requester");
        newRequester.setRole(UserRole.REQUESTER);
        newRequester.setTeam(team);

        Request request = new Request();
        request.setRequestID(1L);
        request.setUser(currentRequester);

        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(userRepository.findById(2L)).thenReturn(Optional.of(newRequester));
        when(requestRepository.save(any(Request.class))).thenAnswer(inv -> inv.getArgument(0));

        RequisitionDto expectedDto = mock(RequisitionDto.class);
        when(requisitionMapper.toDto(any(Request.class))).thenReturn(expectedDto);

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(currentRequester);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);

        RequisitionDto result = requisitionService.changeRequester(1L, 2L);

        assertNotNull(result);
        verify(requestRepository).save(requestCaptor.capture());
        assertEquals(newRequester, requestCaptor.getValue().getUser());
    }

    @Test
    void ChangeRequester_RequestNotFound_ThrowsIllegalArgument() {
        when(requestRepository.findById(99L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> requisitionService.changeRequester(99L, 2L));
        assertTrue(ex.getMessage().contains("Request not found with id: 99"));
        verify(requestRepository, never()).save(any());
    }

    @Test
    void ChangeRequester_NullNewRequesterId_ThrowsIllegalArgument() {
        Request request = new Request();
        request.setRequestID(1L);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> requisitionService.changeRequester(1L, null));
        assertTrue(ex.getMessage().contains("New assigned user must be stated"));
        verify(requestRepository, never()).save(any());
    }

    @Test
    void ChangeRequester_NewRequesterNotFound_ThrowsEntityNotFoundException() {
        Team team = new Team();
        team.setTeamId(10L);

        User currentRequester = new User();
        currentRequester.setId(1L);
        currentRequester.setTeam(team);

        Request request = new Request();
        request.setRequestID(1L);
        request.setUser(currentRequester);

        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> requisitionService.changeRequester(1L, 99L));
        verify(requestRepository, never()).save(any());
    }

    @Test
    void ChangeRequester_NewRequesterWrongRole_ThrowsIllegalArgument() {
        Team team = new Team();
        team.setTeamId(10L);

        User currentRequester = new User();
        currentRequester.setId(1L);
        currentRequester.setRole(UserRole.REQUESTER);
        currentRequester.setTeam(team);

        User manager = new User();
        manager.setId(2L);
        manager.setRole(UserRole.PROCUREMENT_OFFICER);
        manager.setTeam(team);

        Request request = new Request();
        request.setRequestID(1L);
        request.setUser(currentRequester);

        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(userRepository.findById(2L)).thenReturn(Optional.of(manager));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> requisitionService.changeRequester(1L, 2L));
        assertTrue(ex.getMessage().contains("New assigned user must be a requester from the same team"));
        verify(requestRepository, never()).save(any());
    }

    @Test
    void ChangeRequester_NewRequesterDifferentTeam_ThrowsIllegalArgument() {
        Team teamA = new Team();
        teamA.setTeamId(10L);

        Team teamB = new Team();
        teamB.setTeamId(20L);

        User currentRequester = new User();
        currentRequester.setId(1L);
        currentRequester.setRole(UserRole.REQUESTER);
        currentRequester.setTeam(teamA);

        User newRequester = new User();
        newRequester.setId(2L);
        newRequester.setRole(UserRole.REQUESTER);
        newRequester.setTeam(teamB);

        Request request = new Request();
        request.setRequestID(1L);
        request.setUser(currentRequester);

        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(userRepository.findById(2L)).thenReturn(Optional.of(newRequester));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> requisitionService.changeRequester(1L, 2L));
        assertTrue(ex.getMessage().contains("New assigned user must be a requester from the same team"));
        verify(requestRepository, never()).save(any());
    }

    //AI-Generated
    @Test
    void UpdateRequest_ValidInput_SavesAndReturnsDto() {
        Request request = new Request();
        request.setRequestID(1L);
        request.setState(RequestStatus.DRAFT);
        request.setUser(testUser);
        request.setProject(testProject);
        request.setWorkflowDefinition(testWorkflow);

        InternalBudget budget = new InternalBudget();
        budget.setBudgetName("Request: Old Name");
        request.setBudget(budget);

        RequisitionUpdateDto updates = new RequisitionUpdateDto(
                "Updated Laptop", "Need an updated laptop", 1L, 1L, Priority.HIGH,
                List.of(new RequisitionItemCreateDto("MacBook Pro 16", 1, RequestItemUnit.PIECES, "updated")));

        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(requestRepository.save(any(Request.class))).thenAnswer(inv -> inv.getArgument(0));
        RequisitionDto expectedDto = mock(RequisitionDto.class);
        when(requisitionMapper.toDto(any(Request.class))).thenReturn(expectedDto);

        RequisitionDto result = requisitionService.updateRequest(1L, updates, testUser);


        assertAll("Requisition update validation",
                () -> assertNotNull(result, "Resulting DTO should not be null"),
                () -> assertEquals(expectedDto, result, "Returned DTO should match expected mock output"),
                () -> assertEquals("Updated Laptop", request.getRequestName(), "Request name should be updated"),
                () -> assertEquals("Need an updated laptop", request.getDescription(), "Description should be updated"),
                () -> assertEquals(Priority.HIGH, request.getPriority(), "Priority should be updated to HIGH"),
                () -> assertEquals("Updated Laptop", budget.getBudgetName(), "Internal budget name should be synchronized with new request name")
        );
        verify(requestRepository).save(request);
    }

    //AI-Generated
    @Test
    void UpdateRequest_NotDraft_ThrowsWorkflowStateException() {
        Request request = new Request();
        request.setRequestID(1L);
        request.setState(RequestStatus.FINISHED);
        request.setUser(testUser);

        RequisitionUpdateDto updates = new RequisitionUpdateDto(
                "Updated", "Desc", 1L, 1L, Priority.HIGH, List.of());

        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));

        assertThrows(WorkflowStateException.class,
                () -> requisitionService.updateRequest(1L, updates, testUser));
        verify(requestRepository, never()).save(any());
    }

    //AI-Generated
    @Test
    void UpdateRequest_NotOwner_ThrowsAccessDeniedException() {
        Request request = new Request();
        request.setRequestID(1L);
        request.setState(RequestStatus.DRAFT);
        request.setUser(testUser);

        User anotherUser = new User();
        anotherUser.setId(999L);

        RequisitionUpdateDto updates = new RequisitionUpdateDto(
                "Updated", "Desc", 1L, 1L, Priority.HIGH, List.of());

        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));

        assertThrows(AccessDeniedException.class,
                () -> requisitionService.updateRequest(1L, updates, anotherUser));
        verify(requestRepository, never()).save(any());
    }

    //AI-Generated
    @Test
    void UpdateRequest_FieldsChanged_CreatesAuditLog() {
        Request request = new Request();
        request.setRequestID(1L);
        request.setState(RequestStatus.DRAFT);
        request.setUser(testUser);
        request.setProject(testProject);
        request.setWorkflowDefinition(testWorkflow);
        request.setRequestName("Old Laptop");
        request.setDescription("Need old laptop");
        request.setPriority(Priority.LOW);

        RequestItem currentItem = new RequestItem();
        currentItem.setName("Old Item");
        currentItem.setQuantity(5);
        currentItem.setUnit(RequestItemUnit.PIECES);
        currentItem.setDescription("old details");
        request.setItems(new ArrayList<>(List.of(currentItem)));

        Project newProject = new Project();
        newProject.setId(2L);
        newProject.setProjectKey("NEWPRJ");
        newProject.setName("NEWPRJ");
        newProject.setRequestCounter(5);

        RequisitionUpdateDto updates = new RequisitionUpdateDto(
                "Updated Laptop", "Need an updated laptop", 2L, 1L, Priority.HIGH,
                List.of(new RequisitionItemCreateDto("MacBook Pro 16", 1, RequestItemUnit.PIECES, "updated")));

        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(projectRepository.findById(2L)).thenReturn(Optional.of(newProject));
        when(requestRepository.save(any(Request.class))).thenAnswer(inv -> inv.getArgument(0));
        when(requisitionMapper.toDto(any(Request.class))).thenReturn(mock(RequisitionDto.class));

        requisitionService.updateRequest(1L, updates, testUser);

        ArgumentCaptor<String> detailsCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditService).createRequisitionChangeLog(eq(testUser), eq(request), detailsCaptor.capture());

        String details = detailsCaptor.getValue();
        assertAll("Audit log details checks",
                () -> assertTrue(details.contains("Request Name changed from 'Old Laptop' to 'Updated Laptop'")),
                () -> assertTrue(details.contains("Description changed from 'Need old laptop' to 'Need an updated laptop'")),
                () -> assertTrue(details.contains("Priority changed from 'LOW' to 'HIGH'")),
                () -> assertTrue(details.contains("Project changed from 'Test Project' to 'NEWPRJ'")),
                () -> assertTrue(details.contains("Line Items changed"))
        );
    }

    @Test
    void UpdateRequest_DescriptionSet_CreatesCorrectAuditLog() {
        Request request = new Request();
        request.setRequestID(1L);
        request.setState(RequestStatus.DRAFT);
        request.setUser(testUser);
        request.setProject(testProject);
        request.setWorkflowDefinition(testWorkflow);
        request.setRequestName("Laptop");
        request.setDescription(null);
        request.setPriority(Priority.LOW);
        request.setItems(new ArrayList<>());

        RequisitionUpdateDto updates = new RequisitionUpdateDto(
                "Laptop", "Need laptop", 1L, 1L, Priority.LOW, List.of());

        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(requestRepository.save(any(Request.class))).thenAnswer(inv -> inv.getArgument(0));
        when(requisitionMapper.toDto(any(Request.class))).thenReturn(mock(RequisitionDto.class));

        requisitionService.updateRequest(1L, updates, testUser);

        ArgumentCaptor<String> detailsCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditService).createRequisitionChangeLog(eq(testUser), eq(request), detailsCaptor.capture());

        String details = detailsCaptor.getValue();
        assertTrue(details.contains("Description set to 'Need laptop'"));
    }

    @Test
    void UpdateRequest_DescriptionCleared_CreatesCorrectAuditLog() {
        Request request = new Request();
        request.setRequestID(1L);
        request.setState(RequestStatus.DRAFT);
        request.setUser(testUser);
        request.setProject(testProject);
        request.setWorkflowDefinition(testWorkflow);
        request.setRequestName("Laptop");
        request.setDescription("Need laptop");
        request.setPriority(Priority.LOW);
        request.setItems(new ArrayList<>());

        RequisitionUpdateDto updates = new RequisitionUpdateDto(
                "Laptop", null, 1L, 1L, Priority.LOW, List.of());

        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(requestRepository.save(any(Request.class))).thenAnswer(inv -> inv.getArgument(0));
        when(requisitionMapper.toDto(any(Request.class))).thenReturn(mock(RequisitionDto.class));

        requisitionService.updateRequest(1L, updates, testUser);

        ArgumentCaptor<String> detailsCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditService).createRequisitionChangeLog(eq(testUser), eq(request), detailsCaptor.capture());

        String details = detailsCaptor.getValue();
        assertTrue(details.contains("Description cleared (was 'Need laptop')"));
    }

    //AI-Generated
    @Test
    void UpdateRequest_NoChanges_DoesNotCreateAuditLog() {
        Request request = new Request();
        request.setRequestID(1L);
        request.setState(RequestStatus.DRAFT);
        request.setUser(testUser);
        request.setProject(testProject);
        request.setWorkflowDefinition(testWorkflow);
        request.setRequestName("Laptop");
        request.setDescription("Need laptop");
        request.setPriority(Priority.LOW);
        request.setItems(new ArrayList<>());

        RequisitionUpdateDto updates = new RequisitionUpdateDto(
                "Laptop", "Need laptop", 1L, 1L, Priority.LOW, List.of());

        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(requestRepository.save(any(Request.class))).thenAnswer(inv -> inv.getArgument(0));
        when(requisitionMapper.toDto(any(Request.class))).thenReturn(mock(RequisitionDto.class));

        requisitionService.updateRequest(1L, updates, testUser);

        verify(auditService, never()).createRequisitionChangeLog(any(), any(), any());
    }

    //AI-Generated
    @Test
    void UpdateRequest_ProjectNotFound_ThrowsIllegalArgumentException() {
        Request request = new Request();
        request.setRequestID(1L);
        request.setState(RequestStatus.DRAFT);
        request.setUser(testUser);
        request.setProject(testProject);
        request.setWorkflowDefinition(testWorkflow);

        RequisitionUpdateDto updates = new RequisitionUpdateDto(
                "Laptop", "Need laptop", 999L, 1L, Priority.LOW, List.of());

        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(projectRepository.findById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> requisitionService.updateRequest(1L, updates, testUser));
        assertTrue(ex.getMessage().contains("Project not found with ID: 999"));
    }

    //AI-Generated
    @Test
    void UpdateRequest_WorkflowNotFound_ThrowsIllegalArgumentException() {
        Request request = new Request();
        request.setRequestID(1L);
        request.setState(RequestStatus.DRAFT);
        request.setUser(testUser);
        request.setProject(testProject);
        request.setWorkflowDefinition(testWorkflow);

        RequisitionUpdateDto updates = new RequisitionUpdateDto(
                "Laptop", "Need laptop", 1L, 999L, Priority.LOW, List.of());

        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(workflowDefinitionRepository.findById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> requisitionService.updateRequest(1L, updates, testUser));
        assertTrue(ex.getMessage().contains("Workflow not found with ID: 999"));
    }

    //AI-Generated
    @Test
    void UpdateRequest_WorkflowMissingStartEvent_ThrowsIllegalStateException() {
        Request request = new Request();
        request.setRequestID(1L);
        request.setState(RequestStatus.DRAFT);
        request.setUser(testUser);
        request.setProject(testProject);
        request.setWorkflowDefinition(testWorkflow);

        WorkflowDefinition newWorkflow = new WorkflowDefinition();
        newWorkflow.setId(2L);

        RequisitionUpdateDto updates = new RequisitionUpdateDto(
                "Laptop", "Need laptop", 1L, 2L, Priority.LOW, List.of());

        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(workflowDefinitionRepository.findById(2L)).thenReturn(Optional.of(newWorkflow));
        when(workflowStepRepository.findFirstByWorkflowDefinitionAndWorkflowComponent(newWorkflow, WorkflowComponent.START_EVENT))
                .thenReturn(Optional.empty());

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> requisitionService.updateRequest(1L, updates, testUser));
        assertTrue(ex.getMessage().contains("Workflow has no START_EVENT step defined"));
    }

    //AI-Generated
    @Test
    void UpdateRequest_EmptyItems_ClearsLineItems() {
        Request request = new Request();
        request.setRequestID(1L);
        request.setState(RequestStatus.DRAFT);
        request.setUser(testUser);
        request.setProject(testProject);
        request.setWorkflowDefinition(testWorkflow);

        RequestItem currentItem = new RequestItem();
        currentItem.setName("Old Item");
        currentItem.setQuantity(5);
        currentItem.setUnit(RequestItemUnit.PIECES);
        request.setItems(new ArrayList<>(List.of(currentItem)));

        RequisitionUpdateDto updates = new RequisitionUpdateDto(
                "Laptop", "Need laptop", 1L, 1L, Priority.LOW, List.of());

        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(requestRepository.save(any(Request.class))).thenAnswer(inv -> inv.getArgument(0));
        when(requisitionMapper.toDto(any(Request.class))).thenReturn(mock(RequisitionDto.class));

        requisitionService.updateRequest(1L, updates, testUser);

        assertTrue(request.getItems().isEmpty());
        verify(requestItemRepository).deleteByRequestID(1L);
        verify(quoteLineItemRepository).deleteByQuoteRequestID(1L);
        verify(quoteRepository).deleteByRequestID(1L);
    }

    @Test
    void ProcessPayment_WithInvalidRequestId_ThrowsEntityNotFoundException() {
        when(requestRepository.findById(100L)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class, () -> requisitionService.processPayment(100L, null));

        assertTrue(ex.getMessage().contains("Request not found with id: 100"));
        verify(requestRepository, never()).save(any());
        verify(invoiceRepository, never()).save(any());


    }

  @Test
    void ProcessPayment_WithValidRequestIdAndInvalidInvoice_ThrowsEntityNotFoundException() {
        Request request = new Request();
        request.setRequestID(1L);

        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class, () -> requisitionService.processPayment(1L, null));

        assertTrue(ex.getMessage().contains("Invoice not found for request with id: 1"));
        verify(requestRepository, never()).save(any());
        verify(invoiceRepository, never()).save(any());


    }
    //AI-GENERATED
    @Test
    void ProcessPayment_WithValidRequestIdAndInvoice_SuccessfullyProcessesPayment() {
        // Arrange
        Request request = new Request();
        request.setRequestID(1L);
        request.setJiraIssueKey("TEST-1");

        User creator = new User();
        creator.setName("creator");
        creator.setEmail("creator@veritas.com");
        request.setUser(creator);

        User user = new User();
        user.setName("user");

        BigDecimal totalAmount = new BigDecimal("120.00");
        BigDecimal committedSpend = new BigDecimal("200.00");
        BigDecimal actualSpend = new BigDecimal("50.00");

        InternalBudget budget = new InternalBudget();
        budget.setBudgetType(BudgetType.REQUEST);
        budget.setCommittedSpend(committedSpend);
        budget.setActualSpend(actualSpend);
        request.setBudget(budget);

        Invoice invoice = new Invoice();
        invoice.setTotalAmount(totalAmount);
        invoice.setCurrency(Currency.EUR);
        invoice.setIsPaid(false);
        request.setInvoice(invoice);

        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(requestRepository.save(any(Request.class))).thenReturn(request);
        when(currencyConversionService.convert(any(BigDecimal.class), any())).thenReturn(new CurrencyConversionResult(new BigDecimal("120.00"), BigDecimal.ONE, LocalDateTime.now(), ExchangeRateSource.FRANKFURTER));

        // Act
        requisitionService.processPayment(1L, user);

        // Assert
        assertAll(
            () -> assertTrue(invoice.getIsPaid()),
            () -> assertEquals(new BigDecimal("170.00"), budget.getActualSpend()),
            () -> assertEquals(new BigDecimal("0.00"), budget.getCommittedSpend())
        );

        verify(invoiceRepository, times(1)).save(invoice);
        verify(internalBudgetRepository, times(1)).save(budget);
        verify(notificationService).createNotification(eq(creator), eq(request), eq(NotificationType.PAID), anyString());
        verify(jiraSyncService).handleVeritasWorkflowChange(request);
    }


    @Test
    void ProcessPayment_WithValidRequestIdAndInvalidInvoice_ThrowsIllegalStateException() {

        Request request = new Request();
        request.setRequestID(1L);

        BigDecimal committedSpend = new BigDecimal("200.00");
        BigDecimal actualSpend = new BigDecimal("50.00");

        InternalBudget budget = new InternalBudget();
        budget.setCommittedSpend(committedSpend);
        budget.setActualSpend(actualSpend);
        request.setBudget(budget);

        Invoice invoice = new Invoice();
        invoice.setTotalAmount(null);
        invoice.setIsPaid(false);
        request.setInvoice(invoice);

        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));


        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> requisitionService.processPayment(1L, null));

        assertTrue(ex.getMessage().contains("Invoice has no Total amount defined"));

        assertFalse(invoice.getIsPaid());

        verify(invoiceRepository, never()).save(invoice);
        verify(internalBudgetRepository, never()).save(budget);
    }

    @Test
    void DeleteAttachment_ExistingAttachment_DeletesFileAndEntity() throws IOException {
        Path tempFile = Files.createTempFile("delete-attachment", ".pdf");

        Attachment attachment = new Attachment();
        attachment.setAttachmentId(1L);
        attachment.setFileName("invoice.pdf");
        attachment.setStoragePath(tempFile.toString());

        Request request = new Request();
        request.setRequestID(100L);
        WorkflowStep step = new WorkflowStep();
        step.setWorkflowComponent(WorkflowComponent.STEP);
        step.setRole(UserRole.FINANCE_OFFICER);
        request.setCurrentStep(step);
        attachment.setRequest(request);

        when(attachmentRepository.findById(1L)).thenReturn(Optional.of(attachment));

        requisitionService.deleteAttachment(1L, testUser);

        verify(attachmentRepository).delete(attachment);
        assertFalse(Files.exists(tempFile));
    }

    @Test
    void DeleteAttachment_AsRequester_OnAnotherUsersAttachment_ThrowsAccessDenied() {
        User owner = new User();
        owner.setId(1L);
        owner.setRole(UserRole.REQUESTER);

        User caller = new User();
        caller.setId(2L);
        caller.setRole(UserRole.REQUESTER);

        Request request = new Request();
        request.setUser(owner);
        WorkflowStep step = new WorkflowStep();
        step.setWorkflowComponent(WorkflowComponent.STEP);
        step.setRole(UserRole.REQUESTER);
        request.setCurrentStep(step);

        Attachment attachment = new Attachment();
        attachment.setAttachmentId(10L);
        attachment.setFileName("secret.pdf");
        attachment.setStoragePath("/tmp/secret.pdf");
        attachment.setRequest(request);

        when(attachmentRepository.findById(10L)).thenReturn(Optional.of(attachment));
        doThrow(new AccessDeniedException("Not allowed")).when(workflowEngineService).checkAuthorization(any(), eq(caller), any());

        assertThrows(AccessDeniedException.class, () -> requisitionService.deleteAttachment(10L, caller));
        verify(attachmentRepository, never()).delete(any());
    }

    @Test
    void DeleteAttachment_AttachmentNotFound_ThrowsEntityNotFoundException() {
        when(attachmentRepository.findById(999L)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> requisitionService.deleteAttachment(999L, testUser));

        assertTrue(ex.getMessage().contains("Attachment not found with id: 999"));
        verify(attachmentRepository, never()).delete(any());
    }

    @Test
    void DeleteAttachment_FileDeletionFails_ThrowsRuntimeException() {
        Attachment attachment = new Attachment();
        attachment.setAttachmentId(1L);
        attachment.setFileName("broken.pdf");
        attachment.setStoragePath("\0invalid-path");

        when(attachmentRepository.findById(1L)).thenReturn(Optional.of(attachment));

        assertThrows(RuntimeException.class,
                () -> requisitionService.deleteAttachment(1L, testUser));

        verify(attachmentRepository, never()).delete(any());
    }


    //AI-GENERATED
    @Test
    void getNextStepRole_NextStepIsAutomated_ReturnsNull() {
        Request request = new Request();
        request.setRequestID(1L);

        WorkflowStep nextStep = new WorkflowStep();
        nextStep.setRole(UserRole.ADMINISTRATOR);
        nextStep.setIsAutomatedApproval(true);

        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(workflowEngineService.getNextStep(request)).thenReturn(nextStep);

        String result = requisitionService.getNextStepRole(1L, testUser);

        assertNull(result);
    }

    @Test
    void getNextStepRole_NextStepIsNotAutomatedWithRole_ReturnsRoleName() {
        Request request = new Request();
        request.setRequestID(1L);

        WorkflowStep nextStep = new WorkflowStep();
        nextStep.setRole(UserRole.ADMINISTRATOR);
        nextStep.setIsAutomatedApproval(false);

        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(workflowEngineService.getNextStep(request)).thenReturn(nextStep);

        String result = requisitionService.getNextStepRole(1L, testUser);

        assertEquals("ADMINISTRATOR", result);
    }

    // AI-GENERATED
    @Test
    void DeleteInvoice_ValidRequest_DeletesInvoiceAndAttachments() throws IOException {
        Request request = new Request();
        request.setRequestID(1L);

        Invoice invoice = new Invoice();
        invoice.setInvoiceId(1L);
        invoice.setIsPaid(false);

        Path tempFile = Files.createTempFile("test-invoice-attachment", ".pdf");
        Attachment attachment = new Attachment();
        attachment.setAttachmentId(1L);
        attachment.setStoragePath(tempFile.toString());
        attachment.setFileName("test.pdf");

        invoice.setAttachments(new ArrayList<>(List.of(attachment)));
        request.setInvoice(invoice);
        request.setAttachments(new ArrayList<>(List.of(attachment)));

        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));

        requisitionService.deleteInvoice(1L, testUser);

        verify(attachmentRepository).delete(attachment);
        verify(invoiceRepository).delete(invoice);
        assertNull(request.getInvoice());
        assertTrue(request.getAttachments().isEmpty());
        assertFalse(Files.exists(tempFile));
    }

    @Test
    void DeleteInvoice_RequestNotFound_ThrowsEntityNotFoundException() {
        when(requestRepository.findById(999L)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> requisitionService.deleteInvoice(999L, testUser));
        assertTrue(ex.getMessage().contains("Request not found with id: 999"));
        verify(invoiceRepository, never()).delete(any());
    }

    @Test
    void DeleteInvoice_InvoiceNotFound_ThrowsEntityNotFoundException() {
        Request request = new Request();
        request.setRequestID(1L);
        request.setInvoice(null);

        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> requisitionService.deleteInvoice(1L, testUser));
        assertTrue(ex.getMessage().contains("Invoice not found for request with id: 1"));
        verify(invoiceRepository, never()).delete(any());
    }

    @Test
    void DeleteInvoice_InvoiceAlreadyPaid_ThrowsIllegalStateException() {
        Request request = new Request();
        request.setRequestID(1L);

        Invoice invoice = new Invoice();
        invoice.setIsPaid(true);
        request.setInvoice(invoice);

        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> requisitionService.deleteInvoice(1L, testUser));
        assertTrue(ex.getMessage().contains("Cannot delete an invoice that has already been paid."));
        verify(invoiceRepository, never()).delete(any());
    }

    @Test
    void DeleteInvoice_IOExceptionOnDelete_ThrowsRuntimeException() {
        Request request = new Request();
        request.setRequestID(1L);

        Invoice invoice = new Invoice();
        invoice.setIsPaid(false);

        Attachment attachment = new Attachment();
        attachment.setAttachmentId(1L);
        attachment.setStoragePath("\0invalid-path");
        attachment.setFileName("broken.pdf");

        invoice.setAttachments(new ArrayList<>(List.of(attachment)));
        request.setInvoice(invoice);

        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> requisitionService.deleteInvoice(1L, testUser));

        verify(invoiceRepository, never()).delete(any());
    }

    //AI-GENERATED
    @Test
    void GetInvoice_WithValidConversion_ReturnsInvoiceDtoWithEuroValue() {
        Long requestId = 1L;
        Request request = new Request();
        request.setRequestID(requestId);

        Invoice invoice = new Invoice();
        invoice.setInvoiceId(100L);
        invoice.setTotalAmount(BigDecimal.valueOf(200));
        invoice.setCurrency(Currency.EUR);
        invoice.setRequest(request);
        request.setInvoice(invoice);

        when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(currencyConversionService.convert(BigDecimal.valueOf(200), Currency.EUR)).thenReturn(new CurrencyConversionResult(BigDecimal.valueOf(200), BigDecimal.ONE, LocalDateTime.now(), ExchangeRateSource.FRANKFURTER));

        InvoiceDto expectedDto = mock(InvoiceDto.class);
        when(invoiceMapper.toDto(eq(invoice), eq(BigDecimal.valueOf(200)), any(LocalDateTime.class), eq(ExchangeRateSource.FRANKFURTER))).thenReturn(expectedDto);

        InvoiceDto result = requisitionService.getInvoice(requestId, testUser);

        assertNotNull(result);
        verify(currencyConversionService).convert(BigDecimal.valueOf(200), Currency.EUR);
        verify(invoiceMapper).toDto(eq(invoice), eq(BigDecimal.valueOf(200)), any(LocalDateTime.class), eq(ExchangeRateSource.FRANKFURTER));
    }

    //AI-GENERATED
    @Test
    void GetInvoice_WhenCurrencyConversionFails_ReturnsInvoiceDtoWithNullEuroValue() {
        Long requestId = 1L;
        Request request = new Request();
        request.setRequestID(requestId);

        Invoice invoice = new Invoice();
        invoice.setInvoiceId(100L);
        invoice.setTotalAmount(BigDecimal.valueOf(200));
        invoice.setCurrency(Currency.EUR);
        invoice.setRequest(request);
        request.setInvoice(invoice);

        when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(currencyConversionService.convert(any(), any())).thenThrow(new IllegalArgumentException("Conversion failed"));

        InvoiceDto expectedDto = mock(InvoiceDto.class);
        when(invoiceMapper.toDto(eq(invoice), isNull(), isNull(), isNull())).thenReturn(expectedDto);

        InvoiceDto result = requisitionService.getInvoice(requestId, testUser);

        assertNotNull(result);
        verify(invoiceMapper).toDto(eq(invoice), isNull(), isNull(), isNull());
    }

    // AI-Generated
    @Test
    void ApproveRequest_RequestNotFound_ThrowsIllegalArgumentException() {
        when(requestRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> requisitionService.approveRequest(999L, testUser, 2L));
    }

    // AI-Generated
    @Test
    void ApproveRequest_FinishedRequest_ThrowsWorkflowStateException() {
        Request request = new Request();
        request.setRequestID(1L);
        request.setState(RequestStatus.FINISHED);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));

        assertThrows(WorkflowStateException.class, () -> requisitionService.approveRequest(1L, testUser, 2L));
    }

    // AI-Generated
    @Test
    void ApproveRequest_DraftRequest_ThrowsWorkflowStateException() {
        Request request = new Request();
        request.setRequestID(1L);
        request.setState(RequestStatus.DRAFT);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));

        assertThrows(WorkflowStateException.class, () -> requisitionService.approveRequest(1L, testUser, 2L));
    }

    // AI-Generated
    @Test
    void ApproveRequest_PaidInvoice_ThrowsWorkflowStateException() {
        Request request = new Request();
        request.setRequestID(1L);
        request.setState(RequestStatus.ACTIVE);
        Invoice invoice = new Invoice();
        invoice.setIsPaid(true);
        request.setInvoice(invoice);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));

        assertThrows(WorkflowStateException.class, () -> requisitionService.approveRequest(1L, testUser, 2L));
    }

    // AI-Generated
    @Test
    void ApproveRequest_ValidRequest_ApprovesAndReturnsDto() {
        Request request = new Request();
        request.setRequestID(1L);
        request.setState(RequestStatus.ACTIVE);
        request.setRevisionRequired(true);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(requestRepository.save(any(Request.class))).thenAnswer(inv -> inv.getArgument(0));

        RequisitionDto expectedDto = mock(RequisitionDto.class);
        when(requisitionMapper.toDto(any(Request.class))).thenReturn(expectedDto);

        RequisitionDto result = requisitionService.approveRequest(1L, testUser, 2L);

        assertNotNull(result);
        assertEquals(expectedDto, result);
        assertFalse(request.getRevisionRequired());
        verify(workflowEngineService).moveToNextStep(request, testUser, 2L);
        verify(requestRepository).save(request);
    }

    // AI-Generated
    @Test
    void RevertRequest_RequestNotFound_ThrowsIllegalArgumentException() {
        when(requestRepository.findById(999L)).thenReturn(Optional.empty());
        RequisitionRejectDto dto = new RequisitionRejectDto();
        dto.setReason("Reason");
        dto.setRevisionRequired(false);
        assertThrows(IllegalArgumentException.class, () -> requisitionService.revertRequest(999L, testUser, dto));
    }

    // AI-Generated
    @Test
    void RevertRequest_FinishedRequest_ThrowsWorkflowStateException() {
        Request request = new Request();
        request.setRequestID(1L);
        request.setState(RequestStatus.FINISHED);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        RequisitionRejectDto dto = new RequisitionRejectDto();
        dto.setReason("Reason");
        dto.setRevisionRequired(false);

        assertThrows(WorkflowStateException.class, () -> requisitionService.revertRequest(1L, testUser, dto));
    }

    // AI-Generated
    @Test
    void RevertRequest_DraftRequest_ThrowsWorkflowStateException() {
        Request request = new Request();
        request.setRequestID(1L);
        request.setState(RequestStatus.DRAFT);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        RequisitionRejectDto dto = new RequisitionRejectDto();
        dto.setReason("Reason");
        dto.setRevisionRequired(false);

        assertThrows(WorkflowStateException.class, () -> requisitionService.revertRequest(1L, testUser, dto));
    }

    // AI-Generated
    @Test
    void RevertRequest_PaidInvoice_ThrowsWorkflowStateException() {
        Request request = new Request();
        request.setRequestID(1L);
        request.setState(RequestStatus.ACTIVE);
        Invoice invoice = new Invoice();
        invoice.setIsPaid(true);
        request.setInvoice(invoice);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        RequisitionRejectDto dto = new RequisitionRejectDto();
        dto.setReason("Reason");
        dto.setRevisionRequired(false);

        assertThrows(WorkflowStateException.class, () -> requisitionService.revertRequest(1L, testUser, dto));
    }

    // AI-Generated
    @Test
    void RevertRequest_RevisionRequiredTrue_RevertsAndSetsRevisionRequired() {
        User creator = new User();
        creator.setEmail("creator@veritas.com");
        User assignee = new User();
        assignee.setEmail("assignee@veritas.com");

        Request request = new Request();
        request.setRequestID(1L);
        request.setRequestName("Test Request");
        request.setState(RequestStatus.ACTIVE);
        request.setRevisionRequired(false);
        request.setUser(creator);
        request.setAssignee(assignee);

        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(requestRepository.save(any(Request.class))).thenAnswer(inv -> inv.getArgument(0));

        RequisitionDto expectedDto = mock(RequisitionDto.class);
        when(requisitionMapper.toDto(any(Request.class))).thenReturn(expectedDto);

        RequisitionRejectDto dto = new RequisitionRejectDto();
        dto.setReason("Reason");
        dto.setRevisionRequired(true);
        RequisitionDto result = requisitionService.revertRequest(1L, testUser, dto);

        assertNotNull(result);
        assertEquals(expectedDto, result);
        assertTrue(request.getRevisionRequired());
        verify(workflowEngineService).revertToPreviousStep(request, testUser, "Reason");
        verify(requestRepository).save(request);
        verify(notificationService).createNotification(
                eq(creator),
                eq(request),
                eq(NotificationType.REVERTED),
                anyString()
        );
        verify(notificationService).createNotification(
                eq(assignee),
                eq(request),
                eq(NotificationType.ASSIGNED),
                anyString()
        );
    }

    // AI-Generated
    @Test
    void RevertRequest_RevisionRequiredFalse_RevertsWithoutRevisionRequired() {
        User creator = new User();
        creator.setEmail("creator@veritas.com");
        User assignee = new User();
        assignee.setEmail("assignee@veritas.com");

        Request request = new Request();
        request.setRequestID(1L);
        request.setRequestName("Test Request");
        request.setState(RequestStatus.ACTIVE);
        request.setRevisionRequired(false);
        request.setUser(creator);
        request.setAssignee(assignee);

        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(requestRepository.save(any(Request.class))).thenAnswer(inv -> inv.getArgument(0));

        RequisitionDto expectedDto = mock(RequisitionDto.class);
        when(requisitionMapper.toDto(any(Request.class))).thenReturn(expectedDto);

        RequisitionRejectDto dto = new RequisitionRejectDto();
        dto.setReason("Reason");
        dto.setRevisionRequired(false);
        RequisitionDto result = requisitionService.revertRequest(1L, testUser, dto);

        assertNotNull(result);
        assertEquals(expectedDto, result);
        assertFalse(request.getRevisionRequired());
        verify(workflowEngineService).revertToPreviousStep(request, testUser, "Reason");
        verify(requestRepository).save(request);
        verify(notificationService).createNotification(
                eq(creator),
                eq(request),
                eq(NotificationType.REVERTED),
                anyString()
        );
        verify(notificationService).createNotification(
                eq(assignee),
                eq(request),
                eq(NotificationType.ASSIGNED),
                anyString()
        );
    }

    // AI-Generated
    @Test
    void RejectRequest_RequestNotFound_ThrowsIllegalArgumentException() {
        when(requestRepository.findById(999L)).thenReturn(Optional.empty());
        RequisitionRejectDto dto = new RequisitionRejectDto();
        dto.setReason("Reason");
        dto.setRevisionRequired(false);
        assertThrows(IllegalArgumentException.class, () -> requisitionService.rejectRequest(999L, testUser, dto));
    }

    // AI-Generated
    @Test
    void RejectRequest_FinishedRequest_ThrowsWorkflowStateException() {
        Request request = new Request();
        request.setRequestID(1L);
        request.setState(RequestStatus.FINISHED);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        RequisitionRejectDto dto = new RequisitionRejectDto();
        dto.setReason("Reason");
        dto.setRevisionRequired(false);

        assertThrows(WorkflowStateException.class, () -> requisitionService.rejectRequest(1L, testUser, dto));
    }

    // AI-Generated
    @Test
    void RejectRequest_PaidInvoice_ThrowsWorkflowStateException() {
        Request request = new Request();
        request.setRequestID(1L);
        request.setState(RequestStatus.ACTIVE);
        Invoice invoice = new Invoice();
        invoice.setIsPaid(true);
        request.setInvoice(invoice);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        RequisitionRejectDto dto = new RequisitionRejectDto();
        dto.setReason("Reason");
        dto.setRevisionRequired(false);

        assertThrows(WorkflowStateException.class, () -> requisitionService.rejectRequest(1L, testUser, dto));
    }

    // AI-Generated
    @Test
    void RejectRequest_NotAllowedToReject_ThrowsAccessDeniedException() {
        Request request = new Request();
        request.setRequestID(1L);
        request.setState(RequestStatus.ACTIVE);
        WorkflowStep step = new WorkflowStep();
        step.setId(10L);
        request.setCurrentStep(step);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));

        doThrow(new AccessDeniedException("Forbidden")).when(workflowEngineService)
                .checkAuthorization(eq(request), eq(testUser), eq(step));

        RequisitionRejectDto dto = new RequisitionRejectDto();
        dto.setReason("Reason");
        dto.setRevisionRequired(false);
        assertThrows(AccessDeniedException.class, () -> requisitionService.rejectRequest(1L, testUser, dto));
    }

    // AI-Generated
    @Test
    void RejectRequest_Valid_RejectsSuccessfully() {
        User creator = new User();
        creator.setEmail("creator@veritas.com");

        Request request = new Request();
        request.setRequestID(1L);
        request.setRequestName("Test Request");
        request.setState(RequestStatus.ACTIVE);
        request.setUser(creator);
        WorkflowStep step = new WorkflowStep();
        step.setId(10L);
        request.setCurrentStep(step);

        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(requestRepository.save(any(Request.class))).thenAnswer(inv -> inv.getArgument(0));

        RequisitionDto expectedDto = mock(RequisitionDto.class);
        when(requisitionMapper.toDto(any(Request.class))).thenReturn(expectedDto);

        RequisitionRejectDto dto = new RequisitionRejectDto();
        dto.setReason("Reason");
        dto.setRevisionRequired(false);
        RequisitionDto result = requisitionService.rejectRequest(1L, testUser, dto);

        assertNotNull(result);
        assertEquals(expectedDto, result);
        assertEquals(RequestStatus.FINISHED, request.getState());
        assertEquals("Reason", request.getRejectionReason());
        assertNotNull(request.getDeletedAt());
        verify(requestRepository).save(request);
        verify(notificationService).createNotification(
                eq(creator),
                eq(request),
                eq(NotificationType.REJECTED),
                anyString()
        );
    }

    // AI-Generated
    @Test
    void RejectRequest_JiraLinkedRequest_TriggersJiraSync() {
        User creator = new User();
        creator.setEmail("creator@veritas.com");

        Request request = new Request();
        request.setRequestID(1L);
        request.setRequestName("Jira Request");
        request.setState(RequestStatus.ACTIVE);
        request.setUser(creator);
        request.setJiraIssueKey("TEST-42");
        WorkflowStep step = new WorkflowStep();
        step.setId(10L);
        request.setCurrentStep(step);

        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(requestRepository.save(any(Request.class))).thenAnswer(inv -> inv.getArgument(0));

        RequisitionDto expectedDto = mock(RequisitionDto.class);
        when(requisitionMapper.toDto(any(Request.class))).thenReturn(expectedDto);

        RequisitionRejectDto dto = new RequisitionRejectDto();
        dto.setReason("Budget exceeded");
        dto.setRevisionRequired(false);
        RequisitionDto result = requisitionService.rejectRequest(1L, testUser, dto);

        assertNotNull(result);
        assertEquals(RequestStatus.FINISHED, request.getState());
        verify(jiraSyncService).handleVeritasWorkflowChange(request);
    }

    @Test
    void RejectRequest_NonJiraRequest_DoesNotTriggerJiraSync() {
        User creator = new User();
        creator.setEmail("creator@veritas.com");

        Request request = new Request();
        request.setRequestID(1L);
        request.setRequestName("Non-Jira Request");
        request.setState(RequestStatus.ACTIVE);
        request.setUser(creator);
        // jiraIssueKey is null
        WorkflowStep step = new WorkflowStep();
        step.setId(10L);
        request.setCurrentStep(step);

        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(requestRepository.save(any(Request.class))).thenAnswer(inv -> inv.getArgument(0));

        RequisitionDto expectedDto = mock(RequisitionDto.class);
        when(requisitionMapper.toDto(any(Request.class))).thenReturn(expectedDto);

        RequisitionRejectDto dto = new RequisitionRejectDto();
        dto.setReason("Not needed");
        dto.setRevisionRequired(false);
        requisitionService.rejectRequest(1L, testUser, dto);

        verify(jiraSyncService, never()).handleVeritasWorkflowChange(any());
    }

    // AI-Generated
    @Test
    void CancelRequest_FinishedState_ThrowsWorkflowStateException() {
        Request request = new Request();
        request.setRequestID(1L);
        request.setState(RequestStatus.FINISHED);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));

        assertThrows(WorkflowStateException.class, () -> requisitionService.cancelRequest(1L, testUser));
    }

    // AI-Generated
    @Test
    void CancelRequest_NotDraftAndCannotAct_ThrowsWorkflowStateException() {
        Request request = new Request();
        request.setRequestID(1L);
        request.setState(RequestStatus.ACTIVE);
        WorkflowStep step = new WorkflowStep();
        step.setId(10L);
        request.setCurrentStep(step);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));

        doThrow(new AccessDeniedException("Forbidden")).when(workflowEngineService)
                .checkAuthorization(eq(request), eq(testUser), eq(step));

        assertThrows(WorkflowStateException.class, () -> requisitionService.cancelRequest(1L, testUser));
    }

    // AI-Generated
    @Test
    void CancelRequest_NotCreator_ThrowsAccessDeniedException() {
        User anotherUser = new User();
        anotherUser.setId(999L);
        anotherUser.setRole(UserRole.REQUESTER);

        Request request = new Request();
        request.setRequestID(1L);
        request.setState(RequestStatus.DRAFT);
        request.setUser(anotherUser);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));

        testUser.setRole(UserRole.REQUESTER);
        assertThrows(AccessDeniedException.class, () -> requisitionService.cancelRequest(1L, testUser));
    }

    // AI-Generated
    @Test
    void CancelRequest_NotRequesterRole_ThrowsAccessDeniedException() {
        Request request = new Request();
        request.setRequestID(1L);
        request.setState(RequestStatus.DRAFT);
        request.setUser(testUser);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));

        testUser.setRole(UserRole.PROCUREMENT_OFFICER);
        assertThrows(AccessDeniedException.class, () -> requisitionService.cancelRequest(1L, testUser));
    }

    // AI-Generated
    @Test
    void CancelRequest_DraftAndCreator_Succeeds() {
        Request request = new Request();
        request.setRequestID(1L);
        request.setState(RequestStatus.DRAFT);
        request.setUser(testUser);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(requestRepository.save(any(Request.class))).thenAnswer(inv -> inv.getArgument(0));

        RequisitionDto expectedDto = mock(RequisitionDto.class);
        when(requisitionMapper.toDto(any(Request.class))).thenReturn(expectedDto);

        testUser.setRole(UserRole.REQUESTER);
        RequisitionDto result = requisitionService.cancelRequest(1L, testUser);

        assertAll("Cancel draft request validation",
                () -> assertNotNull(result),
                () -> assertEquals(expectedDto, result),
                () -> assertEquals(RequestStatus.FINISHED, request.getState()),
                () -> assertNotNull(request.getDeletedAt())
        );

        verify(requestRepository).save(request);
        verify(auditService).createWorkflowTransitionLog(
                eq(testUser), eq(request), eq(null), eq(CANCEL), any(String.class)
        );
    }

    // AI-Generated
    @Test
    void CancelRequest_ActiveAndCanActAndCreator_Succeeds() {
        Request request = new Request();
        request.setRequestID(1L);
        request.setState(RequestStatus.ACTIVE);
        request.setUser(testUser);
        WorkflowStep step = new WorkflowStep();
        step.setId(10L);
        request.setCurrentStep(step);

        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(requestRepository.save(any(Request.class))).thenAnswer(inv -> inv.getArgument(0));

        RequisitionDto expectedDto = mock(RequisitionDto.class);
        when(requisitionMapper.toDto(any(Request.class))).thenReturn(expectedDto);

        testUser.setRole(UserRole.REQUESTER);
        RequisitionDto result = requisitionService.cancelRequest(1L, testUser);

        assertAll("Cancel active request validation",
                () -> assertNotNull(result),
                () -> assertEquals(expectedDto, result),
                () -> assertEquals(RequestStatus.FINISHED, request.getState()),
                () -> assertNotNull(request.getDeletedAt())
        );

        verify(requestRepository).save(request);
        verify(auditService).createWorkflowTransitionLog(
                eq(testUser), eq(request), eq(null), eq(CANCEL), any(String.class)
        );
    }

    @Test
    void validateBudget_TypeRequest_SkipsAndValidatesParent() {
        InternalBudget requestBudget = new InternalBudget();
        requestBudget.setBudgetType(BudgetType.REQUEST);

        InternalBudget projectBudget = new InternalBudget();
        projectBudget.setBudgetType(BudgetType.PROJECT);
        projectBudget.setBudgetName("Proj Budget");
        projectBudget.setTotalAmount(BigDecimal.valueOf(100.0));
        projectBudget.setActualSpend(BigDecimal.valueOf(10.0));
        projectBudget.setCommittedSpend(BigDecimal.valueOf(20.0));
        projectBudget.setSafetyBuffer(BigDecimal.ZERO);

        requestBudget.setParentBudget(projectBudget);

        // Simulated total = 10 + 20 + 50 = 80 <= 100
        assertDoesNotThrow(() -> requisitionService.validateBudget(requestBudget, BigDecimal.valueOf(50.0), false));
    }

    @Test
    void validateBudget_TotalAmountNull_DefaultsToZero() {
        InternalBudget budget = new InternalBudget();
        budget.setBudgetType(BudgetType.PROJECT);
        budget.setBudgetName(null);
        budget.setTotalAmount(null);
        budget.setActualSpend(BigDecimal.ZERO);
        budget.setCommittedSpend(BigDecimal.ZERO);
        budget.setSafetyBuffer(BigDecimal.ZERO);

        // simulatedTotal = 1.0 > 0.0 -> exhausted
        WorkflowStateException ex = assertThrows(WorkflowStateException.class,
                () -> requisitionService.validateBudget(budget, BigDecimal.ONE, false));
        assertTrue(ex.getMessage().contains("Project budget exhausted."));
    }

    @Test
    void validateBudget_SafetyBufferExhausted_ThrowsException() {
        InternalBudget budget = new InternalBudget();
        budget.setBudgetType(BudgetType.DEPARTMENT);
        budget.setBudgetName("  "); // blank
        budget.setTotalAmount(BigDecimal.valueOf(100.0));
        budget.setActualSpend(BigDecimal.valueOf(70.0));
        budget.setCommittedSpend(BigDecimal.valueOf(10.0));
        budget.setSafetyBuffer(BigDecimal.valueOf(10.0)); // 10% safety buffer

        // threshold = 100 * (1 - 0.1) = 90
        // simulatedTotal = 70 + 10 + 15 = 95 > 90 -> exhausted
        WorkflowStateException ex = assertThrows(WorkflowStateException.class,
                () -> requisitionService.validateBudget(budget, BigDecimal.valueOf(15.0), true));
        assertTrue(ex.getMessage().contains("Department budget exhausted including safety buffer."));
    }

    @Test
    void validateBudget_GlobalBudgetExhausted_ThrowsException() {
        InternalBudget budget = new InternalBudget();
        budget.setBudgetType(BudgetType.GLOBAL);
        budget.setBudgetName("");
        budget.setTotalAmount(BigDecimal.valueOf(1000.0));
        budget.setActualSpend(BigDecimal.valueOf(900.0));
        budget.setCommittedSpend(BigDecimal.valueOf(100.0));
        budget.setSafetyBuffer(BigDecimal.ZERO);

        // simulatedTotal = 900 + 100 + 50 = 1050 > 1000 -> exhausted
        WorkflowStateException ex = assertThrows(WorkflowStateException.class,
                () -> requisitionService.validateBudget(budget, BigDecimal.valueOf(50.0), false));
        assertTrue(ex.getMessage().contains("Global budget exhausted."));
    }

    @Test
    void validateBudget_DefaultBudgetTypeNull_ThrowsNullPointerException() {
        InternalBudget budget = new InternalBudget();
        budget.setBudgetType(null);
        budget.setBudgetName(null);
        budget.setTotalAmount(BigDecimal.valueOf(100.0));
        budget.setActualSpend(BigDecimal.valueOf(90.0));
        budget.setCommittedSpend(BigDecimal.valueOf(10.0));
        budget.setSafetyBuffer(BigDecimal.ZERO);

        // simulatedTotal = 90 + 10 + 5 = 105 > 100 -> switch on null throws NPE
        assertThrows(NullPointerException.class,
                () -> requisitionService.validateBudget(budget, BigDecimal.valueOf(5.0), false));
    }

    @Test
    void rejectRequest_AccessDenied_ThrowsAccessDeniedException() {
        Request request = new Request();
        request.setRequestID(1L);
        request.setState(RequestStatus.ACTIVE);
        WorkflowStep step = new WorkflowStep();
        step.setName("Approval Step");
        request.setCurrentStep(step);

        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));

        User assignee = new User();
        assignee.setId(99L);
        request.setAssignee(assignee);

        User actor = new User();
        actor.setId(100L);
        actor.setRole(UserRole.REQUESTER);

        doThrow(new AccessDeniedException("Forbidden"))
                .when(workflowEngineService).checkAuthorization(any(Request.class), any(User.class), any());

        RequisitionRejectDto rejectDto = new RequisitionRejectDto();
        rejectDto.setReason("Reject reason");

        assertThrows(AccessDeniedException.class,
                () -> requisitionService.rejectRequest(1L, actor, rejectDto));
    }

    @Test
    void cancelRequest_FinishedRequest_ThrowsWorkflowStateException() {
        Request request = new Request();
        request.setRequestID(1L);
        request.setState(RequestStatus.FINISHED);

        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));

        assertThrows(WorkflowStateException.class,
                () -> requisitionService.cancelRequest(1L, testUser));
    }

    @Test
    void cancelRequest_NotCreator_ThrowsAccessDeniedException() {
        Request request = new Request();
        request.setRequestID(1L);
        request.setState(RequestStatus.DRAFT);
        User creator = new User();
        creator.setId(20L);
        request.setUser(creator);

        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));

        User actor = new User();
        actor.setId(21L);
        actor.setRole(UserRole.REQUESTER);

        assertThrows(AccessDeniedException.class,
                () -> requisitionService.cancelRequest(1L, actor));
    }

    @Test
    void changeRequester_FinishedRequest_ThrowsWorkflowStateException() {
        Request request = new Request();
        request.setRequestID(1L);
        request.setState(RequestStatus.FINISHED);

        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));

        assertThrows(WorkflowStateException.class,
                () -> requisitionService.changeRequester(1L, 2L));
    }

    @Test
    void changeRequester_NullNewRequester_ThrowsIllegalArgumentException() {
        Request request = new Request();
        request.setRequestID(1L);
        request.setState(RequestStatus.ACTIVE);

        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));

        assertThrows(IllegalArgumentException.class,
                () -> requisitionService.changeRequester(1L, null));
    }

    @Test
    void changeRequester_NewRequesterWrongRoleOrTeam_ThrowsIllegalArgumentException() {
        Team team1 = new Team();
        team1.setTeamId(1L);
        User creator = new User();
        creator.setTeam(team1);

        Request request = new Request();
        request.setRequestID(1L);
        request.setState(RequestStatus.ACTIVE);
        request.setUser(creator);

        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));

        User badUser1 = new User();
        badUser1.setId(2L);
        badUser1.setRole(UserRole.FINANCE_OFFICER);
        badUser1.setTeam(team1);
        when(userRepository.findById(2L)).thenReturn(Optional.of(badUser1));

        Team team2 = new Team();
        team2.setTeamId(2L);
        User badUser2 = new User();
        badUser2.setId(3L);
        badUser2.setRole(UserRole.REQUESTER);
        badUser2.setTeam(team2);
        when(userRepository.findById(3L)).thenReturn(Optional.of(badUser2));

        assertAll(
            () -> assertThrows(IllegalArgumentException.class, () -> requisitionService.changeRequester(1L, 2L)),
            () -> assertThrows(IllegalArgumentException.class, () -> requisitionService.changeRequester(1L, 3L))
        );
    }

    @Test
    void getEligibleAssignees_RequestNotFound_ThrowsException() {
        when(requestRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> requisitionService.getEligibleAssignees(999L, "REQUESTER", testUser));
    }

    @Test
    void getEligibleAssignees_VariousRoles_ReturnsExpectedAssignees() {
        Department dept = new Department();
        dept.setDepartmentId(10L);
        Team team = new Team();
        team.setDepartment(dept);
        
        User creator = new User();
        creator.setTeam(team);
        
        Request request = new Request();
        request.setRequestID(1L);
        request.setUser(creator);
        
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(userMapper.toUserDto(any(User.class))).thenReturn(new UserDto(1L, "user", "email", true, UserRole.REQUESTER, "team", 1L, "dept", 1L, LocalDateTime.now()));

        User financeOfficer = new User();
        financeOfficer.setRole(UserRole.FINANCE_OFFICER);
        financeOfficer.setIsActive(true);
        when(userRepository.findAllByRoleAndIsActiveTrue(UserRole.FINANCE_OFFICER)).thenReturn(List.of(financeOfficer));

        User procurementOfficer = new User();
        procurementOfficer.setRole(UserRole.PROCUREMENT_OFFICER);
        procurementOfficer.setDepartment(dept);
        procurementOfficer.setIsActive(true);
        when(userRepository.findAllByRoleAndIsActiveTrue(UserRole.PROCUREMENT_OFFICER)).thenReturn(List.of(procurementOfficer));

        User requester = new User();
        requester.setRole(UserRole.REQUESTER);
        requester.setTeam(team);
        requester.setIsActive(true);
        when(userRepository.findAllByRoleAndIsActiveTrue(UserRole.REQUESTER)).thenReturn(List.of(requester));

        assertAll(
            () -> assertFalse(requisitionService.getEligibleAssignees(1L, "FINANCE_OFFICER", testUser).isEmpty()),
            () -> assertFalse(requisitionService.getEligibleAssignees(1L, "PROCUREMENT_OFFICER", testUser).isEmpty()),
            () -> assertFalse(requisitionService.getEligibleAssignees(1L, "REQUESTER", testUser).isEmpty())
        );
    }

    @Test
    void canAct_RequestNotFoundOrStepNull_ReturnsFalse() {
        when(requestRepository.findById(1L)).thenReturn(Optional.empty());
        assertFalse(requisitionService.canAct(1L, testUser));

        Request req = new Request();
        req.setCurrentStep(null);
        when(requestRepository.findById(2L)).thenReturn(Optional.of(req));
        assertFalse(requisitionService.canAct(2L, testUser));
    }

    @Test
    void canAct_AuthorizedOrNot_ReturnsExpected() {
        Request req = new Request();
        WorkflowStep step = new WorkflowStep();
        req.setCurrentStep(step);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(req));

        assertDoesNotThrow(() -> requisitionService.canAct(1L, testUser));
        
        doThrow(new AccessDeniedException("Access Denied"))
            .when(workflowEngineService).checkAuthorization(req, testUser, step);
        assertFalse(requisitionService.canAct(1L, testUser));
    }

    @Test
    void updateRequest_ProjectChanged_UpdatesProjectAndKey() {
        Request request = new Request();
        request.setRequestID(1L);
        request.setState(RequestStatus.DRAFT);
        request.setUser(testUser);
        request.setProject(testProject);
        
        InternalBudget budget = new InternalBudget();
        request.setBudget(budget);

        Project newProject = new Project();
        newProject.setId(2L);
        newProject.setProjectKey("NEWPRJ");
        newProject.setName("New Project");
        newProject.setRequestCounter(5);
        newProject.setInternalBudget(new InternalBudget());

        RequisitionUpdateDto updates = new RequisitionUpdateDto(
                "Updated Laptop", "Need an updated laptop", 2L, null, Priority.HIGH, List.of());

        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(projectRepository.findById(2L)).thenReturn(Optional.of(newProject));
        when(requestRepository.save(any(Request.class))).thenAnswer(inv -> inv.getArgument(0));
        when(requisitionMapper.toDto(any(Request.class))).thenReturn(mock(RequisitionDto.class));

        requisitionService.updateRequest(1L, updates, testUser);

        assertAll(
            () -> assertEquals(newProject, request.getProject()),
            () -> assertEquals("NEWPRJ-6", request.getRequestKey()),
            () -> assertEquals(6, newProject.getRequestCounter()),
            () -> assertEquals(newProject.getInternalBudget(), budget.getParentBudget())
        );
        verify(projectRepository).save(newProject);
    }

    @Test
    void updateRequest_WorkflowChanged_ResetsToDraft() {
        Request request = new Request();
        request.setRequestID(1L);
        request.setState(RequestStatus.DRAFT);
        request.setUser(testUser);
        request.setProject(testProject);
        request.setWorkflowDefinition(testWorkflow);

        WorkflowDefinition newWorkflow = new WorkflowDefinition();
        newWorkflow.setId(2L);
        newWorkflow.setName("New Workflow");

        WorkflowStep startStep = new WorkflowStep();
        startStep.setName("New Start");
        startStep.setWorkflowComponent(WorkflowComponent.START_EVENT);

        RequisitionUpdateDto updates = new RequisitionUpdateDto(
                "Updated Laptop", "Need an updated laptop", 1L, 2L, Priority.HIGH, List.of());

        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(workflowDefinitionRepository.findById(2L)).thenReturn(Optional.of(newWorkflow));
        when(workflowStepRepository.findFirstByWorkflowDefinitionAndWorkflowComponent(newWorkflow, WorkflowComponent.START_EVENT))
            .thenReturn(Optional.of(startStep));
        when(requestRepository.save(any(Request.class))).thenAnswer(inv -> inv.getArgument(0));
        when(requisitionMapper.toDto(any(Request.class))).thenReturn(mock(RequisitionDto.class));

        requisitionService.updateRequest(1L, updates, testUser);

        assertAll(
            () -> assertEquals(newWorkflow, request.getWorkflowDefinition()),
            () -> assertEquals(startStep, request.getCurrentStep()),
            () -> assertEquals(testUser, request.getAssignee()),
            () -> assertEquals(RequestStatus.DRAFT, request.getState())
        );
    }

    @Test
    void approveRequest_ApprovesToFinishedState_SendsNotifications() {
        User creator = new User();
        creator.setId(10L);
        creator.setEmail("creator@veritas.com");
        
        User assignee = new User();
        assignee.setId(11L);
        assignee.setEmail("assignee@veritas.com");

        Request request = new Request();
        request.setRequestID(1L);
        request.setState(RequestStatus.ACTIVE);
        request.setUser(creator);
        request.setAssignee(assignee);
        request.setJiraIssueKey("TEST-1");

        WorkflowStep step = new WorkflowStep();
        step.setName("Approval Step");
        request.setCurrentStep(step);

        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(requestRepository.save(any(Request.class))).thenAnswer(inv -> {
            Request r = inv.getArgument(0);
            r.setState(RequestStatus.FINISHED);
            return r;
        });

        User financeOfficer = new User();
        financeOfficer.setEmail("fo@veritas.com");
        financeOfficer.setRole(UserRole.FINANCE_OFFICER);
        when(userRepository.findAllByRoleAndIsActiveTrue(UserRole.FINANCE_OFFICER)).thenReturn(List.of(financeOfficer));

        RequisitionDto expectedDto = mock(RequisitionDto.class);
        when(requisitionMapper.toDto(any(Request.class))).thenReturn(expectedDto);

        RequisitionDto result = requisitionService.approveRequest(1L, testUser, null);

        assertNotNull(result);
        verify(notificationService).createNotification(eq(creator), any(), eq(NotificationType.APPROVED), anyString());
        verify(notificationService).createNotification(eq(creator), any(), eq(NotificationType.FINISHED), anyString());
        verify(notificationService).createNotification(eq(financeOfficer), any(), eq(NotificationType.ASSIGNED), anyString());
    }

    @Test
    void submitRequest_WithAssignee_SendsNotification() {
        User assignee = new User();
        assignee.setId(20L);
        assignee.setEmail("assignee@veritas.com");

        Request request = new Request();
        request.setRequestID(100L);
        request.setState(RequestStatus.DRAFT);
        request.setWorkflowDefinition(testWorkflow);

        when(requestRepository.findById(100L)).thenReturn(Optional.of(request));
        when(requestRepository.save(any(Request.class))).thenAnswer(inv -> {
            Request r = inv.getArgument(0);
            r.setAssignee(assignee);
            return r;
        });
        when(requisitionMapper.toDto(any(Request.class))).thenReturn(mock(RequisitionDto.class));

        requisitionService.submitRequest(100L, testUser, null);

        verify(notificationService).createNotification(eq(assignee), any(), eq(NotificationType.SUBMITTED), anyString());
    }

    @Test
    void createInvoice_RequestNotFound_ThrowsEntityNotFoundException() {
        when(requestRepository.findById(999L)).thenReturn(Optional.empty());
        InvoiceCreateDto createDto = new InvoiceCreateDto();
        assertThrows(EntityNotFoundException.class, () -> requisitionService.createInvoice(999L, createDto, null, testUser));
    }

    @Test
    void createInvoice_InvoiceAlreadyExists_ThrowsEntityExistsException() {
        Request request = new Request();
        request.setInvoice(new Invoice());
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        
        InvoiceCreateDto createDto = new InvoiceCreateDto();
        assertThrows(EntityExistsException.class, () -> requisitionService.createInvoice(1L, createDto, null, testUser));
    }

    @Test
    void createInvoice_NoSelectedQuote_ThrowsIllegalStateException() {
        Request request = new Request();
        request.setInvoice(null);
        request.getQuotes().clear();
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));

        InvoiceCreateDto createDto = new InvoiceCreateDto();
        assertThrows(IllegalStateException.class, () -> requisitionService.createInvoice(1L, createDto, null, testUser));
    }

    @Test
    void createInvoice_SuccessWithoutFile() {
        Request request = new Request();
        request.setRequestID(1L);
        request.setInvoice(null);
        
        Quote quote = new Quote();
        Vendor vendor = new Vendor();
        vendor.setId(10L);
        quote.setVendorID(vendor);
        quote.setSelected(true);
        request.getQuotes().add(quote);

        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        
        InvoiceCreateDto createDto = new InvoiceCreateDto();
        createDto.setInvoiceNumber("INV-100");
        createDto.setInvoiceDate(LocalDateTime.now().toLocalDate());
        createDto.setTotalAmount(BigDecimal.TEN);
        createDto.setCurrency(Currency.EUR);
        createDto.setDueDate(LocalDateTime.now().toLocalDate().plusDays(30));

        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));
        when(currencyConversionService.convert(any(), any())).thenReturn(new CurrencyConversionResult(BigDecimal.TEN, BigDecimal.ONE, LocalDateTime.now(), ExchangeRateSource.FRANKFURTER));

        InvoiceDto expectedDto = InvoiceDto.builder()
            .invoiceId(100L)
            .invoiceNumber("INV-100")
            .totalAmount(BigDecimal.TEN)
            .currency(Currency.EUR)
            .totalAmountEuro(BigDecimal.TEN)
            .isPaid(false)
            .build();
        when(invoiceMapper.toDto(any(Invoice.class), any(), any(), any())).thenReturn(expectedDto);

        InvoiceDto result = requisitionService.createInvoice(1L, createDto, null, testUser);

        assertAll(
            () -> assertNotNull(result),
            () -> assertEquals("INV-100", result.invoiceNumber())
        );
    }

    @Test
    void getInvoice_RequestNotFound_ThrowsEntityNotFoundException() {
        when(requestRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> requisitionService.getInvoice(999L, testUser));
    }

    @Test
    void getInvoice_InvoiceNotFound_ThrowsEntityNotFoundException() {
        Request request = new Request();
        request.setInvoice(null);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        assertThrows(EntityNotFoundException.class, () -> requisitionService.getInvoice(1L, testUser));
    }

    @Test
    void getInvoice_Success() {
        Request request = new Request();
        request.setRequestID(1L);
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(100L);
        invoice.setTotalAmount(BigDecimal.TEN);
        invoice.setCurrency(Currency.EUR);
        request.setInvoice(invoice);

        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(currencyConversionService.convert(any(), any())).thenReturn(new CurrencyConversionResult(BigDecimal.TEN, BigDecimal.ONE, LocalDateTime.now(), ExchangeRateSource.FRANKFURTER));

        InvoiceDto expectedDto = InvoiceDto.builder()
            .invoiceId(100L)
            .invoiceNumber("INV-100")
            .totalAmount(BigDecimal.TEN)
            .currency(Currency.EUR)
            .totalAmountEuro(BigDecimal.TEN)
            .isPaid(false)
            .build();
        when(invoiceMapper.toDto(any(Invoice.class), any(), any(), any())).thenReturn(expectedDto);

        InvoiceDto result = requisitionService.getInvoice(1L, testUser);
        assertAll(
            () -> assertNotNull(result),
            () -> assertEquals(100L, result.invoiceId())
        );
    }

    @Test
    void deleteInvoice_RequestNotFound_ThrowsEntityNotFoundException() {
        when(requestRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> requisitionService.deleteInvoice(999L, testUser));
    }

    @Test
    void deleteInvoice_InvoiceNotFound_ThrowsEntityNotFoundException() {
        Request request = new Request();
        request.setInvoice(null);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        assertThrows(EntityNotFoundException.class, () -> requisitionService.deleteInvoice(1L, testUser));
    }

    @Test
    void deleteInvoice_InvoicePaid_ThrowsIllegalStateException() {
        Request request = new Request();
        Invoice invoice = new Invoice();
        invoice.setIsPaid(true);
        request.setInvoice(invoice);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        assertThrows(IllegalStateException.class, () -> requisitionService.deleteInvoice(1L, testUser));
    }

    @Test
    void deleteInvoice_Success() {
        Request request = new Request();
        Invoice invoice = new Invoice();
        invoice.setIsPaid(false);
        request.setInvoice(invoice);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));

        assertDoesNotThrow(() -> requisitionService.deleteInvoice(1L, testUser));
        verify(invoiceRepository).delete(invoice);
    }

    @Test
    void downloadAttachment_AttachmentNotFound_ThrowsEntityNotFoundException() {
        when(attachmentRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> requisitionService.downloadAttachment(999L, testUser));
    }

    @Test
    void saveAttachmentFromInputStream_RequestNotFound_ThrowsIllegalArgumentException() {
        when(requestRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> requisitionService.saveAttachmentFromInputStream(999L, "file.txt", "text/plain", 100L, null));
    }

    @Test
    void downloadAttachment_FileNotReadable_ThrowsRuntimeException() {
        Attachment attachment = new Attachment();
        attachment.setAttachmentId(1L);
        attachment.setFileName("test.pdf");
        attachment.setFileType("application/pdf");
        attachment.setStoragePath("non-existent-path/file.pdf");

        when(attachmentRepository.findById(1L)).thenReturn(Optional.of(attachment));

        assertThrows(RuntimeException.class, () -> requisitionService.downloadAttachment(1L, testUser));
    }

    @Test
    void deleteAttachment_WithInvoice_DeletesInvoiceInstead() {
        Attachment attachment = new Attachment();
        attachment.setAttachmentId(1L);
        Request request = new Request();
        request.setRequestID(10L);
        attachment.setRequest(request);
        Invoice invoice = new Invoice();
        invoice.setIsPaid(false);
        invoice.setAttachments(new ArrayList<>());
        request.setInvoice(invoice);
        attachment.setInvoice(invoice);

        when(attachmentRepository.findById(1L)).thenReturn(Optional.of(attachment));
        when(requestRepository.findById(10L)).thenReturn(Optional.of(request));

        requisitionService.deleteAttachment(1L, testUser);

        verify(invoiceRepository).delete(invoice);
    }

    @Test
    void approveRequest_WithPaidInvoice_ThrowsWorkflowStateException() {
        Request request = new Request();
        request.setRequestID(10L);
        request.setState(RequestStatus.ACTIVE);
        Invoice invoice = new Invoice();
        invoice.setIsPaid(true);
        request.setInvoice(invoice);

        when(requestRepository.findById(10L)).thenReturn(Optional.of(request));

        assertThrows(WorkflowStateException.class, () -> requisitionService.approveRequest(10L, testUser, null));
    }

    @Test
    void approveRequest_NotFinished_NoAssignee_SkipsAssigneeNotification() {
        Request request = new Request();
        request.setRequestID(10L);
        request.setState(RequestStatus.ACTIVE);
        request.setAssignee(null);
        request.setRequestName("Test");

        when(requestRepository.findById(10L)).thenReturn(Optional.of(request));
        when(requestRepository.save(any(Request.class))).thenReturn(request);

        requisitionService.approveRequest(10L, testUser, null);

        verify(requestRepository).save(any());
    }

    @Test
    void revertRequest_WithPaidInvoice_ThrowsWorkflowStateException() {
        Request request = new Request();
        request.setRequestID(10L);
        request.setState(RequestStatus.ACTIVE);
        Invoice invoice = new Invoice();
        invoice.setIsPaid(true);
        request.setInvoice(invoice);

        when(requestRepository.findById(10L)).thenReturn(Optional.of(request));

        RequisitionRejectDto rejectDto = new RequisitionRejectDto();
        rejectDto.setReason("Reason");
        rejectDto.setRevisionRequired(false);
        assertThrows(WorkflowStateException.class, () -> requisitionService.revertRequest(10L, testUser, rejectDto));
    }

    @Test
    void revertRequest_BlankReason_AppendsPeriodInMessage() {
        Request request = new Request();
        request.setRequestID(10L);
        request.setState(RequestStatus.ACTIVE);
        User creator = new User();
        creator.setEmail("creator@test.com");
        request.setUser(creator);

        when(requestRepository.findById(10L)).thenReturn(Optional.of(request));
        when(requestRepository.save(any(Request.class))).thenReturn(request);

        RequisitionRejectDto rejectDto = new RequisitionRejectDto();
        rejectDto.setReason("");
        rejectDto.setRevisionRequired(false);
        requisitionService.revertRequest(10L, testUser, rejectDto);

        verify(notificationService).createNotification(
                eq(creator), any(), eq(NotificationType.REVERTED),
                ArgumentMatchers.endsWith(".")
        );
    }

    @Test
    void rejectRequest_WithPaidInvoice_ThrowsWorkflowStateException() {
        Request request = new Request();
        request.setRequestID(10L);
        request.setState(RequestStatus.ACTIVE);
        Invoice invoice = new Invoice();
        invoice.setIsPaid(true);
        request.setInvoice(invoice);

        when(requestRepository.findById(10L)).thenReturn(Optional.of(request));

        RequisitionRejectDto rejectDto = new RequisitionRejectDto();
        rejectDto.setReason("Reason");
        rejectDto.setRevisionRequired(false);
        assertThrows(WorkflowStateException.class, () -> requisitionService.rejectRequest(10L, testUser, rejectDto));
    }

    @Test
    void rejectRequest_NoJiraKey_SkipsJiraSync() {
        Request request = new Request();
        request.setRequestID(10L);
        request.setState(RequestStatus.ACTIVE);
        request.setJiraIssueKey(null);

        when(requestRepository.findById(10L)).thenReturn(Optional.of(request));
        when(requestRepository.save(any(Request.class))).thenReturn(request);

        RequisitionRejectDto rejectDto = new RequisitionRejectDto();
        rejectDto.setReason("Rejected");
        rejectDto.setRevisionRequired(false);
        requisitionService.rejectRequest(10L, testUser, rejectDto);

        verify(jiraSyncService, never()).handleVeritasWorkflowChange(any());
    }

    @Test
    void rejectRequest_BlankReason_AppendsPeriodInMessage() {
        Request request = new Request();
        request.setRequestID(10L);
        request.setState(RequestStatus.ACTIVE);
        request.setJiraIssueKey(null);
        User creator = new User();
        creator.setEmail("c@test.com");
        request.setUser(creator);

        when(requestRepository.findById(10L)).thenReturn(Optional.of(request));
        when(requestRepository.save(any(Request.class))).thenReturn(request);

        RequisitionRejectDto rejectDto = new RequisitionRejectDto();
        rejectDto.setReason("");
        rejectDto.setRevisionRequired(false);
        requisitionService.rejectRequest(10L, testUser, rejectDto);

        verify(notificationService).createNotification(
                eq(creator), any(), eq(NotificationType.REJECTED),
                ArgumentMatchers.endsWith(".")
        );
    }

    @Test
    void getNextStepRole_NullStep_ReturnsNull() {
        Request request = new Request();
        request.setRequestID(10L);

        when(requestRepository.findById(10L)).thenReturn(Optional.of(request));
        when(workflowEngineService.getNextStep(request)).thenReturn(null);

        assertNull(requisitionService.getNextStepRole(10L, testUser));
    }

    @Test
    void getNextStepRole_NullRole_ReturnsNull() {
        Request request = new Request();
        request.setRequestID(10L);

        WorkflowStep nextStep = new WorkflowStep();
        nextStep.setRole(null);

        when(requestRepository.findById(10L)).thenReturn(Optional.of(request));
        when(workflowEngineService.getNextStep(request)).thenReturn(nextStep);

        assertNull(requisitionService.getNextStepRole(10L, testUser));
    }

    @Test
    void getNextStepRole_AutomatedStep_ReturnsNull() {
        Request request = new Request();
        request.setRequestID(10L);

        WorkflowStep nextStep = new WorkflowStep();
        nextStep.setRole(UserRole.REQUESTER);
        nextStep.setIsAutomatedApproval(true);

        when(requestRepository.findById(10L)).thenReturn(Optional.of(request));
        when(workflowEngineService.getNextStep(request)).thenReturn(nextStep);

        assertNull(requisitionService.getNextStepRole(10L, testUser));
    }

    @Test
    void getEligibleAssignees_NullUserDept_ReturnsAllProcurementOfficers() {
        Request request = new Request();
        request.setRequestID(10L);
        request.setUser(null);

        when(requestRepository.findById(10L)).thenReturn(Optional.of(request));

        User candidate = new User();
        candidate.setId(5L);
        candidate.setRole(UserRole.PROCUREMENT_OFFICER);
        Department dept = new Department();
        dept.setDepartmentId(1L);
        candidate.setDepartment(dept);
        when(userRepository.findAllByRoleAndIsActiveTrue(UserRole.PROCUREMENT_OFFICER)).thenReturn(List.of(candidate));

        requisitionService.getEligibleAssignees(10L, "PROCUREMENT_OFFICER", testUser);

        verify(userMapper).toUserDto(candidate);
    }

    @Test
    void getRequests_RequesterWithTeamAndLeaderIsUser_UserIdFilterNull() {
        testUser.setRole(UserRole.REQUESTER);
        Team team = new Team();
        team.setTeamId(2L);
        team.setLeader(testUser);
        testUser.setTeam(team);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        Pageable pageable = PageRequest.of(0, 10);
        when(requestRepository.findFilteredRequests(null, null, null, null, null, 2L, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of()));

        requisitionService.getRequests(null, null, null, null, null, null, testUser, pageable);

        verify(requestRepository).findFilteredRequests(null, null, null, null, null, 2L, null, null, null, pageable);
    }

    @Test
    void getRequests_RequesterWithTeamAndLeaderNotUser_UserIdFilterIsUser() {
        testUser.setRole(UserRole.REQUESTER);
        Team team = new Team();
        team.setTeamId(2L);
        User leader = new User();
        leader.setId(99L);
        team.setLeader(leader);
        testUser.setTeam(team);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        Pageable pageable = PageRequest.of(0, 10);
        when(requestRepository.findFilteredRequests(null, null, null, 1L, null, 2L, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of()));

        requisitionService.getRequests(null, null, null, null, null, null, testUser, pageable);

        verify(requestRepository).findFilteredRequests(null, null, null, 1L, null, 2L, null, null, null, pageable);
    }

    @Test
    void getRequests_RequesterWithoutTeam_UserIdFilterIsUser() {
        testUser.setRole(UserRole.REQUESTER);
        testUser.setTeam(null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        Pageable pageable = PageRequest.of(0, 10);
        when(requestRepository.findFilteredRequests(null, null, null, 1L, null, null, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of()));

        requisitionService.getRequests(null, null, null, null, null, null, testUser, pageable);

        verify(requestRepository).findFilteredRequests(null, null, null, 1L, null, null, null, null, null, pageable);
    }

    @Test
    void getRequests_ProcurementOfficerWithDept_DeptFilterIsSet() {
        testUser.setRole(UserRole.PROCUREMENT_OFFICER);
        Department dept = new Department();
        dept.setDepartmentId(3L);
        testUser.setDepartment(dept);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        Pageable pageable = PageRequest.of(0, 10);
        when(requestRepository.findFilteredRequests(null, null, null, null, null, null, 3L, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of()));

        requisitionService.getRequests(null, null, null, null, null, null, testUser, pageable);

        verify(requestRepository).findFilteredRequests(null, null, null, null, null, null, 3L, null, null, pageable);
    }

    @Test
    void getRequests_ProcurementOfficerWithoutDept_DeptFilterIsMinusOne() {
        testUser.setRole(UserRole.PROCUREMENT_OFFICER);
        testUser.setDepartment(null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        Pageable pageable = PageRequest.of(0, 10);
        when(requestRepository.findFilteredRequests(null, null, null, null, null, null, -1L, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of()));

        requisitionService.getRequests(null, null, null, null, null, null, testUser, pageable);

        verify(requestRepository).findFilteredRequests(null, null, null, null, null, null, -1L, null, null, pageable);
    }

    @Test
    void getRequests_WithCreatorIdAndRequesterRoleWithUserIdFilterNull_SetsUserIdFilterToCreatorId() {
        testUser.setRole(UserRole.REQUESTER);
        Team team = new Team();
        team.setTeamId(2L);
        team.setLeader(testUser);
        testUser.setTeam(team);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        Pageable pageable = PageRequest.of(0, 10);
        when(requestRepository.findFilteredRequests(null, null, null, 99L, null, 2L, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of()));

        requisitionService.getRequests(null, null, null, null, null, 99L, testUser, pageable);

        verify(requestRepository).findFilteredRequests(null, null, null, 99L, null, 2L, null, null, null, pageable);
    }

    @Test
    void getRequests_WithCreatorIdAndAdminRole_SetsUserIdFilterToCreatorId() {
        testUser.setRole(UserRole.ADMINISTRATOR);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        Pageable pageable = PageRequest.of(0, 10);
        when(requestRepository.findFilteredRequests(null, null, null, 99L, null, null, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of()));

        requisitionService.getRequests(null, null, null, null, null, 99L, testUser, pageable);

        verify(requestRepository).findFilteredRequests(null, null, null, 99L, null, null, null, null, null, pageable);
    }

    @Test
    void getRequests_WithDateFilters_ParsesStartAndEndOfDay() {
        testUser.setRole(UserRole.ADMINISTRATOR);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        java.time.LocalDate from = java.time.LocalDate.of(2026, 6, 1);
        java.time.LocalDate to = java.time.LocalDate.of(2026, 6, 20);

        Pageable pageable = PageRequest.of(0, 10);
        when(requestRepository.findFilteredRequests(null, null, null, null, null, null, null, from.atStartOfDay(), to.atTime(23, 59, 59, 999999999), pageable))
                .thenReturn(new PageImpl<>(List.of()));

        requisitionService.getRequests(null, null, null, from, to, null, testUser, pageable);

        verify(requestRepository).findFilteredRequests(null, null, null, null, null, null, null, from.atStartOfDay(), to.atTime(23, 59, 59, 999999999), pageable);
    }

    @Test
    void getEligibleAssignees_NullTeam_ReturnsAllProcurementOfficers() {
        Request request = new Request();
        request.setRequestID(10L);
        User creator = new User();
        creator.setTeam(null);
        request.setUser(creator);

        when(requestRepository.findById(10L)).thenReturn(Optional.of(request));

        User candidate = new User();
        candidate.setId(5L);
        candidate.setRole(UserRole.PROCUREMENT_OFFICER);
        Department dept = new Department();
        dept.setDepartmentId(1L);
        candidate.setDepartment(dept);
        when(userRepository.findAllByRoleAndIsActiveTrue(UserRole.PROCUREMENT_OFFICER)).thenReturn(List.of(candidate));

        requisitionService.getEligibleAssignees(10L, "PROCUREMENT_OFFICER", testUser);

        verify(userMapper).toUserDto(candidate);
    }

    @Test
    void getEligibleAssignees_NullDepartmentOnTeam_ReturnsAllProcurementOfficers() {
        Request request = new Request();
        request.setRequestID(10L);
        User creator = new User();
        Team team = new Team();
        team.setDepartment(null);
        creator.setTeam(team);
        request.setUser(creator);

        when(requestRepository.findById(10L)).thenReturn(Optional.of(request));

        User candidate = new User();
        candidate.setId(5L);
        candidate.setRole(UserRole.PROCUREMENT_OFFICER);
        Department dept = new Department();
        dept.setDepartmentId(1L);
        candidate.setDepartment(dept);
        when(userRepository.findAllByRoleAndIsActiveTrue(UserRole.PROCUREMENT_OFFICER)).thenReturn(List.of(candidate));

        requisitionService.getEligibleAssignees(10L, "PROCUREMENT_OFFICER", testUser);

        verify(userMapper).toUserDto(candidate);
    }

    @Test
    void getEligibleAssignees_RequesterRoleMatchingDept_FiltersCorrectly() {
        Request request = new Request();
        request.setRequestID(10L);
        User creator = new User();
        Team team = new Team();
        Department dept = new Department();
        dept.setDepartmentId(1L);
        team.setDepartment(dept);
        creator.setTeam(team);
        request.setUser(creator);

        when(requestRepository.findById(10L)).thenReturn(Optional.of(request));

        User c1 = new User();
        c1.setId(5L);
        c1.setRole(UserRole.REQUESTER);
        Team c1Team = new Team();
        c1Team.setDepartment(dept);
        c1.setTeam(c1Team);

        User c2 = new User();
        c2.setId(6L);
        c2.setRole(UserRole.REQUESTER);
        Team c2Team = new Team();
        Department diffDept = new Department();
        diffDept.setDepartmentId(2L);
        c2Team.setDepartment(diffDept);
        c2.setTeam(c2Team);

        User c3 = new User();
        c3.setId(7L);
        c3.setRole(UserRole.REQUESTER);
        c3.setTeam(null);

        User c4 = new User();
        c4.setId(8L);
        c4.setRole(UserRole.REQUESTER);
        Team c4Team = new Team();
        c4Team.setDepartment(null);
        c4.setTeam(c4Team);

        when(userRepository.findAllByRoleAndIsActiveTrue(UserRole.REQUESTER)).thenReturn(List.of(c1, c2, c3, c4));

        requisitionService.getEligibleAssignees(10L, "REQUESTER", testUser);

        verify(userMapper).toUserDto(c1);
        verify(userMapper, never()).toUserDto(c2);
        verify(userMapper, never()).toUserDto(c3);
        verify(userMapper, never()).toUserDto(c4);
    }

    @Test
    void getEligibleAssignees_ProcurementOfficerRoleMatchingDept_FiltersCorrectly() {
        Request request = new Request();
        request.setRequestID(10L);
        User creator = new User();
        Team team = new Team();
        Department dept = new Department();
        dept.setDepartmentId(1L);
        team.setDepartment(dept);
        creator.setTeam(team);
        request.setUser(creator);

        when(requestRepository.findById(10L)).thenReturn(Optional.of(request));

        User c1 = new User();
        c1.setId(5L);
        c1.setRole(UserRole.PROCUREMENT_OFFICER);
        c1.setDepartment(dept);

        User c2 = new User();
        c2.setId(6L);
        c2.setRole(UserRole.PROCUREMENT_OFFICER);
        Department diffDept = new Department();
        diffDept.setDepartmentId(2L);
        c2.setDepartment(diffDept);

        User c3 = new User();
        c3.setId(7L);
        c3.setRole(UserRole.PROCUREMENT_OFFICER);
        c3.setDepartment(null);

        when(userRepository.findAllByRoleAndIsActiveTrue(UserRole.PROCUREMENT_OFFICER)).thenReturn(List.of(c1, c2, c3));

        requisitionService.getEligibleAssignees(10L, "PROCUREMENT_OFFICER", testUser);

        verify(userMapper).toUserDto(c1);
        verify(userMapper, never()).toUserDto(c2);
        verify(userMapper, never()).toUserDto(c3);
    }

    @Test
    void updateRequest_NullProjectAndNullWorkflow_SetsDescriptionsAndSaves() {
        Request request = new Request();
        request.setRequestID(10L);
        request.setState(RequestStatus.DRAFT);
        User creator = new User();
        creator.setId(1L);
        request.setUser(creator);
        request.setRequestName("Old Name");
        request.setDescription("Old Desc");
        request.setPriority(Priority.LOW);
        request.setProject(null);
        request.setWorkflowDefinition(null);
        request.setItems(new ArrayList<>());

        when(requestRepository.findById(10L)).thenReturn(Optional.of(request));
        when(requestRepository.save(any())).thenReturn(request);

        RequisitionUpdateDto updates = new RequisitionUpdateDto(
            "New Name", "New Desc", null, null, Priority.HIGH, new ArrayList<>()
        );

        requisitionService.updateRequest(10L, updates, testUser);

        verify(requestRepository).save(any());
        verify(auditService).createRequisitionChangeLog(eq(testUser), eq(request), anyString());
    }

    @Test
    void updateRequest_NullOldDescAndNewDescNotBlank_LogsDescriptionSet() {
        Request request = new Request();
        request.setRequestID(10L);
        request.setState(RequestStatus.DRAFT);
        User creator = new User();
        creator.setId(1L);
        request.setUser(creator);
        request.setRequestName("Name");
        request.setDescription(null);
        request.setPriority(Priority.LOW);
        request.setItems(new ArrayList<>());

        when(requestRepository.findById(10L)).thenReturn(Optional.of(request));
        when(requestRepository.save(any())).thenReturn(request);

        RequisitionUpdateDto updates = new RequisitionUpdateDto(
            "Name", "New Desc", null, null, Priority.LOW, new ArrayList<>()
        );

        requisitionService.updateRequest(10L, updates, testUser);

        verify(auditService).createRequisitionChangeLog(eq(testUser), eq(request), contains("Description set to 'New Desc'"));
    }

    @Test
    void updateRequest_OldDescNotBlankAndNewDescBlank_LogsDescriptionCleared() {
        Request request = new Request();
        request.setRequestID(10L);
        request.setState(RequestStatus.DRAFT);
        User creator = new User();
        creator.setId(1L);
        request.setUser(creator);
        request.setRequestName("Name");
        request.setDescription("Old Desc");
        request.setPriority(Priority.LOW);
        request.setItems(new ArrayList<>());

        when(requestRepository.findById(10L)).thenReturn(Optional.of(request));
        when(requestRepository.save(any())).thenReturn(request);

        RequisitionUpdateDto updates = new RequisitionUpdateDto(
            "Name", "", null, null, Priority.LOW, new ArrayList<>()
        );

        requisitionService.updateRequest(10L, updates, testUser);

        verify(auditService).createRequisitionChangeLog(eq(testUser), eq(request), contains("Description cleared (was 'Old Desc')"));
    }

    @Test
    void updateRequest_ChangeWorkflow_SetsWorkflowAndResetsToDraft() {
        Request request = new Request();
        request.setRequestID(10L);
        request.setState(RequestStatus.DRAFT);
        User creator = new User();
        creator.setId(1L);
        request.setUser(creator);
        request.setRequestName("Name");
        request.setDescription("Desc");
        request.setPriority(Priority.LOW);
        request.setItems(new ArrayList<>());

        WorkflowDefinition oldWf = new WorkflowDefinition();
        oldWf.setId(1L);
        oldWf.setName("Old Workflow");
        request.setWorkflowDefinition(oldWf);

        when(requestRepository.findById(10L)).thenReturn(Optional.of(request));
        
        WorkflowDefinition newWf = new WorkflowDefinition();
        newWf.setId(2L);
        newWf.setName("New Workflow");
        when(workflowDefinitionRepository.findById(2L)).thenReturn(Optional.of(newWf));

        WorkflowStep startStep = new WorkflowStep();
        startStep.setId(5L);
        when(workflowStepRepository.findFirstByWorkflowDefinitionAndWorkflowComponent(newWf, WorkflowComponent.START_EVENT))
                .thenReturn(Optional.of(startStep));

        when(requestRepository.save(any())).thenReturn(request);

        RequisitionUpdateDto updates = new RequisitionUpdateDto(
            "Name", "Desc", null, 2L, Priority.LOW, new ArrayList<>()
        );

        requisitionService.updateRequest(10L, updates, testUser);

        verify(requestRepository).save(any());
        assertEquals(newWf, request.getWorkflowDefinition());
        assertEquals(startStep, request.getCurrentStep());
    }

    @Test
    void updateRequest_LineItemsChangeIncomingNull_RemovesAllItems() {
        Request request = new Request();
        request.setRequestID(10L);
        request.setState(RequestStatus.DRAFT);
        User creator = new User();
        creator.setId(1L);
        request.setUser(creator);
        request.setRequestName("Name");
        request.setItems(new ArrayList<>());
        RequestItem ri = new RequestItem();
        ri.setName("Item1");
        ri.setQuantity(2);
        ri.setUnit(RequestItemUnit.PIECES);
        ri.setDescription("Item Desc");
        request.getItems().add(ri);

        when(requestRepository.findById(10L)).thenReturn(Optional.of(request));
        when(requestRepository.save(any())).thenReturn(request);

        RequisitionUpdateDto updates = new RequisitionUpdateDto(
            "Name", null, null, null, Priority.LOW, null
        );

        requisitionService.updateRequest(10L, updates, testUser);

        verify(requestRepository).save(any());
        assertTrue(request.getItems().isEmpty());
    }

    @Test
    void updateRequest_LineItemsChangeDifferentCount_UpdatesItems() {
        Request request = new Request();
        request.setRequestID(10L);
        request.setState(RequestStatus.DRAFT);
        User creator = new User();
        creator.setId(1L);
        request.setUser(creator);
        request.setRequestName("Name");
        request.setItems(new ArrayList<>());
        RequestItem ri = new RequestItem();
        ri.setName("Item1");
        ri.setQuantity(2);
        ri.setUnit(RequestItemUnit.PIECES);
        request.getItems().add(ri);

        when(requestRepository.findById(10L)).thenReturn(Optional.of(request));
        when(requestRepository.save(any())).thenReturn(request);

        RequisitionItemCreateDto itemDto = new RequisitionItemCreateDto("Item1", 3, RequestItemUnit.PIECES, "New Description");
        RequisitionUpdateDto updates = new RequisitionUpdateDto(
            "Name", null, null, null, Priority.LOW, List.of(itemDto)
        );

        requisitionService.updateRequest(10L, updates, testUser);

        verify(requestRepository).save(any());
    }
}

