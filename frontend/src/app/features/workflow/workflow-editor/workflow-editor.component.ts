import { Component, ElementRef, OnInit, ViewChild, OnDestroy } from '@angular/core';
import BpmnModeler from 'bpmn-js/lib/Modeler';
import { editorModules } from '../custom-renderer';

@Component({
  selector: 'app-workflow-editor',
  templateUrl: './workflow-editor.component.html',
  styleUrls: ['./workflow-editor.component.scss']
})



export class WorkflowEditorComponent implements OnInit, OnDestroy {
  @ViewChild('canvas', { static: true }) private canvas!: ElementRef;
  private bpmnEditor: any;
  workflowTitle = '';
  workflowId = '';

  public showPropertiesPanelTransition: boolean = false;
  public showPropertiesPanelTask: boolean = false;
  public selectedElementId: string = '';
  public currentRule: any = {
    isPdfRequired: false,
    minRequiredVendors: 0,
    optionalFailureMessage: ''
  };


  async ngOnInit() {

    this.bpmnEditor = new BpmnModeler({
      container: this.canvas.nativeElement,
      additionalModules: [
        editorModules
      ]
    });

    try {
      await this.bpmnEditor.importXML(dummyBpmnXml);
      const canvas = this.bpmnEditor.get('canvas');
      const rootElement = canvas.getRootElement();

      this.workflowTitle = rootElement.businessObject.name;
      this.workflowId = rootElement.businessObject.id;
      this.applyTransitionRuleCss();
      canvas.zoom('fit-viewport', 'auto');

    } catch (err) {
      console.error('Failed to render workflow', err);
    }

    this.bpmnEditor.on('selection.changed', (event: any) => {
      const selection = event.newSelection[0];

      if (selection) {
        if (selection.type == 'bpmn:SequenceFlow') {
          this.showPropertiesPanelTask = false;
          this.showPropertiesPanelTransition = true;
          this.loadTransitionRules(selection);
        } else {
          this.showPropertiesPanelTransition = false;
          this.showPropertiesPanelTask = true;
        }
      } else {
        this.showPropertiesPanelTransition = false;
        this.showPropertiesPanelTask = false;
        this.resetRule();
      }
    });
  }

  zoomIn() { this.bpmnEditor.get('zoomScroll').stepZoom(1); }
  zoomOut() { this.bpmnEditor.get('zoomScroll').stepZoom(-1); }
  resetZoom() { this.bpmnEditor.get('canvas').zoom('fit-viewport', 'auto'); }

  async exportXML() {
    try {
      const { xml } = await this.bpmnEditor.saveXML({ format: true });
      const blob = new Blob([xml], { type: 'application/xml' });
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `${this.workflowTitle || 'workflow'}.bpmn`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
    } catch (err) {
      console.error('Failed to export XML', err);
    }
  }

  onFileSelected(event: any) {
    const file = event.target.files[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = async (e: any) => {
        try {
          const xml = e.target.result;
          await this.bpmnEditor.importXML(xml);
          const canvas = this.bpmnEditor.get('canvas');
          const rootElement = canvas.getRootElement();
          this.workflowTitle = rootElement?.businessObject?.name || 'Imported Workflow';
          this.workflowId = rootElement?.businessObject?.id || '';
          this.applyTransitionRuleCss();
          canvas.zoom('fit-viewport', 'auto');
        } catch (err) {
          console.error('Failed to import XML', err);
        }
      };
      reader.readAsText(file);
    }
    event.target.value = '';
  }

  ngOnDestroy() { this.bpmnEditor?.destroy(); }

  loadTransitionRules(selection: any) {
    const elementId = selection.id;
    this.selectedElementId = elementId;

    const elementRegistry = this.bpmnEditor.get('elementRegistry');
    const element = elementRegistry.get(elementId);

    if (!element || !element.businessObject) {
      console.warn(`Could not find businessObject for ID: ${elementId}`);
      this.resetRule();
      return;
    }

    const bo = element.businessObject;
    const extensions = bo.extensionElements;

    if (extensions && extensions.values) {
      const rule = extensions.values.find((e: any) =>
        e.$type === 'veritas:transitionRule' || e.type === 'veritas:transitionRule'
      );

      if (rule) {
        this.currentRule = {
          isPdfRequired: String(rule.isPdfRequired) === 'true',
          minRequiredVendors: parseInt(rule.minRequiredVendors || '0'),
          optionalFailureMessage: rule.failureMessage || ''
        };
        return;
      }
    }

    this.resetRule();
  }

  private resetRule() {
    this.currentRule = { isPdfRequired: false, minRequiredVendors: 0, optionalFailureMessage: '' };
  }
  private applyTransitionRuleCss() {
    const canvas = this.bpmnEditor.get('canvas');
    const elementRegistry = this.bpmnEditor.get('elementRegistry');

    elementRegistry.forEach((element: any) => {

      if (element.type === 'bpmn:SequenceFlow') {
        const bo = element.businessObject;
        const extensions = bo.extensionElements;
        const hasConstraint = extensions?.values?.some((val: any) =>
          [val.$type, val.type].includes('veritas:transitionRule')
        );

        if (hasConstraint) {
          canvas.addMarker(element.id, 'highlight');
        }
      }
    });
  }
}

//AI-Generated dummy for mocking
const dummyBpmnXml = `
<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI" xmlns:dc="http://www.omg.org/spec/DD/20100524/DC" xmlns:di="http://www.omg.org/spec/DD/20100524/DI" xmlns:veritas="http://veritas/schema/1.0" id="Definitions_1" targetNamespace="http://bpmn.io/schema/bpmn">
  <bpmn:process id="PR_Tiered_Approval_001" name="Global Procurement Strategy " isExecutable="false">
    <bpmn:startEvent id="StartEvent_1" name="Request Submitted">
      <bpmn:outgoing>Flow_Start</bpmn:outgoing>
    </bpmn:startEvent>
    <bpmn:exclusiveGateway id="Gateway_Tier" name="Amount Check">
      <bpmn:incoming>Flow_Start</bpmn:incoming>
      <bpmn:outgoing>Flow_Standard</bpmn:outgoing>
      <bpmn:outgoing>Flow_High</bpmn:outgoing>
      <bpmn:outgoing>Flow_Executive</bpmn:outgoing>
    </bpmn:exclusiveGateway>
    <bpmn:sequenceFlow id="Flow_Start" sourceRef="StartEvent_1" targetRef="Gateway_Tier" />
    <bpmn:serviceTask id="Activity_Auto" name="Auto-Approval (Small)">
      <bpmn:incoming>Flow_Standard</bpmn:incoming>
      <bpmn:outgoing>Flow_End_1</bpmn:outgoing>
    </bpmn:serviceTask>
    <bpmn:sequenceFlow id="Flow_Standard" name="Under $500" sourceRef="Gateway_Tier" targetRef="Activity_Auto" />
    <bpmn:userTask id="Activity_Manager" name="Manager Approval">
      <bpmn:incoming>Flow_High</bpmn:incoming>
      <bpmn:outgoing>Flow_End_2</bpmn:outgoing>
    </bpmn:userTask>
    <bpmn:sequenceFlow id="Flow_High" name="Over $500" sourceRef="Gateway_Tier" targetRef="Activity_Manager">
      <bpmn:extensionElements>
        <veritas:transitionRule isPdfRequired="true" minRequiredVendors="0" failureMessage="Manager approval requires a signed requisition PDF." />
      </bpmn:extensionElements>
    </bpmn:sequenceFlow>
    <bpmn:task id="Activity_Board" name="Board Executive Review">
      <bpmn:incoming>Flow_Executive</bpmn:incoming>
      <bpmn:outgoing>Flow_End_3</bpmn:outgoing>
    </bpmn:task>
    <bpmn:sequenceFlow id="Flow_Executive" name="Over $50,000" sourceRef="Gateway_Tier" targetRef="Activity_Board">
      <bpmn:extensionElements>
        <veritas:transitionRule isPdfRequired="true" minRequiredVendors="3" failureMessage="High-value procurement requires at least 3 vendor quotes and executive documentation." />
      </bpmn:extensionElements>
    </bpmn:sequenceFlow>
    <bpmn:endEvent id="EndEvent_1" name="Ready for Payment">
      <bpmn:incoming>Flow_End_1</bpmn:incoming>
      <bpmn:incoming>Flow_End_2</bpmn:incoming>
      <bpmn:incoming>Flow_End_3</bpmn:incoming>
    </bpmn:endEvent>
    <bpmn:sequenceFlow id="Flow_End_1" sourceRef="Activity_Auto" targetRef="EndEvent_1" />
    <bpmn:sequenceFlow id="Flow_End_2" sourceRef="Activity_Manager" targetRef="EndEvent_1" />
    <bpmn:sequenceFlow id="Flow_End_3" sourceRef="Activity_Board" targetRef="EndEvent_1" />
  </bpmn:process>
  <bpmndi:BPMNDiagram id="BPMNDiagram_1">
    <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="PR_Tiered_Approval_001">
      <bpmndi:BPMNShape id="Start_di" bpmnElement="StartEvent_1"><dc:Bounds x="100" y="200" width="36" height="36" /></bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="Gateway_di" bpmnElement="Gateway_Tier" isMarkerVisible="true"><dc:Bounds x="200" y="193" width="50" height="50" /></bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="Auto_di" bpmnElement="Activity_Auto"><dc:Bounds x="350" y="80" width="100" height="80" /></bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="Manager_di" bpmnElement="Activity_Manager"><dc:Bounds x="350" y="178" width="100" height="80" /></bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="Board_di" bpmnElement="Activity_Board"><dc:Bounds x="350" y="280" width="100" height="80" /></bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="End_di" bpmnElement="EndEvent_1"><dc:Bounds x="550" y="200" width="36" height="36" /></bpmndi:BPMNShape>
      <bpmndi:BPMNEdge id="Edge_Start" bpmnElement="Flow_Start"><di:waypoint x="136" y="218" /><di:waypoint x="200" y="218" /></bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Edge_Standard" bpmnElement="Flow_Standard"><di:waypoint x="225" y="193" /><di:waypoint x="225" y="120" /><di:waypoint x="350" y="120" /></bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Edge_High" bpmnElement="Flow_High"><di:waypoint x="250" y="218" /><di:waypoint x="350" y="218" /></bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Edge_Exec" bpmnElement="Flow_Executive"><di:waypoint x="225" y="243" /><di:waypoint x="225" y="320" /><di:waypoint x="350" y="320" /></bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Edge_End1" bpmnElement="Flow_End_1"><di:waypoint x="450" y="120" /><di:waypoint x="500" y="120" /><di:waypoint x="500" y="218" /><di:waypoint x="550" y="218" /></bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Edge_End2" bpmnElement="Flow_End_2"><di:waypoint x="450" y="218" /><di:waypoint x="550" y="218" /></bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Edge_End3" bpmnElement="Flow_End_3"><di:waypoint x="450" y="320" /><di:waypoint x="500" y="320" /><di:waypoint x="500" y="218" /><di:waypoint x="550" y="218" /></bpmndi:BPMNEdge>
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>
</bpmn:definitions>`;
