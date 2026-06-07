package com.veritas.backend.department;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.veritas.backend.BaseDBIntegrationTest;
import com.veritas.backend.audit.repository.AuditLogRepository;
import com.veritas.backend.auth.service.JwtService;
import com.veritas.backend.department.dto.DepartmentCreateDto;
import com.veritas.backend.department.entity.Department;
import com.veritas.backend.department.repository.DepartmentRepository;
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
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class DepartmentControllerIntegrationTest extends BaseDBIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private TeamRepository teamRepository;

        @Autowired
        private DepartmentRepository departmentRepository;

        @Autowired
        private ProjectRepository projectRepository;

        @Autowired
        private AuditLogRepository auditLogRepository;

        @Autowired
        private JwtService jwtService;

        @Autowired
        private PasswordEncoder encoder;

        private Team testingTeam;
        private Department existingDept;

        @BeforeEach
        void setup() {
                auditLogRepository.deleteAll();
                projectRepository.deleteAll();
                userRepository.deleteAll();
                teamRepository.deleteAll();
                departmentRepository.deleteAll();

                existingDept = departmentRepository.save(Department.builder()
                                .name("Existing Department")
                                .build());

                testingTeam = teamRepository.save(Team.builder()
                                .name("Testing Team")
                                .description("Handles QA and testing work")
                                .isActive(true)
                                .department(existingDept)
                                .build());
        }

        @Test
        void GetAllDepartments_Requester_ReturnsAllDepartments() throws Exception {
                User requester = userRepository.save(User.builder()
                                .name("Test Requester")
                                .email("requester@yahoo.com")
                                .passwordHash(encoder.encode("password123"))
                                .role(UserRole.REQUESTER)
                                .team(testingTeam)
                                .isActive(true)
                                .build());

                String token = jwtService.generateAccessToken(requester);

                mockMvc.perform(get("/api/v1/departments")
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(1))
                                .andExpect(jsonPath("$[0].name").value("Existing Department"));
        }

        @Test
        void GetDepartmentById_FinanceOfficer_ReturnsDepartment() throws Exception {
                User financeOfficer = userRepository.save(User.builder()
                                .name("Test Finance")
                                .email("finance@yahoo.com")
                                .passwordHash(encoder.encode("password123"))
                                .role(UserRole.FINANCE_OFFICER)
                                .team(testingTeam)
                                .isActive(true)
                                .build());

                String token = jwtService.generateAccessToken(financeOfficer);

                mockMvc.perform(get("/api/v1/departments/" + existingDept.getDepartmentId())
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.name").value("Existing Department"));
        }

        @Test
        void GetDepartmentById_Requester_IsForbidden() throws Exception {
                User requester = userRepository.save(User.builder()
                                .name("Test Requester")
                                .email("requester@yahoo.com")
                                .passwordHash(encoder.encode("password123"))
                                .role(UserRole.REQUESTER)
                                .team(testingTeam)
                                .isActive(true)
                                .build());

                String token = jwtService.generateAccessToken(requester);

                mockMvc.perform(get("/api/v1/departments/" + existingDept.getDepartmentId())
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isForbidden());
        }

        @Test
        void CreateDepartment_FinanceOfficer_ValidRequest_ReturnsCreatedDepartment() throws Exception {
                User financeOfficer = userRepository.save(User.builder()
                                .name("Test Finance")
                                .email("finance@yahoo.com")
                                .passwordHash(encoder.encode("password123"))
                                .role(UserRole.FINANCE_OFFICER)
                                .team(testingTeam)
                                .isActive(true)
                                .build());

                String token = jwtService.generateAccessToken(financeOfficer);
                DepartmentCreateDto dto = new DepartmentCreateDto("New Department", BigDecimal.valueOf(10000.0));

                mockMvc.perform(post("/api/v1/departments")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.name").value("New Department"));
        }

        @Test
        void CreateDepartment_Requester_IsForbidden() throws Exception {
                User requester = userRepository.save(User.builder()
                                .name("Test Requester")
                                .email("requester@yahoo.com")
                                .passwordHash(encoder.encode("password123"))
                                .role(UserRole.REQUESTER)
                                .team(testingTeam)
                                .isActive(true)
                                .build());

                String token = jwtService.generateAccessToken(requester);
                DepartmentCreateDto dto = new DepartmentCreateDto("Forbidden Department", BigDecimal.valueOf(10000.0));

                mockMvc.perform(post("/api/v1/departments")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                                .andExpect(status().isForbidden());
        }

        @Test
        void CreateDepartment_InvalidName_ReturnsBadRequest() throws Exception {
                User financeOfficer = userRepository.save(User.builder()
                                .name("Test Finance")
                                .email("finance@yahoo.com")
                                .passwordHash(encoder.encode("password123"))
                                .role(UserRole.FINANCE_OFFICER)
                                .team(testingTeam)
                                .isActive(true)
                                .build());

                String token = jwtService.generateAccessToken(financeOfficer);
                DepartmentCreateDto dto = new DepartmentCreateDto("", BigDecimal.valueOf(10000.0)); // Blank name should trigger validation
                                                                       // constraint

                mockMvc.perform(post("/api/v1/departments")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void UpdateDepartment_FinanceOfficer_ReturnsOk() throws Exception {
                User financeOfficer = userRepository.save(User.builder()
                                .name("Test Finance")
                                .email("finance@yahoo.com")
                                .passwordHash(encoder.encode("password123"))
                                .role(UserRole.FINANCE_OFFICER)
                                .team(testingTeam)
                                .isActive(true)
                                .build());

                String token = jwtService.generateAccessToken(financeOfficer);
                DepartmentCreateDto dto = new DepartmentCreateDto("Updated Department", BigDecimal.valueOf(10000.0));

                mockMvc.perform(put("/api/v1/departments/" + existingDept.getDepartmentId())
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.name").value("Updated Department"));
        }

        @Test
        void DeleteDepartment_FinanceOfficer_ReturnsNoContent() throws Exception {
                User financeOfficer = userRepository.save(User.builder()
                                .name("Test Finance")
                                .email("finance@yahoo.com")
                                .passwordHash(encoder.encode("password123"))
                                .role(UserRole.FINANCE_OFFICER)
                                .team(testingTeam)
                                .isActive(true)
                                .build());

                String token = jwtService.generateAccessToken(financeOfficer);

                Department tempDept = departmentRepository.save(Department.builder()
                                .name("Temporary Department")
                                .build());

                mockMvc.perform(delete("/api/v1/departments/" + tempDept.getDepartmentId())
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isNoContent());
        }

        @Test
        void DeleteDepartment_FinanceOfficer_HasReferencingTeams_ReturnsConflict() throws Exception {
                User financeOfficer = userRepository.save(User.builder()
                                .name("Test Finance")
                                .email("finance@yahoo.com")
                                .passwordHash(encoder.encode("password123"))
                                .role(UserRole.FINANCE_OFFICER)
                                .team(testingTeam)
                                .isActive(true)
                                .build());

                String token = jwtService.generateAccessToken(financeOfficer);

                mockMvc.perform(delete("/api/v1/departments/" + existingDept.getDepartmentId())
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isConflict());
        }
}
