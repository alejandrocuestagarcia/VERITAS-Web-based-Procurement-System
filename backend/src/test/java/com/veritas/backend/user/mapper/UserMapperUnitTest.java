package com.veritas.backend.user.mapper;

import static org.junit.jupiter.api.Assertions.*;

import com.veritas.backend.department.entity.Department;
import com.veritas.backend.team.entity.Team;
import com.veritas.backend.user.dto.UserCreationRequestDto;
import com.veritas.backend.user.dto.UserDto;
import com.veritas.backend.user.dto.UserEditDto;
import com.veritas.backend.user.entity.User;
import com.veritas.backend.user.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class UserMapperUnitTest {

    private UserMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(UserMapper.class);
    }

    @Test
    void toUser_NullDto_ReturnsNull() {
        assertNull(mapper.toUser(null));
    }

    @Test
    void toUser_ValidDto_MapsCorrectly() {
        UserCreationRequestDto dto = new UserCreationRequestDto(
            "max@example.com",
            "Max Mustermann",
            "password123",
            UserRole.REQUESTER,
            1L,
            2L,
            false
        );

        User entity = mapper.toUser(dto);

        assertAll(
            () -> assertEquals("Max Mustermann", entity.getName()),
            () -> assertEquals("max@example.com", entity.getEmail()),
            () -> assertNull(entity.getPassword()),
            () -> assertEquals(UserRole.REQUESTER, entity.getRole())
        );
    }

    @Test
    void toUserDto_NullUser_ReturnsNull() {
        assertNull(mapper.toUserDto(null));
    }

    @Test
    void toUserDto_WithNullAssociations_MapsCorrectly() {
        User user = new User();
        user.setId(10L);
        user.setName("Alice");
        user.setEmail("alice@example.com");
        user.setRole(UserRole.FINANCE_OFFICER);
        user.setIsActive(true);
        user.setTeam(null);
        user.setDepartment(null);

        UserDto dto = mapper.toUserDto(user);

        assertAll(
            () -> assertEquals(10L, dto.id()),
            () -> assertEquals("Alice", dto.name()),
            () -> assertEquals("alice@example.com", dto.email()),
            () -> assertEquals(UserRole.FINANCE_OFFICER, dto.role()),
            () -> assertTrue(dto.active()),
            () -> assertNull(dto.teamId()),
            () -> assertNull(dto.teamName()),
            () -> assertNull(dto.departmentId()),
            () -> assertNull(dto.departmentName())
        );
    }

    @Test
    void toUserDto_WithUserDepartment_MapsCorrectly() {
        Department department = new Department();
        department.setDepartmentId(3L);
        department.setName("Finance Dept");

        User user = new User();
        user.setId(10L);
        user.setName("Alice");
        user.setIsActive(false);
        user.setDepartment(department);
        user.setTeam(null);

        UserDto dto = mapper.toUserDto(user);

        assertAll(
            () -> assertEquals(3L, dto.departmentId()),
            () -> assertEquals("Finance Dept", dto.departmentName()),
            () -> assertNull(dto.teamId())
        );
    }

    @Test
    void toUserDto_WithTeamButNoTeamDepartment_MapsCorrectly() {
        Team team = new Team();
        team.setTeamId(4L);
        team.setName("Engineering Team");
        team.setDepartment(null);

        User user = new User();
        user.setId(10L);
        user.setName("Bob");
        user.setTeam(team);
        user.setDepartment(null);

        UserDto dto = mapper.toUserDto(user);

        assertAll(
            () -> assertEquals(4L, dto.teamId()),
            () -> assertEquals("Engineering Team", dto.teamName()),
            () -> assertNull(dto.departmentId()),
            () -> assertNull(dto.departmentName())
        );
    }

    @Test
    void toUserDto_WithTeamAndTeamDepartment_MapsCorrectly() {
        Department department = new Department();
        department.setDepartmentId(5L);
        department.setName("Engineering Dept");

        Team team = new Team();
        team.setTeamId(4L);
        team.setName("Engineering Team");
        team.setDepartment(department);

        User user = new User();
        user.setId(10L);
        user.setName("Bob");
        user.setTeam(team);
        user.setDepartment(null);

        UserDto dto = mapper.toUserDto(user);

        assertAll(
            () -> assertEquals(4L, dto.teamId()),
            () -> assertEquals("Engineering Team", dto.teamName()),
            () -> assertEquals(5L, dto.departmentId()),
            () -> assertEquals("Engineering Dept", dto.departmentName())
        );
    }

    @Test
    void toUserEditDto_NullUser_ReturnsNull() {
        assertNull(mapper.toUserEditDto(null, false));
    }

    @Test
    void toUserEditDto_NullAssociations_MapsCorrectly() {
        User user = new User();
        user.setId(10L);
        user.setName("Charlie");
        user.setEmail("charlie@example.com");
        user.setRole(UserRole.ADMINISTRATOR);
        user.setTeam(null);
        user.setDepartment(null);

        UserEditDto dto = mapper.toUserEditDto(user, true);

        assertAll(
            () -> assertEquals("Charlie", dto.name()),
            () -> assertEquals("charlie@example.com", dto.email()),
            () -> assertEquals(UserRole.ADMINISTRATOR, dto.role()),
            () -> assertTrue(dto.isTeamLeader()),
            () -> assertNull(dto.teamId()),
            () -> assertNull(dto.departmentId())
        );
    }

    @Test
    void toUserEditDto_WithAssociations_MapsCorrectly() {
        Department department = new Department();
        department.setDepartmentId(100L);

        Team team = new Team();
        team.setTeamId(200L);

        User user = new User();
        user.setId(10L);
        user.setTeam(team);
        user.setDepartment(department);

        UserEditDto dto = mapper.toUserEditDto(user, false);

        assertAll(
            () -> assertEquals(200L, dto.teamId()),
            () -> assertEquals(100L, dto.departmentId()),
            () -> assertFalse(dto.isTeamLeader())
        );
    }
}
