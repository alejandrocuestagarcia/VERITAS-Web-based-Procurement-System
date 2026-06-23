package com.veritas.backend.workflow.validation;

import com.veritas.backend.user.entity.UserRole;
import com.veritas.backend.workflow.entity.AttachmentType;
import com.veritas.backend.workflow.dto.SpelFieldDto;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.spel.ast.CompoundExpression;
import org.springframework.expression.spel.ast.PropertyOrFieldReference;
import org.springframework.expression.spel.SpelNode;
import org.springframework.expression.ParseException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Validates a parsed BPMN model for structural, semantic, and security correctness
 * specific to the Veritas procurement workflow system.
 *
 * <p>All rules are evaluated, collecting all errors and warnings into a
 * {@link BpmnValidationResult} so the frontend can display every issue at once.</p>
 */
@Slf4j
@Component
public class BpmnValidator {

    /** Maximum allowed XML size in bytes (512 KB). */
    private static final int MAX_XML_SIZE_BYTES = 512 * 1024;

    /** Maximum allowed number of flow nodes in a workflow. */
    private static final int MAX_NODE_COUNT = 100;

    private static final String ASSIGNEE_PREFIX = "[ASSIGNEE]";
    private static final String TEAM_LEADER_PREFIX = "[TEAM_LEADER]";

    /** Pattern to detect unsafe SpEL constructs. */
    private static final Pattern UNSAFE_SPEL_PATTERN = Pattern.compile(
            "(T\\s*\\(|new\\s+|#|\\bgetClass\\b|\\bforName\\b|\\bRuntime\\b|\\bjava\\.)"
    );

    private static final Set<String> ALLOWED_BRANCHING_PATHS = Set.of(
            "selectedQuoteTotalAmount",
            "priority",
            "totalQuantity",
            "department",
            "department.id",
            "department.name",
            "department.budget",
            "department.budget.id",
            "department.budget.name",
            "department.budget.totalAmount",
            "department.budget.committedSpend",
            "department.budget.actualSpend",
            "department.budget.safetyBuffer",
            "department.budget.remainingAmount",
            "project",
            "project.id",
            "project.name",
            "project.key",
            "project.budget",
            "project.budget.id",
            "project.budget.name",
            "project.budget.totalAmount",
            "project.budget.committedSpend",
            "project.budget.actualSpend",
            "project.budget.safetyBuffer",
            "project.budget.remainingAmount",
            "requester",
            "requester.id",
            "requester.name",
            "requester.email",
            "requester.role",
            "requester.isTeamLeader",
            "budget",
            "budget.id",
            "budget.name",
            "budget.totalAmount",
            "budget.committedSpend",
            "budget.actualSpend",
            "budget.safetyBuffer",
            "budget.remainingAmount",
            "globalBudget",
            "globalBudget.id",
            "globalBudget.name",
            "globalBudget.totalAmount",
            "globalBudget.committedSpend",
            "globalBudget.actualSpend",
            "globalBudget.safetyBuffer",
            "globalBudget.remainingAmount"
    );

    private static final Set<String> OBJECT_PATHS = Set.of(
            "department", "project", "requester", "budget", "globalBudget",
            "department.budget", "project.budget"
    );

    private static final Set<String> STRING_PATHS = Set.of(
            "priority",
            "department.name", "project.name", "project.key",
            "requester.name", "requester.email", "requester.role",
            "budget.name", "globalBudget.name",
            "department.budget.name", "project.budget.name"
    );

    private static final Set<String> BOOLEAN_PATHS = Set.of("requester.isTeamLeader");

    /**
     * Returns the list of all allowed SpEL field paths with their types,
     * derived from the canonical {@link #ALLOWED_BRANCHING_PATHS} set.
     */
    public static List<SpelFieldDto> getSpelFields() {
        return ALLOWED_BRANCHING_PATHS.stream()
                .map(path -> {
                    String type;
                    boolean isObject = OBJECT_PATHS.contains(path);
                    if (isObject) {
                        type = "object";
                    } else if (BOOLEAN_PATHS.contains(path)) {
                        type = "boolean";
                    } else if (STRING_PATHS.contains(path)) {
                        type = "string";
                    } else {
                        type = "number";
                    }
                    return new SpelFieldDto(path, type, isObject);
                })
                .toList();
    }

    private final SpelExpressionParser spelParser = new SpelExpressionParser();

    /**
     * Validates the raw BPMN XML string and the parsed model instance.
     *
     * @param xml            the raw BPMN XML string
     * @param modelInstance  the Camunda-parsed BPMN model
     * @throws BpmnValidationException if any blocking errors are found
     */
    public BpmnValidationResult validate(String xml, BpmnModelInstance modelInstance) {
        BpmnValidationResult result = new BpmnValidationResult();

        int xmlSizeBytes = xml == null ? 0 : xml.getBytes(StandardCharsets.UTF_8).length;
        log.info("Starting BPMN validation (xmlBytes={})", xmlSizeBytes);

        // --- Security / size checks ---
        validateXmlSize(xml, result);
        if (modelInstance == null) {
            result.addError("BPMN model is null");
            log.error("BPMN validation failed: {}", result.getSummary());
            throw new BpmnValidationException(result);
        }
        validateNodeCount(modelInstance, result);

        // --- Structural validations ---
        Process process = validateProcessPresence(modelInstance, result);
        validateProcessName(process, result);
        validateProcessDescription(modelInstance, result);

        Collection<StartEvent> startEvents = modelInstance.getModelElementsByType(StartEvent.class);
        Collection<EndEvent> endEvents = modelInstance.getModelElementsByType(EndEvent.class);
        Collection<FlowNode> flowNodes = modelInstance.getModelElementsByType(FlowNode.class);
        Collection<SequenceFlow> sequenceFlows = modelInstance.getModelElementsByType(SequenceFlow.class);
        Collection<Task> tasks = modelInstance.getModelElementsByType(Task.class);
        Collection<Gateway> gateways = modelInstance.getModelElementsByType(Gateway.class);

        log.debug("Model element counts - startEvents: {}, endEvents: {}, flowNodes: {}, sequenceFlows: {}, tasks: {}, gateways: {}",
            startEvents.size(), endEvents.size(), flowNodes.size(), sequenceFlows.size(), tasks.size(), gateways.size());

        validateSupportedFlowNodeTypes(flowNodes, result);

        StartEvent startEvent = validateStartEvent(startEvents, result);
        validateEndEvents(endEvents, result);
        validateMinimumComplexity(tasks, result);
        Set<String> allNodeIds = flowNodes.stream()
            .map(FlowNode::getId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        List<SequenceFlow> validSequenceFlows = sequenceFlows.stream()
            .filter(flow -> hasValidEndpoints(flow, allNodeIds))
            .collect(Collectors.toList());

        validateStartEventConnectivity(startEvent, validSequenceFlows, result);
        validateEndEventConnectivity(endEvents, validSequenceFlows, result);
        validateNoIsolatedNodes(flowNodes, validSequenceFlows, result);
        validateSequenceFlowReferences(sequenceFlows, flowNodes, result);
        validateNoDuplicateSequenceFlows(validSequenceFlows, result);

        // Build adjacency list for graph analysis
        Map<String, List<String>> adjacency = buildAdjacencyList(validSequenceFlows);
        Map<String, List<String>> reverseAdjacency = buildReverseAdjacencyList(validSequenceFlows);
        Set<String> endEventIds = endEvents.stream()
            .map(EndEvent::getId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        validateNoCycles(adjacency, allNodeIds, flowNodes, result);
        if (startEvent != null) {
            validateAllNodesReachable(startEvent.getId(), adjacency, allNodeIds, flowNodes, result);
        }
        validateAllPathsReachEnd(reverseAdjacency, allNodeIds, endEventIds, flowNodes, result);

        // --- Gateway validations ---
        validateGatewayTypes(gateways, result);
        validateGatewaySemantics(gateways, validSequenceFlows, result);

        // --- Task validations ---
        validateTaskNames(tasks, result);
        validateTaskNameUniqueness(tasks, result);
        validateAssigneeRoles(tasks, result);
        validateTaskDescriptions(tasks, result);

        // --- Sequence flow / transition validations ---
        validateConditionExpressions(sequenceFlows, result);

        // --- Transition rule validations ---
        validateTransitionRules(validSequenceFlows, flowNodes, result);

        if (result.hasErrors()) {
            log.error("BPMN validation failed: {}", result.getSummary());
            throw new BpmnValidationException(result);
        }

        log.info("BPMN validation completed successfully (warnings={})", result.getWarnings().size());

        return result;
    }

    // ========================================================================
    // Security validations
    // ========================================================================

    private void validateXmlSize(String xml, BpmnValidationResult result) {
        if (xml == null) {
            result.addError("BPMN XML is null");
            log.warn("BPMN XML is null");
            return;
        }
        if (xml.getBytes(StandardCharsets.UTF_8).length > MAX_XML_SIZE_BYTES) {
            result.addError("BPMN XML exceeds maximum allowed size of " + (MAX_XML_SIZE_BYTES / 1024) + " KB");
            log.warn("BPMN XML size {} bytes exceeds configured maximum of {} KB",
                    xml.getBytes(StandardCharsets.UTF_8).length, (MAX_XML_SIZE_BYTES / 1024));
        }
    }

    private void validateNodeCount(BpmnModelInstance model, BpmnValidationResult result) {
        if (model == null) {
            result.addError("BPMN model is null");
            return;
        }
        int count = model.getModelElementsByType(FlowNode.class).size();
        if (count > MAX_NODE_COUNT) {
            result.addError("Workflow contains " + count + " nodes, which exceeds the maximum of " + MAX_NODE_COUNT);
            log.warn("Workflow node count {} exceeds maximum {}", count, MAX_NODE_COUNT);
        }
    }

    private Process validateProcessPresence(BpmnModelInstance model, BpmnValidationResult result) {
        if (model == null) {
            result.addError("BPMN model is null");
            return null;
        }
        Collection<Process> processes = model.getModelElementsByType(Process.class);
        if (processes.isEmpty()) {
            result.addError("BPMN XML document contains no Process class");
            return null;
        }
        return processes.iterator().next();
    }

    private void validateProcessName(Process process, BpmnValidationResult result) {
        if (process == null) return;
        String name = process.getName();
        if (name == null || name.isBlank()) {
            result.addError("Workflow name must not be blank");
        } else if (name.length() > 120) {
            result.addError("Workflow name must be at most 120 characters");
        }
    }

    private void validateProcessDescription(BpmnModelInstance modelInstance, BpmnValidationResult result) {
        if (modelInstance == null) return;
        Optional<Documentation> documentation = modelInstance.getModelElementsByType(Documentation.class)
                .stream().findFirst();
        if (documentation.isPresent()) {
            String text = documentation.get().getTextContent();
            if (text != null && text.length() > 1000) {
                result.addError("Workflow description must be at most 1000 characters");
            }
        }
    }

    // ========================================================================
    // Structural validations
    // ========================================================================

    private StartEvent validateStartEvent(Collection<StartEvent> startEvents, BpmnValidationResult result) {
        if (startEvents.isEmpty()) {
            result.addError("Workflow must contain exactly one Start Event, but none was found");
            return null;
        }
        if (startEvents.size() > 1) {
            result.addError("Workflow must contain exactly one Start Event, but " + startEvents.size() + " were found");
        }
        return startEvents.iterator().next();
    }

    private void validateEndEvents(Collection<EndEvent> endEvents, BpmnValidationResult result) {
        if (endEvents.isEmpty()) {
            result.addError("Workflow must contain at least one End Event, but none was found");
        }
    }

    private void validateMinimumComplexity(Collection<Task> tasks, BpmnValidationResult result) {
        if (tasks.isEmpty()) {
            result.addError("Workflow must contain at least one Task (approval step) between Start and End events");
        }
    }

    private void validateStartEventConnectivity(StartEvent startEvent, Collection<SequenceFlow> flows, BpmnValidationResult result) {
        if (startEvent == null) return;
        boolean hasOutgoing = flows.stream()
                .anyMatch(f -> f.getSource().getId().equals(startEvent.getId()));
        if (!hasOutgoing) {
            result.addError("Start Event '" + getNodeName(startEvent) + "' has no outgoing transitions — it must be connected to the next step");
        }
        boolean hasIncoming = flows.stream()
                .anyMatch(f -> f.getTarget().getId().equals(startEvent.getId()));
        if (hasIncoming) {
            result.addError("Start Event must not have incoming transitions");
        }
    }

    private void validateEndEventConnectivity(Collection<EndEvent> endEvents, Collection<SequenceFlow> flows, BpmnValidationResult result) {
        for (EndEvent endEvent : endEvents) {
            boolean hasOutgoing = flows.stream()
                    .anyMatch(f -> f.getSource().getId().equals(endEvent.getId()));
            if (hasOutgoing) {
                result.addError("End Event '" + getNodeName(endEvent) + "' must not have outgoing transitions");
            }
            boolean hasIncoming = flows.stream()
                    .anyMatch(f -> f.getTarget().getId().equals(endEvent.getId()));
            if (!hasIncoming) {
                result.addError("End Event '" + getNodeName(endEvent) + "' has no incoming transitions — it is unreachable");
            }
        }
    }

    private void validateNoIsolatedNodes(Collection<FlowNode> flowNodes, Collection<SequenceFlow> flows, BpmnValidationResult result) {
        Set<String> connectedNodeIds = new HashSet<>();
        for (SequenceFlow flow : flows) {
            connectedNodeIds.add(flow.getSource().getId());
            connectedNodeIds.add(flow.getTarget().getId());
        }
        for (FlowNode node : flowNodes) {
            if (!connectedNodeIds.contains(node.getId())) {
                result.addError("Step '" + getNodeName(node)
                        + "' is isolated — it has no incoming or outgoing transitions");
            }
        }
    }

    private void validateSequenceFlowReferences(Collection<SequenceFlow> flows, Collection<FlowNode> flowNodes, BpmnValidationResult result) {
        Set<String> nodeIds = flowNodes.stream()
                .map(FlowNode::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        for (SequenceFlow flow : flows) {
            FlowNode source = flow.getSource();
            if (source == null || source.getId() == null || !nodeIds.contains(source.getId())) {
                result.addError("A transition references a non-existent source step");
            }
            FlowNode target = flow.getTarget();
            if (target == null || target.getId() == null || !nodeIds.contains(target.getId())) {
                result.addError("A transition references a non-existent target step");
            }
        }
    }

    private void validateNoDuplicateSequenceFlows(Collection<SequenceFlow> flows, BpmnValidationResult result) {
        Set<String> seen = new HashSet<>();
        for (SequenceFlow flow : flows) {
            String key = flow.getSource().getId() + " -> " + flow.getTarget().getId();
            if (!seen.add(key)) {
                result.addError("Duplicate transition from '" + getNodeName(flow.getSource())
                        + "' to '" + getNodeName(flow.getTarget()) + "'");
            }
        }
    }

    private void validateSupportedFlowNodeTypes(Collection<FlowNode> flowNodes, BpmnValidationResult result) {
        for (FlowNode node : flowNodes) {
            if (!(node instanceof StartEvent || node instanceof EndEvent
                    || node instanceof Gateway || node instanceof Task)) {
                result.addError("The BPMN element '" + node.getElementType().getTypeName()
                        + "' is not supported in our procurement system");
            }
        }
    }

    // ========================================================================
    // Graph analysis
    // ========================================================================

    private Map<String, List<String>> buildAdjacencyList(Collection<SequenceFlow> flows) {
        Map<String, List<String>> adj = new HashMap<>();
        for (SequenceFlow flow : flows) {
            adj.computeIfAbsent(flow.getSource().getId(), k -> new ArrayList<>())
                    .add(flow.getTarget().getId());
        }
        return adj;
    }

    private Map<String, List<String>> buildReverseAdjacencyList(Collection<SequenceFlow> flows) {
        Map<String, List<String>> adj = new HashMap<>();
        for (SequenceFlow flow : flows) {
            adj.computeIfAbsent(flow.getTarget().getId(), k -> new ArrayList<>())
                    .add(flow.getSource().getId());
        }
        return adj;
    }

    private boolean hasValidEndpoints(SequenceFlow flow, Set<String> nodeIds) {
        if (flow.getSource() == null || flow.getTarget() == null) {
            return false;
        }
        String sourceId = flow.getSource().getId();
        String targetId = flow.getTarget().getId();
        if (sourceId == null || targetId == null) {
            return false;
        }
        return nodeIds.contains(sourceId) && nodeIds.contains(targetId);
    }

    /** Maximum number of consecutive gateway hops before the engine would stack-overflow. */
    private static final int MAX_GATEWAY_CHAIN_DEPTH = 50;

    /**
     * Detects unsafe cycles — i.e., cycles that consist entirely of non-task nodes
     * (gateways and events). Such cycles would cause infinite recursion in
     * {@code WorkflowEngineServiceImpl.moveToNextStep()} because the engine recurses
     * immediately through BRANCH nodes without waiting for human input.
     *
     * <p>Cycles that pass through at least one Task are <em>allowed</em> because the
     * engine stops at each Task and waits for a user action, breaking the recursion.
     * This enables legitimate rejection/rework loops in procurement workflows.</p>
     */
    private void validateNoCycles(Map<String, List<String>> adjacency, Set<String> allNodeIds, Collection<FlowNode> flowNodes, BpmnValidationResult result) {
        // Collect IDs of all Task nodes — cycles through these are safe
        Set<String> taskIds = flowNodes.stream()
                .filter(n -> n instanceof Task)
                .map(FlowNode::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // Build a subgraph that only contains non-task nodes (gateways, events).
        // If this subgraph has a cycle, the engine would infinitely recurse.
        Map<String, List<String>> nonTaskAdjacency = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : adjacency.entrySet()) {
            String sourceId = entry.getKey();
            if (taskIds.contains(sourceId)) continue; // skip edges from tasks

            List<String> nonTaskNeighbors = entry.getValue().stream()
                    .filter(targetId -> !taskIds.contains(targetId))
                    .collect(Collectors.toList());
            if (!nonTaskNeighbors.isEmpty()) {
                nonTaskAdjacency.put(sourceId, nonTaskNeighbors);
            }
        }

        Set<String> nonTaskNodeIds = allNodeIds.stream()
                .filter(id -> !taskIds.contains(id))
                .collect(Collectors.toSet());

        // DFS cycle detection on the non-task subgraph only
        Map<String, Integer> color = new HashMap<>();
        nonTaskNodeIds.forEach(id -> color.put(id, 0));

        for (String nodeId : nonTaskNodeIds) {
            if (color.getOrDefault(nodeId, 0) == 0) {
                if (hasCycleDfs(nodeId, nonTaskAdjacency, color)) {
                    result.addError("Workflow contains a cycle through gateways or events without any task in between — "
                            + "this would cause infinite execution. Loops are allowed only if they pass through at least one task (human step)");
                    return;
                }
            }
        }

        // Additionally, check for excessively long gateway-only chains (even if acyclic)
        // to guard against deeply nested gateway structures
        for (String nodeId : nonTaskNodeIds) {
            int depth = measureGatewayChainDepth(nodeId, nonTaskAdjacency, new HashSet<>(), 0);
            if (depth > MAX_GATEWAY_CHAIN_DEPTH) {
                result.addError("Workflow has a chain of " + depth + " consecutive gateways/events without a task — "
                        + "maximum allowed is " + MAX_GATEWAY_CHAIN_DEPTH);
                return;
            }
        }
    }

    private boolean hasCycleDfs(String nodeId, Map<String, List<String>> adjacency, Map<String, Integer> color) {
        color.put(nodeId, 1); // GRAY
        for (String neighbor : adjacency.getOrDefault(nodeId, Collections.emptyList())) {
            int neighborColor = color.getOrDefault(neighbor, 0);
            if (neighborColor == 1) {
                return true; // back edge found
            }
            if (neighborColor == 0 && hasCycleDfs(neighbor, adjacency, color)) {
                return true;
            }
        }
        color.put(nodeId, 2); // BLACK
        return false;
    }

    /**
     * Measures the longest path of consecutive non-task nodes from a given starting point.
     */
    private int measureGatewayChainDepth(String nodeId, Map<String, List<String>> adjacency, Set<String> visited, int currentDepth) {
        if (!visited.add(nodeId)) return currentDepth;
        int maxDepth = currentDepth;
        for (String neighbor : adjacency.getOrDefault(nodeId, Collections.emptyList())) {
            maxDepth = Math.max(maxDepth, measureGatewayChainDepth(neighbor, adjacency, visited, currentDepth + 1));
        }
        visited.remove(nodeId);
        return maxDepth;
    }

    /**
     * BFS from start event to verify all nodes are reachable.
     */
    private void validateAllNodesReachable(String startId, Map<String, List<String>> adjacency, Set<String> allNodeIds, Collection<FlowNode> flowNodes, BpmnValidationResult result) {
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();
        queue.add(startId);
        visited.add(startId);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            for (String neighbor : adjacency.getOrDefault(current, Collections.emptyList())) {
                if (visited.add(neighbor)) {
                    queue.add(neighbor);
                }
            }
        }

        Set<String> unreachable = new HashSet<>(allNodeIds);
        unreachable.removeAll(visited);
        if (!unreachable.isEmpty()) {
            String names = unreachable.stream()
                .map(id -> getNodeName(findById(id, flowNodes)))
                .collect(Collectors.joining(", "));
            result.addError("The following steps are not reachable from the Start Event: " + names);
        }
    }

    /**
     * Verifies that from every non-end node, at least one path leads to an end event.
     * Uses reverse BFS from all end events.
     */
    private void validateAllPathsReachEnd(Map<String, List<String>> reverseAdjacency, Set<String> allNodeIds, Set<String> endEventIds, Collection<FlowNode> flowNodes, BpmnValidationResult result) {
        if (endEventIds.isEmpty()) return; // already reported

        // BFS backward from all end events
        Set<String> canReachEnd = new HashSet<>(endEventIds);
        Queue<String> queue = new LinkedList<>(endEventIds);
        while (!queue.isEmpty()) {
            String current = queue.poll();
            for (String predecessor : reverseAdjacency.getOrDefault(current, Collections.emptyList())) {
                if (canReachEnd.add(predecessor)) {
                    queue.add(predecessor);
                }
            }
        }

        Set<String> deadEnds = new HashSet<>(allNodeIds);
        deadEnds.removeAll(canReachEnd);
        if (!deadEnds.isEmpty()) {
            String names = deadEnds.stream()
                .map(id -> getNodeName(findById(id, flowNodes)))
                .collect(Collectors.joining(", "));
            result.addError("The following steps have no path to any End Event (dead ends): " + names);
        }
    }

    // ========================================================================
    // Gateway validations
    // ========================================================================

    private void validateGatewayTypes(Collection<Gateway> gateways, BpmnValidationResult result) {
        for (Gateway gateway : gateways) {
            if (!(gateway instanceof ExclusiveGateway)) {
                result.addError("Only Exclusive (XOR) Gateways are supported. Found unsupported gateway '"
                        + getNodeName(gateway) + "'");
            }
        }
    }

    private void validateGatewaySemantics(Collection<Gateway> gateways, Collection<SequenceFlow> allFlows, BpmnValidationResult result) {
        for (Gateway gateway : gateways) {
            if (!(gateway instanceof ExclusiveGateway)) continue;

            List<SequenceFlow> outgoing = allFlows.stream()
                    .filter(f -> f.getSource().getId().equals(gateway.getId()))
                    .collect(Collectors.toList());
            List<SequenceFlow> incoming = allFlows.stream()
                    .filter(f -> f.getTarget().getId().equals(gateway.getId()))
                    .collect(Collectors.toList());

            String gatewayLabel = getNodeName(gateway);

            // XOR as split (diverging)
            if (outgoing.size() > 1) {
                long flowsWithCondition = outgoing.stream()
                        .filter(f -> {
                            ConditionExpression condition = f.getConditionExpression();
                            if (condition == null) {
                                return false;
                            }
                            String text = condition.getTextContent();
                            return text != null && !text.isBlank();
                        })
                        .count();
                long flowsWithoutCondition = outgoing.size() - flowsWithCondition;

                if (flowsWithoutCondition == 0) {
                    result.addError("XOR Gateway '" + gatewayLabel + "' has no default fallback transition. "
                            + "Exactly one default (unconditional) transition is required to act as a fallback when all other conditions are false");
                } else if (flowsWithoutCondition > 1) {
                    result.addError("XOR Gateway '" + gatewayLabel + "' has " + flowsWithoutCondition
                            + " outgoing transitions without conditions. Exactly one default (unconditional) transition is required to act as a fallback");
                }
            }
        }
    }

    // ========================================================================
    // Task validations
    // ========================================================================

    private void validateTaskNames(Collection<Task> tasks, BpmnValidationResult result) {
        for (Task task : tasks) {
            if (task.getName() == null || task.getName().isBlank()) {
                result.addError("A task step must have a non-blank name");
            }
        }
    }

    private void validateTaskNameUniqueness(Collection<Task> tasks, BpmnValidationResult result) {
        Map<String, Long> nameCounts = tasks.stream()
                .filter(t -> t.getName() != null && !t.getName().isBlank())
                .collect(Collectors.groupingBy(Task::getName, Collectors.counting()));

        nameCounts.entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .forEach(e -> result.addError("Task name '" + e.getKey()
                        + "' is used " + e.getValue() + " times — task names must be unique for clear audit trails"));
    }

    private void validateAssigneeRoles(Collection<Task> tasks, BpmnValidationResult result) {
        for (Task task : tasks) {
            List<String> assignees = new ArrayList<>();
            for (Documentation doc : task.getDocumentations()) {
                String text = doc.getTextContent();
                if (text != null && text.startsWith(ASSIGNEE_PREFIX)) {
                    assignees.add(text.substring(ASSIGNEE_PREFIX.length()).trim());
                }
            }

            if (assignees.isEmpty()) {
                result.addError("Step '" + getNodeName(task) + "' must have a responsible role selected");
            } else if (assignees.size() > 1) {
                result.addError("Step '" + getNodeName(task) + "' has multiple assignees defined. Exactly one required");
            } else {
                String roleName = assignees.getFirst();
                if (roleName.isBlank()) {
                    result.addError("Step '" + getNodeName(task) + "' must have a responsible role selected");
                } else {
                    try {
                        UserRole r = UserRole.valueOf(roleName);
                        boolean hasTeamLeaderDoc = task.getDocumentations().stream()
                                .map(Documentation::getTextContent)
                                .filter(Objects::nonNull)
                                .anyMatch(text -> text.startsWith(TEAM_LEADER_PREFIX) && text.substring(13).trim().equalsIgnoreCase("true"));
                        if (r == UserRole.ADMINISTRATOR) {
                            result.addError("Step '" + getNodeName(task) + "' cannot be assigned to ADMINISTRATOR. Administrators are system supervisors and cannot act as task approvers.");
                        }
                        if (hasTeamLeaderDoc && r != UserRole.REQUESTER) {
                            result.addError("Step '" + getNodeName(task) + "' cannot have Team Leader option enabled for non-REQUESTER roles.");
                        }
                    } catch (IllegalArgumentException e) {
                        result.addError("Invalid role assigned in BPMN: " + roleName);
                    }
                }
            }
        }
    }

    private void validateTaskDescriptions(Collection<Task> tasks, BpmnValidationResult result) {
        for (Task task : tasks) {
            for (Documentation doc : task.getDocumentations()) {
                String text = doc.getTextContent();
                if (text != null && !text.isBlank()
                        && !text.startsWith(ASSIGNEE_PREFIX)
                        && !text.startsWith("[AUTO_APPROVE]")
                        && !text.startsWith(TEAM_LEADER_PREFIX)) {
                    if (text.length() > 500) {
                        result.addError("Step '" + getNodeName(task) + "' description must be at most 500 characters");
                    }
                }
            }
        }
    }

    // ========================================================================
    // Condition expression validations
    // ========================================================================

    private void validateConditionExpressions(Collection<SequenceFlow> flows, BpmnValidationResult result) {
        for (SequenceFlow flow : flows) {
            ConditionExpression conditionExpression = flow.getConditionExpression();
            if (conditionExpression == null) continue;

            String rawText = conditionExpression.getTextContent();
            if (rawText == null || rawText.isBlank()) continue;

            String cleanCondition = rawText.replace("${", "").replace("}", "").trim();
            if (cleanCondition.isEmpty()) continue;

            validateSpelExpression(cleanCondition, flow.getSource(), flow.getTarget(), result, "Condition");
        }
    }

    private void validateSpelExpression(String cleanExpression, FlowNode source, FlowNode target, BpmnValidationResult result, String contextDesc) {
        if (cleanExpression == null || cleanExpression.isBlank()) return;

        // Security: check for unsafe SpEL constructs
        if (UNSAFE_SPEL_PATTERN.matcher(cleanExpression).find()) {
            result.addError(contextDesc + " on transition from '" + getNodeName(source) + "' to '" + getNodeName(target)
                    + "' contains potentially unsafe expressions (method calls, type references, or bean access are not allowed)");
            log.warn("Unsafe SpEL detected on transition from {}: {}", getNodeName(source), cleanExpression);
            return;
        }

        // Syntax: try to parse the SpEL expression
        SpelExpression parsedExpression;
        try {
            parsedExpression = (SpelExpression) spelParser.parseExpression(cleanExpression);
        } catch (ParseException e) {
            result.addError(contextDesc + " on transition from '" + getNodeName(source) + "' to '" + getNodeName(target)
                    + "' has invalid syntax: " + e.getMessage());
            log.warn("Failed to parse SpEL condition on transition from {}: {}", getNodeName(source), e.getMessage());
            return;
        }

        // Semantic: check that referenced properties are known
        validateExpressionProperties(parsedExpression, source, target, result, contextDesc);

        // Type check: check that expression evaluates to a boolean
        try {
            EvaluationContext evalContext = SimpleEvaluationContext.forReadOnlyDataBinding().build();
            Object value = parsedExpression.getValue(evalContext, WorkflowBranchingContext.createDummyContext());
            if (!(value instanceof Boolean)) {
                result.addError(contextDesc + " on transition from '" + getNodeName(source) + "' to '" + getNodeName(target)
                        + "' must evaluate to a boolean, but returned type: " + (value == null ? "null" : value.getClass().getSimpleName()));
            }
        } catch (EvaluationException e) {
            result.addError(contextDesc + " on transition from '" + getNodeName(source) + "' to '" + getNodeName(target)
                    + "' failed to evaluate: '" + cleanExpression + "'");
        }
    }

    private void validateExpressionProperties(SpelExpression expression, FlowNode source, FlowNode target, BpmnValidationResult result, String contextDesc) {
        List<String> paths = new ArrayList<>();
        SpelNode ast = expression.getAST();
        if (ast != null) {
            collectPropertyPaths(ast, paths);
        }

        for (String path : paths) {
            if (!ALLOWED_BRANCHING_PATHS.contains(path)) {
                result.addError(contextDesc + " on transition from '" + getNodeName(source) + "' to '" + getNodeName(target) + "' references property '"
                        + path + "' which is not an allowed field");
            }
        }
    }

    private void collectPropertyPaths(SpelNode node, List<String> paths) {
        if (node instanceof CompoundExpression) {
            StringBuilder pathBuilder = new StringBuilder();
            boolean isPurePropertyChain = true;
            for (int i = 0; i < node.getChildCount(); i++) {
                SpelNode child = node.getChild(i);
                if (child instanceof PropertyOrFieldReference) {
                    if (pathBuilder.length() > 0) {
                        pathBuilder.append(".");
                    }
                    pathBuilder.append(((PropertyOrFieldReference) child).getName());
                } else {
                    isPurePropertyChain = false;
                    break;
                }
            }
            if (isPurePropertyChain && pathBuilder.length() > 0) {
                paths.add(pathBuilder.toString());
                return;
            }
        } else if (node instanceof PropertyOrFieldReference) {
            paths.add(((PropertyOrFieldReference) node).getName());
            return;
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            collectPropertyPaths(node.getChild(i), paths);
        }
    }

    // ========================================================================
    // Transition rule validations
    // ========================================================================

    private void validateTransitionRules(Collection<SequenceFlow> flows, Collection<FlowNode> flowNodes, BpmnValidationResult result) {
        Set<String> taskIds = flowNodes.stream()
                .filter(n -> n instanceof Task)
                .map(FlowNode::getId)
                .collect(Collectors.toSet());

        for (SequenceFlow flow : flows) {
            ExtensionElements extensionElements = flow.getExtensionElements();
            if (extensionElements == null) continue;

            boolean hasTransitionRule = extensionElements.getDomElement().getChildElements().stream()
                    .anyMatch(child -> "transitionRule".equals(child.getLocalName()));

            if (!hasTransitionRule) continue;

            // Validate that transition rules are only on flows from tasks (and not entering end events)
            String sourceId = flow.getSource().getId();
            if (flow.getSource() instanceof StartEvent) {
                result.addError("Transition rules cannot be added on the first transition leaving the Start Event.");
            } else if (flow.getSource() instanceof Gateway) {
                result.addError("Transition rules cannot be added after XOR gateways. Only branching conditions are allowed on transitions leaving a gateway.");
            } else if (flow.getTarget() instanceof EndEvent) {
                result.addError("Transition rules cannot be added on the last transition entering the End Event.");
            } else if (!taskIds.contains(sourceId)) {
                result.addError("Transition rule on a transition leaving '" + getNodeName(flow.getSource())
                        + "' originates from a non-task step — transition rules must be on transitions leaving tasks");
            }

            // Validate minRequiredVendors
            extensionElements.getDomElement().getChildElements().forEach(child -> {
                if ("transitionRule".equals(child.getLocalName())) {
                    String minVendors = child.getAttribute("minRequiredVendors");
                    if (minVendors != null && !minVendors.isBlank()) {
                        try {
                            int value = Integer.parseInt(minVendors);
                            if (value < 0) {
                                result.addError("Transition rule on a transition leaving '" + getNodeName(flow.getSource())
                                        + "' has negative minRequiredVendors (" + value + ")");
                            }
                        } catch (NumberFormatException e) {
                            result.addError("Transition rule on a transition leaving '" + getNodeName(flow.getSource())
                                    + "' has non-numeric minRequiredVendors: '" + minVendors + "'");
                        }
                    }
                    String minReliability = child.getAttribute("minVendorReliabilityScore");
                    if (minReliability != null && !minReliability.isBlank()) {
                        try {
                            double value = Double.parseDouble(minReliability);
                            if (value < 0) {
                                result.addError("Transition rule on a transition leaving '" + getNodeName(flow.getSource())
                                        + "' has negative minVendorReliabilityScore (" + value + ")");
                            } else if (value > 10.0) {
                                result.addError("Transition rule on a transition leaving '" + getNodeName(flow.getSource())
                                        + "' has minVendorReliabilityScore exceeding the maximum of 10.0 (" + value + ")");
                            }
                        } catch (NumberFormatException e) {
                            result.addError("Transition rule on a transition leaving '" + getNodeName(flow.getSource())
                                    + "' has non-numeric minVendorReliabilityScore: '" + minReliability + "'");
                        }
                    }
                    String requiredFileTypes = child.getAttribute("requiredFileTypes");
                    if (requiredFileTypes != null && !requiredFileTypes.isBlank()) {
                        for (String type : requiredFileTypes.split(",")) {
                            String trimmed = type.trim().toUpperCase();
                            if (trimmed.isEmpty()) continue;
                            try {
                                AttachmentType.valueOf(trimmed);
                            } catch (IllegalArgumentException e) {
                                result.addError("Transition rule on a transition leaving '" + getNodeName(flow.getSource())
                                        + "' has invalid required file type: '" + type + "'. Allowed types: " + Arrays.toString(AttachmentType.values()));
                            }
                        }
                    }
                    String advancedRule = child.getAttribute("advancedRule");
                    if (advancedRule != null && !advancedRule.isBlank()) {
                        validateSpelExpression(advancedRule, flow.getSource(), flow.getTarget(), result, "Advanced rule");
                    }
                }
            });
        }
    }

    private String getNodeName(FlowNode node) {
        if (node == null) return "Unknown Step";
        if (node.getName() != null && !node.getName().isBlank()) {
            return node.getName();
        }
        if (node instanceof StartEvent) return "Start Event";
        if (node instanceof EndEvent) return "End Event";
        if (node instanceof Gateway) return "Unnamed Gateway";
        return "Unnamed Step";
    }

    private FlowNode findById(String id, Collection<FlowNode> flowNodes) {
        return flowNodes.stream().filter(n -> id.equals(n.getId())).findFirst().orElse(null);
    }

}
