package com.veritas.backend.project.service;

import com.veritas.backend.budget.entity.InternalBudget;
import com.veritas.backend.budget.entity.BudgetType;
import com.veritas.backend.department.entity.Department;
import com.veritas.backend.project.dto.ProjectCreationDto;
import com.veritas.backend.project.dto.ProjectDto;
import com.veritas.backend.project.dto.ProjectEditDto;
import com.veritas.backend.project.entity.Project;
import com.veritas.backend.project.mapper.ProjectMapper;
import com.veritas.backend.project.repository.ProjectRepository;
import com.veritas.backend.project.service.impl.ProjectServiceImpl;
import com.veritas.backend.requisition.repository.RequestRepository;
import com.veritas.backend.integrations.jira.repository.JiraConfigRepository;
import com.veritas.backend.team.entity.Team;
import com.veritas.backend.team.repository.TeamRepository;
import com.veritas.backend.user.entity.User;
import com.veritas.backend.user.entity.UserRole;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceUnitTest {
    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMapper projectMapper;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private RequestRepository requestRepository;

    @Mock
    private JiraConfigRepository jiraConfigRepository;

    @InjectMocks
    private ProjectServiceImpl projectService;

    @Test
    void GetProjectsForUser_FinanceOfficer_ReturnsAllProjects() {
        Team team = Team.builder().name("Testing Team").build();

        Project project1 = Project.builder().team(team).build();
        Project project2 = Project.builder().team(team).build();

        ProjectDto dto1 = new ProjectDto(1L, "Project 1", null, null, null, null, null, null, "Testing Team",null, null, null, true);
        ProjectDto dto2 = new ProjectDto(2L, "Project 2", null, null, null, null, null, null, "Testing Team",null, null, null, true);

        User financeOfficer = User.builder()
                .role(UserRole.FINANCE_OFFICER)
                .build();

        when(projectRepository.findByIsActiveTrue()).thenReturn(List.of(project1, project2));
        when(projectMapper.toProjectDto(project1)).thenReturn(dto1);
        when(projectMapper.toProjectDto(project2)).thenReturn(dto2);

        var result = projectService.getProjectsForUser(financeOfficer, false);

        assertEquals(2, result.size());
        verify(projectRepository).findByIsActiveTrue();
    }

    @Test
    void GetProjectsForUser_Requester_ReturnsOnlyTeamProjects() {
        Team team = Team.builder().name("Testing Team").build();

        Project project1 = Project.builder().team(team).build();
        ProjectDto dto1 = new ProjectDto(1L, "Project 1", null, null, null, null, null, null,"Testing Team",null, null, null, true);

        User user = User.builder()
                .role(UserRole.REQUESTER)
                .team(team)
                .build();

        when(projectRepository.findByTeamAndIsActiveTrue(team)).thenReturn(List.of(project1));
        when(projectMapper.toProjectDto(project1)).thenReturn(dto1);

        var result = projectService.getProjectsForUser(user, false);

        assertEquals(1, result.size());
        verify(projectRepository).findByTeamAndIsActiveTrue(team);
    }

    @Test
    void GetProjectsForUser_ProcurementOfficer_ReturnsDepartmentProjects() {
        Department department = Department.builder().departmentId(1L).name("QA Department").build();
        Team team = Team.builder().name("Testing Team").department(department).build();

        Project project1 = Project.builder().team(team).build();
        ProjectDto dto1 = new ProjectDto(1L, "Project 1", null, null, null, null, null, null,"Testing Team",null, null, null, true);

        User user = User.builder()
                .role(UserRole.PROCUREMENT_OFFICER)
                .department(department)
                .build();

        when(projectRepository.findByTeamDepartmentAndIsActiveTrue(department)).thenReturn(List.of(project1));
        when(projectMapper.toProjectDto(project1)).thenReturn(dto1);

        var result = projectService.getProjectsForUser(user, false);

        assertEquals(1, result.size());
        verify(projectRepository).findByTeamDepartmentAndIsActiveTrue(department);
    }

    @Test
    void CreateProject_ValidInput_SavesAndReturnsProject() {
        Team team = Team.builder().name("Testing Team").build();

        ProjectCreationDto dto = new ProjectCreationDto("Secret Project", "KEY-123", 1L, null, null, null);

        Project project = Project.builder().name("Secret Project").projectKey("KEY-123").build();
        Project saved = Project.builder().id(1L).name("Secret Project").projectKey("KEY-123").team(team).build();

        ProjectDto mapped = new ProjectDto(1L, "Secret Project", null, null, null, null, null, null,"Team A",null, null, null, true);

        when(projectRepository.existsByNameOrProjectKey("Secret Project", "KEY-123")).thenReturn(false);
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
        when(projectMapper.toProject(dto)).thenReturn(project);
        when(projectRepository.save(project)).thenReturn(saved);
        when(projectMapper.toProjectDto(saved)).thenReturn(mapped);

        var result = projectService.createProject(dto);
        assertEquals(mapped, result);

        verify(projectRepository).save(project);
    }

    @Test
    void CreateProject_DuplicateProject_ThrowsEntityExistsException() {
        ProjectCreationDto dto = new ProjectCreationDto("Duplicate", "DUP-KEY", 1L, null, null, null);

        when(projectRepository.existsByNameOrProjectKey("Duplicate", "DUP-KEY")).thenReturn(true);

        assertThrows(EntityExistsException.class, () -> projectService.createProject(dto));

        verify(projectRepository, never()).save(any());
    }

    @Test
    void CreateProject_TeamNotFound_ThrowsEntityNotFoundException() {
        ProjectCreationDto dto = new ProjectCreationDto("Secret New", "SEC-KEY", 1L, null, null, null);

        when(projectRepository.existsByNameOrProjectKey("Secret New", "SEC-KEY")).thenReturn(false);
        when(teamRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> projectService.createProject(dto));
    }

    @Test
    void GetProjectById_RequesterSameTeam_ReturnsProjectDto() {

        Team team = Team.builder().teamId(10L).name("Testing Team").build();
        Project project = Project.builder().id(1L).name("Secret Project").team(team).build();
        ProjectDto expectedDto = new ProjectDto(1L, "Secret Project", null, null, null, null,null,null,"Testing Team",null, null, null, true);
        User requester = User.builder().role(UserRole.REQUESTER).team(team).build();

        when(projectRepository.findByIdAndTeam(1L, team)).thenReturn(Optional.of(project));
        when(projectMapper.toProjectDto(project)).thenReturn(expectedDto);

        ProjectDto result = projectService.getProjectById(1L, requester);

        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals("Secret Project", result.name())
        );
        verify(projectRepository).findByIdAndTeam(1L, team);
    }

    @Test
    void GetProjectById_RequesterDifferentTeam_ThrowsEntityNotFoundException() {

        Team correctTeam = Team.builder().teamId(10L).name("Testing Team").build();
        Team wrongTeam = Team.builder().teamId(20L).name("Dev Team").build();
        User requester = User.builder().role(UserRole.REQUESTER).team(wrongTeam).build();

        when(projectRepository.findByIdAndTeam(1L, wrongTeam)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
                projectService.getProjectById(1L, requester)
        );
        verify(projectMapper, never()).toProjectDto(any());
    }

    @Test
    void GetProjectById_ProcurementOfficerSameDepartment_ReturnsProjectDto() {

        Department department = Department.builder().departmentId(5L).name("Logistics").build();
        Team team = Team.builder().teamId(10L).department(department).build();
        Project project = Project.builder().id(1L).name("Logistics Project").team(team).build();
        ProjectDto expectedDto = new ProjectDto(1L, "Logistics Project", null, null, null, null, null, null, "Team Logistics",null, null, null, true);
        User procurementOfficer = User.builder().role(UserRole.PROCUREMENT_OFFICER).department(department).build();

        when(projectRepository.findByIdAndTeamDepartment(1L, department)).thenReturn(Optional.of(project));
        when(projectMapper.toProjectDto(project)).thenReturn(expectedDto);

        ProjectDto result = projectService.getProjectById(1L, procurementOfficer);

        assertNotNull(result);
        verify(projectRepository).findByIdAndTeamDepartment(1L, department);
    }

    @Test
    void GetProjectById_ProcurementOfficerDifferentDepartment_ThrowsEntityNotFoundException() {
        Department wrongDepartment = Department.builder().departmentId(9L).name("HR").build();
        User procurementOfficer = User.builder().role(UserRole.PROCUREMENT_OFFICER).department(wrongDepartment).build();

        when(projectRepository.findByIdAndTeamDepartment(1L, wrongDepartment)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
                projectService.getProjectById(1L, procurementOfficer)
        );
    }

    @Test
    void EditProject_ProjectExists_UpdatesFieldsAndReturnsDto() {

        Long projectId = 1L;
        ProjectEditDto editDto =
                new ProjectEditDto("Updated Project Name", BigDecimal.valueOf(50000.00),null,null,null);

        Team team = Team.builder().teamId(10L).build();
        Project existingProject = Project.builder()
                .id(projectId)
                .name("Old Project Name")
                .team(team)
                .internalBudget(InternalBudget.builder()
                        .budgetName("Test Budget")
                        .budgetType(BudgetType.PROJECT)
                        .totalAmount(BigDecimal.valueOf(10000.00))
                        .build())
                .build();

        Project savedProject = Project.builder()
                .id(projectId)
                .name("Updated Project Name")
                .internalBudget(InternalBudget.builder()
                        .budgetName("Test Budget")
                        .budgetType(BudgetType.PROJECT)
                        .totalAmount(BigDecimal.valueOf(50000.00))
                        .build())
                .build();

        ProjectDto expectedDto = new ProjectDto(projectId, "Updated Project Name", null, null, BigDecimal.valueOf(50000.00), null, null, null, null,null, null, null, true);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(existingProject));
        when(projectRepository.save(existingProject)).thenReturn(savedProject);
        when(projectMapper.toProjectDto(savedProject)).thenReturn(expectedDto);


        ProjectDto result = projectService.editProject(projectId, editDto);
        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals("Updated Project Name", result.name()),
                () -> assertTrue(result.budget().compareTo(BigDecimal.valueOf(50000.00)) <= 0),
                () -> assertEquals("Updated Project Name", existingProject.getName()),
                () -> assertEquals(BigDecimal.valueOf(50000.00), existingProject.getInternalBudget().getTotalAmount())
        );


        verify(projectRepository).findById(projectId);
        verify(projectRepository).save(existingProject);
    }

    @Test
    void EditProject_ProjectDoesNotExist_ThrowsEntityNotFoundException() {

        Long projectId = 404L;
        ProjectEditDto editDto =
                new ProjectEditDto("Ghost Update", BigDecimal.valueOf(100),null,null,null);

        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());


        assertThrows(EntityNotFoundException.class, () ->
                projectService.editProject(projectId, editDto)
        );

        verify(projectRepository, never()).save(any());
        verify(projectMapper, never()).toProjectDto(any());
    }

    @Test
    void DeleteProject_NoReferences_HardDeletesSuccessfully() {
        Long projectId = 1L;
        Project project = Project.builder().id(projectId).build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(requestRepository.existsActiveRequisitionsByProjectId(projectId)).thenReturn(false);
        when(requestRepository.existsByProjectId(projectId)).thenReturn(false);
        when(jiraConfigRepository.existsByFallbackProjectId(projectId)).thenReturn(false);

        projectService.deleteProject(projectId);

        verify(projectRepository).delete(project);
        verify(projectRepository, never()).save(any());
    }

    @Test
    void DeleteProject_HasOnlyFinishedRequisitions_SoftDeletesProject() {
        Long projectId = 1L;
        Project project = Project.builder().id(projectId).isActive(true).build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(jiraConfigRepository.existsByFallbackProjectId(projectId)).thenReturn(false);
        when(requestRepository.existsActiveRequisitionsByProjectId(projectId)).thenReturn(false);
        when(requestRepository.existsByProjectId(projectId)).thenReturn(true);
        when(projectRepository.save(any(Project.class))).thenReturn(project);

        projectService.deleteProject(projectId);

        verify(projectRepository, never()).delete(any());
        ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
        verify(projectRepository).save(captor.capture());
        Project saved = captor.getValue();
        assertFalse(saved.getIsActive());
        assertNotNull(saved.getDeactivatedAt());
    }

    @Test
    void DeleteProject_HasActiveRequisitions_ThrowsIllegalStateException() {
        Long projectId = 1L;
        Project project = Project.builder().id(projectId).build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(jiraConfigRepository.existsByFallbackProjectId(projectId)).thenReturn(false);
        when(requestRepository.existsActiveRequisitionsByProjectId(projectId)).thenReturn(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                projectService.deleteProject(projectId)
        );
        assertEquals("Cannot delete project because it has open or in-progress requisitions", ex.getMessage());

        verify(projectRepository, never()).delete(any());
        verify(projectRepository, never()).save(any());
    }

    @Test
    void DeleteProject_IsJiraFallbackProject_ThrowsIllegalStateException() {
        Long projectId = 1L;
        Project project = Project.builder().id(projectId).build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(jiraConfigRepository.existsByFallbackProjectId(projectId)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> projectService.deleteProject(projectId));

        verify(projectRepository, never()).delete(any());
        verify(projectRepository, never()).save(any());
    }

    @Test
    void EditProject_ExceedsDepartmentBudget_ThrowsIllegalArgumentException() {
        Long projectId = 1L;
        ProjectEditDto editDto = new ProjectEditDto("Updated Project Name", BigDecimal.valueOf(50000.00), null, null, null);

        Department department = Department.builder()
                .departmentId(1L)
                .name("Engineering")
                .internalBudget(InternalBudget.builder()
                        .budgetType(BudgetType.DEPARTMENT)
                        .totalAmount(BigDecimal.valueOf(60000.00))
                        .build())
                .build();
        Team team = Team.builder().teamId(10L).department(department).build();

        Project existingProject = Project.builder()
                .id(projectId)
                .name("Old Project Name")
                .team(team)
                .internalBudget(InternalBudget.builder()
                        .budgetName("Test Budget")
                        .budgetType(BudgetType.PROJECT)
                        .totalAmount(BigDecimal.valueOf(10000.00))
                        .build())
                .build();

        Project otherProject = Project.builder()
                .id(2L)
                .name("Other Project")
                .team(team)
                .internalBudget(InternalBudget.builder()
                        .budgetName("Other Budget")
                        .budgetType(BudgetType.PROJECT)
                        .totalAmount(BigDecimal.valueOf(20000.00))
                        .build())
                .build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(existingProject));
        when(projectRepository.findByTeamDepartment(department)).thenReturn(List.of(existingProject, otherProject));

        assertThrows(IllegalArgumentException.class, () -> projectService.editProject(projectId, editDto));
    }

    @Test
    void EditProject_ChangeTeamExceedsDepartmentBudget_ThrowsIllegalArgumentException() {
        Long projectId = 1L;
        ProjectEditDto editDto = new ProjectEditDto(null, null, 20L, null, null);

        Department department1 = Department.builder()
                .departmentId(1L)
                .name("Engineering")
                .internalBudget(InternalBudget.builder()
                        .budgetType(BudgetType.DEPARTMENT)
                        .totalAmount(BigDecimal.valueOf(60000.00))
                        .build())
                .build();
        Team team1 = Team.builder().teamId(10L).department(department1).build();

        Department department2 = Department.builder()
                .departmentId(2L)
                .name("Marketing")
                .internalBudget(InternalBudget.builder()
                        .budgetType(BudgetType.DEPARTMENT)
                        .totalAmount(BigDecimal.valueOf(30000.00))
                        .build())
                .build();
        Team team2 = Team.builder().teamId(20L).department(department2).build();

        Project existingProject = Project.builder()
                .id(projectId)
                .name("Old Project Name")
                .team(team1)
                .internalBudget(InternalBudget.builder()
                        .budgetName("Test Budget")
                        .budgetType(BudgetType.PROJECT)
                        .totalAmount(BigDecimal.valueOf(20000.00))
                        .build())
                .build();

        Project marketingProject = Project.builder()
                .id(3L)
                .name("Marketing Project")
                .team(team2)
                .internalBudget(InternalBudget.builder()
                        .budgetName("Marketing Budget")
                        .budgetType(BudgetType.PROJECT)
                        .totalAmount(BigDecimal.valueOf(20000.00))
                        .build())
                .build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(existingProject));
        when(teamRepository.findById(20L)).thenReturn(Optional.of(team2));
        when(projectRepository.findByTeamDepartment(department2)).thenReturn(List.of(marketingProject));

        assertThrows(IllegalArgumentException.class, () -> projectService.editProject(projectId, editDto));
    }

    @Test
    void EditProject_NoInternalBudget_ThrowsEntityNotFoundException() {
        Long projectId = 1L;
        ProjectEditDto editDto = new ProjectEditDto(null, BigDecimal.valueOf(50000.00), null, null, null);

        Department dept = Department.builder().departmentId(1L).build();
        Team team = Team.builder().teamId(10L).department(dept).build();
        Project existingProject = Project.builder()
                .id(projectId)
                .name("Old Project")
                .team(team)
                .internalBudget(null)
                .build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(existingProject));

        assertThrows(EntityNotFoundException.class, () ->
                projectService.editProject(projectId, editDto)
        );
    }

    @Test
    void EditProject_TeamChangeWithBudgetAndDepartment_SavesSuccessfully() {
        Long projectId = 1L;
        ProjectEditDto editDto = new ProjectEditDto(null, null, 20L, null, null);

        Department dept = Department.builder()
                .departmentId(2L)
                .internalBudget(InternalBudget.builder().totalAmount(BigDecimal.valueOf(100000.00)).build())
                .build();
        Team team = Team.builder().teamId(20L).department(dept).build();

        Project existingProject = Project.builder()
                .id(projectId)
                .name("Old Project")
                .internalBudget(InternalBudget.builder().totalAmount(BigDecimal.valueOf(20000.00)).build())
                .build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(existingProject));
        when(teamRepository.findById(20L)).thenReturn(Optional.of(team));
        when(projectRepository.save(existingProject)).thenReturn(existingProject);
        when(projectMapper.toProjectDto(existingProject)).thenReturn(new ProjectDto(projectId, "Old Project", null, null, null, null, null, null, null, null, null, null, true));

        ProjectDto result = projectService.editProject(projectId, editDto);
        assertNotNull(result);
        assertEquals(team, existingProject.getTeam());
        assertEquals(dept.getInternalBudget(), existingProject.getInternalBudget().getParentBudget());
    }

    @Test
    void EditProject_TeamChangeDepartmentNull_SavesSuccessfully() {
        Long projectId = 1L;
        ProjectEditDto editDto = new ProjectEditDto(null, null, 20L, null, null);

        Team team = Team.builder().teamId(20L).department(null).build();

        Project existingProject = Project.builder()
                .id(projectId)
                .name("Old Project")
                .internalBudget(InternalBudget.builder().totalAmount(BigDecimal.valueOf(20000.00)).build())
                .build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(existingProject));
        when(teamRepository.findById(20L)).thenReturn(Optional.of(team));
        when(projectRepository.save(existingProject)).thenReturn(existingProject);
        when(projectMapper.toProjectDto(existingProject)).thenReturn(new ProjectDto(projectId, "Old Project", null, null, null, null, null, null, null, null, null, null, true));

        ProjectDto result = projectService.editProject(projectId, editDto);
        assertNotNull(result);
        assertEquals(team, existingProject.getTeam());
        assertNull(existingProject.getInternalBudget().getParentBudget());
    }

    @Test
    void GetProjectById_FinanceOfficer_ReturnsProject() {
        Long projectId = 1L;
        Project project = Project.builder()
                .id(projectId)
                .name("Finance Project")
                .build();
        ProjectDto dto = new ProjectDto(projectId, "Finance Project", null, null, null, null, null, null, null, null, null, null, true);

        User user = User.builder().role(UserRole.FINANCE_OFFICER).build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectMapper.toProjectDto(project)).thenReturn(dto);

        ProjectDto result = projectService.getProjectById(projectId, user);

        assertAll(
            () -> assertNotNull(result),
            () -> assertEquals("Finance Project", result.name())
        );
        verify(projectRepository).findById(projectId);
    }

    @Test
    void EditProject_StartDateAlreadyPassed_ThrowsIllegalArgumentException() {
        Long projectId = 1L;
        LocalDate startDate = LocalDate.now().minusDays(5);
        LocalDate newStartDate = LocalDate.now().plusDays(10);
        ProjectEditDto editDto = new ProjectEditDto(null, null, null, newStartDate, null);

        Project existingProject = Project.builder()
                .id(projectId)
                .startDate(startDate)
                .build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(existingProject));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                projectService.editProject(projectId, editDto)
        );
        assertEquals("Start date can only be changed if the project has not started already", ex.getMessage());
    }

    @Test
    void EditProject_StartDateInPast_ThrowsIllegalArgumentException() {
        Long projectId = 1L;
        LocalDate startDate = LocalDate.now().plusDays(5);
        LocalDate newStartDate = LocalDate.now().minusDays(2);
        ProjectEditDto editDto = new ProjectEditDto(null, null, null, newStartDate, null);

        Project existingProject = Project.builder()
                .id(projectId)
                .startDate(startDate)
                .build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(existingProject));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                projectService.editProject(projectId, editDto)
        );
        assertEquals("Start date can only be changed if the new date is in the future", ex.getMessage());
    }

    @Test
    void EditProject_EndDateInPast_ThrowsIllegalArgumentException() {
        Long projectId = 1L;
        LocalDate startDate = LocalDate.now().minusDays(10);
        LocalDate endDate = LocalDate.now().plusDays(10);
        LocalDate newEndDate = LocalDate.now().minusDays(2);
        ProjectEditDto editDto = new ProjectEditDto(null, null, null, startDate, newEndDate);

        Project existingProject = Project.builder()
                .id(projectId)
                .startDate(startDate)
                .endDate(endDate)
                .build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(existingProject));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                projectService.editProject(projectId, editDto)
        );
        assertEquals("End date can only be changed if the new date is in the future", ex.getMessage());
    }

    @Test
    void DeleteProject_DoesNotExist_ThrowsEntityNotFoundException() {
        Long projectId = 999L;
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class, () ->
                projectService.deleteProject(projectId)
        );
        assertEquals("Project not found with id 999", ex.getMessage());
    }

    @Test
    void CreateProject_DepartmentNull_SavesSuccessfully() {
        ProjectCreationDto creationDto = new ProjectCreationDto("Dept Null Proj", "DN-KEY", 10L, LocalDate.now(), LocalDate.now().plusDays(30), BigDecimal.valueOf(10000.00));

        Team team = Team.builder().teamId(10L).department(null).build();
        Project project = Project.builder().name("Dept Null Proj").build();

        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));
        when(projectMapper.toProject(creationDto)).thenReturn(project);
        when(projectRepository.save(any(Project.class))).thenReturn(project);
        when(projectMapper.toProjectDto(project)).thenReturn(new ProjectDto(1L, "Dept Null Proj", null, null, null, null, null, null, null, null, null, null, true));

        ProjectDto result = projectService.createProject(creationDto);
        assertNotNull(result);
        assertNull(project.getInternalBudget().getParentBudget());
    }

    @Test
    void CreateProject_DepartmentNonNullBudgetNull_Succeeds() {
        ProjectCreationDto creationDto = new ProjectCreationDto("Dept Non-Null Proj", "DN2-KEY", 10L, LocalDate.now(), LocalDate.now().plusDays(30), BigDecimal.valueOf(10000.00));

        Department department = Department.builder().departmentId(5L).internalBudget(null).build();
        Team team = Team.builder().teamId(10L).department(department).build();
        Project project = Project.builder().name("Dept Non-Null Proj").build();

        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));
        when(projectMapper.toProject(creationDto)).thenReturn(project);
        when(projectRepository.save(any(Project.class))).thenReturn(project);
        when(projectMapper.toProjectDto(project)).thenReturn(new ProjectDto(1L, "Dept Non-Null Proj", null, null, null, null, null, null, null, null, null, null, true));

        ProjectDto result = projectService.createProject(creationDto);
        assertNotNull(result);
    }

    @Test
    void CreateProject_DepartmentBudgetWithNullLimit_Succeeds() {
        ProjectCreationDto creationDto = new ProjectCreationDto("Dept Null Limit Proj", "DNL-KEY", 10L, LocalDate.now(), LocalDate.now().plusDays(30), BigDecimal.valueOf(10000.00));

        Department department = Department.builder().departmentId(5L).internalBudget(InternalBudget.builder().totalAmount(null).build()).build();
        Team team = Team.builder().teamId(10L).department(department).build();
        Project project = Project.builder().name("Dept Null Limit Proj").build();

        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));
        when(projectMapper.toProject(creationDto)).thenReturn(project);
        when(projectRepository.save(any(Project.class))).thenReturn(project);
        when(projectMapper.toProjectDto(project)).thenReturn(new ProjectDto(1L, "Dept Null Limit Proj", null, null, null, null, null, null, null, null, null, null, true));

        ProjectDto result = projectService.createProject(creationDto);
        assertNotNull(result);
    }

    @Test
    void EditProject_EndDateUnchanged_SavesSuccessfully() {
        Long projectId = 1L;
        LocalDate endDate = LocalDate.now().plusDays(10);
        ProjectEditDto editDto = new ProjectEditDto(null, null, null, null, endDate);

        Project existingProject = Project.builder()
                .id(projectId)
                .endDate(endDate)
                .build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(existingProject));
        when(projectRepository.save(existingProject)).thenReturn(existingProject);
        when(projectMapper.toProjectDto(existingProject)).thenReturn(new ProjectDto(projectId, "Old Project", null, null, null, null, null, null, null, null, null, null, true));

        ProjectDto result = projectService.editProject(projectId, editDto);
        assertNotNull(result);
    }

    @Test
    void EditProject_OnlyTeamIdUpdated_SavesSuccessfully() {
        Long projectId = 1L;
        ProjectEditDto editDto = new ProjectEditDto(null, null, 20L, null, null);

        Department dept = Department.builder().departmentId(2L).internalBudget(InternalBudget.builder().totalAmount(BigDecimal.valueOf(100000.00)).build()).build();
        Team team = Team.builder().teamId(20L).department(dept).build();

        Project existingProject = Project.builder()
                .id(projectId)
                .internalBudget(InternalBudget.builder().totalAmount(BigDecimal.valueOf(20000.00)).build())
                .build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(existingProject));
        when(teamRepository.findById(20L)).thenReturn(Optional.of(team));
        when(projectRepository.save(existingProject)).thenReturn(existingProject);
        when(projectMapper.toProjectDto(existingProject)).thenReturn(new ProjectDto(projectId, "Old Project", null, null, null, null, null, null, null, null, null, null, true));

        ProjectDto result = projectService.editProject(projectId, editDto);
        assertNotNull(result);
    }

    @Test
    void EditProject_BudgetUpdateNullInternalBudget_ThrowsEntityNotFoundException() {
        Long projectId = 1L;
        ProjectEditDto editDto = new ProjectEditDto(null, BigDecimal.valueOf(50000.00), null, null, null);

        Team team = Team.builder().teamId(10L).department(null).build();
        Project existingProject = Project.builder()
                .id(projectId)
                .team(team)
                .internalBudget(null)
                .build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(existingProject));

        assertThrows(EntityNotFoundException.class, () -> projectService.editProject(projectId, editDto));
    }

    @Test
    void EditProject_TeamChangeNullInternalBudget_ThrowsNullPointerException() {
        Long projectId = 1L;
        ProjectEditDto editDto = new ProjectEditDto(null, null, 20L, null, null);

        Team team = Team.builder().teamId(20L).department(null).build();
        Project existingProject = Project.builder()
                .id(projectId)
                .internalBudget(null)
                .build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(existingProject));
        when(teamRepository.findById(20L)).thenReturn(Optional.of(team));

        assertThrows(NullPointerException.class, () -> projectService.editProject(projectId, editDto));
    }

    @Test
    void EditProject_EndDateChangedAndStartDateNull_ThrowsNullPointerException() {
        Long projectId = 1L;
        LocalDate startDate = LocalDate.now().plusDays(5);
        LocalDate newEndDate = LocalDate.now().plusDays(15);
        ProjectEditDto editDto = new ProjectEditDto(null, null, null, null, newEndDate);

        Project existingProject = Project.builder()
                .id(projectId)
                .startDate(startDate)
                .endDate(LocalDate.now().plusDays(10))
                .internalBudget(InternalBudget.builder().totalAmount(BigDecimal.valueOf(20000.00)).build())
                .build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(existingProject));

        assertThrows(NullPointerException.class, () -> projectService.editProject(projectId, editDto));
    }

    @Test
    void EditProject_InactiveProject_ThrowsIllegalStateException() {
        Long projectId = 1L;
        ProjectEditDto editDto = new ProjectEditDto("New Name", null, null, null, null);

        Project inactiveProject = Project.builder()
                .id(projectId)
                .name("Old Name")
                .isActive(false)
                .build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(inactiveProject));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                projectService.editProject(projectId, editDto)
        );
        assertEquals("Cannot edit a deactivated project", ex.getMessage());
        verify(projectRepository, never()).save(any());
    }
}
