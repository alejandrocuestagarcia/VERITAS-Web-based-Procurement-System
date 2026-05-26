export const dummyBpmnXml = `
					<bpmn:definitions
					id="Definitions_1"
					targetNamespace="http://bpmn.io/schema/bpmn"
					xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
					xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
					xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
					xmlns:di="http://www.omg.org/spec/DD/20100524/DI"
					xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
					xmlns:veritas="http://veritas">
					<bpmn:process id="PR_Tiered_Approval_001" name="Tiered Procurement Approval" isExecutable="false">
					<bpmn:startEvent id="Event_1if1b7g" name="Start Procurement">
					<bpmn:outgoing>Flow_1hlbu7w</bpmn:outgoing>
					</bpmn:startEvent>
					<bpmn:task id="Activity_1u1p6ue" name="Fill in Details and Upload Vendor Quotes">
					<bpmn:documentation>[ASSIGNEE]PROCUREMENT_OFFICER</bpmn:documentation>
					<bpmn:incoming>Flow_1hlbu7w</bpmn:incoming>
					<bpmn:incoming>Flow_1s24xzj</bpmn:incoming>
					<bpmn:outgoing>Flow_0vsq5o5</bpmn:outgoing>
					</bpmn:task>
					<bpmn:sequenceFlow id="Flow_1hlbu7w" sourceRef="Event_1if1b7g" targetRef="Activity_1u1p6ue"/>
					<bpmn:exclusiveGateway id="Gateway_0dumvhe" name="Budget Check">
					<bpmn:incoming>Flow_0vsq5o5</bpmn:incoming>
					<bpmn:outgoing>Flow_1kxm0s8</bpmn:outgoing>
					<bpmn:outgoing>Flow_0z8w32s</bpmn:outgoing>
					<bpmn:outgoing>Flow_04ni0t0</bpmn:outgoing>
					</bpmn:exclusiveGateway>
					<bpmn:sequenceFlow id="Flow_0vsq5o5" sourceRef="Activity_1u1p6ue" targetRef="Gateway_0dumvhe"/>
					<bpmn:task id="Activity_0mib3l3" name="Automatic Validation">
					<bpmn:documentation>[ASSIGNEE]FINANCE_OFFICER</bpmn:documentation>
					<bpmn:incoming>Flow_04ni0t0</bpmn:incoming>
					<bpmn:outgoing>Flow_0vl4es9</bpmn:outgoing>
					</bpmn:task>
					<bpmn:task id="Activity_124j12i" name="Normal Finance Review">
					<bpmn:documentation>[ASSIGNEE]FINANCE_OFFICER</bpmn:documentation>
					<bpmn:incoming>Flow_0z8w32s</bpmn:incoming>
					<bpmn:outgoing>Flow_1pumtq5</bpmn:outgoing>
					</bpmn:task>
					<bpmn:task id="Activity_05zq0ij" name="Detailed Finance Review">
					<bpmn:documentation>[ASSIGNEE]FINANCE_OFFICER</bpmn:documentation>
					<bpmn:incoming>Flow_1kxm0s8</bpmn:incoming>
					<bpmn:outgoing>Flow_0nupddp</bpmn:outgoing>
					</bpmn:task>
					<bpmn:sequenceFlow id="Flow_1kxm0s8" name="budget &gt; \$10.000" sourceRef="Gateway_0dumvhe" targetRef="Activity_05zq0ij">
					<bpmn:conditionExpression xsi:type="bpmn:tFormalExpression">\${selectedQuoteTotalAmount != null and selectedQuoteTotalAmount &gt; 10000}</bpmn:conditionExpression>
					</bpmn:sequenceFlow>
					<bpmn:sequenceFlow id="Flow_0z8w32s" name="\$500 &gt; budget &gt; \$10.000" sourceRef="Gateway_0dumvhe" targetRef="Activity_124j12i">
					<bpmn:conditionExpression xsi:type="bpmn:tFormalExpression">\${selectedQuoteTotalAmount != null and selectedQuoteTotalAmount &gt;= 500 and selectedQuoteTotalAmount &lt;= 10000}</bpmn:conditionExpression>
					</bpmn:sequenceFlow>
					<bpmn:sequenceFlow id="Flow_04ni0t0" name="budget &lt; \$500" sourceRef="Gateway_0dumvhe" targetRef="Activity_0mib3l3">
					<bpmn:conditionExpression xsi:type="bpmn:tFormalExpression">\${selectedQuoteTotalAmount != null and selectedQuoteTotalAmount &lt; 500}</bpmn:conditionExpression>
					</bpmn:sequenceFlow>
					<bpmn:exclusiveGateway id="Gateway_0jludpd">
					<bpmn:incoming>Flow_1pumtq5</bpmn:incoming>
					<bpmn:incoming>Flow_0nupddp</bpmn:incoming>
					<bpmn:outgoing>Flow_1s24xzj</bpmn:outgoing>
					<bpmn:outgoing>Flow_0m5sv3t</bpmn:outgoing>
					</bpmn:exclusiveGateway>
					<bpmn:sequenceFlow id="Flow_1pumtq5" sourceRef="Activity_124j12i" targetRef="Gateway_0jludpd">
					<bpmn:extensionElements>
						<veritas:transitionRule isPdfRequired="true" />
					</bpmn:extensionElements>
					</bpmn:sequenceFlow>
					<bpmn:sequenceFlow id="Flow_0nupddp" sourceRef="Activity_05zq0ij" targetRef="Gateway_0jludpd">
					<bpmn:extensionElements>
						<veritas:transitionRule minRequiredVendors="3" optionalFailureMessage="Please provide at least 3 vendor quotes in order for the request to advance!" />
					</bpmn:extensionElements>
					</bpmn:sequenceFlow>
					<bpmn:sequenceFlow id="Flow_1s24xzj" name="Request rejected" sourceRef="Gateway_0jludpd" targetRef="Activity_1u1p6ue"/>
					<bpmn:endEvent id="Event_0zjfdeg">
					<bpmn:incoming>Flow_0m5sv3t</bpmn:incoming>
					<bpmn:incoming>Flow_0vl4es9</bpmn:incoming>
					</bpmn:endEvent>
					<bpmn:sequenceFlow id="Flow_0m5sv3t" name="Request accepted" sourceRef="Gateway_0jludpd" targetRef="Event_0zjfdeg">
					<bpmn:conditionExpression xsi:type="bpmn:tFormalExpression">\${true}</bpmn:conditionExpression>
					</bpmn:sequenceFlow>
					<bpmn:sequenceFlow id="Flow_0vl4es9" name="auto-accept" sourceRef="Activity_0mib3l3" targetRef="Event_0zjfdeg"/>
					</bpmn:process>
					<bpmndi:BPMNDiagram id="BPMNDiagram_1">
					<bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="PR_Tiered_Approval_001">
					<bpmndi:BPMNShape id="Event_1if1b7g_di" bpmnElement="Event_1if1b7g">
					<dc:Bounds x="-78" y="122" width="36" height="36"/>
					<bpmndi:BPMNLabel>
					<dc:Bounds x="-105" y="165" width="90" height="14"/>
					</bpmndi:BPMNLabel>
					</bpmndi:BPMNShape>
					<bpmndi:BPMNShape id="Activity_1u1p6ue_di" bpmnElement="Activity_1u1p6ue">
					<dc:Bounds x="70" y="100" width="100" height="80"/>
					<bpmndi:BPMNLabel/>
					</bpmndi:BPMNShape>
					<bpmndi:BPMNShape id="Gateway_0dumvhe_di" bpmnElement="Gateway_0dumvhe" isMarkerVisible="true">
					<dc:Bounds x="245" y="115" width="50" height="50"/>
					<bpmndi:BPMNLabel>
					<dc:Bounds x="235" y="172" width="70" height="14"/>
					</bpmndi:BPMNLabel>
					</bpmndi:BPMNShape>
					<bpmndi:BPMNShape id="Activity_0mib3l3_di" bpmnElement="Activity_0mib3l3">
					<dc:Bounds x="370" y="-30" width="100" height="80"/>
					<bpmndi:BPMNLabel/>
					</bpmndi:BPMNShape>
					<bpmndi:BPMNShape id="Activity_124j12i_di" bpmnElement="Activity_124j12i">
					<dc:Bounds x="370" y="100" width="100" height="80"/>
					<bpmndi:BPMNLabel/>
					</bpmndi:BPMNShape>
					<bpmndi:BPMNShape id="Activity_05zq0ij_di" bpmnElement="Activity_05zq0ij">
					<dc:Bounds x="370" y="230" width="100" height="80"/>
					<bpmndi:BPMNLabel/>
					</bpmndi:BPMNShape>
					<bpmndi:BPMNShape id="Gateway_0jludpd_di" bpmnElement="Gateway_0jludpd" isMarkerVisible="true">
					<dc:Bounds x="555" y="175" width="50" height="50"/>
					</bpmndi:BPMNShape>
					<bpmndi:BPMNShape id="Event_0zjfdeg_di" bpmnElement="Event_0zjfdeg">
					<dc:Bounds x="692" y="122" width="36" height="36"/>
					</bpmndi:BPMNShape>
					<bpmndi:BPMNEdge id="Flow_1hlbu7w_di" bpmnElement="Flow_1hlbu7w">
					<di:waypoint x="-42" y="140"/>
					<di:waypoint x="70" y="140"/>
					</bpmndi:BPMNEdge>
					<bpmndi:BPMNEdge id="Flow_0vsq5o5_di" bpmnElement="Flow_0vsq5o5">
					<di:waypoint x="170" y="140"/>
					<di:waypoint x="245" y="140"/>
					</bpmndi:BPMNEdge>
					<bpmndi:BPMNEdge id="Flow_1kxm0s8_di" bpmnElement="Flow_1kxm0s8">
					<di:waypoint x="270" y="165"/>
					<di:waypoint x="270" y="270"/>
					<di:waypoint x="370" y="270"/>
					<bpmndi:BPMNLabel>
					<dc:Bounds x="242" y="215" width="86" height="14"/>
					</bpmndi:BPMNLabel>
					</bpmndi:BPMNEdge>
					<bpmndi:BPMNEdge id="Flow_0z8w32s_di" bpmnElement="Flow_0z8w32s">
					<di:waypoint x="295" y="140"/>
					<di:waypoint x="370" y="140"/>
					<bpmndi:BPMNLabel>
					<dc:Bounds x="292" y="116" width="81" height="27"/>
					</bpmndi:BPMNLabel>
					</bpmndi:BPMNEdge>
					<bpmndi:BPMNEdge id="Flow_04ni0t0_di" bpmnElement="Flow_04ni0t0">
					<di:waypoint x="270" y="115"/>
					<di:waypoint x="270" y="10"/>
					<di:waypoint x="370" y="10"/>
					<bpmndi:BPMNLabel>
					<dc:Bounds x="250" y="60" width="71" height="14"/>
					</bpmndi:BPMNLabel>
					</bpmndi:BPMNEdge>
					<bpmndi:BPMNEdge id="Flow_1pumtq5_di" bpmnElement="Flow_1pumtq5">
					<di:waypoint x="470" y="140"/>
					<di:waypoint x="580" y="140"/>
					<di:waypoint x="580" y="175"/>
					<bpmndi:BPMNLabel>
					<dc:Bounds x="491" y="122" width="69" height="14"/>
					</bpmndi:BPMNLabel>
					</bpmndi:BPMNEdge>
					<bpmndi:BPMNEdge id="Flow_0nupddp_di" bpmnElement="Flow_0nupddp">
					<di:waypoint x="420" y="230"/>
					<di:waypoint x="420" y="200"/>
					<di:waypoint x="555" y="200"/>
					<bpmndi:BPMNLabel>
					<dc:Bounds x="401" y="212" width="69" height="14"/>
					</bpmndi:BPMNLabel>
					</bpmndi:BPMNEdge>
					<bpmndi:BPMNEdge id="Flow_1s24xzj_di" bpmnElement="Flow_1s24xzj">
					<di:waypoint x="580" y="225"/>
					<di:waypoint x="580" y="340"/>
					<di:waypoint x="120" y="340"/>
					<di:waypoint x="120" y="180"/>
					<bpmndi:BPMNLabel>
					<dc:Bounds x="308" y="322" width="84" height="14"/>
					</bpmndi:BPMNLabel>
					</bpmndi:BPMNEdge>
					<bpmndi:BPMNEdge id="Flow_0m5sv3t_di" bpmnElement="Flow_0m5sv3t">
					<di:waypoint x="605" y="200"/>
					<di:waypoint x="710" y="200"/>
					<di:waypoint x="710" y="158"/>
					<bpmndi:BPMNLabel>
					<dc:Bounds x="613" y="182" width="89" height="14"/>
					</bpmndi:BPMNLabel>
					</bpmndi:BPMNEdge>
					<bpmndi:BPMNEdge id="Flow_0vl4es9_di" bpmnElement="Flow_0vl4es9">
					<di:waypoint x="470" y="10"/>
					<di:waypoint x="710" y="10"/>
					<di:waypoint x="710" y="122"/>
					<bpmndi:BPMNLabel>
					<dc:Bounds x="561" y="-8" width="58" height="14"/>
					</bpmndi:BPMNLabel>
					</bpmndi:BPMNEdge>
					</bpmndi:BPMNPlane>
					</bpmndi:BPMNDiagram>
					</bpmn:definitions>
`;
