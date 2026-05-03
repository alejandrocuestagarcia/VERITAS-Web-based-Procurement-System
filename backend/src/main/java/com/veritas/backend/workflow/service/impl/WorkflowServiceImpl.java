package com.veritas.backend.workflow.service.impl;

import com.veritas.backend.workflow.dto.WorkflowDto;
import com.veritas.backend.workflow.dto.WorkflowEditDto;
import com.veritas.backend.workflow.dto.WorkflowSaveDto;
import com.veritas.backend.workflow.entity.WorkflowComponent;
import com.veritas.backend.workflow.entity.WorkflowDefinition;
import com.veritas.backend.workflow.entity.WorkflowStep;
import com.veritas.backend.workflow.entity.WorkflowTransition;
import com.veritas.backend.workflow.mapper.WorkflowMapper;
import com.veritas.backend.workflow.repository.WorkflowDefinitionRepository;
import com.veritas.backend.workflow.repository.WorkflowStepRepository;
import com.veritas.backend.workflow.repository.WorkflowTransitionRepository;
import com.veritas.backend.workflow.service.WorkflowService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class WorkflowServiceImpl implements WorkflowService {
    private final WorkflowDefinitionRepository workflowDefinitionRepository;
    private final WorkflowStepRepository workflowStepRepository;
    private final WorkflowTransitionRepository workflowTransitionRepository;
    private final WorkflowMapper workflowMapper;

    @Override
    @Transactional
    public WorkflowDto createWorkflow(WorkflowSaveDto workflowSaveDto) {
        return workflowMapper.toWorkflowDto(parseWorkflow(workflowSaveDto.bpmnXml()));
    }

    @Override
    @Transactional(readOnly = true)
    public WorkflowDto getWorkflow(Long id) {
        return workflowDefinitionRepository.findById(id)
                .map(workflowMapper::toWorkflowDto)
                .orElseThrow(() -> new EntityNotFoundException("Workflow with id '" + id + "' not found"));
    }

    @Override
    @Transactional
    public WorkflowDto editWorkflow(Long id, WorkflowEditDto workflowEditDto) {
        return workflowMapper.toWorkflowDto(parseWorkflow(workflowEditDto.bpmnXml(), id));
    }

    private WorkflowDefinition parseWorkflow(String xml) {
        return parseWorkflow(xml, null);
    }

    private WorkflowDefinition parseWorkflow(String xml, Long id) {
        WorkflowDefinition workflowDefinition = new WorkflowDefinition();
        if (id == null) {
            workflowDefinition.setVersion(1);
        } else {
            WorkflowDefinition oldWorkflowDefinition = workflowDefinitionRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Workflow with id '" + id + "' not found"));
            workflowDefinition.setVersion(oldWorkflowDefinition.getVersion() + 1);
            workflowDefinition.setPreviousVersion(oldWorkflowDefinition);

            oldWorkflowDefinition.setIsActive(false);
            oldWorkflowDefinition.setDeactivatedAt(LocalDateTime.now());
            workflowDefinitionRepository.save(oldWorkflowDefinition);
        }
        workflowDefinition.setBpmnXml(xml);

        BpmnModelInstance modelInstance;
        try {
            modelInstance = Bpmn.readModelFromStream(
                    new ByteArrayInputStream(xml.getBytes())
            );
            Bpmn.validateModel(modelInstance);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error while parsing input xml");
        }
        Optional<Process> process = modelInstance.getModelElementsByType(Process.class)
                .stream().findFirst();
        if (process.isEmpty()) {
            throw new IllegalArgumentException("BPMN XML document contains no Process class");
        }
        String workflowName = process.get().getName();

        if (workflowName.isBlank()) {
            throw new IllegalArgumentException("Workflow name must not be blank");
        }

        Optional<Documentation> documentation = modelInstance.getModelElementsByType(Documentation.class)
                .stream().findFirst();
        documentation.ifPresent(value -> workflowDefinition.setDescription(value.getTextContent()));

        workflowDefinition.setName(workflowName);

        Map<String, WorkflowStep> stepsMap = new HashMap<>();
        Collection<FlowNode> flowNodes = modelInstance.getModelElementsByType(FlowNode.class);
        flowNodes.forEach(node -> {
            WorkflowStep step = new WorkflowStep();
            step.setName(node.getName());
            switch (node) {
                case StartEvent startEvent -> step.setWorkflowComponent(WorkflowComponent.START_EVENT);
                case EndEvent endEvent -> step.setWorkflowComponent(WorkflowComponent.END_EVENT);
                case Gateway gateway -> step.setWorkflowComponent(WorkflowComponent.BRANCH);
                case Task task -> step.setWorkflowComponent(WorkflowComponent.STEP);
                default ->
                        throw new IllegalArgumentException("The BPMN element '" + node.getElementType().getTypeName() +
                                "' is not supported in our procurement system");
            }

            stepsMap.put(node.getId(), step);
        });

        Collection<WorkflowTransition> workflowTransitions = new ArrayList<>();
        Collection<SequenceFlow> sequenceFlows = modelInstance.getModelElementsByType(SequenceFlow.class);
        sequenceFlows.forEach(sequenceFlow -> {
            WorkflowTransition transition = new WorkflowTransition();
            transition.setName(sequenceFlow.getName());
            transition.setFromStep(stepsMap.get(sequenceFlow.getSource().getId()));
            transition.setToStep(stepsMap.get(sequenceFlow.getTarget().getId()));

            workflowTransitions.add(transition);
        });

        workflowDefinitionRepository.save(workflowDefinition);

        stepsMap.values().forEach(step -> step.setWorkflowDefinition(workflowDefinition));
        workflowStepRepository.saveAll(stepsMap.values());

        workflowTransitionRepository.saveAll(workflowTransitions);

        return workflowDefinition;
    }
}
