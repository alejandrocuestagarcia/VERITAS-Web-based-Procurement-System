package com.veritas.backend.integrations.jira.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.veritas.backend.BaseDBIntegrationTest;
import com.veritas.backend.integrations.jira.dto.JiraConfigDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

//AI-GENERATED

@AutoConfigureMockMvc
public class JiraConfigControllerIntegrationTest extends BaseDBIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void GetAllConfigs_ShouldReturnOk() throws Exception {
        mockMvc.perform(get("/api/v1/jira-configs"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void CreateConfig_InvalidData_ShouldReturnBadRequest() throws Exception {
        JiraConfigDto invalidDto = new JiraConfigDto(null, "", "", "", "", "", 0, "", null, null);

        mockMvc.perform(post("/api/v1/jira-configs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "REQUESTER")
    void GetAllConfigs_UserRole_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/jira-configs"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void getJiraSyncAudit_Administrator_ShouldReturnOk() throws Exception {
        mockMvc.perform(get("/api/v1/jira-configs/audit"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @WithMockUser(roles = "REQUESTER")
    void getJiraSyncAudit_RequesterRole_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/jira-configs/audit"))
                .andExpect(status().isForbidden());
    }
}
