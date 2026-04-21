package com.veritas.backend.team.service.impl;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import com.veritas.backend.team.dto.TeamCreateDto;
import com.veritas.backend.team.dto.TeamDto;
import com.veritas.backend.team.dto.TeamEditDto;
import com.veritas.backend.team.entity.Team;
import com.veritas.backend.team.repository.TeamRepository;
import com.veritas.backend.team.service.TeamService;
import com.veritas.backend.user.entity.User;
import com.veritas.backend.user.repository.UserRepository;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TeamServiceImpl implements TeamService {
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TeamDto> getAllTeams() {
        return teamRepository.findAll().stream()
                .map(this::convertTeamToTeamDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TeamDto getTeam(Long id) {
        Team team = teamRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new EntityNotFoundException("Team not found with id " + id));

        return convertTeamToTeamDto(team);
    }

    @Override
    @Transactional
    public TeamDto createTeam(TeamCreateDto request) {
        String teamName = request.getName().trim();
        if (teamRepository.existsByNameIgnoreCase(teamName)) {
            throw new EntityExistsException("Team with name '" + teamName + "' already exists");
        }

        Team team = new Team();
        team.setName(teamName);
        team.setDescription(request.getDescription().trim());
        team.setDepartment(request.getDepartment());
        team.setExpiresAt(request.getExpiresAt());
        team.setIsActive(true);

        if (request.getLeaderId() != null) {
            User leader = userRepository.findById(Objects.requireNonNull(request.getLeaderId()))
                    .orElseThrow(() -> new EntityNotFoundException("Leader not found with id " + request.getLeaderId()));
            team.setLeader(leader);
        }

        Team savedTeam = teamRepository.save(Objects.requireNonNull(team));
        return convertTeamToTeamDto(savedTeam);
    }

    @Override
    @Transactional
    public TeamDto editTeam(Long id, TeamEditDto edits) {
        Team team = teamRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new EntityNotFoundException("Team not found with id " + id));

        if (edits.getName() != null) {
            String updatedName = edits.getName().trim();
            if (updatedName.isEmpty()) {
                throw new IllegalArgumentException("Team name must not be blank");
            }

            boolean nameChanged = !updatedName.equalsIgnoreCase(team.getName());
            if (nameChanged && teamRepository.existsByNameIgnoreCase(updatedName)) {
                throw new EntityExistsException("Team with name '" + updatedName + "' already exists");
            }

            team.setName(updatedName);
        }

        Team updatedTeam = teamRepository.save(Objects.requireNonNull(team));
        return convertTeamToTeamDto(updatedTeam);
    }

    private TeamDto convertTeamToTeamDto(Team team) {
        TeamDto dto = new TeamDto();
        dto.setId(team.getTeamId());
        dto.setName(team.getName());
        dto.setDescription(team.getDescription());
        dto.setDepartment(team.getDepartment() != null ? team.getDepartment().name() : null);
        dto.setLeaderId(team.getLeader() != null ? team.getLeader().getId() : null);
        dto.setIsActive(team.getIsActive());
        dto.setExpiresAt(team.getExpiresAt());
        dto.setCreatedAt(team.getCreatedAt());
        return dto;
    }
}