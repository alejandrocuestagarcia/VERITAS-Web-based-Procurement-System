package com.veritas.backend.project;

import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.veritas.backend.BaseDBIntegrationTest;
import com.veritas.backend.auth.service.JwtService;
import com.veritas.backend.department.entity.Department;
import com.veritas.backend.department.repository.DepartmentRepository;
import com.veritas.backend.budget.entity.InternalBudget;
import com.veritas.backend.project.dto.ProjectCreationDto;
import com.veritas.backend.project.dto.ProjectEditDto;
import com.veritas.backend.project.entity.Project;
import com.veritas.backend.project.repository.ProjectRepository;
import com.veritas.backend.requisition.repository.RequestRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;

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
    DepartmentRepository departmentRepository;

    @Autowired
    RequestRepository requestRepository;

    @Autowired
    JwtService jwtService;

    @Autowired
    PasswordEncoder encoder;

    Team testingTeam;
    Team developmentTeam;
    Project project;
    Department departmentTesting;
    Department departmentDev;

    @BeforeEach
    void setup() {
        requestRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();
        teamRepository.deleteAll();
        departmentRepository.deleteAll();

        departmentTesting = departmentRepository.save(Department.builder()
                .name("Testing")
                .build());

        departmentDev = departmentRepository.save(Department.builder()
                .name("Dev")
                .build());

        testingTeam = teamRepository.save(Team.builder()
                .name("Testing Team")
                .description("Handles QA and testing work")
                .department(departmentTesting)
                .isActive(true)
                .build());

        developmentTeam = teamRepository.save(Team.builder()
                .name("Development Team")
                .description("Builds product features")
                .department(departmentDev)
                .isActive(true)
                .build());

        project = projectRepository.save(Project.builder()
                .name("Super Secret Project")
                .projectKey(UUID.randomUUID().toString())
                .team(testingTeam)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(30))
                .internalBudget(InternalBudget.builder().budgetName("Test Budget").totalAmount(BigDecimal.valueOf(10000.00)).build())
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
                .isActive(true)
                .build());

        String token = jwtService.generateAccessToken(financeOfficer);
        mockMvc.perform(get("/api/v1/projects")
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
                .isActive(true)
                .build());

        String token = jwtService.generateAccessToken(requester);
        mockMvc.perform(get("/api/v1/projects")
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
                .isActive(true)
                .build());

        String token = jwtService.generateAccessToken(financeOfficer);
        ProjectCreationDto dto = new ProjectCreationDto("Controller Project", "CTRL-123", testingTeam.getTeamId(), LocalDate.now(), LocalDate.now().plusDays(10), BigDecimal.valueOf(1000));

        mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(dto))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Controller Project"));
    }

    @Test
    void GetProjectById_RequesterFromSameTeam_ReturnsProject() throws Exception {

        User requester = userRepository.save(User.builder()
                .name("Team Requester")
                .email("requester.team@test.com")
                .passwordHash(encoder.encode("password"))
                .role(UserRole.REQUESTER)
                .team(testingTeam)
                .isActive(true)
                .build());

        String token = jwtService.generateAccessToken(requester);

        mockMvc.perform(get("/api/v1/projects/" + project.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Super Secret Project"));
    }

    @Test
    void GetProjectById_RequesterFromDifferentTeam_ReturnsNotFound() throws Exception {

        User wrongTeamRequester = userRepository.save(User.builder()
                .name("Wrong Team Requester")
                .email("requester.wrong@test.com")
                .passwordHash(encoder.encode("password"))
                .role(UserRole.REQUESTER)
                .team(developmentTeam)
                .isActive(true)
                .build());

        String token = jwtService.generateAccessToken(wrongTeamRequester);

        mockMvc.perform(get("/api/v1/projects/" + project.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void GetProjectById_ProcurementOfficerFromSameDepartment_ReturnsProject() throws Exception {

        User procurementOfficer = userRepository.save(User.builder()
                .name("Dept Procurement")
                .email("procurement.dept@test.com")
                .passwordHash(encoder.encode("password"))
                .role(UserRole.PROCUREMENT_OFFICER)
                .team(testingTeam)
                .department(departmentTesting)
                .isActive(true)
                .build());

        String token = jwtService.generateAccessToken(procurementOfficer);

        mockMvc.perform(get("/api/v1/projects/" + project.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Super Secret Project"));
    }

    @Test
    void GetProjectById_ProcurementOfficerFromDifferentDepartment_ReturnsNotFound() throws Exception {

        User wrongDeptProcurement = userRepository.save(User.builder()
                .name("Wrong Dept Procurement")
                .email("procurement.wrong@test.com")
                .passwordHash(encoder.encode("password"))
                .role(UserRole.PROCUREMENT_OFFICER)
                .team(developmentTeam)
                .department(departmentDev)
                .isActive(true)
                .build());

        String token = jwtService.generateAccessToken(wrongDeptProcurement);

        mockMvc.perform(get("/api/v1/projects/" + project.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void EditProject_FinanceOfficer_UpdatesAndReturnsProject() throws Exception {

        User financeOfficer = userRepository.save(User.builder()
                .name("Finance Edit Exec")
                .email("finance.edit@test.com")
                .passwordHash(encoder.encode("password"))
                .role(UserRole.FINANCE_OFFICER)
                .team(testingTeam)
                .isActive(true)
                .build());

        String token = jwtService.generateAccessToken(financeOfficer);

        ProjectEditDto editDto =
                new ProjectEditDto("Renamed Massive Project", BigDecimal.valueOf(99999.99),null,LocalDate.now().plusDays(1),LocalDate.now().plusYears(1));


        mockMvc.perform(patch("/api/v1/projects/" + project.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(editDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed Massive Project"))
                .andExpect(jsonPath("$.budget").value(99999.99));
    }
    @Test
    void EditProject_FinanceOfficerWrongStartDate_ReturnsBadRequest() throws Exception {

        User financeOfficer = userRepository.save(User.builder()
                .name("Finance Edit Exec")
                .email("finance.edit@test.com")
                .passwordHash(encoder.encode("password"))
                .role(UserRole.FINANCE_OFFICER)
                .team(testingTeam)
                .isActive(true)
                .build());

        String token = jwtService.generateAccessToken(financeOfficer);

        ProjectEditDto editDto =
                new ProjectEditDto("Renamed Massive Project", BigDecimal.valueOf(99999.99),null ,LocalDate.now().plusDays(-1),LocalDate.now().plusYears(1));


        mockMvc.perform(patch("/api/v1/projects/" + project.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(editDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void EditProject_FinanceOfficerWrongEndDate_ReturnsBadRequest() throws Exception {

        User financeOfficer = userRepository.save(User.builder()
                .name("Finance Edit Exec")
                .email("finance.edit@test.com")
                .passwordHash(encoder.encode("password"))
                .role(UserRole.FINANCE_OFFICER)
                .team(testingTeam)
                .isActive(true)
                .build());

        String token = jwtService.generateAccessToken(financeOfficer);

        ProjectEditDto editDto =
                new ProjectEditDto("Renamed Massive Project", BigDecimal.valueOf(99999.99),null ,LocalDate.now().plusDays(1),LocalDate.now().plusYears(-1));


        mockMvc.perform(patch("/api/v1/projects/" + project.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(editDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void EditProject_Requester_ReturnsForbidden() throws Exception {

        User unauthorizedRequester = userRepository.save(User.builder()
                .name("Sneaky Requester")
                .email("sneaky@test.com")
                .passwordHash(encoder.encode("password"))
                .role(UserRole.REQUESTER)
                .team(testingTeam)
                .isActive(true)
                .build());

        String token = jwtService.generateAccessToken(unauthorizedRequester);
        ProjectEditDto editDto =
                new ProjectEditDto("Hack Attempt Name", BigDecimal.valueOf(0),null ,LocalDate.now().plusDays(1),LocalDate.now().plusYears(1));

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/projects/" + project.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(editDto)))
                .andExpect(status().isForbidden());
    }
}
