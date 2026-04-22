package com.veritas.backend.user.service.impl;

import com.veritas.backend.team.entity.Team;
import com.veritas.backend.team.repository.TeamRepository;
import com.veritas.backend.user.dto.UserCreationRequestDto;
import com.veritas.backend.user.dto.UserDto;
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

    // TODO: Restore this once the team feature is fully implemented.
    // Team team = teamRepository.findById(userDto.teamId()).orElseThrow(
    // () -> new EntityNotFoundException("Team with id " + userDto.teamId() + " not
    // found"));

    // Fallback: Pick the first available team since team feature is incomplete
    java.util.List<Team> teams = teamRepository.findAll();
    if (teams.isEmpty()) {
      throw new IllegalStateException("Cannot create user: No teams available in the system.");
    }
    Team team = teams.get(0);

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
      team.setLeader(user);
      teamRepository.save(team);
    }

    return userMapper.toUserDto(savedUser);


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
