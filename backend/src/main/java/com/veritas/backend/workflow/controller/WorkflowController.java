package com.veritas.backend.workflow.controller;

import com.veritas.backend.config.annotations.IsFinanceOfficer;
import com.veritas.backend.workflow.dto.WorkflowDto;
import com.veritas.backend.workflow.dto.WorkflowEditDto;
import com.veritas.backend.workflow.dto.WorkflowSaveDto;
import com.veritas.backend.workflow.service.WorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/workflows")
@IsFinanceOfficer
@Tag(name = "Workflow Module", description = "Management of procurement process templates and BPMN logic")
public class WorkflowController {

    private final WorkflowService workflowService;

    @Operation(summary = "List workflows", description = "Retrieves a list of all available procurement workflow templates.")
    @GetMapping
    public List<WorkflowDto> getAllWorkflows() {
        return List.of();
    }

    @Operation(summary = "Get workflow", description = "Retrieves a workflow.")
    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WorkflowDto> getWorkflow(@PathVariable Long id) {
        return ResponseEntity.ok(workflowService.getWorkflow(id));
    }

    @Operation(summary = "Save workflow", description = "Saves a new workflow configuration (BPMN/XML) created in the editor.")
    @PostMapping(consumes = "application/json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WorkflowDto> saveWorkflow(@Valid @RequestBody WorkflowSaveDto workflowData) {
        WorkflowDto workflowDto = workflowService.createWorkflow(workflowData);
        URI workflowURI = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(workflowDto.id()).toUri();
        return ResponseEntity.created(workflowURI).body(workflowDto);
    }

    @Operation(summary = "Edit Workflow", description = "Edits an existing workflow.")
    @PatchMapping(path = "/{id}", consumes = "application/json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WorkflowDto> editWorkflow(@PathVariable Long id, @Valid @RequestBody WorkflowEditDto workflowEditDto) {
        WorkflowDto workflowDto = workflowService.editWorkflow(id, workflowEditDto);
        return ResponseEntity.ok(workflowDto);
    }
}