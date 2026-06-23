package com.veritas.backend.workflow.service;

import com.veritas.backend.department.entity.Department;
import com.veritas.backend.department.repository.DepartmentRepository;
import com.veritas.backend.team.entity.Team;
import com.veritas.backend.user.entity.User;
import com.veritas.backend.user.entity.UserRole;
import com.veritas.backend.user.repository.UserRepository;
import com.veritas.backend.workflow.dto.WorkflowDto;
import com.veritas.backend.workflow.dto.WorkflowEditDto;
import com.veritas.backend.workflow.dto.WorkflowSaveDto;
import com.veritas.backend.workflow.entity.TransitionRule;
import com.veritas.backend.workflow.entity.WorkflowDefinition;
import com.veritas.backend.workflow.entity.WorkflowStep;
import com.veritas.backend.workflow.entity.WorkflowTransition;
import com.veritas.backend.workflow.mapper.WorkflowMapper;
import com.veritas.backend.workflow.repository.TransitionRuleRepository;
import com.veritas.backend.workflow.repository.WorkflowDefinitionRepository;
import com.veritas.backend.workflow.repository.WorkflowStepRepository;
import com.veritas.backend.workflow.repository.WorkflowTransitionRepository;
import com.veritas.backend.workflow.service.impl.WorkflowServiceImpl;
import com.veritas.backend.workflow.validation.BpmnValidationException;
import com.veritas.backend.workflow.validation.BpmnValidationResult;
import com.veritas.backend.workflow.validation.BpmnValidator;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.PageImpl;

@ExtendWith(MockitoExtension.class)
class WorkflowServiceUnitTest {

    @Mock
    private WorkflowDefinitionRepository workflowDefinitionRepository;

    @Mock
    private WorkflowStepRepository workflowStepRepository;

    @Mock
    private WorkflowTransitionRepository workflowTransitionRepository;

    @Mock
    private TransitionRuleRepository transitionRuleRepository;

    @Mock
    private WorkflowMapper workflowMapper;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private UserRepository userRepository;

    @Spy
    private BpmnValidator bpmnValidator;

    @Captor
    private ArgumentCaptor<WorkflowDefinition> workflowCaptor;

    @Captor
    private ArgumentCaptor<Iterable<WorkflowStep>> stepsCaptor;

    @Captor
    private ArgumentCaptor<Iterable<WorkflowTransition>> transitionsCaptor;

    @Captor
    private ArgumentCaptor<Iterable<TransitionRule>> transitionRulesCaptor;

    @InjectMocks
    private WorkflowServiceImpl workflowService;

    private final User financeUser = User.builder().role(UserRole.FINANCE_OFFICER).build();

    private static final String VALID_BPMN_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" " +
            "id=\"Definitions_1\" targetNamespace=\"http://bpmn.io/schema/bpmn\">\n" +
            "  <bpmn:process id=\"Process_1\" name=\"Test Workflow\" isExecutable=\"true\">\n" +
            "    <bpmn:startEvent id=\"StartEvent_1\" name=\"Start\" />\n" +
            "    <bpmn:task id=\"Task_1\" name=\"Approval Step\">\n" +
            "      <bpmn:documentation>[ASSIGNEE]FINANCE_OFFICER</bpmn:documentation>\n" +
            "    </bpmn:task>\n" +
            "    <bpmn:endEvent id=\"EndEvent_1\" name=\"End\" />\n" +
            "    <bpmn:sequenceFlow id=\"Flow_1\" sourceRef=\"StartEvent_1\" targetRef=\"Task_1\" />\n" +
            "    <bpmn:sequenceFlow id=\"Flow_2\" sourceRef=\"Task_1\" targetRef=\"EndEvent_1\" />\n" +
            "  </bpmn:process>\n" +
            "</bpmn:definitions>";

    @Test
    void GetWorkflow_ExistingId_ReturnsWorkflowDto() {
        WorkflowDefinition wd = new WorkflowDefinition();
        wd.setId(1L);
        wd.setName("Test Workflow");

        WorkflowDto dto = new WorkflowDto(1L, "Test Workflow", VALID_BPMN_XML, 1L, "desc", true, null);

        when(workflowDefinitionRepository.findById(1L)).thenReturn(Optional.of(wd));
        when(workflowMapper.toWorkflowDto(wd)).thenReturn(dto);

        WorkflowDto result = workflowService.getWorkflow(1L, financeUser);

        assertEquals(dto, result);
        verify(workflowDefinitionRepository).findById(1L);
    }

    @Test
    void GetWorkflow_NonExistingId_ThrowsEntityNotFoundException() {
        when(workflowDefinitionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> workflowService.getWorkflow(99L, financeUser));
    }

    @Test
    void DeleteWorkflow_ExistingId_DeactivatesWorkflow() {
        WorkflowDefinition wd = new WorkflowDefinition();
        wd.setId(1L);
        wd.setName("Test Workflow");
        wd.setIsActive(true);

        when(workflowDefinitionRepository.findById(1L)).thenReturn(Optional.of(wd));

        workflowService.deleteWorkflow(1L);

        assertAll("WorkflowDefinition deactivation state",
                () -> assertFalse(wd.getIsActive()),
                () -> assertNotNull(wd.getDeactivatedAt())
        );
        verify(workflowDefinitionRepository).save(wd);
    }

    @Test
    void DeleteWorkflow_NonExistingId_ThrowsEntityNotFoundException() {
        when(workflowDefinitionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> workflowService.deleteWorkflow(99L));
        verify(workflowDefinitionRepository, never()).save(any());
    }

    @Test
    void CreateWorkflow_ValidInput_SavesAndReturnsWorkflowDto() {
        WorkflowSaveDto saveDto = new WorkflowSaveDto(VALID_BPMN_XML, null);

        WorkflowDefinition wd = new WorkflowDefinition();
        wd.setId(1L);
        wd.setName("Test Workflow");

        WorkflowDto dto = new WorkflowDto(1L, "Test Workflow", VALID_BPMN_XML, 1L, "desc", true, null);

        when(workflowMapper.toWorkflowDto(any(WorkflowDefinition.class))).thenReturn(dto);

        WorkflowDto result = workflowService.createWorkflow(saveDto);

        assertEquals(dto, result);
        verify(workflowDefinitionRepository).save(any(WorkflowDefinition.class));
    }

    @Test
    void EditWorkflow_ValidInput_UpdatesOldAndSavesNewWorkflow() {
        WorkflowDefinition oldWd = new WorkflowDefinition();
        oldWd.setId(1L);
        oldWd.setName("Old Name");
        oldWd.setIsActive(true);
        oldWd.setVersion(1);

        WorkflowEditDto editDto = new WorkflowEditDto(VALID_BPMN_XML, null);
        WorkflowDto dto = new WorkflowDto(2L, "Test Workflow", VALID_BPMN_XML, 2L, "desc", true, null);

        when(workflowDefinitionRepository.findById(1L)).thenReturn(Optional.of(oldWd));
        when(workflowMapper.toWorkflowDto(any(WorkflowDefinition.class))).thenReturn(dto);

        WorkflowDto result = workflowService.editWorkflow(1L, editDto);

        assertEquals(dto, result);
        verify(workflowDefinitionRepository, times(2)).save(any(WorkflowDefinition.class));
    }

    @Test
    void CreateWorkflow_WithAdministratorAssignee_ThrowsValidationException() {
        String xml = VALID_BPMN_XML.replace("<bpmn:documentation>[ASSIGNEE]FINANCE_OFFICER</bpmn:documentation>",
                "<bpmn:documentation>[ASSIGNEE]ADMINISTRATOR</bpmn:documentation>");
        WorkflowSaveDto saveDto = new WorkflowSaveDto(xml, null);

        BpmnValidationException ex = assertThrows(BpmnValidationException.class, () -> workflowService.createWorkflow(saveDto));
        assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("cannot be assigned to ADMINISTRATOR")));
        
        verify(workflowDefinitionRepository, never()).save(any(WorkflowDefinition.class));
    }

    @Test
    void CreateWorkflow_WithTaskDescription_SetsDescription() {
        String xml = VALID_BPMN_XML.replace("<bpmn:documentation>[ASSIGNEE]FINANCE_OFFICER</bpmn:documentation>",
                "<bpmn:documentation>[ASSIGNEE]FINANCE_OFFICER</bpmn:documentation>\n" +
                "  <bpmn:documentation>Task Description Text</bpmn:documentation>");
        WorkflowSaveDto saveDto = new WorkflowSaveDto(xml, null);
        when(workflowMapper.toWorkflowDto(any(WorkflowDefinition.class))).thenReturn(new WorkflowDto(1L, "", "", 1L, "", true, null));

        workflowService.createWorkflow(saveDto);

        verify(workflowDefinitionRepository).save(any(WorkflowDefinition.class));
        verify(workflowStepRepository).saveAll(stepsCaptor.capture());
        Iterable<WorkflowStep> savedSteps = stepsCaptor.getValue();
        
        List<WorkflowStep> stepsList = new ArrayList<>();
        savedSteps.forEach(stepsList::add);
        
        assertEquals("Task Description Text", stepsList.stream().filter(s -> "Approval Step".equals(s.getName())).findFirst().get().getDescription());
    }

    @Test
    void CreateWorkflow_WithMultipleTransitionRules_PersistsAllRules() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" " +
                "xmlns:veritas=\"http://veritas\" " +
                "id=\"Definitions_1\" targetNamespace=\"http://bpmn.io/schema/bpmn\">\n" +
                "  <bpmn:process id=\"Process_1\" name=\"Test Workflow\" isExecutable=\"true\">\n" +
                "    <bpmn:startEvent id=\"StartEvent_1\" name=\"Start\" />\n" +
                "    <bpmn:task id=\"Task_1\" name=\"Approval Step A\">\n" +
                "      <bpmn:documentation>[ASSIGNEE]FINANCE_OFFICER</bpmn:documentation>\n" +
                "    </bpmn:task>\n" +
                "    <bpmn:task id=\"Task_2\" name=\"Approval Step B\">\n" +
                "      <bpmn:documentation>[ASSIGNEE]FINANCE_OFFICER</bpmn:documentation>\n" +
                "    </bpmn:task>\n" +
                "    <bpmn:endEvent id=\"EndEvent_1\" name=\"End\" />\n" +
                "    <bpmn:sequenceFlow id=\"Flow_1\" sourceRef=\"StartEvent_1\" targetRef=\"Task_1\" />\n" +
                "    <bpmn:sequenceFlow id=\"Flow_2\" sourceRef=\"Task_1\" targetRef=\"Task_2\">\n" +
                "      <bpmn:extensionElements>\n" +
                "        <veritas:transitionRule minRequiredVendors=\"3\" requiredFileTypes=\"pdf,csv\" />\n" +
                "      </bpmn:extensionElements>\n" +
                "    </bpmn:sequenceFlow>\n" +
                "    <bpmn:sequenceFlow id=\"Flow_3\" sourceRef=\"Task_2\" targetRef=\"EndEvent_1\" />\n" +
                "  </bpmn:process>\n" +
                "</bpmn:definitions>";
        WorkflowSaveDto saveDto = new WorkflowSaveDto(xml, null);
        when(workflowMapper.toWorkflowDto(any(WorkflowDefinition.class))).thenReturn(new WorkflowDto(1L, "", "", 1L, "", true, null));

        workflowService.createWorkflow(saveDto);

        verify(transitionRuleRepository).saveAll(transitionRulesCaptor.capture());
        Iterable<TransitionRule> savedRules = transitionRulesCaptor.getValue();

        List<TransitionRule> rulesList = new ArrayList<>();
        savedRules.forEach(rulesList::add);

        assertEquals(1, rulesList.size());
        TransitionRule rule = rulesList.getFirst();
        assertAll("TransitionRule fields",
                () -> assertEquals(3, rule.getMinRequiredVendors()),
                () -> assertEquals("pdf,csv", rule.getRequiredFileTypes())
        );
    }

    @Test
    void CreateWorkflow_WithoutTransitionRule_PersistsNoRules() {
        WorkflowSaveDto saveDto = new WorkflowSaveDto(VALID_BPMN_XML, null);
        when(workflowMapper.toWorkflowDto(any(WorkflowDefinition.class))).thenReturn(new WorkflowDto(1L, "", "", 1L, "", true, null));

        workflowService.createWorkflow(saveDto);

        verify(transitionRuleRepository).saveAll(transitionRulesCaptor.capture());
        Iterable<TransitionRule> savedRules = transitionRulesCaptor.getValue();

        List<TransitionRule> rulesList = new ArrayList<>();
        savedRules.forEach(rulesList::add);

        assertTrue(rulesList.isEmpty());
    }

    //AI GENERATED

    @Test
    void GetAllWorkflows_WithParams_ReturnsMappedPage() {
        // 1. Setup
        Pageable pageable = PageRequest.of(0, 10);
        String search = "test";
        Boolean isActive = true;

        WorkflowDefinition wd = new WorkflowDefinition();
        wd.setName("Test Workflow");

        Page<WorkflowDefinition> page = new PageImpl<>(List.of(wd));
        WorkflowDto dto = new WorkflowDto(1L, "Test Workflow", VALID_BPMN_XML, 1L, "desc", true, null);

        // 2. Mocking
        when(workflowDefinitionRepository.findAllFiltered(any(), any(), any(), any(), eq(pageable)))
                .thenReturn(page);
        when(workflowMapper.toWorkflowDto(wd)).thenReturn(dto);

        // 3. Execution
        Page<WorkflowDto> result = workflowService.getAllWorkflows(pageable, search, isActive, null);

        // 4. Verification
        assertAll("Workflow page contents",
                () -> assertEquals(1, result.getContent().size()),
                () -> assertEquals("Test Workflow", result.getContent().get(0).name())
        );
        verify(workflowDefinitionRepository).findAllFiltered(any(), any(), any(), any(), eq(pageable));
    }

    @Test
    void GetAllWorkflows_EmptyResults_ReturnsEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        when(workflowDefinitionRepository.findAllFiltered(any(), any(), any(), any(), any()))
                .thenReturn(Page.empty());

        Page<WorkflowDto> result = workflowService.getAllWorkflows(pageable, null, null, null);

        assertTrue(result.isEmpty());
        verify(workflowMapper, never()).toWorkflowDto(any());
    }

    @Test
    void GetAllWorkflows_WithRequester_FiltersByDepartment() {
        Pageable pageable = PageRequest.of(0, 10);
        
        User requester = new User();
        requester.setId(99L);
        requester.setRole(UserRole.REQUESTER);
        
        Department dept = new Department();
        dept.setDepartmentId(12L);
        
        Team team = new Team();
        team.setDepartment(dept);
        requester.setTeam(team);
        
        WorkflowDefinition wd = new WorkflowDefinition();
        wd.setName("Department Workflow");
        
        Page<WorkflowDefinition> page = new PageImpl<>(List.of(wd));
        WorkflowDto dto = new WorkflowDto(1L, "Department Workflow", VALID_BPMN_XML, 12L, "desc", true, null);
        
        when(userRepository.findById(99L)).thenReturn(Optional.of(requester));
        when(workflowDefinitionRepository.findAllFiltered(any(), any(), eq(12L), eq(true), eq(pageable)))
                .thenReturn(page);
        when(workflowMapper.toWorkflowDto(wd)).thenReturn(dto);
        
        Page<WorkflowDto> result = workflowService.getAllWorkflows(pageable, null, null, requester);
        
        assertAll("Workflow page contents by requester",
                () -> assertEquals(1, result.getContent().size()),
                () -> assertEquals("Department Workflow", result.getContent().get(0).name())
        );
        verify(workflowDefinitionRepository).findAllFiltered(any(), any(), eq(12L), eq(true), eq(pageable));
    }

    //AI-GENERATED

    @Test
    void CreateWorkflow_WithAutomatedApproval_SetsIsAutomatedApproval() {
        String xml = VALID_BPMN_XML.replace(
                "<bpmn:documentation>[ASSIGNEE]FINANCE_OFFICER</bpmn:documentation>",
                "<bpmn:documentation>[ASSIGNEE]FINANCE_OFFICER</bpmn:documentation>\n" +
                "      <bpmn:documentation>[AUTO_APPROVE]true</bpmn:documentation>");
        WorkflowSaveDto saveDto = new WorkflowSaveDto(xml, null);
        when(workflowMapper.toWorkflowDto(any(WorkflowDefinition.class))).thenReturn(new WorkflowDto(1L, "", "", 1L, "", true, null));

        workflowService.createWorkflow(saveDto);

        verify(workflowDefinitionRepository).save(any(WorkflowDefinition.class));
        verify(workflowStepRepository).saveAll(stepsCaptor.capture());
        Iterable<WorkflowStep> savedSteps = stepsCaptor.getValue();
        
        List<WorkflowStep> stepsList = new ArrayList<>();
        savedSteps.forEach(stepsList::add);
        
        assertEquals(3, stepsList.size());
        WorkflowStep step = stepsList.stream().filter(s -> "Approval Step".equals(s.getName())).findFirst().get();
        assertTrue(step.getIsAutomatedApproval());
    }

    @Test
    void GetWorkflow_AsRequester_MatchingDepartment_ReturnsWorkflowDto() {
        User requester = User.builder().id(99L).role(UserRole.REQUESTER).build();
        Department dept = new Department();
        dept.setDepartmentId(12L);
        User fullUser = User.builder().id(99L).role(UserRole.REQUESTER).department(dept).build();

        WorkflowDefinition wd = new WorkflowDefinition();
        wd.setId(1L);
        wd.setName("Test Workflow");
        wd.setDepartment(dept);

        WorkflowDto dto = new WorkflowDto(1L, "Test Workflow", VALID_BPMN_XML, 12L, "desc", true, null);

        when(workflowDefinitionRepository.findById(1L)).thenReturn(Optional.of(wd));
        when(userRepository.findById(99L)).thenReturn(Optional.of(fullUser));
        when(workflowMapper.toWorkflowDto(wd)).thenReturn(dto);

        WorkflowDto result = workflowService.getWorkflow(1L, requester);
        assertEquals(dto, result);
    }

    @Test
    void GetWorkflow_AsRequester_MismatchingDepartment_ThrowsAccessDeniedException() {
        User requester = User.builder().id(99L).role(UserRole.REQUESTER).build();
        Department userDept = new Department();
        userDept.setDepartmentId(12L);
        User fullUser = User.builder().id(99L).role(UserRole.REQUESTER).department(userDept).build();

        Department workflowDept = new Department();
        workflowDept.setDepartmentId(99L);
        WorkflowDefinition wd = new WorkflowDefinition();
        wd.setId(1L);
        wd.setName("Test Workflow");
        wd.setDepartment(workflowDept);

        when(workflowDefinitionRepository.findById(1L)).thenReturn(Optional.of(wd));
        when(userRepository.findById(99L)).thenReturn(Optional.of(fullUser));

        assertThrows(AccessDeniedException.class, () -> workflowService.getWorkflow(1L, requester));
    }

    @Test
    void GetWorkflow_AsRequester_NullWorkflowDepartment_ReturnsWorkflowDto() {
        User requester = User.builder().id(99L).role(UserRole.REQUESTER).build();
        Department userDept = new Department();
        userDept.setDepartmentId(12L);
        User fullUser = User.builder().id(99L).role(UserRole.REQUESTER).department(userDept).build();

        WorkflowDefinition wd = new WorkflowDefinition();
        wd.setId(1L);
        wd.setName("Test Workflow");
        wd.setDepartment(null);

        WorkflowDto dto = new WorkflowDto(1L, "Test Workflow", VALID_BPMN_XML, null, "desc", true, null);

        when(workflowDefinitionRepository.findById(1L)).thenReturn(Optional.of(wd));
        when(userRepository.findById(99L)).thenReturn(Optional.of(fullUser));
        when(workflowMapper.toWorkflowDto(wd)).thenReturn(dto);

        WorkflowDto result = workflowService.getWorkflow(1L, requester);
        assertEquals(dto, result);
    }

    @Test
    void GetWorkflow_AsRequester_MatchingDepartmentViaTeam_ReturnsWorkflowDto() {
        User requester = User.builder().id(99L).role(UserRole.REQUESTER).build();
        Department dept = new Department();
        dept.setDepartmentId(12L);
        Team team = new Team();
        team.setDepartment(dept);
        User fullUser = User.builder().id(99L).role(UserRole.REQUESTER).team(team).build();

        WorkflowDefinition wd = new WorkflowDefinition();
        wd.setId(1L);
        wd.setName("Test Workflow");
        wd.setDepartment(dept);

        WorkflowDto dto = new WorkflowDto(1L, "Test Workflow", VALID_BPMN_XML, 12L, "desc", true, null);

        when(workflowDefinitionRepository.findById(1L)).thenReturn(Optional.of(wd));
        when(userRepository.findById(99L)).thenReturn(Optional.of(fullUser));
        when(workflowMapper.toWorkflowDto(wd)).thenReturn(dto);

        WorkflowDto result = workflowService.getWorkflow(1L, requester);
        assertEquals(dto, result);
    }

    @Test
    void GetWorkflow_AsRequester_UserNotFound_ThrowsEntityNotFoundException() {
        User requester = User.builder().id(99L).role(UserRole.REQUESTER).build();

        WorkflowDefinition wd = new WorkflowDefinition();
        wd.setId(1L);
        wd.setName("Test Workflow");

        when(workflowDefinitionRepository.findById(1L)).thenReturn(Optional.of(wd));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> workflowService.getWorkflow(1L, requester));
    }

    @Test
    void GetAllWorkflows_WithRequester_FiltersByDirectDepartment() {
        Pageable pageable = PageRequest.of(0, 10);
        User requester = User.builder().id(99L).role(UserRole.REQUESTER).build();
        Department dept = new Department();
        dept.setDepartmentId(12L);
        User fullUser = User.builder().id(99L).role(UserRole.REQUESTER).department(dept).build();

        WorkflowDefinition wd = new WorkflowDefinition();
        wd.setName("Department Workflow");

        Page<WorkflowDefinition> page = new PageImpl<>(List.of(wd));
        WorkflowDto dto = new WorkflowDto(1L, "Department Workflow", VALID_BPMN_XML, 12L, "desc", true, null);

        when(userRepository.findById(99L)).thenReturn(Optional.of(fullUser));
        when(workflowDefinitionRepository.findAllFiltered(any(), any(), eq(12L), eq(true), eq(pageable)))
                .thenReturn(page);
        when(workflowMapper.toWorkflowDto(wd)).thenReturn(dto);

        Page<WorkflowDto> result = workflowService.getAllWorkflows(pageable, null, null, requester);

        assertAll("Workflow page contents by requester direct department",
                () -> assertEquals(1, result.getContent().size()),
                () -> assertEquals("Department Workflow", result.getContent().get(0).name())
        );
    }

    @Test
    void GetAllWorkflows_WithRequester_UserNotFound_UsesAuthUserFallback() {
        Pageable pageable = PageRequest.of(0, 10);
        Department dept = new Department();
        dept.setDepartmentId(12L);
        User requester = User.builder().id(99L).role(UserRole.REQUESTER).department(dept).build();

        WorkflowDefinition wd = new WorkflowDefinition();
        wd.setName("Department Workflow");

        Page<WorkflowDefinition> page = new PageImpl<>(List.of(wd));
        WorkflowDto dto = new WorkflowDto(1L, "Department Workflow", VALID_BPMN_XML, 12L, "desc", true, null);

        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        when(workflowDefinitionRepository.findAllFiltered(any(), any(), eq(12L), eq(true), eq(pageable)))
                .thenReturn(page);
        when(workflowMapper.toWorkflowDto(wd)).thenReturn(dto);

        Page<WorkflowDto> result = workflowService.getAllWorkflows(pageable, null, null, requester);

        assertAll("Workflow page contents by requester auth user fallback",
                () -> assertEquals(1, result.getContent().size()),
                () -> assertEquals("Department Workflow", result.getContent().get(0).name())
        );
    }

    @Test
    void EditWorkflow_InactiveWorkflow_ThrowsIllegalArgumentException() {
        WorkflowDefinition oldWd = new WorkflowDefinition();
        oldWd.setId(1L);
        oldWd.setName("Old Name");
        oldWd.setIsActive(false);

        WorkflowEditDto editDto = new WorkflowEditDto(VALID_BPMN_XML, null);

        when(workflowDefinitionRepository.findById(1L)).thenReturn(Optional.of(oldWd));

        assertThrows(IllegalArgumentException.class, () -> workflowService.editWorkflow(1L, editDto));
        verify(workflowDefinitionRepository, never()).save(any(WorkflowDefinition.class));
    }

    @Test
    void CreateWorkflow_WithDepartmentId_SavesSuccessfully() {
        WorkflowSaveDto saveDto = new WorkflowSaveDto(VALID_BPMN_XML, 12L);
        Department dept = new Department();
        dept.setDepartmentId(12L);

        when(departmentRepository.findById(12L)).thenReturn(Optional.of(dept));
        when(workflowMapper.toWorkflowDto(any(WorkflowDefinition.class))).thenReturn(new WorkflowDto(1L, "", "", 12L, "", true, null));

        workflowService.createWorkflow(saveDto);

        verify(workflowDefinitionRepository).save(workflowCaptor.capture());
        assertEquals(dept, workflowCaptor.getValue().getDepartment());
    }

    @Test
    void CreateWorkflow_DepartmentNotFound_ThrowsEntityNotFoundException() {
        WorkflowSaveDto saveDto = new WorkflowSaveDto(VALID_BPMN_XML, 99L);
        when(departmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> workflowService.createWorkflow(saveDto));
    }

    @Test
    void EditWorkflow_WithDepartmentId_SavesSuccessfully() {
        WorkflowDefinition oldWd = new WorkflowDefinition();
        oldWd.setId(1L);
        oldWd.setName("Old Name");
        oldWd.setIsActive(true);
        oldWd.setVersion(1);

        WorkflowEditDto editDto = new WorkflowEditDto(VALID_BPMN_XML, 12L);
        Department dept = new Department();
        dept.setDepartmentId(12L);

        when(workflowDefinitionRepository.findById(1L)).thenReturn(Optional.of(oldWd));
        when(departmentRepository.findById(12L)).thenReturn(Optional.of(dept));
        when(workflowMapper.toWorkflowDto(any(WorkflowDefinition.class))).thenReturn(new WorkflowDto(2L, "", "", 12L, "", true, null));

        workflowService.editWorkflow(1L, editDto);

        verify(workflowDefinitionRepository, times(2)).save(workflowCaptor.capture());
        List<WorkflowDefinition> saved = workflowCaptor.getAllValues();
        assertEquals(dept, saved.get(1).getDepartment());
    }

    @Test
    void EditWorkflow_WithoutDepartmentId_UsesOldDepartment() {
        WorkflowDefinition oldWd = new WorkflowDefinition();
        oldWd.setId(1L);
        oldWd.setName("Old Name");
        oldWd.setIsActive(true);
        oldWd.setVersion(1);
        Department oldDept = new Department();
        oldDept.setDepartmentId(15L);
        oldWd.setDepartment(oldDept);

        WorkflowEditDto editDto = new WorkflowEditDto(VALID_BPMN_XML, null);

        when(workflowDefinitionRepository.findById(1L)).thenReturn(Optional.of(oldWd));
        when(workflowMapper.toWorkflowDto(any(WorkflowDefinition.class))).thenReturn(new WorkflowDto(2L, "", "", 15L, "", true, null));

        workflowService.editWorkflow(1L, editDto);

        verify(workflowDefinitionRepository, times(2)).save(workflowCaptor.capture());
        List<WorkflowDefinition> saved = workflowCaptor.getAllValues();
        assertEquals(oldDept, saved.get(1).getDepartment());
    }

    @Test
    void GetAllWorkflows_WithSearchQuery_FormatsQuery() {
        Pageable pageable = PageRequest.of(0, 10);
        when(workflowDefinitionRepository.findAllFiltered("%alex%", true, null, true, pageable)).thenReturn(Page.empty());

        workflowService.getAllWorkflows(pageable, "  ALEX  ", true, null);

        verify(workflowDefinitionRepository).findAllFiltered("%alex%", true, null, true, pageable);
    }

    @Test
    void GetAllWorkflows_WithProcurementOfficer_FiltersByDirectDepartment() {
        Pageable pageable = PageRequest.of(0, 10);
        User officer = User.builder().id(99L).role(UserRole.PROCUREMENT_OFFICER).build();
        Department dept = new Department();
        dept.setDepartmentId(12L);
        User fullUser = User.builder().id(99L).role(UserRole.PROCUREMENT_OFFICER).department(dept).build();

        when(userRepository.findById(99L)).thenReturn(Optional.of(fullUser));
        when(workflowDefinitionRepository.findAllFiltered(any(), any(), eq(12L), eq(true), eq(pageable)))
                .thenReturn(Page.empty());

        workflowService.getAllWorkflows(pageable, null, null, officer);

        verify(workflowDefinitionRepository).findAllFiltered(any(), any(), eq(12L), eq(true), eq(pageable));
    }

    @Test
    void GetAllWorkflows_WithProcurementOfficer_FiltersByTeamDepartment() {
        Pageable pageable = PageRequest.of(0, 10);
        User officer = User.builder().id(99L).role(UserRole.PROCUREMENT_OFFICER).build();
        Department dept = new Department();
        dept.setDepartmentId(12L);
        Team team = new Team();
        team.setDepartment(dept);
        User fullUser = User.builder().id(99L).role(UserRole.PROCUREMENT_OFFICER).team(team).build();

        when(userRepository.findById(99L)).thenReturn(Optional.of(fullUser));
        when(workflowDefinitionRepository.findAllFiltered(any(), any(), eq(12L), eq(true), eq(pageable)))
                .thenReturn(Page.empty());

        workflowService.getAllWorkflows(pageable, null, null, officer);

        verify(workflowDefinitionRepository).findAllFiltered(any(), any(), eq(12L), eq(true), eq(pageable));
    }

    @Test
    void CreateWorkflow_WithAllTransitionRulesAttributes_PersistsSuccessfully() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" " +
                "xmlns:veritas=\"http://veritas\" " +
                "id=\"Definitions_1\" targetNamespace=\"http://bpmn.io/schema/bpmn\">\n" +
                "  <bpmn:process id=\"Process_1\" name=\"Test Workflow\" isExecutable=\"true\">\n" +
                "    <bpmn:startEvent id=\"StartEvent_1\" name=\"Start\" />\n" +
                "    <bpmn:task id=\"Task_1\" name=\"Approval Step A\">\n" +
                "      <bpmn:documentation>[ASSIGNEE]FINANCE_OFFICER</bpmn:documentation>\n" +
                "    </bpmn:task>\n" +
                "    <bpmn:task id=\"Task_2\" name=\"Approval Step B\">\n" +
                "      <bpmn:documentation>[ASSIGNEE]PROCUREMENT_OFFICER</bpmn:documentation>\n" +
                "    </bpmn:task>\n" +
                "    <bpmn:endEvent id=\"EndEvent_1\" name=\"End\" />\n" +
                "    <bpmn:sequenceFlow id=\"Flow_1\" sourceRef=\"StartEvent_1\" targetRef=\"Task_1\" />\n" +
                "    <bpmn:sequenceFlow id=\"Flow_2\" sourceRef=\"Task_1\" targetRef=\"Task_2\">\n" +
                "      <bpmn:extensionElements>\n" +
                "        <veritas:transitionRule minRequiredVendors=\"3\" minVendorReliabilityScore=\"4.5\" requiredFileTypes=\"pdf,image\" advancedRule=\"totalQuantity &gt; 10\" />\n" +
                "      </bpmn:extensionElements>\n" +
                "    </bpmn:sequenceFlow>\n" +
                "    <bpmn:sequenceFlow id=\"Flow_3\" sourceRef=\"Task_2\" targetRef=\"EndEvent_1\" />\n" +
                "  </bpmn:process>\n" +
                "</bpmn:definitions>";
        WorkflowSaveDto saveDto = new WorkflowSaveDto(xml, null);
        when(workflowMapper.toWorkflowDto(any(WorkflowDefinition.class))).thenReturn(new WorkflowDto(1L, "", "", 1L, "", true, null));

        workflowService.createWorkflow(saveDto);

        verify(transitionRuleRepository).saveAll(transitionRulesCaptor.capture());
        TransitionRule rule = transitionRulesCaptor.getValue().iterator().next();

        assertAll("TransitionRule fields with all attributes",
                () -> assertEquals(3, rule.getMinRequiredVendors()),
                () -> assertEquals(4.5, rule.getMinVendorReliabilityScore()),
                () -> assertEquals("pdf,image", rule.getRequiredFileTypes()),
                () -> assertEquals("totalQuantity > 10", rule.getAdvancedRule())
        );
    }

    @Test
    void CreateWorkflow_WithEmptyTransitionRulesAttributes_PersistsSuccessfully() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" " +
                "xmlns:veritas=\"http://veritas\" " +
                "id=\"Definitions_1\" targetNamespace=\"http://bpmn.io/schema/bpmn\">\n" +
                "  <bpmn:process id=\"Process_1\" name=\"Test Workflow\" isExecutable=\"true\">\n" +
                "    <bpmn:startEvent id=\"StartEvent_1\" name=\"Start\" />\n" +
                "    <bpmn:task id=\"Task_1\" name=\"Approval Step A\">\n" +
                "      <bpmn:documentation>[ASSIGNEE]FINANCE_OFFICER</bpmn:documentation>\n" +
                "    </bpmn:task>\n" +
                "    <bpmn:task id=\"Task_2\" name=\"Approval Step B\">\n" +
                "      <bpmn:documentation>[ASSIGNEE]PROCUREMENT_OFFICER</bpmn:documentation>\n" +
                "    </bpmn:task>\n" +
                "    <bpmn:endEvent id=\"EndEvent_1\" name=\"End\" />\n" +
                "    <bpmn:sequenceFlow id=\"Flow_1\" sourceRef=\"StartEvent_1\" targetRef=\"Task_1\" />\n" +
                "    <bpmn:sequenceFlow id=\"Flow_2\" sourceRef=\"Task_1\" targetRef=\"Task_2\">\n" +
                "      <bpmn:extensionElements>\n" +
                "        <veritas:transitionRule minRequiredVendors=\"\" minVendorReliabilityScore=\"\" advancedRule=\"\" />\n" +
                "      </bpmn:extensionElements>\n" +
                "    </bpmn:sequenceFlow>\n" +
                "    <bpmn:sequenceFlow id=\"Flow_3\" sourceRef=\"Task_2\" targetRef=\"EndEvent_1\" />\n" +
                "  </bpmn:process>\n" +
                "</bpmn:definitions>";
        WorkflowSaveDto saveDto = new WorkflowSaveDto(xml, null);
        when(workflowMapper.toWorkflowDto(any(WorkflowDefinition.class))).thenReturn(new WorkflowDto(1L, "", "", 1L, "", true, null));

        workflowService.createWorkflow(saveDto);

        verify(transitionRuleRepository).saveAll(transitionRulesCaptor.capture());
        TransitionRule rule = transitionRulesCaptor.getValue().iterator().next();

        assertAll("TransitionRule empty attributes",
                () -> assertEquals(0, rule.getMinRequiredVendors()),
                () -> assertNull(rule.getMinVendorReliabilityScore()),
                () -> assertNull(rule.getAdvancedRule())
        );
    }

    @Test
    void CreateWorkflow_WithOtherExtensionElements_IgnoresThem() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" " +
                "xmlns:veritas=\"http://veritas\" " +
                "id=\"Definitions_1\" targetNamespace=\"http://bpmn.io/schema/bpmn\">\n" +
                "  <bpmn:process id=\"Process_1\" name=\"Test Workflow\" isExecutable=\"true\">\n" +
                "    <bpmn:startEvent id=\"StartEvent_1\" name=\"Start\" />\n" +
                "    <bpmn:task id=\"Task_1\" name=\"Approval Step A\">\n" +
                "      <bpmn:documentation>[ASSIGNEE]FINANCE_OFFICER</bpmn:documentation>\n" +
                "    </bpmn:task>\n" +
                "    <bpmn:endEvent id=\"EndEvent_1\" name=\"End\" />\n" +
                "    <bpmn:sequenceFlow id=\"Flow_1\" sourceRef=\"StartEvent_1\" targetRef=\"Task_1\" />\n" +
                "    <bpmn:sequenceFlow id=\"Flow_2\" sourceRef=\"Task_1\" targetRef=\"EndEvent_1\">\n" +
                "      <bpmn:extensionElements>\n" +
                "        <veritas:someOtherElement attr=\"value\" />\n" +
                "      </bpmn:extensionElements>\n" +
                "    </bpmn:sequenceFlow>\n" +
                "  </bpmn:process>\n" +
                "</bpmn:definitions>";
        WorkflowSaveDto saveDto = new WorkflowSaveDto(xml, null);
        when(workflowMapper.toWorkflowDto(any(WorkflowDefinition.class))).thenReturn(new WorkflowDto(1L, "", "", 1L, "", true, null));

        workflowService.createWorkflow(saveDto);

        verify(transitionRuleRepository).saveAll(transitionRulesCaptor.capture());
        assertFalse(transitionRulesCaptor.getValue().iterator().hasNext());
    }

    @Test
    void CreateWorkflow_WithUnsupportedFlowNode_ThrowsIllegalStateException() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" " +
                "id=\"Definitions_1\" targetNamespace=\"http://bpmn.io/schema/bpmn\">\n" +
                "  <bpmn:process id=\"Process_1\" name=\"Test Workflow\" isExecutable=\"true\">\n" +
                "    <bpmn:startEvent id=\"StartEvent_1\" name=\"Start\" />\n" +
                "    <bpmn:subProcess id=\"SubProcess_1\" name=\"SubProcess\" />\n" +
                "    <bpmn:endEvent id=\"EndEvent_1\" name=\"End\" />\n" +
                "    <bpmn:sequenceFlow id=\"Flow_1\" sourceRef=\"StartEvent_1\" targetRef=\"SubProcess_1\" />\n" +
                "    <bpmn:sequenceFlow id=\"Flow_2\" sourceRef=\"SubProcess_1\" targetRef=\"EndEvent_1\" />\n" +
                "  </bpmn:process>\n" +
                "</bpmn:definitions>";
        WorkflowSaveDto saveDto = new WorkflowSaveDto(xml, null);
        doReturn(new BpmnValidationResult()).when(bpmnValidator).validate(anyString(), any(BpmnModelInstance.class));

        assertThrows(IllegalStateException.class, () -> workflowService.createWorkflow(saveDto));
    }
}

