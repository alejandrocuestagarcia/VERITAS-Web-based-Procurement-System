package com.veritas.backend.user;

import com.veritas.backend.common.model.Department;
import com.veritas.backend.team.entity.Team;
import com.veritas.backend.team.repository.TeamRepository;
import com.veritas.backend.user.dto.UserCreationRequestDto;
import com.veritas.backend.user.dto.UserDto;
import com.veritas.backend.user.entity.User;
import com.veritas.backend.user.entity.UserRole;
import com.veritas.backend.user.mapper.UserMapper;
import com.veritas.backend.user.repository.UserRepository;
import com.veritas.backend.user.service.impl.UserServiceImpl;
import jakarta.persistence.EntityExistsException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
//AI-GENERATED
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

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
    void createUser_Succeed_whenUserValid() {
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
        when(teamRepository.findAll()).thenReturn(List.of(mockTeam));
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
    void createUser_shouldEntityExistsException_whenDuplicateEmail() {
        UserCreationRequestDto request = new UserCreationRequestDto(
                "duplicate@veritas.com", "Test User", "password123",
                UserRole.FINANCE_OFFICER, 1L, Department.IT, false);

        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        assertThrows(EntityExistsException.class, () -> userService.createUser(request));
        verify(teamRepository, never()).findAll();
    }

    @Test
    void createUser_shouldThrowIllegalStateException_whenNoTeamsAvailable() {
        UserCreationRequestDto request = new UserCreationRequestDto(
                "test@veritas.com", "Test User", "password123",
                UserRole.PROCUREMENT_OFFICER, 1L, Department.IT, false);

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(teamRepository.findAll()).thenReturn(Collections.emptyList());

        assertThrows(IllegalStateException.class, () -> userService.createUser(request));
        verify(userRepository, never()).save(any());
    }
}
