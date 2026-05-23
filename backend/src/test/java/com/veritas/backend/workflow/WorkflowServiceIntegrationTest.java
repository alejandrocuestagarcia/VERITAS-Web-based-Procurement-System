package com.veritas.backend.workflow;

import com.veritas.backend.BaseDBIntegrationTest;
import com.veritas.backend.workflow.dto.WorkflowDto;
import com.veritas.backend.workflow.dto.WorkflowEditDto;
import com.veritas.backend.workflow.dto.WorkflowSaveDto;
import com.veritas.backend.workflow.repository.WorkflowDefinitionRepository;
import com.veritas.backend.workflow.repository.WorkflowStepRepository;
import com.veritas.backend.workflow.repository.WorkflowTransitionRepository;
import com.veritas.backend.workflow.service.WorkflowService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class WorkflowServiceIntegrationTest extends BaseDBIntegrationTest {

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private WorkflowDefinitionRepository workflowDefinitionRepository;

    @Autowired
    private WorkflowStepRepository workflowStepRepository;

    @Autowired
    private WorkflowTransitionRepository workflowTransitionRepository;

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

    private void clearDatabase() {
        jdbcTemplate.update("DELETE FROM invoices");
        jdbcTemplate.update("DELETE FROM vendor_evaluations");
        jdbcTemplate.update("DELETE FROM quote_line_items");
        jdbcTemplate.update("DELETE FROM quotes");
        jdbcTemplate.update("DELETE FROM request_items");
        jdbcTemplate.update("DELETE FROM attachments");
        jdbcTemplate.update("DELETE FROM audit_logs");
        jdbcTemplate.update("DELETE FROM requests");

        workflowTransitionRepository.deleteAll();
        workflowStepRepository.deleteAll();
        jdbcTemplate.update("UPDATE workflow_definitions SET previous_version_id = NULL");
        workflowDefinitionRepository.deleteAll();
    }

    @BeforeEach
    void setup() {
        clearDatabase();
    }

    @AfterEach
    void cleanup() {
        clearDatabase();
    }

    @Test
    void WorkflowCreation_ValidInput_ReturnsCreatedWorkflow() {
        WorkflowSaveDto saveDto = new WorkflowSaveDto(VALID_BPMN_XML, null);
        WorkflowDto result = workflowService.createWorkflow(saveDto);

        assertNotNull(result);
        assertNotNull(result.id());
        assertEquals("Test Workflow", result.name());
        assertEquals(VALID_BPMN_XML, result.bpmnXml());
        assertEquals(1, result.version());
        assertTrue(result.isActive());
    }

    @Test
    void WorkflowRetrieval_ExistingId_ReturnsWorkflow() {
        WorkflowSaveDto saveDto = new WorkflowSaveDto(VALID_BPMN_XML, null);
        WorkflowDto created = workflowService.createWorkflow(saveDto);

        WorkflowDto fetched = workflowService.getWorkflow(created.id());

        assertNotNull(fetched);
        assertEquals(created.id(), fetched.id());
        assertEquals("Test Workflow", fetched.name());
    }

    @Test
    void WorkflowRetrieval_NonExistingId_ThrowsEntityNotFoundException() {
        assertThrows(EntityNotFoundException.class, () -> workflowService.getWorkflow(99999L));
    }

    @Test
    void WorkflowCreate_BlankXmlInput_ThrowsIllegalArgumentException() {
        WorkflowSaveDto saveDto = new WorkflowSaveDto(BLANK_BPMN_XML, null);

        assertThrows(IllegalArgumentException.class, () -> workflowService.createWorkflow(saveDto));
    }

    @Test
    void WorkflowCreate_InvalidXmlInput_ThrowsIllegalArgumentException() {
        WorkflowSaveDto saveDto = new WorkflowSaveDto(INVALID_BPMN_XML, null);

        assertThrows(IllegalArgumentException.class, () -> workflowService.createWorkflow(saveDto));
    }

    @Test
    void WorkflowEdit_ValidInput_UpdatesWorkflowAndIncrementsVersion() {
        WorkflowSaveDto saveDto = new WorkflowSaveDto(VALID_BPMN_XML, null);
        WorkflowDto created = workflowService.createWorkflow(saveDto);

        String updatedBpmnXml = VALID_BPMN_XML.replace("Test Workflow", "Updated Workflow Name");
        WorkflowEditDto editDto = new WorkflowEditDto(updatedBpmnXml, null);

        WorkflowDto updated = workflowService.editWorkflow(created.id(), editDto);

        assertNotNull(updated);
        assertEquals(created.id() + 1, updated.id());
        assertEquals("Updated Workflow Name", updated.name());
        assertEquals(2, updated.version());
        assertTrue(updated.isActive());

        // The old workflow is deactivated
        WorkflowDto oldWorkflow = workflowService.getWorkflow(created.id());
        assertFalse(oldWorkflow.isActive());
    }

    @Test
    void WorkflowEdit_NonExistingId_ThrowsEntityNotFoundException() {
        WorkflowEditDto editDto = new WorkflowEditDto(VALID_BPMN_XML, null);
        assertThrows(EntityNotFoundException.class, () -> workflowService.editWorkflow(99999L, editDto));
    }
}
