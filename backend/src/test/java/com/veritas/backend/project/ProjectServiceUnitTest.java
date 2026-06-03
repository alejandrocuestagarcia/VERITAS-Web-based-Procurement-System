package com.veritas.backend.project;

import com.veritas.backend.budget.entity.InternalBudget;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertAll;
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

        ProjectDto dto1 = new ProjectDto(1L, "Project 1", null, null, null, null, null, null, "Testing Team",null);
        ProjectDto dto2 = new ProjectDto(2L, "Project 2", null, null, null, null, null, null, "Testing Team",null);

        User financeOfficer = User.builder()
                .role(UserRole.FINANCE_OFFICER)
                .build();

        when(projectRepository.findAll()).thenReturn(List.of(project1, project2));
        when(projectMapper.toProjectDto(project1)).thenReturn(dto1);
        when(projectMapper.toProjectDto(project2)).thenReturn(dto2);

        var result = projectService.getProjectsForUser(financeOfficer);

        assertThat(result).hasSize(2);
        verify(projectRepository).findAll();
    }

    @Test
    void GetProjectsForUser_Requester_ReturnsOnlyTeamProjects() {
        Team team = Team.builder().name("Testing Team").build();

        Project project1 = Project.builder().team(team).build();
        ProjectDto dto1 = new ProjectDto(1L, "Project 1", null, null, null, null, null, null,"Testing Team",null);

        User user = User.builder()
                .role(UserRole.REQUESTER)
                .team(team)
                .build();

        when(projectRepository.findByTeam(team)).thenReturn(List.of(project1));
        when(projectMapper.toProjectDto(project1)).thenReturn(dto1);

        var result = projectService.getProjectsForUser(user);

        assertThat(result).hasSize(1);
        verify(projectRepository).findByTeam(team);
    }

    @Test
    void GetProjectsForUser_ProcurementOfficer_ReturnsDepartmentProjects() {
        Department department = Department.builder().departmentId(1L).name("QA Department").build();
        Team team = Team.builder().name("Testing Team").department(department).build();

        Project project1 = Project.builder().team(team).build();
        ProjectDto dto1 = new ProjectDto(1L, "Project 1", null, null, null, null, null, null,"Testing Team",null);

        User user = User.builder()
                .role(UserRole.PROCUREMENT_OFFICER)
                .department(department)
                .build();

        when(projectRepository.findByTeamDepartment(department)).thenReturn(List.of(project1));
        when(projectMapper.toProjectDto(project1)).thenReturn(dto1);

        var result = projectService.getProjectsForUser(user);

        assertThat(result).hasSize(1);
        verify(projectRepository).findByTeamDepartment(department);
    }

    @Test
    void CreateProject_ValidInput_SavesAndReturnsProject() {
        Team team = Team.builder().name("Testing Team").build();

        ProjectCreationDto dto = new ProjectCreationDto("Secret Project", "KEY-123", 1L, null, null, null);

        Project project = Project.builder().name("Secret Project").projectKey("KEY-123").build();
        Project saved = Project.builder().id(1L).name("Secret Project").projectKey("KEY-123").team(team).build();

        ProjectDto mapped = new ProjectDto(1L, "Secret Project", null, null, null, null, null, null,"Team A",null);

        when(projectRepository.existsByNameOrProjectKey("Secret Project", "KEY-123")).thenReturn(false);
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
        when(projectMapper.toProject(dto)).thenReturn(project);
        when(projectRepository.save(project)).thenReturn(saved);
        when(projectMapper.toProjectDto(saved)).thenReturn(mapped);

        var result = projectService.createProject(dto);
        assertThat(result).isEqualTo(mapped);

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

    //AI-Generated
    @Test
    void GetProjectById_RequesterSameTeam_ReturnsProjectDto() {

        Team team = Team.builder().teamId(10L).name("Testing Team").build();
        Project project = Project.builder().id(1L).name("Secret Project").team(team).build();
        ProjectDto expectedDto = new ProjectDto(1L, "Secret Project", null, null, null, null,null,null,"Testing Team",null);
        User requester = User.builder().role(UserRole.REQUESTER).team(team).build();

        when(projectRepository.findByIdAndTeam(1L, team)).thenReturn(Optional.of(project));
        when(projectMapper.toProjectDto(project)).thenReturn(expectedDto);

        ProjectDto result = projectService.getProjectById(1L, requester);

        assertAll(
                ()->assertThat(result).isNotNull(),
                ()->assertThat(result.name()).isEqualTo("Secret Project")
        );
        verify(projectRepository).findByIdAndTeam(1L, team);
    }

    //AI-Generated
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

    //AI-Generated
    @Test
    void GetProjectById_ProcurementOfficerSameDepartment_ReturnsProjectDto() {

        Department department = Department.builder().departmentId(5L).name("Logistics").build();
        Team team = Team.builder().teamId(10L).department(department).build();
        Project project = Project.builder().id(1L).name("Logistics Project").team(team).build();
        ProjectDto expectedDto = new ProjectDto(1L, "Logistics Project", null, null, null, null, null, null, "Team Logistics",null);
        User procurementOfficer = User.builder().role(UserRole.PROCUREMENT_OFFICER).department(department).build();

        when(projectRepository.findByIdAndTeamDepartment(1L, department)).thenReturn(Optional.of(project));
        when(projectMapper.toProjectDto(project)).thenReturn(expectedDto);

        ProjectDto result = projectService.getProjectById(1L, procurementOfficer);

        assertThat(result).isNotNull();
        verify(projectRepository).findByIdAndTeamDepartment(1L, department);
    }

    //AI-Generated
    @Test
    void GetProjectById_ProcurementOfficerDifferentDepartment_ThrowsEntityNotFoundException() {
        Department wrongDepartment = Department.builder().departmentId(9L).name("HR").build();
        User procurementOfficer = User.builder().role(UserRole.PROCUREMENT_OFFICER).department(wrongDepartment).build();

        when(projectRepository.findByIdAndTeamDepartment(1L, wrongDepartment)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
                projectService.getProjectById(1L, procurementOfficer)
        );
    }

    //AI-Generated
    @Test
    void EditProject_ProjectExists_UpdatesFieldsAndReturnsDto() {

        Long projectId = 1L;
        ProjectEditDto editDto =
                new ProjectEditDto("Updated Project Name", BigDecimal.valueOf(50000.00),null,null,null);

        Project existingProject = Project.builder()
                .id(projectId)
                .name("Old Project Name")
                .internalBudget(InternalBudget.builder().budgetName("Test Budget").totalAmount(BigDecimal.valueOf(10000.00)).build())
                .build();

        Project savedProject = Project.builder()
                .id(projectId)
                .name("Updated Project Name")
                .internalBudget(InternalBudget.builder().budgetName("Test Budget").totalAmount(BigDecimal.valueOf(50000.00)).build())
                .build();

        ProjectDto expectedDto = new ProjectDto(projectId, "Updated Project Name", null, null, BigDecimal.valueOf(50000.00), null, null, null, null,null);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(existingProject));
        when(projectRepository.save(existingProject)).thenReturn(savedProject);
        when(projectMapper.toProjectDto(savedProject)).thenReturn(expectedDto);


        ProjectDto result = projectService.editProject(projectId, editDto);
        assertAll(
                () -> assertThat(result).isNotNull(),
                () -> assertThat(result.name()).isEqualTo("Updated Project Name"),
                () -> assertThat(result.budget()).isLessThanOrEqualTo(BigDecimal.valueOf(50000.00)),


                () -> assertThat(existingProject.getName()).isEqualTo("Updated Project Name"),
                () -> assertThat(existingProject.getInternalBudget().getTotalAmount()).isEqualTo(BigDecimal.valueOf(50000.00))
        );


        verify(projectRepository).findById(projectId);
        verify(projectRepository).save(existingProject);
    }

    //AI-Generated
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
    void DeleteProject_NoReferences_DeletesSuccessfully() {
        Long projectId = 1L;

        when(projectRepository.existsById(projectId)).thenReturn(true);
        when(requestRepository.existsByProjectId(projectId)).thenReturn(false);
        when(jiraConfigRepository.existsByFallbackProjectId(projectId)).thenReturn(false);

        projectService.deleteProject(projectId);

        verify(projectRepository).deleteById(projectId);
    }

    @Test
    void DeleteProject_HasRequisitions_ThrowsIllegalStateException() {
        Long projectId = 1L;

        when(projectRepository.existsById(projectId)).thenReturn(true);
        when(requestRepository.existsByProjectId(projectId)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> projectService.deleteProject(projectId));

        verify(projectRepository, never()).deleteById(any());
    }

    @Test
    void DeleteProject_IsJiraFallbackProject_ThrowsIllegalStateException() {
        Long projectId = 1L;

        when(projectRepository.existsById(projectId)).thenReturn(true);
        when(requestRepository.existsByProjectId(projectId)).thenReturn(false);
        when(jiraConfigRepository.existsByFallbackProjectId(projectId)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> projectService.deleteProject(projectId));

        verify(projectRepository, never()).deleteById(any());
    }
}
