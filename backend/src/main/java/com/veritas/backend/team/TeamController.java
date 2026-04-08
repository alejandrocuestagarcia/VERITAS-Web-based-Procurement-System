package com.veritas.backend.team;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/teams")
@Tag(name = "Teams Module", description = "Management of company teams")
public class TeamController {
    @Operation(summary = "List teams", description = "Retrieves all teams.")
    @GetMapping
    public List<Object> getAllTeams() {
        return List.of();
    }

    @Operation(summary = "Get team", description = "Retrieves a team.")
    @GetMapping("/{id}")
    public List<Object> getTeam(@PathVariable Long id) {
        return List.of();
    }

    @Operation(summary = "Edit team", description = "Edits a teams basic info.")
    @PatchMapping("/{id}")
    public String editTeam(@PathVariable Long id) {
        return "Team " + id + " edited";
    }

}
