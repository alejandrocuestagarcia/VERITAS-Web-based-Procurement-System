export const dummyBpmnXml = `
<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI" xmlns:dc="http://www.omg.org/spec/DD/20100524/DC" xmlns:di="http://www.omg.org/spec/DD/20100524/DI" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:veritas="http://veritas" id="Definitions_1" targetNamespace="http://bpmn.io/schema/bpmn">
  <bpmn:process id="PR_Tiered_Approval_001" name="" isExecutable="false">
    <bpmn:documentation></bpmn:documentation>
    <bpmn:startEvent id="Event_1vwb76m" name="Submit Procurement">
      <bpmn:outgoing>Flow_019sxao</bpmn:outgoing>
    </bpmn:startEvent>
    <bpmn:sequenceFlow id="Flow_019sxao" sourceRef="Event_1vwb76m" targetRef="Activity_1scv5mh" />
    <bpmn:task id="Activity_1scv5mh" name="Vendor Quote Selection">
      <bpmn:documentation>[ASSIGNEE]PROCUREMENT_OFFICER</bpmn:documentation>
      <bpmn:documentation>Enter one vendor and select.</bpmn:documentation>
      <bpmn:incoming>Flow_019sxao</bpmn:incoming>
      <bpmn:outgoing>Flow_0l0cq37</bpmn:outgoing>
    </bpmn:task>
    <bpmn:task id="Activity_0wwxjvp" name="Normal Finance Check">
      <bpmn:documentation>[ASSIGNEE]FINANCE_OFFICER</bpmn:documentation>
      <bpmn:documentation>Check request for validity</bpmn:documentation>
      <bpmn:incoming>Flow_0l0cq37</bpmn:incoming>
      <bpmn:outgoing>Flow_0qb5eb3</bpmn:outgoing>
    </bpmn:task>
    <bpmn:sequenceFlow id="Flow_0l0cq37" sourceRef="Activity_1scv5mh" targetRef="Activity_0wwxjvp" />
    <bpmn:endEvent id="Event_13huewk" name="Finish Procurement">
      <bpmn:incoming>Flow_0qb5eb3</bpmn:incoming>
    </bpmn:endEvent>
    <bpmn:sequenceFlow id="Flow_0qb5eb3" sourceRef="Activity_0wwxjvp" targetRef="Event_13huewk" />
  </bpmn:process>
  <bpmndi:BPMNDiagram id="BPMNDiagram_1">
    <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="PR_Tiered_Approval_001">
      <bpmndi:BPMNShape id="Event_1vwb76m_di" bpmnElement="Event_1vwb76m">
        <dc:Bounds x="-148" y="122" width="36" height="36" />
        <bpmndi:BPMNLabel>
          <dc:Bounds x="-161" y="165" width="63" height="27" />
        </bpmndi:BPMNLabel>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="Activity_0wwxjvp_di" bpmnElement="Activity_0wwxjvp">
        <dc:Bounds x="160" y="100" width="100" height="80" />
        <bpmndi:BPMNLabel />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="Event_13huewk_di" bpmnElement="Event_13huewk">
        <dc:Bounds x="342" y="122" width="36" height="36" />
        <bpmndi:BPMNLabel>
          <dc:Bounds x="329" y="165" width="63" height="27" />
        </bpmndi:BPMNLabel>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="Activity_1scv5mh_di" bpmnElement="Activity_1scv5mh">
        <dc:Bounds x="-30" y="100" width="100" height="80" />
        <bpmndi:BPMNLabel />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNEdge id="Flow_019sxao_di" bpmnElement="Flow_019sxao">
        <di:waypoint x="-112" y="140" />
        <di:waypoint x="-30" y="140" />
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Flow_0l0cq37_di" bpmnElement="Flow_0l0cq37">
        <di:waypoint x="70" y="140" />
        <di:waypoint x="160" y="140" />
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Flow_0qb5eb3_di" bpmnElement="Flow_0qb5eb3">
        <di:waypoint x="260" y="140" />
        <di:waypoint x="342" y="140" />
      </bpmndi:BPMNEdge>
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>
</bpmn:definitions>
`;
