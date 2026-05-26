package com.veritas.backend.workflow;

import com.veritas.backend.audit.entity.AuditLog;
import com.veritas.backend.audit.repository.AuditLogRepository;
import com.veritas.backend.audit.service.impl.AuditServiceImpl;
import com.veritas.backend.budget.entity.InternalBudget;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
        testActor.setRole(UserRole.REQUESTER);

        currentStep = new WorkflowStep();
        currentStep.setWorkflowComponent(WorkflowComponent.START_EVENT);
        currentStep.setRole(UserRole.REQUESTER);
        currentStep.setName("Start");

        nextStep = new WorkflowStep();
        nextStep.setWorkflowComponent(WorkflowComponent.END_EVENT);
        nextStep.setName("End");

        testRequest = new Request();
        testRequest.setCurrentStepID(currentStep);
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
        testRequest.setBudgetID(budget);

        WorkflowStateException ex = assertThrows(WorkflowStateException.class,
                () -> workflowEngineService.moveToNextStep(testRequest, testActor, null));

        assertEquals("Budget exhausted including safety buffer.", ex.getMessage());
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
        testRequest.setBudgetID(budget);

        assertDoesNotThrow(() -> workflowEngineService.moveToNextStep(testRequest, testActor, null));
    }

    @Test
    void moveToNextStep_PdfAttachmentRequired_ThrowsException() {
        when(workflowTransitionRepository.findByFromStep(currentStep)).thenReturn(List.of(testTransition));

        TransitionRule rule = new TransitionRule();
        rule.setIsPdfRequired(true);
        rule.setOptionalFailureMessage("Must provide PDF");
        when(transitionRuleRepository.findByTransition(testTransition)).thenReturn(Optional.of(rule));

        WorkflowStateException ex = assertThrows(WorkflowStateException.class,
                () -> workflowEngineService.moveToNextStep(testRequest, testActor, null));
        assertEquals("Must provide PDF", ex.getMessage());
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

        when(quoteRepository.findByRequestRequestID(testRequest.getRequestID()))
            .thenReturn(List.of(quote));

        WorkflowStateException ex = assertThrows(WorkflowStateException.class,
                () -> workflowEngineService.moveToNextStep(testRequest, testActor, null));
        assertEquals("Not enough vendors", ex.getMessage());
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

        when(quoteRepository.findByRequestRequestID(testRequest.getRequestID()))
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
        testRequest.setUserID(reqUser);

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
        assertEquals("You must be in the same department to act on unassigned steps.", ex.getMessage());
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
        testRequest.setUserID(reqUser);

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
        testRequest.setUserID(reqUser);

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
        testRequest.setUserID(reqUser);

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
        rule.setOptionalFailureMessage("Must provide new PDF");
        when(transitionRuleRepository.findByTransition(testTransition)).thenReturn(Optional.of(rule));

        java.time.LocalDateTime entryTime = java.time.LocalDateTime.of(2026, 5, 26, 14, 0, 0);
        com.veritas.backend.audit.entity.AuditLog entryLog = com.veritas.backend.audit.entity.AuditLog.builder()
                .newStep(currentStep)
                .timestamp(entryTime)
                .build();
        when(auditLogRepository.findFirstByRequestAndNewStepOrderByTimestampAsc(testRequest, currentStep))
                .thenReturn(Optional.of(entryLog));

        Attachment oldPdf = new Attachment();
        oldPdf.setFileType("application/pdf");
        oldPdf.setUploadedAt(java.time.LocalDateTime.of(2026, 5, 26, 13, 59, 0));
        testRequest.getAttachments().add(oldPdf);

        WorkflowStateException ex = assertThrows(WorkflowStateException.class,
                () -> workflowEngineService.moveToNextStep(testRequest, testActor, null));
        assertEquals("Must provide new PDF", ex.getMessage());
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
}