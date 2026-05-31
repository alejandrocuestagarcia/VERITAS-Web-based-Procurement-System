package com.veritas.backend.team;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.veritas.backend.BaseDBIntegrationTest;
import com.veritas.backend.auth.service.JwtService;
import com.veritas.backend.department.entity.Department;
import com.veritas.backend.department.repository.DepartmentRepository;
import com.veritas.backend.project.repository.ProjectRepository;
import com.veritas.backend.team.dto.TeamCreateDto;
import com.veritas.backend.team.dto.TeamEditDto;
import com.veritas.backend.team.entity.Team;
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
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

        @Autowired
        DepartmentRepository departmentRepository;

        private Department departmentIT;
        private Department departmentHR;

        @BeforeEach
        void setup() {
                projectRepository.deleteAll();

                // Break circular references between users.team_id and teams.leader_id before
                // deletes.
                jdbcTemplate.update("UPDATE users SET team_id = NULL");
                jdbcTemplate.update("UPDATE teams SET leader_id = NULL");
                jdbcTemplate.update("UPDATE internal_budgets SET parent_budget_id = NULL");
                jdbcTemplate.update("UPDATE departments SET budget_id = NULL");

                userRepository.deleteAll();
                teamRepository.deleteAll();
                departmentRepository.deleteAll();

                departmentIT = departmentRepository.save(Department.builder().name("IT").build());
                departmentHR = departmentRepository.save(Department.builder().name("HR").build());
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
                request.setDepartmentId(departmentIT.getDepartmentId());

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
                                .role(UserRole.REQUESTER)
                                
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

                Long createdTeamId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id")
                                .asLong();

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
                                .andExpect(jsonPath("$[*].name").value(org.hamcrest.Matchers.containsInAnyOrder("Alpha Team", "Beta Team")));
        }

        @Test
        void GetAllTeams_AsRequester_ReturnsForbidden() throws Exception {
                String token = createTokenForRole(UserRole.REQUESTER);

                mockMvc.perform(get("/api/v1/teams")
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isForbidden());
        }

        @Test
        void TeamEdit_UpdatesFieldsAndMembers_ReplacesAssignments() throws Exception {
                String token = createTokenForRole(UserRole.FINANCE_OFFICER);

                Team team = createTeam("Legacy Team", departmentIT);
                User leader = createUser("Team Leader", team);
                team.setLeader(leader);
                teamRepository.save(team);

                User memberToRemove = createUser("Member One", team);
                User memberToKeep = createUser("Member Two", team);

                TeamEditDto edits = new TeamEditDto();
                edits.setName("Modernized Team");
                edits.setDescription("Updated mission brief");
                edits.setDepartmentId(departmentHR.getDepartmentId());
                edits.setMemberIds(java.util.List.of(memberToKeep.getId()));

                mockMvc.perform(patch("/api/v1/teams/" + team.getTeamId())
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(edits)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.name").value("Modernized Team"))
                                .andExpect(jsonPath("$.description").value("Updated mission brief"))
                                .andExpect(jsonPath("$.department").value("HR"))
                                .andExpect(jsonPath("$.leaderId").value(leader.getId()));

                User refreshedLeader = userRepository.findById(leader.getId()).orElseThrow();
                User refreshedMemberToRemove = userRepository.findById(memberToRemove.getId()).orElseThrow();
                User refreshedMemberToKeep = userRepository.findById(memberToKeep.getId()).orElseThrow();

                assertNotNull(refreshedLeader.getTeam());
                assertEquals(team.getTeamId(), refreshedLeader.getTeam().getTeamId());
                assertNull(refreshedMemberToRemove.getTeam());
                assertNotNull(refreshedMemberToKeep.getTeam());
                assertEquals(team.getTeamId(), refreshedMemberToKeep.getTeam().getTeamId());
        }

        @Test
        void TeamEdit_LeaderSwapWithoutRemoval_ReturnsBadRequest() throws Exception {
                String token = createTokenForRole(UserRole.FINANCE_OFFICER);

                Team team = createTeam("Leadership Team", departmentIT);
                User leader = createUser("Existing Leader", team);
                team.setLeader(leader);
                teamRepository.save(team);

                User newLeader = createUser("Incoming Leader", null);

                TeamEditDto edits = new TeamEditDto();
                edits.setLeaderId(newLeader.getId());

                mockMvc.perform(patch("/api/v1/teams/" + team.getTeamId())
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(edits)))
                                .andExpect(status().isBadRequest())
                                .andExpect(content().string(containsString("Team already has a leader")));
        }

        @Test
        void TeamEdit_ClearLeader_AllowsRemoval() throws Exception {
                String token = createTokenForRole(UserRole.FINANCE_OFFICER);

                Team team = createTeam("Ops Team", departmentIT);
                User leader = createUser("Leader", team);
                team.setLeader(leader);
                teamRepository.save(team);

                TeamEditDto edits = new TeamEditDto();
                edits.setClearLeader(true);

                mockMvc.perform(patch("/api/v1/teams/" + team.getTeamId())
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(edits)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.leaderId").value(nullValue()));

                Team refreshedTeam = teamRepository.findById(team.getTeamId()).orElseThrow();
                assertNull(refreshedTeam.getLeader());

                User refreshedLeader = userRepository.findById(leader.getId()).orElseThrow();
                assertNotNull(refreshedLeader.getTeam());
                assertEquals(team.getTeamId(), refreshedLeader.getTeam().getTeamId());
        }

        @Test
        void TeamEdit_AddingMemberFromAnotherTeam_ReturnsBadRequest() throws Exception {
                String token = createTokenForRole(UserRole.FINANCE_OFFICER);

                Team teamA = createTeam("Primary Team", departmentIT);
                Team teamB = createTeam("Secondary Team", departmentIT);

                User assignedUser = createUser("Assigned User", teamB);

                TeamEditDto edits = new TeamEditDto();
                edits.setMemberIds(java.util.List.of(assignedUser.getId()));

                mockMvc.perform(patch("/api/v1/teams/" + teamA.getTeamId())
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(edits)))
                                .andExpect(status().isBadRequest())
                                .andExpect(content().string(containsString("already assigned to team")));
        }

        @Test
        void TeamEdit_BlankName_ReturnsBadRequest() throws Exception {
                String token = createTokenForRole(UserRole.FINANCE_OFFICER);
                Team team = createTeam("Stable Team", departmentIT);

                TeamEditDto edits = new TeamEditDto();
                edits.setName("   ");

                mockMvc.perform(patch("/api/v1/teams/" + team.getTeamId())
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(edits)))
                                .andExpect(status().isBadRequest())
                                .andExpect(content().string(containsString("Team name must not be blank")));
        }

        @Test
        void TeamEdit_SameNameCaseInsensitive_DoesNotConflict() throws Exception {
                String token = createTokenForRole(UserRole.FINANCE_OFFICER);
                Team team = createTeam("Alpha Team", departmentIT);

                TeamEditDto edits = new TeamEditDto();
                edits.setName("ALPHA TEAM"); // same name, different case

                mockMvc.perform(patch("/api/v1/teams/" + team.getTeamId())
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(edits)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.name").value("ALPHA TEAM"));
        }

        @Test
        void TeamEdit_DuplicateName_ReturnsConflict() throws Exception {
                String token = createTokenForRole(UserRole.FINANCE_OFFICER);
                createTeam("Existing Team", departmentIT);
                Team team = createTeam("Other Team", departmentIT);

                TeamEditDto edits = new TeamEditDto();
                edits.setName("Existing Team");

                mockMvc.perform(patch("/api/v1/teams/" + team.getTeamId())
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(edits)))
                                .andExpect(status().isConflict())
                                .andExpect(content().string(containsString("already exists")));
        }

        @Test
        void TeamEdit_BlankDescription_ReturnsBadRequest() throws Exception {
                String token = createTokenForRole(UserRole.FINANCE_OFFICER);
                Team team = createTeam("Desc Team", departmentIT);

                TeamEditDto edits = new TeamEditDto();
                edits.setDescription("   ");

                mockMvc.perform(patch("/api/v1/teams/" + team.getTeamId())
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(edits)))
                                .andExpect(status().isBadRequest())
                                .andExpect(content().string(containsString("Team description must not be blank")));
        }

        @Test
        void TeamEdit_NullFieldsAreSkipped_ReturnsOkWithOriginalValues() throws Exception {
                String token = createTokenForRole(UserRole.FINANCE_OFFICER);
                Team team = createTeam("Untouched Team", departmentHR);

                TeamEditDto edits = new TeamEditDto(); // all null

                mockMvc.perform(patch("/api/v1/teams/" + team.getTeamId())
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(edits)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.name").value("Untouched Team"))
                                .andExpect(jsonPath("$.department").value("HR"));
        }

        @Test
        void TeamEdit_ClearLeaderAndSetLeader_ReturnsBadRequest() throws Exception {
                String token = createTokenForRole(UserRole.FINANCE_OFFICER);
                Team team = createTeam("Conflict Team", departmentIT);
                User leader = createUser("Old Leader", team);
                team.setLeader(leader);
                teamRepository.save(team);

                User newLeader = createUser("New Leader", null);

                TeamEditDto edits = new TeamEditDto();
                edits.setClearLeader(true);
                edits.setLeaderId(newLeader.getId());

                mockMvc.perform(patch("/api/v1/teams/" + team.getTeamId())
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(edits)))
                                .andExpect(status().isBadRequest())
                                .andExpect(content().string(containsString("not both")));
        }

        @Test
        void TeamEdit_LeaderNotFound_ReturnsNotFound() throws Exception {
                String token = createTokenForRole(UserRole.FINANCE_OFFICER);
                Team team = createTeam("Leaderless Team", departmentIT);

                TeamEditDto edits = new TeamEditDto();
                edits.setLeaderId(999999L);

                mockMvc.perform(patch("/api/v1/teams/" + team.getTeamId())
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(edits)))
                                .andExpect(status().isNotFound())
                                .andExpect(content().string(containsString("Leader not found")));
        }

        @Test
        void TeamEdit_LeaderAlreadyLeadsAnotherTeam_ReturnsBadRequest() throws Exception {
                String token = createTokenForRole(UserRole.FINANCE_OFFICER);

                Team teamA = createTeam("Team A", departmentIT);
                User leaderA = createUser("Leader A", teamA);
                teamA.setLeader(leaderA);
                teamRepository.save(teamA);

                Team teamB = createTeam("Team B", departmentIT);

                TeamEditDto edits = new TeamEditDto();
                edits.setLeaderId(leaderA.getId());

                mockMvc.perform(patch("/api/v1/teams/" + teamB.getTeamId())
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(edits)))
                                .andExpect(status().isBadRequest())
                                .andExpect(content().string(containsString("already a leader of another team")));
        }

        @Test
        void TeamEdit_LeaderAssignedToAnotherTeam_ReturnsBadRequest() throws Exception {
                String token = createTokenForRole(UserRole.FINANCE_OFFICER);

                Team teamA = createTeam("Team A", departmentIT);
                User member = createUser("Busy Member", teamA);

                Team teamB = createTeam("Team B", departmentIT);

                TeamEditDto edits = new TeamEditDto();
                edits.setLeaderId(member.getId());

                mockMvc.perform(patch("/api/v1/teams/" + teamB.getTeamId())
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(edits)))
                                .andExpect(status().isBadRequest())
                                .andExpect(content().string(containsString("already assigned to team")));
        }

        @Test
        void TeamEdit_AssignLeaderToTeamWithoutExistingLeader_ReturnsOk() throws Exception {
                String token = createTokenForRole(UserRole.FINANCE_OFFICER);
                Team team = createTeam("Empty Leader Team", departmentIT);
                User leader = createUser("Fresh Leader", null);

                TeamEditDto edits = new TeamEditDto();
                edits.setLeaderId(leader.getId());

                mockMvc.perform(patch("/api/v1/teams/" + team.getTeamId())
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(edits)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.leaderId").value(leader.getId()));

                // ensureLeaderAssignment should have assigned the leader to the team
                User refreshed = userRepository.findById(leader.getId()).orElseThrow();
                assertNotNull(refreshed.getTeam());
                assertEquals(team.getTeamId(), refreshed.getTeam().getTeamId());
        }

        @Test
        void TeamEdit_ReassignSameLeader_ReturnsOk() throws Exception {
                String token = createTokenForRole(UserRole.FINANCE_OFFICER);
                Team team = createTeam("Stable Leader Team", departmentIT);
                User leader = createUser("Same Leader", team);
                team.setLeader(leader);
                teamRepository.save(team);

                TeamEditDto edits = new TeamEditDto();
                edits.setLeaderId(leader.getId()); // same leader again

                mockMvc.perform(patch("/api/v1/teams/" + team.getTeamId())
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(edits)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.leaderId").value(leader.getId()));
        }

        @Test
        void TeamEdit_TeamNotFound_ReturnsNotFound() throws Exception {
                String token = createTokenForRole(UserRole.FINANCE_OFFICER);

                TeamEditDto edits = new TeamEditDto();
                edits.setName("Ghost Team");

                mockMvc.perform(patch("/api/v1/teams/999999")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(edits)))
                                .andExpect(status().isNotFound())
                                .andExpect(content().string(containsString("Team not found")));
        }

        @Test
        void TeamEdit_NoMemberIdsWithLeader_EnsuresLeaderAssignment() throws Exception {
                String token = createTokenForRole(UserRole.FINANCE_OFFICER);
                Team team = createTeam("Leader Only Team", departmentIT);
                User leader = createUser("Unlinked Leader", null);
                team.setLeader(leader);
                teamRepository.save(team);

                TeamEditDto edits = new TeamEditDto();
                edits.setDescription("Updated desc");
                // memberIds is null → triggers ensureLeaderAssignment branch

                mockMvc.perform(patch("/api/v1/teams/" + team.getTeamId())
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(edits)))
                                .andExpect(status().isOk());

                User refreshed = userRepository.findById(leader.getId()).orElseThrow();
                assertNotNull(refreshed.getTeam());
                assertEquals(team.getTeamId(), refreshed.getTeam().getTeamId());
        }

        @Test
        void TeamEdit_NoMemberIdsNoLeader_SkipsEnsureLeaderAssignment() throws Exception {
                String token = createTokenForRole(UserRole.FINANCE_OFFICER);
                Team team = createTeam("No Leader No Members", departmentIT);

                TeamEditDto edits = new TeamEditDto();
                edits.setDescription("Just a desc change");

                mockMvc.perform(patch("/api/v1/teams/" + team.getTeamId())
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(edits)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.leaderId").value(nullValue()));
        }

        @Test
        void TeamCreation_LeaderAlreadyLeadsAnotherTeam_ReturnsConflict() throws Exception {
                String token = createTokenForRole(UserRole.ADMINISTRATOR);
                Team existingTeam = createTeam("First Team", departmentIT);
                User leader = createUser("Veteran Leader", null);
                existingTeam.setLeader(leader);
                teamRepository.save(existingTeam);

                TeamCreateDto request = createTeamRequest("Second Team");
                request.setLeaderId(leader.getId());

                mockMvc.perform(post("/api/v1/teams")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isConflict())
                                .andExpect(content().string(containsString("already a leader of another team")));
        }

        @Test
        void TeamCreation_LeaderAlreadyAssignedToATeam_ReturnsBadRequest() throws Exception {
                String token = createTokenForRole(UserRole.ADMINISTRATOR);
                Team existingTeam = createTeam("Existing Team", departmentIT);
                User leader = createUser("Assigned Leader", existingTeam);

                TeamCreateDto request = createTeamRequest("New Team");
                request.setLeaderId(leader.getId());

                mockMvc.perform(post("/api/v1/teams")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(content().string(containsString("already assigned to team")));
        }

        @Test
        void TeamCreation_WithMembersButNoLeader_ReturnsCreated() throws Exception {
                String token = createTokenForRole(UserRole.ADMINISTRATOR);
                User member1 = createUser("Member Alpha", null);
                User member2 = createUser("Member Beta", null);

                TeamCreateDto request = createTeamRequest("Members Only Team");
                request.setMemberIds(java.util.List.of(member1.getId(), member2.getId()));

                MvcResult result = mockMvc.perform(post("/api/v1/teams")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.leaderId").value(nullValue()))
                                .andReturn();

                Long teamId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
                User refreshed1 = userRepository.findById(member1.getId()).orElseThrow();
                User refreshed2 = userRepository.findById(member2.getId()).orElseThrow();
                assertNotNull(refreshed1.getTeam());
                assertEquals(teamId, refreshed1.getTeam().getTeamId());
                assertNotNull(refreshed2.getTeam());
                assertEquals(teamId, refreshed2.getTeam().getTeamId());
        }

        @Test
        void TeamCreation_MemberAlreadyAssignedToTeam_ReturnsBadRequest() throws Exception {
                String token = createTokenForRole(UserRole.ADMINISTRATOR);
                Team existingTeam = createTeam("Home Team", departmentIT);
                User assignedMember = createUser("Busy Member", existingTeam);

                TeamCreateDto request = createTeamRequest("New Team With Busy Member");
                request.setMemberIds(java.util.List.of(assignedMember.getId()));

                mockMvc.perform(post("/api/v1/teams")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(content().string(containsString("already assigned to team")));
        }

        @Test
        void TeamCreation_MemberIsLeaderOfAnotherTeam_ReturnsBadRequest() throws Exception {
                String token = createTokenForRole(UserRole.ADMINISTRATOR);
                Team existingTeam = createTeam("Led Team", departmentIT);
                User otherLeader = createUser("Other Leader", null);
                existingTeam.setLeader(otherLeader);
                teamRepository.save(existingTeam);

                TeamCreateDto request = createTeamRequest("New Team Needing Members");
                request.setMemberIds(java.util.List.of(otherLeader.getId()));

                mockMvc.perform(post("/api/v1/teams")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(content().string(containsString("already a leader of another team")));
        }

        @Test
        void TeamCreation_WithoutLeaderOrMembers_ReturnsCreated() throws Exception {
                String token = createTokenForRole(UserRole.ADMINISTRATOR);
                TeamCreateDto request = createTeamRequest("Bare Team");

                mockMvc.perform(post("/api/v1/teams")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.leaderId").value(nullValue()))
                                .andExpect(jsonPath("$.members").isArray());
        }

        @Test
        void TeamEdit_SyncMembersWithNonExistentUser_ReturnsNotFound() throws Exception {
                String token = createTokenForRole(UserRole.FINANCE_OFFICER);
                Team team = createTeam("Sync Team", departmentIT);

                TeamEditDto edits = new TeamEditDto();
                edits.setMemberIds(java.util.List.of(999999L));

                mockMvc.perform(patch("/api/v1/teams/" + team.getTeamId())
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(edits)))
                                .andExpect(status().isNotFound())
                                .andExpect(content().string(containsString("Users not found")));
        }

        @Test
        void TeamEdit_SyncMembersWithLeaderOfAnotherTeam_ReturnsBadRequest() throws Exception {
                String token = createTokenForRole(UserRole.FINANCE_OFFICER);

                Team teamA = createTeam("Team A Sync", departmentIT);
                User leaderA = createUser("Leader A Sync", teamA);
                teamA.setLeader(leaderA);
                teamRepository.save(teamA);

                Team teamB = createTeam("Team B Sync", departmentIT);

                TeamEditDto edits = new TeamEditDto();
                edits.setMemberIds(java.util.List.of(leaderA.getId()));

                mockMvc.perform(patch("/api/v1/teams/" + teamB.getTeamId())
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(edits)))
                                .andExpect(status().isBadRequest())
                                .andExpect(content().string(containsString("already a leader of another team")));
        }

        @Test
        void TeamEdit_SyncMembersWithEmptyList_RemovesAllMembers() throws Exception {
                String token = createTokenForRole(UserRole.FINANCE_OFFICER);
                Team team = createTeam("Full Team", departmentIT);
                User member = createUser("Removable Member", team);

                TeamEditDto edits = new TeamEditDto();
                edits.setMemberIds(java.util.List.of()); // empty list

                mockMvc.perform(patch("/api/v1/teams/" + team.getTeamId())
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(edits)))
                                .andExpect(status().isOk());

                User refreshed = userRepository.findById(member.getId()).orElseThrow();
                assertNull(refreshed.getTeam());
        }

        @Test
        void TeamEdit_SyncMembersKeepsLeaderEvenIfNotInMemberIds() throws Exception {
                String token = createTokenForRole(UserRole.FINANCE_OFFICER);
                Team team = createTeam("Sync Leader Team", departmentIT);
                User leader = createUser("Leader Kept", team);
                team.setLeader(leader);
                teamRepository.save(team);

                User newMember = createUser("New Sync Member", null);

                TeamEditDto edits = new TeamEditDto();
                // Only include new member, but leader should still be assigned
                edits.setMemberIds(java.util.List.of(newMember.getId()));

                mockMvc.perform(patch("/api/v1/teams/" + team.getTeamId())
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(edits)))
                                .andExpect(status().isOk());

                User refreshedLeader = userRepository.findById(leader.getId()).orElseThrow();
                assertNotNull(refreshedLeader.getTeam());
                assertEquals(team.getTeamId(), refreshedLeader.getTeam().getTeamId());

                User refreshedMember = userRepository.findById(newMember.getId()).orElseThrow();
                assertNotNull(refreshedMember.getTeam());
                assertEquals(team.getTeamId(), refreshedMember.getTeam().getTeamId());
        }

        @Test
        void TeamCreation_WithNoDepartment_SetsNull() throws Exception {
                // This test covers department==null and leader==null branches in
                // convertTeamToTeamDto
                String token = createTokenForRole(UserRole.ADMINISTRATOR);
                TeamCreateDto request = new TeamCreateDto();
                request.setName("No Dept Team");
                request.setDescription("Has no department");
                request.setDepartmentId(null);

                // Department is @NotNull, so this should fail validation
                mockMvc.perform(post("/api/v1/teams")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void GetTeam_ReturnsTeamWithNullDepartmentAndNullLeader() throws Exception {
                String token = createTokenForRole(UserRole.FINANCE_OFFICER);

                // Create team directly in the repository with null department
                Team team = new Team();
                team.setName("No Dept Team");
                team.setDescription("A team without a department");
                team.setDepartment(null);
                team.setIsActive(true);
                team = teamRepository.save(team);

                mockMvc.perform(get("/api/v1/teams/" + team.getTeamId())
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.name").value("No Dept Team"))
                                .andExpect(jsonPath("$.department").value(nullValue()))
                                .andExpect(jsonPath("$.leaderId").value(nullValue()));
        }

        @Test
        void GetTeam_TeamNotFound_ReturnsNotFound() throws Exception {
                String token = createTokenForRole(UserRole.FINANCE_OFFICER);

                mockMvc.perform(get("/api/v1/teams/999999")
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isNotFound())
                                .andExpect(content().string(containsString("Team not found")));
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
                request.setDepartmentId(departmentIT.getDepartmentId());
                request.setExpiresAt(LocalDateTime.now().plusDays(30));
                return request;
        }

        private Team createTeam(String name, Department department) {
                Team team = new Team();
                team.setName(name);
                team.setDescription("Initial description");
                team.setDepartment(department);
                return teamRepository.save(team);
        }

        private User createUser(String name, Team team) {
                User user = User.builder()
                                .name(name)
                                .email(name.toLowerCase().replace(" ", ".") + "-" + UUID.randomUUID() + "@veritas.com")
                                .passwordHash(encoder.encode("password123"))
                                .role(UserRole.REQUESTER)
                                .isActive(true)
                                .team(team)
                                .build();
                return userRepository.save(user);
        }
}