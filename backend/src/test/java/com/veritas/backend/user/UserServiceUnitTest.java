package com.veritas.backend.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.veritas.backend.department.entity.Department;
import com.veritas.backend.department.repository.DepartmentRepository;
import com.veritas.backend.auth.repository.RefreshTokenRepository;
import com.veritas.backend.requisition.dto.RequisitionDto;
import com.veritas.backend.requisition.entity.Priority;
import com.veritas.backend.requisition.entity.Request;
import com.veritas.backend.requisition.mapper.RequisitionMapper;
import com.veritas.backend.requisition.repository.RequestRepository;
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
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private RequisitionMapper requisitionMapper;

    @Mock
    private RequestRepository requestRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Spy
    private UserMapper userMapper = Mappers.getMapper(UserMapper.class);

    @InjectMocks
    private UserServiceImpl userService;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    @Captor
    private ArgumentCaptor<List<Request>> requestCaptor;

    @Captor
    private ArgumentCaptor<Team> teamCaptor;

    private User testUser;
    private User testFallBackUser;
    private Request testRequest;
    private Team testTeam;

    @BeforeEach
    void setUp() {
        testTeam = new Team();
        testTeam.setTeamId(1L);
        testTeam.setName("Engineering");
        testTeam.setLeader(null);


        testUser = new User();
        testUser.setId(1L);
        testUser.setName("Test User");
        testUser.setTeam(testTeam);

        testFallBackUser = new User();
        testFallBackUser.setId(2L);
        testFallBackUser.setName("Test FallBack User");
        testFallBackUser.setTeam(testTeam);


        testRequest = new Request();
        testRequest.setRequestID(1L);
        testRequest.setRequestName("Test Request");
        testRequest.setUserID(testUser);
        testRequest.setTeamID(testTeam);
    }

    @Test
    void GetPendingRequisitionForUser_ValidUser_ReturnOneRequest() {
        when(requestRepository.findActiveRequestsByUserId(1L)).thenReturn(List.of(testRequest));


        RequisitionDto expectedDto =
            new RequisitionDto(1L, "Test Request", "", "", false, Priority.MEDIUM, "", "", "", "", "", "", null, null, "", "", "", null, null, null, null, null);
        when(requisitionMapper.toDto(any(Request.class))).thenReturn(expectedDto);


        List<RequisitionDto> result = userService.getPendingRequisitionsForUser(1L);

        assertNotNull(result);
        assertThat(result).hasSize(1);
        assertEquals("Test Request", result.getFirst().requestName());


    }

    @Test
    void DeleteUser_ValidUserWithoutFallBackUser_DeactivatesUser() {

        testUser.setTeam(null);


        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        userService.deleteUser(1L, null);

        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();

        assertFalse(savedUser.getIsActive());
        assertNotNull(savedUser.getDeletedAt());


        verify(userRepository, times(1)).findById(1L);
        verify(refreshTokenRepository, times(1)).deleteByUserId(1L);
        verify(teamRepository, never()).save(any(Team.class));


    }


    @Test
    void DeleteUser_ValidUserWithFallBackUserAndWithoutTeam_DeactivatesUser() {

        testUser.setTeam(null);


        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testFallBackUser));
        when(requestRepository.findActiveRequestsByUserId(1L)).thenReturn(List.of(testRequest));

        userService.deleteUser(1L, 2L);

        verify(userRepository).save(userCaptor.capture());

        verify(requestRepository).saveAll(requestCaptor.capture());

        List<Request> requests = requestCaptor.getValue();
        assertNotNull(requests);
        assertThat(requests).hasSize(1);
        assertEquals("Test Request", requests.getFirst().getRequestName());
        assertEquals(testFallBackUser, requests.getFirst().getUserID());

        verify(requestRepository, times(1)).findActiveRequestsByUserId(1L);


        User savedUser = userCaptor.getValue();

        assertFalse(savedUser.getIsActive());
        assertNotNull(savedUser.getDeletedAt());

        verify(teamRepository, never()).save(any(Team.class));

        verify(userRepository, times(1)).findById(1L);
        verify(refreshTokenRepository, times(1)).deleteByUserId(1L);


    }


    @Test
    void DeleteUser_ValidUserWithFallBackUserAndWithTeamAndNotTeamLeader_DeactivatesUser() {


        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        userService.deleteUser(1L, null);


        verify(userRepository).save(userCaptor.capture());


        User savedUser = userCaptor.getValue();

        assertNull(savedUser.getTeam());
        assertFalse(savedUser.getIsActive());
        assertNotNull(savedUser.getDeletedAt());

        verify(teamRepository, never()).save(any(Team.class));

        verify(userRepository, times(1)).findById(1L);
        verify(refreshTokenRepository, times(1)).deleteByUserId(1L);


    }

    @Test
    void DeleteUser_ValidUserWithFallBackUserAndWithTeamAndTeamLeader_DeactivatesUser() {

        testTeam.setLeader(testUser);


        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        userService.deleteUser(1L, null);


        verify(userRepository).save(userCaptor.capture());


        verify(teamRepository).save(teamCaptor.capture());


        Team savedTeam = teamCaptor.getValue();

        assertNull(savedTeam.getLeader());

        User savedUser = userCaptor.getValue();

        assertNull(savedUser.getTeam());
        assertFalse(savedUser.getIsActive());
        assertNotNull(savedUser.getDeletedAt());


        verify(userRepository, times(1)).findById(1L);
        verify(refreshTokenRepository, times(1)).deleteByUserId(1L);


    }

    @Test
    void DeleteUser_FallbackUserDoesNotExist_ThrowsEntityNotFoundException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> {
            userService.deleteUser(1L, 999L);
        });

        verify(requestRepository, never()).save(any(Request.class));
        verify(requestRepository, never()).saveAll(anyList());
        verify(userRepository, never()).save(any(User.class));
    }


    @Test
    void DeleteUser_InValidUser_DoesNotDeactivateAnyUser() {


        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        userService.deleteUser(999L, null);


        verify(userRepository).findById(999L);
        verify(refreshTokenRepository, never()).deleteByUserId(anyLong());
        verify(userRepository, never()).save(any(User.class));


    }

    //AI-GENERATED
    @Test
    void CreateUser_ValidUser_SavesAndReturnsUser() {
        UserCreationRequestDto request = new UserCreationRequestDto(
                "test@veritas.corp", "Test User", "password123",
                UserRole.REQUESTER, 1L, null, false);

        Team mockTeam = new Team();
        mockTeam.setTeamId(1L);

        User mappedUser = new User();
        User savedUser = new User();
        UserDto expectedDto = new UserDto(1L, "Test User", "test@veritas.corp", true, UserRole.REQUESTER, "IT Team", null, LocalDateTime.now());

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
    void CreateUser_AdminWithoutTeam_SavesSuccessfully() {
        UserCreationRequestDto request = new UserCreationRequestDto(
                "admin@veritas.corp", "Admin User", "password123",
                UserRole.ADMINISTRATOR, null, null, false);

        User mappedUser = new User();
        User savedUser = new User();
        UserDto expectedDto = new UserDto(1L, "Admin User", "admin@veritas.corp", true, UserRole.ADMINISTRATOR, null, null, LocalDateTime.now());

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(userMapper.toUser(request)).thenReturn(mappedUser);
        when(passwordEncoder.encode(request.password())).thenReturn("hashedPassword");
        when(userRepository.save(mappedUser)).thenReturn(savedUser);
        when(userMapper.toUserDto(savedUser)).thenReturn(expectedDto);

        UserDto result = userService.createUser(request);

        assertNotNull(result);
        verify(teamRepository, never()).findById(any());
        verify(userRepository).save(mappedUser);
        assertNull(mappedUser.getTeam());
    }

    @Test
    void CreateUser_RequesterWithoutTeam_ThrowsIllegalArgumentException() {
        UserCreationRequestDto request = new UserCreationRequestDto(
                "test@veritas.corp", "Requester", "password123",
                UserRole.REQUESTER, null, null, false);

        when(userRepository.existsByEmail(request.email())).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> userService.createUser(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void CreateUser_ProcurementOfficerWithDepartment_SavesSuccessfully() {
        UserCreationRequestDto request = new UserCreationRequestDto(
                "pro@veritas.corp", "Procurement", "password123",
                UserRole.PROCUREMENT_OFFICER, null, 1L, false);

        Department dept = new Department();
        dept.setDepartmentId(1L);

        User mappedUser = new User();
        User savedUser = new User();
        UserDto expectedDto = new UserDto(1L, "Procurement", "pro@veritas.corp", true, UserRole.PROCUREMENT_OFFICER, null, "IT", LocalDateTime.now());

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(dept));
        when(userMapper.toUser(request)).thenReturn(mappedUser);
        when(passwordEncoder.encode(request.password())).thenReturn("hashedPassword");
        when(userRepository.save(mappedUser)).thenReturn(savedUser);
        when(userMapper.toUserDto(savedUser)).thenReturn(expectedDto);

        UserDto result = userService.createUser(request);

        assertNotNull(result);
        assertEquals(dept, mappedUser.getDepartment());
        verify(userRepository).save(mappedUser);
    }

    @Test
    void CreateUser_DuplicateEmail_ThrowsEntityExistsException() {
        UserCreationRequestDto request = new UserCreationRequestDto(
                "duplicate@veritas.com", "Test User", "password123",
                UserRole.FINANCE_OFFICER, null, null, false);

        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        assertThrows(EntityExistsException.class, () -> userService.createUser(request));
        verify(teamRepository, never()).findAll();
    }

    @Test
    void CreateUser_TeamNotFound_ThrowsEntityNotFoundException() {
        UserCreationRequestDto request = new UserCreationRequestDto(
                "test@veritas.com", "Test User", "password123",
                UserRole.REQUESTER, 1L, null, false);

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
        when(userRepository.countByIsActiveTrue()).thenReturn(100L);
        when(userRepository.countByIsActiveFalse()).thenReturn(15L);

        UserStatsDto stats = userService.getUserStats();

        assertThat(stats.total()).isEqualTo(100);
        assertThat(stats.inactive()).isEqualTo(15);
        //assertThat(stats.activeSessions()).isZero();
    }

    @Test
    void EditUser_UserNotFound_ThrowsEntityNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> userService.editUser(99L, new UserEditDto(null, null, null, null, null, null)));
        verify(userRepository, never()).save(any());
    }

    @Test
    void EditUser_ValidTeamChangeWhenUserIsNotLeader_UpdatesTeam() {
        Team oldTeam = new Team();
        oldTeam.setTeamId(1L);
        oldTeam.setLeader(null);

        Team newTeam = new Team();
        newTeam.setTeamId(2L);

        User user = new User();
        user.setId(1L);
        user.setRole(UserRole.REQUESTER);
        user.setTeam(oldTeam);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(teamRepository.findById(2L)).thenReturn(Optional.of(newTeam));
        when(userRepository.save(user)).thenReturn(user);

        userService.editUser(1L, new UserEditDto(null, null, null, 2L, null, null));

        assertEquals(newTeam, user.getTeam());
    }

    @Test
    void EditUser_ChangeToAdmin_ClearsTeamAndDepartment() {
        Team team = new Team();
        team.setTeamId(1L);
        Department dept = new Department();
        dept.setDepartmentId(1L);

        User user = new User();
        user.setId(1L);
        user.setRole(UserRole.REQUESTER);
        user.setTeam(team);
        user.setDepartment(dept);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        userService.editUser(1L, new UserEditDto(null, null, UserRole.ADMINISTRATOR, null, null, null));

        assertNull(user.getTeam());
        assertNull(user.getDepartment());
        assertEquals(UserRole.ADMINISTRATOR, user.getRole());
    }

    @Test
    void EditUser_SetAsTeamLeaderWhenIsTeamLeaderTrue_SetsUserAsLeader() {
        Team team = new Team();
        team.setTeamId(1L);

        User user = new User();
        user.setId(1L);
        user.setTeam(team);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        userService.editUser(1L, new UserEditDto(null, null, null, 1L, null, true));

        assertEquals(user, team.getLeader());
        verify(teamRepository).save(team);
    }

    @Test
    void EditUser_UnsetTeamLeaderWhenIsTeamLeaderFalse_DemotesLeader() {
        Team team = new Team();
        team.setTeamId(1L);

        User user = new User();
        user.setId(1L);
        user.setTeam(team);
        team.setLeader(user);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        userService.editUser(1L, new UserEditDto(null, null, null, 1L, null, false));

        assertNull(team.getLeader());
        verify(teamRepository).save(team);
    }

    @Test
    void EditUser_UnsetTeamLeaderWhenUserIsNotCurrentLeader_KeepsExistingLeader() {
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

        userService.editUser(1L, new UserEditDto(null, null, null, null, null, false));

        assertEquals(leader, team.getLeader());
        verify(teamRepository, never()).save(any());
    }

    @Test
    void GetUserByIdForEdit_UserNotFound_ThrowsEntityNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> userService.getUserByIdForEdit(99L));
    }

    @Test
    void GetUserByIdForEdit_UserIsLeader_ReturnsIsTeamLeaderTrue() {
        Team team = new Team();
        team.setTeamId(1L);

        User user = new User();
        user.setId(1L);
        user.setEmail("leader@test.com");
        user.setName("Leader");
        user.setTeam(team);
        team.setLeader(user);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserEditDto result = userService.getUserByIdForEdit(1L);

        assertEquals(1L, result.teamId());
        assertTrue(result.isTeamLeader());
    }

    @Test
    void GetUserByIdForEdit_UserIsNotLeader_ReturnsIsTeamLeaderFalse() {
        Team team = new Team();
        team.setTeamId(1L);

        User leader = new User();
        leader.setId(2L);
        team.setLeader(leader);

        User user = new User();
        user.setId(1L);
        user.setEmail("member@test.com");
        user.setName("Member");
        user.setTeam(team);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserEditDto result = userService.getUserByIdForEdit(1L);

        assertEquals(1L, result.teamId());
        assertFalse(result.isTeamLeader());
    }

    @Test
    void CreateUser_NewAccount_SetsRequiresPasswordChangeToTrue() {
        UserCreationRequestDto request = new UserCreationRequestDto("newuser@veritas.com", "New User", "tempPass123", UserRole.REQUESTER, 1L, null, false);

        Team team = Team.builder().teamId(1L).build();

        User user = new User();

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
        when(userMapper.toUser(request)).thenReturn(user);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPass");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        userService.createUser(request);

        assertTrue(user.getRequiresPasswordChange());
    }

}
