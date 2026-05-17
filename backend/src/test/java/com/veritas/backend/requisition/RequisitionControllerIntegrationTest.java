package com.veritas.backend.requisition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.veritas.backend.BaseDBIntegrationTest;
import com.veritas.backend.auth.service.JwtService;
import com.veritas.backend.budget.entity.InternalBudget;
import com.veritas.backend.project.entity.Project;
import com.veritas.backend.project.repository.ProjectRepository;
import com.veritas.backend.requisition.dto.RequisitionCreateDto;
import com.veritas.backend.requisition.dto.RequisitionItemCreateDto;
import com.veritas.backend.requisition.dto.RequisitionRejectDto;
import com.veritas.backend.requisition.entity.Priority;
import com.veritas.backend.requisition.entity.Request;
import com.veritas.backend.requisition.entity.RequestStatus;
import com.veritas.backend.requisition.repository.AttachmentRepository;
import com.veritas.backend.requisition.repository.RequestItemRepository;
import com.veritas.backend.requisition.repository.RequestRepository;
import com.veritas.backend.team.entity.Team;
import com.veritas.backend.team.repository.TeamRepository;
import com.veritas.backend.user.entity.User;
import com.veritas.backend.user.entity.UserRole;
import com.veritas.backend.user.repository.UserRepository;
import com.veritas.backend.workflow.entity.WorkflowComponent;
import com.veritas.backend.workflow.entity.WorkflowDefinition;
import com.veritas.backend.workflow.entity.WorkflowStep;
import com.veritas.backend.workflow.repository.WorkflowDefinitionRepository;
import com.veritas.backend.workflow.repository.WorkflowStepRepository;
import com.veritas.backend.workflow.repository.WorkflowTransitionRepository;
import com.veritas.backend.audit.entity.AuditLog;
import com.veritas.backend.audit.repository.AuditLogRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
//AI-GENERATED
@SpringBootTest
@AutoConfigureMockMvc
class RequisitionControllerIntegrationTest extends BaseDBIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private WorkflowDefinitionRepository workflowDefinitionRepository;
    @Autowired
    private WorkflowStepRepository workflowStepRepository;
    @Autowired
    private WorkflowTransitionRepository workflowTransitionRepository;
    @Autowired
    private RequestRepository requestRepository;
    @Autowired
    private RequestItemRepository requestItemRepository;
    @Autowired
    private AttachmentRepository attachmentRepository;
    @Autowired
    private AuditLogRepository auditLogRepository;

    private String requesterToken;
    private Long projectId;
    private Long workflowId;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAllInBatch();
        attachmentRepository.deleteAllInBatch();
        requestItemRepository.deleteAllInBatch();
        requestRepository.deleteAllInBatch();
        workflowTransitionRepository.deleteAllInBatch();
        workflowStepRepository.deleteAllInBatch();
        workflowDefinitionRepository.deleteAllInBatch();
        projectRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
        teamRepository.deleteAllInBatch();

        Team team = teamRepository.save(Team.builder()
                .name("Engineering")
                .description("Engineering Team")
                .isActive(true)
                .build());

        User requester = new User();
        requester.setEmail("req-integration@veritas.com");
        requester.setName("Integration Requester");
        requester.setPasswordHash("hashed");
        requester.setRole(UserRole.REQUESTER);
        requester.setIsActive(true);
        requester.setRequiresPasswordChange(false);
        requester.setTeam(team);
        requester = userRepository.save(requester);

        requesterToken = jwtService.generateAccessToken(requester);

        Project project = Project.builder()
                .name("Integration Project")
                .projectKey("INT")
                .team(team)
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2026, 12, 31))
                .internalBudget(InternalBudget.builder().budgetName("Test Budget").totalAmount(BigDecimal.valueOf(10000.00)).build())
                .requestCounter(0)
                .build();
        project = projectRepository.save(project);
        projectId = project.getId();

        WorkflowDefinition workflow = new WorkflowDefinition();
        workflow.setName("Standard Workflow");
        workflow.setVersion(1);
        workflow.setIsActive(true);
        workflow.setBpmnXml("<bpmn/>");
        workflow = workflowDefinitionRepository.save(workflow);
        workflowId = workflow.getId();

        WorkflowStep startStep = new WorkflowStep();
        startStep.setWorkflowDefinition(workflow);
        startStep.setWorkflowComponent(WorkflowComponent.START_EVENT);
        startStep.setName("Start");
        workflowStepRepository.save(startStep);
    }

    private RequisitionCreateDto validCreateDto() {
        return new RequisitionCreateDto(
                "Office Equipment",
                "Need new monitors",
                projectId, workflowId,
                Priority.HIGH,
                List.of(new RequisitionItemCreateDto("Monitor", 2, "pcs", "27-inch")));
    }


    @Test
    void RequisitionCreation_AsRequester_PersistsAndReturnsDto() throws Exception {
        mockMvc.perform(post("/api/v1/requisitions")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateDto())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requestName").value("Office Equipment"))
                .andExpect(jsonPath("$.requestKey").value("INT-1"))
                .andExpect(jsonPath("$.status").value("Start"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.projectName").value("Integration Project"))
                .andExpect(jsonPath("$.teamName").value("Engineering"))
                .andExpect(jsonPath("$.requesterName").value("Integration Requester"));

        assertEquals(1, requestRepository.count());
        assertEquals(1, requestItemRepository.count());
        assertEquals(1, projectRepository.findById(projectId).orElseThrow().getRequestCounter());
    }

    @Test
    void RequisitionCreation_MultipleItems_PersistsAllItems() throws Exception {
        RequisitionCreateDto dto = new RequisitionCreateDto(
                "Bulk Order", "Multiple items", projectId, workflowId, Priority.MEDIUM,
                List.of(
                        new RequisitionItemCreateDto("Laptop", 5, "pcs", "16-inch"),
                        new RequisitionItemCreateDto("Mouse", 10, "pcs", "Wireless"),
                        new RequisitionItemCreateDto("Cable", 20, "m", "USB-C")));

        mockMvc.perform(post("/api/v1/requisitions")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());

        assertEquals(1, requestRepository.count());
        assertEquals(3, requestItemRepository.count());
    }

    @Test
    void RequisitionCreation_IncrementsProjectCounter() throws Exception {
        mockMvc.perform(post("/api/v1/requisitions")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateDto())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requestKey").value("INT-1"));

        mockMvc.perform(post("/api/v1/requisitions")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateDto())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requestKey").value("INT-2"));

        assertEquals(2, requestRepository.count());
        assertEquals(2, projectRepository.findById(projectId).orElseThrow().getRequestCounter());
    }

    // ──────────────────────────────────────────────────────────────
    // Security tests
    // ──────────────────────────────────────────────────────────────

    @Test
    void RequisitionCreation_Unauthenticated_ReturnsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/requisitions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateDto())))
                .andExpect(status().isForbidden());
    }

    @Test
    void QuoteUpload_AsRequester_ReturnsOk() throws Exception {
        mockMvc.perform(post("/api/v1/requisitions")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateDto())))
                .andExpect(status().isCreated());

        Long requestId = requestRepository.findAll().get(0).getRequestID();

        MockMultipartFile file = new MockMultipartFile(
                "file", "quote.pdf", "application/pdf", "PDF data".getBytes());

        mockMvc.perform(multipart("/api/v1/requisitions/" + requestId + "/attachments")
                        .file(file)
                        .header("Authorization", "Bearer " + requesterToken))
                .andExpect(status().isOk())
                .andExpect(content().string("File quote.pdf uploaded for request " + requestId));
    }

    // ──────────────────────────────────────────────────────────────
    // Validation tests
    // ──────────────────────────────────────────────────────────────

    @Test
    void RequisitionCreation_BlankRequestName_ReturnsBadRequest() throws Exception {
        RequisitionCreateDto dto = new RequisitionCreateDto(
                "", "desc", projectId, workflowId, Priority.HIGH,
                List.of(new RequisitionItemCreateDto("Item", 1, "pcs", null)));

        mockMvc.perform(post("/api/v1/requisitions")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.requestName").value("Request name is required"));

        assertEquals(0, requestRepository.count());
    }

    @Test
    void RequisitionCreation_NullRequiredFields_ReturnsBadRequest() throws Exception {
        RequisitionCreateDto dto = new RequisitionCreateDto(
                "Valid Name", "desc", null, null, null,
                List.of(new RequisitionItemCreateDto("Item", 1, "pcs", null)));

        mockMvc.perform(post("/api/v1/requisitions")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());

        assertEquals(0, requestRepository.count());
    }

    @Test
    void RequisitionCreation_EmptyItemsList_ReturnsBadRequest() throws Exception {
        RequisitionCreateDto dto = new RequisitionCreateDto(
                "Valid Name", "desc", projectId, workflowId, Priority.HIGH,
                Collections.emptyList());

        mockMvc.perform(post("/api/v1/requisitions")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.items").value("At least one item is required"));

        assertEquals(0, requestRepository.count());
    }

    @Test
    void RequisitionCreation_InvalidItemFields_ReturnsBadRequest() throws Exception {
        RequisitionCreateDto dto = new RequisitionCreateDto(
                "Valid Name", "desc", projectId, workflowId, Priority.HIGH,
                List.of(new RequisitionItemCreateDto("", -1, "", null)));

        mockMvc.perform(post("/api/v1/requisitions")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());

        assertEquals(0, requestRepository.count());
    }

    //AI-Generated
    @Test
    void ApproveRequest_AlreadyFinished_ReturnsConflictStatus() throws Exception {
        // 1. Create a request and manually save it as FINISHED
        Request request = new Request();
        request.setRequestName("Finished Test");
        request.setState(RequestStatus.FINISHED);
        request = requestRepository.save(request);

        // 2. Try to hit the approve endpoint
        mockMvc.perform(post("/api/v1/requisitions/" + request.getRequestID() + "/approve")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON))
                // Asserts against WorkflowStateException mapping to HTTP 409 CONFLICT
                .andExpect(status().isConflict())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("is already finished and cannot be approved")));
    }

    //AI-Generated
    @Test
    void RejectRequest_AlreadyFinished_ReturnsConflictStatus() throws Exception {
        // 1. Create a request and manually save it as FINISHED
        Request request = new Request();
        request.setRequestName("Finished Test Rejection");
        request.setState(RequestStatus.FINISHED);
        request = requestRepository.save(request);

        RequisitionRejectDto rejectDto = new RequisitionRejectDto();
        rejectDto.setReason("Sending back to draft");

        // 2. Try to hit the reject endpoint
        mockMvc.perform(post("/api/v1/requisitions/" + request.getRequestID() + "/reject")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rejectDto)))
                .andExpect(status().isConflict())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("is already finished and cannot be rejected")));
    }

    //AI-Generated
    @Test
    void RejectRequest_NoHistoryExists_ReturnsConflictStatus() throws Exception {
        // 1. Create an active request but don't add any AuditLogs to the DB
        Request request = new Request();
        request.setRequestName("Orphan Step Test");
        request.setState(RequestStatus.ACTIVE);

        // Fetch the Start event we built in setUp() to simulate starting point
        WorkflowDefinition workflow = workflowDefinitionRepository.findAll().get(0);
        WorkflowStep startStep = workflowStepRepository.findAll().get(0);
        request.setWorkflowDefinitionID(workflow);
        request.setCurrentStepID(startStep);
        request = requestRepository.save(request);

        RequisitionRejectDto rejectDto = new RequisitionRejectDto();
        rejectDto.setReason("Nowhere to go");

        // 2. Trigger a reject (revert) which will fail targetStep generation
        mockMvc.perform(post("/api/v1/requisitions/" + request.getRequestID() + "/reject")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rejectDto)))
                .andExpect(status().isConflict())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("No valid step found in history to revert to")));
    }

    //AI-Generated
    @Test
    void RejectRequest_FromFirstStep_SetsStatusToDraft() throws Exception {
        WorkflowDefinition workflow = workflowDefinitionRepository.findAll().get(0);
        WorkflowStep startStep = workflowStepRepository.findAll().get(0);

        WorkflowStep stepOne = new WorkflowStep();
        stepOne.setWorkflowDefinition(workflow);
        stepOne.setWorkflowComponent(WorkflowComponent.STEP);
        stepOne.setName("Manager Review");
        stepOne = workflowStepRepository.save(stepOne);

        Request request = new Request();
        request.setRequestName("Draft Loopback Test");
        request.setState(RequestStatus.ACTIVE);
        request.setWorkflowDefinitionID(workflow);
        request.setCurrentStepID(stepOne);
        request = requestRepository.save(request);

        // 2. Seed an execution history log mapping: startStep -> stepOne via an "APPROVE"
        AuditLog log = new AuditLog().builder()
                .request(request)
                .previousStep(startStep)
                .newStep(stepOne)
                .action("APPROVE")
                .entryHash("mock-hash-123")
                .timestamp(java.time.LocalDateTime.now())
                .build();
        auditLogRepository.save(log);

        RequisitionRejectDto rejectDto = new RequisitionRejectDto();
        rejectDto.setReason("Needs complete rewrite");

        // 3. Reverting from Step 1 back to the START_EVENT
        mockMvc.perform(post("/api/v1/requisitions/" + request.getRequestID() + "/reject")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rejectDto)))
                .andExpect(status().isOk());

        // 4. Validate state machine updated fields correctly
        Request updatedRequest =
                requestRepository.findById(request.getRequestID()).orElseThrow();

        assertAll("Request state rollback verification",
                () -> assertEquals(RequestStatus.DRAFT, updatedRequest.getState(),
                        "The request status should have reverted to DRAFT"),
                () -> assertEquals(startStep.getId(), updatedRequest.getCurrentStepID().getId(),
                        "The current step ID should match the workflow's START_EVENT id")
        );
    }

}
