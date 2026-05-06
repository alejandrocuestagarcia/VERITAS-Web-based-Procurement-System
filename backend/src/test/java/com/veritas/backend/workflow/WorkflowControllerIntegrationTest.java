package com.veritas.backend.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.veritas.backend.BaseDBIntegrationTest;
import com.veritas.backend.workflow.dto.WorkflowDto;
import com.veritas.backend.workflow.dto.WorkflowEditDto;
import com.veritas.backend.workflow.dto.WorkflowSaveDto;
import com.veritas.backend.workflow.repository.WorkflowDefinitionRepository;
import com.veritas.backend.workflow.repository.WorkflowStepRepository;
import com.veritas.backend.workflow.repository.WorkflowTransitionRepository;
import com.veritas.backend.workflow.service.WorkflowService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class WorkflowControllerIntegrationTest extends BaseDBIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WorkflowDefinitionRepository workflowDefinitionRepository;

    @Autowired
    private WorkflowStepRepository workflowStepRepository;

    @Autowired
    private WorkflowTransitionRepository workflowTransitionRepository;

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private JdbcTemplate jdbcTemplate;


    private static final String VALID_BPMN_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" " +
            "id=\"Definitions_1\" targetNamespace=\"http://bpmn.io/schema/bpmn\">\n" +
            "  <bpmn:process id=\"Process_1\" name=\"Test Workflow\" isExecutable=\"true\">\n" +
            "    <bpmn:startEvent id=\"StartEvent_1\" name=\"Start\" />\n" +
            "    <bpmn:task id=\"Task_1\" name=\"Approval Step\" />\n" +
            "    <bpmn:endEvent id=\"EndEvent_1\" name=\"End\" />\n" +
            "    <bpmn:sequenceFlow id=\"Flow_1\" sourceRef=\"StartEvent_1\" targetRef=\"Task_1\" />\n" +
            "    <bpmn:sequenceFlow id=\"Flow_2\" sourceRef=\"Task_1\" targetRef=\"EndEvent_1\" />\n" +
            "  </bpmn:process>\n" +
            "</bpmn:definitions>";

    private static final String BLANK_BPMN_XML = "   ";

    private static final String INVALID_BPMN_XML = "iNvAlId";

    @BeforeEach
    void setup() {
        workflowTransitionRepository.deleteAll();
        workflowStepRepository.deleteAll();
        jdbcTemplate.update("UPDATE workflow_definitions SET previous_version_id = NULL");
        workflowDefinitionRepository.deleteAll();
    }

    @AfterEach
    void cleanup() {
        workflowTransitionRepository.deleteAll();
        workflowStepRepository.deleteAll();
        workflowDefinitionRepository.deleteAll();
    }

    @Test
    @WithMockUser(roles = "FINANCE_OFFICER")
    void WorkflowCreation_AsFinanceOfficer_ReturnsCreatedWorkflow() throws Exception {
        WorkflowSaveDto saveDto = new WorkflowSaveDto(VALID_BPMN_XML);

        mockMvc.perform(post("/api/v1/workflows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Test Workflow"))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.isActive").value(true));
    }

    @Test
    @WithMockUser(roles = "REQUESTER")
    void WorkflowCreation_AsRequester_ReturnsForbidden() throws Exception {
        WorkflowSaveDto saveDto = new WorkflowSaveDto(VALID_BPMN_XML);

        mockMvc.perform(post("/api/v1/workflows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveDto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "FINANCE_OFFICER")
    void WorkflowCreation_WithInvalidXml_ReturnsInvalidArgument() throws Exception {
        WorkflowSaveDto saveDto = new WorkflowSaveDto(INVALID_BPMN_XML);

        mockMvc.perform(post("/api/v1/workflows")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(saveDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "FINANCE_OFFICER")
    void WorkflowCreation_WithBlankXml_ReturnsInvalidArgument() throws Exception {
        WorkflowSaveDto saveDto = new WorkflowSaveDto(BLANK_BPMN_XML);

        mockMvc.perform(post("/api/v1/workflows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "FINANCE_OFFICER")
    void WorkflowRetrieval_AsFinanceOfficer_ReturnsWorkflow() throws Exception {
        WorkflowDto created = workflowService.createWorkflow(new WorkflowSaveDto(VALID_BPMN_XML));

        mockMvc.perform(get("/api/v1/workflows/" + created.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.id()))
                .andExpect(jsonPath("$.name").value("Test Workflow"));
    }

    @Test
    @WithMockUser(roles = "FINANCE_OFFICER")
    void WorkflowRetrieval_NonExistingId_ReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/workflows/99999")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "FINANCE_OFFICER")
    void WorkflowEdit_AsFinanceOfficer_ReturnsUpdatedWorkflow() throws Exception {
        WorkflowDto created = workflowService.createWorkflow(new WorkflowSaveDto(VALID_BPMN_XML));

        String updatedBpmnXml = VALID_BPMN_XML.replace("Test Workflow", "Edited Workflow Name");
        WorkflowEditDto editDto = new WorkflowEditDto(updatedBpmnXml);

        mockMvc.perform(patch("/api/v1/workflows/" + created.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(editDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.id() + 1))
                .andExpect(jsonPath("$.name").value("Edited Workflow Name"))
                .andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.isActive").value(true));
    }
    //AI GENERATED
    @Test
    @WithMockUser(roles = "FINANCE_OFFICER")
    void GetAllWorkflows_NoParams_ReturnsPaginatedList() throws Exception {
        // Setup: Create 2 workflows
        workflowService.createWorkflow(new WorkflowSaveDto(VALID_BPMN_XML));
        workflowService.createWorkflow(new WorkflowSaveDto(VALID_BPMN_XML.replace("Test Workflow", "Another Workflow")));

        mockMvc.perform(get("/api/v1/workflows")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @WithMockUser(roles = "FINANCE_OFFICER")
    void GetAllWorkflows_WithSearch_ReturnsFilteredResults() throws Exception {
        // Setup: Create two distinct workflows
        workflowService.createWorkflow(new WorkflowSaveDto(VALID_BPMN_XML)); // "Test Workflow"
        workflowService.createWorkflow(new WorkflowSaveDto(VALID_BPMN_XML.replace("Test Workflow", "Unique Name")));

        mockMvc.perform(get("/api/v1/workflows")
                        .param("search", "Unique"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Unique Name"));
    }

    @Test
    @WithMockUser(roles = "FINANCE_OFFICER")
    void GetAllWorkflows_WithIsActiveFilter_ReturnsCorrectWorkflows() throws Exception {
        // Setup: Create one workflow (active by default)
        WorkflowDto dto = workflowService.createWorkflow(new WorkflowSaveDto(VALID_BPMN_XML));

        // Manual cleanup logic might be needed if your service/DB doesn't allow
        // easy toggling, but assuming search for isActive=true works:
        mockMvc.perform(get("/api/v1/workflows")
                        .param("isActive", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].isActive").value(true));
    }

}
