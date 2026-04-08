package com.veritas.backend.workflow;

import com.veritas.backend.workflow.dto.WorkflowDto;
import com.veritas.backend.workflow.dto.WorkflowSaveDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/workflows")
@Tag(name = "Workflow Module", description = "Management of procurement process templates and BPMN logic")
public class WorkflowController {

    @Operation(summary = "List workflows", description = "Retrieves a list of all available procurement workflow templates.")
    @GetMapping
    public List<WorkflowDto> getAllWorkflows() {
        return List.of();
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