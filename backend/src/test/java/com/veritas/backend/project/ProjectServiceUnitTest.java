package com.veritas.backend.project;

import com.veritas.backend.project.entity.Project;
import com.veritas.backend.project.repository.ProjectRepository;
import com.veritas.backend.project.service.impl.ProjectServiceImpl;
import com.veritas.backend.team.entity.Team;
import com.veritas.backend.user.entity.User;
import com.veritas.backend.user.entity.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceUnitTest {
    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private ProjectServiceImpl projectService;

    @Test
    void financeOfficerCanSeeAllProjectsTest() {
        Team team = Team.builder().name("Testing Team").build();

        Project project1 = Project.builder().team(team).build();
        Project project2 = Project.builder().team(team).build();

        User financeOfficer = User.builder()
                .role(UserRole.FINANCE_OFFICER)
                .build();

        when(projectRepository.findAll()).thenReturn(List.of(project1, project2));

        var result = projectService.getProjectsForUser(financeOfficer);

        assertThat(result).hasSize(2);
        verify(projectRepository).findAll();
    }

    @Test
    void requesterCanOnlySeeTeamProjectsTest() {
        Team team = Team.builder().name("Testing Team").build();

        Project project1 = Project.builder().team(team).build();

        User user = User.builder()
                .role(UserRole.REQUESTER)
                .team(team)
                .build();

        when(projectRepository.findByTeam(team)).thenReturn(List.of(project1));

        var result = projectService.getProjectsForUser(user);

        assertThat(result).hasSize(1);
        verify(projectRepository).findByTeam(team);
    }

    @Test
    void administratorCannotSeeProjects() {
        User user = User.builder()
                .role(UserRole.ADMINISTRATOR)
                .build();

        var result = projectService.getProjectsForUser(user);

        assertThat(result).isEmpty();
        verifyNoInteractions(projectRepository);
    }
}
