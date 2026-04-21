package com.veritas.backend.team;

import com.veritas.backend.config.annotations.IsFinanceOfficer;
import com.veritas.backend.team.dto.TeamCreateDto;
import com.veritas.backend.team.dto.TeamDto;
import com.veritas.backend.team.dto.TeamEditDto;
import com.veritas.backend.team.service.TeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/teams", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Teams Module", description = "Management of company teams")
public class TeamController {
    private final TeamService teamService;

    @Operation(summary = "List teams", description = "Retrieves all teams.")
    @IsFinanceOfficer
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<TeamDto> getAllTeams() {
        return teamService.getAllTeams();
    }

    @Operation(summary = "Get team", description = "Retrieves a team.")
    @IsFinanceOfficer
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public TeamDto getTeam(@PathVariable Long id) {
        return teamService.getTeam(id);
    }

    @Operation(summary = "Create team", description = "Creates a new team.")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @IsFinanceOfficer
    public TeamDto createTeam(@Valid @RequestBody TeamCreateDto request) {
        return teamService.createTeam(request);
    }

    @Operation(summary = "Edit team", description = "Edits a teams basic info.")
    @IsFinanceOfficer
    @PatchMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public TeamDto editTeam(@PathVariable Long id, @Valid @RequestBody TeamEditDto edits) {
        return teamService.editTeam(id, edits);
    }
}
