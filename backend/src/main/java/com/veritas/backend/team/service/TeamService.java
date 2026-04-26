package com.veritas.backend.team.service;

import com.veritas.backend.team.dto.TeamCreateDto;
import com.veritas.backend.team.dto.TeamDto;
import com.veritas.backend.team.dto.TeamEditDto;
import java.util.List;

public interface TeamService {
    List<TeamDto> getAllTeams();

    TeamDto getTeam(Long id);

    TeamDto createTeam(TeamCreateDto request);

    TeamDto editTeam(Long id, TeamEditDto edits);
}