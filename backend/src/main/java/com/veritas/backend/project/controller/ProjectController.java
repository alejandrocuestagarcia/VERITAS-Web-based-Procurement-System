package com.veritas.backend.project.controller;

import com.veritas.backend.config.annotations.IsFinanceOfficer;
import com.veritas.backend.config.annotations.IsRequester;
import com.veritas.backend.project.dto.ProjectCreationDto;
import com.veritas.backend.project.dto.ProjectDto;
import com.veritas.backend.project.dto.ProjectEditDto;
import com.veritas.backend.project.service.ProjectService;
import com.veritas.backend.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
@Tag(name = "Project Module", description = "Management of company projects")
public class ProjectController {
    private final ProjectService projectService;

    @Operation(summary = "List projects", description = "Retrieves all projects.")
    @IsRequester
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ProjectDto>> getAllProjects(@AuthenticationPrincipal User user) {
        log.info("GET /projects – requested by user: {}", user.getEmail());
        return ResponseEntity.ok(projectService.getProjectsForUser(user));
    }

    @Operation(summary = "Get project", description = "Retrieves a project.")
    @IsRequester
    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProjectDto> getProject(@PathVariable Long id,@AuthenticationPrincipal User user) {
        log.info("GET /projects/{} as user {}", id,user.getEmail());
        return ResponseEntity.ok(projectService.getProjectById(id,user));
    }

    @Operation(summary = "Edit project", description = "Edits a projects basic info.")
    @IsFinanceOfficer
    @PatchMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProjectDto> editProject(@PathVariable Long id, @RequestBody @Valid ProjectEditDto updatedProject) {
        log.info("PATCH /projects/{}", id);
        return ResponseEntity.ok(projectService.editProject(id, updatedProject));
    }

    @Operation(summary = "Create project", description = "Creates a project")
    @IsFinanceOfficer
    @PostMapping(consumes = "application/json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProjectDto> createProject(@Valid @RequestBody ProjectCreationDto projectCreationDto) {
        log.info("POST /projects – name: {}, key: {}", projectCreationDto.name(), projectCreationDto.projectKey());
        ProjectDto projectDto = projectService.createProject(projectCreationDto);
        URI projectURI = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(projectDto.id()).toUri();
        log.info("Project created successfully – id: {}", projectDto.id());
        return ResponseEntity.created(projectURI).body(projectDto);
    }

    @Operation(summary = "Delete project", description = "Deletes an existing project.")
    @DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @IsFinanceOfficer
    public ResponseEntity<Void> deleteProject(@PathVariable Long id) {
        log.info("DELETE /projects/{}", id);
        projectService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }
}

