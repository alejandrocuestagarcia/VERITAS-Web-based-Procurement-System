package com.veritas.backend.project.service;

import com.veritas.backend.BaseDBIntegrationTest;
import com.veritas.backend.budget.entity.InternalBudget;
import com.veritas.backend.budget.entity.BudgetType;
import com.veritas.backend.project.dto.ProjectCreationDto;
import com.veritas.backend.project.repository.ProjectRepository;
import com.veritas.backend.project.service.ProjectService;
import com.veritas.backend.requisition.repository.RequestRepository;
import com.veritas.backend.team.repository.TeamRepository;
import com.veritas.backend.department.entity.Department;
import com.veritas.backend.department.repository.DepartmentRepository;
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

import static org.junit.jupiter.api.Assertions.*;

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
    RequestRepository requestRepository;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    DepartmentRepository departmentRepository;

    Team testingTeam;
    Team developmentTeam;
    Department qaDepartment;
    Department devDepartment;
    Project project;

    @BeforeEach
    void setup() {
        requestRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();
        teamRepository.deleteAll();
        departmentRepository.deleteAll();

        qaDepartment = departmentRepository.save(Department.builder()
                .name("QA Dept")
                .internalBudget(InternalBudget.builder()
                        .budgetName("QA Dept")
                        .budgetType(BudgetType.DEPARTMENT)
                        .totalAmount(BigDecimal.valueOf(1000000.0))
                        .build())
                .build());

        devDepartment = departmentRepository.save(Department.builder()
                .name("Dev Dept")
                .internalBudget(InternalBudget.builder()
                        .budgetName("Dev Dept")
                        .budgetType(BudgetType.DEPARTMENT)
                        .totalAmount(BigDecimal.valueOf(1000000.0))
                        .build())
                .build());

        testingTeam = teamRepository.save(Team.builder()
                .name("Testing Team")
                .description("Handles QA and testing work")
                .isActive(true)
                .department(qaDepartment)
                .build());

        developmentTeam = teamRepository.save(Team.builder()
                .name("Development Team")
                .description("Builds product features")
                .isActive(true)
                .department(devDepartment)
                .build());

        project = projectRepository.save(Project.builder()
                .name("Super Secret Project")
                .projectKey(UUID.randomUUID().toString())
                .team(testingTeam)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(30))
                .internalBudget(InternalBudget.builder()
                        .budgetName("Test Budget")
                        .budgetType(BudgetType.PROJECT)
                        .totalAmount(BigDecimal.valueOf(10000.00))
                        .build())
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

        assertAll(
            () -> assertEquals(1, result.size()),
            () -> assertEquals(project.getName(), result.getFirst().name())
        );
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

        assertAll(
            () -> assertEquals(1, result.size()),
            () -> assertEquals(project.getName(), result.getFirst().name())
        );
    }

    @Test
    void ProjectRetrieval_ProcurementOfficer_ReturnsProjectsFromTheirDepartment() {
        User procurementOfficer = userRepository.save(User.builder()
                .name("Test Procurement")
                .email("test-proc@yahoo.com")
                .passwordHash(encoder.encode("password123"))
                .role(UserRole.PROCUREMENT_OFFICER)
                .department(qaDepartment)
                .isActive(true)
                .build());

        var result = projectService.getProjectsForUser(procurementOfficer);

        assertAll(
            () -> assertEquals(1, result.size()),
            () -> assertEquals(project.getName(), result.getFirst().name())
        );
    }

    @Test
    void ProjectRetrieval_ProcurementOfficer_DoesNotReturnProjectsFromOtherDepartments() {
        User procurementOfficer = userRepository.save(User.builder()
                .name("Test Procurement")
                .email("test-proc@yahoo.com")
                .passwordHash(encoder.encode("password123"))
                .role(UserRole.PROCUREMENT_OFFICER)
                .department(devDepartment)
                .isActive(true)
                .build());

        var result = projectService.getProjectsForUser(procurementOfficer);

        assertTrue(result.isEmpty());
    }

    @Test
    void ProjectCreation_ValidInput_ReturnsCreatedProject() {
        var dto = new ProjectCreationDto("Integration Project", UUID.randomUUID().toString(), testingTeam.getTeamId(), LocalDate.now(), LocalDate.now().plusDays(10), BigDecimal.valueOf(5000));

        var result = projectService.createProject(dto);

        assertAll(
            () -> assertNotNull(result),
            () -> assertEquals("Integration Project", result.name())
        );

        var projects = projectRepository.findAll();
        assertEquals(2, projects.size());
    }
}
