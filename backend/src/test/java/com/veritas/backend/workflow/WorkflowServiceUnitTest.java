package com.veritas.backend.workflow;

import com.veritas.backend.workflow.dto.WorkflowDto;
import com.veritas.backend.workflow.dto.WorkflowEditDto;
import com.veritas.backend.workflow.dto.WorkflowSaveDto;
import com.veritas.backend.workflow.entity.WorkflowDefinition;
import com.veritas.backend.workflow.mapper.WorkflowMapper;
import com.veritas.backend.workflow.repository.WorkflowDefinitionRepository;
import com.veritas.backend.workflow.repository.WorkflowStepRepository;
import com.veritas.backend.workflow.repository.WorkflowTransitionRepository;
import com.veritas.backend.workflow.service.impl.WorkflowServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkflowServiceUnitTest {

    @Mock
    private WorkflowDefinitionRepository workflowDefinitionRepository;

    @Mock
    private WorkflowStepRepository workflowStepRepository;

    @Mock
    private WorkflowTransitionRepository workflowTransitionRepository;

    @Mock
    private WorkflowMapper workflowMapper;

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

        WorkflowDto dto = new WorkflowDto(1L, "Test Workflow", VALID_BPMN_XML, 1L, "desc", true);

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
    void CreateWorkflow_ValidInput_SavesAndReturnsWorkflowDto() {
        WorkflowSaveDto saveDto = new WorkflowSaveDto(VALID_BPMN_XML);

        WorkflowDefinition wd = new WorkflowDefinition();
        wd.setId(1L);
        wd.setName("Test Workflow");

        WorkflowDto dto = new WorkflowDto(1L, "Test Workflow", VALID_BPMN_XML, 1L, "desc", true);

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

        WorkflowEditDto editDto = new WorkflowEditDto(VALID_BPMN_XML);
        WorkflowDto dto = new WorkflowDto(2L, "Test Workflow", VALID_BPMN_XML, 2L, "desc", true);

        when(workflowDefinitionRepository.findById(1L)).thenReturn(Optional.of(oldWd));
        when(workflowMapper.toWorkflowDto(any(WorkflowDefinition.class))).thenReturn(dto);

        WorkflowDto result = workflowService.editWorkflow(1L, editDto);

        assertThat(result).isEqualTo(dto);
        verify(workflowDefinitionRepository, times(2)).save(any(WorkflowDefinition.class));
    }
}
