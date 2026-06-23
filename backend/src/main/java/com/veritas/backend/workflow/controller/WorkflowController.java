package com.veritas.backend.workflow.controller;

import com.veritas.backend.config.annotations.IsFinanceOfficer;
import com.veritas.backend.config.annotations.IsRequester;
import com.veritas.backend.workflow.dto.SpelFieldDto;
import com.veritas.backend.workflow.dto.WorkflowDto;
import com.veritas.backend.workflow.dto.WorkflowEditDto;
import com.veritas.backend.workflow.dto.WorkflowSaveDto;
import com.veritas.backend.workflow.service.WorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.net.URI;
import java.util.List;
import java.util.Arrays;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

import com.veritas.backend.user.entity.User;
import com.veritas.backend.workflow.entity.AttachmentType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/workflows", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Workflow Module", description = "Management of procurement process templates and BPMN logic")
public class WorkflowController {

    private final WorkflowService workflowService;

    @IsRequester
    @Operation(summary = "List workflows", description = "Retrieves a list of workflows with optional filtering.")
    @GetMapping
    public ResponseEntity<Page<WorkflowDto>> getAllWorkflows(
            Pageable pageable,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean isActive,
            @AuthenticationPrincipal User user
    ) {
        log.info("GET /workflows – page: {}, size: {}, search: '{}', isActive, user: {}", pageable.getPageNumber(), pageable.getPageSize(), search, isActive, user != null ? user.getEmail() : "null");
        Page<WorkflowDto> workflows = workflowService.getAllWorkflows(pageable, search, isActive, user);

        return ResponseEntity.ok(workflows);
    }

    @IsRequester
    @Operation(summary = "Get workflow", description = "Retrieves a workflow.")
    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WorkflowDto> getWorkflow(@PathVariable Long id, @AuthenticationPrincipal User user) {
        log.info("GET /workflows/{}", id);
        return ResponseEntity.ok(workflowService.getWorkflow(id, user));
    }

    @IsFinanceOfficer
    @Operation(summary = "Save workflow", description = "Saves a new workflow configuration (BPMN/XML) created in the editor.")
    @PostMapping(consumes = "application/json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WorkflowDto> saveWorkflow(@Valid @RequestBody WorkflowSaveDto workflowData) {
        log.info("POST /workflows");
        WorkflowDto workflowDto = workflowService.createWorkflow(workflowData);
        URI workflowURI = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(workflowDto.id()).toUri();
        return ResponseEntity.created(workflowURI).body(workflowDto);
    }

    @IsFinanceOfficer
    @Operation(summary = "Edit Workflow", description = "Edits an existing workflow.")
    @PatchMapping(path = "/{id}", consumes = "application/json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WorkflowDto> editWorkflow(@PathVariable Long id, @Valid @RequestBody WorkflowEditDto workflowEditDto) {
        log.info("PATCH /workflows/{}", id);
        WorkflowDto workflowDto = workflowService.editWorkflow(id, workflowEditDto);
        return ResponseEntity.ok(workflowDto);
    }

    @IsFinanceOfficer
    @Operation(summary = "Delete workflow", description = "Deactivates a workflow.")
    @DeleteMapping(value = "/{id}")
    public ResponseEntity<Void> deleteWorkflow(@PathVariable Long id) {
        log.info("DELETE /workflows/{}", id);
        workflowService.deleteWorkflow(id);
        return ResponseEntity.noContent().build();
    }

    @IsRequester
    @Operation(summary = "Get SpEL fields", description = "Returns the list of allowed SpEL fields for use in routing expressions and transition rules.")
    @GetMapping("/spel-fields")
    public ResponseEntity<List<SpelFieldDto>> getSpelFields() {
        log.info("GET /workflows/spel-fields");
        return ResponseEntity.ok(workflowService.getSpelFields());
    }

    @IsRequester
    @Operation(summary = "Get allowed attachment types", description = "Retrieves the list of supported attachment file types and their labels.")
    @GetMapping("/attachment-types")
    public ResponseEntity<Map<String, String>> getAttachmentTypes() {
        log.info("GET /workflows/attachment-types");
        Map<String, String> types = Arrays.stream(AttachmentType.values())
                .collect(Collectors.toMap(
                        t -> t.name().toLowerCase(),
                        AttachmentType::getLabel,
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                ));
        return ResponseEntity.ok(types);
    }
}