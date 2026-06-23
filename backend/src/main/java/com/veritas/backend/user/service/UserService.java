package com.veritas.backend.user.service;

import com.veritas.backend.requisition.dto.RequisitionDto;
import com.veritas.backend.user.dto.UserCreationRequestDto;
import com.veritas.backend.user.dto.UserDto;
import com.veritas.backend.user.dto.UserEditDto;
import com.veritas.backend.user.dto.UserStatsDto;
import com.veritas.backend.user.entity.User;
import com.veritas.backend.user.entity.UserRole;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserService {

    /**
     * Creates a new active user with an encoded password and a forced password change flag.
     * Requesters must be assigned to a team; procurement officers to a department.
     * Optionally promotes a requester to team leader if the team has no existing leader.
     *
     * @param userDto the user creation payload
     * @return the created {@link UserDto}
     * @throws EntityExistsException if the email is already registered
     * @throws EntityNotFoundException if the specified team or department is not found
     * @throws IllegalArgumentException if a required team or department is missing, or the team already has a leader
     */
    UserDto createUser(UserCreationRequestDto userDto);

    /**
     * Updates an existing user's profile, role, team or department assignment, and team leader status.
     * If a role change results in permission loss, all refresh tokens for the user are invalidated.
     * Team and department assignments are reconciled based on the user's current role.
     *
     * @param id the ID of the user to edit
     * @param edits the edit payload
     * @return the updated {@link UserDto}
     * @throws EntityNotFoundException if the user, team, or department is not found
     * @throws EntityExistsException if the new email is already registered
     * @throws IllegalArgumentException if assignment constraints are violated
     */
    UserDto editUser(Long id, UserEditDto edits, User currentUser);

    /**
     * Returns a user's current data in edit form, including whether they are their team's leader.
     *
     * @param id the ID of the user
     * @return the {@link UserEditDto} populated with current values
     * @throws EntityNotFoundException if no user exists with the given ID
     */
    UserEditDto getUserByIdForEdit(Long id);

    /**
     * Returns all users as a paginated list.
     *
     * @param pageable pagination and sorting parameters
     * @return a page of {@link UserDto}
     */
    Page<UserDto> getAllUsers(Pageable pageable);

    /**
     * Returns a count of active and inactive users.
     *
     * @return a {@link UserStatsDto} with active and inactive counts
     */
    UserStatsDto getUserStats();

    /**
     * Returns a paginated, optionally filtered list of users, with an optional role filter.
     *
     * @param pageable pagination and sorting parameters
     * @param filter optional search string matched against user fields
     * @param userRole optional role filter; if null, all roles are included
     * @return a page of matching {@link UserDto}
     */
    Page<UserDto> getAllUsersFiltered(Pageable pageable, String filter, UserRole userRole);

    /**
     * Returns all active requisitions assigned to the given user.
     *
     * @param userId the ID of the user
     * @return a list of {@link RequisitionDto}
     */
    List<RequisitionDto> getPendingRequisitionsForUser(Long userId);

    /**
     * Soft-deletes a user by marking them as inactive, clearing their team and department,
     * and invalidating all their refresh tokens. If a fallback user is provided, all active
     * requests are reassigned to them. A user cannot delete their own account.
     *
     * @param id the ID of the user to delete
     * @param fallbackUserId optional ID of a requester from the same team to inherit active requests
     * @param currentUser the authenticated user performing the deletion
     * @throws IllegalArgumentException if the user attempts to delete themselves, or the fallback user is invalid
     * @throws EntityNotFoundException if the fallback user is not found
     */
    void deleteUser(Long id, Long fallbackUserId, User currentUser);

    /**
     * Returns a paginated list of all active users with the REQUESTER role.
     *
     * @param pageable pagination and sorting parameters
     * @return a page of {@link UserDto}
     */
    Page<UserDto> getAllRequesters(Pageable pageable);

    /**
     * Returns all active users with the given role.
     *
     * @param role the role to filter by
     * @return a list of {@link UserDto}
     */
    List<UserDto> getUsersByRole(UserRole role);

    /**
     * Retrieves a user by their ID and maps them to a DTO.
     *
     * @param id the ID of the user
     * @return the mapped {@link UserDto}
     * @throws EntityNotFoundException if the user is not found
     */
    UserDto getUserDtoById(Long id);
}
