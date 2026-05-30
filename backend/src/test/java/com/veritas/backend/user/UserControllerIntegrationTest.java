package com.veritas.backend.user;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.veritas.backend.BaseDBIntegrationTest;
import com.veritas.backend.project.repository.ProjectRepository;
import com.veritas.backend.requisition.entity.Priority;
import com.veritas.backend.requisition.entity.Request;
import com.veritas.backend.requisition.repository.RequestRepository;
import com.veritas.backend.team.entity.Team;
import com.veritas.backend.team.repository.TeamRepository;
import com.veritas.backend.user.dto.UserCreationRequestDto;
import com.veritas.backend.user.dto.UserEditDto;
import com.veritas.backend.user.entity.User;
import com.veritas.backend.user.entity.UserRole;
import com.veritas.backend.user.repository.UserRepository;
import com.veritas.backend.workflow.entity.WorkflowComponent;
import com.veritas.backend.workflow.entity.WorkflowStep;
import com.veritas.backend.workflow.repository.WorkflowStepRepository;
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
import org.springframework.jdbc.core.JdbcTemplate;
import jakarta.persistence.EntityManager;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@SpringBootTest
@AutoConfigureMockMvc
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
  private RequestRepository requestRepository;

  @Autowired
  private PasswordEncoder encoder;

  private Team testTeam;

  @Autowired
  private WorkflowStepRepository workflowStepRepository;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private EntityManager entityManager;

  @BeforeEach
  void setUp() {
    requestRepository.deleteAll();
    projectRepository.deleteAll();

    jdbcTemplate.update("UPDATE users SET team_id = NULL");
    jdbcTemplate.update("UPDATE teams SET leader_id = NULL");

    entityManager.clear();

    userRepository.deleteAll();
    teamRepository.deleteAll();

    testTeam = new Team();
    testTeam.setName("Test Team");
    testTeam.setDescription("Description Placeholder");

    testTeam = teamRepository.save(testTeam);
  }

  @AfterEach
  void tearDown() {
    requestRepository.deleteAll();
    projectRepository.deleteAll();

    jdbcTemplate.update("UPDATE users SET team_id = NULL");
    jdbcTemplate.update("UPDATE teams SET leader_id = NULL");

    entityManager.clear();

    userRepository.deleteAll();
    teamRepository.deleteAll();
  }

  @Test
  @WithMockUser(roles = "ADMINISTRATOR")
  void DeleteByUserId_UserIdAndFallBackUserProvided_DeactivatesUserAndReassignsRequests()
          throws Exception {

    User alex = User.builder().name("Alexander Sterling").email("alex@test.com").team(testTeam)
            .isActive(true).passwordHash(encoder.encode("password123")).role(UserRole.REQUESTER)
            .build();

    User john =
            User.builder().name("John Doe").email("john@test.com").team(testTeam).isActive(true)
                    .passwordHash(encoder.encode("password123")).role(UserRole.REQUESTER).build();

    userRepository.saveAll(java.util.List.of(alex, john));


    Long userId = alex.getId();
    Long fallBackUserId = john.getId();

    mockMvc.perform(
                    delete("/api/v1/users/" + userId)
                            .param("fallbackUserId", fallBackUserId.toString()))
            .andExpect(status().isNoContent());

    User afterOperationAlex = userRepository.findById(userId).get();

    assertFalse(afterOperationAlex.getIsActive());


  }

  @Test
  @WithMockUser(roles = "PROCUREMENT_OFFICER")
  void DeleteByUserId_AsProcurementOfficer_IsForbidden() throws Exception {

    User alex = User.builder().name("Alexander Sterling").email("alex@test.com").team(testTeam)
            .isActive(true).passwordHash(encoder.encode("password123")).role(UserRole.REQUESTER)
            .build();

    User john =
            User.builder().name("John Doe").email("john@test.com").team(testTeam).isActive(true)
                    .passwordHash(encoder.encode("password123")).role(UserRole.FINANCE_OFFICER).build();

    userRepository.saveAll(java.util.List.of(alex, john));


    Long userId = alex.getId();
    Long fallBackUserId = john.getId();

    mockMvc.perform(
                    delete("/api/v1/users/" + userId)
                            .param("fallbackUserId", fallBackUserId.toString()))
            .andExpect(status().isForbidden());

    User afterOperationAlex = userRepository.findById(userId).get();

    assertTrue(afterOperationAlex.getIsActive());


  }

  @Test
  void DeleteByUserId_WithoutAuthenticatedUser_IsForbidden() throws Exception {


    mockMvc.perform(
            delete("/api/v1/users/" + 1)
    ).andExpect(status().isForbidden());

  }


  @Test
  @WithMockUser(roles = "ADMINISTRATOR")
  void GetPendingRequisitions_UserHasRequests_ReturnsList() throws Exception {

    User alex = User.builder()
            .name("Alexander Sterling")
            .email("alex@test.com")
            .isActive(true)
            .passwordHash(encoder.encode("password123"))
            .role(UserRole.REQUESTER)
            .build();
    alex = userRepository.save(alex);

    WorkflowStep startStep = new WorkflowStep();
    startStep.setName("Start");
    startStep.setWorkflowComponent(WorkflowComponent.START_EVENT);
    startStep = workflowStepRepository.save(startStep);

    Request dummyRequest = new Request();
    dummyRequest.setRequestName("New Laptop for Alex");
    dummyRequest.setUserID(alex);
    dummyRequest.setPriority(Priority.MEDIUM);
    dummyRequest.setCurrentStepID(startStep);

    requestRepository.save(dummyRequest);

    mockMvc.perform(get("/api/v1/users/" + alex.getId() + "/pending-requests"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isNotEmpty())
            .andExpect(jsonPath("$[0].requestName").value("New Laptop for Alex"));
  }

  @Test
  @WithMockUser(roles = "PROCUREMENT_OFFICER")
  void GetPendingRequisitions_AsProcurementOfficer_IsForbidden() throws Exception {


    mockMvc.perform(
            get("/api/v1/users/" + 1 + "/pending-requests")).andExpect(status().isForbidden());


  }

  @Test
  void GetPendingRequisitions_WithoutAuthenticatedUser_IsForbidden() throws Exception {


    mockMvc.perform(
            delete("/api/v1/users/" + 1 + "/pending-requests")
    ).andExpect(status().isForbidden());

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
            get("/api/v1/users").param("search", "Alexander").param("page", "0").param("size", "10"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].name").value("Alexander Sterling"));
  }

  @Test
  @WithMockUser(roles = "FINANCE_OFFICER")
  void UserCreation_ValidInput_ReturnsCreated() throws Exception {
    UserCreationRequestDto request = new UserCreationRequestDto(
        "newuser@veritas.com", "New User", "securePassword123",
        UserRole.ADMINISTRATOR, null, null, false);

    mockMvc.perform(post("/api/v1/users")
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
        UserRole.FINANCE_OFFICER, null, null, false);

    mockMvc.perform(post("/api/v1/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request1)))
        .andExpect(status().isCreated());

    UserCreationRequestDto request2 = new UserCreationRequestDto(
        "duplicate@veritas.com", "Second User", "securePassword456",
        UserRole.FINANCE_OFFICER, null, null, false);

    mockMvc.perform(post("/api/v1/users")
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
            get("/api/v1/users").param("search", "sterling").param("page", "0").param("size", "10"))
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
            get("/api/v1/users").param("userRole", "REQUESTER").param("page", "0").param("size", "10"))
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

    mockMvc.perform(get("/api/v1/users").param("search", "Alex").param("userRole", "FINANCE_OFFICER")
            .param("page", "0").param("size", "10")).andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(0));
  }


  @Test
  @WithMockUser(roles = "FINANCE_OFFICER")
  void UserStats_Called_ReturnsTotalAndInactiveCounts() throws Exception {
    mockMvc.perform(get("/api/v1/users/stats")).andExpect(status().isOk())
        .andExpect(jsonPath("$.total").isNumber()).andExpect(jsonPath("$.inactive").isNumber())
        .andExpect(jsonPath("$.activeSessions").isNumber());
  }

  @Test
  @WithMockUser(roles = "REQUESTER")
  void UserRetrieval_AsRequester_ReturnsForbidden() throws Exception {
    mockMvc.perform(get("/api/v1/users")).andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "REQUESTER")
  void UserStats_AsRequester_ReturnsForbidden() throws Exception {
    mockMvc.perform(get("/api/v1/users/stats")).andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "FINANCE_OFFICER")
  void UserEdit_ValidInput_ReturnsUpdatedUser() throws Exception {
    User finance = User.builder()
            .name("Test Finance")
            .email("finance@test.com")
            .team(testTeam)
            .isActive(true)
            .passwordHash(encoder.encode("password"))
            .role(UserRole.REQUESTER)
            .build();
    userRepository.save(finance);

    UserEditDto edit = new UserEditDto("new@test.com", "New Name", null, null, null, null);
    mockMvc.perform(patch("/api/v1/users/" + finance.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(edit)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("New Name"))
            .andExpect(jsonPath("$.email").value("new@test.com"));
  }

  @Test
  @WithMockUser(roles = "FINANCE_OFFICER")
  void UserEdit_DuplicateEmail_ReturnsConflict() throws Exception {
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

    UserEditDto edit = new UserEditDto("finance2@test.com", "First Finance User", null, null, null, null);
    mockMvc.perform(patch("/api/v1/users/" + user1.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(edit)))
            .andExpect(status().isConflict());
  }

  @Test
  @WithMockUser(roles = "FINANCE_OFFICER")
  void UserEdit_AsTeamLeaderChangingTeam_ReturnsBadRequest() throws Exception {
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
    newTeam.setDescription("IT Department Team");
    teamRepository.save(newTeam);

    UserEditDto edit = new UserEditDto(null, null, null, newTeam.getTeamId(), null, true);
    mockMvc.perform(patch("/api/v1/users/" + user.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(edit)))
            .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "FINANCE_OFFICER")
  void UserRetrievalById_UserExists_ReturnsUser() throws Exception {
    User user = User.builder()
            .name("Test Requester")
            .email("requester@test.com")
            .team(testTeam)
            .isActive(true)
            .passwordHash(encoder.encode("password"))
            .role(UserRole.REQUESTER)
            .build();
    userRepository.save(user);

    mockMvc.perform(get("/api/v1/users/" + user.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("requester@test.com"))
            .andExpect(jsonPath("$.name").value("Test Requester"))
            .andExpect(jsonPath("$.teamId").value(testTeam.getTeamId()))
            .andExpect(jsonPath("$.isTeamLeader").value(false));
  }

  //AI-GENERATED

  @Test
  void DeleteUser_AttemptsToDeleteSelf_ReturnsBadRequest() throws Exception {
    User admin = User.builder()
            .name("Admin Self")
            .email("adminself@test.com")
            .isActive(true)
            .passwordHash(encoder.encode("password"))
            .role(UserRole.ADMINISTRATOR)
            .build();
    userRepository.save(admin);

    mockMvc.perform(
            delete("/api/v1/users/" + admin.getId())
                    .with(user(admin))
    ).andExpect(status().isBadRequest());

    User afterOperation = userRepository.findById(admin.getId()).orElseThrow();
    assertTrue(afterOperation.getIsActive());
  }

  @Test
  void DeleteUser_DeletesOtherUser_ReturnsNoContent() throws Exception {
    User admin = User.builder()
            .name("Admin User")
            .email("adminuser@test.com")
            .isActive(true)
            .passwordHash(encoder.encode("password"))
            .role(UserRole.ADMINISTRATOR)
            .build();

    User requester = User.builder()
            .name("Requester User")
            .email("requesteruser@test.com")
            .isActive(true)
            .passwordHash(encoder.encode("password"))
            .role(UserRole.REQUESTER)
            .build();

    userRepository.saveAll(java.util.List.of(admin, requester));

    mockMvc.perform(
            delete("/api/v1/users/" + requester.getId())
                    .with(user(admin))
    ).andExpect(status().isNoContent());

    User afterOperation = userRepository.findById(requester.getId()).orElseThrow();
    assertFalse(afterOperation.getIsActive());
  }
}
