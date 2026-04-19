package com.veritas.backend.project.controller;

import com.veritas.backend.config.annotations.IsFinanceOfficer;
import com.veritas.backend.config.annotations.IsRequester;
import com.veritas.backend.project.dto.ProjectDto;
import com.veritas.backend.project.dto.ProjectEditDto;
import com.veritas.backend.project.service.ProjectService;
import com.veritas.backend.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
@Tag(name = "Project Module", description = "Management of company projects")
public class ProjectController {
    private final ProjectService projectService;

    @Operation(summary = "List projects", description = "Retrieves all projects.")
    @IsRequester
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ProjectDto> getAllProjects(@AuthenticationPrincipal User user) {
        return projectService.getProjectsForUser(user);
    }

    @Operation(summary = "Get project", description = "Retrieves a project.")
    @IsRequester
    @GetMapping("/{id}")
    public ProjectDto getProject(@PathVariable Long id) {
        return null;
    }

    @Operation(summary = "Edit project", description = "Edits a projects basic info.")
    @IsFinanceOfficer
    @PatchMapping("/{id}")
    public ProjectDto editProject(@PathVariable Long id, @RequestBody ProjectEditDto updates) {
        return null;
    }
}
