package com.veritas.backend.integrations.jira.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.veritas.backend.BaseDBIntegrationTest;
import com.veritas.backend.integrations.jira.dto.JiraConfigDto;
import com.veritas.backend.integrations.jira.repository.JiraConfigRepository;
import com.veritas.backend.integrations.jira.entity.JiraConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

//AI-GENERATED

@AutoConfigureMockMvc
public class JiraConfigControllerIntegrationTest extends BaseDBIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JiraConfigRepository repository;



    @BeforeEach
    void setUp() {
        repository.deleteAll();

        JiraConfig config1 = new JiraConfig();
        config1.setName("Production Config");
        config1.setJiraUrl("https://prod.atlassian.net");
        config1.setUsername("admin");
        config1.setApiToken("dummy-token-1");
        config1.setJql("project = PROD");
        config1.setSyncIntervalMinutes(60);
        config1.setCustomFieldId("customfield_1001");

        JiraConfig config2 = new JiraConfig();
        config2.setName("Dev Environment");
        config2.setJiraUrl("https://dev.atlassian.net");
        config2.setUsername("dev-user");
        config2.setApiToken("dummy-token-2");
        config2.setJql("project = DEV");
        config2.setSyncIntervalMinutes(30);
        config2.setCustomFieldId("customfield_1002");

        repository.saveAll(List.of(config1, config2));
    }

    @AfterEach
    void tearDown() {
        repository.deleteAll();
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void GetAllConfigs_ShouldReturnOk() throws Exception {
        mockMvc.perform(get("/api/v1/jira-configs")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void CreateConfig_InvalidData_ShouldReturnBadRequest() throws Exception {
        JiraConfigDto invalidDto = new JiraConfigDto(null, "", "", "", "", "", 0, "", null, null, null, null, null, true);

        mockMvc.perform(post("/api/v1/jira-configs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "REQUESTER")
    void GetAllConfigs_UserRole_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/jira-configs")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void GetJiraSyncAudit_Administrator_ShouldReturnOk() throws Exception {
        mockMvc.perform(get("/api/v1/jira-configs/audit"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @WithMockUser(roles = "REQUESTER")
    void GetJiraSyncAudit_RequesterRole_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/jira-configs/audit"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void getAllConfigs_AsAdmin_Returns200AndContent() throws Exception {
        mockMvc.perform(get("/api/v1/jira-configs")
                        .param("page", "0")
                        .param("size", "5")
                        .param("search", "prod"))
                .andExpect(status().isOk())
                // Check for Spring Data Page structure
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.pageable.pageSize").value(5))
                .andExpect(jsonPath("$.totalElements").exists());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void getAllConfigs_NoSearchParam_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/jira-configs")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").exists());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void DeleteConfig_AsAdmin_ShouldDeleteAndReturnNoContent() throws Exception {
        JiraConfig existing = repository.findAll().get(0);
        Long id = existing.getId();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/v1/jira-configs/" + id))
                .andExpect(status().isNoContent());

        org.junit.jupiter.api.Assertions.assertTrue(repository.findById(id).isEmpty());

        mockMvc.perform(get("/api/v1/jira-configs")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == " + id + ")]").doesNotExist());
    }
}
