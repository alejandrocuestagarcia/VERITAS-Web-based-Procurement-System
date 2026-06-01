export const dummyBpmnXml = `
<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI" xmlns:dc="http://www.omg.org/spec/DD/20100524/DC" xmlns:di="http://www.omg.org/spec/DD/20100524/DI" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:veritas="http://veritas" id="Definitions_1" targetNamespace="http://bpmn.io/schema/bpmn">
  <bpmn:process id="PR_Tiered_Approval_001" name="Standard Procurement Workflow" isExecutable="false">
    <bpmn:documentation></bpmn:documentation>
    <bpmn:startEvent id="Event_1vwb76m" name="Submit Procurement">
      <bpmn:outgoing>Flow_019sxao</bpmn:outgoing>
    </bpmn:startEvent>
    <bpmn:exclusiveGateway id="Gateway_1dwq3xb" name="Team Leader Check">
      <bpmn:incoming>Flow_019sxao</bpmn:incoming>
      <bpmn:outgoing>Flow_0sbraey</bpmn:outgoing>
      <bpmn:outgoing>Flow_1s1k4y5</bpmn:outgoing>
    </bpmn:exclusiveGateway>
    <bpmn:sequenceFlow id="Flow_019sxao" sourceRef="Event_1vwb76m" targetRef="Gateway_1dwq3xb" />
    <bpmn:task id="Activity_1mp955o" name="Team Leader Confirmation">
      <bpmn:documentation>[ASSIGNEE]REQUESTER</bpmn:documentation>
      <bpmn:documentation>[TEAM_LEADER]true</bpmn:documentation>
      <bpmn:documentation>Check if the Request is actually necessary and valid, and give Team Leader Approval. </bpmn:documentation>
      <bpmn:incoming>Flow_0sbraey</bpmn:incoming>
      <bpmn:outgoing>Flow_0qnfjjy</bpmn:outgoing>
    </bpmn:task>
    <bpmn:sequenceFlow id="Flow_0sbraey" sourceRef="Gateway_1dwq3xb" targetRef="Activity_1mp955o">
      <bpmn:conditionExpression xsi:type="bpmn:tFormalExpression">\${user.id != team.leader.id}</bpmn:conditionExpression>
    </bpmn:sequenceFlow>
    <bpmn:task id="Activity_1scv5mh" name="Vendor Quote Selection">
      <bpmn:documentation>Enter 3 different vendors with correct data. Choose the best vendor offer based on our Selection Guidelines.</bpmn:documentation>
      <bpmn:documentation>[ASSIGNEE]PROCUREMENT_OFFICER</bpmn:documentation>
      <bpmn:incoming>Flow_1s1k4y5</bpmn:incoming>
      <bpmn:incoming>Flow_0qnfjjy</bpmn:incoming>
      <bpmn:outgoing>Flow_0b9tqhi</bpmn:outgoing>
    </bpmn:task>
    <bpmn:sequenceFlow id="Flow_1s1k4y5" sourceRef="Gateway_1dwq3xb" targetRef="Activity_1scv5mh">
      <bpmn:conditionExpression xsi:type="bpmn:tFormalExpression">\${user.id == team.leader.id}</bpmn:conditionExpression>
    </bpmn:sequenceFlow>
    <bpmn:sequenceFlow id="Flow_0qnfjjy" sourceRef="Activity_1mp955o" targetRef="Activity_1scv5mh" />
    <bpmn:exclusiveGateway id="Gateway_0no29ob">
      <bpmn:incoming>Flow_0b9tqhi</bpmn:incoming>
      <bpmn:outgoing>Flow_10069wk</bpmn:outgoing>
      <bpmn:outgoing>Flow_17c7d5d</bpmn:outgoing>
      <bpmn:outgoing>Flow_0bzo1dd</bpmn:outgoing>
    </bpmn:exclusiveGateway>
    <bpmn:sequenceFlow id="Flow_0b9tqhi" sourceRef="Activity_1scv5mh" targetRef="Gateway_0no29ob">
      <bpmn:extensionElements>
        <veritas:transitionRule type="veritas:transitionRule" minRequiredVendors="3" />
      </bpmn:extensionElements>
    </bpmn:sequenceFlow>
    <bpmn:task id="Activity_0zpfhjd" name="Automatic Approval">
      <bpmn:documentation>[ASSIGNEE]FINANCE_OFFICER</bpmn:documentation>
      <bpmn:incoming>Flow_17c7d5d</bpmn:incoming>
      <bpmn:outgoing>Flow_0u0m60m</bpmn:outgoing>
    </bpmn:task>
    <bpmn:task id="Activity_17it5r8" name="Standard Finance Review">
      <bpmn:documentation>[ASSIGNEE]FINANCE_OFFICER</bpmn:documentation>
      <bpmn:documentation>Perform final finance review.</bpmn:documentation>
      <bpmn:incoming>Flow_10069wk</bpmn:incoming>
      <bpmn:outgoing>Flow_0hojtag</bpmn:outgoing>
    </bpmn:task>
    <bpmn:sequenceFlow id="Flow_10069wk" sourceRef="Gateway_0no29ob" targetRef="Activity_17it5r8">
      <bpmn:conditionExpression xsi:type="bpmn:tFormalExpression">\${selectedQuoteTotalAmount &gt;= 500 and selectedQuoteTotalAmount &lt; 2000}</bpmn:conditionExpression>
    </bpmn:sequenceFlow>
    <bpmn:sequenceFlow id="Flow_17c7d5d" sourceRef="Gateway_0no29ob" targetRef="Activity_0zpfhjd">
      <bpmn:conditionExpression xsi:type="bpmn:tFormalExpression">\${selectedQuoteTotalAmount &lt; 500}</bpmn:conditionExpression>
    </bpmn:sequenceFlow>
    <bpmn:endEvent id="Event_0f7uyqa" name="Pay Procurement">
      <bpmn:incoming>Flow_1pi5o75</bpmn:incoming>
      <bpmn:incoming>Flow_0hojtag</bpmn:incoming>
      <bpmn:incoming>Flow_0u0m60m</bpmn:incoming>
    </bpmn:endEvent>
    <bpmn:task id="Activity_0fktd09" name="Executive Requistion Report">
      <bpmn:documentation>Since the vendor offer exceeds 2000€, please provide an Executive Requisition Report. The corresponding template is available in the company wiki.</bpmn:documentation>
      <bpmn:documentation>[ASSIGNEE]FINANCE_OFFICER</bpmn:documentation>
      <bpmn:incoming>Flow_0bzo1dd</bpmn:incoming>
      <bpmn:outgoing>Flow_1sapfpf</bpmn:outgoing>
    </bpmn:task>
    <bpmn:task id="Activity_0u8kehp" name="Executive Finance Review">
      <bpmn:documentation>[ASSIGNEE]FINANCE_OFFICER</bpmn:documentation>
      <bpmn:documentation>Check provided Executive Requisition Report for validity and perform final finance review.</bpmn:documentation>
      <bpmn:incoming>Flow_1sapfpf</bpmn:incoming>
      <bpmn:outgoing>Flow_1pi5o75</bpmn:outgoing>
    </bpmn:task>
    <bpmn:sequenceFlow id="Flow_1pi5o75" sourceRef="Activity_0u8kehp" targetRef="Event_0f7uyqa" />
    <bpmn:sequenceFlow id="Flow_0hojtag" sourceRef="Activity_17it5r8" targetRef="Event_0f7uyqa" />
    <bpmn:sequenceFlow id="Flow_0bzo1dd" sourceRef="Gateway_0no29ob" targetRef="Activity_0fktd09">
      <bpmn:conditionExpression xsi:type="bpmn:tFormalExpression">\${selectedQuoteTotalAmount &gt;= 2000}</bpmn:conditionExpression>
    </bpmn:sequenceFlow>
    <bpmn:sequenceFlow id="Flow_1sapfpf" sourceRef="Activity_0fktd09" targetRef="Activity_0u8kehp">
      <bpmn:extensionElements>
        <veritas:transitionRule type="veritas:transitionRule" isPdfRequired="true" />
      </bpmn:extensionElements>
    </bpmn:sequenceFlow>
    <bpmn:sequenceFlow id="Flow_0u0m60m" sourceRef="Activity_0zpfhjd" targetRef="Event_0f7uyqa" />
  </bpmn:process>
  <bpmndi:BPMNDiagram id="BPMNDiagram_1">
    <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="PR_Tiered_Approval_001">
      <bpmndi:BPMNShape id="Event_1vwb76m_di" bpmnElement="Event_1vwb76m">
        <dc:Bounds x="-148" y="122" width="36" height="36" />
        <bpmndi:BPMNLabel>
          <dc:Bounds x="-161" y="165" width="63" height="27" />
        </bpmndi:BPMNLabel>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="Gateway_1dwq3xb_di" bpmnElement="Gateway_1dwq3xb" isMarkerVisible="true">
        <dc:Bounds x="-25" y="115" width="50" height="50" />
        <bpmndi:BPMNLabel>
          <dc:Bounds x="-32.5" y="174.5" width="65" height="27" />
        </bpmndi:BPMNLabel>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="Activity_1mp955o_di" bpmnElement="Activity_1mp955o">
        <dc:Bounds x="60" y="-30" width="100" height="80" />
        <bpmndi:BPMNLabel />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="Activity_1scv5mh_di" bpmnElement="Activity_1scv5mh">
        <dc:Bounds x="170" y="100" width="100" height="80" />
        <bpmndi:BPMNLabel />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="Gateway_0no29ob_di" bpmnElement="Gateway_0no29ob" isMarkerVisible="true">
        <dc:Bounds x="375" y="115" width="50" height="50" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="Activity_0zpfhjd_di" bpmnElement="Activity_0zpfhjd">
        <dc:Bounds x="550" y="-60" width="100" height="80" />
        <bpmndi:BPMNLabel />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="Activity_17it5r8_di" bpmnElement="Activity_17it5r8">
        <dc:Bounds x="550" y="100" width="100" height="80" />
        <bpmndi:BPMNLabel />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="Event_0f7uyqa_di" bpmnElement="Event_0f7uyqa">
        <dc:Bounds x="832" y="122" width="36" height="36" />
        <bpmndi:BPMNLabel>
          <dc:Bounds x="808" y="165" width="85" height="14" />
        </bpmndi:BPMNLabel>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="Activity_0fktd09_di" bpmnElement="Activity_0fktd09">
        <dc:Bounds x="450" y="240" width="100" height="80" />
        <bpmndi:BPMNLabel />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="Activity_0u8kehp_di" bpmnElement="Activity_0u8kehp">
        <dc:Bounds x="650" y="240" width="100" height="80" />
        <bpmndi:BPMNLabel />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNEdge id="Flow_019sxao_di" bpmnElement="Flow_019sxao">
        <di:waypoint x="-112" y="140" />
        <di:waypoint x="-25" y="140" />
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Flow_0sbraey_di" bpmnElement="Flow_0sbraey">
        <di:waypoint x="0" y="115" />
        <di:waypoint x="0" y="10" />
        <di:waypoint x="60" y="10" />
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Flow_1s1k4y5_di" bpmnElement="Flow_1s1k4y5">
        <di:waypoint x="25" y="140" />
        <di:waypoint x="170" y="140" />
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Flow_0qnfjjy_di" bpmnElement="Flow_0qnfjjy">
        <di:waypoint x="160" y="10" />
        <di:waypoint x="220" y="10" />
        <di:waypoint x="220" y="100" />
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Flow_0b9tqhi_di" bpmnElement="Flow_0b9tqhi">
        <di:waypoint x="270" y="140" />
        <di:waypoint x="375" y="140" />
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Flow_10069wk_di" bpmnElement="Flow_10069wk">
        <di:waypoint x="425" y="140" />
        <di:waypoint x="550" y="140" />
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Flow_17c7d5d_di" bpmnElement="Flow_17c7d5d">
        <di:waypoint x="400" y="115" />
        <di:waypoint x="400" y="-20" />
        <di:waypoint x="550" y="-20" />
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Flow_1pi5o75_di" bpmnElement="Flow_1pi5o75">
        <di:waypoint x="750" y="280" />
        <di:waypoint x="791" y="280" />
        <di:waypoint x="791" y="140" />
        <di:waypoint x="832" y="140" />
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Flow_0hojtag_di" bpmnElement="Flow_0hojtag">
        <di:waypoint x="650" y="140" />
        <di:waypoint x="832" y="140" />
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Flow_0bzo1dd_di" bpmnElement="Flow_0bzo1dd">
        <di:waypoint x="400" y="165" />
        <di:waypoint x="400" y="280" />
        <di:waypoint x="450" y="280" />
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Flow_1sapfpf_di" bpmnElement="Flow_1sapfpf">
        <di:waypoint x="550" y="280" />
        <di:waypoint x="650" y="280" />
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Flow_0u0m60m_di" bpmnElement="Flow_0u0m60m">
        <di:waypoint x="650" y="-20" />
        <di:waypoint x="741" y="-20" />
        <di:waypoint x="741" y="140" />
        <di:waypoint x="832" y="140" />
      </bpmndi:BPMNEdge>
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>
</bpmn:definitions>
`;
