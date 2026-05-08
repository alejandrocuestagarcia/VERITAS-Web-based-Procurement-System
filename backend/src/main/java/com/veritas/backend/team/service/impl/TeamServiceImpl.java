package com.veritas.backend.team.service.impl;

import com.veritas.backend.department.entity.Department;
import com.veritas.backend.department.repository.DepartmentRepository;
import com.veritas.backend.user.dto.UserDto;
import com.veritas.backend.user.mapper.UserMapper;
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

import java.util.ArrayList;
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
    private final DepartmentRepository departmentRepository;
    private final UserMapper userMapper;

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

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new EntityNotFoundException("Department with id '" + request.getDepartmentId() + "' not found"));

        Team team = new Team();
        team.setName(teamName);
        team.setDescription(request.getDescription().trim());
        team.setDepartment(department);
        team.setExpiresAt(request.getExpiresAt());
        team.setIsActive(true);

        if (request.getLeaderId() != null) {
            User leader = userRepository.findById(Objects.requireNonNull(request.getLeaderId())).orElseThrow(() -> new EntityNotFoundException("Leader not found with id " + request.getLeaderId()));

            if (teamRepository.existsByLeaderId(request.getLeaderId())) {
                throw new EntityExistsException("User '" + leader.getName() + "' is already a leader of another team");
            }

            team.setLeader(leader);
        }

        Team savedTeam = teamRepository.save(Objects.requireNonNull(team));

        List<Long> allMemberIds = new ArrayList<>();
        if (request.getLeaderId() != null)
            allMemberIds.add(request.getLeaderId());
        if (request.getMemberIds() != null)
            allMemberIds.addAll(request.getMemberIds());

        if (!allMemberIds.isEmpty()) {
            List<User> usersToAssign = userRepository.findAllById(allMemberIds);

            for (User user : usersToAssign) {
                boolean isLeaderOfAnotherTeam = teamRepository.existsByLeaderIdAndTeamIdNot(user.getId(), savedTeam.getTeamId());
                if (isLeaderOfAnotherTeam) {
                    throw new IllegalStateException("User '" + user.getName() + "' is already a leader of another team and cannot be assigned as a member.");
                }
            }

            usersToAssign.forEach(user -> user.setTeam(savedTeam));
            userRepository.saveAll(usersToAssign);
        }

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
        List<UserDto> members = this.userRepository.findAllByTeamTeamId(team.getTeamId())
                .stream()
                .map(userMapper::toUserDto)
                .toList();

        TeamDto dto = new TeamDto();
        dto.setId(team.getTeamId());
        dto.setName(team.getName());
        dto.setDescription(team.getDescription());
        dto.setDepartment(team.getDepartment() != null ? team.getDepartment().getName() : null);
        dto.setLeaderId(team.getLeader() != null ? team.getLeader().getId() : null);
        dto.setMembers(members);
        dto.setIsActive(team.getIsActive());
        dto.setExpiresAt(team.getExpiresAt());
        dto.setCreatedAt(team.getCreatedAt());
        return dto;
    }
}