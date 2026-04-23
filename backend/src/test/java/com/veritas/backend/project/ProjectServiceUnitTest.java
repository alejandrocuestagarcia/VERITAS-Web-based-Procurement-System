package com.veritas.backend.project;

import com.veritas.backend.project.dto.ProjectCreationDto;
import com.veritas.backend.project.dto.ProjectDto;
import com.veritas.backend.project.entity.Project;
import com.veritas.backend.project.mapper.ProjectMapper;
import com.veritas.backend.project.repository.ProjectRepository;
import com.veritas.backend.project.service.impl.ProjectServiceImpl;
import com.veritas.backend.team.entity.Team;
import com.veritas.backend.team.repository.TeamRepository;
import com.veritas.backend.user.entity.User;
import com.veritas.backend.user.entity.UserRole;
import jakarta.persistence.EntityExistsException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceUnitTest {
    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMapper projectMapper;

    @Mock
    private TeamRepository teamRepository;

    @InjectMocks
    private ProjectServiceImpl projectService;

    @Test
    void financeOfficerCanSeeAllProjectsTest() {
        Team team = Team.builder().name("Testing Team").build();

        Project project1 = Project.builder().team(team).build();
        Project project2 = Project.builder().team(team).build();

        ProjectDto dto1 = new ProjectDto(1L, "Project 1", null, null, null, "Testing Team");
        ProjectDto dto2 = new ProjectDto(2L, "Project 2", null, null, null, "Testing Team");

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
    void requesterCanOnlySeeTeamProjectsTest() {
        Team team = Team.builder().name("Testing Team").build();

        Project project1 = Project.builder().team(team).build();
        ProjectDto dto1 = new ProjectDto(1L, "Project 1", null, null, null, "Testing Team");

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
    void createProjectSuccess() {
        Team team = Team.builder().name("Testing Team").build();

        ProjectCreationDto dto = new ProjectCreationDto("Secret Project", "KEY-123", 1L, null, null, null);

        Project project = Project.builder().name("Secret Project").projectKey("KEY-123").build();
        Project saved = Project.builder().id(1L).name("Secret Project").projectKey("KEY-123").team(team).build();

        ProjectDto mapped = new ProjectDto(1L, "Secret Project", null, null, null, "Team A");

        when(projectRepository.existsByNameOrProjectKey("Secret Project", "KEY-123")).thenReturn(false);
        when(teamRepository.findAll()).thenReturn(List.of(team));
        when(projectMapper.toProject(dto)).thenReturn(project);
        when(projectRepository.save(project)).thenReturn(saved);
        when(projectMapper.toProjectDto(saved)).thenReturn(mapped);

        var result = projectService.createProject(dto);
        assertThat(result).isEqualTo(mapped);

        verify(projectRepository).save(project);
    }

    @Test
    void createProjectThrowsIfExists() {
        ProjectCreationDto dto = new ProjectCreationDto("Duplicate", "DUP-KEY", 1L, null, null, null);

        when(projectRepository.existsByNameOrProjectKey("Duplicate", "DUP-KEY")).thenReturn(true);

        assertThrows(EntityExistsException.class, () -> projectService.createProject(dto));

        verify(projectRepository, never()).save(any());
    }

    @Test
    void createProjectThrowsIfNoTeams() {
        ProjectCreationDto dto = new ProjectCreationDto("Secret New", "SEC-KEY", 1L, null, null, null);

        when(projectRepository.existsByNameOrProjectKey("Secret New", "SEC-KEY")).thenReturn(false);
        when(teamRepository.findAll()).thenReturn(List.of());

        assertThrows(IllegalStateException.class, () -> projectService.createProject(dto));
    }
}
