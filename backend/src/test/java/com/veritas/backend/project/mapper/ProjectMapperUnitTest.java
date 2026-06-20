package com.veritas.backend.project.mapper;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.veritas.backend.budget.entity.InternalBudget;
import com.veritas.backend.department.entity.Department;
import com.veritas.backend.project.dto.ProjectCreationDto;
import com.veritas.backend.project.dto.ProjectDto;
import com.veritas.backend.project.entity.Project;
import com.veritas.backend.project.mapper.ProjectMapper;
import com.veritas.backend.team.entity.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDate;

class ProjectMapperUnitTest {

    private ProjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(ProjectMapper.class);
    }

    @Test
    void toProjectDto_NullProject_ReturnsNull() {
        assertNull(mapper.toProjectDto(null));
    }

    @Test
    void toProject_NullDto_ReturnsNull() {
        assertNull(mapper.toProject(null));
    }

    @Test
    void toProjectDto_NullAssociations_MapsCorrectly() {
        Project project = new Project();
        project.setId(1L);
        project.setName("Project Alpha");
        project.setProjectKey("ALPHA");
        project.setStartDate(LocalDate.of(2026, 1, 1));
        project.setEndDate(LocalDate.of(2026, 12, 31));
        project.setTeam(null);
        project.setInternalBudget(null);

        ProjectDto dto = mapper.toProjectDto(project);

        assertAll(
            () -> assertEquals(1L, dto.id()),
            () -> assertEquals("Project Alpha", dto.name()),
            () -> assertEquals(LocalDate.of(2026, 1, 1), dto.startDate()),
            () -> assertEquals(LocalDate.of(2026, 12, 31), dto.endDate()),
            () -> assertNull(dto.budget()),
            () -> assertNull(dto.committedSpend()),
            () -> assertNull(dto.actualSpend()),
            () -> assertNull(dto.safetyBuffer()),
            () -> assertNull(dto.teamName()),
            () -> assertNull(dto.teamId()),
            () -> assertNull(dto.departmentName()),
            () -> assertNull(dto.departmentId())
        );
    }

    @Test
    void toProjectDto_WithNullDepartment_MapsCorrectly() {
        Team team = new Team();
        team.setTeamId(10L);
        team.setName("Engineering");
        team.setDepartment(null);

        Project project = new Project();
        project.setId(1L);
        project.setName("Project Alpha");
        project.setTeam(team);

        ProjectDto dto = mapper.toProjectDto(project);

        assertAll(
            () -> assertEquals("Engineering", dto.teamName()),
            () -> assertEquals(10L, dto.teamId()),
            () -> assertNull(dto.departmentName()),
            () -> assertNull(dto.departmentId())
        );
    }

    @Test
    void toProjectDto_WithAssociations_MapsCorrectly() {
        Department department = new Department();
        department.setDepartmentId(5L);
        department.setName("R&D");

        Team team = new Team();
        team.setTeamId(10L);
        team.setName("Engineering");
        team.setDepartment(department);

        InternalBudget budget = new InternalBudget();
        budget.setTotalAmount(BigDecimal.valueOf(10000.0));
        budget.setCommittedSpend(BigDecimal.valueOf(2500.0));
        budget.setActualSpend(BigDecimal.valueOf(1000.0));
        budget.setSafetyBuffer(BigDecimal.valueOf(500.0));

        Project project = new Project();
        project.setId(1L);
        project.setName("Project Alpha");
        project.setStartDate(LocalDate.of(2026, 1, 1));
        project.setEndDate(LocalDate.of(2026, 12, 31));
        project.setTeam(team);
        project.setInternalBudget(budget);

        ProjectDto dto = mapper.toProjectDto(project);

        assertAll(
            () -> assertEquals(1L, dto.id()),
            () -> assertEquals("Project Alpha", dto.name()),
            () -> assertEquals(LocalDate.of(2026, 1, 1), dto.startDate()),
            () -> assertEquals(LocalDate.of(2026, 12, 31), dto.endDate()),
            () -> assertEquals(BigDecimal.valueOf(10000.0), dto.budget()),
            () -> assertEquals(BigDecimal.valueOf(2500.0), dto.committedSpend()),
            () -> assertEquals(BigDecimal.valueOf(1000.0), dto.actualSpend()),
            () -> assertEquals(BigDecimal.valueOf(500.0), dto.safetyBuffer()),
            () -> assertEquals("Engineering", dto.teamName()),
            () -> assertEquals(10L, dto.teamId()),
            () -> assertEquals("R&D", dto.departmentName()),
            () -> assertEquals(5L, dto.departmentId())
        );
    }

    @Test
    void toProject_MapsCorrectly() {
        ProjectCreationDto dto = new ProjectCreationDto(
            "Project Beta",
            "BETA",
            15L,
            LocalDate.of(2026, 7, 1),
            LocalDate.of(2026, 12, 31),
            BigDecimal.valueOf(50000.0)
        );

        Project project = mapper.toProject(dto);

        assertAll(
            () -> assertEquals("Project Beta", project.getName()),
            () -> assertEquals("BETA", project.getProjectKey()),
            () -> assertEquals(LocalDate.of(2026, 7, 1), project.getStartDate()),
            () -> assertEquals(LocalDate.of(2026, 12, 31), project.getEndDate()),
            () -> assertNull(project.getInternalBudget())
        );
    }

    @Test
    void toProjectDto_PartialNullAssociations() {
        Team team = new Team();
        team.setTeamId(null);
        team.setName(null);
        team.setDepartment(new Department());

        InternalBudget budget = new InternalBudget();
        budget.setTotalAmount(null);
        budget.setCommittedSpend(null);
        budget.setActualSpend(null);
        budget.setSafetyBuffer(null);

        Project project = new Project();
        project.setTeam(team);
        project.setInternalBudget(budget);

        ProjectDto dto = mapper.toProjectDto(project);

        assertAll(
            () -> assertNull(dto.teamId()),
            () -> assertNull(dto.teamName()),
            () -> assertNull(dto.departmentId()),
            () -> assertNull(dto.departmentName()),
            () -> assertNull(dto.budget()),
            () -> assertNull(dto.committedSpend()),
            () -> assertNull(dto.actualSpend()),
            () -> assertNull(dto.safetyBuffer())
        );
    }
}
