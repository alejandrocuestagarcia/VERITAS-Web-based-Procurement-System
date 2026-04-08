package com.veritas.backend.project;

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
    public List<Object> getAllProjects() {
        return List.of();
    }

    @Operation(summary = "Get project", description = "Retrieves a project.")
    @GetMapping("/{id}")
    public List<Object> getProject(@PathVariable Long id) {
        return List.of();
    }

    @Operation(summary = "Edit project", description = "Edits a projects basic info.")
    @PatchMapping("/{id}")
    public String editProject(@PathVariable Long id) {
        return "Project " + id + " edited";
    }
}
