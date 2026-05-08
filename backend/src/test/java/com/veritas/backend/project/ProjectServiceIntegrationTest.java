package com.veritas.backend.project;

import com.veritas.backend.BaseDBIntegrationTest;
import com.veritas.backend.project.dto.ProjectCreationDto;
import com.veritas.backend.project.repository.ProjectRepository;
import com.veritas.backend.project.service.ProjectService;
import com.veritas.backend.team.repository.TeamRepository;
import com.veritas.backend.user.entity.User;
import com.veritas.backend.user.entity.UserRole;
import com.veritas.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.veritas.backend.team.entity.Team;
import com.veritas.backend.project.entity.Project;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ProjectServiceIntegrationTest extends BaseDBIntegrationTest {

    @Autowired
    ProjectService projectService;

    @Autowired
    ProjectRepository projectRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    TeamRepository teamRepository;

    @Autowired
    PasswordEncoder encoder;

    Team testingTeam;
    Team developmentTeam;
    Project project;

    @BeforeEach
    void setup() {
        projectRepository.deleteAll();
        userRepository.deleteAll();
        teamRepository.deleteAll();

        testingTeam = teamRepository.save(Team.builder()
                .name("Testing Team")
                .description("Handles QA and testing work")
                .isActive(true)
                .build());

        developmentTeam = teamRepository.save(Team.builder()
                .name("Development Team")
                .description("Builds product features")
                .isActive(true)
                .build());

        project = projectRepository.save(Project.builder()
                .name("Super Secret Project")
                .projectKey(UUID.randomUUID().toString())
                .team(testingTeam)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(30))
                .budget(BigDecimal.valueOf(10000.00))
                .build());
    }

    @Test
    void ProjectRetrieval_FinanceOfficer_ReturnsAllProjects() {
        User financeOfficer = userRepository.save(User.builder()
                .name("Test Finance")
                .email("test@yahoo.com")
                .passwordHash(encoder.encode("password123"))
                .role(UserRole.FINANCE_OFFICER)
                .team(testingTeam)
                .isActive(true)
                .build());

        var result = projectService.getProjectsForUser(financeOfficer);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().name()).isEqualTo(project.getName());
    }

    @Test
    void ProjectRetrieval_Requester_ReturnsTeamProjects() {
        User requester = userRepository.save(User.builder()
                .name("Test Requester")
                .email("test@yahoo.com")
                .passwordHash(encoder.encode("password123"))
                .role(UserRole.REQUESTER)
                .team(testingTeam)
                .isActive(true)
                .build());

        var result = projectService.getProjectsForUser(requester);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().name()).isEqualTo(project.getName());
    }

    @Test
    void ProjectRetrieval_ProcurementOfficer_ReturnsEmptyForOtherTeams() {
        User procurementOfficer = userRepository.save(User.builder()
                .name("Test Procurement")
                .email("test@yahoo.com")
                .passwordHash(encoder.encode("password123"))
                .role(UserRole.PROCUREMENT_OFFICER)
                .team(developmentTeam)
                .isActive(true)
                .build());

        var result = projectService.getProjectsForUser(procurementOfficer);

        assertThat(result).isEmpty();
    }

    @Test
    void ProjectCreation_ValidInput_ReturnsCreatedProject() {
        var dto = new ProjectCreationDto("Integration Project", UUID.randomUUID().toString(), testingTeam.getTeamId(), LocalDate.now(), LocalDate.now().plusDays(10), BigDecimal.valueOf(5000));

        var result = projectService.createProject(dto);

        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("Integration Project");

        var projects = projectRepository.findAll();
        assertThat(projects).hasSize(2);
    }
}
