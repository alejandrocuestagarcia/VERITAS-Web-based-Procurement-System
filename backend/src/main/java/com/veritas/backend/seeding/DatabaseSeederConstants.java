package com.veritas.backend.seeding;

public class DatabaseSeederConstants {
    public static final String STANDARD_WORKFLOW =
"""
<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI" xmlns:dc="http://www.omg.org/spec/DD/20100524/DC" xmlns:di="http://www.omg.org/spec/DD/20100524/DI" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:veritas="http://veritas" id="Definitions_1" targetNamespace="http://bpmn.io/schema/bpmn">
  <bpmn:process id="PR_Tiered_Approval_001" name="Standard Procurement Workflow" isExecutable="false">
    <bpmn:documentation></bpmn:documentation>
    <bpmn:startEvent id="Event_08nghaf" name="Submit Procurement">
      <bpmn:outgoing>Flow_1fuqcx7</bpmn:outgoing>
    </bpmn:startEvent>
    <bpmn:task id="Activity_05zsl9d" name="Add reasoning for Purchase">
      <bpmn:documentation>[ASSIGNEE]REQUESTER</bpmn:documentation>
      <bpmn:documentation>Add filled and signed template PDF of Purchase Reasoning. The corresponding template is available in the company wiki.</bpmn:documentation>
      <bpmn:incoming>Flow_1fuqcx7</bpmn:incoming>
      <bpmn:outgoing>Flow_1pkf6u1</bpmn:outgoing>
    </bpmn:task>
    <bpmn:task id="Activity_0wyenbf" name="Add &#38; Select Vendor Quotes">
      <bpmn:documentation>[ASSIGNEE]PROCUREMENT_OFFICER</bpmn:documentation>
      <bpmn:documentation>Enter 3 different vendors with correct data. Choose the best vendor offer based on our Selection Guidelines.</bpmn:documentation>
      <bpmn:incoming>Flow_1pkf6u1</bpmn:incoming>
      <bpmn:outgoing>Flow_040ezjb</bpmn:outgoing>
    </bpmn:task>
    <bpmn:task id="Activity_0sel549" name="Standard Finance Check">
      <bpmn:documentation>[ASSIGNEE]FINANCE_OFFICER</bpmn:documentation>
      <bpmn:documentation>Check validity of request and inspect if chosen vendor offer is plausible.</bpmn:documentation>
      <bpmn:incoming>Flow_040ezjb</bpmn:incoming>
      <bpmn:outgoing>Flow_0b0h3fp</bpmn:outgoing>
    </bpmn:task>
    <bpmn:exclusiveGateway id="Gateway_1h5ftka" name="Budget Check">
      <bpmn:incoming>Flow_0b0h3fp</bpmn:incoming>
      <bpmn:outgoing>Flow_1mwl7n2</bpmn:outgoing>
      <bpmn:outgoing>Flow_0qseui0</bpmn:outgoing>
      <bpmn:outgoing>Flow_1nasara</bpmn:outgoing>
    </bpmn:exclusiveGateway>
    <bpmn:task id="Activity_05z59cy" name="Automatic Approval">
      <bpmn:documentation>[ASSIGNEE]FINANCE_OFFICER</bpmn:documentation>
      <bpmn:documentation>Automated approval.</bpmn:documentation>
      <bpmn:incoming>Flow_0qseui0</bpmn:incoming>
      <bpmn:outgoing>Flow_06jwacm</bpmn:outgoing>
    </bpmn:task>
    <bpmn:task id="Activity_1u5crin" name="Standard Finance Review">
      <bpmn:documentation>[ASSIGNEE]FINANCE_OFFICER</bpmn:documentation>
      <bpmn:documentation>Final finance review to check if purchase is valid.</bpmn:documentation>
      <bpmn:incoming>Flow_1mwl7n2</bpmn:incoming>
      <bpmn:outgoing>Flow_0u6qlvy</bpmn:outgoing>
    </bpmn:task>
    <bpmn:task id="Activity_0nk1tm3" name="Executive Requistion Report">
      <bpmn:documentation>[ASSIGNEE]PROCUREMENT_OFFICER</bpmn:documentation>
      <bpmn:documentation>Since the vendor offer exceeds 2000, please provide an Executive Requisition Report. The corresponding template is available in the company wiki.</bpmn:documentation>
      <bpmn:incoming>Flow_1nasara</bpmn:incoming>
      <bpmn:outgoing>Flow_0boq7g8</bpmn:outgoing>
    </bpmn:task>
    <bpmn:task id="Activity_17y825l" name="Executive Finance Review">
      <bpmn:documentation>[ASSIGNEE]FINANCE_OFFICER</bpmn:documentation>
      <bpmn:documentation>Consult finance office board members when making purchase decision.</bpmn:documentation>
      <bpmn:incoming>Flow_0boq7g8</bpmn:incoming>
      <bpmn:outgoing>Flow_0pmcah2</bpmn:outgoing>
    </bpmn:task>
    <bpmn:endEvent id="Event_0kd5jps">
      <bpmn:incoming>Flow_0u6qlvy</bpmn:incoming>
      <bpmn:incoming>Flow_06jwacm</bpmn:incoming>
      <bpmn:incoming>Flow_0pmcah2</bpmn:incoming>
    </bpmn:endEvent>
    <bpmn:sequenceFlow id="Flow_0u6qlvy" sourceRef="Activity_1u5crin" targetRef="Event_0kd5jps" />
    <bpmn:sequenceFlow id="Flow_06jwacm" sourceRef="Activity_05z59cy" targetRef="Event_0kd5jps" />
    <bpmn:sequenceFlow id="Flow_0boq7g8" sourceRef="Activity_0nk1tm3" targetRef="Activity_17y825l">
      <bpmn:extensionElements>
        <veritas:transitionRule type="veritas:transitionRule" isPdfRequired="true" />
      </bpmn:extensionElements>
    </bpmn:sequenceFlow>
    <bpmn:sequenceFlow id="Flow_1mwl7n2" sourceRef="Gateway_1h5ftka" targetRef="Activity_1u5crin">
      <bpmn:conditionExpression xsi:type="bpmn:tFormalExpression">${selectedQuoteTotalAmount &gt;= 500 and selectedQuoteTotalAmount &lt; 2000}</bpmn:conditionExpression>
    </bpmn:sequenceFlow>
    <bpmn:sequenceFlow id="Flow_0qseui0" sourceRef="Gateway_1h5ftka" targetRef="Activity_05z59cy">
      <bpmn:conditionExpression xsi:type="bpmn:tFormalExpression">${selectedQuoteTotalAmount &lt; 500}</bpmn:conditionExpression>
    </bpmn:sequenceFlow>
    <bpmn:sequenceFlow id="Flow_1nasara" sourceRef="Gateway_1h5ftka" targetRef="Activity_0nk1tm3">
      <bpmn:conditionExpression xsi:type="bpmn:tFormalExpression">${selectedQuoteTotalAmount &gt;= 2000}</bpmn:conditionExpression>
    </bpmn:sequenceFlow>
    <bpmn:sequenceFlow id="Flow_0b0h3fp" sourceRef="Activity_0sel549" targetRef="Gateway_1h5ftka" />
    <bpmn:sequenceFlow id="Flow_040ezjb" sourceRef="Activity_0wyenbf" targetRef="Activity_0sel549">
      <bpmn:extensionElements>
        <veritas:transitionRule type="veritas:transitionRule" minRequiredVendors="3" />
      </bpmn:extensionElements>
    </bpmn:sequenceFlow>
    <bpmn:sequenceFlow id="Flow_1pkf6u1" sourceRef="Activity_05zsl9d" targetRef="Activity_0wyenbf">
      <bpmn:documentation />
      <bpmn:extensionElements>
        <veritas:transitionRule type="veritas:transitionRule" isPdfRequired="true" />
      </bpmn:extensionElements>
    </bpmn:sequenceFlow>
    <bpmn:sequenceFlow id="Flow_1fuqcx7" sourceRef="Event_08nghaf" targetRef="Activity_05zsl9d" />
    <bpmn:sequenceFlow id="Flow_0pmcah2" sourceRef="Activity_17y825l" targetRef="Event_0kd5jps" />
  </bpmn:process>
  <bpmndi:BPMNDiagram id="BPMNDiagram_1">
    <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="PR_Tiered_Approval_001">
      <bpmndi:BPMNShape id="Event_08nghaf_di" bpmnElement="Event_08nghaf">
        <dc:Bounds x="-148" y="82" width="36" height="36" />
        <bpmndi:BPMNLabel>
          <dc:Bounds x="-161" y="125" width="63" height="27" />
        </bpmndi:BPMNLabel>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="Activity_05zsl9d_di" bpmnElement="Activity_05zsl9d">
        <dc:Bounds x="-10" y="60" width="100" height="80" />
        <bpmndi:BPMNLabel />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="Activity_0wyenbf_di" bpmnElement="Activity_0wyenbf">
        <dc:Bounds x="190" y="60" width="100" height="80" />
        <bpmndi:BPMNLabel />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="Activity_0sel549_di" bpmnElement="Activity_0sel549">
        <dc:Bounds x="390" y="60" width="100" height="80" />
        <bpmndi:BPMNLabel />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="Gateway_1h5ftka_di" bpmnElement="Gateway_1h5ftka" isMarkerVisible="true">
        <dc:Bounds x="595" y="75" width="50" height="50" />
        <bpmndi:BPMNLabel>
          <dc:Bounds x="585" y="132" width="70" height="14" />
        </bpmndi:BPMNLabel>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="Activity_05z59cy_di" bpmnElement="Activity_05z59cy">
        <dc:Bounds x="720" y="-90" width="100" height="80" />
        <bpmndi:BPMNLabel />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="Activity_1u5crin_di" bpmnElement="Activity_1u5crin">
        <dc:Bounds x="720" y="60" width="100" height="80" />
        <bpmndi:BPMNLabel />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="Activity_0nk1tm3_di" bpmnElement="Activity_0nk1tm3">
        <dc:Bounds x="720" y="210" width="100" height="80" />
        <bpmndi:BPMNLabel />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="Activity_17y825l_di" bpmnElement="Activity_17y825l">
        <dc:Bounds x="926" y="210" width="100" height="80" />
        <bpmndi:BPMNLabel />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="Event_0kd5jps_di" bpmnElement="Event_0kd5jps">
        <dc:Bounds x="1182" y="82" width="36" height="36" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNEdge id="Flow_0u6qlvy_di" bpmnElement="Flow_0u6qlvy">
        <di:waypoint x="820" y="100" />
        <di:waypoint x="1182" y="100" />
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Flow_06jwacm_di" bpmnElement="Flow_06jwacm">
        <di:waypoint x="820" y="-50" />
        <di:waypoint x="960" y="-50" />
        <di:waypoint x="960" y="100" />
        <di:waypoint x="1182" y="100" />
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Flow_0boq7g8_di" bpmnElement="Flow_0boq7g8">
        <di:waypoint x="820" y="250" />
        <di:waypoint x="926" y="250" />
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Flow_1mwl7n2_di" bpmnElement="Flow_1mwl7n2">
        <di:waypoint x="645" y="100" />
        <di:waypoint x="720" y="100" />
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Flow_0qseui0_di" bpmnElement="Flow_0qseui0">
        <di:waypoint x="620" y="75" />
        <di:waypoint x="620" y="-50" />
        <di:waypoint x="720" y="-50" />
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Flow_1nasara_di" bpmnElement="Flow_1nasara">
        <di:waypoint x="620" y="125" />
        <di:waypoint x="620" y="250" />
        <di:waypoint x="720" y="250" />
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Flow_0b0h3fp_di" bpmnElement="Flow_0b0h3fp">
        <di:waypoint x="490" y="100" />
        <di:waypoint x="595" y="100" />
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Flow_040ezjb_di" bpmnElement="Flow_040ezjb">
        <di:waypoint x="290" y="100" />
        <di:waypoint x="390" y="100" />
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Flow_1pkf6u1_di" bpmnElement="Flow_1pkf6u1">
        <di:waypoint x="90" y="100" />
        <di:waypoint x="190" y="100" />
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Flow_1fuqcx7_di" bpmnElement="Flow_1fuqcx7">
        <di:waypoint x="-112" y="100" />
        <di:waypoint x="-10" y="100" />
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Flow_0pmcah2_di" bpmnElement="Flow_0pmcah2">
        <di:waypoint x="1026" y="250" />
        <di:waypoint x="1076" y="250" />
        <di:waypoint x="1076" y="100" />
        <di:waypoint x="1182" y="100" />
      </bpmndi:BPMNEdge>
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>
</bpmn:definitions>
""";
}
