package com.veritas.backend.workflow;

import com.veritas.backend.workflow.dto.WorkflowDto;
import com.veritas.backend.workflow.dto.WorkflowSaveDto;
import com.veritas.backend.workflow.mapper.WorkflowMapper;
import com.veritas.backend.workflow.repository.WorkflowDefinitionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/workflows", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Workflow Module", description = "Management of procurement process templates and BPMN logic")
public class WorkflowController {

    private final WorkflowDefinitionRepository workflowDefinitionRepository;
    private final WorkflowMapper workflowMapper;

    @Operation(summary = "List workflows", description = "Retrieves a list of all available procurement workflow templates.")
    @GetMapping
    public List<WorkflowDto> getAllWorkflows() {
        return workflowDefinitionRepository.findAllByIsActiveTrue().stream()
                .map(workflowMapper::toDto)
                .collect(Collectors.toList());
    }

    @Operation(summary = "Save workflow", description = "Saves a new workflow configuration (BPMN/JSON) created in the editor.")
    @PostMapping
    public WorkflowDto saveWorkflow(@RequestBody WorkflowSaveDto workflowData) {
        return new WorkflowDto();
    }

    @Operation(summary = "Export workflow JSON", description = "Returns the JSON representation of a specific workflow for backup or sharing.")
    @GetMapping("/{id}/export")
    public WorkflowDto exportWorkflow(@PathVariable Long id) {
        WorkflowDto dto = new WorkflowDto();
        dto.setId(id);
        dto.setName("Standard Procurement");
        dto.setVersion("1.0");
        return dto;
    }

    @Operation(summary = "Import workflow JSON", description = "Accepts a JSON file or body to create a new workflow template.")
    @PostMapping("/import")
    public WorkflowDto importWorkflow(@RequestBody WorkflowSaveDto workflowJson) {
        return new WorkflowDto();
    }
}