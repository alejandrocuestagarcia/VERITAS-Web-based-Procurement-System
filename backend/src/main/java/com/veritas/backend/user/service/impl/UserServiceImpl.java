package com.veritas.backend.user.service.impl;

import com.veritas.backend.auth.repository.RefreshTokenRepository;
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
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.veritas.backend.requisition.repository.RequestRepository;
import com.veritas.backend.requisition.mapper.RequisitionMapper;
import com.veritas.backend.requisition.dto.RequisitionDto;
import com.veritas.backend.requisition.entity.Request;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  private final TeamRepository teamRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final UserMapper userMapper;
  private final RequestRepository requestRepository;
  private final RequisitionMapper requisitionMapper;

  @Override
  @Transactional
  public UserDto createUser(UserCreationRequestDto userDto) {
    log.info("Creating user with email: {}, role: {}", userDto.email(), userDto.role());

    if (userRepository.existsByEmail(userDto.email())) {
      log.warn("User creation failed – email already registered: {}", userDto.email());
      throw new EntityExistsException("Email already registered");
    }

    Team team = teamRepository.findById(userDto.teamId())
        .orElseThrow(() -> new EntityNotFoundException("Team with id " + userDto.teamId() + " not found"));
    log.debug("Assigned user to team: {} (id={})", team.getName(), team.getTeamId());

    if (userDto.promoteToTeamLeader() && !team.getDepartment().equals(userDto.department())) {
      log.warn("Team leader promotion rejected – department mismatch: user={}, team={}", userDto.department(), team.getDepartment());
      throw new IllegalArgumentException("A team leader must belong to the same department as the team.");
    }

    User user = userMapper.toUser(userDto);
    user.setPasswordHash(passwordEncoder.encode(userDto.password()));
    user.setTeam(team);
    user.setIsActive(true);
    user.setDepartment(userDto.department());
    user.setRequiresPasswordChange(true);

    User savedUser = userRepository.save(user);
    log.info("User persisted – id: {}, email: {}", savedUser.getId(), savedUser.getEmail());

    if (userDto.promoteToTeamLeader()) {
      team.setLeader(savedUser);
      teamRepository.save(team);
      log.info("User promoted to team leader for team: {} (id={})", team.getName(), team.getTeamId());
    }

    return userMapper.toUserDto(savedUser);


  }

  @Override
  @Transactional
  public UserDto editUser(Long id, UserEditDto edits) {
    User user = userRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("User not found"));

    boolean changingTeam = edits.teamId() != null && (user.getTeam() == null || !edits.teamId().equals(user.getTeam().getTeamId()));
    if (changingTeam && Boolean.TRUE.equals(edits.isTeamLeader())) {
      throw new IllegalArgumentException("Cannot change team and promote to leader in the same request. Change team first, then promote.");
    }

    if (edits.email() != null && !edits.email().equals(user.getEmail())) {
      if (userRepository.existsByEmail(edits.email())) {
        throw new EntityExistsException("Email already registered");
      }
      user.setEmail(edits.email());
    }

    if (edits.name() != null)
      user.setName(edits.name());

    if (edits.role() != null)
      user.setRole(edits.role());

    if (edits.department() != null)
      user.setDepartment(edits.department());

    if (edits.teamId() != null) {
      if (changingTeam) {
        Team newTeam = teamRepository.findById(edits.teamId()).orElseThrow(() -> new EntityNotFoundException("Team not found"));
        user.setTeam(newTeam);
      }
    } else {
      user.setTeam(null);
    }

    User saved = userRepository.save(user);

    boolean isCurrentlyLeader = saved.getTeam() != null && saved.getTeam().getLeader() != null
            && saved.getTeam().getLeader().getId().equals(saved.getId());
    if (isCurrentlyLeader && (edits.isTeamLeader() == null || !edits.isTeamLeader())) {
      saved.getTeam().setLeader(null);
      teamRepository.save(saved.getTeam());
    } else if (Boolean.TRUE.equals(edits.isTeamLeader())) {
      if (saved.getTeam() == null) {
        throw new IllegalStateException("Cannot set team leader: user has no team assigned.");
      }

      saved.getTeam().setLeader(saved);
      teamRepository.save(saved.getTeam());
    }

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

    return userMapper.toUserEditDto(user, isTeamLeader);
  }

  @Override
  @Transactional
  public Page<UserDto> getAllUsers(Pageable pageable) {
    log.debug("Fetching all users – page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());
    return userRepository.findAll(pageable).map(userMapper::toUserDto);

  }

  @Override
  public UserStatsDto getUserStats() {
    long active = userRepository.countByIsActiveTrue();
    long inactive = userRepository.countByIsActiveFalse();

    log.debug("User stats – active: {}, inactive: {}", active, inactive);
    return new UserStatsDto(active, inactive, 0);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<UserDto> getAllUsersFiltered(Pageable pageable, String filter, UserRole userRole) {
    log.debug("Fetching filtered users – filter: '{}', role: {}, page: {}", filter, userRole, pageable.getPageNumber());
    String query = (filter != null && !filter.isBlank()) ? "%" + filter.trim().toLowerCase() + "%" : null;

    return userRepository.findAllFiltered(query, userRole, pageable).map(userMapper::toUserDto);
  }

  @Override
  @Transactional(readOnly = true)
  public List<RequisitionDto> getPendingRequisitionsForUser(Long userId) {
      return requestRepository.findActiveRequestsByUserId(userId).stream()
              .map(requisitionMapper::toDto)
              .toList();
  }

  @Override
  @Transactional
  public void deleteUser(Long id, Long fallbackUserId) {

    Optional<User> user = userRepository.findById(id);

    if (user.isPresent()) {
      User actualUser = user.get();
      actualUser.setIsActive(false);
      actualUser.setDeletedAt(LocalDateTime.now());

      if (fallbackUserId != null) {
          User fallbackUser = userRepository.findById(fallbackUserId)
                  .orElseThrow(() -> new EntityNotFoundException("Fallback user not found"));
          List<Request> activeRequests = requestRepository.findActiveRequestsByUserId(actualUser.getId());
          for (Request req : activeRequests) {
              req.setUserID(fallbackUser);
          }
          requestRepository.saveAll(activeRequests);
      }

      if (actualUser.getTeam() != null) {

        Team team = actualUser.getTeam();

        if (team.getLeader().equals(actualUser)) {
          team.setLeader(null);
          teamRepository.save(team);
        }
        actualUser.setTeam(null);

      }

      refreshTokenRepository.deleteByUserId(actualUser.getId());

      userRepository.save(actualUser);
    }

  }
}
