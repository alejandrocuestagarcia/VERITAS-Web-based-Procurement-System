package com.veritas.backend.workflow;

import com.veritas.backend.workflow.validation.BpmnValidationException;
import com.veritas.backend.workflow.validation.BpmnValidationResult;
import com.veritas.backend.workflow.validation.BpmnValidator;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

// AI-GENERATED
class BpmnValidatorUnitTest {

        private final BpmnValidator validator = new BpmnValidator();

        // ========================================================================
        // Helper
        // ========================================================================

        private BpmnModelInstance parse(String xml) {
                return Bpmn.readModelFromStream(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        }

        // ========================================================================
        // Reusable BPMN XML snippets
        // ========================================================================

        private static final String VALID_SIMPLE_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"\n" +
                        "                  id=\"Definitions_1\" targetNamespace=\"http://bpmn.io/schema/bpmn\">\n" +
                        "  <bpmn:process id=\"Process_1\" name=\"Test Workflow\" isExecutable=\"true\">\n" +
                        "    <bpmn:documentation>A simple test workflow</bpmn:documentation>\n" +
                        "    <bpmn:startEvent id=\"StartEvent_1\" name=\"Start\" />\n" +
                        "    <bpmn:task id=\"Task_1\" name=\"Approval Step\">\n" +
                        "      <bpmn:documentation>[ASSIGNEE]FINANCE_OFFICER</bpmn:documentation>\n" +
                        "    </bpmn:task>\n" +
                        "    <bpmn:endEvent id=\"EndEvent_1\" name=\"End\" />\n" +
                        "    <bpmn:sequenceFlow id=\"Flow_1\" sourceRef=\"StartEvent_1\" targetRef=\"Task_1\" />\n" +
                        "    <bpmn:sequenceFlow id=\"Flow_2\" sourceRef=\"Task_1\" targetRef=\"EndEvent_1\" />\n" +
                        "  </bpmn:process>\n" +
                        "</bpmn:definitions>";

        private static final String VALID_XOR_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"\n" +
                        "                  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                        "                  id=\"Definitions_1\" targetNamespace=\"http://bpmn.io/schema/bpmn\">\n" +
                        "  <bpmn:process id=\"Process_1\" name=\"XOR Workflow\" isExecutable=\"true\">\n" +
                        "    <bpmn:documentation>Workflow with XOR gateway</bpmn:documentation>\n" +
                        "    <bpmn:startEvent id=\"StartEvent_1\" name=\"Start\" />\n" +
                        "    <bpmn:task id=\"Task_1\" name=\"Initial Review\">\n" +
                        "      <bpmn:documentation>[ASSIGNEE]PROCUREMENT_OFFICER</bpmn:documentation>\n" +
                        "    </bpmn:task>\n" +
                        "    <bpmn:exclusiveGateway id=\"XOR_Split\" name=\"Decision\" />\n" +
                        "    <bpmn:task id=\"Task_2\" name=\"Approve\">\n" +
                        "      <bpmn:documentation>[ASSIGNEE]FINANCE_OFFICER</bpmn:documentation>\n" +
                        "    </bpmn:task>\n" +
                        "    <bpmn:task id=\"Task_3\" name=\"Reject\">\n" +
                        "      <bpmn:documentation>[ASSIGNEE]FINANCE_OFFICER</bpmn:documentation>\n" +
                        "    </bpmn:task>\n" +
                        "    <bpmn:exclusiveGateway id=\"XOR_Merge\" name=\"Merge\" />\n" +
                        "    <bpmn:endEvent id=\"EndEvent_1\" name=\"End\" />\n" +
                        "    <bpmn:sequenceFlow id=\"Flow_1\" sourceRef=\"StartEvent_1\" targetRef=\"Task_1\" />\n" +
                        "    <bpmn:sequenceFlow id=\"Flow_2\" sourceRef=\"Task_1\" targetRef=\"XOR_Split\" />\n" +
                        "    <bpmn:sequenceFlow id=\"Flow_3\" sourceRef=\"XOR_Split\" targetRef=\"Task_2\">\n" +
                        "      <bpmn:conditionExpression xsi:type=\"bpmn:tFormalExpression\">${totalQuantity > 100}</bpmn:conditionExpression>\n"
                        +
                        "    </bpmn:sequenceFlow>\n" +
                        "    <bpmn:sequenceFlow id=\"Flow_4\" sourceRef=\"XOR_Split\" targetRef=\"Task_3\" />\n" +
                        "    <bpmn:sequenceFlow id=\"Flow_5\" sourceRef=\"Task_2\" targetRef=\"XOR_Merge\" />\n" +
                        "    <bpmn:sequenceFlow id=\"Flow_6\" sourceRef=\"Task_3\" targetRef=\"XOR_Merge\" />\n" +
                        "    <bpmn:sequenceFlow id=\"Flow_7\" sourceRef=\"XOR_Merge\" targetRef=\"EndEvent_1\" />\n" +
                        "  </bpmn:process>\n" +
                        "</bpmn:definitions>";

        // ========================================================================
        // Input validation tests
        // ========================================================================

        @Test
        void Validate_NullXml_Error() {
                BpmnModelInstance model = parse(VALID_SIMPLE_XML);

                BpmnValidationException ex = assertThrows(BpmnValidationException.class,
                                () -> validator.validate(null, model));

                assertThat(ex.getErrors()).anyMatch(e -> e.contains("BPMN XML is null"));
        }

        @Test
        void Validate_NullModel_Error() {
                BpmnValidationException ex = assertThrows(BpmnValidationException.class,
                                () -> validator.validate(VALID_SIMPLE_XML, null));

                assertThat(ex.getErrors()).anyMatch(e -> e.contains("BPMN model is null"));
        }

        // ========================================================================
        // Happy path tests
        // ========================================================================

        @Test
        void Validate_ValidSimpleWorkflow_NoErrors() {
                BpmnModelInstance model = parse(VALID_SIMPLE_XML);

                BpmnValidationResult result = validator.validate(VALID_SIMPLE_XML, model);

                assertThat(result.hasErrors()).isFalse();
        }

        @Test
        void Validate_ValidWorkflowWithXorGateway_NoErrors() {
                BpmnModelInstance model = parse(VALID_XOR_XML);

                BpmnValidationResult result = validator.validate(VALID_XOR_XML, model);

                assertThat(result.hasErrors()).isFalse();
        }

        // ========================================================================
        // Structural tests
        // ========================================================================

        @Test
        void Validate_BlankProcessName_Error() {
                String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                "<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"\n" +
                                "                  id=\"Definitions_1\" targetNamespace=\"http://bpmn.io/schema/bpmn\">\n"
                                +
                                "  <bpmn:process id=\"Process_1\" name=\"\" isExecutable=\"true\">\n" +
                                "    <bpmn:startEvent id=\"StartEvent_1\" name=\"Start\" />\n" +
                                "    <bpmn:task id=\"Task_1\" name=\"Step\" />\n" +
                                "    <bpmn:endEvent id=\"EndEvent_1\" name=\"End\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_1\" sourceRef=\"StartEvent_1\" targetRef=\"Task_1\" />\n"
                                +
                                "    <bpmn:sequenceFlow id=\"Flow_2\" sourceRef=\"Task_1\" targetRef=\"EndEvent_1\" />\n"
                                +
                                "  </bpmn:process>\n" +
                                "</bpmn:definitions>";
                BpmnModelInstance model = parse(xml);

                BpmnValidationException ex = assertThrows(BpmnValidationException.class,
                                () -> validator.validate(xml, model));

                assertThat(ex.getErrors()).anyMatch(e -> e.contains("Workflow name must not be blank"));
        }

        @Test
        void Validate_UnsupportedFlowNode_Error() {
                String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                "<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"\n" +
                                "                  id=\"Definitions_1\" targetNamespace=\"http://bpmn.io/schema/bpmn\">\n"
                                +
                                "  <bpmn:process id=\"Process_1\" name=\"Unsupported Node\" isExecutable=\"true\">\n" +
                                "    <bpmn:startEvent id=\"StartEvent_1\" name=\"Start\" />\n" +
                                "    <bpmn:task id=\"Task_1\" name=\"Step\" />\n" +
                                "    <bpmn:intermediateThrowEvent id=\"Throw_1\" name=\"Throw\" />\n" +
                                "    <bpmn:endEvent id=\"EndEvent_1\" name=\"End\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_1\" sourceRef=\"StartEvent_1\" targetRef=\"Task_1\" />\n"
                                +
                                "    <bpmn:sequenceFlow id=\"Flow_2\" sourceRef=\"Task_1\" targetRef=\"Throw_1\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_3\" sourceRef=\"Throw_1\" targetRef=\"EndEvent_1\" />\n"
                                +
                                "  </bpmn:process>\n" +
                                "</bpmn:definitions>";
                BpmnModelInstance model = parse(xml);

                BpmnValidationException ex = assertThrows(BpmnValidationException.class,
                                () -> validator.validate(xml, model));

                assertThat(ex.getErrors()).anyMatch(e -> e.contains("not supported"));
        }

        @Test
        void Validate_DuplicateSequenceFlow_Error() {
                String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                "<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"\n" +
                                "                  id=\"Definitions_1\" targetNamespace=\"http://bpmn.io/schema/bpmn\">\n"
                                +
                                "  <bpmn:process id=\"Process_1\" name=\"Duplicate Flow\" isExecutable=\"true\">\n" +
                                "    <bpmn:startEvent id=\"StartEvent_1\" name=\"Start\" />\n" +
                                "    <bpmn:task id=\"Task_1\" name=\"Step\" />\n" +
                                "    <bpmn:endEvent id=\"EndEvent_1\" name=\"End\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_1\" sourceRef=\"StartEvent_1\" targetRef=\"Task_1\" />\n"
                                +
                                "    <bpmn:sequenceFlow id=\"Flow_1_dup\" sourceRef=\"StartEvent_1\" targetRef=\"Task_1\" />\n"
                                +
                                "    <bpmn:sequenceFlow id=\"Flow_2\" sourceRef=\"Task_1\" targetRef=\"EndEvent_1\" />\n"
                                +
                                "  </bpmn:process>\n" +
                                "</bpmn:definitions>";
                BpmnModelInstance model = parse(xml);

                BpmnValidationException ex = assertThrows(BpmnValidationException.class,
                                () -> validator.validate(xml, model));

                assertThat(ex.getErrors()).anyMatch(e -> e.contains("Duplicate transition"));
        }

        @Test
        void Validate_InvalidSequenceFlowTarget_Error() {
                String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                "<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"\n" +
                                "                  id=\"Definitions_1\" targetNamespace=\"http://bpmn.io/schema/bpmn\">\n"
                                +
                                "  <bpmn:process id=\"Process_1\" name=\"Bad Flow Ref\" isExecutable=\"true\">\n" +
                                "    <bpmn:startEvent id=\"StartEvent_1\" name=\"Start\" />\n" +
                                "    <bpmn:task id=\"Task_1\" name=\"Step\" />\n" +
                                "    <bpmn:endEvent id=\"EndEvent_1\" name=\"End\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_1\" sourceRef=\"StartEvent_1\" targetRef=\"Task_1\" />\n"
                                +
                                "    <bpmn:sequenceFlow id=\"Flow_3\" sourceRef=\"Task_1\" targetRef=\"EndEvent_1\" />\n"
                                +
                                "  </bpmn:process>\n" +
                                "</bpmn:definitions>";
                BpmnModelInstance model = parse(xml);

                org.camunda.bpm.model.bpmn.instance.Process process = model.getModelElementById("Process_1");
                org.camunda.bpm.model.bpmn.instance.Task task = model.getModelElementById("Task_1");

                org.camunda.bpm.model.bpmn.instance.SequenceFlow brokenFlow = model
                                .newInstance(org.camunda.bpm.model.bpmn.instance.SequenceFlow.class);
                brokenFlow.setId("Flow_2");
                brokenFlow.setSource(task);
                process.addChildElement(brokenFlow);

                BpmnValidationException ex = assertThrows(BpmnValidationException.class,
                                () -> validator.validate(xml, model));

                assertThat(ex.getErrors()).anyMatch(e -> e.contains("non-existent target step"));
        }

        @Test
        void Validate_NoStartEvent_Error() {
                String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                "<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"\n" +
                                "                  id=\"Definitions_1\" targetNamespace=\"http://bpmn.io/schema/bpmn\">\n"
                                +
                                "  <bpmn:process id=\"Process_1\" name=\"No Start\" isExecutable=\"true\">\n" +
                                "    <bpmn:task id=\"Task_1\" name=\"Step\" />\n" +
                                "    <bpmn:endEvent id=\"EndEvent_1\" name=\"End\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_1\" sourceRef=\"Task_1\" targetRef=\"EndEvent_1\" />\n"
                                +
                                "  </bpmn:process>\n" +
                                "</bpmn:definitions>";
                BpmnModelInstance model = parse(xml);

                BpmnValidationException ex = assertThrows(BpmnValidationException.class,
                                () -> validator.validate(xml, model));

                assertThat(ex.getErrors()).anyMatch(e -> e.contains("Start Event") && e.contains("none was found"));
        }

        @Test
        void Validate_MultipleStartEvents_Error() {
                String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                "<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"\n" +
                                "                  id=\"Definitions_1\" targetNamespace=\"http://bpmn.io/schema/bpmn\">\n"
                                +
                                "  <bpmn:process id=\"Process_1\" name=\"Multi Start\" isExecutable=\"true\">\n" +
                                "    <bpmn:startEvent id=\"StartEvent_1\" name=\"Start1\" />\n" +
                                "    <bpmn:startEvent id=\"StartEvent_2\" name=\"Start2\" />\n" +
                                "    <bpmn:task id=\"Task_1\" name=\"Step\" />\n" +
                                "    <bpmn:endEvent id=\"EndEvent_1\" name=\"End\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_1\" sourceRef=\"StartEvent_1\" targetRef=\"Task_1\" />\n"
                                +
                                "    <bpmn:sequenceFlow id=\"Flow_2\" sourceRef=\"StartEvent_2\" targetRef=\"Task_1\" />\n"
                                +
                                "    <bpmn:sequenceFlow id=\"Flow_3\" sourceRef=\"Task_1\" targetRef=\"EndEvent_1\" />\n"
                                +
                                "  </bpmn:process>\n" +
                                "</bpmn:definitions>";
                BpmnModelInstance model = parse(xml);

                BpmnValidationException ex = assertThrows(BpmnValidationException.class,
                                () -> validator.validate(xml, model));

                assertThat(ex.getErrors())
                                .anyMatch(e -> e.contains("exactly one Start Event") && e.contains("2 were found"));
        }

        @Test
        void Validate_NoEndEvent_Error() {
                String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                "<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"\n" +
                                "                  id=\"Definitions_1\" targetNamespace=\"http://bpmn.io/schema/bpmn\">\n"
                                +
                                "  <bpmn:process id=\"Process_1\" name=\"No End\" isExecutable=\"true\">\n" +
                                "    <bpmn:startEvent id=\"StartEvent_1\" name=\"Start\" />\n" +
                                "    <bpmn:task id=\"Task_1\" name=\"Step\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_1\" sourceRef=\"StartEvent_1\" targetRef=\"Task_1\" />\n"
                                +
                                "  </bpmn:process>\n" +
                                "</bpmn:definitions>";
                BpmnModelInstance model = parse(xml);

                BpmnValidationException ex = assertThrows(BpmnValidationException.class,
                                () -> validator.validate(xml, model));

                assertThat(ex.getErrors()).anyMatch(e -> e.contains("End Event") && e.contains("none was found"));
        }

        @Test
        void Validate_NoTasks_Error() {
                String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                "<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"\n" +
                                "                  id=\"Definitions_1\" targetNamespace=\"http://bpmn.io/schema/bpmn\">\n"
                                +
                                "  <bpmn:process id=\"Process_1\" name=\"No Tasks\" isExecutable=\"true\">\n" +
                                "    <bpmn:startEvent id=\"StartEvent_1\" name=\"Start\" />\n" +
                                "    <bpmn:endEvent id=\"EndEvent_1\" name=\"End\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_1\" sourceRef=\"StartEvent_1\" targetRef=\"EndEvent_1\" />\n"
                                +
                                "  </bpmn:process>\n" +
                                "</bpmn:definitions>";
                BpmnModelInstance model = parse(xml);

                BpmnValidationException ex = assertThrows(BpmnValidationException.class,
                                () -> validator.validate(xml, model));

                assertThat(ex.getErrors()).anyMatch(e -> e.contains("at least one Task"));
        }

        @Test
        void Validate_StartEventNoOutgoing_Error() {
                String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                "<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"\n" +
                                "                  id=\"Definitions_1\" targetNamespace=\"http://bpmn.io/schema/bpmn\">\n"
                                +
                                "  <bpmn:process id=\"Process_1\" name=\"Disconnected Start\" isExecutable=\"true\">\n"
                                +
                                "    <bpmn:startEvent id=\"StartEvent_1\" name=\"Start\" />\n" +
                                "    <bpmn:task id=\"Task_1\" name=\"Step\" />\n" +
                                "    <bpmn:endEvent id=\"EndEvent_1\" name=\"End\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_1\" sourceRef=\"Task_1\" targetRef=\"EndEvent_1\" />\n"
                                +
                                "  </bpmn:process>\n" +
                                "</bpmn:definitions>";
                BpmnModelInstance model = parse(xml);

                BpmnValidationException ex = assertThrows(BpmnValidationException.class,
                                () -> validator.validate(xml, model));

                assertThat(ex.getErrors()).anyMatch(e -> e.contains("Start Event") && e.contains("no outgoing"));
        }

        @Test
        void Validate_StartEventWithIncoming_Error() {
                String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                "<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"\n" +
                                "                  id=\"Definitions_1\" targetNamespace=\"http://bpmn.io/schema/bpmn\">\n"
                                +
                                "  <bpmn:process id=\"Process_1\" name=\"Incoming Start\" isExecutable=\"true\">\n" +
                                "    <bpmn:startEvent id=\"StartEvent_1\" name=\"Start\" />\n" +
                                "    <bpmn:task id=\"Task_1\" name=\"Step\" />\n" +
                                "    <bpmn:endEvent id=\"EndEvent_1\" name=\"End\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_1\" sourceRef=\"StartEvent_1\" targetRef=\"Task_1\" />\n"
                                +
                                "    <bpmn:sequenceFlow id=\"Flow_2\" sourceRef=\"Task_1\" targetRef=\"EndEvent_1\" />\n"
                                +
                                "    <bpmn:sequenceFlow id=\"Flow_3\" sourceRef=\"Task_1\" targetRef=\"StartEvent_1\" />\n"
                                +
                                "  </bpmn:process>\n" +
                                "</bpmn:definitions>";
                BpmnModelInstance model = parse(xml);

                BpmnValidationException ex = assertThrows(BpmnValidationException.class,
                                () -> validator.validate(xml, model));

                assertThat(ex.getErrors()).anyMatch(e -> e.contains("Start Event must not have incoming"));
        }

        @Test
        void Validate_EndEventWithOutgoing_Error() {
                String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                "<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"\n" +
                                "                  id=\"Definitions_1\" targetNamespace=\"http://bpmn.io/schema/bpmn\">\n"
                                +
                                "  <bpmn:process id=\"Process_1\" name=\"Outgoing End\" isExecutable=\"true\">\n" +
                                "    <bpmn:startEvent id=\"StartEvent_1\" name=\"Start\" />\n" +
                                "    <bpmn:task id=\"Task_1\" name=\"Step\" />\n" +
                                "    <bpmn:endEvent id=\"EndEvent_1\" name=\"End\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_1\" sourceRef=\"StartEvent_1\" targetRef=\"Task_1\" />\n"
                                +
                                "    <bpmn:sequenceFlow id=\"Flow_2\" sourceRef=\"Task_1\" targetRef=\"EndEvent_1\" />\n"
                                +
                                "    <bpmn:sequenceFlow id=\"Flow_3\" sourceRef=\"EndEvent_1\" targetRef=\"Task_1\" />\n"
                                +
                                "  </bpmn:process>\n" +
                                "</bpmn:definitions>";
                BpmnModelInstance model = parse(xml);

                BpmnValidationException ex = assertThrows(BpmnValidationException.class,
                                () -> validator.validate(xml, model));

                assertThat(ex.getErrors())
                                .anyMatch(e -> e.contains("End Event") && e.contains("must not have outgoing"));
        }

        @Test
        void Validate_IsolatedNode_Error() {
                String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                "<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"\n" +
                                "                  id=\"Definitions_1\" targetNamespace=\"http://bpmn.io/schema/bpmn\">\n"
                                +
                                "  <bpmn:process id=\"Process_1\" name=\"Isolated\" isExecutable=\"true\">\n" +
                                "    <bpmn:startEvent id=\"StartEvent_1\" name=\"Start\" />\n" +
                                "    <bpmn:task id=\"Task_1\" name=\"Connected Step\" />\n" +
                                "    <bpmn:task id=\"Task_2\" name=\"Isolated Step\" />\n" +
                                "    <bpmn:endEvent id=\"EndEvent_1\" name=\"End\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_1\" sourceRef=\"StartEvent_1\" targetRef=\"Task_1\" />\n"
                                +
                                "    <bpmn:sequenceFlow id=\"Flow_2\" sourceRef=\"Task_1\" targetRef=\"EndEvent_1\" />\n"
                                +
                                "  </bpmn:process>\n" +
                                "</bpmn:definitions>";
                BpmnModelInstance model = parse(xml);

                BpmnValidationException ex = assertThrows(BpmnValidationException.class,
                                () -> validator.validate(xml, model));

                assertThat(ex.getErrors()).anyMatch(e -> e.contains("Isolated Step") && e.contains("isolated"));
        }

        @Test
        void Validate_CycleThroughTasks_Allowed() {
                // Cycle: Task_A → Task_B → Task_A (includes tasks, so it's a safe rework loop)
                String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                "<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"\n" +
                                "                  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                                "                  id=\"Definitions_1\" targetNamespace=\"http://bpmn.io/schema/bpmn\">\n"
                                +
                                "  <bpmn:process id=\"Process_1\" name=\"Rework Loop\" isExecutable=\"true\">\n" +
                                "    <bpmn:startEvent id=\"StartEvent_1\" name=\"Start\" />\n" +
                                "    <bpmn:task id=\"Task_A\" name=\"Submit Details\">\n" +
                                "      <bpmn:documentation>[ASSIGNEE]REQUESTER</bpmn:documentation>\n" +
                                "    </bpmn:task>\n" +
                                "    <bpmn:task id=\"Task_B\" name=\"Finance Review\">\n" +
                                "      <bpmn:documentation>[ASSIGNEE]FINANCE_OFFICER</bpmn:documentation>\n" +
                                "    </bpmn:task>\n" +
                                "    <bpmn:exclusiveGateway id=\"XOR_1\" name=\"Outcome\" />\n" +
                                "    <bpmn:endEvent id=\"EndEvent_1\" name=\"End\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_1\" sourceRef=\"StartEvent_1\" targetRef=\"Task_A\" />\n"
                                +
                                "    <bpmn:sequenceFlow id=\"Flow_2\" sourceRef=\"Task_A\" targetRef=\"Task_B\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_3\" sourceRef=\"Task_B\" targetRef=\"XOR_1\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_4\" sourceRef=\"XOR_1\" targetRef=\"EndEvent_1\">\n" +
                                "      <bpmn:conditionExpression xsi:type=\"bpmn:tFormalExpression\">${true}</bpmn:conditionExpression>\n"
                                +
                                "    </bpmn:sequenceFlow>\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_5\" sourceRef=\"XOR_1\" targetRef=\"Task_A\" />\n" +
                                "  </bpmn:process>\n" +
                                "</bpmn:definitions>";
                BpmnModelInstance model = parse(xml);
 
                // Should NOT throw — cycles through tasks are allowed
                BpmnValidationResult result = validator.validate(xml, model);
                assertThat(result.hasErrors()).isFalse();
        }

        @Test
        void Validate_GatewayOnlyCycle_Error() {
                // Cycle: XOR_A → XOR_B → XOR_A (no tasks in the loop — infinite recursion risk)
                String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                "<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"\n" +
                                "                  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                                "                  id=\"Definitions_1\" targetNamespace=\"http://bpmn.io/schema/bpmn\">\n"
                                +
                                "  <bpmn:process id=\"Process_1\" name=\"Gateway Cycle\" isExecutable=\"true\">\n" +
                                "    <bpmn:startEvent id=\"StartEvent_1\" name=\"Start\" />\n" +
                                "    <bpmn:task id=\"Task_1\" name=\"Step\" />\n" +
                                "    <bpmn:exclusiveGateway id=\"XOR_A\" name=\"Gateway A\" />\n" +
                                "    <bpmn:exclusiveGateway id=\"XOR_B\" name=\"Gateway B\" />\n" +
                                "    <bpmn:endEvent id=\"EndEvent_1\" name=\"End\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_1\" sourceRef=\"StartEvent_1\" targetRef=\"Task_1\" />\n"
                                +
                                "    <bpmn:sequenceFlow id=\"Flow_2\" sourceRef=\"Task_1\" targetRef=\"XOR_A\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_3\" sourceRef=\"XOR_A\" targetRef=\"XOR_B\">\n" +
                                "      <bpmn:conditionExpression xsi:type=\"bpmn:tFormalExpression\">${true}</bpmn:conditionExpression>\n"
                                +
                                "    </bpmn:sequenceFlow>\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_4\" sourceRef=\"XOR_B\" targetRef=\"XOR_A\">\n" +
                                "      <bpmn:conditionExpression xsi:type=\"bpmn:tFormalExpression\">${true}</bpmn:conditionExpression>\n"
                                +
                                "    </bpmn:sequenceFlow>\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_5\" sourceRef=\"XOR_A\" targetRef=\"EndEvent_1\" />\n"
                                +
                                "  </bpmn:process>\n" +
                                "</bpmn:definitions>";
                BpmnModelInstance model = parse(xml);

                BpmnValidationException ex = assertThrows(BpmnValidationException.class,
                                () -> validator.validate(xml, model));

                assertThat(ex.getErrors()).anyMatch(e -> e.contains("cycle") && e.contains("gateways or events"));
        }

        @Test
        void Validate_ProcurementReworkLoop_Allowed() {
                // Realistic procurement workflow: Start → Fill Details → Budget Check (XOR) →
                // Review → Outcome (XOR) → End/loop back
                String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                "<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"\n" +
                                "                  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                                "                  id=\"Definitions_1\" targetNamespace=\"http://bpmn.io/schema/bpmn\">\n"
                                +
                                "  <bpmn:process id=\"Process_1\" name=\"Procurement Workflow\" isExecutable=\"true\">\n"
                                +
                                "    <bpmn:startEvent id=\"StartEvent_1\" name=\"Start\" />\n" +
                                "    <bpmn:task id=\"Task_Fill\" name=\"Fill in Details\">\n" +
                                "      <bpmn:documentation>[ASSIGNEE]REQUESTER</bpmn:documentation>\n" +
                                "    </bpmn:task>\n" +
                                "    <bpmn:exclusiveGateway id=\"XOR_Budget\" name=\"Budget Check\" />\n" +
                                "    <bpmn:task id=\"Task_Review\" name=\"Finance Review\">\n" +
                                "      <bpmn:documentation>[ASSIGNEE]FINANCE_OFFICER</bpmn:documentation>\n" +
                                "    </bpmn:task>\n" +
                                "    <bpmn:exclusiveGateway id=\"XOR_Outcome\" name=\"Review Outcome\" />\n" +
                                "    <bpmn:endEvent id=\"EndEvent_1\" name=\"Approved\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_1\" sourceRef=\"StartEvent_1\" targetRef=\"Task_Fill\" />\n"
                                +
                                "    <bpmn:sequenceFlow id=\"Flow_2\" sourceRef=\"Task_Fill\" targetRef=\"XOR_Budget\" />\n"
                                +
                                "    <bpmn:sequenceFlow id=\"Flow_3\" sourceRef=\"XOR_Budget\" targetRef=\"Task_Review\">\n"
                                +
                                "      <bpmn:conditionExpression xsi:type=\"bpmn:tFormalExpression\">${totalQuantity > 0}</bpmn:conditionExpression>\n"
                                +
                                "    </bpmn:sequenceFlow>\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_4\" sourceRef=\"XOR_Budget\" targetRef=\"EndEvent_1\" />\n"
                                +
                                "    <bpmn:sequenceFlow id=\"Flow_5\" sourceRef=\"Task_Review\" targetRef=\"XOR_Outcome\" />\n"
                                +
                                "    <bpmn:sequenceFlow id=\"Flow_6\" sourceRef=\"XOR_Outcome\" targetRef=\"EndEvent_1\">\n"
                                +
                                "      <bpmn:conditionExpression xsi:type=\"bpmn:tFormalExpression\">${true}</bpmn:conditionExpression>\n"
                                +
                                "    </bpmn:sequenceFlow>\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_7\" sourceRef=\"XOR_Outcome\" targetRef=\"Task_Fill\" />\n"
                                +
                                "  </bpmn:process>\n" +
                                "</bpmn:definitions>";
                BpmnModelInstance model = parse(xml);

                // Should NOT throw — the loop goes through Task_Fill, a human step
                BpmnValidationResult result = validator.validate(xml, model);
                assertThat(result.hasErrors()).isFalse();
        }

        @Test
        void Validate_UnreachableNode_Error() {
                String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                "<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"\n" +
                                "                  id=\"Definitions_1\" targetNamespace=\"http://bpmn.io/schema/bpmn\">\n"
                                +
                                "  <bpmn:process id=\"Process_1\" name=\"Unreachable\" isExecutable=\"true\">\n" +
                                "    <bpmn:startEvent id=\"StartEvent_1\" name=\"Start\" />\n" +
                                "    <bpmn:task id=\"Task_1\" name=\"Reachable Step\" />\n" +
                                "    <bpmn:task id=\"Task_2\" name=\"Unreachable Step\" />\n" +
                                "    <bpmn:endEvent id=\"EndEvent_1\" name=\"End\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_1\" sourceRef=\"StartEvent_1\" targetRef=\"Task_1\" />\n"
                                +
                                "    <bpmn:sequenceFlow id=\"Flow_2\" sourceRef=\"Task_1\" targetRef=\"EndEvent_1\" />\n"
                                +
                                "    <bpmn:sequenceFlow id=\"Flow_3\" sourceRef=\"Task_2\" targetRef=\"EndEvent_1\" />\n"
                                +
                                "  </bpmn:process>\n" +
                                "</bpmn:definitions>";
                BpmnModelInstance model = parse(xml);

                BpmnValidationException ex = assertThrows(BpmnValidationException.class,
                                () -> validator.validate(xml, model));

                assertThat(ex.getErrors()).anyMatch(e -> e.contains("not reachable from the Start Event"));
        }

        @Test
        void Validate_DeadEndNode_Error() {
                String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                "<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"\n" +
                                "                  id=\"Definitions_1\" targetNamespace=\"http://bpmn.io/schema/bpmn\">\n"
                                +
                                "  <bpmn:process id=\"Process_1\" name=\"Dead End\" isExecutable=\"true\">\n" +
                                "    <bpmn:startEvent id=\"StartEvent_1\" name=\"Start\" />\n" +
                                "    <bpmn:task id=\"Task_1\" name=\"Normal Step\" />\n" +
                                "    <bpmn:task id=\"Task_Dead\" name=\"Dead End Step\" />\n" +
                                "    <bpmn:endEvent id=\"EndEvent_1\" name=\"End\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_1\" sourceRef=\"StartEvent_1\" targetRef=\"Task_1\" />\n"
                                +
                                "    <bpmn:sequenceFlow id=\"Flow_2\" sourceRef=\"Task_1\" targetRef=\"EndEvent_1\" />\n"
                                +
                                "    <bpmn:sequenceFlow id=\"Flow_3\" sourceRef=\"StartEvent_1\" targetRef=\"Task_Dead\" />\n"
                                +
                                "  </bpmn:process>\n" +
                                "</bpmn:definitions>";
                BpmnModelInstance model = parse(xml);

                BpmnValidationException ex = assertThrows(BpmnValidationException.class,
                                () -> validator.validate(xml, model));

                assertThat(ex.getErrors())
                                .anyMatch(e -> e.contains("no path to any End Event") || e.contains("dead end"));
        }

        // ========================================================================
        // Gateway tests
        // ========================================================================

        @Test
        void Validate_ParallelGateway_Error() {
                String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                "<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"\n" +
                                "                  id=\"Definitions_1\" targetNamespace=\"http://bpmn.io/schema/bpmn\">\n"
                                +
                                "  <bpmn:process id=\"Process_1\" name=\"Parallel\" isExecutable=\"true\">\n" +
                                "    <bpmn:startEvent id=\"StartEvent_1\" name=\"Start\" />\n" +
                                "    <bpmn:task id=\"Task_1\" name=\"Before\" />\n" +
                                "    <bpmn:parallelGateway id=\"PG_1\" name=\"Parallel\" />\n" +
                                "    <bpmn:task id=\"Task_2\" name=\"Branch A\" />\n" +
                                "    <bpmn:task id=\"Task_3\" name=\"Branch B\" />\n" +
                                "    <bpmn:parallelGateway id=\"PG_2\" name=\"Merge\" />\n" +
                                "    <bpmn:endEvent id=\"EndEvent_1\" name=\"End\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_1\" sourceRef=\"StartEvent_1\" targetRef=\"Task_1\" />\n"
                                +
                                "    <bpmn:sequenceFlow id=\"Flow_2\" sourceRef=\"Task_1\" targetRef=\"PG_1\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_3\" sourceRef=\"PG_1\" targetRef=\"Task_2\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_4\" sourceRef=\"PG_1\" targetRef=\"Task_3\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_5\" sourceRef=\"Task_2\" targetRef=\"PG_2\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_6\" sourceRef=\"Task_3\" targetRef=\"PG_2\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_7\" sourceRef=\"PG_2\" targetRef=\"EndEvent_1\" />\n"
                                +
                                "  </bpmn:process>\n" +
                                "</bpmn:definitions>";
                BpmnModelInstance model = parse(xml);

                BpmnValidationException ex = assertThrows(BpmnValidationException.class,
                                () -> validator.validate(xml, model));

                assertThat(ex.getErrors()).anyMatch(e -> e.contains("Only Exclusive (XOR) Gateways are supported"));
        }

        @Test
        void Validate_XorSplitWithoutConditions_Error() {
                String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                "<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"\n" +
                                "                  id=\"Definitions_1\" targetNamespace=\"http://bpmn.io/schema/bpmn\">\n"
                                +
                                "  <bpmn:process id=\"Process_1\" name=\"XOR No Conditions\" isExecutable=\"true\">\n" +
                                "    <bpmn:startEvent id=\"StartEvent_1\" name=\"Start\" />\n" +
                                "    <bpmn:task id=\"Task_1\" name=\"Review\" />\n" +
                                "    <bpmn:exclusiveGateway id=\"XOR_1\" name=\"Decision\" />\n" +
                                "    <bpmn:task id=\"Task_2\" name=\"Path A\" />\n" +
                                "    <bpmn:task id=\"Task_3\" name=\"Path B\" />\n" +
                                "    <bpmn:exclusiveGateway id=\"XOR_2\" name=\"Merge\" />\n" +
                                "    <bpmn:endEvent id=\"EndEvent_1\" name=\"End\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_1\" sourceRef=\"StartEvent_1\" targetRef=\"Task_1\" />\n"
                                +
                                "    <bpmn:sequenceFlow id=\"Flow_2\" sourceRef=\"Task_1\" targetRef=\"XOR_1\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_3\" sourceRef=\"XOR_1\" targetRef=\"Task_2\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_4\" sourceRef=\"XOR_1\" targetRef=\"Task_3\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_5\" sourceRef=\"Task_2\" targetRef=\"XOR_2\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_6\" sourceRef=\"Task_3\" targetRef=\"XOR_2\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_7\" sourceRef=\"XOR_2\" targetRef=\"EndEvent_1\" />\n"
                                +
                                "  </bpmn:process>\n" +
                                "</bpmn:definitions>";
                BpmnModelInstance model = parse(xml);

                BpmnValidationException ex = assertThrows(BpmnValidationException.class,
                                () -> validator.validate(xml, model));

                assertThat(ex.getErrors()).anyMatch(e -> e.contains("XOR Gateway") && e.contains("outgoing transitions without conditions"));
        }

        // AI-GENERATED
        @Test
        void Validate_XorSplitAllConditional_Error() {
                String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                "<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"\n" +
                                "                  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                                "                  id=\"Definitions_1\" targetNamespace=\"http://bpmn.io/schema/bpmn\">\n"
                                +
                                "  <bpmn:process id=\"Process_1\" name=\"XOR All Conditional\" isExecutable=\"true\">\n" +
                                "    <bpmn:startEvent id=\"StartEvent_1\" name=\"Start\" />\n" +
                                "    <bpmn:task id=\"Task_1\" name=\"Review\" />\n" +
                                "    <bpmn:exclusiveGateway id=\"XOR_1\" name=\"Decision\" />\n" +
                                "    <bpmn:task id=\"Task_2\" name=\"Path A\" />\n" +
                                "    <bpmn:task id=\"Task_3\" name=\"Path B\" />\n" +
                                "    <bpmn:exclusiveGateway id=\"XOR_2\" name=\"Merge\" />\n" +
                                "    <bpmn:endEvent id=\"EndEvent_1\" name=\"End\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_1\" sourceRef=\"StartEvent_1\" targetRef=\"Task_1\" />\n"
                                +
                                "    <bpmn:sequenceFlow id=\"Flow_2\" sourceRef=\"Task_1\" targetRef=\"XOR_1\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_3\" sourceRef=\"XOR_1\" targetRef=\"Task_2\">\n" +
                                "      <bpmn:conditionExpression xsi:type=\"bpmn:tFormalExpression\">${totalQuantity > 100}</bpmn:conditionExpression>\n" +
                                "    </bpmn:sequenceFlow>\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_4\" sourceRef=\"XOR_1\" targetRef=\"Task_3\">\n" +
                                "      <bpmn:conditionExpression xsi:type=\"bpmn:tFormalExpression\">${totalQuantity &lt;= 100}</bpmn:conditionExpression>\n" +
                                "    </bpmn:sequenceFlow>\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_5\" sourceRef=\"Task_2\" targetRef=\"XOR_2\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_6\" sourceRef=\"Task_3\" targetRef=\"XOR_2\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_7\" sourceRef=\"XOR_2\" targetRef=\"EndEvent_1\" />\n"
                                +
                                "  </bpmn:process>\n" +
                                "</bpmn:definitions>";
                BpmnModelInstance model = parse(xml);

                BpmnValidationException ex = assertThrows(BpmnValidationException.class,
                                () -> validator.validate(xml, model));

                assertThat(ex.getErrors()).anyMatch(e -> e.contains("XOR Gateway") && e.contains("has no default fallback transition"));
        }

        // ========================================================================
        // Task tests
        // ========================================================================

        @Test
        void Validate_TaskWithBlankName_Error() {
                String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                "<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"\n" +
                                "                  id=\"Definitions_1\" targetNamespace=\"http://bpmn.io/schema/bpmn\">\n"
                                +
                                "  <bpmn:process id=\"Process_1\" name=\"Blank Name\" isExecutable=\"true\">\n" +
                                "    <bpmn:startEvent id=\"StartEvent_1\" name=\"Start\" />\n" +
                                "    <bpmn:task id=\"Task_1\" name=\"\" />\n" +
                                "    <bpmn:endEvent id=\"EndEvent_1\" name=\"End\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_1\" sourceRef=\"StartEvent_1\" targetRef=\"Task_1\" />\n"
                                +
                                "    <bpmn:sequenceFlow id=\"Flow_2\" sourceRef=\"Task_1\" targetRef=\"EndEvent_1\" />\n"
                                +
                                "  </bpmn:process>\n" +
                                "</bpmn:definitions>";
                BpmnModelInstance model = parse(xml);

                BpmnValidationException ex = assertThrows(BpmnValidationException.class,
                                () -> validator.validate(xml, model));

                assertThat(ex.getErrors()).anyMatch(e -> e.contains("non-blank name"));
        }

        @Test
        void Validate_DuplicateTaskNames_Error() {
                String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                "<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"\n" +
                                "                  id=\"Definitions_1\" targetNamespace=\"http://bpmn.io/schema/bpmn\">\n"
                                +
                                "  <bpmn:process id=\"Process_1\" name=\"Dup Names\" isExecutable=\"true\">\n" +
                                "    <bpmn:documentation>Workflow with duplicate task names</bpmn:documentation>\n" +
                                "    <bpmn:startEvent id=\"StartEvent_1\" name=\"Start\" />\n" +
                                "    <bpmn:task id=\"Task_1\" name=\"Approval\" />\n" +
                                "    <bpmn:task id=\"Task_2\" name=\"Approval\" />\n" +
                                "    <bpmn:endEvent id=\"EndEvent_1\" name=\"End\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_1\" sourceRef=\"StartEvent_1\" targetRef=\"Task_1\" />\n"
                                +
                                "    <bpmn:sequenceFlow id=\"Flow_2\" sourceRef=\"Task_1\" targetRef=\"Task_2\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_3\" sourceRef=\"Task_2\" targetRef=\"EndEvent_1\" />\n"
                                +
                                "  </bpmn:process>\n" +
                                "</bpmn:definitions>";
                BpmnModelInstance model = parse(xml);

                BpmnValidationException ex = assertThrows(BpmnValidationException.class,
                                () -> validator.validate(xml, model));

                assertThat(ex.getErrors()).anyMatch(e -> e.contains("Approval") && e.contains("2 times"));
        }

        @Test
        void Validate_InvalidAssigneeRole_Error() {
                String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                "<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"\n" +
                                "                  id=\"Definitions_1\" targetNamespace=\"http://bpmn.io/schema/bpmn\">\n"
                                +
                                "  <bpmn:process id=\"Process_1\" name=\"Bad Assignee\" isExecutable=\"true\">\n" +
                                "    <bpmn:startEvent id=\"StartEvent_1\" name=\"Start\" />\n" +
                                "    <bpmn:task id=\"Task_1\" name=\"Review\">\n" +
                                "      <bpmn:documentation>[ASSIGNEE]NOT_A_ROLE</bpmn:documentation>\n" +
                                "    </bpmn:task>\n" +
                                "    <bpmn:endEvent id=\"EndEvent_1\" name=\"End\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_1\" sourceRef=\"StartEvent_1\" targetRef=\"Task_1\" />\n"
                                +
                                "    <bpmn:sequenceFlow id=\"Flow_2\" sourceRef=\"Task_1\" targetRef=\"EndEvent_1\" />\n"
                                +
                                "  </bpmn:process>\n" +
                                "</bpmn:definitions>";
                BpmnModelInstance model = parse(xml);

                BpmnValidationException ex = assertThrows(BpmnValidationException.class,
                                () -> validator.validate(xml, model));

                assertThat(ex.getErrors()).anyMatch(e -> e.contains("Invalid role assigned in BPMN"));
        }

        //AI-GENERATED

        @Test
        void Validate_MissingAssigneeRole_Error() {
                String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                "<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"\n" +
                                "                  id=\"Definitions_1\" targetNamespace=\"http://bpmn.io/schema/bpmn\">\n"
                                +
                                "  <bpmn:process id=\"Process_1\" name=\"Missing Assignee\" isExecutable=\"true\">\n" +
                                "    <bpmn:startEvent id=\"StartEvent_1\" name=\"Start\" />\n" +
                                "    <bpmn:task id=\"Task_1\" name=\"Review Step\" />\n" +
                                "    <bpmn:endEvent id=\"EndEvent_1\" name=\"End\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_1\" sourceRef=\"StartEvent_1\" targetRef=\"Task_1\" />\n"
                                +
                                "    <bpmn:sequenceFlow id=\"Flow_2\" sourceRef=\"Task_1\" targetRef=\"EndEvent_1\" />\n"
                                +
                                "  </bpmn:process>\n" +
                                "</bpmn:definitions>";
                BpmnModelInstance model = parse(xml);

                BpmnValidationException ex = assertThrows(BpmnValidationException.class,
                                () -> validator.validate(xml, model));

                assertThat(ex.getErrors()).anyMatch(e -> e.contains("must have a responsible role selected"));
        }

        @Test
        void Validate_EmptyAssigneeRole_Error() {
                String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                "<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"\n" +
                                "                  id=\"Definitions_1\" targetNamespace=\"http://bpmn.io/schema/bpmn\">\n"
                                +
                                "  <bpmn:process id=\"Process_1\" name=\"Empty Assignee\" isExecutable=\"true\">\n" +
                                "    <bpmn:startEvent id=\"StartEvent_1\" name=\"Start\" />\n" +
                                "    <bpmn:task id=\"Task_1\" name=\"Review Step\">\n" +
                                "      <bpmn:documentation>[ASSIGNEE]  </bpmn:documentation>\n" +
                                "    </bpmn:task>\n" +
                                "    <bpmn:endEvent id=\"EndEvent_1\" name=\"End\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_1\" sourceRef=\"StartEvent_1\" targetRef=\"Task_1\" />\n"
                                +
                                "    <bpmn:sequenceFlow id=\"Flow_2\" sourceRef=\"Task_1\" targetRef=\"EndEvent_1\" />\n"
                                +
                                "  </bpmn:process>\n" +
                                "</bpmn:definitions>";
                BpmnModelInstance model = parse(xml);

                BpmnValidationException ex = assertThrows(BpmnValidationException.class,
                                () -> validator.validate(xml, model));

                assertThat(ex.getErrors()).anyMatch(e -> e.contains("must have a responsible role selected"));
        }

        @Test
        void Validate_MultipleAssigneeRoles_Error() {
                String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                "<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"\n" +
                                "                  id=\"Definitions_1\" targetNamespace=\"http://bpmn.io/schema/bpmn\">\n"
                                +
                                "  <bpmn:process id=\"Process_1\" name=\"Multiple Assignees\" isExecutable=\"true\">\n" +
                                "    <bpmn:startEvent id=\"StartEvent_1\" name=\"Start\" />\n" +
                                "    <bpmn:task id=\"Task_1\" name=\"Review Step\">\n" +
                                "      <bpmn:documentation>[ASSIGNEE]FINANCE_OFFICER</bpmn:documentation>\n" +
                                "      <bpmn:documentation>[ASSIGNEE]PROCUREMENT_OFFICER</bpmn:documentation>\n" +
                                "    </bpmn:task>\n" +
                                "    <bpmn:endEvent id=\"EndEvent_1\" name=\"End\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_1\" sourceRef=\"StartEvent_1\" targetRef=\"Task_1\" />\n"
                                +
                                "    <bpmn:sequenceFlow id=\"Flow_2\" sourceRef=\"Task_1\" targetRef=\"EndEvent_1\" />\n"
                                +
                                "  </bpmn:process>\n" +
                                "</bpmn:definitions>";
                BpmnModelInstance model = parse(xml);

                BpmnValidationException ex = assertThrows(BpmnValidationException.class,
                                () -> validator.validate(xml, model));

                assertThat(ex.getErrors()).anyMatch(e -> e.contains("has multiple assignees defined"));
        }

        // ========================================================================
        // Condition expression tests
        // ========================================================================

        @Test
        void Validate_InvalidSpelSyntax_Error() {
                String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                "<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"\n" +
                                "                  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                                "                  id=\"Definitions_1\" targetNamespace=\"http://bpmn.io/schema/bpmn\">\n"
                                +
                                "  <bpmn:process id=\"Process_1\" name=\"Bad SpEL\" isExecutable=\"true\">\n" +
                                "    <bpmn:startEvent id=\"StartEvent_1\" name=\"Start\" />\n" +
                                "    <bpmn:task id=\"Task_1\" name=\"Review\" />\n" +
                                "    <bpmn:exclusiveGateway id=\"XOR_1\" name=\"Decision\" />\n" +
                                "    <bpmn:task id=\"Task_2\" name=\"Path A\" />\n" +
                                "    <bpmn:task id=\"Task_3\" name=\"Path B\" />\n" +
                                "    <bpmn:exclusiveGateway id=\"XOR_2\" name=\"Merge\" />\n" +
                                "    <bpmn:endEvent id=\"EndEvent_1\" name=\"End\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_1\" sourceRef=\"StartEvent_1\" targetRef=\"Task_1\" />\n"
                                +
                                "    <bpmn:sequenceFlow id=\"Flow_2\" sourceRef=\"Task_1\" targetRef=\"XOR_1\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_3\" sourceRef=\"XOR_1\" targetRef=\"Task_2\">\n" +
                                "      <bpmn:conditionExpression xsi:type=\"bpmn:tFormalExpression\">${>>> invalid}</bpmn:conditionExpression>\n"
                                +
                                "    </bpmn:sequenceFlow>\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_4\" sourceRef=\"XOR_1\" targetRef=\"Task_3\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_5\" sourceRef=\"Task_2\" targetRef=\"XOR_2\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_6\" sourceRef=\"Task_3\" targetRef=\"XOR_2\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_7\" sourceRef=\"XOR_2\" targetRef=\"EndEvent_1\" />\n"
                                +
                                "  </bpmn:process>\n" +
                                "</bpmn:definitions>";
                BpmnModelInstance model = parse(xml);

                BpmnValidationException ex = assertThrows(BpmnValidationException.class,
                                () -> validator.validate(xml, model));

                assertThat(ex.getErrors()).anyMatch(e -> e.contains("invalid syntax"));
        }

        @Test
        void Validate_UnsafeSpelExpression_Error() {
                String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                "<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"\n" +
                                "                  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                                "                  id=\"Definitions_1\" targetNamespace=\"http://bpmn.io/schema/bpmn\">\n"
                                +
                                "  <bpmn:process id=\"Process_1\" name=\"Unsafe SpEL\" isExecutable=\"true\">\n" +
                                "    <bpmn:startEvent id=\"StartEvent_1\" name=\"Start\" />\n" +
                                "    <bpmn:task id=\"Task_1\" name=\"Review\" />\n" +
                                "    <bpmn:exclusiveGateway id=\"XOR_1\" name=\"Decision\" />\n" +
                                "    <bpmn:task id=\"Task_2\" name=\"Path A\" />\n" +
                                "    <bpmn:task id=\"Task_3\" name=\"Path B\" />\n" +
                                "    <bpmn:exclusiveGateway id=\"XOR_2\" name=\"Merge\" />\n" +
                                "    <bpmn:endEvent id=\"EndEvent_1\" name=\"End\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_1\" sourceRef=\"StartEvent_1\" targetRef=\"Task_1\" />\n"
                                +
                                "    <bpmn:sequenceFlow id=\"Flow_2\" sourceRef=\"Task_1\" targetRef=\"XOR_1\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_3\" sourceRef=\"XOR_1\" targetRef=\"Task_2\">\n" +
                                "      <bpmn:conditionExpression xsi:type=\"bpmn:tFormalExpression\">${T(java.lang.Runtime).getRuntime().exec('rm -rf /')}</bpmn:conditionExpression>\n"
                                +
                                "    </bpmn:sequenceFlow>\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_4\" sourceRef=\"XOR_1\" targetRef=\"Task_3\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_5\" sourceRef=\"Task_2\" targetRef=\"XOR_2\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_6\" sourceRef=\"Task_3\" targetRef=\"XOR_2\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_7\" sourceRef=\"XOR_2\" targetRef=\"EndEvent_1\" />\n"
                                +
                                "  </bpmn:process>\n" +
                                "</bpmn:definitions>";
                BpmnModelInstance model = parse(xml);

                BpmnValidationException ex = assertThrows(BpmnValidationException.class,
                                () -> validator.validate(xml, model));

                assertThat(ex.getErrors()).anyMatch(e -> e.contains("unsafe"));
        }

        @Test
        void Validate_UnknownRequestProperty_Error() {
                String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                "<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"\n" +
                                "                  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                                "                  id=\"Definitions_1\" targetNamespace=\"http://bpmn.io/schema/bpmn\">\n"
                                +
                                "  <bpmn:process id=\"Process_1\" name=\"Unknown Prop\" isExecutable=\"true\">\n" +
                                "    <bpmn:documentation>Test unknown property</bpmn:documentation>\n" +
                                "    <bpmn:startEvent id=\"StartEvent_1\" name=\"Start\" />\n" +
                                "    <bpmn:task id=\"Task_1\" name=\"Review\" />\n" +
                                "    <bpmn:exclusiveGateway id=\"XOR_1\" name=\"Decision\" />\n" +
                                "    <bpmn:task id=\"Task_2\" name=\"Path A\" />\n" +
                                "    <bpmn:task id=\"Task_3\" name=\"Path B\" />\n" +
                                "    <bpmn:exclusiveGateway id=\"XOR_2\" name=\"Merge\" />\n" +
                                "    <bpmn:endEvent id=\"EndEvent_1\" name=\"End\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_1\" sourceRef=\"StartEvent_1\" targetRef=\"Task_1\" />\n"
                                +
                                "    <bpmn:sequenceFlow id=\"Flow_2\" sourceRef=\"Task_1\" targetRef=\"XOR_1\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_3\" sourceRef=\"XOR_1\" targetRef=\"Task_2\">\n" +
                                "      <bpmn:conditionExpression xsi:type=\"bpmn:tFormalExpression\">${nonExistentField > 100}</bpmn:conditionExpression>\n"
                                +
                                "    </bpmn:sequenceFlow>\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_4\" sourceRef=\"XOR_1\" targetRef=\"Task_3\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_5\" sourceRef=\"Task_2\" targetRef=\"XOR_2\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_6\" sourceRef=\"Task_3\" targetRef=\"XOR_2\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_7\" sourceRef=\"XOR_2\" targetRef=\"EndEvent_1\" />\n"
                                +
                                "  </bpmn:process>\n" +
                                "</bpmn:definitions>";
                BpmnModelInstance model = parse(xml);

                BpmnValidationException ex = assertThrows(BpmnValidationException.class,
                                () -> validator.validate(xml, model));

                assertThat(ex.getErrors()).anyMatch(
                                e -> e.contains("nonExistentField") && e.contains("not an allowed branching field"));
        }

        // AI-GENERATED
        @Test
        void Validate_AllowedBranchingPaths_Success() {
                String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                "<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"\n" +
                                "                  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                                "                  id=\"Definitions_1\" targetNamespace=\"http://bpmn.io/schema/bpmn\">\n"
                                +
                                "  <bpmn:process id=\"Process_1\" name=\"Allowed Paths\" isExecutable=\"true\">\n" +
                                "    <bpmn:startEvent id=\"StartEvent_1\" name=\"Start\" />\n" +
                                "    <bpmn:task id=\"Task_1\" name=\"Review\">\n" +
                                "      <bpmn:documentation>[ASSIGNEE]PROCUREMENT_OFFICER</bpmn:documentation>\n" +
                                "    </bpmn:task>\n" +
                                "    <bpmn:exclusiveGateway id=\"XOR_1\" name=\"Decision\" />\n" +
                                "    <bpmn:task id=\"Task_2\" name=\"Path A\">\n" +
                                "      <bpmn:documentation>[ASSIGNEE]FINANCE_OFFICER</bpmn:documentation>\n" +
                                "    </bpmn:task>\n" +
                                "    <bpmn:task id=\"Task_3\" name=\"Path B\">\n" +
                                "      <bpmn:documentation>[ASSIGNEE]FINANCE_OFFICER</bpmn:documentation>\n" +
                                "    </bpmn:task>\n" +
                                "    <bpmn:exclusiveGateway id=\"XOR_2\" name=\"Merge\" />\n" +
                                "    <bpmn:endEvent id=\"EndEvent_1\" name=\"End\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_1\" sourceRef=\"StartEvent_1\" targetRef=\"Task_1\" />\n"
                                +
                                "    <bpmn:sequenceFlow id=\"Flow_2\" sourceRef=\"Task_1\" targetRef=\"XOR_1\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_3\" sourceRef=\"XOR_1\" targetRef=\"Task_2\">\n" +
                                "      <bpmn:conditionExpression xsi:type=\"bpmn:tFormalExpression\">${selectedQuoteTotalAmount > 5000 and requester.isTeamLeader and department.name == 'IT' and budget.remainingAmount &lt; 0 and project.budget.remainingAmount > 100 and department.budget.committedSpend &lt; 500 and globalBudget.safetyBuffer == 0}</bpmn:conditionExpression>\n"
                                +
                                "    </bpmn:sequenceFlow>\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_4\" sourceRef=\"XOR_1\" targetRef=\"Task_3\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_5\" sourceRef=\"Task_2\" targetRef=\"XOR_2\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_6\" sourceRef=\"Task_3\" targetRef=\"XOR_2\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_7\" sourceRef=\"XOR_2\" targetRef=\"EndEvent_1\" />\n"
                                +
                                "  </bpmn:process>\n" +
                                "</bpmn:definitions>";
                BpmnModelInstance model = parse(xml);
                assertDoesNotThrow(() -> validator.validate(xml, model));
        }

        // AI-GENERATED
        @Test
        void Validate_DisallowedDeepPath_Error() {
                String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                "<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"\n" +
                                "                  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                                "                  id=\"Definitions_1\" targetNamespace=\"http://bpmn.io/schema/bpmn\">\n"
                                +
                                "  <bpmn:process id=\"Process_1\" name=\"Deep Traversal Prop\" isExecutable=\"true\">\n" +
                                "    <bpmn:startEvent id=\"StartEvent_1\" name=\"Start\" />\n" +
                                "    <bpmn:task id=\"Task_1\" name=\"Review\">\n" +
                                "      <bpmn:documentation>[ASSIGNEE]PROCUREMENT_OFFICER</bpmn:documentation>\n" +
                                "    </bpmn:task>\n" +
                                "    <bpmn:exclusiveGateway id=\"XOR_1\" name=\"Decision\" />\n" +
                                "    <bpmn:task id=\"Task_2\" name=\"Path A\">\n" +
                                "      <bpmn:documentation>[ASSIGNEE]FINANCE_OFFICER</bpmn:documentation>\n" +
                                "    </bpmn:task>\n" +
                                "    <bpmn:task id=\"Task_3\" name=\"Path B\">\n" +
                                "      <bpmn:documentation>[ASSIGNEE]FINANCE_OFFICER</bpmn:documentation>\n" +
                                "    </bpmn:task>\n" +
                                "    <bpmn:exclusiveGateway id=\"XOR_2\" name=\"Merge\" />\n" +
                                "    <bpmn:endEvent id=\"EndEvent_1\" name=\"End\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_1\" sourceRef=\"StartEvent_1\" targetRef=\"Task_1\" />\n"
                                +
                                "    <bpmn:sequenceFlow id=\"Flow_2\" sourceRef=\"Task_1\" targetRef=\"XOR_1\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_3\" sourceRef=\"XOR_1\" targetRef=\"Task_2\">\n" +
                                "      <bpmn:conditionExpression xsi:type=\"bpmn:tFormalExpression\">${team.department.id == 1}</bpmn:conditionExpression>\n"
                                +
                                "    </bpmn:sequenceFlow>\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_4\" sourceRef=\"XOR_1\" targetRef=\"Task_3\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_5\" sourceRef=\"Task_2\" targetRef=\"XOR_2\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_6\" sourceRef=\"Task_3\" targetRef=\"XOR_2\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_7\" sourceRef=\"XOR_2\" targetRef=\"EndEvent_1\" />\n"
                                +
                                "  </bpmn:process>\n" +
                                "</bpmn:definitions>";
                BpmnModelInstance model = parse(xml);

                BpmnValidationException ex = assertThrows(BpmnValidationException.class,
                                () -> validator.validate(xml, model));

                assertThat(ex.getErrors()).anyMatch(
                                e -> e.contains("team.department.id") && e.contains("not an allowed branching field"));
        }

        // AI-GENERATED
        @Test
        void Validate_DeprecatedAmount_Error() {
                String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                "<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"\n" +
                                "                  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                                "                  id=\"Definitions_1\" targetNamespace=\"http://bpmn.io/schema/bpmn\">\n"
                                +
                                "  <bpmn:process id=\"Process_1\" name=\"Deprecated Amount\" isExecutable=\"true\">\n" +
                                "    <bpmn:startEvent id=\"StartEvent_1\" name=\"Start\" />\n" +
                                "    <bpmn:task id=\"Task_1\" name=\"Review\">\n" +
                                "      <bpmn:documentation>[ASSIGNEE]PROCUREMENT_OFFICER</bpmn:documentation>\n" +
                                "    </bpmn:task>\n" +
                                "    <bpmn:exclusiveGateway id=\"XOR_1\" name=\"Decision\" />\n" +
                                "    <bpmn:task id=\"Task_2\" name=\"Path A\">\n" +
                                "      <bpmn:documentation>[ASSIGNEE]FINANCE_OFFICER</bpmn:documentation>\n" +
                                "    </bpmn:task>\n" +
                                "    <bpmn:task id=\"Task_3\" name=\"Path B\">\n" +
                                "      <bpmn:documentation>[ASSIGNEE]FINANCE_OFFICER</bpmn:documentation>\n" +
                                "    </bpmn:task>\n" +
                                "    <bpmn:exclusiveGateway id=\"XOR_2\" name=\"Merge\" />\n" +
                                "    <bpmn:endEvent id=\"EndEvent_1\" name=\"End\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_1\" sourceRef=\"StartEvent_1\" targetRef=\"Task_1\" />\n"
                                +
                                "    <bpmn:sequenceFlow id=\"Flow_2\" sourceRef=\"Task_1\" targetRef=\"XOR_1\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_3\" sourceRef=\"XOR_1\" targetRef=\"Task_2\">\n" +
                                "      <bpmn:conditionExpression xsi:type=\"bpmn:tFormalExpression\">${amount > 1000}</bpmn:conditionExpression>\n"
                                +
                                "    </bpmn:sequenceFlow>\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_4\" sourceRef=\"XOR_1\" targetRef=\"Task_3\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_5\" sourceRef=\"Task_2\" targetRef=\"XOR_2\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_6\" sourceRef=\"Task_3\" targetRef=\"XOR_2\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_7\" sourceRef=\"XOR_2\" targetRef=\"EndEvent_1\" />\n"
                                +
                                "  </bpmn:process>\n" +
                                "</bpmn:definitions>";
                BpmnModelInstance model = parse(xml);

                BpmnValidationException ex = assertThrows(BpmnValidationException.class,
                                () -> validator.validate(xml, model));

                assertThat(ex.getErrors()).anyMatch(
                                e -> e.contains("amount") && e.contains("not an allowed branching field"));
        }

        // AI-GENERATED
        @Test
        void Validate_RemovedUser_Error() {
                String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                "<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"\n" +
                                "                  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                                "                  id=\"Definitions_1\" targetNamespace=\"http://bpmn.io/schema/bpmn\">\n"
                                +
                                "  <bpmn:process id=\"Process_1\" name=\"Removed User\" isExecutable=\"true\">\n" +
                                "    <bpmn:startEvent id=\"StartEvent_1\" name=\"Start\" />\n" +
                                "    <bpmn:task id=\"Task_1\" name=\"Review\">\n" +
                                "      <bpmn:documentation>[ASSIGNEE]PROCUREMENT_OFFICER</bpmn:documentation>\n" +
                                "    </bpmn:task>\n" +
                                "    <bpmn:exclusiveGateway id=\"XOR_1\" name=\"Decision\" />\n" +
                                "    <bpmn:task id=\"Task_2\" name=\"Path A\">\n" +
                                "      <bpmn:documentation>[ASSIGNEE]FINANCE_OFFICER</bpmn:documentation>\n" +
                                "    </bpmn:task>\n" +
                                "    <bpmn:task id=\"Task_3\" name=\"Path B\">\n" +
                                "      <bpmn:documentation>[ASSIGNEE]FINANCE_OFFICER</bpmn:documentation>\n" +
                                "    </bpmn:task>\n" +
                                "    <bpmn:exclusiveGateway id=\"XOR_2\" name=\"Merge\" />\n" +
                                "    <bpmn:endEvent id=\"EndEvent_1\" name=\"End\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_1\" sourceRef=\"StartEvent_1\" targetRef=\"Task_1\" />\n"
                                +
                                "    <bpmn:sequenceFlow id=\"Flow_2\" sourceRef=\"Task_1\" targetRef=\"XOR_1\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_3\" sourceRef=\"XOR_1\" targetRef=\"Task_2\">\n" +
                                "      <bpmn:conditionExpression xsi:type=\"bpmn:tFormalExpression\">${user.name == 'Admin'}</bpmn:conditionExpression>\n"
                                +
                                "    </bpmn:sequenceFlow>\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_4\" sourceRef=\"XOR_1\" targetRef=\"Task_3\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_5\" sourceRef=\"Task_2\" targetRef=\"XOR_2\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_6\" sourceRef=\"Task_3\" targetRef=\"XOR_2\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_7\" sourceRef=\"XOR_2\" targetRef=\"EndEvent_1\" />\n"
                                +
                                "  </bpmn:process>\n" +
                                "</bpmn:definitions>";
                BpmnModelInstance model = parse(xml);

                BpmnValidationException ex = assertThrows(BpmnValidationException.class,
                                () -> validator.validate(xml, model));

                assertThat(ex.getErrors()).anyMatch(
                                e -> e.contains("user.name") && e.contains("not an allowed branching field"));
        }

        // AI-GENERATED
        @Test
        void Validate_NonBooleanExpression_Error() {
                String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                "<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"\n" +
                                "                  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                                "                  id=\"Definitions_1\" targetNamespace=\"http://bpmn.io/schema/bpmn\">\n"
                                +
                                "  <bpmn:process id=\"Process_1\" name=\"Non-Boolean Check\" isExecutable=\"true\">\n" +
                                "    <bpmn:startEvent id=\"StartEvent_1\" name=\"Start\" />\n" +
                                "    <bpmn:task id=\"Task_1\" name=\"Review\">\n" +
                                "      <bpmn:documentation>[ASSIGNEE]PROCUREMENT_OFFICER</bpmn:documentation>\n" +
                                "    </bpmn:task>\n" +
                                "    <bpmn:exclusiveGateway id=\"XOR_1\" name=\"Decision\" />\n" +
                                "    <bpmn:task id=\"Task_2\" name=\"Path A\">\n" +
                                "      <bpmn:documentation>[ASSIGNEE]FINANCE_OFFICER</bpmn:documentation>\n" +
                                "    </bpmn:task>\n" +
                                "    <bpmn:task id=\"Task_3\" name=\"Path B\">\n" +
                                "      <bpmn:documentation>[ASSIGNEE]FINANCE_OFFICER</bpmn:documentation>\n" +
                                "    </bpmn:task>\n" +
                                "    <bpmn:exclusiveGateway id=\"XOR_2\" name=\"Merge\" />\n" +
                                "    <bpmn:endEvent id=\"EndEvent_1\" name=\"End\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_1\" sourceRef=\"StartEvent_1\" targetRef=\"Task_1\" />\n"
                                +
                                "    <bpmn:sequenceFlow id=\"Flow_2\" sourceRef=\"Task_1\" targetRef=\"XOR_1\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_3\" sourceRef=\"XOR_1\" targetRef=\"Task_2\">\n" +
                                "      <bpmn:conditionExpression xsi:type=\"bpmn:tFormalExpression\">${department.name}</bpmn:conditionExpression>\n"
                                +
                                "    </bpmn:sequenceFlow>\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_4\" sourceRef=\"XOR_1\" targetRef=\"Task_3\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_5\" sourceRef=\"Task_2\" targetRef=\"XOR_2\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_6\" sourceRef=\"Task_3\" targetRef=\"XOR_2\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_7\" sourceRef=\"XOR_2\" targetRef=\"EndEvent_1\" />\n"
                                +
                                "  </bpmn:process>\n" +
                                "</bpmn:definitions>";
                BpmnModelInstance model = parse(xml);

                BpmnValidationException ex = assertThrows(BpmnValidationException.class,
                                () -> validator.validate(xml, model));

                assertThat(ex.getErrors()).anyMatch(
                                e -> e.contains("must evaluate to a boolean"));
        }

        // ========================================================================
        // Transition rule tests
        // ========================================================================

        @Test
        void Validate_NegativeMinVendors_Error() {
                String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                "<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"\n" +
                                "                  xmlns:veritas=\"http://veritas\"\n" +
                                "                  id=\"Definitions_1\" targetNamespace=\"http://bpmn.io/schema/bpmn\">\n"
                                +
                                "  <bpmn:process id=\"Process_1\" name=\"Negative Vendors\" isExecutable=\"true\">\n" +
                                "    <bpmn:startEvent id=\"StartEvent_1\" name=\"Start\" />\n" +
                                "    <bpmn:task id=\"Task_1\" name=\"Review A\" />\n" +
                                "    <bpmn:task id=\"Task_2\" name=\"Review B\" />\n" +
                                "    <bpmn:endEvent id=\"EndEvent_1\" name=\"End\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_1\" sourceRef=\"StartEvent_1\" targetRef=\"Task_1\" />\n"
                                +
                                "    <bpmn:sequenceFlow id=\"Flow_2\" sourceRef=\"Task_1\" targetRef=\"Task_2\">\n"
                                +
                                "      <bpmn:extensionElements>\n" +
                                "        <veritas:transitionRule minRequiredVendors=\"-1\" />\n" +
                                "      </bpmn:extensionElements>\n" +
                                "    </bpmn:sequenceFlow>\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_3\" sourceRef=\"Task_2\" targetRef=\"EndEvent_1\" />\n"
                                +
                                "  </bpmn:process>\n" +
                                "</bpmn:definitions>";
                BpmnModelInstance model = parse(xml);

                BpmnValidationException ex = assertThrows(BpmnValidationException.class,
                                () -> validator.validate(xml, model));

                assertThat(ex.getErrors()).anyMatch(e -> e.contains("negative minRequiredVendors") && e.contains("-1"));
        }

        @Test
        void Validate_NonNumericMinVendors_Error() {
                String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                "<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"\n" +
                                "                  xmlns:veritas=\"http://veritas\"\n" +
                                "                  id=\"Definitions_1\" targetNamespace=\"http://bpmn.io/schema/bpmn\">\n"
                                +
                                "  <bpmn:process id=\"Process_1\" name=\"Bad Vendors\" isExecutable=\"true\">\n" +
                                "    <bpmn:startEvent id=\"StartEvent_1\" name=\"Start\" />\n" +
                                "    <bpmn:task id=\"Task_1\" name=\"Review A\" />\n" +
                                "    <bpmn:task id=\"Task_2\" name=\"Review B\" />\n" +
                                "    <bpmn:endEvent id=\"EndEvent_1\" name=\"End\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_1\" sourceRef=\"StartEvent_1\" targetRef=\"Task_1\" />\n"
                                +
                                "    <bpmn:sequenceFlow id=\"Flow_2\" sourceRef=\"Task_1\" targetRef=\"Task_2\">\n"
                                +
                                "      <bpmn:extensionElements>\n" +
                                "        <veritas:transitionRule minRequiredVendors=\"abc\" />\n" +
                                "      </bpmn:extensionElements>\n" +
                                "    </bpmn:sequenceFlow>\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_3\" sourceRef=\"Task_2\" targetRef=\"EndEvent_1\" />\n"
                                +
                                "  </bpmn:process>\n" +
                                "</bpmn:definitions>";
                BpmnModelInstance model = parse(xml);

                BpmnValidationException ex = assertThrows(BpmnValidationException.class,
                                () -> validator.validate(xml, model));

                assertThat(ex.getErrors()).anyMatch(e -> e.contains("non-numeric minRequiredVendors"));
        }

        @Test
        void Validate_TransitionRuleOnGateway_Error() {
                String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                "<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"\n" +
                                "                  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                                "                  xmlns:veritas=\"http://veritas\"\n" +
                                "                  id=\"Definitions_1\" targetNamespace=\"http://bpmn.io/schema/bpmn\">\n"
                                +
                                "  <bpmn:process id=\"Process_1\" name=\"Gateway Rule\" isExecutable=\"true\">\n" +
                                "    <bpmn:startEvent id=\"StartEvent_1\" name=\"Start\" />\n" +
                                "    <bpmn:task id=\"Task_1\" name=\"Review\" />\n" +
                                "    <bpmn:exclusiveGateway id=\"XOR_1\" name=\"Decision\" />\n" +
                                "    <bpmn:endEvent id=\"EndEvent_1\" name=\"End A\" />\n" +
                                "    <bpmn:endEvent id=\"EndEvent_2\" name=\"End B\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_1\" sourceRef=\"StartEvent_1\" targetRef=\"Task_1\" />\n"
                                +
                                "    <bpmn:sequenceFlow id=\"Flow_2\" sourceRef=\"Task_1\" targetRef=\"XOR_1\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_3\" sourceRef=\"XOR_1\" targetRef=\"EndEvent_1\">\n" +
                                "      <bpmn:extensionElements>\n" +
                                "        <veritas:transitionRule minRequiredVendors=\"1\" />\n" +
                                "      </bpmn:extensionElements>\n" +
                                "      <bpmn:conditionExpression xsi:type=\"bpmn:tFormalExpression\">${totalQuantity > 10}</bpmn:conditionExpression>\n"
                                +
                                "    </bpmn:sequenceFlow>\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_4\" sourceRef=\"XOR_1\" targetRef=\"EndEvent_2\" />\n"
                                +
                                "  </bpmn:process>\n" +
                                "</bpmn:definitions>";
                BpmnModelInstance model = parse(xml);

                BpmnValidationException ex = assertThrows(BpmnValidationException.class,
                                () -> validator.validate(xml, model));

                assertThat(ex.getErrors()).anyMatch(e -> e.contains("Transition rules cannot be added after XOR gateways"));
        }

        @Test
        void Validate_TransitionRuleOnStartEvent_Error() {
                String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                "<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"\n" +
                                "                  xmlns:veritas=\"http://veritas\"\n" +
                                "                  id=\"Definitions_1\" targetNamespace=\"http://bpmn.io/schema/bpmn\">\n"
                                +
                                "  <bpmn:process id=\"Process_1\" name=\"Start Rule\" isExecutable=\"true\">\n" +
                                "    <bpmn:startEvent id=\"StartEvent_1\" name=\"Start\" />\n" +
                                "    <bpmn:task id=\"Task_1\" name=\"Review\" />\n" +
                                "    <bpmn:endEvent id=\"EndEvent_1\" name=\"End\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_1\" sourceRef=\"StartEvent_1\" targetRef=\"Task_1\">\n"
                                +
                                "      <bpmn:extensionElements>\n" +
                                "        <veritas:transitionRule minRequiredVendors=\"1\" />\n" +
                                "      </bpmn:extensionElements>\n" +
                                "    </bpmn:sequenceFlow>\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_2\" sourceRef=\"Task_1\" targetRef=\"EndEvent_1\" />\n"
                                +
                                "  </bpmn:process>\n" +
                                "</bpmn:definitions>";
                BpmnModelInstance model = parse(xml);

                BpmnValidationException ex = assertThrows(BpmnValidationException.class,
                                () -> validator.validate(xml, model));

                assertThat(ex.getErrors()).anyMatch(e -> e.contains("Transition rules cannot be added on the first transition leaving the Start Event"));
        }

        @Test
        void Validate_TransitionRuleOnEndEvent_Error() {
                String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                "<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"\n" +
                                "                  xmlns:veritas=\"http://veritas\"\n" +
                                "                  id=\"Definitions_1\" targetNamespace=\"http://bpmn.io/schema/bpmn\">\n"
                                +
                                "  <bpmn:process id=\"Process_1\" name=\"End Rule\" isExecutable=\"true\">\n" +
                                "    <bpmn:startEvent id=\"StartEvent_1\" name=\"Start\" />\n" +
                                "    <bpmn:task id=\"Task_1\" name=\"Review\" />\n" +
                                "    <bpmn:endEvent id=\"EndEvent_1\" name=\"End\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_1\" sourceRef=\"StartEvent_1\" targetRef=\"Task_1\" />\n"
                                +
                                "    <bpmn:sequenceFlow id=\"Flow_2\" sourceRef=\"Task_1\" targetRef=\"EndEvent_1\">\n"
                                +
                                "      <bpmn:extensionElements>\n" +
                                "        <veritas:transitionRule minRequiredVendors=\"1\" />\n" +
                                "      </bpmn:extensionElements>\n" +
                                "    </bpmn:sequenceFlow>\n" +
                                "  </bpmn:process>\n" +
                                "</bpmn:definitions>";
                BpmnModelInstance model = parse(xml);

                BpmnValidationException ex = assertThrows(BpmnValidationException.class,
                                () -> validator.validate(xml, model));

                assertThat(ex.getErrors()).anyMatch(e -> e.contains("Transition rules cannot be added on the last transition entering the End Event"));
        }

        // AI-GENERATED
        private String getXmlWithAdvancedRule(String advancedRule) {
                return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                "<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"\n" +
                                "                  xmlns:veritas=\"http://veritas\"\n" +
                                "                  id=\"Definitions_1\" targetNamespace=\"http://bpmn.io/schema/bpmn\">\n"
                                +
                                "  <bpmn:process id=\"Process_1\" name=\"Advanced Rule Test\" isExecutable=\"true\">\n" +
                                "    <bpmn:documentation>Test workflow</bpmn:documentation>\n" +
                                "    <bpmn:startEvent id=\"StartEvent_1\" name=\"Start\" />\n" +
                                "    <bpmn:task id=\"Task_1\" name=\"Submit Details\">\n" +
                                "      <bpmn:documentation>[ASSIGNEE]REQUESTER</bpmn:documentation>\n" +
                                "    </bpmn:task>\n" +
                                "    <bpmn:task id=\"Task_2\" name=\"Review\">\n" +
                                "      <bpmn:documentation>[ASSIGNEE]FINANCE_OFFICER</bpmn:documentation>\n" +
                                "    </bpmn:task>\n" +
                                "    <bpmn:endEvent id=\"EndEvent_1\" name=\"End\" />\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_1\" sourceRef=\"StartEvent_1\" targetRef=\"Task_1\" />\n"
                                +
                                "    <bpmn:sequenceFlow id=\"Flow_2\" sourceRef=\"Task_1\" targetRef=\"Task_2\">\n"
                                +
                                "      <bpmn:extensionElements>\n" +
                                "        <veritas:transitionRule advancedRule=\"" + advancedRule + "\" />\n" +
                                "      </bpmn:extensionElements>\n" +
                                "    </bpmn:sequenceFlow>\n" +
                                "    <bpmn:sequenceFlow id=\"Flow_3\" sourceRef=\"Task_2\" targetRef=\"EndEvent_1\" />\n"
                                +
                                "  </bpmn:process>\n" +
                                "</bpmn:definitions>";
        }

        // AI-GENERATED
        @Test
        void Validate_AdvancedRuleValid_NoErrors() {
                String xml = getXmlWithAdvancedRule("selectedQuoteTotalAmount &lt; 5000");
                BpmnModelInstance model = parse(xml);
                BpmnValidationResult result = validator.validate(xml, model);
                assertThat(result.hasErrors()).isFalse();
        }

        // AI-GENERATED
        @Test
        void Validate_AdvancedRuleWithUnsafeSpel_Error() {
                String xml = getXmlWithAdvancedRule("T(java.lang.Runtime).getRuntime().exec('rm -rf /')");
                BpmnModelInstance model = parse(xml);
                BpmnValidationException ex = assertThrows(BpmnValidationException.class,
                                () -> validator.validate(xml, model));
                assertThat(ex.getErrors()).anyMatch(e -> e.contains("contains potentially unsafe expressions"));
        }

        // AI-GENERATED
        @Test
        void Validate_AdvancedRuleWithInvalidSyntax_Error() {
                String xml = getXmlWithAdvancedRule("&gt;&gt;&gt; invalid");
                BpmnModelInstance model = parse(xml);
                BpmnValidationException ex = assertThrows(BpmnValidationException.class,
                                () -> validator.validate(xml, model));
                assertThat(ex.getErrors()).anyMatch(e -> e.contains("has invalid syntax"));
        }

        // AI-GENERATED
        @Test
        void Validate_AdvancedRuleWithInvalidProperty_Error() {
                String xml = getXmlWithAdvancedRule("unknownProperty == 'test'");
                BpmnModelInstance model = parse(xml);
                BpmnValidationException ex = assertThrows(BpmnValidationException.class,
                                () -> validator.validate(xml, model));
                assertThat(ex.getErrors()).anyMatch(e -> e.contains("references property 'unknownProperty' which is not an allowed branching field"));
        }

        // AI-GENERATED
        @Test
        void Validate_AdvancedRuleWithNonBooleanType_Error() {
                String xml = getXmlWithAdvancedRule("'non-boolean'");
                BpmnModelInstance model = parse(xml);
                BpmnValidationException ex = assertThrows(BpmnValidationException.class,
                                () -> validator.validate(xml, model));
                assertThat(ex.getErrors()).anyMatch(e -> e.contains("must evaluate to a boolean, but returned type: String"));
        }

        // ========================================================================
        // Size limit tests
        // ========================================================================

        @Test
        void Validate_XmlTooLarge_Error() {
                int size = 600 * 1024;
                StringBuilder xmlBuilder = new StringBuilder(size);
                for (int i = 0; i < size; i++) {
                        xmlBuilder.append('a');
                }

                String xml = xmlBuilder.toString();
                BpmnModelInstance model = parse(VALID_SIMPLE_XML);

                BpmnValidationException ex = assertThrows(BpmnValidationException.class,
                                () -> validator.validate(xml, model));

                assertThat(ex.getErrors()).anyMatch(e -> e.contains("exceeds maximum allowed size"));
        }

        @Test
        void Validate_TooManyNodes_Error() {
                // Programmatically build a BPMN with 101 tasks (+ start + end = 103 nodes)
                StringBuilder xmlBuilder = new StringBuilder();
                xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
                xmlBuilder.append("<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"\n");
                xmlBuilder.append(
                                "                  id=\"Definitions_1\" targetNamespace=\"http://bpmn.io/schema/bpmn\">\n");
                xmlBuilder.append("  <bpmn:process id=\"Process_1\" name=\"Too Many Nodes\" isExecutable=\"true\">\n");
                xmlBuilder.append("    <bpmn:startEvent id=\"StartEvent_1\" name=\"Start\" />\n");

                int taskCount = 101;
                for (int i = 1; i <= taskCount; i++) {
                        xmlBuilder.append("    <bpmn:task id=\"Task_").append(i)
                                        .append("\" name=\"Step ").append(i).append("\" />\n");
                }

                xmlBuilder.append("    <bpmn:endEvent id=\"EndEvent_1\" name=\"End\" />\n");

                // Chain: Start -> Task_1 -> Task_2 -> ... -> Task_101 -> End
                xmlBuilder.append(
                                "    <bpmn:sequenceFlow id=\"Flow_start\" sourceRef=\"StartEvent_1\" targetRef=\"Task_1\" />\n");
                for (int i = 1; i < taskCount; i++) {
                        xmlBuilder.append("    <bpmn:sequenceFlow id=\"Flow_").append(i)
                                        .append("\" sourceRef=\"Task_").append(i)
                                        .append("\" targetRef=\"Task_").append(i + 1).append("\" />\n");
                }
                xmlBuilder.append("    <bpmn:sequenceFlow id=\"Flow_end\" sourceRef=\"Task_")
                                .append(taskCount).append("\" targetRef=\"EndEvent_1\" />\n");

                xmlBuilder.append("  </bpmn:process>\n");
                xmlBuilder.append("</bpmn:definitions>");

                String xml = xmlBuilder.toString();
                BpmnModelInstance model = parse(xml);

                BpmnValidationException ex = assertThrows(BpmnValidationException.class,
                                () -> validator.validate(xml, model));

                assertThat(ex.getErrors()).anyMatch(e -> e.contains("exceeds the maximum of 100"));
        }

        // Removed Process description test
}
