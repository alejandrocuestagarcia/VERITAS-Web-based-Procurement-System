package com.veritas.backend.workflow;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/workflows")
@Tag(name = "Workflow Module", description = "Management of procurement process templates and BPMN logic")
public class WorkflowController {

    @Operation(summary = "List workflows", description = "Retrieves a list of all available procurement workflow templates.")
    @GetMapping
    public List<Object> getAllWorkflows() {
        return List.of();
    }

    @Operation(summary = "Save workflow", description = "Saves a new workflow configuration (BPMN/JSON) created in the editor.")
    @PostMapping
    public String saveWorkflow(@RequestBody Map<String, Object> workflowData) {
        return "Workflow saved successfully";
    }

    @Operation(summary = "Export workflow JSON", description = "Returns the JSON representation of a specific workflow for backup or sharing.")
    @GetMapping("/{id}/export")
    public Map<String, Object> exportWorkflow(@PathVariable Long id) {
        return Map.of(
                "workflowId", id,
                "name", "Standard Procurement",
                "version", "1.0",
                "nodes", List.of()
        );
    }

    @Operation(summary = "Import workflow JSON", description = "Accepts a JSON file or body to create a new workflow template.")
    @PostMapping("/import")
    public String importWorkflow(@RequestBody Map<String, Object> workflowJson) {
        return "Workflow imported successfully";
    }
}