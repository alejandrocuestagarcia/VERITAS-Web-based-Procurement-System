package com.veritas.backend.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.veritas.backend.BaseDBIntegrationTest;
import com.veritas.backend.common.model.Department;
import com.veritas.backend.auth.repository.RefreshTokenRepository;
import com.veritas.backend.project.repository.ProjectRepository;
import com.veritas.backend.team.entity.Team;
import com.veritas.backend.team.repository.TeamRepository;
import com.veritas.backend.user.dto.UserCreationRequestDto;
import com.veritas.backend.user.dto.UserEditDto;
import com.veritas.backend.user.entity.User;
import com.veritas.backend.user.entity.UserRole;
import com.veritas.backend.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserControllerIntegrationTest extends BaseDBIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private TeamRepository teamRepository;

  @Autowired
  private ProjectRepository projectRepository;

  @Autowired
  private PasswordEncoder encoder;

  private Team testTeam;

  @Autowired
  private RefreshTokenRepository refreshTokenRepository;

  @BeforeEach
  void setUp() {
    projectRepository.deleteAll();
    userRepository.deleteAll();
    teamRepository.deleteAll();

    testTeam = new Team();
    testTeam.setName("Test Team");
    testTeam.setDepartment(Department.IT);
    testTeam.setDescription("Description Placeholder");

    testTeam = teamRepository.save(testTeam);
  }

  @AfterEach
  void tearDown() {
    teamRepository.findAll().forEach(team -> {
      team.setLeader(null);
      teamRepository.save(team);
    });

    userRepository.deleteAll();
    teamRepository.deleteAll();
  }

  @Test
  @WithMockUser(roles = "FINANCE_OFFICER")
  void UserRetrieval_SearchQueryProvided_ReturnsMatchingUsers()
      throws Exception {
    User alex = User.builder().name("Alexander Sterling").email("alex@test.com").team(testTeam)
        .isActive(true).passwordHash(encoder.encode("password123")).role(UserRole.REQUESTER)
        .build();
    User john = User.builder().name("John Doe").email("john@test.com").team(testTeam).isActive(true)
        .passwordHash(encoder.encode("password123")).role(UserRole.FINANCE_OFFICER).build();
    userRepository.saveAll(java.util.List.of(alex, john));

    mockMvc.perform(
            get("/users").param("search", "Alexander").param("page", "0").param("size", "10"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].name").value("Alexander Sterling"));
  }

  @Test
  @WithMockUser(roles = "FINANCE_OFFICER")
  void UserCreation_ValidInput_ReturnsCreated() throws Exception {
    UserCreationRequestDto request = new UserCreationRequestDto(
        "newuser@veritas.com", "New User", "securePassword123",
        UserRole.ADMINISTRATOR, testTeam.getTeamId(), Department.IT, false);

    mockMvc.perform(post("/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.email").value("newuser@veritas.com"))
        .andExpect(jsonPath("$.name").value("New User"));
  }

  @Test
  @WithMockUser(roles = "FINANCE_OFFICER")
  void UserCreation_DuplicateEmail_ReturnsConflict() throws Exception {
    UserCreationRequestDto request1 = new UserCreationRequestDto(
        "duplicate@veritas.com", "First User", "securePassword123",
        UserRole.FINANCE_OFFICER, testTeam.getTeamId(), Department.IT, false);

    mockMvc.perform(post("/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request1)))
        .andExpect(status().isCreated());

    UserCreationRequestDto request2 = new UserCreationRequestDto(
        "duplicate@veritas.com", "Second User", "securePassword456",
        UserRole.FINANCE_OFFICER, testTeam.getTeamId(), Department.IT, false);

    mockMvc.perform(post("/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request2)))
        .andExpect(status().isConflict());
  }

  @Test
  @WithMockUser(roles = "FINANCE_OFFICER")
  void UserRetrieval_PartialEmailMatch_ReturnsUser() throws Exception {
    User alex = User.builder().name("Alex").email("alexander.sterling@veritas.com").team(testTeam)
        .isActive(true).role(UserRole.REQUESTER).passwordHash("hash").build();
    User john = User.builder().name("John").email("john@test.com").team(testTeam)
        .role(UserRole.FINANCE_OFFICER).isActive(true).passwordHash("hash").build();

    userRepository.save(alex);
    userRepository.save(john);

    mockMvc.perform(
            get("/users").param("search", "sterling").param("page", "0").param("size", "10"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].email").value("alexander.sterling@veritas.com"));
  }

  @Test
  @WithMockUser(roles = "FINANCE_OFFICER")
  void UserRetrieval_RoleMatch_ReturnsUsers() throws Exception {
    User alex =
        User.builder().name("Alex").email("alex@test.com").team(testTeam).role(UserRole.REQUESTER)
            .isActive(true).passwordHash("hash").build();
    User john = User.builder().name("John").email("john@test.com").team(testTeam)
        .role(UserRole.FINANCE_OFFICER).isActive(true).passwordHash("hash").build();
    userRepository.saveAll(java.util.List.of(alex, john));

    mockMvc.perform(
            get("/users").param("userRole", "REQUESTER").param("page", "0").param("size", "10"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].role").value("REQUESTER"));
  }

  @Test
  @WithMockUser(roles = "FINANCE_OFFICER")
  void UserRetrieval_NameMatchesButRoleDoesNot_ReturnsEmpty() throws Exception {
    User alex = User.builder().name("Alexander").email("alex@test.com").team(testTeam)
        .role(UserRole.REQUESTER).isActive(true).passwordHash("hash").build();
    User john = User.builder().name("John").email("john@test.com").team(testTeam)
        .role(UserRole.FINANCE_OFFICER).isActive(true).passwordHash("hash").build();

    userRepository.save(alex);
    userRepository.save(john);

    mockMvc.perform(get("/users").param("search", "Alex").param("userRole", "FINANCE_OFFICER")
            .param("page", "0").param("size", "10")).andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(0));
  }


  @Test
  @WithMockUser(roles = "FINANCE_OFFICER")
  void UserStats_Called_ReturnsTotalAndInactiveCounts() throws Exception {
    mockMvc.perform(get("/users/stats")).andExpect(status().isOk())
        .andExpect(jsonPath("$.total").isNumber()).andExpect(jsonPath("$.inactive").isNumber())
        .andExpect(jsonPath("$.activeSessions").isNumber());
  }

  @Test
  @WithMockUser(roles = "REQUESTER")
  void UserRetrieval_AsRequester_ReturnsForbidden() throws Exception {
    mockMvc.perform(get("/users")).andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "REQUESTER")
  void UserStats_AsRequester_ReturnsForbidden() throws Exception {
    mockMvc.perform(get("/users/stats")).andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "FINANCE_OFFICER")
  void editUser_shouldUpdateNameAndEmail_whenValid() throws Exception {
    User finance = User.builder()
            .name("Test Finance")
            .email("finance@test.com")
            .team(testTeam)
            .isActive(true)
            .passwordHash(encoder.encode("password"))
            .role(UserRole.REQUESTER)
            .build();
    userRepository.save(finance);

    UserEditDto edit = new UserEditDto("new@test.com", "New Name", null, null);
    mockMvc.perform(patch("/users/" + finance.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(edit)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("New Name"))
            .andExpect(jsonPath("$.email").value("new@test.com"));
  }

  @Test
  @WithMockUser(roles = "FINANCE_OFFICER")
  void editUser_shouldReturnConflict_whenEmailExists() throws Exception {
    User user1 = User.builder()
            .name("First Finance User")
            .email("finance1@test.com")
            .team(testTeam)
            .isActive(true)
            .passwordHash(encoder.encode("password"))
            .role(UserRole.REQUESTER)
            .build();

    User user2 = User.builder()
            .name("Second Finance User")
            .email("finance2@test.com")
            .team(testTeam)
            .isActive(true)
            .passwordHash(encoder.encode("password"))
            .role(UserRole.REQUESTER)
            .build();

    userRepository.save(user1);
    userRepository.save(user2);

    UserEditDto edit = new UserEditDto("finance2@test.com", "First Finance User", null, null);
    mockMvc.perform(patch("/users/" + user1.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(edit)))
            .andExpect(status().isConflict());
  }

  @Test
  @WithMockUser(roles = "FINANCE_OFFICER")
  void editUser_shouldReturnBadRequest_whenChangingTeamAsTeamLeader() throws Exception {
    User user = User.builder()
            .name("Leader")
            .email("leader@test.com")
            .team(testTeam)
            .isActive(true)
            .passwordHash(encoder.encode("password"))
            .role(UserRole.REQUESTER)
            .build();

    userRepository.save(user);

    testTeam.setLeader(user);
    teamRepository.save(testTeam);

    Team newTeam = new Team();
    newTeam.setName("Other Team");
    newTeam.setDepartment(Department.IT);
    teamRepository.save(newTeam);

    UserEditDto edit = new UserEditDto(null, null, newTeam.getTeamId(), true);
    mockMvc.perform(patch("/users/" + user.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(edit)))
            .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "FINANCE_OFFICER")
  void getUserByIdForEdit_shouldReturnUser_whenExists() throws Exception {
    User user = User.builder()
            .name("Test Requester")
            .email("requester@test.com")
            .team(testTeam)
            .isActive(true)
            .passwordHash(encoder.encode("password"))
            .role(UserRole.REQUESTER)
            .build();
    userRepository.save(user);

    mockMvc.perform(get("/users/" + user.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("requester@test.com"))
            .andExpect(jsonPath("$.name").value("Test Requester"))
            .andExpect(jsonPath("$.teamId").value(testTeam.getTeamId()))
            .andExpect(jsonPath("$.isTeamLeader").value(false));
  }
}
