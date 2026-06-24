package com.veritas.backend.team.service;

import com.veritas.backend.team.dto.TeamCreateDto;
import com.veritas.backend.team.dto.TeamDto;
import com.veritas.backend.team.dto.TeamEditDto;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;

import java.util.List;

public interface TeamService {

    /**
     * Returns all teams, optionally including inactive (deactivated) teams.
     *
     * @param includeInactive if true, includes deactivated teams
     * @return a list of {@link TeamDto}
     */
    List<TeamDto> getAllTeams(boolean includeInactive);

    /**
     * Returns a single team by ID.
     *
     * @param id the team ID
     * @return the matching {@link TeamDto}
     * @throws EntityNotFoundException if no team exists with the given ID
     */
    TeamDto getTeam(Long id);

    /**
     * Creates a new team, optionally assigning a leader and initial members.
     * All assigned users must be requesters not already belonging to another team,
     * and the leader must not already lead another team.
     *
     * @param request the team creation payload
     * @return the created {@link TeamDto}
     * @throws EntityExistsException if a team with the same name already exists, or the leader already leads another team
     * @throws EntityNotFoundException if the department or leader user is not found
     * @throws IllegalArgumentException if any user is already assigned to another team, is not a requester, or other assignment constraint is violated
     */
    TeamDto createTeam(TeamCreateDto request);

    /**
     * Updates an existing team's name, description, department, leader, and/or member list.
     * Only non-null fields in the payload are applied. If a member list is provided, it is
     * fully reconciled against the current members. Users not in the new list are removed,
     * and new users are added and validated.
     *
     * @param id the ID of the team to edit
     * @param edits the edit payload
     * @return the updated {@link TeamDto}
     * @throws EntityNotFoundException if the team, department, or leader is not found
     * @throws EntityExistsException if the new name is already taken
     * @throws IllegalArgumentException if assignment constraints are violated (e.g. conflicting leader, non-requester user, user already on another team)
     */
    TeamDto editTeam(Long id, TeamEditDto edits);

    /**
     * Deletes or deactivates a team by ID.
     * If the team has no projects, it is hard-deleted.
     * If the team has only inactive projects, it is soft-deleted (deactivated).
     * Deletion is blocked if the team has active projects or assigned users.
     *
     * @param id the ID of the team to delete
     * @return a boolean flag indicating whether the deletion was soft deleted (true) or not
     * @throws EntityNotFoundException if no team exists with the given ID
     * @throws IllegalArgumentException if the team still has active projects or assigned users
     */
    boolean deleteTeam(Long id);
}