package com.veritas.backend.workflow.service.impl;

import com.veritas.backend.user.entity.User;
import com.veritas.backend.user.entity.UserRole;
import com.veritas.backend.user.repository.UserRepository;
import com.veritas.backend.department.entity.Department;
import com.veritas.backend.department.repository.DepartmentRepository;
import com.veritas.backend.workflow.dto.WorkflowDto;
import com.veritas.backend.workflow.dto.WorkflowEditDto;
import com.veritas.backend.workflow.dto.WorkflowSaveDto;
import com.veritas.backend.workflow.entity.WorkflowComponent;
import com.veritas.backend.workflow.entity.WorkflowDefinition;
import com.veritas.backend.workflow.entity.WorkflowStep;
import com.veritas.backend.workflow.entity.WorkflowTransition;
import com.veritas.backend.workflow.entity.TransitionRule;
import com.veritas.backend.workflow.repository.TransitionRuleRepository;
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
import org.camunda.bpm.model.xml.instance.DomElement;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowServiceImpl implements WorkflowService {
    private static final String ASSIGNEE_PREFIX = "[ASSIGNEE]";
    private final WorkflowDefinitionRepository workflowDefinitionRepository;
    private final WorkflowStepRepository workflowStepRepository;
    private final WorkflowTransitionRepository workflowTransitionRepository;
    private final TransitionRuleRepository transitionRuleRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final WorkflowMapper workflowMapper;

    @Override
    @Transactional
    public WorkflowDto createWorkflow(WorkflowSaveDto workflowSaveDto) {
        return workflowMapper.toWorkflowDto(parseWorkflow(workflowSaveDto.bpmnXml(), null, workflowSaveDto.departmentId()));
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
        return workflowMapper.toWorkflowDto(parseWorkflow(workflowEditDto.bpmnXml(), id, workflowEditDto.departmentId()));
    }

    private WorkflowDefinition parseWorkflow(String xml, Long id, Long departmentId) {
        WorkflowDefinition workflowDefinition = new WorkflowDefinition();
        if (id == null) {
            workflowDefinition.setVersion(1);
            if (departmentId != null) {
                Department department = departmentRepository.findById(departmentId)
                        .orElseThrow(() -> new EntityNotFoundException("Department with id '" + departmentId + "' not found"));
                workflowDefinition.setDepartment(department);
            }
        } else {
            WorkflowDefinition oldWorkflowDefinition = workflowDefinitionRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Workflow with id '" + id + "' not found"));
            if (!oldWorkflowDefinition.getIsActive()) {
                throw new IllegalArgumentException("Workflow '" + oldWorkflowDefinition.getName() + "' is not active and can't be edited");
            }
            workflowDefinition.setVersion(oldWorkflowDefinition.getVersion() + 1);
            workflowDefinition.setPreviousVersion(oldWorkflowDefinition);

            if (departmentId != null) {
                Department department = departmentRepository.findById(departmentId)
                        .orElseThrow(() -> new EntityNotFoundException("Department with id '" + departmentId + "' not found"));
                workflowDefinition.setDepartment(department);
            } else {
                workflowDefinition.setDepartment(oldWorkflowDefinition.getDepartment());
            }

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
                case Task task -> {
                    step.setWorkflowComponent(WorkflowComponent.STEP);
                    task.getDocumentations().forEach(doc -> {
                        String text = doc.getTextContent();
                        if (text != null && !text.isBlank()) {
                            if (text.startsWith(ASSIGNEE_PREFIX)) {
                                String roleName = text.substring(ASSIGNEE_PREFIX.length());
                                try {
                                    step.setRole(UserRole.valueOf(roleName));
                                } catch (IllegalArgumentException e) {
                                    throw new IllegalArgumentException("Invalid role assigned in BPMN: " + roleName);
                                }
                            } else {
                                step.setDescription(text);
                            }
                        }
                    });
                }
                default ->
                        throw new IllegalArgumentException("The BPMN element '" + node.getElementType().getTypeName() +
                                "' is not supported in our procurement system");
            }

            stepsMap.put(node.getId(), step);
        });

        Collection<WorkflowTransition> workflowTransitions = new ArrayList<>();
        Map<WorkflowTransition, TransitionRule> transitionRulesMap = new HashMap<>();
        Collection<SequenceFlow> sequenceFlows = modelInstance.getModelElementsByType(SequenceFlow.class);
        sequenceFlows.forEach(sequenceFlow -> {
            WorkflowTransition transition = new WorkflowTransition();
            transition.setName(sequenceFlow.getName());
            transition.setFromStep(stepsMap.get(sequenceFlow.getSource().getId()));
            transition.setToStep(stepsMap.get(sequenceFlow.getTarget().getId()));

            ConditionExpression conditionExpression = sequenceFlow.getConditionExpression();
            if (conditionExpression != null) {
                String textContent = conditionExpression.getTextContent();
                if (textContent != null && !textContent.isBlank()) {
                    log.info("Found condition for flow {}: {}", sequenceFlow.getId(), textContent);

                    String cleanCondition = textContent.replace("${", "").replace("}", "").trim();
                    transition.setConditionExpression(cleanCondition);
                }
            } else {
                log.debug("No condition found for flow {}", sequenceFlow.getId());
            }
            sequenceFlow.getDocumentations().stream()
                    .findFirst()
                    .ifPresent(doc -> transition.setDescription(doc.getTextContent()));

            ExtensionElements extensionElements = sequenceFlow.getExtensionElements();
            if (extensionElements != null) {
                DomElement domElement = extensionElements.getDomElement();
                for (DomElement child : domElement.getChildElements()) {
                    if ("transitionRule".equals(child.getLocalName())) {
                        TransitionRule rule = new TransitionRule();
                        String minVendors = child.getAttribute("minRequiredVendors");
                        if (minVendors != null && !minVendors.isBlank()) {
                            rule.setMinRequiredVendors(Integer.parseInt(minVendors));
                        }
                        String pdfRequired = child.getAttribute("isPdfRequired");
                        if (pdfRequired != null) {
                            rule.setIsPdfRequired(Boolean.parseBoolean(pdfRequired));
                        }
                        String csvRequired = child.getAttribute("isCsvRequired");
                        if (csvRequired != null) {
                            rule.setIsCsvRequired(Boolean.parseBoolean(csvRequired));
                        }
                        String imageRequired = child.getAttribute("isImageRequired");
                        if (imageRequired != null) {
                            rule.setIsImageRequired(Boolean.parseBoolean(imageRequired));
                        }
                        String optionalFailureMessage = child.getAttribute("optionalFailureMessage");
                        if (optionalFailureMessage != null && !optionalFailureMessage.isBlank()) {
                            rule.setOptionalFailureMessage(optionalFailureMessage);
                        }
                        transitionRulesMap.put(transition, rule);
                    }
                }
            }

            workflowTransitions.add(transition);
        });

        workflowDefinitionRepository.save(workflowDefinition);

        stepsMap.values().forEach(step -> step.setWorkflowDefinition(workflowDefinition));
        workflowStepRepository.saveAll(stepsMap.values());

        workflowTransitionRepository.saveAll(workflowTransitions);

        transitionRulesMap.forEach((transition, rule) -> {
            rule.setTransition(transition);
            transition.setRule(rule);
        });
        transitionRuleRepository.saveAll(transitionRulesMap.values());

        return workflowDefinition;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WorkflowDto> getAllWorkflows(Pageable pageable, String filter, Boolean isActive, User authUser) {
        log.debug("Fetching filtered workflows – filter: '{}', page: {}, isActive: {}, user: {}", filter, pageable.getPageNumber(), isActive, authUser != null ? authUser.getEmail() : "null");
        String query = (filter != null && !filter.isBlank()) ? "%" + filter.trim().toLowerCase() + "%" : null;

        Long departmentId = null;
        boolean includeGlobal = true;

        if (authUser != null && (authUser.getRole() == UserRole.REQUESTER || authUser.getRole() == UserRole.PROCUREMENT_OFFICER)) {
            User fullUser = userRepository.findById(authUser.getId()).orElse(authUser);
            if (fullUser.getDepartment() != null) {
                departmentId = fullUser.getDepartment().getDepartmentId();
            } else if (fullUser.getTeam() != null && fullUser.getTeam().getDepartment() != null) {
                departmentId = fullUser.getTeam().getDepartment().getDepartmentId();
            }
        }

        return workflowDefinitionRepository.findAllFiltered(query, isActive, departmentId, includeGlobal, pageable).map(workflowMapper::toWorkflowDto);
    }

    @Override
    @Transactional
    public void deleteWorkflow(Long id) {
        WorkflowDefinition workflowDefinition = workflowDefinitionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Workflow with id '" + id + "' not found"));
        workflowDefinition.setIsActive(false);
        workflowDefinition.setDeactivatedAt(LocalDateTime.now());
        workflowDefinitionRepository.save(workflowDefinition);
    }
}
