package com.veritas.backend.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.veritas.backend.common.model.Department;
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
import com.veritas.backend.user.service.impl.UserServiceImpl;
import jakarta.persistence.EntityExistsException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
//AI-GENERATED
@ExtendWith(MockitoExtension.class)
class UserServiceUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void CreateUser_ValidUser_SavesAndReturnsUser() {
        UserCreationRequestDto request = new UserCreationRequestDto(
                "test@veritas.corp", "Test User", "password123",
                UserRole.REQUESTER, 1L, Department.IT, false);

        Team mockTeam = new Team();
        mockTeam.setDepartment(Department.IT);

        User mappedUser = new User();
        User savedUser = new User();
        UserDto expectedDto = new UserDto(1L, "Test User", "test@veritas.com", UserRole.FINANCE_OFFICER, "IT Team",
                LocalDateTime.now());

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(teamRepository.findById(1L)).thenReturn(java.util.Optional.of(mockTeam));
        when(userMapper.toUser(request)).thenReturn(mappedUser);
        when(passwordEncoder.encode(request.password())).thenReturn("hashedPassword");
        when(userRepository.save(mappedUser)).thenReturn(savedUser);
        when(userMapper.toUserDto(savedUser)).thenReturn(expectedDto);

        UserDto result = userService.createUser(request);

        assertNotNull(result);
        assertEquals(expectedDto.email(), result.email());
        verify(userRepository).save(mappedUser);
        verify(teamRepository, never()).save(any());
    }

    @Test
    void CreateUser_DuplicateEmail_ThrowsEntityExistsException() {
        UserCreationRequestDto request = new UserCreationRequestDto(
                "duplicate@veritas.com", "Test User", "password123",
                UserRole.FINANCE_OFFICER, 1L, Department.IT, false);

        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        assertThrows(EntityExistsException.class, () -> userService.createUser(request));
        verify(teamRepository, never()).findAll();
    }

    @Test
    void CreateUser_TeamNotFound_ThrowsEntityNotFoundException() {
        UserCreationRequestDto request = new UserCreationRequestDto(
                "test@veritas.com", "Test User", "password123",
                UserRole.PROCUREMENT_OFFICER, 1L, Department.IT, false);

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(teamRepository.findById(1L)).thenReturn(java.util.Optional.empty());

        assertThrows(jakarta.persistence.EntityNotFoundException.class, () -> userService.createUser(request));
        verify(userRepository, never()).save(any());
    }

  @Test
  void GetAllUsersFiltered_FilterIsNotEmpty_FormatsQuery() {
    Pageable pageable = PageRequest.of(0, 10);
    when(userRepository.findAllFiltered("%alex%", UserRole.REQUESTER, pageable)).thenReturn(
        new PageImpl<>(List.of()));

    userService.getAllUsersFiltered(pageable, "  ALEX  ", UserRole.REQUESTER);

    verify(userRepository).findAllFiltered("%alex%", UserRole.REQUESTER, pageable);
  }

  @Test
  void GetUserStats_Called_ReturnsMappedStats() {
    when(userRepository.count()).thenReturn(100L);
    when(userRepository.countByIsActiveFalse()).thenReturn(15L);

    UserStatsDto stats = userService.getUserStats();

    assertThat(stats.total()).isEqualTo(100);
    assertThat(stats.inactive()).isEqualTo(15);
//    assertThat(stats.activeSessions()).isZero();
  }

    @Test
    void editUser_shouldThrowEntityNotFoundException_whenUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> userService.editUser(99L, new UserEditDto(null, null, null, null)));
        verify(userRepository, never()).save(any());
    }

    @Test
    void editUser_shouldChangeTeam_whenUserIsNotLeader() {
        Team oldTeam = new Team();
        oldTeam.setTeamId(1L);
        oldTeam.setLeader(null);

        Team newTeam = new Team();
        newTeam.setTeamId(2L);

        User user = new User();
        user.setId(1L);
        user.setTeam(oldTeam);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(teamRepository.findById(2L)).thenReturn(Optional.of(newTeam));
        when(userRepository.save(user)).thenReturn(user);

        userService.editUser(1L, new UserEditDto(null, null, 2L, null));

        assertEquals(newTeam, user.getTeam());
    }

    @Test
    void editUser_shouldSetUserAsTeamLeader_whenIsTeamLeaderTrue() {
        Team team = new Team();
        team.setTeamId(1L);

        User user = new User();
        user.setId(1L);
        user.setTeam(team);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        userService.editUser(1L, new UserEditDto(null, null, null, true));

        assertEquals(user, team.getLeader());
        verify(teamRepository).save(team);
    }

    @Test
    void editUser_shouldDemoteLeader_whenIsTeamLeaderFalse() {
        Team team = new Team();
        team.setTeamId(1L);

        User user = new User();
        user.setId(1L);
        user.setTeam(team);
        team.setLeader(user);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        userService.editUser(1L, new UserEditDto(null, null, null, false));

        assertNull(team.getLeader());
        verify(teamRepository).save(team);
    }

    @Test
    void editUser_shouldNotDemote_whenUserIsNotCurrentLeader() {
        Team team = new Team();
        team.setTeamId(1L);

        User user = new User();
        user.setId(1L);
        user.setTeam(team);

        User leader = new User();
        leader.setId(2L);
        team.setLeader(leader);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        userService.editUser(1L, new UserEditDto(null, null, null, false));

        assertEquals(leader, team.getLeader());
        verify(teamRepository, never()).save(any());
    }

}
