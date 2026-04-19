package com.veritas.backend.auth;

import com.veritas.backend.auth.service.JwtService;
import com.veritas.backend.user.entity.User;
import com.veritas.backend.user.entity.UserRole;
import com.veritas.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class RoleBasedAccessControlTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private com.veritas.backend.auth.repository.RefreshTokenRepository refreshTokenRepository;

        @Autowired
        private JwtService jwtService;

        private String adminToken;
        private String financeToken;
        private String procurementToken;
        private String requesterToken;

        @BeforeEach
        void setup() {
                refreshTokenRepository.deleteAll();
                userRepository.deleteAll();

                adminToken = createUserAndGetToken("admin@veritas.com", UserRole.ADMINISTRATOR);
                financeToken = createUserAndGetToken("finance@veritas.com", UserRole.FINANCE_OFFICER);
                procurementToken = createUserAndGetToken("procurement@veritas.com", UserRole.PROCUREMENT_OFFICER);
                requesterToken = createUserAndGetToken("requester@veritas.com", UserRole.REQUESTER);
        }

        private String createUserAndGetToken(String email, UserRole role) {
                User user = new User();
                user.setEmail(email);
                user.setName(role.name() + " User");
                user.setPasswordHash("hashed_password");
                user.setRole(role);
                userRepository.save(user);

                return jwtService.generateAccessToken(user);
        }

        @Test
        void adminCanAccessEverything() throws Exception {
                mockMvc.perform(get("/test-security/admin").header("Authorization", "Bearer " + adminToken))
                                .andExpect(status().isOk());
                mockMvc.perform(get("/test-security/finance").header("Authorization", "Bearer " + adminToken))
                                .andExpect(status().isOk());
                mockMvc.perform(get("/test-security/procurement").header("Authorization", "Bearer " + adminToken))
                                .andExpect(status().isOk());
                mockMvc.perform(get("/test-security/requester").header("Authorization", "Bearer " + adminToken))
                                .andExpect(status().isOk());
        }

        @Test
        void financeCanAccessFinanceAndBelow() throws Exception {
                mockMvc.perform(get("/test-security/admin").header("Authorization", "Bearer " + financeToken))
                                .andExpect(status().isForbidden()); // Higher level
                mockMvc.perform(get("/test-security/finance").header("Authorization", "Bearer " + financeToken))
                                .andExpect(status().isOk());
                mockMvc.perform(get("/test-security/procurement").header("Authorization", "Bearer " + financeToken))
                                .andExpect(status().isOk());
                mockMvc.perform(get("/test-security/requester").header("Authorization", "Bearer " + financeToken))
                                .andExpect(status().isOk());
        }

        @Test
        void procurementCanAccessProcurementAndBelow() throws Exception {
                mockMvc.perform(get("/test-security/admin").header("Authorization", "Bearer " + procurementToken))
                                .andExpect(status().isForbidden()); // Higher level
                mockMvc.perform(get("/test-security/finance").header("Authorization", "Bearer " + procurementToken))
                                .andExpect(status().isForbidden()); // Higher level
                mockMvc.perform(get("/test-security/procurement").header("Authorization", "Bearer " + procurementToken))
                                .andExpect(status().isOk());
                mockMvc.perform(get("/test-security/requester").header("Authorization", "Bearer " + procurementToken))
                                .andExpect(status().isOk());
        }

        @Test
        void requesterCanOnlyAccessRequester() throws Exception {
                mockMvc.perform(get("/test-security/admin").header("Authorization", "Bearer " + requesterToken))
                                .andExpect(status().isForbidden()); // Higher level
                mockMvc.perform(get("/test-security/finance").header("Authorization", "Bearer " + requesterToken))
                                .andExpect(status().isForbidden()); // Higher level
                mockMvc.perform(get("/test-security/procurement").header("Authorization", "Bearer " + requesterToken))
                                .andExpect(status().isForbidden()); // Higher level
                mockMvc.perform(get("/test-security/requester").header("Authorization", "Bearer " + requesterToken))
                                .andExpect(status().isOk());
        }

        @Test
        void unauthenticatedCannotAccessAnything() throws Exception {
                mockMvc.perform(get("/test-security/admin"))
                                .andExpect(status().isForbidden());
                mockMvc.perform(get("/test-security/finance"))
                                .andExpect(status().isForbidden());
                mockMvc.perform(get("/test-security/procurement"))
                                .andExpect(status().isForbidden());
                mockMvc.perform(get("/test-security/requester"))
                                .andExpect(status().isForbidden());
        }
}
