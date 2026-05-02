import { Component, ElementRef, OnInit, ViewChild, OnDestroy } from '@angular/core';
import BpmnModeler from 'bpmn-js/lib/Modeler';
import BpmnViewer from 'bpmn-js/lib/NavigatedViewer';
import { editorModules, viewerModules } from '../custom-renderer';
import { WorkflowModuleService, WorkflowSaveDto } from 'src/app/core/api';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router, ActivatedRoute } from '@angular/router';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';

export type WorkflowMode = 'create' | 'edit' | 'view';

@Component({
  selector: 'app-workflow-editor',
  templateUrl: './workflow-editor.component.html',
  styleUrls: ['./workflow-editor.component.scss']
})
export class WorkflowEditorComponent implements OnInit, OnDestroy {
  @ViewChild('canvas', { static: true }) private canvas!: ElementRef;

  private bpmnInstance: any;

  mode: WorkflowMode = 'create';
  workflowForm: FormGroup;
  workflowId: number | null = null;
  workflowVersion: number | null = null;
  workflowName: string | null = null;
  workflowDescription: string | null = null;
  workflowIsActive: boolean | null = null;

  public showPropertiesPanelTransition = false;
  public showPropertiesPanelTask = false;
  public selectedElementId = '';
  public currentRule: any = {
    isPdfRequired: false,
    minRequiredVendors: 0,
    optionalFailureMessage: ''
  };

  get isEditable(): boolean {
    return this.mode !== 'view';
  }

  constructor(
    private workflowService: WorkflowModuleService,
    public router: Router,
    private route: ActivatedRoute,
    private snackBar: MatSnackBar,
    private fb: FormBuilder
  ) {
    this.workflowForm = this.fb.group({
      title: ['', Validators.required],
      description: ['']
    });
  }

  async ngOnInit() {
    this.mode = (this.route.snapshot.data['mode'] as WorkflowMode) ?? 'create';

    const BpmnClass = this.mode === 'view' ? BpmnViewer : BpmnModeler;
    const modules = this.mode === 'view' ? viewerModules : editorModules;

    this.bpmnInstance = new BpmnClass({
      container: this.canvas.nativeElement,
      additionalModules: [modules]
    });

    this.bpmnInstance.on('selection.changed', (event: any) => {
      const selection = event.newSelection[0];
      if (selection) {
        if (selection.type === 'bpmn:SequenceFlow') {
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

    if (this.mode === 'create') {
      await this.loadXml(dummyBpmnXml);
    } else {
      const id = Number(this.route.snapshot.paramMap.get('id'));
      this.workflowId = id;
      this.workflowService.getWorkflow(id).subscribe({
        next: async (workflow) => {
          this.workflowVersion = workflow.version ?? 0;
          this.workflowName = workflow.name ?? '';
          this.workflowDescription = workflow.description ?? '';
          this.workflowIsActive = workflow.isActive ?? false;
          this.workflowForm.patchValue({ title: this.workflowName, description: this.workflowDescription });
          await this.loadXml(workflow.bpmnXml ?? '');
        },
        error: (err) => {
          console.error('Failed to load workflow', err);
          this.snackBar.open('Failed to load workflow', 'Close', { duration: 3000 });
          this.router.navigate(['/workflows']);
        }
      });
    }
  }

  private async loadXml(xml: string): Promise<void> {
    try {
      await this.bpmnInstance.importXML(xml);
      const canvas = this.bpmnInstance.get('canvas');

      canvas.zoom('fit-viewport', 'auto');
      setTimeout(() => {
        canvas.resized();
        canvas.zoom('fit-viewport', 'auto');
      }, 1);

      this.applyTransitionRuleCss();
    } catch (err) {
      console.error('Failed to render workflow', err);
    }
  }

  zoomIn() { this.bpmnInstance.get('zoomScroll').stepZoom(1); }
  zoomOut() { this.bpmnInstance.get('zoomScroll').stepZoom(-1); }
  resetZoom() { this.bpmnInstance.get('canvas').zoom('fit-viewport', 'auto'); }

  private async getUpdatedBpmnXml(): Promise<string> {
    const modeling = this.bpmnInstance.get('modeling');
    const bpmnFactory = this.bpmnInstance.get('bpmnFactory');
    const rootElement = this.bpmnInstance.get('canvas').getRootElement();

    const title = this.workflowForm.value.title;
    const description = this.workflowForm.value.description;

    const documentation = bpmnFactory.create('bpmn:Documentation', { text: description || '' });
    modeling.updateProperties(rootElement, {
      name: title,
      documentation: [documentation]
    });

    const { xml } = await this.bpmnInstance.saveXML({ format: true });
    return xml;
  }

  async submitWorkflow() {
    if (this.workflowForm.invalid) {
      this.workflowForm.markAllAsTouched();
      return;
    }

    try {
      const xml = await this.getUpdatedBpmnXml();
      const payload: WorkflowSaveDto = { bpmnXml: xml };

      if (this.mode === 'edit') {
        this.workflowService.editWorkflow(this.workflowId ?? 0, payload).subscribe({
          next: (workflow) => {
            this.snackBar.open('Workflow updated successfully!', 'Close', { duration: 2000 });
            this.router.navigate(['/workflows/view/', workflow.id]);
          },
          error: (err) => {
            console.error('Failed to update workflow', err);
            this.snackBar.open('Failed to update workflow', 'Close', { duration: 3000 });
          }
        });
      } else {
        this.workflowService.saveWorkflow(payload).subscribe({
          next: () => {
            this.snackBar.open('Workflow saved successfully!', 'Close', { duration: 2000 });
            this.router.navigate(['/workflows']);
          },
          error: (err) => {
            console.error('Failed to save workflow', err);
            this.snackBar.open('Failed to save workflow', 'Close', { duration: 3000 });
          }
        });
      }
    } catch (err) {
      console.error('Failed to process workflow', err);
      this.snackBar.open('Failed to process workflow', 'Close', { duration: 3000 });
    }
  }

  async exportXML() {
    try {
      const xml = await this.getUpdatedBpmnXml();
      const blob = new Blob([xml], { type: 'application/xml' });
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      const title = this.workflowForm.value.title;
      a.download = `${title || 'workflow'}.bpmn`;
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
        await this.loadXml(e.target.result);
      };
      reader.readAsText(file);
    }
    event.target.value = '';
  }

  navigateToEdit() {
    if (this.workflowId) {
      this.router.navigate(['/workflows/edit', this.workflowId]);
    }
  }

  ngOnDestroy() { this.bpmnInstance?.destroy(); }

  loadTransitionRules(selection: any) {
    this.selectedElementId = selection.id;
    const elementRegistry = this.bpmnInstance.get('elementRegistry');
    const element = elementRegistry.get(selection.id);

    if (!element?.businessObject) {
      console.warn(`Could not find businessObject for ID: ${selection.id}`);
      this.resetRule();
      return;
    }

    const extensions = element.businessObject.extensionElements;
    if (extensions?.values) {
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
    const canvas = this.bpmnInstance.get('canvas');
    const elementRegistry = this.bpmnInstance.get('elementRegistry');

    elementRegistry.forEach((element: any) => {
      if (element.type === 'bpmn:SequenceFlow') {
        const extensions = element.businessObject.extensionElements;
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

const dummyBpmnXml = `
<bpmn:definitions id="Definitions_1" targetNamespace="http://bpmn.io/schema/bpmn">
<bpmn:process id="PR_Tiered_Approval_001" name="" isExecutable="false">
<bpmn:startEvent id="Event_1if1b7g" name="Start Procurement">
<bpmn:outgoing>Flow_1hlbu7w</bpmn:outgoing>
</bpmn:startEvent>
<bpmn:task id="Activity_1u1p6ue" name="Fill in Details and Upload Vendor Quotes">
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
<bpmn:incoming>Flow_04ni0t0</bpmn:incoming>
<bpmn:outgoing>Flow_0vl4es9</bpmn:outgoing>
</bpmn:task>
<bpmn:task id="Activity_124j12i" name="Normal Finance Review">
<bpmn:incoming>Flow_0z8w32s</bpmn:incoming>
<bpmn:outgoing>Flow_1pumtq5</bpmn:outgoing>
</bpmn:task>
<bpmn:task id="Activity_05zq0ij" name="Detailed Finance Review">
<bpmn:incoming>Flow_1kxm0s8</bpmn:incoming>
<bpmn:outgoing>Flow_0nupddp</bpmn:outgoing>
</bpmn:task>
<bpmn:sequenceFlow id="Flow_1kxm0s8" name="budget > $10.000" sourceRef="Gateway_0dumvhe" targetRef="Activity_05zq0ij"/>
<bpmn:sequenceFlow id="Flow_0z8w32s" name="$500 > budget > $10.000" sourceRef="Gateway_0dumvhe" targetRef="Activity_124j12i"/>
<bpmn:sequenceFlow id="Flow_04ni0t0" name="budget < $500" sourceRef="Gateway_0dumvhe" targetRef="Activity_0mib3l3"/>
<bpmn:exclusiveGateway id="Gateway_0jludpd">
<bpmn:incoming>Flow_1pumtq5</bpmn:incoming>
<bpmn:incoming>Flow_0nupddp</bpmn:incoming>
<bpmn:outgoing>Flow_1s24xzj</bpmn:outgoing>
<bpmn:outgoing>Flow_0m5sv3t</bpmn:outgoing>
</bpmn:exclusiveGateway>
<bpmn:sequenceFlow id="Flow_1pumtq5" name="accept / reject" sourceRef="Activity_124j12i" targetRef="Gateway_0jludpd"/>
<bpmn:sequenceFlow id="Flow_0nupddp" name="accept / reject" sourceRef="Activity_05zq0ij" targetRef="Gateway_0jludpd"/>
<bpmn:sequenceFlow id="Flow_1s24xzj" name="Request rejected" sourceRef="Gateway_0jludpd" targetRef="Activity_1u1p6ue"/>
<bpmn:endEvent id="Event_0zjfdeg">
<bpmn:incoming>Flow_0m5sv3t</bpmn:incoming>
<bpmn:incoming>Flow_0vl4es9</bpmn:incoming>
</bpmn:endEvent>
<bpmn:sequenceFlow id="Flow_0m5sv3t" name="Request accepted" sourceRef="Gateway_0jludpd" targetRef="Event_0zjfdeg"/>
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
