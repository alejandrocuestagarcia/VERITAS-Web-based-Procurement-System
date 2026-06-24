package com.veritas.backend.team.service.impl;

import com.veritas.backend.department.entity.Department;
import com.veritas.backend.department.repository.DepartmentRepository;
import com.veritas.backend.project.repository.ProjectRepository;
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
import com.veritas.backend.user.entity.UserRole;
import com.veritas.backend.user.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeamServiceImpl implements TeamService {
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final ProjectRepository projectRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional(readOnly = true)
    public List<TeamDto> getAllTeams(boolean includeInactive) {
        log.debug("Fetching teams, includeInactive={}", includeInactive);
        return (includeInactive ? teamRepository.findAll() : teamRepository.findByIsActiveTrue()).stream()
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
                .orElseThrow(() -> new EntityNotFoundException(
                        "Department with id '" + request.getDepartmentId() + "' not found"));

        Team team = new Team();
        team.setName(teamName);
        team.setDescription(request.getDescription().trim());
        team.setDepartment(department);
        team.setExpiresAt(request.getExpiresAt());
        team.setIsActive(true);

        if (request.getLeaderId() != null) {
            User leader = userRepository.findById(Objects.requireNonNull(request.getLeaderId())).orElseThrow(
                    () -> new EntityNotFoundException("Leader not found with id " + request.getLeaderId()));

            if (leader.getRole() != UserRole.REQUESTER) {
                throw new IllegalArgumentException(
                        "User '" + leader.getName() + "' must be a requester to be a team leader");
            }

            if (teamRepository.existsByLeaderId(request.getLeaderId())) {
                throw new EntityExistsException("User '" + leader.getName() + "' is already a leader of another team");
            }

            if (leader.getTeam() != null) {
                throw new IllegalArgumentException("User '" + leader.getName() + "' is already assigned to team '"
                        + leader.getTeam().getName() + "'. Remove them from that team before assigning them here.");
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
                boolean isLeaderOfAnotherTeam = teamRepository.existsByLeaderIdAndTeamIdNot(user.getId(),
                        savedTeam.getTeamId());
                if (isLeaderOfAnotherTeam) {
                    throw new IllegalArgumentException("User '" + user.getName()
                            + "' is already a leader of another team. Remove the current leader first.");
                }

                if (user.getTeam() != null) {
                    throw new IllegalArgumentException("User '" + user.getName() + "' is already assigned to team '"
                            + user.getTeam().getName() + "'. Remove them from that team before assigning them here.");
                }

                if (user.getRole() != UserRole.REQUESTER) {
                    throw new IllegalArgumentException(
                            "User '" + user.getName() + "' must be a requester to be added to a team");
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

        if (!team.getIsActive()) {
            throw new IllegalStateException("Cannot edit a deactivated team");
        }

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

        if (edits.getDescription() != null) {
            String updatedDescription = edits.getDescription().trim();
            if (updatedDescription.isEmpty()) {
                throw new IllegalArgumentException("Team description must not be blank");
            }
            team.setDescription(updatedDescription);
        }

        if (edits.getDepartmentId() != null) {
            Department department = departmentRepository.findById(edits.getDepartmentId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Department with id '" + edits.getDepartmentId() + "' not found"));
            team.setDepartment(department);
        }

        if (Boolean.TRUE.equals(edits.getClearLeader()) && edits.getLeaderId() != null) {
            throw new IllegalArgumentException("Provide either a leader id or clear the current leader, not both.");
        }

        if (Boolean.TRUE.equals(edits.getClearLeader())) {
            team.setLeader(null);
        }

        if (edits.getLeaderId() != null) {
            User leader = userRepository.findById(edits.getLeaderId())
                    .orElseThrow(() -> new EntityNotFoundException("Leader not found with id " + edits.getLeaderId()));

            if (leader.getRole() != UserRole.REQUESTER) {
                throw new IllegalArgumentException(
                        "User '" + leader.getName() + "' must be a requester to be a team leader");
            }

            if (team.getLeader() != null && !team.getLeader().getId().equals(leader.getId())) {
                throw new IllegalArgumentException(
                        "Team already has a leader. Remove the current leader before assigning a new one.");
            }

            if (teamRepository.existsByLeaderIdAndTeamIdNot(leader.getId(), team.getTeamId())) {
                throw new IllegalArgumentException("User '" + leader.getName()
                        + "' is already a leader of another team. Remove the current leader first.");
            }

            if (leader.getTeam() != null && !leader.getTeam().getTeamId().equals(team.getTeamId())) {
                throw new IllegalArgumentException("User '" + leader.getName() + "' is already assigned to team '"
                        + leader.getTeam().getName() + "'. Remove them from that team before assigning them here.");
            }

            team.setLeader(leader);
        }

        Team updatedTeam = teamRepository.save(Objects.requireNonNull(team));

        if (edits.getMemberIds() != null) {
            syncTeamMembers(updatedTeam, edits.getMemberIds());
        } else if (updatedTeam.getLeader() != null) {
            ensureLeaderAssignment(updatedTeam.getLeader(), updatedTeam);
        }

        return convertTeamToTeamDto(updatedTeam);
    }

    @Override
    @Transactional
    public void deleteTeam(Long id) {
        Team team = teamRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new EntityNotFoundException("Team not found with id " + id));

        // Block deletion if there are assigned users
        if (userRepository.existsByTeamTeamId(id)) {
            throw new IllegalArgumentException("Cannot delete team because it still has assigned users.");
        }

        // Block deletion if there are active projects
        if (projectRepository.existsByTeamTeamIdAndIsActiveTrue(id)) {
            throw new IllegalArgumentException("Cannot delete team because it has active projects assigned to it.");
        }

        if (projectRepository.existsByTeamTeamId(id)) {
            // Soft-delete: team has only inactive projects, deactivate for audit/history
            team.setIsActive(false);
            team.setLeader(null);
            teamRepository.save(team);
            log.info("Team soft-deleted (deactivated) – id: {}, name: {}", id, team.getName());
        } else {
            // Hard-delete: no projects reference this team
            team.setLeader(null);
            teamRepository.save(team);
            teamRepository.delete(team);
            log.info("Team hard-deleted – id: {}, name: {}", id, team.getName());
        }
    }

    private void ensureLeaderAssignment(User leader, Team team) {
        if (leader.getTeam() == null || !leader.getTeam().getTeamId().equals(team.getTeamId())) {
            leader.setTeam(team);
            userRepository.save(leader);
        }
    }

    private void syncTeamMembers(Team team, List<Long> memberIds) {
        List<User> currentMembers = userRepository.findAllByTeamTeamId(team.getTeamId());

        List<Long> desiredMembers = memberIds == null ? List.of()
                : memberIds.stream()
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();

        List<Long> desiredWithLeader = new ArrayList<>(desiredMembers);
        if (team.getLeader() != null && !desiredWithLeader.contains(team.getLeader().getId())) {
            desiredWithLeader.add(team.getLeader().getId());
        }

        List<Long> currentIds = currentMembers.stream().map(User::getId).toList();

        List<Long> toRemove = currentIds.stream()
                .filter(id -> !desiredWithLeader.contains(id))
                .toList();

        List<Long> toAdd = desiredWithLeader.stream()
                .filter(id -> !currentIds.contains(id))
                .toList();

        if (!toRemove.isEmpty()) {
            List<User> usersToRemove = currentMembers.stream()
                    .filter(user -> toRemove.contains(user.getId()))
                    .toList();
            usersToRemove.forEach(user -> user.setTeam(null));
            userRepository.saveAll(usersToRemove);
        }

        if (!toAdd.isEmpty()) {
            List<User> usersToAdd = userRepository.findAllById(toAdd);
            List<Long> resolvedIds = usersToAdd.stream().map(User::getId).toList();
            List<Long> missingIds = toAdd.stream()
                    .filter(id -> !resolvedIds.contains(id))
                    .toList();

            if (!missingIds.isEmpty()) {
                throw new EntityNotFoundException("Users not found with ids " + missingIds);
            }

            for (User user : usersToAdd) {
                boolean isLeaderOfAnotherTeam = teamRepository.existsByLeaderIdAndTeamIdNot(user.getId(),
                        team.getTeamId());
                if (isLeaderOfAnotherTeam) {
                    throw new IllegalArgumentException("User '" + user.getName()
                            + "' is already a leader of another team. Remove the current leader first.");
                }

                if (user.getTeam() != null && !user.getTeam().getTeamId().equals(team.getTeamId())) {
                    throw new IllegalArgumentException("User '" + user.getName() + "' is already assigned to team '"
                            + user.getTeam().getName() + "'. Remove them from that team before assigning them here.");
                }

                if (user.getRole() != UserRole.REQUESTER) {
                    throw new IllegalArgumentException(
                            "User '" + user.getName() + "' must be a requester to be added to a team");
                }
            }

            usersToAdd.forEach(user -> user.setTeam(team));
            userRepository.saveAll(usersToAdd);
        }
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