package com.veritas.backend.requisition;

import static com.veritas.backend.common.model.AuditActionConstants.APPROVE;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.veritas.backend.BaseDBIntegrationTest;
import com.veritas.backend.auth.service.JwtService;
import com.veritas.backend.budget.entity.InternalBudget;
import com.veritas.backend.budget.repository.InternalBudgetRepository;
import com.veritas.backend.department.entity.Department;
import com.veritas.backend.department.repository.DepartmentRepository;
import com.veritas.backend.integrations.currency.entity.Currency;
import com.veritas.backend.project.entity.Project;
import com.veritas.backend.project.repository.ProjectRepository;
import com.veritas.backend.requisition.dto.RequisitionCreateDto;
import com.veritas.backend.requisition.dto.RequisitionItemCreateDto;
import com.veritas.backend.requisition.dto.RequisitionRejectDto;
import com.veritas.backend.requisition.dto.RequisitionUpdateDto;
import com.veritas.backend.requisition.entity.*;
import com.veritas.backend.requisition.repository.AttachmentRepository;
import com.veritas.backend.requisition.repository.RequestItemRepository;
import com.veritas.backend.requisition.repository.RequestRepository;
import com.veritas.backend.requisition.repository.InvoiceRepository;
import com.veritas.backend.vendor.repository.QuoteLineItemRepository;
import com.veritas.backend.vendor.repository.QuoteRepository;
import com.veritas.backend.team.entity.Team;
import com.veritas.backend.team.repository.TeamRepository;
import com.veritas.backend.user.entity.User;
import com.veritas.backend.user.entity.UserRole;
import com.veritas.backend.user.repository.UserRepository;
import com.veritas.backend.workflow.entity.WorkflowComponent;
import com.veritas.backend.workflow.entity.WorkflowDefinition;
import com.veritas.backend.workflow.entity.WorkflowStep;
import com.veritas.backend.workflow.entity.WorkflowTransition;
import com.veritas.backend.workflow.repository.WorkflowDefinitionRepository;
import com.veritas.backend.workflow.repository.WorkflowStepRepository;
import com.veritas.backend.workflow.repository.WorkflowTransitionRepository;
import com.veritas.backend.workflow.repository.TransitionRuleRepository;
import com.veritas.backend.audit.entity.AuditLog;
import com.veritas.backend.audit.repository.AuditLogRepository;
import com.veritas.backend.vendor.repository.VendorRepository;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
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
    private JdbcTemplate jdbcTemplate;

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
    private TransitionRuleRepository transitionRuleRepository;
    @Autowired
    private RequestRepository requestRepository;
    @Autowired
    private RequestItemRepository requestItemRepository;
    @Autowired
    private AttachmentRepository attachmentRepository;
    @Autowired
    private AuditLogRepository auditLogRepository;
    @Autowired
    private QuoteLineItemRepository quoteLineItemRepository;
    @Autowired
    private QuoteRepository quoteRepository;
    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;
    @Autowired
    private InternalBudgetRepository internalBudgetRepository;
    @Autowired
    private VendorRepository vendorRepository;

    private String requesterToken;
    private String financeOfficerToken;
    private String procurementOfficerToken;
    private Long projectId;
    private Long workflowId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM attachments");
        jdbcTemplate.update("DELETE FROM invoices");
        jdbcTemplate.update("DELETE FROM vendor_evaluations");
        jdbcTemplate.update("DELETE FROM quote_line_items");
        jdbcTemplate.update("DELETE FROM quotes");
        quoteLineItemRepository.deleteAllInBatch();
        quoteRepository.deleteAllInBatch();
        vendorRepository.deleteAllInBatch();
        invoiceRepository.deleteAllInBatch();
        auditLogRepository.deleteAllInBatch();
        attachmentRepository.deleteAllInBatch();
        quoteLineItemRepository.deleteAllInBatch();
        quoteRepository.deleteAllInBatch();
        requestItemRepository.deleteAllInBatch();
        requestRepository.deleteAllInBatch();
        transitionRuleRepository.deleteAllInBatch();
        workflowTransitionRepository.deleteAllInBatch();
        workflowStepRepository.deleteAllInBatch();
        workflowDefinitionRepository.deleteAllInBatch();
        projectRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
        teamRepository.deleteAllInBatch();

        jdbcTemplate.update("UPDATE departments SET budget_id = NULL");
        internalBudgetRepository.deleteAllInBatch();
        departmentRepository.deleteAllInBatch();

        Department department = departmentRepository.save(Department.builder()
                .name("R&D")
                .build());

        Team team = teamRepository.save(Team.builder()
                .name("Engineering")
                .description("Engineering Team")
                .department(department)
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

        User financeOfficer = new User();
        financeOfficer.setEmail("finance-integration@veritas.com");
        financeOfficer.setName("Integration Finance Officer");
        financeOfficer.setPasswordHash("hashed");
        financeOfficer.setRole(UserRole.FINANCE_OFFICER);
        financeOfficer.setIsActive(true);
        financeOfficer.setRequiresPasswordChange(false);
        financeOfficer = userRepository.save(financeOfficer);

        User procurementOfficer = new User();
        procurementOfficer.setEmail("procurement-integration@veritas.com");
        procurementOfficer.setName("Integration Procurement Officer");
        procurementOfficer.setPasswordHash("hashed");
        procurementOfficer.setRole(UserRole.PROCUREMENT_OFFICER);
        procurementOfficer.setIsActive(true);
        procurementOfficer.setRequiresPasswordChange(false);
        procurementOfficer.setDepartment(department);
        procurementOfficer = userRepository.save(procurementOfficer);

        financeOfficerToken = jwtService.generateAccessToken(financeOfficer);
        procurementOfficerToken = jwtService.generateAccessToken(procurementOfficer);
        requesterToken = jwtService.generateAccessToken(requester);

        financeOfficerToken = jwtService.generateAccessToken(financeOfficer);

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
                List.of(new RequisitionItemCreateDto("Monitor", 2, RequestItemUnit.PIECES, "27-inch")));
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
                        new RequisitionItemCreateDto("Laptop", 5, RequestItemUnit.PIECES, "16-inch"),
                        new RequisitionItemCreateDto("Mouse", 10, RequestItemUnit.PIECES, "Wireless"),
                        new RequisitionItemCreateDto("Cable", 20, RequestItemUnit.KG, "USB-C")));

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
                .andExpect(content().string("\"File quote.pdf uploaded for request " + requestId + "\""));
    }

    // ──────────────────────────────────────────────────────────────
    // Validation tests
    // ──────────────────────────────────────────────────────────────

    @Test
    void RequisitionCreation_BlankRequestName_ReturnsBadRequest() throws Exception {
        RequisitionCreateDto dto = new RequisitionCreateDto(
                "", "desc", projectId, workflowId, Priority.HIGH,
                List.of(new RequisitionItemCreateDto("Item", 1, RequestItemUnit.PIECES, null)));

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
                List.of(new RequisitionItemCreateDto("Item", 1, RequestItemUnit.PIECES, null)));

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
                                List.of(new RequisitionItemCreateDto("", -1, null, null)));

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
    void RevertRequest_NoHistoryExists_ReturnsConflictStatus() throws Exception {
        // 1. Create an active request but don't add any AuditLogs to the DB
        Request request = new Request();
        request.setRequestName("Orphan Step Test");
        request.setState(RequestStatus.ACTIVE);

        // Fetch the Start event we built in setUp() to simulate starting point
        WorkflowDefinition workflow = workflowDefinitionRepository.findAll().get(0);
        WorkflowStep startStep = workflowStepRepository.findAll().get(0);
        request.setWorkflowDefinition(workflow);
        request.setCurrentStep(startStep);
        request = requestRepository.save(request);

        RequisitionRejectDto rejectDto = new RequisitionRejectDto();
        rejectDto.setReason("Nowhere to go");

        // 2. Trigger a reject (revert) which will fail targetStep generation
        mockMvc.perform(post("/api/v1/requisitions/" + request.getRequestID() + "/revert")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rejectDto)))
                .andExpect(status().isConflict())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("No valid step found in history to revert to")));
    }

    @Test
    void RevertRequest_InDraft_ReturnsConflictStatus() throws Exception {
        // 1. Create a request and manually save it as DRAFT
        Request request = new Request();
        request.setRequestName("Finished Test Rejection");
        request.setState(RequestStatus.DRAFT);
        request = requestRepository.save(request);

        RequisitionRejectDto rejectDto = new RequisitionRejectDto();
        rejectDto.setReason("Sending back to draft");

        // 2. Try to hit the revert endpoint
        mockMvc.perform(post("/api/v1/requisitions/" + request.getRequestID() + "/revert")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rejectDto)))
                .andExpect(status().isConflict())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("is in draft and cannot be reverted")));
    }

    @Test
    void RevertRequest_AlreadyFinished_ReturnsConflictStatus() throws Exception {
        // 1. Create a request and manually save it as FINISHED
        Request request = new Request();
        request.setRequestName("Finished Test Rejection");
        request.setState(RequestStatus.FINISHED);
        request = requestRepository.save(request);

        RequisitionRejectDto rejectDto = new RequisitionRejectDto();
        rejectDto.setReason("Sending back to draft");

        // 2. Try to hit the revert endpoint
        mockMvc.perform(post("/api/v1/requisitions/" + request.getRequestID() + "/revert")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rejectDto)))
                .andExpect(status().isConflict())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("is already finished and cannot be reverted")));
    }

    //AI-Generated
    @Test
    void RevertRequest_FromFirstStep_SetsStatusToDraft() throws Exception {
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
        request.setWorkflowDefinition(workflow);
        request.setCurrentStep(stepOne);
        request = requestRepository.save(request);

        // 2. Seed an execution history log mapping: startStep -> stepOne via an "APPROVE"
        AuditLog log = new AuditLog().builder()
                .request(request)
                .previousStep(startStep)
                .newStep(stepOne)
                .action(APPROVE)
                .entryHash("mock-hash-123")
                .timestamp(java.time.LocalDateTime.now())
                .build();
        auditLogRepository.save(log);

        RequisitionRejectDto rejectDto = new RequisitionRejectDto();
        rejectDto.setReason("Needs complete rewrite");

        // 3. Reverting from Step 1 back to the START_EVENT
        mockMvc.perform(post("/api/v1/requisitions/" + request.getRequestID() + "/revert")
                        .header("Authorization", "Bearer " + financeOfficerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rejectDto)))
                .andExpect(status().isOk());

        // 4. Validate state machine updated fields correctly
        Request updatedRequest =
                requestRepository.findById(request.getRequestID()).orElseThrow();

        assertAll("Request state rollback verification",
                () -> assertEquals(RequestStatus.DRAFT, updatedRequest.getState(),
                        "The request status should have reverted to DRAFT"),
                () -> assertEquals(startStep.getId(), updatedRequest.getCurrentStep().getId(),
                        "The current step ID should match the workflow's START_EVENT id")
        );
    }

    //AI-Generated
    @Test
    void RejectRequest_SoftDeletesRequestAndSavesReason() throws Exception {
        Project project = projectRepository.findAll().get(0);
        Request request = new Request();
        request.setRequestName("Soft Delete Test Rejection");
        request.setState(RequestStatus.ACTIVE);
        request.setProjectID(project);
        request = requestRepository.save(request);

        RequisitionRejectDto rejectDto = new RequisitionRejectDto();
        rejectDto.setReason("Item is obsolete");

        mockMvc.perform(post("/api/v1/requisitions/" + request.getRequestID() + "/reject")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rejectDto)))
                .andExpect(status().isOk());

        Request updatedRequest = requestRepository.findById(request.getRequestID()).orElseThrow();
        assertNotNull(updatedRequest.getDeletedAt());
        assertEquals("Item is obsolete", updatedRequest.getRejectionReason());
    }

    //AI-Generated
    @Test
    void ChangeRequester_ValidSameTeam_UpdatesRequesterAndReturnsOk() throws Exception {
        User secondRequester = new User();
        secondRequester.setEmail("req2-integration@veritas.com");
        secondRequester.setName("Second Requester");
        secondRequester.setPasswordHash("hashed");
        secondRequester.setRole(UserRole.REQUESTER);
        secondRequester.setIsActive(true);
        secondRequester.setRequiresPasswordChange(false);
        secondRequester.setTeam(userRepository.findByEmail("req-integration@veritas.com").orElseThrow().getTeam());
        secondRequester = userRepository.save(secondRequester);

        mockMvc.perform(post("/api/v1/requisitions")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateDto())))
                .andExpect(status().isCreated());

        Long requestId = requestRepository.findAll().getFirst().getRequestID();

        mockMvc.perform(patch("/api/v1/requisitions/" + requestId + "/requester-change")
                        .header("Authorization", "Bearer " + financeOfficerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondRequester.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requesterName").value("Second Requester"));
    }

    //AI-Generated
    @Test
    void ChangeRequester_RequestNotFound_ReturnsBadRequest() throws Exception {
        mockMvc.perform(patch("/api/v1/requisitions/999/requester-change")
                        .header("Authorization", "Bearer " + financeOfficerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(1L)))
                .andExpect(status().isBadRequest());
    }

    //AI-Generated
    @Test
    void ChangeRequester_NewRequesterFromDifferentTeam_ReturnsBadRequest() throws Exception {
        Team otherTeam = teamRepository.save(Team.builder()
                .name("Marketing")
                .description("Marketing Team")
                .isActive(true)
                .build());

        User outsider = new User();
        outsider.setEmail("outsider@veritas.com");
        outsider.setName("Outsider");
        outsider.setPasswordHash("hashed");
        outsider.setRole(UserRole.REQUESTER);
        outsider.setIsActive(true);
        outsider.setRequiresPasswordChange(false);
        outsider.setTeam(otherTeam);
        outsider = userRepository.save(outsider);

        mockMvc.perform(post("/api/v1/requisitions")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateDto())))
                .andExpect(status().isCreated());

        Long requestId = requestRepository.findAll().getFirst().getRequestID();

        mockMvc.perform(patch("/api/v1/requisitions/" + requestId + "/requester-change")
                        .header("Authorization", "Bearer " + financeOfficerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(outsider.getId())))
                .andExpect(status().isBadRequest());
    }

    //AI-Generated
    @Test
    void ChangeRequester_Unauthenticated_ReturnsForbidden() throws Exception {
        mockMvc.perform(patch("/api/v1/requisitions/1/requester-change")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(2L)))
                .andExpect(status().isForbidden());
    }
    @Test
    void ProcessPayment_AsFinanceOfficerWithValidRequestAndInvoice_ReturnsNoContentAndUpdatesBudget() throws Exception {
        // Arrange
        Project project = projectRepository.findAll().get(0);
        Long budgetId = project.getInternalBudget().getId();
        InternalBudget budget = internalBudgetRepository.findById(budgetId).orElseThrow();
        budget.setCommittedSpend(new BigDecimal("500.00"));
        budget.setActualSpend(new BigDecimal("1000.00"));
        budget = internalBudgetRepository.save(budget);

        Request request = new Request();
        request.setRequestName("Office Furniture");
        request.setState(RequestStatus.ACTIVE);
        request.setProject(project);
        request.setBudget(budget);
        request = requestRepository.save(request);

        Invoice invoice = new Invoice();
        invoice.setRequest(request);
        invoice.setTotalAmount(new BigDecimal("300.00"));
        invoice.setCurrency(Currency.EUR);
        invoice.setIsPaid(false);
        invoice = invoiceRepository.save(invoice);

        // Act & Assert
        mockMvc.perform(post("/api/v1/requisitions/" + request.getRequestID() + "/pay")
                        .header("Authorization", "Bearer " + financeOfficerToken))
                .andExpect(status().isNoContent());

        // Verify Database state
        Invoice updatedInvoice = invoiceRepository.findById(invoice.getInvoiceId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertTrue(updatedInvoice.getIsPaid());

        InternalBudget updatedBudget = internalBudgetRepository.findById(budget.getId()).orElseThrow();
        // actualSpend: 1000.00 + 300.00 = 1300.00
        assertEquals(0, new BigDecimal("1300.00").compareTo(updatedBudget.getActualSpend()));
        // committedSpend: 500.00 - 500.00 = 0.00 (since requestCommittedSpent was 500.00)
        assertEquals(0, new BigDecimal("0.00").compareTo(updatedBudget.getCommittedSpend()));
    }

    @Test
    void ProcessPayment_AsFinanceOfficerWithMissingInvoice_ReturnsNotFound() throws Exception {
        // Arrange
        Project project = projectRepository.findAll().get(0);
        Request request = new Request();
        request.setRequestName("Office Furniture No Invoice");
        request.setState(RequestStatus.ACTIVE);
        request.setProject(project);
        request = requestRepository.save(request);

        // Act & Assert
        mockMvc.perform(post("/api/v1/requisitions/" + request.getRequestID() + "/pay")
                        .header("Authorization", "Bearer " + financeOfficerToken))
                .andExpect(status().isNotFound())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Invoice not found for request with id: " + request.getRequestID())));
    }

    @Test
    void ProcessPayment_AsFinanceOfficerWithNonExistentRequest_ReturnsNotFound() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/requisitions/99999/pay")
                        .header("Authorization", "Bearer " + financeOfficerToken))
                .andExpect(status().isNotFound())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Request not found with id: 99999")));
    }

    @Test
    void ProcessPayment_AsRequester_ReturnsForbidden() throws Exception {
        // Arrange
        Project project = projectRepository.findAll().get(0);
        Request request = new Request();
        request.setRequestName("Office Furniture");
        request.setState(RequestStatus.ACTIVE);
        request.setProject(project);
        request = requestRepository.save(request);

        // Act & Assert
        mockMvc.perform(post("/api/v1/requisitions/" + request.getRequestID() + "/pay")
                        .header("Authorization", "Bearer " + requesterToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void ProcessPayment_TransitionRequestToFinished() throws Exception {
        // Arrange
        Project project = projectRepository.findAll().get(0);
        Long budgetId = project.getInternalBudget().getId();
        InternalBudget budget = internalBudgetRepository.findById(budgetId).orElseThrow();
        budget.setCommittedSpend(new BigDecimal("500.00"));
        budget.setActualSpend(new BigDecimal("1000.00"));
        budget = internalBudgetRepository.save(budget);

        Request request = new Request();
        request.setRequestName("Office Supplies Finished");
        request.setState(RequestStatus.ACTIVE);
        request.setProject(project);
        request.setBudget(budget);
        request = requestRepository.save(request);

        Invoice invoice = new Invoice();
        invoice.setRequest(request);
        invoice.setTotalAmount(new BigDecimal("200.00"));
        invoice.setCurrency(Currency.EUR);
        invoice.setIsPaid(false);
        invoice = invoiceRepository.save(invoice);

        // Act
        mockMvc.perform(post("/api/v1/requisitions/" + request.getRequestID() + "/pay")
                        .header("Authorization", "Bearer " + financeOfficerToken))
                .andExpect(status().isNoContent());

        // Assert Request state is FINISHED
        Request updatedRequest = requestRepository.findById(request.getRequestID()).orElseThrow();
        assertEquals(RequestStatus.FINISHED, updatedRequest.getState());
    }

    @Test
    void RejectRequest_AlreadyPaid_ReturnsConflictStatus() throws Exception {
        // Arrange
        Project project = projectRepository.findAll().get(0);
        Request request = new Request();
        request.setRequestName("Reject Paid Request");
        request.setState(RequestStatus.ACTIVE);
        request.setProject(project);
        request = requestRepository.save(request);

        Invoice invoice = new Invoice();
        invoice.setRequest(request);
        invoice.setTotalAmount(new BigDecimal("100.00"));
        invoice.setCurrency(Currency.EUR);
        invoice.setIsPaid(true); // Manually seed as already paid
        invoice = invoiceRepository.save(invoice);

        RequisitionRejectDto rejectDto = new RequisitionRejectDto();
        rejectDto.setReason("Rejecting paid order");

        // Act & Assert
        mockMvc.perform(post("/api/v1/requisitions/" + request.getRequestID() + "/reject")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rejectDto)))
                .andExpect(status().isConflict())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("has already been paid and cannot be rejected")));
    }

    @Test
    void RevertRequest_AlreadyPaid_ReturnsConflictStatus() throws Exception {
        // Arrange
        Project project = projectRepository.findAll().get(0);
        Request request = new Request();
        request.setRequestName("Revert Paid Request");
        request.setState(RequestStatus.ACTIVE);
        request.setProjectID(project);
        request = requestRepository.save(request);

        Invoice invoice = new Invoice();
        invoice.setRequest(request);
        invoice.setTotalAmount(new BigDecimal("100.00"));
        invoice.setIsPaid(true); // Manually seed as already paid
        invoice = invoiceRepository.save(invoice);

        RequisitionRejectDto rejectDto = new RequisitionRejectDto();
        rejectDto.setReason("Reverting paid order");

        // Act & Assert
        mockMvc.perform(post("/api/v1/requisitions/" + request.getRequestID() + "/revert")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rejectDto)))
                .andExpect(status().isConflict())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("has already been paid and cannot be reverted")));
    }

    @Test
    void ApproveRequest_AlreadyPaid_ReturnsConflictStatus() throws Exception {
        // Arrange
        Project project = projectRepository.findAll().get(0);
        Request request = new Request();
        request.setRequestName("Approve Paid Request");
        request.setState(RequestStatus.ACTIVE);
        request.setProject(project);
        request = requestRepository.save(request);

        Invoice invoice = new Invoice();
        invoice.setRequest(request);
        invoice.setTotalAmount(new BigDecimal("100.00"));
        invoice.setCurrency(Currency.EUR);
        invoice.setIsPaid(true); // Manually seed as already paid
        invoice = invoiceRepository.save(invoice);

        // Act & Assert
        mockMvc.perform(post("/api/v1/requisitions/" + request.getRequestID() + "/approve")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("has already been paid and cannot be approved")));
    }

    @Test
    void DeleteAttachment_ExistingAttachment_ReturnsNoContentAndDeletesAttachment() throws Exception {
        mockMvc.perform(post("/api/v1/requisitions")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateDto())))
                .andExpect(status().isCreated());

        Long requestId = requestRepository.findAll().getFirst().getRequestID();

        Path tempFile = Files.createTempFile("attachment-test", ".pdf");

        Attachment attachment = new Attachment();
        attachment.setRequest(requestRepository.findById(requestId).orElseThrow());
        attachment.setFileName("quote.pdf");
        attachment.setFileType("application/pdf");
        attachment.setFileSize(100L);
        attachment.setStoragePath(tempFile.toString());
        attachment = attachmentRepository.save(attachment);

        mockMvc.perform(delete("/api/v1/requisitions/attachments/" + attachment.getAttachmentId())
                        .header("Authorization", "Bearer " + requesterToken))
                .andExpect(status().isNoContent());

        assertEquals(0, attachmentRepository.count());
        assertFalse(Files.exists(tempFile));
    }

    @Test
    void DeleteAttachment_NonExistingAttachment_ReturnsNotFound() throws Exception {
        mockMvc.perform(delete("/api/v1/requisitions/attachments/99999")
                        .header("Authorization", "Bearer " + requesterToken))
                .andExpect(status().isNotFound())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "Attachment not found with id: 99999")));
    }

    @Test
    void DeleteAttachment_Unauthenticated_ReturnsForbidden() throws Exception {
        mockMvc.perform(delete("/api/v1/requisitions/attachments/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void DeleteAttachment_AsRequester_OnOwnRequest_ReturnsNoContent() throws Exception {
        mockMvc.perform(post("/api/v1/requisitions")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateDto())))
                .andExpect(status().isCreated());

        Long requestId = requestRepository.findAll().getFirst().getRequestID();
        Path tempFile = Files.createTempFile("attachment-own-request", ".pdf");

        Attachment attachment = new Attachment();
        attachment.setRequest(requestRepository.findById(requestId).orElseThrow());
        attachment.setFileName("own-request.pdf");
        attachment.setFileType("application/pdf");
        attachment.setFileSize(100L);
        attachment.setStoragePath(tempFile.toString());
        attachment = attachmentRepository.save(attachment);

        mockMvc.perform(delete("/api/v1/requisitions/attachments/" + attachment.getAttachmentId())
                        .header("Authorization", "Bearer " + requesterToken))
                .andExpect(status().isNoContent());

        assertEquals(0, attachmentRepository.count());
        assertFalse(Files.exists(tempFile));
    }

    @Test
    void DeleteAttachment_AsRequester_OnAnotherRequestersRequest_ReturnsForbidden() throws Exception {
        User otherRequester = new User();
        otherRequester.setEmail("other-req@veritas.com");
        otherRequester.setName("Other Requester");
        otherRequester.setPasswordHash("hashed");
        otherRequester.setRole(UserRole.REQUESTER);
        otherRequester.setIsActive(true);
        otherRequester.setRequiresPasswordChange(false);
        otherRequester.setTeam(userRepository.findAll().getFirst().getTeam());
        otherRequester = userRepository.save(otherRequester);
        String otherRequesterToken = jwtService.generateAccessToken(otherRequester);

        mockMvc.perform(post("/api/v1/requisitions")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateDto())))
                .andExpect(status().isCreated());

        Long requestId = requestRepository.findAll().getFirst().getRequestID();
        Path tempFile = Files.createTempFile("attachment-other-request", ".pdf");

        Attachment attachment = new Attachment();
        attachment.setRequest(requestRepository.findById(requestId).orElseThrow());
        attachment.setFileName("other-request.pdf");
        attachment.setFileType("application/pdf");
        attachment.setFileSize(100L);
        attachment.setStoragePath(tempFile.toString());
        attachment = attachmentRepository.save(attachment);

        mockMvc.perform(delete("/api/v1/requisitions/attachments/" + attachment.getAttachmentId())
                        .header("Authorization", "Bearer " + otherRequesterToken))
                .andExpect(status().isForbidden());

        assertEquals(1, attachmentRepository.count());
        assertTrue(Files.exists(tempFile));

        Files.deleteIfExists(tempFile);
    }

    @Test
    void DeleteAttachment_AsProcurementOfficer_SameDepartment_ReturnsNoContent() throws Exception {
        mockMvc.perform(post("/api/v1/requisitions")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateDto())))
                .andExpect(status().isCreated());

        Long requestId = requestRepository.findAll().getFirst().getRequestID();
        Path tempFile = Files.createTempFile("proc-same-dept", ".pdf");

        Attachment attachment = new Attachment();
        attachment.setRequest(requestRepository.findById(requestId).orElseThrow());
        attachment.setFileName("proc-same-dept.pdf");
        attachment.setFileType("application/pdf");
        attachment.setFileSize(100L);
        attachment.setStoragePath(tempFile.toString());
        attachment = attachmentRepository.save(attachment);

        mockMvc.perform(delete("/api/v1/requisitions/attachments/" + attachment.getAttachmentId())
                        .header("Authorization", "Bearer " + procurementOfficerToken))
                .andExpect(status().isNoContent());

        assertEquals(0, attachmentRepository.count());
        assertFalse(Files.exists(tempFile));
    }

    @Test
    void DeleteAttachment_AsProcurementOfficer_DifferentDepartment_ReturnsForbidden() throws Exception {
        Department department = departmentRepository.save(Department.builder()
                .name("Marketing")
                .build());

        User otherProcurementOfficer = new User();
        otherProcurementOfficer.setEmail("other-procurement@veritas.com");
        otherProcurementOfficer.setName("Integration Procurement Officer");
        otherProcurementOfficer.setPasswordHash("hashed");
        otherProcurementOfficer.setRole(UserRole.PROCUREMENT_OFFICER);
        otherProcurementOfficer.setIsActive(true);
        otherProcurementOfficer.setRequiresPasswordChange(false);
        otherProcurementOfficer.setDepartment(department);
        otherProcurementOfficer = userRepository.save(otherProcurementOfficer);
        String otherProcurementOfficerToken = jwtService.generateAccessToken(otherProcurementOfficer);

        mockMvc.perform(post("/api/v1/requisitions")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateDto())))
                .andExpect(status().isCreated());

        Long requestId = requestRepository.findAll().getFirst().getRequestID();
        Path tempFile = Files.createTempFile("proc-diff-dept", ".pdf");

        Attachment attachment = new Attachment();
        attachment.setRequest(requestRepository.findById(requestId).orElseThrow());
        attachment.setFileName("proc-diff-dept.pdf");
        attachment.setFileType("application/pdf");
        attachment.setFileSize(100L);
        attachment.setStoragePath(tempFile.toString());
        attachment = attachmentRepository.save(attachment);

        mockMvc.perform(delete("/api/v1/requisitions/attachments/" + attachment.getAttachmentId())
                        .header("Authorization", "Bearer " + otherProcurementOfficerToken))
                .andExpect(status().isForbidden());

        assertEquals(1, attachmentRepository.count());
        assertTrue(Files.exists(tempFile));

        Files.deleteIfExists(tempFile);
    }
    //AI-GENERATED

    @Test
    void ApproveRequest_NextStepIsAutomatedApproval_AutomaticallyApprovesAndAdvances() throws Exception {
        // Arrange
        WorkflowDefinition workflow = workflowDefinitionRepository.findAll().get(0);
        WorkflowStep startStep = workflowStepRepository.findAll().get(0);

        WorkflowStep stepOne = new WorkflowStep();
        stepOne.setWorkflowDefinition(workflow);
        stepOne.setWorkflowComponent(WorkflowComponent.STEP);
        stepOne.setName("Finance Review");
        stepOne.setRole(UserRole.FINANCE_OFFICER);
        stepOne = workflowStepRepository.save(stepOne);

        WorkflowStep automatedStep = new WorkflowStep();
        automatedStep.setWorkflowDefinition(workflow);
        automatedStep.setWorkflowComponent(WorkflowComponent.STEP);
        automatedStep.setName("Automated Administrator Step");
        automatedStep.setRole(UserRole.ADMINISTRATOR);
        automatedStep.setIsAutomatedApproval(true);
        automatedStep = workflowStepRepository.save(automatedStep);

        WorkflowStep endStep = new WorkflowStep();
        endStep.setWorkflowDefinition(workflow);
        endStep.setWorkflowComponent(WorkflowComponent.END_EVENT);
        endStep.setName("End");
        endStep = workflowStepRepository.save(endStep);

        // Transitions: startStep -> stepOne -> automatedStep -> endStep
        WorkflowTransition startToStepOne = new WorkflowTransition();
        startToStepOne.setFromStep(startStep);
        startToStepOne.setToStep(stepOne);
        workflowTransitionRepository.save(startToStepOne);

        WorkflowTransition stepOneToAutomated = new WorkflowTransition();
        stepOneToAutomated.setFromStep(stepOne);
        stepOneToAutomated.setToStep(automatedStep);
        workflowTransitionRepository.save(stepOneToAutomated);

        WorkflowTransition automatedToEnd = new WorkflowTransition();
        automatedToEnd.setFromStep(automatedStep);
        automatedToEnd.setToStep(endStep);
        workflowTransitionRepository.save(automatedToEnd);

        Project project = projectRepository.findAll().get(0);
        Request request = new Request();
        request.setRequestName("Automated Approval Requisition");
        request.setState(RequestStatus.ACTIVE);
        request.setProject(project);
        request.setWorkflowDefinition(workflow);
        request.setCurrentStep(stepOne);
        request = requestRepository.save(request);

        // Seed AuditLog for entering stepOne
        AuditLog log = AuditLog.builder()
                .request(request)
                .previousStep(startStep)
                .newStep(stepOne)
                .action("APPROVE")
                .entryHash("hash-abc")
                .timestamp(java.time.LocalDateTime.now())
                .build();
        auditLogRepository.save(log);

        // Act: Approve as Finance Officer at stepOne
        mockMvc.perform(post("/api/v1/requisitions/" + request.getRequestID() + "/approve")
                        .header("Authorization", "Bearer " + financeOfficerToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Assert: Request should have automatically skipped the automatedStep and advanced directly to endStep, changing state to FINISHED
        Request updatedRequest = requestRepository.findById(request.getRequestID()).orElseThrow();
        assertEquals(RequestStatus.FINISHED, updatedRequest.getState());
        assertEquals(endStep.getId(), updatedRequest.getCurrentStep().getId());
    }

    // AI-GENERATED
    @Test
    void DeleteInvoice_AsProcurementOfficer_ReturnsNoContentAndDeletesInvoice() throws Exception {
            Project project = projectRepository.findAll().get(0);
            Request request = new Request();
            request.setRequestName("Delete Invoice Test");
            request.setState(RequestStatus.ACTIVE);
            request.setProject(project);
            request.setTeam(teamRepository.findAll().get(0));
            request = requestRepository.save(request);

            Invoice invoice = new Invoice();
            invoice.setRequest(request);
            invoice.setTotalAmount(new BigDecimal("100.00"));
            invoice.setCurrency(Currency.EUR);
            invoice.setIsPaid(false);
            invoice = invoiceRepository.save(invoice);
            request.setInvoice(invoice);
            requestRepository.save(request);

            Path tempFile = Files.createTempFile("invoice-attachment", ".pdf");
            Attachment attachment = new Attachment();
            attachment.setRequest(request);
            attachment.setFileName("invoice.pdf");
            attachment.setFileType("application/pdf");
            attachment.setFileSize(100L);
            attachment.setStoragePath(tempFile.toString());
            attachment.setInvoice(invoice);
            attachmentRepository.save(attachment);

            mockMvc.perform(delete("/api/v1/requisitions/" + request.getRequestID() + "/invoice")
                            .header("Authorization", "Bearer " + procurementOfficerToken))
                            .andExpect(status().isNoContent());

            assertEquals(0, invoiceRepository.count());
            assertEquals(0, attachmentRepository.count());
            assertFalse(Files.exists(tempFile));
    }

    @Test
    void DeleteInvoice_AsRequester_ReturnsForbidden() throws Exception {
            Project project = projectRepository.findAll().get(0);
            Request request = new Request();
            request.setRequestName("Delete Invoice Forbidden");
            request.setState(RequestStatus.ACTIVE);
            request.setProject(project);
            request.setTeam(teamRepository.findAll().get(0));
            request = requestRepository.save(request);

            mockMvc.perform(delete("/api/v1/requisitions/" + request.getRequestID() + "/invoice")
                            .header("Authorization", "Bearer " + requesterToken))
                            .andExpect(status().isForbidden());
    }

    @Test
    void DeleteInvoice_PaidInvoice_ReturnsConflict() throws Exception {
            Project project = projectRepository.findAll().get(0);
            Request request = new Request();
            request.setRequestName("Delete Paid Invoice");
            request.setState(RequestStatus.ACTIVE);
            request.setProject(project);
            request.setTeam(teamRepository.findAll().get(0));
            request = requestRepository.save(request);

            Invoice invoice = new Invoice();
            invoice.setRequest(request);
            invoice.setTotalAmount(new BigDecimal("100.00"));
            invoice.setCurrency(Currency.EUR);
            invoice.setIsPaid(true);
            invoice = invoiceRepository.save(invoice);
            request.setInvoice(invoice);
            requestRepository.save(request);

            mockMvc.perform(delete("/api/v1/requisitions/" + request.getRequestID() + "/invoice")
                            .header("Authorization", "Bearer " + procurementOfficerToken))
                            .andExpect(status().isConflict())
                            .andExpect(content().string(org.hamcrest.Matchers.containsString(
                                            "Cannot delete an invoice that has already been paid.")));
    }
    @Test
    void RevertRequest_WithRevisionRequired_SetsRevisionRequiredFlag() throws Exception {
        WorkflowDefinition workflow = workflowDefinitionRepository.findAll().get(0);
        WorkflowStep startStep = workflowStepRepository.findAll().get(0);

        WorkflowStep stepOne = new WorkflowStep();
        stepOne.setWorkflowDefinition(workflow);
        stepOne.setWorkflowComponent(WorkflowComponent.STEP);
        stepOne.setName("Manager Review");
        stepOne = workflowStepRepository.save(stepOne);

        Request request = new Request();
        request.setRequestName("Revision Required Test");
        request.setState(RequestStatus.ACTIVE);
        request.setWorkflowDefinitionID(workflow);
        request.setCurrentStepID(stepOne);
        request = requestRepository.save(request);

        AuditLog log = new AuditLog().builder()
                .request(request)
                .previousStep(startStep)
                .newStep(stepOne)
                .action(APPROVE)
                .entryHash("mock-hash-revision")
                .timestamp(java.time.LocalDateTime.now())
                .build();
        auditLogRepository.save(log);

        RequisitionRejectDto rejectDto = new RequisitionRejectDto();
        rejectDto.setReason("Needs complete rewrite");
        rejectDto.setRevisionRequired(true);

        mockMvc.perform(post("/api/v1/requisitions/" + request.getRequestID() + "/revert")
                        .header("Authorization", "Bearer " + financeOfficerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rejectDto)))
                .andExpect(status().isOk());

        Request updatedRequest = requestRepository.findById(request.getRequestID()).orElseThrow();
        assertEquals(RequestStatus.DRAFT, updatedRequest.getState());
        assertTrue(updatedRequest.getRevisionRequired());
    }

    @Test
    void UpdateAndSubmitRequest_WithRevisionRequired_AllowsEditAndResetsFlag() throws Exception {
        WorkflowDefinition workflow = workflowDefinitionRepository.findAll().get(0);
        WorkflowStep startStep = workflowStepRepository.findAll().get(0);

        Project project = projectRepository.findAll().get(0);
        User owner = userRepository.findByEmail("req-integration@veritas.com").orElseThrow();

        Request request = new Request();
        request.setRequestName("Original Name");
        request.setState(RequestStatus.DRAFT);
        request.setWorkflowDefinitionID(workflow);
        request.setCurrentStepID(startStep);
        request.setProjectID(project);
        request.setItems(new java.util.ArrayList<>());
        request.setRevisionRequired(true);
        request.setUserID(owner); // Owner
        request = requestRepository.save(request);

        RequisitionUpdateDto updateDto = new RequisitionUpdateDto(
                "Updated Name", "Desc", projectId, workflowId, Priority.HIGH, List.of(new RequisitionItemCreateDto("Item A", 1, "pcs", "note"))
        );

        // Edit request (should be allowed even though it needs revision)
        mockMvc.perform(patch("/api/v1/requisitions/" + request.getRequestID())
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestName").value("Updated Name"));

        // Submit request (should clear revisionRequired)
        mockMvc.perform(post("/api/v1/requisitions/" + request.getRequestID() + "/submit")
                        .header("Authorization", "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        Request updatedRequest = requestRepository.findById(request.getRequestID()).orElseThrow();
        assertEquals(RequestStatus.ACTIVE, updatedRequest.getState());
        assertFalse(updatedRequest.getRevisionRequired());
    }

}

