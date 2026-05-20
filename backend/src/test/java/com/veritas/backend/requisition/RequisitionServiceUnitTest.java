package com.veritas.backend.requisition;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;

import com.veritas.backend.budget.entity.InternalBudget;
import com.veritas.backend.budget.repository.InternalBudgetRepository;
import com.veritas.backend.project.entity.Project;
import com.veritas.backend.project.repository.ProjectRepository;
import com.veritas.backend.requisition.dto.RequisitionCreateDto;
import com.veritas.backend.requisition.dto.RequisitionDto;
import com.veritas.backend.requisition.dto.RequisitionItemCreateDto;
import com.veritas.backend.requisition.entity.Attachment;
import com.veritas.backend.requisition.entity.Priority;
import com.veritas.backend.requisition.entity.Request;
import com.veritas.backend.requisition.entity.RequestStatus;
import com.veritas.backend.requisition.entity.RequestItem;
import com.veritas.backend.requisition.mapper.RequisitionMapper;
import com.veritas.backend.requisition.repository.AttachmentRepository;
import com.veritas.backend.requisition.repository.RequestItemRepository;
import com.veritas.backend.requisition.repository.RequestRepository;
import com.veritas.backend.requisition.service.impl.RequisitionServiceImpl;
import com.veritas.backend.team.entity.Team;
import com.veritas.backend.user.entity.User;
import com.veritas.backend.user.entity.UserRole;
import com.veritas.backend.user.repository.UserRepository;
import com.veritas.backend.workflow.entity.WorkflowComponent;
import com.veritas.backend.workflow.entity.WorkflowDefinition;
import com.veritas.backend.workflow.entity.WorkflowStep;
import com.veritas.backend.workflow.repository.WorkflowDefinitionRepository;
import com.veritas.backend.workflow.repository.WorkflowStepRepository;
import com.veritas.backend.workflow.service.WorkflowEngineService;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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
    private WorkflowEngineService workflowEngineService;;
    @Mock
    private InternalBudgetRepository internalBudgetRepository;

    @InjectMocks
    private RequisitionServiceImpl requisitionService;

    @Captor
    private ArgumentCaptor<Request> requestCaptor;
    @Captor
    private ArgumentCaptor<RequestItem> itemCaptor;
    @Captor
    private ArgumentCaptor<Attachment> attachmentCaptor;

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

        testProject = new Project();
        testProject.setId(1L);
        testProject.setProjectKey("PRJ");
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

    // createRequest tests

    private void stupRepositories() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
        when(workflowDefinitionRepository.findById(1L)).thenReturn(Optional.of(testWorkflow));
        when(workflowStepRepository.findByWorkflowDefinitionAndWorkflowComponent(
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
        RequisitionDto expectedDto = new RequisitionDto(
                1L, "New Laptop", "PRJ-11", "Start", false,
                Priority.MEDIUM, "Test Project", "PRJ", "Standard Workflow", "Engineering", "Test User", 1L, 1L, null, null, null, null, null, null, null, null, "", null);
        when(requisitionMapper.toDto(any(Request.class))).thenReturn(expectedDto);

        RequisitionCreateDto createDto = new RequisitionCreateDto(
                "New Laptop", "Need a laptop", 1L, 1L, Priority.MEDIUM,
                List.of(new RequisitionItemCreateDto("MacBook Pro", 2, "pcs", "test")));

        RequisitionDto result = requisitionService.createRequest(createDto, testUser);

        assertNotNull(result);
        assertEquals("New Laptop", result.requestName());
        verify(requestRepository).save(requestCaptor.capture());
        Request saved = requestCaptor.getValue();
        assertEquals("New Laptop", saved.getRequestName());
        assertEquals("Need a laptop", saved.getDescription());
        assertEquals(Priority.MEDIUM, saved.getPriority());
        assertEquals(testStartStep, saved.getCurrentStepID());
        assertEquals(testUser, saved.getUserID());
        assertEquals(testTeam, saved.getTeamID());
        assertEquals("PRJ-11", saved.getRequestKey());
        assertEquals(11, testProject.getRequestCounter());
        verify(projectRepository).save(testProject);
    }

    @Test
    void CreateRequest_WithMultipleItems_SavesAllItems() {
        stupRepositories();
        when(requisitionMapper.toDto(any())).thenReturn(mock(RequisitionDto.class));

        List<RequisitionItemCreateDto> items = List.of(
                new RequisitionItemCreateDto("Monitor", 3, "pcs", "27 inch"),
                new RequisitionItemCreateDto("Keyboard", 5, "pcs", "Mechanical"),
                new RequisitionItemCreateDto("Cable", 10, "m", "USB-C"));

        RequisitionCreateDto createDto = new RequisitionCreateDto(
                "Office Equipment", "Test for employees", 1L, 1L, Priority.MEDIUM, items);

        requisitionService.createRequest(createDto, testUser);

        verify(requestItemRepository, times(3)).save(itemCaptor.capture());
        List<RequestItem> savedItems = itemCaptor.getAllValues();
        assertEquals("Monitor", savedItems.get(0).getName());
        assertEquals(3, savedItems.get(0).getQuantity());
        assertEquals("pcs", savedItems.get(0).getUnit());
        assertEquals("27 inch", savedItems.get(0).getDescription());
        assertEquals("Cable", savedItems.get(2).getName());
        assertEquals("m", savedItems.get(2).getUnit());
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
                List.of(new RequisitionItemCreateDto("Item", 1, "pcs", null)));

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
                List.of(new RequisitionItemCreateDto("Item", 1, "pcs", null)));

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
                List.of(new RequisitionItemCreateDto("Item", 1, "pcs", null)));

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
        when(workflowStepRepository.findByWorkflowDefinitionAndWorkflowComponent(any(), any()))
                .thenReturn(Optional.empty());

        RequisitionCreateDto createDto = new RequisitionCreateDto(
                "Test", null, 1L, 1L, Priority.LOW,
                List.of(new RequisitionItemCreateDto("Item", 1, "pcs", null)));

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

        requisitionService.saveAttachment(1L, file);

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

        requisitionService.saveAttachment(2L, file);

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
                () -> requisitionService.saveAttachment(999L, file));
        assertTrue(ex.getMessage().contains("Request not found"));
        verify(attachmentRepository, never()).save(any());
    }

    @Test
    void SaveAttachment_IoException_ThrowsRuntimeException() throws IOException {
        Request request = new Request();
        request.setRequestID(1L);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));

        MockMultipartFile file = mock(MockMultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("broken.txt");
        when(file.getInputStream()).thenThrow(new IOException("Disk full"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> requisitionService.saveAttachment(1L, file));
        assertTrue(ex.getMessage().contains("Could not store file"));
        verify(attachmentRepository, never()).save(any());
    }

    @Test
    void GetRequests_WithStatus_FormatsStatusToUpperCaseAndCallsRepository() {
        testUser.setRole(UserRole.ADMINISTRATOR);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        Pageable pageable = PageRequest.of(0, 10);
        when(requestRepository.findFilteredRequests("OPEN", "search", 1L, null, null, null, WorkflowComponent.END_EVENT, pageable))
                .thenReturn(new PageImpl<>(List.of()));

        requisitionService.getRequests("open", "search", 1L, testUser, pageable);

        verify(requestRepository).findFilteredRequests("OPEN", "search", 1L, null, null, null, WorkflowComponent.END_EVENT, pageable);
    }

    @Test
    void GetRequests_WithNullStatus_CallsRepositoryWithNull() {
        testUser.setRole(UserRole.ADMINISTRATOR);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        Pageable pageable = PageRequest.of(0, 10);
        when(requestRepository.findFilteredRequests(null, null, null, null, null, null, WorkflowComponent.END_EVENT, pageable))
                .thenReturn(new PageImpl<>(List.of()));

        requisitionService.getRequests("", "", null, testUser, pageable);

        verify(requestRepository).findFilteredRequests(null, null, null, null, null, null, WorkflowComponent.END_EVENT, pageable);
    }

    @Test
    void GetRequestById_ValidId_ReturnsMappedDto() {
        Request request = new Request();
        request.setRequestID(1L);
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));

        RequisitionDto expectedDto = mock(RequisitionDto.class);
        when(requisitionMapper.toDto(request)).thenReturn(expectedDto);

        RequisitionDto result = requisitionService.getRequestById(1L);

        assertNotNull(result);
        assertEquals(expectedDto, result);
    }

    @Test
    void GetRequestById_InvalidId_ThrowsEntityNotFoundException() {
        when(requestRepository.findById(999L)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> requisitionService.getRequestById(999L));
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

        ResponseEntity<Resource> response = requisitionService.downloadAttachment(1L);

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
                () -> requisitionService.downloadAttachment(999L));
        assertTrue(ex.getMessage().contains("Attachment not found"));
    }

    //AI-Generated
    @Test
    void SubmitRequest_ValidDraft_MovesToFirstStep() {
        // 1. Arrange a mock Request currently in DRAFT status
        Request request = new Request();
        request.setRequestID(100L);
        request.setState(RequestStatus.DRAFT);
        request.setWorkflowDefinitionID(testWorkflow);

        when(requestRepository.findById(100L)).thenReturn(Optional.of(request));

        // Mock finding the start event scoped specifically to this definition ID

        when(requestRepository.save(any(Request.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RequisitionDto expectedDto = new RequisitionDto(
                100L,                         // 1. id
                "Draft Test",                    // 2. requestName
                "PRJ-12",                        // 3. requestKey
                "Start",                         // 4. currentStep
                false,                           // 5. isClosed
                Priority.MEDIUM,                 // 6. priority
                "Test Project",                  // 7. projectName
                "PRJ",                           // 8. projectKey (Using project code)
                "Standard Workflow",             // 9. workflowName
                "Engineering",                   // 10. teamName
                "Test User",                     // 11. requesterName
                1L,                              // 12. requesterId
                1L,                              // 13. requesterTeamId
                "ROLE_MANAGER",                  // 14. responsibleRole (or null)
                java.time.LocalDateTime.now(),   // 15. createdAt
                java.time.LocalDateTime.now(),   // 16. updatedAt
                "Test Description",              // 17. description
                "JIRA-123",                      // 18. jiraIssueKey (or null)
                "https://jira.com/123",          // 19. jiraIssueUrl (or null)
                null,                            // 20. items
                null,                            // 21. attachments
                "ACTIVE",                        // 22. status (Moved to the end!)
                null                             // 23. rejection reason
        );
        when(requisitionMapper.toDto(any(Request.class))).thenReturn(expectedDto);

        doAnswer(invocation -> {
            Request req = invocation.getArgument(0);
            req.setCurrentStepID(testStartStep); // Simulates what startWorkflow actually does
            return null;
        }).when(workflowEngineService).startWorkflow(any(Request.class), any(User.class));
        // 2. Act
        RequisitionDto result = requisitionService.submitRequest(100L, testUser);

        // 3. Assert
        assertNotNull(result);
        verify(requestRepository).save(requestCaptor.capture());
        Request savedRequest = requestCaptor.getValue();

        assertAll("Workflow Initial Submission State Checks",
                () -> assertEquals(RequestStatus.ACTIVE, savedRequest.getState(), "Request state should change to ACTIVE on submit"),
                () -> assertEquals(testStartStep, savedRequest.getCurrentStepID(), "Request should advance cleanly to the workflow's START_EVENT node")
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
                () -> requisitionService.submitRequest(101L, testUser)
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
        request.setUserID(currentRequester);

        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(userRepository.findById(2L)).thenReturn(Optional.of(newRequester));
        when(requestRepository.save(any(Request.class))).thenAnswer(inv -> inv.getArgument(0));

        RequisitionDto expectedDto = mock(RequisitionDto.class);
        when(requisitionMapper.toDto(any(Request.class))).thenReturn(expectedDto);

        RequisitionDto result = requisitionService.changeRequester(1L, 2L);

        assertNotNull(result);
        verify(requestRepository).save(requestCaptor.capture());
        assertEquals(newRequester, requestCaptor.getValue().getUserID());
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
        request.setUserID(currentRequester);

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
        request.setUserID(currentRequester);

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
        request.setUserID(currentRequester);

        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(userRepository.findById(2L)).thenReturn(Optional.of(newRequester));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> requisitionService.changeRequester(1L, 2L));
        assertTrue(ex.getMessage().contains("New assigned user must be a requester from the same team"));
        verify(requestRepository, never()).save(any());
    }
}
