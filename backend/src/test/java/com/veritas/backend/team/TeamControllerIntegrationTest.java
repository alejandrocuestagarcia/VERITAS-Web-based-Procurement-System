package com.veritas.backend.team;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.veritas.backend.BaseDBIntegrationTest;
import com.veritas.backend.auth.service.JwtService;
import com.veritas.backend.project.repository.ProjectRepository;
import com.veritas.backend.team.dto.TeamCreateDto;
import com.veritas.backend.team.repository.TeamRepository;
import com.veritas.backend.user.entity.User;
import com.veritas.backend.user.entity.UserRole;
import com.veritas.backend.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// AI-GENERATED
@SpringBootTest
@AutoConfigureMockMvc
class TeamControllerIntegrationTest extends BaseDBIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    TeamRepository teamRepository;

    @Autowired
    ProjectRepository projectRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    JwtService jwtService;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setup() {
        projectRepository.deleteAll();

        // Break circular references between users.team_id and teams.leader_id before deletes.
        jdbcTemplate.update("UPDATE users SET team_id = NULL");
        jdbcTemplate.update("UPDATE teams SET leader_id = NULL");

        userRepository.deleteAll();
        teamRepository.deleteAll();
    }

    @Test
    void TeamCreation_AsFinanceOfficer_ReturnsCreated() throws Exception {
        String token = createTokenForRole(UserRole.FINANCE_OFFICER);
        TeamCreateDto request = createTeamRequest("Platform Team");

        mockMvc.perform(post("/api/v1/teams")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Platform Team"))
                .andExpect(jsonPath("$.description").value("Owns internal developer platform"))
                .andExpect(jsonPath("$.department").value("IT"));
    }

    @Test
    void TeamCreation_AsAdministrator_ReturnsCreated() throws Exception {
        String token = createTokenForRole(UserRole.ADMINISTRATOR);
        TeamCreateDto request = createTeamRequest("Operations Team");

        mockMvc.perform(post("/api/v1/teams")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Operations Team"));
    }

    @Test
    void TeamCreation_AsRequester_ReturnsForbidden() throws Exception {
        String token = createTokenForRole(UserRole.REQUESTER);
        TeamCreateDto request = createTeamRequest("Restricted Team");

        mockMvc.perform(post("/api/v1/teams")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void TeamCreation_MissingDescription_ReturnsBadRequest() throws Exception {
        String token = createTokenForRole(UserRole.FINANCE_OFFICER);
        TeamCreateDto request = new TeamCreateDto();
        request.setName("Incomplete Team");

        mockMvc.perform(post("/api/v1/teams")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.description").value("Team description is required"));
    }

    @Test
    void TeamCreation_DuplicateName_ReturnsConflict() throws Exception {
        String token = createTokenForRole(UserRole.ADMINISTRATOR);
        TeamCreateDto first = createTeamRequest("Core Team");
        TeamCreateDto duplicate = createTeamRequest("core team");

        mockMvc.perform(post("/api/v1/teams")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/teams")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicate)))
                .andExpect(status().isConflict())
                .andExpect(content().string(containsString("already exists")));
    }

    @Test
    void TeamCreation_LeaderNotFound_ReturnsNotFound() throws Exception {
        String token = createTokenForRole(UserRole.FINANCE_OFFICER);
        TeamCreateDto request = createTeamRequest("Team With Missing Leader");
        request.setLeaderId(999999L);

        mockMvc.perform(post("/api/v1/teams")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Leader not found with id 999999"));
    }

    @Test
    void TeamCreation_WithLeader_ReturnsCreatedAndSetsLeader() throws Exception {
        String token = createTokenForRole(UserRole.ADMINISTRATOR);
        User leader = userRepository.save(User.builder()
                .name("Leader User")
                .email("leader-" + UUID.randomUUID() + "@veritas.com")
                .passwordHash(encoder.encode("password123"))
                .role(UserRole.PROCUREMENT_OFFICER)
                .isActive(true)
                .build());

        TeamCreateDto request = createTeamRequest("Leadership Team");
        request.setLeaderId(leader.getId());

        MvcResult result = mockMvc.perform(post("/api/v1/teams")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.leaderId").value(leader.getId()))
                .andReturn();

        Long createdTeamId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        User persistedLeader = userRepository.findById(leader.getId()).orElseThrow();
        assertNotNull(persistedLeader.getTeam());
        assertEquals(createdTeamId, persistedLeader.getTeam().getTeamId());
    }

    @Test
    void GetAllTeams_AsAdministrator_ReturnsListOfTeams() throws Exception {
        String token = createTokenForRole(UserRole.ADMINISTRATOR);

        mockMvc.perform(post("/api/v1/teams")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createTeamRequest("Alpha Team"))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/teams")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createTeamRequest("Beta Team"))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/teams")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Alpha Team"))
                .andExpect(jsonPath("$[1].name").value("Beta Team"));
    }

    @Test
    void GetAllTeams_AsRequester_ReturnsForbidden() throws Exception {
        String token = createTokenForRole(UserRole.REQUESTER);

        mockMvc.perform(get("/api/v1/teams")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    private String createTokenForRole(UserRole role) {
        User user = userRepository.save(User.builder()
                .name("Test " + role.name())
                .email(role.name().toLowerCase() + "-" + UUID.randomUUID() + "@veritas.com")
                .passwordHash(encoder.encode("password123"))
                .role(role)
                .isActive(true)
                .build());

        return jwtService.generateAccessToken(user);
    }

    private TeamCreateDto createTeamRequest(String name) {
        TeamCreateDto request = new TeamCreateDto();
        request.setName(name);
        request.setDescription("Owns internal developer platform");
        request.setDepartmentId(1L);
        request.setExpiresAt(LocalDateTime.now().plusDays(30));
        return request;
    }
}