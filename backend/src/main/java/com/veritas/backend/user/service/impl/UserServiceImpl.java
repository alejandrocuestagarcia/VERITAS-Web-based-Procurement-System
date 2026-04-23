package com.veritas.backend.user.service.impl;

import com.veritas.backend.team.entity.Team;
import com.veritas.backend.team.repository.TeamRepository;
import com.veritas.backend.user.dto.UserCreationRequestDto;
import com.veritas.backend.user.dto.UserDto;
import com.veritas.backend.user.dto.UserEditDto;
import com.veritas.backend.user.dto.UserStatsDto;
import com.veritas.backend.user.entity.User;
import com.veritas.backend.user.entity.UserRole;
import com.veritas.backend.user.mapper.UserMapper;
import com.veritas.backend.user.repository.UserRepository;
import com.veritas.backend.user.service.UserService;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  private final TeamRepository teamRepository;
  private final UserMapper userMapper;

  @Override
  @Transactional
  public UserDto createUser(UserCreationRequestDto userDto) {

    if (userRepository.existsByEmail(userDto.email())) {
      throw new EntityExistsException("Email already registered");
    }

    Team team = teamRepository.findById(userDto.teamId())
        .orElseThrow(() -> new EntityNotFoundException("Team with id " + userDto.teamId() + " not found"));

    if (userDto.promoteToTeamLeader() && !team.getDepartment().equals(userDto.department())) {
      throw new IllegalArgumentException("A team leader must belong to the same department as the team.");
    }

    User user = userMapper.toUser(userDto);
    user.setPasswordHash(passwordEncoder.encode(userDto.password()));
    user.setTeam(team);
    user.setIsActive(true);
    user.setDepartment(userDto.department());

    User savedUser = userRepository.save(user);

    if (userDto.promoteToTeamLeader()) {
      team.setLeader(savedUser);
      teamRepository.save(team);
    }

    return userMapper.toUserDto(savedUser);


  }

  @Override
  @Transactional
  public UserDto editUser(Long id, UserEditDto edits) {
    User user = userRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("User not found"));

    if (edits.email() != null && !edits.email().equals(user.getEmail())) {
      if (userRepository.existsByEmail(edits.email())) {
        throw new EntityExistsException("Email already registered");
      }
      user.setEmail(edits.email());
    }

    if (edits.name() != null) {
      user.setName(edits.name());
    }

    if (edits.teamId() != null) {
      // If user is team leader, prevent team change unless demoted
      if (user.getTeam() != null && user.getTeam().getLeader() != null && user.getTeam().getLeader().getId().equals(user.getId())
          && !edits.teamId().equals(user.getTeam().getTeamId())) {
        throw new IllegalArgumentException("Cannot change team while user is team leader. Demote first.");
      }

      Team newTeam = teamRepository.findById(edits.teamId()).orElseThrow(() -> new EntityNotFoundException("Team not found"));
      user.setTeam(newTeam);
    }

    if (edits.isTeamLeader() != null) {
      if (edits.isTeamLeader()) {
        if (user.getTeam() == null) {
          throw new IllegalStateException("Cannot set team leader: user has no team assigned.");
        }

        user.getTeam().setLeader(user);
        teamRepository.save(user.getTeam());
      } else {
        if (user.getTeam() != null && user.getTeam().getLeader() != null &&
                user.getTeam().getLeader().getId().equals(user.getId())) {
          user.getTeam().setLeader(null);
          teamRepository.save(user.getTeam());
        }
      }
    }

    if (edits.role() != null) {
      user.setRole(edits.role());
    }

    if (edits.department() != null) {
      user.setDepartment(edits.department());
    }

    User saved = userRepository.save(user);
    return userMapper.toUserDto(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public UserEditDto getUserByIdForEdit(Long id) {
    User user = userRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("User not found"));

    boolean isTeamLeader = user.getTeam() != null
            && user.getTeam().getLeader() != null
            && user.getTeam().getLeader().getId().equals(user.getId());

    return new UserEditDto(
            user.getEmail(),
            user.getName(),
            user.getRole(),
            user.getTeam() != null ? user.getTeam().getTeamId() : null,
            user.getDepartment(),
            isTeamLeader
    );
  }

  @Override
  @Transactional
  public Page<UserDto> getAllUsers(Pageable pageable) {

    return userRepository.findAll(pageable).map(userMapper::toUserDto);

  }

  @Override
  public UserStatsDto getUserStats() {
    long total = userRepository.count();

    long inactive = userRepository.countByIsActiveFalse();

    return new UserStatsDto(total, inactive, 0);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<UserDto> getAllUsersFiltered(Pageable pageable, String filter, UserRole userRole) {

    String query = (filter != null && !filter.isBlank()) ? "%" + filter.trim().toLowerCase() + "%" : null;

    return userRepository.findAllFiltered(query, userRole, pageable).map(userMapper::toUserDto);
  }
}
