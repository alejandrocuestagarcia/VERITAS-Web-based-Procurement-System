package com.veritas.backend.workflow.service;

import com.veritas.backend.audit.entity.AuditLog;
import com.veritas.backend.audit.repository.AuditLogRepository;
import com.veritas.backend.audit.service.impl.AuditServiceImpl;
import com.veritas.backend.budget.entity.InternalBudget;
import com.veritas.backend.budget.entity.BudgetType;
import com.veritas.backend.requisition.entity.Attachment;
import com.veritas.backend.requisition.entity.Request;
import com.veritas.backend.vendor.entity.Vendor;
import com.veritas.backend.vendor.repository.QuoteRepository;
import com.veritas.backend.user.entity.User;
import com.veritas.backend.user.entity.UserRole;
import com.veritas.backend.user.repository.UserRepository;
import com.veritas.backend.vendor.entity.Quote;
import com.veritas.backend.workflow.entity.TransitionRule;
import com.veritas.backend.workflow.entity.WorkflowComponent;
import com.veritas.backend.workflow.entity.WorkflowDefinition;
import com.veritas.backend.workflow.entity.WorkflowStep;
import com.veritas.backend.workflow.entity.WorkflowTransition;
import com.veritas.backend.workflow.mapper.WorkflowMapper;
import com.veritas.backend.workflow.repository.TransitionRuleRepository;
import com.veritas.backend.workflow.repository.WorkflowDefinitionRepository;
import com.veritas.backend.workflow.repository.WorkflowStepRepository;
import com.veritas.backend.workflow.repository.WorkflowTransitionRepository;
import com.veritas.backend.workflow.service.impl.WorkflowEngineServiceImpl;
import com.veritas.backend.common.exception.WorkflowStateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.veritas.backend.requisition.entity.RequestStatus;
import com.veritas.backend.common.model.AuditActionConstants;
import com.veritas.backend.team.entity.Team;
import com.veritas.backend.department.entity.Department;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// AI-GENERATED
@ExtendWith(MockitoExtension.class)
class WorkflowEngineServiceUnitTest {

    @Mock
    private WorkflowDefinitionRepository workflowDefinitionRepository;
    @Mock
    private WorkflowStepRepository workflowStepRepository;
    @Mock
    private WorkflowTransitionRepository workflowTransitionRepository;
    @Mock
    private TransitionRuleRepository transitionRuleRepository;
    @Mock
    private QuoteRepository quoteRepository;
    @Mock
    private WorkflowMapper workflowMapper;
    @Mock
    private AuditServiceImpl auditService;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private WorkflowEngineServiceImpl workflowEngineService;

    private User testActor;
    private Request testRequest;
    private WorkflowStep currentStep;
    private WorkflowStep nextStep;
    private WorkflowTransition testTransition;

    @BeforeEach
    void setUp() {
        testActor = new User();
        testActor.setId(1L);
        testActor.setRole(UserRole.REQUESTER);

        currentStep = new WorkflowStep();
        currentStep.setWorkflowComponent(WorkflowComponent.START_EVENT);
        currentStep.setRole(UserRole.REQUESTER);
        currentStep.setName("Start");

        nextStep = new WorkflowStep();
        nextStep.setWorkflowComponent(WorkflowComponent.END_EVENT);
        nextStep.setName("End");

        testRequest = new Request();
        testRequest.setCurrentStep(currentStep);
        testRequest.setRequestID(1L);
        testRequest.setAttachments(new ArrayList<>());

        testTransition = new WorkflowTransition();
        testTransition.setFromStep(currentStep);
        testTransition.setToStep(nextStep);
    }

    @Test
    void moveToNextStep_ValidTransitionWithNoRulesAndNoBudget_Success() {
        when(workflowTransitionRepository.findByFromStep(currentStep)).thenReturn(List.of(testTransition));
        when(transitionRuleRepository.findByTransition(testTransition)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> workflowEngineService.moveToNextStep(testRequest, testActor, null));

    }

    @Test
    void moveToNextStep_BudgetExceeded_ThrowsException() {
        when(workflowTransitionRepository.findByFromStep(currentStep)).thenReturn(List.of(testTransition));

        InternalBudget budget = new InternalBudget();
        budget.setTotalAmount(new BigDecimal("1000"));
        budget.setSafetyBuffer(new BigDecimal("10")); // 10% safety buffer meaning 900 limit
        budget.setActualSpend(new BigDecimal("800"));
        budget.setCommittedSpend(new BigDecimal("150")); // Total 950 > 900
        budget.setBudgetName("Test Request");
        testRequest.setBudget(budget);

        WorkflowStateException ex = assertThrows(WorkflowStateException.class,
                () -> workflowEngineService.moveToNextStep(testRequest, testActor, null));

        assertEquals("Budget of : Test Request exhausted including safety buffer.", ex.getMessage());
    }

    //AI-GENERATED
    @Test
    void moveToNextStep_ProjectBudgetExceeded_ThrowsException() {
        when(workflowTransitionRepository.findByFromStep(currentStep)).thenReturn(List.of(testTransition));

        InternalBudget projectBudget = InternalBudget.builder()
                .budgetType(BudgetType.PROJECT)
                .budgetName("Project Budget")
                .totalAmount(new BigDecimal("1000"))
                .safetyBuffer(new BigDecimal("10")) // 900 limit
                .actualSpend(new BigDecimal("800"))
                .committedSpend(new BigDecimal("150")) // 950 > 900
                .build();

        InternalBudget reqBudget = InternalBudget.builder()
                .budgetType(BudgetType.REQUEST)
                .budgetName("Request Budget")
                .parentBudget(projectBudget)
                .build();

        testRequest.setBudget(reqBudget);

        WorkflowStateException ex = assertThrows(WorkflowStateException.class,
                () -> workflowEngineService.moveToNextStep(testRequest, testActor, null));

        assertEquals("Budget of : Project Budget exhausted including safety buffer.", ex.getMessage());
    }

    //AI-GENERATED
    @Test
    void moveToNextStep_DepartmentBudgetExceeded_ThrowsException() {
        when(workflowTransitionRepository.findByFromStep(currentStep)).thenReturn(List.of(testTransition));

        InternalBudget deptBudget = InternalBudget.builder()
                .budgetType(BudgetType.DEPARTMENT)
                .budgetName("Department Budget")
                .totalAmount(new BigDecimal("1000"))
                .safetyBuffer(new BigDecimal("10")) // 900 limit
                .actualSpend(new BigDecimal("800"))
                .committedSpend(new BigDecimal("150")) // 950 > 900
                .build();

        InternalBudget projectBudget = InternalBudget.builder()
                .budgetType(BudgetType.PROJECT)
                .budgetName("Project Budget")
                .totalAmount(new BigDecimal("1000"))
                .safetyBuffer(new BigDecimal("10"))
                .actualSpend(new BigDecimal("500"))
                .committedSpend(new BigDecimal("100")) // 600 <= 900
                .parentBudget(deptBudget)
                .build();

        InternalBudget reqBudget = InternalBudget.builder()
                .budgetType(BudgetType.REQUEST)
                .budgetName("Request Budget")
                .parentBudget(projectBudget)
                .build();

        testRequest.setBudget(reqBudget);

        WorkflowStateException ex = assertThrows(WorkflowStateException.class,
                () -> workflowEngineService.moveToNextStep(testRequest, testActor, null));

        assertEquals("Budget of : Department Budget exhausted including safety buffer.", ex.getMessage());
    }

    //AI-GENERATED
    @Test
    void moveToNextStep_GlobalBudgetExceeded_ThrowsException() {
        when(workflowTransitionRepository.findByFromStep(currentStep)).thenReturn(List.of(testTransition));

        InternalBudget globalBudget = InternalBudget.builder()
                .budgetType(BudgetType.GLOBAL)
                .budgetName("Global Budget")
                .totalAmount(new BigDecimal("1000"))
                .safetyBuffer(new BigDecimal("10")) // 900 limit
                .actualSpend(new BigDecimal("800"))
                .committedSpend(new BigDecimal("150")) // 950 > 900
                .build();

        InternalBudget deptBudget = InternalBudget.builder()
                .budgetType(BudgetType.DEPARTMENT)
                .budgetName("Department Budget")
                .totalAmount(new BigDecimal("1000"))
                .safetyBuffer(new BigDecimal("10"))
                .actualSpend(new BigDecimal("500"))
                .committedSpend(new BigDecimal("100")) // 600 <= 900
                .parentBudget(globalBudget)
                .build();

        InternalBudget projectBudget = InternalBudget.builder()
                .budgetType(BudgetType.PROJECT)
                .budgetName("Project Budget")
                .totalAmount(new BigDecimal("1000"))
                .safetyBuffer(new BigDecimal("10"))
                .actualSpend(new BigDecimal("500"))
                .committedSpend(new BigDecimal("100")) // 600 <= 900
                .parentBudget(deptBudget)
                .build();

        InternalBudget reqBudget = InternalBudget.builder()
                .budgetType(BudgetType.REQUEST)
                .budgetName("Request Budget")
                .parentBudget(projectBudget)
                .build();

        testRequest.setBudget(reqBudget);

        WorkflowStateException ex = assertThrows(WorkflowStateException.class,
                () -> workflowEngineService.moveToNextStep(testRequest, testActor, null));

        assertEquals("Budget of : Global Budget exhausted including safety buffer.", ex.getMessage());
    }

    @Test
    void moveToNextStep_BudgetNotExceeded_Success() {
        when(workflowTransitionRepository.findByFromStep(currentStep)).thenReturn(List.of(testTransition));
        when(transitionRuleRepository.findByTransition(testTransition)).thenReturn(Optional.empty());

        InternalBudget budget = new InternalBudget();
        budget.setTotalAmount(new BigDecimal("1000"));
        budget.setSafetyBuffer(new BigDecimal("10")); // 10% safety buffer -> 900 limit
        budget.setActualSpend(new BigDecimal("800"));
        budget.setCommittedSpend(new BigDecimal("50")); // Total 850 <= 900
        testRequest.setBudget(budget);

        assertDoesNotThrow(() -> workflowEngineService.moveToNextStep(testRequest, testActor, null));
    }

    @Test
    void moveToNextStep_PdfAttachmentRequired_ThrowsException() {
        when(workflowTransitionRepository.findByFromStep(currentStep)).thenReturn(List.of(testTransition));

        TransitionRule rule = new TransitionRule();
        rule.setIsPdfRequired(true);
        when(transitionRuleRepository.findByTransition(testTransition)).thenReturn(Optional.of(rule));

        WorkflowStateException ex = assertThrows(WorkflowStateException.class,
                () -> workflowEngineService.moveToNextStep(testRequest, testActor, null));
        assertEquals("PDF attachment required", ex.getMessage());
    }

    @Test
    void moveToNextStep_PdfAttachmentProvided_Success() {
        when(workflowTransitionRepository.findByFromStep(currentStep)).thenReturn(List.of(testTransition));

        TransitionRule rule = new TransitionRule();
        rule.setIsPdfRequired(true);
        when(transitionRuleRepository.findByTransition(testTransition)).thenReturn(Optional.of(rule));

        Attachment pdf = new Attachment();
        pdf.setFileType("application/pdf");
        testRequest.getAttachments().add(pdf);

        assertDoesNotThrow(() -> workflowEngineService.moveToNextStep(testRequest, testActor, null));
    }

    @Test
    void moveToNextStep_CsvAttachmentRequired_ThrowsException() {
        when(workflowTransitionRepository.findByFromStep(currentStep)).thenReturn(List.of(testTransition));

        TransitionRule rule = new TransitionRule();
        rule.setIsCsvRequired(true);
        when(transitionRuleRepository.findByTransition(testTransition)).thenReturn(Optional.of(rule));

        WorkflowStateException ex = assertThrows(WorkflowStateException.class,
                () -> workflowEngineService.moveToNextStep(testRequest, testActor, null));
        assertEquals("CSV attachment required", ex.getMessage());
    }

    @Test
    void moveToNextStep_ImageAttachmentRequired_ThrowsException() {
        when(workflowTransitionRepository.findByFromStep(currentStep)).thenReturn(List.of(testTransition));

        TransitionRule rule = new TransitionRule();
        rule.setIsImageRequired(true);
        when(transitionRuleRepository.findByTransition(testTransition)).thenReturn(Optional.of(rule));

        Attachment pdf = new Attachment();
        pdf.setFileType("application/pdf");
        testRequest.getAttachments().add(pdf); // Provide pdf instead of image

        WorkflowStateException ex = assertThrows(WorkflowStateException.class,
                () -> workflowEngineService.moveToNextStep(testRequest, testActor, null));
        assertEquals("Image attachment required", ex.getMessage());
    }

    @Test
    void moveToNextStep_MinimumVendorsNotMet_ThrowsException() {
        when(workflowTransitionRepository.findByFromStep(currentStep)).thenReturn(List.of(testTransition));

        TransitionRule rule = new TransitionRule();
        rule.setMinRequiredVendors((int) 2);
        when(transitionRuleRepository.findByTransition(testTransition)).thenReturn(Optional.of(rule));

        // Setup 1 quote with 1 vendor
        Vendor vendor = new Vendor();
        vendor.setId(1L);

        Quote quote = new Quote();
        quote.setVendorID(vendor);

        when(quoteRepository.findByRequestRequestIDOrderByQuoteIDAsc(testRequest.getRequestID()))
            .thenReturn(List.of(quote));

        WorkflowStateException ex = assertThrows(WorkflowStateException.class,
                () -> workflowEngineService.moveToNextStep(testRequest, testActor, null));
        assertEquals("A minimum of 2 distinct vendors is required", ex.getMessage());
    }

    @Test
    void moveToNextStep_MinimumVendorsMet_Success() {
        when(workflowTransitionRepository.findByFromStep(currentStep)).thenReturn(List.of(testTransition));

        TransitionRule rule = new TransitionRule();
        rule.setMinRequiredVendors((int) 2);
        when(transitionRuleRepository.findByTransition(testTransition)).thenReturn(Optional.of(rule));

        // Setup quotes with 2 unique vendors
        Vendor vendor1 = new Vendor();
        vendor1.setId(1L);
        Quote quote1 = new Quote();
        quote1.setVendorID(vendor1);

        Vendor vendor2 = new Vendor();
        vendor2.setId(2L);
        Quote quote2 = new Quote();
        quote2.setVendorID(vendor2);

        when(quoteRepository.findByRequestRequestIDOrderByQuoteIDAsc(testRequest.getRequestID()))
            .thenReturn(List.of(quote1, quote2));

        assertDoesNotThrow(() -> workflowEngineService.moveToNextStep(testRequest, testActor, null));
    }

    //AI-GENERATED
    @Test
    void checkAuthorization_UnassignedStepRequesterActor_ThrowsAccessDeniedException() {
        currentStep.setRole(null);
        currentStep.setWorkflowComponent(WorkflowComponent.STEP);
        testActor.setId(99L);
        testActor.setRole(UserRole.REQUESTER);
        when(userRepository.findById(testActor.getId())).thenReturn(Optional.of(testActor));

        AccessDeniedException ex = assertThrows(
                AccessDeniedException.class,
                () -> workflowEngineService.moveToNextStep(testRequest, testActor, null)
        );
        assertEquals("Requesters are not authorized to act on unassigned steps.", ex.getMessage());
    }

    //AI-GENERATED
    @Test
    void checkAuthorization_UnassignedStepProcurementOfficerDifferentDepartment_ThrowsAccessDeniedException() {
        currentStep.setRole(null);
        currentStep.setWorkflowComponent(WorkflowComponent.STEP);
        
        // Requisition requester
        User reqUser = new User();
        Team reqTeam = new Team();
        Department reqDept = new Department();
        reqDept.setDepartmentId(1L);
        reqTeam.setDepartment(reqDept);
        reqUser.setTeam(reqTeam);
        testRequest.setUser(reqUser);

        // Actor: Procurement Officer in Department 2
        testActor.setId(99L);
        testActor.setRole(UserRole.PROCUREMENT_OFFICER);
        Department actorDept = new Department();
        actorDept.setDepartmentId(2L);
        testActor.setDepartment(actorDept);

        when(userRepository.findById(testActor.getId())).thenReturn(Optional.of(testActor));

        AccessDeniedException ex = assertThrows(
                AccessDeniedException.class,
                () -> workflowEngineService.moveToNextStep(testRequest, testActor, null)
        );
        assertEquals("You are not in the same department as the request.", ex.getMessage());
    }

    //AI-GENERATED
    @Test
    void checkAuthorization_UnassignedStepProcurementOfficerSameDepartment_Success() {
        currentStep.setRole(null);
        currentStep.setWorkflowComponent(WorkflowComponent.STEP);
        when(workflowTransitionRepository.findByFromStep(currentStep)).thenReturn(List.of(testTransition));
        when(transitionRuleRepository.findByTransition(testTransition)).thenReturn(Optional.empty());

        // Requisition requester
        User reqUser = new User();
        Team reqTeam = new Team();
        Department reqDept = new Department();
        reqDept.setDepartmentId(1L);
        reqTeam.setDepartment(reqDept);
        reqUser.setTeam(reqTeam);
        testRequest.setUser(reqUser);

        // Actor: Procurement Officer in Department 1
        testActor.setId(99L);
        testActor.setRole(UserRole.PROCUREMENT_OFFICER);
        Department actorDept = new Department();
        actorDept.setDepartmentId(1L);
        testActor.setDepartment(actorDept);

        when(userRepository.findById(testActor.getId())).thenReturn(Optional.of(testActor));

        assertDoesNotThrow(() -> workflowEngineService.moveToNextStep(testRequest, testActor, null));
    }

    //AI-GENERATED
    @Test
    void checkAuthorization_UnassignedStepFinanceOfficer_Success() {
        currentStep.setRole(null);
        currentStep.setWorkflowComponent(WorkflowComponent.STEP);
        when(workflowTransitionRepository.findByFromStep(currentStep)).thenReturn(List.of(testTransition));
        when(transitionRuleRepository.findByTransition(testTransition)).thenReturn(Optional.empty());

        // Actor: Finance Officer from anywhere
        testActor.setId(99L);
        testActor.setRole(UserRole.FINANCE_OFFICER);

        when(userRepository.findById(testActor.getId())).thenReturn(Optional.of(testActor));

        assertDoesNotThrow(() -> workflowEngineService.moveToNextStep(testRequest, testActor, null));
    }

    //AI-GENERATED
    @Test
    void checkAuthorization_StartEventCreatorActor_Success() {
        currentStep.setRole(null);
        currentStep.setWorkflowComponent(WorkflowComponent.START_EVENT);
        when(workflowTransitionRepository.findByFromStep(currentStep)).thenReturn(List.of(testTransition));
        when(transitionRuleRepository.findByTransition(testTransition)).thenReturn(Optional.empty());

        // Requisition requester (creator)
        User reqUser = new User();
        reqUser.setId(99L);
        testRequest.setUser(reqUser);

        // Actor: same creator
        testActor.setId(99L);
        testActor.setRole(UserRole.REQUESTER);

        when(userRepository.findById(testActor.getId())).thenReturn(Optional.of(testActor));

        assertDoesNotThrow(() -> workflowEngineService.moveToNextStep(testRequest, testActor, null));
    }

    //AI-GENERATED
    @Test
    void checkAuthorization_StartEventNonCreatorActor_ThrowsAccessDeniedException() {
        currentStep.setRole(null);
        currentStep.setWorkflowComponent(WorkflowComponent.START_EVENT);

        // Requisition requester (creator)
        User reqUser = new User();
        reqUser.setId(99L);
        testRequest.setUser(reqUser);

        // Actor: different requester
        testActor.setId(100L);
        testActor.setRole(UserRole.REQUESTER);

        when(userRepository.findById(testActor.getId())).thenReturn(Optional.of(testActor));

        AccessDeniedException ex = assertThrows(
                AccessDeniedException.class,
                () -> workflowEngineService.moveToNextStep(testRequest, testActor, null)
        );
        assertEquals("Only the creator of the request can submit it.", ex.getMessage());
    }

    //AI-GENERATED
    @Test
    void checkAuthorization_BranchStepRequesterActor_Success() {
        currentStep.setRole(null);
        currentStep.setWorkflowComponent(WorkflowComponent.BRANCH);
        when(workflowTransitionRepository.findByFromStep(currentStep)).thenReturn(List.of(testTransition));
        when(transitionRuleRepository.findByTransition(testTransition)).thenReturn(Optional.empty());

        testActor.setId(99L);
        testActor.setRole(UserRole.REQUESTER);

        assertDoesNotThrow(() -> workflowEngineService.moveToNextStep(testRequest, testActor, null));
    }

    //AI-GENERATED
    @Test
    void moveToNextStep_StepWithPdfRequired_OnlyOldPdfExists_ThrowsException() {
        currentStep.setWorkflowComponent(WorkflowComponent.STEP);
        currentStep.setRole(UserRole.REQUESTER);
        when(userRepository.findById(testActor.getId())).thenReturn(Optional.of(testActor));

        when(workflowTransitionRepository.findByFromStep(currentStep)).thenReturn(List.of(testTransition));

        TransitionRule rule = new TransitionRule();
        rule.setIsPdfRequired(true);
        when(transitionRuleRepository.findByTransition(testTransition)).thenReturn(Optional.of(rule));

        LocalDateTime entryTime = LocalDateTime.of(2026, 5, 26, 14, 0, 0);
        AuditLog entryLog = AuditLog.builder()
                .newStep(currentStep)
                .timestamp(entryTime)
                .build();
        when(auditLogRepository.findFirstByRequestAndNewStepOrderByTimestampAsc(testRequest, currentStep))
                .thenReturn(Optional.of(entryLog));

        Attachment oldPdf = new Attachment();
        oldPdf.setFileType("application/pdf");
        oldPdf.setUploadedAt(LocalDateTime.of(2026, 5, 26, 13, 59, 0));
        testRequest.getAttachments().add(oldPdf);

        WorkflowStateException ex = assertThrows(WorkflowStateException.class,
                () -> workflowEngineService.moveToNextStep(testRequest, testActor, null));
        assertEquals("PDF attachment required", ex.getMessage());
    }

    //AI-GENERATED
    @Test
    void moveToNextStep_StepWithPdfRequired_NewPdfExists_Success() {
        currentStep.setWorkflowComponent(WorkflowComponent.STEP);
        currentStep.setRole(UserRole.REQUESTER);
        when(userRepository.findById(testActor.getId())).thenReturn(Optional.of(testActor));

        when(workflowTransitionRepository.findByFromStep(currentStep)).thenReturn(List.of(testTransition));

        TransitionRule rule = new TransitionRule();
        rule.setIsPdfRequired(true);
        when(transitionRuleRepository.findByTransition(testTransition)).thenReturn(Optional.of(rule));

        LocalDateTime entryTime = LocalDateTime.of(2026, 5, 26, 14, 0, 0);
        AuditLog entryLog = AuditLog.builder()
                .newStep(currentStep)
                .timestamp(entryTime)
                .build();
        when(auditLogRepository.findFirstByRequestAndNewStepOrderByTimestampAsc(testRequest, currentStep))
                .thenReturn(Optional.of(entryLog));

        Attachment newPdf = new Attachment();
        newPdf.setFileType("application/pdf");
        newPdf.setUploadedAt(LocalDateTime.of(2026, 5, 26, 14, 1, 0));
        testRequest.getAttachments().add(newPdf);

        assertDoesNotThrow(() -> workflowEngineService.moveToNextStep(testRequest, testActor, null));
    }

    @Test
    void moveToNextStep_StepWithCsvRequired_OldCsvExists_ThrowsException() {
        currentStep.setWorkflowComponent(WorkflowComponent.STEP);
        currentStep.setRole(UserRole.REQUESTER);
        when(userRepository.findById(testActor.getId())).thenReturn(Optional.of(testActor));

        when(workflowTransitionRepository.findByFromStep(currentStep)).thenReturn(List.of(testTransition));

        TransitionRule rule = new TransitionRule();
        rule.setIsCsvRequired(true);
        when(transitionRuleRepository.findByTransition(testTransition)).thenReturn(Optional.of(rule));

        LocalDateTime entryTime = LocalDateTime.of(2026, 5, 26, 14, 0, 0);
        AuditLog entryLog = AuditLog.builder()
                .newStep(currentStep)
                .timestamp(entryTime)
                .build();
        when(auditLogRepository.findFirstByRequestAndNewStepOrderByTimestampAsc(testRequest, currentStep))
                .thenReturn(Optional.of(entryLog));

        Attachment oldCsv = new Attachment();
        oldCsv.setFileType("text/csv");
        oldCsv.setUploadedAt(LocalDateTime.of(2026, 5, 26, 13, 59, 0));
        testRequest.getAttachments().add(oldCsv);

        WorkflowStateException ex = assertThrows(WorkflowStateException.class,
                () -> workflowEngineService.moveToNextStep(testRequest, testActor, null));
        assertEquals("CSV attachment required", ex.getMessage());
    }

    @Test
    void moveToNextStep_StepWithCsvRequired_NewCsvExists_Success() {
        currentStep.setWorkflowComponent(WorkflowComponent.STEP);
        currentStep.setRole(UserRole.REQUESTER);
        when(userRepository.findById(testActor.getId())).thenReturn(Optional.of(testActor));

        when(workflowTransitionRepository.findByFromStep(currentStep)).thenReturn(List.of(testTransition));

        TransitionRule rule = new TransitionRule();
        rule.setIsCsvRequired(true);
        when(transitionRuleRepository.findByTransition(testTransition)).thenReturn(Optional.of(rule));

        LocalDateTime entryTime = LocalDateTime.of(2026, 5, 26, 14, 0, 0);
        AuditLog entryLog = AuditLog.builder()
                .newStep(currentStep)
                .timestamp(entryTime)
                .build();
        when(auditLogRepository.findFirstByRequestAndNewStepOrderByTimestampAsc(testRequest, currentStep))
                .thenReturn(Optional.of(entryLog));

        Attachment newCsv = new Attachment();
        newCsv.setFileType("text/csv");
        newCsv.setUploadedAt(LocalDateTime.of(2026, 5, 26, 14, 1, 0));
        testRequest.getAttachments().add(newCsv);

        assertDoesNotThrow(() -> workflowEngineService.moveToNextStep(testRequest, testActor, null));
    }

    @Test
    void moveToNextStep_StepWithImageRequired_OldImageExists_ThrowsException() {
        currentStep.setWorkflowComponent(WorkflowComponent.STEP);
        currentStep.setRole(UserRole.REQUESTER);
        when(userRepository.findById(testActor.getId())).thenReturn(Optional.of(testActor));

        when(workflowTransitionRepository.findByFromStep(currentStep)).thenReturn(List.of(testTransition));

        TransitionRule rule = new TransitionRule();
        rule.setIsImageRequired(true);
        when(transitionRuleRepository.findByTransition(testTransition)).thenReturn(Optional.of(rule));

        LocalDateTime entryTime = LocalDateTime.of(2026, 5, 26, 14, 0, 0);
        AuditLog entryLog = AuditLog.builder()
                .newStep(currentStep)
                .timestamp(entryTime)
                .build();
        when(auditLogRepository.findFirstByRequestAndNewStepOrderByTimestampAsc(testRequest, currentStep))
                .thenReturn(Optional.of(entryLog));

        Attachment oldImg = new Attachment();
        oldImg.setFileType("image/png");
        oldImg.setUploadedAt(LocalDateTime.of(2026, 5, 26, 13, 59, 0));
        testRequest.getAttachments().add(oldImg);

        WorkflowStateException ex = assertThrows(WorkflowStateException.class,
                () -> workflowEngineService.moveToNextStep(testRequest, testActor, null));
        assertEquals("Image attachment required", ex.getMessage());
    }

    @Test
    void moveToNextStep_StepWithImageRequired_NewImageExists_Success() {
        currentStep.setWorkflowComponent(WorkflowComponent.STEP);
        currentStep.setRole(UserRole.REQUESTER);
        when(userRepository.findById(testActor.getId())).thenReturn(Optional.of(testActor));

        when(workflowTransitionRepository.findByFromStep(currentStep)).thenReturn(List.of(testTransition));

        TransitionRule rule = new TransitionRule();
        rule.setIsImageRequired(true);
        when(transitionRuleRepository.findByTransition(testTransition)).thenReturn(Optional.of(rule));

        LocalDateTime entryTime = LocalDateTime.of(2026, 5, 26, 14, 0, 0);
        AuditLog entryLog = AuditLog.builder()
                .newStep(currentStep)
                .timestamp(entryTime)
                .build();
        when(auditLogRepository.findFirstByRequestAndNewStepOrderByTimestampAsc(testRequest, currentStep))
                .thenReturn(Optional.of(entryLog));

        Attachment newImg = new Attachment();
        newImg.setFileType("image/jpeg");
        newImg.setUploadedAt(LocalDateTime.of(2026, 5, 26, 14, 1, 0));
        testRequest.getAttachments().add(newImg);

        assertDoesNotThrow(() -> workflowEngineService.moveToNextStep(testRequest, testActor, null));
    }
    //AI-GENERATED
    @Test
    void moveToNextStep_NextStepIsAutomatedApproval_TransitionsAutomatically() {
        WorkflowStep automatedStep = new WorkflowStep();
        automatedStep.setWorkflowComponent(WorkflowComponent.STEP);
        automatedStep.setName("Automated Approve Step");
        automatedStep.setIsAutomatedApproval(true);

        WorkflowStep endStep = new WorkflowStep();
        endStep.setWorkflowComponent(WorkflowComponent.END_EVENT);
        endStep.setName("End");

        WorkflowTransition firstTransition = new WorkflowTransition();
        firstTransition.setFromStep(currentStep);
        firstTransition.setToStep(automatedStep);

        WorkflowTransition secondTransition = new WorkflowTransition();
        secondTransition.setFromStep(automatedStep);
        secondTransition.setToStep(endStep);

        when(workflowTransitionRepository.findByFromStep(currentStep)).thenReturn(List.of(firstTransition));
        when(workflowTransitionRepository.findByFromStep(automatedStep)).thenReturn(List.of(secondTransition));
        when(transitionRuleRepository.findByTransition(firstTransition)).thenReturn(Optional.empty());
        when(transitionRuleRepository.findByTransition(secondTransition)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> workflowEngineService.moveToNextStep(testRequest, testActor, null));

        assertEquals(endStep, testRequest.getCurrentStep());
    }

    @Test
    void checkAuthorization_AutomatedStep_ReturnsEarlyWithoutError() {
        WorkflowStep automatedStep = new WorkflowStep();
        automatedStep.setWorkflowComponent(WorkflowComponent.STEP);
        automatedStep.setIsAutomatedApproval(true);
        automatedStep.setRole(UserRole.ADMINISTRATOR);

        testActor.setId(99L);
        testActor.setRole(UserRole.REQUESTER);

        assertDoesNotThrow(() -> workflowEngineService.checkAuthorization(testRequest, testActor, automatedStep));
    }

    // AI-GENERATED
    @Test
    void moveToNextStep_TeamLeaderStep_AssigneeResolvedToTeamLeader_Success() {
        nextStep.setWorkflowComponent(WorkflowComponent.STEP);
        nextStep.setRole(UserRole.REQUESTER);
        nextStep.setIsTeamLeader(true);

        User reqUser = new User();
        Team reqTeam = new Team();
        User teamLeader = new User();
        teamLeader.setId(200L);
        reqTeam.setLeader(teamLeader);
        reqUser.setTeam(reqTeam);
        testRequest.setUser(reqUser);

        when(workflowTransitionRepository.findByFromStep(currentStep)).thenReturn(List.of(testTransition));
        when(transitionRuleRepository.findByTransition(testTransition)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> workflowEngineService.moveToNextStep(testRequest, testActor, null));
        assertEquals(teamLeader, testRequest.getAssignee());
    }

    // AI-GENERATED
    @Test
    void moveToNextStep_TeamLeaderStep_NoTeamOrLeader_ThrowsException() {
        nextStep.setWorkflowComponent(WorkflowComponent.STEP);
        nextStep.setRole(UserRole.REQUESTER);
        nextStep.setIsTeamLeader(true);

        User reqUser = new User();
        testRequest.setUser(reqUser);

        when(workflowTransitionRepository.findByFromStep(currentStep)).thenReturn(List.of(testTransition));

        WorkflowStateException ex = assertThrows(WorkflowStateException.class,
                () -> workflowEngineService.moveToNextStep(testRequest, testActor, null));
        assertTrue(ex.getMessage().contains("team leader could not be resolved"));
    }

    // AI-GENERATED
    @Test
    void checkAuthorization_TeamLeaderStep_NonTeamLeaderActor_ThrowsAccessDeniedException() {
        currentStep.setWorkflowComponent(WorkflowComponent.STEP);
        currentStep.setRole(UserRole.REQUESTER);
        currentStep.setIsTeamLeader(true);

        User reqUser = new User();
        Team reqTeam = new Team();
        User teamLeader = new User();
        teamLeader.setId(200L);
        reqTeam.setLeader(teamLeader);
        reqUser.setTeam(reqTeam);
        testRequest.setUser(reqUser);

        // Actor is different
        testActor.setId(99L);
        when(userRepository.findById(testActor.getId())).thenReturn(Optional.of(testActor));

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> workflowEngineService.checkAuthorization(testRequest, testActor, currentStep));
        assertTrue(ex.getMessage().contains("Only the requester's team leader is authorized"));
    }

    // AI-GENERATED
    @Test
    void checkAuthorization_TeamLeaderStep_TeamLeaderActor_Success() {
        currentStep.setWorkflowComponent(WorkflowComponent.STEP);
        currentStep.setRole(UserRole.REQUESTER);
        currentStep.setIsTeamLeader(true);

        User reqUser = new User();
        Team reqTeam = new Team();
        User teamLeader = new User();
        teamLeader.setId(200L);
        reqTeam.setLeader(teamLeader);
        reqUser.setTeam(reqTeam);
        testRequest.setUser(reqUser);

        // Actor is the team leader
        testActor.setId(200L);
        when(userRepository.findById(testActor.getId())).thenReturn(Optional.of(testActor));

        assertDoesNotThrow(() -> workflowEngineService.checkAuthorization(testRequest, testActor, currentStep));
    }

    @Test
    void moveToNextStep_AdvancedRuleEvaluatesToTrue_Success() {
        when(workflowTransitionRepository.findByFromStep(currentStep)).thenReturn(List.of(testTransition));

        TransitionRule rule = new TransitionRule();
        rule.setAdvancedRule("totalQuantity < 10");
        when(transitionRuleRepository.findByTransition(testTransition)).thenReturn(Optional.of(rule));

        testRequest.setTotalQuantity(5);

        assertDoesNotThrow(() -> workflowEngineService.moveToNextStep(testRequest, testActor, null));
    }

    @Test
    void moveToNextStep_AdvancedRuleEvaluatesToFalse_ThrowsException() {
        when(workflowTransitionRepository.findByFromStep(currentStep)).thenReturn(List.of(testTransition));

        TransitionRule rule = new TransitionRule();
        rule.setAdvancedRule("totalQuantity < 10");
        when(transitionRuleRepository.findByTransition(testTransition)).thenReturn(Optional.of(rule));

        testRequest.setTotalQuantity(15);

        WorkflowStateException ex = assertThrows(WorkflowStateException.class,
                () -> workflowEngineService.moveToNextStep(testRequest, testActor, null));
        assertTrue(ex.getMessage().contains("Advanced validation rule failed: totalQuantity < 10"));
    }

    @Test
    void moveToNextStep_AdvancedRuleEvaluationError_ThrowsException() {
        when(workflowTransitionRepository.findByFromStep(currentStep)).thenReturn(List.of(testTransition));

        TransitionRule rule = new TransitionRule();
        rule.setAdvancedRule("invalidSpelConstruct");
        when(transitionRuleRepository.findByTransition(testTransition)).thenReturn(Optional.of(rule));

        WorkflowStateException ex = assertThrows(WorkflowStateException.class,
                () -> workflowEngineService.moveToNextStep(testRequest, testActor, null));
        assertTrue(ex.getMessage().contains("Advanced validation rule failed: invalidSpelConstruct"));
    }

    @Test
    void moveToNextStep_VendorReliabilityNotMet_ThrowsException() {
        when(workflowTransitionRepository.findByFromStep(currentStep)).thenReturn(List.of(testTransition));
        TransitionRule rule = new TransitionRule();
        rule.setMinVendorReliabilityScore(7.5);
        when(transitionRuleRepository.findByTransition(testTransition)).thenReturn(Optional.of(rule));
        Vendor vendor = new Vendor();
        vendor.setId(1L);
        vendor.setVendorName("Unreliable Vendor");
        vendor.setOverallScore(6.8);
        Quote quote = new Quote();
        quote.setVendorID(vendor);
        quote.setSelected(true);
        testRequest.getQuotes().add(quote);
        WorkflowStateException ex = assertThrows(WorkflowStateException.class,
                () -> workflowEngineService.moveToNextStep(testRequest, testActor, null));
        assertTrue(ex.getMessage().contains("Selected vendor Unreliable Vendor reliability score (6.80) is below the required minimum of 7.50"));
    }

    @Test
    void moveToNextStep_VendorReliabilityMetSelectedQuote_Success() {
        when(workflowTransitionRepository.findByFromStep(currentStep)).thenReturn(List.of(testTransition));
        TransitionRule rule = new TransitionRule();
        rule.setMinVendorReliabilityScore(7.5);
        when(transitionRuleRepository.findByTransition(testTransition)).thenReturn(Optional.of(rule));
        Vendor vendor = new Vendor();
        vendor.setId(1L);
        vendor.setVendorName("Reliable Vendor");
        vendor.setOverallScore(8.2);
        Quote quote = new Quote();
        quote.setVendorID(vendor);
        quote.setSelected(true);
        testRequest.getQuotes().add(quote);
        assertDoesNotThrow(() -> workflowEngineService.moveToNextStep(testRequest, testActor, null));
    }

    @Test
    void moveToNextStep_VendorReliabilityNoSelectedQuote_ThrowsException() {
        when(workflowTransitionRepository.findByFromStep(currentStep)).thenReturn(List.of(testTransition));
        TransitionRule rule = new TransitionRule();
        rule.setMinVendorReliabilityScore(7.5);
        when(transitionRuleRepository.findByTransition(testTransition)).thenReturn(Optional.of(rule));
        Vendor vendor = new Vendor();
        vendor.setId(1L);
        vendor.setOverallScore(8.2);
        Quote quote = new Quote();
        quote.setVendorID(vendor);
        quote.setSelected(false);
        testRequest.getQuotes().add(quote);
        WorkflowStateException ex = assertThrows(WorkflowStateException.class,
                () -> workflowEngineService.moveToNextStep(testRequest, testActor, null));
        assertTrue(ex.getMessage().contains("No quote has been selected for the procurement request"));
    }

    @Test
    void moveToNextStep_VendorReliabilityNoQuotes_ThrowsException() {
        when(workflowTransitionRepository.findByFromStep(currentStep)).thenReturn(List.of(testTransition));
        TransitionRule rule = new TransitionRule();
        rule.setMinVendorReliabilityScore(7.5);
        when(transitionRuleRepository.findByTransition(testTransition)).thenReturn(Optional.of(rule));
        WorkflowStateException ex = assertThrows(WorkflowStateException.class,
                () -> workflowEngineService.moveToNextStep(testRequest, testActor, null));
        assertTrue(ex.getMessage().contains("No quote has been selected for the procurement request"));
    }

    @Test
    void checkAuthorization_AssigneeMismatch_ThrowsAccessDeniedException() {
        User assignee = new User();
        assignee.setId(10L);
        testRequest.setAssignee(assignee);

        User actor = new User();
        actor.setId(11L);

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> workflowEngineService.checkAuthorization(testRequest, actor, currentStep));
        assertEquals("Only the assigned user can act on this step.", ex.getMessage());
    }

    @Test
    void checkAuthorization_TeamLeaderChecks_ThrowsAccessDeniedException() {
        currentStep.setRole(UserRole.REQUESTER);
        currentStep.setIsTeamLeader(true);

        User requester = new User();
        requester.setId(20L);
        testRequest.setUser(requester);

        User actor = new User();
        actor.setId(21L);
        when(userRepository.findById(actor.getId())).thenReturn(Optional.of(actor));

        // Test 1: Team is null
        assertThrows(AccessDeniedException.class,
                () -> workflowEngineService.checkAuthorization(testRequest, actor, currentStep));

        // Test 2: Team leader is null
        Team team = new Team();
        requester.setTeam(team);
        assertThrows(AccessDeniedException.class,
                () -> workflowEngineService.checkAuthorization(testRequest, actor, currentStep));

        // Test 3: Leader mismatch
        User leader = new User();
        leader.setId(22L);
        team.setLeader(leader);
        assertThrows(AccessDeniedException.class,
                () -> workflowEngineService.checkAuthorization(testRequest, actor, currentStep));
    }

    @Test
    void checkAuthorization_RoleMismatch_ThrowsAccessDeniedException() {
        currentStep.setRole(UserRole.FINANCE_OFFICER);
        currentStep.setIsTeamLeader(false);

        User actor = new User();
        actor.setId(30L);
        actor.setRole(UserRole.REQUESTER);
        when(userRepository.findById(actor.getId())).thenReturn(Optional.of(actor));

        assertThrows(AccessDeniedException.class,
                () -> workflowEngineService.checkAuthorization(testRequest, actor, currentStep));
    }

    @Test
    void checkAuthorization_ProcurementOfficerDepartmentMismatch_ThrowsAccessDeniedException() {
        currentStep.setRole(UserRole.PROCUREMENT_OFFICER);

        Department dept1 = new Department();
        dept1.setDepartmentId(1L);
        Team team = new Team();
        team.setDepartment(dept1);
        User requester = new User();
        requester.setTeam(team);
        testRequest.setUser(requester);

        Department dept2 = new Department();
        dept2.setDepartmentId(2L);
        User actor = new User();
        actor.setId(40L);
        actor.setRole(UserRole.PROCUREMENT_OFFICER);
        actor.setDepartment(dept2);
        when(userRepository.findById(actor.getId())).thenReturn(Optional.of(actor));

        assertThrows(AccessDeniedException.class,
                () -> workflowEngineService.checkAuthorization(testRequest, actor, currentStep));
    }

    @Test
    void checkAuthorization_StartEventCreatorMismatch_ThrowsAccessDeniedException() {
        currentStep.setRole(null);
        currentStep.setWorkflowComponent(WorkflowComponent.START_EVENT);

        User creator = new User();
        creator.setId(50L);
        testRequest.setUser(creator);

        User actor = new User();
        actor.setId(51L);
        when(userRepository.findById(actor.getId())).thenReturn(Optional.of(actor));

        assertThrows(AccessDeniedException.class,
                () -> workflowEngineService.checkAuthorization(testRequest, actor, currentStep));
    }

    @Test
    void checkAuthorization_UnassignedStepRequester_ThrowsAccessDeniedException() {
        currentStep.setRole(null);
        currentStep.setWorkflowComponent(WorkflowComponent.STEP);

        User actor = new User();
        actor.setId(60L);
        actor.setRole(UserRole.REQUESTER);
        when(userRepository.findById(actor.getId())).thenReturn(Optional.of(actor));

        assertThrows(AccessDeniedException.class,
                () -> workflowEngineService.checkAuthorization(testRequest, actor, currentStep));
    }

    @Test
    void assertBudgetWithinSafetyBuffer_NullOrBlankNames_ThrowsWorkflowStateException() {
        InternalBudget projBudget = new InternalBudget();
        projBudget.setBudgetType(BudgetType.PROJECT);
        projBudget.setBudgetName("");
        projBudget.setTotalAmount(BigDecimal.valueOf(100));
        projBudget.setActualSpend(BigDecimal.valueOf(95));
        projBudget.setCommittedSpend(BigDecimal.valueOf(10)); // 105 > 100
        projBudget.setSafetyBuffer(BigDecimal.ZERO);

        testRequest.setBudget(projBudget);
        when(workflowTransitionRepository.findByFromStep(currentStep)).thenReturn(List.of(testTransition));

        WorkflowStateException projEx = assertThrows(WorkflowStateException.class,
                () -> workflowEngineService.moveToNextStep(testRequest, testActor, null));

        InternalBudget deptBudget = new InternalBudget();
        deptBudget.setBudgetType(BudgetType.DEPARTMENT);
        deptBudget.setBudgetName(null);
        deptBudget.setTotalAmount(BigDecimal.valueOf(100));
        deptBudget.setActualSpend(BigDecimal.valueOf(95));
        deptBudget.setCommittedSpend(BigDecimal.valueOf(10));
        deptBudget.setSafetyBuffer(BigDecimal.ZERO);
        testRequest.setBudget(deptBudget);

        WorkflowStateException deptEx = assertThrows(WorkflowStateException.class,
                () -> workflowEngineService.moveToNextStep(testRequest, testActor, null));

        InternalBudget globalBudget = new InternalBudget();
        globalBudget.setBudgetType(BudgetType.GLOBAL);
        globalBudget.setBudgetName(" ");
        globalBudget.setTotalAmount(BigDecimal.valueOf(100));
        globalBudget.setActualSpend(BigDecimal.valueOf(95));
        globalBudget.setCommittedSpend(BigDecimal.valueOf(10));
        globalBudget.setSafetyBuffer(BigDecimal.ZERO);
        testRequest.setBudget(globalBudget);

        WorkflowStateException globalEx = assertThrows(WorkflowStateException.class,
                () -> workflowEngineService.moveToNextStep(testRequest, testActor, null));

        assertAll(
            () -> assertTrue(projEx.getMessage().contains("Project budget exhausted")),
            () -> assertTrue(deptEx.getMessage().contains("Department budget exhausted")),
            () -> assertTrue(globalEx.getMessage().contains("Global budget exhausted"))
        );
    }

    @Test
    void moveToNextStep_MaxRecursionExceeded_ThrowsWorkflowStateException() {
        WorkflowStep stepA = new WorkflowStep();
        stepA.setWorkflowComponent(WorkflowComponent.STEP);
        stepA.setName("Step A");
        stepA.setIsAutomatedApproval(true);
        testRequest.setCurrentStep(stepA);

        WorkflowTransition cycleTrans = new WorkflowTransition();
        cycleTrans.setFromStep(stepA);
        cycleTrans.setToStep(stepA);

        when(workflowTransitionRepository.findByFromStep(stepA)).thenReturn(List.of(cycleTrans));

        assertThrows(WorkflowStateException.class,
                () -> workflowEngineService.moveToNextStep(testRequest, testActor, null));
    }

    @Test
    void moveToNextStep_ToStepRequesterTeamLeaderLeaderNull_ThrowsWorkflowStateException() {
        nextStep.setWorkflowComponent(WorkflowComponent.STEP);
        nextStep.setRole(UserRole.REQUESTER);
        nextStep.setIsTeamLeader(true);

        User creator = new User();
        Team team = new Team();
        creator.setTeam(team);
        testRequest.setUser(creator);

        when(workflowTransitionRepository.findByFromStep(currentStep)).thenReturn(List.of(testTransition));

        WorkflowStateException ex = assertThrows(WorkflowStateException.class,
                () -> workflowEngineService.moveToNextStep(testRequest, testActor, null));
        assertTrue(ex.getMessage().contains("requester's team leader could not be resolved"));
    }

    @Test
    void moveToNextStep_ToStepRoleMismatchNextAssignee_ThrowsIllegalArgumentException() {
        nextStep.setWorkflowComponent(WorkflowComponent.STEP);
        nextStep.setRole(UserRole.FINANCE_OFFICER);

        when(workflowTransitionRepository.findByFromStep(currentStep)).thenReturn(List.of(testTransition));

        User badAssignee = new User();
        badAssignee.setId(99L);
        badAssignee.setName("Bad User");
        badAssignee.setRole(UserRole.REQUESTER);
        lenient().when(userRepository.findById(99L)).thenReturn(Optional.of(badAssignee));

        assertThrows(IllegalArgumentException.class,
                () -> workflowEngineService.moveToNextStep(testRequest, testActor, 99L));
    }

    @Test
    void moveToNextStep_TransitionRulesCheckEdgeCases_ThrowsWorkflowStateException() {
        when(workflowTransitionRepository.findByFromStep(currentStep)).thenReturn(List.of(testTransition));
        
        TransitionRule rule = new TransitionRule();
        rule.setMinVendorReliabilityScore(7.0);
        rule.setIsPdfRequired(true);
        rule.setIsCsvRequired(true);
        rule.setIsImageRequired(true);
        when(transitionRuleRepository.findByTransition(testTransition)).thenReturn(Optional.of(rule));

        Quote quote = new Quote();
        quote.setSelected(true);
        testRequest.getQuotes().add(quote);

        WorkflowStateException ex = assertThrows(WorkflowStateException.class,
                () -> workflowEngineService.moveToNextStep(testRequest, testActor, null));
        
        assertAll(
            () -> assertTrue(ex.getMessage().contains("Selected quote has no associated vendor")),
            () -> assertTrue(ex.getMessage().contains("Missing required attachments: PDF, CSV, Image"))
        );
    }

    @Test
    void moveToNextStep_TransitionRulesAdvancedRuleSpelException_ThrowsWorkflowStateException() {
        when(workflowTransitionRepository.findByFromStep(currentStep)).thenReturn(List.of(testTransition));
        
        TransitionRule rule = new TransitionRule();
        rule.setAdvancedRule(">>> invalid spel syntax <<<");
        when(transitionRuleRepository.findByTransition(testTransition)).thenReturn(Optional.of(rule));

        WorkflowStateException ex = assertThrows(WorkflowStateException.class,
                () -> workflowEngineService.moveToNextStep(testRequest, testActor, null));
        assertTrue(ex.getMessage().contains("Advanced validation rule failed"));
    }

    @Test
    void revertToPreviousStep_NoValidStepHistory_ThrowsWorkflowStateException() {
        when(auditLogRepository.findFirstByRequestAndNewStepAndActionOrderByTimestampDesc(testRequest, currentStep, AuditActionConstants.APPROVE))
                .thenReturn(Optional.empty());

        assertThrows(WorkflowStateException.class,
                () -> workflowEngineService.revertToPreviousStep(testRequest, testActor, "reason"));
    }

    @Test
    void revertToPreviousStep_RevertToStartEvent_RevertsToDraftAndNullAssignee() {
        WorkflowStep startStep = new WorkflowStep();
        startStep.setWorkflowComponent(WorkflowComponent.START_EVENT);
        startStep.setName("Start Event");

        AuditLog arrival = AuditLog.builder()
                .previousStep(startStep)
                .newStep(currentStep)
                .build();
        
        when(auditLogRepository.findFirstByRequestAndNewStepAndActionOrderByTimestampDesc(testRequest, currentStep, AuditActionConstants.APPROVE))
                .thenReturn(Optional.of(arrival));

        workflowEngineService.revertToPreviousStep(testRequest, testActor, "revert reason");

        assertAll(
            () -> assertEquals(RequestStatus.DRAFT, testRequest.getState()),
            () -> assertNull(testRequest.getAssignee()),
            () -> assertEquals(startStep, testRequest.getCurrentStep()),
            () -> assertEquals("revert reason", testRequest.getRejectionReason())
        );
    }

    @Test
    void revertToPreviousStep_RevertToNormalStepNoDepartureLog_AssignsToNull() {
        WorkflowStep stepX = new WorkflowStep();
        stepX.setWorkflowComponent(WorkflowComponent.STEP);
        stepX.setName("Step X");

        AuditLog arrival = AuditLog.builder()
                .previousStep(stepX)
                .newStep(currentStep)
                .build();
        
        when(auditLogRepository.findFirstByRequestAndNewStepAndActionOrderByTimestampDesc(testRequest, currentStep, AuditActionConstants.APPROVE))
                .thenReturn(Optional.of(arrival));
        when(auditLogRepository.findFirstByRequestAndPreviousStepAndActionOrderByTimestampDesc(testRequest, stepX, AuditActionConstants.APPROVE))
                .thenReturn(Optional.empty());

        workflowEngineService.revertToPreviousStep(testRequest, testActor, "revert reason");

        assertAll(
            () -> assertNull(testRequest.getAssignee()),
            () -> assertEquals(stepX, testRequest.getCurrentStep())
        );
    }

    @Test
    void startWorkflow_Success() {
        WorkflowDefinition def = new WorkflowDefinition();
        def.setId(10L);
        testRequest.setWorkflowDefinition(def);
        
        when(workflowStepRepository.findFirstByWorkflowDefinitionAndWorkflowComponent(def, WorkflowComponent.START_EVENT))
            .thenReturn(Optional.of(currentStep));

        when(workflowTransitionRepository.findByFromStep(currentStep)).thenReturn(List.of(testTransition));
        when(transitionRuleRepository.findByTransition(testTransition)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> workflowEngineService.startWorkflow(testRequest, testActor, null));
        
        verify(auditService).createWorkflowTransitionLog(eq(testActor), eq(testRequest), isNull(), eq("SUBMIT"), contains("Start"));
    }

    @Test
    void startWorkflow_NoStartStep_ThrowsException() {
        WorkflowDefinition def = new WorkflowDefinition();
        def.setId(10L);
        testRequest.setWorkflowDefinition(def);
        
        when(workflowStepRepository.findFirstByWorkflowDefinitionAndWorkflowComponent(def, WorkflowComponent.START_EVENT))
            .thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, 
            () -> workflowEngineService.startWorkflow(testRequest, testActor, null));
        
        assertEquals("No start step configured in workflow", ex.getMessage());
    }

    @Test
    void getNextStep_DraftRequest_ReturnsNextStep() {
        WorkflowDefinition def = new WorkflowDefinition();
        def.setId(10L);
        testRequest.setWorkflowDefinition(def);
        testRequest.setState(RequestStatus.DRAFT);
        
        WorkflowStep startStep = new WorkflowStep();
        startStep.setName("Start");
        startStep.setWorkflowComponent(WorkflowComponent.START_EVENT);
        
        when(workflowStepRepository.findFirstByWorkflowDefinitionAndWorkflowComponent(def, WorkflowComponent.START_EVENT))
            .thenReturn(Optional.of(startStep));
            
        WorkflowTransition t1 = new WorkflowTransition();
        t1.setFromStep(startStep);
        t1.setToStep(nextStep);
        
        when(workflowTransitionRepository.findByFromStep(startStep)).thenReturn(List.of(t1));

        WorkflowStep result = workflowEngineService.getNextStep(testRequest);
        assertEquals(nextStep, result);
    }

    @Test
    void getNextStep_NullStep_ReturnsNull() {
        testRequest.setState(RequestStatus.ACTIVE);
        testRequest.setCurrentStep(null);

        WorkflowStep result = workflowEngineService.getNextStep(testRequest);
        assertNull(result);
    }

    @Test
    void getNextStep_BranchResolution_ReturnsTargetStep() {
        testRequest.setState(RequestStatus.ACTIVE);
        testRequest.setCurrentStep(currentStep);
        
        WorkflowStep branchStep = new WorkflowStep();
        branchStep.setWorkflowComponent(WorkflowComponent.BRANCH);
        branchStep.setName("Gateway");
        
        WorkflowTransition t1 = new WorkflowTransition();
        t1.setFromStep(currentStep);
        t1.setToStep(branchStep);
        
        WorkflowStep targetStep = new WorkflowStep();
        targetStep.setWorkflowComponent(WorkflowComponent.STEP);
        targetStep.setName("Real Step");
        
        WorkflowTransition t2 = new WorkflowTransition();
        t2.setFromStep(branchStep);
        t2.setToStep(targetStep);
        
        when(workflowTransitionRepository.findByFromStep(currentStep)).thenReturn(List.of(t1));
        when(workflowTransitionRepository.findByFromStep(branchStep)).thenReturn(List.of(t2));

        WorkflowStep result = workflowEngineService.getNextStep(testRequest);
        assertEquals(targetStep, result);
    }

    @Test
    void getNextStep_ConditionEvaluationFails_SkipsTransition() {
        testRequest.setState(RequestStatus.ACTIVE);
        testRequest.setCurrentStep(currentStep);
        
        WorkflowTransition badTransition = new WorkflowTransition();
        badTransition.setFromStep(currentStep);
        badTransition.setToStep(nextStep);
        badTransition.setConditionExpression("invalid syntax here");
        
        WorkflowStep fallbackStep = new WorkflowStep();
        fallbackStep.setName("Fallback");
        
        WorkflowTransition okTransition = new WorkflowTransition();
        okTransition.setFromStep(currentStep);
        okTransition.setToStep(fallbackStep);
        okTransition.setConditionExpression(null);
        
        when(workflowTransitionRepository.findByFromStep(currentStep)).thenReturn(List.of(badTransition, okTransition));

        WorkflowStep result = workflowEngineService.getNextStep(testRequest);
        assertEquals(fallbackStep, result);
    }

    @Test
    void testGetStepNamePrivateMethod() throws Exception {
        Method method = WorkflowEngineServiceImpl.class.getDeclaredMethod("getStepName", WorkflowStep.class);
        method.setAccessible(true);

        WorkflowStep nullStep = null;
        WorkflowStep emptyNameStep = new WorkflowStep();
        emptyNameStep.setName("   ");
        emptyNameStep.setWorkflowComponent(WorkflowComponent.START_EVENT);

        WorkflowStep endStep = new WorkflowStep();
        endStep.setWorkflowComponent(WorkflowComponent.END_EVENT);

        WorkflowStep branchStep = new WorkflowStep();
        branchStep.setWorkflowComponent(WorkflowComponent.BRANCH);

        WorkflowStep stepComponent = new WorkflowStep();
        stepComponent.setWorkflowComponent(WorkflowComponent.STEP);

        WorkflowStep unknownComponent = new WorkflowStep();
        unknownComponent.setWorkflowComponent(null);

        assertAll(
            () -> assertEquals("Unknown", method.invoke(workflowEngineService, nullStep)),
            () -> assertEquals("Start Event", method.invoke(workflowEngineService, emptyNameStep)),
            () -> assertEquals("Finished", method.invoke(workflowEngineService, endStep)),
            () -> assertEquals("Gateway", method.invoke(workflowEngineService, branchStep)),
            () -> assertEquals("Unnamed Step", method.invoke(workflowEngineService, stepComponent)),
            () -> assertEquals("Unknown", method.invoke(workflowEngineService, unknownComponent))
        );
    }

    @Test
    void moveToNextStep_TransitionToRequesterNotLeader_AssignsToRequester() {
        WorkflowStep step = new WorkflowStep();
        step.setWorkflowComponent(WorkflowComponent.STEP);
        step.setRole(UserRole.REQUESTER);
        step.setIsTeamLeader(false);
        step.setName("Requester Step");

        WorkflowTransition t = new WorkflowTransition();
        t.setFromStep(currentStep);
        t.setToStep(step);

        User requester = new User();
        requester.setId(22L);
        testRequest.setUser(requester);

        when(workflowTransitionRepository.findByFromStep(currentStep)).thenReturn(List.of(t));
        when(transitionRuleRepository.findByTransition(t)).thenReturn(Optional.empty());

        workflowEngineService.moveToNextStep(testRequest, testActor, null);

        assertEquals(requester, testRequest.getAssignee());
    }

    @Test
    void moveToNextStep_TransitionWithAssigneeSuccess_AssignsUser() {
        WorkflowStep step = new WorkflowStep();
        step.setWorkflowComponent(WorkflowComponent.STEP);
        step.setRole(UserRole.FINANCE_OFFICER);
        step.setName("Finance Step");

        WorkflowTransition t = new WorkflowTransition();
        t.setFromStep(currentStep);
        t.setToStep(step);

        User assignee = new User();
        assignee.setId(99L);
        assignee.setRole(UserRole.FINANCE_OFFICER);
        assignee.setName("Finance User");

        when(workflowTransitionRepository.findByFromStep(currentStep)).thenReturn(List.of(t));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testActor));
        when(userRepository.findById(99L)).thenReturn(Optional.of(assignee));
        when(transitionRuleRepository.findByTransition(t)).thenReturn(Optional.empty());

        workflowEngineService.moveToNextStep(testRequest, testActor, 99L);

        assertEquals(assignee, testRequest.getAssignee());
    }

    @Test
    void moveToNextStep_TransitionAssigneeNotFound_ThrowsIllegalArgumentException() {
        WorkflowStep step = new WorkflowStep();
        step.setWorkflowComponent(WorkflowComponent.STEP);
        step.setRole(UserRole.FINANCE_OFFICER);
        step.setName("Finance Step");

        WorkflowTransition t = new WorkflowTransition();
        t.setFromStep(currentStep);
        t.setToStep(step);

        when(workflowTransitionRepository.findByFromStep(currentStep)).thenReturn(List.of(t));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testActor));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
            workflowEngineService.moveToNextStep(testRequest, testActor, 99L)
        );
    }

    @Test
    void moveToNextStep_TransitionAssigneeRoleMismatch_ThrowsIllegalArgumentException() {
        WorkflowStep step = new WorkflowStep();
        step.setWorkflowComponent(WorkflowComponent.STEP);
        step.setRole(UserRole.FINANCE_OFFICER);
        step.setName("Finance Step");

        WorkflowTransition t = new WorkflowTransition();
        t.setFromStep(currentStep);
        t.setToStep(step);

        User assignee = new User();
        assignee.setId(99L);
        assignee.setRole(UserRole.REQUESTER);
        assignee.setName("Requester User");

        when(workflowTransitionRepository.findByFromStep(currentStep)).thenReturn(List.of(t));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testActor));
        when(userRepository.findById(99L)).thenReturn(Optional.of(assignee));

        assertThrows(IllegalArgumentException.class, () ->
            workflowEngineService.moveToNextStep(testRequest, testActor, 99L)
        );
    }

    @Test
    void revertToPreviousStep_NoValidStepInHistory_ThrowsWorkflowStateException() {
        when(auditLogRepository.findFirstByRequestAndNewStepAndActionOrderByTimestampDesc(any(), any(), any()))
            .thenReturn(Optional.empty());

        assertThrows(WorkflowStateException.class, () ->
            workflowEngineService.revertToPreviousStep(testRequest, testActor, "reason")
        );
    }

    @Test
    void revertToPreviousStep_TraversesGateways() {
        WorkflowStep branchStep = new WorkflowStep();
        branchStep.setWorkflowComponent(WorkflowComponent.BRANCH);
        branchStep.setName("Gateway");

        WorkflowStep normalStep = new WorkflowStep();
        normalStep.setWorkflowComponent(WorkflowComponent.STEP);
        normalStep.setName("Normal Step");

        AuditLog arrival1 = AuditLog.builder()
                .previousStep(branchStep)
                .newStep(currentStep)
                .build();

        AuditLog arrival2 = AuditLog.builder()
                .previousStep(normalStep)
                .newStep(branchStep)
                .build();

        when(auditLogRepository.findFirstByRequestAndNewStepAndActionOrderByTimestampDesc(testRequest, currentStep, AuditActionConstants.APPROVE))
                .thenReturn(Optional.of(arrival1));
        when(auditLogRepository.findFirstByRequestAndNewStepAndActionOrderByTimestampDesc(testRequest, branchStep, AuditActionConstants.APPROVE))
                .thenReturn(Optional.of(arrival2));
        when(auditLogRepository.findFirstByRequestAndPreviousStepAndActionOrderByTimestampDesc(testRequest, normalStep, AuditActionConstants.APPROVE))
                .thenReturn(Optional.empty());

        workflowEngineService.revertToPreviousStep(testRequest, testActor, "revert");

        assertEquals(normalStep, testRequest.getCurrentStep());
    }

    @Test
    void revertToPreviousStep_TargetDepartureLogPresent_AssignsToActor() {
        WorkflowStep normalStep = new WorkflowStep();
        normalStep.setWorkflowComponent(WorkflowComponent.STEP);
        normalStep.setName("Normal Step");

        AuditLog arrival = AuditLog.builder()
                .previousStep(normalStep)
                .newStep(currentStep)
                .build();

        User departureActor = new User();
        departureActor.setId(55L);
        AuditLog departure = AuditLog.builder()
                .actor(departureActor)
                .build();

        when(auditLogRepository.findFirstByRequestAndNewStepAndActionOrderByTimestampDesc(testRequest, currentStep, AuditActionConstants.APPROVE))
                .thenReturn(Optional.of(arrival));
        when(auditLogRepository.findFirstByRequestAndPreviousStepAndActionOrderByTimestampDesc(testRequest, normalStep, AuditActionConstants.APPROVE))
                .thenReturn(Optional.of(departure));

        workflowEngineService.revertToPreviousStep(testRequest, testActor, "revert");

        assertEquals(departureActor, testRequest.getAssignee());
    }

    @Test
    void checkAuthorization_ProcurementOfficerDifferentDepartment_ThrowsAccessDeniedException() {
        WorkflowStep step = new WorkflowStep();
        step.setWorkflowComponent(WorkflowComponent.STEP);
        step.setRole(UserRole.PROCUREMENT_OFFICER);
        step.setName("Procurement Step");

        User requester = new User();
        Department d1 = new Department();
        d1.setDepartmentId(11L);
        Team t = new Team();
        t.setDepartment(d1);
        requester.setTeam(t);
        testRequest.setUser(requester);

        User officer = new User();
        officer.setId(99L);
        officer.setRole(UserRole.PROCUREMENT_OFFICER);
        Department d2 = new Department();
        d2.setDepartmentId(22L);
        officer.setDepartment(d2);

        when(userRepository.findById(99L)).thenReturn(Optional.of(officer));

        assertThrows(AccessDeniedException.class, () ->
            workflowEngineService.checkAuthorization(testRequest, officer, step)
        );
    }

    @Test
    void checkAuthorization_ProcurementOfficerNullRequestDepartment_Success() {
        WorkflowStep step = new WorkflowStep();
        step.setWorkflowComponent(WorkflowComponent.STEP);
        step.setRole(UserRole.PROCUREMENT_OFFICER);
        step.setName("Procurement Step");

        User requester = new User();
        requester.setTeam(null);
        testRequest.setUser(requester);

        User officer = new User();
        officer.setId(99L);
        officer.setRole(UserRole.PROCUREMENT_OFFICER);

        when(userRepository.findById(99L)).thenReturn(Optional.of(officer));

        assertDoesNotThrow(() ->
            workflowEngineService.checkAuthorization(testRequest, officer, step)
        );
    }

    @Test
    void checkAuthorization_ProcurementOfficerNullActorDepartment_ThrowsAccessDeniedException() {
        WorkflowStep step = new WorkflowStep();
        step.setWorkflowComponent(WorkflowComponent.STEP);
        step.setRole(UserRole.PROCUREMENT_OFFICER);
        step.setName("Procurement Step");

        User requester = new User();
        Department d1 = new Department();
        d1.setDepartmentId(11L);
        Team t = new Team();
        t.setDepartment(d1);
        requester.setTeam(t);
        testRequest.setUser(requester);

        User officer = new User();
        officer.setId(99L);
        officer.setRole(UserRole.PROCUREMENT_OFFICER);
        officer.setDepartment(null);

        when(userRepository.findById(99L)).thenReturn(Optional.of(officer));

        assertThrows(AccessDeniedException.class, () ->
            workflowEngineService.checkAuthorization(testRequest, officer, step)
        );
    }

    @Test
    void assertBudgetWithinSafetyBuffer_NullBudgetType_UsesFallbackIdentifier() {
        when(workflowTransitionRepository.findByFromStep(currentStep)).thenReturn(List.of(testTransition));

        InternalBudget budget = new InternalBudget();
        budget.setBudgetType(null);
        budget.setTotalAmount(new BigDecimal("1000"));
        budget.setSafetyBuffer(new BigDecimal("10"));
        budget.setActualSpend(new BigDecimal("950"));
        budget.setBudgetName(null);
        testRequest.setBudget(budget);

        WorkflowStateException ex = assertThrows(WorkflowStateException.class,
                () -> workflowEngineService.moveToNextStep(testRequest, testActor, null));

        assertEquals("Budget of : Budget exhausted including safety buffer.", ex.getMessage());
    }

    @Test
    void getNextStep_NullTransitionsList_ReturnsNull() {
        testRequest.setState(RequestStatus.ACTIVE);
        testRequest.setCurrentStep(currentStep);

        when(workflowTransitionRepository.findByFromStep(currentStep)).thenReturn(null);

        WorkflowStep result = workflowEngineService.getNextStep(testRequest);
        assertNull(result);
    }

    @Test
    void getNextStep_NoConditionsMet_ReturnsNull() {
        testRequest.setState(RequestStatus.ACTIVE);
        testRequest.setCurrentStep(currentStep);

        WorkflowTransition t = new WorkflowTransition();
        t.setFromStep(currentStep);
        t.setToStep(nextStep);
        t.setConditionExpression("totalQuantity > 100");

        when(workflowTransitionRepository.findByFromStep(currentStep)).thenReturn(List.of(t));

        WorkflowStep result = workflowEngineService.getNextStep(testRequest);
        assertNull(result);
    }
}
