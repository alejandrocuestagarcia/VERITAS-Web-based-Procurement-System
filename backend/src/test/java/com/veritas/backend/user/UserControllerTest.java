package com.veritas.backend.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.veritas.backend.BaseDBIntegrationTest;
import com.veritas.backend.common.model.Department;
import com.veritas.backend.team.entity.Team;
import com.veritas.backend.team.repository.TeamRepository;
import com.veritas.backend.user.dto.UserCreationRequestDto;
import com.veritas.backend.user.entity.UserRole;
import com.veritas.backend.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest extends BaseDBIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private TeamRepository teamRepository;

        @BeforeEach
        void setUp() {
                userRepository.deleteAll();
                teamRepository.deleteAll();

                Team testTeam = new Team();
                testTeam.setName("Test Team");
                testTeam.setDepartment(Department.IT);
                teamRepository.save(testTeam);
        }

        @AfterEach
        void tearDown() {
                userRepository.deleteAll();
                teamRepository.deleteAll();
        }

        @Test
        @WithMockUser(roles = "FINANCE_OFFICER")
        void createUser_shouldSucceed_withValidUserInput() throws Exception {
                UserCreationRequestDto request = new UserCreationRequestDto(
                                "newuser@veritas.com", "New User", "securePassword123",
                                UserRole.ADMINISTRATOR, 999L, Department.IT, false);

                mockMvc.perform(post("/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.email").value("newuser@veritas.com"))
                                .andExpect(jsonPath("$.name").value("New User"));
        }

        @Test
        @WithMockUser(roles = "FINANCE_OFFICER")
        void createUser_shouldConflict_withValidDuplicateUserMail() throws Exception {
                UserCreationRequestDto request1 = new UserCreationRequestDto(
                                "duplicate@veritas.com", "First User", "securePassword123",
                                UserRole.FINANCE_OFFICER, 999L, Department.IT, false);

                mockMvc.perform(post("/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request1)))
                                .andExpect(status().isCreated());

                UserCreationRequestDto request2 = new UserCreationRequestDto(
                                "duplicate@veritas.com", "Second User", "securePassword456",
                                UserRole.FINANCE_OFFICER, 999L, Department.IT, false);

                mockMvc.perform(post("/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request2)))
                                .andExpect(status().isConflict());
        }
}
