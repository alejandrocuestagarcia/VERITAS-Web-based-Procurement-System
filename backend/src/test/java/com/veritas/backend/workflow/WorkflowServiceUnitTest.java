package com.veritas.backend.workflow;

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
import com.veritas.backend.workflow.validation.BpmnValidator;
import jakarta.persistence.EntityNotFoundException;
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

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
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

    private static final String VALID_BPMN_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" " +
            "id=\"Definitions_1\" targetNamespace=\"http://bpmn.io/schema/bpmn\">\n" +
            "  <bpmn:process id=\"Process_1\" name=\"Test Workflow\" isExecutable=\"true\">\n" +
            "    <bpmn:startEvent id=\"StartEvent_1\" name=\"Start\" />\n" +
            "    <bpmn:task id=\"Task_1\" name=\"Approval Step\" />\n" +
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

        WorkflowDto result = workflowService.getWorkflow(1L);

        assertThat(result).isEqualTo(dto);
        verify(workflowDefinitionRepository).findById(1L);
    }

    @Test
    void GetWorkflow_NonExistingId_ThrowsEntityNotFoundException() {
        when(workflowDefinitionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> workflowService.getWorkflow(99L));
    }

    @Test
    void DeleteWorkflow_ExistingId_DeactivatesWorkflow() {
        WorkflowDefinition wd = new WorkflowDefinition();
        wd.setId(1L);
        wd.setName("Test Workflow");
        wd.setIsActive(true);

        when(workflowDefinitionRepository.findById(1L)).thenReturn(Optional.of(wd));

        workflowService.deleteWorkflow(1L);

        assertFalse(wd.getIsActive());
        assertNotNull(wd.getDeactivatedAt());
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

        assertThat(result).isEqualTo(dto);
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

        assertThat(result).isEqualTo(dto);
        verify(workflowDefinitionRepository, times(2)).save(any(WorkflowDefinition.class));
    }

    @Test
    void CreateWorkflow_WithAssignee_SetsAssignedPerson() {
        String xml = VALID_BPMN_XML.replace("<bpmn:task id=\"Task_1\" name=\"Approval Step\" />",
                "<bpmn:task id=\"Task_1\" name=\"Approval Step\">\n" +
                "  <bpmn:documentation>[ASSIGNEE]ADMINISTRATOR</bpmn:documentation>\n" +
                "</bpmn:task>");
        WorkflowSaveDto saveDto = new WorkflowSaveDto(xml, null);
        when(workflowMapper.toWorkflowDto(any(WorkflowDefinition.class))).thenReturn(new WorkflowDto(1L, "", "", 1L, "", true, null));

        workflowService.createWorkflow(saveDto);

        verify(workflowDefinitionRepository).save(any(WorkflowDefinition.class));
        verify(workflowStepRepository).saveAll(stepsCaptor.capture());
        Iterable<WorkflowStep> savedSteps = stepsCaptor.getValue();
        
        java.util.List<WorkflowStep> stepsList = new java.util.ArrayList<>();
        savedSteps.forEach(stepsList::add);
        
        assertThat(stepsList).hasSize(3);
        assertThat(stepsList.stream().filter(s -> "Approval Step".equals(s.getName())).findFirst().get().getRole())
                .isEqualTo(UserRole.ADMINISTRATOR);
    }

    @Test
    void CreateWorkflow_WithTaskDescription_SetsDescription() {
        String xml = VALID_BPMN_XML.replace("<bpmn:task id=\"Task_1\" name=\"Approval Step\" />",
                "<bpmn:task id=\"Task_1\" name=\"Approval Step\">\n" +
                "  <bpmn:documentation>Task Description Text</bpmn:documentation>\n" +
                "</bpmn:task>");
        WorkflowSaveDto saveDto = new WorkflowSaveDto(xml, null);
        when(workflowMapper.toWorkflowDto(any(WorkflowDefinition.class))).thenReturn(new WorkflowDto(1L, "", "", 1L, "", true, null));

        workflowService.createWorkflow(saveDto);

        verify(workflowDefinitionRepository).save(any(WorkflowDefinition.class));
        verify(workflowStepRepository).saveAll(stepsCaptor.capture());
        Iterable<WorkflowStep> savedSteps = stepsCaptor.getValue();
        
        java.util.List<WorkflowStep> stepsList = new java.util.ArrayList<>();
        savedSteps.forEach(stepsList::add);
        
        assertThat(stepsList.stream().filter(s -> "Approval Step".equals(s.getName())).findFirst().get().getDescription())
                .isEqualTo("Task Description Text");
    }

    @Test
    void CreateWorkflow_WithMultipleTransitionRules_PersistsAllRules() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" " +
                "xmlns:veritas=\"http://veritas\" " +
                "id=\"Definitions_1\" targetNamespace=\"http://bpmn.io/schema/bpmn\">\n" +
                "  <bpmn:process id=\"Process_1\" name=\"Test Workflow\" isExecutable=\"true\">\n" +
                "    <bpmn:startEvent id=\"StartEvent_1\" name=\"Start\" />\n" +
                "    <bpmn:task id=\"Task_1\" name=\"Approval Step\" />\n" +
                "    <bpmn:endEvent id=\"EndEvent_1\" name=\"End\" />\n" +
                "    <bpmn:sequenceFlow id=\"Flow_1\" sourceRef=\"StartEvent_1\" targetRef=\"Task_1\">\n" +
                "      <bpmn:extensionElements>\n" +
                "        <veritas:transitionRule minRequiredVendors=\"3\" isPdfRequired=\"true\" isCsvRequired=\"true\" isImageRequired=\"false\" />\n" +
                "      </bpmn:extensionElements>\n" +
                "    </bpmn:sequenceFlow>\n" +
                "    <bpmn:sequenceFlow id=\"Flow_2\" sourceRef=\"Task_1\" targetRef=\"EndEvent_1\" />\n" +
                "  </bpmn:process>\n" +
                "</bpmn:definitions>";
        WorkflowSaveDto saveDto = new WorkflowSaveDto(xml, null);
        when(workflowMapper.toWorkflowDto(any(WorkflowDefinition.class))).thenReturn(new WorkflowDto(1L, "", "", 1L, "", true, null));

        workflowService.createWorkflow(saveDto);

        verify(transitionRuleRepository).saveAll(transitionRulesCaptor.capture());
        Iterable<TransitionRule> savedRules = transitionRulesCaptor.getValue();

        List<TransitionRule> rulesList = new ArrayList<>();
        savedRules.forEach(rulesList::add);

        assertThat(rulesList).hasSize(1);
        assertThat(rulesList.getFirst().getMinRequiredVendors()).isEqualTo(3);
        assertThat(rulesList.getFirst().getIsPdfRequired()).isTrue();
        assertThat(rulesList.getFirst().getIsCsvRequired()).isTrue();
        assertThat(rulesList.getFirst().getIsImageRequired()).isFalse();
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

        assertThat(rulesList).isEmpty();
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
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).name()).isEqualTo("Test Workflow");
        verify(workflowDefinitionRepository).findAllFiltered(any(), any(), any(), any(), eq(pageable));
    }

    @Test
    void GetAllWorkflows_EmptyResults_ReturnsEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        when(workflowDefinitionRepository.findAllFiltered(any(), any(), any(), any(), any()))
                .thenReturn(Page.empty());

        Page<WorkflowDto> result = workflowService.getAllWorkflows(pageable, null, null, null);

        assertThat(result).isEmpty();
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
        
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).name()).isEqualTo("Department Workflow");
        verify(workflowDefinitionRepository).findAllFiltered(any(), any(), eq(12L), eq(true), eq(pageable));
    }
}
