package com.veritas.backend.project;

import com.veritas.backend.project.dto.ProjectDto;
import com.veritas.backend.project.dto.ProjectEditDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/projects")
@Tag(name = "Project Module", description = "Management of company projects")
public class ProjectController {
    @Operation(summary = "List projects", description = "Retrieves all projects.")
    @GetMapping
    public List<ProjectDto> getAllProjects() {
        return List.of();
    }

    @Operation(summary = "Get project", description = "Retrieves a project.")
    @GetMapping("/{id}")
    public ProjectDto getProject(@PathVariable Long id) {
        return new ProjectDto();
    }

    @Operation(summary = "Edit project", description = "Edits a projects basic info.")
    @PatchMapping("/{id}")
    public ProjectDto editProject(@PathVariable Long id, @RequestBody ProjectEditDto updates) {
        return new ProjectDto();
    }
}
