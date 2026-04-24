package com.veritas.backend.project;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.veritas.backend.BaseDBIntegrationTest;
import com.veritas.backend.auth.service.JwtService;
import com.veritas.backend.common.model.Department;
import com.veritas.backend.project.dto.ProjectCreationDto;
import com.veritas.backend.project.entity.Project;
import com.veritas.backend.project.repository.ProjectRepository;
import com.veritas.backend.team.entity.Team;
import com.veritas.backend.team.repository.TeamRepository;
import com.veritas.backend.user.entity.User;
import com.veritas.backend.user.entity.UserRole;
import com.veritas.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
class ProjectControllerIntegrationTest extends BaseDBIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    UserRepository userRepository;

    @Autowired
    TeamRepository teamRepository;

    @Autowired
    ProjectRepository projectRepository;

    @Autowired
    JwtService jwtService;

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
                .isActive(true)
                .build());

        developmentTeam = teamRepository.save(Team.builder()
                .name("Development Team")
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
    void ProjectRetrieval_FinanceOfficer_ReturnsAllProjects() throws Exception {
        User financeOfficer = userRepository.save(User.builder()
                .name("Test Finance")
                .email("test@yahoo.com")
                .passwordHash(encoder.encode("password123"))
                .role(UserRole.FINANCE_OFFICER)
                .team(testingTeam)
                .department(Department.IT)
                .isActive(true)
                .build());

        String token = jwtService.generateAccessToken(financeOfficer);
        mockMvc.perform(get("/projects")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void ProjectRetrieval_Requester_ReturnsTeamProjects() throws Exception {
        User requester = userRepository.save(User.builder()
                .name("Test Requester")
                .email("test@yahoo.com")
                .passwordHash(encoder.encode("password123"))
                .role(UserRole.REQUESTER)
                .team(testingTeam)
                .department(Department.IT)
                .isActive(true)
                .build());

        String token = jwtService.generateAccessToken(requester);
        mockMvc.perform(get("/projects")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void ProjectCreation_FinanceOfficer_ReturnsCreatedProject() throws Exception {
        User financeOfficer = userRepository.save(User.builder()
                .name("Finance")
                .email("finance@test.com")
                .passwordHash(encoder.encode("password"))
                .role(UserRole.FINANCE_OFFICER)
                .team(testingTeam)
                .department(Department.IT)
                .isActive(true)
                .build());

        String token = jwtService.generateAccessToken(financeOfficer);
        ProjectCreationDto dto = new ProjectCreationDto("Controller Project", "CTRL-123", testingTeam.getTeamId(), LocalDate.now(), LocalDate.now().plusDays(10), BigDecimal.valueOf(1000));

        mockMvc.perform(post("/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(dto))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Controller Project"));
    }
}
