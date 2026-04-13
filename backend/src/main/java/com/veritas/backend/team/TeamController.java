package com.veritas.backend.team;

import com.veritas.backend.team.dto.TeamDto;
import com.veritas.backend.team.dto.TeamEditDto;
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
    public List<TeamDto> getAllTeams() {
        return List.of();
    }

    @Operation(summary = "Get team", description = "Retrieves a team.")
    @GetMapping("/{id}")
    public TeamDto getTeam(@PathVariable Long id) {
        return new TeamDto();
    }

    @Operation(summary = "Edit team", description = "Edits a teams basic info.")
    @PatchMapping("/{id}")
    public TeamDto editTeam(@PathVariable Long id, @RequestBody TeamEditDto edits) {
        return new TeamDto();
    }
}
